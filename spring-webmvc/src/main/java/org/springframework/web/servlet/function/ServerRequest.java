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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UriBuilder;

/**
 * 表示由 {@code HandlerFunction} 处理的服务器端 HTTP 请求。
 * 对请求头和请求体的访问分别由 {@link Headers} 和
 * {@link #body(Class)} 提供。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public interface ServerRequest {

	/**
	 * 获取 HTTP 方法。
	 * @return HTTP 方法，以 HttpMethod 枚举值表示；
	 * 如果无法解析（例如非标准 HTTP 方法），则为 {@code null}
	 */
	@Nullable
	default HttpMethod method() {
		return HttpMethod.resolve(methodName());
	}

	/**
	 * 获取 HTTP 方法的名称。
	 * @return 以 String 表示的 HTTP 方法
	 */
	String methodName();

	/**
	 * 获取请求 URI。
	 */
	URI uri();

	/**
	 * 从此 {@code ServerRequest} 关联的 URI 获取
	 * {@code UriBuilderComponents}。
	 * @return URI 构建器
	 */
	UriBuilder uriBuilder();

	/**
	 * 获取请求路径。
	 */
	default String path() {
		return requestPath().pathWithinApplication().value();
	}

	/**
	 * 获取请求路径，类型为 {@code PathContainer}。
	 * @deprecated 自 5.3 起，建议使用 {@link #requestPath()}
	 */
	@Deprecated
	default PathContainer pathContainer() {
		return requestPath();
	}

	/**
	 * 获取请求路径，类型为 {@code PathContainer}。
	 * @since 5.3
	 */
	default RequestPath requestPath() {
		return ServletRequestPathUtils.getParsedRequestPath(servletRequest());
	}

	/**
	 * 获取此请求的请求头。
	 */
	Headers headers();

	/**
	 * 获取此请求的 Cookie。
	 */
	MultiValueMap<String, Cookie> cookies();

	/**
	 * 获取此请求所连接的远程地址（如果可用）。
	 */
	Optional<InetSocketAddress> remoteAddress();

	/**
	 * 获取用于转换此请求体的读取器。
	 */
	List<HttpMessageConverter<?>> messageConverters();

	/**
	 * 将请求体提取为给定类型的对象。
	 * @param bodyType 返回值的类型
	 * @param <T> 消息体类型
	 * @return 消息体
	 */
	<T> T body(Class<T> bodyType) throws ServletException, IOException;

	/**
	 * 将请求体提取为给定类型的对象。
	 * @param bodyType 返回值的类型
	 * @param <T> 消息体类型
	 * @return 消息体
	 */
	<T> T body(ParameterizedTypeReference<T> bodyType) throws ServletException, IOException;

	/**
	 * 获取请求属性值（如果存在）。
	 * @param name 属性名称
	 * @return 属性值
	 */
	default Optional<Object> attribute(String name) {
		Map<String, Object> attributes = attributes();
		if (attributes.containsKey(name)) {
			return Optional.of(attributes.get(name));
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * 获取请求属性的可变映射。
	 * @return 请求属性
	 */
	Map<String, Object> attributes();

	/**
	 * 获取具有给定名称的第一个参数（如果存在）。Servlet
	 * 参数包含在查询字符串或提交的表单数据中。
	 * @param name 参数名称
	 * @return 参数值
	 * @see HttpServletRequest#getParameter(String)
	 */
	default Optional<String> param(String name) {
		List<String> paramValues = params().get(name);
		if (CollectionUtils.isEmpty(paramValues)) {
			return Optional.empty();
		}
		else {
			String value = paramValues.get(0);
			if (value == null) {
				value = "";
			}
			return Optional.of(value);
		}
	}

	/**
	 * 获取此请求的所有参数。Servlet 参数包含
	 * 在查询字符串或提交的表单数据中。
	 * @see HttpServletRequest#getParameterMap()
	 */
	MultiValueMap<String, String> params();

	/**
	 * 获取多部分请求的各部分，前提是 Content-Type 为
	 * {@code "multipart/form-data"}，否则抛出异常。
	 * @return multipart 数据，从名称到部件（一个或多个）的映射
	 * @throws IOException 如果在检索过程中发生 I/O 错误
	 * @throws ServletException 如果此请求的类型不是 {@code "multipart/form-data"}
	 * @since 5.3
	 * @see HttpServletRequest#getParts()
	 */
	MultiValueMap<String, Part> multipartData() throws IOException, ServletException;

	/**
	 * 获取具有给定名称的路径变量（如果存在）。
	 * @param name 变量名称
	 * @return 变量值
	 * @throws IllegalArgumentException 如果不存在具有给定名称的路径变量
	 */
	default String pathVariable(String name) {
		Map<String, String> pathVariables = pathVariables();
		if (pathVariables.containsKey(name)) {
			return pathVariables().get(name);
		}
		else {
			throw new IllegalArgumentException("No path variable with name \"" + name + "\" available");
		}
	}

	/**
	 * 获取此请求的所有路径变量。
	 */
	Map<String, String> pathVariables();

	/**
	 * 获取此请求的 Web 会话。始终保证返回一个实例：它要么
	 * 与客户端请求的会话 ID 匹配，要么是一个新的会话 ID，
	 * 原因可能是客户端未指定会话 ID，或者底层会话已过期。
	 * 使用此方法不会自动创建会话。
	 */
	HttpSession session();

	/**
	 * 获取此请求的已认证用户（如果有）。
	 */
	Optional<Principal> principal();

	/**
	 * 获取此请求所基于的 Servlet 请求。
	 */
	HttpServletRequest servletRequest();

	/**
	 * 在给定最后修改时间戳（由应用程序确定）的前提下，检查所请求的
	 * 资源是否已被修改。
	 * 如果未修改，此方法返回带有相应状态码和响应头的响应，
	 * 否则返回空结果。
	 * <p>典型用法：
	 * <pre class="code">
	 * public ServerResponse myHandleMethod(ServerRequest request) {
	 *   Instant lastModified = // application-specific calculation
	 *	 return request.checkNotModified(lastModified)
	 *	   .orElseGet(() -&gt; {
	 *	     // further request processing, actually building content
	 *		 return ServerResponse.ok().body(...);
	 *	   });
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，也
	 * 适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>你可以使用该
	 * {@code #checkNotModified(Instant)} 方法，或
	 * {@link #checkNotModified(String)}。如果希望同时强制使用
	 * 强实体标签和 Last-Modified 值，
	 * 如 HTTP 规范所建议的，
	 * 则应使用 {@link #checkNotModified(Instant, String)}。
	 * @param lastModified 应用程序为底层资源确定的最后修改时间戳
	 * @return 如果请求可判定为未修改，则返回相应的响应；
	 * 否则返回空结果。
	 * @since 5.2.5
	 */
	default Optional<ServerResponse> checkNotModified(Instant lastModified) {
		Assert.notNull(lastModified, "LastModified must not be null");
		return DefaultServerRequest.checkNotModified(servletRequest(), lastModified, null);
	}

	/**
	 * 在给定 {@code ETag}（实体标签，由应用程序确定）的前提下，
	 * 检查所请求的资源是否已被修改。
	 * 如果未修改，此方法返回带有相应
	 * 状态码和响应头的响应，否则返回空结果。
	 * <p>典型用法：
	 * <pre class="code">
	 * public ServerResponse myHandleMethod(ServerRequest request) {
	 *   String eTag = // application-specific calculation
	 *	 return request.checkNotModified(eTag)
	 *	   .orElseGet(() -&gt; {
	 *	     // further request processing, actually building content
	 *		 return ServerResponse.ok().body(...);
	 *	   });
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，也
	 * 适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>你可以使用
	 * 此 {@link #checkNotModified(Instant)} 方法，或
	 * {@code #checkNotModified(String)}。如果希望同时强制使用
	 * 强实体标签和 Last-Modified 值，
	 * 如 HTTP 规范所建议的，
	 * 则应使用 {@link #checkNotModified(Instant, String)}。
	 * @param etag 应用程序为底层资源确定的实体标签。
	 * 如有必要，此参数将用引号（"）填充。
	 * @return 如果请求可判定为未修改，则返回相应的响应；
	 * 否则返回空结果。
	 * @since 5.2.5
	 */
	default Optional<ServerResponse> checkNotModified(String etag) {
		Assert.notNull(etag, "Etag must not be null");
		return DefaultServerRequest.checkNotModified(servletRequest(), null, etag);
	}

	/**
	 * 在给定的 {@code ETag}（实体标签）和最后修改时间戳
	 * （均由应用程序确定）的前提下，检查所请求的资源是否已被修改。
	 * 如果未修改，此方法返回带有相应
	 * 状态码和响应头的响应，否则返回空结果。
	 * <p>典型用法：
	 * <pre class="code">
	 * public ServerResponse myHandleMethod(ServerRequest request) {
	 *   Instant lastModified = // application-specific calculation
	 *   String eTag = // application-specific calculation
	 *	 return request.checkNotModified(lastModified, eTag)
	 *	   .orElseGet(() -&gt; {
	 *	     // further request processing, actually building content
	 *		 return ServerResponse.ok().body(...);
	 *	   });
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，也
	 * 适用于条件 POST/PUT/DELETE 请求。
	 * @param lastModified 应用程序为底层资源确定的最后修改时间戳
	 * @param etag 应用程序为底层资源确定的实体标签。
	 * 如有必要，此参数将用引号（"）填充。
	 * @return 如果请求可判定为未修改，则返回相应的响应；
	 * 否则返回空结果。
	 * @since 5.2.5
	 */
	default Optional<ServerResponse> checkNotModified(Instant lastModified, String etag) {
		Assert.notNull(lastModified, "LastModified must not be null");
		Assert.notNull(etag, "Etag must not be null");
		return DefaultServerRequest.checkNotModified(servletRequest(), lastModified, etag);
	}


	// Static methods

	/**
	 * 基于给定的 {@code HttpServletRequest} 和
	 * 消息转换器创建新的 {@code ServerRequest}。
	 * @param servletRequest 请求
	 * @param messageReaders 消息读取器
	 * @return 所创建的 {@code ServerRequest}
	 */
	static ServerRequest create(HttpServletRequest servletRequest, List<HttpMessageConverter<?>> messageReaders) {
		return new DefaultServerRequest(servletRequest, messageReaders);
	}

	/**
	 * 创建带有给定请求的状态、响应头和 Cookie 的构建器。
	 * @param other 要从中复制状态、头和 Cookie 的响应
	 * @return 所创建的构建器
	 */
	static Builder from(ServerRequest other) {
		return new DefaultServerRequestBuilder(other);
	}



	/**
	 * 表示 HTTP 请求的请求头。
	 * @see ServerRequest#headers()
	 */
	interface Headers {

		/**
		 * 获取可接受的媒体类型列表，由 {@code Accept}
		 * 请求头指定。
		 * <p>如果未指定可接受的媒体类型，则返回空列表。
		 */
		List<MediaType> accept();

		/**
		 * 获取可接受的字符集列表，由
		 * {@code Accept-Charset} 请求头指定。
		 */
		List<Charset> acceptCharset();

		/**
		 * 获取可接受的语言列表，由
		 * {@code Accept-Language} 请求头指定。
		 */
		List<Locale.LanguageRange> acceptLanguage();

		/**
		 * 获取请求体的长度（以字节为单位），由
		 * {@code Content-Length} 请求头指定。
		 */
		OptionalLong contentLength();

		/**
		 * 获取请求体的媒体类型，由
		 * {@code Content-Type} 请求头指定。
		 */
		Optional<MediaType> contentType();

		/**
		 * 获取 {@code Host} 请求头的值（如果可用）。
		 * <p>如果该请求头的值不包含端口，则返回地址中的
		 * {@linkplain InetSocketAddress#getPort() 端口}为
		 * {@code 0}。
		 */
		@Nullable
		InetSocketAddress host();

		/**
		 * 获取 {@code Range} 请求头的值。
		 * <p>当范围未知时返回空列表。
		 */
		List<HttpRange> range();

		/**
		 * 获取给定名称的请求头的值（可能多个，如果有）。
		 * <p>如果未找到任何请求头值，则返回空列表。
		 * @param headerName 头名称
		 */
		List<String> header(String headerName);

		/**
		 * 获取给定名称的请求头的第一个值（如果有）。
		 * <p>如果未找到任何请求头值，则返回 {@code null}。
		 * @param headerName 头名称
		 * @since 5.2.5
		 */
		@Nullable
		default String firstHeader(String headerName) {
			List<String> list = header(headerName);
			return list.isEmpty() ? null : list.get(0);
		}

		/**
		 * 获取请求头，类型为 {@link HttpHeaders}。
		 */
		HttpHeaders asHttpHeaders();
	}


	/**
	 * 定义请求的构建器。
	 */
	interface Builder {

		/**
		 * 设置请求的方法。
		 * @param method 新的方法
		 * @return 此构建器
		 */
		Builder method(HttpMethod method);

		/**
		 * 设置请求的 URI。
		 * @param uri 新的 URI
		 * @return 此构建器
		 */
		Builder uri(URI uri);

		/**
		 * 在给定名称下添加给定的请求头值。
		 * @param headerName 头名称
		 * @param headerValues 头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 使用给定的消费者操作此请求的请求头。
		 * <p>提供给消费者的请求头是“实时”的，因此消费者可以使用它
		 * {@linkplain HttpHeaders#set(String, String) 覆盖}现有请求头值、
		 * {@linkplain HttpHeaders#remove(Object) 移除}值，或使用任何其他
		 * {@link HttpHeaders} 方法。
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 添加具有给定名称和值的 Cookie。
		 * @param name Cookie 名称
		 * @param values Cookie 值
		 * @return 此构建器
		 */
		Builder cookie(String name, String... values);

		/**
		 * 使用给定的消费者操作此请求的 Cookie。
		 * <p>提供给消费者的映射是“实时”的，因此消费者可以使用它
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 Cookie、
		 * {@linkplain MultiValueMap#remove(Object) 移除} Cookie，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 消费 Cookie 映射的函数
		 * @return 此构建器
		 */
		Builder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * 设置请求体。
		 * <p>调用此方法会
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) 释放}
		 * 构建器现有的请求体。
		 * @param body 新的消息体
		 * @return 此构建器
		 */
		Builder body(byte[] body);

		/**
		 * 将请求体设置为给定字符串的 UTF-8 编码字节。
		 * <p>调用此方法会
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) 释放}
		 * 构建器现有的请求体。
		 * @param body 新的消息体
		 * @return 此构建器
		 */
		Builder body(String body);

		/**
		 * 添加具有给定名称和值的属性。
		 * @param name 属性名称
		 * @param value 属性值
		 * @return 此构建器
		 */
		Builder attribute(String name, Object value);

		/**
		 * 使用给定的消费者操作此请求的属性。
		 * <p>提供给消费者的映射是“实时”的，因此消费者可以使用它
		 * {@linkplain Map#put(Object, Object) 覆盖}现有属性、
		 * {@linkplain Map#remove(Object) 移除}属性，或使用任何其他
		 * {@link Map} 方法。
		 * @param attributesConsumer 消费属性映射的函数
		 * @return 此构建器
		 */
		Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 添加具有给定名称和值的参数。
		 * @param name 参数名称
		 * @param values 参数值
		 * @return 此构建器
		 */
		Builder param(String name, String... values);

		/**
		 * 使用给定的消费者操作此请求的参数。
		 * <p>提供给消费者的映射是“实时”的，因此消费者可以使用它
		 * {@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 Cookie、
		 * {@linkplain MultiValueMap#remove(Object) 移除} Cookie，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 * @param paramsConsumer 消费参数映射的函数
		 * @return 此构建器
		 */
		Builder params(Consumer<MultiValueMap<String, String>> paramsConsumer);

		/**
		 * 设置请求的远程地址。
		 * @param remoteAddress 远程地址
		 * @return 此构建器
		 */
		Builder remoteAddress(InetSocketAddress remoteAddress);

		/**
		 * 构建请求。
		 * @return 所构建的请求
		 */
		ServerRequest build();
	}

}
