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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 用于 {@link Character} 的编辑器，可从 String 值填充 {@code Character} 或 {@code char} 类型的属性。
 *
 * <p>请注意，JDK 并未为 {@code char} 提供默认的 {@link java.beans.PropertyEditor property editor}！
 * {@link org.springframework.beans.BeanWrapperImpl} 将默认注册此编辑器。
 *
 * <p>还支持从 Unicode 字符序列进行转换；例如 {@code u0041} ('A')。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 * @since 1.2
 * @see Character
 * @see org.springframework.beans.BeanWrapperImpl
 */
public class CharacterEditor extends PropertyEditorSupport {

	/**
	 * 用于将字符串标识为 Unicode 字符序列的前缀。
	 */
	private static final String UNICODE_PREFIX = "\\u";

	/**
	 * Unicode 字符序列的长度。
	 */
	private static final int UNICODE_LENGTH = 6;


	private final boolean allowEmpty;


	/**
	 * 创建一个新的 CharacterEditor 实例。<p>"allowEmpty" 参数控制解析时是否允许空 String，即在
	 * {@link #setAsText(String) text is being converted}时是否将其解释为 {@code null} 值。如果为
	 * {@code false}，此时将抛出 {@link IllegalArgumentException}。
	 * @param allowEmpty if empty strings are to be allowed
	 */
	public CharacterEditor(boolean allowEmpty) {
		this.allowEmpty = allowEmpty;
	}


	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasLength(text)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (text == null) {
			throw new IllegalArgumentException("null String cannot be converted to char type");
		}
		else if (isUnicodeCharacterSequence(text)) {
			setAsUnicode(text);
		}
		else if (text.length() == 1) {
			setValue(text.charAt(0));
		}
		else {
			throw new IllegalArgumentException("String [" + text + "] with length " +
					text.length() + " cannot be converted to char type: neither Unicode nor single character");
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}


	private boolean isUnicodeCharacterSequence(String sequence) {
		return (sequence.startsWith(UNICODE_PREFIX) && sequence.length() == UNICODE_LENGTH);
	}

	private void setAsUnicode(String text) {
		int code = Integer.parseInt(text.substring(UNICODE_PREFIX.length()), 16);
		setValue((char) code);
	}

}
