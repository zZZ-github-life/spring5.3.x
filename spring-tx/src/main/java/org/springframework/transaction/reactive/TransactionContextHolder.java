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

import java.util.Deque;

import org.springframework.transaction.NoTransactionException;

/**
 * 响应式事务{@link TransactionContext 上下文}的可变持有者。
 * 此持有者维护对各个{@link TransactionContext}的引用。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionContext
 */
final class TransactionContextHolder {

	private final Deque<TransactionContext> transactionStack;


	TransactionContextHolder(Deque<TransactionContext> transactionStack) {
		this.transactionStack = transactionStack;
	}


	/**
	 * 返回当前的{@link TransactionContext}。
	 * @throws NoTransactionException 如果没有正在进行的事务
	 */
	TransactionContext currentContext() {
		TransactionContext context = this.transactionStack.peek();
		if (context == null) {
			throw new NoTransactionException("No transaction in context");
		}
		return context;
	}

	/**
	 * 创建一个新的{@link TransactionContext}。
	 */
	TransactionContext createContext() {
		TransactionContext context = this.transactionStack.peek();
		if (context != null) {
			context = new TransactionContext(context);
		}
		else {
			context = new TransactionContext();
		}
		this.transactionStack.push(context);
		return context;
	}

	/**
	 * 检查持有者是否包含一个{@link TransactionContext}。
	 * @return 如果关联了{@link TransactionContext}则返回{@literal true}
	 */
	boolean hasContext() {
		return !this.transactionStack.isEmpty();
	}

}
