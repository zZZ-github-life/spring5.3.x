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

package org.springframework.transaction.jta;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 实现JTA {@link javax.transaction.Synchronization}接口的适配器, 委托给底层的Spring
 * {@link org.springframework.transaction.support.TransactionSynchronization}。
 *
 * <p>对于将Spring资源管理代码与纯JTA / EJB CMT事务进行同步非常有用,
 * 尽管原始代码是为Spring事务同步而构建的。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.transaction.Transaction#registerSynchronization
 * @see org.springframework.transaction.support.TransactionSynchronization
 */
public class SpringJtaSynchronizationAdapter implements Synchronization {

	protected static final Log logger = LogFactory.getLog(SpringJtaSynchronizationAdapter.class);

	private final TransactionSynchronization springSynchronization;

	@Nullable
	private UserTransaction jtaTransaction;

	private boolean beforeCompletionCalled = false;


	/**
	 * 为给定的Spring TransactionSynchronization创建一个新的SpringJtaSynchronizationAdapter。
	 * @param springSynchronization the Spring TransactionSynchronization to delegate to
	 */
	public SpringJtaSynchronizationAdapter(TransactionSynchronization springSynchronization) {
		Assert.notNull(springSynchronization, "TransactionSynchronization must not be null");
		this.springSynchronization = springSynchronization;
	}

	/**
	 * 为给定的Spring TransactionSynchronization和JTA TransactionManager创建一个新的SpringJtaSynchronizationAdapter。
	 * <p>请注意, 此适配器在WebLogic上永远不会执行rollback-only调用,
	 * 因为已知WebLogic Server在出现{@code beforeCompletion}异常时会自动将事务标记为rollback-only。
	 * 因此, 在WLS上, 此构造器等同于单参数构造器。
	 * @param springSynchronization the Spring TransactionSynchronization to delegate to
	 * @param jtaUserTransaction the JTA UserTransaction to use for rollback-only
	 * setting in case of an exception thrown in {@code beforeCompletion}
	 * (can be omitted if the JTA provider itself marks the transaction rollback-only
	 * in such a scenario, which is required by the JTA specification as of JTA 1.1).
	 */
	public SpringJtaSynchronizationAdapter(TransactionSynchronization springSynchronization,
			@Nullable UserTransaction jtaUserTransaction) {

		this(springSynchronization);
		if (jtaUserTransaction != null && !jtaUserTransaction.getClass().getName().startsWith("weblogic.")) {
			this.jtaTransaction = jtaUserTransaction;
		}
	}

	/**
	 * 为给定的Spring TransactionSynchronization和JTA TransactionManager创建一个新的SpringJtaSynchronizationAdapter。
	 * <p>请注意, 此适配器在WebLogic上永远不会执行rollback-only调用,
	 * 因为已知WebLogic Server在出现{@code beforeCompletion}异常时会自动将事务标记为rollback-only。
	 * 因此, 在WLS上, 此构造器等同于单参数构造器。
	 * @param springSynchronization the Spring TransactionSynchronization to delegate to
	 * @param jtaTransactionManager the JTA TransactionManager to use for rollback-only
	 * setting in case of an exception thrown in {@code beforeCompletion}
	 * (can be omitted if the JTA provider itself marks the transaction rollback-only
	 * in such a scenario, which is required by the JTA specification as of JTA 1.1)
	 */
	public SpringJtaSynchronizationAdapter(
			TransactionSynchronization springSynchronization, @Nullable TransactionManager jtaTransactionManager) {

		this(springSynchronization);
		if (jtaTransactionManager != null && !jtaTransactionManager.getClass().getName().startsWith("weblogic.")) {
			this.jtaTransaction = new UserTransactionAdapter(jtaTransactionManager);
		}
	}


	/**
	 * JTA {@code beforeCompletion}回调: 在提交之前调用。
	 * <p>如果出现异常, JTA事务将被标记为rollback-only。
	 * @see org.springframework.transaction.support.TransactionSynchronization#beforeCommit
	 */
	@Override
	public void beforeCompletion() {
		try {
			boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
			this.springSynchronization.beforeCommit(readOnly);
		}
		catch (RuntimeException | Error ex) {
			setRollbackOnlyIfPossible();
			throw ex;
		}
		finally {
			// Process Spring's beforeCompletion early, in order to avoid issues
			// with strict JTA implementations that issue warnings when doing JDBC
			// operations after transaction completion (e.g. Connection.getWarnings).
			this.beforeCompletionCalled = true;
			this.springSynchronization.beforeCompletion();
		}
	}

	/**
	 * 将底层JTA事务设置为rollback-only。
	 */
	private void setRollbackOnlyIfPossible() {
		if (this.jtaTransaction != null) {
			try {
				this.jtaTransaction.setRollbackOnly();
			}
			catch (UnsupportedOperationException ex) {
				// Probably Hibernate's WebSphereExtendedJTATransactionLookup pseudo JTA stuff...
				logger.debug("JTA transaction handle does not support setRollbackOnly method - " +
						"relying on JTA provider to mark the transaction as rollback-only based on " +
						"the exception thrown from beforeCompletion", ex);
			}
			catch (Throwable ex) {
				logger.error("Could not set JTA transaction rollback-only", ex);
			}
		}
		else {
			logger.debug("No JTA transaction handle available and/or running on WebLogic - " +
						"relying on JTA provider to mark the transaction as rollback-only based on " +
						"the exception thrown from beforeCompletion");
			}
	}

	/**
	 * JTA {@code afterCompletion}回调: 在提交/回滚之后调用。
	 * <p>在回滚的情况下, 需要在这个较晚的阶段调用Spring同步的{@code beforeCompletion},
	 * 因为JTA没有相应的回调。
	 * @see org.springframework.transaction.support.TransactionSynchronization#beforeCompletion
	 * @see org.springframework.transaction.support.TransactionSynchronization#afterCompletion
	 */
	@Override
	public void afterCompletion(int status) {
		if (!this.beforeCompletionCalled) {
			// beforeCompletion not called before (probably because of JTA rollback).
			// Perform the cleanup here.
			this.springSynchronization.beforeCompletion();
		}
		// Call afterCompletion with the appropriate status indication.
		switch (status) {
			case Status.STATUS_COMMITTED:
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
				break;
			case Status.STATUS_ROLLEDBACK:
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
				break;
			default:
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
		}
	}

}
