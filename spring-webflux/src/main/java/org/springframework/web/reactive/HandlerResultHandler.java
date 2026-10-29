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

package org.springframework.web.reactive;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * 处理通常由 {@link HandlerAdapter} 返回的 {@link HandlerResult}。
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HandlerResultHandler {

	/**
	 * 此处理器是否支持给定的 {@link HandlerResult}。
	 * @param result the result object to check
	 * @return whether this object can use the given result
	 */
	boolean supports(HandlerResult result);

	/**
	 * 处理给定的结果，修改响应头并/或向响应写入数据。
	 * @param exchange current server exchange
	 * @param result the result from the handling
	 * @return {@code Mono<Void>} to indicate when request handling is complete.
	 */
	Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result);

}
