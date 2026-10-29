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

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 用于 Boolean/boolean 属性的属性编辑器。
 *
 * <p>它并非用作系统 PropertyEditor，而是在自定义控制器代码中用作区域特定的 Boolean 编辑器，用于将
 * UI 产生的布尔字符串解析为 Bean 的 boolean 属性，并在 UI 表单中校验它们。
 *
 * <p>在 Web MVC 代码中，通常会通过 {@code binder.registerCustomEditor} 调用注册此编辑器。
 *
 * @author Juergen Hoeller
 * @since 10.06.2003
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class CustomBooleanEditor extends PropertyEditorSupport {

	/**
	 * 值为 {@code "true"}。
	 */
	public static final String VALUE_TRUE = "true";

	/**
	 * 值为 {@code "false"}。
	 */
	public static final String VALUE_FALSE = "false";

	/**
	 * 值为 {@code "on"}。
	 */
	public static final String VALUE_ON = "on";

	/**
	 * 值为 {@code "off"}。
	 */
	public static final String VALUE_OFF = "off";

	/**
	 * 值为 {@code "yes"}。
	 */
	public static final String VALUE_YES = "yes";

	/**
	 * 值为 {@code "no"}。
	 */
	public static final String VALUE_NO = "no";

	/**
	 * 值为 {@code "1"}。
	 */
	public static final String VALUE_1 = "1";

	/**
	 * 值为 {@code "0"}。
	 */
	public static final String VALUE_0 = "0";


	@Nullable
	private final String trueString;

	@Nullable
	private final String falseString;

	private final boolean allowEmpty;


	/**
	 * 创建一个新的 CustomBooleanEditor 实例，将 "true"/"on"/"yes" 和 "false"/"off"/"no" 作为可识别的
	 * String 值。<p>"allowEmpty" 参数表明解析时是否应允许空 String，即将其解释为 null 值。否则，在这种
	 * 情况下将抛出 IllegalArgumentException。
	 * @param allowEmpty if empty strings should be allowed
	 */
	public CustomBooleanEditor(boolean allowEmpty) {
		this(null, null, allowEmpty);
	}

	/**
	 * 创建一个新的 CustomBooleanEditor 实例，可为 true 和 false 配置 String 值。<p>"allowEmpty" 参数表
	 * 明解析时是否应允许空 String，即将其解释为 null 值。否则，在这种情况下将抛出
	 * IllegalArgumentException。
	 * @param trueString the String value that represents true:
	 * for example, "true" (VALUE_TRUE), "on" (VALUE_ON),
	 * "yes" (VALUE_YES) or some custom value
	 * @param falseString the String value that represents false:
	 * for example, "false" (VALUE_FALSE), "off" (VALUE_OFF),
	 * "no" (VALUE_NO) or some custom value
	 * @param allowEmpty if empty strings should be allowed
	 * @see #VALUE_TRUE
	 * @see #VALUE_FALSE
	 * @see #VALUE_ON
	 * @see #VALUE_OFF
	 * @see #VALUE_YES
	 * @see #VALUE_NO
	 */
	public CustomBooleanEditor(@Nullable String trueString, @Nullable String falseString, boolean allowEmpty) {
		this.trueString = trueString;
		this.falseString = falseString;
		this.allowEmpty = allowEmpty;
	}


	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		String input = (text != null ? text.trim() : null);
		if (this.allowEmpty && !StringUtils.hasLength(input)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (this.trueString != null && this.trueString.equalsIgnoreCase(input)) {
			setValue(Boolean.TRUE);
		}
		else if (this.falseString != null && this.falseString.equalsIgnoreCase(input)) {
			setValue(Boolean.FALSE);
		}
		else if (this.trueString == null &&
				(VALUE_TRUE.equalsIgnoreCase(input) || VALUE_ON.equalsIgnoreCase(input) ||
						VALUE_YES.equalsIgnoreCase(input) || VALUE_1.equals(input))) {
			setValue(Boolean.TRUE);
		}
		else if (this.falseString == null &&
				(VALUE_FALSE.equalsIgnoreCase(input) || VALUE_OFF.equalsIgnoreCase(input) ||
						VALUE_NO.equalsIgnoreCase(input) || VALUE_0.equals(input))) {
			setValue(Boolean.FALSE);
		}
		else {
			throw new IllegalArgumentException("Invalid boolean value [" + text + "]");
		}
	}

	@Override
	public String getAsText() {
		if (Boolean.TRUE.equals(getValue())) {
			return (this.trueString != null ? this.trueString : VALUE_TRUE);
		}
		else if (Boolean.FALSE.equals(getValue())) {
			return (this.falseString != null ? this.falseString : VALUE_FALSE);
		}
		else {
			return "";
		}
	}

}
