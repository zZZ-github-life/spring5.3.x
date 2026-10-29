/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jca.cci.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * CCI ConnectionFactory 适配器，在所有 {@code getConnection} 调用中返回相同的 Connection，
 * 并忽略 {@code Connection.close()} 调用。
 *
 * <p>适用于测试和独立环境，用于保持对多次 CciTemplate 调用使用相同的 Connection，
 * 而无需使用池化 ConnectionFactory，也可跨越任意数量的事务。
 *
 * <p>你可以直接传入 CCI Connection，或者让此工厂通过给定的目标 ConnectionFactory 延迟创建 Connection。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #getConnection()
 * @see javax.resource.cci.Connection#close()
 * @see org.springframework.jca.cci.core.CciTemplate
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@SuppressWarnings("serial")
public class SingleConnectionFactory extends DelegatingConnectionFactory implements DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	/** 包装的 Connection。 */
	@Nullable
	private Connection target;

	/** 代理 Connection。 */
	@Nullable
	private Connection connection;

	/** 共享 Connection 的同步监视器。 */
	private final Object connectionMonitor = new Object();


	/**
	 * 创建新的 SingleConnectionFactory，用于 bean 风格的使用。
	 * @see #setTargetConnectionFactory
	 */
	public SingleConnectionFactory() {
	}

	/**
	 * 创建新的 SingleConnectionFactory，始终返回给定的 Connection。
	 * @param target the single Connection
	 */
	public SingleConnectionFactory(Connection target) {
		Assert.notNull(target, "Target Connection must not be null");
		this.target = target;
		this.connection = getCloseSuppressingConnectionProxy(target);
	}

	/**
	 * 创建新的 SingleConnectionFactory，始终返回通过给定的目标 ConnectionFactory
	 * 延迟创建的单个 Connection。
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public SingleConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "Target ConnectionFactory must not be null");
		setTargetConnectionFactory(targetConnectionFactory);
	}


	/**
	 * 确保已设置 Connection 或 ConnectionFactory。
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.connection == null && getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Connection or 'targetConnectionFactory' is required");
		}
	}


	@Override
	public Connection getConnection() throws ResourceException {
		synchronized (this.connectionMonitor) {
			if (this.connection == null) {
				initConnection();
			}
			return this.connection;
		}
	}

	@Override
	public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
		throw new NotSupportedException(
				"SingleConnectionFactory does not support custom ConnectionSpec");
	}

	/**
	 * 关闭底层的 Connection。
	 * 此 ConnectionFactory 的提供者需要负责正确关闭。
	 * <p>由于此 bean 实现了 DisposableBean，bean 工厂会在销毁其缓存的单例时自动调用此方法。
	 */
	@Override
	public void destroy() {
		resetConnection();
	}


	/**
	 * 初始化底层的单个 Connection。
	 * <p>如果已经存在底层 Connection，则关闭并重新初始化。
	 * @throws javax.resource.ResourceException if thrown by CCI API methods
	 */
	public void initConnection() throws ResourceException {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalStateException(
					"'targetConnectionFactory' is required for lazily initializing a Connection");
		}
		synchronized (this.connectionMonitor) {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = doCreateConnection();
			prepareConnection(this.target);
			if (logger.isDebugEnabled()) {
				logger.debug("Established shared CCI Connection: " + this.target);
			}
			this.connection = getCloseSuppressingConnectionProxy(this.target);
		}
	}

	/**
	 * 重置底层共享的 Connection，在下次访问时重新初始化。
	 */
	public void resetConnection() {
		synchronized (this.connectionMonitor) {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = null;
			this.connection = null;
		}
	}

	/**
	 * 通过此模板的 ConnectionFactory 创建 CCI Connection。
	 * @return the new CCI Connection
	 * @throws javax.resource.ResourceException if thrown by CCI API methods
	 */
	protected Connection doCreateConnection() throws ResourceException {
		ConnectionFactory connectionFactory = getTargetConnectionFactory();
		Assert.state(connectionFactory != null, "No 'targetConnectionFactory' set");
		return connectionFactory.getConnection();
	}

	/**
	 * 在给定的 Connection 暴露之前进行准备。
	 * <p>默认实现为空。可以在子类中重写。
	 * @param con the Connection to prepare
	 */
	protected void prepareConnection(Connection con) throws ResourceException {
	}

	/**
	 * 关闭给定的 Connection。
	 * @param con the Connection to close
	 */
	protected void closeConnection(Connection con) {
		try {
			con.close();
		}
		catch (Throwable ex) {
			logger.warn("Could not close shared CCI Connection", ex);
		}
	}

	/**
	 * 使用抑制 close 调用的代理包装给定的 Connection。
	 * 这允许应用程序代码像处理来自 CCI ConnectionFactory 的普通 Connection 一样
	 * 处理特殊的框架 Connection。
	 * @param target the original Connection to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getCloseSuppressingConnectionProxy(Connection target) {
		return (Connection) Proxy.newProxyInstance(
				Connection.class.getClassLoader(),
				new Class<?>[] {Connection.class},
				new CloseSuppressingInvocationHandler(target));
	}


	/**
	 * 抑制 CCI Connection close 调用的调用处理器。
	 */
	private static final class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		private CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of Connection proxy.
					return System.identityHashCode(proxy);
				case "close":
					// Handle close method: don't pass the call on.
					return null;
			}

			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
