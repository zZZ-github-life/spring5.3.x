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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * 为 {@link ContextLoader} SPI 的所有具体实现提供基础的抽象应用上下文加载器。基于 <em>Template Method</em> 模式提供处理（{@link #processLocations processing}）资源位置的方法。
 *
 * <p>从 Spring 3.1 开始，{@code AbstractContextLoader} 还为 {@link SmartContextLoader} SPI 的所有具体实现提供基础。为了向后兼容 {@code ContextLoader} SPI，{@link #processContextConfiguration(ContextConfigurationAttributes)} 会委托给 {@link #processLocations(Class, String...)}。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.5
 * @see #generateDefaultLocations
 * @see #getResourceSuffixes
 * @see #modifyLocations
 * @see #prepareContext
 * @see #customizeContext
 */
public abstract class AbstractContextLoader implements SmartContextLoader {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final Log logger = LogFactory.getLog(AbstractContextLoader.class);


	// SmartContextLoader

	/**
	 * 为了向后兼容 {@link ContextLoader} SPI，默认实现简单地委托给 {@link #processLocations(Class, String...)}，将 {@link ContextConfigurationAttributes#getDeclaringClass() declaring class} 和从所提供的 {@link ContextConfigurationAttributes configuration attributes} 中获取的 {@link ContextConfigurationAttributes#getLocations() resource locations} 传递给它。随后，处理后的位置会被{@link ContextConfigurationAttributes#setLocations(String[]) 设置}到所提供的配置属性中。
	 * <p>可在子类中重写 &mdash; 例如，用于处理注解类而不是资源位置。
	 * @since 3.1
	 * @see #processLocations(Class, String...)
	 */
	@Override
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
		String[] processedLocations =
				processLocations(configAttributes.getDeclaringClass(), configAttributes.getLocations());
		configAttributes.setLocations(processedLocations);
	}

	/**
	 * 在读取 Bean 定义<i>之前</i>，准备由该 {@code SmartContextLoader} 创建的 {@link ConfigurableApplicationContext}。
	 * <p>默认实现：
	 * <ul>
	 * <li>将所提供的 {@code MergedContextConfiguration} 中的 <em>活动 Bean 定义配置文件</em>设置到上下文的 {@link org.springframework.core.env.Environment Environment} 中。</li>
	 * <li>将为所提供的 {@code MergedContextConfiguration} 中的所有 {@linkplain MergedContextConfiguration#getPropertySourceLocations() 资源位置}和 {@linkplain MergedContextConfiguration#getPropertySourceProperties() 内联属性}创建的 {@link PropertySource PropertySources} 添加到上下文的 {@code Environment} 中。</li>
	 * <li>确定通过 {@code MergedContextConfiguration} 提供了哪些上下文初始化器类（如果有），并实例化每个类，然后用给定的应用上下文{@linkplain ApplicationContextInitializer#initialize 调用}它们。
	 * <ul>
	 * <li>任何实现了 {@link org.springframework.core.Ordered Ordered} 或使用 {@link org.springframework.core.annotation.Order @Order} 注解的 {@code ApplicationContextInitializers} 都会被相应地排序。</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * @param context 新创建的应用上下文
	 * @param mergedConfig 合并后的上下文配置
	 * @since 3.2
	 * @see TestPropertySourceUtils#addPropertiesFilesToEnvironment
	 * @see TestPropertySourceUtils#addInlinedPropertiesToEnvironment
	 * @see ApplicationContextInitializer#initialize(ConfigurableApplicationContext)
	 * @see #loadContext(MergedContextConfiguration)
	 * @see ConfigurableApplicationContext#setId
	 */
	protected void prepareContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		context.getEnvironment().setActiveProfiles(mergedConfig.getActiveProfiles());
		TestPropertySourceUtils.addPropertiesFilesToEnvironment(context, mergedConfig.getPropertySourceLocations());
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, mergedConfig.getPropertySourceProperties());
		invokeApplicationContextInitializers(context, mergedConfig);
	}

	@SuppressWarnings("unchecked")
	private void invokeApplicationContextInitializers(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedConfig) {

		Set<Class<? extends ApplicationContextInitializer<?>>> initializerClasses =
				mergedConfig.getContextInitializerClasses();
		if (initializerClasses.isEmpty()) {
			// no ApplicationContextInitializers have been declared -> nothing to do
			return;
		}

		List<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerInstances = new ArrayList<>();
		Class<?> contextClass = context.getClass();

		for (Class<? extends ApplicationContextInitializer<?>> initializerClass : initializerClasses) {
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(context)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						contextClass.getName()));
			}
			initializerInstances.add((ApplicationContextInitializer<ConfigurableApplicationContext>) BeanUtils.instantiateClass(initializerClass));
		}

		AnnotationAwareOrderComparator.sort(initializerInstances);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : initializerInstances) {
			initializer.initialize(context);
		}
	}

	/**
	 * 在 Bean 定义已加载到上下文<i>之后</i>但上下文刷新<i>之前</i>，定制由该 {@code ContextLoader} 创建的 {@link ConfigurableApplicationContext}。
	 * <p>默认实现委托给已注册到所提供的 {@code mergedConfig} 的所有 {@link MergedContextConfiguration#getContextCustomizers context customizers}。
	 * @param context 新创建的应用上下文
	 * @param mergedConfig 合并后的上下文配置
	 * @since 4.3
	 */
	protected void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		for (ContextCustomizer contextCustomizer : mergedConfig.getContextCustomizers()) {
			contextCustomizer.customizeContext(context, mergedConfig);
		}
	}


	// ContextLoader

	/**
	 * 如果所提供的 {@code locations} 为 {@code null} 或<em>空</em>，且 {@link #isGenerateDefaultLocations()} 返回 {@code true}，则将针对指定的 {@link Class class} 和配置的 {@linkplain #getResourceSuffixes() 资源后缀}生成（即检测）默认位置；否则，将按需{@linkplain #modifyLocations 修改}所提供的 {@code locations} 并返回。
	 * @param clazz 位置所关联的类：在生成默认位置时使用
	 * @param locations 用于加载应用上下文的未修改位置
	 * （可以为 {@code null} 或空）
	 * @return 处理后的应用上下文资源位置数组
	 * @since 2.5
	 * @see #isGenerateDefaultLocations()
	 * @see #generateDefaultLocations(Class)
	 * @see #modifyLocations(Class, String...)
	 * @see org.springframework.test.context.ContextLoader#processLocations(Class, String...)
	 * @see #processContextConfiguration(ContextConfigurationAttributes)
	 */
	@Override
	public final String[] processLocations(Class<?> clazz, String... locations) {
		return (ObjectUtils.isEmpty(locations) && isGenerateDefaultLocations()) ?
				generateDefaultLocations(clazz) : modifyLocations(clazz, locations);
	}

	/**
	 * 根据所提供的类生成默认的类路径资源位置数组。
	 * <p>例如，如果所提供的类是 {@code com.example.MyTest}，则生成的位置将包含单个字符串，其值为 {@code "classpath:com/example/MyTest<suffix>"}，其中 {@code <suffix>} 是第一个配置的 {@linkplain #getResourceSuffixes() 资源后缀}的值，且该后缀对应的生成位置在类路径中实际存在。
	 * <p>从 Spring 3.1 开始，此方法的实现遵循 {@link SmartContextLoader} SPI 中定义的契约。具体而言，此方法将<em>预先</em>验证生成的默认位置确实存在。如果不存在，此方法将记录一条警告并返回空数组。
	 * <p>子类可以重写此方法以实现不同的<em>默认位置生成</em>策略。
	 * @param clazz 要为其生成默认位置的类
	 * @return 默认应用上下文资源位置数组
	 * @since 2.5
	 * @see #getResourceSuffixes()
	 */
	protected String[] generateDefaultLocations(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		String[] suffixes = getResourceSuffixes();
		for (String suffix : suffixes) {
			Assert.hasText(suffix, "Resource suffix must not be empty");
			String resourcePath = ClassUtils.convertClassNameToResourcePath(clazz.getName()) + suffix;
			ClassPathResource classPathResource = new ClassPathResource(resourcePath);
			if (classPathResource.exists()) {
				String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Detected default resource location \"%s\" for test class [%s]",
							prefixedResourcePath, clazz.getName()));
				}
				return new String[] {prefixedResourcePath};
			}
			else if (logger.isDebugEnabled()) {
				logger.debug(String.format("Did not detect default resource location for test class [%s]: " +
						"%s does not exist", clazz.getName(), classPathResource));
			}
		}

		if (logger.isInfoEnabled()) {
			logger.info(String.format("Could not detect default resource locations for test class [%s]: " +
					"no resource found for suffixes %s.", clazz.getName(), ObjectUtils.nullSafeToString(suffixes)));
		}

		return EMPTY_STRING_ARRAY;
	}

	/**
	 * 生成所提供位置数组的修改版本并返回。
	 * <p>默认实现简单地委托给 {@link TestContextResourceUtils#convertToClasspathResourcePaths}。
	 * <p>子类可以重写此方法以实现不同的<em>位置修改</em>策略。
	 * @param clazz 位置所关联的类
	 * @param locations 要修改的资源位置
	 * @return 修改后的应用上下文资源位置数组
	 * @since 2.5
	 */
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		return TestContextResourceUtils.convertToClasspathResourcePaths(clazz, locations);
	}

	/**
	 * 确定如果传递给 {@link #processLocations(Class, String...)} 的 {@code locations} 为 {@code null} 或空，是否应生成<em>默认</em>资源位置。
	 * <p>从 Spring 3.1 开始，此方法的语义已被扩展，以涵盖对默认资源位置或默认配置类的检测。因此，如果传递给 {@link #processContextConfiguration(ContextConfigurationAttributes)} 的 {@link ContextConfigurationAttributes configuration attributes} 中的 {@code classes} 为 {@code null} 或空，此方法还可用于确定是否应检测<em>默认</em>配置类。
	 * <p>子类可以重写此方法以更改默认行为。
	 * @return 默认情况下始终为 {@code true}
	 * @since 2.5
	 */
	protected boolean isGenerateDefaultLocations() {
		return true;
	}

	/**
	 * 获取在检测默认位置时要追加到 {@link ApplicationContext} 资源位置的后缀。
	 * <p>默认实现只是将 {@link #getResourceSuffix()} 返回的值包装在单元素数组中，但子类可以重写此方法以支持多个后缀。
	 * @return 资源后缀；从不为 {@code null} 或空
	 * @since 4.1
	 * @see #generateDefaultLocations(Class)
	 */
	protected String[] getResourceSuffixes() {
		return new String[] {getResourceSuffix()};
	}

	/**
	 * 获取在检测默认位置时要追加到 {@link ApplicationContext} 资源位置的后缀。
	 * <p>子类必须提供此方法的一个实现，返回单个后缀。或者，子类可以提供此方法的<em>无操作</em>实现，并重写 {@link #getResourceSuffixes()} 以提供多个自定义后缀。
	 * @return 资源后缀；从不为 {@code null} 或空
	 * @since 2.5
	 * @see #generateDefaultLocations(Class)
	 * @see #getResourceSuffixes()
	 */
	protected abstract String getResourceSuffix();

}
