/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.http.HttpRequest;

/**
 * 表示客户端 HTTP 请求执行的上下文。
 *
 * <p>用于调用拦截器链中的下一个拦截器，
 * 或者如果当前拦截器是最后一个，则执行请求本身。
 *
 * @author Arjen Poutsma
 * @since 3.1
 * @see ClientHttpRequestInterceptor
 */
@FunctionalInterface
public interface ClientHttpRequestExecution {

	/**
	 * 使用给定的请求属性和正文执行请求，
	 * 并返回响应。
	 * @param request 请求，包含方法、URI 和头信息
	 * @param body 要执行的请求体
	 * @return 响应
	 * @throws IOException 如果发生 I/O 错误
	 */
	ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException;

}
