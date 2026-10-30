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

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link TransactionFactory}策略接口的默认实现, 简单地包装标准的JTA
 * {@link javax.transaction.TransactionManager}。
 *
 * <p>不支持事务名称; 简单地忽略任何指定的名称。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.transaction.TransactionManager#setTransactionTimeout(int)
 * @see javax.transaction.TransactionManager#begin()
 * @see javax.transaction.TransactionManager#getTransaction()
 */
public class SimpleTransactionFactory implements TransactionFactory {

	private final TransactionManager transactionManager;


	/**
	 * 为给定的TransactionManager创建一个新的SimpleTransactionFactory。
	 * @param transactionManager 要包装的 JTA TransactionManager
	 */
	public SimpleTransactionFactory(TransactionManager transactionManager) {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
	}


	@Override
	public Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException {
		if (timeout >= 0) {
			this.transactionManager.setTransactionTimeout(timeout);
		}
		this.transactionManager.begin();
		return new ManagedTransactionAdapter(this.transactionManager);
	}

	@Override
	public boolean supportsResourceAdapterManagedTransactions() {
		return false;
	}

}
