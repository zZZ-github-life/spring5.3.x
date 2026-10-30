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

package org.springframework.web.client;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * 包含实际 HTTP 响应数据的异常的公共基类。
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class RestClientResponseException extends RestClientException {

	private static final long serialVersionUID = -8803556342728481792L;

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	private final int rawStatusCode;

	private final String statusText;

	private final byte[] responseBody;

	@Nullable
	private final HttpHeaders responseHeaders;

	@Nullable
	private final String responseCharset;


	/**
	 * 使用给定响应数据构造新实例。
	 * @param statusCode 原始状态码值
	 * @param statusText 状态文本
	 * @param responseHeaders 响应头（可能为 {@code null}）
	 * @param responseBody 响应体内容（可能为 {@code null}）
	 * @param responseCharset 响应体字符集（可能为 {@code null}）
	 */
	public RestClientResponseException(String message, int statusCode, String statusText,
			@Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(message);
		this.rawStatusCode = statusCode;
		this.statusText = statusText;
		this.responseHeaders = responseHeaders;
		this.responseBody = (responseBody != null ? responseBody : new byte[0]);
		this.responseCharset = (responseCharset != null ? responseCharset.name() : null);
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
	public byte[] getResponseBodyAsByteArray() {
		return this.responseBody;
	}

	/**
	 * 返回转换为 String 的响应体。所使用的字符集来自响应的 "Content-Type"，
	 * 否则为 {@code "UTF-8"}。
	 */
	public String getResponseBodyAsString() {
		return getResponseBodyAsString(DEFAULT_CHARSET);
	}

	/**
	 * 返回转换为 String 的响应体。所使用的字符集来自响应的 "Content-Type"，
	 * 否则使用给定字符集。
	 * @param fallbackCharset 响应未指定时要使用的字符集。
	 * @since 5.1.11
	 */
	public String getResponseBodyAsString(Charset fallbackCharset) {
		if (this.responseCharset == null) {
			return new String(this.responseBody, fallbackCharset);
		}
		try {
			return new String(this.responseBody, this.responseCharset);
		}
		catch (UnsupportedEncodingException ex) {
			// should not occur
			throw new IllegalStateException(ex);
		}
	}

}
