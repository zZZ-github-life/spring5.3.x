/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code MergedContextConfiguration} 封装在测试类及其所有超类和
 * 封闭类上通过 {@link ContextConfiguration @ContextConfiguration}、
 * {@link ActiveProfiles @ActiveProfiles} 和
 * {@link TestPropertySource @TestPropertySource} 声明的<em>合并</em>上下文
 * 配置。
 *
 * <p>合并后的上下文资源位置、注解类、活动配置、
 * 属性资源位置和内联属性表示测试类层次结构和封闭类层次结构中
 * 所有声明的值，同时
 * 考虑 {@link ContextConfiguration#inheritLocations}、
 * {@link ActiveProfiles#inheritProfiles}、
 * {@link TestPropertySource#inheritLocations} 和
 * {@link TestPropertySource#inheritProperties} 标志的语义。
 *
 * <p>{@link SmartContextLoader} 使用 {@code MergedContextConfiguration}
 * 加载 {@link org.springframework.context.ApplicationContext ApplicationContext}。
 *
 * <p>{@link org.springframework.test.context.cache.ContextCache ContextCache} 还使用
 * {@code MergedContextConfiguration} 作为键，缓存使用此 {@code MergedContextConfiguration} 的属性
 * 加载的
 * {@link org.springframework.context.ApplicationContext ApplicationContext}。
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.1
 * @see ContextConfiguration
 * @see ContextHierarchy
 * @see ActiveProfiles
 * @see TestPropertySource
 * @see ContextConfigurationAttributes
 * @see SmartContextLoader#loadContext(MergedContextConfiguration)
 */
public class MergedContextConfiguration implements Serializable {

	private static final long serialVersionUID = -3290560718464957422L;

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

	private static final Set<Class<? extends ApplicationContextInitializer<?>>> EMPTY_INITIALIZER_CLASSES =
			Collections.<Class<? extends ApplicationContextInitializer<?>>> emptySet();

	private static final Set<ContextCustomizer> EMPTY_CONTEXT_CUSTOMIZERS = Collections.<ContextCustomizer> emptySet();


	private final Class<?> testClass;

	private final String[] locations;

	private final Class<?>[] classes;

	private final Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses;

	private final String[] activeProfiles;

	private final String[] propertySourceLocations;

	private final String[] propertySourceProperties;

	private final Set<ContextCustomizer> contextCustomizers;

	private final ContextLoader contextLoader;

	@Nullable
	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	@Nullable
	private final MergedContextConfiguration parent;


	/**
	 * 为提供的参数创建一个新的 {@code MergedContextConfiguration} 实例。
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged context resource locations
	 * @param classes the merged annotated classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param contextLoader the resolved {@code ContextLoader}
	 */
	public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
			@Nullable String[] activeProfiles, @Nullable ContextLoader contextLoader) {

		this(testClass, locations, classes, null, activeProfiles, contextLoader);
	}

	/**
	 * 为提供的参数创建一个新的 {@code MergedContextConfiguration} 实例。
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged context resource locations
	 * @param classes the merged annotated classes
	 * @param contextInitializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param contextLoader the resolved {@code ContextLoader}
	 */
	public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
			@Nullable String[] activeProfiles, @Nullable ContextLoader contextLoader) {

		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, contextLoader, null, null);
	}

	/**
	 * 为提供的参数创建一个新的 {@code MergedContextConfiguration} 实例。
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged context resource locations
	 * @param classes the merged annotated classes
	 * @param contextInitializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param contextLoader the resolved {@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
	 * delegate with which to retrieve the parent context
	 * @param parent the parent configuration or {@code null} if there is no parent
	 * @since 3.2.2
	 */
	public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
			@Nullable String[] activeProfiles, @Nullable ContextLoader contextLoader,
			@Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
			@Nullable MergedContextConfiguration parent) {

		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, null, null,
				contextLoader, cacheAwareContextLoaderDelegate, parent);
	}

	/**
	 * 通过从提供的 {@code MergedContextConfiguration} 复制所有
	 * 字段创建一个新的 {@code MergedContextConfiguration} 实例。
	 * @since 4.1
	 */
	public MergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		this(mergedConfig.testClass, mergedConfig.locations, mergedConfig.classes,
				mergedConfig.contextInitializerClasses, mergedConfig.activeProfiles, mergedConfig.propertySourceLocations,
				mergedConfig.propertySourceProperties, mergedConfig.contextCustomizers,
				mergedConfig.contextLoader, mergedConfig.cacheAwareContextLoaderDelegate, mergedConfig.parent);
	}

	/**
	 * 为提供的参数创建一个新的 {@code MergedContextConfiguration} 实例。
	 * <p>如果为 {@code locations}、{@code classes}、{@code activeProfiles}、
	 * {@code propertySourceLocations} 或 {@code propertySourceProperties}
	 * 提供 {@code null} 值，将改为存储空数组。
	 * 如果为 {@code contextInitializerClasses} 提供 {@code null} 值，
	 * 将改为存储空集。此外，活动配置将排序，重复的配置
	 * 将被移除。
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged context resource locations
	 * @param classes the merged annotated classes
	 * @param contextInitializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param propertySourceLocations the merged {@code PropertySource} locations
	 * @param propertySourceProperties the merged {@code PropertySource} properties
	 * @param contextLoader the resolved {@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
	 * delegate with which to retrieve the parent context
	 * @param parent the parent configuration or {@code null} if there is no parent
	 * @since 4.1
	 */
	public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
			@Nullable String[] activeProfiles, @Nullable String[] propertySourceLocations,
			@Nullable String[] propertySourceProperties, @Nullable ContextLoader contextLoader,
			@Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
			@Nullable MergedContextConfiguration parent) {

		this(testClass, locations, classes, contextInitializerClasses, activeProfiles,
				propertySourceLocations, propertySourceProperties,
				EMPTY_CONTEXT_CUSTOMIZERS, contextLoader,
				cacheAwareContextLoaderDelegate, parent);
	}

	/**
	 * 为提供的参数创建一个新的 {@code MergedContextConfiguration} 实例。
	 * <p>如果为 {@code locations}、{@code classes}、{@code activeProfiles}、
	 * {@code propertySourceLocations} 或 {@code propertySourceProperties}
	 * 提供 {@code null} 值，将改为存储空数组。
	 * 如果为 {@code contextInitializerClasses} 或 {@code contextCustomizers}
	 * 提供 {@code null} 值，将改为存储空集。
	 * 此外，活动配置将排序，重复的配置
	 * 将被移除。
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged context resource locations
	 * @param classes the merged annotated classes
	 * @param contextInitializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param propertySourceLocations the merged {@code PropertySource} locations
	 * @param propertySourceProperties the merged {@code PropertySource} properties
	 * @param contextCustomizers the context customizers
	 * @param contextLoader the resolved {@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
	 * delegate with which to retrieve the parent context
	 * @param parent the parent configuration or {@code null} if there is no parent
	 * @since 4.3
	 */
	public MergedContextConfiguration(Class<?> testClass, @Nullable String[] locations, @Nullable Class<?>[] classes,
			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
			@Nullable String[] activeProfiles, @Nullable String[] propertySourceLocations,
			@Nullable String[] propertySourceProperties, @Nullable Set<ContextCustomizer> contextCustomizers,
			@Nullable ContextLoader contextLoader, @Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
			@Nullable MergedContextConfiguration parent) {

		this.testClass = testClass;
		this.locations = processStrings(locations);
		this.classes = processClasses(classes);
		this.contextInitializerClasses = processContextInitializerClasses(contextInitializerClasses);
		this.activeProfiles = processActiveProfiles(activeProfiles);
		this.propertySourceLocations = processStrings(propertySourceLocations);
		this.propertySourceProperties = processStrings(propertySourceProperties);
		this.contextCustomizers = processContextCustomizers(contextCustomizers);
		this.contextLoader = contextLoader;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
		this.parent = parent;
	}


	/**
	 * 获取与此 {@code MergedContextConfiguration} 关联的
	 * {@linkplain Class 测试类}。
	 */
	public Class<?> getTestClass() {
		return this.testClass;
	}

	/**
	 * 获取 {@linkplain #getTestClass() 测试类}的 {@code ApplicationContext}
	 * 配置文件的合并资源位置。
	 * <p>上下文资源位置通常表示 XML 配置
	 * 文件或 Groovy 脚本。
	 */
	public String[] getLocations() {
		return this.locations;
	}

	/**
	 * 获取 {@linkplain #getTestClass() 测试类}的合并注解类。
	 */
	public Class<?>[] getClasses() {
		return this.classes;
	}

	/**
	 * 确定此 {@code MergedContextConfiguration} 实例是否具有
	 * 基于路径的上下文资源位置。
	 * @return {@code true} if the {@link #getLocations() locations} array is not empty
	 * @since 4.0.4
	 * @see #hasResources()
	 * @see #hasClasses()
	 */
	public boolean hasLocations() {
		return !ObjectUtils.isEmpty(getLocations());
	}

	/**
	 * 确定此 {@code MergedContextConfiguration} 实例是否具有
	 * 基于类的资源。
	 * @return {@code true} if the {@link #getClasses() classes} array is not empty
	 * @since 4.0.4
	 * @see #hasResources()
	 * @see #hasLocations()
	 */
	public boolean hasClasses() {
		return !ObjectUtils.isEmpty(getClasses());
	}

	/**
	 * 确定此 {@code MergedContextConfiguration} 实例是否具有
	 * 基于路径的上下文资源位置或基于类的资源。
	 * @return {@code true} if either the {@link #getLocations() locations}
	 * or the {@link #getClasses() classes} array is not empty
	 * @since 4.0.4
	 * @see #hasLocations()
	 * @see #hasClasses()
	 */
	public boolean hasResources() {
		return (hasLocations() || hasClasses());
	}

	/**
	 * 获取 {@linkplain #getTestClass() 测试类}的合并
	 * {@code ApplicationContextInitializer} 类。
	 */
	public Set<Class<? extends ApplicationContextInitializer<?>>> getContextInitializerClasses() {
		return this.contextInitializerClasses;
	}

	/**
	 * 获取 {@linkplain #getTestClass() 测试类}的合并活动
	 * Bean 定义配置。
	 * @see ActiveProfiles
	 */
	public String[] getActiveProfiles() {
		return this.activeProfiles;
	}

	/**
	 * 获取 {@linkplain #getTestClass() 测试类}的测试 {@code PropertySources}
	 * 的合并资源位置。
	 * @see TestPropertySource#locations
	 * @see java.util.Properties
	 */
	public String[] getPropertySourceLocations() {
		return this.propertySourceLocations;
	}

	/**
	 * 获取 {@linkplain #getTestClass() 测试类}的合并测试 {@code PropertySource} 属性。
	 * <p>属性将加载到 {@code Environment} 的
	 * {@code PropertySources} 集合中。
	 * @see TestPropertySource#properties
	 * @see java.util.Properties
	 */
	public String[] getPropertySourceProperties() {
		return this.propertySourceProperties;
	}

	/**
	 * 获取加载应用程序上下文时将应用的合并
	 * {@link ContextCustomizer ContextCustomizer}。
	 */
	public Set<ContextCustomizer> getContextCustomizers() {
		return this.contextCustomizers;
	}

	/**
	 * 获取 {@linkplain #getTestClass() 测试类}的已解析 {@link ContextLoader}。
	 */
	public ContextLoader getContextLoader() {
		return this.contextLoader;
	}

	/**
	 * 获取上下文层次结构中父应用程序上下文的
	 * {@link MergedContextConfiguration}。
	 * @return the parent configuration or {@code null} if there is no parent
	 * @since 3.2.2
	 * @see #getParentApplicationContext()
	 */
	@Nullable
	public MergedContextConfiguration getParent() {
		return this.parent;
	}

	/**
	 * 从上下文缓存中获取此 {@code MergedContextConfiguration} 定义的上下文的
	 * 父 {@link ApplicationContext}。
	 * <p>如果父上下文尚未加载，将加载它、存储
	 * 到缓存中，然后返回。
	 * @return the parent {@code ApplicationContext} or {@code null} if there is no parent
	 * @since 3.2.2
	 * @see #getParent()
	 */
	@Nullable
	public ApplicationContext getParentApplicationContext() {
		if (this.parent == null) {
			return null;
		}
		Assert.state(this.cacheAwareContextLoaderDelegate != null,
				"Cannot retrieve a parent application context without access to the CacheAwareContextLoaderDelegate");
		return this.cacheAwareContextLoaderDelegate.loadContext(this.parent);
	}


	/**
	 * 通过比较两个对象的 {@linkplain #getLocations() 位置}、
	 * {@linkplain #getClasses() 注解类}、
	 * {@linkplain #getContextInitializerClasses() 上下文初始化器类}、
	 * {@linkplain #getActiveProfiles() 活动配置}、
	 * {@linkplain #getPropertySourceLocations() 属性源位置}、
	 * {@linkplain #getPropertySourceProperties() 属性源属性}、
	 * {@linkplain #getParent() 父配置}以及其
	 * {@link #getContextLoader() ContextLoader} 的完全限定名，
	 * 确定提供的对象是否等于此 {@code MergedContextConfiguration} 实例。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}

		MergedContextConfiguration otherConfig = (MergedContextConfiguration) other;
		if (!Arrays.equals(this.locations, otherConfig.locations)) {
			return false;
		}
		if (!Arrays.equals(this.classes, otherConfig.classes)) {
			return false;
		}
		if (!this.contextInitializerClasses.equals(otherConfig.contextInitializerClasses)) {
			return false;
		}
		if (!Arrays.equals(this.activeProfiles, otherConfig.activeProfiles)) {
			return false;
		}
		if (!Arrays.equals(this.propertySourceLocations, otherConfig.propertySourceLocations)) {
			return false;
		}
		if (!Arrays.equals(this.propertySourceProperties, otherConfig.propertySourceProperties)) {
			return false;
		}
		if (!this.contextCustomizers.equals(otherConfig.contextCustomizers)) {
			return false;
		}

		if (this.parent == null) {
			if (otherConfig.parent != null) {
				return false;
			}
		}
		else if (!this.parent.equals(otherConfig.parent)) {
			return false;
		}

		if (!nullSafeClassName(this.contextLoader).equals(nullSafeClassName(otherConfig.contextLoader))) {
			return false;
		}

		return true;
	}

	/**
	 * 为此 {@code MergedContextConfiguration} 的所有属性（不包括
	 * {@linkplain #getTestClass() 测试类}）生成唯一的哈希码。
	 */
	@Override
	public int hashCode() {
		int result = Arrays.hashCode(this.locations);
		result = 31 * result + Arrays.hashCode(this.classes);
		result = 31 * result + this.contextInitializerClasses.hashCode();
		result = 31 * result + Arrays.hashCode(this.activeProfiles);
		result = 31 * result + Arrays.hashCode(this.propertySourceLocations);
		result = 31 * result + Arrays.hashCode(this.propertySourceProperties);
		result = 31 * result + this.contextCustomizers.hashCode();
		result = 31 * result + (this.parent != null ? this.parent.hashCode() : 0);
		result = 31 * result + nullSafeClassName(this.contextLoader).hashCode();
		return result;
	}

	/**
	 * 提供 {@linkplain #getTestClass() 测试类}、
	 * {@linkplain #getLocations() 位置}、{@linkplain #getClasses() 注解类}、
	 * {@linkplain #getContextInitializerClasses() 上下文初始化器类}、
	 * {@linkplain #getActiveProfiles() 活动配置}、
	 * {@linkplain #getPropertySourceLocations() 属性源位置}、
	 * {@linkplain #getPropertySourceProperties() 属性源属性}、
	 * {@linkplain #getContextCustomizers() 上下文自定义器}、
	 * {@link #getContextLoader() ContextLoader} 的名称以及
	 * {@linkplain #getParent() 父配置}的 String 表示形式。
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("testClass", this.testClass)
				.append("locations", ObjectUtils.nullSafeToString(this.locations))
				.append("classes", ObjectUtils.nullSafeToString(this.classes))
				.append("contextInitializerClasses", ObjectUtils.nullSafeToString(this.contextInitializerClasses))
				.append("activeProfiles", ObjectUtils.nullSafeToString(this.activeProfiles))
				.append("propertySourceLocations", ObjectUtils.nullSafeToString(this.propertySourceLocations))
				.append("propertySourceProperties", ObjectUtils.nullSafeToString(this.propertySourceProperties))
				.append("contextCustomizers", this.contextCustomizers)
				.append("contextLoader", nullSafeClassName(this.contextLoader))
				.append("parent", this.parent)
				.toString();
	}


	private static String[] processStrings(@Nullable String[] array) {
		return (array != null ? array : EMPTY_STRING_ARRAY);
	}

	private static Class<?>[] processClasses(@Nullable Class<?>[] classes) {
		return (classes != null ? classes : EMPTY_CLASS_ARRAY);
	}

	private static Set<Class<? extends ApplicationContextInitializer<?>>> processContextInitializerClasses(
			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses) {

		return (contextInitializerClasses != null ?
				Collections.unmodifiableSet(contextInitializerClasses) : EMPTY_INITIALIZER_CLASSES);
	}

	private static Set<ContextCustomizer> processContextCustomizers(
			@Nullable Set<ContextCustomizer> contextCustomizers) {

		return (contextCustomizers != null ?
				Collections.unmodifiableSet(contextCustomizers) : EMPTY_CONTEXT_CUSTOMIZERS);
	}

	private static String[] processActiveProfiles(@Nullable String[] activeProfiles) {
		if (activeProfiles == null) {
			return EMPTY_STRING_ARRAY;
		}

		// Active profiles must be unique
		Set<String> profilesSet = new LinkedHashSet<>(Arrays.asList(activeProfiles));
		return StringUtils.toStringArray(profilesSet);
	}

	/**
	 * 仅基于加载器的完全限定名或（如果提供的加载器为 {@code null} 时的）&quot;null&quot;，
	 * 生成提供的
	 * {@link ContextLoader} 的 null 安全 {@link String} 表示形式。
	 */
	protected static String nullSafeClassName(@Nullable ContextLoader contextLoader) {
		return (contextLoader != null ? contextLoader.getClass().getName() : "null");
	}

}
