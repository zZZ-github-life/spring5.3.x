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

package org.springframework.test.context.web;

import java.util.Set;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code WebMergedContextConfiguration} 封装了通过 {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}、{@link WebAppConfiguration @WebAppConfiguration} 和 {@link org.springframework.test.context.ActiveProfiles @ActiveProfiles} 在测试类及其所有超类上声明的<em>合并后的</em>上下文配置。
 * <p>{@code WebMergedContextConfiguration} 扩展了 {@link MergedContextConfiguration} 的契约，增加了对通过 {@code @WebAppConfiguration} 配置的{@link #getResourceBasePath() 资源基路径}的支持。这允许 {@link org.springframework.test.context.TestContext TestContext} 正确缓存使用此 {@code WebMergedContextConfiguration} 的属性加载的相应 {@link org.springframework.web.context.WebApplicationContext WebApplicationContext}。
 *
 * @author Sam Brannen
 * @since 3.2
 * @see WebAppConfiguration
 * @see MergedContextConfiguration
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.ActiveProfiles
 * @see org.springframework.test.context.ContextConfigurationAttributes
 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
 */
public class WebMergedContextConfiguration extends MergedContextConfiguration {

	private static final long serialVersionUID = 7323361588604247458L;

	private final String resourceBasePath;


	/**
	 * 通过复制所提供的 {@code MergedContextConfiguration} 中的所有属性，创建一个新的 {@code WebMergedContextConfiguration} 实例。
	 * <p>如果为 {@code resourceBasePath} 提供了<em>空</em>值，则将使用空字符串。
	 * @param resourceBasePath the resource path to the root directory of the web application
	 * @since 4.1
	 */
	public WebMergedContextConfiguration(MergedContextConfiguration mergedConfig, String resourceBasePath) {
		super(mergedConfig);
		this.resourceBasePath = !StringUtils.hasText(resourceBasePath) ? "" : resourceBasePath;
	}

	/**
	 * 为所提供的参数创建一个新的 {@code WebMergedContextConfiguration} 实例。
	 * <p>如果为 {@code locations}、{@code classes}、{@code activeProfiles}、{@code propertySourceLocations} 或 {@code propertySourceProperties} 提供了 {@code null} 值，则将改为存储空数组。如果为 {@code contextInitializerClasses} 提供了 {@code null} 值，则将改为存储空集合。如果为 {@code resourceBasePath} 提供了<em>空</em>值，则将使用空字符串。此外，活动配置文件将被排序，重复的配置文件将被移除。
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged resource locations
	 * @param classes the merged annotated classes
	 * @param contextInitializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param propertySourceLocations the merged {@code PropertySource} locations
	 * @param propertySourceProperties the merged {@code PropertySource} properties
	 * @param resourceBasePath the resource path to the root directory of the web application
	 * @param contextLoader the resolved {@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
	 * delegate with which to retrieve the parent context
	 * @param parent the parent configuration or {@code null} if there is no parent
	 * @since 4.1
	 */
	public WebMergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
			@Nullable String[] activeProfiles, @Nullable String[] propertySourceLocations, @Nullable String[] propertySourceProperties,
			String resourceBasePath, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, @Nullable MergedContextConfiguration parent) {

		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, propertySourceLocations,
			propertySourceProperties, null, resourceBasePath, contextLoader, cacheAwareContextLoaderDelegate, parent);
	}

	/**
	 * 为所提供的参数创建一个新的 {@code WebMergedContextConfiguration} 实例。
	 * <p>如果为 {@code locations}、{@code classes}、{@code activeProfiles}、{@code propertySourceLocations} 或 {@code propertySourceProperties} 提供了 {@code null} 值，则将改为存储空数组。如果为 {@code contextInitializerClasses} 或 {@code contextCustomizers} 提供了 {@code null} 值，则将改为存储空集合。如果为 {@code resourceBasePath} 提供了<em>空</em>值，则将使用空字符串。此外，活动配置文件将被排序，重复的配置文件将被移除。
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged context resource locations
	 * @param classes the merged annotated classes
	 * @param contextInitializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param propertySourceLocations the merged {@code PropertySource} locations
	 * @param propertySourceProperties the merged {@code PropertySource} properties
	 * @param contextCustomizers the context customizers
	 * @param resourceBasePath the resource path to the root directory of the web application
	 * @param contextLoader the resolved {@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
	 * delegate with which to retrieve the parent context
	 * @param parent the parent configuration or {@code null} if there is no parent
	 * @since 4.3
	 */
	public WebMergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
			@Nullable String[] activeProfiles, @Nullable String[] propertySourceLocations, @Nullable String[] propertySourceProperties,
			@Nullable Set<ContextCustomizer> contextCustomizers, String resourceBasePath, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, @Nullable MergedContextConfiguration parent) {

		super(testClass, locations, classes, contextInitializerClasses, activeProfiles, propertySourceLocations,
			propertySourceProperties, contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parent);

		this.resourceBasePath = (StringUtils.hasText(resourceBasePath) ? resourceBasePath : "");
	}

	/**
	 * 获取{@linkplain #getTestClass() 测试类}的 Web 应用程序根目录资源路径（通过 {@code @WebAppConfiguration} 配置）。
	 * @see WebAppConfiguration
	 */
	public String getResourceBasePath() {
		return this.resourceBasePath;
	}


	/**
	 * 通过比较两个对象的{@linkplain #getLocations() 位置}、{@linkplain #getClasses() 带注解的类}、{@linkplain #getContextInitializerClasses() 上下文初始化器类}、{@linkplain #getActiveProfiles() 活动配置文件}、{@linkplain #getResourceBasePath() 资源基路径}、{@linkplain #getParent() 父级}及其 {@link #getContextLoader() ContextLoaders} 的完全限定名，确定所提供的对象是否等于此 {@code WebMergedContextConfiguration} 实例。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (super.equals(other) &&
				this.resourceBasePath.equals(((WebMergedContextConfiguration) other).resourceBasePath)));
	}

	/**
	 * 为此 {@code WebMergedContextConfiguration} 的所有属性（不包括{@linkplain #getTestClass() 测试类}）生成唯一的哈希码。
	 */
	@Override
	public int hashCode() {
		return (31 * super.hashCode() + this.resourceBasePath.hashCode());
	}

	/**
	 * 提供{@linkplain #getTestClass() 测试类}、{@linkplain #getLocations() 位置}、{@linkplain #getClasses() 带注解的类}、{@linkplain #getContextInitializerClasses() 上下文初始化器类}、{@linkplain #getActiveProfiles() 活动配置文件}、{@linkplain #getPropertySourceLocations() 属性源位置}、{@linkplain #getPropertySourceProperties() 属性源属性}、{@linkplain #getContextCustomizers() 上下文自定义器}、{@linkplain #getResourceBasePath() 资源基路径}、{@link #getContextLoader() ContextLoader} 的名称以及{@linkplain #getParent() 父配置}的字符串表示形式。
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("testClass", getTestClass())
				.append("locations", ObjectUtils.nullSafeToString(getLocations()))
				.append("classes", ObjectUtils.nullSafeToString(getClasses()))
				.append("contextInitializerClasses", ObjectUtils.nullSafeToString(getContextInitializerClasses()))
				.append("activeProfiles", ObjectUtils.nullSafeToString(getActiveProfiles()))
				.append("propertySourceLocations", ObjectUtils.nullSafeToString(getPropertySourceLocations()))
				.append("propertySourceProperties", ObjectUtils.nullSafeToString(getPropertySourceProperties()))
				.append("contextCustomizers", getContextCustomizers())
				.append("resourceBasePath", getResourceBasePath())
				.append("contextLoader", nullSafeClassName(getContextLoader()))
				.append("parent", getParent())
				.toString();
	}

}
