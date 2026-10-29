/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.client.match;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * 用于 {@link RequestMatcher} 类的静态工厂方法。通常用于
 * 为 {@link MockRestServiceServer#expect(RequestMatcher)} 提供输入。
 *
 * <h3>Eclipse 用户</h3>
 * <p>考虑将此类别添加为 Java 编辑器收藏项。要导航到
 * 此设置，请打开“首选项”并输入“favorites”。
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Simon Baslé
 * @since 3.2
 */
public abstract class MockRestRequestMatchers {

	/**
	 * 匹配任意请求。
	 */
	public static RequestMatcher anything() {
		return request -> {};
	}

	/**
	 * 断言请求的 {@link HttpMethod}。
	 * @param method the HTTP method
	 * @return the request matcher
	 */
	public static RequestMatcher method(HttpMethod method) {
		Assert.notNull(method, "'method' must not be null");
		return request -> assertEquals("Unexpected HttpMethod", method, request.getMethod());
	}

	/**
	 * 使用给定的 Hamcrest 匹配器断言请求 URI 字符串。
	 * @param matcher the String matcher for the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(Matcher<? super String> matcher) {
		Assert.notNull(matcher, "'matcher' must not be null");
		return request -> assertThat("Request URI", request.getURI().toString(), matcher);
	}

	/**
	 * 断言请求 URI 与给定字符串匹配。
	 * @param expectedUri the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(String expectedUri) {
		Assert.notNull(expectedUri, "'uri' must not be null");
		return request -> assertEquals("Request URI", expectedUri, request.getURI().toString());
	}

	/**
	 * 对 {@link #requestTo(URI)} 的变体，通过 {@link UriComponentsBuilder}
	 * 从 URI 模板及可选变量准备 URI，包括编码。
	 * @param expectedUri the expected URI template
	 * @param uriVars zero or more URI variables to populate the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestToUriTemplate(String expectedUri, Object... uriVars) {
		Assert.notNull(expectedUri, "'uri' must not be null");
		URI uri = UriComponentsBuilder.fromUriString(expectedUri).buildAndExpand(uriVars).encode().toUri();
		return requestTo(uri);
	}

	/**
	 * 预期对给定 URI 的请求。
	 * @param uri the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(URI uri) {
		Assert.notNull(uri, "'uri' must not be null");
		return request -> assertEquals("Unexpected request", uri, request.getURI());
	}

	/**
	 * 使用给定的 Hamcrest 匹配器断言请求查询参数值，
	 * 匹配整个值 {@link List}。
	 * <p>例如，可用于检查查询参数值列表是否至少有一个值与给定 Hamcrest 匹配器匹配（如
	 * {@link org.hamcrest.Matchers#hasItem(Matcher)}），列表中的每个值是否
	 * 都匹配通用条件（如 {@link org.hamcrest.Matchers#everyItem(Matcher)}），
	 * 列表中的每个值是否都匹配相应的专用条件
	 * （如 {@link org.hamcrest.Matchers#contains(Matcher[])}）等。
	 * @param name the name of the query parameter whose value(s) will be asserted
	 * @param matcher the Hamcrest matcher to apply to the entire list of values
	 * for the given query parameter
	 * @since 5.3.27
	 * @see #queryParam(String, Matcher...)
	 * @see #queryParam(String, String...)
	 */
	public static RequestMatcher queryParamList(String name, Matcher<? super List<String>> matcher) {
		return request -> {
			MultiValueMap<String, String> params = getQueryParams(request);
			List<String> paramValues = params.get(name);
			if (paramValues == null) {
				fail("Expected query param <" + name + "> to exist but was null");
			}
			assertThat("Query param [" + name + "] values", paramValues, matcher);
		};
	}

	/**
	 * 使用给定的 Hamcrest 匹配器断言请求查询参数值。
	 * <p>如果查询参数值列表大于提供的
	 * {@code matchers} 数量，则不会对多余的查询参数值应用任何匹配器，
	 * 实际上会忽略额外的参数值。如果提供的 {@code matchers} 数量超过查询参数值的数量，
	 * 将抛出 {@link AssertionError} 以表示不匹配。
	 * <p>有关接受适用于整个值列表（而不是仅适用于单个值）的
	 * {@code Matcher} 的变体，请参见 {@link #queryParamList(String, Matcher)}。
	 * @param name the name of the query parameter whose value(s) will be asserted
	 * @param matchers the Hamcrest matchers to apply to individual query parameter
	 * values; the n<sup>th</sup> matcher is applied to the n<sup>th</sup> query
	 * parameter value
	 * @see #queryParamList(String, Matcher)
	 * @see #queryParam(String, String...)
	 */
	@SafeVarargs
	public static RequestMatcher queryParam(String name, Matcher<? super String>... matchers) {
		return request -> {
			MultiValueMap<String, String> params = getQueryParams(request);
			assertValueCount("query param", name, params, matchers.length);
			for (int i = 0 ; i < matchers.length; i++) {
				assertThat("Query param", params.get(name).get(i), matchers[i]);
			}
		};
	}

	/**
	 * 断言请求查询参数值。
	 * <p>如果查询参数值列表大于
	 * {@code expectedValues} 的数量，将不会对多余的查询参数值应用任何断言，
	 * 实际上会忽略额外的参数值。如果 {@code expectedValues} 的数量超过查询参数值的数量，
	 * 将抛出 {@link AssertionError} 以表示不匹配。
	 * <p>有关接受适用于整个值列表（而不是仅断言单个值）的
	 * Hamcrest {@code Matcher} 的变体，请参见 {@link #queryParamList(String, Matcher)}。
	 * @param name the name of the query parameter whose value(s) will be asserted
	 * @param expectedValues the expected values of individual query parameter values;
	 * the n<sup>th</sup> expected value is compared to the n<sup>th</sup> query
	 * parameter value
	 * @see #queryParamList(String, Matcher)
	 * @see #queryParam(String, Matcher...)
	 */
	public static RequestMatcher queryParam(String name, String... expectedValues) {
		return request -> {
			MultiValueMap<String, String> params = getQueryParams(request);
			assertValueCount("query param", name, params, expectedValues.length);
			for (int i = 0 ; i < expectedValues.length; i++) {
				assertEquals("Query param [" + name + "]", expectedValues[i], params.get(name).get(i));
			}
		};
	}

	private static MultiValueMap<String, String> getQueryParams(ClientHttpRequest request) {
		return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
	}

	/**
	 * 使用给定的 Hamcrest 匹配器断言请求头值，
	 * 匹配整个值 {@link List}。
	 * <p>例如，可用于检查请求头值列表是否至少有一个值与给定 Hamcrest 匹配器匹配（如
	 * {@link org.hamcrest.Matchers#hasItem(Matcher)}），列表中的每个值是否
	 * 都匹配通用条件（如 {@link org.hamcrest.Matchers#everyItem(Matcher)}），
	 * 列表中的每个值是否都匹配相应的专用条件
	 * （如 {@link org.hamcrest.Matchers#contains(Matcher[])}）等。
	 * @param name the name of the header whose value(s) will be asserted
	 * @param matcher the Hamcrest matcher to apply to the entire list of values
	 * for the given header
	 * @since 5.3.27
	 * @see #header(String, Matcher...)
	 * @see #header(String, String...)
	 */
	public static RequestMatcher headerList(String name, Matcher<? super List<String>> matcher) {
		return request -> {
			List<String> headerValues = request.getHeaders().get(name);
			if (headerValues == null) {
				fail("Expected header <" + name + "> to exist but was null");
			}
			assertThat("Request header [" + name + "] values", headerValues, matcher);
		};
	}

	/**
	 * 使用给定的 Hamcrest 匹配器断言请求头值。
	 * <p>如果请求头值列表大于提供的
	 * {@code matchers} 数量，则不会对多余的请求头值应用任何匹配器，
	 * 实际上会忽略额外的请求头值。如果提供的 {@code matchers} 数量超过请求头值的数量，
	 * 将抛出 {@link AssertionError} 以表示不匹配。
	 * <p>有关接受适用于整个值列表（而不是仅适用于单个值）的
	 * Hamcrest {@code Matcher} 的变体，请参见 {@link #headerList(String, Matcher)}。
	 * @param name the name of the header whose value(s) will be asserted
	 * @param matchers the Hamcrest matchers to apply to individual header values;
	 * the n<sup>th</sup> matcher is applied to the n<sup>th</sup> header value
	 * @see #headerList(String, Matcher)
	 * @see #header(String, String...)
	 */
	@SafeVarargs
	public static RequestMatcher header(String name, Matcher<? super String>... matchers) {
		return request -> {
			assertValueCount("header", name, request.getHeaders(), matchers.length);
			List<String> headerValues = request.getHeaders().get(name);
			Assert.state(headerValues != null, "No header values");
			for (int i = 0; i < matchers.length; i++) {
				assertThat("Request header [" + name + "]", headerValues.get(i), matchers[i]);
			}
		};
	}

	/**
	 * 断言请求头值。
	 * <p>如果请求头值列表大于 {@code expectedValues} 的数量，
	 * 则不会对多余的请求头值应用任何匹配器，实际上会忽略
	 * 额外的请求头值。如果 {@code expectedValues} 的数量超过请求头值的数量，
	 * 将抛出 {@link AssertionError} 以表示
	 * 不匹配。
	 * <p>有关接受适用于整个值列表（而不是仅适用于单个值）的
	 * Hamcrest {@code Matcher} 的变体，请参见 {@link #headerList(String, Matcher)}。
	 * @param name the name of the header whose value(s) will be asserted
	 * @param expectedValues the expected values of individual header values; the
	 * n<sup>th</sup> expected value is compared to the n<sup>th</sup> header value
	 * @see #headerList(String, Matcher)
	 * @see #header(String, Matcher...)
	 */
	public static RequestMatcher header(String name, String... expectedValues) {
		return request -> {
			assertValueCount("header", name, request.getHeaders(), expectedValues.length);
			List<String> headerValues = request.getHeaders().get(name);
			Assert.state(headerValues != null, "No header values");
			for (int i = 0; i < expectedValues.length; i++) {
				assertEquals("Request header [" + name + "]", expectedValues[i], headerValues.get(i));
			}
		};
	}

	/**
	 * 断言给定请求头不存在。
	 * @since 5.2
	 */
	public static RequestMatcher headerDoesNotExist(String name) {
		return request -> {
			List<String> headerValues = request.getHeaders().get(name);
			if (headerValues != null) {
				fail("Expected header <" + name + "> not to exist, but it exists with values: " +
						headerValues);
			}
		};
	}

	/**
	 * 访问请求体匹配器。
	 */
	public static ContentRequestMatchers content() {
		return new ContentRequestMatchers();
	}

	/**
	 * 使用 <a href="https://github.com/jayway/JsonPath">JsonPath</a> 表达式
	 * 访问请求体匹配器，以检查请求体的特定子集。JSON 路径表达式可以是
	 * 使用 {@link String#format(String, Object...)} 中定义的格式说明符的
	 * 参数化字符串。
	 * @param expression the JSON path optionally parameterized with arguments
	 * @param args arguments to parameterize the JSON path expression with
	 */
	public static JsonPathRequestMatchers jsonPath(String expression, Object... args) {
		return new JsonPathRequestMatchers(expression, args);
	}

	/**
	 * 使用 <a href="https://github.com/jayway/JsonPath">JsonPath</a> 表达式
	 * 和 Hamcrest 匹配器访问请求体匹配器，以检查请求体的特定子集
	 * 并断言在 JSON 路径处找到的值。
	 * @param expression the JSON path expression
	 * @param matcher a matcher for the value expected at the JSON path
	 */
	public static <T> RequestMatcher jsonPath(String expression, Matcher<? super T> matcher) {
		return new JsonPathRequestMatchers(expression).value(matcher);
	}

	/**
	 * 使用 XPath 访问请求体匹配器，以检查请求体的特定
	 * 子集。XPath 表达式可以是使用
	 * {@link String#format(String, Object...)} 中定义的格式说明符的
	 * 参数化字符串。
	 * @param expression the XPath optionally parameterized with arguments
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathRequestMatchers xpath(String expression, Object... args) throws XPathExpressionException {
		return new XpathRequestMatchers(expression, null, args);
	}

	/**
	 * 使用 XPath 访问响应体匹配器，以检查响应体的特定
	 * 子集。XPath 表达式可以是使用
	 * {@link String#format(String, Object...)} 中定义的格式说明符的
	 * 参数化字符串。
	 * @param expression the XPath optionally parameterized with arguments
	 * @param namespaces the namespaces referenced in the XPath expression
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathRequestMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		return new XpathRequestMatchers(expression, namespaces, args);
	}


	private static void assertValueCount(
			String valueType, String name, MultiValueMap<String, String> map, int count) {

		List<String> values = map.get(name);
		String message = "Expected " + valueType + " <" + name + ">";
		if (values == null) {
			fail(message + " to exist but was null");
		}
		if (count > values.size()) {
			fail(message + " to have at least <" + count + "> values but found " + values);
		}
	}

}
