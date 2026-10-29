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

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * RMI 导出器，将指定的服务以指定名称暴露为 RMI 对象。这样的服务可以通过普通 RMI 或通过
 * {@link RmiProxyFactoryBean} 访问。还支持通过 RMI 调用器暴露任意的非 RMI 服务，由
 * {@link RmiClientInterceptor} / {@link RmiProxyFactoryBean} 对此类调用器的自动检测来访问。
 *
 * <p>使用 RMI 调用器时，RMI 通信工作在 {@link RmiInvocationHandler} 层面，任何服务都只需要一个存根。
 * 服务接口不必扩展 {@code java.rmi.Remote}，也不必在所有方法上抛出
 * {@code java.rmi.RemoteException}，但入参和出参必须是可序列化的。
 *
 * <p>与 Hessian 相比，RMI 的主要优势在于序列化。实际上，任何可序列化的 Java 对象都可以轻松传输。
 * Hessian 有自己的一套（反）序列化机制，但它基于 HTTP，因此比 RMI 容易配置得多。或者，可以考虑
 * Spring 的 HTTP 调用器，将 Java 序列化与基于 HTTP 的传输结合起来。
 *
 * <p>注意：RMI 会尽力获取完全限定的主机名。如果无法确定，它会回退为使用 IP 地址。取决于你的网络配
 * 置，某些情况下它会把该 IP 解析为回环地址。为确保 RMI 使用绑定到正确网络接口的主机名，应使用 "-D"
 * JVM 参数将 {@code java.rmi.server.hostname} 属性传给将要导出注册表和/或服务的 JVM。例如：
 * {@code -Djava.rmi.server.hostname=myserver.com}
 *
 * @author Juergen Hoeller
 * @since 13.05.2003
 * @see RmiClientInterceptor
 * @see RmiProxyFactoryBean
 * @see java.rmi.Remote
 * @see java.rmi.RemoteException
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class RmiServiceExporter extends RmiBasedExporter implements InitializingBean, DisposableBean {

	private String serviceName;

	private int servicePort = 0;  // anonymous port

	private RMIClientSocketFactory clientSocketFactory;

	private RMIServerSocketFactory serverSocketFactory;

	private Registry registry;

	private String registryHost;

	private int registryPort = Registry.REGISTRY_PORT;

	private RMIClientSocketFactory registryClientSocketFactory;

	private RMIServerSocketFactory registryServerSocketFactory;

	private boolean alwaysCreateRegistry = false;

	private boolean replaceExistingBinding = true;

	private Remote exportedObject;

	private boolean createdRegistry = false;


	/**
	 * 设置所导出 RMI 服务的名称，即 {@code rmi://host:port/NAME}
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * 设置所导出 RMI 服务将使用的端口。
	 * <p>默认为 0（匿名端口）。
	 */
	public void setServicePort(int servicePort) {
		this.servicePort = servicePort;
	}

	/**
	 * 设置用于导出服务的自定义 RMI 客户端套接字工厂。
	 * <p>如果给定的对象还实现了 {@code java.rmi.server.RMIServerSocketFactory}，它也会自动注册为服务器
	 * 套接字工厂。
	 * @see #setServerSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see UnicastRemoteObject#exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)
	 */
	public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
		this.clientSocketFactory = clientSocketFactory;
	}

	/**
	 * 设置用于导出服务的自定义 RMI 服务器套接字工厂。
	 * <p>仅在客户端套接字工厂尚未实现 {@code java.rmi.server.RMIServerSocketFactory} 时才需要指定。
	 * @see #setClientSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see UnicastRemoteObject#exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * 指定要向其注册所导出服务的 RMI 注册表。通常与 RmiRegistryFactoryBean 结合使用。
	 * <p>或者，你可以在本地指定所有注册表属性。此导出器随后会尝试定位指定的注册表，并在适当的情况下自
	 * 动创建新的本地注册表。
	 * <p>默认是在默认端口（1099）上的本地注册表，必要时即时创建。
	 * @see RmiRegistryFactoryBean
	 * @see #setRegistryHost
	 * @see #setRegistryPort
	 * @see #setRegistryClientSocketFactory
	 * @see #setRegistryServerSocketFactory
	 */
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	/**
	 * 设置所导出 RMI 服务的注册表主机，即 {@code rmi://HOST:port/name}
	 * <p>默认为 localhost。
	 */
	public void setRegistryHost(String registryHost) {
		this.registryHost = registryHost;
	}

	/**
	 * 设置所导出 RMI 服务的注册表端口，即 {@code rmi://host:PORT/name}
	 * <p>默认为 {@code Registry.REGISTRY_PORT}（1099）。
	 * @see java.rmi.registry.Registry#REGISTRY_PORT
	 */
	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * 设置用于 RMI 注册表的自定义 RMI 客户端套接字工厂。
	 * <p>如果给定的对象还实现了 {@code java.rmi.server.RMIServerSocketFactory}，它也会自动注册为服务器
	 * 套接字工厂。
	 * @see #setRegistryServerSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see LocateRegistry#getRegistry(String, int, RMIClientSocketFactory)
	 */
	public void setRegistryClientSocketFactory(RMIClientSocketFactory registryClientSocketFactory) {
		this.registryClientSocketFactory = registryClientSocketFactory;
	}

	/**
	 * 设置用于 RMI 注册表的自定义 RMI 服务器套接字工厂。
	 * <p>仅在客户端套接字工厂尚未实现 {@code java.rmi.server.RMIServerSocketFactory} 时才需要指定。
	 * @see #setRegistryClientSocketFactory
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.server.RMIServerSocketFactory
	 * @see LocateRegistry#createRegistry(int, RMIClientSocketFactory, RMIServerSocketFactory)
	 */
	public void setRegistryServerSocketFactory(RMIServerSocketFactory registryServerSocketFactory) {
		this.registryServerSocketFactory = registryServerSocketFactory;
	}

	/**
	 * 设置是否始终在进程内创建注册表，而不尝试在指定端口定位已存在的注册表。
	 * <p>默认为 "false"。如果你无论如何都打算创建新的注册表，可将此标志切换为 "true"，以避免定位已存在
	 * 注册表的开销。
	 */
	public void setAlwaysCreateRegistry(boolean alwaysCreateRegistry) {
		this.alwaysCreateRegistry = alwaysCreateRegistry;
	}

	/**
	 * 设置是否替换 RMI 注册表中已有的绑定，也就是说，在注册表中出现命名冲突时，是否直接用指定的服务覆
	 * 盖已有的绑定。
	 * <p>默认为 "true"，假定该导出器服务名已有的绑定是上一次执行偶然遗留的。将其切换为 "false" 可使导
	 * 出器在此类情况下失败，表明已经绑定了某个 RMI 对象。
	 */
	public void setReplaceExistingBinding(boolean replaceExistingBinding) {
		this.replaceExistingBinding = replaceExistingBinding;
	}


	@Override
	public void afterPropertiesSet() throws RemoteException {
		prepare();
	}

	/**
	 * 初始化此服务导出器，将服务注册为 RMI 对象。
	 * <p>如果指定端口上不存在 RMI 注册表，则创建一个。
	 * @throws RemoteException if service registration failed
	 */
	public void prepare() throws RemoteException {
		checkService();

		if (this.serviceName == null) {
			throw new IllegalArgumentException("Property 'serviceName' is required");
		}

		// Check socket factories for exported object.
		if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
			this.serverSocketFactory = (RMIServerSocketFactory) this.clientSocketFactory;
		}
		if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
				(this.clientSocketFactory == null && this.serverSocketFactory != null)) {
			throw new IllegalArgumentException(
					"Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
		}

		// Check socket factories for RMI registry.
		if (this.registryClientSocketFactory instanceof RMIServerSocketFactory) {
			this.registryServerSocketFactory = (RMIServerSocketFactory) this.registryClientSocketFactory;
		}
		if (this.registryClientSocketFactory == null && this.registryServerSocketFactory != null) {
			throw new IllegalArgumentException(
					"RMIServerSocketFactory without RMIClientSocketFactory for registry not supported");
		}

		this.createdRegistry = false;

		// Determine RMI registry to use.
		if (this.registry == null) {
			this.registry = getRegistry(this.registryHost, this.registryPort,
				this.registryClientSocketFactory, this.registryServerSocketFactory);
			this.createdRegistry = true;
		}

		// Initialize and cache exported object.
		this.exportedObject = getObjectToExport();

		if (logger.isDebugEnabled()) {
			logger.debug("Binding service '" + this.serviceName + "' to RMI registry: " + this.registry);
		}

		// Export RMI object.
		if (this.clientSocketFactory != null) {
			UnicastRemoteObject.exportObject(
					this.exportedObject, this.servicePort, this.clientSocketFactory, this.serverSocketFactory);
		}
		else {
			UnicastRemoteObject.exportObject(this.exportedObject, this.servicePort);
		}

		// Bind RMI object to registry.
		try {
			if (this.replaceExistingBinding) {
				this.registry.rebind(this.serviceName, this.exportedObject);
			}
			else {
				this.registry.bind(this.serviceName, this.exportedObject);
			}
		}
		catch (AlreadyBoundException ex) {
			// Already an RMI object bound for the specified service name...
			unexportObjectSilently();
			throw new IllegalStateException(
					"Already an RMI object bound for name '"  + this.serviceName + "': " + ex.toString());
		}
		catch (RemoteException ex) {
			// Registry binding failed: let's unexport the RMI object as well.
			unexportObjectSilently();
			throw ex;
		}
	}


	/**
	 * 为此导出器定位或创建 RMI 注册表。
	 * @param registryHost the registry host to use (if this is specified,
	 * no implicit creation of a RMI registry will happen)
	 * @param registryPort the registry port to use
	 * @param clientSocketFactory the RMI client socket factory for the registry (if any)
	 * @param serverSocketFactory the RMI server socket factory for the registry (if any)
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
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
	 * 为此导出器定位或创建 RMI 注册表。
	 * @param registryPort the registry port to use
	 * @param clientSocketFactory the RMI client socket factory for the registry (if any)
	 * @param serverSocketFactory the RMI server socket factory for the registry (if any)
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(int registryPort,
			@Nullable RMIClientSocketFactory clientSocketFactory, @Nullable RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (clientSocketFactory != null) {
			if (this.alwaysCreateRegistry) {
				logger.debug("Creating new RMI registry");
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
					return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
				}
			}
		}

		else {
			return getRegistry(registryPort);
		}
	}

	/**
	 * 为此导出器定位或创建 RMI 注册表。
	 * @param registryPort the registry port to use
	 * @return the RMI registry
	 * @throws RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(int registryPort) throws RemoteException {
		if (this.alwaysCreateRegistry) {
			logger.debug("Creating new RMI registry");
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
				return LocateRegistry.createRegistry(registryPort);
			}
		}
	}

	/**
	 * 测试给定的 RMI 注册表，在其上调用某个操作以检查它是否仍然处于活动状态。
	 * <p>默认实现调用 {@code Registry.list()}。
	 * @param registry the RMI registry to test
	 * @throws RemoteException if thrown by registry methods
	 * @see java.rmi.registry.Registry#list()
	 */
	protected void testRegistry(Registry registry) throws RemoteException {
		registry.list();
	}


	/**
	 * 在 Bean 工厂关闭时从注册表中解绑该 RMI 服务。
	 */
	@Override
	public void destroy() throws RemoteException {
		if (logger.isDebugEnabled()) {
			logger.debug("Unbinding RMI service '" + this.serviceName +
					"' from registry" + (this.createdRegistry ? (" at port '" + this.registryPort + "'") : ""));
		}
		try {
			this.registry.unbind(this.serviceName);
		}
		catch (NotBoundException ex) {
			if (logger.isInfoEnabled()) {
				logger.info("RMI service '" + this.serviceName + "' is not bound to registry" +
						(this.createdRegistry ? (" at port '" + this.registryPort + "' anymore") : ""), ex);
			}
		}
		finally {
			unexportObjectSilently();
		}
	}

	/**
	 * 取消导出已注册的 RMI 对象，并记录出现的任何异常。
	 */
	private void unexportObjectSilently() {
		try {
			UnicastRemoteObject.unexportObject(this.exportedObject, true);
		}
		catch (NoSuchObjectException ex) {
			if (logger.isInfoEnabled()) {
				logger.info("RMI object for service '" + this.serviceName + "' is not exported anymore", ex);
			}
		}
	}

}
