/*
 * Copyright 2002-2020 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * 抽象基类，用于支持处理 {@link HandlerMethod} 类型处理器
 * 所抛出异常的
 * {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver}
 * 实现。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractHandlerMethodExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * 检查处理器是否为 {@link HandlerMethod}，如果是，则将
	 * {@code HandlerMethod} 的 bean 传递给基类实现
	 * {@code #shouldApplyTo(HttpServletRequest, Object)}。否则返回
	 * {@code false}。
	 */
	@Override
	protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
		if (handler == null) {
			return super.shouldApplyTo(request, null);
		}
		else if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			handler = handlerMethod.getBean();
			return super.shouldApplyTo(request, handler);
		}
		else if (hasGlobalExceptionHandlers() && hasHandlerMappings()) {
			return super.shouldApplyTo(request, handler);
		}
		else {
			return false;
		}
	}

	/**
	 * 此解析器是否具有全局异常处理器（例如未在引发异常的
	 * {@code HandlerMethod} 所在类中声明），
	 * 因而可以应用于任何处理器。
	 * @since 5.3
	 */
	protected boolean hasGlobalExceptionHandlers() {
		return false;
	}

	@Override
	@Nullable
	protected final ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		HandlerMethod handlerMethod = (handler instanceof HandlerMethod ? (HandlerMethod) handler : null);
		return doResolveHandlerMethodException(request, response, handlerMethod, ex);
	}

	/**
	 * 实际解析处理器执行期间抛出的给定异常，
	 * 如果合适，返回表示特定错误页面的 ModelAndView。
	 * <p>可以在子类中重写，以应用特定的异常检查。
	 * 请注意，此模板方法会在检查此解析器是否适用（"mappedHandlers" 等）
	 * <i>之后</i>被调用，因此实现可以简单地继续执行
	 * 实际的异常处理。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handlerMethod the executed handler method, or {@code null} if none chosen at the time
	 * of the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to, or {@code null} for default processing
	 */
	@Nullable
	protected abstract ModelAndView doResolveHandlerMethodException(
			HttpServletRequest request, HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception ex);

}
