/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * {@link ResponseErrorHandler} 的实现，使用 {@link HttpMessageConverter
 * HttpMessageConverters} 将 HTTP 错误响应转换为 {@link RestClientException
 * RestClientExceptions}。
 *
 * <p>要使用此错误处理器，必须指定 {@linkplain #setStatusMapping(Map) 状态映射}
 * 和/或 {@linkplain #setSeriesMapping(Map) 系列映射}。如果其中任一映射与给定
 * {@code ClientHttpResponse} 的 {@linkplain ClientHttpResponse#getStatusCode() 状态码}
 * 匹配，{@link #hasError(ClientHttpResponse)} 将返回 {@code true}，
 * {@link #handleError(ClientHttpResponse)} 将尝试使用
 * {@linkplain #setMessageConverters(List) 配置的消息转换器} 将响应转换为
 * 映射的 {@link RestClientException} 子类。请注意，
 * {@linkplain #setStatusMapping(Map) 状态映射} 优先于
 * {@linkplain #setSeriesMapping(Map) 系列映射}。
 *
 * <p>如果没有匹配项，此错误处理器将默认采用
 * {@link DefaultResponseErrorHandler} 的行为。请注意，可以通过指定从
 * {@code HttpStatus.Series#CLIENT_ERROR} 和/或 {@code HttpStatus.Series#SERVER_ERROR}
 * 到 {@code null} 的 {@linkplain #setSeriesMapping(Map) 系列映射}
 * 来覆盖此默认行为。
 *
 * @author Simon Galperin
 * @author Arjen Poutsma
 * @since 5.0
 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
 */
public class ExtractingResponseErrorHandler extends DefaultResponseErrorHandler {

	private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

	private final Map<HttpStatus, Class<? extends RestClientException>> statusMapping = new LinkedHashMap<>();

	private final Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping = new LinkedHashMap<>();


	/**
	 * 创建一个新的空 {@code ExtractingResponseErrorHandler}。
	 * <p>请注意，使用此构造函数时必须调用 {@link #setMessageConverters(List)}。
	 */
	public ExtractingResponseErrorHandler() {
	}

	/**
	 * 使用给定的 {@link HttpMessageConverter} 实例创建一个新的
	 * {@code ExtractingResponseErrorHandler}。
	 * @param messageConverters 要使用的消息转换器
	 */
	public ExtractingResponseErrorHandler(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}


	/**
	 * 设置此提取器要使用的消息转换器。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * 设置从 HTTP 状态码到 {@code RestClientException} 子类的映射。
	 * 如果此映射与给定 {@code ClientHttpResponse} 的
	 * {@linkplain ClientHttpResponse#getStatusCode() 状态码} 匹配，
	 * {@link #hasError(ClientHttpResponse)} 将返回 {@code true}，
	 * {@link #handleError(ClientHttpResponse)} 将尝试使用
	 * {@linkplain #setMessageConverters(List) 配置的消息转换器}
	 * 将响应转换为映射的 {@link RestClientException} 子类。
	 */
	public void setStatusMapping(Map<HttpStatus, Class<? extends RestClientException>> statusMapping) {
		if (!CollectionUtils.isEmpty(statusMapping)) {
			this.statusMapping.putAll(statusMapping);
		}
	}

	/**
	 * 设置从 HTTP 状态系列到 {@code RestClientException} 子类的映射。
	 * 如果此映射与给定 {@code ClientHttpResponse} 的
	 * {@linkplain ClientHttpResponse#getStatusCode() 状态码} 匹配，
	 * {@link #hasError(ClientHttpResponse)} 将返回 {@code true}，
	 * {@link #handleError(ClientHttpResponse)} 将尝试使用
	 * {@linkplain #setMessageConverters(List) 配置的消息转换器}
	 * 将响应转换为映射的 {@link RestClientException} 子类。
	 */
	public void setSeriesMapping(Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping) {
		if (!CollectionUtils.isEmpty(seriesMapping)) {
			this.seriesMapping.putAll(seriesMapping);
		}
	}


	@Override
	protected boolean hasError(HttpStatus statusCode) {
		if (this.statusMapping.containsKey(statusCode)) {
			return this.statusMapping.get(statusCode) != null;
		}
		else if (this.seriesMapping.containsKey(statusCode.series())) {
			return this.seriesMapping.get(statusCode.series()) != null;
		}
		else {
			return super.hasError(statusCode);
		}
	}

	@Override
	public void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
		if (this.statusMapping.containsKey(statusCode)) {
			extract(this.statusMapping.get(statusCode), response);
		}
		else if (this.seriesMapping.containsKey(statusCode.series())) {
			extract(this.seriesMapping.get(statusCode.series()), response);
		}
		else {
			super.handleError(response, statusCode);
		}
	}

	private void extract(@Nullable Class<? extends RestClientException> exceptionClass,
			ClientHttpResponse response) throws IOException {

		if (exceptionClass == null) {
			return;
		}

		HttpMessageConverterExtractor<? extends RestClientException> extractor =
				new HttpMessageConverterExtractor<>(exceptionClass, this.messageConverters);
		RestClientException exception = extractor.extractData(response);
		if (exception != null) {
			throw exception;
		}
	}

}
