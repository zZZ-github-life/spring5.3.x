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

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;

import org.springframework.lang.Nullable;

/**
 * 目标 CCI {@link javax.resource.cci.ConnectionFactory} 的代理，
 * 增加了对 Spring 管理事务的感知。类似于 Java EE 服务器提供的事务性 JNDI ConnectionFactory。
 *
 * <p>不需要感知 Spring 数据访问支持的数据访问代码可以使用此代理来无缝参与
 * Spring 管理的事务。注意，事务管理器（例如 {@link CciLocalTransactionManager}）
 * 仍然需要操作底层的 ConnectionFactory，<i>而不是</i>此代理。
 *
 * <p><b>确保 TransactionAwareConnectionFactoryProxy 是 ConnectionFactory 代理/适配器链中
 * 最外层的 ConnectionFactory。</b> TransactionAwareConnectionFactoryProxy 可以直接委托给
 * 目标连接池，也可以委托给某些中间代理/适配器，如 {@link ConnectionSpecConnectionFactoryAdapter}。
 *
 * <p>委托 {@link ConnectionFactoryUtils} 自动参与线程绑定的事务，
 * 例如由 {@link CciLocalTransactionManager} 管理的事务。
 * 返回的 Connection 上的 {@code getConnection} 调用和 {@code close} 调用
 * 将在事务中正确运行，即始终操作事务性 Connection。如果不在事务中，
 * 则应用普通的 ConnectionFactory 行为。
 *
 * <p>此代理允许数据访问代码使用纯 JCA CCI API 工作，同时参与 Spring 管理的事务，
 * 类似于 Java EE/JTA 环境中的 CCI 代码。但是，如果可能的话，请使用 Spring 的
 * ConnectionFactoryUtils、CciTemplate 或 CCI 操作对象来获得事务参与，
 * 而无需为目标 ConnectionFactory 设置代理，从而避免首先定义此类代理的需要。
 *
 * <p><b>注意：</b>此 ConnectionFactory 代理需要返回包装的 Connection 以正确处理 close 调用。
 * 因此，返回的 Connection 不能转换为原生 CCI Connection 类型或连接池实现类型。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see javax.resource.cci.ConnectionFactory#getConnection
 * @see javax.resource.cci.Connection#close
 * @see ConnectionFactoryUtils#doGetConnection
 * @see ConnectionFactoryUtils#doReleaseConnection
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@SuppressWarnings("serial")
public class TransactionAwareConnectionFactoryProxy extends DelegatingConnectionFactory {

	/**
	 * 创建新的 TransactionAwareConnectionFactoryProxy。
	 * @see #setTargetConnectionFactory
	 */
	public TransactionAwareConnectionFactoryProxy() {
	}

	/**
	 * 创建新的 TransactionAwareConnectionFactoryProxy。
	 * @param targetConnectionFactory 目标 ConnectionFactory
	 */
	public TransactionAwareConnectionFactoryProxy(ConnectionFactory targetConnectionFactory) {
		setTargetConnectionFactory(targetConnectionFactory);
		afterPropertiesSet();
	}


	/**
	 * 委托 ConnectionFactoryUtils 自动参与 Spring 管理的事务。
	 * 如有原始 ResourceException 则抛出。
	 * @return 如果存在事务性 Connection 则返回它，否则返回新的 Connection
	 * @see org.springframework.jca.cci.connection.ConnectionFactoryUtils#doGetConnection
	 */
	@Override
	public Connection getConnection() throws ResourceException {
		ConnectionFactory targetConnectionFactory = obtainTargetConnectionFactory();
		Connection con = ConnectionFactoryUtils.doGetConnection(targetConnectionFactory);
		return getTransactionAwareConnectionProxy(con, targetConnectionFactory);
	}

	/**
	 * 使用代理包装给定的 Connection，该代理将每个方法调用委托给它，
	 * 但将 {@code close} 调用委托给 ConnectionFactoryUtils。
	 * @param target 要包装的原始 Connection
	 * @param cf 该 Connection 所来自的 ConnectionFactory
	 * @return 被包装的 Connection
	 * @see javax.resource.cci.Connection#close()
	 * @see ConnectionFactoryUtils#doReleaseConnection
	 */
	protected Connection getTransactionAwareConnectionProxy(Connection target, ConnectionFactory cf) {
		return (Connection) Proxy.newProxyInstance(
				Connection.class.getClassLoader(),
				new Class<?>[] {Connection.class},
				new TransactionAwareInvocationHandler(target, cf));
	}


	/**
	 * 将 CCI Connection 的 close 调用委托给 ConnectionFactoryUtils 以感知线程绑定事务的调用处理器。
	 */
	private static class TransactionAwareInvocationHandler implements InvocationHandler {

		private final Connection target;

		private final ConnectionFactory connectionFactory;

		public TransactionAwareInvocationHandler(Connection target, ConnectionFactory cf) {
			this.target = target;
			this.connectionFactory = cf;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on Connection interface coming in...

			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of Connection proxy.
					return System.identityHashCode(proxy);
				case "getLocalTransaction":
					if (ConnectionFactoryUtils.isConnectionTransactional(this.target, this.connectionFactory)) {
						throw new javax.resource.spi.IllegalStateException(
								"Local transaction handling not allowed within a managed transaction");
					}
					return this.target.getLocalTransaction();
				case "close":
					// Handle close method: only close if not within a transaction.
					ConnectionFactoryUtils.doReleaseConnection(this.target, this.connectionFactory);
					return null;
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
