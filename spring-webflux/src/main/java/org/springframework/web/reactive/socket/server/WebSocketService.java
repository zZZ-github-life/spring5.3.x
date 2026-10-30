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

package org.springframework.web.reactive.socket.server;

import reactor.core.publisher.Mono;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于将 WebSocket 相关的 HTTP 请求委托给它的服务。
 *
 * <p>对于 WebSocket 端点，这意味着处理初始的 WebSocket HTTP 握手请求。对于 SockJS 端点，
 * 则可能意味着处理 SockJS 协议中定义的所有 HTTP 请求。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
 */
public interface WebSocketService {

	/**
	 * 使用给定的 {@link WebSocketHandler} 处理请求。
	 * @param exchange 当前交换
	 * @param webSocketHandler WebSocket 会话的处理器
	 * @return 在应用对 WebSocket 会话的处理完成时
	 * 完成的 {@code Mono<Void>}。
	 */
	Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler webSocketHandler);

}
