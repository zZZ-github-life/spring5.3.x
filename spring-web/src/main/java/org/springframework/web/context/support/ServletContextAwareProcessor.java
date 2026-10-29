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

package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * 实现，将 ServletContext 传递给实现了
 * {@link ServletContextAware} 接口的 bean。
 *
 * <p>Web 应用上下文会自动将其注册到底层的 bean 工厂。应用程序不会直接使用它。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 12.03.2004
 * @see org.springframework.web.context.ServletContextAware
 * @see org.springframework.web.context.support.XmlWebApplicationContext#postProcessBeanFactory
 */
public class ServletContextAwareProcessor implements BeanPostProcessor {

	@Nullable
	private ServletContext servletContext;

	@Nullable
	private ServletConfig servletConfig;


	/**
	 * 创建一个不带初始上下文或配置的 ServletContextAwareProcessor。使用此构造函数时，应覆盖
	 * {@link #getServletContext()} 和/或 {@link #getServletConfig()} 方法。
	 */
	protected ServletContextAwareProcessor() {
	}

	/**
	 * 为给定的上下文创建一个新的 ServletContextAwareProcessor。
	 */
	public ServletContextAwareProcessor(ServletContext servletContext) {
		this(servletContext, null);
	}

	/**
	 * 为给定的配置创建一个新的 ServletContextAwareProcessor。
	 */
	public ServletContextAwareProcessor(ServletConfig servletConfig) {
		this(null, servletConfig);
	}

	/**
	 * 为给定的上下文和配置创建一个新的 ServletContextAwareProcessor。
	 */
	public ServletContextAwareProcessor(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {
		this.servletContext = servletContext;
		this.servletConfig = servletConfig;
	}


	/**
	 * 返回要注入的 {@link ServletContext} 或 {@code null}。当上下文是在后处理器注册之后才获得时，子
	 * 类可以覆盖此方法。
	 */
	@Nullable
	protected ServletContext getServletContext() {
		if (this.servletContext == null && getServletConfig() != null) {
			return getServletConfig().getServletContext();
		}
		return this.servletContext;
	}

	/**
	 * 返回要注入的 {@link ServletConfig} 或 {@code null}。当上下文是在后处理器注册之后才获得时，子类
	 * 可以覆盖此方法。
	 */
	@Nullable
	protected ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (getServletContext() != null && bean instanceof ServletContextAware) {
			((ServletContextAware) bean).setServletContext(getServletContext());
		}
		if (getServletConfig() != null && bean instanceof ServletConfigAware) {
			((ServletConfigAware) bean).setServletConfig(getServletConfig());
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

}
