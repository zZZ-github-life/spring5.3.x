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

package org.springframework.web.reactive.function.client;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

/**
 * 包含实际 HTTP 响应数据的异常。
 * @author Arjen Poutsma
 * @since 5.0
 */
public class WebClientResponseException extends WebClientException {

	private static final long serialVersionUID = 4127543205414951611L;


	private final int statusCode;

	private final String statusText;

	private final byte[] responseBody;

	private final HttpHeaders headers;

	@Nullable
	private final Charset responseCharset;

	@Nullable
	private transient final HttpRequest request;


	/**
	 * 仅使用响应数据和默认消息的构造函数。
	 * @since 5.1
	 */
	public WebClientResponseException(int statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {

		this(statusCode, statusText, headers, body, charset, null);
	}

	/**
	 * 仅使用响应数据和默认消息的构造函数。
	 * @since 5.1.4
	 */
	public WebClientResponseException(int status, String reasonPhrase,
			@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset,
			@Nullable HttpRequest request) {

		this(initMessage(status, reasonPhrase, request), status, reasonPhrase, headers, body, charset, request);
	}

	private static String initMessage(int status, String reasonPhrase, @Nullable HttpRequest request) {
		return status + " " + reasonPhrase +
				(request != null ? " from " + request.getMethodValue() + " " + request.getURI() : "");
	}

	/**
	 * 使用预先准备的消息的构造函数。
	 */
	public WebClientResponseException(String message, int statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset) {
		this(message, statusCode, statusText, headers, responseBody, charset, null);
	}

	/**
	 * 使用预先准备的消息的构造函数。
	 * @since 5.1.4
	 */
	public WebClientResponseException(String message, int statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset,
			@Nullable HttpRequest request) {

		super(message);

		this.statusCode = statusCode;
		this.statusText = statusText;
		this.headers = copy(headers);
		this.responseBody = (responseBody != null ? responseBody : new byte[0]);
		this.responseCharset = charset;
		this.request = request;
	}

	/**
	 * 并非所有 {@code HttpHeaders} 实现都可序列化，因此我们进行复制以确保 {@code WebClientResponseException} 可序列化。
	 */
	private static HttpHeaders copy(@Nullable HttpHeaders headers) {
		if (headers == null) {
			return HttpHeaders.EMPTY;
		}
		else {
			HttpHeaders result = new HttpHeaders();
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				for (String value : entry.getValue()) {
					result.add(entry.getKey(), value);
				}
			}
			return result;
		}
	}


	/**
	 * 返回 HTTP 状态码值。
	 * @throws IllegalArgumentException in case of an unknown HTTP status code
	 */
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.statusCode);
	}

	/**
	 * 返回原始 HTTP 状态码值。
	 */
	public int getRawStatusCode() {
		return this.statusCode;
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
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * 将响应正文作为字节数组返回。
	 */
	public byte[] getResponseBodyAsByteArray() {
		return this.responseBody;
	}

	/**
	 * 使用响应的媒体类型字符集（如果可用）将响应内容作为 String 返回，否则回退到 {@literal ISO-8859-1}。如果希望回退到其他默认字符集，请使用 {@link #getResponseBodyAsString(Charset)}。
	 */
	public String getResponseBodyAsString() {
		return getResponseBodyAsString(StandardCharsets.ISO_8859_1);
	}

	/**
	 * {@link #getResponseBodyAsString()} 的变体，允许指定在无法从响应的媒体类型获取字符集时回退使用的字符集。
	 * @param defaultCharset the charset to use if the {@literal Content-Type}
	 * of the response does not specify one.
	 * @since 5.3.7
	 */
	public String getResponseBodyAsString(Charset defaultCharset) {
		return new String(this.responseBody,
				(this.responseCharset != null ? this.responseCharset : defaultCharset));
	}

	/**
	 * 返回对应的请求。
	 * @since 5.1.4
	 */
	@Nullable
	public HttpRequest getRequest() {
		return this.request;
	}

	/**
	 * 创建 {@code WebClientResponseException} 或特定 HTTP 状态的子类。
	 * @since 5.1
	 */
	public static WebClientResponseException create(
			int statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		return create(statusCode, statusText, headers, body, charset, null);
	}

	/**
	 * 创建 {@code WebClientResponseException} 或特定 HTTP 状态的子类。
	 * @since 5.1.4
	 */
	public static WebClientResponseException create(
			int statusCode, String statusText, HttpHeaders headers, byte[] body,
			@Nullable Charset charset, @Nullable HttpRequest request) {

		HttpStatus httpStatus = HttpStatus.resolve(statusCode);
		if (httpStatus != null) {
			switch (httpStatus) {
				case BAD_REQUEST:
					return new WebClientResponseException.BadRequest(statusText, headers, body, charset, request);
				case UNAUTHORIZED:
					return new WebClientResponseException.Unauthorized(statusText, headers, body, charset, request);
				case FORBIDDEN:
					return new WebClientResponseException.Forbidden(statusText, headers, body, charset, request);
				case NOT_FOUND:
					return new WebClientResponseException.NotFound(statusText, headers, body, charset, request);
				case METHOD_NOT_ALLOWED:
					return new WebClientResponseException.MethodNotAllowed(statusText, headers, body, charset, request);
				case NOT_ACCEPTABLE:
					return new WebClientResponseException.NotAcceptable(statusText, headers, body, charset, request);
				case CONFLICT:
					return new WebClientResponseException.Conflict(statusText, headers, body, charset, request);
				case GONE:
					return new WebClientResponseException.Gone(statusText, headers, body, charset, request);
				case UNSUPPORTED_MEDIA_TYPE:
					return new WebClientResponseException.UnsupportedMediaType(statusText, headers, body, charset, request);
				case TOO_MANY_REQUESTS:
					return new WebClientResponseException.TooManyRequests(statusText, headers, body, charset, request);
				case UNPROCESSABLE_ENTITY:
					return new WebClientResponseException.UnprocessableEntity(statusText, headers, body, charset, request);
				case INTERNAL_SERVER_ERROR:
					return new WebClientResponseException.InternalServerError(statusText, headers, body, charset, request);
				case NOT_IMPLEMENTED:
					return new WebClientResponseException.NotImplemented(statusText, headers, body, charset, request);
				case BAD_GATEWAY:
					return new WebClientResponseException.BadGateway(statusText, headers, body, charset, request);
				case SERVICE_UNAVAILABLE:
					return new WebClientResponseException.ServiceUnavailable(statusText, headers, body, charset, request);
				case GATEWAY_TIMEOUT:
					return new WebClientResponseException.GatewayTimeout(statusText, headers, body, charset, request);
			}
		}
		return new WebClientResponseException(statusCode, statusText, headers, body, charset, request);
	}



	// Subclasses for specific, client-side, HTTP status codes

	/**
	 * 用于 HTTP 状态 400 Bad Request 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class BadRequest extends WebClientResponseException {

		BadRequest(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.BAD_REQUEST.value(), statusText, headers, body, charset, request);
		}

	}

	/**
	 * 用于 HTTP 状态 401 Unauthorized 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Unauthorized extends WebClientResponseException {

		Unauthorized(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.UNAUTHORIZED.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 用于 HTTP 状态 403 Forbidden 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Forbidden extends WebClientResponseException {

		Forbidden(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.FORBIDDEN.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 用于 HTTP 状态 404 Not Found 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotFound extends WebClientResponseException {

		NotFound(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.NOT_FOUND.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 用于 HTTP 状态 405 Method Not Allowed 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class MethodNotAllowed extends WebClientResponseException {

		MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.METHOD_NOT_ALLOWED.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * 用于 HTTP 状态 406 Not Acceptable 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotAcceptable extends WebClientResponseException {

		NotAcceptable(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.NOT_ACCEPTABLE.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 用于 HTTP 状态 409 Conflict 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Conflict extends WebClientResponseException {

		Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.CONFLICT.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 用于 HTTP 状态 410 Gone 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Gone extends WebClientResponseException {

		Gone(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.GONE.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 用于 HTTP 状态 415 Unsupported Media Type 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class UnsupportedMediaType extends WebClientResponseException {

		UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {

			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * 用于 HTTP 状态 422 Unprocessable Entity 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class UnprocessableEntity extends WebClientResponseException {

		UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.UNPROCESSABLE_ENTITY.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * 用于 HTTP 状态 429 Too Many Requests 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class TooManyRequests extends WebClientResponseException {

		TooManyRequests(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.TOO_MANY_REQUESTS.value(), statusText, headers, body, charset,
					request);
		}
	}



	// Subclasses for specific, server-side, HTTP status codes

	/**
	 * 用于 HTTP 状态 500 Internal Server Error 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class InternalServerError extends WebClientResponseException {

		InternalServerError(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.INTERNAL_SERVER_ERROR.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * 用于 HTTP 状态 501 Not Implemented 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotImplemented extends WebClientResponseException {

		NotImplemented(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.NOT_IMPLEMENTED.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 用于 HTTP 状态 502 Bad Gateway 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class BadGateway extends WebClientResponseException {

		BadGateway(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.BAD_GATEWAY.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * 用于 HTTP 状态 503 Service Unavailable 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class ServiceUnavailable extends WebClientResponseException {

		ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.SERVICE_UNAVAILABLE.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * 用于 HTTP 状态 504 Gateway Timeout 的 {@link WebClientResponseException}。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class GatewayTimeout extends WebClientResponseException {

		GatewayTimeout(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.GATEWAY_TIMEOUT.value(), statusText, headers, body, charset,
					request);
		}
	}

}
