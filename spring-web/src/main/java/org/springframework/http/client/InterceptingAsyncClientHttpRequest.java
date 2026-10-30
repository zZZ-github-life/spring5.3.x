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
import java.util.Iterator;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 一个 {@link AsyncClientHttpRequest} 包装器，它在执行实际请求之前
 * 调用已注册的拦截器。
 *
 * @author Jakub Narloch
 * @author Rossen Stoyanchev
 * @see InterceptingAsyncClientHttpRequestFactory
 * @deprecated as of Spring 5.0, with no direct replacement
 */
@Deprecated
class InterceptingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private AsyncClientHttpRequestFactory requestFactory;

	private List<AsyncClientHttpRequestInterceptor> interceptors;

	private URI uri;

	private HttpMethod httpMethod;


	/**
	 * 创建 {@link InterceptingAsyncClientHttpRequest} 的新实例。
	 * @param requestFactory 异步请求工厂
	 * @param interceptors 拦截器列表
	 * @param uri 请求 URI
	 * @param httpMethod HTTP 方法
	 */
	public InterceptingAsyncClientHttpRequest(AsyncClientHttpRequestFactory requestFactory,
			List<AsyncClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod httpMethod) {

		this.requestFactory = requestFactory;
		this.interceptors = interceptors;
		this.uri = uri;
		this.httpMethod = httpMethod;
	}


	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] body)
			throws IOException {

		return new AsyncRequestExecution().executeAsync(this, body);
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public String getMethodValue() {
		return this.httpMethod.name();
	}

	@Override
	public URI getURI() {
		return this.uri;
	}


	private class AsyncRequestExecution implements AsyncClientHttpRequestExecution {

		private Iterator<AsyncClientHttpRequestInterceptor> iterator;

		public AsyncRequestExecution() {
			this.iterator = interceptors.iterator();
		}

		@Override
		public ListenableFuture<ClientHttpResponse> executeAsync(HttpRequest request, byte[] body)
				throws IOException {

			if (this.iterator.hasNext()) {
				AsyncClientHttpRequestInterceptor interceptor = this.iterator.next();
				return interceptor.intercept(request, body, this);
			}
			else {
				URI uri = request.getURI();
				HttpMethod method = request.getMethod();
				HttpHeaders headers = request.getHeaders();

				Assert.state(method != null, "No standard HTTP method");
				AsyncClientHttpRequest delegate = requestFactory.createAsyncRequest(uri, method);
				delegate.getHeaders().putAll(headers);
				if (body.length > 0) {
					StreamUtils.copy(body, delegate.getBody());
				}

				return delegate.executeAsync();
			}
		}
	}

}
