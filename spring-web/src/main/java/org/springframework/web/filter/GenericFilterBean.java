/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.filter;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.util.NestedServletException;

/**
 * {@link javax.servlet.Filter} 的简单基础实现，将其配置参数
 * （{@code web.xml} 中 {@code filter} 标签内的 {@code init-param} 条目）
 * 视为 Bean 属性。
 *
 * <p>它是各类过滤器的便捷超类。配置参数的类型转换是自动的，
 * 相应的 setter 方法会以转换后的值被调用。子类还可以指定必需属性。
 * 没有匹配 Bean 属性 setter 的参数将被直接忽略。
 *
 * <p>此过滤器将实际过滤工作留给子类，子类必须实现
 * {@link javax.servlet.Filter#doFilter} 方法。
 *
 * <p>此通用过滤器基类不依赖于 Spring 的
 * {@link org.springframework.context.ApplicationContext} 概念。
 * 过滤器通常不加载自己的上下文，而是从 Spring 根应用上下文中访问服务 Bean，
 * 可通过过滤器的 {@link #getServletContext() ServletContext} 访问
 * （参见
 * {@link org.springframework.web.context.support.WebApplicationContextUtils}）。
 *
 * @author Juergen Hoeller
 * @since 06.12.2003
 * @see #addRequiredProperty
 * @see #initFilterBean
 * @see #doFilter
 */
public abstract class GenericFilterBean implements Filter, BeanNameAware, EnvironmentAware,
		EnvironmentCapable, ServletContextAware, InitializingBean, DisposableBean {

	/** 可供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private String beanName;

	@Nullable
	private Environment environment;

	@Nullable
	private ServletContext servletContext;

	@Nullable
	private FilterConfig filterConfig;

	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * 保存 Spring Bean 工厂中定义的 Bean 名称。
	 * <p>仅在作为 Bean 初始化时才有意义，以便在通常由 FilterConfig 实例提供的
	 * 过滤器名称之外提供备用名称。
	 * @see org.springframework.beans.factory.BeanNameAware
	 * @see #getFilterName()
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 设置此过滤器运行所在的 {@code Environment}。
	 * <p>此处设置的任何环境都会覆盖默认提供的
	 * {@link StandardServletEnvironment}。
	 * <p>此 {@code Environment} 对象仅用于解析传入此过滤器初始化参数的
	 * 资源路径中的占位符。如果未使用任何初始化参数，则基本上可以忽略此
	 * {@code Environment}。
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * 返回与此过滤器关联的 {@link Environment}。
	 * <p>如果未指定，将通过 {@link #createEnvironment()} 初始化一个默认环境。
	 * @since 4.3.9
	 */
	@Override
	public Environment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回一个新的 {@link StandardServletEnvironment}。
	 * <p>子类可以重写此方法以配置环境或定制返回的环境类型。
	 * @since 4.3.9
	 */
	protected Environment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 保存 Bean 工厂运行所在的 ServletContext。
	 * <p>仅在作为 Bean 初始化时才有意义，以便在通常由 FilterConfig 实例提供的
	 * 上下文之外提供备用 ServletContext。
	 * @see org.springframework.web.context.ServletContextAware
	 * @see #getServletContext()
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 调用 {@code initFilterBean()} 方法，该方法可能包含子类的自定义初始化逻辑。
	 * <p>仅在作为 Bean 初始化时才有意义，此时不会调用标准的
	 * {@code init(FilterConfig)} 方法。
	 * @see #initFilterBean()
	 * @see #init(javax.servlet.FilterConfig)
	 */
	@Override
	public void afterPropertiesSet() throws ServletException {
		initFilterBean();
	}

	/**
	 * 子类可以重写此方法以执行自定义的过滤器关闭操作。
	 * <p>注意：此方法会在标准过滤器销毁以及 Spring 应用上下文中的过滤器 Bean
	 * 销毁时被调用。
	 * <p>此默认实现为空。
	 */
	@Override
	public void destroy() {
	}


	/**
	 * 子类可以调用此方法来指定某个属性（必须与其公开的 JavaBean 属性匹配）为必需属性，
	 * 且必须作为配置参数提供。应在子类的构造函数中调用此方法。
	 * <p>此方法仅在由 FilterConfig 实例驱动的传统初始化场景中才有意义。
	 * @param property name of the required property
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * 初始化此过滤器的标准方式。
	 * 将配置参数映射到此过滤器的 Bean 属性，并调用子类初始化逻辑。
	 * @param filterConfig the configuration for this filter
	 * @throws ServletException if bean properties are invalid (or required
	 * properties are missing), or if subclass initialization fails.
	 * @see #initFilterBean
	 */
	@Override
	public final void init(FilterConfig filterConfig) throws ServletException {
		Assert.notNull(filterConfig, "FilterConfig must not be null");

		this.filterConfig = filterConfig;

		// Set bean properties from init parameters.
		PropertyValues pvs = new FilterConfigPropertyValues(filterConfig, this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(filterConfig.getServletContext());
				Environment env = this.environment;
				if (env == null) {
					env = new StandardServletEnvironment();
				}
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, env));
				initBeanWrapper(bw);
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				String msg = "Failed to set bean properties on filter '" +
						filterConfig.getFilterName() + "': " + ex.getMessage();
				logger.error(msg, ex);
				throw new NestedServletException(msg, ex);
			}
		}

		// Let subclasses do whatever initialization they like.
		initFilterBean();

		if (logger.isDebugEnabled()) {
			logger.debug("Filter '" + filterConfig.getFilterName() + "' configured for use");
		}
	}

	/**
	 * 为此 GenericFilterBean 初始化 BeanWrapper，可能还会使用自定义编辑器。
	 * <p>此默认实现为空。
	 * @param bw the BeanWrapper to initialize
	 * @throws BeansException if thrown by BeanWrapper methods
	 * @see org.springframework.beans.BeanWrapper#registerCustomEditor
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}

	/**
	 * 子类可以重写此方法以执行自定义初始化。
	 * 在调用此方法之前，此过滤器的所有 Bean 属性都将已被设置。
	 * <p>注意：此方法会在标准过滤器初始化以及 Spring 应用上下文中的过滤器 Bean
	 * 初始化时被调用。在这两种情况下都可以获取过滤器名称和 ServletContext。
	 * <p>此默认实现为空。
	 * @throws ServletException if subclass initialization fails
	 * @see #getFilterName()
	 * @see #getServletContext()
	 */
	protected void initFilterBean() throws ServletException {
	}

	/**
	 * 使此过滤器的 FilterConfig（如果有）可用。
	 * 类似于 GenericServlet 的 {@code getServletConfig()}。
	 * <p>将其声明为 public 是为了与 WebLogic 6.1 附带的 Servlet Filter 版本中的
	 * {@code getFilterConfig()} 方法保持一致。
	 * @return the FilterConfig instance, or {@code null} if none available
	 * @see javax.servlet.GenericServlet#getServletConfig()
	 */
	@Nullable
	public FilterConfig getFilterConfig() {
		return this.filterConfig;
	}

	/**
	 * 使子类可以获取此过滤器的名称。
	 * 类似于 GenericServlet 的 {@code getServletName()}。
	 * <p>默认采用 FilterConfig 的过滤器名称。
	 * 如果在 Spring 应用上下文中作为 Bean 初始化，则回退到 Bean 工厂中定义的 Bean 名称。
	 * @return the filter name, or {@code null} if none available
	 * @see javax.servlet.GenericServlet#getServletName()
	 * @see javax.servlet.FilterConfig#getFilterName()
	 * @see #setBeanName
	 */
	@Nullable
	protected String getFilterName() {
		return (this.filterConfig != null ? this.filterConfig.getFilterName() : this.beanName);
	}

	/**
	 * 使子类可以获取此过滤器的 ServletContext。
	 * 类似于 GenericServlet 的 {@code getServletContext()}。
	 * <p>默认采用 FilterConfig 的 ServletContext。
	 * 如果在 Spring 应用上下文中作为 Bean 初始化，则回退到 Bean 工厂运行所在的
	 * ServletContext。
	 * @return the ServletContext instance
	 * @throws IllegalStateException if no ServletContext is available
	 * @see javax.servlet.GenericServlet#getServletContext()
	 * @see javax.servlet.FilterConfig#getServletContext()
	 * @see #setServletContext
	 */
	protected ServletContext getServletContext() {
		if (this.filterConfig != null) {
			return this.filterConfig.getServletContext();
		}
		else if (this.servletContext != null) {
			return this.servletContext;
		}
		else {
			throw new IllegalStateException("No ServletContext");
		}
	}


	/**
	 * 由 FilterConfig 初始化参数创建的 PropertyValues 实现。
	 */
	@SuppressWarnings("serial")
	private static class FilterConfigPropertyValues extends MutablePropertyValues {

		/**
		 * 创建新的 FilterConfigPropertyValues。
		 * @param config the FilterConfig we'll use to take PropertyValues from
		 * @param requiredProperties set of property names we need, where
		 * we can't accept default values
		 * @throws ServletException if any required properties are missing
		 */
		public FilterConfigPropertyValues(FilterConfig config, Set<String> requiredProperties)
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
						"Initialization from FilterConfig for filter '" + config.getFilterName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
