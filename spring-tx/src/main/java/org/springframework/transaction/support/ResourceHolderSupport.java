/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Date;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionTimedOutException;

/**
 * 资源持有者的便捷基类。
 *
 * <p>为参与事务提供 rollback-only 支持。
 * 可以在一定的秒数或毫秒数后过期，以确定事务超时。
 *
 * @author Juergen Hoeller
 * @since 02.02.2004
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager#doBegin
 * @see org.springframework.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
 */
public abstract class ResourceHolderSupport implements ResourceHolder {

	private boolean synchronizedWithTransaction = false;

	private boolean rollbackOnly = false;

	@Nullable
	private Date deadline;

	private int referenceCount = 0;

	private boolean isVoid = false;


	/**
	 * 将资源标记为与事务同步。
	 */
	public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
		this.synchronizedWithTransaction = synchronizedWithTransaction;
	}

	/**
	 * 返回资源是否与事务同步。
	 */
	public boolean isSynchronizedWithTransaction() {
		return this.synchronizedWithTransaction;
	}

	/**
	 * 将资源事务标记为 rollback-only。
	 */
	public void setRollbackOnly() {
		this.rollbackOnly = true;
	}

	/**
	 * 重置此资源事务的 rollback-only 状态。
	 * <p>仅真正用于在保留原始资源活动的自定义回滚步骤之后调用，例如在保存点的情况下。
	 * @since 5.0
	 * @see org.springframework.transaction.SavepointManager#rollbackToSavepoint
	 */
	public void resetRollbackOnly() {
		this.rollbackOnly = false;
	}

	/**
	 * 返回资源事务是否被标记为 rollback-only。
	 */
	public boolean isRollbackOnly() {
		return this.rollbackOnly;
	}

	/**
	 * 设置此对象的超时时间（以秒为单位）。
	 * @param seconds 到期前的秒数
	 */
	public void setTimeoutInSeconds(int seconds) {
		setTimeoutInMillis(seconds * 1000L);
	}

	/**
	 * 设置此对象的超时时间（以毫秒为单位）。
	 * @param millis 到期前的毫秒数
	 */
	public void setTimeoutInMillis(long millis) {
		this.deadline = new Date(System.currentTimeMillis() + millis);
	}

	/**
	 * 返回此对象是否有关联的超时时间。
	 */
	public boolean hasTimeout() {
		return (this.deadline != null);
	}

	/**
	 * 返回此对象的到期截止时间。
	 * @return 截止时间（Date 对象）
	 */
	@Nullable
	public Date getDeadline() {
		return this.deadline;
	}

	/**
	 * 返回此对象的存活时间（以秒为单位）。
	 * 向上取整，例如 9.00001 仍为 10。
	 * @return 到期前的秒数
	 * @throws TransactionTimedOutException 如果截止时间已到达
	 */
	public int getTimeToLiveInSeconds() {
		double diff = ((double) getTimeToLiveInMillis()) / 1000;
		int secs = (int) Math.ceil(diff);
		checkTransactionTimeout(secs <= 0);
		return secs;
	}

	/**
	 * 返回此对象的存活时间（以毫秒为单位）。
	 * @return 到期前的毫秒数
	 * @throws TransactionTimedOutException 如果截止时间已到达
	 */
	public long getTimeToLiveInMillis() throws TransactionTimedOutException{
		if (this.deadline == null) {
			throw new IllegalStateException("No timeout specified for this resource holder");
		}
		long timeToLive = this.deadline.getTime() - System.currentTimeMillis();
		checkTransactionTimeout(timeToLive <= 0);
		return timeToLive;
	}

	/**
	 * 如果截止时间已到达，设置事务为 rollback-only 并抛出 TransactionTimedOutException。
	 */
	private void checkTransactionTimeout(boolean deadlineReached) throws TransactionTimedOutException {
		if (deadlineReached) {
			setRollbackOnly();
			throw new TransactionTimedOutException("Transaction timed out: deadline was " + this.deadline);
		}
	}

	/**
	 * 将引用计数加一，因为持有者已被请求（即有人请求了它持有的资源）。
	 */
	public void requested() {
		this.referenceCount++;
	}

	/**
	 * 将引用计数减一，因为持有者已被释放（即有人释放了它持有的资源）。
	 */
	public void released() {
		this.referenceCount--;
	}

	/**
	 * 返回此持有者是否仍有未关闭的引用。
	 */
	public boolean isOpen() {
		return (this.referenceCount > 0);
	}

	/**
	 * 清除此资源持有者的事务状态。
	 */
	public void clear() {
		this.synchronizedWithTransaction = false;
		this.rollbackOnly = false;
		this.deadline = null;
	}

	/**
	 * 重置此资源持有者 - 事务状态以及引用计数。
	 */
	@Override
	public void reset() {
		clear();
		this.referenceCount = 0;
	}

	@Override
	public void unbound() {
		this.isVoid = true;
	}

	@Override
	public boolean isVoid() {
		return this.isVoid;
	}

}
