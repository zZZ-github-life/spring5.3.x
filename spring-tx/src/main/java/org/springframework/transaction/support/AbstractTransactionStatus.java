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
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionUsageException;

/**
 * {@link org.springframework.transaction.TransactionStatus} 接口的抽象基类实现。
 *
 * <p>预实现了本地 rollback-only 和 completed 标志的处理，以及委托给底层
 * {@link org.springframework.transaction.SavepointManager} 的功能。
 * 还提供了在事务内持有保存点的选项。
 *
 * <p>不假定任何特定的内部事务处理方式，如底层事务对象，也不假定事务同步机制。
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see #setRollbackOnly()
 * @see #isRollbackOnly()
 * @see #setCompleted()
 * @see #isCompleted()
 * @see #getSavepointManager()
 * @see SimpleTransactionStatus
 * @see DefaultTransactionStatus
 */
public abstract class AbstractTransactionStatus implements TransactionStatus {

	private boolean rollbackOnly = false;

	private boolean completed = false;

	@Nullable
	private Object savepoint;


	//---------------------------------------------------------------------
	// Implementation of TransactionExecution
	//---------------------------------------------------------------------

	@Override
	public void setRollbackOnly() {
		this.rollbackOnly = true;
	}

	/**
	 * 通过检查此 TransactionStatus 的本地 rollback-only 标志以及底层事务的全局
	 * rollback-only 标志（如果有）来确定 rollback-only 标志。
	 * @see #isLocalRollbackOnly()
	 * @see #isGlobalRollbackOnly()
	 */
	@Override
	public boolean isRollbackOnly() {
		return (isLocalRollbackOnly() || isGlobalRollbackOnly());
	}

	/**
	 * 通过检查此 TransactionStatus 来确定 rollback-only 标志。
	 * <p>仅当应用程序在此 TransactionStatus 对象上调用了 {@code setRollbackOnly} 时才返回 "true"。
	 */
	public boolean isLocalRollbackOnly() {
		return this.rollbackOnly;
	}

	/**
	 * 用于确定底层事务的全局 rollback-only 标志的模板方法（如果有）。
	 * <p>此实现始终返回 {@code false}。
	 */
	public boolean isGlobalRollbackOnly() {
		return false;
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


	//---------------------------------------------------------------------
	// Handling of current savepoint state
	//---------------------------------------------------------------------

	@Override
	public boolean hasSavepoint() {
		return (this.savepoint != null);
	}

	/**
	 * 为此事务设置保存点。对 PROPAGATION_NESTED 有用。
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NESTED
	 */
	protected void setSavepoint(@Nullable Object savepoint) {
		this.savepoint = savepoint;
	}

	/**
	 * 获取此事务的保存点（如果有）。
	 */
	@Nullable
	protected Object getSavepoint() {
		return this.savepoint;
	}

	/**
	 * 创建保存点并为事务持有它。
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
	 * 如果底层事务不支持保存点
	 */
	public void createAndHoldSavepoint() throws TransactionException {
		setSavepoint(getSavepointManager().createSavepoint());
	}

	/**
	 * 回滚到为事务持有的保存点并立即释放该保存点。
	 */
	public void rollbackToHeldSavepoint() throws TransactionException {
		Object savepoint = getSavepoint();
		if (savepoint == null) {
			throw new TransactionUsageException(
					"Cannot roll back to savepoint - no savepoint associated with current transaction");
		}
		getSavepointManager().rollbackToSavepoint(savepoint);
		getSavepointManager().releaseSavepoint(savepoint);
		setSavepoint(null);
	}

	/**
	 * 释放为事务持有的保存点。
	 */
	public void releaseHeldSavepoint() throws TransactionException {
		Object savepoint = getSavepoint();
		if (savepoint == null) {
			throw new TransactionUsageException(
					"Cannot release savepoint - no savepoint associated with current transaction");
		}
		getSavepointManager().releaseSavepoint(savepoint);
		setSavepoint(null);
	}


	//---------------------------------------------------------------------
	// Implementation of SavepointManager
	//---------------------------------------------------------------------

	/**
	 * 此实现将操作委托给底层事务的 SavepointManager（如果可能）。
	 * @see #getSavepointManager()
	 * @see SavepointManager#createSavepoint()
	 */
	@Override
	public Object createSavepoint() throws TransactionException {
		return getSavepointManager().createSavepoint();
	}

	/**
	 * 此实现将操作委托给底层事务的 SavepointManager（如果可能）。
	 * @see #getSavepointManager()
	 * @see SavepointManager#rollbackToSavepoint(Object)
	 */
	@Override
	public void rollbackToSavepoint(Object savepoint) throws TransactionException {
		getSavepointManager().rollbackToSavepoint(savepoint);
	}

	/**
	 * 此实现将操作委托给底层事务的 SavepointManager（如果可能）。
	 * @see #getSavepointManager()
	 * @see SavepointManager#releaseSavepoint(Object)
	 */
	@Override
	public void releaseSavepoint(Object savepoint) throws TransactionException {
		getSavepointManager().releaseSavepoint(savepoint);
	}

	/**
	 * 返回底层事务的 SavepointManager（如果可能）。
	 * <p>默认实现始终抛出 NestedTransactionNotSupportedException。
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
	 * 如果底层事务不支持保存点
	 */
	protected SavepointManager getSavepointManager() {
		throw new NestedTransactionNotSupportedException("This transaction does not support savepoints");
	}


	//---------------------------------------------------------------------
	// Flushing support
	//---------------------------------------------------------------------

	/**
	 * 此实现为空，将 flush 视为无操作。
	 */
	@Override
	public void flush() {
	}

}
