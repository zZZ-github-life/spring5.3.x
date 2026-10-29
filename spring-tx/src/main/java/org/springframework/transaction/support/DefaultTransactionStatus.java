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

package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.transaction.TransactionStatus} 接口的默认实现，
 * 由 {@link AbstractPlatformTransactionManager} 使用。基于底层"事务对象"的概念。
 *
 * <p>持有 {@link AbstractPlatformTransactionManager} 内部所需的所有状态信息，
 * 包括由具体事务管理器实现确定的通用事务对象。
 *
 * <p>支持将保存点相关方法委托给实现 {@link SavepointManager} 接口的事务对象。
 *
 * <p><b>注意：</b>此<i>不</i>用于其他 PlatformTransactionManager 实现，
 * 特别是不用于测试环境中的模拟事务管理器。请使用替代的 {@link SimpleTransactionStatus} 类
 * 或纯 {@link org.springframework.transaction.TransactionStatus} 接口的模拟。
 *
 * @author Juergen Hoeller
 * @since 19.01.2004
 * @see AbstractPlatformTransactionManager
 * @see org.springframework.transaction.SavepointManager
 * @see #getTransaction
 * @see #createSavepoint
 * @see #rollbackToSavepoint
 * @see #releaseSavepoint
 * @see SimpleTransactionStatus
 */
public class DefaultTransactionStatus extends AbstractTransactionStatus {

	@Nullable
	private final Object transaction;

	private final boolean newTransaction;

	private final boolean newSynchronization;

	private final boolean readOnly;

	private final boolean debug;

	@Nullable
	private final Object suspendedResources;


	/**
	 * 创建新的 {@code DefaultTransactionStatus} 实例。
	 * @param transaction 可以持有内部事务实现状态的底层事务对象
	 * @param newTransaction 如果是新事务，否则参与现有事务
	 * @param newSynchronization 如果为给定事务打开了新的事务同步
	 * @param readOnly 事务是否标记为只读
	 * @param debug 是否应为此事务的处理启用调试日志？
	 * 在此缓存它可以防止重复调用日志系统以确认是否应启用调试日志。
	 * @param suspendedResources 如果有被挂起的此事务资源持有者
	 */
	public DefaultTransactionStatus(
			@Nullable Object transaction, boolean newTransaction, boolean newSynchronization,
			boolean readOnly, boolean debug, @Nullable Object suspendedResources) {

		this.transaction = transaction;
		this.newTransaction = newTransaction;
		this.newSynchronization = newSynchronization;
		this.readOnly = readOnly;
		this.debug = debug;
		this.suspendedResources = suspendedResources;
	}


	/**
	 * 返回底层事务对象。
	 * @throws IllegalStateException 如果没有活动事务
	 */
	public Object getTransaction() {
		Assert.state(this.transaction != null, "No transaction active");
		return this.transaction;
	}

	/**
	 * 返回是否有实际事务处于活动状态。
	 */
	public boolean hasTransaction() {
		return (this.transaction != null);
	}

	@Override
	public boolean isNewTransaction() {
		return (hasTransaction() && this.newTransaction);
	}

	/**
	 * 返回是否为此事务打开了新的事务同步。
	 */
	public boolean isNewSynchronization() {
		return this.newSynchronization;
	}

	/**
	 * 返回此事务是否定义为只读事务。
	 */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * 返回此事务的进度是否被调试。此方法由 {@link AbstractPlatformTransactionManager}
	 * 作为优化使用，以防止重复调用 {@code logger.isDebugEnabled()}。并非真正用于客户端代码。
	 */
	public boolean isDebug() {
		return this.debug;
	}

	/**
	 * 返回为此事务被挂起的资源持有者（如果有）。
	 */
	@Nullable
	public Object getSuspendedResources() {
		return this.suspendedResources;
	}


	//---------------------------------------------------------------------
	// Enable functionality through underlying transaction object
	//---------------------------------------------------------------------

	/**
	 * 通过检查事务对象来确定 rollback-only 标志，前提是后者实现了 {@link SmartTransactionObject} 接口。
	 * <p>如果全局事务本身已被事务协调者标记为 rollback-only（例如超时的情况），将返回 {@code true}。
	 * @see SmartTransactionObject#isRollbackOnly()
	 */
	@Override
	public boolean isGlobalRollbackOnly() {
		return ((this.transaction instanceof SmartTransactionObject) &&
				((SmartTransactionObject) this.transaction).isRollbackOnly());
	}

	/**
	 * 此实现暴露底层事务对象的 {@link SavepointManager} 接口（如果有的话）。
	 * @throws NestedTransactionNotSupportedException 如果不支持保存点
	 * @see #isTransactionSavepointManager()
	 */
	@Override
	protected SavepointManager getSavepointManager() {
		Object transaction = this.transaction;
		if (!(transaction instanceof SavepointManager)) {
			throw new NestedTransactionNotSupportedException(
					"Transaction object [" + this.transaction + "] does not support savepoints");
		}
		return (SavepointManager) transaction;
	}

	/**
	 * 返回底层事务是否实现了 {@link SavepointManager} 接口，因此支持保存点。
	 * @see #getTransaction()
	 * @see #getSavepointManager()
	 */
	public boolean isTransactionSavepointManager() {
		return (this.transaction instanceof SavepointManager);
	}

	/**
	 * 将刷新操作委托给事务对象，前提是后者实现了 {@link SmartTransactionObject} 接口。
	 * @see SmartTransactionObject#flush()
	 */
	@Override
	public void flush() {
		if (this.transaction instanceof SmartTransactionObject) {
			((SmartTransactionObject) this.transaction).flush();
		}
	}

}
