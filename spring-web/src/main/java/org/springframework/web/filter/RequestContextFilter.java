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

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servlet 过滤器，通过
 * {@link org.springframework.context.i18n.LocaleContextHolder} 和
 * {@link RequestContextHolder} 将请求暴露给当前线程。
 * 可在 {@code web.xml} 中注册为过滤器。
 *
 * <p>另外，Spring 的
 * {@link org.springframework.web.context.request.RequestContextListener}
 * 和 Spring 的 {@link org.springframework.web.servlet.DispatcherServlet}
 * 也会将相同的请求上下文暴露给当前线程。
 *
 * <p>此过滤器主要用于第三方 Servlet，例如 JSF FacesServlet。
 * 在 Spring 自身的 Web 支持中，DispatcherServlet 的处理已完全足够。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rossen Stoyanchev
 * @since 2.0
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.context.request.RequestContextHolder
 * @see org.springframework.web.context.request.RequestContextListener
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public class RequestContextFilter extends OncePerRequestFilter {

	private boolean threadContextInheritable = false;


	/**
	 * 设置是否将 LocaleContext 和 RequestAttributes 暴露为子线程可继承
	 * （使用 {@link java.lang.InheritableThreadLocal}）。
	 * <p>默认值为 "false"，以避免对派生的后台线程产生副作用。
	 * 将其设置为 "true" 可为在请求处理期间派生、且仅用于该请求
	 * （即在其初始任务完成后结束，线程不会被重用）的自定义子线程启用继承。
	 * <p><b>警告：</b>如果你访问的线程池配置为可能按需添加新线程
	 * （例如 JDK 的 {@link java.util.concurrent.ThreadPoolExecutor}），
	 * 请不要对子线程使用继承，因为这会将会话上下文暴露给此类池中的线程。
	 */
	public void setThreadContextInheritable(boolean threadContextInheritable) {
		this.threadContextInheritable = threadContextInheritable;
	}


	/**
	 * 返回 "false"，以便过滤器可以在每个异步派发的线程中设置请求上下文。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回 "false"，以便过滤器可以在错误派发中设置请求上下文。
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		ServletRequestAttributes attributes = new ServletRequestAttributes(request, response);
		initContextHolders(request, attributes);

		try {
			filterChain.doFilter(request, response);
		}
		finally {
			resetContextHolders();
			if (logger.isTraceEnabled()) {
				logger.trace("Cleared thread-bound request context: " + request);
			}
			attributes.requestCompleted();
		}
	}

	private void initContextHolders(HttpServletRequest request, ServletRequestAttributes requestAttributes) {
		LocaleContextHolder.setLocale(request.getLocale(), this.threadContextInheritable);
		RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
		if (logger.isTraceEnabled()) {
			logger.trace("Bound request context to thread: " + request);
		}
	}

	private void resetContextHolders() {
		LocaleContextHolder.resetLocaleContext();
		RequestContextHolder.resetRequestAttributes();
	}

}
