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

package org.springframework.messaging.converter;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * 一个默认的 {@link ContentTypeResolver}，它检查 {@link MessageHeaders#CONTENT_TYPE}
 * 头，如果不存在则回退到默认值。
 *
 * <p>头值应为 {@link org.springframework.util.MimeType}
 * 或可解析为 {@code MimeType} 的 {@code String}。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultContentTypeResolver implements ContentTypeResolver {

	@Nullable
	private MimeType defaultMimeType;


	/**
	 * 设置当不存在 {@link MessageHeaders#CONTENT_TYPE} 头时使用的默认 MIME 类型。
	 * <p>此属性没有默认值。
	 */
	public void setDefaultMimeType(@Nullable MimeType defaultMimeType) {
		this.defaultMimeType = defaultMimeType;
	}

	/**
	 * 返回当不存在 {@link MessageHeaders#CONTENT_TYPE} 头时使用的默认 MIME 类型。
	 */
	@Nullable
	public MimeType getDefaultMimeType() {
		return this.defaultMimeType;
	}


	@Override
	@Nullable
	public MimeType resolve(@Nullable MessageHeaders headers) {
		if (headers == null || headers.get(MessageHeaders.CONTENT_TYPE) == null) {
			return this.defaultMimeType;
		}
		Object value = headers.get(MessageHeaders.CONTENT_TYPE);
		if (value == null) {
			return null;
		}
		else if (value instanceof MimeType) {
			return (MimeType) value;
		}
		else if (value instanceof String) {
			return MimeType.valueOf((String) value);
		}
		else {
			throw new IllegalArgumentException(
					"Unknown type for contentType header value: " + value.getClass());
		}
	}

	@Override
	public String toString() {
		return "DefaultContentTypeResolver[" + "defaultMimeType=" + this.defaultMimeType + "]";
	}

}
