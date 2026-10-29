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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;

/**
 * 对 HTTP 客户端的抽象，驱动底层 HTTP 客户端连接到源服务器，
 * 并提供发送 {@link ClientHttpRequest} 和接收
 * {@link ClientHttpResponse} 所需的所有基础设施。
 *
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpConnector {

	/**
	 * 使用给定的 {@code HttpMethod} 和 {@code URI} 连接到源服务器，
	 * 并在底层 API 的 HTTP 请求可以初始化并写入时应用给定的
	 * {@code requestCallback}。
	 * @param method the HTTP request method
	 * @param uri the HTTP request URI
	 * @param requestCallback a function that prepares and writes to the request,
	 * returning a publisher that signals when it's done writing.
	 * Implementations can return a {@code Mono<Void>} by calling
	 * {@link ClientHttpRequest#writeWith} or {@link ClientHttpRequest#setComplete}.
	 * @return publisher for the {@link ClientHttpResponse}
	 */
	Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback);

}
