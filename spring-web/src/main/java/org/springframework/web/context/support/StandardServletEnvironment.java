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

package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiPropertySource;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebEnvironment;

/**
 * 供基于 {@code Servlet} 的 Web 应用使用的 {@link Environment} 实现。所有与 Web 相关的（基于
 * Servlet 的）{@code ApplicationContext} 类默认都会初始化一个实例。
 *
 * <p>提供基于 {@code ServletConfig}、{@code ServletContext} 以及 JNDI 的
 * {@link PropertySource} 实例。详情参见 {@link #customizePropertySources} 方法的文档。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see StandardEnvironment
 */
public class StandardServletEnvironment extends StandardEnvironment implements ConfigurableWebEnvironment {

	/** Servlet 上下文初始化参数的属性源名称：{@value}。 */
	public static final String SERVLET_CONTEXT_PROPERTY_SOURCE_NAME = "servletContextInitParams";

	/** Servlet 配置初始化参数的属性源名称：{@value}。 */
	public static final String SERVLET_CONFIG_PROPERTY_SOURCE_NAME = "servletConfigInitParams";

	/** JNDI 属性源名称：{@value}。 */
	public static final String JNDI_PROPERTY_SOURCE_NAME = "jndiProperties";


	// Defensive reference to JNDI API for JDK 9+ (optional java.naming module)
	private static final boolean jndiPresent = ClassUtils.isPresent(
			"javax.naming.InitialContext", StandardServletEnvironment.class.getClassLoader());


	/**
	 * 创建一个新的 {@code StandardServletEnvironment} 实例。
	 */
	public StandardServletEnvironment() {
	}

	/**
	 * 使用特定的 {@link MutablePropertySources} 实例创建一个新的 {@code StandardServletEnvironment}
	 * 实例。
	 * @param propertySources 要使用的属性源
	 * @since 5.3.4
	 */
	protected StandardServletEnvironment(MutablePropertySources propertySources) {
		super(propertySources);
	}


	/**
	 * 定制属性源集合，包括由父类提供的属性源以及适用于标准基于 Servlet 环境的属性源：
	 * <ul>
	 * <li>{@value #SERVLET_CONFIG_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SERVLET_CONTEXT_PROPERTY_SOURCE_NAME}
	 * <li>{@value #JNDI_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>在 {@value #SERVLET_CONFIG_PROPERTY_SOURCE_NAME} 中存在的属性
	 * 优先于 {@value #SERVLET_CONTEXT_PROPERTY_SOURCE_NAME} 中的属性，而上述两者中的属性
	 * 优先于 {@value #JNDI_PROPERTY_SOURCE_NAME} 中的属性。
	 * <p>上述任何属性源中的属性都优先于 {@link StandardEnvironment} 父类提供的
	 * 系统属性和环境变量。
	 * <p>在此阶段，与 {@code Servlet} 相关的属性源会以
	 * {@link StubPropertySource 存根} 的形式添加，待实际的 {@link ServletContext}
	 * 对象可用后再{@linkplain #initPropertySources(ServletContext, ServletConfig) 完全初始化}。
	 * <p>可以通过 {@link JndiLocatorDelegate#IGNORE_JNDI_PROPERTY_NAME}
	 * 禁用 {@value #JNDI_PROPERTY_SOURCE_NAME} 的添加。
	 * @see StandardEnvironment#customizePropertySources
	 * @see org.springframework.core.env.AbstractEnvironment#customizePropertySources
	 * @see ServletConfigPropertySource
	 * @see ServletContextPropertySource
	 * @see org.springframework.jndi.JndiPropertySource
	 * @see org.springframework.context.support.AbstractApplicationContext#initPropertySources
	 * @see #initPropertySources(ServletContext, ServletConfig)
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(new StubPropertySource(SERVLET_CONFIG_PROPERTY_SOURCE_NAME));
		propertySources.addLast(new StubPropertySource(SERVLET_CONTEXT_PROPERTY_SOURCE_NAME));
		if (jndiPresent && JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()) {
			propertySources.addLast(new JndiPropertySource(JNDI_PROPERTY_SOURCE_NAME));
		}
		super.customizePropertySources(propertySources);
	}

	@Override
	public void initPropertySources(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {
		WebApplicationContextUtils.initServletPropertySources(getPropertySources(), servletContext, servletConfig);
	}

}
