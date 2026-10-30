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

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

/**
 * {@link ClientHttpRequestFactory} 包装器，支持
 * {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors}。
 *
 * @author Arjen Poutsma
 * @since 3.1
 * @see ClientHttpRequestFactory
 * @see ClientHttpRequestInterceptor
 */
public class InterceptingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	private final List<ClientHttpRequestInterceptor> interceptors;


	/**
	 * 使用给定参数创建 {@code InterceptingClientHttpRequestFactory} 的新实例。
	 * @param requestFactory 要包装的请求工厂
	 * @param interceptors 要应用的拦截器（可以为 {@code null}）
	 */
	public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
			@Nullable List<ClientHttpRequestInterceptor> interceptors) {

		super(requestFactory);
		this.interceptors = (interceptors != null ? interceptors : Collections.emptyList());
	}


	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) {
		return new InterceptingClientHttpRequest(requestFactory, this.interceptors, uri, httpMethod);
	}

}
