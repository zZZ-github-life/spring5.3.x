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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;

/**
 * 扩展 {@code HandlerInterceptor}，增加一个在异步请求处理开始后调用的回调方法。
 *
 * <p>当处理器开始异步请求时，{@link DispatcherServlet} 会直接退出，
 * 而不会像处理同步请求那样调用 {@code postHandle} 和 {@code afterCompletion}，
 * 因为请求处理的结果（例如 ModelAndView）很可能尚未就绪，
 * 并将由另一个线程并发产生。在此类场景中，将改为调用
 * {@link #afterConcurrentHandlingStarted}，允许实现在将线程释放回
 * Servlet 容器之前执行诸如清理线程绑定属性等任务。
 *
 * <p>当异步处理完成时，请求会被重新分派到容器以进行后续处理。
 * 在此阶段，{@code DispatcherServlet} 会调用 {@code preHandle}、
 * {@code postHandle} 和 {@code afterCompletion}。
 * 为了区分初始请求与异步处理完成后的后续分派，拦截器可以检查
 * {@link javax.servlet.ServletRequest} 的 {@code javax.servlet.DispatcherType}
 * 是 {@code "REQUEST"} 还是 {@code "ASYNC"}。
 *
 * <p>请注意，{@code HandlerInterceptor} 的实现可能需要在异步请求超时
 * 或因网络错误而完成时执行一些工作。对于这些情况，Servlet 容器不会进行分派，
 * 因此 {@code postHandle} 和 {@code afterCompletion} 方法将不会被调用。
 * 取而代之的是，拦截器可以通过
 * {@link org.springframework.web.context.request.async.WebAsyncManager
 * WebAsyncManager} 上的 {@code registerCallbackInterceptor} 和
 * {@code registerDeferredResultInterceptor} 方法注册以跟踪异步请求。
 * 无论异步请求处理是否会启动，都可以在每个请求的 {@code preHandle} 中
 * 主动完成此操作。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @see org.springframework.web.context.request.async.WebAsyncManager
 * @see org.springframework.web.context.request.async.CallableProcessingInterceptor
 * @see org.springframework.web.context.request.async.DeferredResultProcessingInterceptor
 */
public interface AsyncHandlerInterceptor extends HandlerInterceptor {

	/**
	 * 当处理器正在并发执行时调用，用于替代 {@code postHandle} 和
	 * {@code afterCompletion}。
	 * <p>实现可以使用所提供的请求和响应，但应避免以与处理器并发执行
	 * 相冲突的方式修改它们。此方法的典型用途是清理线程局部变量。
	 * @param request the current request
	 * @param response the current response
	 * @param handler the handler (or {@link HandlerMethod}) that started async
	 * execution, for type and/or instance examination
	 * @throws Exception in case of errors
	 */
	default void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
	}

}
