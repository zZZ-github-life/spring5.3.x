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

package org.springframework.test.util;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 独立于任何第三方断言库的测试断言。
 *
 * @author Lukas Krecan
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class AssertionErrors {

	/**
	 * 使用给定消息使测试失败。
	 * @param message 描述失败原因的消息
	 */
	public static void fail(String message) {
		throw new AssertionError(message);
	}

	/**
	 * 使用给定消息使测试失败，并同时传入要追加到消息中的期望值和实际值。
	 * <p>例如，给定：
	 * <pre class="code">
	 * String name = "Accept";
	 * String expected = "application/json";
	 * String actual = "text/plain";
	 * fail("Response header [" + name + "]", expected, actual);
	 * </pre>
	 * <p>生成的消息为：
	 * <pre class="code">
	 * Response header [Accept] expected:&lt;application/json&gt; but was:&lt;text/plain&gt;
	 * </pre>
	 * @param message 描述失败的用例的消息
	 * @param expected 期望值
	 * @param actual 实际值
	 */
	public static void fail(String message, @Nullable Object expected, @Nullable Object actual) {
		throw new AssertionError(message + " expected:<" + expected + "> but was:<" + actual + ">");
	}

	/**
	 * 断言给定条件为 {@code true}，否则抛出 {@link AssertionError}。
	 * @param message 描述失败原因的消息
	 * @param condition 要测试的条件
	 */
	public static void assertTrue(String message, boolean condition) {
		if (!condition) {
			fail(message);
		}
	}

	/**
	 * 断言给定条件为 {@code false}，否则抛出 {@link AssertionError}。
	 * @param message 描述失败原因的消息
	 * @param condition 要测试的条件
	 * @since 5.2.1
	 */
	public static void assertFalse(String message, boolean condition) {
		if (condition) {
			fail(message);
		}
	}

	/**
	 * 断言给定对象为 {@code null}，否则抛出 {@link AssertionError}。
	 * @param message 描述失败原因的消息
	 * @param object 要检查的对象
	 * @since 5.2.1
	 */
	public static void assertNull(String message, @Nullable Object object) {
		assertTrue(message, object == null);
	}

	/**
	 * 断言给定对象不为 {@code null}，否则抛出 {@link AssertionError}。
	 * @param message 描述失败原因的消息
	 * @param object 要检查的对象
	 * @since 5.1.8
	 */
	public static void assertNotNull(String message, @Nullable Object object) {
		assertTrue(message, object != null);
	}

	/**
	 * 断言两个对象相等，否则抛出 {@link AssertionError}。
	 * <p>例如：
	 * <pre class="code">
	 * assertEquals("Response header [" + name + "]", expected, actual);
	 * </pre>
	 * @param message 描述被检查的值的消息
	 * @param expected 期望值
	 * @param actual 实际值
	 * @see #fail(String, Object, Object)
	 */
	public static void assertEquals(String message, @Nullable Object expected, @Nullable Object actual) {
		if (!ObjectUtils.nullSafeEquals(expected, actual)) {
			fail(message, ObjectUtils.nullSafeToString(expected), ObjectUtils.nullSafeToString(actual));
		}
	}

	/**
	 * 断言两个对象不相等，否则抛出 {@link AssertionError}。
	 * <p>例如：
	 * <pre class="code">
	 * assertNotEquals("Response header [" + name + "]", expected, actual);
	 * </pre>
	 * @param message 描述被检查的值的消息
	 * @param expected 期望值
	 * @param actual 实际值
	 */
	public static void assertNotEquals(String message, @Nullable Object expected, @Nullable Object actual) {
		if (ObjectUtils.nullSafeEquals(expected, actual)) {
			throw new AssertionError(message + " was not expected to be:" +
					"<" + ObjectUtils.nullSafeToString(actual) + ">");
		}
	}

}
