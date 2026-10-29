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

package org.springframework.transaction.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * 简化编程式事务界定和事务异常处理的操作器类。
 *
 * <p>核心方法是{@link #transactional}，支持对函数式序列代码进行
 * 事务包装。此操作器处理事务生命周期和可能的异常，
 * 使得ReactiveTransactionCallback实现和调用代码都不需要
 * 显式处理事务。
 *
 * <p>典型用法：允许编写使用数据库连接等资源的
 * 底层数据访问对象，而这些对象本身不具备事务感知能力。
 * 相反，它们可以隐式参与由使用此类的高层应用程序服务
 * 处理的事务，通过内部类回调对象调用底层服务。
 *
 * <p><strong>注意：</strong>事务性Publisher应避免Subscription取消。
 * 请参阅Spring Framework参考文档的
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#tx-prog-operator-cancel">取消信号</a>
 * 章节了解更多详情。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see #execute
 * @see ReactiveTransactionManager
 */
public interface TransactionalOperator {

	/**
	 * 将给定Flux指定的函数式序列包装在事务中。
	 * @param flux the Flux that should be executed within the transaction
	 * @return a result publisher returned by the callback, or {@code null} if none
	 * @throws TransactionException in case of initialization, rollback, or system errors
	 * @throws RuntimeException if thrown by the TransactionCallback
	 */
	default <T> Flux<T> transactional(Flux<T> flux) {
		return execute(it -> flux);
	}

	/**
	 * 将给定Mono指定的函数式序列包装在事务中。
	 * @param mono the Mono that should be executed within the transaction
	 * @return a result publisher returned by the callback
	 * @throws TransactionException in case of initialization, rollback, or system errors
	 * @throws RuntimeException if thrown by the TransactionCallback
	 */
	<T> Mono<T> transactional(Mono<T> mono);

	/**
	 * 在事务中执行给定回调对象指定的操作。
	 * <p>允许返回在事务中创建的结果对象，即
	 * 领域对象或领域对象的集合。回调抛出的RuntimeException
	 * 被视为强制回滚的致命异常。
	 * 此类异常将传播到模板的调用者。
	 * @param action the callback object that specifies the transactional action
	 * @return a result object returned by the callback
	 * @throws TransactionException in case of initialization, rollback, or system errors
	 * @throws RuntimeException if thrown by the TransactionCallback
	 */
	<T> Flux<T> execute(TransactionCallback<T> action) throws TransactionException;


	// Static builder methods

	/**
	 * 使用{@link ReactiveTransactionManager}创建一个新的{@link TransactionalOperator}，
	 * 使用默认事务。
	 * @param transactionManager the transaction management strategy to be used
	 * @return the transactional operator
	 */
	static TransactionalOperator create(ReactiveTransactionManager transactionManager){
		return create(transactionManager, TransactionDefinition.withDefaults());
	}

	/**
	 * 使用{@link ReactiveTransactionManager}和{@link TransactionDefinition}
	 * 创建一个新的{@link TransactionalOperator}。
	 * @param transactionManager the transaction management strategy to be used
	 * @param transactionDefinition the transaction definition to apply
	 * @return the transactional operator
	 */
	static TransactionalOperator create(
			ReactiveTransactionManager transactionManager, TransactionDefinition transactionDefinition){

		return new TransactionalOperatorImpl(transactionManager, transactionDefinition);
	}

}
