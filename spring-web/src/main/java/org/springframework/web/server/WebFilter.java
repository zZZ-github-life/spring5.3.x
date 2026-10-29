/*
 * Copyright 2002-2015 the original author or authors.
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

/**
 * 用于拦截式、链式处理 Web 请求的契约，
 * 可用于实现横切性的、与具体应用无关的需求，例如安全性、超时等。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebFilter {

	/**
	 * 处理 Web 请求，并（可选地）通过给定的 {@link WebFilterChain}
	 * 委托给下一个 {@code WebFilter}。
	 * @param exchange the current server exchange
	 * @param chain provides a way to delegate to the next filter
	 * @return {@code Mono<Void>} to indicate when request processing is complete
	 */
	Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain);

}
