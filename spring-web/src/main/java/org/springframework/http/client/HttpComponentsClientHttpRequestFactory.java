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

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.function.BiFunction;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 使用 <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents
 * HttpClient</a> 创建请求的 {@link org.springframework.http.client.ClientHttpRequestFactory} 实现。
 *
 * <p>允许使用预配置的 {@link HttpClient} 实例 -
 * 可能带有身份验证、HTTP 连接池等。
 *
 * <p><b>注意：</b>从 Spring 4.0 开始，需要 Apache HttpComponents 4.3 或更高版本。
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 */
public class HttpComponentsClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

	private HttpClient httpClient;

	@Nullable
	private RequestConfig requestConfig;

	private boolean bufferRequestBody = true;

	@Nullable
	private BiFunction<HttpMethod, URI, HttpContext> httpContextFactory;


	/**
	 * 使用基于系统属性的默认 {@link HttpClient}
	 * 创建新的 {@code HttpComponentsClientHttpRequestFactory} 实例。
	 */
	public HttpComponentsClientHttpRequestFactory() {
		this.httpClient = HttpClients.createSystem();
	}

	/**
	 * 使用给定的 {@link HttpClient} 实例
	 * 创建新的 {@code HttpComponentsClientHttpRequestFactory} 实例。
	 * @param httpClient 此请求工厂要使用的 HttpClient 实例
	 */
	public HttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
		this.httpClient = httpClient;
	}


	/**
	 * 设置用于
	 * {@linkplain #createRequest(URI, HttpMethod) 同步执行}的 {@code HttpClient}。
	 */
	public void setHttpClient(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient must not be null");
		this.httpClient = httpClient;
	}

	/**
	 * 返回用于
	 * {@linkplain #createRequest(URI, HttpMethod) 同步执行}的 {@code HttpClient}。
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * 设置底层 {@link RequestConfig} 的连接超时。
	 * 超时值 0 表示无限超时。
	 * <p>可以通过在自定义 {@link HttpClient} 上指定
	 * {@link RequestConfig} 实例来配置其他属性。
	 * <p>此选项不影响 SSL 握手或 CONNECT 请求的连接超时；
	 * 为此，需要在 {@link HttpClient} 自身上使用
	 * {@link org.apache.http.config.SocketConfig}。
	 * @param timeout 以毫秒为单位的超时值
	 * @see RequestConfig#getConnectTimeout()
	 * @see org.apache.http.config.SocketConfig#getSoTimeout
	 */
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = requestConfigBuilder().setConnectTimeout(timeout).build();
	}

	/**
	 * 使用底层 {@link RequestConfig} 设置从连接管理器请求连接时
	 * 使用的超时（以毫秒为单位）。
	 * 超时值 0 表示无限超时。
	 * <p>可以通过在自定义 {@link HttpClient} 上指定
	 * {@link RequestConfig} 实例来配置其他属性。
	 * @param connectionRequestTimeout 以毫秒为单位的请求连接超时值
	 * @see RequestConfig#getConnectionRequestTimeout()
	 */
	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.requestConfig = requestConfigBuilder()
				.setConnectionRequestTimeout(connectionRequestTimeout).build();
	}

	/**
	 * 设置底层 {@link RequestConfig} 的套接字读取超时。
	 * 超时值 0 表示无限超时。
	 * <p>可以通过在自定义 {@link HttpClient} 上指定
	 * {@link RequestConfig} 实例来配置其他属性。
	 * @param timeout 以毫秒为单位的超时值
	 * @see RequestConfig#getSocketTimeout()
	 */
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = requestConfigBuilder().setSocketTimeout(timeout).build();
	}

	/**
	 * 指示此请求工厂是否应在内部缓冲请求正文。
	 * <p>默认值为 {@code true}。通过 POST 或 PUT 发送大量数据时，
	 * 建议将此属性更改为 {@code false}，以免耗尽内存。
	 * @since 4.0
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}

	/**
	 * 配置工厂为每个请求预先创建 {@link HttpContext}。
	 * <p>例如，在双向 TLS 身份验证中，这可能很有用：为每个客户端证书
	 * 使用不同的 {@code RestTemplate}，以便通过给定 {@code RestTemplate} 实例
	 * 发出的所有调用都关联到同一客户端身份。{@link HttpClientContext#setUserToken(Object)}
	 * 可用于为所有请求指定固定的用户令牌。
	 * @param httpContextFactory 要使用的上下文工厂
	 * @since 5.2.7
	 */
	public void setHttpContextFactory(BiFunction<HttpMethod, URI, HttpContext> httpContextFactory) {
		this.httpContextFactory = httpContextFactory;
	}

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpClient client = getHttpClient();

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

		if (this.bufferRequestBody) {
			return new HttpComponentsClientHttpRequest(client, httpRequest, context);
		}
		else {
			return new HttpComponentsStreamingClientHttpRequest(client, httpRequest, context);
		}
	}


	/**
	 * 返回用于修改工厂级 {@link RequestConfig} 的构建器。
	 * @since 4.2
	 */
	private RequestConfig.Builder requestConfigBuilder() {
		return (this.requestConfig != null ? RequestConfig.copy(this.requestConfig) : RequestConfig.custom());
	}

	/**
	 * 创建要与给定客户端一起使用的默认 {@link RequestConfig}。
	 * 可以返回 {@code null} 以表示不应设置自定义请求配置，
	 * 而应使用 {@link HttpClient} 的默认值。
	 * <p>默认实现会尝试将客户端的默认值
	 * 与此工厂实例的本地自定义配置（如果有）合并。
	 * @param client 要检查的 {@link HttpClient}（或 {@code HttpAsyncClient}）
	 * @return 实际要使用的 RequestConfig（可能为 {@code null}）
	 * @since 4.2
	 * @see #mergeRequestConfig(RequestConfig)
	 */
	@Nullable
	protected RequestConfig createRequestConfig(Object client) {
		if (client instanceof Configurable) {
			RequestConfig clientRequestConfig = ((Configurable) client).getConfig();
			return mergeRequestConfig(clientRequestConfig);
		}
		return this.requestConfig;
	}

	/**
	 * 如有必要，将给定的 {@link HttpClient} 级 {@link RequestConfig}
	 * 与工厂级 {@link RequestConfig} 合并。
	 * @param clientConfig 当前对象持有的配置
	 * @return 合并后的请求配置
	 * @since 4.2
	 */
	protected RequestConfig mergeRequestConfig(RequestConfig clientConfig) {
		if (this.requestConfig == null) {  // nothing to merge
			return clientConfig;
		}

		RequestConfig.Builder builder = RequestConfig.copy(clientConfig);
		int connectTimeout = this.requestConfig.getConnectTimeout();
		if (connectTimeout >= 0) {
			builder.setConnectTimeout(connectTimeout);
		}
		int connectionRequestTimeout = this.requestConfig.getConnectionRequestTimeout();
		if (connectionRequestTimeout >= 0) {
			builder.setConnectionRequestTimeout(connectionRequestTimeout);
		}
		int socketTimeout = this.requestConfig.getSocketTimeout();
		if (socketTimeout >= 0) {
			builder.setSocketTimeout(socketTimeout);
		}
		return builder.build();
	}

	/**
	 * 为给定的 HTTP 方法和 URI 规范创建 Commons HttpMethodBase 对象。
	 * @param httpMethod HTTP 方法
	 * @param uri 该 URI
	 * @return Commons 的 HttpMethodBase 对象
	 */
	protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
		switch (httpMethod) {
			case GET:
				return new HttpGet(uri);
			case HEAD:
				return new HttpHead(uri);
			case POST:
				return new HttpPost(uri);
			case PUT:
				return new HttpPut(uri);
			case PATCH:
				return new HttpPatch(uri);
			case DELETE:
				return new HttpDelete(uri);
			case OPTIONS:
				return new HttpOptions(uri);
			case TRACE:
				return new HttpTrace(uri);
			default:
				throw new IllegalArgumentException("Invalid HTTP method: " + httpMethod);
		}
	}

	/**
	 * 允许在将 {@link HttpUriRequest} 作为
	 * {@link HttpComponentsClientHttpRequest} 的一部分返回之前对其进行操作的模板方法。
	 * <p>默认实现为空。
	 * @param request 要处理的请求
	 */
	protected void postProcessHttpRequest(HttpUriRequest request) {
	}

	/**
	 * 为给定的 HTTP 方法和 URI 创建 {@link HttpContext} 的模板方法。
	 * <p>默认实现返回 {@code null}。
	 * @param httpMethod HTTP 方法
	 * @param uri 该 URI
	 * @return HTTP 上下文
	 */
	@Nullable
	protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
		return (this.httpContextFactory != null ? this.httpContextFactory.apply(httpMethod, uri) : null);
	}


	/**
	 * 关闭底层
	 * {@link org.apache.http.conn.HttpClientConnectionManager ClientConnectionManager}
	 * 连接池（如果有）的关闭钩子。
	 */
	@Override
	public void destroy() throws Exception {
		HttpClient httpClient = getHttpClient();
		if (httpClient instanceof Closeable) {
			((Closeable) httpClient).close();
		}
	}


	/**
	 * {@link org.apache.http.client.methods.HttpDelete} 的替代方案，它
	 * 扩展 {@link org.apache.http.client.methods.HttpEntityEnclosingRequestBase}
	 * 而不是 {@link org.apache.http.client.methods.HttpRequestBase}，
	 * 因此允许带请求正文的 HTTP DELETE。用于 RestTemplate
	 * 的 exchange 方法，这些方法允许将 HTTP DELETE 与实体组合使用。
	 * @since 4.1.2
	 */
	private static class HttpDelete extends HttpEntityEnclosingRequestBase {

		public HttpDelete(URI uri) {
			super();
			setURI(uri);
		}

		@Override
		public String getMethod() {
			return "DELETE";
		}
	}

}
