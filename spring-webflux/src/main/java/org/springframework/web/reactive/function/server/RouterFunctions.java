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

package org.springframework.web.reactive.function.server;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.Assert;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * <strong>Spring 函数式 Web 框架的中央入口点。</strong>公开路由功能，例如使用可发现的构建器风格 API {@linkplain #route() 创建} {@code RouterFunction}，在给定 {@code RequestPredicate} 和 {@code HandlerFunction} 的情况下{@linkplain #route(RequestPredicate, HandlerFunction) 创建} {@code RouterFunction}，以及对现有路由函数进行进一步的{@linkplain #nest(RequestPredicate, RouterFunction) 子路由}。
 *
 * <p>此外，此类还可以将 {@code RouterFunction} {@linkplain #toHttpHandler(RouterFunction) 转换}为 {@code HttpHandler}，后者可在 Servlet 3.1+、Reactor 或 Undertow 中运行。
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class RouterFunctions {

	private static final Log logger = LogFactory.getLog(RouterFunctions.class);

	/**
	 * 包含 {@link ServerRequest} 的 {@link ServerWebExchange} 属性名称。
	 */
	public static final String REQUEST_ATTRIBUTE = RouterFunctions.class.getName() + ".request";

	/**
	 * 包含 URI 模板映射（将变量名映射到值）的 {@link ServerWebExchange} 属性名称。
	 */
	public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE =
			RouterFunctions.class.getName() + ".uriTemplateVariables";

	/**
	 * 包含匹配模式（类型为 {@link org.springframework.web.util.pattern.PathPattern}）的 {@link ServerWebExchange#getAttributes() 属性}名称。
	 */
	public static final String MATCHING_PATTERN_ATTRIBUTE =
			RouterFunctions.class.getName() + ".matchingPattern";




	/**
	 * 提供一种可通过构建器风格接口发现的方式来创建路由函数。
	 * @return a router function builder
	 * @since 5.1
	 */
	public static Builder route() {
		return new RouterFunctionBuilder();
	}

	/**
	 * 如果给定请求谓词适用，则路由到给定处理函数。
	 * <p>例如，以下示例将 "/user" 的 GET 请求路由到 {@code userController} 中的 {@code listUsers} 方法：
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; route =
	 *     RouterFunctions.route(RequestPredicates.GET("/user"), userController::listUsers);
	 * </pre>
	 * @param predicate the predicate to test
	 * @param handlerFunction the handler function to route to if the predicate applies
	 * @param <T> the type of response returned by the handler function
	 * @return a router function that routes to {@code handlerFunction} if
	 * {@code predicate} evaluates to {@code true}
	 * @see RequestPredicates
	 */
	public static <T extends ServerResponse> RouterFunction<T> route(
			RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

		return new DefaultRouterFunction<>(predicate, handlerFunction);
	}

	/**
	 * 如果给定请求谓词适用，则路由到给定路由函数。此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径（前缀）、头或其他请求谓词。
	 * <p>例如，以下示例首先创建一个组合路由，对于 GET 解析到 {@code listUsers}，对于 POST 解析到 {@code createUser}。然后，该组合路由与 "/user" 路径谓词嵌套，因此 "/user" 的 GET 请求将列出用户，而 "/user" 的 POST 请求将创建新用户。
	 * <pre class="code">
	 * RouterFunction&lt;ServerResponse&gt; userRoutes =
	 *   RouterFunctions.route(RequestPredicates.method(HttpMethod.GET), this::listUsers)
	 *     .andRoute(RequestPredicates.method(HttpMethod.POST), this::createUser);
	 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
	 *   RouterFunctions.nest(RequestPredicates.path("/user"), userRoutes);
	 * </pre>
	 * @param predicate the predicate to test
	 * @param routerFunction the nested router function to delegate to if the predicate applies
	 * @param <T> the type of response returned by the handler function
	 * @return a router function that routes to {@code routerFunction} if
	 * {@code predicate} evaluates to {@code true}
	 * @see RequestPredicates
	 */
	public static <T extends ServerResponse> RouterFunction<T> nest(
			RequestPredicate predicate, RouterFunction<T> routerFunction) {

		return new DefaultNestedRouterFunction<>(predicate, routerFunction);
	}

	/**
	 * 将与给定模式匹配的请求路由到相对于给定根位置的资源。例如
	 * <pre class="code">
	 * Resource location = new FileSystemResource("public-resources/");
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
	 * </pre>
	 * @param pattern the pattern to match
	 * @param location the location directory relative to which resources should be resolved
	 * @return a router function that routes to resources
	 * @see #resourceLookupFunction(String, Resource)
	 */
	public static RouterFunction<ServerResponse> resources(String pattern, Resource location) {
		return resources(resourceLookupFunction(pattern, location));
	}

	/**
	 * 返回由 {@link #resources(String, Resource)} 使用的资源查找函数。返回的函数可以{@linkplain Function#andThen(Function) 组合}，例如在查找函数不匹配时返回默认资源：
	 * <pre class="code">
	 * Mono&lt;Resource&gt; defaultResource = Mono.just(new ClassPathResource("index.html"));
	 * Function&lt;ServerRequest, Mono&lt;Resource&gt;&gt; lookupFunction =
	 *   RouterFunctions.resourceLookupFunction("/resources/**", new FileSystemResource("public-resources/"))
	 *     .andThen(resourceMono -&gt; resourceMono.switchIfEmpty(defaultResource));
	 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources(lookupFunction);
	 * </pre>
	 * @param pattern the pattern to match
	 * @param location the location directory relative to which resources should be resolved
	 * @return the default resource lookup function for the given parameters.
	 */
	public static Function<ServerRequest, Mono<Resource>> resourceLookupFunction(String pattern, Resource location) {
		return new PathResourceLookupFunction(pattern, location);
	}

	/**
	 * 使用提供的查找函数路由到资源。如果查找函数为给定请求提供 {@link Resource}，则将通过处理 GET、HEAD 和 OPTIONS 请求的 {@link HandlerFunction} 暴露该资源。
	 * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
	 * @return a router function that routes to resources
	 */
	public static RouterFunction<ServerResponse> resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
		return new ResourcesRouterFunction(lookupFunction);
	}

	/**
	 * 将给定的{@linkplain RouterFunction 路由函数}转换为 {@link HttpHandler}。此转换使用{@linkplain HandlerStrategies#builder() 默认策略}。
	 * <p>返回的处理器可适配为在以下环境中运行：
	 * <ul>
	 * <li>Servlet 3.1+，使用 {@link org.springframework.http.server.reactive.ServletHttpHandlerAdapter}，</li>
	 * <li>Reactor，使用 {@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter}，</li>
	 * <li>Undertow，使用 {@link org.springframework.http.server.reactive.UndertowHttpHandlerAdapter}。</li>
	 * </ul>
	 * <p>请注意，{@code HttpWebHandlerAdapter} 还实现了 {@link WebHandler}，允许通过 {@link WebHttpHandlerBuilder} 注册额外的过滤器和异常处理器。
	 * @param routerFunction the router function to convert
	 * @return an HTTP handler that handles HTTP request using the given router function
	 */
	public static HttpHandler toHttpHandler(RouterFunction<?> routerFunction) {
		return toHttpHandler(routerFunction, HandlerStrategies.withDefaults());
	}

	/**
	 * 使用给定策略将给定的{@linkplain RouterFunction 路由函数}转换为 {@link HttpHandler}。
	 * <p>返回的 {@code HttpHandler} 可适配为在以下环境中运行：
	 * <ul>
	 * <li>Servlet 3.1+，使用 {@link org.springframework.http.server.reactive.ServletHttpHandlerAdapter}，</li>
	 * <li>Reactor，使用 {@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter}，</li>
	 * <li>Undertow，使用 {@link org.springframework.http.server.reactive.UndertowHttpHandlerAdapter}。</li>
	 * </ul>
	 * @param routerFunction the router function to convert
	 * @param strategies the strategies to use
	 * @return an HTTP handler that handles HTTP request using the given router function
	 */
	public static HttpHandler toHttpHandler(RouterFunction<?> routerFunction, HandlerStrategies strategies) {
		WebHandler webHandler = toWebHandler(routerFunction, strategies);
		return WebHttpHandlerBuilder.webHandler(webHandler)
				.filters(filters -> filters.addAll(strategies.webFilters()))
				.exceptionHandlers(handlers -> handlers.addAll(strategies.exceptionHandlers()))
				.localeContextResolver(strategies.localeContextResolver())
				.build();
	}

	/**
	 * 将给定的{@linkplain RouterFunction 路由函数}转换为 {@link WebHandler}。此转换使用{@linkplain HandlerStrategies#builder() 默认策略}。
	 * @param routerFunction the router function to convert
	 * @return a web handler that handles web request using the given router function
	 */
	public static WebHandler toWebHandler(RouterFunction<?> routerFunction) {
		return toWebHandler(routerFunction, HandlerStrategies.withDefaults());
	}

	/**
	 * 使用给定策略将给定的{@linkplain RouterFunction 路由函数}转换为 {@link WebHandler}。
	 * @param routerFunction the router function to convert
	 * @param strategies the strategies to use
	 * @return a web handler that handles web request using the given router function
	 */
	public static WebHandler toWebHandler(RouterFunction<?> routerFunction, HandlerStrategies strategies) {
		Assert.notNull(routerFunction, "RouterFunction must not be null");
		Assert.notNull(strategies, "HandlerStrategies must not be null");

		return new RouterFunctionWebHandler(strategies, routerFunction);
	}

	/**
	 * 更改给定{@linkplain RouterFunction 路由函数}上的 {@link PathPatternParser}。此方法可用于更改 {@code PathPatternParser} 的默认属性，例如更改{@linkplain PathPatternParser#setCaseSensitive(boolean) 大小写敏感性}。
	 * @param routerFunction the router function to change the parser in
	 * @param parser the parser to change to.
	 * @param <T> the type of response returned by the handler function
	 * @return the change router function
	 */
	public static <T extends ServerResponse> RouterFunction<T> changeParser(RouterFunction<T> routerFunction,
			PathPatternParser parser) {

		Assert.notNull(routerFunction, "RouterFunction must not be null");
		Assert.notNull(parser, "Parser must not be null");

		ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(parser);
		routerFunction.accept(visitor);
		return routerFunction;
	}


	/**
	 * 表示可发现的路由函数构建器。通过 {@link RouterFunctions#route()} 获取。
	 * @since 5.1
	 */
	public interface Builder {

		/**
		 * 添加一个路由，将 HTTP {@code GET} 请求交给给定处理函数处理。
		 * @param handlerFunction the handler function to handle all {@code GET} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder GET(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定模式的 HTTP {@code GET} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code GET} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder GET(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定谓词的 HTTP {@code GET} 请求交给给定处理函数处理。
		 * @param predicate predicate to match
		 * @param handlerFunction the handler function to handle all {@code GET} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder GET(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有同时匹配给定模式和谓词的 HTTP {@code GET} 请求交给给定处理函数处理。
		 * <p>例如，以下示例将接受 JSON 的 "/user" GET 请求路由到 {@code userController} 中的 {@code listUsers} 方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .GET("/user", RequestPredicates.accept(MediaType.APPLICATION_JSON), userController::listUsers)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code GET} requests that
		 * match {@code pattern} and the predicate
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder GET(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将 HTTP {@code HEAD} 请求交给给定处理函数处理。
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder HEAD(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定模式的 HTTP {@code HEAD} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder HEAD(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定谓词的 HTTP {@code HEAD} 请求交给给定处理函数处理。
		 * @param predicate predicate to match
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder HEAD(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有同时匹配给定模式和谓词的 HTTP {@code HEAD} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code HEAD} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder HEAD(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将 HTTP {@code POST} 请求交给给定处理函数处理。
		 * @param handlerFunction the handler function to handle all {@code POST} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder POST(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定模式的 HTTP {@code POST} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code POST} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder POST(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定谓词的 HTTP {@code POST} 请求交给给定处理函数处理。
		 * @param predicate predicate to match
		 * @param handlerFunction the handler function to handle all {@code POST} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder POST(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有同时匹配给定模式和谓词的 HTTP {@code POST} 请求交给给定处理函数处理。
		 * <p>例如，以下示例将包含 JSON 的 "/user" POST 请求路由到 {@code userController} 中的 {@code addUser} 方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .POST("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::addUser)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code POST} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder POST(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将 HTTP {@code PUT} 请求交给给定处理函数处理。
		 * @param handlerFunction the handler function to handle all {@code PUT} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder PUT(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定模式的 HTTP {@code PUT} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code PUT} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder PUT(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定谓词的 HTTP {@code PUT} 请求交给给定处理函数处理。
		 * @param predicate predicate to match
		 * @param handlerFunction the handler function to handle all {@code PUT} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder PUT(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有同时匹配给定模式和谓词的 HTTP {@code PUT} 请求交给给定处理函数处理。
		 * <p>例如，以下示例将包含 JSON 的 "/user" PUT 请求路由到 {@code userController} 中的 {@code editUser} 方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PUT("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code PUT} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder PUT(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将 HTTP {@code PATCH} 请求交给给定处理函数处理。
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder PATCH(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定模式的 HTTP {@code PATCH} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder PATCH(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定谓词的 HTTP {@code PATCH} 请求交给给定处理函数处理。
		 * @param predicate predicate to match
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder PATCH(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有同时匹配给定模式和谓词的 HTTP {@code PATCH} 请求交给给定处理函数处理。
		 * <p>例如，以下示例将包含 JSON 的 "/user" PATCH 请求路由到 {@code userController} 中的 {@code editUser} 方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .PATCH("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code PATCH} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder PATCH(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将 HTTP {@code DELETE} 请求交给给定处理函数处理。
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder DELETE(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定模式的 HTTP {@code DELETE} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder DELETE(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定谓词的 HTTP {@code DELETE} 请求交给给定处理函数处理。
		 * @param predicate predicate to match
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder DELETE(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有同时匹配给定模式和谓词的 HTTP {@code DELETE} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code DELETE} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder DELETE(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将 HTTP {@code OPTIONS} 请求交给给定处理函数处理。
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests
		 * @return this builder
		 * @since 5.3
		 */
		Builder OPTIONS(HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定模式的 HTTP {@code OPTIONS} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder OPTIONS(String pattern, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定谓词的 HTTP {@code OPTIONS} 请求交给给定处理函数处理。
		 * @param predicate predicate to match
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
		 * match {@code predicate}
		 * @return this builder
		 * @since 5.3
		 * @see RequestPredicates
		 */
		Builder OPTIONS(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有同时匹配给定模式和谓词的 HTTP {@code OPTIONS} 请求交给给定处理函数处理。
		 * @param pattern the pattern to match to
		 * @param predicate additional predicate to match
		 * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
		 * match {@code pattern}
		 * @return this builder
		 */
		Builder OPTIONS(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 添加一个路由，将所有匹配给定谓词的请求交给给定处理函数处理。
		 * @param predicate the request predicate to match
		 * @param handlerFunction the handler function to handle all requests that match the predicate
		 * @return this builder
		 * @since 5.2
		 * @see RequestPredicates
		 */
		Builder route(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

		/**
		 * 将给定路由添加到此构建器。可用于将外部定义的路由函数合并到此构建器中，也可以与 {@link RouterFunctions#route(RequestPredicate, HandlerFunction)} 结合使用，以实现更灵活的谓词匹配。
		 * <p>例如，以下示例将从 {@code OrderController.routerFunction()} 返回的路由函数添加到 {@code userController} 的 {@code changeUser} 方法：
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; route =
		 *   RouterFunctions.route()
		 *     .GET("/users", userController::listUsers)
		 *     .add(orderController.routerFunction());
		 *     .build();
		 * </pre>
		 * @param routerFunction the router function to be added
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder add(RouterFunction<ServerResponse> routerFunction);

		/**
		 * 将与给定模式匹配的请求路由到相对于给定根位置的资源。例如
		 * <pre class="code">
		 * Resource location = new FileSystemResource("public-resources/");
		 * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
		 * </pre>
		 * @param pattern the pattern to match
		 * @param location the location directory relative to which resources should be resolved
		 * @return this builder
		 */
		Builder resources(String pattern, Resource location);

		/**
		 * 使用提供的查找函数路由到资源。如果查找函数为给定请求提供 {@link Resource}，则将通过处理 GET、HEAD 和 OPTIONS 请求的 {@link HandlerFunction} 暴露该资源。
		 * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
		 * @return this builder
		 */
		Builder resources(Function<ServerRequest, Mono<Resource>> lookupFunction);

		/**
		 * 如果给定请求谓词适用，则路由到提供的路由函数。此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径（前缀）、头或其他请求谓词。
		 * <p>例如，以下示例创建带有 "/user" 路径谓词的嵌套路由，使 "/user" 的 GET 请求列出用户，而 "/user" 的 POST 请求创建新用户。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .nest(RequestPredicates.path("/user"), () -&gt;
		 *       RouterFunctions.route()
		 *         .GET(this::listUsers)
		 *         .POST(this::createUser)
		 *         .build())
		 *     .build();
		 * </pre>
		 * @param predicate the predicate to test
		 * @param routerFunctionSupplier supplier for the nested router function to delegate to if
		 * the predicate applies
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder nest(RequestPredicate predicate, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * 如果给定请求谓词适用，则路由到已构建的路由函数。此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径（前缀）、头或其他请求谓词。
		 * <p>例如，以下示例创建带有 "/user" 路径谓词的嵌套路由，使 "/user" 的 GET 请求列出用户，而 "/user" 的 POST 请求创建新用户。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .nest(RequestPredicates.path("/user"), builder -&gt;
		 *       builder.GET(this::listUsers)
		 *              .POST(this::createUser))
		 *     .build();
		 * </pre>
		 * @param predicate the predicate to test
		 * @param builderConsumer consumer for a {@code Builder} that provides the nested router
		 * function
		 * @return this builder
		 * @see RequestPredicates
		 */
		Builder nest(RequestPredicate predicate, Consumer<Builder> builderConsumer);

		/**
		 * 如果给定路径前缀模式适用，则路由到提供的路由函数。此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径前缀。具体而言，此方法可用于将外部定义的路由函数合并到某个路径前缀下。
		 * <p>例如，以下示例创建带有 "/user" 路径谓词的嵌套路由，委托给 {@code userController} 中定义的路由函数，并使用 "/order" 路径委托给 {@code orderController}。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .path("/user", userController::routerFunction)
		 *     .path("/order", orderController::routerFunction)
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param routerFunctionSupplier supplier for the nested router function to delegate to if
		 * the pattern matches
		 * @return this builder
		 */
		Builder path(String pattern, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

		/**
		 * 如果给定路径前缀模式适用，则路由到已构建的路由函数。此方法可用于创建<strong>嵌套路由</strong>，其中一组路由共享公共路径前缀。
		 * <p>例如，以下示例创建带有 "/user" 路径谓词的嵌套路由，使 "/user" 的 GET 请求列出用户，而 "/user" 的 POST 请求创建新用户。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; nestedRoute =
		 *   RouterFunctions.route()
		 *     .path("/user", builder -&gt;
		 *       builder.GET(this::listUsers)
		 *              .POST(this::createUser))
		 *     .build();
		 * </pre>
		 * @param pattern the pattern to match to
		 * @param builderConsumer consumer for a {@code Builder} that provides the nested router
		 * function
		 * @return this builder
		 */
		Builder path(String pattern, Consumer<Builder> builderConsumer);

		/**
		 * 使用给定过滤器函数过滤此构建器创建的所有路由。过滤器函数通常用于处理横切关注点，例如日志记录、安全性等。
		 * <p>例如，以下示例创建一个过滤器，在请求不包含必要的认证头时返回 401 Unauthorized 响应。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .filter((request, next) -&gt; {
		 *       // check for authentication headers
		 *       if (isAuthenticated(request)) {
		 *         return next.handle(request);
		 *       }
		 *       else {
		 *         return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
		 *       }
		 *     })
		 *     .build();
		 * </pre>
		 * @param filterFunction the function to filter all routes built by this builder
		 * @return this builder
		 */
		Builder filter(HandlerFilterFunction<ServerResponse, ServerResponse> filterFunction);

		/**
		 * 使用给定请求处理函数过滤此构建器创建的所有路由的请求对象。过滤器通常用于处理横切关注点，例如日志记录、安全性等。
		 * <p>例如，以下示例创建一个在处理器函数执行前记录请求的过滤器。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .before(request -&gt; {
		 *       log(request);
		 *       return request;
		 *     })
		 *     .build();
		 * </pre>
		 * @param requestProcessor a function that transforms the request
		 * @return this builder
		 */
		Builder before(Function<ServerRequest, ServerRequest> requestProcessor);

		/**
		 * 使用给定响应处理函数过滤此构建器创建的所有路由的响应对象。过滤器通常用于处理横切关注点，例如日志记录、安全性等。
		 * <p>例如，以下示例创建一个在处理器函数执行后记录响应的过滤器。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .after((request, response) -&gt; {
		 *       log(response);
		 *       return response;
		 *     })
		 *     .build();
		 * </pre>
		 * @param responseProcessor a function that transforms the response
		 * @return this builder
		 */
		Builder after(BiFunction<ServerRequest, ServerResponse, ServerResponse> responseProcessor);

		/**
		 * 通过应用给定响应提供者函数，过滤所有匹配谓词的异常。
		 * <p>例如，以下示例创建一个在发生 {@code IllegalStateException} 时返回 500 响应状态的过滤器。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .onError(e -&gt; e instanceof IllegalStateException,
		 *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
		 *     .build();
		 * </pre>
		 * @param predicate the type of exception to filter
		 * @param responseProvider a function that creates a response
		 * @return this builder
		 */
		Builder onError(Predicate<? super Throwable> predicate,
				BiFunction<? super  Throwable, ServerRequest, Mono<ServerResponse>> responseProvider);

		/**
		 * 通过应用给定响应提供者函数，过滤给定类型的所有异常。
		 * <p>例如，以下示例创建一个在发生 {@code IllegalStateException} 时返回 500 响应状态的过滤器。
		 * <pre class="code">
		 * RouterFunction&lt;ServerResponse&gt; filteredRoute =
		 *   RouterFunctions.route()
		 *     .GET("/user", this::listUsers)
		 *     .onError(IllegalStateException.class,
		 *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
		 *     .build();
		 * </pre>
		 * @param exceptionType the type of exception to filter
		 * @param responseProvider a function that creates a response
		 * @return this builder
		 */
		<T extends Throwable> Builder onError(Class<T> exceptionType,
				BiFunction<? super T, ServerRequest, Mono<ServerResponse>> responseProvider);

		/**
		 * 将具有给定名称和值的属性添加到使用此构建器构建的最后一个路由。
		 * @param name the attribute name
		 * @param value the attribute value
		 * @return this builder
		 * @since 5.3
		 */
		Builder withAttribute(String name, Object value);

		/**
		 * 使用给定消费者操作最后一个构建路由的属性。
		 * <p>提供给消费者的映射是“实时”的，因此消费者可用于{@linkplain Map#put(Object, Object) 覆盖}现有属性、{@linkplain Map#remove(Object) 删除}属性，或使用任何其他 {@link Map} 方法。
		 * @param attributesConsumer a function that consumes the attributes map
		 * @return this builder
		 * @since 5.3
		 */
		Builder withAttributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 构建 {@code RouterFunction}。所有创建的路由彼此{@linkplain RouterFunction#and(RouterFunction) 组合}，如果有过滤器，则将其应用到结果。
		 * @return the built router function
		 */
		RouterFunction<ServerResponse> build();
	}


	/**
	 * 接收来自路由函数逻辑结构的通知。
	 */
	public interface Visitor {

		/**
		 * 接收嵌套路由函数开始的通知。
		 * @param predicate the predicate that applies to the nested router functions
		 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
		 */
		void startNested(RequestPredicate predicate);

		/**
		 * 接收嵌套路由函数结束的通知。
		 * @param predicate the predicate that applies to the nested router functions
		 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
		 */
		void endNested(RequestPredicate predicate);

		/**
		 * 接收标准谓词路由到处理函数的通知。
		 * @param predicate the predicate that applies to the handler function
		 * @param handlerFunction the handler function.
		 * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
		 */
		void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction);

		/**
		 * 接收资源路由函数的通知。
		 * @param lookupFunction the lookup function for the resources
		 * @see RouterFunctions#resources(Function)
		 */
		void resources(Function<ServerRequest, Mono<Resource>> lookupFunction);

		/**
		 * 接收带有属性的路由函数的通知。给定属性适用于紧随此通知之后的路由通知。
		 * @param attributes the attributes that apply to the following router
		 * @since 5.3
		 */
		void attributes(Map<String, Object> attributes);

		/**
		 * 接收未知路由函数的通知。对于不是通过各种 {@link RouterFunctions} 方法创建的路由函数，将调用此方法。
		 * @param routerFunction the router function
		 */
		void unknown(RouterFunction<?> routerFunction);
	}


	abstract static class AbstractRouterFunction<T extends ServerResponse> implements RouterFunction<T> {

		@Override
		public String toString() {
			ToStringVisitor visitor = new ToStringVisitor();
			accept(visitor);
			return visitor.toString();
		}
	}


	/**
	 * 组合路由函数，先调用一个函数，如果此路由{@linkplain Mono#empty() 没有结果}，则调用另一个函数（相同响应类型 {@code T}）。
	 * @param <T> the server response type
	 */
	static final class SameComposedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		private final RouterFunction<T> first;

		private final RouterFunction<T> second;

		public SameComposedRouterFunction(RouterFunction<T> first, RouterFunction<T> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public Mono<HandlerFunction<T>> route(ServerRequest request) {
			return Flux.concat(this.first.route(request), Mono.defer(() -> this.second.route(request)))
					.next();
		}

		@Override
		public void accept(Visitor visitor) {
			this.first.accept(visitor);
			this.second.accept(visitor);
		}
	}


	/**
	 * 组合路由函数，先调用一个函数，如果此路由{@linkplain Mono#empty() 没有结果}，则调用另一个函数（不同响应类型）。
	 */
	static final class DifferentComposedRouterFunction extends AbstractRouterFunction<ServerResponse> {

		private final RouterFunction<?> first;

		private final RouterFunction<?> second;

		public DifferentComposedRouterFunction(RouterFunction<?> first, RouterFunction<?> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			return Flux.concat(this.first.route(request), Mono.defer(() -> this.second.route(request)))
					.next()
					.map(this::cast);
		}

		@SuppressWarnings("unchecked")
		private <T extends ServerResponse> HandlerFunction<T> cast(HandlerFunction<?> handlerFunction) {
			return (HandlerFunction<T>) handlerFunction;
		}

		@Override
		public void accept(Visitor visitor) {
			this.first.accept(visitor);
			this.second.accept(visitor);
		}
	}


	/**
	 * 使用给定的{@linkplain HandlerFilterFunction 过滤器函数}过滤指定的{@linkplain HandlerFunction 处理函数}。
	 * @param <T> the type of the {@linkplain HandlerFunction handler function} to filter
	 * @param <S> the type of the response of the function
	 */
	static final class FilteredRouterFunction<T extends ServerResponse, S extends ServerResponse>
			implements RouterFunction<S> {

		private final RouterFunction<T> routerFunction;

		private final HandlerFilterFunction<T, S> filterFunction;

		public FilteredRouterFunction(
				RouterFunction<T> routerFunction,
				HandlerFilterFunction<T, S> filterFunction) {
			this.routerFunction = routerFunction;
			this.filterFunction = filterFunction;
		}

		@Override
		public Mono<HandlerFunction<S>> route(ServerRequest request) {
			return this.routerFunction.route(request).map(this.filterFunction::apply);
		}

		@Override
		public void accept(Visitor visitor) {
			this.routerFunction.accept(visitor);
		}

		@Override
		public String toString() {
			return this.routerFunction.toString();
		}
	}


	private static final class DefaultRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		private final RequestPredicate predicate;

		private final HandlerFunction<T> handlerFunction;

		public DefaultRouterFunction(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(handlerFunction, "HandlerFunction must not be null");
			this.predicate = predicate;
			this.handlerFunction = handlerFunction;
		}

		@Override
		public Mono<HandlerFunction<T>> route(ServerRequest request) {
			if (this.predicate.test(request)) {
				if (logger.isTraceEnabled()) {
					String logPrefix = request.exchange().getLogPrefix();
					logger.trace(logPrefix + String.format("Matched %s", this.predicate));
				}
				return Mono.just(this.handlerFunction);
			}
			else {
				return Mono.empty();
			}
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.route(this.predicate, this.handlerFunction);
		}

	}


	private static final class DefaultNestedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		private final RequestPredicate predicate;

		private final RouterFunction<T> routerFunction;

		public DefaultNestedRouterFunction(RequestPredicate predicate, RouterFunction<T> routerFunction) {
			Assert.notNull(predicate, "Predicate must not be null");
			Assert.notNull(routerFunction, "RouterFunction must not be null");
			this.predicate = predicate;
			this.routerFunction = routerFunction;
		}

		@Override
		public Mono<HandlerFunction<T>> route(ServerRequest serverRequest) {
			return this.predicate.nest(serverRequest)
					.map(nestedRequest -> {
								if (logger.isTraceEnabled()) {
									String logPrefix = serverRequest.exchange().getLogPrefix();
									logger.trace(logPrefix + String.format("Matched nested %s", this.predicate));
								}
								return this.routerFunction.route(nestedRequest)
										.doOnNext(match -> {
											if (nestedRequest != serverRequest) {
												serverRequest.attributes().clear();
												serverRequest.attributes()
														.putAll(nestedRequest.attributes());
											}
										});
							}
					).orElseGet(Mono::empty);
		}


		@Override
		public void accept(Visitor visitor) {
			visitor.startNested(this.predicate);
			this.routerFunction.accept(visitor);
			visitor.endNested(this.predicate);
		}

	}


	private static class ResourcesRouterFunction extends  AbstractRouterFunction<ServerResponse> {

		private final Function<ServerRequest, Mono<Resource>> lookupFunction;

		public ResourcesRouterFunction(Function<ServerRequest, Mono<Resource>> lookupFunction) {
			Assert.notNull(lookupFunction, "Function must not be null");
			this.lookupFunction = lookupFunction;
		}

		@Override
		public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			return this.lookupFunction.apply(request).map(ResourceHandlerFunction::new);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.resources(this.lookupFunction);
		}
	}


	static final class AttributesRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

		private final RouterFunction<T> delegate;

		private final Map<String,Object> attributes;

		public AttributesRouterFunction(RouterFunction<T> delegate, Map<String, Object> attributes) {
			this.delegate = delegate;
			this.attributes = initAttributes(attributes);
		}

		private static Map<String, Object> initAttributes(Map<String, Object> attributes) {
			if (attributes.isEmpty()) {
				return Collections.emptyMap();
			}
			else {
				return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
			}
		}

		@Override
		public Mono<HandlerFunction<T>> route(ServerRequest request) {
			return this.delegate.route(request);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.attributes(this.attributes);
			this.delegate.accept(visitor);
		}

		@Override
		public RouterFunction<T> withAttribute(String name, Object value) {
			Assert.hasLength(name, "Name must not be empty");
			Assert.notNull(value, "Value must not be null");

			Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
			attributes.put(name, value);
			return new AttributesRouterFunction<>(this.delegate, attributes);
		}

		@Override
		public RouterFunction<T> withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
			Assert.notNull(attributesConsumer, "AttributesConsumer must not be null");

			Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
			attributesConsumer.accept(attributes);
			return new AttributesRouterFunction<>(this.delegate, attributes);
		}
	}


	private static class HandlerStrategiesResponseContext implements ServerResponse.Context {

		private final HandlerStrategies strategies;

		public HandlerStrategiesResponseContext(HandlerStrategies strategies) {
			this.strategies = strategies;
		}

		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return this.strategies.messageWriters();
		}

		@Override
		public List<ViewResolver> viewResolvers() {
			return this.strategies.viewResolvers();
		}
	}


	private static class RouterFunctionWebHandler implements WebHandler {

		private final HandlerStrategies strategies;

		private final RouterFunction<?> routerFunction;

		public RouterFunctionWebHandler(HandlerStrategies strategies, RouterFunction<?> routerFunction) {
			this.strategies = strategies;
			this.routerFunction = routerFunction;
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			return Mono.defer(() -> {
				ServerRequest request = new DefaultServerRequest(exchange, this.strategies.messageReaders());
				addAttributes(exchange, request);
				return this.routerFunction.route(request)
						.switchIfEmpty(createNotFoundError())
						.flatMap(handlerFunction -> wrapException(() -> handlerFunction.handle(request)))
						.flatMap(response -> wrapException(() -> response.writeTo(exchange,
								new HandlerStrategiesResponseContext(this.strategies))));
			});
		}

		private void addAttributes(ServerWebExchange exchange, ServerRequest request) {
			Map<String, Object> attributes = exchange.getAttributes();
			attributes.put(REQUEST_ATTRIBUTE, request);
		}

		private <R> Mono<R> createNotFoundError() {
			return Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
					"No matching router function")));
		}

		private static <T> Mono<T> wrapException(Supplier<Mono<T>> supplier) {
			try {
				return supplier.get();
			}
			catch (Throwable ex) {
				return Mono.error(ex);
			}
		}
	}
}
