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

package org.springframework.test.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@code MetaAnnotationUtils} 是一组实用方法，补充 {@link AnnotationUtils} 中已有的标准支持。
 *
 * <p>虽然 {@code AnnotationUtils} 提供了<em>获取</em>或<em>查找</em>注解的工具，但 {@code MetaAnnotationUtils} 更进一步，提供了确定直接或通过<em>组合注解</em>间接声明注解的<em>根类</em>的支持。这些附加信息封装在 {@link AnnotationDescriptor} 中。
 *
 * <p>为了支持对 {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}、{@link org.springframework.test.context.TestExecutionListeners @TestExecutionListeners} 和 {@link org.springframework.test.context.ActiveProfiles @ActiveProfiles} 等注解进行类层次结构遍历，<em>Spring TestContext Framework</em> 需要 {@code AnnotationDescriptor} 提供的附加信息，这些注解支持合并和覆盖各种<em>继承</em>的注解属性（例如 {@link org.springframework.test.context.ContextConfiguration#inheritLocations}）。
 *
 * @author Sam Brannen
 * @since 4.0
 * @see AnnotationUtils
 * @see AnnotationDescriptor
 * @deprecated as of Spring Framework 5.3 in favor of
 * {@link org.springframework.test.context.TestContextAnnotationUtils}
 */
@Deprecated
public abstract class MetaAnnotationUtils {

	/**
	 * 为所提供的 {@link Class} 上的给定 {@code annotationType} 查找 {@link AnnotationDescriptor}；如果在给定类本身上找不到注解，则遍历其注解、接口和父类。
	 * <p>此方法显式处理未声明为{@linkplain java.lang.annotation.Inherited 继承的}类级注解<em>以及元注解</em>。
	 * <p>算法如下：
	 * <ol>
	 * <li>在给定类上搜索该注解，如果找到则返回对应的 {@code AnnotationDescriptor}。
	 * <li>递归搜索给定类声明的所有注解。
	 * <li>递归搜索给定类实现的所有接口。
	 * <li>递归搜索给定类的父类层次结构。
	 * </ol>
	 * <p>在此上下文中，<em>递归</em>一词意味着搜索过程会继续返回到步骤 #1，并将当前注解、接口或父类作为要查找注解的类。
	 * @param clazz the class to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * @return the corresponding annotation descriptor if the annotation was found;
	 * otherwise {@code null}
	 * @see #findAnnotationDescriptorForTypes(Class, Class...)
	 */
	@Nullable
	public static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(
			Class<?> clazz, Class<T> annotationType) {

		return findAnnotationDescriptor(clazz, new HashSet<>(), annotationType);
	}

	/**
	 * 执行 {@link #findAnnotationDescriptor(Class, Class)} 的搜索算法，通过跟踪已<em>访问</em>的注解来避免无限递归。
	 * @param clazz the class to look for annotations on
	 * @param visited the set of annotations that have already been visited
	 * @param annotationType the type of annotation to look for
	 * @return the corresponding annotation descriptor if the annotation was found;
	 * otherwise {@code null}
	 */
	@Nullable
	private static <T extends Annotation> AnnotationDescriptor<T> findAnnotationDescriptor(
			@Nullable Class<?> clazz, Set<Annotation> visited, Class<T> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null");
		if (clazz == null || Object.class == clazz) {
			return null;
		}

		// Declared locally?
		if (AnnotationUtils.isAnnotationDeclaredLocally(annotationType, clazz)) {
			return new AnnotationDescriptor<>(clazz, clazz.getAnnotation(annotationType));
		}

		// Declared on a composed annotation (i.e., as a meta-annotation)?
		for (Annotation composedAnn : clazz.getDeclaredAnnotations()) {
			Class<? extends Annotation> composedType = composedAnn.annotationType();
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(composedType.getName()) && visited.add(composedAnn)) {
				AnnotationDescriptor<T> descriptor = findAnnotationDescriptor(composedType, visited, annotationType);
				if (descriptor != null) {
					return new AnnotationDescriptor<>(
							clazz, descriptor.getDeclaringClass(), composedAnn, descriptor.getAnnotation());
				}
			}
		}

		// Declared on an interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			AnnotationDescriptor<T> descriptor = findAnnotationDescriptor(ifc, visited, annotationType);
			if (descriptor != null) {
				return new AnnotationDescriptor<>(clazz, descriptor.getDeclaringClass(),
						descriptor.getComposedAnnotation(), descriptor.getAnnotation());
			}
		}

		// Declared on a superclass?
		return findAnnotationDescriptor(clazz.getSuperclass(), visited, annotationType);
	}

	/**
	 * 在指定 {@code clazz} 的继承层次结构中（包括指定 {@code clazz} 本身）查找第一个声明了至少一个指定 {@code annotationTypes} 的 {@link Class} 的 {@link UntypedAnnotationDescriptor}。
	 * <p>如果在给定类本身上找不到注解，则此方法会遍历指定 {@code clazz} 的注解、接口和父类。
	 * <p>此方法显式处理未声明为{@linkplain java.lang.annotation.Inherited 继承的}类级注解<em>以及元注解</em>。
	 * <p>算法如下：
	 * <ol>
	 * <li>在给定类上搜索指定注解类型之一的本地声明，如果找到则返回对应的 {@code UntypedAnnotationDescriptor}。
	 * <li>递归搜索给定类声明的所有注解。
	 * <li>递归搜索给定类实现的所有接口。
	 * <li>递归搜索给定类的父类层次结构。
	 * </ol>
	 * <p>在此上下文中，<em>递归</em>一词意味着搜索过程会继续返回到步骤 #1，并将当前注解、接口或父类作为要查找注解的类。
	 * @param clazz the class to look for annotations on
	 * @param annotationTypes the types of annotations to look for
	 * @return the corresponding annotation descriptor if one of the annotations
	 * was found; otherwise {@code null}
	 * @see #findAnnotationDescriptor(Class, Class)
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static UntypedAnnotationDescriptor findAnnotationDescriptorForTypes(
			Class<?> clazz, Class<? extends Annotation>... annotationTypes) {

		return findAnnotationDescriptorForTypes(clazz, new HashSet<>(), annotationTypes);
	}

	/**
	 * 执行 {@link #findAnnotationDescriptorForTypes(Class, Class...)} 的搜索算法，通过跟踪已<em>访问</em>的注解来避免无限递归。
	 * @param clazz the class to look for annotations on
	 * @param visited the set of annotations that have already been visited
	 * @param annotationTypes the types of annotations to look for
	 * @return the corresponding annotation descriptor if one of the annotations
	 * was found; otherwise {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private static UntypedAnnotationDescriptor findAnnotationDescriptorForTypes(@Nullable Class<?> clazz,
			Set<Annotation> visited, Class<? extends Annotation>... annotationTypes) {

		assertNonEmptyAnnotationTypeArray(annotationTypes, "The list of annotation types must not be empty");
		if (clazz == null || Object.class == clazz) {
			return null;
		}

		// Declared locally?
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			if (AnnotationUtils.isAnnotationDeclaredLocally(annotationType, clazz)) {
				return new UntypedAnnotationDescriptor(clazz, clazz.getAnnotation(annotationType));
			}
		}

		// Declared on a composed annotation (i.e., as a meta-annotation)?
		for (Annotation composedAnnotation : clazz.getDeclaredAnnotations()) {
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(composedAnnotation) && visited.add(composedAnnotation)) {
				UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
						composedAnnotation.annotationType(), visited, annotationTypes);
				if (descriptor != null) {
					return new UntypedAnnotationDescriptor(clazz, descriptor.getDeclaringClass(),
							composedAnnotation, descriptor.getAnnotation());
				}
			}
		}

		// Declared on an interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(ifc, visited, annotationTypes);
			if (descriptor != null) {
				return new UntypedAnnotationDescriptor(clazz, descriptor.getDeclaringClass(),
						descriptor.getComposedAnnotation(), descriptor.getAnnotation());
			}
		}

		// Declared on a superclass?
		return findAnnotationDescriptorForTypes(clazz.getSuperclass(), visited, annotationTypes);
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
	 * 用于 {@link Annotation} 的描述符，包括<em>声明</em>该注解的{@linkplain #getDeclaringClass() 类}以及实际的{@linkplain #getAnnotation() 注解}实例。
	 * <p>如果该注解用作元注解，描述符还包含其上存在该注解的{@linkplain #getComposedAnnotation() 组合注解}。在这种情况下，<em>根声明类</em>并非直接带有该注解，而是通过组合注解间接带有该注解。
	 * <p>给定以下示例，如果我们在 {@code TransactionalTests} 类<em>上</em>搜索 {@code @Transactional} 注解，则 {@code AnnotationDescriptor} 的属性将如下所示：
	 * <ul>
	 * <li>rootDeclaringClass: {@code TransactionalTests} 类对象</li>
	 * <li>declaringClass: {@code TransactionalTests} 类对象</li>
	 * <li>composedAnnotation: {@code null}</li>
	 * <li>annotation: {@code Transactional} 注解的实例</li>
	 * </ul>
	 * <p><pre style="code">
	 * &#064;Transactional
	 * &#064;ContextConfiguration({"/test-datasource.xml", "/repository-config.xml"})
	 * public class TransactionalTests { }
	 * </pre>
	 * <p>给定以下示例，如果我们在 {@code UserRepositoryTests} 类<em>上</em>搜索 {@code @Transactional} 注解，则 {@code AnnotationDescriptor} 的属性将如下所示：
	 * <ul>
	 * <li>rootDeclaringClass: {@code UserRepositoryTests} 类对象</li>
	 * <li>declaringClass: {@code RepositoryTests} 类对象</li>
	 * <li>composedAnnotation: {@code RepositoryTests} 注解的实例</li>
	 * <li>annotation: {@code Transactional} 注解的实例</li>
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
	 * @param <T> the annotation type
	 */
	public static class AnnotationDescriptor<T extends Annotation> {

		private final Class<?> rootDeclaringClass;

		private final Class<?> declaringClass;

		@Nullable
		private final Annotation composedAnnotation;

		private final T annotation;

		private final AnnotationAttributes annotationAttributes;

		public AnnotationDescriptor(Class<?> rootDeclaringClass, T annotation) {
			this(rootDeclaringClass, rootDeclaringClass, null, annotation);
		}

		public AnnotationDescriptor(Class<?> rootDeclaringClass, Class<?> declaringClass,
				@Nullable Annotation composedAnnotation, T annotation) {

			Assert.notNull(rootDeclaringClass, "'rootDeclaringClass' must not be null");
			Assert.notNull(annotation, "Annotation must not be null");
			this.rootDeclaringClass = rootDeclaringClass;
			this.declaringClass = declaringClass;
			this.composedAnnotation = composedAnnotation;
			this.annotation = annotation;
			AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
					rootDeclaringClass, annotation.annotationType().getName(), false, false);
			Assert.state(attributes != null, "No annotation attributes");
			this.annotationAttributes = attributes;
		}

		public Class<?> getRootDeclaringClass() {
			return this.rootDeclaringClass;
		}

		public Class<?> getDeclaringClass() {
			return this.declaringClass;
		}

		public T getAnnotation() {
			return this.annotation;
		}

		/**
		 * 将此描述符中合并后的 {@link #getAnnotationAttributes AnnotationAttributes} 合成为目标{@linkplain #getAnnotationType 注解类型}的注解。
		 * @since 4.2
		 * @see #getAnnotationAttributes()
		 * @see #getAnnotationType()
		 * @see AnnotationUtils#synthesizeAnnotation(java.util.Map, Class, java.lang.reflect.AnnotatedElement)
		 */
		@SuppressWarnings("unchecked")
		public T synthesizeAnnotation() {
			return AnnotationUtils.synthesizeAnnotation(
					getAnnotationAttributes(), (Class<T>) getAnnotationType(), getRootDeclaringClass());
		}

		public Class<? extends Annotation> getAnnotationType() {
			return this.annotation.annotationType();
		}

		public AnnotationAttributes getAnnotationAttributes() {
			return this.annotationAttributes;
		}

		@Nullable
		public Annotation getComposedAnnotation() {
			return this.composedAnnotation;
		}

		@Nullable
		public Class<? extends Annotation> getComposedAnnotationType() {
			return (this.composedAnnotation != null ? this.composedAnnotation.annotationType() : null);
		}

		/**
		 * 提供此 {@code AnnotationDescriptor} 的文本表示。
		 */
		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("rootDeclaringClass", this.rootDeclaringClass)
					.append("declaringClass", this.declaringClass)
					.append("composedAnnotation", this.composedAnnotation)
					.append("annotation", this.annotation)
					.toString();
		}
	}


	/**
	 * {@link AnnotationDescriptor} 的<em>无类型</em>扩展，用于描述多个候选注解类型中某一个的声明，其中实际注解类型无法预先确定。
	 */
	public static class UntypedAnnotationDescriptor extends AnnotationDescriptor<Annotation> {

		public UntypedAnnotationDescriptor(Class<?> rootDeclaringClass, Annotation annotation) {
			this(rootDeclaringClass, rootDeclaringClass, null, annotation);
		}

		public UntypedAnnotationDescriptor(Class<?> rootDeclaringClass, Class<?> declaringClass,
				@Nullable Annotation composedAnnotation, Annotation annotation) {

			super(rootDeclaringClass, declaringClass, composedAnnotation, annotation);
		}

		/**
		 * 抛出 {@link UnsupportedOperationException}，因为 {@code UntypedAnnotationDescriptor} 中由 {@link #getAnnotationAttributes AnnotationAttributes} 表示的注解类型未知。
		 * @since 4.2
		 */
		@Override
		public Annotation synthesizeAnnotation() {
			throw new UnsupportedOperationException(
					"synthesizeAnnotation() is unsupported in UntypedAnnotationDescriptor");
		}
	}

}
