/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.transaction;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

/**
 * 特定 {@link TestContext} 的事务上下文。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 */
class TransactionContext {

	private static final Log logger = LogFactory.getLog(TransactionContext.class);

	private final TestContext testContext;

	private final TransactionDefinition transactionDefinition;

	private final PlatformTransactionManager transactionManager;

	private final boolean defaultRollback;

	private boolean flaggedForRollback;

	@Nullable
	private TransactionStatus transactionStatus;

	private final AtomicInteger transactionsStarted = new AtomicInteger();


	TransactionContext(TestContext testContext, PlatformTransactionManager transactionManager,
			TransactionDefinition transactionDefinition, boolean defaultRollback) {

		this.testContext = testContext;
		this.transactionManager = transactionManager;
		this.transactionDefinition = transactionDefinition;
		this.defaultRollback = defaultRollback;
		this.flaggedForRollback = defaultRollback;
	}


	@Nullable
	TransactionStatus getTransactionStatus() {
		return this.transactionStatus;
	}

	/**
	 * 当前事务是否已标记为回滚？
	 * <p>换言之，当前测试完成时，我们应回滚还是提交当前事务？
	 */
	boolean isFlaggedForRollback() {
		return this.flaggedForRollback;
	}

	void setFlaggedForRollback(boolean flaggedForRollback) {
		Assert.state(this.transactionStatus != null, () ->
				"Failed to set rollback flag - transaction does not exist: " + this.testContext);
		this.flaggedForRollback = flaggedForRollback;
	}

	/**
	 * 为配置的测试上下文启动一个新事务。
	 * <p>仅在已调用 {@link #endTransaction} 或之前未启动任何事务时调用此方法。
	 * @throws TransactionException if starting the transaction fails
	 */
	void startTransaction() {
		Assert.state(this.transactionStatus == null,
				"Cannot start a new transaction without ending the existing transaction first");

		this.flaggedForRollback = this.defaultRollback;
		this.transactionStatus = this.transactionManager.getTransaction(this.transactionDefinition);
		int transactionsStarted = this.transactionsStarted.incrementAndGet();

		if (logger.isInfoEnabled()) {
			logger.info(String.format(
					"Began transaction (%s) for test context %s; transaction manager [%s]; rollback [%s]",
					transactionsStarted, this.testContext, this.transactionManager, this.flaggedForRollback));
		}
	}

	/**
	 * 根据{@linkplain #isFlaggedForRollback 回滚标志}，立即为配置的测试上下文强制<em>提交</em>或<em>回滚</em>事务。
	 */
	void endTransaction() {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format(
					"Ending transaction for test context %s; transaction status [%s]; rollback [%s]",
					this.testContext, this.transactionStatus, this.flaggedForRollback));
		}
		Assert.state(this.transactionStatus != null,
				() -> "Failed to end transaction - transaction does not exist: " + this.testContext);

		try {
			if (this.flaggedForRollback) {
				this.transactionManager.rollback(this.transactionStatus);
			}
			else {
				this.transactionManager.commit(this.transactionStatus);
			}
		}
		finally {
			this.transactionStatus = null;
		}

		if (logger.isInfoEnabled()) {
			logger.info((this.flaggedForRollback ? "Rolled back" : "Committed") +
					" transaction for test: " + this.testContext);
		}
	}

}
