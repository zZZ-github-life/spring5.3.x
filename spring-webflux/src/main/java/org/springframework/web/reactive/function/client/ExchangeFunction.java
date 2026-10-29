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

import reactor.core.publisher.Mono;

/**
 * 表示将{@linkplain ClientRequest 请求}交换为（延迟的）{@linkplain ClientResponse 响应}的函数。可作为 {@link WebClient} 的替代方案。
 *
 * <p>例如：
 * <pre class="code">
 * ExchangeFunction exchangeFunction =
 *         ExchangeFunctions.create(new ReactorClientHttpConnector());
 *
 * URI url = URI.create("https://example.com/resource");
 * ClientRequest request = ClientRequest.create(HttpMethod.GET, url).build();
 *
 * Mono&lt;String&gt; bodyMono = exchangeFunction
 *     .exchange(request)
 *     .flatMap(response -&gt; response.bodyToMono(String.class));
 * </pre>
 * @author Arjen Poutsma
 * @since 5.0
 */
@FunctionalInterface
public interface ExchangeFunction {

	/**
	 * 将给定请求交换为 {@link ClientResponse} 的 promise。
	 *
	 * <p><strong>注意：</strong>当从以某种方式处理响应的 {@link ExchangeFilterFunction} 中调用此方法时，必须格外小心，始终消费其内容，或以其他方式将其向下游传播以供进一步处理（例如由 {@link WebClient} 处理）。详情请参阅参考文档。
	 * @param request the request to exchange
	 * @return the delayed response
	 */
	Mono<ClientResponse> exchange(ClientRequest request);

	/**
	 * 使用给定的 {@code ExchangeFilterFunction} 过滤交换函数，生成过滤后的 {@code ExchangeFunction}。
	 * @param filter the filter to apply to this exchange
	 * @return the filtered exchange
	 * @see ExchangeFilterFunction#apply(ExchangeFunction)
	 */
	default ExchangeFunction filter(ExchangeFilterFunction filter) {
		return filter.apply(this);
	}

}
