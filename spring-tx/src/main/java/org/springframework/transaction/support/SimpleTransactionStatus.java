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

/**
 * 一个简单的 {@link org.springframework.transaction.TransactionStatus} 实现。
 * 派生自 {@link AbstractTransactionStatus}，添加了显式的 {@link #isNewTransaction() "newTransaction"} 标志。
 *
 * <p>此类不被 Spring 预置的任何 {@link org.springframework.transaction.PlatformTransactionManager}
 * 实现使用。它主要作为自定义事务管理器实现的起点，以及用于测试事务代码的静态模拟
 * （作为模拟 {@code PlatformTransactionManager} 的一部分，或作为传递给要测试的
 * {@link TransactionCallback} 的参数）。
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see TransactionCallback#doInTransaction
 */
public class SimpleTransactionStatus extends AbstractTransactionStatus {

	private final boolean newTransaction;


	/**
	 * 创建新的 {@code SimpleTransactionStatus} 实例，指示新事务。
	 */
	public SimpleTransactionStatus() {
		this(true);
	}

	/**
	 * 创建新的 {@code SimpleTransactionStatus} 实例。
	 * @param newTransaction 是否指示新事务
	 */
	public SimpleTransactionStatus(boolean newTransaction) {
		this.newTransaction = newTransaction;
	}


	@Override
	public boolean isNewTransaction() {
		return this.newTransaction;
	}

}
