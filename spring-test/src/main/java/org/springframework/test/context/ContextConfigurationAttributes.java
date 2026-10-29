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

package org.springframework.test.context;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code ContextConfigurationAttributes} 封装通过
 * {@link ContextConfiguration @ContextConfiguration} 声明的上下文配置属性。
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.1
 * @see ContextConfiguration
 * @see SmartContextLoader#processContextConfiguration(ContextConfigurationAttributes)
 * @see MergedContextConfiguration
 */
public class ContextConfigurationAttributes {

	private static final String[] EMPTY_LOCATIONS = new String[0];

	private static final Class<?>[] EMPTY_CLASSES = new Class<?>[0];


	private static final Log logger = LogFactory.getLog(ContextConfigurationAttributes.class);

	private final Class<?> declaringClass;

	private Class<?>[] classes = new Class<?>[0];

	private String[] locations = new String[0];

	private final boolean inheritLocations;

	private final Class<? extends ApplicationContextInitializer<?>>[] initializers;

	private final boolean inheritInitializers;

	@Nullable
	private final String name;

	private final Class<? extends ContextLoader> contextLoaderClass;


	/**
	 * 使用默认值构造一个新的 {@link ContextConfigurationAttributes} 实例。
	 * @param declaringClass the test class that declared {@code @ContextConfiguration},
	 * either explicitly or implicitly
	 * @since 4.3
	 */
	@SuppressWarnings("unchecked")
	public ContextConfigurationAttributes(Class<?> declaringClass) {
		this(declaringClass, EMPTY_LOCATIONS, EMPTY_CLASSES, false, (Class[]) EMPTY_CLASSES, true, ContextLoader.class);
	}

	/**
	 * 为提供的 {@link ContextConfiguration @ContextConfiguration} 注解及其
	 * 声明它的 {@linkplain Class 测试类}构造一个新的 {@link ContextConfigurationAttributes} 实例。
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param contextConfiguration the annotation from which to retrieve the attributes
	 */
	public ContextConfigurationAttributes(Class<?> declaringClass, ContextConfiguration contextConfiguration) {
		this(declaringClass, contextConfiguration.locations(), contextConfiguration.classes(),
				contextConfiguration.inheritLocations(), contextConfiguration.initializers(),
				contextConfiguration.inheritInitializers(), contextConfiguration.name(), contextConfiguration.loader());
	}

	/**
	 * 为提供的 {@link AnnotationAttributes}（从
	 * {@link ContextConfiguration @ContextConfiguration} 注解解析而来）及其
	 * 声明它们的 {@linkplain Class 测试类}构造一个新的 {@link ContextConfigurationAttributes} 实例。
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param annAttrs the annotation attributes from which to retrieve the attributes
	 */
	@SuppressWarnings("unchecked")
	public ContextConfigurationAttributes(Class<?> declaringClass, AnnotationAttributes annAttrs) {
		this(declaringClass, annAttrs.getStringArray("locations"), annAttrs.getClassArray("classes"),
				annAttrs.getBoolean("inheritLocations"),
				(Class<? extends ApplicationContextInitializer<?>>[]) annAttrs.getClassArray("initializers"),
				annAttrs.getBoolean("inheritInitializers"), annAttrs.getString("name"), annAttrs.getClass("loader"));
	}

	/**
	 * 为声明 {@link ContextConfiguration @ContextConfiguration} 注解及其
	 * 对应属性的 {@linkplain Class 测试类}构造一个新的 {@link ContextConfigurationAttributes}
	 * 实例。
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param locations the resource locations declared via {@code @ContextConfiguration}
	 * @param classes the annotated classes declared via {@code @ContextConfiguration}
	 * @param inheritLocations the {@code inheritLocations} flag declared via {@code @ContextConfiguration}
	 * @param initializers the context initializers declared via {@code @ContextConfiguration}
	 * @param inheritInitializers the {@code inheritInitializers} flag declared via {@code @ContextConfiguration}
	 * @param contextLoaderClass the {@code ContextLoader} class declared via {@code @ContextConfiguration}
	 * @throws IllegalArgumentException if the {@code declaringClass} or {@code contextLoaderClass} is
	 * {@code null}
	 */
	public ContextConfigurationAttributes(
			Class<?> declaringClass, String[] locations, Class<?>[] classes, boolean inheritLocations,
			Class<? extends ApplicationContextInitializer<?>>[] initializers,
			boolean inheritInitializers, Class<? extends ContextLoader> contextLoaderClass) {

		this(declaringClass, locations, classes, inheritLocations, initializers, inheritInitializers, null,
				contextLoaderClass);
	}

	/**
	 * 为声明 {@link ContextConfiguration @ContextConfiguration} 注解及其
	 * 对应属性的 {@linkplain Class 测试类}构造一个新的 {@link ContextConfigurationAttributes}
	 * 实例。
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @param locations the resource locations declared via {@code @ContextConfiguration}
	 * @param classes the annotated classes declared via {@code @ContextConfiguration}
	 * @param inheritLocations the {@code inheritLocations} flag declared via {@code @ContextConfiguration}
	 * @param initializers the context initializers declared via {@code @ContextConfiguration}
	 * @param inheritInitializers the {@code inheritInitializers} flag declared via {@code @ContextConfiguration}
	 * @param name the name of level in the context hierarchy, or {@code null} if not applicable
	 * @param contextLoaderClass the {@code ContextLoader} class declared via {@code @ContextConfiguration}
	 * @throws IllegalArgumentException if the {@code declaringClass} or {@code contextLoaderClass} is
	 * {@code null}
	 */
	public ContextConfigurationAttributes(
			Class<?> declaringClass, String[] locations, Class<?>[] classes, boolean inheritLocations,
			Class<? extends ApplicationContextInitializer<?>>[] initializers,
			boolean inheritInitializers, @Nullable String name, Class<? extends ContextLoader> contextLoaderClass) {

		Assert.notNull(declaringClass, "'declaringClass' must not be null");
		Assert.notNull(contextLoaderClass, "'contextLoaderClass' must not be null");

		if (!ObjectUtils.isEmpty(locations) && !ObjectUtils.isEmpty(classes) && logger.isDebugEnabled()) {
			logger.debug(String.format(
					"Test class [%s] has been configured with @ContextConfiguration's 'locations' (or 'value') %s " +
					"and 'classes' %s attributes. Most SmartContextLoader implementations support " +
					"only one declaration of resources per @ContextConfiguration annotation.",
					declaringClass.getName(), ObjectUtils.nullSafeToString(locations),
					ObjectUtils.nullSafeToString(classes)));
		}

		this.declaringClass = declaringClass;
		this.locations = locations;
		this.classes = classes;
		this.inheritLocations = inheritLocations;
		this.initializers = initializers;
		this.inheritInitializers = inheritInitializers;
		this.name = (StringUtils.hasText(name) ? name : null);
		this.contextLoaderClass = contextLoaderClass;
	}


	/**
	 * 获取显式或隐式声明
	 * {@link ContextConfiguration @ContextConfiguration} 注解的 {@linkplain Class 类}。
	 * @return the declaring class (never {@code null})
	 */
	public Class<?> getDeclaringClass() {
		return this.declaringClass;
	}

	/**
	 * 设置<em>处理后的</em>注解类，实际上覆盖
	 * 通过 {@link ContextConfiguration @ContextConfiguration} 声明的原始值。
	 * @see #getClasses()
	 */
	public void setClasses(Class<?>... classes) {
		this.classes = classes;
	}

	/**
	 * 获取通过
	 * {@link ContextConfiguration @ContextConfiguration} 声明的注解类。
	 * <p>注意：这是一个可变属性。因此，返回的值可能
	 * 表示<em>处理后的</em>值，与通过
	 * {@link ContextConfiguration @ContextConfiguration} 声明的原始值不匹配。
	 * @return the annotated classes (potentially {<em>empty</em>)
	 * @see ContextConfiguration#classes
	 * @see #setClasses(Class[])
	 */
	public Class<?>[] getClasses() {
		return this.classes;
	}

	/**
	 * 确定此 {@code ContextConfigurationAttributes} 实例是否具有
	 * 基于类的资源。
	 * @return {@code true} if the {@link #getClasses() classes} array is not empty
	 * @see #hasResources()
	 * @see #hasLocations()
	 */
	public boolean hasClasses() {
		return (getClasses().length > 0);
	}

	/**
	 * 设置<em>处理后的</em>资源位置，实际上覆盖
	 * 通过 {@link ContextConfiguration @ContextConfiguration} 声明的原始值。
	 * @see #getLocations()
	 */
	public void setLocations(String... locations) {
		this.locations = locations;
	}

	/**
	 * 获取通过
	 * {@link ContextConfiguration @ContextConfiguration} 声明的资源位置。
	 * <p>注意：这是一个可变属性。因此，返回的值可能
	 * 表示<em>处理后的</em>值，与通过
	 * {@link ContextConfiguration @ContextConfiguration} 声明的原始值不匹配。
	 * @return the resource locations (potentially <em>empty</em>)
	 * @see ContextConfiguration#value
	 * @see ContextConfiguration#locations
	 * @see #setLocations
	 */
	public String[] getLocations() {
		return this.locations;
	}

	/**
	 * 确定此 {@code ContextConfigurationAttributes} 实例是否具有
	 * 基于路径的资源位置。
	 * @return {@code true} if the {@link #getLocations() locations} array is not empty
	 * @see #hasResources()
	 * @see #hasClasses()
	 */
	public boolean hasLocations() {
		return (getLocations().length > 0);
	}

	/**
	 * 确定此 {@code ContextConfigurationAttributes} 实例是否具有
	 * 基于路径的资源位置或基于类的资源。
	 * @return {@code true} if either the {@link #getLocations() locations}
	 * or the {@link #getClasses() classes} array is not empty
	 * @see #hasLocations()
	 * @see #hasClasses()
	 */
	public boolean hasResources() {
		return (hasLocations() || hasClasses());
	}

	/**
	 * 获取通过
	 * {@link ContextConfiguration @ContextConfiguration} 声明的 {@code inheritLocations} 标志。
	 * @return the {@code inheritLocations} flag
	 * @see ContextConfiguration#inheritLocations
	 */
	public boolean isInheritLocations() {
		return this.inheritLocations;
	}

	/**
	 * 获取通过
	 * {@link ContextConfiguration @ContextConfiguration} 声明的 {@code ApplicationContextInitializer} 类。
	 * @return the {@code ApplicationContextInitializer} classes
	 * @since 3.2
	 */
	public Class<? extends ApplicationContextInitializer<?>>[] getInitializers() {
		return this.initializers;
	}

	/**
	 * 获取通过
	 * {@link ContextConfiguration @ContextConfiguration} 声明的 {@code inheritInitializers} 标志。
	 * @return the {@code inheritInitializers} flag
	 * @since 3.2
	 */
	public boolean isInheritInitializers() {
		return this.inheritInitializers;
	}

	/**
	 * 获取通过
	 * {@link ContextConfiguration @ContextConfiguration} 声明的上下文层次结构级别的名称。
	 * @return the name of the context hierarchy level or {@code null} if not applicable
	 * @since 3.2.2
	 * @see ContextConfiguration#name()
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 获取通过
	 * {@link ContextConfiguration @ContextConfiguration} 声明的 {@code ContextLoader} 类。
	 * @return the {@code ContextLoader} class
	 * @see ContextConfiguration#loader
	 */
	public Class<? extends ContextLoader> getContextLoaderClass() {
		return this.contextLoaderClass;
	}


	/**
	 * 通过比较两个对象的
	 * {@linkplain #getDeclaringClass() 声明类}、
	 * {@linkplain #getLocations() 位置}、
	 * {@linkplain #getClasses() 注解类}、
	 * {@linkplain #isInheritLocations() inheritLocations 标志}、
	 * {@linkplain #getInitializers() 上下文初始化器类}、
	 * {@linkplain #isInheritInitializers() inheritInitializers 标志}以及
	 * {@link #getContextLoaderClass() ContextLoader 类}，
	 * 确定提供的对象是否等于此 {@code ContextConfigurationAttributes} 实例。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ContextConfigurationAttributes)) {
			return false;
		}
		ContextConfigurationAttributes otherAttr = (ContextConfigurationAttributes) other;
		return (ObjectUtils.nullSafeEquals(this.declaringClass, otherAttr.declaringClass) &&
				Arrays.equals(this.classes, otherAttr.classes)) &&
				Arrays.equals(this.locations, otherAttr.locations) &&
				this.inheritLocations == otherAttr.inheritLocations &&
				Arrays.equals(this.initializers, otherAttr.initializers) &&
				this.inheritInitializers == otherAttr.inheritInitializers &&
				ObjectUtils.nullSafeEquals(this.name, otherAttr.name) &&
				ObjectUtils.nullSafeEquals(this.contextLoaderClass, otherAttr.contextLoaderClass);
	}

	/**
	 * 为此 {@code ContextConfigurationAttributes} 实例的所有属性（不包括
	 * {@linkplain #getName() 名称}）生成唯一的哈希码。
	 */
	@Override
	public int hashCode() {
		int result = this.declaringClass.hashCode();
		result = 31 * result + Arrays.hashCode(this.classes);
		result = 31 * result + Arrays.hashCode(this.locations);
		result = 31 * result + Arrays.hashCode(this.initializers);
		return result;
	}

	/**
	 * 提供上下文配置属性和声明类的 String 表示形式。
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("declaringClass", this.declaringClass.getName())
				.append("classes", ObjectUtils.nullSafeToString(this.classes))
				.append("locations", ObjectUtils.nullSafeToString(this.locations))
				.append("inheritLocations", this.inheritLocations)
				.append("initializers", ObjectUtils.nullSafeToString(this.initializers))
				.append("inheritInitializers", this.inheritInitializers)
				.append("name", this.name)
				.append("contextLoaderClass", this.contextLoaderClass.getName())
				.toString();
	}

}
