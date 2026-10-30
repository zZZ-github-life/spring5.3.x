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

package org.springframework.transaction.reactive;

import org.springframework.lang.Nullable;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.util.Assert;

/**
 * {@link ReactiveTransaction}接口的默认实现，
 * 由{@link AbstractReactiveTransactionManager}使用。基于
 * 底层"事务对象"的概念。
 *
 * <p>持有{@link AbstractReactiveTransactionManager}内部需要的所有状态信息，
 * 包括由具体事务管理器实现确定的通用事务对象。
 *
 * <p><b>注意：</b>此类<i>不</i>用于其他ReactiveTransactionManager实现，
 * 特别是不用于测试环境中的模拟事务管理器。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see AbstractReactiveTransactionManager
 * @see #getTransaction
 */
public class GenericReactiveTransaction implements ReactiveTransaction {

	@Nullable
	private final Object transaction;

	private final boolean newTransaction;

	private final boolean newSynchronization;

	private final boolean readOnly;

	private final boolean debug;

	@Nullable
	private final Object suspendedResources;

	private boolean rollbackOnly = false;

	private boolean completed = false;


	/**
	 * 创建一个新的 {@code DefaultReactiveTransactionStatus} 实例。
	 * @param transaction 底层事务对象，可为内部事务实现保存状态
	 * @param newTransaction 事务是否为新事务，否则表示参与现有事务
	 * @param newSynchronization 是否为给定事务开启了新的事务同步
	 * @param readOnly 事务是否被标记为只读
	 * @param debug 是否应为该事务的处理启用调试日志？
	 * 在此处缓存可以避免反复询问日志系统
	 * 是否应启用调试日志。
	 * @param suspendedResources 用于保存为该事务挂起的资源的持有者（如果有）
	 */
	public GenericReactiveTransaction(
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
	 * @throws IllegalStateException 如果没有活跃的事务
	 */
	public Object getTransaction() {
		Assert.state(this.transaction != null, "No transaction active");
		return this.transaction;
	}

	/**
	 * 返回是否有实际事务处于活跃状态。
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
	 * 返回此事务是否被定义为只读事务。
	 */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * 返回此事务的进度是否被调试。这由
	 * {@link AbstractReactiveTransactionManager}作为优化使用，
	 * 以防止重复调用{@code logger.isDebugEnabled()}。
	 * 并非真正用于客户端代码。
	 */
	public boolean isDebug() {
		return this.debug;
	}

	/**
	 * 返回为此事务挂起的资源持有者（如果有）。
	 */
	@Nullable
	public Object getSuspendedResources() {
		return this.suspendedResources;
	}

	@Override
	public void setRollbackOnly() {
		this.rollbackOnly = true;
	}

	/**
	 * 通过检查此ReactiveTransactionStatus来确定rollback-only标志。
	 * <p>仅当应用程序在此TransactionStatus对象上调用了
	 * {@code setRollbackOnly}时才返回"true"。
	 */
	@Override
	public boolean isRollbackOnly() {
		return this.rollbackOnly;
	}

	/**
	 * 将此事务标记为已完成，即已提交或已回滚。
	 */
	public void setCompleted() {
		this.completed = true;
	}

	@Override
	public boolean isCompleted() {
		return this.completed;
	}

}
