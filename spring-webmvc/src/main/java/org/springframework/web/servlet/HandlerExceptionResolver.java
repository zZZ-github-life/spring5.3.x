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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 由能够解析处理器映射或处理器执行过程中抛出的异常的对象实现的接口，
 * 典型情况下解析为错误视图。实现类通常注册为应用上下文中的 bean。
 *
 * <p>错误视图类似于 JSP 错误页，但可以用于任何类型的异常（包括受检异常），
 * 并且可以为特定处理器提供细粒度的映射。
 *
 * @author Juergen Hoeller
 * @since 22.11.2003
 */
public interface HandlerExceptionResolver {

	/**
	 * 尝试解析处理器执行期间抛出的给定异常，
	 * 并在适当的情况下返回表示特定错误页面的 {@link ModelAndView}。
	 * <p>返回的 {@code ModelAndView} 可以是 {@linkplain ModelAndView#isEmpty() 空的}，
	 * 表示异常已成功解析但不应渲染任何视图，例如仅设置状态码。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the
	 * time of the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding {@code ModelAndView} to forward to,
	 * or {@code null} for default processing in the resolution chain
	 */
	@Nullable
	ModelAndView resolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex);

}
