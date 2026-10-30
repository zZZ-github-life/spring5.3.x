/*
 * Copyright 2002-2022 the original author or authors.
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

import org.skyscreamer.jsonassert.JSONAssert;

/**
 * 用于 JSON 内容断言的辅助类。
 *
 * <p>使用此类需要 <a href="https://jsonassert.skyscreamer.org/">JSONassert</a> 库。
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class JsonExpectationsHelper {

	/**
	 * 将期望字符串和实际字符串解析为 JSON，并断言两者“相似” - 即无论格式如何，它们都包含相同的属性值对，且采用宽松检查（可扩展内容和非严格数组顺序）。
	 * @param expected 期望的 JSON 内容
	 * @param actual 实际的 JSON 内容
	 * @since 4.1
	 * @see #assertJsonEqual(String, String, boolean)
	 */
	public void assertJsonEqual(String expected, String actual) throws Exception {
		assertJsonEqual(expected, actual, false);
	}

	/**
	 * 将期望字符串和实际字符串解析为 JSON，并断言两者“相似” - 即无论格式如何，它们都包含相同的属性值对。
	 * <p>根据 {@code strict} 参数值，可以按两种模式进行比较：
	 * <ul>
	 * <li>{@code true}：严格检查。不可扩展且严格数组顺序。</li>
	 * <li>{@code false}：宽松检查。可扩展且非严格数组顺序。</li>
	 * </ul>
	 * @param expected 期望的 JSON 内容
	 * @param actual 实际的 JSON 内容
	 * @param strict 如果为 {@code true} 则启用严格检查
	 * @since 4.2
	 */
	public void assertJsonEqual(String expected, String actual, boolean strict) throws Exception {
		JSONAssert.assertEquals(expected, actual, strict);
	}

	/**
	 * 将期望字符串和实际字符串解析为 JSON，并断言两者“不相似” - 即无论格式如何，它们都包含不同的属性值对，且采用宽松检查（可扩展，以及非严格数组顺序）。
	 * @param expected 期望的 JSON 内容
	 * @param actual 实际的 JSON 内容
	 * @since 4.1
	 * @see #assertJsonNotEqual(String, String, boolean)
	 */
	public void assertJsonNotEqual(String expected, String actual) throws Exception {
		assertJsonNotEqual(expected, actual, false);
	}

	/**
	 * 将期望字符串和实际字符串解析为 JSON，并断言两者“不相似” - 即无论格式如何，它们都包含不同的属性值对。
	 * <p>根据 {@code strict} 参数值，可以按两种模式进行比较：
	 * <ul>
	 * <li>{@code true}：严格检查。不可扩展，且严格数组顺序。</li>
	 * <li>{@code false}：宽松检查。可扩展，且非严格数组顺序。</li>
	 * </ul>
	 * @param expected 期望的 JSON 内容
	 * @param actual 实际的 JSON 内容
	 * @param strict 启用严格检查
	 * @since 4.2
	 */
	public void assertJsonNotEqual(String expected, String actual, boolean strict) throws Exception {
		JSONAssert.assertNotEquals(expected, actual, strict);
	}

}
