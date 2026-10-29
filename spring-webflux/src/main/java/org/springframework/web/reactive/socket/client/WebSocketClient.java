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

package org.springframework.web.reactive.socket.client;

import java.net.URI;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * WebSocket 会话的响应式风格处理的契约。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketClient {

	/**
	 * 向给定 url 执行握手请求，并使用给定处理器处理由此产生的 WebSocket 会话。
	 * @param url the handshake url
	 * @param handler the handler of the WebSocket session
	 * @return completion {@code Mono<Void>} to indicate the outcome of the
	 * WebSocket session handling.
	 */
	Mono<Void> execute(URI url, WebSocketHandler handler);

	/**
	 * {@link #execute(URI, WebSocketHandler)} 的一个变体，支持自定义头。
	 * @param url the handshake url
	 * @param headers custom headers for the handshake request
	 * @param handler the handler of the WebSocket session
	 * @return completion {@code Mono<Void>} to indicate the outcome of the
	 * WebSocket session handling.
	 */
	Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler);

}
