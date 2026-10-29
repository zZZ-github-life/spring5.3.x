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

package org.springframework.transaction.reactive;

import org.reactivestreams.Publisher;

import org.springframework.transaction.ReactiveTransaction;

/**
 * 响应式事务代码的回调接口。与{@link TransactionalOperator}的
 * {@code execute}方法一起使用，通常作为方法实现中的匿名类。
 *
 * <p>通常用于将对事务不感知数据访问服务的各种调用
 * 组装到具有事务界定的高层服务方法中。作为替代方案，
 * 可以考虑使用声明式事务界定（例如通过Spring的
 * {@link org.springframework.transaction.annotation.Transactional}注解）。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @param <T> 结果类型
 * @see TransactionalOperator
 */
@FunctionalInterface
public interface TransactionCallback<T> {

	/**
	 * 在事务上下文中由{@link TransactionalOperator}调用。
	 * 不需要关心事务本身，尽管它可以通过给定的状态对象
	 * 检索和影响当前事务的状态，例如设置rollback-only。
	 * @param status associated transaction status
	 * @return a result publisher
	 * @see TransactionalOperator#transactional
	 */
	Publisher<T> doInTransaction(ReactiveTransaction status);

}
