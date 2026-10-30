/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction;

import org.springframework.lang.Nullable;

/**
 * 定义符合 Spring 规范的事务属性的接口。
 * 基于类似于 EJB CMT 属性的传播行为定义。
 *
 * <p>请注意，隔离级别和超时设置不会自动生效，
 * 除非实际启动了一个新事务。因为只有 {@link #PROPAGATION_REQUIRED}、
 * {@link #PROPAGATION_REQUIRES_NEW} 和 {@link #PROPAGATION_NESTED} 才会触发这种情况，
 * 所以在其他情况下指定这些设置通常没有意义。
 * 此外，请注意并非所有事务管理器都支持这些高级特性，
 * 因此在给定非默认值时可能会抛出相应的异常。
 *
 * <p>{@link #isReadOnly() 只读标志} 适用于任何事务上下文，
 * 无论是由实际资源事务支持还是在资源级别以非事务方式运行。
 * 在后一种情况下，该标志仅适用于应用程序内的托管资源，
 * 例如 Hibernate {@code Session}。
 *
 * @author Juergen Hoeller
 * @since 08.05.2003
 * @see PlatformTransactionManager#getTransaction(TransactionDefinition)
 * @see org.springframework.transaction.support.DefaultTransactionDefinition
 * @see org.springframework.transaction.interceptor.TransactionAttribute
 */
public interface TransactionDefinition {

	/**
	 * 支持当前事务；如果不存在则创建一个新事务。
	 * 类似于同名的 EJB 事务属性。
	 * <p>这通常是事务定义的默认设置，
	 * 通常定义了一个事务同步范围。
	 */
	int PROPAGATION_REQUIRED = 0;

	/**
	 * 支持当前事务；如果不存在则以非事务方式执行。
	 * 类似于同名的 EJB 事务属性。
	 * <p><b>注意：</b>对于具有事务同步的事务管理器，
	 * {@code PROPAGATION_SUPPORTS} 与完全没有事务略有不同，
	 * 因为它定义了一个同步可能适用的事务范围。
	 * 因此，相同的资源（JDBC {@code Connection}、
	 * Hibernate {@code Session} 等）将在整个指定范围内共享。
	 * 请注意，确切的行为取决于事务管理器的实际同步配置！
	 * <p>通常，请谨慎使用 {@code PROPAGATION_SUPPORTS}！
	 * 特别是，不要在 {@code PROPAGATION_SUPPORTS} 范围<i>内</i>
	 * 依赖 {@code PROPAGATION_REQUIRED} 或 {@code PROPAGATION_REQUIRES_NEW}
	 * （这可能导致运行时的同步冲突）。
	 * 如果这种嵌套不可避免，请确保适当地配置你的事务管理器
	 * （通常切换到"实际事务同步"）。
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
	 */
	int PROPAGATION_SUPPORTS = 1;

	/**
	 * 支持当前事务；如果不存在则抛出异常。
	 * 类似于同名的 EJB 事务属性。
	 * <p>请注意，{@code PROPAGATION_MANDATORY} 范围内的事务同步
	 * 始终由周围的事务驱动。
	 */
	int PROPAGATION_MANDATORY = 2;

	/**
	 * 创建一个新事务，如果存在当前事务则挂起当前事务。
	 * 类似于同名的 EJB 事务属性。
	 * <p><b>注意：</b>实际的事务挂起并非在所有事务管理器上都能开箱即用。
	 * 这尤其适用于 {@link org.springframework.transaction.jta.JtaTransactionManager}，
	 * 它要求 {@code javax.transaction.TransactionManager} 对其可用
	 * （在标准 Java EE 中这是特定于服务器的）。
	 * <p>{@code PROPAGATION_REQUIRES_NEW} 范围始终定义自己的事务同步。
	 * 现有的同步将被挂起并适当地恢复。
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	int PROPAGATION_REQUIRES_NEW = 3;

	/**
	 * 不支持当前事务；而是始终以非事务方式执行。
	 * 类似于同名的 EJB 事务属性。
	 * <p><b>注意：</b>实际的事务挂起并非在所有事务管理器上都能开箱即用。
	 * 这尤其适用于 {@link org.springframework.transaction.jta.JtaTransactionManager}，
	 * 它要求 {@code javax.transaction.TransactionManager} 对其可用
	 * （在标准 Java EE 中这是特定于服务器的）。
	 * <p>请注意，事务同步在 {@code PROPAGATION_NOT_SUPPORTED} 范围内<i>不可用</i>。
	 * 现有的同步将被挂起并适当地恢复。
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	int PROPAGATION_NOT_SUPPORTED = 4;

	/**
	 * 不支持当前事务；如果存在当前事务则抛出异常。
	 * 类似于同名的 EJB 事务属性。
	 * <p>请注意，事务同步在 {@code PROPAGATION_NEVER} 范围内<i>不可用</i>。
	 */
	int PROPAGATION_NEVER = 5;

	/**
	 * 如果存在当前事务则在嵌套事务中执行，
	 * 否则表现得像 {@link #PROPAGATION_REQUIRED}。
	 * EJB 中没有类似的特性。
	 * <p><b>注意：</b>实际创建嵌套事务仅在特定的事务管理器上有效。
	 * 开箱即用的情况下，这仅适用于在 JDBC 3.0 驱动上工作的
	 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}。
	 * 某些 JTA 提供程序可能也支持嵌套事务。
	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
	 */
	int PROPAGATION_NESTED = 6;


	/**
	 * 使用底层数据存储的默认隔离级别。
	 * 所有其他级别对应于 JDBC 隔离级别。
	 * @see java.sql.Connection
	 */
	int ISOLATION_DEFAULT = -1;

	/**
	 * 表示可能发生脏读、不可重复读和幻读。
	 * <p>此级别允许一个事务读取另一个事务尚未提交的行更改（即"脏读"）。
	 * 如果任何更改被回滚，第二个事务将检索到无效的行。
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 */
	int ISOLATION_READ_UNCOMMITTED = 1;  // same as java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;

	/**
	 * 表示防止脏读；但可能发生不可重复读和幻读。
	 * <p>此级别仅禁止事务读取包含未提交更改的行。
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 */
	int ISOLATION_READ_COMMITTED = 2;  // same as java.sql.Connection.TRANSACTION_READ_COMMITTED;

	/**
	 * 表示防止脏读和不可重复读；但可能发生幻读。
	 * <p>此级别禁止事务读取包含未提交更改的行，
	 * 同时禁止以下情况：一个事务读取一行数据，
	 * 第二个事务修改该行，第一个事务重新读取该行，
	 * 第二次获取到不同的值（即"不可重复读"）。
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 */
	int ISOLATION_REPEATABLE_READ = 4;  // same as java.sql.Connection.TRANSACTION_REPEATABLE_READ;

	/**
	 * 表示防止脏读、不可重复读和幻读。
	 * <p>此级别包含 {@link #ISOLATION_REPEATABLE_READ} 中的禁止项，
	 * 并进一步禁止以下情况：一个事务读取所有满足 {@code WHERE} 条件的行，
	 * 第二个事务插入一行满足该 {@code WHERE} 条件的行，
	 * 第一个事务使用相同条件重新读取，
	 * 在第二次读取中检索到额外的"幻"行。
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 */
	int ISOLATION_SERIALIZABLE = 8;  // same as java.sql.Connection.TRANSACTION_SERIALIZABLE;


	/**
	 * 使用底层事务系统的默认超时时间，
	 * 如果不支持超时则不使用超时。
	 */
	int TIMEOUT_DEFAULT = -1;


	/**
	 * 返回传播行为。
	 * <p>必须返回定义在 {@link TransactionDefinition 此接口}上的
	 * {@code PROPAGATION_XXX} 常量之一。
	 * <p>默认为 {@link #PROPAGATION_REQUIRED}。
	 * @return 传播行为
	 * @see #PROPAGATION_REQUIRED
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
	 */
	default int getPropagationBehavior() {
		return PROPAGATION_REQUIRED;
	}

	/**
	 * 返回隔离级别。
	 * <p>必须返回定义在 {@link TransactionDefinition 此接口}上的
	 * {@code ISOLATION_XXX} 常量之一。这些常量的设计
	 * 与 {@link java.sql.Connection} 上同名常量的值相匹配。
	 * <p>仅设计用于 {@link #PROPAGATION_REQUIRED} 或
	 * {@link #PROPAGATION_REQUIRES_NEW}，因为它仅适用于新启动的事务。
	 * 如果你希望在参与具有不同隔离级别的现有事务时
	 * 拒绝隔离级别声明，请考虑将事务管理器上的
	 * "validateExistingTransactions"标志切换为 "true"。
	 * <p>默认为 {@link #ISOLATION_DEFAULT}。请注意，
	 * 不支持自定义隔离级别的事务管理器在给定
	 * {@link #ISOLATION_DEFAULT} 以外的任何级别时将抛出异常。
	 * @return 隔离级别
	 * @see #ISOLATION_DEFAULT
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
	 */
	default int getIsolationLevel() {
		return ISOLATION_DEFAULT;
	}

	/**
	 * 返回事务超时时间。
	 * <p>必须返回秒数，或 {@link #TIMEOUT_DEFAULT}。
	 * <p>仅设计用于 {@link #PROPAGATION_REQUIRED} 或
	 * {@link #PROPAGATION_REQUIRES_NEW}，因为它仅适用于新启动的事务。
	 * <p>请注意，不支持超时的事务管理器在给定
	 * {@link #TIMEOUT_DEFAULT} 以外的任何超时时将抛出异常。
	 * <p>默认为 {@link #TIMEOUT_DEFAULT}。
	 * @return 事务超时时间
	 */
	default int getTimeout() {
		return TIMEOUT_DEFAULT;
	}

	/**
	 * 返回是否优化为只读事务。
	 * <p>只读标志适用于任何事务上下文，无论是由实际资源事务支持
	 * （{@link #PROPAGATION_REQUIRED}/
	 * {@link #PROPAGATION_REQUIRES_NEW}）还是在资源级别以非事务方式运行
	 * （{@link #PROPAGATION_SUPPORTS}）。在后一种情况下，
	 * 该标志仅适用于应用程序内的托管资源，例如 Hibernate {@code Session}。
	 * <p>这仅作为实际事务子系统的提示；
	 * 它<i>不一定</i>会导致写入访问尝试失败。
	 * 无法解读只读提示的事务管理器在被要求只读事务时<i>不会</i>抛出异常。
	 * @return 如果事务要优化为只读，则为 {@code true}
	 * （默认为 {@code false}）
	 * @see org.springframework.transaction.support.TransactionSynchronization#beforeCommit(boolean)
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
	 */
	default boolean isReadOnly() {
		return false;
	}

	/**
	 * 返回此事务的名称。可以为 {@code null}。
	 * <p>如果适用（例如 WebLogic 的事务监视器），
	 * 此名称将用作事务监视器中显示的事务名称。
	 * <p>对于 Spring 的声明式事务，公开的名称默认为
	 * {@code 全限定类名 + "." + 方法名}。
	 * @return 此事务的名称（默认为 {@code null}）
	 * @see org.springframework.transaction.interceptor.TransactionAspectSupport
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionName()
	 */
	@Nullable
	default String getName() {
		return null;
	}


	// Static builder methods

	/**
	 * 返回一个使用默认值的不可修改的 {@code TransactionDefinition}。
	 * <p>出于自定义目的，请改用可修改的
	 * {@link org.springframework.transaction.support.DefaultTransactionDefinition}。
	 * @since 5.2
	 */
	static TransactionDefinition withDefaults() {
		return StaticTransactionDefinition.INSTANCE;
	}

}
