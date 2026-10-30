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
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 使用标准 JDK 设施的 {@link ClientHttpRequestFactory} 实现。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.net.HttpURLConnection
 * @see HttpComponentsClientHttpRequestFactory
 */
@SuppressWarnings("deprecation")
public class SimpleClientHttpRequestFactory implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory {

	private static final int DEFAULT_CHUNK_SIZE = 4096;


	@Nullable
	private Proxy proxy;

	private boolean bufferRequestBody = true;

	private int chunkSize = DEFAULT_CHUNK_SIZE;

	private int connectTimeout = -1;

	private int readTimeout = -1;

	private boolean outputStreaming = true;

	@Nullable
	private AsyncListenableTaskExecutor taskExecutor;


	/**
	 * 设置用于此请求工厂的 {@link Proxy}。
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * 指示此请求工厂是否应在内部缓冲
	 * {@linkplain ClientHttpRequest#getBody() 请求正文}。
	 * <p>默认值为 {@code true}。通过 POST 或 PUT 发送大量数据时，
	 * 建议将此属性更改为 {@code false}，以免
	 * 耗尽内存。这将导致 {@link ClientHttpRequest} 要么直接
	 * 流式传输到底层 {@link HttpURLConnection}（如果
	 * {@link org.springframework.http.HttpHeaders#getContentLength() Content-Length}
	 * 提前已知），要么使用"分块传输编码"
	 * （如果 {@code Content-Length} 提前未知）。
	 * @see #setChunkSize(int)
	 * @see HttpURLConnection#setFixedLengthStreamingMode(int)
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}

	/**
	 * 设置不在本地缓冲请求正文时
	 * 每个块写入的字节数。
	 * <p>请注意，仅当
	 * {@link #setBufferRequestBody(boolean) bufferRequestBody} 设置为 {@code false}，
	 * 且 {@link org.springframework.http.HttpHeaders#getContentLength() Content-Length}
	 * 提前未知时，才使用此参数。
	 * @see #setBufferRequestBody(boolean)
	 */
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	/**
	 * 设置底层 URLConnection 的连接超时（以毫秒为单位）。
	 * 超时值 0 表示无限超时。
	 * <p>默认值为系统的默认超时。
	 * @see URLConnection#setConnectTimeout(int)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 设置底层 URLConnection 的读取超时（以毫秒为单位）。
	 * 超时值 0 表示无限超时。
	 * <p>默认值为系统的默认超时。
	 * @see URLConnection#setReadTimeout(int)
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * 设置底层 URLConnection 是否可以设置为"输出流式"模式。
	 * 默认值为 {@code true}。
	 * <p>启用输出流式传输时，无法自动处理身份验证和重定向。
	 * 如果禁用输出流式传输，则底层连接的
	 * {@link HttpURLConnection#setFixedLengthStreamingMode} 和
	 * {@link HttpURLConnection#setChunkedStreamingMode} 方法将永远不会被调用。
	 * @param outputStreaming 是否启用输出流式传输
	 */
	public void setOutputStreaming(boolean outputStreaming) {
		this.outputStreaming = outputStreaming;
	}

	/**
	 * 设置此请求工厂的任务执行器。要为
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) 创建异步请求}，必须设置此属性。
	 * @param taskExecutor 任务执行器
	 */
	public void setTaskExecutor(AsyncListenableTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		prepareConnection(connection, httpMethod.name());

		if (this.bufferRequestBody) {
			return new SimpleBufferingClientHttpRequest(connection, this.outputStreaming);
		}
		else {
			return new SimpleStreamingClientHttpRequest(connection, this.chunkSize, this.outputStreaming);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>在调用此方法之前，需要设置 {@link #setTaskExecutor taskExecutor} 属性。
	 */
	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		Assert.state(this.taskExecutor != null, "Asynchronous execution requires TaskExecutor to be set");

		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		prepareConnection(connection, httpMethod.name());

		if (this.bufferRequestBody) {
			return new SimpleBufferingAsyncClientHttpRequest(
					connection, this.outputStreaming, this.taskExecutor);
		}
		else {
			return new SimpleStreamingAsyncClientHttpRequest(
					connection, this.chunkSize, this.outputStreaming, this.taskExecutor);
		}
	}

	/**
	 * 打开并返回到给定 URL 的连接。
	 * <p>默认实现使用给定的 {@linkplain #setProxy(java.net.Proxy) 代理}
	 * （如果有）来打开连接。
	 * @param url 要打开连接的 URL
	 * @param proxy 要使用的代理，可以为 {@code null}
	 * @return 已打开的连接
	 * @throws IOException 如果发生 I/O 错误
	 */
	protected HttpURLConnection openConnection(URL url, @Nullable Proxy proxy) throws IOException {
		URLConnection urlConnection = (proxy != null ? url.openConnection(proxy) : url.openConnection());
		if (!(urlConnection instanceof HttpURLConnection)) {
			throw new IllegalStateException(
					"HttpURLConnection required for [" + url + "] but got: " + urlConnection);
		}
		return (HttpURLConnection) urlConnection;
	}

	/**
	 * 用于准备给定 {@link HttpURLConnection} 的模板方法。
	 * <p>默认实现为输入和输出准备连接，并设置 HTTP 方法。
	 *
	 * @param connection 要准备的连接
	 * @param httpMethod HTTP 请求方法（{@code GET}、{@code POST} 等）
	 * @throws IOException 如果发生 I/O 错误
	 */
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		if (this.connectTimeout >= 0) {
			connection.setConnectTimeout(this.connectTimeout);
		}
		if (this.readTimeout >= 0) {
			connection.setReadTimeout(this.readTimeout);
		}

		boolean mayWrite =
				("POST".equals(httpMethod) || "PUT".equals(httpMethod) ||
						"PATCH".equals(httpMethod) || "DELETE".equals(httpMethod));

		connection.setDoInput(true);
		connection.setInstanceFollowRedirects("GET".equals(httpMethod));
		connection.setDoOutput(mayWrite);
		connection.setRequestMethod(httpMethod);
	}

}
