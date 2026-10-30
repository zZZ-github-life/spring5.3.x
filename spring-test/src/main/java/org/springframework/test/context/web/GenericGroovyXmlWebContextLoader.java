/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.context.web;

import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * {@link AbstractGenericWebContextLoader} 的具体实现，从 Groovy 脚本<em>和</em> XML 配置文件加载 bean 定义。
 * <p>默认资源位置使用后缀 {@code "-context.xml"} 和 {@code "Context.groovy"} 检测。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see GroovyBeanDefinitionReader
 * @see GenericXmlWebContextLoader
 * @see AnnotationConfigWebContextLoader
 */
public class GenericGroovyXmlWebContextLoader extends GenericXmlWebContextLoader {

	/**
	 * 使用 {@link GroovyBeanDefinitionReader}，从所提供的 {@code WebMergedContextConfiguration} 中的位置加载 bean 定义到所提供的 {@link GenericWebApplicationContext context} 中。
	 * @param context 应将 Bean 定义加载到的上下文
	 * @param webMergedConfig 合并后的上下文配置
	 * @see AbstractGenericWebContextLoader#loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {
		new GroovyBeanDefinitionReader(context).loadBeanDefinitions(webMergedConfig.getLocations());
	}

	/**
	 * 返回 {@code "-context.xml" 和 "Context.groovy"}，以支持检测默认 XML 配置文件或 Groovy 脚本。
	 */
	@Override
	protected String[] getResourceSuffixes() {
		return new String[] { super.getResourceSuffix(), "Context.groovy" };
	}

	/**
	 * {@code GenericGroovyXmlWebContextLoader} 同时支持 Groovy 和 XML 资源类型以检测默认值。因此，不支持此方法。
	 * @throws UnsupportedOperationException 在此实现中
	 * @see #getResourceSuffixes()
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"GenericGroovyXmlWebContextLoader does not support the getResourceSuffix() method");
	}

}
