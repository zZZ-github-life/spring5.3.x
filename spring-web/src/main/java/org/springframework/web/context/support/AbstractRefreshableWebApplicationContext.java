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

package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.context.support.AbstractRefreshableApplicationContext}
 * 的子类，为 Web 环境实现了
 * {@link org.springframework.web.context.ConfigurableWebApplicationContext}
 * 接口。提供 "configLocations" 属性，
 * 可在 Web 应用启动时通过 ConfigurableWebApplicationContext 接口进行填充。
 *
 * <p>此类与 AbstractRefreshableApplicationContext 一样易于子类化：只需实现
 * {@link #loadBeanDefinitions} 方法即可；详细信息请参见父类 javadoc。注意，实现应当从
 * {@link #getConfigLocations} 方法返回的位置所指定的文件中加载 bean 定义。
 *
 * <p>将资源路径解释为 Servlet 上下文资源，即 Web 应用根路径下的路径。
 * 绝对路径（例如 Web 应用根路径之外的文件）可以通过 "file:" URL 访问，
 * 具体由 {@link org.springframework.core.io.DefaultResourceLoader} 实现。
 *
 * <p>除了
 * {@link org.springframework.context.support.AbstractApplicationContext} 检测的特殊 bean 之外，
 * 此类还会在上下文中以特殊 bean 名称 "themeSource" 检测
 * 类型为 {@link org.springframework.ui.context.ThemeSource} 的 bean。
 *
 * <p><b>这是为不同的 bean 定义格式而应子类化的 Web 上下文。</b>
 * 这种上下文实现可以指定为
 * {@link org.springframework.web.context.ContextLoader} 的 "contextClass" context-param，
 * 或 {@link org.springframework.web.servlet.FrameworkServlet} 的 "contextClass"
 * init-param，取代默认的 {@link XmlWebApplicationContext}。之后它会自动
 * 接收相应的 "contextConfigLocation" context-param 或 init-param。
 *
 * <p>注意，WebApplicationContext 实现通常应根据通过 {@link ConfigurableWebApplicationContext} 接
 * 口收到的配置进行自我配置。相比之下，独立的应用程序上下文可能允许在自定义启动代码中进行配置（例
 * 如 {@link org.springframework.context.support.GenericApplicationContext}）。
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see #loadBeanDefinitions
 * @see org.springframework.web.context.ConfigurableWebApplicationContext#setConfigLocations
 * @see org.springframework.ui.context.ThemeSource
 * @see XmlWebApplicationContext
 */
public abstract class AbstractRefreshableWebApplicationContext extends AbstractRefreshableConfigApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	/** 此上下文运行所在的 Servlet 上下文。 */
	@Nullable
	private ServletContext servletContext;

	/** 此上下文运行所在的 Servlet 配置（如果有）。 */
	@Nullable
	private ServletConfig servletConfig;

	/** 此上下文的命名空间，如果是根上下文则为 {@code null}。 */
	@Nullable
	private String namespace;

	/** 此 ApplicationContext 的 ThemeSource。 */
	@Nullable
	private ThemeSource themeSource;


	public AbstractRefreshableWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}


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
			setServletContext(servletConfig.getServletContext());
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

	@Override
	public String[] getConfigLocations() {
		return super.getConfigLocations();
	}

	@Override
	public String getApplicationName() {
		return (this.servletContext != null ? this.servletContext.getContextPath() : "");
	}

	/**
	 * 创建并返回一个新的 {@link StandardServletEnvironment}。子类可以覆盖此方法以配置环境或特化所返
	 * 回的环境类型。
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
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
			((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, this.servletConfig);
		}
	}

	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		Assert.state(this.themeSource != null, "No ThemeSource available");
		return this.themeSource.getTheme(themeName);
	}

}
