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

import org.springframework.web.server.ServerWebExchange;

/**
 * CORS 预检请求的处理器。
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 * @see PreFlightRequestWebFilter
 */
public interface PreFlightRequestHandler {

	/**
	 * 通过查找并应用与预期实际请求匹配的 CORS 配置来处理预检请求。
	 * 处理完成后，应使用 CORS 头更新响应，或以
	 * {@link org.springframework.http.HttpStatus#FORBIDDEN} 拒绝请求。
	 * @param exchange 请求对应的交换
	 * @return 一个完成句柄
	 */
	Mono<Void> handlePreFlight(ServerWebExchange exchange);

}
