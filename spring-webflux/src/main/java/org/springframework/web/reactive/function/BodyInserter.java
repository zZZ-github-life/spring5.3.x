/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * 可填充 {@link ReactiveHttpOutputMessage} 正文的函数的组合。
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> the type of data to insert
 * @param <M> the type of {@link ReactiveHttpOutputMessage} this inserter can be applied to
 * @see BodyInserters
 */
@FunctionalInterface
public interface BodyInserter<T, M extends ReactiveHttpOutputMessage> {

	/**
	 * 插入到给定的输出消息中。
	 * @param outputMessage the response to insert into
	 * @param context the context to use
	 * @return a {@code Mono} that indicates completion or error
	 */
	Mono<Void> insert(M outputMessage, Context context);


	/**
	 * 定义插入过程中使用的上下文。
	 */
	interface Context {

		/**
		 * 返回用于响应正文转换的 {@link HttpMessageWriter HttpMessageWriters}。
		 * @return the stream of message writers
		 */
		List<HttpMessageWriter<?>> messageWriters();

		/**
		 * 可选地返回 {@link ServerHttpRequest}（如果存在）。
		 */
		Optional<ServerHttpRequest> serverRequest();

		/**
		 * 返回用于响应正文转换的提示映射。
		 */
		Map<String, Object> hints();
	}

}
