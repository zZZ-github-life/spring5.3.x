/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.ModelAndView;

/**
 * Spring Controller 实现，它在内部包装并管理一个 Servlet 实例。
 * 被包装的 Servlet 在此控制器之外不可见；
 * 其整个生命周期都在此处管理（与 {@link ServletForwardingController} 不同）。
 *
 * <p>适用于通过 Spring 的分派基础设施调用现有 Servlet，
 * 例如对其请求应用 Spring HandlerInterceptor。
 *
 * <p>请注意，Struts 有一个特殊要求：它会解析 {@code web.xml}
 * 以查找其 Servlet 映射。因此，需要在此控制器上将 DispatcherServlet 的
 * Servlet 名称指定为 "servletName"，以便 Struts 能够找到
 * DispatcherServlet 的映射（并认为它指向的是 ActionServlet）。
 *
 * <p><b>示例：</b>一个 DispatcherServlet XML 上下文，将 "*.do" 转发到由
 * ServletWrappingController 包装的 Struts ActionServlet。所有此类请求都会经过
 * 配置的 HandlerInterceptor 链（例如 OpenSessionInViewInterceptor）。
 * 从 Struts 的角度来看，一切都将照常工作。
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
 *       &lt;prop key="*.do"&gt;strutsWrappingController&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="strutsWrappingController" class="org.springframework.web.servlet.mvc.ServletWrappingController"&gt;
 *   &lt;property name="servletClass"&gt;
 *     &lt;value&gt;org.apache.struts.action.ActionServlet&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="servletName"&gt;
 *     &lt;value&gt;action&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="initParameters"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="config"&gt;/WEB-INF/struts-config.xml&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 * @author Juergen Hoeller
 * @since 1.1.1
 * @see ServletForwardingController
 */
public class ServletWrappingController extends AbstractController
		implements BeanNameAware, InitializingBean, DisposableBean {

	@Nullable
	private Class<? extends Servlet> servletClass;

	@Nullable
	private String servletName;

	private Properties initParameters = new Properties();

	@Nullable
	private String beanName;

	@Nullable
	private Servlet servletInstance;


	public ServletWrappingController() {
		super(false);
	}


	/**
	 * 设置要包装的 Servlet 类。
	 * 需要实现 {@code javax.servlet.Servlet}。
	 * @see javax.servlet.Servlet
	 */
	public void setServletClass(Class<? extends Servlet> servletClass) {
		this.servletClass = servletClass;
	}

	/**
	 * 设置要包装的 Servlet 名称。
	 * 默认值为该控制器的 bean 名称。
	 */
	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	/**
	 * 以名称-值对的形式为要包装的 Servlet
	 * 指定初始化参数。
	 */
	public void setInitParameters(Properties initParameters) {
		this.initParameters = initParameters;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	/**
	 * 初始化被包装的 Servlet 实例。
	 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.servletClass == null) {
			throw new IllegalArgumentException("'servletClass' is required");
		}
		if (this.servletName == null) {
			this.servletName = this.beanName;
		}
		this.servletInstance = ReflectionUtils.accessibleConstructor(this.servletClass).newInstance();
		this.servletInstance.init(new DelegatingServletConfig());
	}


	/**
	 * 调用被包装的 Servlet 实例。
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Assert.state(this.servletInstance != null, "No Servlet instance");
		this.servletInstance.service(request, response);
		return null;
	}


	/**
	 * 销毁被包装的 Servlet 实例。
	 * @see javax.servlet.Servlet#destroy()
	 */
	@Override
	public void destroy() {
		if (this.servletInstance != null) {
			this.servletInstance.destroy();
		}
	}


	/**
	 * ServletConfig 接口的内部实现，用于传递给
	 * 被包装的 Servlet。委托到 ServletWrappingController 的字段
	 * 和方法，以提供初始化参数及其他环境信息。
	 */
	private class DelegatingServletConfig implements ServletConfig {

		@Override
		@Nullable
		public String getServletName() {
			return servletName;
		}

		@Override
		@Nullable
		public ServletContext getServletContext() {
			return ServletWrappingController.this.getServletContext();
		}

		@Override
		public String getInitParameter(String paramName) {
			return initParameters.getProperty(paramName);
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public Enumeration<String> getInitParameterNames() {
			return (Enumeration) initParameters.keys();
		}
	}

}
