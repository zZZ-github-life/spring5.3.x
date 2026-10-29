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

package org.springframework.web.multipart.support;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用于将 {@link MultipartFile MultipartFiles} 转换为 String 的
 * 自定义 {@link java.beans.PropertyEditor}。
 *
 * <p>允许指定要使用的字符集。
 *
 * @author Juergen Hoeller
 * @since 13.10.2003
 */
public class StringMultipartFileEditor extends PropertyEditorSupport {

	@Nullable
	private final String charsetName;


	/**
	 * 使用默认字符集创建一个新的 {@link StringMultipartFileEditor}。
	 */
	public StringMultipartFileEditor() {
		this.charsetName = null;
	}

	/**
	 * 使用给定字符集创建一个新的 {@link StringMultipartFileEditor}。
	 * @param charsetName valid charset name
	 * @see java.lang.String#String(byte[],String)
	 */
	public StringMultipartFileEditor(String charsetName) {
		this.charsetName = charsetName;
	}


	@Override
	public void setAsText(String text) {
		setValue(text);
	}

	@Override
	public void setValue(Object value) {
		if (value instanceof MultipartFile) {
			MultipartFile multipartFile = (MultipartFile) value;
			try {
				super.setValue(this.charsetName != null ?
						new String(multipartFile.getBytes(), this.charsetName) :
						new String(multipartFile.getBytes()));
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
			}
		}
		else {
			super.setValue(value);
		}
	}

}
