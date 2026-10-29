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

package org.springframework.web.servlet.handler;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}，
 * 对实现 {@link javax.servlet.Servlet} 接口的 bean 应用
 * 初始化和销毁回调。
 *
 * <p>在 bean 实例初始化之后，将使用一个 ServletConfig 调用 Servlet 的
 * {@code init} 方法，该 ServletConfig 包含 Servlet 的 bean 名称
 * 及其运行所在的 ServletContext。
 *
 * <p>在 bean 实例销毁之前，将调用 Servlet 的 {@code destroy}
 * 方法。
 *
 * <p><b>请注意，此后置处理器不支持 Servlet 初始化
 * 参数。</b>实现 Servlet 接口的 bean 实例
 * 应像任何其他 Spring bean 一样进行配置，即通过
 * 构造函数参数或 bean 属性。
 *
 * <p>如果要在普通 Servlet 容器中复用 Servlet 实现，
 * 同时在 Spring 上下文中作为 bean 使用，可以考虑派生自 Spring 的
 * {@link org.springframework.web.servlet.HttpServletBean} 基类，该类
 * 将 Servlet 初始化参数作为 bean 属性应用，同时支持
 * 标准 Servlet 和 Spring bean 两种初始化风格。
 *
 * <p><b>或者，可以考虑用 Spring 的
 * {@link org.springframework.web.servlet.mvc.ServletWrappingController} 包装 Servlet。</b>
 * 这对于现有的 Servlet 类尤其合适，
 * 允许指定 Servlet 初始化参数等。
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
 * @see javax.servlet.Servlet#destroy()
 * @see SimpleServletHandlerAdapter
 */
public class SimpleServletPostProcessor implements
		DestructionAwareBeanPostProcessor, ServletContextAware, ServletConfigAware {

	private boolean useSharedServletConfig = true;

	@Nullable
	private ServletContext servletContext;

	@Nullable
	private ServletConfig servletConfig;


	/**
	 * 设置是否使用通过 {@code setServletConfig} 传入的
	 * 共享 ServletConfig 对象（如果可用）。
	 * <p>默认值为 "true"。将此设置改为 "false" 可以传入
	 * 一个模拟的 ServletConfig 对象，以 bean 名称作为 servlet 名称，
	 * 并持有当前的 ServletContext。
	 * @see #setServletConfig
	 */
	public void setUseSharedServletConfig(boolean useSharedServletConfig) {
		this.useSharedServletConfig = useSharedServletConfig;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Servlet) {
			ServletConfig config = this.servletConfig;
			if (config == null || !this.useSharedServletConfig) {
				config = new DelegatingServletConfig(beanName, this.servletContext);
			}
			try {
				((Servlet) bean).init(config);
			}
			catch (ServletException ex) {
				throw new BeanInitializationException("Servlet.init threw exception", ex);
			}
		}
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Servlet) {
			((Servlet) bean).destroy();
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return (bean instanceof Servlet);
	}


	/**
	 * {@link ServletConfig} 接口的内部实现，
	 * 用于传递给被包装的 servlet。
	 */
	private static class DelegatingServletConfig implements ServletConfig {

		private final String servletName;

		@Nullable
		private final ServletContext servletContext;

		public DelegatingServletConfig(String servletName, @Nullable ServletContext servletContext) {
			this.servletName = servletName;
			this.servletContext = servletContext;
		}

		@Override
		public String getServletName() {
			return this.servletName;
		}

		@Override
		@Nullable
		public ServletContext getServletContext() {
			return this.servletContext;
		}

		@Override
		@Nullable
		public String getInitParameter(String paramName) {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(Collections.emptySet());
		}
	}

}
