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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;

/**
 * 用于 {@link ClientHttpRequestFactory} 的包装器，在内存中缓冲
 * 所有传出和传入流。
 *
 * <p>使用此包装器可以多次读取
 * {@linkplain ClientHttpResponse#getBody() 响应正文}。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public class BufferingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	/**
	 * 为给定的 {@link ClientHttpRequestFactory} 创建缓冲包装器。
	 * @param requestFactory 要包装的目标请求工厂
	 */
	public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
		super(requestFactory);
	}


	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory)
			throws IOException {

		ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
		if (shouldBuffer(uri, httpMethod)) {
			return new BufferingClientHttpRequestWrapper(request);
		}
		else {
			return request;
		}
	}

	/**
	 * 指示给定 URI 和方法的请求/响应交换是否应在内存中缓冲。
	 * <p>默认实现对所有 URI 和方法返回 {@code true}。
	 * 子类可以覆盖此方法以更改此行为。
	 * @param uri 该 URI
	 * @param httpMethod HTTP 方法
	 * @return 如果应缓冲该交换则返回 {@code true}；否则返回 {@code false}
	 */
	protected boolean shouldBuffer(URI uri, HttpMethod httpMethod) {
		return true;
	}

}
