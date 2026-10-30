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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link GenericApplicationContext} 的子类，适用于 Web 环境。
 *
 * <p>实现了 {@link ConfigurableWebApplicationContext}，但不适用于在 {@code web.xml}
 * 中进行声明式配置。相反，它面向编程式配置，例如用于构建嵌套上下文，或用于
 * {@link org.springframework.web.WebApplicationInitializer WebApplicationInitializers} 中。
 *
 * <p>将资源路径解释为 Servlet 上下文资源，即 Web 应用根路径下的路径。绝对路径
 * &mdash; 例如 Web 应用根路径之外的文件 &mdash; 可以通过 {@code file:} URL 访问，
 * 具体由 {@code AbstractApplicationContext} 实现。
 *
 * <p>除了
 * {@link org.springframework.context.support.AbstractApplicationContext AbstractApplicationContext}
 * 检测的特殊 bean 之外，此类还会检测上下文中名称为 "themeSource" 的 {@link ThemeSource} bean。
 *
 * <p>如果希望向 {@code GenericWebApplicationContext} 注册带注解的<em>组件类</em>，
 * 可以使用 {@link org.springframework.context.annotation.AnnotatedBeanDefinitionReader
 * AnnotatedBeanDefinitionReader}，如下例所示。组件类特别包括
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * 类，也包括普通的 {@link org.springframework.stereotype.Component @Component}
 * 类，以及使用 {@code javax.inject} 注解的符合 JSR-330 的类。
 *
 * <pre class="code">
 * GenericWebApplicationContext context = new GenericWebApplicationContext();
 * AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);
 * reader.register(AppConfig.class, UserController.class, UserRepository.class);</pre>
 *
 * <p>如果打算实现一个从配置文件读取 bean 定义的 {@code WebApplicationContext}，可考虑继承
 * {@link AbstractRefreshableWebApplicationContext}，并在 {@code loadBeanDefinitions} 方法的实现
 * 中读取 bean 定义。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 1.2
 */
public class GenericWebApplicationContext extends GenericApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	@Nullable
	private ServletContext servletContext;

	@Nullable
	private ThemeSource themeSource;


	/**
	 * 创建一个新的 {@code GenericWebApplicationContext}。
	 * @see #setServletContext
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext() {
		super();
	}

	/**
	 * 为给定的 {@link ServletContext} 创建一个新的 {@code GenericWebApplicationContext}。
	 * @param servletContext 要在其中运行的 {@code ServletContext}
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 使用给定的 {@link DefaultListableBeanFactory} 创建一个新的
	 * {@code GenericWebApplicationContext}。
	 * @param beanFactory 用于此上下文的 {@code DefaultListableBeanFactory} 实例
	 * @see #setServletContext
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * 使用给定的 {@link DefaultListableBeanFactory} 和 {@link ServletContext}
	 * 创建一个新的 {@code GenericWebApplicationContext}。
	 * @param beanFactory 用于此上下文的 {@code DefaultListableBeanFactory} 实例
	 * @param servletContext 要在其中运行的 {@code ServletContext}
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory, ServletContext servletContext) {
		super(beanFactory);
		this.servletContext = servletContext;
	}


	/**
	 * 设置此 {@code WebApplicationContext} 运行所在的 {@link ServletContext}。
	 */
	@Override
	public void setServletContext(@Nullable ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	@Nullable
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public String getApplicationName() {
		return (this.servletContext != null ? this.servletContext.getContextPath() : "");
	}

	/**
	 * 创建并返回一个新的 {@link StandardServletEnvironment}。
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 注册请求/会话作用域、环境 bean、{@link ServletContextAwareProcessor} 等。
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		if (this.servletContext != null) {
			beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
			beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		}
		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext);
	}

	/**
	 * 此实现支持 {@link ServletContext} 根路径下的文件路径。
	 * @see ServletContextResource
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		Assert.state(this.servletContext != null, "No ServletContext available");
		return new ServletContextResource(this.servletContext, path);
	}

	/**
	 * 此实现也支持未展开 WAR 中的模式匹配。
	 * @see ServletContextResourcePatternResolver
	 */
	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	/**
	 * 初始化主题（theme）能力。
	 */
	@Override
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>替换与 {@code Servlet} 相关的属性源。
	 */
	@Override
	protected void initPropertySources() {
		ConfigurableEnvironment env = getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, null);
		}
	}

	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		Assert.state(this.themeSource != null, "No ThemeSource available");
		return this.themeSource.getTheme(themeName);
	}


	// ---------------------------------------------------------------------
	// Pseudo-implementation of ConfigurableWebApplicationContext
	// ---------------------------------------------------------------------

	@Override
	public void setServletConfig(@Nullable ServletConfig servletConfig) {
		// no-op
	}

	@Override
	@Nullable
	public ServletConfig getServletConfig() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getServletConfig()");
	}

	@Override
	public void setNamespace(@Nullable String namespace) {
		// no-op
	}

	@Override
	@Nullable
	public String getNamespace() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getNamespace()");
	}

	@Override
	public void setConfigLocation(String configLocation) {
		if (StringUtils.hasText(configLocation)) {
			throw new UnsupportedOperationException(
					"GenericWebApplicationContext does not support setConfigLocation(). " +
					"Do you still have a 'contextConfigLocation' init-param set?");
		}
	}

	@Override
	public void setConfigLocations(String... configLocations) {
		if (!ObjectUtils.isEmpty(configLocations)) {
			throw new UnsupportedOperationException(
					"GenericWebApplicationContext does not support setConfigLocations(). " +
					"Do you still have a 'contextConfigLocations' init-param set?");
		}
	}

	@Override
	public String[] getConfigLocations() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getConfigLocations()");
	}

}
