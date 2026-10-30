/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;

/**
 * 表示客户端 HTTP 请求。
 * 通过 {@link ClientHttpRequestFactory} 的实现创建。
 *
 * <p>可以 {@linkplain #execute() 执行} {@code ClientHttpRequest}，
 * 接收可读取的 {@link ClientHttpResponse}。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see ClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 */
public interface ClientHttpRequest extends HttpRequest, HttpOutputMessage {

	/**
	 * 执行此请求，返回可读取的 {@link ClientHttpResponse}。
	 * @return 执行的响应结果
	 * @throws IOException 如果发生 I/O 错误
	 */
	ClientHttpResponse execute() throws IOException;

}
