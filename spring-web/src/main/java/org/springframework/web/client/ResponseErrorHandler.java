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
	 * @param response 要检查的响应
	 * @return 如果响应表示错误则返回 {@code true}；否则返回 {@code false}
	 * @throws IOException 发生 I/O 错误时
	 */
	boolean hasError(ClientHttpResponse response) throws IOException;

	/**
	 * 处理给定响应中的错误。
	 * <p>仅当 {@link #hasError(ClientHttpResponse)} 返回 {@code true} 时才会调用此方法。
	 * @param response 带有错误的响应
	 * @throws IOException 发生 I/O 错误时
	 */
	void handleError(ClientHttpResponse response) throws IOException;

	/**
	 * {@link #handleError(ClientHttpResponse)} 的替代方法，提供额外信息，
	 * 可访问请求 URL 和 HTTP 方法。
	 * @param url 请求 URL
	 * @param method HTTP 方法
	 * @param response 带有错误的响应
	 * @throws IOException 发生 I/O 错误时
	 * @since 5.0
	 */
	default void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		handleError(response);
	}

}
