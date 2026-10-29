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

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * 表示由头和正文组成的 HTTP 请求或响应实体。
 * <p>通常与 {@link org.springframework.web.client.RestTemplate} 结合使用，如下所示：
 * <pre class="code">
 * HttpHeaders headers = new HttpHeaders();
 * headers.setContentType(MediaType.TEXT_PLAIN);
 * HttpEntity&lt;String&gt; entity = new HttpEntity&lt;&gt;("Hello World", headers);
 * URI location = template.postForLocation("https://example.com", entity);
 * </pre>
 * 或者
 * <pre class="code">
 * HttpEntity&lt;String&gt; entity = template.getForEntity("https://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * </pre>
 * 也可用于 Spring MVC，作为 @Controller 方法的返回值：
 * <pre class="code">
 * &#64;GetMapping("/handle")
 * public HttpEntity&lt;String&gt; handle() {
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new HttpEntity&lt;&gt;("Hello World", responseHeaders);
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0.2
 * @param <T> the body type
 * @see org.springframework.web.client.RestTemplate
 * @see #getBody()
 * @see #getHeaders()
 */
public class HttpEntity<T> {

	/**
	 * 空的 {@code HttpEntity}，没有正文和头。
	 */
	public static final HttpEntity<?> EMPTY = new HttpEntity<>();


	private final HttpHeaders headers;

	@Nullable
	private final T body;


	/**
	 * 创建一个新的空 {@code HttpEntity}。
	 */
	protected HttpEntity() {
		this(null, null);
	}

	/**
	 * 使用给定正文创建一个新的 {@code HttpEntity}，不包含头。
	 * @param body the entity body
	 */
	public HttpEntity(T body) {
		this(body, null);
	}

	/**
	 * 使用给定头创建一个新的 {@code HttpEntity}，不包含正文。
	 * @param headers the entity headers
	 */
	public HttpEntity(MultiValueMap<String, String> headers) {
		this(null, headers);
	}

	/**
	 * 使用给定正文和头创建一个新的 {@code HttpEntity}。
	 * @param body the entity body
	 * @param headers the entity headers
	 */
	public HttpEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers) {
		this.body = body;
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers != null ? headers : new HttpHeaders());
	}


	/**
	 * 返回此实体的头。
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * 返回此实体的正文。
	 */
	@Nullable
	public T getBody() {
		return this.body;
	}

	/**
	 * 指示此实体是否包含正文。
	 */
	public boolean hasBody() {
		return (this.body != null);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		HttpEntity<?> otherEntity = (HttpEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(this.headers, otherEntity.headers) &&
				ObjectUtils.nullSafeEquals(this.body, otherEntity.body));
	}

	@Override
	public int hashCode() {
		return (ObjectUtils.nullSafeHashCode(this.headers) * 29 + ObjectUtils.nullSafeHashCode(this.body));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		if (this.body != null) {
			builder.append(this.body);
			builder.append(',');
		}
		builder.append(this.headers);
		builder.append('>');
		return builder.toString();
	}

}
