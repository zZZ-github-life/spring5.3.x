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

package org.springframework.web.servlet.view.groovy;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import groovy.text.markup.TemplateResolver;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Groovy {@link groovy.text.markup.TemplateConfiguration} 的扩展，
 * 同时也是 Spring MVC {@link GroovyMarkupConfig} 的实现，
 * 用于创建供 Web 应用使用的 {@code MarkupTemplateEngine}。
 * 配置此类最基本的方式是设置 "resourceLoaderPath"。例如：
 *
 * <pre class="code">
 *
 * // Add the following to an &#64;Configuration class
 *
 * &#64;Bean
 * public GroovyMarkupConfig groovyMarkupConfigurer() {
 *     GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
 *     configurer.setResourceLoaderPath("classpath:/WEB-INF/groovymarkup/");
 *     return configurer;
 * }
 * </pre>
 *
 * 默认情况下，此 bean 将创建一个 {@link MarkupTemplateEngine}，其中包含：
 * <ul>
 * <li>用于加载 Groovy 模板及其引用的父 ClassLoader
 * <li>基类 {@link TemplateConfiguration} 中的默认配置
 * <li>用于解析模板文件的 {@link groovy.text.markup.TemplateResolver}
 * </ul>
 *
 * 你可以直接向此 bean 提供 {@link MarkupTemplateEngine} 实例，
 * 在这种情况下，所有其他属性都将被忽略。
 *
 * <p>使用 Spring MVC {@link GroovyMarkupView} 进行渲染的任何应用，
 * 都必须在其应用上下文中包含此 bean。它存在的唯一目的就是配置 Groovy 的 Markup 模板。
 * 它并不是供应用组件直接引用的。它实现了 GroovyMarkupConfig，
 * 以便 GroovyMarkupView 无需依赖 bean 名称即可找到它。
 * 每个 DispatcherServlet 都可以根据需要定义自己的 GroovyMarkupConfigurer。
 *
 * <p>注意，{@link MarkupTemplateEngine} 默认启用资源缓存。
 * 可按需使用 {@link #setCacheTemplates(boolean)} 进行配置。

 * <p>Spring 的 Groovy Markup 模板支持要求 Groovy 2.3.1 或更高版本。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see GroovyMarkupView
 * @see <a href="https://groovy-lang.org/templating.html#_the_markuptemplateengine">
 *     Groovy Markup Template engine documentation</a>
 */
public class GroovyMarkupConfigurer extends TemplateConfiguration
		implements GroovyMarkupConfig, ApplicationContextAware, InitializingBean {

	private String resourceLoaderPath = "classpath:";

	@Nullable
	private MarkupTemplateEngine templateEngine;

	@Nullable
	private ApplicationContext applicationContext;


	/**
	 * 通过 Spring 资源位置设置 Groovy Markup 模板的资源加载路径。
	 * 可以以逗号分隔的路径列表形式接受多个位置。
	 * 支持 Spring 的 {@link org.springframework.core.io.ResourceLoader} 所理解的
	 * 诸如 "file:" 和 "classpath:" 等标准 URL 以及伪 URL。
	 * 在 ApplicationContext 中运行时允许使用相对路径。
	 *
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

	/**
	 * 为 Groovy Markup 模板的 Web 配置设置预先配置好的 MarkupTemplateEngine。
	 * <p>注意，此引擎实例必须手动配置，
	 * 因为此配置器的所有其他 bean 属性都将被忽略。
	 */
	public void setTemplateEngine(MarkupTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	@Override
	public MarkupTemplateEngine getTemplateEngine() {
		Assert.state(this.templateEngine != null, "No MarkupTemplateEngine set");
		return this.templateEngine;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		Assert.state(this.applicationContext != null, "No ApplicationContext set");
		return this.applicationContext;
	}

	/**
	 * 不应使用此方法，因为解析模板时考虑的 Locale 是当前 HTTP 请求的 Locale。
	 */
	@Override
	public void setLocale(Locale locale) {
		super.setLocale(locale);
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.templateEngine == null) {
			this.templateEngine = createTemplateEngine();
		}
	}

	protected MarkupTemplateEngine createTemplateEngine() throws IOException {
		if (this.templateEngine == null) {
			ClassLoader templateClassLoader = createTemplateClassLoader();
			this.templateEngine = new MarkupTemplateEngine(templateClassLoader, this, new LocaleTemplateResolver());
		}
		return this.templateEngine;
	}

	/**
	 * 创建一个父 ClassLoader，供 Groovy 在加载和编译模板时用作父 ClassLoader。
	 */
	protected ClassLoader createTemplateClassLoader() throws IOException {
		String[] paths = StringUtils.commaDelimitedListToStringArray(getResourceLoaderPath());
		List<URL> urls = new ArrayList<>();
		for (String path : paths) {
			Resource[] resources = getApplicationContext().getResources(path);
			if (resources.length > 0) {
				for (Resource resource : resources) {
					if (resource.exists()) {
						urls.add(resource.getURL());
					}
				}
			}
		}
		ClassLoader classLoader = getApplicationContext().getClassLoader();
		Assert.state(classLoader != null, "No ClassLoader");
		return (!urls.isEmpty() ? new URLClassLoader(urls.toArray(new URL[0]), classLoader) : classLoader);
	}

	/**
	 * 从给定的模板路径解析模板。
	 * <p>默认实现使用当前请求关联的 Locale
	 * （通过 {@link org.springframework.context.i18n.LocaleContextHolder LocaleContextHolder} 获取）
	 * 来查找模板文件。实际上，引擎级别配置的 locale 会被忽略。
	 * @see LocaleContextHolder
	 * @see #setLocale
	 */
	protected URL resolveTemplate(ClassLoader classLoader, String templatePath) throws IOException {
		MarkupTemplateEngine.TemplateResource resource = MarkupTemplateEngine.TemplateResource.parse(templatePath);
		Locale locale = LocaleContextHolder.getLocale();
		URL url = classLoader.getResource(resource.withLocale(StringUtils.replace(locale.toString(), "-", "_")).toString());
		if (url == null) {
			url = classLoader.getResource(resource.withLocale(locale.getLanguage()).toString());
		}
		if (url == null) {
			url = classLoader.getResource(resource.withLocale(null).toString());
		}
		if (url == null) {
			throw new IOException("Unable to load template:" + templatePath);
		}
		return url;
	}


	/**
	 * 自定义的 {@link TemplateResolver 模板解析器}，仅委托给
	 * {@link #resolveTemplate(ClassLoader, String)}。
	 */
	private class LocaleTemplateResolver implements TemplateResolver {

		@Nullable
		private ClassLoader classLoader;

		@Override
		public void configure(ClassLoader templateClassLoader, TemplateConfiguration configuration) {
			this.classLoader = templateClassLoader;
		}

		@Override
		public URL resolveTemplate(String templatePath) throws IOException {
			Assert.state(this.classLoader != null, "No template ClassLoader available");
			return GroovyMarkupConfigurer.this.resolveTemplate(this.classLoader, templatePath);
		}
	}

}
