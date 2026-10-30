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
	 * @param name 属性名称
	 * @return 属性值
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
	 * @return 日志消息前缀；如果未设置
	 * {@link #LOG_ID_ATTRIBUTE LOG_ID_ATTRIBUTE}，则为空字符串。
	 * @since 5.1
	 */
	String logPrefix();

	/**
	 * 将此请求写入给定的 {@link ClientHttpRequest}。
	 * @param request 要写入的客户端 HTTP 请求
	 * @param strategies 写入时要使用的策略
	 * @return {@code Mono<Void>}，用于指示写入何时完成
	 */
	Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies);


	// Static builder methods

	/**
	 * 创建使用给定请求的 HTTP 方法、url、头、cookie、属性和正文初始化的构建器。
	 * @param other 要从中复制的请求
	 * @return 构建器实例
	 */
	static Builder from(ClientRequest other) {
		return new DefaultClientRequestBuilder(other);
	}

	/**
	 * 使用给定的 HTTP 方法和 url 创建构建器。
	 * @param method HTTP 方法（GET、POST 等）
	 * @param url URL（作为 URI 实例）
	 * @return 创建的构建器
	 * @deprecated in favor of {@link #create(HttpMethod, URI)}
	 */
	@Deprecated
	static Builder method(HttpMethod method, URI url) {
		return new DefaultClientRequestBuilder(method, url);
	}

	/**
	 * 使用给定的 HTTP 方法和 url 创建请求构建器。
	 * @param method HTTP 方法（GET、POST 等）
	 * @param url URL（作为 URI 实例）
	 * @return 创建的构建器
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
		 * @param method 新的方法
		 * @return 此构建器
		 * @since 5.0.1
		 */
		Builder method(HttpMethod method);

		/**
		 * 设置请求的 url。
		 * @param url 新的 URL
		 * @return 此构建器
		 * @since 5.0.1
		 */
		Builder url(URI url);

		/**
		 * 在给定名称下添加给定的头值。
		 * @param headerName 头名称
		 * @param headerValues 头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此请求的头。提供给消费者的头是“实时”的，因此消费者可用于{@linkplain HttpHeaders#set(String, String) 覆盖}现有头值、{@linkplain HttpHeaders#remove(Object) 删除}值，或使用任何其他 {@link HttpHeaders} 方法。
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
		 * 使用给定的消费者操作此请求的 cookie。提供给消费者的映射是“实时”的，因此消费者可用于{@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 cookie 值、{@linkplain MultiValueMap#remove(Object) 删除} cookie，或使用任何其他 {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 消费 Cookie 映射的函数
		 * @return 此构建器
		 */
		Builder cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * 将请求正文设置为给定的 {@code BodyInserter}。
		 * @param inserter 写入请求的 {@code BodyInserter}
		 * @return 此构建器
		 */
		Builder body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * 将请求正文设置为给定的 {@code Publisher} 并返回该构建器。
		 * @param publisher 要写入请求的 {@code Publisher}
		 * @param elementClass publisher 中包含的元素的类
		 * @param <S> publisher 中包含的元素的类型
		 * @param <P> {@code Publisher} 类型
		 * @return 构建的请求
		 */
		<S, P extends Publisher<S>> Builder body(P publisher, Class<S> elementClass);

		/**
		 * 将请求正文设置为给定的 {@code Publisher} 并返回该构建器。
		 * @param publisher 要写入请求的 {@code Publisher}
		 * @param typeReference 描述 publisher 中包含元素的类型引用
		 * @param <S> publisher 中包含的元素的类型
		 * @param <P> {@code Publisher} 类型
		 * @return 构建的请求
		 */
		<S, P extends Publisher<S>> Builder body(P publisher, ParameterizedTypeReference<S> typeReference);

		/**
		 * 将具有给定名称的属性设置为给定值。
		 * @param name 要添加的属性的名称
		 * @param value 要添加的属性的值
		 * @return 此构建器
		 */
		Builder attribute(String name, Object value);

		/**
		 * 使用给定的消费者操作请求属性。提供给消费者的属性是“实时”的，因此消费者可用于检查属性、删除属性或使用映射提供的任何其他方法。
		 * @param attributesConsumer 消费属性的函数
		 * @return 此构建器
		 */
		Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 用于访问 {@link ClientHttpRequest} 的回调，后者进而提供对底层 HTTP 库原生请求的访问。这对于设置底层库所暴露的高级按请求选项可能很有用。
		 * @param requestConsumer 用于访问 {@code ClientHttpRequest} 的消费者
		 * @return 此构建器
		 * @since 5.3
		 */
		Builder httpRequest(Consumer<ClientHttpRequest> requestConsumer);

		/**
		 * 构建请求。
		 */
		ClientRequest build();
	}

}
