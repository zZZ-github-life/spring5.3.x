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

package org.springframework.test.context.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoaderUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * {@link AbstractGenericWebContextLoader} 的具体实现，从带注解的类加载 bean 定义。
 * <p>有关<em>带注解的类</em>的定义，请参见 {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration} 的 Javadoc。
 * <p>注意：{@code AnnotationConfigWebContextLoader} 支持<em>带注解的类</em>，而不支持旧版 {@link org.springframework.test.context.ContextLoader ContextLoader} API 定义的基于字符串的资源位置。因此，尽管 {@code AnnotationConfigWebContextLoader} 扩展了 {@code AbstractGenericWebContextLoader}，但 {@code AnnotationConfigWebContextLoader} <em>不</em>支持 {@link org.springframework.test.context.support.AbstractContextLoader AbstractContextLoader} 或 {@code AbstractGenericWebContextLoader} 定义的任何基于字符串的方法。因此，{@code AnnotationConfigWebContextLoader} 主要应被视为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}，而不是 {@link org.springframework.test.context.ContextLoader ContextLoader}。
 *
 * @author Sam Brannen
 * @since 3.2
 * @see #processContextConfiguration(ContextConfigurationAttributes)
 * @see #detectDefaultConfigurationClasses(Class)
 * @see #loadBeanDefinitions(GenericWebApplicationContext, WebMergedContextConfiguration)
 * @see GenericXmlWebContextLoader
 * @see GenericGroovyXmlWebContextLoader
 */
public class AnnotationConfigWebContextLoader extends AbstractGenericWebContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigWebContextLoader.class);


	// SmartContextLoader

	/**
	 * 处理所提供的 {@link ContextConfigurationAttributes} 中的<em>带注解的类</em>。
	 * <p>如果<em>带注解的类</em>为 {@code null} 或空，并且 {@link #isGenerateDefaultLocations()} 返回 {@code true}，则此 {@code SmartContextLoader} 将尝试{@linkplain #detectDefaultConfigurationClasses 检测默认配置类}。如果检测到默认值，将{@linkplain ContextConfigurationAttributes#setClasses(Class[]) 设置}到所提供的配置属性中。否则，所提供的配置属性中的属性将不会被修改。
	 * @param configAttributes 要处理的上下文配置属性
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

	/**
	 * 检测所提供测试类的默认配置类。
	 * <p>默认实现仅委托给 {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses(Class)}。
	 * @param declaringClass 声明了 {@code @ContextConfiguration} 的测试类
	 * @return 默认配置类的数组，可能为空，但从不为 {@code null}
	 * @see AnnotationConfigContextLoaderUtils
	 */
	protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
	}


	// AbstractContextLoader

	/**
	 * {@code AnnotationConfigWebContextLoader} 应作为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} 使用，而不是旧版 {@link org.springframework.test.context.ContextLoader ContextLoader}。因此，不支持此方法。
	 * @throws UnsupportedOperationException 在此实现中
	 * @see org.springframework.test.context.support.AbstractContextLoader#modifyLocations
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException(
				"AnnotationConfigWebContextLoader does not support the modifyLocations(Class, String...) method");
	}

	/**
	 * {@code AnnotationConfigWebContextLoader} 应作为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} 使用，而不是旧版 {@link org.springframework.test.context.ContextLoader ContextLoader}。因此，不支持此方法。
	 * @throws UnsupportedOperationException 在此实现中
	 * @see org.springframework.test.context.support.AbstractContextLoader#generateDefaultLocations
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		throw new UnsupportedOperationException(
				"AnnotationConfigWebContextLoader does not support the generateDefaultLocations(Class) method");
	}

	/**
	 * {@code AnnotationConfigWebContextLoader} 应作为 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} 使用，而不是旧版 {@link org.springframework.test.context.ContextLoader ContextLoader}。因此，不支持此方法。
	 * @throws UnsupportedOperationException 在此实现中
	 * @see org.springframework.test.context.support.AbstractContextLoader#getResourceSuffix
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"AnnotationConfigWebContextLoader does not support the getResourceSuffix() method");
	}


	// AbstractGenericWebContextLoader

	/**
	 * 从所提供的 {@link WebMergedContextConfiguration} 中的类，将类注册到所提供的{@linkplain GenericWebApplicationContext 上下文}中。
	 * <p>每个类都必须表示一个<em>带注解的类</em>。使用 {@link AnnotatedBeanDefinitionReader} 注册相应的 bean 定义。
	 * @param context 应在其上注册注解类的上下文
	 * @param webMergedConfig 应从中检索类的合并配置
	 * @see AbstractGenericWebContextLoader#loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(
			GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {

		Class<?>[] annotatedClasses = webMergedConfig.getClasses();
		if (logger.isDebugEnabled()) {
			logger.debug("Registering annotated classes: " + ObjectUtils.nullSafeToString(annotatedClasses));
		}
		new AnnotatedBeanDefinitionReader(context).register(annotatedClasses);
	}

	/**
	 * 确保所提供的 {@link WebMergedContextConfiguration} 不包含{@link MergedContextConfiguration#getLocations() 位置}。
	 * @since 4.0.4
	 * @see AbstractGenericWebContextLoader#validateMergedContextConfiguration
	 */
	@Override
	protected void validateMergedContextConfiguration(WebMergedContextConfiguration webMergedConfig) {
		if (webMergedConfig.hasLocations()) {
			String msg = String.format("Test class [%s] has been configured with @ContextConfiguration's 'locations' " +
							"(or 'value') attribute %s, but %s does not support resource locations.",
					webMergedConfig.getTestClass().getName(),
					ObjectUtils.nullSafeToString(webMergedConfig.getLocations()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
