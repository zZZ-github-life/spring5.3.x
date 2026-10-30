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

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 基于 {@link HttpStatus} 的异常的抽象基类。
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public abstract class HttpStatusCodeException extends RestClientResponseException {

	private static final long serialVersionUID = 5696801857651587810L;


	private final HttpStatus statusCode;


	/**
	 * 使用 {@link HttpStatus} 构造新实例。
	 * @param statusCode 状态码
	 */
	protected HttpStatusCodeException(HttpStatus statusCode) {
		this(statusCode, statusCode.name(), null, null, null);
	}

	/**
	 * 使用 {@link HttpStatus} 和状态文本构造新实例。
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText) {
		this(statusCode, statusText, null, null, null);
	}

	/**
	 * 使用 {@link HttpStatus}、状态文本和内容构造实例。
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 * @param responseBody 响应正文内容，可能为 {@code null}
	 * @param responseCharset 响应正文字符集，可能为 {@code null}
	 * @since 3.0.5
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText,
			@Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		this(statusCode, statusText, null, responseBody, responseCharset);
	}

	/**
	 * 使用 {@link HttpStatus}、状态文本、内容和响应字符集构造实例。
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 * @param responseHeaders 响应头，可能为 {@code null}
	 * @param responseBody 响应体内容，可能为 {@code null}
	 * @param responseCharset 响应体字符集，可能为 {@code null}
	 * @since 3.1.2
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText,
			@Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		this(getMessage(statusCode, statusText),
				statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}

	/**
	 * 使用 {@link HttpStatus}、状态文本、内容和响应字符集构造实例。
	 * @param message 异常消息
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 * @param responseHeaders 响应头，可能为 {@code null}
	 * @param responseBody 响应体内容，可能为 {@code null}
	 * @param responseCharset 响应体字符集，可能为 {@code null}
	 * @since 5.2.2
	 */
	protected HttpStatusCodeException(String message, HttpStatus statusCode, String statusText,
			@Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(message, statusCode.value(), statusText, responseHeaders, responseBody, responseCharset);
		this.statusCode = statusCode;
	}

	private static String getMessage(HttpStatus statusCode, String statusText) {
		if (!StringUtils.hasLength(statusText)) {
			statusText = statusCode.getReasonPhrase();
		}
		return statusCode.value() + " " + statusText;
	}

	/**
	 * 返回 HTTP 状态码。
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

}
