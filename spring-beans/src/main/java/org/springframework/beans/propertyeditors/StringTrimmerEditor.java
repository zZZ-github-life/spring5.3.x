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
import org.springframework.util.StringUtils;

/**
 * 用于去除 String 首尾空白的属性编辑器。
 *
 * <p>可选择将空字符串转换为 {@code null} 值。需要显式注册，例如用于命令绑定。
 *
 * @author Juergen Hoeller
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class StringTrimmerEditor extends PropertyEditorSupport {

	@Nullable
	private final String charsToDelete;

	private final boolean emptyAsNull;


	/**
	 * 创建一个新的 StringTrimmerEditor。
	 * @param emptyAsNull 如果要将空 String 转换为 {@code null}，
	 * 则为 {@code true}
	 */
	public StringTrimmerEditor(boolean emptyAsNull) {
		this.charsToDelete = null;
		this.emptyAsNull = emptyAsNull;
	}

	/**
	 * 创建一个新的 StringTrimmerEditor。
	 * @param charsToDelete 除去除输入 String 的首尾空白外还要删除的一组字符。
	 * 适用于删除不需要的换行符：
	 * 例如 "\r\n\f" 会删除 String 中的所有换行符和换页符。
	 * @param emptyAsNull 如果要将空 String 转换为 {@code null}，
	 * 则为 {@code true}
	 */
	public StringTrimmerEditor(String charsToDelete, boolean emptyAsNull) {
		this.charsToDelete = charsToDelete;
		this.emptyAsNull = emptyAsNull;
	}


	@Override
	public void setAsText(@Nullable String text) {
		if (text == null) {
			setValue(null);
		}
		else {
			String value = text.trim();
			if (this.charsToDelete != null) {
				value = StringUtils.deleteAny(value, this.charsToDelete);
			}
			if (this.emptyAsNull && value.isEmpty()) {
				setValue(null);
			}
			else {
				setValue(value);
			}
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}

}
