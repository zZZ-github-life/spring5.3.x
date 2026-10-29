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

package org.springframework.test.web.servlet.result;

import javax.servlet.http.Cookie;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertNull;

/**
 * 响应 cookie 断言的工厂。
 *
 * <p>此类的实例通常通过 {@link MockMvcResultMatchers#cookie} 访问。
 * @author Rossen Stoyanchev
 * @author Thomas Bruyelle
 * @since 3.2
 */
public class CookieResultMatchers {

	/**
	 * 受保护的构造函数。
	 * 请使用 {@link MockMvcResultMatchers#cookie()}。
	 */
	protected CookieResultMatchers() {
	}


	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言 cookie 值。
	 */
	public ResultMatcher value(String name, Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "'", cookie.getValue(), matcher);
		};
	}

	/**
	 * 断言 cookie 值。
	 */
	public ResultMatcher value(String name, String expectedValue) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie", expectedValue, cookie.getValue());
		};
	}

	/**
	 * 断言某个 cookie 存在。存在性检查与 max age 是否为 0
	 * （即是否已过期）无关。
	 */
	public ResultMatcher exists(String name) {
		return result -> getCookie(result, name);
	}

	/**
	 * 断言某个 cookie 不存在。请注意，存在性检查与 max age 是否为 0
	 * （即已过期）无关。
	 */
	public ResultMatcher doesNotExist(String name) {
		return result -> {
			Cookie cookie = result.getResponse().getCookie(name);
			assertNull("Unexpected cookie with name '" + name + "'", cookie);
		};
	}

	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言 cookie 的 maxAge。
	 */
	public ResultMatcher maxAge(String name, Matcher<? super Integer> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' maxAge", cookie.getMaxAge(), matcher);
		};
	}

	/**
	 * 断言 cookie 的 maxAge。
	 */
	public ResultMatcher maxAge(String name, int maxAge) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' maxAge", maxAge, cookie.getMaxAge());
		};
	}

	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言 cookie 的路径。
	 */
	public ResultMatcher path(String name, Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' path", cookie.getPath(), matcher);
		};
	}

	/**
	 * 断言 cookie 的路径。
	 */
	public ResultMatcher path(String name, String path) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' path", path, cookie.getPath());
		};
	}

	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言 cookie 的域。
	 */
	public ResultMatcher domain(String name, Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' domain", cookie.getDomain(), matcher);
		};
	}

	/**
	 * 断言 cookie 的域。
	 */
	public ResultMatcher domain(String name, String domain) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' domain", domain, cookie.getDomain());
		};
	}

	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言 cookie 的注释。
	 */
	public ResultMatcher comment(String name, Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' comment", cookie.getComment(), matcher);
		};
	}

	/**
	 * 断言 cookie 的注释。
	 */
	public ResultMatcher comment(String name, String comment) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' comment", comment, cookie.getComment());
		};
	}

	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言 cookie 的版本。
	 */
	public ResultMatcher version(String name, Matcher<? super Integer> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' version", cookie.getVersion(), matcher);
		};
	}

	/**
	 * 断言 cookie 的版本。
	 */
	public ResultMatcher version(String name, int version) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' version", version, cookie.getVersion());
		};
	}

	/**
	 * 断言 cookie 是否必须通过安全协议发送。
	 */
	public ResultMatcher secure(String name, boolean secure) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' secure", secure, cookie.getSecure());
		};
	}

	/**
	 * 断言 cookie 是否必须仅限 HTTP。
	 * @since 4.3.9
	 */
	public ResultMatcher httpOnly(String name, boolean httpOnly) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' httpOnly", httpOnly, cookie.isHttpOnly());
		};
	}


	private static Cookie getCookie(MvcResult result, String name) {
		Cookie cookie = result.getResponse().getCookie(name);
		assertNotNull("No cookie with name '" + name + "'", cookie);
		return cookie;
	}

}
