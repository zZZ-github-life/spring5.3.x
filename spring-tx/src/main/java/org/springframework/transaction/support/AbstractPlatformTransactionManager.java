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

package org.springframework.transaction.support;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * 实现 Spring 标准事务工作流的抽象基类，
 * 作为具体平台事务管理器的基础，如 {@link org.springframework.transaction.jta.JtaTransactionManager}。
 *
 * <p>此基类提供以下工作流处理：
 * <ul>
 * <li>判断是否存在现有事务；
 * <li>应用相应的传播行为；
 * <li>在必要时挂起和恢复事务；
 * <li>在提交时检查 rollback-only 标志；
 * <li>在回滚时应用相应的处理（实际回滚或设置 rollback-only）；
 * <li>触发已注册的同步回调（如果事务同步处于激活状态）。
 * </ul>
 *
 * <p>子类需要为事务的特定状态实现特定的模板方法，例如：begin、suspend、resume、commit、rollback。
 * 其中最重要的方法是抽象方法，必须由具体实现提供；其余方法提供了默认实现，因此重写是可选的。
 *
 * <p>事务同步是一种通用机制，用于注册在事务完成时被调用的回调。这主要由 JDBC、Hibernate、JPA
 * 等数据访问支持类在 JTA 事务中内部使用：它们注册在事务内打开的资源，以便在事务完成时关闭，
 * 从而允许例如在事务内重用同一个 Hibernate Session。同样的机制也可以用于应用程序中自定义的同步需求。
 *
 * <p>此类的状态是可序列化的，允许将事务策略与携带事务拦截器的代理一起序列化。
 * 子类可以自行决定是否也使其状态可序列化。如果需要，它们应实现 {@code java.io.Serializable}
 * 标记接口，并可能需要实现私有的 {@code readObject()} 方法（根据 Java 序列化规则）
 * 以恢复任何瞬态状态。
 *
 * @author Juergen Hoeller
 * @since 28.03.2003
 * @see #setTransactionSynchronization
 * @see TransactionSynchronizationManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 */
@SuppressWarnings("serial")
public abstract class AbstractPlatformTransactionManager implements PlatformTransactionManager, Serializable {

	/**
	 * 始终激活事务同步，即使对于因 PROPAGATION_SUPPORTS 且没有现有后端事务而产生的"空"事务。
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_SUPPORTS
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NOT_SUPPORTED
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NEVER
	 */
	public static final int SYNCHRONIZATION_ALWAYS = 0;

	/**
	 * 仅对实际事务激活事务同步，即不对因 PROPAGATION_SUPPORTS 且没有现有后端事务而产生的空事务激活。
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_MANDATORY
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRES_NEW
	 */
	public static final int SYNCHRONIZATION_ON_ACTUAL_TRANSACTION = 1;

	/**
	 * 从不同步事务，即使是实际事务也不同步。
	 */
	public static final int SYNCHRONIZATION_NEVER = 2;


	/** AbstractPlatformTransactionManager 的常量实例。 */
	private static final Constants constants = new Constants(AbstractPlatformTransactionManager.class);


	protected transient Log logger = LogFactory.getLog(getClass());

	private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;

	private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;

	private boolean nestedTransactionAllowed = false;

	private boolean validateExistingTransaction = false;

	private boolean globalRollbackOnParticipationFailure = true;

	private boolean failEarlyOnGlobalRollbackOnly = false;

	private boolean rollbackOnCommitFailure = false;


	/**
	 * 通过此类中对应常量的名称设置事务同步，例如 "SYNCHRONIZATION_ALWAYS"。
	 * @param constantName 常量名称
	 * @see #SYNCHRONIZATION_ALWAYS
	 */
	public final void setTransactionSynchronizationName(String constantName) {
		setTransactionSynchronization(constants.asNumber(constantName).intValue());
	}

	/**
	 * 设置此事务管理器何时应激活线程绑定的事务同步支持。默认为 "always"。
	 * <p>请注意，不同事务管理器不支持多个并发事务的事务同步。
	 * 任何时候只允许一个事务管理器激活它。
	 * @see #SYNCHRONIZATION_ALWAYS
	 * @see #SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
	 * @see #SYNCHRONIZATION_NEVER
	 * @see TransactionSynchronizationManager
	 * @see TransactionSynchronization
	 */
	public final void setTransactionSynchronization(int transactionSynchronization) {
		this.transactionSynchronization = transactionSynchronization;
	}

	/**
	 * 返回此事务管理器是否应激活线程绑定的事务同步支持。
	 */
	public final int getTransactionSynchronization() {
		return this.transactionSynchronization;
	}

	/**
	 * 指定当事务级别未指定超时时，此事务管理器应使用的默认超时时间（以秒为单位）。
	 * <p>默认为底层事务基础设施的默认超时时间，例如 JTA 提供者通常为 30 秒，
	 * 由 {@code TransactionDefinition.TIMEOUT_DEFAULT} 值表示。
	 * @see org.springframework.transaction.TransactionDefinition#TIMEOUT_DEFAULT
	 */
	public final void setDefaultTimeout(int defaultTimeout) {
		if (defaultTimeout < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid default timeout", defaultTimeout);
		}
		this.defaultTimeout = defaultTimeout;
	}

	/**
	 * 返回当事务级别未指定超时时，此事务管理器应使用的默认超时时间（以秒为单位）。
	 * <p>返回 {@code TransactionDefinition.TIMEOUT_DEFAULT} 表示底层事务基础设施的默认超时时间。
	 */
	public final int getDefaultTimeout() {
		return this.defaultTimeout;
	}

	/**
	 * 设置是否允许嵌套事务。默认为 "false"。
	 * <p>通常由具体事务管理器子类以适当的默认值初始化。
	 */
	public final void setNestedTransactionAllowed(boolean nestedTransactionAllowed) {
		this.nestedTransactionAllowed = nestedTransactionAllowed;
	}

	/**
	 * 返回是否允许嵌套事务。
	 */
	public final boolean isNestedTransactionAllowed() {
		return this.nestedTransactionAllowed;
	}

	/**
	 * 设置在参与现有事务之前是否应对其进行验证。
	 * <p>当参与现有事务时（例如 PROPAGATION_REQUIRED 或 PROPAGATION_SUPPORTS 遇到现有事务），
	 * 外部事务的特征将应用于内部事务范围。验证将检测内部事务定义中不兼容的隔离级别和只读设置，
	 * 并通过抛出相应异常来拒绝参与。
	 * <p>默认为 "false"，宽松地忽略内部事务设置，直接用外部事务的特征覆盖。
	 * 将此标志切换为 "true" 以强制执行严格验证。
	 * @since 2.5.1
	 */
	public final void setValidateExistingTransaction(boolean validateExistingTransaction) {
		this.validateExistingTransaction = validateExistingTransaction;
	}

	/**
	 * 返回在参与现有事务之前是否应对其进行验证。
	 * @since 2.5.1
	 */
	public final boolean isValidateExistingTransaction() {
		return this.validateExistingTransaction;
	}

	/**
	 * 设置在参与事务失败后，是否将现有事务全局标记为 rollback-only。
	 * <p>默认为 "true"：如果参与事务（例如 PROPAGATION_REQUIRED 或 PROPAGATION_SUPPORTS
	 * 遇到现有事务）失败，该事务将被全局标记为 rollback-only。此类事务唯一可能的结果是回滚：
	 * 事务发起者<i>不再</i>能使事务提交。
	 * <p>将此切换为 "false"，让事务发起者自行决定回滚。如果参与事务抛出异常，
	 * 调用者仍可以在事务中决定继续走不同的路径。但是，请注意，这只有在所有参与资源
	 * 即使在数据访问失败后仍能继续提交事务的情况下才有效：例如，Hibernate Session
	 * 通常不是这种情况；一系列 JDBC insert/update/delete 操作也不是。
	 * <p><b>注意：</b>此标志仅适用于子事务的显式回滚尝试，通常由数据访问操作抛出的异常引起
	 * （其中 TransactionInterceptor 将根据回滚规则触发 {@code PlatformTransactionManager.rollback()}
	 * 调用）。如果此标志关闭，调用者可以处理异常并自行决定回滚，而不受子事务回滚规则的影响。
	 * 但是，此标志<i>不</i>适用于 {@code TransactionStatus} 上的显式 {@code setRollbackOnly}
	 * 调用，后者将始终导致最终的全局回滚（因为在 rollback-only 调用后可能不会抛出异常）。
	 * <p>处理子事务失败的推荐方案是"嵌套事务"，其中全局事务可以回滚到子事务开始时获取的保存点。
	 * PROPAGATION_NESTED 正好提供了这些语义；但是，它只在嵌套事务支持可用时才能工作。
	 * DataSourceTransactionManager 支持嵌套事务，但 JtaTransactionManager 不支持。
	 * @see #setNestedTransactionAllowed
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	public final void setGlobalRollbackOnParticipationFailure(boolean globalRollbackOnParticipationFailure) {
		this.globalRollbackOnParticipationFailure = globalRollbackOnParticipationFailure;
	}

	/**
	 * 返回在参与事务失败后，是否将现有事务全局标记为 rollback-only。
	 */
	public final boolean isGlobalRollbackOnParticipationFailure() {
		return this.globalRollbackOnParticipationFailure;
	}

	/**
	 * 设置在事务被全局标记为 rollback-only 时是否提前失败。
	 * <p>默认为 "false"，仅在最外层事务边界导致 UnexpectedRollbackException。
	 * 启用此标志后，一旦首次检测到全局 rollback-only 标记，就会立即导致
	 * UnexpectedRollbackException，即使在内部事务边界内也是如此。
	 * <p>请注意，从 Spring 2.0 开始，全局 rollback-only 标记的提前失败行为已统一：
	 * 所有事务管理器默认仅在最外层事务边界导致 UnexpectedRollbackException。
	 * 这样，例如，即使操作失败且事务永远不会完成，单元测试仍可继续。
	 * 只有将此标志显式设置为 "true"，所有事务管理器才会提前失败。
	 * @since 2.0
	 * @see org.springframework.transaction.UnexpectedRollbackException
	 */
	public final void setFailEarlyOnGlobalRollbackOnly(boolean failEarlyOnGlobalRollbackOnly) {
		this.failEarlyOnGlobalRollbackOnly = failEarlyOnGlobalRollbackOnly;
	}

	/**
	 * 返回在事务被全局标记为 rollback-only 时是否提前失败。
	 * @since 2.0
	 */
	public final boolean isFailEarlyOnGlobalRollbackOnly() {
		return this.failEarlyOnGlobalRollbackOnly;
	}

	/**
	 * 设置在 {@code doCommit} 调用失败时是否应执行 {@code doRollback}。
	 * 通常不需要，因此应避免，因为它可能会用后续的回滚异常覆盖提交异常。
	 * <p>默认为 "false"。
	 * @see #doCommit
	 * @see #doRollback
	 */
	public final void setRollbackOnCommitFailure(boolean rollbackOnCommitFailure) {
		this.rollbackOnCommitFailure = rollbackOnCommitFailure;
	}

	/**
	 * 返回在 {@code doCommit} 调用失败时是否应执行 {@code doRollback}。
	 */
	public final boolean isRollbackOnCommitFailure() {
		return this.rollbackOnCommitFailure;
	}


	//---------------------------------------------------------------------
	// Implementation of PlatformTransactionManager
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
	public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
			throws TransactionException {

		// Use defaults if no transaction definition given.
		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());

		Object transaction = doGetTransaction();
		boolean debugEnabled = logger.isDebugEnabled();

		if (isExistingTransaction(transaction)) {
			// Existing transaction found -> check propagation behavior to find out how to behave.
			return handleExistingTransaction(def, transaction, debugEnabled);
		}

		// Check definition settings for new transaction.
		if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());
		}

		// No existing transaction found -> check propagation behavior to find out how to proceed.
		if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
			throw new IllegalTransactionStateException(
					"No existing transaction found for transaction marked with propagation 'mandatory'");
		}
		else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			SuspendedResourcesHolder suspendedResources = suspend(null);
			if (debugEnabled) {
				logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
			}
			try {
				return startTransaction(def, transaction, debugEnabled, suspendedResources);
			}
			catch (RuntimeException | Error ex) {
				resume(null, suspendedResources);
				throw ex;
			}
		}
		else {
			// Create "empty" transaction: no actual transaction, but potentially synchronization.
			if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
				logger.warn("Custom isolation level specified but no actual transaction initiated; " +
						"isolation level will effectively be ignored: " + def);
			}
			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
		}
	}

	/**
	 * 启动一个新事务。
	 */
	private TransactionStatus startTransaction(TransactionDefinition definition, Object transaction,
			boolean debugEnabled, @Nullable SuspendedResourcesHolder suspendedResources) {

		boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
		DefaultTransactionStatus status = newTransactionStatus(
				definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
		doBegin(transaction, definition);
		prepareSynchronization(status, definition);
		return status;
	}

	/**
	 * 为现有事务创建 TransactionStatus。
	 */
	private TransactionStatus handleExistingTransaction(
			TransactionDefinition definition, Object transaction, boolean debugEnabled)
			throws TransactionException {

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
			throw new IllegalTransactionStateException(
					"Existing transaction found for transaction marked with propagation 'never'");
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction");
			}
			Object suspendedResources = suspend(transaction);
			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			return prepareTransactionStatus(
					definition, null, false, newSynchronization, debugEnabled, suspendedResources);
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction, creating new transaction with name [" +
						definition.getName() + "]");
			}
			SuspendedResourcesHolder suspendedResources = suspend(transaction);
			try {
				return startTransaction(definition, transaction, debugEnabled, suspendedResources);
			}
			catch (RuntimeException | Error beginEx) {
				resumeAfterBeginException(transaction, suspendedResources, beginEx);
				throw beginEx;
			}
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			if (!isNestedTransactionAllowed()) {
				throw new NestedTransactionNotSupportedException(
						"Transaction manager does not allow nested transactions by default - " +
						"specify 'nestedTransactionAllowed' property with value 'true'");
			}
			if (debugEnabled) {
				logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
			}
			if (useSavepointForNestedTransaction()) {
				// Create savepoint within existing Spring-managed transaction,
				// through the SavepointManager API implemented by TransactionStatus.
				// Usually uses JDBC 3.0 savepoints. Never activates Spring synchronization.
				DefaultTransactionStatus status =
						prepareTransactionStatus(definition, transaction, false, false, debugEnabled, null);
				status.createAndHoldSavepoint();
				return status;
			}
			else {
				// Nested transaction through nested begin and commit/rollback calls.
				// Usually only for JTA: Spring synchronization might get activated here
				// in case of a pre-existing JTA transaction.
				return startTransaction(definition, transaction, debugEnabled, null);
			}
		}

		// Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
		if (debugEnabled) {
			logger.debug("Participating in existing transaction");
		}
		if (isValidateExistingTransaction()) {
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				Integer currentIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
				if (currentIsolationLevel == null || currentIsolationLevel != definition.getIsolationLevel()) {
					Constants isoConstants = DefaultTransactionDefinition.constants;
					throw new IllegalTransactionStateException("Participating transaction with definition [" +
							definition + "] specifies isolation level which is incompatible with existing transaction: " +
							(currentIsolationLevel != null ?
									isoConstants.toCode(currentIsolationLevel, DefaultTransactionDefinition.PREFIX_ISOLATION) :
									"(unknown)"));
				}
			}
			if (!definition.isReadOnly()) {
				if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
					throw new IllegalTransactionStateException("Participating transaction with definition [" +
							definition + "] is not marked as read-only but existing transaction is");
				}
			}
		}
		boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
		return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
	}

	/**
	 * 为给定参数创建新的 TransactionStatus，
	 * 同时按需初始化事务同步。
	 * @see #newTransactionStatus
	 * @see #prepareTransactionStatus
	 */
	protected final DefaultTransactionStatus prepareTransactionStatus(
			TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {

		DefaultTransactionStatus status = newTransactionStatus(
				definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
		prepareSynchronization(status, definition);
		return status;
	}

	/**
	 * 为给定参数创建 TransactionStatus 实例。
	 */
	protected DefaultTransactionStatus newTransactionStatus(
			TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {

		boolean actualNewSynchronization = newSynchronization &&
				!TransactionSynchronizationManager.isSynchronizationActive();
		return new DefaultTransactionStatus(
				transaction, newTransaction, actualNewSynchronization,
				definition.isReadOnly(), debug, suspendedResources);
	}

	/**
	 * 按需初始化事务同步。
	 */
	protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
			TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
					definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
							definition.getIsolationLevel() : null);
			TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
			TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
			TransactionSynchronizationManager.initSynchronization();
		}
	}

	/**
	 * 确定给定定义要使用的实际超时时间。
	 * 如果事务定义未指定非默认值，则回退到此管理器的默认超时时间。
	 * @param definition 事务定义
	 * @return 要使用的实际超时时间
	 * @see org.springframework.transaction.TransactionDefinition#getTimeout()
	 * @see #setDefaultTimeout
	 */
	protected int determineTimeout(TransactionDefinition definition) {
		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			return definition.getTimeout();
		}
		return getDefaultTimeout();
	}


	/**
	 * 挂起给定事务。先挂起事务同步，然后委托给 {@code doSuspend} 模板方法。
	 * @param transaction 当前事务对象
	 * （或 {@code null} 表示仅挂起活动同步，如果有的话）
	 * @return 持有被挂起资源的对象
	 * （如果事务和同步都不活动则为 {@code null}）
	 * @see #doSuspend
	 * @see #resume
	 */
	@Nullable
	protected final SuspendedResourcesHolder suspend(@Nullable Object transaction) throws TransactionException {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
			try {
				Object suspendedResources = null;
				if (transaction != null) {
					suspendedResources = doSuspend(transaction);
				}
				String name = TransactionSynchronizationManager.getCurrentTransactionName();
				TransactionSynchronizationManager.setCurrentTransactionName(null);
				boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
				Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
				TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
				boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
				TransactionSynchronizationManager.setActualTransactionActive(false);
				return new SuspendedResourcesHolder(
						suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
			}
			catch (RuntimeException | Error ex) {
				// doSuspend failed - original transaction is still active...
				doResumeSynchronization(suspendedSynchronizations);
				throw ex;
			}
		}
		else if (transaction != null) {
			// Transaction active but no synchronization active.
			Object suspendedResources = doSuspend(transaction);
			return new SuspendedResourcesHolder(suspendedResources);
		}
		else {
			// Neither transaction nor synchronization active.
			return null;
		}
	}

	/**
	 * 恢复给定事务。先委托给 {@code doResume} 模板方法，然后恢复事务同步。
	 * @param transaction 当前事务对象
	 * @param resourcesHolder 持有被挂起资源的对象，
	 * 由 {@code suspend} 返回（或 {@code null} 表示仅恢复同步，如果有的话）
	 * @see #doResume
	 * @see #suspend
	 */
	protected final void resume(@Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder)
			throws TransactionException {

		if (resourcesHolder != null) {
			Object suspendedResources = resourcesHolder.suspendedResources;
			if (suspendedResources != null) {
				doResume(transaction, suspendedResources);
			}
			List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
			if (suspendedSynchronizations != null) {
				TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
				TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
				TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
				doResumeSynchronization(suspendedSynchronizations);
			}
		}
	}

	/**
	 * 在内部事务 begin 失败后恢复外部事务。
	 */
	private void resumeAfterBeginException(
			Object transaction, @Nullable SuspendedResourcesHolder suspendedResources, Throwable beginEx) {

		try {
			resume(transaction, suspendedResources);
		}
		catch (RuntimeException | Error resumeEx) {
			String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
			logger.error(exMessage, beginEx);
			throw resumeEx;
		}
	}

	/**
	 * 挂起所有当前同步并停用当前线程的事务同步。
	 * @return 被挂起的 TransactionSynchronization 对象列表
	 */
	private List<TransactionSynchronization> doSuspendSynchronization() {
		List<TransactionSynchronization> suspendedSynchronizations =
				TransactionSynchronizationManager.getSynchronizations();
		for (TransactionSynchronization synchronization : suspendedSynchronizations) {
			synchronization.suspend();
		}
		TransactionSynchronizationManager.clearSynchronization();
		return suspendedSynchronizations;
	}

	/**
	 * 重新激活当前线程的事务同步并恢复所有给定的同步。
	 * @param suspendedSynchronizations TransactionSynchronization 对象列表
	 */
	private void doResumeSynchronization(List<TransactionSynchronization> suspendedSynchronizations) {
		TransactionSynchronizationManager.initSynchronization();
		for (TransactionSynchronization synchronization : suspendedSynchronizations) {
			synchronization.resume();
			TransactionSynchronizationManager.registerSynchronization(synchronization);
		}
	}


	/**
	 * 此提交的实现处理参与现有事务和编程式回滚请求。
	 * 委托给 {@code isRollbackOnly}、{@code doCommit} 和 {@code rollback}。
	 * @see org.springframework.transaction.TransactionStatus#isRollbackOnly()
	 * @see #doCommit
	 * @see #rollback
	 */
	@Override
	public final void commit(TransactionStatus status) throws TransactionException {
		if (status.isCompleted()) {
			throw new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction");
		}

		DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
		if (defStatus.isLocalRollbackOnly()) {
			if (defStatus.isDebug()) {
				logger.debug("Transactional code has requested rollback");
			}
			processRollback(defStatus, false);
			return;
		}

		if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
			if (defStatus.isDebug()) {
				logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");
			}
			processRollback(defStatus, true);
			return;
		}

		processCommit(defStatus);
	}

	/**
	 * 处理实际提交。
	 * rollback-only 标志已经检查并应用。
	 * @param status 表示事务的对象
	 * @throws TransactionException 如果提交失败
	 */
	private void processCommit(DefaultTransactionStatus status) throws TransactionException {
		try {
			boolean beforeCompletionInvoked = false;

			try {
				boolean unexpectedRollback = false;
				prepareForCommit(status);
				triggerBeforeCommit(status);
				triggerBeforeCompletion(status);
				beforeCompletionInvoked = true;

				if (status.hasSavepoint()) {
					if (status.isDebug()) {
						logger.debug("Releasing transaction savepoint");
					}
					unexpectedRollback = status.isGlobalRollbackOnly();
					status.releaseHeldSavepoint();
				}
				else if (status.isNewTransaction()) {
					if (status.isDebug()) {
						logger.debug("Initiating transaction commit");
					}
					unexpectedRollback = status.isGlobalRollbackOnly();
					doCommit(status);
				}
				else if (isFailEarlyOnGlobalRollbackOnly()) {
					unexpectedRollback = status.isGlobalRollbackOnly();
				}

				// Throw UnexpectedRollbackException if we have a global rollback-only
				// marker but still didn't get a corresponding exception from commit.
				if (unexpectedRollback) {
					throw new UnexpectedRollbackException(
							"Transaction silently rolled back because it has been marked as rollback-only");
				}
			}
			catch (UnexpectedRollbackException ex) {
				// can only be caused by doCommit
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
				throw ex;
			}
			catch (TransactionException ex) {
				// can only be caused by doCommit
				if (isRollbackOnCommitFailure()) {
					doRollbackOnCommitException(status, ex);
				}
				else {
					triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
				}
				throw ex;
			}
			catch (RuntimeException | Error ex) {
				if (!beforeCompletionInvoked) {
					triggerBeforeCompletion(status);
				}
				doRollbackOnCommitException(status, ex);
				throw ex;
			}

			// Trigger afterCommit callbacks, with an exception thrown there
			// propagated to callers but the transaction still considered as committed.
			try {
				triggerAfterCommit(status);
			}
			finally {
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
			}

		}
		finally {
			cleanupAfterCompletion(status);
		}
	}

	/**
	 * 此回滚的实现处理参与现有事务。委托给 {@code doRollback} 和
	 * {@code doSetRollbackOnly}。
	 * @see #doRollback
	 * @see #doSetRollbackOnly
	 */
	@Override
	public final void rollback(TransactionStatus status) throws TransactionException {
		if (status.isCompleted()) {
			throw new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction");
		}

		DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
		processRollback(defStatus, false);
	}

	/**
	 * 处理实际回滚。
	 * completed 标志已经检查过。
	 * @param status 表示事务的对象
	 * @throws TransactionException 如果回滚失败
	 */
	private void processRollback(DefaultTransactionStatus status, boolean unexpected) {
		try {
			boolean unexpectedRollback = unexpected;

			try {
				triggerBeforeCompletion(status);

				if (status.hasSavepoint()) {
					if (status.isDebug()) {
						logger.debug("Rolling back transaction to savepoint");
					}
					status.rollbackToHeldSavepoint();
				}
				else if (status.isNewTransaction()) {
					if (status.isDebug()) {
						logger.debug("Initiating transaction rollback");
					}
					doRollback(status);
				}
				else {
					// Participating in larger transaction
					if (status.hasTransaction()) {
						if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
							if (status.isDebug()) {
								logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
							}
							doSetRollbackOnly(status);
						}
						else {
							if (status.isDebug()) {
								logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
							}
						}
					}
					else {
						logger.debug("Should roll back transaction but cannot - no transaction available");
					}
					// Unexpected rollback only matters here if we're asked to fail early
					if (!isFailEarlyOnGlobalRollbackOnly()) {
						unexpectedRollback = false;
					}
				}
			}
			catch (RuntimeException | Error ex) {
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
				throw ex;
			}

			triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);

			// Raise UnexpectedRollbackException if we had a global rollback-only marker
			if (unexpectedRollback) {
				throw new UnexpectedRollbackException(
						"Transaction rolled back because it has been marked as rollback-only");
			}
		}
		finally {
			cleanupAfterCompletion(status);
		}
	}

	/**
	 * 调用 {@code doRollback}，正确处理回滚异常。
	 * @param status 表示事务的对象
	 * @param ex 抛出的应用程序异常或错误
	 * @throws TransactionException 如果回滚失败
	 * @see #doRollback
	 */
	private void doRollbackOnCommitException(DefaultTransactionStatus status, Throwable ex) throws TransactionException {
		try {
			if (status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.debug("Initiating transaction rollback after commit exception", ex);
				}
				doRollback(status);
			}
			else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
				if (status.isDebug()) {
					logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
				}
				doSetRollbackOnly(status);
			}
		}
		catch (RuntimeException | Error rbex) {
			logger.error("Commit exception overridden by rollback exception", ex);
			triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
			throw rbex;
		}
		triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
	}


	/**
	 * 触发 {@code beforeCommit} 回调。
	 * @param status 表示事务的对象
	 */
	protected final void triggerBeforeCommit(DefaultTransactionStatus status) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationUtils.triggerBeforeCommit(status.isReadOnly());
		}
	}

	/**
	 * 触发 {@code beforeCompletion} 回调。
	 * @param status 表示事务的对象
	 */
	protected final void triggerBeforeCompletion(DefaultTransactionStatus status) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationUtils.triggerBeforeCompletion();
		}
	}

	/**
	 * 触发 {@code afterCommit} 回调。
	 * @param status 表示事务的对象
	 */
	private void triggerAfterCommit(DefaultTransactionStatus status) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationUtils.triggerAfterCommit();
		}
	}

	/**
	 * 触发 {@code afterCompletion} 回调。
	 * @param status 表示事务的对象
	 * @param completionStatus 根据 TransactionSynchronization 常量的完成状态
	 */
	private void triggerAfterCompletion(DefaultTransactionStatus status, int completionStatus) {
		if (status.isNewSynchronization()) {
			List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
			TransactionSynchronizationManager.clearSynchronization();
			if (!status.hasTransaction() || status.isNewTransaction()) {
				// No transaction or new transaction for the current scope ->
				// invoke the afterCompletion callbacks immediately
				invokeAfterCompletion(synchronizations, completionStatus);
			}
			else if (!synchronizations.isEmpty()) {
				// Existing transaction that we participate in, controlled outside
				// the scope of this Spring transaction manager -> try to register
				// an afterCompletion callback with the existing (JTA) transaction.
				registerAfterCompletionWithExistingTransaction(status.getTransaction(), synchronizations);
			}
		}
	}

	/**
	 * 实际调用给定 Spring TransactionSynchronization 对象的 {@code afterCompletion} 方法。
	 * <p>由此抽象管理器本身调用，或由 {@code registerAfterCompletionWithExistingTransaction}
	 * 回调的特殊实现调用。
	 * @param synchronizations TransactionSynchronization 对象列表
	 * @param completionStatus 根据 TransactionSynchronization 接口常量的完成状态
	 * @see #registerAfterCompletionWithExistingTransaction(Object, java.util.List)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	protected final void invokeAfterCompletion(List<TransactionSynchronization> synchronizations, int completionStatus) {
		TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
	}

	/**
	 * 完成后清理，必要时清除同步，并调用 doCleanupAfterCompletion。
	 * @param status 表示事务的对象
	 * @see #doCleanupAfterCompletion
	 */
	private void cleanupAfterCompletion(DefaultTransactionStatus status) {
		status.setCompleted();
		if (status.isNewSynchronization()) {
			TransactionSynchronizationManager.clear();
		}
		if (status.isNewTransaction()) {
			doCleanupAfterCompletion(status.getTransaction());
		}
		if (status.getSuspendedResources() != null) {
			if (status.isDebug()) {
				logger.debug("Resuming suspended transaction after completion of inner transaction");
			}
			Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
			resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
		}
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented in subclasses
	//---------------------------------------------------------------------

	/**
	 * 返回当前事务状态的事务对象。
	 * <p>返回的对象通常特定于具体事务管理器实现，以可修改的方式携带相应的事务状态。
	 * 此对象将被传入其他模板方法（例如 doBegin 和 doCommit），可以直接传入或作为
	 * DefaultTransactionStatus 实例的一部分。
	 * <p>返回的对象应包含有关任何现有事务的信息，即在事务管理器上当前
	 * {@code getTransaction} 调用之前已经启动的事务。因此，{@code doGetTransaction}
	 * 实现通常会查找现有事务并将相应状态存储在返回的事务对象中。
	 * @return 当前事务对象
	 * @throws org.springframework.transaction.CannotCreateTransactionException
	 * 如果事务支持不可用
	 * @throws TransactionException 如果查找或系统出错
	 * @see #doBegin
	 * @see #doCommit
	 * @see #doRollback
	 * @see DefaultTransactionStatus#getTransaction
	 */
	protected abstract Object doGetTransaction() throws TransactionException;

	/**
	 * 检查给定事务对象是否表示现有事务（即已启动的事务）。
	 * <p>结果将根据为新事务指定的传播行为进行评估。现有事务可能会被挂起
	 * （在 PROPAGATION_REQUIRES_NEW 的情况下），或者新事务可能会参与现有事务
	 * （在 PROPAGATION_REQUIRED 的情况下）。
	 * <p>默认实现返回 {@code false}，假定通常不支持参与现有事务。
	 * 当然鼓励子类提供这样的支持。
	 * @param transaction doGetTransaction 返回的事务对象
	 * @return 是否存在现有事务
	 * @throws TransactionException 如果系统出错
	 * @see #doGetTransaction
	 */
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		return false;
	}

	/**
	 * 返回是否为嵌套事务使用保存点。
	 * <p>默认为 {@code true}，这会委托给 DefaultTransactionStatus 来创建和持有保存点。
	 * 如果事务对象未实现 SavepointManager 接口，将抛出 NestedTransactionNotSupportedException。
	 * 否则，将要求 SavepointManager 创建一个新的保存点来界定嵌套事务的开始。
	 * <p>子类可以重写此方法以返回 {@code false}，从而在已经存在的事务上下文中进一步调用
	 * {@code doBegin}。{@code doBegin} 实现需要在这种情况下进行相应处理。
	 * 例如，这对于 JTA 是合适的。
	 * @see DefaultTransactionStatus#createAndHoldSavepoint
	 * @see DefaultTransactionStatus#rollbackToHeldSavepoint
	 * @see DefaultTransactionStatus#releaseHeldSavepoint
	 * @see #doBegin
	 */
	protected boolean useSavepointForNestedTransaction() {
		return true;
	}

	/**
	 * 根据给定事务定义的语义开始一个新事务。不需要关心应用传播行为，
	 * 因为这已经由此抽象管理器处理。
	 * <p>当事务管理器决定实际启动新事务时调用此方法。要么之前没有事务，
	 * 要么之前的事务已被挂起。
	 * <p>一个特殊的场景是没有保存点的嵌套事务：如果 {@code useSavepointForNestedTransaction()}
	 * 返回 "false"，则在必要时调用此方法来启动嵌套事务。在这种情况下，将存在一个活动事务：
	 * 此方法的实现必须检测到这一点并启动适当的嵌套事务。
	 * @param transaction {@code doGetTransaction} 返回的事务对象
	 * @param definition TransactionDefinition 实例，描述传播行为、隔离级别、只读标志、超时时间和事务名称
	 * @throws TransactionException 如果创建或系统出错
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
	 * 如果底层事务不支持嵌套
	 */
	protected abstract void doBegin(Object transaction, TransactionDefinition definition)
			throws TransactionException;

	/**
	 * 挂起当前事务的资源。事务同步将已经被挂起。
	 * <p>默认实现抛出 TransactionSuspensionNotSupportedException，
	 * 假定通常不支持事务挂起。
	 * @param transaction {@code doGetTransaction} 返回的事务对象
	 * @return 持有被挂起资源的对象（将不经检查地传递给 doResume）
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * 如果事务管理器实现不支持挂起
	 * @throws TransactionException 如果系统出错
	 * @see #doResume
	 */
	protected Object doSuspend(Object transaction) throws TransactionException {
		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * 恢复当前事务的资源。事务同步将在此之后恢复。
	 * <p>默认实现抛出 TransactionSuspensionNotSupportedException，
	 * 假定通常不支持事务挂起。
	 * @param transaction {@code doGetTransaction} 返回的事务对象
	 * @param suspendedResources 持有被挂起资源的对象，由 doSuspend 返回
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * 如果事务管理器实现不支持恢复
	 * @throws TransactionException 如果系统出错
	 * @see #doSuspend
	 */
	protected void doResume(@Nullable Object transaction, Object suspendedResources) throws TransactionException {
		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * 返回是否应对已全局标记为 rollback-only 的事务调用 {@code doCommit}。
	 * <p>如果应用程序通过 TransactionStatus 在本地将事务设置为 rollback-only，则不适用，
	 * 仅适用于事务本身被事务协调者标记为 rollback-only 的情况。
	 * <p>默认为 "false"：本地事务策略通常不在事务本身中持有 rollback-only 标记，
	 * 因此它们无法在事务提交时处理 rollback-only 事务。因此，AbstractPlatformTransactionManager
	 * 在这种情况下将触发回滚，然后抛出 UnexpectedRollbackException。
	 * <p>如果具体事务管理器期望对 rollback-only 事务也调用 {@code doCommit}，
	 * 则重写此方法返回 "true"，允许在那里进行特殊处理。例如，对于 JTA 将是这种情况，
	 * 其中 {@code UserTransaction.commit} 将自行检查 read-only 标志并抛出相应的
	 * RollbackException，其中可能包含具体原因（如事务超时）。
	 * <p>如果此方法返回 "true" 但 {@code doCommit} 实现未抛出异常，
	 * 则此事务管理器将自行抛出 UnexpectedRollbackException。
	 * 这不应该是典型情况；它主要用于检查那些在调用代码未请求回滚时静默回滚的
	 * 行为不当的 JTA 提供者。
	 * @see #doCommit
	 * @see DefaultTransactionStatus#isGlobalRollbackOnly()
	 * @see DefaultTransactionStatus#isLocalRollbackOnly()
	 * @see org.springframework.transaction.TransactionStatus#setRollbackOnly()
	 * @see org.springframework.transaction.UnexpectedRollbackException
	 * @see javax.transaction.UserTransaction#commit()
	 * @see javax.transaction.RollbackException
	 */
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return false;
	}

	/**
	 * 为提交做准备工作，在 {@code beforeCommit} 同步回调之前执行。
	 * <p>请注意，异常将传播到提交调用者并导致事务回滚。
	 * @param status 事务的状态表示
	 * @throws RuntimeException 如果出错；将<b>传播到调用者</b>
	 * （注意：不要在此抛出 TransactionException 子类！）
	 */
	protected void prepareForCommit(DefaultTransactionStatus status) {
	}

	/**
	 * 执行给定事务的实际提交。
	 * <p>实现不需要检查 "new transaction" 标志或 rollback-only 标志；
	 * 这些在此之前已经处理过了。通常，将对传入状态中包含的事务对象执行直接提交。
	 * @param status 事务的状态表示
	 * @throws TransactionException 如果提交或系统出错
	 * @see DefaultTransactionStatus#getTransaction
	 */
	protected abstract void doCommit(DefaultTransactionStatus status) throws TransactionException;

	/**
	 * 执行给定事务的实际回滚。
	 * <p>实现不需要检查 "new transaction" 标志；在此之前已经处理过了。
	 * 通常，将对传入状态中包含的事务对象执行直接回滚。
	 * @param status 事务的状态表示
	 * @throws TransactionException 如果系统出错
	 * @see DefaultTransactionStatus#getTransaction
	 */
	protected abstract void doRollback(DefaultTransactionStatus status) throws TransactionException;

	/**
	 * 将给定事务设置为 rollback-only。仅在回滚时且当前事务参与现有事务时调用。
	 * <p>默认实现抛出 IllegalTransactionStateException，
	 * 假定通常不支持参与现有事务。当然鼓励子类提供这样的支持。
	 * @param status 事务的状态表示
	 * @throws TransactionException 如果系统出错
	 */
	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
		throw new IllegalTransactionStateException(
				"Participating in existing transactions is not supported - when 'isExistingTransaction' " +
				"returns true, appropriate 'doSetRollbackOnly' behavior must be provided");
	}

	/**
	 * 将给定的事务同步列表注册到现有事务中。
	 * <p>当 Spring 事务管理器的控制以及所有 Spring 事务同步结束时，
	 * 但事务尚未完成时调用。例如，参与现有 JTA 或 EJB CMT 事务时就是这种情况。
	 * <p>默认实现直接调用 {@code afterCompletion} 方法，传入 "STATUS_UNKNOWN"。
	 * 如果无法确定外部事务的实际结果，这是我们能做的最好的处理。
	 * @param transaction {@code doGetTransaction} 返回的事务对象
	 * @param synchronizations TransactionSynchronization 对象列表
	 * @throws TransactionException 如果系统出错
	 * @see #invokeAfterCompletion(java.util.List, int)
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	protected void registerAfterCompletionWithExistingTransaction(
			Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {

		logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " +
				"processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
		invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
	}

	/**
	 * 事务完成后清理资源。
	 * <p>在 {@code doCommit} 和 {@code doRollback} 执行后调用，无论结果如何。
	 * 默认实现不做任何操作。
	 * <p>不应抛出任何异常，只应在出错时发出警告。
	 * @param transaction {@code doGetTransaction} 返回的事务对象
	 */
	protected void doCleanupAfterCompletion(Object transaction) {
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
	 * 被挂起资源的持有者。
	 * 由 {@code suspend} 和 {@code resume} 内部使用。
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

		private SuspendedResourcesHolder(Object suspendedResources) {
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

}
