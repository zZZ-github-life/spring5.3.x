/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.FastByteArrayOutputStream;

/**
 * {@link javax.servlet.http.HttpServletResponse} 的包装器，缓存写入
 * {@linkplain #getOutputStream() 输出流}和 {@linkplain #getWriter() writer}的所有
 * 内容，并允许通过 {@link #getContentAsByteArray() 字节数组}获取这些内容。
 *
 * <p>例如由 {@link org.springframework.web.filter.ShallowEtagHeaderFilter} 使用。
 * 注意：自 Spring Framework 5.0 起，此包装器基于 Servlet 3.1 API 构建。
 *
 * @author Juergen Hoeller
 * @since 4.1.3
 * @see ContentCachingRequestWrapper
 */
public class ContentCachingResponseWrapper extends HttpServletResponseWrapper {

	private final FastByteArrayOutputStream content = new FastByteArrayOutputStream(1024);

	@Nullable
	private ServletOutputStream outputStream;

	@Nullable
	private PrintWriter writer;

	@Nullable
	private Integer contentLength;


	/**
	 * 为给定的 Servlet 响应创建一个新的 ContentCachingResponseWrapper。
	 * @param response 原始 Servlet 响应
	 */
	public ContentCachingResponseWrapper(HttpServletResponse response) {
		super(response);
	}


	@Override
	public void sendError(int sc) throws IOException {
		copyBodyToResponse(false);
		try {
			super.sendError(sc);
		}
		catch (IllegalStateException ex) {
			// Possibly on Tomcat when called too late: fall back to silent setStatus
			super.setStatus(sc);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void sendError(int sc, String msg) throws IOException {
		copyBodyToResponse(false);
		try {
			super.sendError(sc, msg);
		}
		catch (IllegalStateException ex) {
			// Possibly on Tomcat when called too late: fall back to silent setStatus
			super.setStatus(sc, msg);
		}
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		copyBodyToResponse(false);
		super.sendRedirect(location);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (this.outputStream == null) {
			this.outputStream = new ResponseServletOutputStream(getResponse().getOutputStream());
		}
		return this.outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (this.writer == null) {
			String characterEncoding = getCharacterEncoding();
			this.writer = (characterEncoding != null ? new ResponsePrintWriter(characterEncoding) :
					new ResponsePrintWriter(WebUtils.DEFAULT_CHARACTER_ENCODING));
		}
		return this.writer;
	}

	@Override
	public void flushBuffer() throws IOException {
		// do not flush the underlying response as the content has not been copied to it yet
	}

	@Override
	public void setContentLength(int len) {
		if (len > this.content.size()) {
			this.content.resize(len);
		}
		this.contentLength = len;
	}

	// Overrides Servlet 3.1 setContentLengthLong(long) at runtime
	@Override
	public void setContentLengthLong(long len) {
		if (len > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Content-Length exceeds ContentCachingResponseWrapper's maximum (" +
					Integer.MAX_VALUE + "): " + len);
		}
		int lenInt = (int) len;
		if (lenInt > this.content.size()) {
			this.content.resize(lenInt);
		}
		this.contentLength = lenInt;
	}

	@Override
	public void setBufferSize(int size) {
		if (size > this.content.size()) {
			this.content.resize(size);
		}
	}

	@Override
	public void resetBuffer() {
		this.content.reset();
	}

	@Override
	public void reset() {
		super.reset();
		this.content.reset();
	}

	/**
	 * 返回响应上指定的状态码。
	 * @deprecated as of 5.2 in favor of {@link HttpServletResponse#getStatus()}
	 */
	@Deprecated
	public int getStatusCode() {
		return getStatus();
	}

	/**
	 * 以字节数组形式返回缓存的响应内容。
	 */
	public byte[] getContentAsByteArray() {
		return this.content.toByteArray();
	}

	/**
	 * 返回指向缓存内容的 {@link InputStream}。
	 * @since 4.2
	 */
	public InputStream getContentInputStream() {
		return this.content.getInputStream();
	}

	/**
	 * 返回缓存内容的当前大小。
	 * @since 4.2
	 */
	public int getContentSize() {
		return this.content.size();
	}

	/**
	 * 将完整的缓存正文内容复制到响应。
	 * @since 4.2
	 */
	public void copyBodyToResponse() throws IOException {
		copyBodyToResponse(true);
	}

	/**
	 * 将缓存的正文内容复制到响应。
	 * @param complete 是否为完整的缓存正文内容设置相应的内容长度
	 * @since 4.2
	 */
	protected void copyBodyToResponse(boolean complete) throws IOException {
		if (this.content.size() > 0) {
			HttpServletResponse rawResponse = (HttpServletResponse) getResponse();
			if ((complete || this.contentLength != null) && !rawResponse.isCommitted()) {
				if (rawResponse.getHeader(HttpHeaders.TRANSFER_ENCODING) == null) {
					rawResponse.setContentLength(complete ? this.content.size() : this.contentLength);
				}
				this.contentLength = null;
			}
			this.content.writeTo(rawResponse.getOutputStream());
			this.content.reset();
			if (complete) {
				super.flushBuffer();
			}
		}
	}


	private class ResponseServletOutputStream extends ServletOutputStream {

		private final ServletOutputStream os;

		public ResponseServletOutputStream(ServletOutputStream os) {
			this.os = os;
		}

		@Override
		public void write(int b) throws IOException {
			content.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			content.write(b, off, len);
		}

		@Override
		public boolean isReady() {
			return this.os.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			this.os.setWriteListener(writeListener);
		}
	}


	private class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(String characterEncoding) throws UnsupportedEncodingException {
			super(new OutputStreamWriter(content, characterEncoding));
		}

		@Override
		public void write(char[] buf, int off, int len) {
			super.write(buf, off, len);
			super.flush();
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
		}

		@Override
		public void write(int c) {
			super.write(c);
			super.flush();
		}
	}

}
