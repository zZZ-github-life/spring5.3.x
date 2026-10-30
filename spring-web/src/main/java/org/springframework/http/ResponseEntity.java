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

package org.springframework.http;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * 扩展 {@link HttpEntity}，增加了 {@link HttpStatus} 状态码。用于 {@code RestTemplate} 以及 {@code @Controller} 方法中。
 * <p>在 {@code RestTemplate} 中，此类由 {@link org.springframework.web.client.RestTemplate#getForEntity getForEntity()} 和 {@link org.springframework.web.client.RestTemplate#exchange exchange()} 返回：
 * <pre class="code">
 * ResponseEntity&lt;String&gt; entity = template.getForEntity("https://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * HttpStatus statusCode = entity.getStatusCode();
 * </pre>
 * <p>也可在 Spring MVC 中用作 {@code @Controller} 方法的返回值：
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.setLocation(location);
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new ResponseEntity&lt;String&gt;("Hello World", responseHeaders, HttpStatus.CREATED);
 * }
 * </pre>
 *
 * 或者，使用可通过静态方法访问的构建器：
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   return ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 3.0.2
 * @param <T> 正文类型
 * @see #getStatusCode()
 * @see org.springframework.web.client.RestOperations#getForEntity(String, Class, Object...)
 * @see org.springframework.web.client.RestOperations#getForEntity(String, Class, java.util.Map)
 * @see org.springframework.web.client.RestOperations#getForEntity(URI, Class)
 * @see RequestEntity
 */
public class ResponseEntity<T> extends HttpEntity<T> {

	private final Object status;


	/**
	 * 创建一个仅包含状态码的 {@code ResponseEntity}。
	 * @param status 状态码
	 */
	public ResponseEntity(HttpStatus status) {
		this(null, null, status);
	}

	/**
	 * 创建一个包含正文和状态码的 {@code ResponseEntity}。
	 * @param body 实体正文
	 * @param status 状态码
	 */
	public ResponseEntity(@Nullable T body, HttpStatus status) {
		this(body, null, status);
	}

	/**
	 * 创建一个包含头和状态码的 {@code ResponseEntity}。
	 * @param headers 实体标头
	 * @param status 状态码
	 */
	public ResponseEntity(MultiValueMap<String, String> headers, HttpStatus status) {
		this(null, headers, status);
	}

	/**
	 * 创建一个包含正文、头和状态码的 {@code ResponseEntity}。
	 * @param body 实体正文
	 * @param headers 实体标头
	 * @param status 状态码
	 */
	public ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, HttpStatus status) {
		this(body, headers, (Object) status);
	}

	/**
	 * 创建一个包含正文、头和原始状态码的 {@code ResponseEntity}。
	 * @param body 实体正文
	 * @param headers 实体标头
	 * @param rawStatus 状态码值
	 * @since 5.3.2
	 */
	public ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, int rawStatus) {
		this(body, headers, (Object) rawStatus);
	}

	/**
	 * 私有构造函数。
	 */
	private ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, Object status) {
		super(body, headers);
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status;
	}


	/**
	 * 返回响应的 HTTP 状态码。
	 * @return 以 HttpStatus 枚举条目表示的 HTTP 状态
	 */
	public HttpStatus getStatusCode() {
		if (this.status instanceof HttpStatus) {
			return (HttpStatus) this.status;
		}
		else {
			return HttpStatus.valueOf((Integer) this.status);
		}
	}

	/**
	 * 返回响应的 HTTP 状态码。
	 * @return 以 int 值表示的 HTTP 状态
	 * @since 4.3
	 */
	public int getStatusCodeValue() {
		if (this.status instanceof HttpStatus) {
			return ((HttpStatus) this.status).value();
		}
		else {
			return (Integer) this.status;
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		ResponseEntity<?> otherEntity = (ResponseEntity<?>) other;
		return ObjectUtils.nullSafeEquals(this.status, otherEntity.status);
	}

	@Override
	public int hashCode() {
		return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.status));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		builder.append(this.status);
		if (this.status instanceof HttpStatus) {
			builder.append(' ');
			builder.append(((HttpStatus) this.status).getReasonPhrase());
		}
		builder.append(',');
		T body = getBody();
		HttpHeaders headers = getHeaders();
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
	 * 使用给定状态创建一个构建器。
	 * @param status 响应状态
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		return new DefaultBuilder(status);
	}

	/**
	 * 使用给定状态创建一个构建器。
	 * @param status 响应状态
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder status(int status) {
		return new DefaultBuilder(status);
	}

	/**
	 * 创建一个状态设置为 {@linkplain HttpStatus#OK OK} 的构建器。
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * 创建具有给定正文且状态设置为 {@linkplain HttpStatus#OK OK} 的 {@code ResponseEntity} 的快捷方式。
	 * @param body 响应实体的正文（可能为空）
	 * @return 创建的 {@code ResponseEntity}
	 * @since 4.1
	 */
	public static <T> ResponseEntity<T> ok(@Nullable T body) {
		return ok().body(body);
	}

	/**
	 * 创建具有给定正文和 {@linkplain HttpStatus#OK OK} 状态的 {@code ResponseEntity} 的快捷方式；如果参数为 {@linkplain Optional#empty()}，则创建空正文和 {@linkplain HttpStatus#NOT_FOUND NOT FOUND} 状态。
	 * @return 创建的 {@code ResponseEntity}
	 * @since 5.1
	 */
	public static <T> ResponseEntity<T> of(Optional<T> body) {
		Assert.notNull(body, "Body must not be null");
		return body.map(ResponseEntity::ok).orElseGet(() -> notFound().build());
	}

	/**
	 * 创建一个新构建器，其状态为 {@linkplain HttpStatus#CREATED CREATED}，并将位置头设置为给定 URI。
	 * @param location 位置 URI
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder created(URI location) {
		return status(HttpStatus.CREATED).location(location);
	}

	/**
	 * 创建具有 {@linkplain HttpStatus#ACCEPTED ACCEPTED} 状态的构建器。
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * 创建具有 {@linkplain HttpStatus#NO_CONTENT NO_CONTENT} 状态的构建器。
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * 创建具有 {@linkplain HttpStatus#BAD_REQUEST BAD_REQUEST} 状态的构建器。
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 创建具有 {@linkplain HttpStatus#NOT_FOUND NOT_FOUND} 状态的构建器。
	 * @return 创建的构建器
	 * @since 4.1
	 */
	public static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * 创建具有 {@linkplain HttpStatus#UNPROCESSABLE_ENTITY UNPROCESSABLE_ENTITY} 状态的构建器。
	 * @return 创建的构建器
	 * @since 4.1.3
	 */
	public static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * 创建具有 {@linkplain HttpStatus#INTERNAL_SERVER_ERROR INTERNAL_SERVER_ERROR} 状态的构建器。
	 * @return 创建的构建器
	 * @since 5.3.8
	 */
	public static BodyBuilder internalServerError() {
		return status(HttpStatus.INTERNAL_SERVER_ERROR);
	}


	/**
	 * 定义向响应实体添加头的构建器。
	 * @since 4.1
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
		 * @since 4.1.2
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
		 * 设置允许的 {@link HttpMethod HTTP methods} 集合，由 {@code Allow} 头指定。
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * 设置正文的实体标签，由 {@code ETag} 头指定。
		 * @param etag 新的实体标签
		 * @return 此构建器
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String etag);

		/**
		 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
		 * @param lastModified 最后修改日期
		 * @return 此构建器
		 * @since 5.1.4
		 * @see HttpHeaders#setLastModified(ZonedDateTime)
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
		 * @param lastModified 最后修改日期
		 * @return 此构建器
		 * @since 5.1.4
		 * @see HttpHeaders#setLastModified(Instant)
		 */
		B lastModified(Instant lastModified);

		/**
		 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
		 * <p>日期应指定为自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数。
		 * @param lastModified 最后修改日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(long lastModified);

		/**
		 * 设置资源的位置，由 {@code Location} 头指定。
		 * @param location 位置
		 * @return 此构建器
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * 设置资源的缓存指令，由 HTTP 1.1 的 {@code Cache-Control} 头指定。
		 * <p>可以像 {@code CacheControl.maxAge(3600).cachePublic().noTransform()} 这样构建 {@code CacheControl} 实例。
		 * @param cacheControl 用于缓存相关 HTTP 响应标头的构建器
		 * @return 此构建器
		 * @since 4.2
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求头名称（例如 "Accept-Language"），将其添加到 "Vary" 响应头中，以告知客户端响应会根据这些请求头的值进行内容协商并产生差异。仅当响应 "Vary" 头中尚不存在这些请求头名称时，才会添加它们。
		 * @param requestHeaders 请求标头名称
		 * @since 4.3
		 */
		B varyBy(String... requestHeaders);

		/**
		 * 构建不带正文的响应实体。
		 * @return 响应实体
		 * @see BodyBuilder#body(Object)
		 */
		<T> ResponseEntity<T> build();
	}


	/**
	 * 定义向响应实体添加正文的构建器。
	 * @since 4.1
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
		 * 设置响应实体的正文并返回它。
		 * @param <T> 正文的类型
		 * @param body 响应实体的正文
		 * @return 构建的响应实体
		 */
		<T> ResponseEntity<T> body(@Nullable T body);
	}


	private static class DefaultBuilder implements BodyBuilder {

		private final Object statusCode;

		private final HttpHeaders headers = new HttpHeaders();

		public DefaultBuilder(Object statusCode) {
			this.statusCode = statusCode;
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
		public BodyBuilder allow(HttpMethod... allowedMethods) {
			this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
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
		public BodyBuilder eTag(String etag) {
			if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
				etag = "\"" + etag;
			}
			if (!etag.endsWith("\"")) {
				etag = etag + "\"";
			}
			this.headers.setETag(etag);
			return this;
		}

		@Override
		public BodyBuilder lastModified(ZonedDateTime date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder lastModified(Instant date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder lastModified(long date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder location(URI location) {
			this.headers.setLocation(location);
			return this;
		}

		@Override
		public BodyBuilder cacheControl(CacheControl cacheControl) {
			this.headers.setCacheControl(cacheControl);
			return this;
		}

		@Override
		public BodyBuilder varyBy(String... requestHeaders) {
			this.headers.setVary(Arrays.asList(requestHeaders));
			return this;
		}

		@Override
		public <T> ResponseEntity<T> build() {
			return body(null);
		}

		@Override
		public <T> ResponseEntity<T> body(@Nullable T body) {
			return new ResponseEntity<>(body, this.headers, this.statusCode);
		}
	}

}
