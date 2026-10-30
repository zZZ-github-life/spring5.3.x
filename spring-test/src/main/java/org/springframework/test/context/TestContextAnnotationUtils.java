/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.ObjectUtils;

/**
 * {@code TestContextAnnotationUtils} 是一组工具方法，补充
 * {@link AnnotationUtils} 和 {@link AnnotatedElementUtils} 中已有的标准支持，
 * 同时透明地遵循
 * {@link NestedTestConfiguration @NestedTestConfiguration} 语义。
 *
 * <p>主要用于 <em>Spring TestContext Framework</em> 内部。
 *
 * <p>{@code AnnotationUtils} 和 {@code AnnotatedElementUtils} 提供
 * <em>获取</em>或<em>查找</em>注解的工具，
 * 而 {@code TestContextAnnotationUtils} 更进一步，支持
 * 确定注解直接或通过<em>组合注解</em>间接声明所在的<em>根类</em>。这些
 * 附加信息封装在 {@link AnnotationDescriptor} 中。
 *
 * <p>{@code AnnotationDescriptor} 提供的附加信息是
 * <em>Spring TestContext Framework</em> 所需的，以便能够
 * 支持诸如 {@link ContextConfiguration @ContextConfiguration}、
 * {@link TestExecutionListeners @TestExecutionListeners} 和
 * {@link ActiveProfiles @ActiveProfiles} 等注解的类继承和封闭类层次结构遍历，
 * 这些注解支持合并和覆盖各种<em>继承的</em>注解属性 ——
 * 例如 {@link ContextConfiguration#inheritLocations}。
 *
 * @author Sam Brannen
 * @since 5.3, though originally since 4.0 as {@link org.springframework.test.util.MetaAnnotationUtils}
 * @see AnnotationUtils
 * @see AnnotatedElementUtils
 * @see AnnotationDescriptor
 */
public abstract class TestContextAnnotationUtils {

	private static final ConcurrentLruCache<Class<?>, EnclosingConfiguration> cachedEnclosingConfigurationModes =
			new ConcurrentLruCache<>(32, TestContextAnnotationUtils::lookUpEnclosingConfiguration);

	@Nullable
	private static volatile EnclosingConfiguration defaultEnclosingConfigurationMode;


	/**
	 * 根据 {@link #findMergedAnnotation(Class, Class)} 中使用的
	 * 搜索算法，确定指定的 {@code annotationType} 注解是否
	 * <em>存在</em>或<em>元存在</em>于提供的 {@link Class} 上。
	 * <p>如果此方法返回 {@code true}，则 {@code findMergedAnnotation(...)}
	 * 将返回非 null 值。
	 * @param clazz 要查找注解的类
	 * @param annotationType 要查找的注解类型
	 * @return 如果存在匹配的注解，则返回 {@code true}。
	 * @since 5.3.3
	 * @see #findMergedAnnotation(Class, Class)
	 */
	public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationType) {
		return (findMergedAnnotation(clazz, annotationType) != null);
	}

	/**
	 * 在提供的类<em>之上</em>的注解层次结构中查找指定 {@code annotationType} 的第一个注解，
	 * 将该注解的属性与注解层次结构较低级别中注解的<em>匹配</em>
	 * 属性合并，并将结果合成为指定 {@code annotationType} 的注解。
	 * <p>在此方法的上下文中，术语 "above" 指的是
	 * 所提供类的 {@linkplain Class#getSuperclass() 超类}层次结构或
	 * {@linkplain Class#getEnclosingClass() 封闭类}层次结构中的位置。封闭类层次结构
	 * 将仅根据 {@link NestedTestConfiguration @NestedTestConfiguration}
	 * 语义进行搜索。
	 * <p>完全支持 {@link org.springframework.core.annotation.AliasFor @AliasFor} 语义，
	 * 无论是在单个注解内还是在注解层次结构内。
	 * @param clazz 要查找注解的类
	 * @param annotationType 要查找的注解类型
	 * @return 合并、合成的 {@code Annotation}，如果未找到则为 {@code null}
	 * @see AnnotatedElementUtils#findMergedAnnotation(java.lang.reflect.AnnotatedElement, Class)
	 * @see #findAnnotationDescriptor(Class, Class)
	 * @see #searchEnclosingClass(Class)
	 */
	@Nullable
	public static <T extends Annotation> T findMergedAnnotation(Class<?> clazz, Class<T> annotationType) {
		return findMergedAnnotation(clazz, annotationType, TestContextAnnotationUtils::searchEnclosingClass);
	}

	@Nullable
	private static <T extends Annotation> T findMergedAnnotation(Class<?> clazz, Class<T> annotationType,
			Predicate<Class<?>> searchEnclosingClass) {

		AnnotationDescriptor<T> descriptor =
				findAnnotationDescriptor(clazz, annotationType, searchEnclosingClass, new HashSet<>());
		return (descriptor != null ? descriptor.getAnnotation() : null);
	}

	/**
	 * 在提供的类<em>之上</em>的注解层次结构中获取指定 {@code annotationType}
	 * 的所有<em>可重复注解</em>；对于找到的每个注解，
	 * 将该注解的属性与注解层次结构较低级别中注解的<em>匹配</em>
	 * 属性合并，并将结果合成为指定 {@code annotationType} 的注解。
	 * <p>如果提供的类没有任何可重复注解的本地声明，此方法将查找在超类上声明的
	 * {@link java.lang.annotation.Inherited @Inherited} 注解。
	 * 如果未找到继承的注解，此方法将在
	 * 所提供类的 {@linkplain Class#getEnclosingClass() 封闭类}层次结构中搜索。
	 * 封闭类层次结构将仅根据 {@link NestedTestConfiguration @NestedTestConfiguration}
	 * 语义进行搜索。
	 * <p>存放可重复注解的容器类型将通过 {@link java.lang.annotation.Repeatable} 查找。
	 * <p>完全支持 {@link org.springframework.core.annotation.AliasFor @AliasFor} 语义，
	 * 无论是在单个注解内还是在注解层次结构内。
	 * @param clazz 要在其上搜索注解的类（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @return 找到的所有合并可重复注解的集合，
	 * 如果未找到则为空集合
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(java.lang.reflect.AnnotatedElement, Class)
	 * @see #searchEnclosingClass(Class)
	 */
	public static <T extends Annotation> Set<T> getMergedRepeatableAnnotations(
			Class<?> clazz, Class<T> annotationType) {

		// Present (via @Inherited semantics), directly present, or meta-present?
		Set<T> mergedAnnotations = MergedAnnotations.from(clazz, SearchStrategy.INHERITED_ANNOTATIONS)
				.stream(annotationType)
				.collect(MergedAnnotationCollectors.toAnnotationSet());

		if (!mergedAnnotations.isEmpty()) {
			return mergedAnnotations;
		}

		// Declared on an enclosing class of an inner class?
		if (searchEnclosingClass(clazz)) {
			// Then mimic @Inherited semantics within the enclosing class hierarchy.
			return getMergedRepeatableAnnotations(clazz.getEnclosingClass(), annotationType);
		}

		return Collections.emptySet();
	}

	/**
	 * 在提供的 {@link Class} 上查找指定 {@code annotationType} 的
	 * {@link AnnotationDescriptor}；如果在给定类本身上找不到注解，则遍历其注解、接口、
	 * 超类和封闭类。
	 * <p>此方法显式处理未声明为 {@linkplain java.lang.annotation.Inherited 继承}的
	 * 类级注解<em>以及元注解</em>。
	 * <p>算法运行如下：
	 * <ol>
	 * <li>在给定类上搜索该注解，如果找到则返回相应的
	 * {@code AnnotationDescriptor}。
	 * <li>递归搜索给定类声明的所有注解。</li>
	 * <li>递归搜索给定类实现的所有接口。</li>
	 * <li>递归搜索给定类的超类层次结构。</li>
	 * <li>如果根据 {@link NestedTestConfiguration @NestedTestConfiguration}
	 * 语义适当，递归搜索给定类的封闭类层次结构。</li>
	 * </ol>
	 * <p>在此上下文中，术语<em>递归</em>意味着搜索
	 * 过程继续返回到第 1 步，将当前注解、
	 * 接口、超类或封闭类作为要查找注解的类。
	 * @param clazz 要查找注解的类
	 * @param annotationType 要查找的注解类型
	 * @return 如果找到该注解，则为对应的注解描述符；
	 * 否则为 {@code null}
	 * @see #findAnnotationDescriptorForTypes(Class, Class...)
	 */
	@Nullable
	public static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(
			Class<?> clazz, Class<T> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null");
		return findAnnotationDescriptor(clazz, annotationType, TestContextAnnotationUtils::searchEnclosingClass,
			new HashSet<>());
	}

	/**
	 * 执行 {@link #findAnnotationDescriptor(Class, Class)} 的搜索算法，
	 * 通过跟踪已<em>访问</em>的注解来避免无限递归。
	 * @param clazz 要查找注解的类
	 * @param annotationType 要查找的注解类型
	 * @param searchEnclosingClass 一个谓词，如果应对封闭类执行搜索，
	 * 则其求值为 {@code true}
	 * @param visited 已访问过的注解集合
	 * @return 如果找到该注解，则为对应的注解描述符；
	 * 否则为 {@code null}
	 */
	@Nullable
	private static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(
			@Nullable Class<?> clazz, Class<T> annotationType, Predicate<Class<?>> searchEnclosingClass,
			Set<Annotation> visited) {

		if (clazz == null || Object.class == clazz) {
			return null;
		}

		// Declared locally?
		if (AnnotationUtils.isAnnotationDeclaredLocally(annotationType, clazz)) {
			return new AnnotationDescriptor<>(clazz, clazz.getAnnotation(annotationType));
		}

		AnnotationDescriptor<T> descriptor = null;

		// Declared on a composed annotation (i.e., as a meta-annotation)?
		for (Annotation composedAnn : clazz.getDeclaredAnnotations()) {
			Class<? extends Annotation> composedType = composedAnn.annotationType();
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(composedType.getName()) && visited.add(composedAnn)) {
				descriptor = findAnnotationDescriptor(composedType, annotationType, searchEnclosingClass, visited);
				if (descriptor != null) {
					return new AnnotationDescriptor<>(clazz, descriptor.getDeclaringClass(), descriptor.getAnnotation());
				}
			}
		}

		// Declared on an interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			descriptor = findAnnotationDescriptor(ifc, annotationType, searchEnclosingClass, visited);
			if (descriptor != null) {
				return new AnnotationDescriptor<>(clazz, descriptor.getDeclaringClass(), descriptor.getAnnotation());
			}
		}

		// Declared on a superclass?
		descriptor = findAnnotationDescriptor(clazz.getSuperclass(), annotationType, searchEnclosingClass, visited);
		if (descriptor != null) {
			return descriptor;
		}

		// Declared on an enclosing class of an inner class?
		if (searchEnclosingClass.test(clazz)) {
			descriptor = findAnnotationDescriptor(clazz.getEnclosingClass(), annotationType, searchEnclosingClass, visited);
			if (descriptor != null) {
				return descriptor;
			}
		}

		return null;
	}

	/**
	 * 在指定 {@code clazz} 的继承层次结构中（包括
	 * 指定 {@code clazz} 本身）查找第一个声明了至少一个指定
	 * {@code annotationTypes} 的 {@link Class} 的 {@link UntypedAnnotationDescriptor}。
	 * <p>如果在给定类本身上找不到注解，此方法会遍历指定 {@code clazz}
	 * 的注解、接口、超类和封闭类。
	 * <p>此方法显式处理未声明为 {@linkplain java.lang.annotation.Inherited 继承}的
	 * 类级注解<em>以及元注解</em>。
	 * <p>算法运行如下：
	 * <ol>
	 * <li>在给定类上搜索其中一个注解类型的本地声明，
	 * 如果找到则返回相应的 {@code UntypedAnnotationDescriptor}。
	 * <li>递归搜索给定类声明的所有注解。</li>
	 * <li>递归搜索给定类实现的所有接口。</li>
	 * <li>递归搜索给定类的超类层次结构。</li>
	 * <li>如果根据 {@link NestedTestConfiguration @NestedTestConfiguration}
	 * 语义适当，递归搜索给定类的封闭类层次结构。</li>
	 * </ol>
	 * <p>在此上下文中，术语<em>递归</em>意味着搜索
	 * 过程继续返回到第 1 步，将当前注解、
	 * 接口、超类或封闭类作为要查找注解的类。
	 * @param clazz 要查找注解的类
	 * @param annotationTypes 要查找的注解类型
	 * @return 如果找到其中一个注解，则为对应的注解描述符；
	 * 否则为 {@code null}
	 * @see #findAnnotationDescriptor(Class, Class)
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static UntypedAnnotationDescriptor findAnnotationDescriptorForTypes(
			Class<?> clazz, Class<? extends Annotation>... annotationTypes) {

		assertNonEmptyAnnotationTypeArray(annotationTypes, "The list of annotation types must not be empty");
		return findAnnotationDescriptorForTypes(clazz, annotationTypes, new HashSet<>());
	}

	/**
	 * 执行 {@link #findAnnotationDescriptorForTypes(Class, Class...)} 的搜索算法，
	 * 通过跟踪已<em>访问</em>的注解来避免无限递归。
	 * @param clazz 要查找注解的类
	 * @param annotationTypes 要查找的注解类型
	 * @param visited 已访问过的注解集合
	 * @return 如果找到其中一个注解，则为对应的注解描述符；
	 * 否则为 {@code null}
	 */
	@Nullable
	private static UntypedAnnotationDescriptor findAnnotationDescriptorForTypes(@Nullable Class<?> clazz,
			Class<? extends Annotation>[] annotationTypes, Set<Annotation> visited) {

		if (clazz == null || Object.class == clazz) {
			return null;
		}

		// Declared locally?
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			if (AnnotationUtils.isAnnotationDeclaredLocally(annotationType, clazz)) {
				return new UntypedAnnotationDescriptor(clazz, clazz.getAnnotation(annotationType), annotationTypes);
			}
		}

		// Declared on a composed annotation (i.e., as a meta-annotation)?
		for (Annotation composedAnnotation : clazz.getDeclaredAnnotations()) {
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(composedAnnotation) && visited.add(composedAnnotation)) {
				UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
						composedAnnotation.annotationType(), annotationTypes, visited);
				if (descriptor != null) {
					return new UntypedAnnotationDescriptor(clazz, descriptor.getDeclaringClass(),
							descriptor.getAnnotation(), annotationTypes);
				}
			}
		}

		// Declared on an interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(ifc, annotationTypes, visited);
			if (descriptor != null) {
				return new UntypedAnnotationDescriptor(clazz, descriptor.getDeclaringClass(),
						descriptor.getAnnotation(), annotationTypes);
			}
		}

		// Declared on a superclass?
		UntypedAnnotationDescriptor descriptor =
				findAnnotationDescriptorForTypes(clazz.getSuperclass(), annotationTypes, visited);
		if (descriptor != null) {
			return descriptor;
		}

		// Declared on an enclosing class of an inner class?
		if (searchEnclosingClass(clazz)) {
			descriptor = findAnnotationDescriptorForTypes(clazz.getEnclosingClass(), annotationTypes, visited);
			if (descriptor != null) {
				return descriptor;
			}
		}

		return null;
	}

	/**
	 * 确定 <em>Spring TestContext Framework</em> 中的注解搜索算法是否应搜索
	 * 所提供类的封闭类上的注解。
	 * @param clazz 可能应搜索其封闭类的类
	 * @return 如果提供的类是内部类且应搜索其封闭类，
	 * 则返回 {@code true}。
	 * @see ClassUtils#isInnerClass(Class)
	 * @see NestedTestConfiguration @NestedTestConfiguration
	 */
	public static boolean searchEnclosingClass(Class<?> clazz) {
		return (ClassUtils.isInnerClass(clazz) &&
				getEnclosingConfiguration(clazz) == EnclosingConfiguration.INHERIT);
	}

	static void clearCaches() {
		cachedEnclosingConfigurationModes.clear();
		defaultEnclosingConfigurationMode = null;
	}

	/**
	 * 获取所提供类的 {@link EnclosingConfiguration} 模式。
	 * @param clazz 应为其解析封闭配置模式的类
	 * @return 解析得到的封闭配置模式
	 */
	private static EnclosingConfiguration getEnclosingConfiguration(Class<?> clazz) {
		return cachedEnclosingConfigurationModes.get(clazz);
	}

	private static EnclosingConfiguration lookUpEnclosingConfiguration(Class<?> clazz) {
		// @NestedTestConfiguration should not be discovered on an enclosing class
		// for a nested interface (which is always static), so our predicate simply
		// ensures that the candidate class is an inner class.
		Predicate<Class<?>> searchEnclosingClass = ClassUtils::isInnerClass;
		NestedTestConfiguration nestedTestConfiguration =
				findMergedAnnotation(clazz, NestedTestConfiguration.class, searchEnclosingClass);
		return (nestedTestConfiguration != null ? nestedTestConfiguration.value() : getDefaultEnclosingConfigurationMode());
	}

	private static EnclosingConfiguration getDefaultEnclosingConfigurationMode() {
		EnclosingConfiguration defaultConfigurationMode = defaultEnclosingConfigurationMode;
		if (defaultConfigurationMode == null) {
			String value = SpringProperties.getProperty(NestedTestConfiguration.ENCLOSING_CONFIGURATION_PROPERTY_NAME);
			EnclosingConfiguration enclosingConfigurationMode = EnclosingConfiguration.from(value);
			defaultConfigurationMode =
					(enclosingConfigurationMode != null ? enclosingConfigurationMode : EnclosingConfiguration.INHERIT);
			defaultEnclosingConfigurationMode = defaultConfigurationMode;
		}
		return defaultConfigurationMode;
	}

	private static void assertNonEmptyAnnotationTypeArray(Class<?>[] annotationTypes, String message) {
		if (ObjectUtils.isEmpty(annotationTypes)) {
			throw new IllegalArgumentException(message);
		}
		for (Class<?> clazz : annotationTypes) {
			if (!Annotation.class.isAssignableFrom(clazz)) {
				throw new IllegalArgumentException("Array elements must be of type Annotation");
			}
		}
	}


	/**
	 * 注解的描述符，包括注解<em>声明</em>所在的
	 * {@linkplain #getDeclaringClass() 类}以及 {@linkplain #getAnnotation() 合并后的注解}
	 * 实例。
	 * <p>如果注解用作元注解，则<em>根声明
	 * 类</em>并未直接使用该注解，而是通过组合注解
	 * 间接使用。
	 * <p>在以下示例中，如果我们在 {@code TransactionalTests} 类<em>上</em>
	 * 搜索 {@code @Transactional} 注解，则
	 * {@code AnnotationDescriptor} 的属性如下。
	 * <ul>
	 * <li>rootDeclaringClass：{@code TransactionalTests} 类对象</li>
	 * <li>declaringClass：{@code TransactionalTests} 类对象</li>
	 * <li>annotation：{@code Transactional} 注解的实例</li>
	 * </ul>
	 * <p><pre style="code">
	 * &#064;Transactional
	 * &#064;ContextConfiguration({"/test-datasource.xml", "/repository-config.xml"})
	 * public class TransactionalTests { }
	 * </pre>
	 * <p>在以下示例中，如果我们在 {@code UserRepositoryTests} 类<em>上</em>
	 * 搜索 {@code @Transactional} 注解，则
	 * {@code AnnotationDescriptor} 的属性如下。
	 * <ul>
	 * <li>rootDeclaringClass：{@code UserRepositoryTests} 类对象</li>
	 * <li>declaringClass：{@code RepositoryTests} 类对象</li>
	 * <li>annotation：{@code Transactional} 注解的实例</li>
	 * </ul>
	 * <p><pre style="code">
	 * &#064;Transactional
	 * &#064;ContextConfiguration({"/test-datasource.xml", "/repository-config.xml"})
	 * &#064;Retention(RetentionPolicy.RUNTIME)
	 * public &#064;interface RepositoryTests { }
	 *
	 * &#064;RepositoryTests
	 * public class UserRepositoryTests { }
	 * </pre>
	 *
	 * @param <T> 注解类型
	 */
	public static class AnnotationDescriptor<T extends Annotation> {

		private final Class<?> rootDeclaringClass;

		private final Class<?> declaringClass;

		private final T annotation;

		AnnotationDescriptor(Class<?> rootDeclaringClass, T annotation) {
			this(rootDeclaringClass, rootDeclaringClass, annotation);
		}

		@SuppressWarnings("unchecked")
		AnnotationDescriptor(Class<?> rootDeclaringClass, Class<?> declaringClass, T annotation) {
			Assert.notNull(rootDeclaringClass, "'rootDeclaringClass' must not be null");
			Assert.notNull(declaringClass, "'declaringClass' must not be null");
			Assert.notNull(annotation, "Annotation must not be null");
			this.rootDeclaringClass = rootDeclaringClass;
			this.declaringClass = declaringClass;
			T mergedAnnotation = (T) AnnotatedElementUtils.findMergedAnnotation(
					rootDeclaringClass, annotation.annotationType());
			Assert.state(mergedAnnotation != null,
					() -> "Failed to find merged annotation for " + annotation);
			this.annotation = mergedAnnotation;
		}

		public Class<?> getRootDeclaringClass() {
			return this.rootDeclaringClass;
		}

		public Class<?> getDeclaringClass() {
			return this.declaringClass;
		}

		/**
		 * 获取此描述符的合并注解。
		 */
		public T getAnnotation() {
			return this.annotation;
		}

		@SuppressWarnings("unchecked")
		Class<T> getAnnotationType() {
			return (Class<T>) this.annotation.annotationType();
		}

		/**
		 * 在此描述符的 {@linkplain #getRootDeclaringClass()
		 * 根声明类}<em>之上</em>的层次结构中查找指定注解类型的下一个
		 * {@link AnnotationDescriptor}。
		 * <p>如果在根声明类的超类层次结构中找到
		 * 相应的注解，则返回该注解。否则，如果{@linkplain #searchEnclosingClass 适当}，
		 * 将尝试在
		 * 根声明类的 {@linkplain Class#getEnclosingClass() 封闭类}层次结构中
		 * 查找相应的注解。
		 * @return 如果找到该注解，则为下一个对应的注解描述符；
		 * 否则为 {@code null}
		 */
		@Nullable
		public AnnotationDescriptor<T> next() {
			// Declared on a superclass?
			AnnotationDescriptor<T> descriptor =
					findAnnotationDescriptor(getRootDeclaringClass().getSuperclass(), getAnnotationType());
			// Declared on an enclosing class of an inner class?
			if (descriptor == null && searchEnclosingClass(getRootDeclaringClass())) {
				descriptor = findAnnotationDescriptor(getRootDeclaringClass().getEnclosingClass(), getAnnotationType());
			}
			return descriptor;
		}

		/**
		 * 查找<em>存在</em>或<em>元存在</em>于此描述符的 {@linkplain #getRootDeclaringClass()
		 * 根声明类}上或根声明类实现的任何接口上的指定注解类型的
		 * <strong>所有</strong>注解。
		 * @return 找到的所有合并、合成的 {@code Annotation} 集合，
		 * 如果未找到则为空集合
		 */
		public Set<T> findAllLocalMergedAnnotations() {
			SearchStrategy searchStrategy = SearchStrategy.TYPE_HIERARCHY;
			return MergedAnnotations.from(getRootDeclaringClass(), searchStrategy, RepeatableContainers.none())
					.stream(getAnnotationType())
					.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
					.collect(MergedAnnotationCollectors.toAnnotationSet());
		}

		/**
		 * 提供此 {@code AnnotationDescriptor} 的文本表示形式。
		 */
		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("rootDeclaringClass", this.rootDeclaringClass.getName())
					.append("declaringClass", this.declaringClass.getName())
					.append("annotation", this.annotation)
					.toString();
		}
	}


	/**
	 * {@link AnnotationDescriptor} 的<em>无类型</em>扩展，用于
	 * 描述多个候选注解类型之一的声明，
	 * 其中实际注解类型无法预先确定。
	 */
	public static class UntypedAnnotationDescriptor extends AnnotationDescriptor<Annotation> {

		private final Class<? extends Annotation>[] annotationTypes;

		UntypedAnnotationDescriptor(Class<?> rootDeclaringClass, Annotation annotation,
				Class<? extends Annotation>[] annotationTypes) {

			this(rootDeclaringClass, rootDeclaringClass, annotation, annotationTypes);
		}

		UntypedAnnotationDescriptor(Class<?> rootDeclaringClass, Class<?> declaringClass,
				Annotation annotation, Class<? extends Annotation>[] annotationTypes) {

			super(rootDeclaringClass, declaringClass, annotation);
			this.annotationTypes = annotationTypes;
		}

		/**
		 * 在此描述符的
		 * {@linkplain #getRootDeclaringClass() 根声明类}<em>之上</em>的层次结构中查找指定
		 * 注解类型的下一个 {@link UntypedAnnotationDescriptor}。
		 * <p>如果在根声明类的超类层次结构中找到其中一个相应的注解，
		 * 则返回该注解。否则，如果{@linkplain #searchEnclosingClass 适当}，
		 * 将尝试在根声明类的
		 * {@linkplain Class#getEnclosingClass() 封闭类}层次结构中
		 * 查找相应的注解。
		 * @return 如果找到其中一个注解，
		 * 则为下一个对应的注解描述符；否则为 {@code null}
		 * @see AnnotationDescriptor#next()
		 */
		@Override
		@Nullable
		public UntypedAnnotationDescriptor next() {
			// Declared on a superclass?
			UntypedAnnotationDescriptor descriptor =
					findAnnotationDescriptorForTypes(getRootDeclaringClass().getSuperclass(), this.annotationTypes);
			// Declared on an enclosing class of an inner class?
			if (descriptor == null && searchEnclosingClass(getRootDeclaringClass())) {
				descriptor = findAnnotationDescriptorForTypes(getRootDeclaringClass().getEnclosingClass(), this.annotationTypes);
			}
			return descriptor;
		}

		/**
		 * 抛出 {@link UnsupportedOperationException}，因为 {@code UntypedAnnotationDescriptor}
		 * 所表示的注解类型未知。
		 */
		@Override
		public Set<Annotation> findAllLocalMergedAnnotations() {
			throw new UnsupportedOperationException(
					"findAllLocalMergedAnnotations() is unsupported in UntypedAnnotationDescriptor");
		}
	}

}
