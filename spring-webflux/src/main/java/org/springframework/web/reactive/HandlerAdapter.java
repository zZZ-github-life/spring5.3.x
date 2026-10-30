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

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * 将 {@link DispatcherHandler} 与调用处理器的具体细节解耦的契约，使得支持任意处理器类型成为可能。
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HandlerAdapter {

	/**
	 * 此 {@code HandlerAdapter} 是否支持给定的 {@code handler}。
	 * @param handler 要检查的处理器对象
	 * @return 该处理器是否受支持
	 */
	boolean supports(Object handler);

	/**
	 * 使用给定的处理器处理请求。
	 * <p>鼓励实现类按顺序处理调用处理器时产生的异常，并在必要时返回表示错误响应的替代结果。
	 * <p>此外，由于异步的 {@code HandlerResult} 可能在后续结果处理过程中产生错误，因此也鼓励实现类在 {@code HandlerResult} 上{@link HandlerResult#setExceptionHandler(Function) 设置异常处理器}，以便在结果处理之后也能应用该处理器。
	 * @param exchange 当前的服务器交换
	 * @param handler 选定的处理器，必须事先通过 {@link #supports(Object)} 进行检查
	 * @return {@link Mono}，发出单个 {@code HandlerResult}；如果请求
	 * 已被完全处理且无需进一步处理，则不发出任何值
	 */
	Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler);

}
