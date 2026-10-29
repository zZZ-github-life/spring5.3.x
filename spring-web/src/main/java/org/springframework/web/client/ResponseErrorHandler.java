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
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 由 {@link RestTemplate} 使用的策略接口，用于确定特定响应是否包含错误。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ResponseErrorHandler {

	/**
	 * 指示给定响应是否有任何错误。
	 * <p>实现通常会检查响应的
	 * {@link ClientHttpResponse#getStatusCode() HttpStatus}。
	 * @param response the response to inspect
	 * @return {@code true} if the response indicates an error; {@code false} otherwise
	 * @throws IOException in case of I/O errors
	 */
	boolean hasError(ClientHttpResponse response) throws IOException;

	/**
	 * 处理给定响应中的错误。
	 * <p>仅当 {@link #hasError(ClientHttpResponse)} 返回 {@code true} 时才会调用此方法。
	 * @param response the response with the error
	 * @throws IOException in case of I/O errors
	 */
	void handleError(ClientHttpResponse response) throws IOException;

	/**
	 * {@link #handleError(ClientHttpResponse)} 的替代方法，提供额外信息，
	 * 可访问请求 URL 和 HTTP 方法。
	 * @param url the request URL
	 * @param method the HTTP method
	 * @param response the response with the error
	 * @throws IOException in case of I/O errors
	 * @since 5.0
	 */
	default void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		handleError(response);
	}

}
