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

package org.springframework.test.util;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleNamespaceContext;

/**
 * 用于通过 XPath 表达式应用断言的辅助类。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class XpathExpectationsHelper {

	private final String expression;

	private final XPathExpression xpathExpression;

	private final boolean hasNamespaces;


	/**
	 * XpathExpectationsHelper 构造函数。
	 * @param expression the XPath expression
	 * @param namespaces the XML namespaces referenced in the XPath expression, or {@code null}
	 * @param args arguments to parameterize the XPath expression with using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @throws XPathExpressionException if expression compilation failed
	 */
	public XpathExpectationsHelper(String expression, @Nullable Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		this.expression = String.format(expression, args);
		this.xpathExpression = compileXpathExpression(this.expression, namespaces);
		this.hasNamespaces = !CollectionUtils.isEmpty(namespaces);
	}

	private static XPathExpression compileXpathExpression(String expression,
			@Nullable Map<String, String> namespaces) throws XPathExpressionException {

		SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
		namespaceContext.setBindings(namespaces != null ? namespaces : Collections.emptyMap());
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(namespaceContext);
		return xpath.compile(expression);
	}


	/**
	 * 返回编译后的 XPath 表达式。
	 */
	protected XPathExpression getXpathExpression() {
		return this.xpathExpression;
	}


	/**
	 * 解析内容，将 XPath 表达式求值为 {@link Node}，并使用给定的 {@code Matcher<Node>} 对其进行断言。
	 */
	public void assertNode(byte[] content, @Nullable String encoding, Matcher<? super Node> matcher)
			throws Exception {

		Node node = evaluateXpath(content, encoding, Node.class);
		MatcherAssert.assertThat("XPath " + this.expression, node, matcher);
	}

	/**
	 * 解析内容，将 XPath 表达式求值为 {@link NodeList}，并使用给定的 {@code Matcher<NodeList>} 对其进行断言。
	 * @since 5.2.2
	 */
	public void assertNodeList(byte[] content, @Nullable String encoding, Matcher<? super NodeList> matcher)
			throws Exception {

		Document document = parseXmlByteArray(content, encoding);
		NodeList nodeList = evaluateXpath(document, XPathConstants.NODESET, NodeList.class);
		MatcherAssert.assertThat("XPath " + this.getXpathExpression(), nodeList, matcher);
	}

	/**
	 * 应用 XPath 表达式并断言结果内容存在。
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void exists(byte[] content, @Nullable String encoding) throws Exception {
		Node node = evaluateXpath(content, encoding, Node.class);
		AssertionErrors.assertNotNull("XPath " + this.expression + " does not exist", node);
	}

	/**
	 * 应用 XPath 表达式并断言结果内容不存在。
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void doesNotExist(byte[] content, @Nullable String encoding) throws Exception {
		Node node = evaluateXpath(content, encoding, Node.class);
		AssertionErrors.assertNull("XPath " + this.expression + " exists", node);
	}

	/**
	 * 应用 XPath 表达式，并使用给定的 Hamcrest 匹配器断言结果内容。
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNodeCount(byte[] content, @Nullable String encoding, Matcher<? super Integer> matcher)
			throws Exception {

		NodeList nodeList = evaluateXpath(content, encoding, NodeList.class);
		String reason = "nodeCount for XPath " + this.expression;
		MatcherAssert.assertThat(reason, nodeList != null ? nodeList.getLength() : 0, matcher);
	}

	/**
	 * 应用 XPath 表达式，并将结果内容作为整数进行断言。
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNodeCount(byte[] content, @Nullable String encoding, int expectedCount) throws Exception {
		NodeList nodeList = evaluateXpath(content, encoding, NodeList.class);
		AssertionErrors.assertEquals("nodeCount for XPath " + this.expression, expectedCount,
				(nodeList != null ? nodeList.getLength() : 0));
	}

	/**
	 * 应用 XPath 表达式，并使用给定的 Hamcrest 匹配器断言结果内容。
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertString(byte[] content, @Nullable String encoding, Matcher<? super String> matcher)
			throws Exception {

		String actual = evaluateXpath(content, encoding, String.class);
		MatcherAssert.assertThat("XPath " + this.expression, actual, matcher);
	}

	/**
	 * 应用 XPath 表达式，并将结果内容作为 String 进行断言。
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertString(byte[] content, @Nullable String encoding, String expectedValue) throws Exception {
		String actual = evaluateXpath(content, encoding, String.class);
		AssertionErrors.assertEquals("XPath " + this.expression, expectedValue, actual);
	}

	/**
	 * 应用 XPath 表达式，并使用给定的 Hamcrest 匹配器断言结果内容。
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNumber(byte[] content, @Nullable String encoding, Matcher<? super Double> matcher) throws Exception {
		Double actual = evaluateXpath(content, encoding, Double.class);
		MatcherAssert.assertThat("XPath " + this.expression, actual, matcher);
	}

	/**
	 * 应用 XPath 表达式，并将结果内容作为 Double 进行断言。
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNumber(byte[] content, @Nullable String encoding, Double expectedValue) throws Exception {
		Double actual = evaluateXpath(content, encoding, Double.class);
		AssertionErrors.assertEquals("XPath " + this.expression, expectedValue, actual);
	}

	/**
	 * 应用 XPath 表达式，并将结果内容作为 Boolean 进行断言。
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertBoolean(byte[] content, @Nullable String encoding, boolean expectedValue) throws Exception {
		String actual = evaluateXpath(content, encoding, String.class);
		AssertionErrors.assertEquals("XPath " + this.expression, expectedValue, Boolean.parseBoolean(actual));
	}

	/**
	 * 求值 XPath 并返回结果值。
	 * @param content the content to evaluate against
	 * @param encoding the encoding to use (optionally)
	 * @param targetClass the target class, one of Number, String, Boolean,
	 * org.w3c.Node, or NodeList
	 * @throws Exception if content parsing or expression evaluation fails
	 * @since 5.1
	 */
	@Nullable
	public <T> T evaluateXpath(byte[] content, @Nullable String encoding, Class<T> targetClass) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		return evaluateXpath(document, toQName(targetClass), targetClass);
	}

	/**
	 * 将给定的 XML 内容解析为 {@link Document}。
	 * @param xml the content to parse
	 * @param encoding optional content encoding, if provided as metadata (e.g. in HTTP headers)
	 * @return the parsed document
	 */
	protected Document parseXmlByteArray(byte[] xml, @Nullable String encoding) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(this.hasNamespaces);
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		InputSource inputSource = new InputSource(new ByteArrayInputStream(xml));
		if (StringUtils.hasText(encoding)) {
			inputSource.setEncoding(encoding);
		}
		return documentBuilder.parse(inputSource);
	}

	/**
	 * 将 XPath 表达式应用于给定文档。
	 * @throws XPathExpressionException if expression evaluation failed
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected <T> T evaluateXpath(Document document, QName evaluationType, Class<T> expectedClass)
			throws XPathExpressionException {

		return (T) getXpathExpression().evaluate(document, evaluationType);
	}

	private <T> QName toQName(Class<T> expectedClass) {
		QName evaluationType;
		if (Number.class.isAssignableFrom(expectedClass)) {
			evaluationType = XPathConstants.NUMBER;
		}
		else if (CharSequence.class.isAssignableFrom(expectedClass)) {
			evaluationType = XPathConstants.STRING;
		}
		else if (Boolean.class.isAssignableFrom(expectedClass)) {
			evaluationType = XPathConstants.BOOLEAN;
		}
		else if (Node.class.isAssignableFrom(expectedClass)) {
			evaluationType = XPathConstants.NODE;
		}
		else if (NodeList.class.isAssignableFrom(expectedClass)) {
			evaluationType = XPathConstants.NODESET;
		}
		else {
			throw new IllegalArgumentException("Unexpected target class " + expectedClass + ". " +
					"Supported: numbers, strings, boolean, and org.w3c.Node and NodeList");
		}
		return evaluationType;
	}

}
