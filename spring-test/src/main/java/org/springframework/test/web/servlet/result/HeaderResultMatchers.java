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

package org.springframework.test.web.servlet.result;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matcher;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * 响应头断言的工厂。
 *
 * <p>此类的实例可通过 {@link MockMvcResultMatchers#header} 获取。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 3.2
 */
public class HeaderResultMatchers {

	/**
	 * 受保护的构造函数。
	 * 请参见 {@link MockMvcResultMatchers#header()}。
	 */
	protected HeaderResultMatchers() {
	}


	/**
	 * 使用给定的 Hamcrest String {@code Matcher} 断言响应头的主值。
	 */
	public ResultMatcher string(String name, Matcher<? super String> matcher) {
		return result -> assertThat("Response header '" + name + "'", result.getResponse().getHeader(name), matcher);
	}

	/**
	 * 使用给定的 Hamcrest Iterable {@link Matcher} 断言响应头的值。
	 * @since 4.3
	 */
	public ResultMatcher stringValues(String name, Matcher<? super Iterable<String>> matcher) {
		return result -> {
			List<String> values = result.getResponse().getHeaders(name);
			assertThat("Response header '" + name + "'", values, matcher);
		};
	}

	/**
	 * 以字符串值形式断言响应头的主值。
	 */
	public ResultMatcher string(String name, String value) {
		return result -> assertEquals("Response header '" + name + "'", value, result.getResponse().getHeader(name));
	}

	/**
	 * 以字符串值形式断言响应头的值。
	 * @since 4.3
	 */
	public ResultMatcher stringValues(String name, String... values) {
		return result -> {
			List<Object> actual = result.getResponse().getHeaderValues(name);
			assertEquals("Response header '" + name + "'", Arrays.asList(values), actual);
		};
	}

	/**
	 * 断言指定的响应头存在。
	 * @since 5.0.3
	 */
	public ResultMatcher exists(String name) {
		return result -> assertTrue("Response should contain header '" + name + "'",
				result.getResponse().containsHeader(name));
	}

	/**
	 * 断言指定的响应头不存在。
	 * @since 4.0
	 */
	public ResultMatcher doesNotExist(String name) {
		return result -> assertFalse("Response should not contain header '" + name + "'",
				result.getResponse().containsHeader(name));
	}

	/**
	 * 以 {@code long} 形式断言指定响应头的主值。
	 * <p>如果响应不包含指定的头，或提供的 {@code value} 与主值不匹配，
	 * 此方法返回的 {@link ResultMatcher} 会抛出 {@link AssertionError}。
	 */
	public ResultMatcher longValue(String name, long value) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			assertTrue("Response does not contain header '" + name + "'", response.containsHeader(name));
			String headerValue = response.getHeader(name);
			if (headerValue != null) {
				assertEquals("Response header '" + name + "'", value, Long.parseLong(headerValue));
			}
		};
	}

	/**
	 * 按照 RFC 7231 中描述的首选日期格式，将指定响应头的主值
	 * 解析为日期并进行断言。
	 * <p>如果响应不包含指定的头，或提供的 {@code value} 与主值不匹配，
	 * 此方法返回的 {@link ResultMatcher} 会抛出 {@link AssertionError}。
	 * @since 4.2
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	public ResultMatcher dateValue(String name, long value) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			String headerValue = response.getHeader(name);
			assertNotNull("Response does not contain header '" + name + "'", headerValue);

			HttpHeaders headers = new HttpHeaders();
			headers.setDate("expected", value);
			headers.set("actual", headerValue);

			assertEquals("Response header '" + name + "'='" + headerValue + "' " +
							"does not match expected value '" + headers.getFirst("expected") + "'",
					headers.getFirstDate("expected"), headers.getFirstDate("actual"));
		};
	}

}
