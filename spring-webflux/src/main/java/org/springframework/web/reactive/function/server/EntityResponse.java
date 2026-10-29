/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * 公开实体数据的 {@link ServerResponse} 实体专用子类型。
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 * @param <T> the entity type
 */
public interface EntityResponse<T> extends ServerResponse {

	/**
	 * 返回构成此响应的实体。
	 */
	T entity();

	/**
	 * 返回将实体写入输出流的 {@code BodyInserter}。
	 */
	BodyInserter<T, ? super ServerHttpResponse> inserter();


	// Static builder methods

	/**
	 * 使用给定对象创建构建器。
	 * @param body the object that represents the body of the response
	 * @param <T> the type of the body
	 * @return the created builder
	 */
	static <T> Builder<T> fromObject(T body) {
		return new DefaultEntityResponseBuilder<>(body, BodyInserters.fromValue(body));
	}

	/**
	 * 使用给定生产者创建构建器。
	 * @param producer the producer that represents the body of the response
	 * @param elementClass the class of elements contained in the publisher
	 * @return the created builder
	 * @since 5.2
	 */
	static <T> Builder<T> fromProducer(T producer, Class<?> elementClass) {
		return new DefaultEntityResponseBuilder<>(producer,
				BodyInserters.fromProducer(producer, elementClass));
	}

	/**
	 * 使用给定生产者创建构建器。
	 * @param producer the producer that represents the body of the response
	 * @param typeReference the type of elements contained in the producer
	 * @return the created builder
	 * @since 5.2
	 */
	static <T> Builder<T> fromProducer(T producer, ParameterizedTypeReference<?> typeReference) {
		return new DefaultEntityResponseBuilder<>(producer,
				BodyInserters.fromProducer(producer, typeReference));
	}

	/**
	 * 使用给定发布者创建构建器。
	 * @param publisher the publisher that represents the body of the response
	 * @param elementClass the class of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <P> the type of the {@code Publisher}
	 * @return the created builder
	 */
	static <T, P extends Publisher<T>> Builder<P> fromPublisher(P publisher, Class<T> elementClass) {
		return new DefaultEntityResponseBuilder<>(publisher,
				BodyInserters.fromPublisher(publisher, elementClass));
	}

	/**
	 * 使用给定发布者创建构建器。
	 * @param publisher the publisher that represents the body of the response
	 * @param typeReference the type of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <P> the type of the {@code Publisher}
	 * @return the created builder
	 */
	static <T, P extends Publisher<T>> Builder<P> fromPublisher(P publisher,
			ParameterizedTypeReference<T> typeReference) {

		return new DefaultEntityResponseBuilder<>(publisher,
				BodyInserters.fromPublisher(publisher, typeReference));
	}


	/**
	 * 定义 {@code EntityResponse} 的构建器。
	 * @param <T> a self reference to the builder type
	 */
	interface Builder<T> {

		/**
		 * 在给定名称下添加给定的头值。
		 * @param headerName   the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder<T> header(String headerName, String... headerValues);

		/**
		 * 将给定的头复制到实体的头映射中。
		 * @param headers the existing HttpHeaders to copy from
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder<T> headers(HttpHeaders headers);

		/**
		 * 设置 HTTP 状态。
		 * @param status the response status
		 * @return this builder
		 */
		Builder<T> status(HttpStatus status);

		/**
		 * 设置 HTTP 状态。
		 * @param status the response status
		 * @return this builder
		 * @since 5.0.3
		 */
		Builder<T> status(int status);

		/**
		 * 将给定 cookie 添加到响应中。
		 * @param cookie the cookie to add
		 * @return this builder
		 */
		Builder<T> cookie(ResponseCookie cookie);

		/**
		 * 使用给定的消费者操作此响应的 cookie。提供给消费者的 cookie 是“实时”的，因此消费者可用于{@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 cookie、{@linkplain MultiValueMap#remove(Object) 删除} cookie，或使用任何其他 {@link MultiValueMap} 方法。
		 * @param cookiesConsumer a function that consumes the cookies
		 * @return this builder
		 */
		Builder<T> cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法}集合，由 {@code Allow} 头指定。
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(HttpMethod... allowedMethods);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法}集合，由 {@code Allow} 头指定。
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(Set<HttpMethod> allowedMethods);

		/**
		 * 设置正文的实体标签，由 {@code ETag} 头指定。
		 * @param etag the new entity tag
		 * @return this builder
		 * @see HttpHeaders#setETag(String)
		 */
		Builder<T> eTag(String etag);

		/**
		 * 设置资源的最后修改时间，由 {@code Last-Modified} 头指定。
		 * <p>日期应指定为自格林威治标准时间 1970 年 1 月 1 日以来的毫秒数。
		 * @param lastModified the last modified date
		 * @return this builder
		 * @see HttpHeaders#setLastModified(long)
		 */
		Builder<T> lastModified(ZonedDateTime lastModified);

		/**
		 * 设置资源的最后修改时间，由 {@code Last-Modified} 头指定。
		 * <p>日期应指定为自格林威治标准时间 1970 年 1 月 1 日以来的毫秒数。
		 * @param lastModified the last modified date
		 * @return this builder
		 * @since 5.1.4
		 * @see HttpHeaders#setLastModified(long)
		 */
		Builder<T> lastModified(Instant lastModified);

		/**
		 * 设置资源的位置，由 {@code Location} 头指定。
		 * @param location the location
		 * @return this builder
		 * @see HttpHeaders#setLocation(URI)
		 */
		Builder<T> location(URI location);

		/**
		 * 设置资源的缓存指令，由 HTTP 1.1 的 {@code Cache-Control} 头指定。
		 * <p>{@code CacheControl} 实例可以这样构建：{@code CacheControl.maxAge(3600).cachePublic().noTransform()}。
		 * @param cacheControl a builder for cache-related HTTP response headers
		 * @return this builder
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		Builder<T> cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求头名称（例如 "Accept-Language"），将其添加到 "Vary" 响应头，以告知客户端响应会受到内容协商影响，并会因给定请求头的值而变化。仅当响应 "Vary" 头中尚不存在时，才会添加所配置的请求头名称。
		 * @param requestHeaders request header names
		 * @return this builder
		 */
		Builder<T> varyBy(String... requestHeaders);

		/**
		 * 设置正文的字节长度，由 {@code Content-Length} 头指定。
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		Builder<T> contentLength(long contentLength);

		/**
		 * 设置正文的{@linkplain MediaType 媒体类型}，由 {@code Content-Type} 头指定。
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		Builder<T> contentType(MediaType contentType);

		/**
		 * 添加序列化提示（例如 {@link Jackson2CodecSupport#JSON_VIEW_HINT}）以自定义正文的序列化方式。
		 * @param key the hint key
		 * @param value the hint value
		 */
		Builder<T> hint(String key, Object value);

		/**
		 * 使用给定的消费者自定义序列化提示。
		 * @param hintsConsumer a function that consumes the hints
		 * @return this builder
		 * @since 5.1.6
		 */
		Builder<T> hints(Consumer<Map<String, Object>> hintsConsumer);

		/**
		 * 构建响应。
		 * @return the built response
		 */
		Mono<EntityResponse<T>> build();
	}

}
