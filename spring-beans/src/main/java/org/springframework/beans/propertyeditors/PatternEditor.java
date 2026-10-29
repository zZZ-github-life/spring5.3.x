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
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;

/**
 * 用于 {@code java.util.regex.Pattern} 的编辑器，可直接填充 Pattern 属性。期望与 Pattern 的
 * {@code compile} 方法使用相同的语法。
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see java.util.regex.Pattern
 * @see java.util.regex.Pattern#compile(String)
 */
public class PatternEditor extends PropertyEditorSupport {

	private final int flags;


	/**
	 * 使用默认设置创建一个新的 PatternEditor。
	 */
	public PatternEditor() {
		this.flags = 0;
	}

	/**
	 * 使用给定设置创建一个新的 PatternEditor。
	 * @param flags the {@code java.util.regex.Pattern} flags to apply
	 * @see java.util.regex.Pattern#compile(String, int)
	 * @see java.util.regex.Pattern#CASE_INSENSITIVE
	 * @see java.util.regex.Pattern#MULTILINE
	 * @see java.util.regex.Pattern#DOTALL
	 * @see java.util.regex.Pattern#UNICODE_CASE
	 * @see java.util.regex.Pattern#CANON_EQ
	 */
	public PatternEditor(int flags) {
		this.flags = flags;
	}


	@Override
	public void setAsText(@Nullable String text) {
		setValue(text != null ? Pattern.compile(text, this.flags) : null);
	}

	@Override
	public String getAsText() {
		Pattern value = (Pattern) getValue();
		return (value != null ? value.pattern() : "");
	}

}
