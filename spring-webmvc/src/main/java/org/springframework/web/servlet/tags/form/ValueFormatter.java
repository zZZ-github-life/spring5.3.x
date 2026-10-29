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

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * 包级可见的辅助类，用于格式化通过表单标签渲染的值。支持两种格式化风格：普通格式化和 {@link PropertyEditor} 感知格式化。<p>普通格式化仅防止字符串 '{@code null}' 出现，将其替换为空 String，并根据需要添加 HTML 转义。<p>{@link PropertyEditor} 感知格式化会先尝试使用给定的 {@link PropertyEditor} 渲染任何非 String 值，然后再应用普通格式化的默认规则。
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
abstract class ValueFormatter {

	/**
	 * 构建给定 {@code Object} 的显示值，并按需进行 HTML 转义。此版本<strong>不</strong>感知 {@link PropertyEditor}。
	 * @see #getDisplayString(Object, java.beans.PropertyEditor, boolean)
	 */
	public static String getDisplayString(@Nullable Object value, boolean htmlEscape) {
		String displayValue = ObjectUtils.getDisplayString(value);
		return (htmlEscape ? HtmlUtils.htmlEscape(displayValue) : displayValue);
	}

	/**
	 * 构建给定 {@code Object} 的显示值，并按需进行 HTML 转义。如果给定值不是 {@link String} 且给定的 {@link PropertyEditor} 不为 null，则使用 {@link PropertyEditor} 获取显示值。
	 * @see #getDisplayString(Object, boolean)
	 */
	public static String getDisplayString(
			@Nullable Object value, @Nullable PropertyEditor propertyEditor, boolean htmlEscape) {

		if (propertyEditor != null && !(value instanceof String)) {
			try {
				propertyEditor.setValue(value);
				String text = propertyEditor.getAsText();
				if (text != null) {
					return getDisplayString(text, htmlEscape);
				}
			}
			catch (Throwable ex) {
				// The PropertyEditor might not support this value... pass through.
			}
		}
		return getDisplayString(value, htmlEscape);
	}

}
