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

package org.springframework.http;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriTemplateHandler;

/**
 * 扩展 {@link HttpEntity}，还公开了 HTTP 方法和目标 URL。用于在 {@code RestTemplate} 中准备请求，以及在 {@code @Controller} 方法中表示请求输入。
 * <p>在 {@code RestTemplate} 中的使用示例：
 * <pre class="code">
 * MyRequest body = ...
 * RequestEntity&lt;MyRequest&gt; request = RequestEntity
 *     .post(&quot;https://example.com/{foo}&quot;, &quot;bar&quot;)
 *     .accept(MediaType.APPLICATION_JSON)
 *     .body(body);
 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
 * </pre>
 * <p>在 {@code @Controller} 中的使用示例：
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public void handle(RequestEntity&lt;String&gt; request) {
 *   HttpMethod method = request.getMethod();
 *   URI url = request.getUrl();
 *   String body = request.getBody();
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Parviz Rozikov
 * @since 4.1
 * @param <T> 正文类型
 * @see #getMethod()
 * @see #getUrl()
 * @see org.springframework.web.client.RestOperations#exchange(RequestEntity, Class)
 * @see ResponseEntity
 */
public class RequestEntity<T> extends HttpEntity<T> {

	@Nullable
	private final HttpMethod method;

	@Nullable
	private final URI url;

	@Nullable
	private final Type type;

	/**
	 * 使用方法和 URL 构造，不包含正文和头。
	 * @param method 方法
	 * @param url 该 URL
	 */
	public RequestEntity(HttpMethod method, URI url) {
		this(null, null, method, url, null);
	}

	/**
	 * 使用方法、URL 和正文构造，不包含头。
	 * @param body 正文
	 * @param method 方法
	 * @param url 该 URL
	 */
	public RequestEntity(@Nullable T body, HttpMethod method, URI url) {
		this(body, null, method, url, null);
	}

	/**
	 * 使用方法、URL、正文和类型构造，不包含头。
	 * @param body 正文
	 * @param method 方法
	 * @param url 该 URL
	 * @param type 用于泛型类型解析的类型
	 * @since 4.3
	 */
	public RequestEntity(@Nullable T body, HttpMethod method, URI url, Type type) {
		this(body, null, method, url, type);
	}

	/**
	 * 使用方法、URL 和头构造，不包含正文。
	 * @param headers 标头
	 * @param method 方法
	 * @param url 该 URL
	 */
	public RequestEntity(MultiValueMap<String, String> headers, HttpMethod method, URI url) {
		this(null, headers, method, url, null);
	}

	/**
	 * 使用方法、URL、头和正文构造。
	 * @param body 正文
	 * @param headers 标头
	 * @param method 方法
	 * @param url 该 URL
	 */
	public RequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers,
			@Nullable HttpMethod method, URI url) {

		this(body, headers, method, url, null);
	}

	/**
	 * 使用方法、URL、头、正文和类型构造。
	 * @param body 正文
	 * @param headers 标头
	 * @param method 方法
	 * @param url 该 URL
	 * @param type 用于泛型类型解析的类型
	 * @since 4.3
	 */
	public RequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers,
			@Nullable HttpMethod method, @Nullable URI url, @Nullable Type type) {

		super(body, headers);
		this.method = method;
		this.url = url;
		this.type = type;
	}


	/**
	 * 返回请求的 HTTP 方法。
	 * @return 以 {@code HttpMethod} 枚举值表示的 HTTP 方法
	 */
	@Nullable
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * 返回目标 HTTP 端点的 {@link URI}。
	 * <p><strong>注意：</strong>如果 {@code RequestEntity} 是使用 URI 模板和变量而不是 {@link URI} 实例创建的，则此方法会抛出 {@link UnsupportedOperationException}。这是因为如果没有关于如何展开模板和编码 URI 的更多信息，就无法创建 URI。在这种情况下，{@code URI} 由 {@link org.springframework.web.client.RestTemplate} 借助其配置的 {@link UriTemplateHandler} 来准备。
	 */
	public URI getUrl() {
		if (this.url == null) {
			throw new UnsupportedOperationException(
					"The RequestEntity was created with a URI template and variables, " +
							"and there is not enough information on how to correctly expand and " +
							"encode the URI template. This will be done by the RestTemplate instead " +
							"with help from the UriTemplateHandler it is configured with.");
		}
		return this.url;
	}


	/**
	 * 返回请求正文的类型。
	 * @return 请求的正文类型，如果未知则为 {@code null}
	 * @since 4.3
	 */
	@Nullable
	public Type getType() {
		if (this.type == null) {
			T body = getBody();
			if (body != null) {
				return body.getClass();
			}
		}
		return this.type;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		RequestEntity<?> otherEntity = (RequestEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(this.method, otherEntity.method) &&
				ObjectUtils.nullSafeEquals(this.url, otherEntity.url));
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.method);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.url);
		return hashCode;
	}

	@Override
	public String toString() {
		return format(getMethod(), getUrl().toString(), getBody(), getHeaders());
	}

	static <T> String format(@Nullable HttpMethod httpMethod, String url, @Nullable T body, HttpHeaders headers) {
		StringBuilder builder = new StringBuilder("<");
		builder.append(httpMethod);
		builder.append(' ');
		builder.append(url);
		builder.append(',');
		if (body != null) {
			builder.append(body);
			builder.append(',');
		}
		builder.append(headers);
		builder.append('>');
		return builder.toString();
	}


	// Static builder methods

	/**
	 * 使用给定的方法和 url 创建构建器。
	 * @param method HTTP 方法（GET、POST 等）
	 * @param url 该 URL
	 * @return 创建的构建器
	 */
	public static BodyBuilder method(HttpMethod method, URI url) {
		return new DefaultBodyBuilder(method, url);
	}

	/**
	 * 使用给定的 HTTP 方法、URI 模板和变量创建构建器。
	 * @param method HTTP 方法（GET、POST 等）
	 * @param uriTemplate 要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder method(HttpMethod method, String uriTemplate, Object... uriVariables) {
		return new DefaultBodyBuilder(method, uriTemplate, uriVariables);
	}

	/**
	 * 使用给定的 HTTP 方法、URI 模板和变量创建构建器。
	 * @param method HTTP 方法（GET、POST 等）
	 * @param uriTemplate 要使用的 URI 模板
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder method(HttpMethod method, String uriTemplate, Map<String, ?> uriVariables) {
		return new DefaultBodyBuilder(method, uriTemplate, uriVariables);
	}


	/**
	 * 使用给定 url 创建 HTTP GET 构建器。
	 * @param url 该 URL
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> get(URI url) {
		return method(HttpMethod.GET, url);
	}

	/**
	 * 使用给定的字符串基础 URI 模板创建 HTTP GET 构建器。
	 * @param uriTemplate 要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static HeadersBuilder<?> get(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.GET, uriTemplate, uriVariables);
	}

	/**
	 * 使用给定 url 创建 HTTP HEAD 构建器。
	 * @param url 该 URL
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> head(URI url) {
		return method(HttpMethod.HEAD, url);
	}

	/**
	 * 使用给定的字符串基础 URI 模板创建 HTTP HEAD 构建器。
	 * @param uriTemplate 要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static HeadersBuilder<?> head(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.HEAD, uriTemplate, uriVariables);
	}

	/**
	 * 使用给定 url 创建 HTTP POST 构建器。
	 * @param url 该 URL
	 * @return 创建的构建器
	 */
	public static BodyBuilder post(URI url) {
		return method(HttpMethod.POST, url);
	}

	/**
	 * 使用给定的字符串基础 URI 模板创建 HTTP POST 构建器。
	 * @param uriTemplate 要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder post(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.POST, uriTemplate, uriVariables);
	}

	/**
	 * 使用给定 url 创建 HTTP PUT 构建器。
	 * @param url 该 URL
	 * @return 创建的构建器
	 */
	public static BodyBuilder put(URI url) {
		return method(HttpMethod.PUT, url);
	}

	/**
	 * 使用给定的字符串基础 URI 模板创建 HTTP PUT 构建器。
	 * @param uriTemplate 要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder put(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.PUT, uriTemplate, uriVariables);
	}

	/**
	 * 使用给定 url 创建 HTTP PATCH 构建器。
	 * @param url 该 URL
	 * @return 创建的构建器
	 */
	public static BodyBuilder patch(URI url) {
		return method(HttpMethod.PATCH, url);
	}

	/**
	 * 使用给定的字符串基础 URI 模板创建 HTTP PATCH 构建器。
	 * @param uriTemplate 要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static BodyBuilder patch(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.PATCH, uriTemplate, uriVariables);
	}

	/**
	 * 使用给定 url 创建 HTTP DELETE 构建器。
	 * @param url 该 URL
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> delete(URI url) {
		return method(HttpMethod.DELETE, url);
	}

	/**
	 * 使用给定的字符串基础 URI 模板创建 HTTP DELETE 构建器。
	 * @param uriTemplate 要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static HeadersBuilder<?> delete(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.DELETE, uriTemplate, uriVariables);
	}

	/**
	 * 使用给定 url 创建 HTTP OPTIONS 构建器。
	 * @param url 该 URL
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> options(URI url) {
		return method(HttpMethod.OPTIONS, url);
	}

	/**
	 * 使用给定的字符串基础 URI 模板创建 HTTP OPTIONS 构建器。
	 * @param uriTemplate 要使用的 URI 模板
	 * @param uriVariables 用于扩展 URI 模板的变量
	 * @return 创建的构建器
	 * @since 5.3
	 */
	public static HeadersBuilder<?> options(String uriTemplate, Object... uriVariables) {
		return method(HttpMethod.OPTIONS, uriTemplate, uriVariables);
	}


	/**
	 * 定义向请求实体添加头的构建器。
	 * @param <B> 构建器子类
	 */
	public interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * 在给定名称下添加给定的单个头值。
		 * @param headerName 标头名称
		 * @param headerValues 标头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 将给定的头复制到实体的头映射中。
		 * @param headers 要复制的现有 HttpHeaders
		 * @return 此构建器
		 * @since 5.2
		 * @see HttpHeaders#add(String, String)
		 */
		B headers(@Nullable HttpHeaders headers);

		/**
		 * 使用给定的消费者操作此实体的头。提供给消费者的头是“实时”的，因此消费者可以使用它{@linkplain HttpHeaders#set(String, String) 覆盖}现有头值、{@linkplain HttpHeaders#remove(Object) 移除}值，或使用任何其他 {@link HttpHeaders} 方法。
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 * @since 5.2
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 设置可接受的 {@linkplain MediaType media types} 列表，由 {@code Accept} 头指定。
		 * @param acceptableMediaTypes 可接受的媒体类型
		 */
		B accept(MediaType... acceptableMediaTypes);

		/**
		 * 设置可接受的 {@linkplain Charset charsets} 列表，由 {@code Accept-Charset} 头指定。
		 * @param acceptableCharsets 可接受的字符集
		 */
		B acceptCharset(Charset... acceptableCharsets);

		/**
		 * 设置 {@code If-Modified-Since} 头的值。
		 * @param ifModifiedSince 该标头的新值
		 * @since 5.1.4
		 */
		B ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * 设置 {@code If-Modified-Since} 头的值。
		 * @param ifModifiedSince 该标头的新值
		 * @since 5.1.4
		 */
		B ifModifiedSince(Instant ifModifiedSince);

		/**
		 * 设置 {@code If-Modified-Since} 头的值。
		 * <p>日期应指定为自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数。
		 * @param ifModifiedSince 该标头的新值
		 */
		B ifModifiedSince(long ifModifiedSince);

		/**
		 * 设置 {@code If-None-Match} 头的值。
		 * @param ifNoneMatches 该标头的新值
		 */
		B ifNoneMatch(String... ifNoneMatches);

		/**
		 * 构建不带正文的请求实体。
		 * @return 请求实体
		 * @see BodyBuilder#body(Object)
		 */
		RequestEntity<Void> build();
	}


	/**
	 * 定义向响应实体添加正文的构建器。
	 */
	public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * 设置正文的字节长度，由 {@code Content-Length} 头指定。
		 * @param contentLength 内容长度
		 * @return 此构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置正文的 {@linkplain MediaType media type}，由 {@code Content-Type} 头指定。
		 * @param contentType 内容类型
		 * @return 此构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 设置请求实体的正文并构建 RequestEntity。
		 * @param <T> 正文的类型
		 * @param body 请求实体的正文
		 * @return 构建的请求实体
		 */
		<T> RequestEntity<T> body(T body);

		/**
		 * 设置请求实体的正文和类型并构建 RequestEntity。
		 * @param <T> 正文的类型
		 * @param body 请求实体的正文
		 * @param type 正文的类型，可用于泛型类型解析
		 * @return 构建的请求实体
		 * @since 4.3
		 */
		<T> RequestEntity<T> body(T body, Type type);
	}


	private static class DefaultBodyBuilder implements BodyBuilder {

		private final HttpMethod method;

		private final HttpHeaders headers = new HttpHeaders();

		@Nullable
		private final URI uri;

		@Nullable
		private final String uriTemplate;

		@Nullable
		private final Object[] uriVarsArray;

		@Nullable
		private final Map<String, ?> uriVarsMap;

		DefaultBodyBuilder(HttpMethod method, URI url) {
			this.method = method;
			this.uri = url;
			this.uriTemplate = null;
			this.uriVarsArray = null;
			this.uriVarsMap = null;
		}

		DefaultBodyBuilder(HttpMethod method, String uriTemplate, Object... uriVars) {
			this.method = method;
			this.uri = null;
			this.uriTemplate = uriTemplate;
			this.uriVarsArray = uriVars;
			this.uriVarsMap = null;
		}

		DefaultBodyBuilder(HttpMethod method, String uriTemplate, Map<String, ?> uriVars) {
			this.method = method;
			this.uri = null;
			this.uriTemplate = uriTemplate;
			this.uriVarsArray = null;
			this.uriVarsMap = uriVars;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				this.headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(@Nullable HttpHeaders headers) {
			if (headers != null) {
				this.headers.putAll(headers);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(this.headers);
			return this;
		}

		@Override
		public BodyBuilder accept(MediaType... acceptableMediaTypes) {
			this.headers.setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public BodyBuilder acceptCharset(Charset... acceptableCharsets) {
			this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public BodyBuilder contentLength(long contentLength) {
			this.headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public BodyBuilder contentType(MediaType contentType) {
			this.headers.setContentType(contentType);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(ZonedDateTime ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(Instant ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(long ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
			this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public RequestEntity<Void> build() {
			return buildInternal(null, null);
		}

		@Override
		public <T> RequestEntity<T> body(T body) {
			return buildInternal(body, null);
		}

		@Override
		public <T> RequestEntity<T> body(T body, Type type) {
			return buildInternal(body, type);
		}

		private <T> RequestEntity<T> buildInternal(@Nullable T body, @Nullable Type type) {
			if (this.uri != null) {
				return new RequestEntity<>(body, this.headers, this.method, this.uri, type);
			}
			else if (this.uriTemplate != null){
				return new UriTemplateRequestEntity<>(body, this.headers, this.method, type,
						this.uriTemplate, this.uriVarsArray, this.uriVarsMap);
			}
			else {
				throw new IllegalStateException("Neither URI nor URI template");
			}
		}
	}


	/**
	 * 使用 URI 模板和变量而不是 {@link URI} 初始化的 RequestEntity。
	 * @since 5.3
	 * @param <T> 正文类型
	 */
	public static class UriTemplateRequestEntity<T> extends RequestEntity<T> {

		private final String uriTemplate;

		@Nullable
		private final Object[] uriVarsArray;

		@Nullable
		private final Map<String, ?> uriVarsMap;

		UriTemplateRequestEntity(
				@Nullable T body, @Nullable MultiValueMap<String, String> headers,
				@Nullable HttpMethod method, @Nullable Type type, String uriTemplate,
				@Nullable Object[] uriVarsArray, @Nullable Map<String, ?> uriVarsMap) {

			super(body, headers, method, null, type);
			this.uriTemplate = uriTemplate;
			this.uriVarsArray = uriVarsArray;
			this.uriVarsMap = uriVarsMap;
		}

		public String getUriTemplate() {
			return this.uriTemplate;
		}

		@Nullable
		public Object[] getVars() {
			return this.uriVarsArray;
		}

		@Nullable
		public Map<String, ?> getVarsMap() {
			return this.uriVarsMap;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			UriTemplateRequestEntity<?> otherEntity = (UriTemplateRequestEntity<?>) other;
			return (ObjectUtils.nullSafeEquals(this.uriTemplate, otherEntity.uriTemplate) &&
					ObjectUtils.nullSafeEquals(this.uriVarsArray, otherEntity.uriVarsArray) &&
					ObjectUtils.nullSafeEquals(this.uriVarsMap, otherEntity.uriVarsMap));
		}

		@Override
		public int hashCode() {
			return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.uriTemplate));
		}

		@Override
		public String toString() {
			return format(getMethod(), getUriTemplate(), getBody(), getHeaders());
		}
	}

}
