/*
 * Copyright 2002-2021 the original author or authors.
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

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.util.WebUtils;

/**
 * 过滤器基类，旨在保证在任何 Servlet 容器上每次请求派发只执行一次。
 * 它提供了接收 HttpServletRequest 和 HttpServletResponse 参数的
 * {@link #doFilterInternal} 方法。
 *
 * <p>从 Servlet 3.0 开始，过滤器可以在不同线程中发生的
 * {@link javax.servlet.DispatcherType#REQUEST REQUEST} 或
 * {@link javax.servlet.DispatcherType#ASYNC ASYNC} 派发过程中被调用。
 * 可以在 {@code web.xml} 中配置过滤器是否应参与异步派发。不过，在某些情况下，
 * Servlet 容器会采用不同的默认配置。因此，子类可以重写
 * {@link #shouldNotFilterAsyncDispatch()} 方法，静态地声明它们是否应在这两类派发
 * 期间被调用<em>一次</em>，以便进行线程初始化、日志记录、安全处理等。
 * 此机制是对在 {@code web.xml} 中按派发器类型配置过滤器的补充，而不是替代它。
 *
 * <p>子类可以使用 {@link #isAsyncDispatch(HttpServletRequest)} 判断过滤器是否
 * 作为异步派发的一部分被调用，并使用
 * {@link #isAsyncStarted(HttpServletRequest)} 判断请求是否已进入异步模式、
 * 因而当前派发并不是该请求的最后一次派发。
 *
 * <p>还有一种同样在独立线程中发生的派发类型：
 * {@link javax.servlet.DispatcherType#ERROR ERROR}。子类如果希望静态声明是否应在
 * 错误派发期间被调用<em>一次</em>，可以重写
 * {@link #shouldNotFilterErrorDispatch()}。
 *
 * <p>{@link #getAlreadyFilteredAttributeName} 方法用于确定如何识别请求已被过滤。
 * 默认实现基于具体过滤器实例的配置名称。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 06.12.2003
 */
public abstract class OncePerRequestFilter extends GenericFilterBean {

	/**
	 * 追加到过滤器名称之后、用于“已过滤”请求属性的后缀。
	 * @see #getAlreadyFilteredAttributeName
	 */
	public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";


	/**
	 * 此 {@code doFilter} 实现会保存一个“已过滤”请求属性，如果该属性已存在，
	 * 则继续执行而不再进行过滤。
	 * @see #getAlreadyFilteredAttributeName
	 * @see #shouldNotFilter
	 * @see #doFilterInternal
	 */
	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("OncePerRequestFilter just supports HTTP requests");
		}
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
		boolean hasAlreadyFilteredAttribute = request.getAttribute(alreadyFilteredAttributeName) != null;

		if (skipDispatch(httpRequest) || shouldNotFilter(httpRequest)) {
			// Proceed without invoking this filter...
			filterChain.doFilter(request, response);
		}
		else if (hasAlreadyFilteredAttribute) {
			if (DispatcherType.ERROR.equals(request.getDispatcherType())) {
				doFilterNestedErrorDispatch(httpRequest, httpResponse, filterChain);
				return;
			}

			// Proceed without invoking this filter...
			filterChain.doFilter(request, response);
		}
		else {
			// Do invoke this filter...
			request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
			try {
				doFilterInternal(httpRequest, httpResponse, filterChain);
			}
			finally {
				// Remove the "already filtered" request attribute for this request.
				request.removeAttribute(alreadyFilteredAttributeName);
			}
		}
	}

	private boolean skipDispatch(HttpServletRequest request) {
		if (isAsyncDispatch(request) && shouldNotFilterAsyncDispatch()) {
			return true;
		}
		if (request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null && shouldNotFilterErrorDispatch()) {
			return true;
		}
		return false;
	}

	/**
	 * Servlet 3.0 引入的派发器类型 {@code javax.servlet.DispatcherType.ASYNC}
	 * 意味着在单个请求过程中，过滤器可以在多个线程中被调用。如果过滤器当前正在
	 * 异步派发中执行，此方法返回 {@code true}。
	 * @param request 当前请求
	 * @since 3.2
	 * @see WebAsyncManager#hasConcurrentResult()
	 */
	protected boolean isAsyncDispatch(HttpServletRequest request) {
		return DispatcherType.ASYNC.equals(request.getDispatcherType());
	}

	/**
	 * 请求处理是否处于异步模式，这意味着当前线程退出后响应不会被提交。
	 * @param request 当前请求
	 * @since 3.2
	 * @see WebAsyncManager#isConcurrentHandlingStarted()
	 */
	protected boolean isAsyncStarted(HttpServletRequest request) {
		return WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted();
	}

	/**
	 * 返回用于标识请求已被过滤的请求属性名称。
	 * <p>默认实现取具体过滤器实例的配置名称并追加 ".FILTERED"。
	 * 如果过滤器尚未完全初始化，则回退到其类名。
	 * @see #getFilterName
	 * @see #ALREADY_FILTERED_SUFFIX
	 */
	protected String getAlreadyFilteredAttributeName() {
		String name = getFilterName();
		if (name == null) {
			name = getClass().getName();
		}
		return name + ALREADY_FILTERED_SUFFIX;
	}

	/**
	 * 子类可以重写此方法以进行自定义过滤控制，返回 {@code true} 可跳过对给定请求的过滤。
	 * <p>默认实现始终返回 {@code false}。
	 * @param request 当前的 HTTP 请求
	 * @return 给定请求是否<i>不应</i>被过滤
	 * @throws ServletException 如果出错
	 */
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return false;
	}

	/**
	 * Servlet 3.0 引入的派发器类型 {@code javax.servlet.DispatcherType.ASYNC}
	 * 意味着在单个请求过程中，过滤器可以在多个线程中被调用。有些过滤器只需要过滤
	 * 初始线程（例如请求包装），而另一些过滤器可能需要在每个额外线程中至少被调用一次，
	 * 例如用于设置线程局部变量或在最后执行最终处理。
	 * <p>请注意，尽管可以通过 {@code web.xml} 或在 Java 中通过
	 * {@code ServletContext} 将过滤器映射到特定的派发器类型，但 Servlet 容器可能会
	 * 对派发器类型强制执行不同的默认设置。此标志用于强制执行过滤器的设计意图。
	 * <p>默认返回值为 "true"，表示过滤器不会在后续异步派发期间被调用。
	 * 如果为 "false"，则过滤器会在异步派发期间被调用，并同样保证在单个线程中的
	 * 一次请求内只被调用一次。
	 * @since 3.2
	 */
	protected boolean shouldNotFilterAsyncDispatch() {
		return true;
	}

	/**
	 * 是否过滤错误派发，例如当 Servlet 容器处理 {@code web.xml} 中映射的错误时。
	 * 默认返回值为 "true"，表示发生错误派发时不会调用该过滤器。
	 * @since 3.2
	 */
	protected boolean shouldNotFilterErrorDispatch() {
		return true;
	}


	/**
	 * 与 {@code doFilter} 的约定相同，但保证在单个请求线程中每次请求只被调用一次。
	 * 详情请参阅 {@link #shouldNotFilterAsyncDispatch()}。
	 * <p>提供 HttpServletRequest 和 HttpServletResponse 参数，
	 * 而不是默认的 ServletRequest 和 ServletResponse 参数。
	 */
	protected abstract void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException;

	/**
	 * 通常 ERROR 派发发生在 REQUEST 派发完成之后，过滤器链会重新开始。
	 * 但在某些服务器上，ERROR 派发可能嵌套在 REQUEST 派发之内，
	 * 例如由于在响应上调用了 {@code sendError}。在这种情况下，我们仍处于过滤器链中、
	 * 位于同一线程上，但请求和响应已被切换回原始的、未包装的实例。
	 * <p>子类可以使用此方法来过滤此类嵌套的 ERROR 派发，并在请求或响应上重新应用包装。
	 * 如果有 {@code ThreadLocal} 上下文，它应仍然有效，因为我们仍嵌套在过滤器链中。
	 * @since 5.1.9
	 */
	protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		filterChain.doFilter(request, response);
	}

}
