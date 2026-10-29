/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;

/**
 * multipart HTTP 消息写入器的支持类。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class MultipartWriterSupport extends LoggingCodecSupport {

	/** 写入器使用的默认字符集。 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private final List<MediaType> supportedMediaTypes;

	private Charset charset = DEFAULT_CHARSET;


	/**
	 * 使用受支持的媒体类型列表的构造函数。
	 */
	protected MultipartWriterSupport(List<MediaType> supportedMediaTypes) {
		this.supportedMediaTypes = supportedMediaTypes;
	}


	/**
	 * 返回为部件头配置的字符集。
	 */
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * 设置用于部件头（如 "Content-Disposition" 及其 filename 参数）的
	 * 字符集。
	 * <p>默认设置为 "UTF-8"。如果偏离此默认值，
	 * "Content-Type" 头将带有一个 "charset" 参数，用于指定
	 * 所使用的字符集。
	 */
	public void setCharset(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		this.charset = charset;
	}

	public List<MediaType> getWritableMediaTypes() {
		return this.supportedMediaTypes;
	}


	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		if (MultiValueMap.class.isAssignableFrom(elementType.toClass())) {
			if (mediaType == null) {
				return true;
			}
			for (MediaType supportedMediaType : this.supportedMediaTypes) {
				if (supportedMediaType.isCompatibleWith(mediaType)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 生成 multipart 边界。
	 * <p>默认委托给 {@link MimeTypeUtils#generateMultipartBoundary()}。
	 */
	protected byte[] generateMultipartBoundary() {
		return MimeTypeUtils.generateMultipartBoundary();
	}

	/**
	 * 准备要使用的 {@code MediaType}，即向给定的 {@code mediaType}
	 * 添加 "boundary" 和 "charset" 参数，否则默认使用
	 * "multipart/form-data"。
	 */
	protected MediaType getMultipartMediaType(@Nullable MediaType mediaType, byte[] boundary) {
		Map<String, String> params = new HashMap<>();
		if (mediaType != null) {
			params.putAll(mediaType.getParameters());
		}
		params.put("boundary", new String(boundary, StandardCharsets.US_ASCII));
		Charset charset = getCharset();
		if (!charset.equals(StandardCharsets.UTF_8) &&
				!charset.equals(StandardCharsets.US_ASCII) ) {
			params.put("charset", charset.name());
		}

		mediaType = (mediaType != null ? mediaType : MediaType.MULTIPART_FORM_DATA);
		mediaType = new MediaType(mediaType, params);
		return mediaType;
	}

	protected Mono<DataBuffer> generateBoundaryLine(byte[] boundary, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			DataBuffer buffer = bufferFactory.allocateBuffer(boundary.length + 4);
			buffer.write((byte)'-');
			buffer.write((byte)'-');
			buffer.write(boundary);
			buffer.write((byte)'\r');
			buffer.write((byte)'\n');
			return buffer;
		});
	}

	protected Mono<DataBuffer> generateNewLine(DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			DataBuffer buffer = bufferFactory.allocateBuffer(2);
			buffer.write((byte)'\r');
			buffer.write((byte)'\n');
			return buffer;
		});
	}

	protected Mono<DataBuffer> generateLastLine(byte[] boundary, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			DataBuffer buffer = bufferFactory.allocateBuffer(boundary.length + 6);
			buffer.write((byte)'-');
			buffer.write((byte)'-');
			buffer.write(boundary);
			buffer.write((byte)'-');
			buffer.write((byte)'-');
			buffer.write((byte)'\r');
			buffer.write((byte)'\n');
			return buffer;
		});
	}

	protected Mono<DataBuffer> generatePartHeaders(HttpHeaders headers, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			DataBuffer buffer = bufferFactory.allocateBuffer();
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				byte[] headerName = entry.getKey().getBytes(getCharset());
				for (String headerValueString : entry.getValue()) {
					byte[] headerValue = headerValueString.getBytes(getCharset());
					buffer.write(headerName);
					buffer.write((byte)':');
					buffer.write((byte)' ');
					buffer.write(headerValue);
					buffer.write((byte)'\r');
					buffer.write((byte)'\n');
				}
			}
			buffer.write((byte)'\r');
			buffer.write((byte)'\n');
			return buffer;
		});
	}

}
