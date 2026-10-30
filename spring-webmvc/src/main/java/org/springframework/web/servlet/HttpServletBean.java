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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * {@link javax.servlet.http.HttpServlet} 的简单扩展，它将配置参数
 * （{@code web.xml} 中 {@code servlet} 标签内的 {@code init-param} 条目）
 * 视为 bean 属性。
 *
 * <p>这是适用于任何类型 Servlet 的便捷超类。配置参数的类型转换是自动完成的，
 * 相应的 setter 方法会以转换后的值被调用。子类还可以指定必需的属性。
 * 没有匹配 bean 属性 setter 的参数将被直接忽略。
 *
 * <p>此 Servlet 将请求处理留给子类，继承 HttpServlet 的默认行为
 * （{@code doGet}、{@code doPost} 等）。
 *
 * <p>此通用 Servlet 基类不依赖于 Spring 的
 * {@link org.springframework.context.ApplicationContext} 概念。
 * 简单的 Servlet 通常不会加载自己的上下文，而是从 Spring 根应用上下文中
 * 访问服务 bean，可通过过滤器的 {@link #getServletContext() ServletContext} 访问
 * （参见 {@link org.springframework.web.context.support.WebApplicationContextUtils}）。
 *
 * <p>{@link FrameworkServlet} 类是一个更具体的 Servlet 基类，
 * 它会加载自己的应用上下文。FrameworkServlet 是 Spring 功能完备的
 * {@link DispatcherServlet} 的直接基类。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initServletBean
 * @see #doGet
 * @see #doPost
 */
@SuppressWarnings("serial")
public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConfigurableEnvironment environment;

	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * 子类可以调用此方法来指定某个属性（必须与其暴露的某个 JavaBean 属性匹配）
	 * 是必需的，并且必须作为配置参数提供。应从子类的构造函数中调用此方法。
	 * <p>此方法仅在由 ServletConfig 实例驱动的传统初始化场景中才有意义。
	 * @param property 必需属性的名称
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * 设置此 Servlet 运行所处的 {@code Environment}。
	 * <p>此处设置的任何环境都会覆盖默认提供的 {@link StandardServletEnvironment}。
	 * @throws IllegalArgumentException 如果 environment 不可赋值给
	 * {@code ConfigurableEnvironment}
	 */
	@Override
	public void setEnvironment(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * 返回与此 Servlet 关联的 {@link Environment}。
	 * <p>如果未指定，则会通过 {@link #createEnvironment()} 初始化默认环境。
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回一个新的 {@link StandardServletEnvironment}。
	 * <p>子类可以重写此方法以配置环境，或者特化所返回的环境类型。
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 将配置参数映射到此 Servlet 的 bean 属性上，并调用子类初始化。
	 * @throws ServletException 如果 Bean 属性无效（或缺少必需属性），
	 * 或子类初始化失败。
	 */
	@Override
	public final void init() throws ServletException {

		// Set bean properties from init parameters.
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
				initBeanWrapper(bw);
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
				}
				throw ex;
			}
		}

		// Let subclasses do whatever initialization they like.
		initServletBean();
	}

	/**
	 * 为此 HttpServletBean 初始化 BeanWrapper，可能使用自定义编辑器。
	 * <p>此默认实现为空。
	 * @param bw 要初始化的 BeanWrapper
	 * @throws BeansException 如果由 BeanWrapper 方法抛出
	 * @see org.springframework.beans.BeanWrapper#registerCustomEditor
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}

	/**
	 * 子类可以重写此方法以执行自定义初始化。
	 * 在调用此方法之前，此 Servlet 的所有 bean 属性都已设置完毕。
	 * <p>此默认实现为空。
	 * @throws ServletException 如果子类初始化失败
	 */
	protected void initServletBean() throws ServletException {
	}

	/**
	 * 重写的方法，在尚未设置 ServletConfig 时直接返回 {@code null}。
	 * @see #getServletConfig()
	 */
	@Override
	@Nullable
	public String getServletName() {
		return (getServletConfig() != null ? getServletConfig().getServletName() : null);
	}


	/**
	 * PropertyValues 实现，由 ServletConfig 的初始化参数创建。
	 */
	private static class ServletConfigPropertyValues extends MutablePropertyValues {

		/**
		 * 创建一个新的 ServletConfigPropertyValues。
		 * @param config 用于获取 PropertyValues 的 ServletConfig
		 * @param requiredProperties 所需的属性名称集合，
		 * 这些属性不能接受默认值
		 * @throws ServletException 如果缺少任何必需属性
		 */
		public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
				throws ServletException {

			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<>(requiredProperties) : null);

			Enumeration<String> paramNames = config.getInitParameterNames();
			while (paramNames.hasMoreElements()) {
				String property = paramNames.nextElement();
				Object value = config.getInitParameter(property);
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					missingProps.remove(property);
				}
			}

			// Fail if we are still missing properties.
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from ServletConfig for servlet '" + config.getServletName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
