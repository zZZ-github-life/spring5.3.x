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
import java.text.NumberFormat;

import org.springframework.lang.Nullable;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

/**
 * 用于任何 Number 子类的属性编辑器，例如 Short、Integer、Long、BigInteger、Float、Double、
 * BigDecimal。可以使用给定的 NumberFormat 进行（区域特定的）解析和渲染，或者使用默认的
 * {@code decode} / {@code valueOf} / {@code toString} 方法。
 *
 * <p>它并非用作系统 PropertyEditor，而是在自定义控制器代码中用作区域特定的数字编辑器，用于将用户输
 * 入的数字字符串解析为 Bean 的 Number 属性，并在 UI 表单中渲染它们。
 *
 * <p>在 Web MVC 代码中，通常会通过 {@code binder.registerCustomEditor} 调用注册此编辑器。
 *
 * @author Juergen Hoeller
 * @since 06.06.2003
 * @see Number
 * @see java.text.NumberFormat
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class CustomNumberEditor extends PropertyEditorSupport {

	private final Class<? extends Number> numberClass;

	@Nullable
	private final NumberFormat numberFormat;

	private final boolean allowEmpty;


	/**
	 * 使用默认的 {@code valueOf} 方法进行解析、使用 {@code toString} 方法进行渲染，创建新的
	 * CustomNumberEditor 实例。<p>"allowEmpty" 参数表明解析时是否应允许空 String，即将其解释为
	 * {@code null} 值。否则，在这种情况下将抛出 IllegalArgumentException。
	 * @param numberClass the Number subclass to generate
	 * @param allowEmpty if empty strings should be allowed
	 * @throws IllegalArgumentException if an invalid numberClass has been specified
	 * @see org.springframework.util.NumberUtils#parseNumber(String, Class)
	 * @see Integer#valueOf
	 * @see Integer#toString
	 */
	public CustomNumberEditor(Class<? extends Number> numberClass, boolean allowEmpty) throws IllegalArgumentException {
		this(numberClass, null, allowEmpty);
	}

	/**
	 * 使用给定的 NumberFormat 进行解析和渲染，创建新的 CustomNumberEditor 实例。<p>allowEmpty 参数表明
	 * 解析时是否应允许空 String，即将其解释为 {@code null} 值。否则，在这种情况下将抛出
	 * IllegalArgumentException。
	 * @param numberClass the Number subclass to generate
	 * @param numberFormat the NumberFormat to use for parsing and rendering
	 * @param allowEmpty if empty strings should be allowed
	 * @throws IllegalArgumentException if an invalid numberClass has been specified
	 * @see org.springframework.util.NumberUtils#parseNumber(String, Class, java.text.NumberFormat)
	 * @see java.text.NumberFormat#parse
	 * @see java.text.NumberFormat#format
	 */
	public CustomNumberEditor(Class<? extends Number> numberClass,
			@Nullable NumberFormat numberFormat, boolean allowEmpty) throws IllegalArgumentException {

		if (!Number.class.isAssignableFrom(numberClass)) {
			throw new IllegalArgumentException("Property class must be a subclass of Number");
		}
		this.numberClass = numberClass;
		this.numberFormat = numberFormat;
		this.allowEmpty = allowEmpty;
	}


	/**
	 * 使用指定的 NumberFormat 从给定文本中解析 Number。
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (this.numberFormat != null) {
			// Use given NumberFormat for parsing text.
			setValue(NumberUtils.parseNumber(text, this.numberClass, this.numberFormat));
		}
		else {
			// Use default valueOf methods for parsing text.
			setValue(NumberUtils.parseNumber(text, this.numberClass));
		}
	}

	/**
	 * 如有必要，将 Number 值强制转换为所需的目标类。
	 */
	@Override
	public void setValue(@Nullable Object value) {
		if (value instanceof Number) {
			super.setValue(NumberUtils.convertNumberToTargetClass((Number) value, this.numberClass));
		}
		else {
			super.setValue(value);
		}
	}

	/**
	 * 使用指定的 NumberFormat 将 Number 格式化为 String。
	 */
	@Override
	public String getAsText() {
		Object value = getValue();
		if (value == null) {
			return "";
		}
		if (this.numberFormat != null) {
			// Use NumberFormat for rendering value.
			return this.numberFormat.format(value);
		}
		else {
			// Use toString method for rendering value.
			return value.toString();
		}
	}

}
