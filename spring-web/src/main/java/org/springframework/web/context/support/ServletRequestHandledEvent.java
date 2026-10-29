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

package org.springframework.web.context.support;

import org.springframework.lang.Nullable;

/**
 * RequestHandledEvent 的 Servlet 特定子类，增加了 Servlet 特定的上下文信息。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.servlet.FrameworkServlet
 * @see org.springframework.context.ApplicationContext#publishEvent
 */
@SuppressWarnings("serial")
public class ServletRequestHandledEvent extends RequestHandledEvent {

	/** 触发该请求的 URL。 */
	private final String requestUrl;

	/** 请求来源的 IP 地址。 */
	private final String clientAddress;

	/** 通常是 GET 或 POST。 */
	private final String method;

	/** 处理该请求的 Servlet 名称。 */
	private final String servletName;

	/** 响应的 HTTP 状态码。 */
	private final int statusCode;


	/**
	 * 创建一个新的 ServletRequestHandledEvent。
	 * @param source the component that published the event
	 * @param requestUrl the URL of the request
	 * @param clientAddress the IP address that the request came from
	 * @param method the HTTP method of the request (usually GET or POST)
	 * @param servletName the name of the servlet that handled the request
	 * @param sessionId the id of the HTTP session, if any
	 * @param userName the name of the user that was associated with the
	 * request, if any (usually the UserPrincipal)
	 * @param processingTimeMillis the processing time of the request in milliseconds
	 */
	public ServletRequestHandledEvent(Object source, String requestUrl,
			String clientAddress, String method, String servletName,
			@Nullable String sessionId, @Nullable String userName, long processingTimeMillis) {

		super(source, sessionId, userName, processingTimeMillis);
		this.requestUrl = requestUrl;
		this.clientAddress = clientAddress;
		this.method = method;
		this.servletName = servletName;
		this.statusCode = -1;
	}

	/**
	 * 创建一个新的 ServletRequestHandledEvent。
	 * @param source the component that published the event
	 * @param requestUrl the URL of the request
	 * @param clientAddress the IP address that the request came from
	 * @param method the HTTP method of the request (usually GET or POST)
	 * @param servletName the name of the servlet that handled the request
	 * @param sessionId the id of the HTTP session, if any
	 * @param userName the name of the user that was associated with the
	 * request, if any (usually the UserPrincipal)
	 * @param processingTimeMillis the processing time of the request in milliseconds
	 * @param failureCause the cause of failure, if any
	 */
	public ServletRequestHandledEvent(Object source, String requestUrl,
			String clientAddress, String method, String servletName, @Nullable String sessionId,
			@Nullable String userName, long processingTimeMillis, @Nullable Throwable failureCause) {

		super(source, sessionId, userName, processingTimeMillis, failureCause);
		this.requestUrl = requestUrl;
		this.clientAddress = clientAddress;
		this.method = method;
		this.servletName = servletName;
		this.statusCode = -1;
	}

	/**
	 * 创建一个新的 ServletRequestHandledEvent。
	 * @param source the component that published the event
	 * @param requestUrl the URL of the request
	 * @param clientAddress the IP address that the request came from
	 * @param method the HTTP method of the request (usually GET or POST)
	 * @param servletName the name of the servlet that handled the request
	 * @param sessionId the id of the HTTP session, if any
	 * @param userName the name of the user that was associated with the
	 * request, if any (usually the UserPrincipal)
	 * @param processingTimeMillis the processing time of the request in milliseconds
	 * @param failureCause the cause of failure, if any
	 * @param statusCode the HTTP status code of the response
	 */
	public ServletRequestHandledEvent(Object source, String requestUrl,
			String clientAddress, String method, String servletName, @Nullable String sessionId,
			@Nullable String userName, long processingTimeMillis, @Nullable Throwable failureCause, int statusCode) {

		super(source, sessionId, userName, processingTimeMillis, failureCause);
		this.requestUrl = requestUrl;
		this.clientAddress = clientAddress;
		this.method = method;
		this.servletName = servletName;
		this.statusCode = statusCode;
	}


	/**
	 * 返回请求的 URL。
	 */
	public String getRequestUrl() {
		return this.requestUrl;
	}

	/**
	 * 返回请求来源的 IP 地址。
	 */
	public String getClientAddress() {
		return this.clientAddress;
	}

	/**
	 * 返回请求的 HTTP 方法（通常是 GET 或 POST）。
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * 返回处理该请求的 Servlet 名称。
	 */
	public String getServletName() {
		return this.servletName;
	}

	/**
	 * 返回响应的 HTTP 状态码；如果状态码不可用则返回 -1。
	 * @since 4.1
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	@Override
	public String getShortDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("url=[").append(getRequestUrl()).append("]; ");
		sb.append("client=[").append(getClientAddress()).append("]; ");
		sb.append(super.getShortDescription());
		return sb.toString();
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("url=[").append(getRequestUrl()).append("]; ");
		sb.append("client=[").append(getClientAddress()).append("]; ");
		sb.append("method=[").append(getMethod()).append("]; ");
		sb.append("servlet=[").append(getServletName()).append("]; ");
		sb.append(super.getDescription());
		return sb.toString();
	}

	@Override
	public String toString() {
		return "ServletRequestHandledEvent: " + getDescription();
	}

}
