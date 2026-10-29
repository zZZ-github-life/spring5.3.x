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

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.protocol.HttpContext;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link HttpComponentsClientHttpRequestFactory} 的异步扩展。使用
 * <a href="https://hc.apache.org/httpcomponents-asyncclient-dev/">Apache HttpComponents
 * HttpAsyncClient 4.0</a> 创建请求。
 *
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @since 4.0
 * @see HttpAsyncClient
 * @deprecated as of Spring 5.0, in favor of
 * {@link org.springframework.http.client.reactive.HttpComponentsClientHttpConnector}
 */
@Deprecated
public class HttpComponentsAsyncClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory
		implements AsyncClientHttpRequestFactory, InitializingBean {

	private HttpAsyncClient asyncClient;


	/**
	 * 使用默认的 {@link HttpAsyncClient} 和 {@link HttpClient}
	 * 创建新的 {@code HttpComponentsAsyncClientHttpRequestFactory} 实例。
	 */
	public HttpComponentsAsyncClientHttpRequestFactory() {
		super();
		this.asyncClient = HttpAsyncClients.createSystem();
	}

	/**
	 * 使用给定的 {@link HttpAsyncClient} 实例和默认的 {@link HttpClient}
	 * 创建新的 {@code HttpComponentsAsyncClientHttpRequestFactory} 实例。
	 * @param asyncClient the HttpAsyncClient instance to use for this request factory
	 * @since 4.3.10
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpAsyncClient asyncClient) {
		super();
		this.asyncClient = asyncClient;
	}

	/**
	 * 使用给定的 {@link CloseableHttpAsyncClient} 实例和默认的 {@link HttpClient}
	 * 创建新的 {@code HttpComponentsAsyncClientHttpRequestFactory} 实例。
	 * @param asyncClient the CloseableHttpAsyncClient instance to use for this request factory
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(CloseableHttpAsyncClient asyncClient) {
		super();
		this.asyncClient = asyncClient;
	}

	/**
	 * 使用给定的 {@link HttpClient} 和 {@link HttpAsyncClient} 实例
	 * 创建新的 {@code HttpComponentsAsyncClientHttpRequestFactory} 实例。
	 * @param httpClient the HttpClient instance to use for this request factory
	 * @param asyncClient the HttpAsyncClient instance to use for this request factory
	 * @since 4.3.10
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpClient httpClient, HttpAsyncClient asyncClient) {
		super(httpClient);
		this.asyncClient = asyncClient;
	}

	/**
	 * 使用给定的 {@link CloseableHttpClient} 和 {@link CloseableHttpAsyncClient} 实例
	 * 创建新的 {@code HttpComponentsAsyncClientHttpRequestFactory} 实例。
	 * @param httpClient the CloseableHttpClient instance to use for this request factory
	 * @param asyncClient the CloseableHttpAsyncClient instance to use for this request factory
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(
			CloseableHttpClient httpClient, CloseableHttpAsyncClient asyncClient) {

		super(httpClient);
		this.asyncClient = asyncClient;
	}


	/**
	 * 设置用于
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) 同步执行}的 {@code HttpAsyncClient}。
	 * @since 4.3.10
	 * @see #setHttpClient(HttpClient)
	 */
	public void setAsyncClient(HttpAsyncClient asyncClient) {
		Assert.notNull(asyncClient, "HttpAsyncClient must not be null");
		this.asyncClient = asyncClient;
	}

	/**
	 * 返回用于
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) 同步执行}的 {@code HttpAsyncClient}。
	 * @since 4.3.10
	 * @see #getHttpClient()
	 */
	public HttpAsyncClient getAsyncClient() {
		return this.asyncClient;
	}

	/**
	 * 设置用于
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) 异步执行}的 {@code CloseableHttpAsyncClient}。
	 * @deprecated as of 4.3.10, in favor of {@link #setAsyncClient(HttpAsyncClient)}
	 */
	@Deprecated
	public void setHttpAsyncClient(CloseableHttpAsyncClient asyncClient) {
		this.asyncClient = asyncClient;
	}

	/**
	 * 返回用于
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) 异步执行}的 {@code CloseableHttpAsyncClient}。
	 * @deprecated as of 4.3.10, in favor of {@link #getAsyncClient()}
	 */
	@Deprecated
	public CloseableHttpAsyncClient getHttpAsyncClient() {
		Assert.state(this.asyncClient instanceof CloseableHttpAsyncClient,
				"No CloseableHttpAsyncClient - use getAsyncClient() instead");
		return (CloseableHttpAsyncClient) this.asyncClient;
	}


	@Override
	public void afterPropertiesSet() {
		startAsyncClient();
	}

	private HttpAsyncClient startAsyncClient() {
		HttpAsyncClient client = getAsyncClient();
		if (client instanceof CloseableHttpAsyncClient) {
			@SuppressWarnings("resource")
			CloseableHttpAsyncClient closeableAsyncClient = (CloseableHttpAsyncClient) client;
			if (!closeableAsyncClient.isRunning()) {
				closeableAsyncClient.start();
			}
		}
		return client;
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpAsyncClient client = startAsyncClient();

		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
		HttpContext context = createHttpContext(httpMethod, uri);
		if (context == null) {
			context = HttpClientContext.create();
		}

		// Request configuration not set in the context
		if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
			// Use request configuration given by the user, when available
			RequestConfig config = null;
			if (httpRequest instanceof Configurable) {
				config = ((Configurable) httpRequest).getConfig();
			}
			if (config == null) {
				config = createRequestConfig(client);
			}
			if (config != null) {
				context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
			}
		}

		return new HttpComponentsAsyncClientHttpRequest(client, httpRequest, context);
	}

	@Override
	public void destroy() throws Exception {
		try {
			super.destroy();
		}
		finally {
			HttpAsyncClient asyncClient = getAsyncClient();
			if (asyncClient instanceof Closeable) {
				((Closeable) asyncClient).close();
			}
		}
	}

}
