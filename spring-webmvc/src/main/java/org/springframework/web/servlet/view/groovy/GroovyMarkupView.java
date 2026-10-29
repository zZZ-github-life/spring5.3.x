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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.springframework.web.util.NestedServletException;

/**
 * 基于 Groovy XML/XHTML markup 模板的 {@link AbstractTemplateView} 子类。
 *
 * <p>Spring 的 Groovy Markup 模板支持要求 Groovy 2.3.1 或更高版本。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see GroovyMarkupViewResolver
 * @see GroovyMarkupConfigurer
 * @see <a href="https://groovy-lang.org/templating.html#_the_markuptemplateengine">
 * Groovy Markup Template engine documentation</a>
 */
public class GroovyMarkupView extends AbstractTemplateView {

	@Nullable
	private MarkupTemplateEngine engine;


	/**
	 * 设置此视图要使用的 MarkupTemplateEngine。
	 * <p>如果未设置，则会通过查找 Web 应用上下文中单个
	 * {@link GroovyMarkupConfig} bean 并从中获取已配置的
	 * {@code MarkupTemplateEngine} 实例来自动检测引擎。
	 * @see GroovyMarkupConfig
	 */
	public void setTemplateEngine(MarkupTemplateEngine engine) {
		this.engine = engine;
	}

	/**
	 * 在启动时调用。
	 * 如果未手动设置 {@link #setTemplateEngine(MarkupTemplateEngine) templateEngine}，
	 * 此方法会按类型查找 {@link GroovyMarkupConfig} bean，
	 * 并使用它获取 Groovy Markup 模板引擎。
	 * @see GroovyMarkupConfig
	 * @see #setTemplateEngine(groovy.text.markup.MarkupTemplateEngine)
	 */
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext();
		if (this.engine == null) {
			setTemplateEngine(autodetectMarkupTemplateEngine());
		}
	}

	/**
	 * 通过 ApplicationContext 自动检测 MarkupTemplateEngine。
	 * 在未手动配置 MarkupTemplateEngine 时调用。
	 */
	protected MarkupTemplateEngine autodetectMarkupTemplateEngine() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(obtainApplicationContext(),
					GroovyMarkupConfig.class, true, false).getTemplateEngine();
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException("Expected a single GroovyMarkupConfig bean in the current " +
					"Servlet web application context or the parent root context: GroovyMarkupConfigurer is " +
					"the usual implementation. This bean may have any name.", ex);
		}
	}


	@Override
	public boolean checkResource(Locale locale) throws Exception {
		Assert.state(this.engine != null, "No MarkupTemplateEngine set");
		try {
			this.engine.resolveTemplate(getUrl());
		}
		catch (IOException ex) {
			return false;
		}
		return true;
	}

	@Override
	protected void renderMergedTemplateModel(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		Template template = getTemplate(url);
		template.make(model).writeTo(new BufferedWriter(response.getWriter()));
	}

	/**
	 * 返回由已配置的 Groovy Markup 模板引擎针对给定视图 URL 编译的模板。
	 */
	protected Template getTemplate(String viewUrl) throws Exception {
		Assert.state(this.engine != null, "No MarkupTemplateEngine set");
		try {
			return this.engine.createTemplateByPath(viewUrl);
		}
		catch (ClassNotFoundException ex) {
			Throwable cause = (ex.getCause() != null ? ex.getCause() : ex);
			throw new NestedServletException(
					"Could not find class while rendering Groovy Markup view with name '" +
					getUrl() + "': " + ex.getMessage() + "'", cause);
		}
	}

}
