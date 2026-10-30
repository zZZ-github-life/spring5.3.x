/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * {@link javax.servlet.http.HttpServletRequest} 的包装器，缓存从
 * {@linkplain #getInputStream() 输入流}和 {@linkplain #getReader() reader}读取的
 * 所有内容，并允许通过 {@link #getContentAsByteArray() 字节数组}获取这些内容。
 *
 * <p>此类充当拦截器，仅在内容被读取时进行缓存，除此之外不会导致内容被读取。
 * 这意味着如果请求内容未被消费，则不会被缓存，也无法通过
 * {@link #getContentAsByteArray()} 获取。
 *
 * <p>例如由 {@link org.springframework.web.filter.AbstractRequestLoggingFilter} 使用。
 * 注意：自 Spring Framework 5.0 起，此包装器基于 Servlet 3.1 API 构建。
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 4.1.3
 * @see ContentCachingResponseWrapper
 */
public class ContentCachingRequestWrapper extends HttpServletRequestWrapper {

	private final ByteArrayOutputStream cachedContent;

	@Nullable
	private final Integer contentCacheLimit;

	@Nullable
	private ServletInputStream inputStream;

	@Nullable
	private BufferedReader reader;


	/**
	 * 为给定的 Servlet 请求创建一个新的 ContentCachingRequestWrapper。
	 * @param request 原始 Servlet 请求
	 */
	public ContentCachingRequestWrapper(HttpServletRequest request) {
		super(request);
		int contentLength = request.getContentLength();
		this.cachedContent = new ByteArrayOutputStream(contentLength >= 0 ? contentLength : 1024);
		this.contentCacheLimit = null;
	}

	/**
	 * 为给定 servlet 请求创建一个新的 ContentCachingRequestWrapper。
	 * @param request 原始 Servlet 请求
	 * @param contentCacheLimit 每个请求要缓存的最大字节数
	 * @since 4.3.6
	 * @see #handleContentOverflow(int)
	 */
	public ContentCachingRequestWrapper(HttpServletRequest request, int contentCacheLimit) {
		super(request);
		this.cachedContent = new ByteArrayOutputStream(contentCacheLimit);
		this.contentCacheLimit = contentCacheLimit;
	}


	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (this.inputStream == null) {
			this.inputStream = new ContentCachingInputStream(getRequest().getInputStream());
		}
		return this.inputStream;
	}

	@Override
	public String getCharacterEncoding() {
		String enc = super.getCharacterEncoding();
		return (enc != null ? enc : WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (this.reader == null) {
			this.reader = new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
		}
		return this.reader;
	}

	@Override
	public String getParameter(String name) {
		if (this.cachedContent.size() == 0 && isFormPost()) {
			writeRequestParametersToCachedContent();
		}
		return super.getParameter(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (this.cachedContent.size() == 0 && isFormPost()) {
			writeRequestParametersToCachedContent();
		}
		return super.getParameterMap();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		if (this.cachedContent.size() == 0 && isFormPost()) {
			writeRequestParametersToCachedContent();
		}
		return super.getParameterNames();
	}

	@Override
	public String[] getParameterValues(String name) {
		if (this.cachedContent.size() == 0 && isFormPost()) {
			writeRequestParametersToCachedContent();
		}
		return super.getParameterValues(name);
	}


	private boolean isFormPost() {
		String contentType = getContentType();
		return (contentType != null && contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE) &&
				HttpMethod.POST.matches(getMethod()));
	}

	private void writeRequestParametersToCachedContent() {
		try {
			if (this.cachedContent.size() == 0) {
				String requestEncoding = getCharacterEncoding();
				Map<String, String[]> form = super.getParameterMap();
				for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext(); ) {
					String name = nameIterator.next();
					List<String> values = Arrays.asList(form.get(name));
					for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
						String value = valueIterator.next();
						this.cachedContent.write(URLEncoder.encode(name, requestEncoding).getBytes());
						if (value != null) {
							this.cachedContent.write('=');
							this.cachedContent.write(URLEncoder.encode(value, requestEncoding).getBytes());
							if (valueIterator.hasNext()) {
								this.cachedContent.write('&');
							}
						}
					}
					if (nameIterator.hasNext()) {
						this.cachedContent.write('&');
					}
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write request parameters to cached content", ex);
		}
	}

	/**
	 * 以字节数组形式返回缓存的请求内容。
	 * <p>返回的数组绝不会大于内容缓存限制。
	 * <p><strong>注意：</strong>此方法返回的字节数组反映调用时已读取的内容量。
	 * 如果应用未读取内容，此方法返回空数组。
	 * @see #ContentCachingRequestWrapper(HttpServletRequest, int)
	 */
	public byte[] getContentAsByteArray() {
		return this.cachedContent.toByteArray();
	}

	/**
	 * 用于处理内容溢出的模板方法：具体而言，即读取的请求体超出
	 * 指定的内容缓存限制。
	 * <p>默认实现为空。子类可以重写此方法以抛出载荷过大异常等。
	 * @param contentCacheLimit 每个请求要缓存的最大字节数，该限制刚刚被超出
	 * @since 4.3.6
	 * @see #ContentCachingRequestWrapper(HttpServletRequest, int)
	 */
	protected void handleContentOverflow(int contentCacheLimit) {
	}


	private class ContentCachingInputStream extends ServletInputStream {

		private final ServletInputStream is;

		private boolean overflow = false;

		public ContentCachingInputStream(ServletInputStream is) {
			this.is = is;
		}

		@Override
		public int read() throws IOException {
			int ch = this.is.read();
			if (ch != -1 && !this.overflow) {
				if (contentCacheLimit != null && cachedContent.size() == contentCacheLimit) {
					this.overflow = true;
					handleContentOverflow(contentCacheLimit);
				}
				else {
					cachedContent.write(ch);
				}
			}
			return ch;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int count = this.is.read(b);
			writeToCache(b, 0, count);
			return count;
		}

		private void writeToCache(final byte[] b, final int off, int count) {
			if (!this.overflow && count > 0) {
				if (contentCacheLimit != null &&
						count + cachedContent.size() > contentCacheLimit) {
					this.overflow = true;
					cachedContent.write(b, off, contentCacheLimit - cachedContent.size());
					handleContentOverflow(contentCacheLimit);
					return;
				}
				cachedContent.write(b, off, count);
			}
		}

		@Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
			int count = this.is.read(b, off, len);
			writeToCache(b, off, count);
			return count;
		}

		@Override
		public int readLine(final byte[] b, final int off, final int len) throws IOException {
			int count = this.is.readLine(b, off, len);
			writeToCache(b, off, count);
			return count;
		}

		@Override
		public boolean isFinished() {
			return this.is.isFinished();
		}

		@Override
		public boolean isReady() {
			return this.is.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			this.is.setReadListener(readListener);
		}
	}

}
