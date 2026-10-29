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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.transaction.NoTransactionException;
import org.springframework.util.Assert;

/**
 * 中心委托，按订阅者上下文管理资源和事务同步。
 * 供资源管理代码使用，而非典型的应用程序代码。
 *
 * <p>支持每个键一个资源而不覆盖，即需要先移除资源
 * 才能为同一个键设置新的资源。
 * 如果同步处于活跃状态，则支持事务同步列表。
 *
 * <p>资源管理代码应通过{@code getResource}检查绑定到上下文的资源，
 * 例如数据库连接。此类代码通常不应将资源绑定到工作单元，
 * 因为这是事务管理器的职责。另一个选择是，如果事务同步处于
 * 活跃状态，则在首次使用时延迟绑定，以执行跨越任意数量资源的
 * 事务。
 *
 * <p>事务同步必须由事务管理器通过{@link #initSynchronization()}
 * 和{@link #clearSynchronization()}来激活和停用。
 * {@link AbstractReactiveTransactionManager}以及所有标准Spring事务管理器
 * 自动支持此功能。
 *
 * <p>资源管理代码应仅在此管理器处于活跃状态时注册同步，
 * 可以通过{@link #isSynchronizationActive}进行检查；
 * 否则应执行立即的资源清理。如果事务同步不活跃，
 * 则要么没有当前事务，要么事务管理器不支持事务同步。
 *
 * <p>同步例如用于在事务中始终返回相同的资源，
 * 例如对于任何给定的连接工厂返回相同的数据库连接。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see #isSynchronizationActive
 * @see #registerSynchronization
 * @see TransactionSynchronization
 */
public class TransactionSynchronizationManager {

	private final TransactionContext transactionContext;


	public TransactionSynchronizationManager(TransactionContext transactionContext) {
		this.transactionContext = transactionContext;
	}


	/**
	 * 获取与当前事务上下文关联的
	 * {@link TransactionSynchronizationManager}。
	 * <p>主要用于想要绑定资源或同步的代码。
	 * @throws NoTransactionException 如果无法找到事务信息 &mdash;
	 * 例如，因为方法在托管事务之外被调用
	 */
	public static Mono<TransactionSynchronizationManager> forCurrentTransaction() {
		return TransactionContextManager.currentContext().map(TransactionSynchronizationManager::new);
	}

	/**
	 * 检查给定键是否有资源绑定到当前线程。
	 * @param key the key to check (usually the resource factory)
	 * @return if there is a value bound to the current thread
	 */
	public boolean hasResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doGetResource(actualKey);
		return (value != null);
	}

	/**
	 * 检索绑定到当前线程的给定键的资源。
	 * @param key the key to check (usually the resource factory)
	 * @return a value bound to the current thread (usually the active
	 * resource object), or {@code null} if none
	 */
	@Nullable
	public Object getResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doGetResource(actualKey);
	}

	/**
	 * 实际检查为给定键绑定的资源值。
	 */
	@Nullable
	private Object doGetResource(Object actualKey) {
		return this.transactionContext.getResources().get(actualKey);
	}

	/**
	 * 将给定资源绑定到给定键的当前上下文。
	 * @param key the key to bind the value to (usually the resource factory)
	 * @param value the value to bind (usually the active resource object)
	 * @throws IllegalStateException if there is already a value bound to the context
	 */
	public void bindResource(Object key, Object value) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Assert.notNull(value, "Value must not be null");
		Map<Object, Object> map = this.transactionContext.getResources();
		Object oldValue = map.put(actualKey, value);
		if (oldValue != null) {
			throw new IllegalStateException(
					"Already value [" + oldValue + "] for key [" + actualKey + "] bound to context");
		}
	}

	/**
	 * 从当前上下文中解绑给定键的资源。
	 * @param key 要解绑的键（通常是资源工厂）
	 * @return 之前绑定的值（通常是活跃的资源对象）
	 * @throws IllegalStateException 如果上下文中没有绑定值
	 */
	public Object unbindResource(Object key) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doUnbindResource(actualKey);
		if (value == null) {
			throw new IllegalStateException("No value for key [" + actualKey + "] bound to context");
		}
		return value;
	}

	/**
	 * 从当前上下文中解绑给定键的资源。
	 * @param key 要解绑的键（通常是资源工厂）
	 * @return 之前绑定的值，如果没有绑定则返回{@code null}
	 */
	@Nullable
	public Object unbindResourceIfPossible(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doUnbindResource(actualKey);
	}

	/**
	 * 实际移除为给定键绑定的资源值。
	 */
	@Nullable
	private Object doUnbindResource(Object actualKey) {
		Map<Object, Object> map = this.transactionContext.getResources();
		return map.remove(actualKey);
	}


	//-------------------------------------------------------------------------
	// Management of transaction synchronizations
	//-------------------------------------------------------------------------

	/**
	 * 返回当前上下文的事务同步是否处于活跃状态。
	 * 可以在注册之前调用，以避免不必要的实例创建。
	 * @see #registerSynchronization
	 */
	public boolean isSynchronizationActive() {
		return (this.transactionContext.getSynchronizations() != null);
	}

	/**
	 * 为当前上下文激活事务同步。
	 * 由事务管理器在事务开始时调用。
	 * @throws IllegalStateException if synchronization is already active
	 */
	public void initSynchronization() throws IllegalStateException {
		if (isSynchronizationActive()) {
			throw new IllegalStateException("Cannot activate transaction synchronization - already active");
		}
		this.transactionContext.setSynchronizations(new LinkedHashSet<>());
	}

	/**
	 * 为当前上下文注册一个新的事务同步。
	 * 通常由资源管理代码调用。
	 * <p>请注意，同步可以实现
	 * {@link org.springframework.core.Ordered}接口。
	 * 它们将根据其顺序值（如果有）按顺序执行。
	 * @param synchronization the synchronization object to register
	 * @throws IllegalStateException if transaction synchronization is not active
	 * @see org.springframework.core.Ordered
	 */
	public void registerSynchronization(TransactionSynchronization synchronization)
			throws IllegalStateException {

		Assert.notNull(synchronization, "TransactionSynchronization must not be null");
		Set<TransactionSynchronization> synchs = this.transactionContext.getSynchronizations();
		if (synchs == null) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		synchs.add(synchronization);
	}

	/**
	 * 返回当前上下文所有已注册同步的不可修改快照列表。
	 * @return unmodifiable List of TransactionSynchronization instances
	 * @throws IllegalStateException if synchronization is not active
	 * @see TransactionSynchronization
	 */
	public List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
		Set<TransactionSynchronization> synchs = this.transactionContext.getSynchronizations();
		if (synchs == null) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		// Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
		// while iterating and invoking synchronization callbacks that in turn
		// might register further synchronizations.
		if (synchs.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			// Sort lazily here, not in registerSynchronization.
			List<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
			AnnotationAwareOrderComparator.sort(sortedSynchs);
			return Collections.unmodifiableList(sortedSynchs);
		}
	}

	/**
	 * 为当前上下文停用事务同步。
	 * 由事务管理器在事务清理时调用。
	 * @throws IllegalStateException if synchronization is not active
	 */
	public void clearSynchronization() throws IllegalStateException {
		if (!isSynchronizationActive()) {
			throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
		}
		this.transactionContext.setSynchronizations(null);
	}


	//-------------------------------------------------------------------------
	// Exposure of transaction characteristics
	//-------------------------------------------------------------------------

	/**
	 * 暴露当前事务的名称（如果有）。
	 * 由事务管理器在事务开始和清理时调用。
	 * @param name the name of the transaction, or {@code null} to reset it
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	public void setCurrentTransactionName(@Nullable String name) {
		this.transactionContext.setCurrentTransactionName(name);
	}

	/**
	 * 返回当前事务的名称，如果未设置则返回{@code null}。
	 * 供资源管理代码按用例进行优化调用，
	 * 例如为特定的命名事务优化获取策略。
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	@Nullable
	public String getCurrentTransactionName() {
		return this.transactionContext.getCurrentTransactionName();
	}

	/**
	 * 为当前事务暴露只读标志。
	 * 由事务管理器在事务开始和清理时调用。
	 * @param readOnly {@code true} to mark the current transaction
	 * as read-only; {@code false} to reset such a read-only marker
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 */
	public void setCurrentTransactionReadOnly(boolean readOnly) {
		this.transactionContext.setCurrentTransactionReadOnly(readOnly);
	}

	/**
	 * 返回当前事务是否被标记为只读。
	 * 供资源管理代码在准备新创建的资源时调用。
	 * <p>请注意，事务同步会接收只读标志作为
	 * {@code beforeCommit}回调的参数，以便在提交时
	 * 抑制变更检测。此方法用于更早的只读检查。
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	public boolean isCurrentTransactionReadOnly() {
		return this.transactionContext.isCurrentTransactionReadOnly();
	}

	/**
	 * 为当前事务暴露隔离级别。
	 * 由事务管理器在事务开始和清理时调用。
	 * @param isolationLevel the isolation level to expose, according to the
	 * R2DBC Connection constants (equivalent to the corresponding Spring
	 * TransactionDefinition constants), or {@code null} to reset it
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	public void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
		this.transactionContext.setCurrentTransactionIsolationLevel(isolationLevel);
	}

	/**
	 * 返回当前事务的隔离级别（如果有）。
	 * 供资源管理代码在准备新创建的资源时调用
	 * （例如R2DBC Connection）。
	 * @return the currently exposed isolation level, according to the
	 * R2DBC Connection constants (equivalent to the corresponding Spring
	 * TransactionDefinition constants), or {@code null} if none
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	@Nullable
	public Integer getCurrentTransactionIsolationLevel() {
		return this.transactionContext.getCurrentTransactionIsolationLevel();
	}

	/**
	 * 暴露当前是否有实际事务处于活跃状态。
	 * 由事务管理器在事务开始和清理时调用。
	 * @param active {@code true} to mark the current context as being associated
	 * with an actual transaction; {@code false} to reset that marker
	 */
	public void setActualTransactionActive(boolean active) {
		this.transactionContext.setActualTransactionActive(active);
	}

	/**
	 * 返回当前是否有实际事务处于活跃状态。
	 * 这表明当前上下文是与实际事务关联，
	 * 而不仅仅是活跃的事务同步。
	 * <p>供想要区分活跃事务同步（有无底层资源事务；
	 * 也包括PROPAGATION_SUPPORTS）和实际事务处于活跃状态
	 * （有底层资源事务；在PROPAGATION_REQUIRED、
	 * PROPAGATION_REQUIRES_NEW等情况下）的资源管理代码调用。
	 * @see #isSynchronizationActive()
	 */
	public boolean isActualTransactionActive() {
		return this.transactionContext.isActualTransactionActive();
	}

	/**
	 * 清除整个事务同步状态：
	 * 已注册的同步以及各种事务特性。
	 * @see #clearSynchronization()
	 * @see #setCurrentTransactionName
	 * @see #setCurrentTransactionReadOnly
	 * @see #setCurrentTransactionIsolationLevel
	 * @see #setActualTransactionActive
	 */
	public void clear() {
		this.transactionContext.clear();
	}

}
