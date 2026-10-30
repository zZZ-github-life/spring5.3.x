/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.remoting.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * 用于定位 {@link java.rmi.registry.Registry} 并将其暴露出来供 Bean 引用的 {@link FactoryBean}。如
 * 果尚不存在，也可以即时创建一个本地 RMI 注册表。
 *
 * <p>可用于设置实际的 Registry 对象，并将其传递给需要与 RMI 打交道的应用对象。这类对象的一个例子是
 * Spring 的 {@link RmiServiceExporter}，它要么使用传入的 Registry 引用，要么回退到由其本地属性和默
 * 认值所指定的注册表。
 *
 * <p>对于强制在给定端口创建本地 RMI 注册表也很有用，例如用于 JMX 连接器。如果与
 * {@link org.springframework.jmx.support.ConnectorServerFactoryBean} 一起使用，建议将连接器定义
 * （ConnectorServerFactoryBean）标记为 "depends-on" 该注册表定义（RmiRegistryFactoryBean），以保证先
 * 启动注册表。
 *
 * <p>注意：此类的实现与 {@link RmiServiceExporter} 中相应的逻辑一致，并且也提供相同的自定义钩子。
 * RmiServiceExporter 出于便利实现了自己的注册表查找：直接依赖注册表默认值的情况非常常见。
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see RmiServiceExporter#setRegistry
 * @see org.springframework.jmx.support.ConnectorServerFactoryBean
 * @see java.rmi.registry.Registry
 * @see java.rmi.registry.LocateRegistry
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class RmiRegistryFactoryBean implements FactoryBean<Registry>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private String host;

	private int port = Registry.REGISTRY_PORT;

	private RMIClientSocketFactory clientSocketFactory;

	private RMIServerSocketFactory serverSocketFactory;

	private Registry registry;

	private boolean alwaysCreate = false;

	private boolean created = false;


	/**
	 * 设置所导出 RMI 服务的注册表主机，即 {@code rmi://HOST:port/name}
	 * <p>默认为 localhost。
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 返回所导出 RMI 服务的注册表主机。
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * 设置所导出 RMI 服务的注册表端口，即 {@code rmi://host:PORT/name}
	 * <p>默认为 {@code Registry.REGISTRY_PORT}（1099）。
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 返回所导出 RMI 服务的注册表端口。
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 设置用于 RMI 注册表的自定义 RMI 客户端套接字工厂。
	 * <p>如果给定的对象还实现了 {@code java.rmi.server.RMIServerSocketFactory}，它也会自动注册为服务器
	 * 套接字工厂。
	 * @see #setServerSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see java.rmi.registry.LocateRegistry#getRegistry(String, int, java.rmi.server.RMIClientSocketFactory)
	 */
	public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
		this.clientSocketFactory = clientSocketFactory;
	}

	/**
	 * 设置用于 RMI 注册表的自定义 RMI 服务器套接字工厂。
	 * <p>仅在客户端套接字工厂尚未实现 {@code java.rmi.server.RMIServerSocketFactory} 时才需要指定。
	 * @see #setClientSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see java.rmi.registry.LocateRegistry#createRegistry(int, RMIClientSocketFactory, java.rmi.server.RMIServerSocketFactory)
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * 设置是否始终在进程内创建注册表，而不尝试在指定端口定位已存在的注册表。
	 * <p>默认为 "false"。如果你无论如何都打算创建新的注册表，可将此标志切换为 "true"，以避免定位已存在
	 * 注册表的开销。
	 */
	public void setAlwaysCreate(boolean alwaysCreate) {
		this.alwaysCreate = alwaysCreate;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		// Check socket factories for registry.
		if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
			this.serverSocketFactory = (RMIServerSocketFactory) this.clientSocketFactory;
		}
		if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
				(this.clientSocketFactory == null && this.serverSocketFactory != null)) {
			throw new IllegalArgumentException(
					"Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
		}

		// Fetch RMI registry to expose.
		this.registry = getRegistry(this.host, this.port, this.clientSocketFactory, this.serverSocketFactory);
	}


	/**
	 * 定位或创建 RMI 注册表。
	 * @param registryHost 要使用的注册表主机
	 * （如果指定了此项，则不会隐式创建 RMI 注册表）
	 * @param registryPort 要使用的注册表端口
	 * @param clientSocketFactory 用于注册表的 RMI 客户端套接字工厂（如果有）
	 * @param serverSocketFactory 用于注册表的 RMI 服务器套接字工厂（如果有）
	 * @return RMI 注册表
	 * @throws java.rmi.RemoteException 如果无法定位或创建注册表
	 */
	protected Registry getRegistry(String registryHost, int registryPort,
			@Nullable RMIClientSocketFactory clientSocketFactory, @Nullable RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (registryHost != null) {
			// Host explicitly specified: only lookup possible.
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for RMI registry at port '" + registryPort + "' of host [" + registryHost + "]");
			}
			Registry reg = LocateRegistry.getRegistry(registryHost, registryPort, clientSocketFactory);
			testRegistry(reg);
			return reg;
		}

		else {
			return getRegistry(registryPort, clientSocketFactory, serverSocketFactory);
		}
	}

	/**
	 * 定位或创建 RMI 注册表。
	 * @param registryPort 要使用的注册表端口
	 * @param clientSocketFactory 用于注册表的 RMI 客户端套接字工厂（如果有）
	 * @param serverSocketFactory 用于注册表的 RMI 服务器套接字工厂（如果有）
	 * @return RMI 注册表
	 * @throws RemoteException 如果无法定位或创建注册表
	 */
	protected Registry getRegistry(int registryPort,
			@Nullable RMIClientSocketFactory clientSocketFactory, @Nullable RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (clientSocketFactory != null) {
			if (this.alwaysCreate) {
				logger.debug("Creating new RMI registry");
				this.created = true;
				return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for RMI registry at port '" + registryPort + "', using custom socket factory");
			}
			synchronized (LocateRegistry.class) {
				try {
					// Retrieve existing registry.
					Registry reg = LocateRegistry.getRegistry(null, registryPort, clientSocketFactory);
					testRegistry(reg);
					return reg;
				}
				catch (RemoteException ex) {
					logger.trace("RMI registry access threw exception", ex);
					logger.debug("Could not detect RMI registry - creating new one");
					// Assume no registry found -> create new one.
					this.created = true;
					return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
				}
			}
		}

		else {
			return getRegistry(registryPort);
		}
	}

	/**
	 * 定位或创建 RMI 注册表。
	 * @param registryPort 要使用的注册表端口
	 * @return RMI 注册表
	 * @throws RemoteException 如果无法定位或创建注册表
	 */
	protected Registry getRegistry(int registryPort) throws RemoteException {
		if (this.alwaysCreate) {
			logger.debug("Creating new RMI registry");
			this.created = true;
			return LocateRegistry.createRegistry(registryPort);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for RMI registry at port '" + registryPort + "'");
		}
		synchronized (LocateRegistry.class) {
			try {
				// Retrieve existing registry.
				Registry reg = LocateRegistry.getRegistry(registryPort);
				testRegistry(reg);
				return reg;
			}
			catch (RemoteException ex) {
				logger.trace("RMI registry access threw exception", ex);
				logger.debug("Could not detect RMI registry - creating new one");
				// Assume no registry found -> create new one.
				this.created = true;
				return LocateRegistry.createRegistry(registryPort);
			}
		}
	}

	/**
	 * 测试给定的 RMI 注册表，在其上调用某个操作以检查它是否仍然处于活动状态。
	 * <p>默认实现调用 {@code Registry.list()}。
	 * @param registry 要测试的 RMI 注册表
	 * @throws RemoteException 如果由注册表方法抛出
	 * @see java.rmi.registry.Registry#list()
	 */
	protected void testRegistry(Registry registry) throws RemoteException {
		registry.list();
	}


	@Override
	public Registry getObject() throws Exception {
		return this.registry;
	}

	@Override
	public Class<? extends Registry> getObjectType() {
		return (this.registry != null ? this.registry.getClass() : Registry.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 在 Bean 工厂关闭时取消导出 RMI 注册表，前提是此 Bean 确实创建了注册表。
	 */
	@Override
	public void destroy() throws RemoteException {
		if (this.created) {
			logger.debug("Unexporting RMI registry");
			UnicastRemoteObject.unexportObject(this.registry, true);
		}
	}

}
