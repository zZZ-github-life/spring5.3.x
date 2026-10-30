/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link MessageCodesResolver} 接口的默认实现。
 *
 * <p>会为对象错误创建两个消息代码，顺序如下（使用 {@link Format#PREFIX_ERROR_CODE prefixed}
 * {@link #setMessageCodeFormatter(MessageCodeFormatter) formatter} 时）：
 * <ul>
 * <li>1.：code + "." + 对象名
 * <li>2.：code
 * </ul>
 *
 * <p>会为字段规格创建四个消息代码，顺序如下：
 * <ul>
 * <li>1.：code + "." + 对象名 + "." + 字段
 * <li>2.：code + "." + 字段
 * <li>3.：code + "." + 字段类型
 * <li>4.：code
 * </ul>
 *
 * <p>例如，当代码为 "typeMismatch"、对象名为 "user"、字段为 "age" 时：
 * <ul>
 * <li>1. 尝试 "typeMismatch.user.age"
 * <li>2. 尝试 "typeMismatch.age"
 * <li>3. 尝试 "typeMismatch.int"
 * <li>4. 尝试 "typeMismatch"
 * </ul>
 *
 * <p>因此可以利用这一解析算法，例如为 "required" 和 "typeMismatch" 之类的绑定错误显示特定的消息：
 * <ul>
 * <li>在对象 + 字段级别（"age" 字段，但仅限 "user" 对象）；
 * <li>在字段级别（所有 "age" 字段，无论对象名是什么）；
 * <li>或在通用级别（任何对象上的所有字段）。
 * </ul>
 *
 * <p>对于数组、{@link List} 或 {@link java.util.Map} 属性，会同时为具体元素和整个集合生成代码。假
 * 设对象 "user" 中有一个名为 "groups" 的数组字段 "name"：
 * <ul>
 * <li>1. 尝试 "typeMismatch.user.groups[0].name"
 * <li>2. 尝试 "typeMismatch.user.groups.name"
 * <li>3. 尝试 "typeMismatch.groups[0].name"
 * <li>4. 尝试 "typeMismatch.groups.name"
 * <li>5. 尝试 "typeMismatch.name"
 * <li>6. 尝试 "typeMismatch.java.lang.String"
 * <li>7. 尝试 "typeMismatch"
 * </ul>
 *
 * <p>默认情况下，{@code errorCode} 会置于所构造消息字符串的开头。可以使用
 * {@link #setMessageCodeFormatter(MessageCodeFormatter) messageCodeFormatter} 属性来指定另一种拼接
 * {@link MessageCodeFormatter format}。
 *
 * <p>为了在资源包中将所有代码归入某个特定类别，例如使用 "validation.typeMismatch.name" 代替默认的
 * "typeMismatch.name"，可以考虑指定要应用的 {@link #setPrefix prefix}。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Chris Beams
 * @since 1.0.1
 */
@SuppressWarnings("serial")
public class DefaultMessageCodesResolver implements MessageCodesResolver, Serializable {

	/**
	 * 此实现在解析消息代码时使用的分隔符。
	 */
	public static final String CODE_SEPARATOR = ".";

	private static final MessageCodeFormatter DEFAULT_FORMATTER = Format.PREFIX_ERROR_CODE;


	private String prefix = "";

	private MessageCodeFormatter formatter = DEFAULT_FORMATTER;


	/**
	 * 指定要应用于此解析器所构建的任何代码的前缀。
	 *
	 * <p>默认无前缀。例如，指定 "validation."，即可得到 "validation.typeMismatch.name" 之类的错误代码。
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回要应用于此解析器所构建的任何代码的前缀。
	 *
	 * <p>无前缀时返回空字符串。
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 指定此解析器所构建消息代码的格式。
	 *
	 * <p>默认为 {@link Format#PREFIX_ERROR_CODE}。
	 * @since 3.2
	 * @see Format
	 */
	public void setMessageCodeFormatter(@Nullable MessageCodeFormatter formatter) {
		this.formatter = (formatter != null ? formatter : DEFAULT_FORMATTER);
	}


	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName) {
		return resolveMessageCodes(errorCode, objectName, "", null);
	}

	/**
	 * 为给定的代码和字段构建代码列表：一个对象/字段特定的代码、一个字段特定的代码、一个普通错误代码。
	 *
	 * <p>数组、List 和 Map 会同时针对具体元素和整个集合进行解析。
	 *
	 * <p>有关所生成代码的详情，请参见 {@link DefaultMessageCodesResolver class level javadoc}。
	 * @return 代码列表。
	 */
	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName, String field, @Nullable Class<?> fieldType) {
		Set<String> codeList = new LinkedHashSet<>();
		List<String> fieldList = new ArrayList<>();
		buildFieldList(field, fieldList);
		addCodes(codeList, errorCode, objectName, fieldList);
		int dotIndex = field.lastIndexOf('.');
		if (dotIndex != -1) {
			buildFieldList(field.substring(dotIndex + 1), fieldList);
		}
		addCodes(codeList, errorCode, null, fieldList);
		if (fieldType != null) {
			addCode(codeList, errorCode, null, fieldType.getName());
		}
		addCode(codeList, errorCode, null, null);
		return StringUtils.toStringArray(codeList);
	}

	private void addCodes(Collection<String> codeList, String errorCode, @Nullable String objectName, Iterable<String> fields) {
		for (String field : fields) {
			addCode(codeList, errorCode, objectName, field);
		}
	}

	private void addCode(Collection<String> codeList, String errorCode, @Nullable String objectName, @Nullable String field) {
		codeList.add(postProcessMessageCode(this.formatter.format(errorCode, objectName, field)));
	}

	/**
	 * 将所提供 {@code field} 的带键和不带键条目都添加到所提供的字段列表中。
	 */
	protected void buildFieldList(String field, List<String> fieldList) {
		fieldList.add(field);
		String plainField = field;
		int keyIndex = plainField.lastIndexOf('[');
		while (keyIndex != -1) {
			int endKeyIndex = plainField.indexOf(']', keyIndex);
			if (endKeyIndex != -1) {
				plainField = plainField.substring(0, keyIndex) + plainField.substring(endKeyIndex + 1);
				fieldList.add(plainField);
				keyIndex = plainField.lastIndexOf('[');
			}
			else {
				keyIndex = -1;
			}
		}
	}

	/**
	 * 对由此解析器构建的给定消息代码进行后处理。
	 *
	 * <p>默认实现会应用指定的前缀（如果有）。
	 * @param code 由该解析器构建的消息代码。
	 * @return 最终返回的消息代码。
	 * @see #setPrefix
	 */
	protected String postProcessMessageCode(String code) {
		return getPrefix() + code;
	}


	/**
	 * 常见的消息代码格式。
	 * @see MessageCodeFormatter
	 * @see DefaultMessageCodesResolver#setMessageCodeFormatter(MessageCodeFormatter)
	 */
	public enum Format implements MessageCodeFormatter {

		/**
		 * 将错误代码加在生成的消息代码开头。例如：
		 * {@code errorCode + "." + object name + "." + field}
		 */
		PREFIX_ERROR_CODE {
			@Override
			public String format(String errorCode, @Nullable String objectName, @Nullable String field) {
				return toDelimitedString(errorCode, objectName, field);
			}
		},

		/**
		 * 将错误代码加在生成的消息代码末尾。例如：
		 * {@code object name + "." + field + "." + errorCode}
		 */
		POSTFIX_ERROR_CODE {
			@Override
			public String format(String errorCode, @Nullable String objectName, @Nullable String field) {
				return toDelimitedString(objectName, field, errorCode);
			}
		};

		/**
		 * 使用 {@link DefaultMessageCodesResolver#CODE_SEPARATOR} 作为分隔符拼接给定元素，完全跳过长度为零
		 * 或为 null 的元素。
		 */
		public static String toDelimitedString(String... elements) {
			StringJoiner rtn = new StringJoiner(CODE_SEPARATOR);
			for (String element : elements) {
				if (StringUtils.hasLength(element)) {
					rtn.add(element);
				}
			}
			return rtn.toString();
		}
	}

}
