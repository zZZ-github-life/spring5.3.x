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

package org.springframework.http.client.support;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;

import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.web.client.AsyncRestTemplate}
 * 和其他 HTTP 访问网关辅助类的基类，定义要操作的
 * {@link org.springframework.http.client.AsyncClientHttpRequestFactory}
 * 等公共属性。
 *
 * <p>不打算直接使用。请参阅
 * {@link org.springframework.web.client.AsyncRestTemplate}。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see org.springframework.web.client.AsyncRestTemplate
 * @deprecated as of Spring 5.0, with no direct replacement
 */
@Deprecated
public class AsyncHttpAccessor {

	/** 供子类使用的 Logger。 */
	protected final Log logger = HttpLogging.forLogName(getClass());

	@Nullable
	private org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory;


	/**
	 * 设置此访问器用于获取
	 * {@link org.springframework.http.client.ClientHttpRequest HttpRequests} 的请求工厂。
	 */
	public void setAsyncRequestFactory(
			org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory) {

		Assert.notNull(asyncRequestFactory, "AsyncClientHttpRequestFactory must not be null");
		this.asyncRequestFactory = asyncRequestFactory;
	}

	/**
	 * 返回此访问器用于获取
	 * {@link org.springframework.http.client.ClientHttpRequest HttpRequests} 的请求工厂。
	 */
	public org.springframework.http.client.AsyncClientHttpRequestFactory getAsyncRequestFactory() {
		Assert.state(this.asyncRequestFactory != null, "No AsyncClientHttpRequestFactory set");
		return this.asyncRequestFactory;
	}

	/**
	 * 通过此模板的
	 * {@link org.springframework.http.client.AsyncClientHttpRequestFactory} 创建新的
	 * {@link org.springframework.http.client.AsyncClientHttpRequest}。
	 * @param url the URL to connect to
	 * @param method the HTTP method to execute (GET, POST, etc.)
	 * @return the created request
	 * @throws IOException in case of I/O errors
	 */
	protected org.springframework.http.client.AsyncClientHttpRequest createAsyncRequest(URI url, HttpMethod method)
			throws IOException {

		org.springframework.http.client.AsyncClientHttpRequest request =
				getAsyncRequestFactory().createAsyncRequest(url, method);
		if (logger.isDebugEnabled()) {
			logger.debug("Created asynchronous " + method.name() + " request for \"" + url + "\"");
		}
		return request;
	}

}
