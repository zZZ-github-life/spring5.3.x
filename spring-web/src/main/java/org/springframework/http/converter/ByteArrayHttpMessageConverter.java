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

package org.springframework.http.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * 可读写字节数组的 {@link HttpMessageConverter} 实现。
 *
 * <p>默认情况下，此转换器支持所有媒体类型（<code>&#42;/&#42;</code>），
 * 并使用 {@code application/octet-stream} 作为 {@code Content-Type} 进行写入。
 * 可以通过设置 {@link #setSupportedMediaTypes supportedMediaTypes} 属性来覆盖。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {

	/**
	 * 创建 {@code ByteArrayHttpMessageConverter} 的新实例。
	 */
	public ByteArrayHttpMessageConverter() {
		super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return byte[].class == clazz;
	}

	@Override
	public byte[] readInternal(Class<? extends byte[]> clazz, HttpInputMessage inputMessage) throws IOException {
		long contentLength = inputMessage.getHeaders().getContentLength();
		ByteArrayOutputStream bos =
				new ByteArrayOutputStream(contentLength >= 0 ? (int) contentLength : StreamUtils.BUFFER_SIZE);
		StreamUtils.copy(inputMessage.getBody(), bos);
		return bos.toByteArray();
	}

	@Override
	protected Long getContentLength(byte[] bytes, @Nullable MediaType contentType) {
		return (long) bytes.length;
	}

	@Override
	protected void writeInternal(byte[] bytes, HttpOutputMessage outputMessage) throws IOException {
		StreamUtils.copy(bytes, outputMessage.getBody());
	}

}
