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

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link RestTemplate} 所抛异常的基类，适用于请求因服务器错误响应
 * （由 {@link ResponseErrorHandler#hasError(ClientHttpResponse)} 判定）、
 * 响应解码失败或底层 I/O 错误而失败的情况。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class RestClientException extends NestedRuntimeException {

	private static final long serialVersionUID = -4084444984163796577L;


	/**
	 * 使用给定消息构造一个 {@code RestClientException} 新实例。
	 * @param msg the message
	 */
	public RestClientException(String msg) {
		super(msg);
	}

	/**
	 * 使用给定消息和异常构造一个 {@code RestClientException} 新实例。
	 * @param msg the message
	 * @param ex the exception
	 */
	public RestClientException(String msg, Throwable ex) {
		super(msg, ex);
	}

}
