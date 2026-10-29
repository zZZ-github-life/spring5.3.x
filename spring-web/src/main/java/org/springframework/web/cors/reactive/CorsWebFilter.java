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

package org.springframework.web.cors.reactive;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;


/**
 * 负责处理 CORS 预检请求，并借助 {@link CorsProcessor} 实现
 * （默认为 {@link DefaultCorsProcessor}）拦截 CORS 简单请求和实际请求的
 * {@link WebFilter}，从而使用所提供的 {@link CorsConfigurationSource}
 * （例如 {@link UrlBasedCorsConfigurationSource} 实例）添加相关的 CORS 响应头
 * （如 {@code Access-Control-Allow-Origin}）。
 *
 * <p>这是 Spring WebFlux Java 配置 CORS 的一种替代方案，
 * 主要适用于使用函数式 API 的应用程序。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 */
public class CorsWebFilter implements WebFilter {

	private final CorsConfigurationSource configSource;

	private final CorsProcessor processor;


	/**
	 * 构造函数，接受过滤器用来为每个传入请求查找要使用的
	 * {@link CorsConfiguration} 的 {@link CorsConfigurationSource}。
	 * @see UrlBasedCorsConfigurationSource
	 */
	public CorsWebFilter(CorsConfigurationSource configSource) {
		this(configSource, new DefaultCorsProcessor());
	}

	/**
	 * 构造函数，接受过滤器用来为每个传入请求查找要使用的
	 * {@link CorsConfiguration} 的 {@link CorsConfigurationSource}，
	 * 以及用于对请求应用匹配到的 {@link CorsConfiguration} 的自定义
	 * {@link CorsProcessor}。
	 * @see UrlBasedCorsConfigurationSource
	 */
	public CorsWebFilter(CorsConfigurationSource configSource, CorsProcessor processor) {
		Assert.notNull(configSource, "CorsConfigurationSource must not be null");
		Assert.notNull(processor, "CorsProcessor must not be null");
		this.configSource = configSource;
		this.processor = processor;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		CorsConfiguration corsConfiguration = this.configSource.getCorsConfiguration(exchange);
		boolean isValid = this.processor.process(corsConfiguration, exchange);
		if (!isValid || CorsUtils.isPreFlightRequest(request)) {
			return Mono.empty();
		}
		return chain.filter(exchange);
	}

}
