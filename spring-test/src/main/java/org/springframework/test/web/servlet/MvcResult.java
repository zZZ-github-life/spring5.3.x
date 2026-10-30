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
	 * @return 请求，永不为 {@code null}
	 */
	MockHttpServletRequest getRequest();

	/**
	 * 返回结果响应。
	 * @return 响应，永不为 {@code null}
	 */
	MockHttpServletResponse getResponse();

	/**
	 * 返回已执行的处理器。
	 * @return 处理器，如果没有执行任何处理器则可能为 {@code null}
	 */
	@Nullable
	Object getHandler();

	/**
	 * 返回处理器周围的拦截器。
	 * @return 拦截器，如果没有选择任何拦截器则为 {@code null}
	 */
	@Nullable
	HandlerInterceptor[] getInterceptors();

	/**
	 * 返回由处理器准备的 {@code ModelAndView}。
	 * @return 一个 {@code ModelAndView}，如果没有则为 {@code null}
	 */
	@Nullable
	ModelAndView getModelAndView();

	/**
	 * 返回处理器抛出并已通过 {@link HandlerExceptionResolver}
	 * 成功解析的任何异常。
	 * @return 一个异常，如果没有则为 {@code null}
	 */
	@Nullable
	Exception getResolvedException();

	/**
	 * 返回请求处理期间保存的“输出”Flash 属性。
	 * @return {@code FlashMap}，可能为空
	 */
	FlashMap getFlashMap();

	/**
	 * 获取异步执行的结果。
	 * <p>此方法将等待在异步请求关联的超时值内设置异步结果，请参见
	 * {@link org.springframework.mock.web.MockAsyncContext#setTimeout
	 * MockAsyncContext#setTimeout}。或者，使用
	 * {@link #getAsyncResult(long)} 指定等待时间。
	 * @throws IllegalStateException 如果未设置异步结果
	 */
	Object getAsyncResult();

	/**
	 * 获取异步执行的结果，并在必要时等待。
	 * @param timeToWait 等待异步结果被设置的时间，单位为毫秒；
	 * 如果为 -1，则回退到与异步请求关联的超时值，
	 * 详见 {@link org.springframework.mock.web.MockAsyncContext#setTimeout
	 * MockAsyncContext#setTimeout}。
	 * @throws IllegalStateException 如果未设置异步结果
	 */
	Object getAsyncResult(long timeToWait);

}
