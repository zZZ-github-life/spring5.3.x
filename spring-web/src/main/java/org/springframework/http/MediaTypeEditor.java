/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.http;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * 用于 {@link MediaType} 描述符的 {@link java.beans.PropertyEditor Editor}，可自动将 {@code String} 规范（例如 {@code "text/html"}）转换为 {@code MediaType} 属性。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see MediaType
 */
public class MediaTypeEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		if (StringUtils.hasText(text)) {
			setValue(MediaType.parseMediaType(text));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		MediaType mediaType = (MediaType) getValue();
		return (mediaType != null ? mediaType.toString() : "");
	}

}
