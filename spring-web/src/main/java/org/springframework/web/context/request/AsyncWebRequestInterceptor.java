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

package org.springframework.web.context.request;

/**
 * 扩展 {@code WebRequestInterceptor}，新增一个在异步请求处理期间调用的回调方法。
 *
 * <p>当处理器开始异步请求处理时，DispatcherServlet 会像平常一样退出，但不调用 {@code postHandle}
 * 和 {@code afterCompletion}，因为请求处理的结果（例如 ModelAndView）在当前线程中尚不可用，且处
 * 理尚未完成。在此类场景中，改为调用 {@link #afterConcurrentHandlingStarted(WebRequest)} 方法，
 * 使实现能够执行诸如清理线程绑定属性之类的任务。
 *
 * <p>当异步处理完成后，请求会被分派回容器以进一步处理。在此阶段，DispatcherServlet 会像往常一样
 * 调用 {@code preHandle}、{@code postHandle} 和 {@code afterCompletion}。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see org.springframework.web.context.request.async.WebAsyncManager
 */
public interface AsyncWebRequestInterceptor extends WebRequestInterceptor{

	/**
	 * 当处理器开始并发处理请求时，代替 {@code postHandle} 和 {@code afterCompletion} 被调用。
	 * @param request 当前请求
	 */
	void afterConcurrentHandlingStarted(WebRequest request);

}
