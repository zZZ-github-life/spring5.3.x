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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.NamedThreadLocal;
import org.springframework.core.OrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 管理每线程资源和事务同步的中心代理。供资源管理代码使用，而非典型应用程序代码。
 *
 * <p>支持每个键一个资源而不覆盖，即在为同一键设置新资源之前需要先移除旧资源。
 * 在同步激活时支持事务同步列表。
 *
 * <p>资源管理代码应通过 {@code getResource} 检查线程绑定的资源，例如 JDBC Connection
 * 或 Hibernate Session。此类代码通常不应将资源绑定到线程，因为这是事务管理器的职责。
 * 另一个选项是在事务同步激活时延迟绑定（首次使用时绑定），以执行跨越任意数量资源的事务。
 *
 * <p>事务同步必须由事务管理器通过 {@link #initSynchronization()} 和
 * {@link #clearSynchronization()} 来激活和停用。{@link AbstractPlatformTransactionManager}
 * 以及所有标准 Spring 事务管理器（如
 * {@link org.springframework.transaction.jta.JtaTransactionManager} 和
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}）自动支持此功能。
 *
 * <p>资源管理代码应仅在此管理器处于活动状态时注册同步，可通过 {@link #isSynchronizationActive}
 * 检查；否则应执行立即资源清理。如果事务同步未激活，则要么没有当前事务，
 * 要么事务管理器不支持事务同步。
 *
 * <p>同步用于例如在 JTA 事务中始终为任何给定的 DataSource 或 SessionFactory
 * 返回相同的资源，分别为 JDBC Connection 或 Hibernate Session。
 *
 * @author Juergen Hoeller
 * @since 02.06.2003
 * @see #isSynchronizationActive
 * @see #registerSynchronization
 * @see TransactionSynchronization
 * @see AbstractPlatformTransactionManager#setTransactionSynchronization
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
 */
public abstract class TransactionSynchronizationManager {

	private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<>("Transactional resources");

	private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");

	private static final ThreadLocal<String> currentTransactionName =
			new NamedThreadLocal<>("Current transaction name");

	private static final ThreadLocal<Boolean> currentTransactionReadOnly =
			new NamedThreadLocal<>("Current transaction read-only status");

	private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
			new NamedThreadLocal<>("Current transaction isolation level");

	private static final ThreadLocal<Boolean> actualTransactionActive =
			new NamedThreadLocal<>("Actual transaction active");


	//-------------------------------------------------------------------------
	// Management of transaction-associated resource handles
	//-------------------------------------------------------------------------

	/**
	 * 返回绑定到当前线程的所有资源。
	 * <p>主要用于调试目的。资源管理器应始终针对它们感兴趣的特定资源键调用 {@code hasResource}。
	 * @return 包含资源键（通常是资源工厂）和资源值（通常是活动资源对象）的 Map，
	 * 如果当前没有绑定资源则返回空 Map
	 * @see #hasResource
	 */
	public static Map<Object, Object> getResourceMap() {
		Map<Object, Object> map = resources.get();
		return (map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap());
	}

	/**
	 * 检查当前线程是否绑定了给定键的资源。
	 * @param key 要检查的键（通常是资源工厂）
	 * @return 如果当前线程绑定了值
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	public static boolean hasResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doGetResource(actualKey);
		return (value != null);
	}

	/**
	 * 检索绑定到当前线程的给定键的资源。
	 * @param key 要检查的键（通常是资源工厂）
	 * @return 绑定到当前线程的值（通常是活动资源对象），如果没有则为 {@code null}
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	@Nullable
	public static Object getResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doGetResource(actualKey);
	}

	/**
	 * 实际检查为给定键绑定的资源值。
	 */
	@Nullable
	private static Object doGetResource(Object actualKey) {
		Map<Object, Object> map = resources.get();
		if (map == null) {
			return null;
		}
		Object value = map.get(actualKey);
		// Transparently remove ResourceHolder that was marked as void...
		if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
			map.remove(actualKey);
			// Remove entire ThreadLocal if empty...
			if (map.isEmpty()) {
				resources.remove();
			}
			value = null;
		}
		return value;
	}

	/**
	 * 将给定资源绑定到给定键 的当前线程。
	 * @param key 绑定值的键（通常是资源工厂）
	 * @param value 要绑定的值（通常是活动资源对象）
	 * @throws IllegalStateException 如果线程已绑定值
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	public static void bindResource(Object key, Object value) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Assert.notNull(value, "Value must not be null");
		Map<Object, Object> map = resources.get();
		// set ThreadLocal Map if none found
		if (map == null) {
			map = new HashMap<>();
			resources.set(map);
		}
		Object oldValue = map.put(actualKey, value);
		// Transparently suppress a ResourceHolder that was marked as void...
		if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
			oldValue = null;
		}
		if (oldValue != null) {
			throw new IllegalStateException(
					"Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread");
		}
	}

	/**
	 * 从当前线程解绑给定键的资源。
	 * @param key 要解绑的键（通常是资源工厂）
	 * @return 之前绑定的值（通常是活动资源对象）
	 * @throws IllegalStateException 如果当前线程没有绑定值
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	public static Object unbindResource(Object key) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doUnbindResource(actualKey);
		if (value == null) {
			throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread");
		}
		return value;
	}

	/**
	 * 从当前线程解绑给定键的资源。
	 * @param key 要解绑的键（通常是资源工厂）
	 * @return 之前绑定的值，如果没有绑定则为 {@code null}
	 */
	@Nullable
	public static Object unbindResourceIfPossible(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doUnbindResource(actualKey);
	}

	/**
	 * 实际移除为给定键绑定的资源值。
	 */
	@Nullable
	private static Object doUnbindResource(Object actualKey) {
		Map<Object, Object> map = resources.get();
		if (map == null) {
			return null;
		}
		Object value = map.remove(actualKey);
		// Remove entire ThreadLocal if empty...
		if (map.isEmpty()) {
			resources.remove();
		}
		// Transparently suppress a ResourceHolder that was marked as void...
		if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
			value = null;
		}
		return value;
	}


	//-------------------------------------------------------------------------
	// Management of transaction synchronizations
	//-------------------------------------------------------------------------

	/**
	 * 返回当前线程的事务同步是否处于活动状态。
	 * 可在注册前调用以避免不必要的实例创建。
	 * @see #registerSynchronization
	 */
	public static boolean isSynchronizationActive() {
		return (synchronizations.get() != null);
	}

	/**
	 * 为当前线程激活事务同步。由事务管理器在事务开始时调用。
	 * @throws IllegalStateException 如果同步已处于活动状态
	 */
	public static void initSynchronization() throws IllegalStateException {
		if (isSynchronizationActive()) {
			throw new IllegalStateException("Cannot activate transaction synchronization - already active");
		}
		synchronizations.set(new LinkedHashSet<>());
	}

	/**
	 * 为当前线程注册新的事务同步。通常由资源管理代码调用。
	 * <p>请注意，同步可以实现 {@link org.springframework.core.Ordered} 接口。
	 * 它们将根据其 order 值（如果有）按顺序执行。
	 * @param synchronization 要注册的同步对象
	 * @throws IllegalStateException 如果事务同步未激活
	 * @see org.springframework.core.Ordered
	 */
	public static void registerSynchronization(TransactionSynchronization synchronization)
			throws IllegalStateException {

		Assert.notNull(synchronization, "TransactionSynchronization must not be null");
		Set<TransactionSynchronization> synchs = synchronizations.get();
		if (synchs == null) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		synchs.add(synchronization);
	}

	/**
	 * 返回当前线程所有已注册同步的不可变快照列表。
	 * @return TransactionSynchronization 实例的不可变 List
	 * @throws IllegalStateException 如果同步未激活
	 * @see TransactionSynchronization
	 */
	public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
		Set<TransactionSynchronization> synchs = synchronizations.get();
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
			OrderComparator.sort(sortedSynchs);
			return Collections.unmodifiableList(sortedSynchs);
		}
	}

	/**
	 * 停用当前线程的事务同步。由事务管理器在事务清理时调用。
	 * @throws IllegalStateException 如果同步未激活
	 */
	public static void clearSynchronization() throws IllegalStateException {
		if (!isSynchronizationActive()) {
			throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
		}
		synchronizations.remove();
	}


	//-------------------------------------------------------------------------
	// Exposure of transaction characteristics
	//-------------------------------------------------------------------------

	/**
	 * 暴露当前事务的名称（如果有）。由事务管理器在事务开始和清理时调用。
	 * @param name 事务名称，或 {@code null} 以重置
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	public static void setCurrentTransactionName(@Nullable String name) {
		currentTransactionName.set(name);
	}

	/**
	 * 返回当前事务的名称，如果未设置则为 {@code null}。
	 * 供资源管理代码按用例进行优化调用，例如针对特定命名事务优化获取策略。
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	@Nullable
	public static String getCurrentTransactionName() {
		return currentTransactionName.get();
	}

	/**
	 * 为当前事务暴露只读标志。由事务管理器在事务开始和清理时调用。
	 * @param readOnly {@code true} 标记当前事务为只读；{@code false} 重置此类只读标记
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 */
	public static void setCurrentTransactionReadOnly(boolean readOnly) {
		currentTransactionReadOnly.set(readOnly ? Boolean.TRUE : null);
	}

	/**
	 * 返回当前事务是否被标记为只读。供资源管理代码在准备新创建的资源
	 * （例如 Hibernate Session）时调用。
	 * <p>请注意，事务同步通过 {@code beforeCommit} 回调的参数接收只读标志，
	 * 以便能够在提交时抑制变更检测。本方法用于更早的只读检查，
	 * 例如提前将 Hibernate Session 的刷新模式设置为 "FlushMode.MANUAL"。
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	public static boolean isCurrentTransactionReadOnly() {
		return (currentTransactionReadOnly.get() != null);
	}

	/**
	 * 为当前事务暴露隔离级别。由事务管理器在事务开始和清理时调用。
	 * @param isolationLevel 要暴露的隔离级别，根据 JDBC Connection 常量
	 * （等同于相应的 Spring TransactionDefinition 常量），或 {@code null} 以重置
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	public static void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
		currentTransactionIsolationLevel.set(isolationLevel);
	}

	/**
	 * 返回当前事务的隔离级别（如果有）。供资源管理代码在准备新创建的资源
	 * （例如 JDBC Connection）时调用。
	 * @return 当前暴露的隔离级别，根据 JDBC Connection 常量
	 * （等同于相应的 Spring TransactionDefinition 常量），如果没有则为 {@code null}
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	@Nullable
	public static Integer getCurrentTransactionIsolationLevel() {
		return currentTransactionIsolationLevel.get();
	}

	/**
	 * 暴露当前是否有一个实际事务处于活动状态。由事务管理器在事务开始和清理时调用。
	 * @param active {@code true} 标记当前线程与一个实际事务关联；{@code false} 重置该标记
	 */
	public static void setActualTransactionActive(boolean active) {
		actualTransactionActive.set(active ? Boolean.TRUE : null);
	}

	/**
	 * 返回当前是否有一个实际事务处于活动状态。
	 * 这指示当前线程是与一个实际事务关联，还是仅与活动的事务同步关联。
	 * <p>供需要区分活动事务同步（有或没有底层资源事务；也在 PROPAGATION_SUPPORTS 上）
	 * 和实际事务活动（有底层资源事务；在 PROPAGATION_REQUIRED、PROPAGATION_REQUIRES_NEW 等上）
	 * 的资源管理代码调用。
	 * @see #isSynchronizationActive()
	 */
	public static boolean isActualTransactionActive() {
		return (actualTransactionActive.get() != null);
	}


	/**
	 * 清除当前线程的整个事务同步状态：已注册的同步以及各种事务特征。
	 * @see #clearSynchronization()
	 * @see #setCurrentTransactionName
	 * @see #setCurrentTransactionReadOnly
	 * @see #setCurrentTransactionIsolationLevel
	 * @see #setActualTransactionActive
	 */
	public static void clear() {
		synchronizations.remove();
		currentTransactionName.remove();
		currentTransactionReadOnly.remove();
		currentTransactionIsolationLevel.remove();
		actualTransactionActive.remove();
	}

}
