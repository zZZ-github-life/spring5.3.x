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

package org.springframework.util.xml;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import org.springframework.util.Assert;

/**
 * 包含与 {@link javax.xml.transform.Transformer Transformers}
 * 以及 {@code javax.xml.transform} 包相关的通用行为。
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.5.5
 */
public abstract class TransformerUtils {

	/**
	 * 启用缩进时缩进字符的数量。
	 * <p>默认为 "2"。
	 */
	public static final int DEFAULT_INDENT_AMOUNT = 2;


	/**
	 * 为提供的 {@link javax.xml.transform.Transformer} 启用缩进。
	 * <p>如果底层 XSLT 引擎是 Xalan，则特殊输出键 {@code indent-amount}
	 * 也将设置为 {@link #DEFAULT_INDENT_AMOUNT} 个字符的值。
	 * @param transformer 目标转换器
	 * @see javax.xml.transform.Transformer#setOutputProperty(String, String)
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public static void enableIndenting(Transformer transformer) {
		enableIndenting(transformer, DEFAULT_INDENT_AMOUNT);
	}

	/**
	 * 为提供的 {@link javax.xml.transform.Transformer} 启用缩进。
	 * <p>如果底层 XSLT 引擎是 Xalan，则特殊输出键 {@code indent-amount}
	 * 也将设置为 {@link #DEFAULT_INDENT_AMOUNT} 个字符的值。
	 * @param transformer 目标转换器
	 * @param indentAmount 缩进的大小（2 个字符、3 个字符等）
	 * @see javax.xml.transform.Transformer#setOutputProperty(String, String)
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public static void enableIndenting(Transformer transformer, int indentAmount) {
		Assert.notNull(transformer, "Transformer must not be null");
		if (indentAmount < 0) {
			throw new IllegalArgumentException("Invalid indent amount (must not be less than zero): " + indentAmount);
		}
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		try {
			// Xalan-specific, but this is the most common XSLT engine in any case
			transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", String.valueOf(indentAmount));
		}
		catch (IllegalArgumentException ignored) {
		}
	}

	/**
	 * 为提供的 {@link javax.xml.transform.Transformer} 禁用缩进。
	 * @param transformer 目标转换器
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public static void disableIndenting(Transformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
	}

}
