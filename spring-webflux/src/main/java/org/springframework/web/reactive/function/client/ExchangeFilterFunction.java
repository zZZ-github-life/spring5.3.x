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

package org.springframework.web.reactive.function.client;

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * 表示过滤{@linkplain ExchangeFunction 交换函数}的函数。
 * <p>当 {@code Subscriber} 订阅由 {@code WebClient} 返回的 {@code Publisher} 时，将执行该过滤器。
 * @author Arjen Poutsma
 * @since 5.0
 */
@FunctionalInterface
public interface ExchangeFilterFunction {

	/**
	 * 将此过滤器应用于给定请求和交换函数。
	 * <p>给定的 {@linkplain ExchangeFunction} 表示链中的下一个实体，可通过 {@linkplain ExchangeFunction#exchange(ClientRequest) 调用}它来继续交换，或不调用它以短路该链。
	 *
	 * <p><strong>注意：</strong>当过滤器在调用 {@link ExchangeFunction#exchange} 之后处理响应时，必须格外小心，始终消费其内容，或以其他方式将其向下游传播以供进一步处理（例如由 {@link WebClient} 处理）。详情请参阅参考文档。
	 * @param request 当前请求
	 * @param next 链中的下一个交换函数
	 * @return 过滤后的响应
	 */
	Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next);

	/**
	 * 返回组合过滤器函数，先应用此过滤器，然后应用给定的 {@code "after"} 过滤器。
	 * @param afterFilter 在此过滤器之后应用的过滤器
	 * @return 组合后的过滤器
	 */
	default ExchangeFilterFunction andThen(ExchangeFilterFunction afterFilter) {
		Assert.notNull(afterFilter, "ExchangeFilterFunction must not be null");
		return (request, next) ->
				filter(request, afterRequest -> afterFilter.filter(afterRequest, next));
	}

	/**
	 * 将此过滤器应用于给定的 {@linkplain ExchangeFunction}，生成过滤后的交换函数。
	 * @param exchange 要过滤的交换函数
	 * @return 过滤后的交换函数
	 */
	default ExchangeFunction apply(ExchangeFunction exchange) {
		Assert.notNull(exchange, "ExchangeFunction must not be null");
		return request -> this.filter(request, exchange);
	}

	/**
	 * 将给定的请求处理器函数适配为仅对 {@code ClientRequest} 操作的过滤器函数。
	 * @param processor 请求处理器
	 * @return 生成的过滤器适配器
	 */
	static ExchangeFilterFunction ofRequestProcessor(Function<ClientRequest, Mono<ClientRequest>> processor) {
		Assert.notNull(processor, "ClientRequest Function must not be null");
		return (request, next) -> processor.apply(request).flatMap(next::exchange);
	}

	/**
	 * 将给定的响应处理器函数适配为仅对 {@code ClientResponse} 操作的过滤器函数。
	 * @param processor 响应处理器
	 * @return 生成的过滤器适配器
	 */
	static ExchangeFilterFunction ofResponseProcessor(Function<ClientResponse, Mono<ClientResponse>> processor) {
		Assert.notNull(processor, "ClientResponse Function must not be null");
		return (request, next) -> next.exchange(request).flatMap(processor);
	}

}
