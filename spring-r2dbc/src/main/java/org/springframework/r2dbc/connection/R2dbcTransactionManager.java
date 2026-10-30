/*
 * Copyright 2002-2023 the original author or authors.
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

import java.time.Duration;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Result;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.reactive.AbstractReactiveTransactionManager;
import org.springframework.transaction.reactive.GenericReactiveTransaction;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 适用于单个 R2DBC {@link ConnectionFactory} 的
 * {@link org.springframework.transaction.ReactiveTransactionManager} 实现。
 * 只要配置使用 {@code ConnectionFactory} 作为 {@link Connection} 的工厂机制，
 * 此类就可以在任何环境中与任何 R2DBC 驱动一起工作。它将指定
 * {@code ConnectionFactory} 中的 R2DBC {@code Connection} 绑定到当前订阅者上下文，
 * 从而可能为每个 {@code ConnectionFactory} 提供一个绑定到上下文的 {@code Connection}。
 *
 * <p><b>注意：此事务管理器所操作的 {@code ConnectionFactory} 必须返回相互独立的
 * {@code Connection}。</b>{@code Connection} 可以来自连接池（这是典型情况），
 * 但 {@code ConnectionFactory} 不得返回作用域化的 {@code Connection} 之类的东西。
 * 此事务管理器会根据指定的传播行为，自行将 {@code Connection} 与绑定到上下文的
 * 事务关联。它假定即使在事务进行期间，也能获取到单独的、独立的 {@code Connection}。
 *
 * <p>应用程序代码需要通过
 * {@link ConnectionFactoryUtils#getConnection(ConnectionFactory)}
 * 获取 R2DBC Connection，而不是采用标准的 R2DBC 风格
 * {@link ConnectionFactory#create()} 调用。诸如 {@code DatabaseClient} 之类的
 * Spring 类会隐式使用此策略。如果不与此事务管理器配合使用，
 * {@link ConnectionFactoryUtils} 的查找策略的行为与原生
 * {@code ConnectionFactory} 查找完全一致，因此可以以可移植的方式使用。
 *
 * <p>或者，你也可以允许应用程序代码使用标准的 R2DBC 查找方式
 * {@link ConnectionFactory#create()}，例如对于完全不感知 Spring 的代码。
 * 这种情况下，请为目标 {@code ConnectionFactory} 定义一个
 * {@link TransactionAwareConnectionFactoryProxy}，并将该代理
 * {@code ConnectionFactory} 传给 DAO，这样在访问时就会自动参与 Spring 管理的事务。
 *
 * <p>此事务管理器会在已注册的事务同步上触发 flush 回调（如果同步总体上处于活动状态），
 * 并假定这些资源操作的是底层的 R2DBC {@code Connection}。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see ConnectionFactoryUtils#getConnection(ConnectionFactory)
 * @see ConnectionFactoryUtils#releaseConnection
 * @see TransactionAwareConnectionFactoryProxy
 */
@SuppressWarnings("serial")
public class R2dbcTransactionManager extends AbstractReactiveTransactionManager implements InitializingBean {

	@Nullable
	private ConnectionFactory connectionFactory;

	private boolean enforceReadOnly = false;


	/**
	 * 创建一个新的 {@code R2dbcTransactionManager} 实例。
	 * 必须设置 ConnectionFactory 才能使用它。
	 * @see #setConnectionFactory
	 */
	public R2dbcTransactionManager() {}

	/**
	 * 创建一个新的 {@code R2dbcTransactionManager} 实例。
	 * @param connectionFactory 要为其管理事务的 R2DBC ConnectionFactory
	 */
	public R2dbcTransactionManager(ConnectionFactory connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应为其管理事务的 R2DBC {@link ConnectionFactory}。
	 * <p>通常这是一个本地定义的 {@code ConnectionFactory}，例如连接池。
	 * <p><b>此处传入的 {@code ConnectionFactory} 必须返回相互独立的 {@link Connection}。</b>
	 * {@code Connection} 可以来自连接池（这是典型情况），但 {@code ConnectionFactory}
	 * 不得返回作用域化的 {@code Connection} 之类的东西。
	 * @see TransactionAwareConnectionFactoryProxy
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * 返回此实例为其管理事务的 R2DBC {@link ConnectionFactory}。
	 */
	@Nullable
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * 获取实际使用的 {@link ConnectionFactory}。
	 * @return {@code ConnectionFactory}（永不为 {@code null}）
	 * @throws IllegalStateException 在未设置 ConnectionFactory 的情况下
	 */
	protected ConnectionFactory obtainConnectionFactory() {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory;
	}

	/**
	 * 指定是否通过事务连接上的显式语句来强制事务的只读特性
	 * （由 {@link TransactionDefinition#isReadOnly()} 指示）：即 Oracle、
	 * MySQL 和 Postgres 所理解的 "SET TRANSACTION READ ONLY"。
	 * <p>具体的处理方式（包括在连接上执行的任何 SQL 语句）可以通过
	 * {@link #prepareTransactionalConnection(Connection, TransactionDefinition)} 进行自定义。
	 * @see #prepareTransactionalConnection(Connection, TransactionDefinition)
	 */
	public void setEnforceReadOnly(boolean enforceReadOnly) {
		this.enforceReadOnly = enforceReadOnly;
	}

	/**
	 * 返回是否通过事务连接上的显式语句来强制事务的只读特性。
	 * @see #setEnforceReadOnly
	 */
	public boolean isEnforceReadOnly() {
		return this.enforceReadOnly;
	}

	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}

	@Override
	protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) throws TransactionException {
		ConnectionFactoryTransactionObject txObject = new ConnectionFactoryTransactionObject();
		ConnectionHolder conHolder = (ConnectionHolder) synchronizationManager.getResource(obtainConnectionFactory());
		txObject.setConnectionHolder(conHolder, false);
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) transaction;
		return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction,
			TransactionDefinition definition) throws TransactionException {

		ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) transaction;

		return Mono.defer(() -> {
			Mono<Connection> connectionMono;

			if (!txObject.hasConnectionHolder() || txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
				Mono<Connection> newCon = Mono.from(obtainConnectionFactory().create());
				connectionMono = newCon.doOnNext(connection -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Acquired Connection [" + connection + "] for R2DBC transaction");
					}
					txObject.setConnectionHolder(new ConnectionHolder(connection), true);
				});
			}
			else {
				txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
				connectionMono = Mono.just(txObject.getConnectionHolder().getConnection());
			}

			return connectionMono.flatMap(con -> prepareTransactionalConnection(con, definition, transaction)
					.then(Mono.from(con.beginTransaction()))
					.then(prepareTransactionalConnection(con, definition))
					.doOnSuccess(v -> {
						txObject.getConnectionHolder().setTransactionActive(true);
						Duration timeout = determineTimeout(definition);
						if (!timeout.isNegative() && !timeout.isZero()) {
							txObject.getConnectionHolder().setTimeoutInMillis(timeout.toMillis());
						}
						// Bind the connection holder to the thread.
						if (txObject.isNewConnectionHolder()) {
							synchronizationManager.bindResource(obtainConnectionFactory(), txObject.getConnectionHolder());
						}
					}).thenReturn(con).onErrorResume(e -> {
						if (txObject.isNewConnectionHolder()) {
							return ConnectionFactoryUtils.releaseConnection(con, obtainConnectionFactory())
									.doOnTerminate(() -> txObject.setConnectionHolder(null, false))
									.then(Mono.error(e));
						}
						return Mono.error(e);
			})).onErrorResume(e -> {
				CannotCreateTransactionException ex = new CannotCreateTransactionException(
						"Could not open R2DBC Connection for transaction", e);
				return Mono.error(ex);
			});
		}).then();
	}

	/**
	 * 确定为给定定义实际使用的超时时间。
	 * 如果事务定义未指定非默认值，则回退到本管理器的默认超时时间。
	 * @param definition 事务定义
	 * @return 实际要使用的超时时间
	 * @see org.springframework.transaction.TransactionDefinition#getTimeout()
	 */
	protected Duration determineTimeout(TransactionDefinition definition) {
		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			return Duration.ofSeconds(definition.getTimeout());
		}
		return Duration.ZERO;
	}

	@Override
	protected Mono<Object> doSuspend(TransactionSynchronizationManager synchronizationManager, Object transaction)
			throws TransactionException {

		return Mono.defer(() -> {
			ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) transaction;
			txObject.setConnectionHolder(null);
			return Mono.justOrEmpty(synchronizationManager.unbindResource(obtainConnectionFactory()));
		});
	}

	@Override
	protected Mono<Void> doResume(TransactionSynchronizationManager synchronizationManager,
			@Nullable Object transaction, Object suspendedResources) throws TransactionException {

		return Mono.defer(() -> {
			synchronizationManager.bindResource(obtainConnectionFactory(), suspendedResources);
			return Mono.empty();
		});
	}

	@Override
	protected Mono<Void> doCommit(TransactionSynchronizationManager TransactionSynchronizationManager,
			GenericReactiveTransaction status) throws TransactionException {

		ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) status.getTransaction();
		Connection connection = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Committing R2DBC transaction on Connection [" + connection + "]");
		}
		return Mono.from(connection.commitTransaction())
				.onErrorMap(R2dbcException.class, ex -> translateException("R2DBC commit", ex));
	}

	@Override
	protected Mono<Void> doRollback(TransactionSynchronizationManager TransactionSynchronizationManager,
			GenericReactiveTransaction status) throws TransactionException {

		ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) status.getTransaction();
		Connection connection = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Rolling back R2DBC transaction on Connection [" + connection + "]");
		}
		return Mono.from(connection.rollbackTransaction())
				.onErrorMap(R2dbcException.class, ex -> translateException("R2DBC rollback", ex));
	}

	@Override
	protected Mono<Void> doSetRollbackOnly(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) throws TransactionException {

		return Mono.fromRunnable(() -> {
			ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) status.getTransaction();
			if (status.isDebug()) {
				logger.debug("Setting R2DBC transaction [" + txObject.getConnectionHolder().getConnection() +
						"] rollback-only");
			}
			txObject.setRollbackOnly();
		});
	}

	@Override
	protected Mono<Void> doCleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			Object transaction) {

		return Mono.defer(() -> {
			ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) transaction;

			// Remove the connection holder from the context, if exposed.
			if (txObject.isNewConnectionHolder()) {
				synchronizationManager.unbindResource(obtainConnectionFactory());
			}

			// Reset connection.
			Connection con = txObject.getConnectionHolder().getConnection();

			Mono<Void> afterCleanup = Mono.empty();

			if (txObject.isMustRestoreAutoCommit()) {
				Mono<Void> restoreAutoCommitStep = safeCleanupStep(
						"doCleanupAfterCompletion when restoring autocommit", Mono.from(con.setAutoCommit(true)));
				afterCleanup = afterCleanup.then(restoreAutoCommitStep);
			}

			if (txObject.getPreviousIsolationLevel() != null) {
				Mono<Void> restoreIsolationStep = safeCleanupStep(
						"doCleanupAfterCompletion when restoring isolation level",
						Mono.from(con.setTransactionIsolationLevel(txObject.getPreviousIsolationLevel())));
				afterCleanup = afterCleanup.then(restoreIsolationStep);
			}

			Mono<Void> releaseConnectionStep = Mono.defer(() -> {
				try {
					if (txObject.isNewConnectionHolder()) {
						if (logger.isDebugEnabled()) {
							logger.debug("Releasing R2DBC Connection [" + con + "] after transaction");
						}
						return safeCleanupStep("doCleanupAfterCompletion when releasing R2DBC Connection",
								ConnectionFactoryUtils.releaseConnection(con, obtainConnectionFactory()));
					}
				}
				finally {
					txObject.getConnectionHolder().clear();
				}
				return Mono.empty();
			});
			return afterCleanup.then(releaseConnectionStep);
		});
	}

	private Mono<Void> safeCleanupStep(String stepDescription, Mono<Void> stepMono) {
		if (!logger.isDebugEnabled()) {
			return stepMono.onErrorComplete();
		}
		else {
			return stepMono.doOnError(e ->
							logger.debug(String.format("Error ignored during %s: %s", stepDescription, e)))
					.onErrorComplete();
		}
	}

	/**
	 * 在事务开始之前准备事务性 {@link Connection}。
	 * @deprecated in favor of {@link #prepareTransactionalConnection(Connection, TransactionDefinition)}
	 * since this variant gets called too early (before transaction begin) for read-only customization
	 */
	@Deprecated
	protected Mono<Void> prepareTransactionalConnection(
			Connection con, TransactionDefinition definition, Object transaction) {

		ConnectionFactoryTransactionObject txObject = (ConnectionFactoryTransactionObject) transaction;
		Mono<Void> prepare = Mono.empty();

		// Apply specific isolation level, if any.
		IsolationLevel isolationLevelToUse = resolveIsolationLevel(definition.getIsolationLevel());
		if (isolationLevelToUse != null && definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {

			if (logger.isDebugEnabled()) {
				logger.debug("Changing isolation level of R2DBC Connection [" + con + "] to " + isolationLevelToUse.asSql());
			}
			IsolationLevel currentIsolation = con.getTransactionIsolationLevel();
			if (!currentIsolation.asSql().equalsIgnoreCase(isolationLevelToUse.asSql())) {

				txObject.setPreviousIsolationLevel(currentIsolation);
				prepare = prepare.then(Mono.from(con.setTransactionIsolationLevel(isolationLevelToUse)));
			}
		}

		// Switch to manual commit if necessary. This is very expensive in some R2DBC drivers,
		// so we don't want to do it unnecessarily (for example if we've explicitly
		// configured the connection pool to set it already).
		if (con.isAutoCommit()) {
			txObject.setMustRestoreAutoCommit(true);
			if (logger.isDebugEnabled()) {
				logger.debug("Switching R2DBC Connection [" + con + "] to manual commit");
			}
			prepare = prepare.then(Mono.from(con.setAutoCommit(false)));
		}

		return prepare;
	}

	/**
	 * 在事务开始之后立即准备事务性 {@link Connection}。
	 * <p>如果 {@link #setEnforceReadOnly "enforceReadOnly"} 标志设置为 {@code true}
	 * 且事务定义指示为只读事务，默认实现会执行 "SET TRANSACTION READ ONLY" 语句。
	 * <p>"SET TRANSACTION READ ONLY" 为 Oracle、MySQL 和 Postgres 所理解，
	 * 也可能适用于其他数据库。如果你希望调整这一处理方式，请相应地覆盖此方法。
	 * @param con 事务性 R2DBC Connection
	 * @param definition 当前事务定义
	 * @since 5.3.22
	 * @see #setEnforceReadOnly
	 */
	protected Mono<Void> prepareTransactionalConnection(Connection con, TransactionDefinition definition) {
		Mono<Void> prepare = Mono.empty();
		if (isEnforceReadOnly() && definition.isReadOnly()) {
			prepare = Mono.from(con.createStatement("SET TRANSACTION READ ONLY").execute())
					.flatMapMany(Result::getRowsUpdated)
					.then();
		}
		return prepare;
	}

	/**
	 * 将 {@linkplain TransactionDefinition#getIsolationLevel() 隔离级别常量}
	 * 解析为 R2DBC 的 {@link IsolationLevel}。如果你希望为特定厂商的
	 * {@code IsolationLevel} 扩展隔离级别转换，请相应地覆盖此方法。
	 * @param isolationLevel 要转换的隔离级别。
	 * @return 解析后的隔离级别。如果无法解析，或隔离级别应保持
	 * {@link TransactionDefinition#ISOLATION_DEFAULT default}，则可为 {@code null}。
	 * @see TransactionDefinition#getIsolationLevel()
	 */
	@Nullable
	protected IsolationLevel resolveIsolationLevel(int isolationLevel) {
		switch (isolationLevel) {
			case TransactionDefinition.ISOLATION_READ_COMMITTED:
				return IsolationLevel.READ_COMMITTED;
			case TransactionDefinition.ISOLATION_READ_UNCOMMITTED:
				return IsolationLevel.READ_UNCOMMITTED;
			case TransactionDefinition.ISOLATION_REPEATABLE_READ:
				return IsolationLevel.REPEATABLE_READ;
			case TransactionDefinition.ISOLATION_SERIALIZABLE:
				return IsolationLevel.SERIALIZABLE;
		}
		return null;
	}

	/**
	 * 将给定的 R2DBC 提交/回滚异常转换为通用的 Spring 异常，
	 * 以便从 {@link #commit}/{@link #rollback} 调用中传播出去。
	 * @param task 任务描述（提交或回滚）。
	 * @param ex 由 commit/rollback 抛出的 SQLException。
	 * @return 要发出的转换后异常
	 */
	protected RuntimeException translateException(String task, R2dbcException ex) {
		return ConnectionFactoryUtils.convertR2dbcException(task, null, ex);
	}


	/**
	 * ConnectionFactory 事务对象，表示一个 ConnectionHolder。
	 * 由 R2dbcTransactionManager 用作事务对象。
	 */
	private static class ConnectionFactoryTransactionObject {

		@Nullable
		private ConnectionHolder connectionHolder;

		@Nullable
		private IsolationLevel previousIsolationLevel;

		private boolean newConnectionHolder;

		private boolean mustRestoreAutoCommit;

		void setConnectionHolder(@Nullable ConnectionHolder connectionHolder, boolean newConnectionHolder) {
			setConnectionHolder(connectionHolder);
			this.newConnectionHolder = newConnectionHolder;
		}

		boolean isNewConnectionHolder() {
			return this.newConnectionHolder;
		}

		void setRollbackOnly() {
			getConnectionHolder().setRollbackOnly();
		}

		public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder) {
			this.connectionHolder = connectionHolder;
		}

		public ConnectionHolder getConnectionHolder() {
			Assert.state(this.connectionHolder != null, "No ConnectionHolder available");
			return this.connectionHolder;
		}

		public boolean hasConnectionHolder() {
			return (this.connectionHolder != null);
		}

		public void setPreviousIsolationLevel(@Nullable IsolationLevel previousIsolationLevel) {
			this.previousIsolationLevel = previousIsolationLevel;
		}

		@Nullable
		public IsolationLevel getPreviousIsolationLevel() {
			return this.previousIsolationLevel;
		}

		public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
			this.mustRestoreAutoCommit = mustRestoreAutoCommit;
		}

		public boolean isMustRestoreAutoCommit() {
			return this.mustRestoreAutoCommit;
		}
	}

}
