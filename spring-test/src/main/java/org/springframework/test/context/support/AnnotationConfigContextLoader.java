/*
 * Copyright 2002-2019 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;

/**
 * 从组件类加载 Bean 定义的 {@link AbstractGenericContextLoader} 的具体实现。
 *
 * <p>有关<em>组件类</em>的定义，请参见 {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration} 的 Javadoc。
 *
 * <p>注意：{@code AnnotationConfigContextLoader} 支持<em>组件类</em>，而不是旧版 {@link org.springframework.test.context.ContextLoader ContextLoader} API 所定义的基于字符串的资源位置。因此，尽管 {@code AnnotationConfigContextLoader} 扩展了 {@code AbstractGenericContextLoader}，但 {@code AnnotationConfigContextLoader} <em>不</em>支持 {@code AbstractContextLoader} 或 {@code AbstractGenericContextLoader} 定义的任何基于字符串的方法。因此，{@code AnnotationConfigContextLoader} 主要应被视为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}，而不是 {@link org.springframework.test.context.ContextLoader ContextLoader}。
 *
 * @author Sam Brannen
 * @since 3.1
 * @see #processContextConfiguration(ContextConfigurationAttributes)
 * @see #detectDefaultConfigurationClasses(Class)
 * @see #loadBeanDefinitions(GenericApplicationContext, MergedContextConfiguration)
 * @see GenericXmlContextLoader
 * @see GenericGroovyXmlContextLoader
 */
public class AnnotationConfigContextLoader extends AbstractGenericContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoader.class);


	// SmartContextLoader

	/**
	 * 处理所提供的 {@link ContextConfigurationAttributes} 中的<em>组件类</em>。
	 * <p>如果<em>组件类</em>为 {@code null} 或空，且 {@link #isGenerateDefaultLocations()} 返回 {@code true}，则此 {@code SmartContextLoader} 将尝试{@link #detectDefaultConfigurationClasses 检测默认配置类}。如果检测到默认值，它们将被{@link ContextConfigurationAttributes#setClasses(Class[]) 设置}到所提供的配置属性中。否则，所提供的配置属性中的属性将不会被修改。
	 * @param configAttributes the context configuration attributes to process
	 * @see org.springframework.test.context.SmartContextLoader#processContextConfiguration(ContextConfigurationAttributes)
	 * @see #isGenerateDefaultLocations()
	 * @see #detectDefaultConfigurationClasses(Class)
	 */
	@Override
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
		if (!configAttributes.hasClasses() && isGenerateDefaultLocations()) {
			configAttributes.setClasses(detectDefaultConfigurationClasses(configAttributes.getDeclaringClass()));
		}
	}


	// AnnotationConfigContextLoader

	/**
	 * 为所提供的测试类检测默认配置类。
	 * <p>默认实现简单地委托给 {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses(Class)}。
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @return an array of default configuration classes, potentially empty but
	 * never {@code null}
	 * @see AnnotationConfigContextLoaderUtils
	 */
	protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
	}


	// AbstractContextLoader

	/**
	 * {@code AnnotationConfigContextLoader} 应作为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} 使用，而不是作为旧版 {@link org.springframework.test.context.ContextLoader ContextLoader} 使用。因此，不支持此方法。
	 * @throws UnsupportedOperationException in this implementation
	 * @see AbstractContextLoader#modifyLocations
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException(
				"AnnotationConfigContextLoader does not support the modifyLocations(Class, String...) method");
	}

	/**
	 * {@code AnnotationConfigContextLoader} 应作为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} 使用，而不是作为旧版 {@link org.springframework.test.context.ContextLoader ContextLoader} 使用。因此，不支持此方法。
	 * @throws UnsupportedOperationException in this implementation
	 * @see AbstractContextLoader#generateDefaultLocations
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		throw new UnsupportedOperationException(
				"AnnotationConfigContextLoader does not support the generateDefaultLocations(Class) method");
	}

	/**
	 * {@code AnnotationConfigContextLoader} 应作为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} 使用，而不是作为旧版 {@link org.springframework.test.context.ContextLoader ContextLoader} 使用。因此，不支持此方法。
	 * @throws UnsupportedOperationException in this implementation
	 * @see AbstractContextLoader#getResourceSuffix
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"AnnotationConfigContextLoader does not support the getResourceSuffix() method");
	}


	// AbstractGenericContextLoader

	/**
	 * 确保所提供的 {@link MergedContextConfiguration} 不包含 {@link MergedContextConfiguration#getLocations() 位置}。
	 * @since 4.0.4
	 * @see AbstractGenericContextLoader#validateMergedContextConfiguration
	 */
	@Override
	protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		if (mergedConfig.hasLocations()) {
			String msg = String.format("Test class [%s] has been configured with @ContextConfiguration's 'locations' " +
							"(or 'value') attribute %s, but %s does not support resource locations.",
					mergedConfig.getTestClass().getName(), ObjectUtils.nullSafeToString(mergedConfig.getLocations()),
					getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

	/**
	 * 从所提供的 {@link MergedContextConfiguration} 中的类，将类注册到所提供的 {@link GenericApplicationContext context} 中。
	 * <p>每个类都必须表示一个<em>组件类</em>。使用 {@link AnnotatedBeanDefinitionReader} 注册相应的 Bean 定义。
	 * <p>请注意，此方法不调用 {@link #createBeanDefinitionReader}，因为 {@code AnnotatedBeanDefinitionReader} 不是 {@link BeanDefinitionReader} 的实例。
	 * @param context the context in which the component classes should be registered
	 * @param mergedConfig the merged configuration from which the classes should be retrieved
	 * @see AbstractGenericContextLoader#loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		Class<?>[] componentClasses = mergedConfig.getClasses();
		if (logger.isDebugEnabled()) {
			logger.debug("Registering component classes: " + ObjectUtils.nullSafeToString(componentClasses));
		}
		new AnnotatedBeanDefinitionReader(context).register(componentClasses);
	}

	/**
	 * {@code AnnotationConfigContextLoader} 应作为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} 使用，而不是作为旧版 {@link org.springframework.test.context.ContextLoader ContextLoader} 使用。因此，不支持此方法。
	 * @throws UnsupportedOperationException in this implementation
	 * @see #loadBeanDefinitions
	 * @see AbstractGenericContextLoader#createBeanDefinitionReader
	 */
	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		throw new UnsupportedOperationException(
				"AnnotationConfigContextLoader does not support the createBeanDefinitionReader(GenericApplicationContext) method");
	}

}
