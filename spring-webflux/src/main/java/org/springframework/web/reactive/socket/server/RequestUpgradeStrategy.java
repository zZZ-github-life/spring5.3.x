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

import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * 根据底层网络运行时，将 HTTP 请求升级为 WebSocket 会话的策略。
 *
 * <p>通常每种 {@link ServerHttpRequest} 和 {@link ServerHttpResponse} 类型都有
 * 一个这样的策略，但 Servlet 容器除外：标准 Java WebSocket API JSR-356 未定义
 * 升级请求的方式，因此每个 Servlet 容器都需要自定义策略。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestUpgradeStrategy {

	/**
	 * 升级为 WebSocket 会话并使用给定处理器处理它。
	 * @param exchange 当前交换
	 * @param webSocketHandler WebSocket 会话的处理器
	 * @param subProtocol 为处理器选定的子协议
	 * @return 用于指示 WebSocket 会话处理结果的
	 * 完成信号 {@code Mono<Void>}。
	 * @deprecated as of 5.1 in favor of
	 * {@link #upgrade(ServerWebExchange, WebSocketHandler, String, Supplier)}
	 */
	@Deprecated
	default Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler,
			@Nullable String subProtocol) {

		return Mono.error(new UnsupportedOperationException());
	}

	/**
	 * 升级为 WebSocket 会话并使用给定处理器处理它。
	 * @param exchange 当前交换
	 * @param webSocketHandler WebSocket 会话的处理器
	 * @param subProtocol 为处理器选定的子协议
	 * @param handshakeInfoFactory 为 WebSocket 会话创建 HandshakeInfo 的工厂
	 * @return 用于指示 WebSocket 会话处理结果的
	 * 完成信号 {@code Mono<Void>}。
	 * @since 5.1
	 */
	default Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		return upgrade(exchange, webSocketHandler, subProtocol);
	}

}
