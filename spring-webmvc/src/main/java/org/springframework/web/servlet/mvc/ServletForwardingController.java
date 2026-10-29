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

package org.springframework.web.servlet.mvc;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

/**
 * Spring Controller 实现，它转发到指定名称的 Servlet，
 * 即 web.xml 中的 "servlet-name"，而不是 URL 路径映射。
 * 目标 Servlet 甚至根本不需要在 web.xml 中配置 "servlet-mapping"：
 * 只要有 "servlet" 声明就足够了。
 *
 * <p>适用于通过 Spring 的分派基础设施调用现有 Servlet，
 * 例如对其请求应用 Spring HandlerInterceptor。即使在不支持 Servlet 过滤器的
 * 最小化 Servlet 容器中，这种方式也能工作。
 *
 * <p><b>示例：</b>web.xml 将所有 "/myservlet" 请求映射到一个 Spring 分派器。
 * 同时定义了一个自定义的 "myServlet"，但<i>没有</i>配置 Servlet 映射。
 *
 * <pre class="code">
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;myServlet&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;mypackage.TestServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;myDispatcher&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;org.springframework.web.servlet.DispatcherServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;myDispatcher&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/myservlet&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;</pre>
 *
 * <b>示例：</b>myDispatcher-servlet.xml 进而将 "/myservlet" 转发到你的
 * Servlet（通过 Servlet 名称标识）。所有此类请求都会经过
 * 配置的 HandlerInterceptor 链（例如 OpenSessionInViewInterceptor）。
 * 从 Servlet 的角度来看，一切都将照常工作。
 *
 * <pre class="code">
 * &lt;bean id="urlMapping" class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping"&gt;
 *   &lt;property name="interceptors"&gt;
 *     &lt;list&gt;
 *       &lt;ref bean="openSessionInViewInterceptor"/&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 *   &lt;property name="mappings"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="/myservlet"&gt;myServletForwardingController&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myServletForwardingController" class="org.springframework.web.servlet.mvc.ServletForwardingController"&gt;
 *   &lt;property name="servletName"&gt;&lt;value&gt;myServlet&lt;/value&gt;&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 * @author Juergen Hoeller
 * @since 1.1.1
 * @see ServletWrappingController
 * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor
 * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter
 */
public class ServletForwardingController extends AbstractController implements BeanNameAware {

	@Nullable
	private String servletName;

	@Nullable
	private String beanName;


	public ServletForwardingController() {
		super(false);
	}


	/**
	 * 设置要转发到的 Servlet 名称，
	 * 即 web.xml 中目标 Servlet 的 "servlet-name"。
	 * <p>默认值为该控制器的 bean 名称。
	 */
	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
		if (this.servletName == null) {
			this.servletName = name;
		}
	}


	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		ServletContext servletContext = getServletContext();
		Assert.state(servletContext != null, "No ServletContext");
		RequestDispatcher rd = servletContext.getNamedDispatcher(this.servletName);
		if (rd == null) {
			throw new ServletException("No servlet with name '" + this.servletName + "' defined in web.xml");
		}

		// If already included, include again, else forward.
		if (useInclude(request, response)) {
			rd.include(request, response);
			if (logger.isTraceEnabled()) {
				logger.trace("Included servlet [" + this.servletName +
						"] in ServletForwardingController '" + this.beanName + "'");
			}
		}
		else {
			rd.forward(request, response);
			if (logger.isTraceEnabled()) {
				logger.trace("Forwarded to servlet [" + this.servletName +
						"] in ServletForwardingController '" + this.beanName + "'");
			}
		}

		return null;
	}

	/**
	 * 确定使用 RequestDispatcher 的 {@code include} 还是
	 * {@code forward} 方法。
	 * <p>会检查请求中是否存在 include URI 属性（表示这是一次 include 请求），
	 * 以及响应是否已被提交。
	 * 在这两种情况下都会执行 include，因为此时已无法再执行 forward。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return {@code true} for include, {@code false} for forward
	 * @see javax.servlet.RequestDispatcher#forward
	 * @see javax.servlet.RequestDispatcher#include
	 * @see javax.servlet.ServletResponse#isCommitted
	 * @see org.springframework.web.util.WebUtils#isIncludeRequest
	 */
	protected boolean useInclude(HttpServletRequest request, HttpServletResponse response) {
		return (WebUtils.isIncludeRequest(request) || response.isCommitted());
	}

}
