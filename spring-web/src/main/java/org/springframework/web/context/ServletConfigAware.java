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

package org.springframework.web.context;

import javax.servlet.ServletConfig;

import org.springframework.beans.factory.Aware;

/**
 * 由任何希望获知自己运行所在 {@link ServletConfig}（通常由
 * {@link WebApplicationContext} 确定）的对象实现的接口。
 *
 * <p>注意：只有在实际运行于 Servlet 特定的 WebApplicationContext 中时才会生效。
 * 否则，不会设置任何 ServletConfig。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 * @see ServletContextAware
 */
public interface ServletConfigAware extends Aware {

	/**
	 * 设置此对象运行所在的 {@link ServletConfig}。
	 * <p>在填充普通 bean 属性之后、在诸如 InitializingBean 的
	 * {@code afterPropertiesSet} 或自定义 init-method 等初始化回调之前调用。
	 * 在 ApplicationContextAware 的 {@code setApplicationContext} 之后调用。
	 * @param servletConfig 此对象要使用的 {@link ServletConfig}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setServletConfig(ServletConfig servletConfig);

}
