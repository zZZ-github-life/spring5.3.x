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

package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

/**
 * 用于为 Web 应用程序提供配置的接口。在应用程序运行期间它是只读的，
 * 但如果实现支持，也可以重新加载。
 *
 * <p>此接口在通用 ApplicationContext 接口的基础上添加了 {@code getServletContext()}
 * 方法，并定义了一个众所周知的应用程序属性名，根上下文必须在引导过程中绑定到该名称。
 *
 * <p>与通用应用上下文一样，Web 应用上下文也是分层的。每个应用程序有一个根上下文，
 * 而应用程序中的每个 servlet（包括 MVC 框架中的 dispatcher servlet）都有自己的子上下文。
 *
 * <p>除了标准应用上下文生命周期能力之外，WebApplicationContext 实现还需要
 * 检测 {@link ServletContextAware} bean 并相应地调用 {@code setServletContext} 方法。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since January 19, 2001
 * @see ServletContextAware#setServletContext
 */
public interface WebApplicationContext extends ApplicationContext {

	/**
	 * 在成功启动时用于绑定根 WebApplicationContext 的上下文属性。
	 * <p>注意：如果根上下文启动失败，此属性可能包含异常或错误作为值。
	 * 使用 WebApplicationContextUtils 可以方便地查找根 WebApplicationContext。
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getRequiredWebApplicationContext
	 */
	String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

	/**
	 * request 作用域的作用域标识符："request"。
	 * 除标准作用域 "singleton" 和 "prototype" 之外还受支持。
	 */
	String SCOPE_REQUEST = "request";

	/**
	 * session 作用域的作用域标识符："session"。
	 * 除标准作用域 "singleton" 和 "prototype" 之外还受支持。
	 */
	String SCOPE_SESSION = "session";

	/**
	 * 全局 Web 应用程序作用域的作用域标识符："application"。
	 * 除标准作用域 "singleton" 和 "prototype" 之外还受支持。
	 */
	String SCOPE_APPLICATION = "application";

	/**
	 * 工厂中 ServletContext 环境 bean 的名称。
	 * @see javax.servlet.ServletContext
	 */
	String SERVLET_CONTEXT_BEAN_NAME = "servletContext";

	/**
	 * 工厂中 ServletContext 初始化参数环境 bean 的名称。
	 * <p>注意：可能会与 ServletConfig 参数合并。
	 * ServletConfig 参数会覆盖同名的 ServletContext 参数。
	 * @see javax.servlet.ServletContext#getInitParameterNames()
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 * @see javax.servlet.ServletConfig#getInitParameterNames()
	 * @see javax.servlet.ServletConfig#getInitParameter(String)
	 */
	String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

	/**
	 * 工厂中 ServletContext 属性环境 bean 的名称。
	 * @see javax.servlet.ServletContext#getAttributeNames()
	 * @see javax.servlet.ServletContext#getAttribute(String)
	 */
	String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";


	/**
	 * 返回此应用程序的标准 Servlet API ServletContext。
	 */
	@Nullable
	ServletContext getServletContext();

}
