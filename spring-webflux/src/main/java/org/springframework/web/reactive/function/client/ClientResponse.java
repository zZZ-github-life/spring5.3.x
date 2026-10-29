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

package org.springframework.web.reactive.function.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;

/**
 * 表示由 {@link WebClient} 以及 {@link ExchangeFunction} 返回的 HTTP 响应。提供对响应状态和头的访问，以及消费响应正文的方法。
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ClientResponse {

	/**
	 * 以 {@link HttpStatus} 枚举值形式返回 HTTP 状态码。
	 * @return the HTTP status as an HttpStatus enum value (never {@code null})
	 * @throws IllegalArgumentException in case of an unknown HTTP status code
	 * @since #getRawStatusCode()
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus statusCode();

	/**
	 * 返回此响应的（可能非标准的）状态码。
	 * @return the HTTP status as an integer value
	 * @since 5.1
	 * @see #statusCode()
	 * @see HttpStatus#resolve(int)
	 */
	int rawStatusCode();

	/**
	 * 返回此响应的头。
	 */
	Headers headers();

	/**
	 * 返回此响应的 cookie。
	 */
	MultiValueMap<String, ResponseCookie> cookies();

	/**
	 * 返回用于转换此响应正文的策略。
	 */
	ExchangeStrategies strategies();

	/**
	 * 使用给定的 {@code BodyExtractor} 提取正文。
	 * @param extractor the {@code BodyExtractor} that reads from the response
	 * @param <T> the type of the body returned
	 * @return the extracted body
	 */
	<T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor);

	/**
	 * 将正文提取为 {@code Mono}。
	 * @param elementClass the class of element in the {@code Mono}
	 * @param <T> the element type
	 * @return a mono containing the body of the given type {@code T}
	 */
	<T> Mono<T> bodyToMono(Class<? extends T> elementClass);

	/**
	 * 将正文提取为 {@code Mono}。
	 * @param elementTypeRef the type reference of element in the {@code Mono}
	 * @param <T> the element type
	 * @return a mono containing the body of the given type {@code T}
	 */
	<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef);

	/**
	 * 将正文提取为 {@code Flux}。
	 * @param elementClass the class of elements in the {@code Flux}
	 * @param <T> the element type
	 * @return a flux containing the body of the given type {@code T}
	 */
	<T> Flux<T> bodyToFlux(Class<? extends T> elementClass);

	/**
	 * 将正文提取为 {@code Flux}。
	 * @param elementTypeRef the type reference of elements in the {@code Flux}
	 * @param <T> the element type
	 * @return a flux containing the body of the given type {@code T}
	 */
	<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef);

	/**
	 * 释放此响应的正文。
	 * @return a completion signal
	 * @since 5.2
	 * @see org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer)
	 */
	Mono<Void> releaseBody();

	/**
	 * 将此响应返回为延迟的 {@code ResponseEntity}。
	 * @param bodyClass the expected response body type
	 * @param <T> response body type
	 * @return {@code Mono} with the {@code ResponseEntity}
	 */
	<T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass);

	/**
	 * 将此响应返回为延迟的 {@code ResponseEntity}。
	 * @param bodyTypeReference a type reference describing the expected response body type
	 * @param <T> response body type
	 * @return {@code Mono} with the {@code ResponseEntity}
	 */
	<T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference);

	/**
	 * 将此响应返回为延迟的 {@code ResponseEntity} 列表。
	 * @param elementClass the expected response body list element class
	 * @param <T> the type of elements in the list
	 * @return {@code Mono} with the list of {@code ResponseEntity}s
	 */
	<T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass);

	/**
	 * 将此响应返回为延迟的 {@code ResponseEntity} 列表。
	 * @param elementTypeRef the expected response body list element reference type
	 * @param <T> the type of elements in the list
	 * @return {@code Mono} with the list of {@code ResponseEntity}s
	 */
	<T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef);

	/**
	 * 将此响应返回为包含状态和头但没有正文的延迟 {@code ResponseEntity}。调用此方法将{@linkplain #releaseBody() 释放}响应正文。
	 * @return {@code Mono} with the bodiless {@code ResponseEntity}
	 * @since 5.2
	 */
	Mono<ResponseEntity<Void>> toBodilessEntity();

	/**
	 * 创建包含响应状态、头、正文以及原始请求的 {@link WebClientResponseException}。
	 * @return a {@code Mono} with the created exception
	 * @since 5.2
	 */
	Mono<WebClientResponseException> createException();

	/**
	 * 返回用于关联此交换消息的日志消息前缀。
	 * <p>该前缀基于 {@linkplain ClientRequest#logPrefix()}，而后者又基于 {@link ClientRequest#LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} 请求属性的值，并进一步用 "[" 和 "]" 包围。
	 * @return the log message prefix or an empty String if the
	 * {@link ClientRequest#LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} is not set
	 * @since 5.2.3
	 */
	String logPrefix();

	/**
	 * 返回用于修改此响应的构建器，例如更改状态、头、cookie，以及替换或转换正文。
	 * @return a builder to mutate the response with
	 * @since 5.3
	 */
	default Builder mutate() {
		return new DefaultClientResponseBuilder(this, true);
	}


	// Static builder methods

	/**
	 * 使用给定响应的状态、头和 cookie 创建构建器。
	 * <p><strong>注意：</strong>返回的构建器中的正文默认为 {@link Flux#empty()}。要沿用原始响应的正文，请使用 {@code otherResponse.bodyToFlux(DataBuffer.class)}，或直接使用基于实例的 {@link #mutate()} 方法。
	 * @param other the response to copy the status, headers, and cookies from
	 * @return the created builder
	 * @deprecated as of 5.3 in favor of the instance based {@link #mutate()}.
	 */
	@Deprecated
	static Builder from(ClientResponse other) {
		return new DefaultClientResponseBuilder(other, false);
	}

	/**
	 * 使用给定状态码并采用默认正文读取策略创建响应构建器。
	 * @param statusCode the status code
	 * @return the created builder
	 */
	static Builder create(HttpStatus statusCode) {
		return create(statusCode, ExchangeStrategies.withDefaults());
	}

	/**
	 * 使用给定状态码和正文读取策略创建响应构建器。
	 * @param statusCode the status code
	 * @param strategies the strategies
	 * @return the created builder
	 */
	static Builder create(HttpStatus statusCode, ExchangeStrategies strategies) {
		return new DefaultClientResponseBuilder(strategies).statusCode(statusCode);
	}

	/**
	 * 使用给定原始状态码和正文读取策略创建响应构建器。
	 * @param statusCode the status code
	 * @param strategies the strategies
	 * @return the created builder
	 * @since 5.1.9
	 */
	static Builder create(int statusCode, ExchangeStrategies strategies) {
		return new DefaultClientResponseBuilder(strategies).rawStatusCode(statusCode);
	}

	/**
	 * 使用给定状态码和消息正文读取器创建响应构建器。
	 * @param statusCode the status code
	 * @param messageReaders the message readers
	 * @return the created builder
	 */
	static Builder create(HttpStatus statusCode, List<HttpMessageReader<?>> messageReaders) {
		return create(statusCode, new ExchangeStrategies() {
			@Override
			public List<HttpMessageReader<?>> messageReaders() {
				return messageReaders;
			}
			@Override
			public List<HttpMessageWriter<?>> messageWriters() {
				// not used in the response
				return Collections.emptyList();
			}
		});
	}


	/**
	 * 表示 HTTP 响应的头。
	 * @see ClientResponse#headers()
	 */
	interface Headers {

		/**
		 * 返回正文的字节长度，由 {@code Content-Length} 头指定。
		 */
		OptionalLong contentLength();

		/**
		 * 返回正文的{@linkplain MediaType 媒体类型}，由 {@code Content-Type} 头指定。
		 */
		Optional<MediaType> contentType();

		/**
		 * 返回给定名称头的头值（如果有）。
		 * <p>如果未找到头值，则返回空列表。
		 * @param headerName the header name
		 */
		List<String> header(String headerName);

		/**
		 * 将头作为 {@link HttpHeaders} 实例返回。
		 */
		HttpHeaders asHttpHeaders();
	}


	/**
	 * 定义响应的构建器。
	 */
	interface Builder {

		/**
		 * 设置响应的状态码。
		 * @param statusCode the new status code
		 * @return this builder
		 */
		Builder statusCode(HttpStatus statusCode);

		/**
		 * 设置响应的原始状态码。
		 * @param statusCode the new status code
		 * @return this builder
		 * @since 5.1.9
		 */
		Builder rawStatusCode(int statusCode);

		/**
		 * 在给定名称下添加给定的头值。
		 * @param headerName the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此响应的头。
		 * <p>提供给消费者的头是“实时”的，因此消费者可用于{@linkplain HttpHeaders#set(String, String) 覆盖}现有头值、{@linkplain HttpHeaders#remove(Object) 删除}值，或使用任何其他 {@link HttpHeaders} 方法。
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 添加具有给定名称和值的 cookie。
		 * @param name the cookie name
		 * @param values the cookie value(s)
		 * @return this builder
		 */
		Builder cookie(String name, String... values);

		/**
		 * 使用给定的消费者操作此响应的 cookie。
		 * <p>提供给消费者的映射是“实时”的，因此消费者可用于{@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 cookie 值、{@linkplain MultiValueMap#remove(Object) 删除}值，或使用任何其他 {@link MultiValueMap} 方法。
		 * @param cookiesConsumer a function that consumes the cookies map
		 * @return this builder
		 */
		Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * 如果构建器中已设置正文，则转换响应正文。
		 * @param transformer the transformation function to use
		 * @return this builder
		 * @since 5.3
		 */
		Builder body(Function<Flux<DataBuffer>, Flux<DataBuffer>> transformer);

		/**
		 * 设置响应正文。
		 * <p><strong>注意：</strong>此方法将排空构建器中已设置的现有正文。
		 * @param body the new body to use
		 * @return this builder
		 */
		Builder body(Flux<DataBuffer> body);

		/**
		 * 将响应正文设置为给定字符串的 UTF-8 编码字节。
		 * <p><strong>注意：</strong>此方法将排空构建器中已设置的现有正文。
		 * @param body the new body.
		 * @return this builder
		 */
		Builder body(String body);

		/**
		 * 设置与响应关联的请求。
		 * @param request the request
		 * @return this builder
		 * @since 5.2
		 */
		Builder request(HttpRequest request);

		/**
		 * 构建响应。
		 */
		ClientResponse build();
	}

}
