/*
 * Copyright 2002-2015 the original author or authors.
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
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * 用于装饰另一个请求工厂的 {@link ClientHttpRequestFactory} 实现的
 * 抽象基类。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public abstract class AbstractClientHttpRequestFactoryWrapper implements ClientHttpRequestFactory {

	private final ClientHttpRequestFactory requestFactory;


	/**
	 * 创建一个包装给定请求工厂的 {@code AbstractClientHttpRequestFactoryWrapper}。
	 * @param requestFactory 要包装的请求工厂
	 */
	protected AbstractClientHttpRequestFactoryWrapper(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");
		this.requestFactory = requestFactory;
	}


	/**
	 * 此实现仅使用传递给 {@linkplain #AbstractClientHttpRequestFactoryWrapper(ClientHttpRequestFactory) 构造函数}
	 * 的被包装请求工厂调用
	 * {@link #createRequest(URI, HttpMethod, ClientHttpRequestFactory)}。
	 */
	@Override
	public final ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequest(uri, httpMethod, this.requestFactory);
	}

	/**
	 * 通过传入的请求工厂为指定的 URI 和 HTTP 方法
	 * 创建新的 {@link ClientHttpRequest}。
	 * <p>由 {@link #createRequest(URI, HttpMethod)} 调用。
	 * @param uri 为其创建请求的 URI
	 * @param httpMethod 要执行的 HTTP 方法
	 * @param requestFactory 被包装的请求工厂
	 * @return 所创建的请求
	 * @throws IOException 如果发生 I/O 错误
	 */
	protected abstract ClientHttpRequest createRequest(
			URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) throws IOException;

}
