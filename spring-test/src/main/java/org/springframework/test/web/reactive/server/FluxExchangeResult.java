/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.util.function.Consumer;

import reactor.core.publisher.Flux;

/**
 * 响应体被解码为 {@code Flux<T>} 但尚未消费的
 * {@code ExchangeResult} 变体。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 响应体中元素的类型
 * @see EntityExchangeResult
 */
public class FluxExchangeResult<T> extends ExchangeResult {

	private final Flux<T> body;


	FluxExchangeResult(ExchangeResult result, Flux<T> body) {
		super(result);
		this.body = body;
	}


	/**
	 * 以解码元素的 {@code Flux<T>} 形式返回响应体。
	 *
	 * <p>随后可以使用“reactor-test”的 {@code StepVerifier} 进一步消费响应体流，
	 * 并在从（可能无限的）流中消费了足够元素后取消：
	 *
	 * <pre>
	 * FluxExchangeResult&lt;Person&gt; result = this.client.get()
	 * .uri("/persons")
	 * .accept(TEXT_EVENT_STREAM)
	 * .exchange()
	 * .expectStatus().isOk()
	 * .expectHeader().contentType(TEXT_EVENT_STREAM)
	 * .expectBody(Person.class)
	 * .returnResult();
	 *
	 * StepVerifier.create(result.getResponseBody())
	 * .expectNext(new Person("Jane"), new Person("Jason"))
	 * .expectNextCount(4)
	 * .expectNext(new Person("Jay"))
	 * .thenCancel()
	 * .verify();
	 * </pre>
	 */
	public Flux<T> getResponseBody() {
		return this.body;
	}

	/**
	 * 在 {@link #assertWithDiagnostics(Runnable)} 中调用给定消费者，
	 * 并将 {@code “this”} 实例传递给它。此方法允许以下操作，
	 * 而无需离开 {@code WebTestClient} 调用链：
	 * <pre class="code">
	 * client.get()
	 * .uri("/persons")
	 * .accept(TEXT_EVENT_STREAM)
	 * .exchange()
	 * .expectStatus().isOk()
	 * .returnResult()
	 * .consumeWith(result -&gt; assertThat(...);
	 * </pre>
	 * @param consumer 用于 {@code "this"} 实例的消费者
	 */
	public void consumeWith(Consumer<FluxExchangeResult<T>> consumer) {
		assertWithDiagnostics(() -> consumer.accept(this));
	}

}
