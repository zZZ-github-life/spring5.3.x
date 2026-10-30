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

package org.springframework.web.util;

import org.springframework.util.Assert;

/**
 * 用于 HTML 转义的实用工具类。
 *
 * <p>基于 W3C HTML 4.01 建议执行转义和反转义，并处理
 * 字符实体引用。
 *
 * <p>参考：
 * <a href="https://www.w3.org/TR/html4/charset.html">https://www.w3.org/TR/html4/charset.html</a>
 *
 * <p>如需全面的字符串转义工具集，可考虑
 * <a href="https://commons.apache.org/proper/commons-text/">Apache Commons Text</a>
 * 及其 {@code StringEscapeUtils} 类。我们在此处不使用该类，以避免仅为 HTML 转义
 * 而引入对 Commons Text 的运行时依赖。此外，Spring 的 HTML 转义更灵活，
 * 并且 100% 符合 HTML 4.0。
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @author Craig Andrews
 * @since 01.03.2003
 */
public abstract class HtmlUtils {

	/**
	 * 预解析的 HTML 字符实体引用的共享实例。
	 */
	private static final HtmlCharacterEntityReferences characterEntityReferences =
			new HtmlCharacterEntityReferences();


	/**
	 * 将特殊字符转换为 HTML 字符引用。
	 * <p>处理 HTML 4.01 建议中定义的完整字符集。
	 * <p>将所有特殊字符转义为其对应的实体引用
	 * （例如 {@code &lt;}）。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input （未转义的）输入字符串
	 * @return 转义后的字符串
	 */
	public static String htmlEscape(String input) {
		return htmlEscape(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 将特殊字符转换为 HTML 字符引用。
	 * <p>处理 HTML 4.01 建议中定义的完整字符集。
	 * <p>将所有特殊字符至少按指定编码的要求转义为其对应的
	 * 实体引用（例如 {@code &lt;}）。换言之，如果某个特殊字符
	 * 在给定编码下不必转义，则可能不会被转义。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input （未转义的）输入字符串
	 * @param encoding 受支持的 {@link java.nio.charset.Charset charset} 名称
	 * @return 转义后的字符串
	 * @since 4.1.2
	 */
	public static String htmlEscape(String input, String encoding) {
		Assert.notNull(input, "Input is required");
		Assert.notNull(encoding, "Encoding is required");
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			String reference = characterEntityReferences.convertToReference(character, encoding);
			if (reference != null) {
				escaped.append(reference);
			}
			else {
				escaped.append(character);
			}
		}
		return escaped.toString();
	}

	/**
	 * 将特殊字符转换为 HTML 字符引用。
	 * <p>处理 HTML 4.01 建议中定义的完整字符集。
	 * <p>将所有特殊字符转义为其对应的十进制格式
	 * 数字引用（&amp;#<i>Decimal</i>;）。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input （未转义的）输入字符串
	 * @return 转义后的字符串
	 */
	public static String htmlEscapeDecimal(String input) {
		return htmlEscapeDecimal(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 将特殊字符转换为 HTML 字符引用。
	 * <p>处理 HTML 4.01 建议中定义的完整字符集。
	 * <p>将所有特殊字符至少按指定编码的要求转义为其对应的十进制格式
	 * 数字引用（&amp;#<i>Decimal</i>;）。换言之，如果某个特殊字符
	 * 在给定编码下不必转义，则可能不会被转义。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input （未转义的）输入字符串
	 * @param encoding 受支持的 {@link java.nio.charset.Charset charset} 名称
	 * @return 转义后的字符串
	 * @since 4.1.2
	 */
	public static String htmlEscapeDecimal(String input, String encoding) {
		Assert.notNull(input, "Input is required");
		Assert.notNull(encoding, "Encoding is required");
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			if (characterEntityReferences.isMappedToReference(character, encoding)) {
				escaped.append(HtmlCharacterEntityReferences.DECIMAL_REFERENCE_START);
				escaped.append((int) character);
				escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
			}
			else {
				escaped.append(character);
			}
		}
		return escaped.toString();
	}

	/**
	 * 将特殊字符转换为 HTML 字符引用。
	 * <p>处理 HTML 4.01 建议中定义的完整字符集。
	 * <p>将所有特殊字符转义为其对应的十六进制格式
	 * 数字引用（&amp;#x<i>Hex</i>;）。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input （未转义的）输入字符串
	 * @return 转义后的字符串
	 */
	public static String htmlEscapeHex(String input) {
		return htmlEscapeHex(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 将特殊字符转换为 HTML 字符引用。
	 * <p>处理 HTML 4.01 建议中定义的完整字符集。
	 * <p>将所有特殊字符至少按指定编码的要求转义为其对应的十六进制格式
	 * 数字引用（&amp;#x<i>Hex</i>;）。换言之，如果某个特殊字符
	 * 在给定编码下不必转义，则可能不会被转义。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input （未转义的）输入字符串
	 * @param encoding 受支持的 {@link java.nio.charset.Charset charset} 名称
	 * @return 转义后的字符串
	 * @since 4.1.2
	 */
	public static String htmlEscapeHex(String input, String encoding) {
		Assert.notNull(input, "Input is required");
		Assert.notNull(encoding, "Encoding is required");
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			if (characterEntityReferences.isMappedToReference(character, encoding)) {
				escaped.append(HtmlCharacterEntityReferences.HEX_REFERENCE_START);
				escaped.append(Integer.toString(character, 16));
				escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
			}
			else {
				escaped.append(character);
			}
		}
		return escaped.toString();
	}

	/**
	 * 将 HTML 字符引用转换为其等效的纯文本 UNICODE。
	 * <p>处理 HTML 4.01 建议中定义的完整字符集
	 * 以及所有引用类型（十进制、十六进制和实体）。
	 * <p>正确转换以下格式：
	 * <blockquote>
	 * &amp;#<i>Entity</i>; - <i>（示例：&amp;amp;）区分大小写</i>
	 * &amp;#<i>Decimal</i>; - <i>（示例：&amp;#68;）</i><br>
	 * &amp;#x<i>Hex</i>; - <i>（示例：&amp;#xE5;）不区分大小写</i><br>
	 * </blockquote>
	 * <p>遇到格式错误的字符引用时，会按原样复制原始字符，
	 * 从而优雅地处理这种情况。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input （已转义的）输入字符串
	 * @return 未转义的字符串
	 */
	public static String htmlUnescape(String input) {
		return new HtmlCharacterEntityDecoder(characterEntityReferences, input).decode();
	}

}
