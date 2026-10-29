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

package org.springframework.web.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.function.Predicate;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

/**
 * 在请求处理前后执行日志记录操作的 {@code Filter} 的基类。
 *
 * <p>子类应重写 {@code beforeRequest(HttpServletRequest, String)} 和
 * {@code afterRequest(HttpServletRequest, String)} 方法，以在请求前后执行实际的日志记录。
 *
 * <p>子类会在 {@code beforeRequest} 和 {@code afterRequest} 方法中接收到要写入日志的消息。
 * 默认情况下只记录请求的 URI。不过，将 {@code includeQueryString} 属性设置为
 * {@code true} 还会包含请求的查询字符串；这可以通过 {@code includeClientInfo}
 * 和 {@code includeHeaders} 进一步扩展。请求的载荷（正文内容）可以通过
 * {@code includePayload} 标志记录：请注意，这只会记录实际已读取的那部分载荷，
 * 而不一定是请求的整个正文。
 *
 * <p>前置和后置消息的前缀与后缀可以通过 {@code beforeMessagePrefix}、
 * {@code afterMessagePrefix}、{@code beforeMessageSuffix} 和
 * {@code afterMessageSuffix} 属性进行配置。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 1.2.5
 * @see #beforeRequest
 * @see #afterRequest
 */
public abstract class AbstractRequestLoggingFilter extends OncePerRequestFilter {

	/**
	 * 在请求处理<i>之前</i>写入的日志消息所追加的默认前缀值。
	 */
	public static final String DEFAULT_BEFORE_MESSAGE_PREFIX = "Before request [";

	/**
	 * 在请求处理<i>之前</i>写入的日志消息所追加的默认后缀值。
	 */
	public static final String DEFAULT_BEFORE_MESSAGE_SUFFIX = "]";

	/**
	 * 在请求处理<i>之后</i>写入的日志消息所追加的默认前缀值。
	 */
	public static final String DEFAULT_AFTER_MESSAGE_PREFIX = "After request [";

	/**
	 * 在请求处理<i>之后</i>写入的日志消息所追加的默认后缀值。
	 */
	public static final String DEFAULT_AFTER_MESSAGE_SUFFIX = "]";

	private static final int DEFAULT_MAX_PAYLOAD_LENGTH = 50;


	private boolean includeQueryString = false;

	private boolean includeClientInfo = false;

	private boolean includeHeaders = false;

	private boolean includePayload = false;

	@Nullable
	private Predicate<String> headerPredicate;

	private int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;

	private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;

	private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;

	private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;

	private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;


	/**
	 * 设置日志消息中是否应包含查询字符串。
	 * <p>应在 {@code web.xml} 的过滤器定义中，通过名为
	 * "includeQueryString" 的 {@code <init-param>} 进行配置。
	 */
	public void setIncludeQueryString(boolean includeQueryString) {
		this.includeQueryString = includeQueryString;
	}

	/**
	 * 返回日志消息中是否应包含查询字符串。
	 */
	protected boolean isIncludeQueryString() {
		return this.includeQueryString;
	}

	/**
	 * 设置日志消息中是否应包含客户端地址和会话 id。
	 * <p>应在 {@code web.xml} 的过滤器定义中，通过名为
	 * "includeClientInfo" 的 {@code <init-param>} 进行配置。
	 */
	public void setIncludeClientInfo(boolean includeClientInfo) {
		this.includeClientInfo = includeClientInfo;
	}

	/**
	 * 返回日志消息中是否应包含客户端地址和会话 id。
	 */
	protected boolean isIncludeClientInfo() {
		return this.includeClientInfo;
	}

	/**
	 * 设置日志消息中是否应包含请求头。
	 * <p>应在 {@code web.xml} 的过滤器定义中，通过名为
	 * "includeHeaders" 的 {@code <init-param>} 进行配置。
	 * @since 4.3
	 */
	public void setIncludeHeaders(boolean includeHeaders) {
		this.includeHeaders = includeHeaders;
	}

	/**
	 * 返回日志消息中是否应包含请求头。
	 * @since 4.3
	 */
	protected boolean isIncludeHeaders() {
		return this.includeHeaders;
	}

	/**
	 * 设置日志消息中是否应包含请求载荷（正文）。
	 * <p>应在 {@code web.xml} 的过滤器定义中，通过名为
	 * "includePayload" 的 {@code <init-param>} 进行配置。
	 * @since 3.0
	 */
	public void setIncludePayload(boolean includePayload) {
		this.includePayload = includePayload;
	}

	/**
	 * 返回日志消息中是否应包含请求载荷（正文）。
	 * @since 3.0
	 */
	protected boolean isIncludePayload() {
		return this.includePayload;
	}

	/**
	 * 配置一个谓词，用于在 {@link #setIncludeHeaders(boolean)} 设置为
	 * {@code true} 时选择应记录哪些请求头。
	 * <p>默认情况下不设置此属性，此时会记录所有请求头。
	 * @param headerPredicate the predicate to use
	 * @since 5.2
	 */
	public void setHeaderPredicate(@Nullable Predicate<String> headerPredicate) {
		this.headerPredicate = headerPredicate;
	}

	/**
	 * 已配置的 {@link #setHeaderPredicate(Predicate) headerPredicate}。
	 * @since 5.2
	 */
	@Nullable
	protected Predicate<String> getHeaderPredicate() {
		return this.headerPredicate;
	}

	/**
	 * 设置日志消息中包含的载荷正文的最大长度。默认为 50 个字符。
	 * @since 3.0
	 */
	public void setMaxPayloadLength(int maxPayloadLength) {
		Assert.isTrue(maxPayloadLength >= 0, "'maxPayloadLength' must be greater than or equal to 0");
		this.maxPayloadLength = maxPayloadLength;
	}

	/**
	 * 返回日志消息中包含的载荷正文的最大长度。
	 * @since 3.0
	 */
	protected int getMaxPayloadLength() {
		return this.maxPayloadLength;
	}

	/**
	 * 设置应追加到请求处理<i>之前</i>写入的日志消息前面的值。
	 */
	public void setBeforeMessagePrefix(String beforeMessagePrefix) {
		this.beforeMessagePrefix = beforeMessagePrefix;
	}

	/**
	 * 设置应追加到请求处理<i>之前</i>写入的日志消息后面的值。
	 */
	public void setBeforeMessageSuffix(String beforeMessageSuffix) {
		this.beforeMessageSuffix = beforeMessageSuffix;
	}

	/**
	 * 设置应追加到请求处理<i>之后</i>写入的日志消息前面的值。
	 */
	public void setAfterMessagePrefix(String afterMessagePrefix) {
		this.afterMessagePrefix = afterMessagePrefix;
	}

	/**
	 * 设置应追加到请求处理<i>之后</i>写入的日志消息后面的值。
	 */
	public void setAfterMessageSuffix(String afterMessageSuffix) {
		this.afterMessageSuffix = afterMessageSuffix;
	}


	/**
	 * 默认值为 "false"，以便过滤器可以在请求处理开始时记录一条“前置”消息，
	 * 并在最后一个异步派发线程退出时记录一条“后置”消息。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 将请求转发给链中的下一个过滤器，并委托给子类在请求处理前后执行实际的请求日志记录。
	 * @see #beforeRequest
	 * @see #afterRequest
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		boolean isFirstRequest = !isAsyncDispatch(request);
		HttpServletRequest requestToUse = request;

		if (isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
			requestToUse = new ContentCachingRequestWrapper(request, getMaxPayloadLength());
		}

		boolean shouldLog = shouldLog(requestToUse);
		if (shouldLog && isFirstRequest) {
			beforeRequest(requestToUse, getBeforeMessage(requestToUse));
		}
		try {
			filterChain.doFilter(requestToUse, response);
		}
		finally {
			if (shouldLog && !isAsyncStarted(requestToUse)) {
				afterRequest(requestToUse, getAfterMessage(requestToUse));
			}
		}
	}

	/**
	 * 获取在请求之前写入日志的消息。
	 * @see #createMessage
	 */
	private String getBeforeMessage(HttpServletRequest request) {
		return createMessage(request, this.beforeMessagePrefix, this.beforeMessageSuffix);
	}

	/**
	 * 获取在请求之后写入日志的消息。
	 * @see #createMessage
	 */
	private String getAfterMessage(HttpServletRequest request) {
		return createMessage(request, this.afterMessagePrefix, this.afterMessageSuffix);
	}

	/**
	 * 为给定的请求、前缀和后缀创建日志消息。
	 * <p>如果 {@code includeQueryString} 为 {@code true}，则日志消息的内部部分
	 * 将采用 {@code request_uri?query_string} 的形式；否则消息将简单地采用
	 * {@code request_uri} 的形式。
	 * <p>最终消息由上述内部部分以及所提供的前缀和后缀组成。
	 */
	protected String createMessage(HttpServletRequest request, String prefix, String suffix) {
		StringBuilder msg = new StringBuilder();
		msg.append(prefix);
		msg.append(request.getMethod()).append(' ');
		msg.append(request.getRequestURI());

		if (isIncludeQueryString()) {
			String queryString = request.getQueryString();
			if (queryString != null) {
				msg.append('?').append(queryString);
			}
		}

		if (isIncludeClientInfo()) {
			String client = request.getRemoteAddr();
			if (StringUtils.hasLength(client)) {
				msg.append(", client=").append(client);
			}
			HttpSession session = request.getSession(false);
			if (session != null) {
				msg.append(", session=").append(session.getId());
			}
			String user = request.getRemoteUser();
			if (user != null) {
				msg.append(", user=").append(user);
			}
		}

		if (isIncludeHeaders()) {
			HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
			if (getHeaderPredicate() != null) {
				Enumeration<String> names = request.getHeaderNames();
				while (names.hasMoreElements()) {
					String header = names.nextElement();
					if (!getHeaderPredicate().test(header)) {
						headers.set(header, "masked");
					}
				}
			}
			msg.append(", headers=").append(headers);
		}

		if (isIncludePayload()) {
			String payload = getMessagePayload(request);
			if (payload != null) {
				msg.append(", payload=").append(payload);
			}
		}

		msg.append(suffix);
		return msg.toString();
	}

	/**
	 * 当 {@link #isIncludePayload()} 返回 true 时，提取由
	 * {@link #createMessage(HttpServletRequest, String, String)} 创建的消息中的
	 * 载荷部分。
	 * @since 5.0.3
	 */
	@Nullable
	protected String getMessagePayload(HttpServletRequest request) {
		ContentCachingRequestWrapper wrapper =
				WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
		if (wrapper != null) {
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				int length = Math.min(buf.length, getMaxPayloadLength());
				try {
					return new String(buf, 0, length, wrapper.getCharacterEncoding());
				}
				catch (UnsupportedEncodingException ex) {
					return "[unknown]";
				}
			}
		}
		return null;
	}


	/**
	 * 确定是否要为当前请求调用 {@link #beforeRequest}/{@link #afterRequest}
	 * 方法，即当前是否启用了日志记录（以及是否值得构建日志消息）。
	 * <p>默认实现始终返回 {@code true}。子类可以通过日志级别检查来重写此方法。
	 * @param request current HTTP request
	 * @return {@code true} if the before/after method should get called;
	 * {@code false} otherwise
	 * @since 4.1.5
	 */
	protected boolean shouldLog(HttpServletRequest request) {
		return true;
	}

	/**
	 * 具体子类应实现此方法，以在请求处理<i>之前</i>写入日志消息。
	 * @param request current HTTP request
	 * @param message the message to log
	 */
	protected abstract void beforeRequest(HttpServletRequest request, String message);

	/**
	 * 具体子类应实现此方法，以在请求处理<i>之后</i>写入日志消息。
	 * @param request current HTTP request
	 * @param message the message to log
	 */
	protected abstract void afterRequest(HttpServletRequest request, String message);

}
