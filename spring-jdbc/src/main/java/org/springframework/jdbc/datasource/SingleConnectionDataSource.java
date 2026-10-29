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

package org.springframework.jdbc.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link SmartDataSource} 的实现，包装单个使用后不关闭的 JDBC Connection。
 * 显然，它不支持多线程。
 *
 * <p>请注意，在关闭时，应该有人通过 {@code close()} 方法关闭底层 Connection。
 * 如果客户端代码能感知 SmartDataSource（例如使用
 * {@code DataSourceUtils.releaseConnection}），则永远不会在 Connection 句柄上调用 close。
 *
 * <p>如果客户端代码会假定 Connection 来自连接池而调用 {@code close()}
 * （例如使用持久化工具时），请将 "suppressClose" 设置为 "true"。
 * 这样将返回一个抑制关闭操作的代理，而不是物理 Connection。
 *
 * <p>此类主要用于测试。例如，它使得期望在 DataSource 上工作的代码
 * 可以在应用服务器之外轻松测试。与 {@link DriverManagerDataSource} 不同，
 * 它始终重用同一个 Connection，避免了物理 Connection 的过度创建。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConnection()
 * @see java.sql.Connection#close()
 * @see DataSourceUtils#releaseConnection
 */
public class SingleConnectionDataSource extends DriverManagerDataSource implements SmartDataSource, DisposableBean {

	/** 是否创建抑制关闭操作的代理？ */
	private boolean suppressClose;

	/** 是否覆盖自动提交状态？ */
	@Nullable
	private Boolean autoCommit;

	/** 被包装的 Connection。 */
	@Nullable
	private Connection target;

	/** 代理 Connection。 */
	@Nullable
	private Connection connection;

	/** 共享 Connection 的同步监视器。 */
	private final Object connectionMonitor = new Object();


	/**
	 * 用于 Bean 风格配置的构造函数。
	 */
	public SingleConnectionDataSource() {
	}

	/**
	 * 使用给定的标准 DriverManager 参数创建一个新的 SingleConnectionDataSource。
	 * @param url the JDBC URL to use for accessing the DriverManager
	 * @param username the JDBC username to use for accessing the DriverManager
	 * @param password the JDBC password to use for accessing the DriverManager
	 * @param suppressClose if the returned Connection should be a
	 * close-suppressing proxy or the physical Connection
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 */
	public SingleConnectionDataSource(String url, String username, String password, boolean suppressClose) {
		super(url, username, password);
		this.suppressClose = suppressClose;
	}

	/**
	 * 使用给定的标准 DriverManager 参数创建一个新的 SingleConnectionDataSource。
	 * @param url the JDBC URL to use for accessing the DriverManager
	 * @param suppressClose if the returned Connection should be a
	 * close-suppressing proxy or the physical Connection
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 */
	public SingleConnectionDataSource(String url, boolean suppressClose) {
		super(url);
		this.suppressClose = suppressClose;
	}

	/**
	 * 使用给定的 Connection 创建一个新的 SingleConnectionDataSource。
	 * @param target underlying target Connection
	 * @param suppressClose if the Connection should be wrapped with a Connection that
	 * suppresses {@code close()} calls (to allow for normal {@code close()}
	 * usage in applications that expect a pooled Connection but do not know our
	 * SmartDataSource interface)
	 */
	public SingleConnectionDataSource(Connection target, boolean suppressClose) {
		Assert.notNull(target, "Connection must not be null");
		this.target = target;
		this.suppressClose = suppressClose;
		this.connection = (suppressClose ? getCloseSuppressingConnectionProxy(target) : target);
	}


	/**
	 * 设置返回的 Connection 应是抑制关闭操作的代理
	 * 还是物理 Connection。
	 */
	public void setSuppressClose(boolean suppressClose) {
		this.suppressClose = suppressClose;
	}

	/**
	 * 返回返回的 Connection 将是抑制关闭操作的代理
	 * 还是物理 Connection。
	 */
	protected boolean isSuppressClose() {
		return this.suppressClose;
	}

	/**
	 * 设置是否应覆盖返回的 Connection 的 "autoCommit" 设置。
	 */
	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = (autoCommit);
	}

	/**
	 * 返回是否应覆盖返回的 Connection 的 "autoCommit" 设置。
	 * @return the "autoCommit" value, or {@code null} if none to be applied
	 */
	@Nullable
	protected Boolean getAutoCommitValue() {
		return this.autoCommit;
	}


	@Override
	public Connection getConnection() throws SQLException {
		synchronized (this.connectionMonitor) {
			if (this.connection == null) {
				// No underlying Connection -> lazy init via DriverManager.
				initConnection();
			}
			if (this.connection.isClosed()) {
				throw new SQLException(
						"Connection was closed in SingleConnectionDataSource. Check that user code checks " +
						"shouldClose() before closing Connections, or set 'suppressClose' to 'true'");
			}
			return this.connection;
		}
	}

	/**
	 * 指定自定义用户名和密码对于单个 Connection 而言没有意义。
	 * 如果给定的用户名和密码相同，则返回这个唯一的 Connection；
	 * 否则抛出 SQLException。
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		if (ObjectUtils.nullSafeEquals(username, getUsername()) &&
				ObjectUtils.nullSafeEquals(password, getPassword())) {
			return getConnection();
		}
		else {
			throw new SQLException("SingleConnectionDataSource does not support custom username and password");
		}
	}

	/**
	 * 这里只有一个 Connection：在“归还”到“连接池”时不要关闭它。
	 */
	@Override
	public boolean shouldClose(Connection con) {
		synchronized (this.connectionMonitor) {
			return (con != this.connection && con != this.target);
		}
	}

	/**
	 * 关闭底层 Connection。
	 * 此 DataSource 的提供方需要负责正确地关闭。
	 * <p>由于此 Bean 实现了 DisposableBean，Bean 工厂会在销毁其缓存的
	 * 单例时自动调用此方法。
	 */
	@Override
	public void destroy() {
		synchronized (this.connectionMonitor) {
			closeConnection();
		}
	}


	/**
	 * 通过 DriverManager 初始化底层 Connection。
	 */
	public void initConnection() throws SQLException {
		if (getUrl() == null) {
			throw new IllegalStateException("'url' property is required for lazily initializing a Connection");
		}
		synchronized (this.connectionMonitor) {
			closeConnection();
			this.target = getConnectionFromDriver(getUsername(), getPassword());
			prepareConnection(this.target);
			if (logger.isDebugEnabled()) {
				logger.debug("Established shared JDBC Connection: " + this.target);
			}
			this.connection = (isSuppressClose() ? getCloseSuppressingConnectionProxy(this.target) : this.target);
		}
	}

	/**
	 * 重置底层的共享 Connection，以便在下次访问时重新初始化。
	 */
	public void resetConnection() {
		synchronized (this.connectionMonitor) {
			closeConnection();
			this.target = null;
			this.connection = null;
		}
	}

	/**
	 * 在暴露之前准备给定的 Connection。
	 * <p>默认实现会在必要时应用自动提交标志。
	 * 可以在子类中重写。
	 * @param con the Connection to prepare
	 * @see #setAutoCommit
	 */
	protected void prepareConnection(Connection con) throws SQLException {
		Boolean autoCommit = getAutoCommitValue();
		if (autoCommit != null && con.getAutoCommit() != autoCommit) {
			con.setAutoCommit(autoCommit);
		}
	}

	/**
	 * 关闭底层的共享 Connection。
	 */
	private void closeConnection() {
		if (this.target != null) {
			try {
				this.target.close();
			}
			catch (Throwable ex) {
				logger.info("Could not close shared JDBC Connection", ex);
			}
		}
	}

	/**
	 * 用代理包装给定的 Connection，该代理将每个方法调用都委托给它，
	 * 但抑制 close 调用。
	 * @param target the original Connection to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getCloseSuppressingConnectionProxy(Connection target) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new CloseSuppressingInvocationHandler(target));
	}


	/**
	 * 调用处理器，抑制对 JDBC Connection 的 close 调用。
	 */
	private static class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		public CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

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
				case "isClosed":
					return this.target.isClosed();
				case "getTargetConnection":
					// Handle getTargetConnection method: return underlying Connection.
					return this.target;
				case "unwrap":
					return (((Class<?>) args[0]).isInstance(proxy) ? proxy : this.target.unwrap((Class<?>) args[0]));
				case "isWrapperFor":
					return (((Class<?>) args[0]).isInstance(proxy) || this.target.isWrapperFor((Class<?>) args[0]));
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
