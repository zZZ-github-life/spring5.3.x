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
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * JTA事务对象, 表示{@link javax.transaction.UserTransaction}。
 * 由Spring的{@link JtaTransactionManager}用作事务对象。
 *
 * <p>注意: 这是一个SPI类, 不打算被应用程序使用。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see JtaTransactionManager
 * @see javax.transaction.UserTransaction
 */
public class JtaTransactionObject implements SmartTransactionObject {

	private final UserTransaction userTransaction;

	boolean resetTransactionTimeout = false;


	/**
	 * 为给定的JTA UserTransaction创建一个新的JtaTransactionObject。
	 * @param userTransaction 当前事务的 JTA UserTransaction
	 * （可以是共享对象，也可以通过每次事务的全新查找获得）
	 */
	public JtaTransactionObject(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	/**
	 * 返回当前事务的JTA UserTransaction对象。
	 */
	public final UserTransaction getUserTransaction() {
		return this.userTransaction;
	}


	/**
	 * 此实现检查UserTransaction的rollback-only标志。
	 */
	@Override
	public boolean isRollbackOnly() {
		try {
			int jtaStatus = this.userTransaction.getStatus();
			return (jtaStatus == Status.STATUS_MARKED_ROLLBACK || jtaStatus == Status.STATUS_ROLLEDBACK);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on getStatus", ex);
		}
	}

	/**
	 * 此实现触发flush回调, 假定它们会flush所有受影响的ORM会话。
	 * @see org.springframework.transaction.support.TransactionSynchronization#flush()
	 */
	@Override
	public void flush() {
		TransactionSynchronizationUtils.triggerFlush();
	}

}
