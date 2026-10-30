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

import java.util.ArrayDeque;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.transaction.NoTransactionException;

/**
 * 用于注册和获取事务上下文的委托类。
 *
 * <p>通常由拦截或编排事务流的组件使用，
 * 例如AOP拦截器或事务操作器。
 *
 * @author Mark Paluch
 * @since 5.2
 * @see TransactionSynchronization
 */
public abstract class TransactionContextManager {

	private TransactionContextManager() {
	}


	/**
	 * 从订阅者上下文或事务上下文持有者中获取当前的
	 * {@link TransactionContext}。如果没有注册上下文或上下文持有者，
	 * 上下文获取将因NoTransactionException而失败。
	 * @return 当前的 {@link TransactionContext}
	 * @throws NoTransactionException 如果在订阅者上下文中找不到 TransactionContext，
	 * 或在持有者中找不到上下文
	 */
	public static Mono<TransactionContext> currentContext() throws NoTransactionException {
		return Mono.deferContextual(ctx -> {
			if (ctx.hasKey(TransactionContext.class)) {
				return Mono.just(ctx.get(TransactionContext.class));
			}
			if (ctx.hasKey(TransactionContextHolder.class)) {
				TransactionContextHolder holder = ctx.get(TransactionContextHolder.class);
				if (holder.hasContext()) {
					return Mono.just(holder.currentContext());
				}
			}
			return Mono.error(new NoTransactionInContextException());
		});
	}

	/**
	 * 创建一个{@link TransactionContext}并将其注册到订阅者{@link Context}中。
	 * @return 函数式的上下文注册。
	 * @throws IllegalStateException 如果已经关联了事务上下文。
	 * @see Mono#contextWrite(Function)
	 * @see Flux#contextWrite(Function)
	 */
	public static Function<Context, Context> createTransactionContext() {
		return context -> context.put(TransactionContext.class, new TransactionContext());
	}

	/**
	 * 返回一个{@link Function}，用于创建或关联一个新的{@link TransactionContext}。
	 * 通过{@link TransactionSynchronizationManager}与事务资源交互
	 * 需要在订阅者上下文中注册一个TransactionContext。
	 * @return 函数式的上下文注册。
	 */
	public static Function<Context, Context> getOrCreateContext() {
		return context -> {
			TransactionContextHolder holder = context.get(TransactionContextHolder.class);
			if (holder.hasContext()) {
				return context.put(TransactionContext.class, holder.currentContext());
			}
			return context.put(TransactionContext.class, holder.createContext());
		};
	}

	/**
	 * 返回一个{@link Function}，用于创建或关联一个新的
	 * {@link TransactionContextHolder}。在响应式流中创建和释放事务
	 * 需要一个遵循自上而下执行方案的可变持有者。
	 * Reactor的订阅者上下文在变更可见性方面遵循自下而上的方式。
	 * @return 函数式的上下文注册。
	 */
	public static Function<Context, Context> getOrCreateContextHolder() {
		return context -> {
			if (!context.hasKey(TransactionContextHolder.class)) {
				return context.put(TransactionContextHolder.class, new TransactionContextHolder(new ArrayDeque<>()));
			}
			return context;
		};
	}


	/**
	 * 用于响应式流的无堆栈{@link NoTransactionException}变体。
	 */
	@SuppressWarnings("serial")
	private static class NoTransactionInContextException extends NoTransactionException {

		public NoTransactionInContextException() {
			super("No transaction in context");
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			// stackless exception
			return this;
		}
	}

}
