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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * 静态的 {@link org.springframework.web.context.WebApplicationContext}
 * 实现，用于测试。不适用于生产应用。
 *
 * <p>实现了 {@link org.springframework.web.context.ConfigurableWebApplicationContext}
 * 接口，以便可以直接替换 {@link XmlWebApplicationContext}，
 * 尽管它实际上并不支持外部配置文件。
 *
 * <p>将资源路径解释为 Servlet 上下文资源，即 Web 应用根路径下的路径。绝对路径
 * （例如 Web 应用根路径之外的文件）可以通过 "file:" URL 访问，
 * 具体由 {@link org.springframework.core.io.DefaultResourceLoader} 实现。
 *
 * <p>除了
 * {@link org.springframework.context.support.AbstractApplicationContext} 检测的特殊 bean 之外，
 * 此类还会在上下文中以特殊 bean 名称 "themeSource" 检测
 * 类型为 {@link org.springframework.ui.context.ThemeSource} 的 bean。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.ui.context.ThemeSource
 */
public class StaticWebApplicationContext extends StaticApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	@Nullable
	private ServletContext servletContext;

	@Nullable
	private ServletConfig servletConfig;

	@Nullable
	private String namespace;

	@Nullable
	private ThemeSource themeSource;


	public StaticWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}


	/**
	 * 设置此 WebApplicationContext 运行所在的 ServletContext。
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
	public void setServletConfig(@Nullable ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
		if (servletConfig != null && this.servletContext == null) {
			this.servletContext = servletConfig.getServletContext();
		}
	}

	@Override
	@Nullable
	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public void setNamespace(@Nullable String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
		}
	}

	@Override
	@Nullable
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * {@link StaticWebApplicationContext} 类不支持此方法。
	 * @throws UnsupportedOperationException <b>always</b>
	 */
	@Override
	public void setConfigLocation(String configLocation) {
		throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
	}

	/**
	 * {@link StaticWebApplicationContext} 类不支持此方法。
	 * @throws UnsupportedOperationException <b>always</b>
	 */
	@Override
	public void setConfigLocations(String... configLocations) {
		throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
	}

	@Override
	public String[] getConfigLocations() {
		return null;
	}


	/**
	 * 注册请求/会话作用域、{@link ServletContextAwareProcessor} 等。
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
	}

	/**
	 * 此实现支持 ServletContext 根路径下的文件路径。
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
	 * 创建并返回一个新的 {@link StandardServletEnvironment}。
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 初始化主题（theme）能力。
	 */
	@Override
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	@Override
	protected void initPropertySources() {
		WebApplicationContextUtils.initServletPropertySources(getEnvironment().getPropertySources(),
				this.servletContext, this.servletConfig);
	}

	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		Assert.state(this.themeSource != null, "No ThemeSource available");
		return this.themeSource.getTheme(themeName);
	}

}
