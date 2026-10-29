/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager} 接口的扩展，
 * 暴露一个在事务中执行给定回调的方法。
 *
 * <p>此接口的实现者自动表达了对回调的偏好，而非编程式的 {@code getTransaction}、
 * {@code commit} 和 {@code rollback} 调用。调用代码可以检查给定事务管理器是否实现了此接口，
 * 以选择准备回调而非显式事务划分控制。
 *
 * <p>Spring 的 {@link TransactionTemplate} 和
 * {@link org.springframework.transaction.interceptor.TransactionInterceptor}
 * 会自动检测并使用此 PlatformTransactionManager 变体。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see TransactionTemplate
 * @see org.springframework.transaction.interceptor.TransactionInterceptor
 */
public interface CallbackPreferringPlatformTransactionManager extends PlatformTransactionManager {

	/**
	 * 在事务中执行给定回调对象指定的操作。
	 * <p>允许返回在事务中创建的结果对象，即领域对象或领域对象集合。
	 * 回调抛出的 RuntimeException 被视为强制回滚的致命异常。
	 * 此类异常将传播到模板的调用者。
	 * @param definition 用于包装回调的事务定义
	 * @param callback 指定事务操作的回调对象
	 * @return 回调返回的结果对象，如果没有则为 {@code null}
	 * @throws TransactionException 如果初始化、回滚或系统出错
	 * @throws RuntimeException 如果由 TransactionCallback 抛出
	 */
	@Nullable
	<T> T execute(@Nullable TransactionDefinition definition, TransactionCallback<T> callback)
			throws TransactionException;

}
