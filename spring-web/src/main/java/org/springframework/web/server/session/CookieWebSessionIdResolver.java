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

package org.springframework.web.server.session;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

/**
 * 基于 Cookie 的 {@link WebSessionIdResolver}。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class CookieWebSessionIdResolver implements WebSessionIdResolver {

	private String cookieName = "SESSION";

	private Duration cookieMaxAge = Duration.ofSeconds(-1);

	@Nullable
	private Consumer<ResponseCookie.ResponseCookieBuilder> cookieInitializer = null;


	/**
	 * 设置用于保存会话 ID 的 Cookie 名称。
	 * <p>默认设置为 "SESSION"。
	 * @param cookieName Cookie 名称
	 */
	public void setCookieName(String cookieName) {
		Assert.hasText(cookieName, "'cookieName' must not be empty");
		this.cookieName = cookieName;
	}

	/**
	 * 获取已配置的 Cookie 名称。
	 */
	public String getCookieName() {
		return this.cookieName;
	}

	/**
	 * 设置保存会话 ID 的 Cookie 的 "Max-Age" 属性值。
	 * <p>取值范围参见 {@link ResponseCookie#getMaxAge()}。
	 * <p>默认设置为 -1。
	 * @param maxAge maxAge 时长值
	 */
	public void setCookieMaxAge(Duration maxAge) {
		this.cookieMaxAge = maxAge;
	}

	/**
	 * 获取会话 Cookie 已配置的 "Max-Age" 属性值。
	 */
	public Duration getCookieMaxAge() {
		return this.cookieMaxAge;
	}

	/**
	 * 为 {@code ResponseCookieBuilder} 添加一个 {@link Consumer}，它将在每个 Cookie
	 * 构建过程中、调用 {@code build()} 之前被调用。
	 * @param initializer 用于 Cookie 构建器的消费者
	 * @since 5.1
	 */
	public void addCookieInitializer(Consumer<ResponseCookie.ResponseCookieBuilder> initializer) {
		this.cookieInitializer = this.cookieInitializer != null ?
				this.cookieInitializer.andThen(initializer) : initializer;
	}


	@Override
	public List<String> resolveSessionIds(ServerWebExchange exchange) {
		MultiValueMap<String, HttpCookie> cookieMap = exchange.getRequest().getCookies();
		List<HttpCookie> cookies = cookieMap.get(getCookieName());
		if (cookies == null) {
			return Collections.emptyList();
		}
		return cookies.stream().map(HttpCookie::getValue).collect(Collectors.toList());
	}

	@Override
	public void setSessionId(ServerWebExchange exchange, String id) {
		Assert.notNull(id, "'id' is required");
		ResponseCookie cookie = initSessionCookie(exchange, id, getCookieMaxAge());
		exchange.getResponse().getCookies().set(this.cookieName, cookie);
	}

	@Override
	public void expireSession(ServerWebExchange exchange) {
		ResponseCookie cookie = initSessionCookie(exchange, "", Duration.ZERO);
		exchange.getResponse().getCookies().set(this.cookieName, cookie);
	}

	private ResponseCookie initSessionCookie(
			ServerWebExchange exchange, String id, Duration maxAge) {

		ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(this.cookieName, id)
				.path(exchange.getRequest().getPath().contextPath().value() + "/")
				.maxAge(maxAge)
				.httpOnly(true)
				.secure("https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme()))
				.sameSite("Lax");

		if (this.cookieInitializer != null) {
			this.cookieInitializer.accept(cookieBuilder);
		}

		return cookieBuilder.build();
	}

}
