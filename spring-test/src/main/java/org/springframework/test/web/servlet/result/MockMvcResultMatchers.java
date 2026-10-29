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

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * 基于 {@link ResultMatcher} 的结果操作的静态工厂方法。
 *
 * <h3>Eclipse 用户</h3>
 * <p>建议将此添加为 Java 编辑器收藏项。要导航到此设置，
 * 请打开 Preferences 并输入 “favorites”。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class MockMvcResultMatchers {

	private static final AntPathMatcher pathMatcher = new AntPathMatcher();


	/**
	 * 访问与请求相关的断言。
	 */
	public static RequestResultMatchers request() {
		return new RequestResultMatchers();
	}

	/**
	 * 访问针对处理该请求的处理器的断言。
	 */
	public static HandlerResultMatchers handler() {
		return new HandlerResultMatchers();
	}

	/**
	 * 访问与模型相关的断言。
	 */
	public static ModelResultMatchers model() {
		return new ModelResultMatchers();
	}

	/**
	 * 访问对所选视图的断言。
	 */
	public static ViewResultMatchers view() {
		return new ViewResultMatchers();
	}

	/**
	 * 访问 flash 属性断言。
	 */
	public static FlashAttributeResultMatchers flash() {
		return new FlashAttributeResultMatchers();
	}

	/**
	 * 断言请求已转发到给定 URL。
	 * <p>此方法仅接受精确匹配。
	 * @param expectedUrl the exact URL expected
	 */
	public static ResultMatcher forwardedUrl(@Nullable String expectedUrl) {
		return result -> assertEquals("Forwarded URL", expectedUrl, result.getResponse().getForwardedUrl());
	}

	/**
	 * 断言请求已转发到给定 URL 模板。
	 * <p>此方法接受对展开并编码后的 URL 模板的精确匹配。
	 * @param urlTemplate a URL template; the expanded URL will be encoded
	 * @param uriVars zero or more URI variables to populate the template
	 * @see UriComponentsBuilder#fromUriString(String)
	 */
	public static ResultMatcher forwardedUrlTemplate(String urlTemplate, Object... uriVars) {
		String uri = UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(uriVars).encode().toUriString();
		return forwardedUrl(uri);
	}

	/**
	 * 断言请求已转发到给定 URL。
	 * <p>此方法接受 {@link org.springframework.util.AntPathMatcher}
	 * 模式。
	 * @param urlPattern an Ant-style path pattern to match against
	 * @since 4.0
	 * @see org.springframework.util.AntPathMatcher
	 */
	public static ResultMatcher forwardedUrlPattern(String urlPattern) {
		return result -> {
			assertTrue("'" + urlPattern + "' is not an Ant-style path pattern",
					pathMatcher.isPattern(urlPattern));
			String url = result.getResponse().getForwardedUrl();
			assertTrue("Forwarded URL '" + url + "' does not match the expected URL pattern '" + urlPattern + "'",
					(url != null && pathMatcher.match(urlPattern, url)));
		};
	}

	/**
	 * 断言请求已重定向到给定 URL。
	 * <p>此方法仅接受精确匹配。
	 * @param expectedUrl the exact URL expected
	 */
	public static ResultMatcher redirectedUrl(String expectedUrl) {
		return result -> assertEquals("Redirected URL", expectedUrl, result.getResponse().getRedirectedUrl());
	}

	/**
	 * 断言请求已重定向到给定 URL 模板。
	 * <p>此方法接受对展开并编码后的 URL 模板的精确匹配。
	 * @param urlTemplate a URL template; the expanded URL will be encoded
	 * @param uriVars zero or more URI variables to populate the template
	 * @see UriComponentsBuilder#fromUriString(String)
	 */
	public static ResultMatcher redirectedUrlTemplate(String urlTemplate, Object... uriVars) {
		String uri = UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(uriVars).encode().toUriString();
		return redirectedUrl(uri);
	}

	/**
	 * 断言请求已重定向到给定 URL。
	 * <p>此方法接受 {@link org.springframework.util.AntPathMatcher}
	 * 模式。
	 * @param urlPattern an Ant-style path pattern to match against
	 * @since 4.0
	 * @see org.springframework.util.AntPathMatcher
	 */
	public static ResultMatcher redirectedUrlPattern(String urlPattern) {
		return result -> {
			assertTrue("'" + urlPattern + "' is not an Ant-style path pattern",
					pathMatcher.isPattern(urlPattern));
			String url = result.getResponse().getRedirectedUrl();
			assertTrue("Redirected URL '" + url + "' does not match the expected URL pattern '" + urlPattern + "'",
					(url != null && pathMatcher.match(urlPattern, url)));
		};
	}

	/**
	 * 访问响应状态断言。
	 */
	public static StatusResultMatchers status() {
		return new StatusResultMatchers();
	}

	/**
	 * 访问响应头断言。
	 */
	public static HeaderResultMatchers header() {
		return new HeaderResultMatchers();
	}

	/**
	 * 访问响应体断言。
	 */
	public static ContentResultMatchers content() {
		return new ContentResultMatchers();
	}

	/**
	 * 访问使用 <a href="https://github.com/jayway/JsonPath">JsonPath</a> 表达式
	 * 检查响应体特定子集的响应体断言。
	 * <p>JSON 路径表达式可以是使用
	 * {@link String#format(String, Object...)} 中定义的格式说明符的参数化字符串。
	 * @param expression the JSON path expression, optionally parameterized with arguments
	 * @param args arguments to parameterize the JSON path expression with
	 * @see #jsonPath(String, Matcher)
	 * @see #jsonPath(String, Matcher, Class)
	 */
	public static JsonPathResultMatchers jsonPath(String expression, Object... args) {
		return new JsonPathResultMatchers(expression, args);
	}

	/**
	 * 对响应体求值给定的 <a href="https://github.com/jayway/JsonPath">JsonPath</a>
	 * 表达式，并使用给定的 Hamcrest {@link Matcher} 断言结果值。
	 * @param expression the JSON path expression
	 * @param matcher a matcher for the value expected at the JSON path
	 * @see #jsonPath(String, Object...)
	 * @see #jsonPath(String, Matcher, Class)
	 */
	public static <T> ResultMatcher jsonPath(String expression, Matcher<? super T> matcher) {
		return new JsonPathResultMatchers(expression).value(matcher);
	}

	/**
	 * 对响应体求值给定的 <a href="https://github.com/jayway/JsonPath">JsonPath</a>
	 * 表达式，并在应用匹配器之前将结果值强制转换为给定目标类型，
	 * 然后使用给定的 Hamcrest {@link Matcher} 断言结果值。
	 * <p>这对于可靠地匹配数字很有用 &mdash; 例如，
	 * 将整数强制转换为 double。
	 * @param expression the JSON path expression
	 * @param matcher a matcher for the value expected at the JSON path
	 * @param targetType the target type to coerce the matching value into
	 * @since 5.2
	 * @see #jsonPath(String, Object...)
	 * @see #jsonPath(String, Matcher)
	 */
	public static <T> ResultMatcher jsonPath(String expression, Matcher<? super T> matcher, Class<T> targetType) {
		return new JsonPathResultMatchers(expression).value(matcher, targetType);
	}

	/**
	 * 访问使用 XPath 表达式检查响应体特定子集的响应体断言。
	 * <p>XPath 表达式可以是使用格式化说明符的参数化字符串，
	 * 这些说明符定义于 {@link String#format(String, Object...)}。
	 * @param expression the XPath expression, optionally parameterized with arguments
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathResultMatchers xpath(String expression, Object... args) throws XPathExpressionException {
		return new XpathResultMatchers(expression, null, args);
	}

	/**
	 * 访问使用 XPath 表达式检查响应体特定子集的响应体断言。
	 * <p>XPath 表达式可以是使用格式化说明符的参数化字符串，
	 * 这些说明符定义于 {@link String#format(String, Object...)}。
	 * @param expression the XPath expression, optionally parameterized with arguments
	 * @param namespaces the namespaces referenced in the XPath expression
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathResultMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		return new XpathResultMatchers(expression, namespaces, args);
	}

	/**
	 * 访问响应 cookie 断言。
	 */
	public static CookieResultMatchers cookie() {
		return new CookieResultMatchers();
	}

}
