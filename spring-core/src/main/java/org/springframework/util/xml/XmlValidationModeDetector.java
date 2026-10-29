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

package org.springframework.util.xml;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 检测 XML 流使用的是基于 DTD 还是基于 XSD 的验证。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 */
public class XmlValidationModeDetector {

	/**
	 * 表示应禁用验证。
	 */
	public static final int VALIDATION_NONE = 0;

	/**
	 * 表示应自动推测验证模式，因为无法找到
	 * 明确的指示（可能是被某些特殊字符干扰了，或类似情况）。
	 */
	public static final int VALIDATION_AUTO = 1;

	/**
	 * 表示应使用 DTD 验证（我们找到了 "DOCTYPE" 声明）。
	 */
	public static final int VALIDATION_DTD = 2;

	/**
	 * 表示应使用 XSD 验证（未找到 "DOCTYPE" 声明）。
	 */
	public static final int VALIDATION_XSD = 3;


	/**
	 * XML 文档中用于声明验证所用 DTD 的标记，
	 * 因此表示正在使用 DTD 验证。
	 */
	private static final String DOCTYPE = "DOCTYPE";

	/**
	 * 表示 XML 注释开始的标记。
	 */
	private static final String START_COMMENT = "<!--";

	/**
	 * 表示 XML 注释结束的标记。
	 */
	private static final String END_COMMENT = "-->";


	/**
	 * 指示当前解析位置是否位于 XML 注释内。
	 */
	private boolean inComment;


	/**
	 * 检测提供的 {@link InputStream} 中 XML 文档的验证模式。
	 * <p>请注意，此方法在返回前会关闭提供的 {@link InputStream}。
	 * @param inputStream the InputStream to parse
	 * @throws IOException in case of I/O failure
	 * @see #VALIDATION_DTD
	 * @see #VALIDATION_XSD
	 */
	public int detectValidationMode(InputStream inputStream) throws IOException {
		this.inComment = false;

		// Peek into the file to look for DOCTYPE.
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			boolean isDtdValidated = false;
			String content;
			while ((content = reader.readLine()) != null) {
				content = consumeCommentTokens(content);
				if (!StringUtils.hasText(content)) {
					continue;
				}
				if (hasDoctype(content)) {
					isDtdValidated = true;
					break;
				}
				if (hasOpeningTag(content)) {
					// End of meaningful data...
					break;
				}
			}
			return (isDtdValidated ? VALIDATION_DTD : VALIDATION_XSD);
		}
		catch (CharConversionException ex) {
			// Choked on some character encoding...
			// Leave the decision up to the caller.
			return VALIDATION_AUTO;
		}
	}


	/**
	 * 内容是否包含 DTD DOCTYPE 声明？
	 */
	private boolean hasDoctype(String content) {
		return content.contains(DOCTYPE);
	}

	/**
	 * 确定提供的内容是否包含 XML 开始标签。
	 * <p>预期在将剩余内容传给此方法之前，提供的内容中所有注释标记
	 * 都已被消费。不过，作为一项检查，如果当前的解析状态
	 * 位于 XML 注释中，此方法始终返回 {@code false}。
	 */
	private boolean hasOpeningTag(String content) {
		if (this.inComment) {
			return false;
		}
		int openTagIndex = content.indexOf('<');
		return (openTagIndex > -1 && (content.length() > openTagIndex + 1) &&
				Character.isLetter(content.charAt(openTagIndex + 1)));
	}

	/**
	 * 消费给定 String 中的所有注释并返回剩余内容，
	 * 该内容可能为空，因为提供的内容可能全是注释数据。
	 * <p>此方法会考虑当前的 "in comment" 解析状态。
	 */
	private String consumeCommentTokens(String line) {
		int indexOfStartComment = line.indexOf(START_COMMENT);
		if (indexOfStartComment == -1 && !line.contains(END_COMMENT)) {
			return line;
		}

		String result = "";
		String currLine = line;
		if (!this.inComment && (indexOfStartComment >= 0)) {
			result = line.substring(0, indexOfStartComment);
			currLine = line.substring(indexOfStartComment);
		}

		if ((currLine = consume(currLine)) != null) {
			result += consumeCommentTokens(currLine);
		}
		return result;
	}

	/**
	 * 消费下一个注释标记，更新 "inComment" 标志，
	 * 并返回剩余内容。
	 */
	@Nullable
	private String consume(String line) {
		int index = (this.inComment ? endComment(line) : startComment(line));
		return (index == -1 ? null : line.substring(index));
	}

	/**
	 * 尝试消费 {@link #START_COMMENT} 标记。
	 * @see #commentToken(String, String, boolean)
	 */
	private int startComment(String line) {
		return commentToken(line, START_COMMENT, true);
	}

	/**
	 * 尝试消费 {@link #END_COMMENT} 标记。
	 * @see #commentToken(String, String, boolean)
	 */
	private int endComment(String line) {
		return commentToken(line, END_COMMENT, false);
	}

	/**
	 * 尝试根据提供的内容消费提供的标记，并将
	 * "in comment" 解析状态更新为提供的值。
	 * <p>返回内容中该标记之后的索引，如果未找到
	 * 该标记则返回 -1。
	 */
	private int commentToken(String line, String token, boolean inCommentIfPresent) {
		int index = line.indexOf(token);
		if (index > - 1) {
			this.inComment = inCommentIfPresent;
		}
		return (index == -1 ? index : index + token.length());
	}

}
