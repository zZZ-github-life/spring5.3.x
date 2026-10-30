/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.mock.http.client.reactive;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.AbstractClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link ClientHttpRequest} 的模拟实现。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockClientHttpRequest extends AbstractClientHttpRequest {

	private final HttpMethod httpMethod;

	private final URI url;

	private Flux<DataBuffer> body = Flux.error(
			new IllegalStateException("The body is not set. " +
					"Did handling complete with success? Is a custom \"writeHandler\" configured?"));

	private Function<Flux<DataBuffer>, Mono<Void>> writeHandler;


	public MockClientHttpRequest(HttpMethod httpMethod, String urlTemplate, Object... vars) {
		this(httpMethod, UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(vars).encode().toUri());
	}

	public MockClientHttpRequest(HttpMethod httpMethod, URI url) {
		this.httpMethod = httpMethod;
		this.url = url;
		this.writeHandler = body -> {
			this.body = body.cache();
			return this.body.then();
		};
	}


	/**
	 * 配置用于写入请求体的自定义处理器。
	 *
	 * <p>默认写入处理器会消费并缓存请求体，以便后续访问，
	 * 例如在测试断言中。当请求体是无限流时，可使用此属性。
	 * @param writeHandler 要使用的写处理器，在正文被“写入”（即被消费）后
	 * 返回 {@code Mono<Void>}。
	 */
	public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
		Assert.notNull(writeHandler, "'writeHandler' is required");
		this.writeHandler = writeHandler;
	}


	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public URI getURI() {
		return this.url;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return DefaultDataBufferFactory.sharedInstance;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest() {
		return (T) this;
	}

	@Override
	protected void applyHeaders() {
	}

	@Override
	protected void applyCookies() {
		getCookies().values().stream().flatMap(Collection::stream)
				.forEach(cookie -> getHeaders().add(HttpHeaders.COOKIE, cookie.toString()));
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return doCommit(() -> Mono.defer(() -> this.writeHandler.apply(Flux.from(body))));
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return writeWith(Flux.from(body).flatMap(p -> p));
	}

	@Override
	public Mono<Void> setComplete() {
		return writeWith(Flux.empty());
	}


	/**
	 * 返回请求体；如果从未设置请求体或配置了 {@link #setWriteHandler}，
	 * 则返回一个错误流。
	 */
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	/**
	 * 聚合响应数据，并使用 "Content-Type" 的字符集
	 * （默认为 "UTF-8"）将其转换为 String。
	 */
	public Mono<String> getBodyAsString() {

		Charset charset = Optional.ofNullable(getHeaders().getContentType()).map(MimeType::getCharset)
				.orElse(StandardCharsets.UTF_8);

		return DataBufferUtils.join(getBody())
				.map(buffer -> {
					String s = buffer.toString(charset);
					DataBufferUtils.release(buffer);
					return s;
				})
				.defaultIfEmpty("");
	}

}
