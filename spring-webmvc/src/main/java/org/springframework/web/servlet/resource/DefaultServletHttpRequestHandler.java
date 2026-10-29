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

package org.springframework.web.servlet.resource;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link HttpRequestHandler}，用于使用 Servlet 容器的“默认”Servlet
 * 提供静态文件服务。
 *
 * <p>当 {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * 映射到 "/" 时，此处理器旨在与 "/*" 映射一起使用，
 * 从而覆盖 Servlet 容器对静态资源的默认处理。
 * 对此处理器的映射通常应排在链的最后，以便仅当
 * 无法匹配其他更具体的映射（即到控制器的映射）时才会执行。
 *
 * <p>请求处理通过 {@link RequestDispatcher} 转发完成，
 * 该 RequestDispatcher 通过 {@link #setDefaultServletName "defaultServletName" 属性}指定的名称获取。
 * 在大多数情况下，无需显式设置 {@code defaultServletName}，因为
 * 处理器会在初始化时检查知名容器（如 Tomcat、Jetty、Resin、WebLogic 和 WebSphere）
 * 的默认 Servlet 是否存在。不过，如果在
 * 默认 Servlet 名称未知的容器中运行，或者其名称已通过服务器配置
 * 自定义，则需要显式设置 {@code defaultServletName}。
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @since 3.0.4
 */
public class DefaultServletHttpRequestHandler implements HttpRequestHandler, ServletContextAware {

	/** Tomcat、Jetty、JBoss 和 GlassFish 使用的默认 Servlet 名称。 */
	private static final String COMMON_DEFAULT_SERVLET_NAME = "default";

	/** Google App Engine 使用的默认 Servlet 名称。 */
	private static final String GAE_DEFAULT_SERVLET_NAME = "_ah_default";

	/** Resin 使用的默认 Servlet 名称。 */
	private static final String RESIN_DEFAULT_SERVLET_NAME = "resin-file";

	/** WebLogic 使用的默认 Servlet 名称。 */
	private static final String WEBLOGIC_DEFAULT_SERVLET_NAME = "FileServlet";

	/** WebSphere 使用的默认 Servlet 名称。 */
	private static final String WEBSPHERE_DEFAULT_SERVLET_NAME = "SimpleFileServlet";


	@Nullable
	private String defaultServletName;

	@Nullable
	private ServletContext servletContext;


	/**
	 * 设置静态资源请求要转发到的默认 Servlet 名称。
	 */
	public void setDefaultServletName(String defaultServletName) {
		this.defaultServletName = defaultServletName;
	}

	/**
	 * 如果未显式设置 {@code defaultServletName} 属性，
	 * 则尝试使用已知的常见容器特定名称
	 * 定位默认 Servlet。
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
		if (!StringUtils.hasText(this.defaultServletName)) {
			if (this.servletContext.getNamedDispatcher(COMMON_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = COMMON_DEFAULT_SERVLET_NAME;
			}
			else if (this.servletContext.getNamedDispatcher(GAE_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = GAE_DEFAULT_SERVLET_NAME;
			}
			else if (this.servletContext.getNamedDispatcher(RESIN_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = RESIN_DEFAULT_SERVLET_NAME;
			}
			else if (this.servletContext.getNamedDispatcher(WEBLOGIC_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = WEBLOGIC_DEFAULT_SERVLET_NAME;
			}
			else if (this.servletContext.getNamedDispatcher(WEBSPHERE_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = WEBSPHERE_DEFAULT_SERVLET_NAME;
			}
			else {
				throw new IllegalStateException("Unable to locate the default servlet for serving static content. " +
						"Please set the 'defaultServletName' property explicitly.");
			}
		}
	}


	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Assert.state(this.servletContext != null, "No ServletContext set");
		RequestDispatcher rd = this.servletContext.getNamedDispatcher(this.defaultServletName);
		if (rd == null) {
			throw new IllegalStateException("A RequestDispatcher could not be located for the default servlet '" +
					this.defaultServletName + "'");
		}
		rd.forward(request, response);
	}

}
