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

package org.springframework.core.style;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于构建美观输出的 {@code toString()} 方法的工具类，支持可插拔的样式约定。默认情况下，
 * ToStringCreator 遵循 Spring 的 {@code toString()} 样式约定。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public class ToStringCreator {

	/**
	 * 此 ToStringCreator 所使用的默认 ToStringStyler 实例。
	 */
	private static final ToStringStyler DEFAULT_TO_STRING_STYLER =
			new DefaultToStringStyler(StylerUtils.DEFAULT_VALUE_STYLER);


	private final StringBuilder buffer = new StringBuilder(256);

	private final ToStringStyler styler;

	private final Object object;

	private boolean styledFirstField;


	/**
	 * 为给定的对象创建一个 ToStringCreator。
	 * @param obj the object to be stringified
	 */
	public ToStringCreator(Object obj) {
		this(obj, (ToStringStyler) null);
	}

	/**
	 * 为给定的对象创建一个 ToStringCreator，并使用所提供的样式。
	 * @param obj the object to be stringified
	 * @param styler the ValueStyler encapsulating pretty-print instructions
	 */
	public ToStringCreator(Object obj, @Nullable ValueStyler styler) {
		this(obj, new DefaultToStringStyler(styler != null ? styler : StylerUtils.DEFAULT_VALUE_STYLER));
	}

	/**
	 * 为给定的对象创建一个 ToStringCreator，并使用所提供的样式。
	 * @param obj the object to be stringified
	 * @param styler the ToStringStyler encapsulating pretty-print instructions
	 */
	public ToStringCreator(Object obj, @Nullable ToStringStyler styler) {
		Assert.notNull(obj, "The object to be styled must not be null");
		this.object = obj;
		this.styler = (styler != null ? styler : DEFAULT_TO_STRING_STYLER);
		this.styler.styleStart(this.buffer, this.object);
	}


	/**
	 * 追加一个 byte 字段值。
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, byte value) {
		return append(fieldName, Byte.valueOf(value));
	}

	/**
	 * 追加一个 short 字段值。
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, short value) {
		return append(fieldName, Short.valueOf(value));
	}

	/**
	 * 追加一个 int 字段值。
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, int value) {
		return append(fieldName, Integer.valueOf(value));
	}

	/**
	 * 追加一个 long 字段值。
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, long value) {
		return append(fieldName, Long.valueOf(value));
	}

	/**
	 * 追加一个 float 字段值。
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, float value) {
		return append(fieldName, Float.valueOf(value));
	}

	/**
	 * 追加一个 double 字段值。
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, double value) {
		return append(fieldName, Double.valueOf(value));
	}

	/**
	 * 追加一个 boolean 字段值。
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, boolean value) {
		return append(fieldName, Boolean.valueOf(value));
	}

	/**
	 * 追加一个字段值。
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, @Nullable Object value) {
		printFieldSeparatorIfNecessary();
		this.styler.styleField(this.buffer, fieldName, value);
		return this;
	}

	private void printFieldSeparatorIfNecessary() {
		if (this.styledFirstField) {
			this.styler.styleFieldSeparator(this.buffer);
		}
		else {
			this.styledFirstField = true;
		}
	}

	/**
	 * 追加所提供的值。
	 * @param value the value to append
	 * @return this, to support call-chaining.
	 */
	public ToStringCreator append(Object value) {
		this.styler.styleValue(this.buffer, value);
		return this;
	}


	/**
	 * 返回此 ToStringCreator 构建的 String 表示形式。
	 */
	@Override
	public String toString() {
		this.styler.styleEnd(this.buffer, this.object);
		return this.buffer.toString();
	}

}
