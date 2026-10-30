/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;

/**
 * Spring 对 {@link ResponseErrorHandler} 接口的默认实现。
 *
 * <p>此错误处理器会检查 {@link ClientHttpResponse} 上的状态码。
 * 4xx 或 5xx 系列中的任何代码都被视为错误。可以通过覆盖
 * {@link #hasError(HttpStatus)} 来改变此行为。未知状态码将被
 * {@link #hasError(ClientHttpResponse)} 忽略。
 *
 * <p>有关特定异常类型的更多详情，请参见 {@link #handleError(ClientHttpResponse)}。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.0
 * @see RestTemplate#setErrorHandler
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {

	/**
	 * 使用响应的状态码委托给 {@link #hasError(HttpStatus)}（针对标准状态枚举值）
	 * 或 {@link #hasError(int)}（针对未知状态码）。
	 * @see ClientHttpResponse#getRawStatusCode()
	 * @see #hasError(HttpStatus)
	 * @see #hasError(int)
	 */
	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		int rawStatusCode = response.getRawStatusCode();
		HttpStatus statusCode = HttpStatus.resolve(rawStatusCode);
		return (statusCode != null ? hasError(statusCode) : hasError(rawStatusCode));
	}

	/**
	 * 由 {@link #hasError(ClientHttpResponse)} 调用的模板方法。
	 * <p>默认实现检查 {@link HttpStatus#isError()}。
	 * 可以在子类中覆盖。
	 * @param statusCode 作为枚举值的 HTTP 状态码
	 * @return 如果响应表示错误则返回 {@code true}，否则返回 {@code false}
	 * @see HttpStatus#isError()
	 */
	protected boolean hasError(HttpStatus statusCode) {
		return statusCode.isError();
	}

	/**
	 * 由 {@link #hasError(ClientHttpResponse)} 调用的模板方法。
	 * <p>默认实现检查给定状态码是否为
	 * {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR CLIENT_ERROR} 或
	 * {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR SERVER_ERROR}。
	 * 可以在子类中覆盖。
	 * @param unknownStatusCode 作为原始值的 HTTP 状态码
	 * @return 如果响应表示错误则返回 {@code true}，否则返回 {@code false}
	 * @since 4.3.21
	 * @see org.springframework.http.HttpStatus.Series#CLIENT_ERROR
	 * @see org.springframework.http.HttpStatus.Series#SERVER_ERROR
	 */
	protected boolean hasError(int unknownStatusCode) {
		HttpStatus.Series series = HttpStatus.Series.resolve(unknownStatusCode);
		return (series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR);
	}

	/**
	 * 使用已解析的状态码处理给定响应中的错误。
	 * <p>默认实现会抛出：
	 * <ul>
	 * <li>{@link HttpClientErrorException}（如果状态码在 4xx 系列中），
	 * 或其某个子类，如 {@link HttpClientErrorException.BadRequest} 等。
	 * <li>{@link HttpServerErrorException}（如果状态码在 5xx 系列中），
	 * 或其某个子类，如 {@link HttpServerErrorException.InternalServerError} 等。
	 * <li>{@link UnknownHttpStatusCodeException}（对于不在 {@link HttpStatus}
	 * 枚举范围内的错误状态码）。
	 * </ul>
	 * @throws UnknownHttpStatusCodeException 状态码无法解析时
	 * @see #handleError(ClientHttpResponse, HttpStatus)
	 */
	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		HttpStatus statusCode = HttpStatus.resolve(response.getRawStatusCode());
		if (statusCode == null) {
			byte[] body = getResponseBody(response);
			String message = getErrorMessage(response.getRawStatusCode(),
					response.getStatusText(), body, getCharset(response));
			throw new UnknownHttpStatusCodeException(message,
					response.getRawStatusCode(), response.getStatusText(),
					response.getHeaders(), body, getCharset(response));
		}
		handleError(response, statusCode);
	}

	/**
	 * 返回包含响应体详细信息的错误消息。例如：
	 * <pre>
	 * 404 Not Found: [{'id': 123, 'message': 'my message'}]
	 * </pre>
	 */
	private String getErrorMessage(
			int rawStatusCode, String statusText, @Nullable byte[] responseBody, @Nullable Charset charset) {

		String preface = rawStatusCode + " " + statusText + ": ";

		if (ObjectUtils.isEmpty(responseBody)) {
			return preface + "[no body]";
		}

		charset = (charset != null ? charset : StandardCharsets.UTF_8);

		String bodyText = new String(responseBody, charset);
		bodyText = LogFormatUtils.formatValue(bodyText, -1, true);

		return preface + bodyText;
	}

	/**
	 * 基于已解析的状态码处理错误。
	 *
	 * <p>默认实现对于 4xx 范围内的错误委托给
	 * {@link HttpClientErrorException#create}，对于 5xx 范围内的错误委托给
	 * {@link HttpServerErrorException#create}，否则抛出
	 * {@link UnknownHttpStatusCodeException}。
	 * @since 5.0
	 * @see HttpClientErrorException#create
	 * @see HttpServerErrorException#create
	 */
	protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
		String statusText = response.getStatusText();
		HttpHeaders headers = response.getHeaders();
		byte[] body = getResponseBody(response);
		Charset charset = getCharset(response);
		String message = getErrorMessage(statusCode.value(), statusText, body, charset);

		switch (statusCode.series()) {
			case CLIENT_ERROR:
				throw HttpClientErrorException.create(message, statusCode, statusText, headers, body, charset);
			case SERVER_ERROR:
				throw HttpServerErrorException.create(message, statusCode, statusText, headers, body, charset);
			default:
				throw new UnknownHttpStatusCodeException(message, statusCode.value(), statusText, headers, body, charset);
		}
	}

	/**
	 * 确定给定响应的 HTTP 状态。
	 * @param response 要检查的响应
	 * @return 关联的 HTTP 状态
	 * @throws IOException 发生 I/O 错误时
	 * @throws UnknownHttpStatusCodeException 状态码未知且无法用 {@link HttpStatus} 枚举表示时
	 * @since 4.3.8
	 * @deprecated as of 5.0, in favor of {@link #handleError(ClientHttpResponse, HttpStatus)}
	 */
	@Deprecated
	protected HttpStatus getHttpStatusCode(ClientHttpResponse response) throws IOException {
		HttpStatus statusCode = HttpStatus.resolve(response.getRawStatusCode());
		if (statusCode == null) {
			throw new UnknownHttpStatusCodeException(response.getRawStatusCode(), response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		}
		return statusCode;
	}

	/**
	 * 读取给定响应的响应体（用于包含在状态异常中）。
	 * @param response 要检查的响应
	 * @return 响应正文的字节数组；如果无法读取正文则为空字节数组
	 * @since 4.3.8
	 */
	protected byte[] getResponseBody(ClientHttpResponse response) {
		try {
			return FileCopyUtils.copyToByteArray(response.getBody());
		}
		catch (IOException ex) {
			// ignore
		}
		return new byte[0];
	}

	/**
	 * 确定响应的字符集（用于包含在状态异常中）。
	 * @param response 要检查的响应
	 * @return 关联的字符集，如果没有则为 {@code null}
	 * @since 4.3.8
	 */
	@Nullable
	protected Charset getCharset(ClientHttpResponse response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return (contentType != null ? contentType.getCharset() : null);
	}

}
