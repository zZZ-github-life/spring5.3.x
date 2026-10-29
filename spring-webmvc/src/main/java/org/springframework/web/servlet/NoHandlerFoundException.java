/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

/**
 * 默认情况下，当 DispatcherServlet 无法为某个请求找到处理器时会发送 404 响应。
 * 但如果其 "throwExceptionIfNoHandlerFound" 属性设置为 {@code true}，
 * 则会抛出此异常，并可由配置的 HandlerExceptionResolver 进行处理。
 *
 * @author Brian Clozel
 * @since 4.0
 * @see DispatcherServlet#setThrowExceptionIfNoHandlerFound(boolean)
 * @see DispatcherServlet#noHandlerFound(HttpServletRequest, HttpServletResponse)
 */
@SuppressWarnings("serial")
public class NoHandlerFoundException extends ServletException {

	private final String httpMethod;

	private final String requestURL;

	private final HttpHeaders headers;


	/**
	 * NoHandlerFoundException 的构造函数。
	 * @param httpMethod the HTTP method
	 * @param requestURL the HTTP request URL
	 * @param headers the HTTP request headers
	 */
	public NoHandlerFoundException(String httpMethod, String requestURL, HttpHeaders headers) {
		super("No handler found for " + httpMethod + " " + requestURL);
		this.httpMethod = httpMethod;
		this.requestURL = requestURL;
		this.headers = headers;
	}


	public String getHttpMethod() {
		return this.httpMethod;
	}

	public String getRequestURL() {
		return this.requestURL;
	}

	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
