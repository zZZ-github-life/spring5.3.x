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

package org.springframework.web.servlet.view.script;

import java.util.Locale;
import java.util.function.Function;

import org.springframework.context.ApplicationContext;

/**
 * 传递给 {@link ScriptTemplateView} 渲染函数的上下文，
 * 以便在脚本侧访问应用上下文、locale、模板加载器和 url。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class RenderingContext {

	private final ApplicationContext applicationContext;

	private final Locale locale;

	private final Function<String, String> templateLoader;

	private final String url;


	/**
	 * 创建一个新的 {@code RenderingContext}。
	 *
	 * @param applicationContext 应用上下文
	 * @param locale 所渲染模板的 Locale
	 * @param templateLoader 一个函数，接收模板路径作为输入
	 * 并返回 String 形式的模板内容
	 * @param url 所渲染模板的 URL
	 */
	public RenderingContext(ApplicationContext applicationContext, Locale locale,
			Function<String, String> templateLoader, String url) {

		this.applicationContext = applicationContext;
		this.locale = locale;
		this.templateLoader = templateLoader;
		this.url = url;
	}


	/**
	 * 返回应用上下文。
	 */
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 返回所渲染模板的 locale。
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * 返回一个函数，该函数以模板路径为输入，返回模板内容字符串。
	 */
	public Function<String, String> getTemplateLoader() {
		return this.templateLoader;
	}

	/**
	 * 返回所渲染模板的 URL。
	 */
	public String getUrl() {
		return this.url;
	}

}
