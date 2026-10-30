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

package org.springframework.web.client;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * 当找不到合适的
 * {@link org.springframework.http.converter.HttpMessageConverter}
 * 来提取响应时抛出。
 *
 * @author Rossen Stoyanchev
 * @since 5.2.7
 */
public class UnknownContentTypeException extends RestClientException {

	private static final long serialVersionUID = 2759516676367274084L;


	private final Type targetType;

	private final MediaType contentType;

	private final int rawStatusCode;

	private final String statusText;

	private final byte[] responseBody;

	private final HttpHeaders responseHeaders;


	/**
	 * 使用给定响应数据构造新实例。
	 * @param targetType 期望的目标类型
	 * @param contentType 响应的内容类型
	 * @param statusCode 原始状态码值
	 * @param statusText 状态文本
	 * @param responseHeaders 响应头（可能为 {@code null}）
	 * @param responseBody 响应体内容（可能为 {@code null}）
	 */
	public UnknownContentTypeException(Type targetType, MediaType contentType,
			int statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody) {

		super("Could not extract response: no suitable HttpMessageConverter found " +
				"for response type [" + targetType + "] and content type [" + contentType + "]");

		this.targetType = targetType;
		this.contentType = contentType;
		this.rawStatusCode = statusCode;
		this.statusText = statusText;
		this.responseHeaders = responseHeaders;
		this.responseBody = responseBody;
	}


	/**
	 * 返回响应所预期的目标类型。
	 */
	public Type getTargetType() {
		return this.targetType;
	}

	/**
	 * 返回响应的内容类型，或 "application/octet-stream"。
	 */
	public MediaType getContentType() {
		return this.contentType;
	}

	/**
	 * 返回原始 HTTP 状态码值。
	 */
	public int getRawStatusCode() {
		return this.rawStatusCode;
	}

	/**
	 * 返回 HTTP 状态文本。
	 */
	public String getStatusText() {
		return this.statusText;
	}

	/**
	 * 返回 HTTP 响应头。
	 */
	@Nullable
	public HttpHeaders getResponseHeaders() {
		return this.responseHeaders;
	}

	/**
	 * 以字节数组形式返回响应体。
	 */
	public byte[] getResponseBody() {
		return this.responseBody;
	}

	/**
	 * 返回转换为 String 的响应体，使用响应 "Content-Type" 中的字符集，
	 * 否则使用 {@code "UTF-8"}。
	 */
	public String getResponseBodyAsString() {
		return new String(this.responseBody, this.contentType.getCharset() != null ?
				this.contentType.getCharset() : StandardCharsets.UTF_8);
	}

}
