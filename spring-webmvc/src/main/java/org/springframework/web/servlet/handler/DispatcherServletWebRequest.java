/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * {@link ServletWebRequest} 的子类，可感知
 * {@link org.springframework.web.servlet.DispatcherServlet} 的
 * 请求上下文，例如由所配置的
 * {@link org.springframework.web.servlet.LocaleResolver} 确定的 Locale。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #getLocale()
 * @see org.springframework.web.servlet.LocaleResolver
 */
public class DispatcherServletWebRequest extends ServletWebRequest {

	/**
	 * 为给定请求创建新的 DispatcherServletWebRequest 实例。
	 * @param request 当前 HTTP 请求
	 */
	public DispatcherServletWebRequest(HttpServletRequest request) {
		super(request);
	}

	/**
	 * 为给定请求和响应创建新的 DispatcherServletWebRequest 实例。
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 */
	public DispatcherServletWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	@Override
	public Locale getLocale() {
		return RequestContextUtils.getLocale(getRequest());
	}

}
