/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * {@link AbstractGenericWebContextLoader} 的具体实现，从 XML 资源加载 bean 定义。
 * <p>默认资源位置使用后缀 {@code "-context.xml"} 检测。
 *
 * @author Sam Brannen
 * @since 3.2
 * @see GenericGroovyXmlWebContextLoader
 * @see AnnotationConfigWebContextLoader
 */
public class GenericXmlWebContextLoader extends AbstractGenericWebContextLoader {

	/**
	 * 使用 {@link XmlBeanDefinitionReader}，从所提供的 {@code WebMergedContextConfiguration} 中的位置加载 bean 定义到所提供的 {@link GenericWebApplicationContext context} 中。
	 * @see AbstractGenericWebContextLoader#loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(webMergedConfig.getLocations());
	}

	/**
	 * 返回 {@code "-context.xml"}，以支持检测默认 XML 配置文件。
	 */
	@Override
	protected String getResourceSuffix() {
		return "-context.xml";
	}

	/**
	 * 确保所提供的 {@link WebMergedContextConfiguration} 不包含{@link MergedContextConfiguration#getClasses() 类}。
	 * @since 4.0.4
	 * @see AbstractGenericWebContextLoader#validateMergedContextConfiguration
	 */
	@Override
	protected void validateMergedContextConfiguration(WebMergedContextConfiguration webMergedConfig) {
		if (webMergedConfig.hasClasses()) {
			String msg = String.format(
				"Test class [%s] has been configured with @ContextConfiguration's 'classes' attribute %s, "
						+ "but %s does not support annotated classes.", webMergedConfig.getTestClass().getName(),
				ObjectUtils.nullSafeToString(webMergedConfig.getClasses()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
