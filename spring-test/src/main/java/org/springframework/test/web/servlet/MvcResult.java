/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.web.servlet;

import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 提供对已执行请求结果的访问。
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface MvcResult {

	/**
	 * 返回已执行的请求。
	 * @return the request, never {@code null}
	 */
	MockHttpServletRequest getRequest();

	/**
	 * 返回结果响应。
	 * @return the response, never {@code null}
	 */
	MockHttpServletResponse getResponse();

	/**
	 * 返回已执行的处理器。
	 * @return the handler, possibly {@code null} if none were executed
	 */
	@Nullable
	Object getHandler();

	/**
	 * 返回处理器周围的拦截器。
	 * @return interceptors, or {@code null} if none were selected
	 */
	@Nullable
	HandlerInterceptor[] getInterceptors();

	/**
	 * 返回由处理器准备的 {@code ModelAndView}。
	 * @return a {@code ModelAndView}, or {@code null} if none
	 */
	@Nullable
	ModelAndView getModelAndView();

	/**
	 * 返回处理器抛出并已通过 {@link HandlerExceptionResolver}
	 * 成功解析的任何异常。
	 * @return an exception, or {@code null} if none
	 */
	@Nullable
	Exception getResolvedException();

	/**
	 * 返回请求处理期间保存的“输出”Flash 属性。
	 * @return the {@code FlashMap}, possibly empty
	 */
	FlashMap getFlashMap();

	/**
	 * 获取异步执行的结果。
	 * <p>此方法将等待在异步请求关联的超时值内设置异步结果，请参见
	 * {@link org.springframework.mock.web.MockAsyncContext#setTimeout
	 * MockAsyncContext#setTimeout}。或者，使用
	 * {@link #getAsyncResult(long)} 指定等待时间。
	 * @throws IllegalStateException if the async result was not set
	 */
	Object getAsyncResult();

	/**
	 * 获取异步执行的结果，并在必要时等待。
	 * @param timeToWait how long to wait for the async result to be set, in
	 * 	milliseconds; if -1, then fall back on the timeout value associated with
	 * 	the async request, see
	 * 	{@link org.springframework.mock.web.MockAsyncContext#setTimeout
	 * 	MockAsyncContext#setTimeout} for more details.
	 * @throws IllegalStateException if the async result was not set
	 */
	Object getAsyncResult(long timeToWait);

}
