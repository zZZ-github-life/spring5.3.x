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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.nio.charset.Charset;

import org.springframework.util.StringUtils;

/**
 * 用于 {@code java.nio.charset.Charset} 的编辑器，可在字符集 String 表示与 Charset 对象之间相互转
 * 换。
 *
 * <p>期望使用与 Charset 的 {@link java.nio.charset.Charset#name()} 相同的语法，例如 {@code UTF-8}、
 * {@code ISO-8859-16} 等。
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 2.5.4
 * @see Charset
 */
public class CharsetEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			setValue(Charset.forName(text.trim()));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Charset value = (Charset) getValue();
		return (value != null ? value.name() : "");
	}

}
