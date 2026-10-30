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

import java.time.Duration;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 带有 "Set-Cookie" 响应头所允许的附加属性的 {@code HttpCookie} 子类。要构建实例，请使用 {@link #from} 静态方法。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 */
public final class ResponseCookie extends HttpCookie {

	private final Duration maxAge;

	@Nullable
	private final String domain;

	@Nullable
	private final String path;

	private final boolean secure;

	private final boolean httpOnly;

	@Nullable
	private final String sameSite;


	/**
	 * 私有构造函数。请参见 {@link #from(String, String)}。
	 */
	private ResponseCookie(String name, String value, Duration maxAge, @Nullable String domain,
			@Nullable String path, boolean secure, boolean httpOnly, @Nullable String sameSite) {

		super(name, value);
		Assert.notNull(maxAge, "Max age must not be null");

		this.maxAge = maxAge;
		this.domain = domain;
		this.path = path;
		this.secure = secure;
		this.httpOnly = httpOnly;
		this.sameSite = sameSite;

		Rfc6265Utils.validateCookieName(name);
		Rfc6265Utils.validateCookieValue(value);
		Rfc6265Utils.validateDomain(domain);
		Rfc6265Utils.validatePath(path);
	}


	/**
	 * 以秒为单位返回 Cookie 的 "Max-Age" 属性。
	 * <p>正值表示 Cookie 相对于当前时间的过期时间。值为 0 表示 Cookie 应立即过期。负值表示没有 "Max-Age" 属性，此时 Cookie 会在浏览器关闭时被移除。
	 */
	public Duration getMaxAge() {
		return this.maxAge;
	}

	/**
	 * 返回 Cookie 的 "Domain" 属性；如果未设置则返回 {@code null}。
	 */
	@Nullable
	public String getDomain() {
		return this.domain;
	}

	/**
	 * 返回 Cookie 的 "Path" 属性；如果未设置则返回 {@code null}。
	 */
	@Nullable
	public String getPath() {
		return this.path;
	}

	/**
	 * 如果 Cookie 具有 "Secure" 属性，则返回 {@code true}。
	 */
	public boolean isSecure() {
		return this.secure;
	}

	/**
	 * 如果 Cookie 具有 "HttpOnly" 属性，则返回 {@code true}。
	 * @see <a href="https://www.owasp.org/index.php/HTTPOnly">https://www.owasp.org/index.php/HTTPOnly</a>
	 */
	public boolean isHttpOnly() {
		return this.httpOnly;
	}

	/**
	 * 返回 Cookie 的 "SameSite" 属性；如果未设置则返回 {@code null}。
	 * <p>这会限制 Cookie 的作用范围：当值为 {@code "Strict"} 时仅附加到同站请求，当值为 {@code "Lax"} 时附加到跨站请求。
	 * @since 5.1
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
	 */
	@Nullable
	public String getSameSite() {
		return this.sameSite;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ResponseCookie)) {
			return false;
		}
		ResponseCookie otherCookie = (ResponseCookie) other;
		return (getName().equalsIgnoreCase(otherCookie.getName()) &&
				ObjectUtils.nullSafeEquals(this.path, otherCookie.getPath()) &&
				ObjectUtils.nullSafeEquals(this.domain, otherCookie.getDomain()));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.domain);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.path);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append('=').append(getValue());
		if (StringUtils.hasText(getPath())) {
			sb.append("; Path=").append(getPath());
		}
		if (StringUtils.hasText(this.domain)) {
			sb.append("; Domain=").append(this.domain);
		}
		if (!this.maxAge.isNegative()) {
			sb.append("; Max-Age=").append(this.maxAge.getSeconds());
			sb.append("; Expires=");
			long millis = this.maxAge.getSeconds() > 0 ? System.currentTimeMillis() + this.maxAge.toMillis() : 0;
			sb.append(HttpHeaders.formatDate(millis));
		}
		if (this.secure) {
			sb.append("; Secure");
		}
		if (this.httpOnly) {
			sb.append("; HttpOnly");
		}
		if (StringUtils.hasText(this.sameSite)) {
			sb.append("; SameSite=").append(this.sameSite);
		}
		return sb.toString();
	}


	/**
	 * 工厂方法，用于获取服务器端定义的 Cookie 的构建器；该 Cookie 以名称-值对开始，也可以包含属性。
	 * @param name Cookie 名称
	 * @param value Cookie 值
	 * @return 用于创建 Cookie 的构建器
	 */
	public static ResponseCookieBuilder from(final String name, final String value) {
		return from(name, value, false);
	}

	/**
	 * 工厂方法，用于获取服务器端定义的 Cookie 的构建器。与 {@link #from(String, String)} 不同，此选项假定输入来自远程服务器，因此可以更宽松地处理，例如忽略用双引号表示的空白域名。
	 * @param name Cookie 名称
	 * @param value Cookie 值
	 * @return 用于创建 Cookie 的构建器
	 * @since 5.2.5
	 */
	public static ResponseCookieBuilder fromClientResponse(final String name, final String value) {
		return from(name, value, true);
	}


	private static ResponseCookieBuilder from(final String name, final String value, boolean lenient) {

		return new ResponseCookieBuilder() {

			private Duration maxAge = Duration.ofSeconds(-1);

			@Nullable
			private String domain;

			@Nullable
			private String path;

			private boolean secure;

			private boolean httpOnly;

			@Nullable
			private String sameSite;

			@Override
			public ResponseCookieBuilder maxAge(Duration maxAge) {
				this.maxAge = maxAge;
				return this;
			}

			@Override
			public ResponseCookieBuilder maxAge(long maxAgeSeconds) {
				this.maxAge = maxAgeSeconds >= 0 ? Duration.ofSeconds(maxAgeSeconds) : Duration.ofSeconds(-1);
				return this;
			}

			@Override
			public ResponseCookieBuilder domain(@Nullable String domain) {
				this.domain = initDomain(domain);
				return this;
			}

			@Nullable
			private String initDomain(@Nullable String domain) {
				if (lenient && StringUtils.hasLength(domain)) {
					String str = domain.trim();
					if (str.startsWith("\"") && str.endsWith("\"")) {
						if (str.substring(1, str.length() - 1).trim().isEmpty()) {
							return null;
						}
					}
				}
				return domain;
			}

			@Override
			public ResponseCookieBuilder path(@Nullable String path) {
				this.path = path;
				return this;
			}

			@Override
			public ResponseCookieBuilder secure(boolean secure) {
				this.secure = secure;
				return this;
			}

			@Override
			public ResponseCookieBuilder httpOnly(boolean httpOnly) {
				this.httpOnly = httpOnly;
				return this;
			}

			@Override
			public ResponseCookieBuilder sameSite(@Nullable String sameSite) {
				this.sameSite = sameSite;
				return this;
			}

			@Override
			public ResponseCookie build() {
				return new ResponseCookie(name, value, this.maxAge, this.domain, this.path,
						this.secure, this.httpOnly, this.sameSite);
			}
		};
	}


	/**
	 * 用于构建带属性的服务器端定义 HttpCookie 的构建器。
	 */
	public interface ResponseCookieBuilder {

		/**
		 * 设置 Cookie 的 "Max-Age" 属性。
		 * <p>正值表示 Cookie 应相对于当前时间过期的时间。值为 0 表示 Cookie 应立即过期。负值表示没有 "Max-Age" 属性，此时 Cookie 会在浏览器关闭时被移除。
		 */
		ResponseCookieBuilder maxAge(Duration maxAge);

		/**
		 * 接受以秒为单位值的 {@link #maxAge(Duration)} 变体。
		 */
		ResponseCookieBuilder maxAge(long maxAgeSeconds);

		/**
		 * 设置 Cookie 的 "Path" 属性。
		 */
		ResponseCookieBuilder path(@Nullable String path);

		/**
		 * 设置 Cookie 的 "Domain" 属性。
		 */
		ResponseCookieBuilder domain(@Nullable String domain);

		/**
		 * 向 Cookie 添加 "Secure" 属性。
		 */
		ResponseCookieBuilder secure(boolean secure);

		/**
		 * 向 Cookie 添加 "HttpOnly" 属性。
		 * @see <a href="https://www.owasp.org/index.php/HTTPOnly">https://www.owasp.org/index.php/HTTPOnly</a>
		 */
		ResponseCookieBuilder httpOnly(boolean httpOnly);

		/**
		 * 向 Cookie 添加 "SameSite" 属性。
		 * <p>这会限制 Cookie 的作用范围：当值为 {@code "Strict"} 时仅附加到同站请求，当值为 {@code "Lax"} 时附加到跨站请求。
		 * @since 5.1
		 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
		 */
		ResponseCookieBuilder sameSite(@Nullable String sameSite);

		/**
		 * 创建 HttpCookie。
		 */
		ResponseCookie build();
	}


	private static class Rfc6265Utils {

		private static final String SEPARATOR_CHARS = new String(new char[] {
				'(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}', ' '
		});

		private static final String DOMAIN_CHARS =
				"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-";


		public static void validateCookieName(String name) {
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				// CTL = <US-ASCII control chars (octets 0 - 31) and DEL (127)>
				if (c <= 0x1F || c == 0x7F) {
					throw new IllegalArgumentException(
							name + ": RFC2616 token cannot have control chars");
				}
				if (SEPARATOR_CHARS.indexOf(c) >= 0) {
					throw new IllegalArgumentException(
							name + ": RFC2616 token cannot have separator chars such as '" + c + "'");
				}
				if (c >= 0x80) {
					throw new IllegalArgumentException(
							name + ": RFC2616 token can only have US-ASCII: 0x" + Integer.toHexString(c));
				}
			}
		}

		public static void validateCookieValue(@Nullable String value) {
			if (value == null) {
				return;
			}
			int start = 0;
			int end = value.length();
			if (end > 1 && value.charAt(0) == '"' && value.charAt(end - 1) == '"') {
				start = 1;
				end--;
			}
			for (int i = start; i < end; i++) {
				char c = value.charAt(i);
				if (c < 0x21 || c == 0x22 || c == 0x2c || c == 0x3b || c == 0x5c || c == 0x7f) {
					throw new IllegalArgumentException(
							"RFC2616 cookie value cannot have '" + c + "'");
				}
				if (c >= 0x80) {
					throw new IllegalArgumentException(
							"RFC2616 cookie value can only have US-ASCII chars: 0x" + Integer.toHexString(c));
				}
			}
		}

		public static void validateDomain(@Nullable String domain) {
			if (!StringUtils.hasLength(domain)) {
				return;
			}
			int char1 = domain.charAt(0);
			int charN = domain.charAt(domain.length() - 1);
			if (char1 == '-' || charN == '.' || charN == '-') {
				throw new IllegalArgumentException("Invalid first/last char in cookie domain: " + domain);
			}
			for (int i = 0, c = -1; i < domain.length(); i++) {
				int p = c;
				c = domain.charAt(i);
				if (DOMAIN_CHARS.indexOf(c) == -1 || (p == '.' && (c == '.' || c == '-')) || (p == '-' && c == '.')) {
					throw new IllegalArgumentException(domain + ": invalid cookie domain char '" + c + "'");
				}
			}
		}

		public static void validatePath(@Nullable String path) {
			if (path == null) {
				return;
			}
			for (int i = 0; i < path.length(); i++) {
				char c = path.charAt(i);
				if (c < 0x20 || c > 0x7E || c == ';') {
					throw new IllegalArgumentException(path + ": Invalid cookie path char '" + c + "'");
				}
			}
		}
	}

}
