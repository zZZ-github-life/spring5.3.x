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
 * 表示客户端 HTTP 请求执行的上下文。
 *
 * <p>用于调用拦截器链中的下一个拦截器，或者
 * 如果当前拦截器是最后一个，则执行请求本身。
 *
 * @author Jakub Narloch
 * @author Rossen Stoyanchev
 * @since 4.3
 * @see AsyncClientHttpRequestInterceptor
 * @deprecated as of Spring 5.0, in favor of
 * {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}
 */
@Deprecated
public interface AsyncClientHttpRequestExecution {

	/**
	 * 通过调用链中的下一个拦截器或向远程服务执行请求
	 * 来恢复请求执行。
	 * @param request HTTP 请求，包含 HTTP 方法和头信息
	 * @param body 请求体
	 * @return 对应的 Future 句柄
	 * @throws IOException 如果发生 I/O 错误
	 */
	ListenableFuture<ClientHttpResponse> executeAsync(HttpRequest request, byte[] body) throws IOException;

}
