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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.util.AssertionErrors;
import org.springframework.util.CollectionUtils;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * 对响应头的断言。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.0
 * @see WebTestClient.ResponseSpec#expectHeader()
 */
public class HeaderAssertions {

	private final ExchangeResult exchangeResult;

	private final WebTestClient.ResponseSpec responseSpec;


	HeaderAssertions(ExchangeResult result, WebTestClient.ResponseSpec spec) {
		this.exchangeResult = result;
		this.responseSpec = spec;
	}


	/**
	 * 预期具有给定名称的请求头与指定值匹配。
	 */
	public WebTestClient.ResponseSpec valueEquals(String headerName, String... values) {
		return assertHeader(headerName, Arrays.asList(values), getHeaders().getOrEmpty(headerName));
	}

	/**
	 * 预期具有给定名称的请求头与给定 long 值匹配。
	 * @since 5.3
	 */
	public WebTestClient.ResponseSpec valueEquals(String headerName, long value) {
		String actual = getHeaders().getFirst(headerName);
		this.exchangeResult.assertWithDiagnostics(() ->
				assertTrue("Response does not contain header '" + headerName + "'", actual != null));
		return assertHeader(headerName, value, Long.parseLong(Objects.requireNonNull(actual)));
	}

	/**
	 * 预期具有给定名称的请求头与指定的 long 值匹配，
	 * 该值使用 RFC 7231 中描述的首选日期格式解析为日期。
	 * <p>如果响应不包含指定请求头，或者提供的 {@code value} 与
	 * 主请求头值不匹配，则抛出 {@link AssertionError}。
	 * @since 5.3
	 */
	public WebTestClient.ResponseSpec valueEqualsDate(String headerName, long value) {
		this.exchangeResult.assertWithDiagnostics(() -> {
			String headerValue = getHeaders().getFirst(headerName);
			assertNotNull("Response does not contain header '" + headerName + "'", headerValue);

			HttpHeaders headers = new HttpHeaders();
			headers.setDate("expected", value);
			headers.set("actual", headerValue);

			assertEquals("Response header '" + headerName + "'='" + headerValue + "' " +
							"does not match expected value '" + headers.getFirst("expected") + "'",
					headers.getFirstDate("expected"), headers.getFirstDate("actual"));
		});
		return this.responseSpec;
	}

	/**
	 * 使用正则表达式匹配响应头的第一个值。
	 * @param name 请求头名称
	 * @param pattern 正则表达式模式
	 */
	public WebTestClient.ResponseSpec valueMatches(String name, String pattern) {
		String value = getRequiredValue(name);
		String message = getMessage(name) + "=[" + value + "] does not match [" + pattern + "]";
		this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.assertTrue(message, value.matches(pattern)));
		return this.responseSpec;
	}

	/**
	 * 使用给定的正则表达式模式匹配响应头的所有值，
	 * 这些模式按相同顺序应用于请求头的各个值。请注意，模式数量必须
	 * 与实际值数量匹配。
	 * @param name 请求头名称
	 * @param patterns 一个或多个正则表达式模式，每个对应一个期望值
	 * @since 5.3
	 */
	public WebTestClient.ResponseSpec valuesMatch(String name, String... patterns) {
		this.exchangeResult.assertWithDiagnostics(() -> {
			List<String> values = getRequiredValues(name);
			AssertionErrors.assertTrue(
					getMessage(name) + " has fewer or more values " + values +
							" than number of patterns to match with " + Arrays.toString(patterns),
					values.size() == patterns.length);
			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				String pattern = patterns[i];
				AssertionErrors.assertTrue(
						getMessage(name) + "[" + i + "]='" + value + "' does not match '" + pattern + "'",
						value.matches(pattern));
			}
		});
		return this.responseSpec;
	}

	/**
	 * 使用 Hamcrest {@link Matcher} 断言响应头的第一个值。
	 * @param name 请求头名称
	 * @param matcher 要使用的匹配器
	 * @since 5.1
	 */
	public WebTestClient.ResponseSpec value(String name, Matcher<? super String> matcher) {
		String value = getHeaders().getFirst(name);
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name);
			MatcherAssert.assertThat(message, value, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * 使用 Hamcrest {@link Matcher} 断言响应头的所有值。
	 * @param name 请求头名称
	 * @param matcher 要使用的匹配器
	 * @since 5.3
	 */
	public WebTestClient.ResponseSpec values(String name, Matcher<? super Iterable<String>> matcher) {
		List<String> values = getHeaders().get(name);
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name);
			MatcherAssert.assertThat(message, values, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * 消费指定响应头的第一个值。
	 * @param name 请求头名称
	 * @param consumer 要使用的消费者
	 * @since 5.1
	 */
	public WebTestClient.ResponseSpec value(String name, Consumer<String> consumer) {
		String value = getRequiredValue(name);
		this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(value));
		return this.responseSpec;
	}

	/**
	 * 消费指定响应头的所有值。
	 * @param name 请求头名称
	 * @param consumer 要使用的消费者
	 * @since 5.3
	 */
	public WebTestClient.ResponseSpec values(String name, Consumer<List<String>> consumer) {
		List<String> values = getRequiredValues(name);
		this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(values));
		return this.responseSpec;
	}

	private String getRequiredValue(String name) {
		return getRequiredValues(name).get(0);
	}

	private List<String> getRequiredValues(String name) {
		List<String> values = getHeaders().get(name);
		if (CollectionUtils.isEmpty(values)) {
			this.exchangeResult.assertWithDiagnostics(() ->
					AssertionErrors.fail(getMessage(name) + " not found"));
		}
		return Objects.requireNonNull(values);
	}

	/**
	 * 预期具有给定名称的请求头存在。
	 * @since 5.0.3
	 */
	public WebTestClient.ResponseSpec exists(String name) {
		if (!getHeaders().containsKey(name)) {
			String message = getMessage(name) + " does not exist";
			this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.fail(message));
		}
		return this.responseSpec;
	}

	/**
	 * 预期具有给定名称的请求头不存在。
	 */
	public WebTestClient.ResponseSpec doesNotExist(String name) {
		if (getHeaders().containsKey(name)) {
			String message = getMessage(name) + " exists with value=[" + getHeaders().getFirst(name) + "]";
			this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.fail(message));
		}
		return this.responseSpec;
	}

	/**
	 * 预期具有给定值的“Cache-Control”请求头。
	 */
	public WebTestClient.ResponseSpec cacheControl(CacheControl cacheControl) {
		return assertHeader("Cache-Control", cacheControl.getHeaderValue(), getHeaders().getCacheControl());
	}

	/**
	 * 预期具有给定值的“Content-Disposition”请求头。
	 */
	public WebTestClient.ResponseSpec contentDisposition(ContentDisposition contentDisposition) {
		return assertHeader("Content-Disposition", contentDisposition, getHeaders().getContentDisposition());
	}

	/**
	 * 预期具有给定值的“Content-Length”请求头。
	 */
	public WebTestClient.ResponseSpec contentLength(long contentLength) {
		return assertHeader("Content-Length", contentLength, getHeaders().getContentLength());
	}

	/**
	 * 预期具有给定值的“Content-Type”请求头。
	 */
	public WebTestClient.ResponseSpec contentType(MediaType mediaType) {
		return assertHeader("Content-Type", mediaType, getHeaders().getContentType());
	}

	/**
	 * 预期具有给定值的“Content-Type”请求头。
	 */
	public WebTestClient.ResponseSpec contentType(String mediaType) {
		return contentType(MediaType.parseMediaType(mediaType));
	}

	/**
	 * 预期与给定值兼容的“Content-Type”请求头。
	 */
	public WebTestClient.ResponseSpec contentTypeCompatibleWith(MediaType mediaType) {
		MediaType actual = getHeaders().getContentType();
		String message = getMessage("Content-Type") + "=[" + actual + "] is not compatible with [" + mediaType + "]";
		this.exchangeResult.assertWithDiagnostics(() ->
				AssertionErrors.assertTrue(message, (actual != null && actual.isCompatibleWith(mediaType))));
		return this.responseSpec;
	}

	/**
	 * 预期与给定值兼容的“Content-Type”请求头。
	 */
	public WebTestClient.ResponseSpec contentTypeCompatibleWith(String mediaType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(mediaType));
	}

	/**
	 * 预期具有给定值的“Expires”请求头。
	 */
	public WebTestClient.ResponseSpec expires(long expires) {
		return assertHeader("Expires", expires, getHeaders().getExpires());
	}

	/**
	 * 预期具有给定值的“Last-Modified”请求头。
	 */
	public WebTestClient.ResponseSpec lastModified(long lastModified) {
		return assertHeader("Last-Modified", lastModified, getHeaders().getLastModified());
	}

	/**
	 * 预期具有给定值的“Location”请求头。
	 * @since 5.3
	 */
	public WebTestClient.ResponseSpec location(String location) {
		return assertHeader("Location", URI.create(location), getHeaders().getLocation());
	}


	private HttpHeaders getHeaders() {
		return this.exchangeResult.getResponseHeaders();
	}

	private String getMessage(String headerName) {
		return "Response header '" + headerName + "'";
	}

	private WebTestClient.ResponseSpec assertHeader(String name, @Nullable Object expected, @Nullable Object actual) {
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name);
			AssertionErrors.assertEquals(message, expected, actual);
		});
		return this.responseSpec;
	}

}
