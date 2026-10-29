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

package org.springframework.test.web.servlet.setup;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * {@link AbstractMockMvcBuilder} 的具体实现，提供通过构造函数参数
 * 传入的 {@link WebApplicationContext}。
 *
 * <p>此外，如果提供的 {@code WebApplicationContext} 中的
 * {@link ServletContext} 不包含
 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
 * 键的条目，则将检测根 {@code WebApplicationContext}，并将其存储在
 * {@code ServletContext} 中的 {@code ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} 键下。
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 3.2
 */
public class DefaultMockMvcBuilder extends AbstractMockMvcBuilder<DefaultMockMvcBuilder> {

	private final WebApplicationContext webAppContext;


	/**
	 * 受保护的构造函数。不用于直接实例化。
	 * @see MockMvcBuilders#webAppContextSetup(WebApplicationContext)
	 */
	protected DefaultMockMvcBuilder(WebApplicationContext webAppContext) {
		Assert.notNull(webAppContext, "WebApplicationContext is required");
		Assert.notNull(webAppContext.getServletContext(), "WebApplicationContext must have a ServletContext");
		this.webAppContext = webAppContext;
	}


	@Override
	protected WebApplicationContext initWebAppContext() {
		ServletContext servletContext = this.webAppContext.getServletContext();
		Assert.state(servletContext != null, "No ServletContext");
		ApplicationContext rootWac = WebApplicationContextUtils.getWebApplicationContext(servletContext);

		if (rootWac == null) {
			rootWac = this.webAppContext;
			ApplicationContext parent = this.webAppContext.getParent();
			while (parent != null) {
				if (parent instanceof WebApplicationContext && !(parent.getParent() instanceof WebApplicationContext)) {
					rootWac = parent;
					break;
				}
				parent = parent.getParent();
			}
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootWac);
		}

		return this.webAppContext;
	}

}
