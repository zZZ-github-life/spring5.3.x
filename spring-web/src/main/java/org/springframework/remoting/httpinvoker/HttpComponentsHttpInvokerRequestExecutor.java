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

package org.springframework.remoting.httpinvoker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.remoting.httpinvoker.HttpInvokerRequestExecutor} 实现，使用
 * <a href="https://hc.apache.org/httpcomponents-client-ga/httpclient/">Apache HttpComponents HttpClient</a>
 * 执行 POST 请求。
 *
 * <p>允许使用预先配置的 {@link org.apache.http.client.HttpClient}
 * 实例，该实例可能带有身份验证、HTTP 连接池等配置。
 * 同时易于子类化，并提供了特定的模板方法。
 *
 * <p>从 Spring 4.1 起，此请求执行器要求 Apache HttpComponents 4.3 或更高版本。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 * @see org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class HttpComponentsHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {

	private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 100;

	private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 5;

	private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (60 * 1000);


	private HttpClient httpClient;

	@Nullable
	private RequestConfig requestConfig;


	/**
	 * 使用默认的 {@link HttpClient}（采用默认的
	 * {@code org.apache.http.impl.conn.PoolingClientConnectionManager}）创建
	 * HttpComponentsHttpInvokerRequestExecutor 的新实例。
	 */
	public HttpComponentsHttpInvokerRequestExecutor() {
		this(createDefaultHttpClient(), RequestConfig.custom()
				.setSocketTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS).build());
	}

	/**
	 * 使用给定的 {@link HttpClient} 实例创建
	 * HttpComponentsClientHttpRequestFactory 的新实例。
	 * @param httpClient 要用于此请求执行器的 HttpClient 实例
	 */
	public HttpComponentsHttpInvokerRequestExecutor(HttpClient httpClient) {
		this(httpClient, null);
	}

	private HttpComponentsHttpInvokerRequestExecutor(HttpClient httpClient, @Nullable RequestConfig requestConfig) {
		this.httpClient = httpClient;
		this.requestConfig = requestConfig;
	}


	private static HttpClient createDefaultHttpClient() {
		Registry<ConnectionSocketFactory> schemeRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", SSLConnectionSocketFactory.getSocketFactory())
				.build();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(schemeRegistry);
		connectionManager.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
		connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);

		return HttpClientBuilder.create().setConnectionManager(connectionManager).build();
	}


	/**
	 * 设置此请求执行器使用的 {@link HttpClient} 实例。
	 */
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * 返回此请求执行器使用的 {@link HttpClient} 实例。
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * 设置底层 HttpClient 的连接超时时间。
	 * 超时值为 0 表示无限超时。
	 * <p>可以通过在自定义的 {@link HttpClient} 上指定
	 * {@link RequestConfig} 实例来配置其他属性。
	 * @param timeout 以毫秒为单位的超时值
	 * @see RequestConfig#getConnectTimeout()
	 */
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = cloneRequestConfig().setConnectTimeout(timeout).build();
	}

	/**
	 * 设置使用底层 HttpClient 从连接管理器请求连接时使用的超时时间（毫秒）。
	 * 超时值为 0 表示无限超时。
	 * <p>可以通过在自定义的 {@link HttpClient} 上指定
	 * {@link RequestConfig} 实例来配置其他属性。
	 * @param connectionRequestTimeout 以毫秒为单位的连接请求超时值
	 * @see RequestConfig#getConnectionRequestTimeout()
	 */
	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.requestConfig = cloneRequestConfig().setConnectionRequestTimeout(connectionRequestTimeout).build();
	}

	/**
	 * 设置底层 HttpClient 的 socket 读取超时时间。
	 * 超时值为 0 表示无限超时。
	 * <p>可以通过在自定义的 {@link HttpClient} 上指定
	 * {@link RequestConfig} 实例来配置其他属性。
	 * @param timeout 以毫秒为单位的超时值
	 * @see #DEFAULT_READ_TIMEOUT_MILLISECONDS
	 * @see RequestConfig#getSocketTimeout()
	 */
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = cloneRequestConfig().setSocketTimeout(timeout).build();
	}

	private RequestConfig.Builder cloneRequestConfig() {
		return (this.requestConfig != null ? RequestConfig.copy(this.requestConfig) : RequestConfig.custom());
	}


	/**
	 * 通过 HttpClient 执行给定的请求。
	 * <p>此方法实现了基本的处理流程：
	 * 实际工作发生在此类的模板方法中。
	 * @see #createHttpPost
	 * @see #setRequestBody
	 * @see #executeHttpPost
	 * @see #validateResponse
	 * @see #getResponseBody
	 */
	@Override
	protected RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException, ClassNotFoundException {

		HttpPost postMethod = createHttpPost(config);
		setRequestBody(config, postMethod, baos);
		try {
			HttpResponse response = executeHttpPost(config, getHttpClient(), postMethod);
			validateResponse(config, response);
			InputStream responseBody = getResponseBody(config, response);
			return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
		}
		finally {
			postMethod.releaseConnection();
		}
	}

	/**
	 * 为给定的配置创建 HttpPost。
	 * <p>默认实现创建一个标准的 HttpPost，
	 * 并将 "application/x-java-serialized-object" 作为 "Content-Type" 头。
	 * @param config 指定目标服务的 HTTP invoker 配置
	 * @return HttpPost 实例
	 * @throws java.io.IOException 如果由 I/O 方法抛出则传播
	 */
	protected HttpPost createHttpPost(HttpInvokerClientConfiguration config) throws IOException {
		HttpPost httpPost = new HttpPost(config.getServiceUrl());

		RequestConfig requestConfig = createRequestConfig(config);
		if (requestConfig != null) {
			httpPost.setConfig(requestConfig);
		}

		LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null) {
				httpPost.addHeader(HTTP_HEADER_ACCEPT_LANGUAGE, locale.toLanguageTag());
			}
		}

		if (isAcceptGzipEncoding()) {
			httpPost.addHeader(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}

		return httpPost;
	}

	/**
	 * 为给定的配置创建 {@link RequestConfig}。可以返回 {@code null}
	 * 表示不应设置自定义请求配置，而应使用 {@link HttpClient} 的默认配置。
	 * <p>默认实现会尝试将客户端的默认配置与此实例的
	 * 本地自定义配置（如果有）合并。
	 * @param config 指定目标服务的 HTTP invoker 配置
	 * @return 要使用的 RequestConfig
	 */
	@Nullable
	protected RequestConfig createRequestConfig(HttpInvokerClientConfiguration config) {
		HttpClient client = getHttpClient();
		if (client instanceof Configurable) {
			RequestConfig clientRequestConfig = ((Configurable) client).getConfig();
			return mergeRequestConfig(clientRequestConfig);
		}
		return this.requestConfig;
	}

	private RequestConfig mergeRequestConfig(RequestConfig defaultRequestConfig) {
		if (this.requestConfig == null) {  // nothing to merge
			return defaultRequestConfig;
		}

		RequestConfig.Builder builder = RequestConfig.copy(defaultRequestConfig);
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
	 * 将给定的已序列化远程调用设置为请求体。
	 * <p>默认实现仅将已序列化的调用设置为
	 * HttpPost 的请求体。可重写此方法，例如以特定编码写入，
	 * 并可能设置适当的 HTTP 请求头。
	 * @param config 指定目标服务的 HTTP invoker 配置
	 * @param httpPost 要为其设置请求体的 HttpPost
	 * @param baos 包含序列化后 RemoteInvocation 对象的
	 * ByteArrayOutputStream
	 * @throws java.io.IOException 如果由 I/O 方法抛出则传播
	 */
	protected void setRequestBody(
			HttpInvokerClientConfiguration config, HttpPost httpPost, ByteArrayOutputStream baos)
			throws IOException {

		ByteArrayEntity entity = new ByteArrayEntity(baos.toByteArray());
		entity.setContentType(getContentType());
		httpPost.setEntity(entity);
	}

	/**
	 * 执行给定的 HttpPost 实例。
	 * @param config 指定目标服务的 HTTP invoker 配置
	 * @param httpClient 要对其执行请求的 HttpClient
	 * @param httpPost 要执行的 HttpPost
	 * @return 得到的 HttpResponse
	 * @throws java.io.IOException 如果由 I/O 方法抛出则传播
	 */
	protected HttpResponse executeHttpPost(
			HttpInvokerClientConfiguration config, HttpClient httpClient, HttpPost httpPost)
			throws IOException {

		return httpClient.execute(httpPost);
	}

	/**
	 * 验证 HttpPost 对象中包含的给定响应，
	 * 如果响应不对应于成功的 HTTP 响应，则抛出异常。
	 * <p>默认实现拒绝 2xx 之外的任何 HTTP 状态码，以避免
	 * 解析响应体并尝试从损坏的流中反序列化。
	 * @param config 指定目标服务的 HTTP invoker 配置
	 * @param response 要校验的得到的 HttpResponse
	 * @throws java.io.IOException 如果校验失败则抛出
	 */
	protected void validateResponse(HttpInvokerClientConfiguration config, HttpResponse response)
			throws IOException {

		StatusLine status = response.getStatusLine();
		if (status.getStatusCode() >= 300) {
			throw new NoHttpResponseException(
					"Did not receive successful HTTP response: status code = " + status.getStatusCode() +
					", status message = [" + status.getReasonPhrase() + "]");
		}
	}

	/**
	 * 从给定的已执行远程调用请求中提取响应体。
	 * <p>默认实现仅获取 HttpPost 的响应体流。
	 * 如果响应被识别为 GZIP 响应，则会将 InputStream 包装在
	 * GZIPInputStream 中。
	 * @param config 指定目标服务的 HTTP invoker 配置
	 * @param httpResponse 要从中读取响应体的得到的 HttpResponse
	 * @return 响应体的 InputStream
	 * @throws java.io.IOException 如果由 I/O 方法抛出则传播
	 * @see #isGzipResponse
	 * @see java.util.zip.GZIPInputStream
	 */
	protected InputStream getResponseBody(HttpInvokerClientConfiguration config, HttpResponse httpResponse)
			throws IOException {

		if (isGzipResponse(httpResponse)) {
			return new GZIPInputStream(httpResponse.getEntity().getContent());
		}
		else {
			return httpResponse.getEntity().getContent();
		}
	}

	/**
	 * 确定给定的响应是否为 GZIP 响应。
	 * <p>默认实现检查 HTTP "Content-Encoding"
	 * 头是否包含 "gzip"（不区分大小写）。
	 * @param httpResponse 要检查的得到的 HttpResponse
	 * @return 给定响应是否表示 GZIP 响应
	 */
	protected boolean isGzipResponse(HttpResponse httpResponse) {
		Header encodingHeader = httpResponse.getFirstHeader(HTTP_HEADER_CONTENT_ENCODING);
		return (encodingHeader != null && encodingHeader.getValue() != null &&
				encodingHeader.getValue().toLowerCase().contains(ENCODING_GZIP));
	}

}
