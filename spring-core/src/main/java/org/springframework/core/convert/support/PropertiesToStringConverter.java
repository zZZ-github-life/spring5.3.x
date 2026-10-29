/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.core.convert.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.core.convert.converter.Converter;

/**
 * 通过调用 {@link Properties#store(java.io.OutputStream, String)} 将 Properties 转换为 String。
 * 在返回 String 之前使用 ISO-8859-1 字符集进行解码。
 *
 * @author Keith Donald
 * @since 3.0
 */
final class PropertiesToStringConverter implements Converter<Properties, String> {

	@Override
	public String convert(Properties source) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream(256);
			source.store(os, null);
			return os.toString("ISO-8859-1");
		}
		catch (IOException ex) {
			// Should never happen.
			throw new IllegalArgumentException("Failed to store [" + source + "] into String", ex);
		}
	}

}
