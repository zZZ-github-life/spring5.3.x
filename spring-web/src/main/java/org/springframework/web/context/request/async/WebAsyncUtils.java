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

package org.springframework.web.context.request.async;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * 与处理异步 Web 请求相关的工具方法。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public abstract class WebAsyncUtils {

	/**
	 * 包含 {@link WebAsyncManager} 的名称属性。
	 */
	public static final String WEB_ASYNC_MANAGER_ATTRIBUTE =
			WebAsyncManager.class.getName() + ".WEB_ASYNC_MANAGER";


	/**
	 * 获取当前请求的 {@link WebAsyncManager}；如果未找到，则创建并关联到该请求。
	 */
	public static WebAsyncManager getAsyncManager(ServletRequest servletRequest) {
		WebAsyncManager asyncManager = null;
		Object asyncManagerAttr = servletRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE);
		if (asyncManagerAttr instanceof WebAsyncManager) {
			asyncManager = (WebAsyncManager) asyncManagerAttr;
		}
		if (asyncManager == null) {
			asyncManager = new WebAsyncManager();
			servletRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager);
		}
		return asyncManager;
	}

	/**
	 * 获取当前请求的 {@link WebAsyncManager}；如果未找到，则创建并关联到该请求。
	 */
	public static WebAsyncManager getAsyncManager(WebRequest webRequest) {
		int scope = RequestAttributes.SCOPE_REQUEST;
		WebAsyncManager asyncManager = null;
		Object asyncManagerAttr = webRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, scope);
		if (asyncManagerAttr instanceof WebAsyncManager) {
			asyncManager = (WebAsyncManager) asyncManagerAttr;
		}
		if (asyncManager == null) {
			asyncManager = new WebAsyncManager();
			webRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager, scope);
		}
		return asyncManager;
	}

	/**
	 * 创建一个 AsyncWebRequest 实例。默认情况下会创建 {@link StandardServletAsyncWebRequest} 实例。
	 * @param request the current request
	 * @param response the current response
	 * @return an AsyncWebRequest instance (never {@code null})
	 */
	public static AsyncWebRequest createAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		return new StandardServletAsyncWebRequest(request, response);
	}

}
