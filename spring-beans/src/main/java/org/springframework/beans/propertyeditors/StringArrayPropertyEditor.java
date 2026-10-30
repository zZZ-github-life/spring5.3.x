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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于 String 数组的自定义 {@link java.beans.PropertyEditor}。
 *
 * <p>字符串必须采用 CSV 格式，分隔符可自定义。默认情况下，结果中的值会被去除首尾空白。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Dave Syer
 * @see org.springframework.util.StringUtils#delimitedListToStringArray
 * @see org.springframework.util.StringUtils#arrayToDelimitedString
 */
public class StringArrayPropertyEditor extends PropertyEditorSupport {

	/**
	 * 用于拆分 String 的默认分隔符：逗号（","）。
	 */
	public static final String DEFAULT_SEPARATOR = ",";


	private final String separator;

	@Nullable
	private final String charsToDelete;

	private final boolean emptyArrayAsNull;

	private final boolean trimValues;


	/**
	 * 使用默认分隔符（逗号）创建一个新的 {@code StringArrayPropertyEditor}。
	 * <p>空文本（不含元素）将被转换为空数组。
	 */
	public StringArrayPropertyEditor() {
		this(DEFAULT_SEPARATOR, null, false);
	}

	/**
	 * 使用给定的分隔符创建一个新的 {@code StringArrayPropertyEditor}。
	 * <p>空文本（不含元素）将被转换为空数组。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 */
	public StringArrayPropertyEditor(String separator) {
		this(separator, null, false);
	}

	/**
	 * 使用给定的分隔符创建一个新的 {@code StringArrayPropertyEditor}。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 * @param emptyArrayAsNull 如果要将空 String 数组转换为 {@code null}，
	 * 则为 {@code true}
	 */
	public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull) {
		this(separator, null, emptyArrayAsNull);
	}

	/**
	 * 使用给定的分隔符创建一个新的 {@code StringArrayPropertyEditor}。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 * @param emptyArrayAsNull 如果要将空 String 数组转换为 {@code null}，
	 * 则为 {@code true}
	 * @param trimValues 如果要去除所解析数组中值的空白，则为 {@code true}
	 * （默认为 {@code true}）
	 */
	public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull, boolean trimValues) {
		this(separator, null, emptyArrayAsNull, trimValues);
	}

	/**
	 * 使用给定的分隔符创建一个新的 {@code StringArrayPropertyEditor}。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 * @param charsToDelete 除去除输入 String 的首尾空白外还要删除的一组字符。
	 * 适用于删除不需要的换行符：
	 * 例如 "\r\n\f" 会删除 String 中的所有换行符和换页符。
	 * @param emptyArrayAsNull 如果要将空 String 数组转换为 {@code null}，
	 * 则为 {@code true}
	 */
	public StringArrayPropertyEditor(String separator, @Nullable String charsToDelete, boolean emptyArrayAsNull) {
		this(separator, charsToDelete, emptyArrayAsNull, true);
	}

	/**
	 * 使用给定的分隔符创建一个新的 {@code StringArrayPropertyEditor}。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 * @param charsToDelete 除去除输入 String 的首尾空白外还要删除的一组字符。
	 * 适用于删除不需要的换行符：
	 * 例如 "\r\n\f" 会删除 String 中的所有换行符和换页符。
	 * @param emptyArrayAsNull 如果要将空 String 数组转换为 {@code null}，
	 * 则为 {@code true}
	 * @param trimValues 如果要去除所解析数组中值的空白，则为 {@code true}
	 * （默认为 {@code true}）
	 */
	public StringArrayPropertyEditor(
			String separator, @Nullable String charsToDelete, boolean emptyArrayAsNull, boolean trimValues) {

		this.separator = separator;
		this.charsToDelete = charsToDelete;
		this.emptyArrayAsNull = emptyArrayAsNull;
		this.trimValues = trimValues;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String[] array = StringUtils.delimitedListToStringArray(text, this.separator, this.charsToDelete);
		if (this.emptyArrayAsNull && array.length == 0) {
			setValue(null);
		}
		else {
			if (this.trimValues) {
				array = StringUtils.trimArrayElements(array);
			}
			setValue(array);
		}
	}

	@Override
	public String getAsText() {
		return StringUtils.arrayToDelimitedString(ObjectUtils.toObjectArray(getValue()), this.separator);
	}

}
