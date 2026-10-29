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

package org.springframework.test.web.reactive.server;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * 用于 {@link WebTestClient} 的 XPath 断言。
 * @author Eric Deandrea
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class XpathAssertions {

	private final WebTestClient.BodyContentSpec bodySpec;

	private final XpathExpectationsHelper xpathHelper;


	XpathAssertions(WebTestClient.BodyContentSpec spec,
			String expression, @Nullable Map<String, String> namespaces, Object... args) {

		this.bodySpec = spec;
		this.xpathHelper = initXpathHelper(expression, namespaces, args);
	}

	private static XpathExpectationsHelper initXpathHelper(
			String expression, @Nullable Map<String, String> namespaces, Object[] args) {

		try {
			return new XpathExpectationsHelper(expression, namespaces, args);
		}
		catch (XPathExpressionException ex) {
			throw new AssertionError("XML parsing error", ex);
		}
	}


	/**
	 * 委托给 {@link XpathExpectationsHelper#assertString(byte[], String, String)}。
	 */
	public WebTestClient.BodyContentSpec isEqualTo(String expectedValue) {
		return assertWith(() -> this.xpathHelper.assertString(getContent(), getCharset(), expectedValue));
	}

	/**
	 * 委托给 {@link XpathExpectationsHelper#assertNumber(byte[], String, Double)}。
	 */
	public WebTestClient.BodyContentSpec isEqualTo(Double expectedValue) {
		return assertWith(() -> this.xpathHelper.assertNumber(getContent(), getCharset(), expectedValue));
	}

	/**
	 * 委托给 {@link XpathExpectationsHelper#assertBoolean(byte[], String, boolean)}。
	 */
	public WebTestClient.BodyContentSpec isEqualTo(boolean expectedValue) {
		return assertWith(() -> this.xpathHelper.assertBoolean(getContent(), getCharset(), expectedValue));
	}

	/**
	 * 委托给 {@link XpathExpectationsHelper#exists(byte[], String)}。
	 */
	public WebTestClient.BodyContentSpec exists() {
		return assertWith(() -> this.xpathHelper.exists(getContent(), getCharset()));
	}

	/**
	 * 委托给 {@link XpathExpectationsHelper#doesNotExist(byte[], String)}。
	 */
	public WebTestClient.BodyContentSpec doesNotExist() {
		return assertWith(() -> this.xpathHelper.doesNotExist(getContent(), getCharset()));
	}

	/**
	 * 委托给 {@link XpathExpectationsHelper#assertNodeCount(byte[], String, int)}。
	 */
	public WebTestClient.BodyContentSpec nodeCount(int expectedCount) {
		return assertWith(() -> this.xpathHelper.assertNodeCount(getContent(), getCharset(), expectedCount));
	}

	/**
	 * 委托给 {@link XpathExpectationsHelper#assertString(byte[], String, Matcher)}。
	 * @since 5.1
	 */
	public WebTestClient.BodyContentSpec string(Matcher<? super String> matcher){
		return assertWith(() -> this.xpathHelper.assertString(getContent(), getCharset(), matcher));
	}

	/**
	 * 委托给 {@link XpathExpectationsHelper#assertNumber(byte[], String, Matcher)}。
	 * @since 5.1
	 */
	public WebTestClient.BodyContentSpec number(Matcher<? super Double> matcher){
		return assertWith(() -> this.xpathHelper.assertNumber(getContent(), getCharset(), matcher));
	}

	/**
	 * 委托给 {@link XpathExpectationsHelper#assertNodeCount(byte[], String, Matcher)}。
	 * @since 5.1
	 */
	public WebTestClient.BodyContentSpec nodeCount(Matcher<? super Integer> matcher){
		return assertWith(() -> this.xpathHelper.assertNodeCount(getContent(), getCharset(), matcher));
	}

	/**
	 * 以 String 形式消费 XPath 求值的结果。
	 * @since 5.1
	 */
	public WebTestClient.BodyContentSpec string(Consumer<String> consumer){
		return assertWith(() -> {
			String value = this.xpathHelper.evaluateXpath(getContent(), getCharset(), String.class);
			consumer.accept(value);
		});
	}

	/**
	 * 以 Double 形式消费 XPath 求值的结果。
	 * @since 5.1
	 */
	public WebTestClient.BodyContentSpec number(Consumer<Double> consumer){
		return assertWith(() -> {
			Double value = this.xpathHelper.evaluateXpath(getContent(), getCharset(), Double.class);
			consumer.accept(value);
		});
	}

	/**
	 * 消费作为 XPath 求值结果的节点数。
	 * @since 5.1
	 */
	public WebTestClient.BodyContentSpec nodeCount(Consumer<Integer> consumer){
		return assertWith(() -> {
			Integer value = this.xpathHelper.evaluateXpath(getContent(), getCharset(), Integer.class);
			consumer.accept(value);
		});
	}

	private WebTestClient.BodyContentSpec assertWith(CheckedExceptionTask task) {
		try {
			task.run();
		}
		catch (Exception ex) {
			throw new AssertionError("XML parsing error", ex);
		}
		return this.bodySpec;
	}

	private byte[] getContent() {
		byte[] body = this.bodySpec.returnResult().getResponseBody();
		Assert.notNull(body, "Expected body content");
		return body;
	}

	private String getCharset() {
		return Optional.of(this.bodySpec.returnResult())
				.map(EntityExchangeResult::getResponseHeaders)
				.map(HttpHeaders::getContentType)
				.map(MimeType::getCharset)
				.orElse(StandardCharsets.UTF_8)
				.name();
	}


	@Override
	public boolean equals(Object obj) {
		throw new AssertionError("Object#equals is disabled " +
				"to avoid being used in error instead of XPathAssertions#isEqualTo(String).");
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}


	/**
	 * 使我们能够使用可能抛出受检异常的 lambda 表达式，因为
	 * {@link XpathExpectationsHelper} 的方法会抛出 {@link Exception}。
	 */
	private interface CheckedExceptionTask {

		void run() throws Exception;

	}
}
