/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 拦截客户端 HTTP 请求。此接口的实现可以通过
 * {@linkplain org.springframework.web.client.AsyncRestTemplate#setInterceptors registered 注册}
 * 到 {@link org.springframework.web.client.AsyncRestTemplate}，以修改发出的 {@link HttpRequest}
 * 和/或注册以借助
 * {@link org.springframework.util.concurrent.ListenableFutureAdapter} 修改传入的
 * {@link ClientHttpResponse}。
 *
 * <p>拦截器的主要入口点是 {@link #intercept}。
 *
 * @author Jakub Narloch
 * @author Rossen Stoyanchev
 * @since 4.3
 * @see org.springframework.web.client.AsyncRestTemplate
 * @see org.springframework.http.client.support.InterceptingAsyncHttpAccessor
 * @deprecated as of Spring 5.0, in favor of
 * {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}
 */
@Deprecated
public interface AsyncClientHttpRequestInterceptor {

	/**
	 * 拦截给定请求并返回响应的 future。给定的
	 * {@link AsyncClientHttpRequestExecution} 允许拦截器将
	 * 请求传递给链中的下一个实体。
	 * <p>实现可以遵循以下模式：
	 * <ol>
	 * <li>检查 {@linkplain HttpRequest 请求} 和正文</li>
	 * <li>可选地 {@linkplain org.springframework.http.client.support.HttpRequestWrapper 包装}
	 * 请求以过滤 HTTP 属性。</li>
	 * <li>可选地修改请求正文。</li>
	 * <li>执行以下操作之一：
	 * <ul>
	 * <li>通过 {@link ClientHttpRequestExecution} 执行请求</li>
	 * <li>不执行请求以完全阻止执行</li>
	 * </ul>
	 * <li>可选地借助 {@link org.springframework.util.concurrent.ListenableFutureAdapter ListenableFutureAdapter}
	 * 调整响应以过滤 HTTP 属性。</li>
	 * </ol>
	 * @param request the request, containing method, URI, and headers
	 * @param body the body of the request
	 * @param execution the request execution
	 * @return the response future
	 * @throws IOException in case of I/O errors
	 */
	ListenableFuture<ClientHttpResponse> intercept(HttpRequest request, byte[] body,
			AsyncClientHttpRequestExecution execution) throws IOException;

}
