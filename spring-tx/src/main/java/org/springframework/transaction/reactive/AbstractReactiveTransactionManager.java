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

package org.springframework.transaction.reactive;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * 实现Spring标准响应式事务工作流的抽象基类，
 * 作为具体平台事务管理器的基础。
 *
 * <p>此基类提供以下工作流处理：
 * <ul>
 * <li>判断是否存在现有事务；
 * <li>应用适当的传播行为；
 * <li>必要时挂起和恢复事务；
 * <li>在提交时检查rollback-only标志；
 * <li>在回滚时应用适当的修改
 * （实际回滚或设置rollback-only）；
 * <li>触发已注册的同步回调。
 * </ul>
 *
 * <p>子类需要为事务的特定状态实现特定的模板方法，
 * 例如：begin、suspend、resume、commit、rollback。
 * 其中最重要的方法是抽象的，必须由具体实现提供；
 * 其余方法提供了默认实现，因此重写是可选的。
 *
 * <p>事务同步是一种通用机制，用于注册在事务完成时
 * 被调用的回调。这主要由R2DBC、MongoDB等的数据访问支持类
 * 在内部使用。同样的机制也可以用于应用程序中自定义的同步需求。
 *
 * <p>此类的状态是可序列化的，以允许将事务策略与
 * 携带事务拦截器的代理一起序列化。子类是否希望
 * 使其状态也可序列化由子类自行决定。在这种情况下，
 * 它们应实现{@code java.io.Serializable}标记接口，
 * 并且如果需要恢复任何瞬态状态，可能需要一个私有的
 * {@code readObject()}方法（根据Java序列化规则）。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionSynchronizationManager
 */
@SuppressWarnings("serial")
public abstract class AbstractReactiveTransactionManager implements ReactiveTransactionManager, Serializable {

	protected transient Log logger = LogFactory.getLog(getClass());


	//---------------------------------------------------------------------
	// Implementation of ReactiveTransactionManager
	//---------------------------------------------------------------------

	/**
	 * 此实现处理传播行为。委托给
	 * {@code doGetTransaction}、{@code isExistingTransaction}
	 * 和 {@code doBegin}。
	 * @see #doGetTransaction
	 * @see #isExistingTransaction
	 * @see #doBegin
	 */
	@Override
	public final Mono<ReactiveTransaction> getReactiveTransaction(@Nullable TransactionDefinition definition)
			throws TransactionException {

		// Use defaults if no transaction definition given.
		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());

		return TransactionSynchronizationManager.forCurrentTransaction()
				.flatMap(synchronizationManager -> {

			Object transaction = doGetTransaction(synchronizationManager);

			// Cache debug flag to avoid repeated checks.
			boolean debugEnabled = logger.isDebugEnabled();

			if (isExistingTransaction(transaction)) {
				// Existing transaction found -> check propagation behavior to find out how to behave.
				return handleExistingTransaction(synchronizationManager, def, transaction, debugEnabled);
			}

			// Check definition settings for new transaction.
			if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
				return Mono.error(new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout()));
			}

			// No existing transaction found -> check propagation behavior to find out how to proceed.
			if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
				return Mono.error(new IllegalTransactionStateException(
						"No existing transaction found for transaction marked with propagation 'mandatory'"));
			}
			else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
					def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
					def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {

				return TransactionContextManager.currentContext()
						.map(TransactionSynchronizationManager::new)
						.flatMap(nestedSynchronizationManager ->
								suspend(nestedSynchronizationManager, null)
								.map(Optional::of)
								.defaultIfEmpty(Optional.empty())
								.flatMap(suspendedResources -> {
							if (debugEnabled) {
								logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
							}
							return Mono.defer(() -> {
								GenericReactiveTransaction status = newReactiveTransaction(
										nestedSynchronizationManager, def, transaction, true,
										debugEnabled, suspendedResources.orElse(null));
								return doBegin(nestedSynchronizationManager, transaction, def)
										.doOnSuccess(ignore -> prepareSynchronization(nestedSynchronizationManager, status, def))
										.thenReturn(status);
							}).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR,
									ex -> resume(nestedSynchronizationManager, null, suspendedResources.orElse(null))
									.then(Mono.error(ex)));
						}));
			}
			else {
				// Create "empty" transaction: no actual transaction, but potentially synchronization.
				if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
					logger.warn("Custom isolation level specified but no actual transaction initiated; " +
							"isolation level will effectively be ignored: " + def);
				}
				return Mono.just(prepareReactiveTransaction(synchronizationManager, def, null, true, debugEnabled, null));
			}
		});
	}

	/**
	 * 为现有事务创建一个ReactiveTransaction。
	 */
	private Mono<ReactiveTransaction> handleExistingTransaction(TransactionSynchronizationManager synchronizationManager,
			TransactionDefinition definition, Object transaction, boolean debugEnabled) throws TransactionException {

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
			return Mono.error(new IllegalTransactionStateException(
					"Existing transaction found for transaction marked with propagation 'never'"));
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction");
			}
			Mono<SuspendedResourcesHolder> suspend = suspend(synchronizationManager, transaction);
			return suspend.map(suspendedResources -> prepareReactiveTransaction(synchronizationManager,
					definition, null, false, debugEnabled, suspendedResources)) //
					.switchIfEmpty(Mono.fromSupplier(() -> prepareReactiveTransaction(synchronizationManager,
							definition, null, false, debugEnabled, null)))
					.cast(ReactiveTransaction.class);
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction, creating new transaction with name [" +
						definition.getName() + "]");
			}
			Mono<SuspendedResourcesHolder> suspendedResources = suspend(synchronizationManager, transaction);
			return suspendedResources.flatMap(suspendedResourcesHolder -> {
				GenericReactiveTransaction status = newReactiveTransaction(synchronizationManager,
						definition, transaction, true, debugEnabled, suspendedResourcesHolder);
				return doBegin(synchronizationManager, transaction, definition).doOnSuccess(ignore ->
						prepareSynchronization(synchronizationManager, status, definition)).thenReturn(status)
						.onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, beginEx ->
								resumeAfterBeginException(synchronizationManager, transaction, suspendedResourcesHolder, beginEx)
										.then(Mono.error(beginEx)));
			});
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			if (debugEnabled) {
				logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
			}
			// Nested transaction through nested begin and commit/rollback calls.
			GenericReactiveTransaction status = newReactiveTransaction(synchronizationManager,
					definition, transaction, true, debugEnabled, null);
			return doBegin(synchronizationManager, transaction, definition).doOnSuccess(ignore ->
					prepareSynchronization(synchronizationManager, status, definition)).thenReturn(status);
		}

		// Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
		if (debugEnabled) {
			logger.debug("Participating in existing transaction");
		}
		return Mono.just(prepareReactiveTransaction(synchronizationManager, definition, transaction, false, debugEnabled, null));
	}

	/**
	 * 为给定的参数创建一个新的ReactiveTransaction，
	 * 同时根据需要初始化事务同步。
	 * @see #newReactiveTransaction
	 * @see #prepareReactiveTransaction
	 */
	private GenericReactiveTransaction prepareReactiveTransaction(
			TransactionSynchronizationManager synchronizationManager, TransactionDefinition definition,
			@Nullable Object transaction, boolean newTransaction, boolean debug, @Nullable Object suspendedResources) {

		GenericReactiveTransaction status = newReactiveTransaction(synchronizationManager,
				definition, transaction, newTransaction, debug, suspendedResources);
		prepareSynchronization(synchronizationManager, status, definition);
		return status;
	}

	/**
	 * 为给定的参数创建一个ReactiveTransaction实例。
	 */
	private GenericReactiveTransaction newReactiveTransaction(
			TransactionSynchronizationManager synchronizationManager, TransactionDefinition definition,
			@Nullable Object transaction, boolean newTransaction, boolean debug, @Nullable Object suspendedResources) {

		return new GenericReactiveTransaction(transaction, newTransaction,
				!synchronizationManager.isSynchronizationActive(),
				definition.isReadOnly(), debug, suspendedResources);
	}

	/**
	 * 根据需要初始化事务同步。
	 */
	private void prepareSynchronization(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status, TransactionDefinition definition) {

		if (status.isNewSynchronization()) {
			synchronizationManager.setActualTransactionActive(status.hasTransaction());
			synchronizationManager.setCurrentTransactionIsolationLevel(
					definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
							definition.getIsolationLevel() : null);
			synchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
			synchronizationManager.setCurrentTransactionName(definition.getName());
			synchronizationManager.initSynchronization();
		}
	}


	/**
	 * 挂起给定的事务。首先挂起事务同步，
	 * 然后委托给{@code doSuspend}模板方法。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the current transaction object
	 * (or {@code null} to just suspend active synchronizations, if any)
	 * @return an object that holds suspended resources
	 * (or {@code null} if neither transaction nor synchronization active)
	 * @see #doSuspend
	 * @see #resume
	 */
	private Mono<SuspendedResourcesHolder> suspend(TransactionSynchronizationManager synchronizationManager,
			@Nullable Object transaction) throws TransactionException {

		if (synchronizationManager.isSynchronizationActive()) {
			Mono<List<TransactionSynchronization>> suspendedSynchronizations = doSuspendSynchronization(synchronizationManager);
			return suspendedSynchronizations.flatMap(synchronizations -> {
				Mono<Optional<Object>> suspendedResources = (transaction != null ?
						doSuspend(synchronizationManager, transaction).map(Optional::of).defaultIfEmpty(Optional.empty()) :
						Mono.just(Optional.empty()));
				return suspendedResources.map(it -> {
					String name = synchronizationManager.getCurrentTransactionName();
					synchronizationManager.setCurrentTransactionName(null);
					boolean readOnly = synchronizationManager.isCurrentTransactionReadOnly();
					synchronizationManager.setCurrentTransactionReadOnly(false);
					Integer isolationLevel = synchronizationManager.getCurrentTransactionIsolationLevel();
					synchronizationManager.setCurrentTransactionIsolationLevel(null);
					boolean wasActive = synchronizationManager.isActualTransactionActive();
					synchronizationManager.setActualTransactionActive(false);
					return new SuspendedResourcesHolder(
							it.orElse(null), synchronizations, name, readOnly, isolationLevel, wasActive);
				}).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR,
						ex -> doResumeSynchronization(synchronizationManager, synchronizations)
								.cast(SuspendedResourcesHolder.class));
			});
		}
		else if (transaction != null) {
			// Transaction active but no synchronization active.
			Mono<Optional<Object>> suspendedResources =
					doSuspend(synchronizationManager, transaction).map(Optional::of).defaultIfEmpty(Optional.empty());
			return suspendedResources.map(it -> new SuspendedResourcesHolder(it.orElse(null)));
		}
		else {
			// Neither transaction nor synchronization active.
			return Mono.empty();
		}
	}

	/**
	 * 恢复给定的事务。首先委托给{@code doResume}
	 * 模板方法，然后恢复事务同步。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the current transaction object
	 * @param resourcesHolder the object that holds suspended resources,
	 * as returned by {@code suspend} (or {@code null} to just
	 * resume synchronizations, if any)
	 * @see #doResume
	 * @see #suspend
	 */
	private Mono<Void> resume(TransactionSynchronizationManager synchronizationManager,
			@Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder)
			throws TransactionException {

		Mono<Void> resume = Mono.empty();

		if (resourcesHolder != null) {
			Object suspendedResources = resourcesHolder.suspendedResources;
			if (suspendedResources != null) {
				resume =  doResume(synchronizationManager, transaction, suspendedResources);
			}
			List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
			if (suspendedSynchronizations != null) {
				synchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
				synchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
				synchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
				synchronizationManager.setCurrentTransactionName(resourcesHolder.name);
				return resume.then(doResumeSynchronization(synchronizationManager, suspendedSynchronizations));
			}
		}

		return resume;
	}

	/**
	 * 在内部事务开始失败后恢复外部事务。
	 */
	private Mono<Void> resumeAfterBeginException(TransactionSynchronizationManager synchronizationManager,
			Object transaction, @Nullable SuspendedResourcesHolder suspendedResources, Throwable beginEx) {

		String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
		return resume(synchronizationManager, transaction, suspendedResources).doOnError(ErrorPredicates.RUNTIME_OR_ERROR,
				ex -> logger.error(exMessage, beginEx));
	}

	/**
	 * 挂起当前所有的同步，并为当前事务上下文
	 * 停用事务同步。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @return the List of suspended TransactionSynchronization objects
	 */
	private Mono<List<TransactionSynchronization>> doSuspendSynchronization(
			TransactionSynchronizationManager synchronizationManager) {

		List<TransactionSynchronization> suspendedSynchronizations = synchronizationManager.getSynchronizations();
		return Flux.fromIterable(suspendedSynchronizations)
				.concatMap(TransactionSynchronization::suspend)
				.then(Mono.defer(() -> {
					synchronizationManager.clearSynchronization();
					return Mono.just(suspendedSynchronizations);
				}));
	}

	/**
	 * 为当前事务上下文重新激活事务同步，
	 * 并恢复所有给定的同步。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param suspendedSynchronizations a List of TransactionSynchronization objects
	 */
	private Mono<Void> doResumeSynchronization(TransactionSynchronizationManager synchronizationManager,
			List<TransactionSynchronization> suspendedSynchronizations) {

		synchronizationManager.initSynchronization();
		return Flux.fromIterable(suspendedSynchronizations)
				.concatMap(synchronization -> synchronization.resume()
						.doOnSuccess(ignore -> synchronizationManager.registerSynchronization(synchronization))).then();
	}


	/**
	 * 此提交的实现处理参与现有事务和编程式回滚请求。
	 * 委托给{@code isRollbackOnly}、{@code doCommit}
	 * 和{@code rollback}。
	 * @see ReactiveTransaction#isRollbackOnly()
	 * @see #doCommit
	 * @see #rollback
	 */
	@Override
	public final Mono<Void> commit(ReactiveTransaction transaction) throws TransactionException {
		if (transaction.isCompleted()) {
			return Mono.error(new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction"));
		}

		return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
			GenericReactiveTransaction reactiveTx = (GenericReactiveTransaction) transaction;
			if (reactiveTx.isRollbackOnly()) {
				if (reactiveTx.isDebug()) {
					logger.debug("Transactional code has requested rollback");
				}
				return processRollback(synchronizationManager, reactiveTx);
			}
			return processCommit(synchronizationManager, reactiveTx);
		});
	}

	/**
	 * 处理实际的提交。
	 * 回滚标志已经被检查并应用。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @throws TransactionException in case of commit failure
	 */
	private Mono<Void> processCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) throws TransactionException {

		AtomicBoolean beforeCompletionInvoked = new AtomicBoolean();

		Mono<Object> commit = prepareForCommit(synchronizationManager, status)
				.then(triggerBeforeCommit(synchronizationManager, status))
				.then(triggerBeforeCompletion(synchronizationManager, status))
				.then(Mono.defer(() -> {
					beforeCompletionInvoked.set(true);
					if (status.isNewTransaction()) {
						if (status.isDebug()) {
							logger.debug("Initiating transaction commit");
						}
						return doCommit(synchronizationManager, status);
					}
					return Mono.empty();
				})).then(Mono.empty().onErrorResume(ex -> {
					Mono<Object> propagateException = Mono.error(ex);
					// Store result in a local variable in order to appease the
					// Eclipse compiler with regard to inferred generics.
					Mono<Object> result = propagateException;
					if (ErrorPredicates.UNEXPECTED_ROLLBACK.test(ex)) {
						result = triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_ROLLED_BACK)
								.then(propagateException);
					}
					else if (ErrorPredicates.TRANSACTION_EXCEPTION.test(ex)) {
						result = triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_UNKNOWN)
								.then(propagateException);
					}
					else if (ErrorPredicates.RUNTIME_OR_ERROR.test(ex)) {
						Mono<Void> mono;
						if (!beforeCompletionInvoked.get()) {
							mono = triggerBeforeCompletion(synchronizationManager, status);
						}
						else {
							mono = Mono.empty();
						}
						result = mono.then(doRollbackOnCommitException(synchronizationManager, status, ex))
								.then(propagateException);
					}

					return result;
				})).then(Mono.defer(() -> triggerAfterCommit(synchronizationManager, status).onErrorResume(ex ->
						triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_COMMITTED).then(Mono.error(ex)))
						.then(triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_COMMITTED))));

		return commit
				.onErrorResume(ex -> cleanupAfterCompletion(synchronizationManager, status)
						.then(Mono.error(ex))).then(cleanupAfterCompletion(synchronizationManager, status));
	}

	/**
	 * 此回滚的实现处理参与现有事务。
	 * 委托给{@code doRollback}和{@code doSetRollbackOnly}。
	 * @see #doRollback
	 * @see #doSetRollbackOnly
	 */
	@Override
	public final Mono<Void> rollback(ReactiveTransaction transaction) throws TransactionException {
		if (transaction.isCompleted()) {
			return Mono.error(new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction"));
		}
		return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
			GenericReactiveTransaction reactiveTx = (GenericReactiveTransaction) transaction;
			return processRollback(synchronizationManager, reactiveTx);
		});
	}

	/**
	 * 处理实际的回滚。
	 * 完成标志已经被检查。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @throws TransactionException in case of rollback failure
	 */
	private Mono<Void> processRollback(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		return triggerBeforeCompletion(synchronizationManager, status).then(Mono.defer(() -> {
			if (status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.debug("Initiating transaction rollback");
				}
				return doRollback(synchronizationManager, status);
			}
			else {
				Mono<Void> beforeCompletion = Mono.empty();
				// Participating in larger transaction
				if (status.hasTransaction()) {
					if (status.isDebug()) {
						logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
					}
					beforeCompletion = doSetRollbackOnly(synchronizationManager, status);
				}
				else {
					logger.debug("Should roll back transaction but cannot - no transaction available");
				}
				return beforeCompletion;
			}
		})).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, ex -> triggerAfterCompletion(
				synchronizationManager, status, TransactionSynchronization.STATUS_UNKNOWN)
				.then(Mono.error(ex)))
				.then(Mono.defer(() -> triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_ROLLED_BACK)))
				.onErrorResume(ex -> cleanupAfterCompletion(synchronizationManager, status).then(Mono.error(ex)))
			.then(cleanupAfterCompletion(synchronizationManager, status));
	}

	/**
	 * 调用{@code doRollback}，正确处理回滚异常。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @param ex the thrown application exception or error
	 * @throws TransactionException in case of rollback failure
	 * @see #doRollback
	 */
	private Mono<Void> doRollbackOnCommitException(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status, Throwable ex) throws TransactionException {

		return Mono.defer(() -> {
			if (status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.debug("Initiating transaction rollback after commit exception", ex);
				}
				return doRollback(synchronizationManager, status);
			}
			else if (status.hasTransaction()) {
				if (status.isDebug()) {
					logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
				}
				return doSetRollbackOnly(synchronizationManager, status);
			}
			return Mono.empty();
		}).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, rbex -> {
			logger.error("Commit exception overridden by rollback exception", ex);
			return triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_UNKNOWN)
				.then(Mono.error(rbex));
		}).then(triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_ROLLED_BACK));
	}


	/**
	 * 触发{@code beforeCommit}回调。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 */
	private Mono<Void> triggerBeforeCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		if (status.isNewSynchronization()) {
			return TransactionSynchronizationUtils.triggerBeforeCommit(
					synchronizationManager.getSynchronizations(), status.isReadOnly());
		}
		return Mono.empty();
	}

	/**
	 * 触发{@code beforeCompletion}回调。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 */
	private Mono<Void> triggerBeforeCompletion(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		if (status.isNewSynchronization()) {
			return TransactionSynchronizationUtils.triggerBeforeCompletion(synchronizationManager.getSynchronizations());
		}
		return Mono.empty();
	}

	/**
	 * 触发{@code afterCommit}回调。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 */
	private Mono<Void> triggerAfterCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		if (status.isNewSynchronization()) {
			return TransactionSynchronizationUtils.invokeAfterCommit(synchronizationManager.getSynchronizations());
		}
		return Mono.empty();
	}

	/**
	 * 触发{@code afterCompletion}回调。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @param completionStatus completion status according to TransactionSynchronization constants
	 */
	private Mono<Void> triggerAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status, int completionStatus) {

		if (status.isNewSynchronization()) {
			List<TransactionSynchronization> synchronizations = synchronizationManager.getSynchronizations();
			synchronizationManager.clearSynchronization();
			if (!status.hasTransaction() || status.isNewTransaction()) {
				// No transaction or new transaction for the current scope ->
				// invoke the afterCompletion callbacks immediately
				return invokeAfterCompletion(synchronizationManager, synchronizations, completionStatus);
			}
			else if (!synchronizations.isEmpty()) {
				// Existing transaction that we participate in, controlled outside
				// the scope of this Spring transaction manager -> try to register
				// an afterCompletion callback with the existing (JTA) transaction.
				return registerAfterCompletionWithExistingTransaction(
						synchronizationManager, status.getTransaction(), synchronizations);
			}
		}
		return Mono.empty();
	}

	/**
	 * 实际调用给定TransactionSynchronization对象的
	 * {@code afterCompletion}方法。
	 * <p>由此抽象管理器自身调用，或由
	 * {@code registerAfterCompletionWithExistingTransaction}回调的特殊实现调用。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @param completionStatus the completion status according to the
	 * constants in the TransactionSynchronization interface
	 * @see #registerAfterCompletionWithExistingTransaction(TransactionSynchronizationManager, Object, List)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	private Mono<Void> invokeAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			List<TransactionSynchronization> synchronizations, int completionStatus) {

		return TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
	}

	/**
	 * 完成后清理，必要时清除同步，
	 * 并调用doCleanupAfterCompletion。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @see #doCleanupAfterCompletion
	 */
	private Mono<Void> cleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		return Mono.defer(() -> {
			status.setCompleted();
			if (status.isNewSynchronization()) {
				synchronizationManager.clear();
			}
			Mono<Void> cleanup = Mono.empty();
			if (status.isNewTransaction()) {
				cleanup = doCleanupAfterCompletion(synchronizationManager, status.getTransaction());
			}
			if (status.getSuspendedResources() != null) {
				if (status.isDebug()) {
					logger.debug("Resuming suspended transaction after completion of inner transaction");
				}
				Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
				return cleanup.then(resume(synchronizationManager, transaction,
						(SuspendedResourcesHolder) status.getSuspendedResources()));
			}
			return cleanup;
		});
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented in subclasses
	//---------------------------------------------------------------------

	/**
	 * 返回当前事务状态的事务对象。
	 * <p>返回的对象通常是特定于具体事务管理器实现的，
	 * 以可修改的方式携带相应的事务状态。此对象将被传入
	 * 其他模板方法（例如doBegin和doCommit），可以直接传入，
	 * 也可以作为DefaultReactiveTransactionStatus实例的一部分传入。
	 * <p>返回的对象应包含有关任何现有事务的信息，
	 * 即在事务管理器上调用当前{@code getTransaction}之前
	 * 已经启动的事务。因此，{@code doGetTransaction}实现通常会
	 * 查找现有事务并将相应的状态存储在返回的事务对象中。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @return the current transaction object
	 * @throws org.springframework.transaction.CannotCreateTransactionException
	 * if transaction support is not available
	 * @throws TransactionException in case of lookup or system errors
	 * @see #doBegin
	 * @see #doCommit
	 * @see #doRollback
	 * @see GenericReactiveTransaction#getTransaction
	 */
	protected abstract Object doGetTransaction(TransactionSynchronizationManager synchronizationManager)
			throws TransactionException;

	/**
	 * 检查给定的事务对象是否指示存在一个事务
	 * （即已经启动的事务）。
	 * <p>结果将根据为新事务指定的传播行为进行评估。
	 * 现有事务可能会被挂起（在PROPAGATION_REQUIRES_NEW的情况下），
	 * 或者新事务可能会参与现有事务（在PROPAGATION_REQUIRED的情况下）。
	 * <p>默认实现返回{@code false}，假定通常不支持
	 * 参与现有事务。当然鼓励子类提供此类支持。
	 * @param transaction the transaction object returned by doGetTransaction
	 * @return if there is an existing transaction
	 * @throws TransactionException in case of system errors
	 * @see #doGetTransaction
	 */
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		return false;
	}

	/**
	 * 根据给定的事务定义语义开始一个新事务。
	 * 不需要关心应用传播行为，因为这已经由此抽象管理器处理。
	 * <p>当事务管理器决定实际启动一个新事务时，会调用此方法。
	 * 要么之前没有任何事务，要么之前的事务已被挂起。
	 * <p>一个特殊的场景是嵌套事务：此方法将在必要时被调用以
	 * 启动嵌套事务。在这种情况下，将存在一个活跃的事务：
	 * 此方法的实现必须检测到这一点并启动一个适当的嵌套事务。
	 * @param synchronizationManager the synchronization manager bound to the new transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @param definition a TransactionDefinition instance, describing propagation
	 * behavior, isolation level, read-only flag, timeout, and transaction name
	 * @throws TransactionException in case of creation or system errors
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
	 * if the underlying transaction does not support nesting (e.g. through savepoints)
	 */
	protected abstract Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager,
			Object transaction, TransactionDefinition definition) throws TransactionException;

	/**
	 * 挂起当前事务的资源。
	 * 事务同步将已经被挂起。
	 * <p>默认实现抛出TransactionSuspensionNotSupportedException，
	 * 假定通常不支持事务挂起。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @return an object that holds suspended resources
	 * (will be kept unexamined for passing it into doResume)
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * if suspending is not supported by the transaction manager implementation
	 * @throws TransactionException in case of system errors
	 * @see #doResume
	 */
	protected Mono<Object> doSuspend(TransactionSynchronizationManager synchronizationManager,
			Object transaction) throws TransactionException {

		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * 恢复当前事务的资源。
	 * 事务同步将在此之后恢复。
	 * <p>默认实现抛出TransactionSuspensionNotSupportedException，
	 * 假定通常不支持事务挂起。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @param suspendedResources the object that holds suspended resources,
	 * as returned by doSuspend
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * if suspending is not supported by the transaction manager implementation
	 * @throws TransactionException in case of system errors
	 * @see #doSuspend
	 */
	protected Mono<Void> doResume(TransactionSynchronizationManager synchronizationManager,
			@Nullable Object transaction, Object suspendedResources) throws TransactionException {

		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * 为提交做准备，在{@code beforeCommit}同步回调发生之前执行。
	 * <p>请注意，异常将传播到提交调用者，
	 * 并导致事务回滚。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status the status representation of the transaction
	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
	 * (note: do not throw TransactionException subclasses here!)
	 */
	protected Mono<Void> prepareForCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		return Mono.empty();
	}

	/**
	 * 执行给定事务的实际提交。
	 * <p>实现不需要检查"新事务"标志或rollback-only标志；
	 * 这些在此之前已经被处理。通常，将对传入状态中包含的
	 * 事务对象执行直接提交。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of commit or system errors
	 * @see GenericReactiveTransaction#getTransaction
	 */
	protected abstract Mono<Void> doCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) throws TransactionException;

	/**
	 * 执行给定事务的实际回滚。
	 * <p>实现不需要检查"新事务"标志；
	 * 这在此之前已经被处理。通常，将对传入状态中包含的
	 * 事务对象执行直接回滚。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of system errors
	 * @see GenericReactiveTransaction#getTransaction
	 */
	protected abstract Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) throws TransactionException;

	/**
	 * 将给定事务设置为rollback-only。仅在回滚时调用，
	 * 当前事务参与一个现有事务的情况下。
	 * <p>默认实现抛出IllegalTransactionStateException，
	 * 假定通常不支持参与现有事务。
	 * 当然鼓励子类提供此类支持。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of system errors
	 */
	protected Mono<Void> doSetRollbackOnly(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) throws TransactionException {

		throw new IllegalTransactionStateException(
				"Participating in existing transactions is not supported - when 'isExistingTransaction' " +
				"returns true, appropriate 'doSetRollbackOnly' behavior must be provided");
	}

	/**
	 * 将给定的事务同步列表注册到现有事务中。
	 * <p>当Spring事务管理器的控制以及所有Spring事务同步结束时，
	 * 而事务尚未完成时调用。例如，参与现有JTA或EJB CMT事务时
	 * 就是这种情况。
	 * <p>默认实现直接调用{@code afterCompletion}方法，
	 * 传入"STATUS_UNKNOWN"。如果无法确定外部事务的实际结果，
	 * 这是我们能做的最好的处理。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @throws TransactionException in case of system errors
	 * @see #invokeAfterCompletion(TransactionSynchronizationManager, List, int)
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	protected Mono<Void> registerAfterCompletionWithExistingTransaction(TransactionSynchronizationManager synchronizationManager,
			Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {

		logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " +
				"processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
		return invokeAfterCompletion(synchronizationManager, synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
	}

	/**
	 * 事务完成后清理资源。
	 * <p>在{@code doCommit}和{@code doRollback}执行之后调用，
	 * 无论结果如何。默认实现不执行任何操作。
	 * <p>不应抛出任何异常，只在出错时发出警告。
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 */
	protected Mono<Void> doCleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			Object transaction) {

		return Mono.empty();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.logger = LogFactory.getLog(getClass());
	}


	/**
	 * 挂起资源的持有者。
	 * 由{@code suspend}和{@code resume}内部使用。
	 */
	protected static final class SuspendedResourcesHolder {

		@Nullable
		private final Object suspendedResources;

		@Nullable
		private List<TransactionSynchronization> suspendedSynchronizations;

		@Nullable
		private String name;

		private boolean readOnly;

		@Nullable
		private Integer isolationLevel;

		private boolean wasActive;

		private SuspendedResourcesHolder(@Nullable Object suspendedResources) {
			this.suspendedResources = suspendedResources;
		}

		private SuspendedResourcesHolder(
				@Nullable Object suspendedResources, List<TransactionSynchronization> suspendedSynchronizations,
				@Nullable String name, boolean readOnly, @Nullable Integer isolationLevel, boolean wasActive) {

			this.suspendedResources = suspendedResources;
			this.suspendedSynchronizations = suspendedSynchronizations;
			this.name = name;
			this.readOnly = readOnly;
			this.isolationLevel = isolationLevel;
			this.wasActive = wasActive;
		}
	}


	/**
	 * 事务性错误处理适用的异常类型谓词。
	 */
	private enum ErrorPredicates implements Predicate<Throwable> {

		/**
		 * 匹配{@link RuntimeException}或{@link Error}的谓词。
		 */
		RUNTIME_OR_ERROR {
			@Override
			public boolean test(Throwable throwable) {
				return throwable instanceof RuntimeException || throwable instanceof Error;
			}
		},

		/**
		 * 匹配{@link TransactionException}的谓词。
		 */
		TRANSACTION_EXCEPTION {
			@Override
			public boolean test(Throwable throwable) {
				return throwable instanceof TransactionException;
			}
		},

		/**
		 * 匹配{@link UnexpectedRollbackException}的谓词。
		 */
		UNEXPECTED_ROLLBACK {
			@Override
			public boolean test(Throwable throwable) {
				return throwable instanceof UnexpectedRollbackException;
			}
		};

		@Override
		public abstract boolean test(Throwable throwable);
	}

}
