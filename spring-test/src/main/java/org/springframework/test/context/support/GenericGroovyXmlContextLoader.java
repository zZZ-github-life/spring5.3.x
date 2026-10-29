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

package org.springframework.test.context.support;

import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * 从 Groovy 脚本<em>和</em> XML 配置文件读取 Bean 定义的 {@link AbstractGenericContextLoader} 的具体实现。
 *
 * <p>默认资源位置使用后缀 {@code "-context.xml"} 和 {@code "Context.groovy"} 进行检测。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see GroovyBeanDefinitionReader
 * @see GenericXmlContextLoader
 * @see AnnotationConfigContextLoader
 */
public class GenericGroovyXmlContextLoader extends GenericXmlContextLoader {

	/**
	 * 使用 {@link GroovyBeanDefinitionReader}，从所提供的 {@code MergedContextConfiguration} 中的位置将 Bean 定义加载到所提供的 {@link GenericApplicationContext context} 中。
	 * @param context the context into which the bean definitions should be loaded
	 * @param mergedConfig the merged context configuration
	 * @see org.springframework.test.context.support.AbstractGenericContextLoader#loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		new GroovyBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
	}

	/**
	 * 返回 {@code "-context.xml"} 和 {@code "Context.groovy"}，以支持检测默认的 XML 配置文件或 Groovy 脚本。
	 */
	@Override
	protected String[] getResourceSuffixes() {
		return new String[] { super.getResourceSuffix(), "Context.groovy" };
	}

	/**
	 * {@code GenericGroovyXmlContextLoader} 同时支持 Groovy 和 XML 资源类型来检测默认值。因此，不支持此方法。
	 * @throws UnsupportedOperationException in this implementation
	 * @see #getResourceSuffixes()
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"GenericGroovyXmlContextLoader does not support the getResourceSuffix() method");
	}

}
