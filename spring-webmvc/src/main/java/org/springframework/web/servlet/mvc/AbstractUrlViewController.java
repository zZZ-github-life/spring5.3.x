/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 基于请求 URL 返回视图名的 {@code Controllers} 的抽象基类。
 *
 * <p>提供从 URL 确定视图名的基础设施，以及可配置的
 * URL 查找。有关后者的信息，请参见 {@code alwaysUseFullPath}
 * 和 {@code urlDecode} 属性。
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see #setAlwaysUseFullPath
 * @see #setUrlDecode
 */
public abstract class AbstractUrlViewController extends AbstractController {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * 设置 URL 查找是否应始终使用当前 Servlet 上下文内的完整路径。
	 * 否则，如果适用（即 web.xml 中配置了 ".../*" 的 Servlet 映射），
	 * 将使用当前 Servlet 映射内的路径。默认值为 "false"。
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * 设置是否应对上下文路径和请求 URI 进行 URL 解码。
	 * 与 Servlet 路径不同，Servlet API 返回的这两者都是<i>未解码</i>的。
	 * <p>根据 Servlet 规范使用请求编码或默认编码（ISO-8859-1）。
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * 设置是否应从请求 URI 中剥离 ";"（分号）内容。
	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	/**
	 * 设置用于解析查找路径的 UrlPathHelper。
	 * <p>可使用此项以自定义子类覆盖默认的 UrlPathHelper，
	 * 或在多个 MethodNameResolver 和 HandlerMapping 之间共享通用的
	 * UrlPathHelper 设置。
	 * @see org.springframework.web.servlet.handler.AbstractUrlHandlerMapping#setUrlPathHelper
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回用于解析查找路径的 UrlPathHelper。
	 */
	protected UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	/**
	 * 获取用于查找的 URL 路径并委托给
	 * {@link #getViewNameForRequest}。同时将
	 * {@link RequestContextUtils#getInputFlashMap} 的内容添加到模型中。
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
		String viewName = getViewNameForRequest(request);
		if (logger.isTraceEnabled()) {
			logger.trace("Returning view name '" + viewName + "'");
		}
		return new ModelAndView(viewName, RequestContextUtils.getInputFlashMap(request));
	}

	/**
	 * 返回要为此请求渲染的视图名称，依据给定的查找路径。
	 * 由 {@link #handleRequestInternal} 调用。
	 * @param request current HTTP request
	 * @return a view name for this request (never {@code null})
	 * @see #handleRequestInternal
	 * @see #setAlwaysUseFullPath
	 * @see #setUrlDecode
	 */
	protected abstract String getViewNameForRequest(HttpServletRequest request);

}
