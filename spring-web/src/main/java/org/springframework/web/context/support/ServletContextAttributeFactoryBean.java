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

import javax.servlet.ServletContext;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link FactoryBean}，获取特定的现有 ServletContext 属性。当作为 bean 引用使用时暴露该
 * ServletContext 属性，从而使其作为命名的 Spring bean 实例可用。
 *
 * <p>旨在链接到在 Spring 应用上下文启动之前就已存在的 ServletContext 属性。通常，这些属性由第三
 * 方 Web 框架放入。在纯基于 Spring 的 Web 应用中，无需进行此类 ServletContext 属性的链接。
 *
 * <p><b>注意：</b>自 Spring 3.0 起，你也可以使用类型为 Map 的 "contextAttributes" 默认 bean，并
 * 通过 "#{contextAttributes.myKey}" 表达式引用它来按名称访问特定属性。
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see org.springframework.web.context.WebApplicationContext#CONTEXT_ATTRIBUTES_BEAN_NAME
 * @see ServletContextParameterFactoryBean
 */
public class ServletContextAttributeFactoryBean implements FactoryBean<Object>, ServletContextAware {

	@Nullable
	private String attributeName;

	@Nullable
	private Object attribute;


	/**
	 * 设置要暴露的 ServletContext 属性的名称。
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.attributeName == null) {
			throw new IllegalArgumentException("Property 'attributeName' is required");
		}
		this.attribute = servletContext.getAttribute(this.attributeName);
		if (this.attribute == null) {
			throw new IllegalStateException("No ServletContext attribute '" + this.attributeName + "' found");
		}
	}


	@Override
	@Nullable
	public Object getObject() throws Exception {
		return this.attribute;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.attribute != null ? this.attribute.getClass() : null);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
