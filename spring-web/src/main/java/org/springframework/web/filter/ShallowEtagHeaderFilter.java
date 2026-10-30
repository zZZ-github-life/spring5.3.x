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

package org.springframework.web.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

/**
 * {@link javax.servlet.Filter}，根据响应内容生成 {@code ETag} 值。
 * 该 ETag 会与请求的 {@code If-None-Match} 头进行比较。如果这些头相同，
 * 则不会发送响应内容，而是返回 {@code 304 "Not Modified"} 状态。
 *
 * <p>由于 ETag 基于响应内容，响应（例如
 * {@link org.springframework.web.servlet.View}）仍会被渲染。
 * 因此，此过滤器只能节省带宽，而不会提升服务器性能。
 *
 * <p><b>注意：</b>从 Spring Framework 5.0 开始，此过滤器使用基于 Servlet 3.1 API
 * 构建的请求/响应装饰器。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ShallowEtagHeaderFilter extends OncePerRequestFilter {

	private static final String DIRECTIVE_NO_STORE = "no-store";

	private static final String STREAMING_ATTRIBUTE = ShallowEtagHeaderFilter.class.getName() + ".STREAMING";


	private boolean writeWeakETag = false;


	/**
	 * 设置写入响应的 ETag 值是否应为弱 ETag（依据 RFC 7232）。
	 * <p>应在 {@code web.xml} 的过滤器定义中，通过名为 "writeWeakETag" 的
	 * {@code <init-param>} 进行配置。
	 * @since 4.3
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">RFC 7232 section 2.3</a>
	 */
	public void setWriteWeakETag(boolean writeWeakETag) {
		this.writeWeakETag = writeWeakETag;
	}

	/**
	 * 返回写入响应的 ETag 值是否应为弱 ETag（依据 RFC 7232）。
	 * @since 4.3
	 */
	public boolean isWriteWeakETag() {
		return this.writeWeakETag;
	}


	/**
	 * 默认值为 {@code false}，以便过滤器可以将 ETag 的生成推迟到最后一个异步派发线程。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		HttpServletResponse responseToUse = response;
		if (!isAsyncDispatch(request) && !(response instanceof ConditionalContentCachingResponseWrapper)) {
			responseToUse = new ConditionalContentCachingResponseWrapper(response, request);
		}

		filterChain.doFilter(request, responseToUse);

		if (!isAsyncStarted(request) && !isContentCachingDisabled(request)) {
			updateResponse(request, responseToUse);
		}
	}

	private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ConditionalContentCachingResponseWrapper wrapper =
				WebUtils.getNativeResponse(response, ConditionalContentCachingResponseWrapper.class);
		Assert.notNull(wrapper, "ContentCachingResponseWrapper not found");
		HttpServletResponse rawResponse = (HttpServletResponse) wrapper.getResponse();

		if (isEligibleForEtag(request, wrapper, wrapper.getStatus(), wrapper.getContentInputStream())) {
			String eTag = wrapper.getHeader(HttpHeaders.ETAG);
			if (!StringUtils.hasText(eTag)) {
				eTag = generateETagHeaderValue(wrapper.getContentInputStream(), this.writeWeakETag);
				rawResponse.setHeader(HttpHeaders.ETAG, eTag);
			}
			if (new ServletWebRequest(request, rawResponse).checkNotModified(eTag)) {
				return;
			}
		}

		wrapper.copyBodyToResponse();
	}

	/**
	 * 是否应为给定的请求和响应交换计算 ETag。默认情况下，如果以下条件全部满足，
	 * 则返回 {@code true}：
	 * <ul>
	 * <li>响应尚未提交。</li>
	 * <li>响应状态码属于 {@code 2xx} 系列。</li>
	 * <li>请求方法为 GET。</li>
	 * <li>响应 Cache-Control 头不包含 "no-store"（或完全不存在）。</li>
	 * </ul>
	 * @param request HTTP 请求
	 * @param response HTTP 响应
	 * @param responseStatusCode HTTP 响应状态码
	 * @param inputStream 响应体
	 * @return 如果适合生成 ETag 则返回 {@code true}，否则返回 {@code false}
	 */
	protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
			int responseStatusCode, InputStream inputStream) {

		if (!response.isCommitted() &&
				responseStatusCode >= 200 && responseStatusCode < 300 &&
				HttpMethod.GET.matches(request.getMethod())) {

			String cacheControl = response.getHeader(HttpHeaders.CACHE_CONTROL);
			return (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE));
		}

		return false;
	}

	/**
	 * 根据给定的响应体字节数组生成 ETag 头值。
	 * <p>默认实现生成 MD5 哈希值。
	 * @param inputStream 作为 InputStream 的响应体
	 * @param isWeak 生成的 ETag 是否应为弱 ETag
	 * @return ETag 标头值
	 * @see org.springframework.util.DigestUtils
	 */
	protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
		// length of W/ + " + 0 + 32bits md5 hash + "
		StringBuilder builder = new StringBuilder(37);
		if (isWeak) {
			builder.append("W/");
		}
		builder.append("\"0");
		DigestUtils.appendMd5DigestAsHex(inputStream, builder);
		builder.append('"');
		return builder.toString();
	}


	/**
	 * 此方法可用于禁用 ShallowEtagHeaderFilter 的内容缓存响应包装器。
	 * 主要原因是流式场景，此类场景不应被缓存，也不需要 ETag。
	 * <p><strong>注意：</strong>为了在不缓存的情况下写出整个响应内容，
	 * 必须在写入响应之前调用此方法。
	 * @since 4.2
	 */
	public static void disableContentCaching(ServletRequest request) {
		Assert.notNull(request, "ServletRequest must not be null");
		request.setAttribute(STREAMING_ATTRIBUTE, true);
	}

	private static boolean isContentCachingDisabled(HttpServletRequest request) {
		return (request.getAttribute(STREAMING_ATTRIBUTE) != null);
	}


	/**
	 * 如果 {@link #isContentCachingDisabled}，则返回原始的 OutputStream，
	 * 而不是执行缓存的那个。
	 */
	private static class ConditionalContentCachingResponseWrapper extends ContentCachingResponseWrapper {

		private final HttpServletRequest request;

		ConditionalContentCachingResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
			super(response);
			this.request = request;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return (isContentCachingDisabled(this.request) || hasETag() ?
					getResponse().getOutputStream() : super.getOutputStream());
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return (isContentCachingDisabled(this.request) || hasETag()?
					getResponse().getWriter() : super.getWriter());
		}

		private boolean hasETag() {
			return StringUtils.hasText(getHeader(HttpHeaders.ETAG));
		}
	}

}
