/*
 * Copyright 2002-2022 the original author or authors.
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

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcNonTransientException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcPermissionDeniedException;
import io.r2dbc.spi.R2dbcRollbackException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.R2dbcTransientException;
import io.r2dbc.spi.R2dbcTransientResourceException;
import io.r2dbc.spi.Wrapped;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.r2dbc.UncategorizedR2dbcException;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.reactive.TransactionSynchronization;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 辅助类，提供从 {@link ConnectionFactory} 获取 R2DBC Connection 的静态方法。
 *
 * <p>供 Spring 的 {@code DatabaseClient} 以及 Spring 的 R2DBC 操作对象内部使用。
 * 也可以直接在应用程序代码中使用。
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 5.3
 * @see R2dbcTransactionManager
 * @see org.springframework.transaction.reactive.TransactionSynchronizationManager
 */
public abstract class ConnectionFactoryUtils {

	/**
	 * 用于清理 R2DBC Connection 的 ReactiveTransactionSynchronization 对象的顺序值。
	 */
	public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;


	/**
	 * 从给定的 {@link ConnectionFactory} 获取一个 {@link Connection}。
	 * 将异常转换为 Spring 的非受检通用数据访问异常层次结构，
	 * 从而简化调用代码并使抛出的任何异常更具意义。
	 * <p>能够识别绑定到当前 {@link TransactionSynchronizationManager} 的相应 Connection。
	 * 如果事务同步处于活动状态，会将 Connection 绑定到
	 * {@link TransactionSynchronizationManager}。
	 * @param connectionFactory 用于从中获取 {@link Connection Connections} 的
	 * {@link ConnectionFactory}
	 * @return 来自给定 {@link ConnectionFactory} 的 R2DBC Connection
	 * @throws DataAccessResourceFailureException 如果尝试获取 {@link Connection} 失败
	 * @see #releaseConnection
	 */
	public static Mono<Connection> getConnection(ConnectionFactory connectionFactory) {
		return doGetConnection(connectionFactory)
				.onErrorMap(e -> new DataAccessResourceFailureException("Failed to obtain R2DBC Connection", e));
	}

	/**
	 * 实际从给定的 {@link ConnectionFactory} 获取 R2DBC Connection。
	 * 与 {@link #getConnection} 相同，但保留原始异常。
	 * <p>能够识别绑定到当前 {@link TransactionSynchronizationManager} 的相应 Connection。
	 * 如果事务同步处于活动状态，会将 Connection 绑定到
	 * {@link TransactionSynchronizationManager}
	 * @param connectionFactory 用于从中获取 Connection 的 {@link ConnectionFactory}
	 * @return 来自给定 {@link ConnectionFactory} 的 R2DBC {@link Connection}。
	 */
	public static Mono<Connection> doGetConnection(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {

			ConnectionHolder conHolder = (ConnectionHolder) synchronizationManager.getResource(connectionFactory);
			if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
				conHolder.requested();
				if (!conHolder.hasConnection()) {
					return fetchConnection(connectionFactory).doOnNext(conHolder::setConnection);
				}
				return Mono.just(conHolder.getConnection());
			}
			// Else we either got no holder or an empty thread-bound holder here.

			Mono<Connection> con = fetchConnection(connectionFactory);
			if (synchronizationManager.isSynchronizationActive()) {
				return con.flatMap(connection -> Mono.just(connection).doOnNext(conn -> {
					// Use same Connection for further R2DBC actions within the transaction.
					// Thread-bound object will get removed by synchronization at transaction completion.
					ConnectionHolder holderToUse = conHolder;
					if (holderToUse == null) {
						holderToUse = new ConnectionHolder(conn);
					}
					else {
						holderToUse.setConnection(conn);
					}
					holderToUse.requested();
					synchronizationManager
							.registerSynchronization(new ConnectionSynchronization(holderToUse, connectionFactory));
					holderToUse.setSynchronizedWithTransaction(true);
					if (holderToUse != conHolder) {
						synchronizationManager.bindResource(connectionFactory, holderToUse);
					}
				})      // Unexpected exception from external delegation call -> close Connection and rethrow.
				.onErrorResume(e -> releaseConnection(connection, connectionFactory).then(Mono.error(e))));
			}
			return con;
		}).onErrorResume(NoTransactionException.class, e -> Mono.from(connectionFactory.create()));
	}

	/**
	 * 实际从给定的 {@link ConnectionFactory} 获取一个 {@link Connection}。
	 * @param connectionFactory 用于从中获取 {@link Connection} 的 {@link ConnectionFactory}
	 * @return 来自给定 {@link ConnectionFactory} 的 R2DBC {@link Connection}
	 * （永不为 {@code null}）。
	 * @throws IllegalStateException 如果 {@link ConnectionFactory} 返回了 {@code null} 值。
	 * @see ConnectionFactory#create()
	 */
	private static Mono<Connection> fetchConnection(ConnectionFactory connectionFactory) {
		return Mono.from(connectionFactory.create());
	}

	/**
	 * 关闭从给定 {@link ConnectionFactory} 获取的 {@link Connection}，
	 * 前提是它不由外部管理（即未绑定到当前订阅）。
	 * @param con 需要时关闭的 {@link Connection}
	 * @param connectionFactory 获取该 Connection 的 {@link ConnectionFactory}
	 * @see #getConnection
	 */
	public static Mono<Void> releaseConnection(Connection con, ConnectionFactory connectionFactory) {
		return doReleaseConnection(con, connectionFactory)
				.onErrorMap(e -> new DataAccessResourceFailureException("Failed to close R2DBC Connection", e));
	}

	/**
	 * 实际关闭从给定 {@link ConnectionFactory} 获取的 {@link Connection}。
	 * 与 {@link #releaseConnection} 相同，但保留原始异常。
	 * @param connection 需要时关闭的 {@link Connection}
	 * @param connectionFactory 获取该 Connection 的 {@link ConnectionFactory}
	 * @see #doGetConnection
	 */
	public static Mono<Void> doReleaseConnection(Connection connection, ConnectionFactory connectionFactory) {
		return TransactionSynchronizationManager.forCurrentTransaction()
				.flatMap(synchronizationManager -> {
			ConnectionHolder conHolder = (ConnectionHolder) synchronizationManager.getResource(connectionFactory);
			if (conHolder != null && connectionEquals(conHolder, connection)) {
				// It's the transactional Connection: Don't close it.
				conHolder.released();
			}
			return Mono.from(connection.close());
		}).onErrorResume(NoTransactionException.class, e -> Mono.from(connection.close()));
	}

	/**
	 * 从当前 {@link TransactionSynchronizationManager} 获取 {@link ConnectionFactory}。
	 * @param connectionFactory 获取该 Connection 的 {@link ConnectionFactory}
	 * @see TransactionSynchronizationManager
	 */
	public static Mono<ConnectionFactory> currentConnectionFactory(ConnectionFactory connectionFactory) {
		return TransactionSynchronizationManager.forCurrentTransaction()
				.filter(TransactionSynchronizationManager::isSynchronizationActive)
				.filter(synchronizationManager -> {
					ConnectionHolder conHolder = (ConnectionHolder) synchronizationManager.getResource(connectionFactory);
					return conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction());
				}).map(synchronizationManager -> connectionFactory);
	}

	/**
	 * 将给定的 {@link R2dbcException} 转换为通用的 {@link DataAccessException}。
	 * <p>返回的 DataAccessException 应当以原始 {@link R2dbcException} 作为根本原因。
	 * 不过，由于 DataAccessException 也可能由其他资源 API 引起，
	 * 客户端代码通常不应依赖这一点。话虽如此，在期望发生基于 R2DBC 的访问时，
	 * {@code getRootCause() instanceof R2dbcException} 检查（以及随后的类型转换）
	 * 被认为是可靠的。
	 * @param task 描述当前尝试执行任务的可读文本
	 * @param sql 导致问题的 SQL 查询或更新（如果已知）
	 * @param ex 出错的 {@link R2dbcException}
	 * @return 相应的 DataAccessException 实例
	 */
	public static DataAccessException convertR2dbcException(String task, @Nullable String sql, R2dbcException ex) {
		if (ex instanceof R2dbcTransientException) {
			if (ex instanceof R2dbcTransientResourceException) {
				return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcRollbackException) {
				return new ConcurrencyFailureException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcTimeoutException) {
				return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
			}
		}
		else if (ex instanceof R2dbcNonTransientException) {
			if (ex instanceof R2dbcNonTransientResourceException) {
				return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcDataIntegrityViolationException) {
				return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcPermissionDeniedException) {
				return new PermissionDeniedDataAccessException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof R2dbcBadGrammarException) {
				return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
			}
		}
		return new UncategorizedR2dbcException(buildMessage(task, sql, ex), sql, ex);
	}

	/**
	 * 为给定的 {@link R2dbcException} 构建消息 {@code String}。
	 * <p>由转换器子类在创建通用 {@link org.springframework.dao.DataAccessException}
	 * 类的实例时调用。
	 * @param task 描述当前尝试执行任务的可读文本
	 * @param sql 导致问题的 SQL 语句
	 * @param ex 出错的 {@code R2dbcException}
	 * @return 要使用的消息 {@code String}
	 */
	private static String buildMessage(String task, @Nullable String sql, R2dbcException ex) {
		return task + "; " + (sql != null ? ("SQL [" + sql + "]; ") : "") + ex.getMessage();
	}

	/**
	 * 判断给定的两个 {@link Connection} 是否相等；如果遇到代理，则询问目标
	 * {@link Connection}。用于在用户传入的是原始目标 Connection 而持有的是代理时，
	 * 仍能检测到相等性。
	 * @param conHolder 持有 {@link Connection} 的 {@link ConnectionHolder}
	 * （可能是一个代理）
	 * @param passedInCon 用户传入的 {@link Connection}（可能是
	 * 未经过代理的目标 {@link Connection}）。
	 * @return 给定的 Connections 是否相等
	 * @see #getTargetConnection
	 */
	private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
		if (!conHolder.hasConnection()) {
			return false;
		}
		Connection heldCon = conHolder.getConnection();
		// Explicitly check for identity too: for Connection handles that do not implement
		// "equals" properly).
		return (heldCon == passedInCon || heldCon.equals(passedInCon) || getTargetConnection(heldCon).equals(passedInCon));
	}

	/**
	 * 返回给定 {@link Connection} 最内层的目标 {@link Connection}。
	 * 如果给定的 {@link Connection} 被包装，则会逐层解包，直到找到普通的
	 * {@link Connection} 为止。否则，将按原样返回传入的 Connection。
	 * @param con 要解包的 {@link Connection} 包装器
	 * @return 最内层的目标 Connection，若未包装则返回传入的实例
	 * @see Wrapped#unwrap()
	 */
	@SuppressWarnings("unchecked")
	public static Connection getTargetConnection(Connection con) {
		Connection conToUse = con;
		while (conToUse instanceof Wrapped<?>) {
			conToUse = ((Wrapped<Connection>) conToUse).unwrap();
		}
		return conToUse;
	}

	/**
	 * 确定用于给定 {@link ConnectionFactory} 的连接同步顺序。
	 * {@link ConnectionFactory} 每嵌套一层（通过 {@link DelegatingConnectionFactory}
	 * 的嵌套层级检查），顺序值就递减一次。
	 * @param connectionFactory 要检查的 {@link ConnectionFactory}
	 * @return 要使用的连接同步顺序
	 * @see #CONNECTION_SYNCHRONIZATION_ORDER
	 */
	private static int getConnectionSynchronizationOrder(ConnectionFactory connectionFactory) {
		int order = CONNECTION_SYNCHRONIZATION_ORDER;
		ConnectionFactory current = connectionFactory;
		while (current instanceof DelegatingConnectionFactory) {
			order--;
			current = ((DelegatingConnectionFactory) current).getTargetConnectionFactory();
		}
		return order;
	}


	/**
	 * 用于在非原生 R2DBC 事务结束时清理资源的回调。
	 */
	private static class ConnectionSynchronization implements TransactionSynchronization, Ordered {

		private final ConnectionHolder connectionHolder;

		private final ConnectionFactory connectionFactory;

		private final int order;

		private boolean holderActive = true;

		ConnectionSynchronization(ConnectionHolder connectionHolder, ConnectionFactory connectionFactory) {
			this.connectionHolder = connectionHolder;
			this.connectionFactory = connectionFactory;
			this.order = getConnectionSynchronizationOrder(connectionFactory);
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public Mono<Void> suspend() {
			if (this.holderActive) {
				return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
					synchronizationManager.unbindResource(this.connectionFactory);
					if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
						// Release Connection on suspend if the application doesn't keep
						// a handle to it anymore. We will fetch a fresh Connection if the
						// application accesses the ConnectionHolder again after resume,
						// assuming that it will participate in the same transaction.
						return releaseConnection(this.connectionHolder.getConnection(), this.connectionFactory)
								.doOnTerminate(() -> this.connectionHolder.setConnection(null));
					}
					return Mono.empty();
				});
			}
			return Mono.empty();
		}

		@Override
		public Mono<Void> resume() {
			if (this.holderActive) {
				return TransactionSynchronizationManager.forCurrentTransaction()
						.doOnNext(synchronizationManager ->
								synchronizationManager.bindResource(this.connectionFactory, this.connectionHolder))
						.then();
			}
			return Mono.empty();
		}

		@Override
		public Mono<Void> beforeCompletion() {
			// Release Connection early if the holder is not open anymore (that is,
			// not used by another resource that has its own cleanup via transaction
			// synchronization), to avoid issues with strict transaction implementations
			// that expect the close call before transaction completion.
			if (!this.connectionHolder.isOpen()) {
				return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
					synchronizationManager.unbindResource(this.connectionFactory);
					this.holderActive = false;
					if (this.connectionHolder.hasConnection()) {
						return releaseConnection(this.connectionHolder.getConnection(), this.connectionFactory);
					}
					return Mono.empty();
				});
			}

			return Mono.empty();
		}

		@Override
		public Mono<Void> afterCompletion(int status) {
			// If we haven't closed the Connection in beforeCompletion,
			// close it now.
			if (this.holderActive) {
				// The bound ConnectionHolder might not be available anymore,
				// since afterCompletion might get called from a different thread.
				return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
					synchronizationManager.unbindResourceIfPossible(this.connectionFactory);
					this.holderActive = false;
					if (this.connectionHolder.hasConnection()) {
						return releaseConnection(this.connectionHolder.getConnection(), this.connectionFactory)
								// Reset the ConnectionHolder: It might remain bound to the context.
								.doOnTerminate(() -> this.connectionHolder.setConnection(null));
					}
					return Mono.empty();
				});
			}
			this.connectionHolder.reset();
			return Mono.empty();
		}
	}

}
