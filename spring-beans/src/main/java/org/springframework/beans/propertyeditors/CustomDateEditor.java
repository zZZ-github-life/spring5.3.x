/*
 * Copyright 2002-2018 the original author or authors.
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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 用于 {@code java.util.Date} 的属性编辑器，支持自定义的 {@code java.text.DateFormat}。
 *
 * <p>它并非用作系统 PropertyEditor，而是在自定义控制器代码中用作区域特定的日期编辑器，用于将用户输
 * 入的数字字符串解析为 Bean 的 Date 属性，并在 UI 表单中渲染它们。
 *
 * <p>在 Web MVC 代码中，通常会通过 {@code binder.registerCustomEditor} 注册此编辑器。
 *
 * @author Juergen Hoeller
 * @since 28.04.2003
 * @see java.util.Date
 * @see java.text.DateFormat
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class CustomDateEditor extends PropertyEditorSupport {

	private final DateFormat dateFormat;

	private final boolean allowEmpty;

	private final int exactDateLength;


	/**
	 * 使用给定的 DateFormat 进行解析和渲染，创建新的 CustomDateEditor 实例。<p>"allowEmpty" 参数表明解
	 * 析时是否应允许空 String，即将其解释为 null 值。否则，在这种情况下将抛出 IllegalArgumentException。
	 * @param dateFormat 用于解析和渲染的 DateFormat
	 * @param allowEmpty 是否应允许空字符串
	 */
	public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty) {
		this.dateFormat = dateFormat;
		this.allowEmpty = allowEmpty;
		this.exactDateLength = -1;
	}

	/**
	 * 使用给定的 DateFormat 进行解析和渲染，创建新的 CustomDateEditor 实例。<p>"allowEmpty" 参数表明解
	 * 析时是否应允许空 String，即将其解释为 null 值。否则，在这种情况下将抛出 IllegalArgumentException。
	 * <p>"exactDateLength" 参数表明，如果 String 的长度与指定的长度不完全匹配，将抛出
	 * IllegalArgumentException。这很有用，因为 SimpleDateFormat 不会对年份部分强制执行严格解析，即使使
	 * 用 {@code setLenient(false)} 也是如此。如果未指定 "exactDateLength"，"01/01/05" 会被解析为
	 * "01/01/0005"。然而，即使指定了 "exactDateLength"，日期或月份部分的前导零仍可能允许较短的年份部分，
	 * 因此请将其视为只是让您更接近预期日期格式的又一项断言。
	 * @param dateFormat 用于解析和渲染的 DateFormat
	 * @param allowEmpty 是否应允许空字符串
	 * @param exactDateLength 日期字符串的精确期望长度
	 */
	public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty, int exactDateLength) {
		this.dateFormat = dateFormat;
		this.allowEmpty = allowEmpty;
		this.exactDateLength = exactDateLength;
	}


	/**
	 * 使用指定的 DateFormat 从给定文本中解析 Date。
	 */
	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (text != null && this.exactDateLength >= 0 && text.length() != this.exactDateLength) {
			throw new IllegalArgumentException(
					"Could not parse date: it is not exactly" + this.exactDateLength + "characters long");
		}
		else {
			try {
				setValue(this.dateFormat.parse(text));
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException("Could not parse date: " + ex.getMessage(), ex);
			}
		}
	}

	/**
	 * 使用指定的 DateFormat 将 Date 格式化为 String。
	 */
	@Override
	public String getAsText() {
		Date value = (Date) getValue();
		return (value != null ? this.dateFormat.format(value) : "");
	}

}
