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

package org.springframework.http.converter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * 可读写 {@link Resource Resource} 并支持字节范围请求的
 * {@link HttpMessageConverter} 实现。
 *
 * <p>默认情况下，此转换器可以读取所有媒体类型。{@link MediaTypeFactory}
 * 用于确定所写入资源的 {@code Content-Type}。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @since 3.0.2
 */
public class ResourceHttpMessageConverter extends AbstractHttpMessageConverter<Resource> {

	private final boolean supportsReadStreaming;


	/**
	 * 创建 {@code ResourceHttpMessageConverter} 的新实例，
	 * 支持读取流式处理，即可将
	 * {@code HttpInputMessage} 转换为 {@code InputStreamResource}。
	 */
	public ResourceHttpMessageConverter() {
		super(MediaType.ALL);
		this.supportsReadStreaming = true;
	}

	/**
	 * 创建 {@code ResourceHttpMessageConverter} 的新实例。
	 * @param supportsReadStreaming 该转换器是否应支持读流式传输，
	 * 即转换为 {@code InputStreamResource}
	 * @since 5.0
	 */
	public ResourceHttpMessageConverter(boolean supportsReadStreaming) {
		super(MediaType.ALL);
		this.supportsReadStreaming = supportsReadStreaming;
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Resource.class.isAssignableFrom(clazz);
	}

	@Override
	protected Resource readInternal(Class<? extends Resource> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		if (this.supportsReadStreaming && InputStreamResource.class == clazz) {
			return new InputStreamResource(inputMessage.getBody()) {
				@Override
				public String getFilename() {
					return inputMessage.getHeaders().getContentDisposition().getFilename();
				}
				@Override
				public long contentLength() throws IOException {
					long length = inputMessage.getHeaders().getContentLength();
					return (length != -1 ? length : super.contentLength());
				}
			};
		}
		else if (Resource.class == clazz || ByteArrayResource.class.isAssignableFrom(clazz)) {
			byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
			return new ByteArrayResource(body) {
				@Override
				@Nullable
				public String getFilename() {
					return inputMessage.getHeaders().getContentDisposition().getFilename();
				}
			};
		}
		else {
			throw new HttpMessageNotReadableException("Unsupported resource class: " + clazz, inputMessage);
		}
	}

	@Override
	protected MediaType getDefaultContentType(Resource resource) {
		return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Override
	protected Long getContentLength(Resource resource, @Nullable MediaType contentType) throws IOException {
		// Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
		// Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
		if (InputStreamResource.class == resource.getClass()) {
			return null;
		}
		long contentLength = resource.contentLength();
		return (contentLength < 0 ? null : contentLength);
	}

	@Override
	protected void writeInternal(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeContent(resource, outputMessage);
	}

	protected void writeContent(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		// We cannot use try-with-resources here for the InputStream, since we have
		// custom handling of the close() method in a finally-block.
		try {
			InputStream in = resource.getInputStream();
			try {
				StreamUtils.copy(in, outputMessage.getBody());
			}
			catch (NullPointerException ex) {
				// ignore, see SPR-13620
			}
			finally {
				try {
					in.close();
				}
				catch (Throwable ex) {
					// ignore, see SPR-12999
				}
			}
		}
		catch (FileNotFoundException ex) {
			// ignore, see SPR-12999
		}
	}

}
