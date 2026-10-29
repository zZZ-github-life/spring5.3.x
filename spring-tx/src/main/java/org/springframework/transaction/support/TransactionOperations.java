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

package org.springframework.transaction.support;

import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * 指定基本事务执行操作的接口。由 {@link TransactionTemplate} 实现。
 * 不常直接使用，但作为增强可测试性的有用选项，因为它很容易被模拟或存根。
 *
 * @author Juergen Hoeller
 * @since 2.0.4
 */
public interface TransactionOperations {

	/**
	 * 在事务中执行给定回调对象指定的操作。
	 * <p>允许返回在事务中创建的结果对象，即领域对象或领域对象集合。
	 * 回调抛出的 RuntimeException 被视为强制回滚的致命异常。
	 * 此类异常将传播到模板的调用者。
	 * @param action 指定事务操作的回调对象
	 * @return 回调返回的结果对象，如果没有则为 {@code null}
	 * @throws TransactionException 如果初始化、回滚或系统出错
	 * @throws RuntimeException 如果由 TransactionCallback 抛出
	 * @see #executeWithoutResult(Consumer)
	 */
	@Nullable
	<T> T execute(TransactionCallback<T> action) throws TransactionException;

	/**
	 * 在事务中执行给定 {@link Runnable} 指定的操作。
	 * <p>如果需要从回调中返回对象或从回调中访问
	 * {@link org.springframework.transaction.TransactionStatus}，
	 * 请改用 {@link #execute(TransactionCallback)}。
	 * <p>此变体类似于使用 {@link TransactionCallbackWithoutResult}，
	 * 但签名更简化，适用于常见场景 - 并且可以方便地与 Java 8 lambda 表达式一起使用。
	 * @param action 指定事务操作的 Runnable
	 * @throws TransactionException 如果初始化、回滚或系统出错
	 * @throws RuntimeException 如果由 Runnable 抛出
	 * @since 5.2
	 * @see #execute(TransactionCallback)
	 * @see TransactionCallbackWithoutResult
	 */
	default void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
		execute(status -> {
			action.accept(status);
			return null;
		});
	}


	/**
	 * 返回 {@code TransactionOperations} 接口的实现，该实现执行给定的 {@link TransactionCallback}
	 * 而不在实际事务中。
	 * <p>对测试有用：行为等同于使用没有实际事务的事务管理器
	 * （PROPAGATION_SUPPORTS）且没有同步（SYNCHRONIZATION_NEVER）运行。
	 * <p>对于具有实际事务处理的 {@link TransactionOperations} 实现，
	 * 请使用 {@link TransactionTemplate} 配合适当的
	 * {@link org.springframework.transaction.PlatformTransactionManager}。
	 * @since 5.2
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_SUPPORTS
	 * @see AbstractPlatformTransactionManager#SYNCHRONIZATION_NEVER
	 * @see TransactionTemplate
	 */
	static TransactionOperations withoutTransaction() {
		return WithoutTransactionOperations.INSTANCE;
	}

}
