/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.transaction.TransactionStatus;

/**
 * 事务代码的回调接口。与 {@link TransactionTemplate} 的 {@code execute} 方法一起使用，
 * 通常作为方法实现中的匿名类。
 *
 * <p>典型用法是将各种对事务不感知数据访问服务的调用组装到具有事务划分的更高级服务方法中。
 * 作为替代方案，考虑使用声明式事务划分（例如通过 Spring 的
 * {@link org.springframework.transaction.annotation.Transactional} 注解）。
 *
 * @author Juergen Hoeller
 * @since 17.03.2003
 * @param <T> 结果类型
 * @see TransactionTemplate
 * @see CallbackPreferringPlatformTransactionManager
 */
@FunctionalInterface
public interface TransactionCallback<T> {

	/**
	 * 由 {@link TransactionTemplate#execute} 在事务上下文中调用。
	 * 不需要关心事务本身，虽然它可以通过给定的状态对象检索和影响当前事务的状态，
	 * 例如设置 rollback-only。
	 * <p>允许返回在事务中创建的结果对象，即领域对象或领域对象集合。
	 * 回调抛出的 RuntimeException 被视为强制回滚的应用程序异常。
	 * 此类异常将传播到模板的调用者，除非回滚时出现问题，
	 * 在这种情况下将抛出 TransactionException。
	 * @param status 关联的事务状态
	 * @return 结果对象，或 {@code null}
	 * @see TransactionTemplate#execute
	 * @see CallbackPreferringPlatformTransactionManager#execute
	 */
	@Nullable
	T doInTransaction(TransactionStatus status);

}
