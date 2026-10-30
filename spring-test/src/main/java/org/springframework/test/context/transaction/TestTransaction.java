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

package org.springframework.test.context.transaction;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

/**
 * {@code TestTransaction} 提供了一组静态工具方法，用于在 <em>测试</em>方法、<em>before</em> 方法和 <em>after</em> 方法中以编程方式与<em>测试管理的事务</em>交互。
 * <p>有关<em>测试管理的事务</em>的详细说明，请查阅 {@link TransactionalTestExecutionListener} 的 javadoc。
 * <p>只要启用了 {@code TransactionalTestExecutionListener}，对 {@code TestTransaction} 的支持就自动可用。请注意，{@code TransactionalTestExecutionListener} 通常默认启用，但也可以通过 {@link TestExecutionListeners @TestExecutionListeners} 注解手动启用。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see TransactionalTestExecutionListener
 */
public final class TestTransaction {


	private TestTransaction() {
	}


	/**
	 * 确定测试管理的事务当前是否<em>活动</em>。
	 * @return 如果当前有测试管理的事务处于活动状态，则为 {@code true}
	 * @see #start()
	 * @see #end()
	 */
	public static boolean isActive() {
		TransactionContext transactionContext = TransactionContextHolder.getCurrentTransactionContext();
		if (transactionContext != null) {
			TransactionStatus transactionStatus = transactionContext.getTransactionStatus();
			return (transactionStatus != null && !transactionStatus.isCompleted());
		}
		return false;
	}

	/**
	 * 确定当前测试管理的事务已被{@linkplain #flagForRollback() 标记为回滚}还是被{@linkplain #flagForCommit() 标记为提交}。
	 * @return 如果当前测试管理的事务被标记为回滚，则为 {@code true}；
	 * 如果当前测试管理的事务被标记为提交，则为 {@code false}
	 * @throws IllegalStateException 如果当前测试没有活动的事务
	 * @see #isActive()
	 * @see #flagForRollback()
	 * @see #flagForCommit()
	 */
	public static boolean isFlaggedForRollback() {
		return requireCurrentTransactionContext().isFlaggedForRollback();
	}

	/**
	 * 将当前测试管理的事务标记为<em>回滚</em>。
	 * <p>调用此方法<em>不会</em>结束当前事务。相反，该标志的值将用于确定当前测试管理的事务在{@linkplain #end 结束}后应回滚还是提交。
	 * @throws IllegalStateException 如果当前测试没有活动的事务
	 * @see #isActive()
	 * @see #isFlaggedForRollback()
	 * @see #start()
	 * @see #end()
	 */
	public static void flagForRollback() {
		setFlaggedForRollback(true);
	}

	/**
	 * 将当前测试管理的事务标记为<em>提交</em>。
	 * <p>调用此方法<em>不会</em>结束当前事务。相反，该标志的值将用于确定当前测试管理的事务在{@linkplain #end 结束}后应回滚还是提交。
	 * @throws IllegalStateException 如果当前测试没有活动的事务
	 * @see #isActive()
	 * @see #isFlaggedForRollback()
	 * @see #start()
	 * @see #end()
	 */
	public static void flagForCommit() {
		setFlaggedForRollback(false);
	}

	/**
	 * 启动一个新的测试管理的事务。
	 * <p>仅在已调用 {@link #end} 或之前未启动任何事务时调用此方法。
	 * @throws IllegalStateException 如果无法检索事务上下文，
	 * 或者当前测试已有活动的事务
	 * @see #isActive()
	 * @see #end()
	 */
	public static void start() {
		requireCurrentTransactionContext().startTransaction();
	}

	/**
	 * 根据{@linkplain #isFlaggedForRollback 回滚标志}，立即强制<em>提交</em>或<em>回滚</em>当前测试管理的事务。
	 * @throws IllegalStateException 如果无法检索事务上下文，
	 * 或者当前测试没有活动的事务
	 * @see #isActive()
	 * @see #start()
	 */
	public static void end() {
		requireCurrentTransactionContext().endTransaction();
	}


	private static TransactionContext requireCurrentTransactionContext() {
		TransactionContext txContext = TransactionContextHolder.getCurrentTransactionContext();
		Assert.state(txContext != null, "TransactionContext is not active");
		return txContext;
	}

	private static void setFlaggedForRollback(boolean flag) {
		requireCurrentTransactionContext().setFlaggedForRollback(flag);
	}

}
