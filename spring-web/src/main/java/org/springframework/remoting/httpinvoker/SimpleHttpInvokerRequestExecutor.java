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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * {@link org.springframework.remoting.httpinvoker.HttpInvokerRequestExecutor} 实现，
 * 使用标准 Java 设施执行 POST 请求，不支持 HTTP 身份验证
 * 或高级配置选项。
 *
 * <p>设计上易于子类化和自定义特定的模板方法。不过，对于更复杂的需求，
 * 请考虑 {@code HttpComponentsHttpInvokerRequestExecutor}：
 * 标准的 {@link HttpURLConnection} 类在功能上相当有限。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see java.net.HttpURLConnection
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class SimpleHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {

	private int connectTimeout = -1;

	private int readTimeout = -1;


	/**
	 * 设置底层 URLConnection 的连接超时时间（毫秒）。
	 * 超时值为 0 表示无限超时。
	 * <p>默认为系统的默认超时时间。
	 * @see URLConnection#setConnectTimeout(int)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 设置底层 URLConnection 的读取超时时间（毫秒）。
	 * 超时值为 0 表示无限超时。
	 * <p>默认为系统的默认超时时间。
	 * @see URLConnection#setReadTimeout(int)
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}


	/**
	 * 通过标准 {@link HttpURLConnection} 执行给定的请求。
	 * <p>此方法实现了基本的处理流程：
	 * 实际工作发生在此类的模板方法中。
	 * @see #openConnection
	 * @see #prepareConnection
	 * @see #writeRequestBody
	 * @see #validateResponse
	 * @see #readResponseBody
	 */
	@Override
	protected RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException, ClassNotFoundException {

		HttpURLConnection con = openConnection(config);
		prepareConnection(con, baos.size());
		writeRequestBody(config, con, baos);
		validateResponse(config, con);
		InputStream responseBody = readResponseBody(config, con);

		return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
	}

	/**
	 * 为给定的远程调用请求打开一个 {@link HttpURLConnection}。
	 * @param config the HTTP invoker configuration that specifies the
	 * target service
	 * @return the HttpURLConnection for the given request
	 * @throws IOException if thrown by I/O methods
	 * @see java.net.URL#openConnection()
	 */
	protected HttpURLConnection openConnection(HttpInvokerClientConfiguration config) throws IOException {
		URLConnection con = new URL(config.getServiceUrl()).openConnection();
		if (!(con instanceof HttpURLConnection)) {
			throw new IOException(
					"Service URL [" + config.getServiceUrl() + "] does not resolve to an HTTP connection");
		}
		return (HttpURLConnection) con;
	}

	/**
	 * 准备给定的 HTTP 连接。
	 * <p>默认实现将方法指定为 POST，
	 * 将 "application/x-java-serialized-object" 作为 "Content-Type" 头，
	 * 并将给定的内容长度作为 "Content-Length" 头。
	 * @param connection the HTTP connection to prepare
	 * @param contentLength the length of the content to send
	 * @throws IOException if thrown by HttpURLConnection methods
	 * @see java.net.HttpURLConnection#setRequestMethod
	 * @see java.net.HttpURLConnection#setRequestProperty
	 */
	protected void prepareConnection(HttpURLConnection connection, int contentLength) throws IOException {
		if (this.connectTimeout >= 0) {
			connection.setConnectTimeout(this.connectTimeout);
		}
		if (this.readTimeout >= 0) {
			connection.setReadTimeout(this.readTimeout);
		}

		connection.setDoOutput(true);
		connection.setRequestMethod(HTTP_METHOD_POST);
		connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, getContentType());
		connection.setRequestProperty(HTTP_HEADER_CONTENT_LENGTH, Integer.toString(contentLength));

		LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null) {
				connection.setRequestProperty(HTTP_HEADER_ACCEPT_LANGUAGE, locale.toLanguageTag());
			}
		}

		if (isAcceptGzipEncoding()) {
			connection.setRequestProperty(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}
	}

	/**
	 * 将给定的已序列化远程调用设置为请求体。
	 * <p>默认实现仅将已序列化的调用写入
	 * HttpURLConnection 的 OutputStream。可重写此方法，例如以特定编码写入，
	 * 并可能设置适当的 HTTP 请求头。
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param con the HttpURLConnection to write the request body to
	 * @param baos the ByteArrayOutputStream that contains the serialized
	 * RemoteInvocation object
	 * @throws IOException if thrown by I/O methods
	 * @see java.net.HttpURLConnection#getOutputStream()
	 * @see java.net.HttpURLConnection#setRequestProperty
	 */
	protected void writeRequestBody(
			HttpInvokerClientConfiguration config, HttpURLConnection con, ByteArrayOutputStream baos)
			throws IOException {

		baos.writeTo(con.getOutputStream());
	}

	/**
	 * 验证 {@link HttpURLConnection} 对象中包含的给定响应，
	 * 如果响应不对应于成功的 HTTP 响应，则抛出异常。
	 * <p>默认实现拒绝 2xx 之外的任何 HTTP 状态码，以避免
	 * 解析响应体并尝试从损坏的流中反序列化。
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param con the HttpURLConnection to validate
	 * @throws IOException if validation failed
	 * @see java.net.HttpURLConnection#getResponseCode()
	 */
	protected void validateResponse(HttpInvokerClientConfiguration config, HttpURLConnection con)
			throws IOException {

		if (con.getResponseCode() >= 300) {
			throw new IOException(
					"Did not receive successful HTTP response: status code = " + con.getResponseCode() +
					", status message = [" + con.getResponseMessage() + "]");
		}
	}

	/**
	 * 从给定的已执行远程调用请求中提取响应体。
	 * <p>默认实现仅从 HttpURLConnection 的 InputStream 读取
	 * 已序列化的调用。如果响应被识别为 GZIP 响应，
	 * 则会将 InputStream 包装在 GZIPInputStream 中。
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param con the HttpURLConnection to read the response body from
	 * @return an InputStream for the response body
	 * @throws IOException if thrown by I/O methods
	 * @see #isGzipResponse
	 * @see java.util.zip.GZIPInputStream
	 * @see java.net.HttpURLConnection#getInputStream()
	 * @see java.net.HttpURLConnection#getHeaderField(int)
	 * @see java.net.HttpURLConnection#getHeaderFieldKey(int)
	 */
	protected InputStream readResponseBody(HttpInvokerClientConfiguration config, HttpURLConnection con)
			throws IOException {

		if (isGzipResponse(con)) {
			// GZIP response found - need to unzip.
			return new GZIPInputStream(con.getInputStream());
		}
		else {
			// Plain response found.
			return con.getInputStream();
		}
	}

	/**
	 * 确定给定的响应是否为 GZIP 响应。
	 * <p>默认实现检查 HTTP "Content-Encoding"
	 * 头是否包含 "gzip"（不区分大小写）。
	 * @param con the HttpURLConnection to check
	 */
	protected boolean isGzipResponse(HttpURLConnection con) {
		String encodingHeader = con.getHeaderField(HTTP_HEADER_CONTENT_ENCODING);
		return (encodingHeader != null && encodingHeader.toLowerCase().contains(ENCODING_GZIP));
	}

}
