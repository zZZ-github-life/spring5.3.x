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

import java.util.function.BiFunction;

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;

/**
 * 表示一个针对（延迟的）{@link Result} 流执行 {@link Statement} 的函数。
 *
 * <p>请注意，被丢弃的 {@link Result} 对象必须根据 R2DBC 规范，
 * 通过 {@link Result#getRowsUpdated()} 或 {@link Result#map(BiFunction)}
 * 进行消费。
 *
 * <p>通常，实现会调用 {@link Statement#execute()} 方法来启动语句对象的执行。
 *
 * 例如：
 * <p><pre class="code">
 * DatabaseClient.builder()
 *		.executeFunction(statement -&gt; statement.execute())
 * 		.build();
 * </pre>
 *
 * @author Mark Paluch
 * @since 5.3
 * @see Statement#execute()
 */
@FunctionalInterface
public interface ExecuteFunction {

	/**
	 * 对 {@link Result} 对象流执行给定的 {@link Statement}。
	 * @param statement the request to execute
	 * @return the delayed result stream
	 */
	Publisher<? extends Result> execute(Statement statement);

}
