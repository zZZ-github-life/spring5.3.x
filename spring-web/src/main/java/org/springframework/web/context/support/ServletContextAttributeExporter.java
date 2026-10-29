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

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletContextAware;

/**
 * 导出器，接收 Spring 定义的对象并将其暴露为 ServletContext 属性。通常使用 bean 引用来将 Spring
 * 定义的 bean 导出为 ServletContext 属性。
 *
 * <p>这对于让那些完全不了解 Spring、只了解 Servlet API 的代码能够使用 Spring 定义的 bean 很有用。
 * 客户端代码随后可以使用普通的 ServletContext 属性查找来访问这些对象，尽管它们是在 Spring 应用上
 * 下文中定义的。
 *
 * <p>或者，可以考虑使用 WebApplicationContextUtils 类，通过 WebApplicationContext 接口访问
 * Spring 定义的 bean。当然，这会让客户端代码依赖于 Spring API。
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see javax.servlet.ServletContext#getAttribute
 * @see WebApplicationContextUtils#getWebApplicationContext
 */
public class ServletContextAttributeExporter implements ServletContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Map<String, Object> attributes;


	/**
	 * 设置要暴露的 ServletContext 属性，以键值对形式提供。每个键将被视为 ServletContext 属性键，每个
	 * 值将用作相应的属性值。
	 * <p>通常，你会为值使用 bean 引用，以便将 Spring 定义的 bean 导出为 ServletContext 属性。当然，
	 * 也可以定义普通值来导出。
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.attributes != null) {
			for (Map.Entry<String, Object> entry : this.attributes.entrySet()) {
				String attributeName = entry.getKey();
				if (logger.isDebugEnabled()) {
					if (servletContext.getAttribute(attributeName) != null) {
						logger.debug("Replacing existing ServletContext attribute with name '" + attributeName + "'");
					}
				}
				servletContext.setAttribute(attributeName, entry.getValue());
				if (logger.isTraceEnabled()) {
					logger.trace("Exported ServletContext attribute with name '" + attributeName + "'");
				}
			}
		}
	}

}
