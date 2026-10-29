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

package org.springframework.web.server;

import reactor.core.publisher.Mono;

import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * 用于处理 Web 请求的契约。
 *
 * <p>使用 {@link HttpWebHandlerAdapter} 可将 {@code WebHandler} 适配为
 * {@link org.springframework.http.server.reactive.HttpHandler HttpHandler}。
 * {@link WebHttpHandlerBuilder} 提供了一种便捷方式来完成此适配，
 * 同时还可以选择性地配置一个或多个过滤器以及/或者异常处理器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebHandler {

	/**
	 * 处理 Web 服务器交换。
	 * @param exchange the current server exchange
	 * @return {@code Mono<Void>} to indicate when request handling is complete
	 */
	Mono<Void> handle(ServerWebExchange exchange);

}
