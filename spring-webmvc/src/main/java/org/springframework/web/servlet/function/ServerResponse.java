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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

/**
 * 表示有类型的服务器端 HTTP 响应，由
 * {@linkplain HandlerFunction 处理器函数}或
 * {@linkplain HandlerFilterFunction 过滤函数}返回。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public interface ServerResponse {

	/**
	 * 返回此响应的状态码。
	 * @return 状态，以 HttpStatus 枚举值表示
	 * @throws IllegalArgumentException 在 HTTP 状态码未知的情况下
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus statusCode();

	/**
	 * 返回此响应的（可能是非标准的）状态码。
	 * @return 以整数表示的状态
	 * @see #statusCode()
	 * @see HttpStatus#valueOf(int)
	 */
	int rawStatusCode();

	/**
	 * 返回此响应的响应头。
	 */
	HttpHeaders headers();

	/**
	 * 返回此响应的 Cookie。
	 */
	MultiValueMap<String, Cookie> cookies();

	/**
	 * 将此响应写入给定的 Servlet 响应。
	 * @param request 当前请求
	 * @param response 要写入的响应
	 * @param context 写入时使用的上下文
	 * @return 要渲染的 {@code ModelAndView}；如果已直接处理，则为 {@code null}
	 */
	@Nullable
	ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context)
		throws ServletException, IOException;


	// Static methods

	/**
	 * 创建带有给定响应的状态码和响应头的构建器。
	 * @param other 要从中复制状态和头的响应
	 * @return 所创建的构建器
	 */
	static BodyBuilder from(ServerResponse other) {
		return new DefaultServerResponseBuilder(other);
	}

	/**
	 * 创建带有给定 HTTP 状态的构建器。
	 * @param status 响应状态
	 * @return 所创建的构建器
	 */
	static BodyBuilder status(HttpStatus status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * 创建带有给定 HTTP 状态的构建器。
	 * @param status 响应状态
	 * @return 所创建的构建器
	 */
	static BodyBuilder status(int status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * 创建状态设置为 {@linkplain HttpStatus#OK 200 OK} 的构建器。
	 * @return 所创建的构建器
	 */
	static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#CREATED 201 Created}、
	 * 且 location 请求头设置为给定 URI 的构建器。
	 * @param location 位置 URI
	 * @return 所创建的构建器
	 */
	static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#ACCEPTED 202 Accepted} 的构建器。
	 * @return 所创建的构建器
	 */
	static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#NO_CONTENT 204 No Content} 的构建器。
	 * @return 所创建的构建器
	 */
	static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#SEE_OTHER 303 See Other}、
	 * 且 location 请求头设置为给定 URI 的构建器。
	 * @param location 位置 URI
	 * @return 所创建的构建器
	 */
	static BodyBuilder seeOther(URI location) {
		BodyBuilder builder = status(HttpStatus.SEE_OTHER);
		return builder.location(location);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#TEMPORARY_REDIRECT 307 Temporary Redirect}、
	 * 且 location 请求头设置为给定 URI 的构建器。
	 * @param location 位置 URI
	 * @return 所创建的构建器
	 */
	static BodyBuilder temporaryRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.TEMPORARY_REDIRECT);
		return builder.location(location);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#PERMANENT_REDIRECT 308 Permanent Redirect}、
	 * 且 location 请求头设置为给定 URI 的构建器。
	 * @param location 位置 URI
	 * @return 所创建的构建器
	 */
	static BodyBuilder permanentRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.PERMANENT_REDIRECT);
		return builder.location(location);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#BAD_REQUEST 400 Bad Request} 的构建器。
	 * @return 所创建的构建器
	 */
	static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 创建状态为 {@linkplain HttpStatus#NOT_FOUND 404 Not Found} 的构建器。
	 * @return 所创建的构建器
	 */
	static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * 创建状态为
	 * {@linkplain HttpStatus#UNPROCESSABLE_ENTITY 422 Unprocessable Entity} 的构建器。
	 * @return 所创建的构建器
	 */
	static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * 使用给定的异步响应创建（已构建的）响应。
	 * 参数 {@code asyncResponse} 可以是
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} 或
	 * {@link Publisher Publisher&lt;ServerResponse&gt;}（也可以是任何
	 * 可通过 {@link ReactiveAdapterRegistry} 适配的单值异步
	 * {@code ServerResponse} 生产者）。
	 *
	 * <p>此方法可用于基于异步结果设置响应状态码、响应头和
	 * 响应体。如果只有响应体是异步的，则
	 * 可以改用 {@link BodyBuilder#body(Object)}。
	 * @param asyncResponse {@code CompletableFuture<ServerResponse>} 或
	 * {@code Publisher<ServerResponse>}
	 * @return 异步响应
	 * @since 5.3
	 */
	static ServerResponse async(Object asyncResponse) {
		return DefaultAsyncServerResponse.create(asyncResponse, null);
	}

	/**
	 * 使用给定的异步响应创建（已构建的）响应。
	 * 参数 {@code asyncResponse} 可以是
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} 或
	 * {@link Publisher Publisher&lt;ServerResponse&gt;}（也可以是任何
	 * 可通过 {@link ReactiveAdapterRegistry} 适配的单值异步
	 * {@code ServerResponse} 生产者）。
	 *
	 * <p>此方法可用于基于异步结果设置响应状态码、响应头和
	 * 响应体。如果只有响应体是异步的，则
	 * 可以改用 {@link BodyBuilder#body(Object)}。
	 * @param asyncResponse {@code CompletableFuture<ServerResponse>} 或
	 * {@code Publisher<ServerResponse>}
	 * @param timeout 超时前等待的最长时间
	 * @return 异步响应
	 * @since 5.3.2
	 */
	static ServerResponse async(Object asyncResponse, Duration timeout) {
		return DefaultAsyncServerResponse.create(asyncResponse, timeout);
	}

	/**
	 * 创建服务器发送事件响应。提供给 {@code consumer} 的
	 * {@link SseBuilder} 可用于构建和发送事件。
	 *
	 * <p>例如：
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse.send("Hello World!"));
	 * }
	 * </pre>
	 *
	 * <p>或者，同时设置 id 和事件类型：
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse
	 *         .id("42)
	 *         .event("event")
	 *         .send("Hello World!"));
	 * }
	 * </pre>
	 * @param consumer 将被提供事件构建器的消费者
	 * @return 服务器端事件响应
	 * @since 5.3.2
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>
	 */
	static ServerResponse sse(Consumer<SseBuilder> consumer) {
		return SseServerResponse.create(consumer, null);
	}

	/**
	 * 创建服务器发送事件响应。提供给 {@code consumer} 的
	 * {@link SseBuilder} 可用于构建和发送事件。
	 *
	 * <p>例如：
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse.send("Hello World!"));
	 * }
	 * </pre>
	 *
	 * <p>或者，同时设置 id 和事件类型：
	 * <pre class="code">
	 * public ServerResponse handleSse(ServerRequest request) {
	 *     return ServerResponse.sse(sse -&gt; sse
	 *         .id("42)
	 *         .event("event")
	 *         .send("Hello World!"));
	 * }
	 * </pre>
	 * @param consumer 将被提供事件构建器的消费者
	 * @param timeout 超时前等待的最长时间
	 * @return 服务器端事件响应
	 * @since 5.3.2
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>
	 */
	static ServerResponse sse(Consumer<SseBuilder> consumer, Duration timeout) {
		return SseServerResponse.create(consumer, timeout);
	}


	/**
	 * 定义向响应添加请求头的构建器。
	 * @param <B> 构建器子类
	 */
	interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * 在给定名称下添加给定的请求头值。
		 * @param headerName 头名称
		 * @param headerValues 头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此响应的请求头。
		 * 提供给消费者的请求头是“实时”的，因此消费者可以使用它
		 * {@linkplain HttpHeaders#set(String, String) 覆盖}现有请求头值、
		 * {@linkplain HttpHeaders#remove(Object) 移除}值，或使用任何其他
		 * {@link HttpHeaders} 方法。
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 将给定 Cookie 添加到响应。
		 * @param cookie 要添加的 Cookie
		 * @return 此构建器
		 */
		B cookie(Cookie cookie);

		/**
		 * 使用给定的消费者操作此响应的 Cookie。
		 * 提供给消费者的 Cookie 是“实时”的，因此消费者可以使用它
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 Cookie、
		 * {@linkplain MultiValueMap#remove(Object) 移除} Cookie，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 消费 Cookie 的函数
		 * @return 此构建器
		 */
		B cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法}集合，由
		 * {@code Allow} 请求头指定。
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法}集合，由
		 * {@code Allow} 请求头指定。
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(Set<HttpMethod> allowedMethods);

		/**
		 * 设置响应体的实体标签，由 {@code ETag} 请求头指定。
		 * @param eTag 新的实体标签
		 * @return 此构建器
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String eTag);

		/**
		 * 设置资源最后更改的时间，由
		 * {@code Last-Modified} 请求头指定。
		 * @param lastModified 最后修改日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * 设置资源最后更改的时间，由
		 * {@code Last-Modified} 请求头指定。
		 * @param lastModified 最后修改日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(Instant lastModified);
		/**
		 * 设置资源的位置，由 {@code Location} 请求头指定。
		 * @param location 位置
		 * @return 此构建器
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * 设置资源的缓存指令，由 HTTP 1.1
		 * {@code Cache-Control} 请求头指定。
		 * <p>{@code CacheControl} 实例可以像
		 * {@code CacheControl.maxAge(3600).cachePublic().noTransform()} 这样构建。
		 * @param cacheControl 用于缓存相关 HTTP 响应头的构建器
		 * @return 此构建器
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求头名称（例如 "Accept-Language"），将其
		 * 添加到 "Vary" 响应头，以告知客户端响应会进行内容协商，并
		 * 根据给定请求头的值发生变化。仅当响应 "Vary" 请求头中
		 * 尚不存在时，才会添加所配置的请求头名称。
		 * @param requestHeaders 请求头名称
		 * @return 此构建器
		 */
		B varyBy(String... requestHeaders);

		/**
		 * 构建不带响应体的响应实体。
		 */
		ServerResponse build();

		/**
		 * 使用自定义写入函数构建响应实体。
		 * @param writeFunction 用于写入 {@link HttpServletResponse} 的函数
		 */
		ServerResponse build(BiFunction<HttpServletRequest, HttpServletResponse,
				ModelAndView> writeFunction);

	}


	/**
	 * 定义向响应添加响应体的构建器。
	 */
	interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * 设置响应体的长度（以字节为单位），由
		 * {@code Content-Length} 请求头指定。
		 * @param contentLength 内容长度
		 * @return 此构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置响应体的 {@linkplain MediaType 媒体类型}，由
		 * {@code Content-Type} 请求头指定。
		 * @param contentType 内容类型
		 * @return 此构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 将响应体设置为给定 {@code Object} 并返回
		 * 该响应。
		 *
		 * <p>通过将 {@link CompletionStage} 或 {@link Publisher} 作为
		 * 响应体（或任何可通过 {@link ReactiveAdapterRegistry} 适配的单实体
		 * 异步生产者），支持异步响应体。
		 * @param body 响应的消息体
		 * @return 所构建的响应
		 */
		ServerResponse body(Object body);

		/**
		 * 将响应体设置为给定 {@code Object} 并返回该响应。参数
		 * {@code bodyType} 用于捕获泛型类型。
		 * @param body 响应的消息体
		 * @param bodyType 消息体的类型，用于捕获泛型类型
		 * @return 所构建的响应
		 */
		<T> ServerResponse body(T body, ParameterizedTypeReference<T> bodyType);

		/**
		 * 使用给定的 {@code modelAttributes} 渲染名为给定 {@code name} 的模板。
		 * 模型属性会按
		 * {@linkplain org.springframework.core.Conventions#getVariableName 生成的名称}进行映射。
		 * <p><em>注意：使用此方法时，空的 {@link Collection Collections}
		 * 不会添加到模型中，因为我们无法正确确定
		 * 真实的约定名称。</em>
		 * @param name 要渲染的模板名称
		 * @param modelAttributes 用于渲染模板的 modelAttributes
		 * @return 所构建的响应
		 */
		ServerResponse render(String name, Object... modelAttributes);

		/**
		 * 使用给定的 {@code model} 渲染名为给定 {@code name} 的模板。
		 * @param name 要渲染的模板名称
		 * @param model 用于渲染模板的模型
		 * @return 所构建的响应
		 */
		ServerResponse render(String name, Map<String, ?> model);
	}


	/**
	 * 定义用于发送服务器发送事件的响应体构建器。
	 *
	 * @since 5.3.2
	 */
	interface SseBuilder {

		/**
		 * 将给定对象作为服务器发送事件发送。
		 * 字符串将作为 UTF-8 编码字节发送，其他对象将
		 * 使用{@linkplain HttpMessageConverter 消息转换器}转换为 JSON。
		 *
		 * <p>此便捷方法与
		 * {@link #data(Object)} 的效果相同。
		 * @param object 要发送的对象
		 * @throws IOException 在出现 I/O 错误的情况下
		 */
		void send(Object object) throws IOException;

		/**
		 * 添加一行 SSE "id"。
		 * @param id 事件标识符
		 * @return 此构建器
		 */
		SseBuilder id(String id);

		/**
		 * 添加一行 SSE "event"。
		 * @param eventName 事件名称
		 * @return 此构建器
		 */
		SseBuilder event(String eventName);

		/**
		 * 添加一行 SSE "retry"。
		 * @param duration 要转换为毫秒的时长
		 * @return 此构建器
		 */
		SseBuilder retry(Duration duration);

		/**
		 * 添加一条 SSE 注释。
		 * @param comment 注释
		 * @return 此构建器
		 */
		SseBuilder comment(String comment);

		/**
		 * 为给定对象添加一行 SSE "data"，并将构建的
		 * 服务器发送事件发送到客户端。
		 * 字符串将作为 UTF-8 编码字节发送，其他对象将
		 * 使用{@linkplain HttpMessageConverter 消息转换器}转换为 JSON。
		 * @param object 要作为数据发送的对象
		 * @throws IOException 在出现 I/O 错误的情况下
		 */
		void data(Object object) throws IOException;

		/**
		 * 使用给定的错误完成事件流。
		 *
		 * <p>该 throwable 会被分派回 Spring MVC，并传递给
		 * 其异常处理机制。由于此时响应
		 * 已提交，因此响应状态无法更改。
		 * @param t 要分派的 throwable
		 */
		void error(Throwable t);

		/**
		 * 完成事件流。
		 */
		void complete();

		/**
		 * 注册一个回调，在 SSE 请求超时时调用。
		 * @param onTimeout 超时时调用的回调
		 * @return 此构建器
		 */
		SseBuilder onTimeout(Runnable onTimeout);

		/**
		 * 注册一个回调，在 SSE 处理期间发生错误时调用。
		 * @param onError 出错时调用的回调
		 * @return 此构建器
		 */
		SseBuilder onError(Consumer<Throwable> onError);

		/**
		 * 注册一个回调，在 SSE 请求完成时调用。
		 * @param onCompletion 完成时调用的回调
		 * @return 此构建器
		 */
		SseBuilder onComplete(Runnable onCompletion);
	}


	/**
	 * 定义 {@link #writeTo(HttpServletRequest, HttpServletResponse, Context)} 期间使用的上下文。
	 */
	interface Context {

		/**
		 * 返回用于响应体转换的 {@link HttpMessageConverter HttpMessageConverters}。
		 * @return 消息写入器列表
		 */
		List<HttpMessageConverter<?>> messageConverters();

	}

}
