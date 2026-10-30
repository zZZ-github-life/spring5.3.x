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

package org.springframework.r2dbc.core;

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;

import org.springframework.util.Assert;

/**
 * 表示一个用于过滤 {@link ExecuteFunction 执行函数}的函数。
 *
 * <p>当 {@link org.reactivestreams.Subscriber} 订阅 {@link DatabaseClient}
 * 返回的 {@link Publisher} 时，该过滤器会被执行。
 *
 * <p>StatementFilterFunction 通常用于指定 Statement 对象上的附加细节，
 * 例如 {@code fetchSize} 或键生成。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.3
 * @see ExecuteFunction
 */
@FunctionalInterface
public interface StatementFilterFunction {

	/**
	 * 一个空的 {@link StatementFilterFunction}，委托给 {@link ExecuteFunction}。
	 */
	StatementFilterFunction EMPTY_FILTER = (statement, next) -> next.execute(statement);


	/**
	 * 将此过滤器应用于给定的 {@link Statement} 和 {@link ExecuteFunction}。
	 * <p>给定的 {@link ExecuteFunction} 表示链中的下一个实体，
	 * 应通过 {@link ExecuteFunction#execute(Statement)} 调用它
	 * 以继续执行，或者不调用它来短路该链。
	 * @param statement 当前 {@link Statement}
	 * @param next 链中的下一个执行函数
	 * @return 过滤后的 {@link Result}。
	 */
	Publisher<? extends Result> filter(Statement statement, ExecuteFunction next);

	/**
	 * 返回一个组合过滤器函数，它先应用此过滤器，
	 * 然后应用给定的 {@code "after"} 过滤器。
	 * @param afterFilter 在此过滤器之后应用的过滤器
	 * @return 组合后的过滤器。
	 */
	default StatementFilterFunction andThen(StatementFilterFunction afterFilter) {
		Assert.notNull(afterFilter, "StatementFilterFunction must not be null");
		return (request, next) -> filter(request, afterRequest -> afterFilter.filter(afterRequest, next));
	}

}
