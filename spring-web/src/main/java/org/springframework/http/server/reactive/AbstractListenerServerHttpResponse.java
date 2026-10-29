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

package org.springframework.http.server.reactive;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;

/**
 * 基于监听器的服务器响应的抽象基类，例如 Servlet 3.1
 * 和 Undertow。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractListenerServerHttpResponse extends AbstractServerHttpResponse {

	private final AtomicBoolean writeCalled = new AtomicBoolean();


	public AbstractListenerServerHttpResponse(DataBufferFactory bufferFactory) {
		super(bufferFactory);
	}

	public AbstractListenerServerHttpResponse(DataBufferFactory bufferFactory, HttpHeaders headers) {
		super(bufferFactory, headers);
	}


	@Override
	protected final Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
		return writeAndFlushWithInternal(Mono.just(body));
	}

	@Override
	protected final Mono<Void> writeAndFlushWithInternal(
			Publisher<? extends Publisher<? extends DataBuffer>> body) {

		if (!this.writeCalled.compareAndSet(false, true)) {
			return Mono.error(new IllegalStateException(
					"writeWith() or writeAndFlushWith() has already been called"));
		}
		Processor<? super Publisher<? extends DataBuffer>, Void> processor = createBodyFlushProcessor();
		return Mono.from(subscriber -> {
			body.subscribe(processor);
			processor.subscribe(subscriber);
		});
	}

	/**
	 * 抽象模板方法，用于创建一个 {@code Processor<Publisher<DataBuffer>, Void>}，
	 * 该处理器将写入响应体，并在每次刷新时刷新到底层输出。由
	 * {@link #writeAndFlushWithInternal(Publisher)} 调用。
	 */
	protected abstract Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor();

}
