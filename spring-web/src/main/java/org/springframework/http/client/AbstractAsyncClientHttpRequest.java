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
import java.io.OutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 用于 {@link AsyncClientHttpRequest} 的抽象基类，确保头部和正文
 * 不会被多次写入。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @deprecated as of Spring 5.0, in favor of {@link org.springframework.http.client.reactive.AbstractClientHttpRequest}
 */
@Deprecated
abstract class AbstractAsyncClientHttpRequest implements AsyncClientHttpRequest {

	private final HttpHeaders headers = new HttpHeaders();

	private boolean executed = false;


	@Override
	public final HttpHeaders getHeaders() {
		return (this.executed ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public final OutputStream getBody() throws IOException {
		assertNotExecuted();
		return getBodyInternal(this.headers);
	}

	@Override
	public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
		assertNotExecuted();
		ListenableFuture<ClientHttpResponse> result = executeInternal(this.headers);
		this.executed = true;
		return result;
	}

	/**
	 * 断言此请求尚未被 {@linkplain #executeAsync() 执行}。
	 * @throws IllegalStateException if this request has been executed
	 */
	protected void assertNotExecuted() {
		Assert.state(!this.executed, "ClientHttpRequest already executed");
	}


	/**
	 * 返回正文的抽象模板方法。
	 * @param headers the HTTP headers
	 * @return the body output stream
	 */
	protected abstract OutputStream getBodyInternal(HttpHeaders headers) throws IOException;

	/**
	 * 将给定头部和内容写入 HTTP 请求的抽象模板方法。
	 * @param headers the HTTP headers
	 * @return the response object for the executed request
	 */
	protected abstract ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers)
			throws IOException;

}
