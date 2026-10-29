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

package org.springframework.test.web.reactive.server;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.http.ResponseCookie;
import org.springframework.test.util.AssertionErrors;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 对响应 Cookie 的断言。
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class CookieAssertions {

	private final ExchangeResult exchangeResult;

	private final WebTestClient.ResponseSpec responseSpec;


	public CookieAssertions(ExchangeResult exchangeResult, WebTestClient.ResponseSpec responseSpec) {
		this.exchangeResult = exchangeResult;
		this.responseSpec = responseSpec;
	}


	/**
	 * 预期具有给定名称的 Cookie 与指定值匹配。
	 */
	public WebTestClient.ResponseSpec valueEquals(String name, String value) {
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name);
			AssertionErrors.assertEquals(message, value, getCookie(name).getValue());
		});
		return this.responseSpec;
	}

	/**
	 * 使用 Hamcrest {@link Matcher} 断言响应 Cookie 的第一个值。
	 */
	public WebTestClient.ResponseSpec value(String name, Matcher<? super String> matcher) {
		String value = getCookie(name).getValue();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name);
			MatcherAssert.assertThat(message, value, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * 消费响应 Cookie 的值。
	 */
	public WebTestClient.ResponseSpec value(String name, Consumer<String> consumer) {
		String value = getCookie(name).getValue();
		this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(value));
		return this.responseSpec;
	}

	/**
	 * 预期具有给定名称的 Cookie 存在。
	 */
	public WebTestClient.ResponseSpec exists(String name) {
		getCookie(name);
		return this.responseSpec;
	}

	/**
	 * 预期具有给定名称的 Cookie 不存在。
	 */
	public WebTestClient.ResponseSpec doesNotExist(String name) {
		ResponseCookie cookie = this.exchangeResult.getResponseCookies().getFirst(name);
		if (cookie != null) {
			String message = getMessage(name) + " exists with value=[" + cookie.getValue() + "]";
			this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.fail(message));
		}
		return this.responseSpec;
	}

	/**
	 * 断言 Cookie 的 maxAge 属性。
	 */
	public WebTestClient.ResponseSpec maxAge(String name, Duration expected) {
		Duration maxAge = getCookie(name).getMaxAge();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " maxAge";
			AssertionErrors.assertEquals(message, expected, maxAge);
		});
		return this.responseSpec;
	}

	/**
	 * 使用 Hamcrest {@link Matcher} 断言 Cookie 的 maxAge 属性。
	 */
	public WebTestClient.ResponseSpec maxAge(String name, Matcher<? super Long> matcher) {
		long maxAge = getCookie(name).getMaxAge().getSeconds();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " maxAge";
			assertThat(message, maxAge, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * 断言 Cookie 的 path 属性。
	 */
	public WebTestClient.ResponseSpec path(String name, String expected) {
		String path = getCookie(name).getPath();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " path";
			AssertionErrors.assertEquals(message, expected, path);
		});
		return this.responseSpec;
	}

	/**
	 * 使用 Hamcrest {@link Matcher} 断言 Cookie 的 path 属性。
	 */
	public WebTestClient.ResponseSpec path(String name, Matcher<? super String> matcher) {
		String path = getCookie(name).getPath();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " path";
			assertThat(message, path, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * 断言 Cookie 的 domain 属性。
	 */
	public WebTestClient.ResponseSpec domain(String name, String expected) {
		String path = getCookie(name).getDomain();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " domain";
			AssertionErrors.assertEquals(message, expected, path);
		});
		return this.responseSpec;
	}

	/**
	 * 使用 Hamcrest {@link Matcher} 断言 Cookie 的 domain 属性。
	 */
	public WebTestClient.ResponseSpec domain(String name, Matcher<? super String> matcher) {
		String domain = getCookie(name).getDomain();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " domain";
			assertThat(message, domain, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * 断言 Cookie 的 secure 属性。
	 */
	public WebTestClient.ResponseSpec secure(String name, boolean expected) {
		boolean isSecure = getCookie(name).isSecure();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " secure";
			AssertionErrors.assertEquals(message, expected, isSecure);
		});
		return this.responseSpec;
	}

	/**
	 * 断言 Cookie 的 httpOnly 属性。
	 */
	public WebTestClient.ResponseSpec httpOnly(String name, boolean expected) {
		boolean isHttpOnly = getCookie(name).isHttpOnly();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " httpOnly";
			AssertionErrors.assertEquals(message, expected, isHttpOnly);
		});
		return this.responseSpec;
	}

	/**
	 * 断言 Cookie 的 sameSite 属性。
	 */
	public WebTestClient.ResponseSpec sameSite(String name, String expected) {
		String sameSite = getCookie(name).getSameSite();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " sameSite";
			AssertionErrors.assertEquals(message, expected, sameSite);
		});
		return this.responseSpec;
	}


	private ResponseCookie getCookie(String name) {
		ResponseCookie cookie = this.exchangeResult.getResponseCookies().getFirst(name);
		if (cookie == null) {
			this.exchangeResult.assertWithDiagnostics(() ->
					AssertionErrors.fail("No cookie with name '" + name + "'"));
		}
		return Objects.requireNonNull(cookie);
	}

	private String getMessage(String cookie) {
		return "Response cookie '" + cookie + "'";
	}

}
