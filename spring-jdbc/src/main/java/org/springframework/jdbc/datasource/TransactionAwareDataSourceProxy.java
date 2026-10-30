/*
 * Copyright 2002-2021 the original author or authors.
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
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 目标 JDBC {@link javax.sql.DataSource} 的代理，增加了对
 * Spring 管理事务的感知能力。类似于 Java EE 服务器提供的
 * 事务性 JNDI DataSource。
 *
 * <p>希望保持对 Spring 数据访问支持无感知的数据访问代码可以使用此代理，
 * 从而无缝参与 Spring 管理的事务。请注意，事务管理器（例如
 * {@link DataSourceTransactionManager}）仍然需要基于底层的 DataSource 工作，
 * 而<i>不是</i>基于此代理。
 *
 * <p><b>确保 TransactionAwareDataSourceProxy 是 DataSource 代理/适配器
 * 链中最外层的 DataSource。</b>TransactionAwareDataSourceProxy 可以直接
 * 委托给目标连接池，也可以委托给某个中间代理/适配器，例如
 * {@link LazyConnectionDataSourceProxy} 或 {@link UserCredentialsDataSourceAdapter}。
 *
 * <p>委托给 {@link DataSourceUtils}，以自动参与线程绑定的事务，
 * 例如由 {@link DataSourceTransactionManager} 管理的事务。
 * {@code getConnection} 调用以及对返回的 Connection 的 {@code close} 调用
 * 在事务内会正确工作，即始终操作事务性 Connection。如果不在事务内，
 * 则应用正常的 DataSource 行为。
 *
 * <p>此代理允许数据访问代码使用朴素的 JDBC API，同时仍然参与
 * Spring 管理的事务，类似于 Java EE/JTA 环境中的 JDBC 代码。
 * 不过，如果可能的话，请使用 Spring 的 DataSourceUtils、JdbcTemplate
 * 或 JDBC 操作对象，这样即使没有目标 DataSource 的代理也能获得事务参与，
 * 从而无需在一开始就定义这样的代理。
 *
 * <p>另一个额外效果是，使用事务感知的 DataSource 会将剩余的事务超时
 * 应用到所有创建的 JDBC (Prepared/Callable)Statement。这意味着通过标准
 * JDBC 执行的所有操作都将自动参与 Spring 管理的事务超时。
 *
 * <p><b>注意：</b>此 DataSource 代理需要返回包装后的 Connection
 * （实现 {@link ConnectionProxy} 接口），以便正确处理 close 调用。
 * 请使用 {@link Connection#unwrap} 获取原生 JDBC Connection。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see javax.sql.DataSource#getConnection()
 * @see java.sql.Connection#close()
 * @see DataSourceUtils#doGetConnection
 * @see DataSourceUtils#applyTransactionTimeout
 * @see DataSourceUtils#doReleaseConnection
 */
public class TransactionAwareDataSourceProxy extends DelegatingDataSource {

	private boolean reobtainTransactionalConnections = false;


	/**
	 * 创建一个新的 TransactionAwareDataSourceProxy。
	 * @see #setTargetDataSource
	 */
	public TransactionAwareDataSourceProxy() {
	}

	/**
	 * 创建一个新的 TransactionAwareDataSourceProxy。
	 * @param targetDataSource 目标 DataSource
	 */
	public TransactionAwareDataSourceProxy(DataSource targetDataSource) {
		super(targetDataSource);
	}

	/**
	 * 指定是否为事务内执行的每个操作重新获取目标 Connection。
	 * <p>默认为 "false"。设置为 "true" 可为 Connection 代理上的每次调用
	 * 重新获取事务性 Connection；如果你在事务边界之间持有 Connection 句柄，
	 * 在 JBoss 上建议这样做。
	 * <p>此设置的效果类似于 "hibernate.connection.release_mode" 的
	 * "after_statement" 值。
	 */
	public void setReobtainTransactionalConnections(boolean reobtainTransactionalConnections) {
		this.reobtainTransactionalConnections = reobtainTransactionalConnections;
	}


	/**
	 * 委托给 DataSourceUtils，以自动参与 Spring 管理的事务。
	 * 如果有原始 SQLException 则抛出。
	 * <p>返回的 Connection 句柄实现 ConnectionProxy 接口，
	 * 从而可以获取底层的目标 Connection。
	 * @return 事务性 Connection（如果有），否则为新的 Connection
	 * @see DataSourceUtils#doGetConnection
	 * @see ConnectionProxy#getTargetConnection
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return getTransactionAwareConnectionProxy(obtainTargetDataSource());
	}

	/**
	 * 用代理包装给定的 Connection，该代理将每个方法调用都委托给它，
	 * 但将 {@code close()} 调用委托给 DataSourceUtils。
	 * @param targetDataSource 该 Connection 来源的 DataSource
	 * @return 包装后的 Connection
	 * @see java.sql.Connection#close()
	 * @see DataSourceUtils#doReleaseConnection
	 */
	protected Connection getTransactionAwareConnectionProxy(DataSource targetDataSource) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new TransactionAwareInvocationHandler(targetDataSource));
	}

	/**
	 * 确定是为代理获取固定的目标 Connection，
	 * 还是为每个操作重新获取目标 Connection。
	 * <p>对于所有标准情况，默认实现返回 {@code true}。
	 * 这可以通过 {@link #setReobtainTransactionalConnections "reobtainTransactionalConnections"}
	 * 标志来覆盖，该标志会在活动事务内强制使用非固定的目标 Connection。
	 * 请注意，非事务性访问将始终使用固定的 Connection。
	 * @param targetDataSource 目标 DataSource
	 */
	protected boolean shouldObtainFixedConnection(DataSource targetDataSource) {
		return (!TransactionSynchronizationManager.isSynchronizationActive() ||
				!this.reobtainTransactionalConnections);
	}


	/**
	 * 调用处理器，将对 JDBC Connection 的 close 调用委托给
	 * DataSourceUtils，以便感知线程绑定的事务。
	 */
	private class TransactionAwareInvocationHandler implements InvocationHandler {

		private final DataSource targetDataSource;

		@Nullable
		private Connection target;

		private boolean closed = false;

		public TransactionAwareInvocationHandler(DataSource targetDataSource) {
			this.targetDataSource = targetDataSource;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			switch (method.getName()) {
				case "equals":
					// Only considered as equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of Connection proxy.
					return System.identityHashCode(proxy);
				case "toString":
					// Allow for differentiating between the proxy and the raw Connection.
					StringBuilder sb = new StringBuilder("Transaction-aware proxy for target Connection ");
					if (this.target != null) {
						sb.append('[').append(this.target.toString()).append(']');
					}
					else {
						sb.append(" from DataSource [").append(this.targetDataSource).append(']');
					}
					return sb.toString();
				case "close":
					// Handle close method: only close if not within a transaction.
					DataSourceUtils.doReleaseConnection(this.target, this.targetDataSource);
					this.closed = true;
					return null;
				case "isClosed":
					return this.closed;
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

			if (this.target == null) {
				if (method.getName().equals("getWarnings") || method.getName().equals("clearWarnings")) {
					// Avoid creation of target Connection on pre-close cleanup (e.g. Hibernate Session)
					return null;
				}
				if (this.closed) {
					throw new SQLException("Connection handle already closed");
				}
				if (shouldObtainFixedConnection(this.targetDataSource)) {
					this.target = DataSourceUtils.doGetConnection(this.targetDataSource);
				}
			}
			Connection actualTarget = this.target;
			if (actualTarget == null) {
				actualTarget = DataSourceUtils.doGetConnection(this.targetDataSource);
			}

			if (method.getName().equals("getTargetConnection")) {
				// Handle getTargetConnection method: return underlying Connection.
				return actualTarget;
			}

			// Invoke method on target Connection.
			try {
				Object retVal = method.invoke(actualTarget, args);

				// If return value is a Statement, apply transaction timeout.
				// Applies to createStatement, prepareStatement, prepareCall.
				if (retVal instanceof Statement) {
					DataSourceUtils.applyTransactionTimeout((Statement) retVal, this.targetDataSource);
				}

				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				if (actualTarget != this.target) {
					DataSourceUtils.doReleaseConnection(actualTarget, this.targetDataSource);
				}
			}
		}
	}

}
