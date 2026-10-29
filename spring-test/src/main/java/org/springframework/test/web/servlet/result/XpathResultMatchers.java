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

package org.springframework.test.web.servlet.result;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * 使用 XPath 表达式对响应内容进行断言的工厂。
 *
 * <p>此类的实例通常通过 {@link MockMvcResultMatchers#xpath} 访问。
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class XpathResultMatchers {

	private final XpathExpectationsHelper xpathHelper;


	/**
	 * 受保护的构造函数，不用于直接实例化。请使用
	 * {@link MockMvcResultMatchers#xpath(String, Object...)} 或
	 * {@link MockMvcResultMatchers#xpath(String, Map, Object...)}。
	 * @param expression the XPath expression
	 * @param namespaces the XML namespaces referenced in the XPath expression, or {@code null}
	 * @param args arguments to parameterize the XPath expression with using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 */
	protected XpathResultMatchers(String expression, @Nullable Map<String, String> namespaces, Object ... args)
			throws XPathExpressionException {

		this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
	}


	/**
	 * 求值 XPath，并使用给定的 Hamcrest {@link Matcher}
	 * 断言找到的 {@link Node} 内容。
	 */
	public ResultMatcher node(Matcher<? super Node> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNode(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * 求值 XPath，并使用给定的 Hamcrest {@link Matcher}
	 * 断言找到的 {@link NodeList} 内容。
	 * @since 5.2.2
	 */
	public ResultMatcher nodeList(Matcher<? super NodeList> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNodeList(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * 如果响应中显式定义了响应编码，则返回该编码，否则返回 {@code null}。
	 */
	@Nullable
	private String getDefinedEncoding(MockHttpServletResponse response) {
		return (response.isCharset() ? response.getCharacterEncoding() : null);
	}

	/**
	 * 求值 XPath，并断言内容存在。
	 */
	public ResultMatcher exists() {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.exists(response.getContentAsByteArray(), getDefinedEncoding(response));
		};
	}

	/**
	 * 求值 XPath，并断言内容不存在。
	 */
	public ResultMatcher doesNotExist() {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.doesNotExist(response.getContentAsByteArray(), getDefinedEncoding(response));
		};
	}

	/**
	 * 求值 XPath，并使用给定的 Hamcrest {@link Matcher}
	 * 断言找到的节点数量。
	 */
	public ResultMatcher nodeCount(Matcher<? super Integer> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNodeCount(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * 求值 XPath，并断言找到的节点数量。
	 */
	public ResultMatcher nodeCount(int expectedCount) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNodeCount(response.getContentAsByteArray(), getDefinedEncoding(response), expectedCount);
		};
	}

	/**
	 * 应用 XPath，并使用给定的 Hamcrest {@link Matcher}
	 * 断言找到的 {@link String} 值。
	 */
	public ResultMatcher string(Matcher<? super String> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertString(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * 应用 XPath，并断言找到的 {@link String} 值。
	 */
	public ResultMatcher string(String expectedValue) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertString(response.getContentAsByteArray(), getDefinedEncoding(response), expectedValue);
		};
	}

	/**
	 * 求值 XPath，并使用给定的 Hamcrest {@link Matcher}
	 * 断言找到的 {@link Double} 值。
	 */
	public ResultMatcher number(Matcher<? super Double> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNumber(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * 求值 XPath，并断言找到的 {@link Double} 值。
	 */
	public ResultMatcher number(Double expectedValue) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNumber(response.getContentAsByteArray(), getDefinedEncoding(response), expectedValue);
		};
	}

	/**
	 * 求值 XPath，并断言找到的 {@link Boolean} 值。
	 */
	public ResultMatcher booleanValue(Boolean value) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertBoolean(response.getContentAsByteArray(), getDefinedEncoding(response), value);
		};
	}

}
