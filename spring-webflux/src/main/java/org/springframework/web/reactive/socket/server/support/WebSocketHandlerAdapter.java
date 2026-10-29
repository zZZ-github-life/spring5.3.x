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

package org.springframework.web.reactive.socket.server.support;

import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerAdapter}，使
 * {@link org.springframework.web.reactive.DispatcherHandler} 能够支持
 * {@link WebSocketHandler} 类型的处理器，此类处理器可通过
 * {@link org.springframework.web.reactive.handler.SimpleUrlHandlerMapping}
 * 映射到 URL 模式。
 *
 * <p>请求通过委托给 {@link WebSocketService}（默认是
 * {@link HandshakeWebSocketService}）来处理，该服务会检查 WebSocket 握手请求参数，
 * 升级为 WebSocket 交互，并使用 {@link WebSocketHandler} 进行处理。
 *
 * <p>自 5.3 起，通过 {@code @EnableWebFlux} 导入的 WebFlux Java 配置已包含对此
 * 适配器的声明，因此它不再需要出现在应用程序配置中。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebSocketHandlerAdapter implements HandlerAdapter, Ordered {

	private final WebSocketService webSocketService;

	private int order = 2;


	/**
	 * 默认构造函数，创建并使用一个 {@link HandshakeWebSocketService}。
	 */
	public WebSocketHandlerAdapter() {
		this(new HandshakeWebSocketService());
	}

	/**
	 * 指定要使用的 {@link WebSocketService} 的备用构造函数。
	 */
	public WebSocketHandlerAdapter(WebSocketService webSocketService) {
		Assert.notNull(webSocketService, "'webSocketService' is required");
		this.webSocketService = webSocketService;
	}


	/**
	 * 设置此适配器的顺序值。
	 * <p>默认设置为 2。
	 * @param order the value to set to
	 * @since 5.3
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * 返回此实例的 {@link #setOrder(int) 配置}顺序。
	 * @since 5.3
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 返回配置的用于处理请求的 {@code WebSocketService}。
	 */
	public WebSocketService getWebSocketService() {
		return this.webSocketService;
	}


	@Override
	public boolean supports(Object handler) {
		return WebSocketHandler.class.isAssignableFrom(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		WebSocketHandler webSocketHandler = (WebSocketHandler) handler;
		return getWebSocketService().handleRequest(exchange, webSocketHandler).then(Mono.empty());
	}

}
