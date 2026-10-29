/*
 * Copyright 2002-2022 the original author or authors.
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
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * 用于执行 HTTP 请求的非阻塞、响应式客户端，在 Reactor Netty 等底层 HTTP 客户端库之上提供流畅的响应式 API。
 *
 * <p>使用静态工厂方法 {@link #create()} 或 {@link #create(String)}，或 {@link WebClient#builder()} 来准备实例。
 *
 * <p>有关带响应正文的示例，请参见：
 * <ul>
 * <li>{@link RequestHeadersSpec#retrieve() retrieve()}
 * <li>{@link RequestHeadersSpec#exchangeToMono(Function) exchangeToMono()}
 * <li>{@link RequestHeadersSpec#exchangeToFlux(Function) exchangeToFlux()}
 * </ul>
 * <p>有关带请求正文的示例，请参见：
 * <ul>
 * <li>{@link RequestBodySpec#bodyValue(Object) bodyValue(Object)}
 * <li>{@link RequestBodySpec#body(Publisher, Class) body(Publisher,Class)}
 * </ul>
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
public interface WebClient {

	/**
	 * 开始构建 HTTP GET 请求。
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> get();

	/**
	 * 开始构建 HTTP HEAD 请求。
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> head();

	/**
	 * 开始构建 HTTP POST 请求。
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec post();

	/**
	 * 开始构建 HTTP PUT 请求。
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec put();

	/**
	 * 开始构建 HTTP PATCH 请求。
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec patch();

	/**
	 * 开始构建 HTTP DELETE 请求。
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> delete();

	/**
	 * 开始构建 HTTP OPTIONS 请求。
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> options();

	/**
	 * 开始为给定的 {@code HttpMethod} 构建请求。
	 * @return a spec for specifying the target URL
	 */
	RequestBodyUriSpec method(HttpMethod method);


	/**
	 * 返回用于创建新的 {@code WebClient} 的构建器，其设置从当前 {@code WebClient} 复制。
	 */
	Builder mutate();


	// Static, factory methods

	/**
	 * 默认使用 Reactor Netty 创建新的 {@code WebClient}。
	 * @see #create(String)
	 * @see #builder()
	 */
	static WebClient create() {
		return new DefaultWebClientBuilder().build();
	}

	/**
	 * {@link #create()} 的变体，接受默认基础 URL。更多详情请参见 {@link Builder#baseUrl(String) Builder.baseUrl(String)}。
	 * @param baseUrl the base URI for all requests
	 * @see #builder()
	 */
	static WebClient create(String baseUrl) {
		return new DefaultWebClientBuilder().baseUrl(baseUrl).build();
	}

	/**
	 * 获取 {@code WebClient} 构建器。
	 */
	static WebClient.Builder builder() {
		return new DefaultWebClientBuilder();
	}


	/**
	 * 用于创建 {@link WebClient} 的可变构建器。
	 */
	interface Builder {

		/**
		 * 为请求配置基础 URL。实际上是以下代码的快捷方式：
		 * <p>
		 * <pre class="code">
		 * String baseUrl = "https://abc.go.com/v1";
		 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
		 * WebClient client = WebClient.builder().uriBuilderFactory(factory).build();
		 * </pre>
		 * <p>{@code DefaultUriBuilderFactory} 用于为每个请求准备带有给定基础 URL 的 URL，除非给定 URL 的请求是绝对 URL，此时将忽略基础 URL。
		 * <p><strong>注意：</strong>此方法与 {@link #uriBuilderFactory(UriBuilderFactory)} 互斥。如果同时使用，此处提供的 baseUrl 值将被忽略。
		 * @see DefaultUriBuilderFactory#DefaultUriBuilderFactory(String)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		Builder baseUrl(String baseUrl);

		/**
		 * 配置在使用 {@link Map} 展开 URI 模板时使用的默认 URL 变量值。实际上是以下代码的快捷方式：
		 * <p>
		 * <pre class="code">
		 * Map&lt;String, ?&gt; defaultVars = ...;
		 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		 * factory.setDefaultVariables(defaultVars);
		 * WebClient client = WebClient.builder().uriBuilderFactory(factory).build();
		 * </pre>
		 * <p><strong>注意：</strong>此方法与 {@link #uriBuilderFactory(UriBuilderFactory)} 互斥。如果同时使用，此处提供的 defaultUriVariables 值将被忽略。
		 * @see DefaultUriBuilderFactory#setDefaultUriVariables(Map)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		Builder defaultUriVariables(Map<String, ?> defaultUriVariables);

		/**
		 * 提供预先配置的 {@link UriBuilderFactory} 实例。这是以下快捷属性的替代方案，并会实际覆盖它们：
		 * <ul>
		 * <li>{@link #baseUrl(String)}
		 * <li>{@link #defaultUriVariables(Map)}.
		 * </ul>
		 * @param uriBuilderFactory the URI builder factory to use
		 * @see #baseUrl(String)
		 * @see #defaultUriVariables(Map)
		 */
		Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		/**
		 * 用于指定要添加到每个请求的头的全局选项（如果请求尚不包含此类头）。
		 * @param header the header name
		 * @param values the header values
		 */
		Builder defaultHeader(String header, String... values);

		/**
		 * 提供对目前声明的每个 {@link #defaultHeader(String, String...)} 的访问，并可以添加、替换或删除。
		 * @param headersConsumer the consumer
		 */
		Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 用于指定要添加到每个请求的 cookie 的全局选项（如果请求尚不包含此类 cookie）。
		 * @param cookie the cookie name
		 * @param values the cookie values
		 */
		Builder defaultCookie(String cookie, String... values);

		/**
		 * 提供对目前声明的每个 {@link #defaultCookie(String, String...)} 的访问，并可以添加、替换或删除。
		 * @param cookiesConsumer a function that consumes the cookies map
		 */
		Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * 提供用于自定义每个正在构建的请求的消费者。
		 * @param defaultRequest the consumer to use for modifying requests
		 * @since 5.1
		 */
		Builder defaultRequest(Consumer<RequestHeadersSpec<?>> defaultRequest);

		/**
		 * 将给定过滤器添加到过滤器链的末尾。
		 * @param filter the filter to be added to the chain
		 */
		Builder filter(ExchangeFilterFunction filter);

		/**
		 * 使用给定的消费者操作过滤器。提供给消费者的列表是“实时”的，因此消费者可用于删除过滤器、更改顺序等。
		 * @param filtersConsumer a function that consumes the filter list
		 * @return this builder
		 */
		Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer);

		/**
		 * 配置要使用的 {@link ClientHttpConnector}。这对于插入和/或自定义底层 HTTP 客户端库的选项（例如 SSL）很有用。
		 * <p>默认设置为 {@link org.springframework.http.client.reactive.ReactorClientHttpConnector ReactorClientHttpConnector}。
		 * @param connector the connector to use
		 */
		Builder clientConnector(ClientHttpConnector connector);

		/**
		 * 在{@link #exchangeStrategies(ExchangeStrategies) 底层} {@code ExchangeStrategies} 中为 {@code WebClient} 配置编解码器。
		 * @param configurer the configurer to apply
		 * @since 5.1.13
		 */
		Builder codecs(Consumer<ClientCodecConfigurer> configurer);

		/**
		 * 配置要使用的 {@link ExchangeStrategies}。
		 * <p>在大多数情况下，优先使用 {@link #codecs(Consumer)}，它允许自定义 {@code ExchangeStrategies} 中的编解码器，而不是替换它们。这可确保多方都能参与编解码器配置。
		 * <p>默认设置为 {@link ExchangeStrategies#withDefaults()}。
		 * @param strategies the strategies to use
		 */
		Builder exchangeStrategies(ExchangeStrategies strategies);

		/**
		 * 自定义通过 {@link #exchangeStrategies(ExchangeStrategies)} 配置的策略。此方法适用于多方希望更新 {@code ExchangeStrategies} 的场景。
		 * @deprecated as of 5.1.13 in favor of {@link #codecs(Consumer)}
		 */
		@Deprecated
		Builder exchangeStrategies(Consumer<ExchangeStrategies.Builder> configurer);

		/**
		 * 提供预先配置了 {@link ClientHttpConnector} 和 {@link ExchangeStrategies} 的 {@link ExchangeFunction}。
		 * <p>这是 {@link #clientConnector} 和 {@link #exchangeStrategies(ExchangeStrategies)} 的替代方案，并会实际覆盖它们。
		 * @param exchangeFunction the exchange function to use
		 */
		Builder exchangeFunction(ExchangeFunction exchangeFunction);

		/**
		 * 将给定 {@code Consumer} 应用于此构建器实例。
		 * <p>这对于应用预先打包的自定义配置很有用。
		 * @param builderConsumer the consumer to apply
		 */
		Builder apply(Consumer<Builder> builderConsumer);

		/**
		 * 克隆此 {@code WebClient.Builder}。
		 */
		Builder clone();

		/**
		 * 构建 {@link WebClient} 实例。
		 */
		WebClient build();
	}


	/**
	 * 用于指定请求 URI 的契约。
	 * @param <S> a self reference to the spec type
	 */
	interface UriSpec<S extends RequestHeadersSpec<?>> {

		/**
		 * 使用绝对且完整构造的 {@link URI} 指定 URI。
		 */
		S uri(URI uri);

		/**
		 * 使用 URI 模板和 URI 变量指定请求的 URI。如果为客户端配置了 {@link UriBuilderFactory}（例如带有基础 URI），则将使用它来展开 URI 模板。
		 */
		S uri(String uri, Object... uriVariables);

		/**
		 * 使用 URI 模板和 URI 变量指定请求的 URI。如果为客户端配置了 {@link UriBuilderFactory}（例如带有基础 URI），则将使用它来展开 URI 模板。
		 */
		S uri(String uri, Map<String, ?> uriVariables);

		/**
		 * 以 URI 模板开始，并以从该模板创建的 {@link UriBuilder} 结束来指定 URI。
		 * @since 5.2
		 */
		S uri(String uri, Function<UriBuilder, URI> uriFunction);

		/**
		 * 通过 {@link UriBuilder} 指定 URI。
		 * @see #uri(String, Function)
		 */
		S uri(Function<UriBuilder, URI> uriFunction);
	}


	/**
	 * 用于指定交换前请求头的契约。
	 * @param <S> a self reference to the spec type
	 */
	interface RequestHeadersSpec<S extends RequestHeadersSpec<S>> {

		/**
		 * 设置可接受的{@linkplain MediaType 媒体类型}列表，由 {@code Accept} 头指定。
		 * @param acceptableMediaTypes the acceptable media types
		 * @return this builder
		 */
		S accept(MediaType... acceptableMediaTypes);

		/**
		 * 设置可接受的{@linkplain Charset 字符集}列表，由 {@code Accept-Charset} 头指定。
		 * @param acceptableCharsets the acceptable charsets
		 * @return this builder
		 */
		S acceptCharset(Charset... acceptableCharsets);

		/**
		 * 添加具有给定名称和值的 cookie。
		 * @param name the cookie name
		 * @param value the cookie value
		 * @return this builder
		 */
		S cookie(String name, String value);

		/**
		 * 提供对目前声明的每个 cookie 的访问，并可以添加、替换或删除值。
		 * @param cookiesConsumer the consumer to provide access to
		 * @return this builder
		 */
		S cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * 设置 {@code If-Modified-Since} 头的值。
		 * <p>日期应指定为自格林威治标准时间 1970 年 1 月 1 日以来的毫秒数。
		 * @param ifModifiedSince the new value of the header
		 * @return this builder
		 */
		S ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * 设置 {@code If-None-Match} 头的值。
		 * @param ifNoneMatches the new value of the header
		 * @return this builder
		 */
		S ifNoneMatch(String... ifNoneMatches);

		/**
		 * 在给定名称下添加给定的单个头值。
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 */
		S header(String headerName, String... headerValues);

		/**
		 * 提供对目前声明的每个头的访问，并可以添加、替换或删除值。
		 * @param headersConsumer the consumer to provide access to
		 * @return this builder
		 */
		S headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 将具有给定名称的属性设置为给定值。
		 * @param name the name of the attribute to add
		 * @param value the value of the attribute to add
		 * @return this builder
		 */
		S attribute(String name, Object value);

		/**
		 * 提供对目前声明的每个属性的访问，并可以添加、替换或删除值。
		 * @param attributesConsumer the consumer to provide access to
		 * @return this builder
		 */
		S attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 提供用于填充 Reactor {@code Context} 的函数。
		 * @param contextModifier the function to modify the context with
		 * @since 5.3.1
		 * @deprecated in 5.3.2 to be removed soon after; this method cannot
		 * provide context to downstream (nested or subsequent) requests and is
		 * of limited value.
		 */
		@Deprecated
		S context(Function<Context, Context> contextModifier);

		/**
		 * 用于访问 {@link ClientHttpRequest} 的回调，后者进而提供对底层 HTTP 库原生请求的访问。这对于设置底层库所暴露的高级按请求选项可能很有用。
		 * @param requestConsumer a consumer to access the
		 * {@code ClientHttpRequest} with
		 * @return {@code ResponseSpec} to specify how to decode the body
		 * @since 5.3
		 */
		S httpRequest(Consumer<ClientHttpRequest> requestConsumer);

		/**
		 * 继续声明如何提取响应。例如，提取包含状态、头和正文的 {@link ResponseEntity}：
		 * <p><pre>
		 * Mono&lt;ResponseEntity&lt;Person&gt;&gt; entityMono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .retrieve()
		 *     .toEntity(Person.class);
		 * </pre>
		 * <p>或者如果只关心正文：
		 * <p><pre>
		 * Mono&lt;Person&gt; entityMono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .retrieve()
		 *     .bodyToMono(Person.class);
		 * </pre>
		 * <p>默认情况下，4xx 和 5xx 响应会产生 {@link WebClientResponseException}。要自定义错误处理，请使用 {@link ResponseSpec#onStatus(Predicate, Function) onStatus} 处理器。
		 */
		ResponseSpec retrieve();

		/**
		 * {@link #retrieve()} 的替代方案，通过访问 {@link ClientResponse} 提供更多控制。这对于高级场景很有用，例如根据响应状态以不同方式解码响应：
		 * <p><pre>
		 * Mono&lt;Person&gt; entityMono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchangeToMono(response -&gt; {
		 *         if (response.statusCode().equals(HttpStatus.OK)) {
		 *             return response.bodyToMono(Person.class);
		 *         }
		 *         else {
		 *             return response.createException().flatMap(Mono::error);
		 *         }
		 *     });
		 * </pre>
		 * <p><strong>注意：</strong>返回的 {@code Mono} 完成后，如果响应正文尚未被消费，它将自动释放。如果需要响应内容，所提供的函数必须声明如何解码它。
		 * @param responseHandler the function to handle the response with
		 * @param <V> the type of Object the response will be transformed to
		 * @return a {@code Mono} produced from the response
		 * @since 5.3
		 */
		<V> Mono<V> exchangeToMono(Function<ClientResponse, ? extends Mono<V>> responseHandler);

		/**
		 * {@link #retrieve()} 的替代方案，通过访问 {@link ClientResponse} 提供更多控制。这对于高级场景很有用，例如根据响应状态以不同方式解码响应：
		 * <p><pre>
		 * Flux&lt;Person&gt; entityMono = client.get()
		 *     .uri("/persons")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchangeToFlux(response -&gt; {
		 *         if (response.statusCode().equals(HttpStatus.OK)) {
		 *             return response.bodyToFlux(Person.class);
		 *         }
		 *         else {
		 *             return response.createException().flatMapMany(Mono::error);
		 *         }
		 *     });
		 * </pre>
		 * <p><strong>注意：</strong>返回的 {@code Flux} 完成后，如果响应正文尚未被消费，它将自动释放。如果需要响应内容，所提供的函数必须声明如何解码它。
		 * @param responseHandler the function to handle the response with
		 * @param <V> the type of Objects the response will be transformed to
		 * @return a {@code Flux} of Objects produced from the response
		 * @since 5.3
		 */
		<V> Flux<V> exchangeToFlux(Function<ClientResponse, ? extends Flux<V>> responseHandler);

		/**
		 * 执行 HTTP 请求并返回包含响应状态和头的 {@link ClientResponse}。然后可以使用响应的方法来消费正文：
		 * <p><pre>
		 * Mono&lt;Person&gt; mono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchange()
		 *     .flatMap(response -&gt; response.bodyToMono(Person.class));
		 *
		 * Flux&lt;Person&gt; flux = client.get()
		 *     .uri("/persons")
		 *     .accept(MediaType.APPLICATION_STREAM_JSON)
		 *     .exchange()
		 *     .flatMapMany(response -&gt; response.bodyToFlux(Person.class));
		 * </pre>
		 * <p><strong>注意：</strong>与 {@link #retrieve()} 不同，使用 {@code exchange()} 时，应用有责任消费任何响应内容，无论场景如何（成功、错误、意外数据等）。否则可能导致内存泄漏。有关消费正文的所有可用选项，请参见 {@link ClientResponse}。通常优先使用 {@link #retrieve()}，除非你有充分理由使用 {@code exchange()}，它确实允许在决定如何消费响应或是否消费响应之前检查响应状态和头。
		 * @return a {@code Mono} for the response
		 * @see #retrieve()
		 * @deprecated since 5.3 due to the possibility to leak memory and/or
		 * connections; please, use {@link #exchangeToMono(Function)},
		 * {@link #exchangeToFlux(Function)}; consider also using
		 * {@link #retrieve()} which provides access to the response status
		 * and headers via {@link ResponseEntity} along with error status
		 * handling.
		 */
		@Deprecated
		Mono<ClientResponse> exchange();
	}


	/**
	 * 用于指定交换前请求头和正文的契约。
	 */
	interface RequestBodySpec extends RequestHeadersSpec<RequestBodySpec> {

		/**
		 * 设置正文的字节长度，由 {@code Content-Length} 头指定。
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		RequestBodySpec contentLength(long contentLength);

		/**
		 * 设置正文的{@linkplain MediaType 媒体类型}，由 {@code Content-Type} 头指定。
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		RequestBodySpec contentType(MediaType contentType);

		/**
		 * 使用{@linkplain BodyInserters#fromValue 值插入器}的 {@link #body(BodyInserter)} 快捷方法。例如：
		 * <p><pre class="code">
		 * Person person = ... ;
		 *
		 * Mono&lt;Void&gt; result = client.post()
		 *     .uri("/persons/{id}", id)
		 *     .contentType(MediaType.APPLICATION_JSON)
		 *     .bodyValue(person)
		 *     .retrieve()
		 *     .bodyToMono(Void.class);
		 * </pre>
		 * <p>对于 multipart 请求，可考虑提供使用 {@link org.springframework.http.client.MultipartBodyBuilder MultipartBodyBuilder} 准备的 {@link org.springframework.util.MultiValueMap MultiValueMap}。
		 * @param body the value to write to the request body
		 * @return this builder
		 * @throws IllegalArgumentException if {@code body} is a
		 * {@link Publisher} or producer known to {@link ReactiveAdapterRegistry}
		 * @since 5.2
		 */
		RequestHeadersSpec<?> bodyValue(Object body);

		/**
		 * 使用{@linkplain BodyInserters#fromPublisher Publisher 插入器}的 {@link #body(BodyInserter)} 快捷方法。例如：
		 * <p><pre>
		 * Mono&lt;Person&gt; personMono = ... ;
		 *
		 * Mono&lt;Void&gt; result = client.post()
		 *     .uri("/persons/{id}", id)
		 *     .contentType(MediaType.APPLICATION_JSON)
		 *     .body(personMono, Person.class)
		 *     .retrieve()
		 *     .bodyToMono(Void.class);
		 * </pre>
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementClass the type of elements published
		 * @param <T> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return this builder
		 */
		<T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, Class<T> elementClass);

		/**
		 * {@link #body(Publisher, Class)} 的变体，允许提供带泛型的元素类型信息。
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementTypeRef the type of elements published
		 * @param <T> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return this builder
		 */
		<T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher,
				ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * {@link #body(Publisher, Class)} 的变体，允许使用可通过 {@link ReactiveAdapterRegistry} 解析为 {@link Publisher} 的任何生产者。
		 * @param producer the producer to write to the request
		 * @param elementClass the type of elements produced
		 * @return this builder
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, Class<?> elementClass);

		/**
		 * {@link #body(Publisher, ParameterizedTypeReference)} 的变体，允许使用可通过 {@link ReactiveAdapterRegistry} 解析为 {@link Publisher} 的任何生产者。
		 * @param producer the producer to write to the request
		 * @param elementTypeRef the type of elements produced
		 * @return this builder
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, ParameterizedTypeReference<?> elementTypeRef);

		/**
		 * 使用给定的正文插入器设置请求正文。有关内置 {@link BodyInserter} 实现，请参见 {@link BodyInserters}。
		 * @param inserter the body inserter to use for the request body
		 * @return this builder
		 * @see org.springframework.web.reactive.function.BodyInserters
		 */
		RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * 使用{@linkplain BodyInserters#fromValue 值插入器}的 {@link #body(BodyInserter)} 快捷方法。从 5.2 开始，此方法委托给 {@link #bodyValue(Object)}。
		 * @deprecated as of Spring Framework 5.2 in favor of {@link #bodyValue(Object)}
		 */
		@Deprecated
		RequestHeadersSpec<?> syncBody(Object body);
	}


	/**
	 * 用于指定交换后响应操作的契约。
	 */
	interface ResponseSpec {

		/**
		 * 提供用于将特定错误状态码映射为错误信号并向下游传播（而不是传播响应）的函数。
		 * <p>默认情况下，如果没有匹配的状态处理器，状态码大于等于 400 的响应会映射为 {@link WebClientResponseException}，它由 {@link ClientResponse#createException()} 创建。
		 * <p>要将某个状态码视为正常响应而不作为错误处理，请从函数返回 {@code Mono.empty()}。响应随后将向下游传播以供处理。
		 * <p>要完全忽略错误响应，既不传播响应也不传播错误，请使用 {@link ExchangeFilterFunction 过滤器}，或在下游添加 {@code onErrorResume}，例如：
		 * <pre class="code">
		 * webClient.get()
		 *     .uri("https://abc.com/account/123")
		 *     .retrieve()
		 *     .bodyToMono(Account.class)
		 *     .onErrorResume(WebClientResponseException.class,
		 *          ex -&gt; ex.getRawStatusCode() == 404 ? Mono.empty() : Mono.error(ex));
		 * </pre>
		 * @param statusPredicate to match responses with
		 * @param exceptionFunction to map the response to an error signal
		 * @return this builder
		 * @see ClientResponse#createException()
		 */
		ResponseSpec onStatus(Predicate<HttpStatus> statusPredicate,
				Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction);

		/**
		 * {@link #onStatus(Predicate, Function)} 的变体，使用原始状态码值。这对于自定义状态码很有用。
		 * @param statusCodePredicate to match responses with
		 * @param exceptionFunction to map the response to an error signal
		 * @return this builder
		 * @since 5.1.9
		 */
		ResponseSpec onRawStatus(IntPredicate statusCodePredicate,
				Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction);

		/**
		 * 将正文解码为给定的目标类型。对于错误响应（4xx 或 5xx 状态码），{@code Mono} 会发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 * @param elementClass the type to decode to
		 * @param <T> the target body type
		 * @return the decoded body
		 */
		<T> Mono<T> bodyToMono(Class<T> elementClass);

		/**
		 * 带 {@link ParameterizedTypeReference} 的 {@link #bodyToMono(Class)} 变体。
		 * @param elementTypeRef the type to decode to
		 * @param <T> the target body type
		 * @return the decoded body
		 */
		<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * 将正文解码为包含给定类型元素的 {@link Flux}。对于错误响应（4xx 或 5xx 状态码），{@code Mono} 会发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 * @param elementClass the type of element to decode to
		 * @param <T> the body element type
		 * @return the decoded body
		 */
		<T> Flux<T> bodyToFlux(Class<T> elementClass);

		/**
		 * 带 {@link ParameterizedTypeReference} 的 {@link #bodyToMono(Class)} 变体。
		 * @param elementTypeRef the type of element to decode to
		 * @param <T> the body element type
		 * @return the decoded body
		 */
		<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * 返回 {@code ResponseEntity}，其正文解码为给定类型的对象。对于错误响应（4xx 或 5xx 状态码），{@code Mono} 会发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 * @param bodyClass the expected response body type
		 * @param <T> response body type
		 * @return the {@code ResponseEntity} with the decoded body
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass);

		/**
		 * 带 {@link ParameterizedTypeReference} 的 {@link #bodyToMono(Class)} 变体。
		 * @param bodyTypeReference the expected response body type
		 * @param <T> the response body type
		 * @return the {@code ResponseEntity} with the decoded body
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference);

		/**
		 * 返回 {@code ResponseEntity}，其正文解码为给定类型元素的 {@code List}。对于错误响应（4xx 或 5xx 状态码），{@code Mono} 会发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 * @param elementClass the type of element to decode the target Flux to
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass);

		/**
		 * 带 {@link ParameterizedTypeReference} 的 {@link #toEntity(Class)} 变体。
		 * @param elementTypeRef the type of element to decode the target Flux to
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * 返回 {@code ResponseEntity}，其正文解码为给定类型元素的 {@code Flux}。对于错误响应（4xx 或 5xx 状态码），{@code Mono} 会发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 * <p><strong>注意：</strong>表示正文的 {@code Flux} 必须被订阅，否则相关资源将不会被释放。
		 * @param elementType the type of element to decode the target Flux to
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.3.1
		 */
		<T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(Class<T> elementType);

		/**
		 * 带 {@link ParameterizedTypeReference} 的 {@link #toEntityFlux(Class)} 变体。
		 * @param elementTypeReference the type of element to decode the target Flux to
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.3.1
		 */
		<T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(ParameterizedTypeReference<T> elementTypeReference);

		/**
		 * 带 {@link BodyExtractor} 的 {@link #toEntityFlux(Class)} 变体。
		 * @param bodyExtractor the {@code BodyExtractor} that reads from the response
		 * @param <T> the body element type
		 * @return the {@code ResponseEntity}
		 * @since 5.3.2
		 */
		<T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(BodyExtractor<Flux<T>, ? super ClientHttpResponse> bodyExtractor);

		/**
		 * 返回不带正文的 {@code ResponseEntity}。对于错误响应（4xx 或 5xx 状态码），{@code Mono} 会发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 * @return the {@code ResponseEntity}
		 * @since 5.2
		 */
		Mono<ResponseEntity<Void>> toBodilessEntity();
	}


	/**
	 * 用于指定请求头和请求 URI 的契约。
	 * @param <S> a self reference to the spec type
	 */
	interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>>
			extends UriSpec<S>, RequestHeadersSpec<S> {
	}


	/**
	 * 用于指定请求头、正文和请求 URI 的契约。
	 */
	interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {
	}


}
