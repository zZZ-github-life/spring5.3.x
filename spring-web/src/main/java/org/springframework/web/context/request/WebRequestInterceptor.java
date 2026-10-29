/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.context.request;

import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;

/**
 * 用于通用 Web 请求拦截的接口。通过基于 {@link WebRequest} 抽象构建，可将其应用于 Servlet 请求。
 *
 * <p>此接口假定采用 MVC 风格的请求处理：处理器被执行，暴露一组模型对象，然后基于该模型渲染视图。
 * 或者，处理器也可以完全处理请求而不渲染任何视图。
 *
 * <p>在异步处理场景中，处理器可能在单独的线程中执行，而主线程退出时既不渲染视图，
 * 也不调用 {@code postHandle} 和 {@code afterCompletion} 回调。当并发处理器执行完成后，
 * 请求会被重新分派回来以继续渲染模型，并且此约定的所有方法会再次被调用。
 * 有关更多选项和说明，请参见
 * {@code org.springframework.web.context.request.async.AsyncWebRequestInterceptor}
 *
 * <p>此接口刻意保持极简，以尽可能减少通用请求拦截器的依赖。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ServletWebRequest
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setInterceptors
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public interface WebRequestInterceptor {

	/**
	 * 在请求处理器被调用<i>之前</i>拦截其执行。
	 * <p>可用于准备上下文资源（例如 Hibernate Session），并将其暴露为请求属性或线程本地对象。
	 * @param request the current web request
	 * @throws Exception in case of errors
	 */
	void preHandle(WebRequest request) throws Exception;

	/**
	 * 在请求处理器成功调用<i>之后</i>、渲染视图（如果有）之前拦截其执行。
	 * <p>可用于在处理器成功执行后修改上下文资源（例如刷新 Hibernate Session）。
	 * @param request the current web request
	 * @param model the map of model objects that will be exposed to the view
	 * (may be {@code null}). Can be used to analyze the exposed model
	 * and/or to add further model attributes, if desired.
	 * @throws Exception in case of errors
	 */
	void postHandle(WebRequest request, @Nullable ModelMap model) throws Exception;

	/**
	 * 请求处理完成后的回调，即在渲染视图之后调用。无论处理器执行结果如何都会调用，因此可用于正确清理
	 * 资源。
	 * <p>注意：仅当此拦截器的 {@code preHandle} 方法成功完成后才会调用！
	 * @param request the current web request
	 * @param ex exception thrown on handler execution, if any
	 * @throws Exception in case of errors
	 */
	void afterCompletion(WebRequest request, @Nullable Exception ex) throws Exception;

}
