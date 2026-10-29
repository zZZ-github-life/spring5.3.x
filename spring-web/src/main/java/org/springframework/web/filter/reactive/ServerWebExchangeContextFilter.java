/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.filter.reactive;

import java.util.Optional;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * 在 Reactor {@link Context} 中插入一个属性，使当前
 * {@link ServerWebExchange} 可以在属性名
 * {@link #EXCHANGE_CONTEXT_ATTRIBUTE} 下获取。这对于无需将 exchange 显式传递给
 * 参与请求处理的组件即可访问它非常有用。
 *
 * <p>便捷方法 {@link #get(Context)} 用于查找该 exchange。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ServerWebExchangeContextFilter implements WebFilter {

	/** 在上下文中保存 exchange 的属性名。 */
	public static final String EXCHANGE_CONTEXT_ATTRIBUTE =
			ServerWebExchangeContextFilter.class.getName() + ".EXCHANGE_CONTEXT";


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return chain.filter(exchange)
				.contextWrite(cxt -> cxt.put(EXCHANGE_CONTEXT_ATTRIBUTE, exchange));
	}


	/**
	 * 从 Reactor Context 中访问 {@link ServerWebExchange}（如果可用），
	 * 即配置了 {@link ServerWebExchangeContextFilter} 且给定上下文来自请求处理链时。
	 * @param context the context in which to access the exchange
	 * @return the exchange
	 */
	public static Optional<ServerWebExchange> get(Context context) {
		return context.getOrEmpty(EXCHANGE_CONTEXT_ATTRIBUTE);
	}

}
