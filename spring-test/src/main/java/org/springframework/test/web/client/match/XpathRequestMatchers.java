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

package org.springframework.test.web.client.match;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;

/**
 * 使用 XPath 表达式的请求内容 {@code RequestMatcher} 实现的
 * 工厂方法。
 *
 * <p>通常通过
 * {@link MockRestRequestMatchers#xpath(String, Object...)} 或
 * {@link MockRestRequestMatchers#xpath(String, Map, Object...)} 访问此类的实例。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class XpathRequestMatchers {

	private static final String DEFAULT_ENCODING = "UTF-8";

	private final XpathExpectationsHelper xpathHelper;


	/**
	 * 类构造函数，不用于直接实例化。
	 * <p>请使用 {@link MockRestRequestMatchers#xpath(String, Object...)} 或
	 * {@link MockRestRequestMatchers#xpath(String, Map, Object...)}。
	 * @param expression XPath 表达式
	 * @param namespaces XPath 表达式中引用的 XML 命名空间，或 {@code null}
	 * @param args 用于参数化 XPath 表达式的参数，使用
	 * {@link String#format(String, Object...)} 中定义的格式说明符
	 * @throws XPathExpressionException 如果表达式编译失败。
	 */
	protected XpathRequestMatchers(String expression, @Nullable Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
	}


	/**
	 * 应用 XPath，并使用给定的 {@code Matcher<Node>} 进行断言。
	 */
	public RequestMatcher node(Matcher<? super Node> matcher) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNode(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
	}

	/**
	 * 断言给定 XPath 处存在内容。
	 */
	public RequestMatcher exists() {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.exists(request.getBodyAsBytes(), DEFAULT_ENCODING);
	}

	/**
	 * 断言给定 XPath 处不存在内容。
	 */
	public RequestMatcher doesNotExist() {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.doesNotExist(request.getBodyAsBytes(), DEFAULT_ENCODING);
	}

	/**
	 * 应用 XPath，并使用给定的
	 * {@code Matcher<Integer>} 断言找到的节点数。
	 */
	public RequestMatcher nodeCount(Matcher<? super Integer> matcher) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
	}

	/**
	 * 应用 XPath，并断言找到的节点数。
	 */
	public RequestMatcher nodeCount(int expectedCount) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, expectedCount);
	}

	/**
	 * 应用 XPath，并使用给定匹配器断言找到的 String 内容。
	 */
	public RequestMatcher string(Matcher<? super String> matcher) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
	}

	/**
	 * 应用 XPath，并断言找到的 String 内容。
	 */
	public RequestMatcher string(String content) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, content);
	}

	/**
	 * 应用 XPath，并使用给定匹配器断言找到的数值。
	 */
	public RequestMatcher number(Matcher<? super Double> matcher) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
	}

	/**
	 * 应用 XPath，并断言找到的数值。
	 */
	public RequestMatcher number(Double value) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
	}

	/**
	 * 应用 XPath，并断言找到的布尔值。
	 */
	public RequestMatcher booleanValue(Boolean value) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertBoolean(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
	}


	/**
	 * XPath {@link RequestMatcher} 实现的功能接口。
	 */
	@FunctionalInterface
	private interface XpathRequestMatcher extends RequestMatcher {

		@Override
		default void match(ClientHttpRequest request) {
			try {
				matchInternal((MockClientHttpRequest) request);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse XML request content", ex);
			}
		}

		void matchInternal(MockClientHttpRequest request) throws Exception;
	}

}
