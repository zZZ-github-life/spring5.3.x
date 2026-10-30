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

package org.springframework.r2dbc.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * 目标 R2DBC {@link ConnectionFactory} 的代理，增加了对 Spring 管理事务的感知能力。
 *
 * <p>那些应当对 Spring 的数据访问支持保持无感知的数据访问代码，可以使用此代理
 * 无缝地参与 Spring 管理的事务。
 * 请注意，事务管理器（例如 {@link R2dbcTransactionManager}）仍然需要与底层的
 * {@link ConnectionFactory} 配合使用，而<i>不是</i>与此代理配合使用。
 *
 * <p><b>请确保 {@link TransactionAwareConnectionFactoryProxy} 是
 * {@link ConnectionFactory} 代理/适配器链中最外层的 {@link ConnectionFactory}。</b>
 * {@link TransactionAwareConnectionFactoryProxy} 既可以直接委托给目标连接池，
 * 也可以委托给某个中间代理/适配器。
 *
 * <p>通过委托给 {@link ConnectionFactoryUtils} 来自动参与绑定到事务的事务，
 * 例如由 {@link R2dbcTransactionManager} 管理的事务。
 * 在事务内，{@link #create()} 调用以及返回的 {@link Connection} 上的
 * {@code close} 调用会表现出正确的行为，即始终操作事务性 Connection。
 * 如果不在事务内，则应用常规的 {@link ConnectionFactory} 行为。
 *
 * <p>此代理允许数据访问代码使用普通的 R2DBC API。不过，如果可能的话，
 * 请使用 Spring 的 {@link ConnectionFactoryUtils} 或 {@code DatabaseClient}，
 * 这样即使没有目标 {@link ConnectionFactory} 的代理也能获得事务参与能力，
 * 从而一开始就无需定义这样的代理。
 *
 * <p><b>注意：</b>此 {@link ConnectionFactory} 代理需要返回被包装的
 * {@link Connection}，才能正确处理 close 调用。
 * 使用 {@link Wrapped#unwrap()} 来获取原生 R2DBC Connection。
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 5.3
 * @see ConnectionFactory#create
 * @see Connection#close
 * @see ConnectionFactoryUtils#doGetConnection
 * @see ConnectionFactoryUtils#doReleaseConnection
 */
public class TransactionAwareConnectionFactoryProxy extends DelegatingConnectionFactory {

	/**
	 * 创建一个新的 {@link TransactionAwareConnectionFactoryProxy}。
	 * @param targetConnectionFactory 目标 {@link ConnectionFactory}
	 */
	public TransactionAwareConnectionFactoryProxy(ConnectionFactory targetConnectionFactory) {
		super(targetConnectionFactory);
	}


	/**
	 * 委托给 {@link ConnectionFactoryUtils} 以自动参与 Spring 管理的事务。
	 * @return 如果存在则返回事务性 {@link Connection}，否则返回新的连接。
	 * @see ConnectionFactoryUtils#doGetConnection
	 */
	@Override
	public Mono<Connection> create() {
		return getTransactionAwareConnectionProxy(getTargetConnectionFactory());
	}

	/**
	 * 用代理包装给定的 {@link Connection}，该代理将所有方法调用委托给目标，
	 * 但将 {@code close()} 调用委托给 {@link ConnectionFactoryUtils}。
	 * @param targetConnectionFactory {@link Connection} 来源的 {@link ConnectionFactory}
	 * @return 包装后的 {@link Connection}。
	 * @see Connection#close()
	 * @see ConnectionFactoryUtils#doReleaseConnection
	 */
	protected Mono<Connection> getTransactionAwareConnectionProxy(ConnectionFactory targetConnectionFactory) {
		return ConnectionFactoryUtils.getConnection(targetConnectionFactory)
				.map(connection -> proxyConnection(connection, targetConnectionFactory));
	}

	private static Connection proxyConnection(Connection connection, ConnectionFactory targetConnectionFactory) {
		return (Connection) Proxy.newProxyInstance(TransactionAwareConnectionFactoryProxy.class.getClassLoader(),
				new Class<?>[] {Connection.class, Wrapped.class},
				new TransactionAwareInvocationHandler(connection, targetConnectionFactory));
	}


	/**
	 * 调用处理器，将 R2DBC Connection 上的 close 调用委托给
	 * {@link ConnectionFactoryUtils}，以便感知绑定到上下文的事务。
	 */
	private static class TransactionAwareInvocationHandler implements InvocationHandler {

		private final Connection connection;

		private final ConnectionFactory targetConnectionFactory;

		private boolean closed = false;

		TransactionAwareInvocationHandler(Connection connection, ConnectionFactory targetConnectionFactory) {
			this.connection = connection;
			this.targetConnectionFactory = targetConnectionFactory;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isObjectMethod(method)) {
				if (ReflectionUtils.isToStringMethod(method)) {
					return proxyToString(proxy);
				}
				if (ReflectionUtils.isEqualsMethod(method)) {
					return (proxy == args[0]);
				}
				if (ReflectionUtils.isHashCodeMethod(method)) {
					return System.identityHashCode(proxy);
				}
			}

			switch (method.getName()) {
				case "unwrap":
					return this.connection;
				case "close":
					// Handle close method: only close if not within a transaction.
					return ConnectionFactoryUtils.doReleaseConnection(this.connection, this.targetConnectionFactory)
							.doOnSubscribe(n -> this.closed = true);
				case "isClosed":
					return this.closed;
			}

			if (this.closed) {
				throw new IllegalStateException("Connection handle already closed");
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.connection, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private String proxyToString(@Nullable Object proxy) {
			// Allow for differentiating between the proxy and the raw Connection.
			return "Transaction-aware proxy for target Connection [" + this.connection.toString() + "]";
		}

	}

}
