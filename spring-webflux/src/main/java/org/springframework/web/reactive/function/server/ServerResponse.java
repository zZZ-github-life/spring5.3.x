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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * 表示类型化的服务器端 HTTP 响应，由{@linkplain HandlerFunction 处理函数}或{@linkplain HandlerFilterFunction 过滤器函数}返回。
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerResponse {

	/**
	 * 返回此响应的状态码。
	 * @return 以 HttpStatus 枚举值表示的状态
	 * @throws IllegalArgumentException 在 HTTP 状态码未知的情况下
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus statusCode();

	/**
	 * 返回此响应的（可能非标准的）状态码。
	 * @return 以整数表示的状态
	 * @since 5.2
	 * @see #statusCode()
	 * @see HttpStatus#resolve(int)
	 */
	int rawStatusCode();

	/**
	 * 返回此响应的头。
	 */
	HttpHeaders headers();

	/**
	 * 返回此响应的 cookie。
	 */
	MultiValueMap<String, ResponseCookie> cookies();

	/**
	 * 将此响应写入给定的 Web 交换。
	 * @param exchange 要写入的 Web 交换
	 * @param context 写入时使用的上下文
	 * @return 写入完成时发出信号的 {@code Mono<Void>}
	 */
	Mono<Void> writeTo(ServerWebExchange exchange, Context context);


	// Static methods

	/**
	 * 使用给定响应的状态码和头创建构建器。
	 * @param other 要从中复制状态和标头的响应
	 * @return 创建的构建器
	 */
	static BodyBuilder from(ServerResponse other) {
		return new DefaultServerResponseBuilder(other);
	}

	/**
	 * 使用给定 HTTP 状态创建构建器。
	 * @param status 响应状态
	 * @return 创建的构建器
	 */
	static BodyBuilder status(HttpStatus status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * 使用给定 HTTP 状态创建构建器。
	 * @param status 响应状态
	 * @return 创建的构建器
	 * @since 5.0.3
	 */
	static BodyBuilder status(int status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * 创建状态设置为 {@linkplain HttpStatus#OK 200 OK} 的构建器。
	 * @return 创建的构建器
	 */
	static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * 创建新构建器，状态为 {@linkplain HttpStatus#CREATED 201 Created}，并将 location 头设置为给定 URI。
	 * @param location 位置 URI
	 * @return 创建的构建器
	 */
	static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#ACCEPTED 202 Accepted} 的构建器。
	 * @return 创建的构建器
	 */
	static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#NO_CONTENT 204 No Content} 的构建器。
	 * @return 创建的构建器
	 */
	static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#SEE_OTHER 303 See Other} 且 location 头设置为给定 URI 的构建器。
	 * @param location 位置 URI
	 * @return 创建的构建器
	 */
	static BodyBuilder seeOther(URI location) {
		BodyBuilder builder = status(HttpStatus.SEE_OTHER);
		return builder.location(location);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#TEMPORARY_REDIRECT 307 Temporary Redirect} 且 location 头设置为给定 URI 的构建器。
	 * @param location 位置 URI
	 * @return 创建的构建器
	 */
	static BodyBuilder temporaryRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.TEMPORARY_REDIRECT);
		return builder.location(location);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#PERMANENT_REDIRECT 308 Permanent Redirect} 且 location 头设置为给定 URI 的构建器。
	 * @param location 位置 URI
	 * @return 创建的构建器
	 */
	static BodyBuilder permanentRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.PERMANENT_REDIRECT);
		return builder.location(location);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#BAD_REQUEST 400 Bad Request} 的构建器。
	 * @return 创建的构建器
	 */
	static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#NOT_FOUND 404 Not Found} 的构建器。
	 * @return 创建的构建器
	 */
	static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#UNPROCESSABLE_ENTITY 422 Unprocessable Entity} 的构建器。
	 * @return 创建的构建器
	 */
	static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}


	/**
	 * 定义用于向响应添加头的构建器。
	 * @param <B> 构建器子类
	 */
	interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * 在给定名称下添加给定的头值。
		 * @param headerName 标头名称
		 * @param headerValues 标头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此响应的头。提供给消费者的头是“实时”的，因此消费者可用于{@linkplain HttpHeaders#set(String, String) 覆盖}现有头值、{@linkplain HttpHeaders#remove(Object) 删除}值，或使用任何其他 {@link HttpHeaders} 方法。
		 * @param headersConsumer 用于消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 将给定 cookie 添加到响应中。
		 * @param cookie 要添加的 Cookie
		 * @return 此构建器
		 */
		B cookie(ResponseCookie cookie);

		/**
		 * 使用给定的消费者操作此响应的 cookie。提供给消费者的 cookie 是“实时”的，因此消费者可用于{@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 cookie、{@linkplain MultiValueMap#remove(Object) 删除} cookie，或使用任何其他 {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 用于消费 Cookie 的函数
		 * @return 此构建器
		 */
		B cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法}集合，由 {@code Allow} 头指定。
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法}集合，由 {@code Allow} 头指定。
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(Set<HttpMethod> allowedMethods);

		/**
		 * 设置正文的实体标签，由 {@code ETag} 头指定。
		 * @param eTag 新的实体标签
		 * @return 此构建器
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String eTag);

		/**
		 * 设置资源的最后修改时间，由 {@code Last-Modified} 头指定。
		 * @param lastModified 最后修改日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * 设置资源的最后修改时间，由 {@code Last-Modified} 头指定。
		 * @param lastModified 最后修改日期
		 * @return 此构建器
		 * @since 5.1.4
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(Instant lastModified);

		/**
		 * 设置资源的位置，由 {@code Location} 头指定。
		 * @param location 位置
		 * @return 此构建器
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * 设置资源的缓存指令，由 HTTP 1.1 的 {@code Cache-Control} 头指定。
		 * <p>{@code CacheControl} 实例可以这样构建：{@code CacheControl.maxAge(3600).cachePublic().noTransform()}。
		 * @param cacheControl 用于缓存相关 HTTP 响应标头的构建器
		 * @return 此构建器
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求头名称（例如 "Accept-Language"），将其添加到 "Vary" 响应头，以告知客户端响应会受到内容协商影响，并会因给定请求头的值而变化。仅当响应 "Vary" 头中尚不存在时，才会添加所配置的请求头名称。
		 * @param requestHeaders 请求标头名称
		 * @return 此构建器
		 */
		B varyBy(String... requestHeaders);

		/**
		 * 构建不带正文的响应实体。
		 */
		Mono<ServerResponse> build();

		/**
		 * 构建不带正文的响应实体。
		 * <p>当给定的 {@code voidPublisher} 完成时，响应将被提交。
		 * @param voidPublisher 指示响应应何时提交的发布者
		 */
		Mono<ServerResponse> build(Publisher<Void> voidPublisher);

		/**
		 * 使用自定义写出函数构建响应实体。
		 * @param writeFunction 用于写入 {@link ServerWebExchange} 的函数
		 */
		Mono<ServerResponse> build(BiFunction<ServerWebExchange, Context, Mono<Void>> writeFunction);
	}


	/**
	 * 定义用于向响应添加正文的构建器。
	 */
	interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * 设置正文的字节长度，由 {@code Content-Length} 头指定。
		 * @param contentLength 内容长度
		 * @return 此构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置正文的{@linkplain MediaType 媒体类型}，由 {@code Content-Type} 头指定。
		 * @param contentType 内容类型
		 * @return 此构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 添加序列化提示（例如 {@link Jackson2CodecSupport#JSON_VIEW_HINT}）以自定义正文的序列化方式。
		 * @param key 提示键
		 * @param value 提示值
		 */
		BodyBuilder hint(String key, Object value);

		/**
		 * 使用给定的消费者自定义序列化提示。
		 * @param hintsConsumer 用于消费提示的函数
		 * @return 此构建器
		 * @since 5.1.6
		 */
		BodyBuilder hints(Consumer<Map<String, Object>> hintsConsumer);

		/**
		 * 将响应正文设置为给定 {@code Object} 并返回构建器。这是使用 {@link #body(BodyInserter)} 和{@linkplain BodyInserters#fromValue 值插入器}的快捷方式。
		 * @param body 响应正文
		 * @return 构建出的响应
		 * @throws IllegalArgumentException 如果 {@code body} 是 {@link Publisher}，
		 * 或者是 {@link ReactiveAdapterRegistry} 已知的生产者
		 * @since 5.2
		 */
		Mono<ServerResponse> bodyValue(Object body);

		/**
		 * 从给定 {@code Publisher} 设置正文。这是使用 {@link #body(BodyInserter)} 和{@linkplain BodyInserters#fromPublisher Publisher 插入器}的快捷方式。
		 * @param publisher 要写入响应的 {@code Publisher}
		 * @param elementClass 发布的元素类型
		 * @param <T> 发布者中包含的元素类型
		 * @param <P> {@code Publisher} 的类型
		 * @return 构建出的响应
		 */
		<T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher, Class<T> elementClass);

		/**
		 * {@link #body(Publisher, Class)} 的变体，允许使用可通过 {@link ReactiveAdapterRegistry} 解析为 {@link Publisher} 的任何生产者。
		 * @param publisher 用于写入响应的 {@code Publisher}
		 * @param elementTypeRef 生成的元素类型
		 * @param <T> 发布者中包含的元素类型
		 * @param <P> {@code Publisher} 的类型
		 * @return 构建出的响应
		 */
		<T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher,
				ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * {@link #body(Publisher, Class)} 的变体，允许使用可通过 {@link ReactiveAdapterRegistry} 解析为 {@link Publisher} 的任何生产者。
		 * @param producer 要写入请求的生产者
		 * @param elementClass 生成的元素类型
		 * @return 构建出的响应
		 * @since 5.2
		 */
		Mono<ServerResponse> body(Object producer, Class<?> elementClass);

		/**
		 * {@link #body(Publisher, ParameterizedTypeReference)} 的变体，允许使用可通过 {@link ReactiveAdapterRegistry} 解析为 {@link Publisher} 的任何生产者。
		 * @param producer 要写入响应的生产者
		 * @param elementTypeRef 生成的元素类型
		 * @return 构建出的响应
		 * @since 5.2
		 */
		Mono<ServerResponse> body(Object producer, ParameterizedTypeReference<?> elementTypeRef);

		/**
		 * 将响应正文设置为给定 {@code BodyInserter} 并返回构建器。
		 * @param inserter 写入响应的 {@code BodyInserter}
		 * @return 构建出的响应
		 */
		Mono<ServerResponse> body(BodyInserter<?, ? super ServerHttpResponse> inserter);

		/**
		 * 将响应正文设置为给定 {@code Object} 并返回构建器。从 5.2 开始，此方法委托给 {@link #bodyValue(Object)}。
		 * @deprecated as of Spring Framework 5.2 in favor of {@link #bodyValue(Object)}
		 */
		@Deprecated
		Mono<ServerResponse> syncBody(Object body);

		/**
		 * 使用给定的 {@code modelAttributes} 渲染具有给定 {@code name} 的模板。模型属性以{@linkplain org.springframework.core.Conventions#getVariableName 生成的名称}映射。
		 * <p><em>注意：使用此方法时，空的 {@link Collection Collections} 不会添加到模型中，因为我们无法正确确定真实的约定名称。</em>
		 * @param name 要渲染的模板名称
		 * @param modelAttributes 用于渲染模板的 modelAttributes
		 * @return 构建出的响应
		 */
		Mono<ServerResponse> render(String name, Object... modelAttributes);

		/**
		 * 使用给定 {@code model} 渲染具有给定 {@code name} 的模板。
		 * @param name 要渲染的模板名称
		 * @param model 用于渲染模板的模型
		 * @return 构建出的响应
		 */
		Mono<ServerResponse> render(String name, Map<String, ?> model);
	}


	/**
	 * 定义在 {@link #writeTo(ServerWebExchange, Context)} 期间使用的上下文。
	 */
	interface Context {

		/**
		 * 返回用于响应正文转换的 {@link HttpMessageWriter HttpMessageWriters}。
		 * @return 消息写入器列表
		 */
		List<HttpMessageWriter<?>> messageWriters();

		/**
		 * 返回用于视图名称解析的 {@link ViewResolver ViewResolvers}。
		 * @return 视图解析器列表
		 */
		List<ViewResolver> viewResolvers();
	}

}
