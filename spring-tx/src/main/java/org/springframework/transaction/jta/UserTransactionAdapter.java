/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.springframework.util.Assert;

/**
 * JTA UserTransaction句柄的适配器, 接受JTA {@link javax.transaction.TransactionManager}
 * 引用并为其创建JTA {@link javax.transaction.UserTransaction}句柄。
 *
 * <p>JTA UserTransaction接口是JTA TransactionManager接口的精确子集。
 * 不幸的是, 它并不是TransactionManager的超接口, 因此当打算通过UserTransaction接口
 * 与TransactionManager句柄通信时, 需要此类适配器。
 *
 * <p>由Spring的{@link JtaTransactionManager}在某些场景下内部使用。
 * 不 intended 在应用程序代码中直接使用。
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 */
public class UserTransactionAdapter implements UserTransaction {

	private final TransactionManager transactionManager;


	/**
	 * 为给定的TransactionManager创建一个新的UserTransactionAdapter。
	 * @param transactionManager 要包装的 JTA TransactionManager
	 */
	public UserTransactionAdapter(TransactionManager transactionManager) {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回此适配器委托的JTA TransactionManager。
	 */
	public final TransactionManager getTransactionManager() {
		return this.transactionManager;
	}


	@Override
	public void setTransactionTimeout(int timeout) throws SystemException {
		this.transactionManager.setTransactionTimeout(timeout);
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		this.transactionManager.begin();
	}

	@Override
	public void commit()
			throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, SystemException {
		this.transactionManager.commit();
	}

	@Override
	public void rollback() throws SecurityException, SystemException {
		this.transactionManager.rollback();
	}

	@Override
	public void setRollbackOnly() throws SystemException {
		this.transactionManager.setRollbackOnly();
	}

	@Override
	public int getStatus() throws SystemException {
		return this.transactionManager.getStatus();
	}

}
