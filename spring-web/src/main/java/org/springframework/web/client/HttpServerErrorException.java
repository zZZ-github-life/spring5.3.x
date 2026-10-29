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

/**
 * 收到 HTTP 5xx 时抛出的异常。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see DefaultResponseErrorHandler
 */
public class HttpServerErrorException extends HttpStatusCodeException {

	private static final long serialVersionUID = -2915754006618138282L;


	/**
	 * 仅包含状态码的构造函数。
	 */
	public HttpServerErrorException(HttpStatus statusCode) {
		super(statusCode);
	}

	/**
	 * 包含状态码和状态文本的构造函数。
	 */
	public HttpServerErrorException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	/**
	 * 包含状态码、状态文本和内容的构造函数。
	 */
	public HttpServerErrorException(
			HttpStatus statusCode, String statusText, @Nullable byte[] body, @Nullable Charset charset) {

		super(statusCode, statusText, body, charset);
	}

	/**
	 * 包含状态码、状态文本、头和内容的构造函数。
	 */
	public HttpServerErrorException(HttpStatus statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {

		super(statusCode, statusText, headers, body, charset);
	}

	/**
	 * 包含状态码、状态文本、头、内容以及预先准备的消息的构造函数。
	 * @since 5.2.2
	 */
	public HttpServerErrorException(String message, HttpStatus statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {

		super(message, statusCode, statusText, headers, body, charset);
	}

	/**
	 * 创建 {@code HttpServerErrorException} 或特定于 HTTP 状态的子类。
	 * @since 5.1
	 */
	public static HttpServerErrorException create(HttpStatus statusCode,
			String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		return create(null, statusCode, statusText, headers, body, charset);
	}

	/**
	 * {@link #create(String, HttpStatus, String, HttpHeaders, byte[], Charset)} 的变体，
	 * 带有可选的预先准备的消息。
	 * @since 5.2.2.
	 */
	public static HttpServerErrorException create(@Nullable String message, HttpStatus statusCode,
			String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		switch (statusCode) {
			case INTERNAL_SERVER_ERROR:
				return message != null ?
						new HttpServerErrorException.InternalServerError(message, statusText, headers, body, charset) :
						new HttpServerErrorException.InternalServerError(statusText, headers, body, charset);
			case NOT_IMPLEMENTED:
				return message != null ?
						new HttpServerErrorException.NotImplemented(message, statusText, headers, body, charset) :
						new HttpServerErrorException.NotImplemented(statusText, headers, body, charset);
			case BAD_GATEWAY:
				return message != null ?
						new HttpServerErrorException.BadGateway(message, statusText, headers, body, charset) :
						new HttpServerErrorException.BadGateway(statusText, headers, body, charset);
			case SERVICE_UNAVAILABLE:
				return message != null ?
						new HttpServerErrorException.ServiceUnavailable(message, statusText, headers, body, charset) :
						new HttpServerErrorException.ServiceUnavailable(statusText, headers, body, charset);
			case GATEWAY_TIMEOUT:
				return message != null ?
						new HttpServerErrorException.GatewayTimeout(message, statusText, headers, body, charset) :
						new HttpServerErrorException.GatewayTimeout(statusText, headers, body, charset);
			default:
				return message != null ?
						new HttpServerErrorException(message, statusCode, statusText, headers, body, charset) :
						new HttpServerErrorException(statusCode, statusText, headers, body, charset);
		}
	}


	// Subclasses for specific HTTP status codes

	/**
	 * 用于状态 HTTP 500 Internal Server Error 的 {@link HttpServerErrorException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class InternalServerError extends HttpServerErrorException {

		private InternalServerError(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.INTERNAL_SERVER_ERROR, statusText, headers, body, charset);
		}

		private InternalServerError(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.INTERNAL_SERVER_ERROR, statusText, headers, body, charset);
		}
	}

	/**
	 * 用于状态 HTTP 501 Not Implemented 的 {@link HttpServerErrorException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class NotImplemented extends HttpServerErrorException {

		private NotImplemented(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.NOT_IMPLEMENTED, statusText, headers, body, charset);
		}

		private NotImplemented(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.NOT_IMPLEMENTED, statusText, headers, body, charset);
		}
	}

	/**
	 * 用于 HTTP 状态 502 Bad Gateway 的 {@link HttpServerErrorException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class BadGateway extends HttpServerErrorException {

		private BadGateway(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.BAD_GATEWAY, statusText, headers, body, charset);
		}

		private BadGateway(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.BAD_GATEWAY, statusText, headers, body, charset);
		}
	}

	/**
	 * 用于状态 HTTP 503 Service Unavailable 的 {@link HttpServerErrorException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class ServiceUnavailable extends HttpServerErrorException {

		private ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.SERVICE_UNAVAILABLE, statusText, headers, body, charset);
		}

		private ServiceUnavailable(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.SERVICE_UNAVAILABLE, statusText, headers, body, charset);
		}
	}

	/**
	 * 用于状态 HTTP 504 Gateway Timeout 的 {@link HttpServerErrorException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class GatewayTimeout extends HttpServerErrorException {

		private GatewayTimeout(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.GATEWAY_TIMEOUT, statusText, headers, body, charset);
		}

		private GatewayTimeout(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.GATEWAY_TIMEOUT, statusText, headers, body, charset);
		}
	}

}
