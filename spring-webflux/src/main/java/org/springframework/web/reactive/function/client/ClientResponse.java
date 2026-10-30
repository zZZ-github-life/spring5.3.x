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
	 * @return HTTP 状态，以 HttpStatus 枚举值表示（永不为 {@code null}）
	 * @throws IllegalArgumentException 在 HTTP 状态码未知的情况下
	 * @since #getRawStatusCode()
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus statusCode();

	/**
	 * 返回此响应的（可能非标准的）状态码。
	 * @return HTTP 状态，以整数值表示
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
	 * @param extractor 从响应读取的 {@code BodyExtractor}
	 * @param <T> 返回的主体的类型
	 * @return 提取的主体
	 */
	<T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor);

	/**
	 * 将正文提取为 {@code Mono}。
	 * @param elementClass {@code Mono} 中元素的类
	 * @param <T> 元素类型
	 * @return 包含给定类型 {@code T} 的主体的 mono
	 */
	<T> Mono<T> bodyToMono(Class<? extends T> elementClass);

	/**
	 * 将正文提取为 {@code Mono}。
	 * @param elementTypeRef {@code Mono} 中元素的类型引用
	 * @param <T> 元素类型
	 * @return 包含给定类型 {@code T} 的主体的 mono
	 */
	<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef);

	/**
	 * 将正文提取为 {@code Flux}。
	 * @param elementClass {@code Flux} 中元素的类
	 * @param <T> 元素类型
	 * @return 包含给定类型 {@code T} 的主体的 flux
	 */
	<T> Flux<T> bodyToFlux(Class<? extends T> elementClass);

	/**
	 * 将正文提取为 {@code Flux}。
	 * @param elementTypeRef {@code Flux} 中元素的类型引用
	 * @param <T> 元素类型
	 * @return 包含给定类型 {@code T} 的主体的 flux
	 */
	<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef);

	/**
	 * 释放此响应的正文。
	 * @return 完成信号
	 * @since 5.2
	 * @see org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer)
	 */
	Mono<Void> releaseBody();

	/**
	 * 将此响应返回为延迟的 {@code ResponseEntity}。
	 * @param bodyClass 预期的响应主体类型
	 * @param <T> 响应主体类型
	 * @return 包含 {@code ResponseEntity} 的 {@code Mono}
	 */
	<T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass);

	/**
	 * 将此响应返回为延迟的 {@code ResponseEntity}。
	 * @param bodyTypeReference 描述预期响应主体类型的类型引用
	 * @param <T> 响应主体类型
	 * @return 包含 {@code ResponseEntity} 的 {@code Mono}
	 */
	<T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference);

	/**
	 * 将此响应返回为延迟的 {@code ResponseEntity} 列表。
	 * @param elementClass 预期的响应主体列表元素类
	 * @param <T> 列表中元素的类型
	 * @return 包含 {@code ResponseEntity} 列表的 {@code Mono}
	 */
	<T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass);

	/**
	 * 将此响应返回为延迟的 {@code ResponseEntity} 列表。
	 * @param elementTypeRef 预期的响应主体列表元素的引用类型
	 * @param <T> 列表中元素的类型
	 * @return 包含 {@code ResponseEntity} 列表的 {@code Mono}
	 */
	<T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef);

	/**
	 * 将此响应返回为包含状态和头但没有正文的延迟 {@code ResponseEntity}。调用此方法将{@linkplain #releaseBody() 释放}响应正文。
	 * @return 包含无主体 {@code ResponseEntity} 的 {@code Mono}
	 * @since 5.2
	 */
	Mono<ResponseEntity<Void>> toBodilessEntity();

	/**
	 * 创建包含响应状态、头、正文以及原始请求的 {@link WebClientResponseException}。
	 * @return 包含所创建异常的 {@code Mono}
	 * @since 5.2
	 */
	Mono<WebClientResponseException> createException();

	/**
	 * 返回用于关联此交换消息的日志消息前缀。
	 * <p>该前缀基于 {@linkplain ClientRequest#logPrefix()}，而后者又基于 {@link ClientRequest#LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} 请求属性的值，并进一步用 "[" 和 "]" 包围。
	 * @return 日志消息前缀；如果未设置
	 * {@link ClientRequest#LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE}，则为空字符串
	 * @since 5.2.3
	 */
	String logPrefix();

	/**
	 * 返回用于修改此响应的构建器，例如更改状态、头、cookie，以及替换或转换正文。
	 * @return 用于修改响应的构建器
	 * @since 5.3
	 */
	default Builder mutate() {
		return new DefaultClientResponseBuilder(this, true);
	}


	// Static builder methods

	/**
	 * 使用给定响应的状态、头和 cookie 创建构建器。
	 * <p><strong>注意：</strong>返回的构建器中的正文默认为 {@link Flux#empty()}。要沿用原始响应的正文，请使用 {@code otherResponse.bodyToFlux(DataBuffer.class)}，或直接使用基于实例的 {@link #mutate()} 方法。
	 * @param other 要从中复制状态、头和 Cookie 的响应
	 * @return 创建的构建器
	 * @deprecated as of 5.3 in favor of the instance based {@link #mutate()}.
	 */
	@Deprecated
	static Builder from(ClientResponse other) {
		return new DefaultClientResponseBuilder(other, false);
	}

	/**
	 * 使用给定状态码并采用默认正文读取策略创建响应构建器。
	 * @param statusCode 状态码
	 * @return 创建的构建器
	 */
	static Builder create(HttpStatus statusCode) {
		return create(statusCode, ExchangeStrategies.withDefaults());
	}

	/**
	 * 使用给定状态码和正文读取策略创建响应构建器。
	 * @param statusCode 状态码
	 * @param strategies 策略
	 * @return 创建的构建器
	 */
	static Builder create(HttpStatus statusCode, ExchangeStrategies strategies) {
		return new DefaultClientResponseBuilder(strategies).statusCode(statusCode);
	}

	/**
	 * 使用给定原始状态码和正文读取策略创建响应构建器。
	 * @param statusCode 状态码
	 * @param strategies 策略
	 * @return 创建的构建器
	 * @since 5.1.9
	 */
	static Builder create(int statusCode, ExchangeStrategies strategies) {
		return new DefaultClientResponseBuilder(strategies).rawStatusCode(statusCode);
	}

	/**
	 * 使用给定状态码和消息正文读取器创建响应构建器。
	 * @param statusCode 状态码
	 * @param messageReaders 消息读取器
	 * @return 创建的构建器
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
		 * @param headerName 头名称
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
		 * @param statusCode 新的状态码
		 * @return 此构建器
		 */
		Builder statusCode(HttpStatus statusCode);

		/**
		 * 设置响应的原始状态码。
		 * @param statusCode 新的状态码
		 * @return 此构建器
		 * @since 5.1.9
		 */
		Builder rawStatusCode(int statusCode);

		/**
		 * 在给定名称下添加给定的头值。
		 * @param headerName 头名称
		 * @param headerValues 头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此响应的头。
		 * <p>提供给消费者的头是“实时”的，因此消费者可用于{@linkplain HttpHeaders#set(String, String) 覆盖}现有头值、{@linkplain HttpHeaders#remove(Object) 删除}值，或使用任何其他 {@link HttpHeaders} 方法。
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 添加具有给定名称和值的 cookie。
		 * @param name Cookie 名称
		 * @param values Cookie 值
		 * @return 此构建器
		 */
		Builder cookie(String name, String... values);

		/**
		 * 使用给定的消费者操作此响应的 cookie。
		 * <p>提供给消费者的映射是“实时”的，因此消费者可用于{@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 cookie 值、{@linkplain MultiValueMap#remove(Object) 删除}值，或使用任何其他 {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 消费 Cookie 映射的函数
		 * @return 此构建器
		 */
		Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * 如果构建器中已设置正文，则转换响应正文。
		 * @param transformer 要使用的转换函数
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder body(Function<Flux<DataBuffer>, Flux<DataBuffer>> transformer);

		/**
		 * 设置响应正文。
		 * <p><strong>注意：</strong>此方法将排空构建器中已设置的现有正文。
		 * @param body 要使用的新主体
		 * @return 此构建器
		 */
		Builder body(Flux<DataBuffer> body);

		/**
		 * 将响应正文设置为给定字符串的 UTF-8 编码字节。
		 * <p><strong>注意：</strong>此方法将排空构建器中已设置的现有正文。
		 * @param body 新的主体。
		 * @return 此构建器
		 */
		Builder body(String body);

		/**
		 * 设置与响应关联的请求。
		 * @param request 请求
		 * @return 此构建器
		 * @since 5.2
		 */
		Builder request(HttpRequest request);

		/**
		 * 构建响应。
		 */
		ClientResponse build();
	}

}
