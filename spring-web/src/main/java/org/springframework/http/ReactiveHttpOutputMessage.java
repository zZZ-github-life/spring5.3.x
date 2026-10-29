/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

/**
 * 以 {@link Publisher} 形式接受输出的 "响应式" HTTP 输出消息。
 * <p>通常由客户端的 HTTP 请求或服务器端的 HTTP 响应实现。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ReactiveHttpOutputMessage extends HttpMessage {

	/**
	 * 返回可用于创建正文的 {@link DataBufferFactory}。
	 * @return a buffer factory
	 * @see #writeWith(Publisher)
	 */
	DataBufferFactory bufferFactory();

	/**
	 * 注册一个在 HttpOutputMessage 提交之前执行的操作。
	 * <p><strong>注意：</strong>提供的操作必须正确延迟执行，例如通过 {@link Mono#defer} 或 {@link Mono#fromRunnable}，以确保它相对于其他操作按正确顺序执行。
	 * @param action the action to apply
	 */
	void beforeCommit(Supplier<? extends Mono<Void>> action);

	/**
	 * 判断 HttpOutputMessage 是否已提交。
	 */
	boolean isCommitted();

	/**
	 * 使用给定的 {@link Publisher} 将消息正文写入底层 HTTP 层。
	 * @param body the body content publisher
	 * @return a {@link Mono} that indicates completion or error
	 */

	Mono<Void> writeWith(Publisher<? extends DataBuffer> body);

	/**
	 * 使用给定的 {@code Publishers} 的 {@link Publisher} 将 HttpOutputMessage 的正文写入底层 HTTP 层，并在每个 {@code Publisher<DataBuffer>} 之后刷新。
	 * @param body the body content publisher
	 * @return a {@link Mono} that indicates completion or error
	 */
	Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body);

	/**
	 * 指示消息处理已完成，从而可以执行任何清理或处理结束任务，例如将通过 {@link #getHeaders()} 所做的头更改应用到基础 HTTP 消息（如果尚未应用）。
	 * <p>此方法应在消息处理结束时自动调用，因此应用程序通常无需调用它。如果多次调用，应没有副作用。
	 * @return a {@link Mono} that indicates completion or error
	 */
	Mono<Void> setComplete();

}
