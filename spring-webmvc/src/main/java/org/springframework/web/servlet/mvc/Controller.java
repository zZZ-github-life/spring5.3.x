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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;

/**
 * 基础 Controller 接口，表示一个接收
 * {@code HttpServletRequest} 和 {@code HttpServletResponse}
 * 实例的组件，其行为类似于 {@code HttpServlet}，但能够
 * 参与 MVC 工作流程。控制器可与 Struts 的
 * {@code Action} 概念相类比。
 *
 * <p>Controller 接口的任何实现都应是
 * <i>可复用、线程安全</i>的类，能够在应用程序的整个生命周期内
 * 处理多个 HTTP 请求。为了便于配置 Controller，鼓励将
 * Controller 实现为（通常也是如此）JavaBean。
 *
 * <h3><a name="workflow">工作流程</a></h3>
 *
 * <p>当 {@code DispatcherServlet} 接收到请求并完成了
 * 区域设置、主题等解析工作后，它会尝试
 * 使用 {@link org.springframework.web.servlet.HandlerMapping HandlerMapping}
 * 解析出一个 Controller。
 * 当找到用于处理该请求的 Controller 后，将调用
 * 所定位 Controller 的
 * {@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest}
 * 方法；随后，所定位的 Controller
 * 负责处理实际请求，并在适用的情况下
 * 返回适当的
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView}。
 * 因此实际上，此方法是
 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * 的主要入口点，由它将请求委托给控制器。
 *
 * <p>因此，基本上 {@code Controller} 接口的任何<i>直接</i>实现
 * 都只是处理 HttpServletRequest 并应返回 ModelAndView，以便由
 * DispatcherServlet 进一步解释。任何附加功能，例如
 * 可选的校验、表单处理等，都应通过扩展
 * {@link org.springframework.web.servlet.mvc.AbstractController AbstractController}
 * 或其某个子类来获得。
 *
 * <h3>关于设计与测试的说明</h3>
 *
 * <p>Controller 接口被明确设计为像 HttpServlet 一样
 * 操作 HttpServletRequest 和 HttpServletResponse 对象。
 * 与例如 WebWork、JSF 或 Tapestry 不同，它并不试图
 * 将自身与 Servlet API 解耦。
 * 相反，Servlet API 的全部能力都可用，这使得 Controller 可以
 * 成为通用组件：Controller 不仅能够处理 Web 用户界面
 * 请求，还能处理远程调用协议或按需生成报表。
 *
 * <p>通过将 HttpServletRequest 和 HttpServletResponse 对象的
 * 模拟对象作为参数传递给
 * {@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest}
 * 方法，可以轻松测试控制器。为方便起见，Spring 附带了一组
 * Servlet API 模拟对象，适用于测试任何类型的 Web 组件，但尤其
 * 适合测试 Spring Web 控制器。与 Struts Action 不同，
 * 无需模拟 ActionServlet 或任何其他基础设施；
 * 仅模拟 HttpServletRequest 和 HttpServletResponse 就足够了。
 *
 * <p>如果 Controller 需要感知特定的环境引用，它们可以
 * 选择实现特定的感知（awareness）接口，就像 Spring（Web）应用上下文中的
 * 任何其他 bean 一样，例如：
 * <ul>
 * <li>{@code org.springframework.context.ApplicationContextAware}</li>
 * <li>{@code org.springframework.context.ResourceLoaderAware}</li>
 * <li>{@code org.springframework.web.context.ServletContextAware}</li>
 * </ul>
 *
 * <p>这类环境引用可以在测试环境中通过各感知接口中定义的
 * 相应 setter 轻松传入。
 * 通常建议尽量保持依赖最少：例如，如果只需要资源加载，
 * 就仅实现 ResourceLoaderAware。
 * 另一种选择是继承 WebApplicationObjectSupport 基类，它通过便捷的
 * 访问器提供所有这些引用，但要求在初始化时提供
 * ApplicationContext 引用。
 *
 * <p>Controller 可以使用
 * {@link org.springframework.web.context.request.WebRequest} 上的
 * {@code checkNotModified} 方法获得 HTTP 缓存支持。
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleControllerHandlerAdapter
 * @see AbstractController
 * @see org.springframework.mock.web.MockHttpServletRequest
 * @see org.springframework.mock.web.MockHttpServletResponse
 * @see org.springframework.context.ApplicationContextAware
 * @see org.springframework.context.ResourceLoaderAware
 * @see org.springframework.web.context.ServletContextAware
 * @see org.springframework.web.context.support.WebApplicationObjectSupport
 */
@FunctionalInterface
public interface Controller {

	/**
	 * 处理请求并返回一个 ModelAndView 对象，由 DispatcherServlet
	 * 进行渲染。返回 {@code null} 不是错误：它表示
	 * 此对象已自行完成请求处理，因此没有
	 * 需要渲染的 ModelAndView。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return a ModelAndView to render, or {@code null} if handled directly
	 * @throws Exception in case of errors
	 */
	@Nullable
	ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
