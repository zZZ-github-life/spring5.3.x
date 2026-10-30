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

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.reactivestreams.Publisher;

import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.session.WebSessionManager;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * 用于测试 Web 服务器的客户端，它在内部使用 {@link WebClient} 执行请求，同时提供用于验证响应的流畅 API。此客户端可以通过 HTTP 连接到任何服务器，也可以通过模拟请求和响应对象连接到 WebFlux 应用程序。
 *
 * <p>使用某个 bindToXxx 方法创建实例。例如：
 * <ul>
 * <li>{@link #bindToController(Object...)}
 * <li>{@link #bindToRouterFunction(RouterFunction)}
 * <li>{@link #bindToApplicationContext(ApplicationContext)}
 * <li>{@link #bindToServer()}
 * <li>...
 * </ul>
 *
 * <p><strong>警告</strong>：由于 <a href="https://youtrack.jetbrains.com/issue/KT-5464">类型推断问题</a>，
 * {@code WebTestClient} 目前尚不能在 Kotlin 中使用，该问题预计在 Kotlin 1.3 中修复。您可以关注
 * <a href="https://github.com/spring-projects/spring-framework/issues/20606">gh-20606</a>
 * 了解最新信息。同时，建议的替代方案是直接使用
 * {@link WebClient} 及其 Reactor 和 Spring Kotlin 扩展，
 * 在嵌入式 WebFlux 服务器上执行集成测试。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @author Michał Rowicki
 * @since 5.0
 * @see StatusAssertions
 * @see HeaderAssertions
 * @see JsonPathAssertions
 */
public interface WebTestClient {

	/**
	 * 一个请求头的名称，用于为通过 {@code WebTestClient} 执行的每个请求分配唯一 id。这对于在请求处理的所有阶段（例如来自服务器端组件）在该 id 下存储上下文信息，并在获得 {@link ExchangeResult} 后查找该信息很有用。
	 */
	String WEBTESTCLIENT_REQUEST_ID = "WebTestClient-Request-Id";


	/**
	 * 准备 HTTP GET 请求。
	 * @return 用于指定目标 URL 的规格
	 */
	RequestHeadersUriSpec<?> get();

	/**
	 * 准备 HTTP HEAD 请求。
	 * @return 用于指定目标 URL 的规格
	 */
	RequestHeadersUriSpec<?> head();

	/**
	 * 准备 HTTP POST 请求。
	 * @return 用于指定目标 URL 的规格
	 */
	RequestBodyUriSpec post();

	/**
	 * 准备 HTTP PUT 请求。
	 * @return 用于指定目标 URL 的规格
	 */
	RequestBodyUriSpec put();

	/**
	 * 准备 HTTP PATCH 请求。
	 * @return 用于指定目标 URL 的规格
	 */
	RequestBodyUriSpec patch();

	/**
	 * 准备 HTTP DELETE 请求。
	 * @return 用于指定目标 URL 的规格
	 */
	RequestHeadersUriSpec<?> delete();

	/**
	 * 准备 HTTP OPTIONS 请求。
	 * @return 用于指定目标 URL 的规格
	 */
	RequestHeadersUriSpec<?> options();

	/**
	 * 为指定的 {@code HttpMethod} 准备请求。
	 * @return 用于指定目标 URL 的规格
	 */
	RequestBodyUriSpec method(HttpMethod method);


	/**
	 * 返回用于修改此 Web 测试客户端属性的构建器。
	 */
	Builder mutate();

	/**
	 * 修改 {@link WebTestClient}，应用给定配置器，并构建
	 * 新实例。本质上是以下代码的快捷方式：
	 * <pre>
	 * mutate().apply(configurer).build();
	 * </pre>
	 * @param configurer 要应用的配置器
	 * @return 变更后的测试客户端
	 */
	WebTestClient mutateWith(WebTestClientConfigurer configurer);


	// Static factory methods

	/**
	 * 使用此服务器设置一次测试一个 {@code @Controller}。
	 * 此选项加载 {@link org.springframework.web.reactive.config.EnableWebFlux @EnableWebFlux} 的默认配置。
	 * 提供了构建器方法来自定义 Java 配置。生成的
	 * WebFlux 应用程序将在没有 HTTP 服务器的情况下使用模拟
	 * 请求和响应进行测试。
	 * @param controllers 一个或多个要测试的控制器实例
	 * （指定的 {@code Class} 将被转换为实例）
	 * @return 用于定制服务器和客户端配置的链式 API；
	 * 使用 {@link MockServerSpec#configureClient()} 切换到客户端配置
	 */
	static ControllerSpec bindToController(Object... controllers) {
		return new DefaultControllerSpec(controllers);
	}

	/**
	 * 使用此选项从 {@link RouterFunction} 设置服务器。
	 * 在内部，提供的配置会传递给 {@code RouterFunctions#toWebHandler}。生成的 WebFlux 应用程序
	 * 将在没有 HTTP 服务器的情况下使用模拟请求和响应进行测试。
	 * @param routerFunction 要测试的 RouterFunction
	 * @return 用于定制服务器和客户端配置的链式 API；
	 * 使用 {@link MockServerSpec#configureClient()} 切换到客户端配置
	 */
	static RouterFunctionSpec bindToRouterFunction(RouterFunction<?> routerFunction) {
		return new DefaultRouterFunctionSpec(routerFunction);
	}

	/**
	 * 使用此选项从应用程序的 Spring 配置或其某个子集设置服务器。
	 * 在内部，提供的配置会传递给 {@code WebHttpHandlerBuilder} 以设置请求
	 * 处理链。生成的 WebFlux 应用程序将在没有 HTTP 服务器的情况下
	 * 使用模拟请求和响应进行测试。
	 * <p>考虑使用 TestContext 框架和
	 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}，
	 * 以便高效地将 Spring 配置加载并注入到
	 * 测试类中。
	 * @param applicationContext Spring 上下文
	 * @return 用于定制服务器和客户端配置的链式 API；
	 * 使用 {@link MockServerSpec#configureClient()} 切换到客户端配置
	 */
	static MockServerSpec<?> bindToApplicationContext(ApplicationContext applicationContext) {
		return new ApplicationContextSpec(applicationContext);
	}

	/**
	 * 针对给定 WebHandler 的“模拟”服务器集成测试。
	 * @param webHandler 要测试的处理器
	 * @return 用于定制服务器和客户端配置的链式 API；
	 * 使用 {@link MockServerSpec#configureClient()} 切换到客户端配置
	 */
	static MockServerSpec<?> bindToWebHandler(WebHandler webHandler) {
		return new DefaultMockServerSpec(webHandler);
	}

	/**
	 * 此服务器设置选项允许您通过 Reactor Netty 客户端连接器连接到活动服务器。
	 * <p><pre class="code">
	 * WebTestClient client = WebTestClient.bindToServer()
	 * .baseUrl("http://localhost:8080")
	 * .build();
	 * </pre>
	 * @return 用于定制客户端配置的链式 API
	 */
	static Builder bindToServer() {
		return new DefaultWebTestClientBuilder();
	}

	/**
	 * 带有预配置连接器的 {@link #bindToServer()} 变体。
	 * @return 用于定制客户端配置的链式 API
	 * @since 5.0.2
	 */
	static Builder bindToServer(ClientHttpConnector connector) {
		return new DefaultWebTestClientBuilder(connector);
	}


	/**
	 * 用于设置无服务器测试的基础规范。
	 * @param <B> 对构建器类型的自引用
	 */
	interface MockServerSpec<B extends MockServerSpec<B>> {

		/**
		 * 注册要添加到模拟服务器的 {@link WebFilter} 实例。
		 * @param filter 一个或多个过滤器
		 */
		<T extends B> T webFilter(WebFilter... filter);

		/**
		 * 为模拟服务器提供会话管理器实例。
		 * <p>默认使用
		 * {@link org.springframework.web.server.session.DefaultWebSessionManager
		 * DefaultWebSessionManager} 的实例。
		 * @param sessionManager 要使用的会话管理器
		 */
		<T extends B> T webSessionManager(WebSessionManager sessionManager);

		/**
		 * 用于对模拟服务器设置进行预打包自定义配置的快捷方式。
		 * @param configurer 要应用的配置器
		 */
		<T extends B> T apply(MockServerConfigurer configurer);

		/**
		 * 继续配置并构建测试客户端。
		 */
		Builder configureClient();

		/**
		 * 构建测试客户端的快捷方式。
		 */
		WebTestClient build();
	}


	/**
	 * 用于自定义控制器配置的规范，等价于 {@link WebFluxConfigurer}
	 * 并在内部委托给它。
	 */
	interface ControllerSpec extends MockServerSpec<ControllerSpec> {

		/**
		 * 注册一个或多个用于测试的 {@link org.springframework.web.bind.annotation.ControllerAdvice}
		 * 实例（指定的 {@code Class} 将被转换为实例）。
		 */
		ControllerSpec controllerAdvice(Object... controllerAdvice);

		/**
		 * 自定义内容类型解析。
		 * @see WebFluxConfigurer#configureContentTypeResolver
		 */
		ControllerSpec contentTypeResolver(Consumer<RequestedContentTypeResolverBuilder> consumer);

		/**
		 * 配置 CORS 支持。
		 * @see WebFluxConfigurer#addCorsMappings
		 */
		ControllerSpec corsMappings(Consumer<CorsRegistry> consumer);

		/**
		 * 配置路径匹配选项。
		 * @see WebFluxConfigurer#configurePathMatching
		 */
		ControllerSpec pathMatching(Consumer<PathMatchConfigurer> consumer);

		/**
		 * 配置自定义控制器方法参数的解析器。
		 * @see WebFluxConfigurer#configureHttpMessageCodecs
		 */
		ControllerSpec argumentResolvers(Consumer<ArgumentResolverConfigurer> configurer);

		/**
		 * 配置自定义 HTTP 消息读写器，或覆盖内置的读写器。
		 * @see WebFluxConfigurer#configureHttpMessageCodecs
		 */
		ControllerSpec httpMessageCodecs(Consumer<ServerCodecConfigurer> configurer);

		/**
		 * 注册用于类型转换的格式化器和转换器。
		 * @see WebFluxConfigurer#addFormatters
		 */
		ControllerSpec formatters(Consumer<FormatterRegistry> consumer);

		/**
		 * 配置全局 Validator。
		 * @see WebFluxConfigurer#getValidator()
		 */
		ControllerSpec validator(Validator validator);

		/**
		 * 配置视图解析。
		 * @see WebFluxConfigurer#configureViewResolvers
		 */
		ControllerSpec viewResolvers(Consumer<ViewResolverRegistry> consumer);
	}


	/**
	 * 用于自定义路由函数配置的规范。
	 */
	interface RouterFunctionSpec extends MockServerSpec<RouterFunctionSpec> {

		/**
		 * 配置处理器策略。
		 */
		RouterFunctionSpec handlerStrategies(HandlerStrategies handlerStrategies);
	}


	/**
	 * 用于自定义测试所用 {@link WebClient} 的步骤，
	 * 在内部委托给
	 * {@link org.springframework.web.reactive.function.client.WebClient.Builder
	 * WebClient.Builder}。
	 */
	interface Builder {

		/**
		 * 配置基础 URI，如
		 * {@link org.springframework.web.reactive.function.client.WebClient#create(String)
		 * WebClient.create(String)} 所述。
		 */
		Builder baseUrl(String baseUrl);

		/**
		 * 提供预配置的 {@link UriBuilderFactory} 实例，作为
		 * {@link #baseUrl(String)} 的替代并实际覆盖它。
		 */
		Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		/**
		 * 将给定请求头添加到所有尚未添加该请求头的请求中。
		 * @param headerName 请求头名称
		 * @param headerValues 请求头的值
		 */
		Builder defaultHeader(String headerName, String... headerValues);

		/**
		 * 使用给定消费者操作默认请求头。提供给消费者的请求头是“实时”的，
		 * 因此消费者可以使用它来
		 * {@linkplain HttpHeaders#set(String, String) 覆盖}现有请求头值、
		 * {@linkplain HttpHeaders#remove(Object) 删除}值，或使用其他任意
		 * {@link HttpHeaders} 方法。
		 * @param headersConsumer 使用 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 将给定请求头添加到所有尚未添加该请求头的请求中。
		 * @param cookieName Cookie 名称
		 * @param cookieValues Cookie 的值
		 */
		Builder defaultCookie(String cookieName, String... cookieValues);

		/**
		 * 使用给定消费者操作默认 Cookie。提供给消费者的
		 * 映射是“实时”的，因此消费者可以使用它来
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有请求头值、
		 * {@linkplain MultiValueMap#remove(Object) 删除}值，或使用其他任意
		 * {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 使用 Cookie 映射的函数
		 * @return 此构建器
		 */
		Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * 将给定过滤器添加到过滤器链。
		 * @param filter 要添加到链中的过滤器
		 */
		Builder filter(ExchangeFilterFunction filter);

		/**
		 * 使用给定消费者操作过滤器。提供给消费者的
		 * 列表是“实时”的，因此消费者可以使用它移除
		 * 过滤器、更改顺序等。
		 * @param filtersConsumer 使用过滤器列表的函数
		 * @return 此构建器
		 */
		Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer);

		/**
		 * 配置 {@code EntityExchangeResult} 回调，每次响应被完整解码为单个实体、
		 * 实体 List 或 byte[] 后都会调用该回调。实际上，等价于以下每一项，
		 * 但只全局注册一次：
		 * <pre>
		 * client.get().uri("/accounts/1")
		 * .exchange()
		 * .expectBody(Person.class).consumeWith(exchangeResult -&gt; ... ));
		 *
		 * client.get().uri("/accounts")
		 * .exchange()
		 * .expectBodyList(Person.class).consumeWith(exchangeResult -&gt; ... ));
		 *
		 * client.get().uri("/accounts/1")
		 * .exchange()
		 * .expectBody().consumeWith(exchangeResult -&gt; ... ));
		 * </pre>
		 * <p>请注意，配置的消费者不适用于解码为 {@code Flux<T>} 的响应，
		 * 此类响应可以在测试客户端工作流之外消费，例如通过 {@code reactor.test.StepVerifier}。
		 * @param consumer 应用于实体响应的消费者
		 * @return 构建器
		 * @since 5.3.5
		 */
		Builder entityExchangeResultConsumer(Consumer<EntityExchangeResult<?>> consumer);

		/**
		 * 为 {@link #exchangeStrategies(ExchangeStrategies) 底层}
		 * {@code ExchangeStrategies} 中的 {@code WebClient} 配置编解码器。
		 * @param configurer 要应用的配置器
		 * @since 5.1.13
		 */
		Builder codecs(Consumer<ClientCodecConfigurer> configurer);

		/**
		 * 配置要使用的 {@link ExchangeStrategies}。
		 * <p>大多数情况下，优先使用 {@link #codecs(Consumer)}，它允许
		 * 自定义 {@code ExchangeStrategies} 中的编解码器，而不是
		 * 替换它们。这可确保多方都能参与编解码器
		 * 配置。
		 * <p>默认设置为 {@link ExchangeStrategies#withDefaults()}。
		 * @param strategies 要使用的策略
		 */
		Builder exchangeStrategies(ExchangeStrategies strategies);

		/**
		 * 自定义通过 {@link #exchangeStrategies(ExchangeStrategies)} 配置的策略。
		 * 此方法设计用于多方希望更新 {@code ExchangeStrategies} 的场景。
		 * @deprecated as of 5.1.13 in favor of {@link #codecs(Consumer)}
		 */
		@Deprecated
		Builder exchangeStrategies(Consumer<ExchangeStrategies.Builder> configurer);

		/**
		 * 等待响应的最长时间。
		 * <p>默认为 5 秒。
		 * @param timeout 响应超时值
		 */
		Builder responseTimeout(Duration timeout);

		/**
		 * 将给定配置器应用于此构建器实例。
		 * <p>这对于应用预打包的自定义配置很有用。
		 * @param configurer 要应用的配置器
		 */
		Builder apply(WebTestClientConfigurer configurer);

		/**
		 * 构建 {@link WebTestClient} 实例。
		 */
		WebTestClient build();
	}


	/**
	 * 用于提供请求 URI 的规范。
	 * @param <S> 对规格类型的自引用
	 */
	interface UriSpec<S extends RequestHeadersSpec<?>> {

		/**
		 * 使用绝对且完整构造的 {@link java.net.URI} 指定 URI。
		 * <p>如果为客户端配置了带基础 URI 的 {@link UriBuilderFactory}，
		 * 该基础 URI <strong>不会</strong>应用于提供的 {@code java.net.URI}。如果希望将基础 URI 应用于
		 * {@code java.net.URI}，必须调用 {@link #uri(String, Object...)}
		 * 或 {@link #uri(String, Map)} &mdash; 例如 {@code uri(myUri.toString())}。
		 * @return 用于添加请求头或执行交换的规格
		 */
		S uri(URI uri);

		/**
		 * 使用 URI 模板和 URI 变量指定请求的 URI。
		 * <p>如果为客户端配置了 {@link UriBuilderFactory}（例如
		 * 带基础 URI），它将用于展开 URI 模板。
		 * @return 用于添加请求头或执行交换的规格
		 */
		S uri(String uri, Object... uriVariables);

		/**
		 * 使用 URI 模板和 URI 变量指定请求的 URI。
		 * <p>如果为客户端配置了 {@link UriBuilderFactory}（例如
		 * 带基础 URI），它将用于展开 URI 模板。
		 * @return 用于添加请求头或执行交换的规格
		 */
		S uri(String uri, Map<String, ?> uriVariables);

		/**
		 * 使用通过为此客户端配置的 {@link UriBuilderFactory} 获取的 {@link UriBuilder}
		 * 构建请求 URI。
		 * @return 用于添加请求头或执行交换的规格
		 */
		S uri(Function<UriBuilder, URI> uriFunction);
	}


	/**
	 * 用于添加请求头并执行交换的规范。
	 * @param <S> 对规格类型的自引用
	 */
	interface RequestHeadersSpec<S extends RequestHeadersSpec<S>> {

		/**
		 * 设置可接受的 {@linkplain MediaType 媒体类型}列表，
		 * 由 {@code Accept} 头指定。
		 * @param acceptableMediaTypes 可接受的媒体类型
		 * @return 同一实例
		 */
		S accept(MediaType... acceptableMediaTypes);

		/**
		 * 设置可接受的 {@linkplain Charset 字符集}列表，
		 * 由 {@code Accept-Charset} 头指定。
		 * @param acceptableCharsets 可接受的字符集
		 * @return 同一实例
		 */
		S acceptCharset(Charset... acceptableCharsets);

		/**
		 * 添加具有给定名称和值的 Cookie。
		 * @param name Cookie 名称
		 * @param value Cookie 的值
		 * @return 同一实例
		 */
		S cookie(String name, String value);

		/**
		 * 使用给定消费者操作此请求的 Cookie。提供给消费者的
		 * 映射是“实时”的，因此消费者可以使用它来
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有请求头值、
		 * {@linkplain MultiValueMap#remove(Object) 删除}值，或使用其他任意
		 * {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 使用 Cookie 映射的函数
		 * @return 此构建器
		 */
		S cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * 设置 {@code If-Modified-Since} 头的值。
		 * <p>日期应指定为自
		 * 1970 年 1 月 1 日 GMT 起的毫秒数。
		 * @param ifModifiedSince 请求头的新值
		 * @return 同一实例
		 */
		S ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * 设置 {@code If-None-Match} 头的值。
		 * @param ifNoneMatches 请求头的新值
		 * @return 同一实例
		 */
		S ifNoneMatch(String... ifNoneMatches);

		/**
		 * 在给定名称下添加给定的单个请求头值。
		 * @param headerName 请求头名称
		 * @param headerValues 请求头的值
		 * @return 同一实例
		 */
		S header(String headerName, String... headerValues);

		/**
		 * 使用给定消费者操作请求头。提供给消费者的
		 * 请求头是“实时”的，因此消费者可以使用它来
		 * {@linkplain HttpHeaders#set(String, String) 覆盖}现有请求头值、
		 * {@linkplain HttpHeaders#remove(Object) 删除}值，或使用其他任意
		 * {@link HttpHeaders} 方法。
		 * @param headersConsumer 使用 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		S headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 将具有给定名称的属性设置为给定值。
		 * @param name 要添加的属性的名称
		 * @param value 要添加的属性的值
		 * @return 此构建器
		 */
		S attribute(String name, Object value);

		/**
		 * 使用给定消费者操作请求属性。提供给消费者的属性是“实时”的，
		 * 因此消费者可以使用它检查属性、移除属性，或使用其他任意映射提供的方法。
		 * @param attributesConsumer 使用属性的函数
		 * @return 此构建器
		 */
		S attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 执行不带请求体的交换。
		 * @return 用于解码响应的规格
		 */
		ResponseSpec exchange();
	}


	/**
	 * 用于提供请求体的规范。
	 */
	interface RequestBodySpec extends RequestHeadersSpec<RequestBodySpec> {
		/**
		 * 设置正文的字节长度，由
		 * {@code Content-Length} 头指定。
		 * @param contentLength 内容长度
		 * @return 同一实例
		 * @see HttpHeaders#setContentLength(long)
		 */
		RequestBodySpec contentLength(long contentLength);

		/**
		 * 设置正文的 {@linkplain MediaType 媒体类型}，
		 * 由 {@code Content-Type} 头指定。
		 * @param contentType 内容类型
		 * @return 同一实例
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		RequestBodySpec contentType(MediaType contentType);

		/**
		 * 将正文设置为给定的 {@code Object} 值。此方法会在底层
		 * {@code WebClient} 上调用
		 * {@link org.springframework.web.reactive.function.client.WebClient.RequestBodySpec#bodyValue(Object)
		 * bodyValue} 方法。
		 * @param body 要写入请求体的值
		 * @return 用于进一步声明请求的规格
		 * @since 5.2
		 */
		RequestHeadersSpec<?> bodyValue(Object body);

		/**
		 * 从给定 {@code Publisher} 设置正文。使用
		 * {@linkplain BodyInserters#fromPublisher Publisher 插入器} 调用
		 * {@link #body(BodyInserter)} 的快捷方式。
		 * @param publisher 请求体数据
		 * @param elementClass publisher 中包含的元素的类
		 * @param <T> publisher 中包含的元素的类型
		 * @param <S> {@code Publisher} 的类型
		 * @return 用于进一步声明请求的规格
		 */
		<T, S extends Publisher<T>> RequestHeadersSpec<?> body(S publisher, Class<T> elementClass);

		/**
		 * 对 {@link #body(Publisher, Class)} 的变体，允许
		 * 提供带泛型的元素类型信息。
		 * @param publisher 请求体数据
		 * @param elementTypeRef publisher 中包含的元素的类型引用
		 * @param <T> publisher 中包含的元素的类型
		 * @param <S> {@code Publisher} 的类型
		 * @return 用于进一步声明请求的规格
		 * @since 5.2
		 */
		<T, S extends Publisher<T>> RequestHeadersSpec<?> body(
				S publisher, ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * 从给定生产者设置正文。此方法会在底层
		 * {@code WebClient} 上调用
		 * {@link org.springframework.web.reactive.function.client.WebClient.RequestBodySpec#body(Object, Class)
		 * body(Object, Class)} 方法。
		 * @param producer 要写入请求的生产者。它必须是 {@link Publisher}，
		 * 或可通过 {@link ReactiveAdapterRegistry} 适配为
		 * {@code Publisher} 的其他生产者
		 * @param elementClass producer 中包含的元素的类
		 * @return 用于进一步声明请求的规格
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, Class<?> elementClass);

		/**
		 * 从给定生产者设置正文。此方法会在底层
		 * {@code WebClient} 上调用
		 * {@link org.springframework.web.reactive.function.client.WebClient.RequestBodySpec#body(Object, ParameterizedTypeReference)
		 * body(Object, ParameterizedTypeReference)} 方法。
		 * @param producer 要写入请求的生产者。它必须是 {@link Publisher}，
		 * 或可通过 {@link ReactiveAdapterRegistry} 适配为
		 * {@code Publisher} 的其他生产者
		 * @param elementTypeRef producer 中包含的元素的类型引用
		 * @return 用于进一步声明请求的规格
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, ParameterizedTypeReference<?> elementTypeRef);

		/**
		 * 将请求正文设置为给定的 {@code BodyInserter}。
		 * 此方法会在底层 {@code WebClient} 上调用
		 * {@link org.springframework.web.reactive.function.client.WebClient.RequestBodySpec#body(BodyInserter)
		 * body(BodyInserter)} 方法。
		 * @param inserter 要使用的请求体插入器
		 * @return 用于进一步声明请求的规格
		 * @see org.springframework.web.reactive.function.BodyInserters
		 */
		RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * 使用 {@linkplain BodyInserters#fromValue 值插入器} 调用
		 * {@link #body(BodyInserter)} 的快捷方式。
		 * 从 5.2 开始，此方法委托给 {@link #bodyValue(Object)}。
		 * @deprecated as of Spring Framework 5.2 in favor of {@link #bodyValue(Object)}
		 */
		@Deprecated
		RequestHeadersSpec<?> syncBody(Object body);
	}


	/**
	 * 用于提供请求头和请求 URI 的规范。
	 * @param <S> 对规格类型的自引用
	 */
	interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>> extends UriSpec<S>, RequestHeadersSpec<S> {
	}

	/**
	 * 用于提供请求体和请求 URI 的规范。
	 */
	interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {
	}


	/**
	 * 用于对响应应用断言的链式 API。
	 */
	interface ResponseSpec {

		/**
		 * 使用给定的 {@linkplain ResponseSpecConsumer 消费者} 对响应应用多个断言，
		 * 并保证即使一个或多个断言因异常失败，
		 * 所有断言仍会被应用。
		 * <p>如果抛出单个 {@link Error} 或 {@link RuntimeException}，
		 * 它将被重新抛出。
		 * <p>如果抛出多个异常，此方法将抛出
		 * {@link AssertionError}，其错误消息是所有异常的摘要。此外，每个异常都会作为
		 * {@linkplain Throwable#addSuppressed(Throwable) 抑制异常} 添加到
		 * 该 {@code AssertionError}。
		 * <p>此功能类似于 AssertJ 中的 {@code SoftAssertions} 支持和
		 * JUnit Jupiter 中的 {@code assertAll()} 支持。
		 *
		 * <h4>示例</h4>
		 * <pre class="code">
		 * webTestClient.get().uri("/hello").exchange()
		 * .expectAll(
		 * responseSpec -&gt; responseSpec.expectStatus().isOk(),
		 * responseSpec -&gt; responseSpec.expectBody(String.class).isEqualTo("Hello, World!")
		 * );
		 * </pre>
		 * @param consumers {@code ResponseSpec} 消费者列表
		 * @since 5.3.10
		 */
		ResponseSpec expectAll(ResponseSpecConsumer... consumers);

		/**
		 * 对响应状态的断言。
		 */
		StatusAssertions expectStatus();

		/**
		 * 对响应头的断言。
		 */
		HeaderAssertions expectHeader();

		/**
		 * 对响应 Cookie 的断言。
		 * @since 5.3
		 */
		CookieAssertions expectCookie();

		/**
		 * 消费响应体并将其解码为 {@code <B>} 类型的单个对象，
		 * 然后应用断言。
		 * @param bodyType 期望的响应体类型
		 */
		<B> BodySpec<B, ?> expectBody(Class<B> bodyType);

		/**
		 * 对 {@link #expectBody(Class)} 的替代方法，接受
		 * 带泛型的目标类型信息。
		 */
		<B> BodySpec<B, ?> expectBody(ParameterizedTypeReference<B> bodyType);

		/**
		 * 消费响应体并将其解码为 {@code List<E>}，然后应用
		 * 特定于列表的断言。
		 * @param elementType 期望的 List 元素类型
		 */
		<E> ListBodySpec<E> expectBodyList(Class<E> elementType);

		/**
		 * 对 {@link #expectBodyList(Class)} 的替代方法，接受
		 * 带泛型的目标类型信息。
		 */
		<E> ListBodySpec<E> expectBodyList(ParameterizedTypeReference<E> elementType);

		/**
		 * 消费响应体并将其解码为 {@code byte[]}，然后对
		 * 原始内容应用断言（例如 isEmpty、JSONPath 等）。
		 */
		BodyContentSpec expectBody();

		/**
		 * 退出链式流程，以便在外部消费响应体，
		 * 例如通过 {@link reactor.test.StepVerifier}。
		 * <p>请注意，当传入 {@code Void.class} 时，响应体
		 * 会被消费并释放。如果不期望有内容，请考虑
		 * 使用 {@code .expectBody().isEmpty()}，它会断言
		 * 没有内容。
		 */
		<T> FluxExchangeResult<T> returnResult(Class<T> elementClass);

		/**
		 * 对 {@link #returnResult(Class)} 的替代方法，接受
		 * 带泛型的目标类型信息。
		 */
		<T> FluxExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * {@link Consumer} of a {@link ResponseSpec}.
		 * @since 5.3.10
		 * @see ResponseSpec#expectAll(ResponseSpecConsumer...)
		 */
		@FunctionalInterface
		interface ResponseSpecConsumer extends Consumer<ResponseSpec> {
		}

	}


	/**
	 * 对解码为单个对象的响应体进行预期的规范。
	 * @param <S> 对规格类型的自引用
	 * @param <B> 响应体类型
	 */
	interface BodySpec<B, S extends BodySpec<B, S>> {

		/**
		 * 断言提取的正文等于给定值。
		 */
		<T extends S> T isEqualTo(B expected);

		/**
		 * 使用 {@link Matcher} 断言提取的正文。
		 * @since 5.1
		 */
		<T extends S> T value(Matcher<? super B> matcher);

		/**
		 * 使用函数转换提取的正文（例如提取属性），
		 * 并使用 {@link Matcher} 断言映射后的值。
		 * @since 5.1
		 */
		<T extends S, R> T value(Function<B, R> bodyMapper, Matcher<? super R> matcher);

		/**
		 * 使用 {@link Consumer} 断言提取的正文。
		 * @since 5.1
		 */
		<T extends S> T value(Consumer<B> consumer);

		/**
		 * 使用给定 {@link Consumer} 断言交换结果。
		 */
		<T extends S> T consumeWith(Consumer<EntityExchangeResult<B>> consumer);

		/**
		 * 退出链式 API 并返回包含
		 * 解码后响应内容的 {@code ExchangeResult}。
		 */
		EntityExchangeResult<B> returnResult();
	}


	/**
	 * 对解码为 List 的响应体进行预期的规范。
	 * @param <E> 响应体列表的元素类型
	 */
	interface ListBodySpec<E> extends BodySpec<List<E>, ListBodySpec<E>> {

		/**
		 * 断言提取的值列表具有给定大小。
		 * @param size 期望的大小
		 */
		ListBodySpec<E> hasSize(int size);

		/**
		 * 断言提取的值列表包含给定元素。
		 * @param elements 要检查的元素
		 */
		@SuppressWarnings("unchecked")
		ListBodySpec<E> contains(E... elements);

		/**
		 * 断言提取的值列表不包含给定元素。
		 * @param elements 要检查的元素
		 */
		@SuppressWarnings("unchecked")
		ListBodySpec<E> doesNotContain(E... elements);
	}


	/**
	 * 对响应体内容进行预期的规范。
	 */
	interface BodyContentSpec {

		/**
		 * 断言响应体为空，并返回交换结果。
		 */
		EntityExchangeResult<Void> isEmpty();

		/**
		 * 将预期和实际响应内容解析为 JSON 并执行
		 * 比较，验证它们在格式不同的情况下仍包含相同的属性值对，
		 * 使用<em>宽松</em>检查（可扩展
		 * 且数组顺序不严格）。
		 * <p>使用此方法需要类路径上有
		 * <a href="https://jsonassert.skyscreamer.org/">JSONassert</a> 库。
		 * @param expectedJson 期望的 JSON 内容
		 * @see #json(String, boolean)
		 */
		default BodyContentSpec json(String expectedJson) {
			return json(expectedJson, false);
		}

		/**
		 * 将预期和实际响应内容解析为 JSON 并执行
		 * 比较，验证它们在格式不同的情况下仍包含相同的属性值对。
		 * <p>可根据 {@code strict} 参数值以两种模式进行比较：
		 * <ul>
		 * <li>{@code true}：严格检查。不可扩展且数组顺序严格。</li>
		 * <li>{@code false}：宽松检查。可扩展且数组顺序不严格。</li>
		 * </ul>
		 * <p>使用此方法需要类路径上有
		 * <a href="https://jsonassert.skyscreamer.org/">JSONassert</a> 库。
		 * @param expectedJson 期望的 JSON 内容
		 * @param strict 如果为 {@code true}，则启用严格检查
		 * @since 5.3.16
		 * @see #json(String)
		 */
		BodyContentSpec json(String expectedJson, boolean strict);

		/**
		 * 将预期和实际响应内容解析为 XML，并断言
		 * 二者“相似”，即它们包含相同的元素和
		 * 属性，与顺序无关。
		 * <p>使用此方法需要类路径上有
		 * <a href="https://github.com/xmlunit/xmlunit">XMLUnit</a> 库。
		 * @param expectedXml 期望的 JSON 内容。
		 * @since 5.1
		 * @see org.springframework.test.util.XmlExpectationsHelper#assertXmlEqual(String, String)
		 */
		BodyContentSpec xml(String expectedXml);

		/**
		 * 使用 <a href="https://github.com/jayway/JsonPath">JsonPath</a> 表达式
		 * 访问响应体断言，以检查响应体的特定子集。
		 * <p>JSON 路径表达式可以是使用
		 * {@link String#format} 中定义的格式说明符的参数化字符串。
		 * @param expression JsonPath 表达式
		 * @param args 用于对表达式进行参数化的参数
		 */
		JsonPathAssertions jsonPath(String expression, Object... args);

		/**
		 * 使用 XPath 表达式访问响应体断言，以
		 * 检查响应体的特定子集。
		 * <p>XPath 表达式可以是使用
		 * {@link String#format} 中定义的格式说明符的参数化字符串。
		 * @param expression XPath 表达式
		 * @param args 用于对表达式进行参数化的参数
		 * @since 5.1
		 * @see #xpath(String, Map, Object...)
		 */
		default XpathAssertions xpath(String expression, Object... args) {
			return xpath(expression, null, args);
		}

		/**
		 * 使用带特定命名空间的 XPath 表达式访问响应体断言，
		 * 以检查响应体的特定子集。
		 * <p>XPath 表达式可以是使用
		 * {@link String#format} 中定义的格式说明符的参数化字符串。
		 * @param expression XPath 表达式
		 * @param namespaces 要使用的命名空间
		 * @param args 用于对表达式进行参数化的参数
		 * @since 5.1
		 */
		XpathAssertions xpath(String expression, @Nullable Map<String, String> namespaces, Object... args);

		/**
		 * 使用给定 {@link Consumer} 断言响应体内容。
		 * @param consumer 用于响应正文的消费者；如果没有响应正文，
		 * 则输入的 {@code byte[]} 可能为 {@code null}。
		 */
		BodyContentSpec consumeWith(Consumer<EntityExchangeResult<byte[]>> consumer);

		/**
		 * 退出链式 API 并返回包含
		 * 原始响应内容的 {@code ExchangeResult}。
		 */
		EntityExchangeResult<byte[]> returnResult();
	}

}
