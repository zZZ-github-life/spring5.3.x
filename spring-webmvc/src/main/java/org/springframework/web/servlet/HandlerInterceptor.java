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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;

/**
 * 允许自定义处理器执行链的工作流接口。
 * 应用可以为特定的处理器组注册任意数量的现有或自定义拦截器，
 * 从而添加通用的预处理行为，而无需修改每个处理器实现。
 *
 * <p>HandlerInterceptor 会在适当的 HandlerAdapter 触发处理器本身执行之前被调用。
 * 该机制可用于大量预处理方面的场景，例如授权检查，
 * 或者诸如区域设置、主题变更之类的通用处理器行为。
 * 其主要目的是将重复的处理器代码抽取出来。
 *
 * <p>在异步处理场景中，处理器可能在单独的线程中执行，而主线程退出时不会进行渲染，
 * 也不会调用 {@code postHandle} 和 {@code afterCompletion} 回调。
 * 当并发处理器执行完成后，请求会被重新分派，以便继续渲染模型，
 * 此时本约定中的所有方法都会再次被调用。更多选项和细节请参见
 * {@code org.springframework.web.servlet.AsyncHandlerInterceptor}
 *
 * <p>通常拦截器链是按 HandlerMapping bean 定义的，并共享其粒度。
 * 若要将某个拦截器链应用于一组处理器，需要通过一个 HandlerMapping bean
 * 映射所需的处理器。拦截器本身在应用上下文中定义为 bean，
 * 并由映射 bean 定义通过其 "interceptors" 属性引用（在 XML 中是一个 &lt;list&gt;，包含若干 &lt;ref&gt;）。
 *
 * <p>HandlerInterceptor 基本上类似于 Servlet Filter，但与后者不同的是，
 * 它仅允许自定义预处理（可以选择阻止处理器本身的执行）以及自定义后处理。
 * Filter 的功能更强大，例如它允许替换沿链传递的请求和响应对象。
 * 请注意，Filter 在 web.xml 中配置，而 HandlerInterceptor 在应用上下文中配置。
 *
 * <p>作为基本准则，细粒度的处理器相关预处理任务适合由 HandlerInterceptor 实现，
 * 特别是抽取出来的通用处理器代码和授权检查。另一方面，Filter 非常适合
 * 请求内容和视图内容的处理，例如 multipart 表单和 GZIP 压缩。
 * 当需要将过滤器映射到特定内容类型（例如图片）或所有请求时，这一点通常很明显。
 *
 * @author Juergen Hoeller
 * @since 20.06.2003
 * @see HandlerExecutionChain#getInterceptors
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setInterceptors
 * @see org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor
 * @see org.springframework.web.servlet.i18n.LocaleChangeInterceptor
 * @see org.springframework.web.servlet.theme.ThemeChangeInterceptor
 * @see javax.servlet.Filter
 */
public interface HandlerInterceptor {

	/**
	 * 处理器执行之前的拦截点。在 HandlerMapping 确定合适的处理器对象之后、
	 * HandlerAdapter 调用处理器之前被调用。
	 * <p>DispatcherServlet 按执行链处理处理器，该执行链由任意数量的拦截器组成，
	 * 处理器本身位于末尾。通过此方法，每个拦截器都可以决定中止执行链，
	 * 通常是发送 HTTP 错误或写入自定义响应。
	 * <p><strong>注意：</strong>异步请求处理有特殊的注意事项。
	 * 更多细节请参见 {@link org.springframework.web.servlet.AsyncHandlerInterceptor}。
	 * <p>默认实现返回 {@code true}。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler chosen handler to execute, for type and/or instance evaluation
	 * @return {@code true} if the execution chain should proceed with the
	 * next interceptor or the handler itself. Else, DispatcherServlet assumes
	 * that this interceptor has already dealt with the response itself.
	 * @throws Exception in case of errors
	 */
	default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return true;
	}

	/**
	 * 处理器成功执行之后的拦截点。
	 * 在 HandlerAdapter 实际调用处理器之后、DispatcherServlet 渲染视图之前被调用。
	 * 可以通过给定的 ModelAndView 向视图暴露额外的模型对象。
	 * <p>DispatcherServlet 按执行链处理处理器，该执行链由任意数量的拦截器组成，
	 * 处理器本身位于末尾。通过此方法，每个拦截器都可以对一次执行进行后处理，
	 * 应用顺序与执行链的顺序相反。
	 * <p><strong>注意：</strong>异步请求处理有特殊的注意事项。
	 * 更多细节请参见 {@link org.springframework.web.servlet.AsyncHandlerInterceptor}。
	 * <p>默认实现为空。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the handler (or {@link HandlerMethod}) that started asynchronous
	 * execution, for type and/or instance examination
	 * @param modelAndView the {@code ModelAndView} that the handler returned
	 * (can also be {@code null})
	 * @throws Exception in case of errors
	 */
	default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
	}

	/**
	 * 请求处理完成之后（即渲染视图之后）的回调。
	 * 无论处理器执行的结果如何都会被调用，因此可用于正确清理资源。
	 * <p>注意：只有当此拦截器的 {@code preHandle} 方法成功完成并返回 {@code true} 时才会被调用！
	 * <p>与 {@code postHandle} 方法一样，该方法会按相反顺序在每个拦截器上调用，
	 * 因此第一个拦截器将最后被调用。
	 * <p><strong>注意：</strong>异步请求处理有特殊的注意事项。
	 * 更多细节请参见 {@link org.springframework.web.servlet.AsyncHandlerInterceptor}。
	 * <p>默认实现为空。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the handler (or {@link HandlerMethod}) that started asynchronous
	 * execution, for type and/or instance examination
	 * @param ex any exception thrown on handler execution, if any; this does not
	 * include exceptions that have been handled through an exception resolver
	 * @throws Exception in case of errors
	 */
	default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
	}

}
