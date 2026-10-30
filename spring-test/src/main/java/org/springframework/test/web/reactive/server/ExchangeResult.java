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

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * 用于保存通过 {@link WebTestClient} 执行的交换的请求和响应详细信息的容器。
 *
 * <p>请注意，此级别不暴露解码后的响应体，因为响应体可能尚未被解码和消费。子类型
 * {@link EntityExchangeResult} 和 {@link FluxExchangeResult} 分别提供对解码后的响应实体
 * 和解码后（但未消费）响应体的访问。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 * @see EntityExchangeResult
 * @see FluxExchangeResult
 */
public class ExchangeResult {

	private static final Log logger = LogFactory.getLog(ExchangeResult.class);

	private static final List<MediaType> PRINTABLE_MEDIA_TYPES = Arrays.asList(
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
			MediaType.parseMediaType("text/*"), MediaType.APPLICATION_FORM_URLENCODED);


	private final ClientHttpRequest request;

	private final ClientHttpResponse response;

	private final Mono<byte[]> requestBody;

	private final Mono<byte[]> responseBody;

	private final Duration timeout;

	@Nullable
	private final String uriTemplate;

	@Nullable
	private final Object mockServerResult;

	/** 确保只记录一次日志，例如对于 expectAll。 */
	private boolean diagnosticsLogged;


	/**
	 * 使用 HTTP 请求和响应以及序列化请求体和响应体内容的 Promise 创建实例。
	 * @param request HTTP 请求
	 * @param response HTTP 响应
	 * @param requestBody 序列化请求体内容的捕获
	 * @param responseBody 序列化响应体内容的捕获
	 * @param timeout 等待内容具体化的时长
	 * @param uriTemplate 用于设置请求的 URI 模板（如果有）
	 * @param serverResult 模拟服务器交换的结果（如果适用）。
	 */
	ExchangeResult(ClientHttpRequest request, ClientHttpResponse response,
			Mono<byte[]> requestBody, Mono<byte[]> responseBody, Duration timeout, @Nullable String uriTemplate,
			@Nullable Object serverResult) {

		Assert.notNull(request, "ClientHttpRequest is required");
		Assert.notNull(response, "ClientHttpResponse is required");
		Assert.notNull(requestBody, "'requestBody' is required");
		Assert.notNull(responseBody, "'responseBody' is required");

		this.request = request;
		this.response = response;
		this.requestBody = requestBody;
		this.responseBody = responseBody;
		this.timeout = timeout;
		this.uriTemplate = uriTemplate;
		this.mockServerResult = serverResult;
	}

	/**
	 * 在响应体被解码和/或消费后使用的复制构造函数。
	 */
	ExchangeResult(ExchangeResult other) {
		this.request = other.request;
		this.response = other.response;
		this.requestBody = other.requestBody;
		this.responseBody = other.responseBody;
		this.timeout = other.timeout;
		this.uriTemplate = other.uriTemplate;
		this.mockServerResult = other.mockServerResult;
		this.diagnosticsLogged = other.diagnosticsLogged;
	}


	/**
	 * 返回请求的方法。
	 */
	public HttpMethod getMethod() {
		return this.request.getMethod();
	}

	/**
	 * 返回请求的 URI。
	 */
	public URI getUrl() {
		return this.request.getURI();
	}

	/**
	 * 返回用于准备请求的原始 URI 模板（如果有）。
	 */
	@Nullable
	public String getUriTemplate() {
		return this.uriTemplate;
	}

	/**
	 * 返回发送到服务器的请求头。
	 */
	public HttpHeaders getRequestHeaders() {
		return this.request.getHeaders();
	}

	/**
	 * 返回通过请求写入的原始请求体内容。
	 * <p><strong>注意：</strong>如果请求内容由于任何原因尚未被消费，使用此方法将触发消费。
	 * @throws IllegalStateException 如果请求体尚未完全写入。
	 */
	@Nullable
	public byte[] getRequestBodyContent() {
		return this.requestBody.block(this.timeout);
	}


	/**
	 * 以 {@link HttpStatus} 枚举值形式返回 HTTP 状态码。
	 * @throws IllegalArgumentException 遇到未知的 HTTP 状态码时。
	 * @see #getRawStatusCode()
	 */
	public HttpStatus getStatus() {
		return this.response.getStatusCode();
	}

	/**
	 * 以整数形式返回 HTTP 状态码（可能是非标准且无法通过
	 * {@link HttpStatus} 枚举解析的）。
	 * @since 5.1.10
	 */
	public int getRawStatusCode() {
		return this.response.getRawStatusCode();
	}

	/**
	 * 返回从服务器接收的响应头。
	 */
	public HttpHeaders getResponseHeaders() {
		return this.response.getHeaders();
	}

	/**
	 * 返回从服务器接收的响应 Cookie。
	 */
	public MultiValueMap<String, ResponseCookie> getResponseCookies() {
		return this.response.getCookies();
	}

	/**
	 * 返回写入响应的原始响应体内容。
	 * <p><strong>注意：</strong>如果响应内容尚未被消费，
	 * 使用此方法将触发消费。
	 * @throws IllegalStateException 如果响应尚未完全读取。
	 */
	@Nullable
	public byte[] getResponseBodyContent() {
		return this.responseBody.block(this.timeout);
	}

	/**
	 * 返回模拟服务器交换的结果（如果适用），以便
	 * 对服务器响应的状态进行进一步断言。
	 * @since 5.3
	 * @see org.springframework.test.web.servlet.client.MockMvcWebTestClient#resultActionsFor(ExchangeResult)
	 */
	@Nullable
	public Object getMockServerResult() {
		return this.mockServerResult;
	}

	/**
	 * 执行给定的 Runnable，捕获任何 {@link AssertionError}，在类日志
	 * 类别下以 ERROR 级别记录有关请求和响应的详细信息，
	 * 然后重新抛出该错误。
	 */
	public void assertWithDiagnostics(Runnable assertion) {
		try {
			assertion.run();
		}
		catch (AssertionError ex) {
			if (!this.diagnosticsLogged && logger.isErrorEnabled()) {
				this.diagnosticsLogged = true;
				logger.error("Request details for assertion failure:\n" + this);
			}
			throw ex;
		}
	}


	@Override
	public String toString() {
		return "\n" +
				"> " + getMethod() + " " + getUrl() + "\n" +
				"> " + formatHeaders(getRequestHeaders(), "\n> ") + "\n" +
				"\n" +
				formatBody(getRequestHeaders().getContentType(), this.requestBody) + "\n" +
				"\n" +
				"< " + formatStatus() + "\n" +
				"< " + formatHeaders(getResponseHeaders(), "\n< ") + "\n" +
				"\n" +
				formatBody(getResponseHeaders().getContentType(), this.responseBody) +"\n" +
				formatMockServerResult();
	}

	private String formatStatus() {
		try {
			return getStatus() + " " + getStatus().getReasonPhrase();
		}
		catch (Exception ex) {
			return Integer.toString(getRawStatusCode());
		}
	}

	private String formatHeaders(HttpHeaders headers, String delimiter) {
		return headers.entrySet().stream()
				.map(entry -> entry.getKey() + ": " + entry.getValue())
				.collect(Collectors.joining(delimiter));
	}

	@Nullable
	private String formatBody(@Nullable MediaType contentType, Mono<byte[]> body) {
		return body
				.map(bytes -> {
					if (contentType == null) {
						return bytes.length + " bytes of content (unknown content-type).";
					}
					Charset charset = contentType.getCharset();
					if (charset != null) {
						return new String(bytes, charset);
					}
					if (PRINTABLE_MEDIA_TYPES.stream().anyMatch(contentType::isCompatibleWith)) {
						return new String(bytes, StandardCharsets.UTF_8);
					}
					return bytes.length + " bytes of content.";
				})
				.defaultIfEmpty("No content")
				.onErrorResume(ex -> Mono.just("Failed to obtain content: " + ex.getMessage()))
				.block(this.timeout);
	}

	private String formatMockServerResult() {
		return (this.mockServerResult != null ?
				"\n======================  MockMvc (Server) ===============================\n" +
						this.mockServerResult + "\n" : "");
	}

}
