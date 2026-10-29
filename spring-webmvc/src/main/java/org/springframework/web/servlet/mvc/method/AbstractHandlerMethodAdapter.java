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

package org.springframework.web.servlet.mvc.method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;

/**
 * {@link HandlerAdapter} 实现的抽象基类，用于支持
 * 类型为 {@link HandlerMethod} 的处理器。
 * @author Arjen Poutsma
 * @since 3.1
 */
public abstract class AbstractHandlerMethodAdapter extends WebContentGenerator implements HandlerAdapter, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;


	public AbstractHandlerMethodAdapter() {
		// no restriction of HTTP methods by default
		super(false);
	}


	/**
	 * 指定此 HandlerAdapter bean 的顺序值。
	 * <p>默认值为 {@code Ordered.LOWEST_PRECEDENCE}，表示无序。
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 此实现要求处理器为 {@link HandlerMethod}。
	 * @param handler the handler instance to check
	 * @return whether this adapter can adapt the given handler
	 */
	@Override
	public final boolean supports(Object handler) {
		return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));
	}

	/**
	 * 给定处理器方法，返回此适配器是否支持它。
	 * @param handlerMethod the handler method to check
	 * @return whether this adapter can adapt the given method
	 */
	protected abstract boolean supportsInternal(HandlerMethod handlerMethod);

	/**
	 * 此实现要求处理器为 {@link HandlerMethod}。
	 */
	@Override
	@Nullable
	public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return handleInternal(request, response, (HandlerMethod) handler);
	}

	/**
	 * 使用给定的处理器方法来处理请求。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handlerMethod handler method to use. This object must have previously been passed to the
	 * {@link #supportsInternal(HandlerMethod)} this interface, which must have returned {@code true}.
	 * @return a ModelAndView object with the name of the view and the required model data,
	 * or {@code null} if the request has been handled directly
	 * @throws Exception in case of errors
	 */
	@Nullable
	protected abstract ModelAndView handleInternal(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception;

	/**
	 * 此实现要求处理器为 {@link HandlerMethod}。
	 */
	@Override
	@SuppressWarnings("deprecation")
	public final long getLastModified(HttpServletRequest request, Object handler) {
		return getLastModifiedInternal(request, (HandlerMethod) handler);
	}

	/**
	 * 约定与 {@link javax.servlet.http.HttpServlet#getLastModified(HttpServletRequest)} 相同。
	 * @param request current HTTP request
	 * @param handlerMethod handler method to use
	 * @return the lastModified value for the given handler
	 * @deprecated as of 5.3.9 along with
	 * {@link org.springframework.web.servlet.mvc.LastModified}.
	 */
	@Deprecated
	protected abstract long getLastModifiedInternal(HttpServletRequest request, HandlerMethod handlerMethod);

}
