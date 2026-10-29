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

package org.springframework.http;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 用于创建 "Cache-Control" HTTP 响应头的构建器。
 * <p>向 HTTP 响应中添加 Cache-Control 指令可以显著改善客户端与 Web 应用交互时的体验。此构建器仅使用响应指令创建带有明确倾向的 "Cache-Control" 头，并考虑了若干使用场景。
 * <ul>
 * <li>使用 {@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS)} 缓存 HTTP 响应将得到 {@code Cache-Control: "max-age=3600"}</li>
 * <li>使用 {@code CacheControl cc = CacheControl.noStore()} 禁用缓存将得到 {@code Cache-Control: "no-store"}</li>
 * <li>诸如 {@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).noTransform().cachePublic()} 等高级场景将得到 {@code Cache-Control: "max-age=3600, no-transform, public"}</li>
 * </ul>
 * <p>请注意，为了保持高效，Cache-Control 头应与 "Last-Modified" 或 "ETag" 等 HTTP 验证器一起写入。
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.2
 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2">rfc7234 section 5.2.2</a>
 * @see <a href="https://developers.google.com/web/fundamentals/performance/optimizing-content-efficiency/http-caching">
 * HTTP caching - Google developers reference</a>
 * @see <a href="https://www.mnot.net/cache_docs/">Mark Nottingham's cache documentation</a>
 */
public class CacheControl {

	@Nullable
	private Duration maxAge;

	private boolean noCache = false;

	private boolean noStore = false;

	private boolean mustRevalidate = false;

	private boolean noTransform = false;

	private boolean cachePublic = false;

	private boolean cachePrivate = false;

	private boolean proxyRevalidate = false;

	@Nullable
	private Duration staleWhileRevalidate;

	@Nullable
	private Duration staleIfError;

	@Nullable
	private Duration sMaxAge;


	/**
	 * 创建一个空的 CacheControl 实例。
	 * @see #empty()
	 */
	protected CacheControl() {
	}


	/**
	 * 返回一个空指令。
	 * <p>这非常适合在不使用 "max-age"、"no-cache" 或 "no-store" 的情况下使用其他可选指令。
	 * @return {@code this}, to facilitate method chaining
	 */
	public static CacheControl empty() {
		return new CacheControl();
	}

	/**
	 * 添加 "max-age=" 指令。
	 * <p>该指令非常适合公开缓存资源，前提是这些资源在配置的时间段内不会发生变化。如果资源不应被共享缓存缓存（{@link #cachePrivate()}）或转换（{@link #noTransform()}），还可以同时使用其他指令。
	 * <p>为了防止缓存在响应已过期（即已超过 "max-age" 延迟）时仍复用缓存的响应，应设置 "must-revalidate" 指令（{@link #mustRevalidate()}
	 * @param maxAge the maximum time the response should be cached
	 * @param unit the time unit of the {@code maxAge} argument
	 * @return {@code this}, to facilitate method chaining
	 * @see #maxAge(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">rfc7234 section 5.2.2.8</a>
	 */
	public static CacheControl maxAge(long maxAge, TimeUnit unit) {
		return maxAge(Duration.ofSeconds(unit.toSeconds(maxAge)));
	}

	/**
	 * 添加 "max-age=" 指令。
	 * <p>该指令非常适合公开缓存资源，前提是这些资源在配置的时间段内不会发生变化。如果资源不应被共享缓存缓存（{@link #cachePrivate()}）或转换（{@link #noTransform()}），还可以同时使用其他指令。
	 * <p>为了防止缓存在响应已过期（即已超过 "max-age" 延迟）时仍复用缓存的响应，应设置 "must-revalidate" 指令（{@link #mustRevalidate()}
	 * @param maxAge the maximum time the response should be cached
	 * @return {@code this}, to facilitate method chaining
	 * @since 5.2
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">rfc7234 section 5.2.2.8</a>
	 */
	public static CacheControl maxAge(Duration maxAge) {
		CacheControl cc = new CacheControl();
		cc.maxAge = maxAge;
		return cc;
	}

	/**
	 * 添加 "no-cache" 指令。
	 * <p>该指令适合告知缓存：只有当客户端向服务器重新验证响应之后，该响应才可以被复用。此指令不会完全禁用缓存，客户端可能会发送条件请求（带有 "ETag"、"If-Modified-Since" 头），服务器则以 "304 - Not Modified" 状态响应。
	 * <p>为了禁用缓存并尽量减少请求/响应交换，应使用 {@link #noStore()} 指令而不是 {@code #noCache()}。
	 * @return {@code this}, to facilitate method chaining
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.2">rfc7234 section 5.2.2.2</a>
	 */
	public static CacheControl noCache() {
		CacheControl cc = new CacheControl();
		cc.noCache = true;
		return cc;
	}

	/**
	 * 添加 "no-store" 指令。
	 * <p>该指令适合阻止缓存（浏览器和代理）缓存响应内容。
	 * @return {@code this}, to facilitate method chaining
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.3">rfc7234 section 5.2.2.3</a>
	 */
	public static CacheControl noStore() {
		CacheControl cc = new CacheControl();
		cc.noStore = true;
		return cc;
	}


	/**
	 * 添加 "must-revalidate" 指令。
	 * <p>该指令表示，一旦响应过期，缓存在未对源服务器成功验证之前，不得使用该响应来满足后续请求。
	 * @return {@code this}, to facilitate method chaining
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.1">rfc7234 section 5.2.2.1</a>
	 */
	public CacheControl mustRevalidate() {
		this.mustRevalidate = true;
		return this;
	}

	/**
	 * 添加 "no-transform" 指令。
	 * <p>该指令表示中间层（缓存及其他组件）不应转换响应内容。这可用于强制缓存和 CDN 不要自动对响应内容进行 gzip 压缩或优化。
	 * @return {@code this}, to facilitate method chaining
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.4">rfc7234 section 5.2.2.4</a>
	 */
	public CacheControl noTransform() {
		this.noTransform = true;
		return this;
	}

	/**
	 * 添加 "public" 指令。
	 * <p>该指令表示任何缓存都可以存储该响应，即使该响应通常不可缓存，或只能在私有缓存中缓存。
	 * @return {@code this}, to facilitate method chaining
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.5">rfc7234 section 5.2.2.5</a>
	 */
	public CacheControl cachePublic() {
		this.cachePublic = true;
		return this;
	}

	/**
	 * 添加 "private" 指令。
	 * <p>该指令表示该响应消息仅面向单个用户，不得由共享缓存存储。
	 * @return {@code this}, to facilitate method chaining
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.6">rfc7234 section 5.2.2.6</a>
	 */
	public CacheControl cachePrivate() {
		this.cachePrivate = true;
		return this;
	}

	/**
	 * 添加 "proxy-revalidate" 指令。
	 * <p>该指令与 "must-revalidate" 指令含义相同，区别在于它不适用于私有缓存（即浏览器、HTTP 客户端）。
	 * @return {@code this}, to facilitate method chaining
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.7">rfc7234 section 5.2.2.7</a>
	 */
	public CacheControl proxyRevalidate() {
		this.proxyRevalidate = true;
		return this;
	}

	/**
	 * 添加 "s-maxage" 指令。
	 * <p>该指令表示在共享缓存中，此指令指定的最大年龄会覆盖其他指令指定的最大年龄。
	 * @param sMaxAge the maximum time the response should be cached
	 * @param unit the time unit of the {@code sMaxAge} argument
	 * @return {@code this}, to facilitate method chaining
	 * @see #sMaxAge(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">rfc7234 section 5.2.2.9</a>
	 */
	public CacheControl sMaxAge(long sMaxAge, TimeUnit unit) {
		return sMaxAge(Duration.ofSeconds(unit.toSeconds(sMaxAge)));
	}

	/**
	 * 添加 "s-maxage" 指令。
	 * <p>该指令表示在共享缓存中，此指令指定的最大年龄会覆盖其他指令指定的最大年龄。
	 * @param sMaxAge the maximum time the response should be cached
	 * @return {@code this}, to facilitate method chaining
	 * @since 5.2
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">rfc7234 section 5.2.2.9</a>
	 */
	public CacheControl sMaxAge(Duration sMaxAge) {
		this.sMaxAge = sMaxAge;
		return this;
	}

	/**
	 * 添加 "stale-while-revalidate" 指令。
	 * <p>该指令表示缓存可以在响应过期后继续提供该响应，最长不超过指定的秒数。如果由于此扩展的存在而以过期状态提供缓存响应，缓存应尝试在继续提供过期响应的同时重新验证它（即不阻塞）。
	 * @param staleWhileRevalidate the maximum time the response should be used while being revalidated
	 * @param unit the time unit of the {@code staleWhileRevalidate} argument
	 * @return {@code this}, to facilitate method chaining
	 * @see #staleWhileRevalidate(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-3">rfc5861 section 3</a>
	 */
	public CacheControl staleWhileRevalidate(long staleWhileRevalidate, TimeUnit unit) {
		return staleWhileRevalidate(Duration.ofSeconds(unit.toSeconds(staleWhileRevalidate)));
	}

	/**
	 * 添加 "stale-while-revalidate" 指令。
	 * <p>该指令表示缓存可以在响应过期后继续提供该响应，最长不超过指定的秒数。如果由于此扩展的存在而以过期状态提供缓存响应，缓存应尝试在继续提供过期响应的同时重新验证它（即不阻塞）。
	 * @param staleWhileRevalidate the maximum time the response should be used while being revalidated
	 * @return {@code this}, to facilitate method chaining
	 * @since 5.2
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-3">rfc5861 section 3</a>
	 */
	public CacheControl staleWhileRevalidate(Duration staleWhileRevalidate) {
		this.staleWhileRevalidate = staleWhileRevalidate;
		return this;
	}

	/**
	 * 添加 "stale-if-error" 指令。
	 * <p>该指令表示当遇到错误时，可以使用缓存的过期响应来满足请求，而不考虑其他新鲜度信息。
	 * @param staleIfError the maximum time the response should be used when errors are encountered
	 * @param unit the time unit of the {@code staleIfError} argument
	 * @return {@code this}, to facilitate method chaining
	 * @see #staleIfError(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-4">rfc5861 section 4</a>
	 */
	public CacheControl staleIfError(long staleIfError, TimeUnit unit) {
		return staleIfError(Duration.ofSeconds(unit.toSeconds(staleIfError)));
	}

	/**
	 * 添加 "stale-if-error" 指令。
	 * <p>该指令表示当遇到错误时，可以使用缓存的过期响应来满足请求，而不考虑其他新鲜度信息。
	 * @param staleIfError the maximum time the response should be used when errors are encountered
	 * @return {@code this}, to facilitate method chaining
	 * @since 5.2
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-4">rfc5861 section 4</a>
	 */
	public CacheControl staleIfError(Duration staleIfError) {
		this.staleIfError = staleIfError;
		return this;
	}

	/**
	 * 返回 "Cache-Control" 头值（如果有）。
	 * @return the header value, or {@code null} if no directive was added
	 */
	@Nullable
	public String getHeaderValue() {
		String headerValue = toHeaderValue();
		return (StringUtils.hasText(headerValue) ? headerValue : null);
	}

	/**
	 * 返回 "Cache-Control" 头值。
	 * @return the header value (potentially empty)
	 */
	private String toHeaderValue() {
		StringBuilder headerValue = new StringBuilder();
		if (this.maxAge != null) {
			appendDirective(headerValue, "max-age=" + this.maxAge.getSeconds());
		}
		if (this.noCache) {
			appendDirective(headerValue, "no-cache");
		}
		if (this.noStore) {
			appendDirective(headerValue, "no-store");
		}
		if (this.mustRevalidate) {
			appendDirective(headerValue, "must-revalidate");
		}
		if (this.noTransform) {
			appendDirective(headerValue, "no-transform");
		}
		if (this.cachePublic) {
			appendDirective(headerValue, "public");
		}
		if (this.cachePrivate) {
			appendDirective(headerValue, "private");
		}
		if (this.proxyRevalidate) {
			appendDirective(headerValue, "proxy-revalidate");
		}
		if (this.sMaxAge != null) {
			appendDirective(headerValue, "s-maxage=" + this.sMaxAge.getSeconds());
		}
		if (this.staleIfError != null) {
			appendDirective(headerValue, "stale-if-error=" + this.staleIfError.getSeconds());
		}
		if (this.staleWhileRevalidate != null) {
			appendDirective(headerValue, "stale-while-revalidate=" + this.staleWhileRevalidate.getSeconds());
		}
		return headerValue.toString();
	}

	private void appendDirective(StringBuilder builder, String value) {
		if (builder.length() > 0) {
			builder.append(", ");
		}
		builder.append(value);
	}


	@Override
	public String toString() {
		return "CacheControl [" + toHeaderValue() + "]";
	}

}
