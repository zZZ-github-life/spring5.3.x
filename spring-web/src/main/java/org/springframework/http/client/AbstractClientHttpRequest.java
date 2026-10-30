/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于 {@link ClientHttpRequest} 的抽象基类，确保头部
 * 和正文不会被多次写入。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

	private final HttpHeaders headers = new HttpHeaders();

	private boolean executed = false;

	@Nullable
	private HttpHeaders readOnlyHeaders;


	@Override
	public final HttpHeaders getHeaders() {
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		}
		else if (this.executed) {
			this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
			return this.readOnlyHeaders;
		}
		else {
			return this.headers;
		}
	}

	@Override
	public final OutputStream getBody() throws IOException {
		assertNotExecuted();
		return getBodyInternal(this.headers);
	}

	@Override
	public final ClientHttpResponse execute() throws IOException {
		assertNotExecuted();
		ClientHttpResponse result = executeInternal(this.headers);
		this.executed = true;
		return result;
	}

	/**
	 * 断言此请求尚未被 {@linkplain #execute() 执行}。
	 * @throws IllegalStateException 如果此请求已被执行
	 */
	protected void assertNotExecuted() {
		Assert.state(!this.executed, "ClientHttpRequest already executed");
	}


	/**
	 * 返回正文的抽象模板方法。
	 * @param headers HTTP 头
	 * @return 请求体输出流
	 */
	protected abstract OutputStream getBodyInternal(HttpHeaders headers) throws IOException;

	/**
	 * 将给定头部和内容写入 HTTP 请求的抽象模板方法。
	 * @param headers HTTP 头
	 * @return 所执行请求的响应对象
	 */
	protected abstract ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException;

}
