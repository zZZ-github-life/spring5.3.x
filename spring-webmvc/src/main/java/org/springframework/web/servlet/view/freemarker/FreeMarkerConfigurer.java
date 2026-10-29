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

package org.springframework.web.servlet.view.freemarker;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

/**
 * 用于为 Web 使用配置 FreeMarker 的 JavaBean，可通过 "configLocation"
 * 和/或 "freemarkerSettings" 和/或 "templateLoaderPath" 属性进行配置。
 * 使用此类最简单的方式是指定一个 "templateLoaderPath"；
 * 这样就不需要任何进一步配置了。
 *
 * <pre class="code">
 * &lt;bean id="freemarkerConfig" class="org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer"&gt;
 *   &lt;property name="templateLoaderPath"&gt;&lt;value&gt;/WEB-INF/freemarker/&lt;/value&gt;&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 任何使用 Spring 的 FreeMarkerView 提供 Web MVC 的应用，都必须在其应用上下文中
 * 包含此 bean。它存在的唯一目的就是配置 FreeMarker。
 * 它并不是供应用组件引用的，而只是供 FreeMarkerView 在内部使用。
 * 它实现了 FreeMarkerConfig，以便 FreeMarkerView 无需依赖配置器的 bean 名称
 * 即可找到它。每个 DispatcherServlet 都可以根据需要定义自己的 FreeMarkerConfigurer。
 *
 * <p>注意，你也可以通过 "configuration" 属性引用预先配置好的 FreeMarker Configuration
 * 实例，例如由 FreeMarkerConfigurationFactoryBean 设置的实例。
 * 这样便可以在 Web 和电子邮件等场景之间共享同一个 FreeMarker Configuration。
 *
 * <p>此配置器为此包注册了一个模板加载器，
 * 从而可以引用此包中包含的 "spring.ftl" 宏库：
 *
 * <pre class="code">
 * &lt;#import "/spring.ftl" as spring/&gt;
 * &lt;@spring.bind "person.age"/&gt;
 * age is ${spring.status.value}</pre>
 *
 * 注意：Spring 的 FreeMarker 支持要求 FreeMarker 2.3 或更高版本。
 *
 * @author Darren Davison
 * @author Rob Harrop
 * @since 03.03.2004
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setTemplateLoaderPath
 * @see #setConfiguration
 * @see org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean
 * @see FreeMarkerView
 */
public class FreeMarkerConfigurer extends FreeMarkerConfigurationFactory
		implements FreeMarkerConfig, InitializingBean, ResourceLoaderAware, ServletContextAware {

	@Nullable
	private Configuration configuration;

	@Nullable
	private TaglibFactory taglibFactory;


	/**
	 * 设置用于 FreeMarker Web 配置的预先配置好的 Configuration，
	 * 例如通过 FreeMarkerConfigurationFactoryBean 设置的、供 Web 和电子邮件共用的 Configuration。
	 * 如果未设置此项，则必须指定 FreeMarkerConfigurationFactory 的属性
	 * （此类继承了这些属性）。
	 * @see org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 为给定的 ServletContext 初始化 {@link TaglibFactory}。
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.taglibFactory = new TaglibFactory(servletContext);
	}


	/**
	 * 如果未通过预先配置的 FreeMarker Configuration 覆盖，
	 * 则初始化 FreeMarkerConfigurationFactory 的 Configuration。
	 * <p>设置一个 ClassTemplateLoader，用于加载 Spring 宏。
	 * @see #createConfiguration
	 * @see #setConfiguration
	 */
	@Override
	public void afterPropertiesSet() throws IOException, TemplateException {
		if (this.configuration == null) {
			this.configuration = createConfiguration();
		}
	}

	/**
	 * 此实现额外注册了一个用于 Spring 提供的宏的 ClassTemplateLoader，
	 * 并将其添加到列表末尾。
	 */
	@Override
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
		templateLoaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class, ""));
	}


	/**
	 * 返回此 bean 包装的 Configuration 对象。
	 */
	@Override
	public Configuration getConfiguration() {
		Assert.state(this.configuration != null, "No Configuration available");
		return this.configuration;
	}

	/**
	 * 返回此 bean 包装的 TaglibFactory 对象。
	 */
	@Override
	public TaglibFactory getTaglibFactory() {
		Assert.state(this.taglibFactory != null, "No TaglibFactory available");
		return this.taglibFactory;
	}

}
