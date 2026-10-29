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

package org.springframework.transaction;

import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;

/**
 * 这是 Spring 响应式事务基础设施中的核心接口。
 * 应用程序可以直接使用它，但它主要不是作为 API 使用的：
 * 通常，应用程序会使用事务操作符或通过 AOP 进行
 * 声明式事务界定。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see org.springframework.transaction.reactive.TransactionalOperator
 * @see org.springframework.transaction.interceptor.TransactionInterceptor
 * @see org.springframework.transaction.PlatformTransactionManager
 */
public interface ReactiveTransactionManager extends TransactionManager {

	/**
	 * 根据指定的传播行为，发出当前活跃的响应式事务或创建一个新事务。
	 * <p>请注意，隔离级别或超时等参数仅会应用于新事务，
	 * 因此在参与现有活跃事务时会被忽略。
	 * <p>此外，并非所有事务管理器都支持每种事务定义设置：
	 * 正确的事务管理器实现遇到不支持的设置时应抛出异常。
	 * <p>上述规则的一个例外是只读标志，如果不支持显式的只读模式，
	 * 则应忽略该标志。本质上，只读标志只是一个潜在优化的提示。
	 * @param definition the TransactionDefinition instance,
	 * describing propagation behavior, isolation level, timeout etc.
	 * @return transaction status object representing the new or current transaction
	 * @throws TransactionException in case of lookup, creation, or system errors
	 * @throws IllegalTransactionStateException if the given transaction definition
	 * cannot be executed (for example, if a currently active transaction is in
	 * conflict with the specified propagation behavior)
	 * @see TransactionDefinition#getPropagationBehavior
	 * @see TransactionDefinition#getIsolationLevel
	 * @see TransactionDefinition#getTimeout
	 * @see TransactionDefinition#isReadOnly
	 */
	Mono<ReactiveTransaction> getReactiveTransaction(@Nullable TransactionDefinition definition)
			throws TransactionException;

	/**
	 * 根据事务的状态提交给定的事务。如果事务已被程序化地标记为仅回滚，则执行回滚。
	 * <p>如果事务不是新事务，则省略提交以正确参与周围的事务。
	 * 如果之前的一个事务被挂起以便创建一个新事务，则在提交新事务后恢复之前的事务。
	 * <p>请注意，当 commit 调用完成时，无论是正常完成还是抛出异常，
	 * 事务都必须被完全完成和清理。在这种情况下不应期望再调用 rollback。
	 * <p>如果此方法抛出了 TransactionException 以外的异常，
	 * 则表示某些提交前的错误导致提交尝试失败。例如，
	 * 一个 O/R Mapping 工具可能在提交前尝试将更改刷新到数据库，
	 * 由此产生的 DataAccessException 导致事务失败。
	 * 在这种情况下，原始异常将传播到此 commit 方法的调用者。
	 * @param transaction object returned by the {@code getTransaction} method
	 * @throws UnexpectedRollbackException in case of an unexpected rollback
	 * that the transaction coordinator initiated
	 * @throws HeuristicCompletionException in case of a transaction failure
	 * caused by a heuristic decision on the side of the transaction coordinator
	 * @throws TransactionSystemException in case of commit or system errors
	 * (typically caused by fundamental resource failures)
	 * @throws IllegalTransactionStateException if the given transaction
	 * is already completed (that is, committed or rolled back)
	 * @see ReactiveTransaction#setRollbackOnly
	 */
	Mono<Void> commit(ReactiveTransaction transaction) throws TransactionException;

	/**
	 * 对给定的事务执行回滚。
	 * <p>如果事务不是新事务，则将其设置为仅回滚，以正确参与周围的事务。
	 * 如果之前的一个事务被挂起以便创建一个新事务，则在新事务回滚后恢复之前的事务。
	 * <p><b>如果 commit 抛出异常，不要对事务调用 rollback。</b>
	 * 事务在 commit 返回时已经被完成和清理，即使在 commit 异常的情况下也是如此。
	 * 因此，在 commit 失败后调用 rollback 将导致 IllegalTransactionStateException。
	 * @param transaction object returned by the {@code getTransaction} method
	 * @throws TransactionSystemException in case of rollback or system errors
	 * (typically caused by fundamental resource failures)
	 * @throws IllegalTransactionStateException if the given transaction
	 * is already completed (that is, committed or rolled back)
	 */
	Mono<Void> rollback(ReactiveTransaction transaction) throws TransactionException;

}
