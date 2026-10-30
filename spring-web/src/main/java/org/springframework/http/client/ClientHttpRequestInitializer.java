/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.http.client.support.HttpAccessor;

/**
 * 在 {@link ClientHttpRequest} 被使用之前对其进行初始化的
 * 回调接口。
 *
 * <p>通常与 {@link HttpAccessor} 及其子类（例如
 * {@link org.springframework.web.client.RestTemplate RestTemplate}）一起使用，以便为每个请求应用
 * 一致的设置或头部。
 *
 * <p>与 {@link ClientHttpRequestInterceptor} 不同，此接口可以应用
 * 自定义设置，而无需将整个请求正文读入内存。
 *
 * @author Phillip Webb
 * @since 5.2
 * @see HttpAccessor#getClientHttpRequestInitializers()
 */
@FunctionalInterface
public interface ClientHttpRequestInitializer {

	/**
	 * 初始化给定的客户端 HTTP 请求。
	 * @param request 要配置的请求
	 */
	void initialize(ClientHttpRequest request);

}
