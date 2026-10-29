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

/**
 * MVC 框架的 SPI，允许对核心 MVC 工作流进行参数化。
 *
 * <p>每种处理请求的处理器类型都必须实现该接口。
 * 该接口使得 {@link DispatcherServlet} 可以无限扩展。{@code DispatcherServlet}
 * 通过该接口访问所有已安装的处理器，这意味着它不包含任何特定于某种处理器类型的代码。
 *
 * <p>请注意，处理器的类型可以是 {@code Object}。这样做是为了让其他框架的处理器
 * 无需自定义编码即可集成到本框架中，同时也允许使用不遵循任何特定 Java 接口的
 * 注解驱动处理器对象。
 *
 * <p>该接口并非面向应用开发者。它提供给希望开发自身 Web 工作流的处理器使用。
 *
 * <p>注意：{@code HandlerAdapter} 的实现类可以实现
 * {@link org.springframework.core.Ordered} 接口，以便指定排序顺序（也就是优先级），
 * 从而决定被 {@code DispatcherServlet} 应用的先后次序。
 * 未实现 Ordered 的实例会被视为最低优先级。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter
 * @see org.springframework.web.servlet.handler.SimpleServletHandlerAdapter
 */
public interface HandlerAdapter {

	/**
	 * 给定一个处理器实例，返回此 {@code HandlerAdapter} 是否支持它。
	 * 典型的 HandlerAdapter 会基于处理器类型作出判断。
	 * HandlerAdapter 通常每个只支持一种处理器类型。
	 * <p>典型的实现方式如下：
	 * <p>{@code
	 * return (handler instanceof MyHandler);
	 * }
	 * @param handler the handler object to check
	 * @return whether this object can use the given handler
	 */
	boolean supports(Object handler);

	/**
	 * 使用给定的处理器处理此请求。
	 * 所需的工作流可能千差万别。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the handler to use. This object must have previously been passed
	 * to the {@code supports} method of this interface, which must have
	 * returned {@code true}.
	 * @return a ModelAndView object with the name of the view and the required
	 * model data, or {@code null} if the request has been handled directly
	 * @throws Exception in case of errors
	 */
	@Nullable
	ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

	/**
	 * 与 HttpServlet 的 {@code getLastModified} 方法具有相同的约定。
	 * 如果处理器类不支持该功能，可以直接返回 -1。
	 * @param request current HTTP request
	 * @param handler the handler to use
	 * @return the lastModified value for the given handler
	 * @deprecated as of 5.3.9 along with
	 * {@link org.springframework.web.servlet.mvc.LastModified}.
	 */
	@Deprecated
	long getLastModified(HttpServletRequest request, Object handler);

}
