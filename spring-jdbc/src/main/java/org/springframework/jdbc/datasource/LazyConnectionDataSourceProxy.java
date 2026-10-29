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
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;

/**
 * 目标 DataSource 的代理，延迟获取实际的 JDBC Connection，
 * 即直到第一次创建 Statement 时才获取。诸如自动提交模式、事务隔离级别
 * 和只读模式之类的连接初始化属性会被保留，并在实际获取 Connection
 * （如果有）后立即应用到实际的 JDBC Connection 上。
 * 因此，如果尚未创建任何 Statement，commit 和 rollback 调用将被忽略。
 *
 * <p>此 DataSource 代理可以避免在实际并不需要时从连接池获取 JDBC Connection。
 * JDBC 事务控制可以在不从连接池获取 Connection、也不与数据库通信的情况下
 * 进行；这些操作会在首次创建 JDBC Statement 时延迟执行。
 *
 * <p><b>如果同时配置了 LazyConnectionDataSourceProxy 和
 * TransactionAwareDataSourceProxy，请确保后者是最外层的 DataSource。</b>
 * 在这种场景下，数据访问代码将与事务感知的 DataSource 交互，
 * 而后者又会与 LazyConnectionDataSourceProxy 配合工作。
 *
 * <p>延迟获取物理 JDBC Connection 在通用的事务划分环境中尤其有益。
 * 它允许你在所有可能执行数据访问的方法上划分事务，
 * 而不必为并未实际发生的数据访问付出性能代价。
 *
 * <p>即使采用 DataSourceTransactionManager 或 HibernateTransactionManager
 * 等本地事务策略，此 DataSource 代理也能为你提供类似于 JTA 和事务性 JNDI
 * DataSource（由 Java EE 服务器提供）的行为。如果以 Spring 的
 * JtaTransactionManager 作为事务策略，则它不会带来额外价值。
 *
 * <p>对于 Hibernate 的只读操作，也建议延迟获取 JDBC Connection，
 * 特别是在结果很有希望从二级缓存中解析的情况下。这样可以避免为此类
 * 只读操作与数据库进行任何通信。使用非事务性读取也能获得同样的效果，
 * 但延迟获取 JDBC Connection 可以让你仍然在事务中执行读取。
 *
 * <p><b>注意：</b>此 DataSource 代理需要返回包装后的 Connection
 * （实现 {@link ConnectionProxy} 接口），以便处理实际 JDBC Connection 的
 * 延迟获取。请使用 {@link Connection#unwrap} 获取原生 JDBC Connection。
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see DataSourceTransactionManager
 */
public class LazyConnectionDataSourceProxy extends DelegatingDataSource {

	/** TransactionDefinition 的 Constants 实例。 */
	private static final Constants constants = new Constants(Connection.class);

	private static final Log logger = LogFactory.getLog(LazyConnectionDataSourceProxy.class);

	@Nullable
	private Boolean defaultAutoCommit;

	@Nullable
	private Integer defaultTransactionIsolation;


	/**
	 * 创建一个新的 LazyConnectionDataSourceProxy。
	 * @see #setTargetDataSource
	 */
	public LazyConnectionDataSourceProxy() {
	}

	/**
	 * 创建一个新的 LazyConnectionDataSourceProxy。
	 * @param targetDataSource the target DataSource
	 */
	public LazyConnectionDataSourceProxy(DataSource targetDataSource) {
		setTargetDataSource(targetDataSource);
		afterPropertiesSet();
	}


	/**
	 * 设置尚未获取目标 Connection 时要暴露的默认自动提交模式
	 * （此时实际的 JDBC Connection 默认值尚不清楚）。
	 * <p>如果未指定，默认值将通过启动时检查目标 Connection 来确定。
	 * 如果该检查失败，则默认值将在首次访问 Connection 时延迟确定。
	 * @see java.sql.Connection#setAutoCommit
	 */
	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		this.defaultAutoCommit = defaultAutoCommit;
	}

	/**
	 * 设置尚未获取目标 Connection 时要暴露的默认事务隔离级别
	 * （此时实际的 JDBC Connection 默认值尚不清楚）。
	 * <p>此属性接受 {@link java.sql.Connection} 接口中定义的 int 常量值
	 * （例如 8）；它主要用于编程方式使用。可考虑使用
	 * "defaultTransactionIsolationName" 属性按名称设置值
	 * （例如 "TRANSACTION_SERIALIZABLE"）。
	 * <p>如果未指定，默认值将通过启动时检查目标 Connection 来确定。
	 * 如果该检查失败，则默认值将在首次访问 Connection 时延迟确定。
	 * @see #setDefaultTransactionIsolationName
	 * @see java.sql.Connection#setTransactionIsolation
	 */
	public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
		this.defaultTransactionIsolation = defaultTransactionIsolation;
	}

	/**
	 * 通过 {@link java.sql.Connection} 中相应常量的名称设置默认事务隔离级别，
	 * 例如 "TRANSACTION_SERIALIZABLE"。
	 * @param constantName name of the constant
	 * @see #setDefaultTransactionIsolation
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 */
	public void setDefaultTransactionIsolationName(String constantName) {
		setDefaultTransactionIsolation(constants.asNumber(constantName).intValue());
	}


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// Determine default auto-commit and transaction isolation
		// via a Connection from the target DataSource, if possible.
		if (this.defaultAutoCommit == null || this.defaultTransactionIsolation == null) {
			try {
				try (Connection con = obtainTargetDataSource().getConnection()) {
					checkDefaultConnectionProperties(con);
				}
			}
			catch (SQLException ex) {
				logger.debug("Could not retrieve default auto-commit and transaction isolation settings", ex);
			}
		}
	}

	/**
	 * 检查默认连接属性（自动提交、事务隔离级别），
	 * 并保留它们，以便在不从目标 DataSource 获取实际 JDBC Connection
	 * 的情况下也能正确地暴露这些属性。
	 * <p>此方法会在启动时调用一次，在每次获取目标 Connection 时也会调用。
	 * 如果启动时检查失败（因为数据库已关闭），我们会延迟获取这些设置。
	 * @param con the Connection to use for checking
	 * @throws SQLException if thrown by Connection methods
	 */
	protected synchronized void checkDefaultConnectionProperties(Connection con) throws SQLException {
		if (this.defaultAutoCommit == null) {
			this.defaultAutoCommit = con.getAutoCommit();
		}
		if (this.defaultTransactionIsolation == null) {
			this.defaultTransactionIsolation = con.getTransactionIsolation();
		}
	}

	/**
	 * 暴露默认的自动提交值。
	 */
	@Nullable
	protected Boolean defaultAutoCommit() {
		return this.defaultAutoCommit;
	}

	/**
	 * 暴露默认的事务隔离级别值。
	 */
	@Nullable
	protected Integer defaultTransactionIsolation() {
		return this.defaultTransactionIsolation;
	}


	/**
	 * 返回一个 Connection 句柄，在请求 Statement（或 PreparedStatement
	 * 或 CallableStatement）时才延迟获取实际的 JDBC Connection。
	 * <p>返回的 Connection 句柄实现 ConnectionProxy 接口，
	 * 从而可以获取底层的目标 Connection。
	 * @return a lazy Connection handle
	 * @see ConnectionProxy#getTargetConnection()
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new LazyConnectionInvocationHandler());
	}

	/**
	 * 返回一个 Connection 句柄，在请求 Statement（或 PreparedStatement
	 * 或 CallableStatement）时才延迟获取实际的 JDBC Connection。
	 * <p>返回的 Connection 句柄实现 ConnectionProxy 接口，
	 * 从而可以获取底层的目标 Connection。
	 * @param username the per-Connection username
	 * @param password the per-Connection password
	 * @return a lazy Connection handle
	 * @see ConnectionProxy#getTargetConnection()
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new LazyConnectionInvocationHandler(username, password));
	}


	/**
	 * 调用处理器，将实际 JDBC Connection 的获取推迟到
	 * 首次创建 Statement 时。
	 */
	private class LazyConnectionInvocationHandler implements InvocationHandler {

		@Nullable
		private String username;

		@Nullable
		private String password;

		@Nullable
		private Boolean autoCommit;

		@Nullable
		private Integer transactionIsolation;

		private boolean readOnly = false;

		private int holdability = ResultSet.CLOSE_CURSORS_AT_COMMIT;

		private boolean closed = false;

		@Nullable
		private Connection target;

		public LazyConnectionInvocationHandler() {
			this.autoCommit = defaultAutoCommit();
			this.transactionIsolation = defaultTransactionIsolation();
		}

		public LazyConnectionInvocationHandler(String username, String password) {
			this();
			this.username = username;
			this.password = password;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			switch (method.getName()) {
				case "equals":
					// We must avoid fetching a target Connection for "equals".
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// We must avoid fetching a target Connection for "hashCode",
					// and we must return the same hash code even when the target
					// Connection has been fetched: use hashCode of Connection proxy.
					return System.identityHashCode(proxy);
				case "getTargetConnection":
					// Handle getTargetConnection method: return underlying connection.
					return getTargetConnection(method);
				case "unwrap":
					if (((Class<?>) args[0]).isInstance(proxy)) {
						return proxy;
					}
					break;
				case "isWrapperFor":
					if (((Class<?>) args[0]).isInstance(proxy)) {
						return true;
					}
					break;
			}

			if (!hasTargetConnection()) {
				// No physical target Connection kept yet ->
				// resolve transaction demarcation methods without fetching
				// a physical JDBC Connection until absolutely necessary.

				switch (method.getName()) {
					case "toString":
						return "Lazy Connection proxy for target DataSource [" + getTargetDataSource() + "]";
					case "getAutoCommit":
						if (this.autoCommit != null) {
							return this.autoCommit;
						}
						// Else fetch actual Connection and check there,
						// because we didn't have a default specified.
						break;
					case "setAutoCommit":
						this.autoCommit = (Boolean) args[0];
						return null;
					case "getTransactionIsolation":
						if (this.transactionIsolation != null) {
							return this.transactionIsolation;
						}
						// Else fetch actual Connection and check there,
						// because we didn't have a default specified.
						break;
					case "setTransactionIsolation":
						this.transactionIsolation = (Integer) args[0];
						return null;
					case "isReadOnly":
						return this.readOnly;
					case "setReadOnly":
						this.readOnly = (Boolean) args[0];
						return null;
					case "getHoldability":
						return this.holdability;
					case "setHoldability":
						this.holdability = (Integer) args[0];
						return null;
					case "commit":
					case "rollback":
						// Ignore: no statements created yet.
						return null;
					case "getWarnings":
					case "clearWarnings":
						// Ignore: no warnings to expose yet.
						return null;
					case "close":
						// Ignore: no target connection yet.
						this.closed = true;
						return null;
					case "isClosed":
						return this.closed;
					default:
						if (this.closed) {
							// Connection proxy closed, without ever having fetched a
							// physical JDBC Connection: throw corresponding SQLException.
							throw new SQLException("Illegal operation: connection is closed");
						}
				}
			}

			// Target Connection already fetched,
			// or target Connection necessary for current operation ->
			// invoke method on target connection.
			try {
				return method.invoke(getTargetConnection(method), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		/**
		 * 返回代理当前是否持有目标 Connection。
		 */
		private boolean hasTargetConnection() {
			return (this.target != null);
		}

		/**
		 * 返回目标 Connection，必要时获取并初始化它。
		 */
		private Connection getTargetConnection(Method operation) throws SQLException {
			if (this.target == null) {
				// No target Connection held -> fetch one.
				if (logger.isTraceEnabled()) {
					logger.trace("Connecting to database for operation '" + operation.getName() + "'");
				}

				// Fetch physical Connection from DataSource.
				this.target = (this.username != null) ?
						obtainTargetDataSource().getConnection(this.username, this.password) :
						obtainTargetDataSource().getConnection();

				// If we still lack default connection properties, check them now.
				checkDefaultConnectionProperties(this.target);

				// Apply kept transaction settings, if any.
				if (this.readOnly) {
					try {
						this.target.setReadOnly(true);
					}
					catch (Exception ex) {
						// "read-only not supported" -> ignore, it's just a hint anyway
						logger.debug("Could not set JDBC Connection read-only", ex);
					}
				}
				if (this.transactionIsolation != null &&
						!this.transactionIsolation.equals(defaultTransactionIsolation())) {
					this.target.setTransactionIsolation(this.transactionIsolation);
				}
				if (this.autoCommit != null && this.autoCommit != this.target.getAutoCommit()) {
					this.target.setAutoCommit(this.autoCommit);
				}
			}

			else {
				// Target Connection already held -> return it.
				if (logger.isTraceEnabled()) {
					logger.trace("Using existing database connection for operation '" + operation.getName() + "'");
				}
			}

			return this.target;
		}
	}

}
