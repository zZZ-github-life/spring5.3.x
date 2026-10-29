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

package org.springframework.web.cors.reactive;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * 通过 {@link PreFlightRequestHandler} 处理预检请求并跳过链中其余部分的
 * WebFilter。
 *
 * <p>WebFlux 应用程序可以简单地注入 PreFlightRequestHandler 并使用它来创建
 * 此 WebFilter 的实例，因为 {@code @EnableWebFlux} 会将
 * {@code DispatcherHandler} 声明为 Bean，而它本身就是一个
 * PreFlightRequestHandler。
 *
 * @author Rossen Stoyanchev
 * @since 5.3.7
 */
public class PreFlightRequestWebFilter implements WebFilter {

	private final PreFlightRequestHandler handler;


	/**
	 * 创建一个将委托给给定处理器的实例。
	 */
	public PreFlightRequestWebFilter(PreFlightRequestHandler handler) {
		Assert.notNull(handler, "PreFlightRequestHandler is required");
		this.handler = handler;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return (CorsUtils.isPreFlightRequest(exchange.getRequest()) ?
				this.handler.handlePreFlight(exchange) : chain.filter(exchange));
	}

}
