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

package org.springframework.web.servlet.handler;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * 适配器，用于将 Servlet 接口与通用 DispatcherServlet 配合使用。
 * 调用 Servlet 的 {@code service} 方法来处理请求。
 *
 * <p>不显式支持最后修改时间检查：这通常由
 * Servlet 实现自身处理（通常派生自
 * HttpServlet 基类）。
 *
 * <p>此适配器默认不会激活；需要在 DispatcherServlet 上下文中
 * 将其定义为 bean。届时它会自动应用于实现了 Servlet 接口的
 * 已映射处理器 bean。
 *
 * <p>请注意，定义为 bean 的 Servlet 实例不会收到初始化和
 * 销毁回调，除非在 DispatcherServlet 上下文中定义了特殊的
 * 后置处理器，例如 SimpleServletPostProcessor。
 *
 * <p><b>或者，可以考虑用 Spring 的
 * ServletWrappingController 包装 Servlet。</b>这对于
 * 现有的 Servlet 类尤其合适，允许指定 Servlet 初始化
 * 参数等。
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see javax.servlet.Servlet
 * @see javax.servlet.http.HttpServlet
 * @see SimpleServletPostProcessor
 * @see org.springframework.web.servlet.mvc.ServletWrappingController
 */
public class SimpleServletHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Servlet);
	}

	@Override
	@Nullable
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		((Servlet) handler).service(request, response);
		return null;
	}

	@Override
	@SuppressWarnings("deprecation")
	public long getLastModified(HttpServletRequest request, Object handler) {
		return -1;
	}

}
