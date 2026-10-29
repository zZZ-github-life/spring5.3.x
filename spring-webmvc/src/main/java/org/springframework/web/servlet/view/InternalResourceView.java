/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * 同一 Web 应用中 JSP 或其他资源的包装器。
 * 将模型对象作为请求属性公开，并使用 {@link javax.servlet.RequestDispatcher}
 * 将请求转发到指定的资源 URL。
 *
 * <p>此视图的 URL 应指定 Web 应用内的资源，
 * 适合 RequestDispatcher 的 {@code forward} 或
 * {@code include} 方法。
 *
 * <p>如果在已包含的请求内或在已提交的响应中运行，
 * 此视图将回退为 include 而不是 forward。可以通过在渲染视图之前
 * 调用 {@code response.flushBuffer()}（将提交响应）来强制执行此行为。
 *
 * <p>从 DispatcherServlet 上下文定义的角度来看，
 * 与 {@link InternalResourceViewResolver} 配合使用的典型用法如下：
 *
 * <pre class="code">&lt;bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"&gt;
 *   &lt;property name="prefix" value="/WEB-INF/jsp/"/&gt;
 *   &lt;property name="suffix" value=".jsp"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 处理器返回的每个视图名称都将被转换为 JSP 资源
 * （例如："myView" &rarr; "/WEB-INF/jsp/myView.jsp"），
 * 默认使用此视图类。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see javax.servlet.RequestDispatcher#forward
 * @see javax.servlet.RequestDispatcher#include
 * @see javax.servlet.ServletResponse#flushBuffer
 * @see InternalResourceViewResolver
 * @see JstlView
 */
public class InternalResourceView extends AbstractUrlBasedView {

	private boolean alwaysInclude = false;

	private boolean preventDispatchLoop = false;


	/**
	 * 作为 bean 使用的构造函数。
	 * @see #setUrl
	 * @see #setAlwaysInclude
	 */
	public InternalResourceView() {
	}

	/**
	 * 使用给定 URL 创建一个新的 InternalResourceView。
	 * @param url the URL to forward to
	 * @see #setAlwaysInclude
	 */
	public InternalResourceView(String url) {
		super(url);
	}

	/**
	 * 使用给定 URL 创建一个新的 InternalResourceView。
	 * @param url the URL to forward to
	 * @param alwaysInclude whether to always include the view rather than forward to it
	 */
	public InternalResourceView(String url, boolean alwaysInclude) {
		super(url);
		this.alwaysInclude = alwaysInclude;
	}


	/**
	 * 指定是否始终 include 视图而不是 forward 到它。
	 * <p>默认为 "false"。开启此标志即可强制使用
	 * Servlet include，即使 forward 是可行的。
	 * @see javax.servlet.RequestDispatcher#forward
	 * @see javax.servlet.RequestDispatcher#include
	 * @see #useInclude(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void setAlwaysInclude(boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}

	/**
	 * 设置是否显式阻止回派到
	 * 当前处理器路径。
	 * <p>默认为 "false"。对于基于约定的视图，
	 * 如果回派到当前处理器路径是明确的错误，
	 * 请将其设为 "true"。
	 */
	public void setPreventDispatchLoop(boolean preventDispatchLoop) {
		this.preventDispatchLoop = preventDispatchLoop;
	}

	/**
	 * InternalResourceView 并不严格要求提供 ApplicationContext。
	 */
	@Override
	protected boolean isContextRequired() {
		return false;
	}


	/**
	 * 根据指定的模型渲染内部资源。
	 * 其中包括将模型设置为请求属性。
	 */
	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Expose the model object as request attributes.
		exposeModelAsRequestAttributes(model, request);

		// Expose helpers as request attributes, if any.
		exposeHelpers(request);

		// Determine the path for the request dispatcher.
		String dispatcherPath = prepareForRendering(request, response);

		// Obtain a RequestDispatcher for the target resource (typically a JSP).
		RequestDispatcher rd = getRequestDispatcher(request, dispatcherPath);
		if (rd == null) {
			throw new ServletException("Could not get RequestDispatcher for [" + getUrl() +
					"]: Check that the corresponding file exists within your web application archive!");
		}

		// If already included or response already committed, perform include, else forward.
		if (useInclude(request, response)) {
			response.setContentType(getContentType());
			if (logger.isDebugEnabled()) {
				logger.debug("Including [" + getUrl() + "]");
			}
			rd.include(request, response);
		}

		else {
			// Note: The forwarded resource is supposed to determine the content type itself.
			if (logger.isDebugEnabled()) {
				logger.debug("Forwarding to [" + getUrl() + "]");
			}
			rd.forward(request, response);
		}
	}

	/**
	 * 公开每次渲染操作所特有的辅助对象。这是必要的，以便
	 * 不同的渲染操作不会相互覆盖各自的上下文等。
	 * <p>由 {@link #renderMergedOutputModel(Map, HttpServletRequest, HttpServletResponse)} 调用。
	 * 默认实现为空。可以重写此方法以将
	 * 自定义辅助对象添加为请求属性。
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding attributes
	 * @see #renderMergedOutputModel
	 * @see JstlView#exposeHelpers
	 */
	protected void exposeHelpers(HttpServletRequest request) throws Exception {
	}

	/**
	 * 为渲染做准备，并确定要 forward 到（或 include）的
	 * 请求调度器路径。
	 * <p>此实现仅返回所配置的 URL。
	 * 子类可以重写此方法以确定要渲染的资源，
	 * 通常以不同方式解释该 URL。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return the request dispatcher path to use
	 * @throws Exception if preparations failed
	 * @see #getUrl()
	 */
	protected String prepareForRendering(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String path = getUrl();
		Assert.state(path != null, "'url' not set");

		if (this.preventDispatchLoop) {
			String uri = request.getRequestURI();
			if (path.startsWith("/") ? uri.equals(path) : uri.equals(StringUtils.applyRelativePath(uri, path))) {
				throw new ServletException("Circular view path [" + path + "]: would dispatch back " +
						"to the current handler URL [" + uri + "] again. Check your ViewResolver setup! " +
						"(Hint: This may be the result of an unspecified view, due to default view name generation.)");
			}
		}
		return path;
	}

	/**
	 * 获取用于 forward/include 的 RequestDispatcher。
	 * <p>默认实现只是调用
	 * {@link HttpServletRequest#getRequestDispatcher(String)}。
	 * 可在子类中重写。
	 * @param request current HTTP request
	 * @param path the target URL (as returned from {@link #prepareForRendering})
	 * @return a corresponding RequestDispatcher
	 */
	@Nullable
	protected RequestDispatcher getRequestDispatcher(HttpServletRequest request, String path) {
		return request.getRequestDispatcher(path);
	}

	/**
	 * 确定使用 RequestDispatcher 的 {@code include} 还是
	 * {@code forward} 方法。
	 * <p>会检查请求中是否找到 include URI 属性（
	 * 表示这是一个 include 请求），以及响应是否已提交。
	 * 在这两种情况下，都将执行 include，因为已无法再执行 forward。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return {@code true} for include, {@code false} for forward
	 * @see javax.servlet.RequestDispatcher#forward
	 * @see javax.servlet.RequestDispatcher#include
	 * @see javax.servlet.ServletResponse#isCommitted
	 * @see org.springframework.web.util.WebUtils#isIncludeRequest
	 */
	protected boolean useInclude(HttpServletRequest request, HttpServletResponse response) {
		return (this.alwaysInclude || WebUtils.isIncludeRequest(request) || response.isCommitted());
	}

}
