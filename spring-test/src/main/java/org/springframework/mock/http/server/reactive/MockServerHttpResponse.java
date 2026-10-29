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

package org.springframework.mock.http.server.reactive;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * {@link AbstractServerHttpResponse} 的 mock 扩展，用于在没有
 * 实际服务器的情况下进行测试。
 *
 * <p>默认情况下，响应内容在写入时会被完整消费并缓存
 * 以供后续访问，不过也可以设置自定义的
 * {@link #setWriteHandler(Function) writeHandler}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockServerHttpResponse extends AbstractServerHttpResponse {

	private Flux<DataBuffer> body = Flux.error(new IllegalStateException(
			"No content was written nor was setComplete() called on this response."));

	private Function<Flux<DataBuffer>, Mono<Void>> writeHandler;


	public MockServerHttpResponse() {
		this(DefaultDataBufferFactory.sharedInstance);
	}

	public MockServerHttpResponse(DataBufferFactory dataBufferFactory) {
		super(dataBufferFactory);
		this.writeHandler = body -> {
			// Avoid .then() that causes data buffers to be discarded and released
			Sinks.Empty<Void> completion = Sinks.unsafe().empty();
			this.body = body.cache();
			this.body.subscribe(aVoid -> {}, completion::tryEmitError, completion::tryEmitEmpty); // Signals are serialized
			return completion.asMono();
		};
	}


	/**
	 * 配置用于消费响应体的自定义处理器。
	 * <p>默认情况下，响应正文内容会被完整消费并缓存，
	 * 以便在测试中后续访问。使用此选项可以控制
	 * 响应体的消费方式。
	 * @param writeHandler the write handler to use returning {@code Mono<Void>}
	 * when the body has been "written" (i.e. consumed).
	 */
	public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
		Assert.notNull(writeHandler, "'writeHandler' is required");
		this.body = Flux.error(new IllegalStateException("Not available with custom write handler."));
		this.writeHandler = writeHandler;
	}

	@Override
	public <T> T getNativeResponse() {
		throw new IllegalStateException("This is a mock. No running server, no native response.");
	}


	@Override
	protected void applyStatusCode() {
	}

	@Override
	protected void applyHeaders() {
	}

	@Override
	protected void applyCookies() {
		for (List<ResponseCookie> cookies : getCookies().values()) {
			for (ResponseCookie cookie : cookies) {
				getHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
			}
		}
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
		return this.writeHandler.apply(Flux.from(body));
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(
			Publisher<? extends Publisher<? extends DataBuffer>> body) {

		return this.writeHandler.apply(Flux.from(body).concatMap(Flux::from));
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit(() -> Mono.defer(() -> this.writeHandler.apply(Flux.empty())));
	}

	/**
	 * 返回响应体；如果未设置正文，则返回错误流。
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
