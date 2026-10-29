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
 * {@link FactoryBean}，获取特定的 ServletContext 初始化参数（即在 {@code web.xml} 中定义的
 * "context-param"）。当作为 bean 引用使用时暴露该 ServletContext 初始化参数，从而使其作为命名的
 * Spring bean 实例可用。
 *
 * <p><b>注意：</b>自 Spring 3.0 起，你也可以使用类型为 Map 的 "contextParameters" 默认 bean，并
 * 通过 "#{contextParameters.myKey}" 表达式引用它来按名称访问特定参数。
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see org.springframework.web.context.WebApplicationContext#CONTEXT_PARAMETERS_BEAN_NAME
 * @see ServletContextAttributeFactoryBean
 */
public class ServletContextParameterFactoryBean implements FactoryBean<String>, ServletContextAware {

	@Nullable
	private String initParamName;

	@Nullable
	private String paramValue;


	/**
	 * 设置要暴露的 ServletContext 初始化参数的名称。
	 */
	public void setInitParamName(String initParamName) {
		this.initParamName = initParamName;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.initParamName == null) {
			throw new IllegalArgumentException("initParamName is required");
		}
		this.paramValue = servletContext.getInitParameter(this.initParamName);
		if (this.paramValue == null) {
			throw new IllegalStateException("No ServletContext init parameter '" + this.initParamName + "' found");
		}
	}


	@Override
	@Nullable
	public String getObject() {
		return this.paramValue;
	}

	@Override
	public Class<String> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
