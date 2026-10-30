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

package org.springframework.web.util;

/**
 * 用于 JavaScript 转义的实用工具类。
 * 基于 JavaScript 1.5 建议进行转义。
 *
 * <p>参考：
 * <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Grammar_and_types#string_literals">
 * Mozilla Developer Network 上的 JavaScript 指南</a>。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
public abstract class JavaScriptUtils {

	/**
	 * 将 JavaScript 特殊字符转换为转义字符。
	 * @param input 输入字符串
	 * @return 字符已转义的字符串
	 */
	public static String javaScriptEscape(String input) {
		StringBuilder filtered = new StringBuilder(input.length());
		char prevChar = '\u0000';
		char c;
		for (int i = 0; i < input.length(); i++) {
			c = input.charAt(i);
			if (c == '"') {
				filtered.append("\\\"");
			}
			else if (c == '\'') {
				filtered.append("\\'");
			}
			else if (c == '\\') {
				filtered.append("\\\\");
			}
			else if (c == '/') {
				filtered.append("\\/");
			}
			else if (c == '\t') {
				filtered.append("\\t");
			}
			else if (c == '\n') {
				if (prevChar != '\r') {
					filtered.append("\\n");
				}
			}
			else if (c == '\r') {
				filtered.append("\\n");
			}
			else if (c == '\f') {
				filtered.append("\\f");
			}
			else if (c == '\b') {
				filtered.append("\\b");
			}
			// No '\v' in Java, use octal value for VT ascii char
			else if (c == '\013') {
				filtered.append("\\v");
			}
			else if (c == '<') {
				filtered.append("\\u003C");
			}
			else if (c == '>') {
				filtered.append("\\u003E");
			}
			// Unicode for PS (line terminator in ECMA-262)
			else if (c == '\u2028') {
				filtered.append("\\u2028");
			}
			// Unicode for LS (line terminator in ECMA-262)
			else if (c == '\u2029') {
				filtered.append("\\u2029");
			}
			else {
				filtered.append(c);
			}
			prevChar = c;

		}
		return filtered.toString();
	}

}
