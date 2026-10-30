/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.reactive;

import reactor.core.publisher.Mono;

/**
 * 响应式事务同步回调的接口。
 * 由{@link AbstractReactiveTransactionManager}支持。
 *
 * <p>TransactionSynchronization实现可以实现
 * {@link org.springframework.core.Ordered}接口来影响其执行顺序。
 * 未实现{@link org.springframework.core.Ordered}接口的同步
 * 将被追加到同步链的末尾。
 *
 * <p>Spring本身执行的系统同步使用特定的顺序值，
 * 允许与其执行顺序进行细粒度的交互（如有必要）。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionSynchronizationManager
 * @see AbstractReactiveTransactionManager
 */
public interface TransactionSynchronization {

	/** 正常提交时的完成状态。 */
	int STATUS_COMMITTED = 0;

	/** 正常回滚时的完成状态。 */
	int STATUS_ROLLED_BACK = 1;

	/** 启发式混合完成或系统错误时的完成状态。 */
	int STATUS_UNKNOWN = 2;


	/**
	 * 挂起此同步。
	 * 如果管理资源，应该从TransactionSynchronizationManager解绑资源。
	 * @see TransactionSynchronizationManager#unbindResource
	 */
	default Mono<Void> suspend() {
		return Mono.empty();
	}

	/**
	 * 恢复此同步。
	 * 如果管理资源，应该将资源重新绑定到TransactionSynchronizationManager。
	 * @see TransactionSynchronizationManager#bindResource
	 */
	default Mono<Void> resume() {
		return Mono.empty();
	}

	/**
	 * 在事务提交之前调用（在"beforeCompletion"之前）。
	 * <p>此回调<i>并不</i>意味着事务一定会被提交。
	 * 在调用此方法后仍可能发生回滚决定。此回调的目的
	 * 是执行仅在提交仍有可能发生时才有意义的工作，
	 * 例如将SQL语句刷新到数据库。
	 * <p>请注意，异常将传播到提交调用者，
	 * 并导致事务回滚。
	 * @param readOnly 事务是否被定义为只读事务
	 * @throws RuntimeException 发生错误时；将被 <b>传播给调用者</b>
	 * （注意：此处不要抛出 TransactionException 子类！）
	 * @see #beforeCompletion
	 */
	default Mono<Void> beforeCommit(boolean readOnly) {
		return Mono.empty();
	}

	/**
	 * 在事务提交/回滚之前调用。
	 * 可以在事务完成<i>之前</i>执行资源清理。
	 * <p>此方法将在{@code beforeCommit}之后被调用，
	 * 即使{@code beforeCommit}抛出了异常。此回调允许
	 * 在事务完成之前关闭资源，无论结果如何。
	 * @throws RuntimeException 发生错误时；将被 <b>记录日志但不传播</b>
	 * （注意：此处不要抛出 TransactionException 子类！）
	 * @see #beforeCommit
	 * @see #afterCompletion
	 */
	default Mono<Void> beforeCompletion() {
		return Mono.empty();
	}

	/**
	 * 在事务提交之后调用。可以在主事务<i>成功</i>提交后
	 * 立即执行进一步操作。
	 * <p>例如，可以提交应在主事务成功提交后执行的
	 * 进一步操作，如确认消息或邮件。
	 * <p><b>注意：</b>事务已经被提交，但事务资源可能
	 * 仍然活跃且可访问。因此，在此时触发的任何数据访问代码
	 * 仍将"参与"原始事务，允许执行一些清理（不会有后续的提交！），
	 * 除非它显式声明需要在单独的事务中运行。
	 * 因此：<b>对于从此处调用的任何事务操作，
	 * 使用{@code PROPAGATION_REQUIRES_NEW}。</b>
	 * @throws RuntimeException 发生错误时；将被 <b>传播给调用者</b>
	 * （注意：此处不要抛出 TransactionException 子类！）
	 */
	default Mono<Void> afterCommit() {
		return Mono.empty();
	}

	/**
	 * 在事务提交/回滚之后调用。
	 * 可以在事务完成<i>之后</i>执行资源清理。
	 * <p><b>注意：</b>事务已经被提交或回滚，
	 * 但事务资源可能仍然活跃且可访问。因此，
	 * 在此时触发的任何数据访问代码仍将"参与"原始事务，
	 * 允许执行一些清理（不会有后续的提交！），
	 * 除非它显式声明需要在单独的事务中运行。
	 * 因此：<b>对于从此处调用的任何事务操作，
	 * 使用{@code PROPAGATION_REQUIRES_NEW}。</b>
	 * @param status 根据 {@code STATUS_*} 常量表示的完成状态
	 * @throws RuntimeException 发生错误时；将被 <b>记录日志但不传播</b>
	 * （注意：此处不要抛出 TransactionException 子类！）
	 * @see #STATUS_COMMITTED
	 * @see #STATUS_ROLLED_BACK
	 * @see #STATUS_UNKNOWN
	 * @see #beforeCompletion
	 */
	default Mono<Void> afterCompletion(int status) {
		return Mono.empty();
	}

}
