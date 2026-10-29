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

package org.springframework.transaction.support;

import java.io.Flushable;

import org.springframework.core.Ordered;

/**
 * 事务同步回调的接口。由 AbstractPlatformTransactionManager 支持。
 *
 * <p>TransactionSynchronization 实现可以实现 Ordered 接口以影响其执行顺序。
 * 未实现 Ordered 接口的同步将被追加到同步链的末尾。
 *
 * <p>Spring 本身执行的系统同步使用特定的顺序值，允许精细控制其执行顺序（如有必要）。
 *
 * <p>从 5.3 起，实现 {@link Ordered} 接口以支持声明式控制同步的执行顺序。
 * 默认 {@link #getOrder() 顺序}为 {@link Ordered#LOWEST_PRECEDENCE}，表示延迟执行；
 * 返回较低的值表示较早执行。
 *
 * @author Juergen Hoeller
 * @since 02.06.2003
 * @see TransactionSynchronizationManager
 * @see AbstractPlatformTransactionManager
 * @see org.springframework.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
 */
public interface TransactionSynchronization extends Ordered, Flushable {

	/** 正常提交时的完成状态。 */
	int STATUS_COMMITTED = 0;

	/** 正常回滚时的完成状态。 */
	int STATUS_ROLLED_BACK = 1;

	/** 启发式混合完成或系统出错时的完成状态。 */
	int STATUS_UNKNOWN = 2;


	/**
	 * 返回此事务同步的执行顺序。
	 * <p>默认为 {@link Ordered#LOWEST_PRECEDENCE}。
	 */
	@Override
	default int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * 挂起此同步。假定在管理资源的情况下，将资源从 TransactionSynchronizationManager 解绑。
	 * @see TransactionSynchronizationManager#unbindResource
	 */
	default void suspend() {
	}

	/**
	 * 恢复此同步。假定在管理资源的情况下，将资源重新绑定到 TransactionSynchronizationManager。
	 * @see TransactionSynchronizationManager#bindResource
	 */
	default void resume() {
	}

	/**
	 * 如果适用，将底层会话刷新到数据存储：例如 Hibernate/JPA 会话。
	 * @see org.springframework.transaction.TransactionStatus#flush()
	 */
	@Override
	default void flush() {
	}

	/**
	 * 在事务提交之前调用（在 "beforeCompletion" 之前）。
	 * 例如，可以将事务性 O/R Mapping 会话刷新到数据库。
	 * <p>此回调<i>不</i>意味着事务实际上会被提交。在此方法调用后仍可能发生回滚决定。
	 * 此回调主要用于执行仅在提交仍有可能发生时才有意义的工作，
	 * 例如将 SQL 语句刷新到数据库。
	 * <p>请注意，异常将传播到提交调用者并导致事务回滚。
	 * @param readOnly 事务是否定义为只读事务
	 * @throws RuntimeException 如果出错；将<b>传播到调用者</b>
	 * （注意：不要在此抛出 TransactionException 子类！）
	 * @see #beforeCompletion
	 */
	default void beforeCommit(boolean readOnly) {
	}

	/**
	 * 在事务提交/回滚之前调用。可以在事务完成<i>之前</i>执行资源清理。
	 * <p>此方法将在 {@code beforeCommit} 之后调用，即使 {@code beforeCommit} 抛出异常。
	 * 此回调允许在事务完成前关闭资源，无论结果如何。
	 * @throws RuntimeException 如果出错；将<b>记录日志但不传播</b>
	 * （注意：不要在此抛出 TransactionException 子类！）
	 * @see #beforeCommit
	 * @see #afterCompletion
	 */
	default void beforeCompletion() {
	}

	/**
	 * 在事务提交之后调用。可以在主事务<i>成功</i>提交<i>之后</i>执行进一步操作。
	 * <p>例如，可以提交旨在跟随主事务成功提交的进一步操作，如确认消息或邮件。
	 * <p><b>注意：</b>事务将已经被提交，但事务资源可能仍然活跃且可访问。
	 * 因此，在此时触发的任何数据访问代码仍将"参与"原始事务，允许执行一些清理
	 * （不会再有提交！），除非它显式声明需要在单独的事务中运行。
	 * 因此：<b>对于从此处调用的任何事务操作，使用 {@code PROPAGATION_REQUIRES_NEW}。</b>
	 * @throws RuntimeException 如果出错；将<b>传播到调用者</b>
	 * （注意：不要在此抛出 TransactionException 子类！）
	 */
	default void afterCommit() {
	}

	/**
	 * 在事务提交/回滚之后调用。可以在事务完成<i>之后</i>执行资源清理。
	 * <p><b>注意：</b>事务将已经被提交或回滚，但事务资源可能仍然活跃且可访问。
	 * 因此，在此时触发的任何数据访问代码仍将"参与"原始事务，允许执行一些清理
	 * （不会再有提交！），除非它显式声明需要在单独的事务中运行。
	 * 因此：<b>对于从此处调用的任何事务操作，使用 {@code PROPAGATION_REQUIRES_NEW}。</b>
	 * @param status 根据 {@code STATUS_*} 常量的完成状态
	 * @throws RuntimeException 如果出错；将<b>记录日志但不传播</b>
	 * （注意：不要在此抛出 TransactionException 子类！）
	 * @see #STATUS_COMMITTED
	 * @see #STATUS_ROLLED_BACK
	 * @see #STATUS_UNKNOWN
	 * @see #beforeCompletion
	 */
	default void afterCompletion(int status) {
	}

}
