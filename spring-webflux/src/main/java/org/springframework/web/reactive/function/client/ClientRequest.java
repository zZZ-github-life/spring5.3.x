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

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;

/**
 * 表示由 {@link ExchangeFunction} 执行的类型化、不可变的客户端 HTTP 请求。可以通过静态构建器方法创建此接口的实例。
 *
 * <p>请注意，应用通常通过 {@link WebClient} 执行请求，而不是直接使用此类。
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ClientRequest {

	/**
	 * 某个 {@link #attributes() 属性}的名称，其值可用于关联此请求的日志消息。使用 {@link #logPrefix()} 获取基于该属性的格式一致的前缀。
	 * @since 5.1
	 * @see #logPrefix()
	 */
	String LOG_ID_ATTRIBUTE = ClientRequest.class.getName() + ".LOG_ID";


	/**
	 * 返回 HTTP 方法。
	 */
	HttpMethod method();

	/**
	 * 返回请求 URI。
	 */
	URI url();

	/**
	 * 返回此请求的头。
	 */
	HttpHeaders headers();

	/**
	 * 返回此请求的 cookie。
	 */
	MultiValueMap<String, String> cookies();

	/**
	 * 返回此请求的正文插入器。
	 */
	BodyInserter<?, ? super ClientHttpRequest> body();

	/**
	 * 如果存在，返回请求属性值。
	 * @param name the attribute name
	 * @return the attribute value
	 */
	default Optional<Object> attribute(String name) {
		return Optional.ofNullable(attributes().get(name));
	}

	/**
	 * 返回此请求的属性。
	 */
	Map<String, Object> attributes();

	/**
	 * 返回配置用于访问 {@link ClientHttpRequest} 的消费者。
	 * @since 5.3
	 */
	@Nullable
	Consumer<ClientHttpRequest> httpRequest();


	/**
	 * 返回用于关联此请求消息的日志消息前缀。该前缀基于 {@link #LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} 属性的值，并用 "[" 和 "]" 包围。
	 * @return the log message prefix or an empty String if the
	 * {@link #LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE} is not set.
	 * @since 5.1
	 */
	String logPrefix();

	/**
	 * 将此请求写入给定的 {@link ClientHttpRequest}。
	 * @param request the client http request to write to
	 * @param strategies the strategies to use when writing
	 * @return {@code Mono<Void>} to indicate when writing is complete
	 */
	Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies);


	// Static builder methods

	/**
	 * 创建使用给定请求的 HTTP 方法、url、头、cookie、属性和正文初始化的构建器。
	 * @param other the request to copy from
	 * @return the builder instance
	 */
	static Builder from(ClientRequest other) {
		return new DefaultClientRequestBuilder(other);
	}

	/**
	 * 使用给定的 HTTP 方法和 url 创建构建器。
	 * @param method the HTTP method (GET, POST, etc)
	 * @param url the url (as a URI instance)
	 * @return the created builder
	 * @deprecated in favor of {@link #create(HttpMethod, URI)}
	 */
	@Deprecated
	static Builder method(HttpMethod method, URI url) {
		return new DefaultClientRequestBuilder(method, url);
	}

	/**
	 * 使用给定的 HTTP 方法和 url 创建请求构建器。
	 * @param method the HTTP method (GET, POST, etc)
	 * @param url the url (as a URI instance)
	 * @return the created builder
	 */
	static Builder create(HttpMethod method, URI url) {
		return new DefaultClientRequestBuilder(method, url);
	}


	/**
	 * 定义请求的构建器。
	 */
	interface Builder {

		/**
		 * 设置请求的方法。
		 * @param method the new method
		 * @return this builder
		 * @since 5.0.1
		 */
		Builder method(HttpMethod method);

		/**
		 * 设置请求的 url。
		 * @param url the new url
		 * @return this builder
		 * @since 5.0.1
		 */
		Builder url(URI url);

		/**
		 * 在给定名称下添加给定的头值。
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此请求的头。提供给消费者的头是“实时”的，因此消费者可用于{@linkplain HttpHeaders#set(String, String) 覆盖}现有头值、{@linkplain HttpHeaders#remove(Object) 删除}值，或使用任何其他 {@link HttpHeaders} 方法。
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
		 * 使用给定的消费者操作此请求的 cookie。提供给消费者的映射是“实时”的，因此消费者可用于{@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 cookie 值、{@linkplain MultiValueMap#remove(Object) 删除} cookie，或使用任何其他 {@link MultiValueMap} 方法。
		 * @param cookiesConsumer a function that consumes the cookies map
		 * @return this builder
		 */
		Builder cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * 将请求正文设置为给定的 {@code BodyInserter}。
		 * @param inserter the {@code BodyInserter} that writes to the request
		 * @return this builder
		 */
		Builder body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * 将请求正文设置为给定的 {@code Publisher} 并返回该构建器。
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementClass the class of elements contained in the publisher
		 * @param <S> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return the built request
		 */
		<S, P extends Publisher<S>> Builder body(P publisher, Class<S> elementClass);

		/**
		 * 将请求正文设置为给定的 {@code Publisher} 并返回该构建器。
		 * @param publisher the {@code Publisher} to write to the request
		 * @param typeReference a type reference describing the elements contained in the publisher
		 * @param <S> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return the built request
		 */
		<S, P extends Publisher<S>> Builder body(P publisher, ParameterizedTypeReference<S> typeReference);

		/**
		 * 将具有给定名称的属性设置为给定值。
		 * @param name the name of the attribute to add
		 * @param value the value of the attribute to add
		 * @return this builder
		 */
		Builder attribute(String name, Object value);

		/**
		 * 使用给定的消费者操作请求属性。提供给消费者的属性是“实时”的，因此消费者可用于检查属性、删除属性或使用映射提供的任何其他方法。
		 * @param attributesConsumer a function that consumes the attributes
		 * @return this builder
		 */
		Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 用于访问 {@link ClientHttpRequest} 的回调，后者进而提供对底层 HTTP 库原生请求的访问。这对于设置底层库所暴露的高级按请求选项可能很有用。
		 * @param requestConsumer a consumer to access the
		 * {@code ClientHttpRequest} with
		 * @return this builder
		 * @since 5.3
		 */
		Builder httpRequest(Consumer<ClientHttpRequest> requestConsumer);

		/**
		 * 构建请求。
		 */
		ClientRequest build();
	}

}
