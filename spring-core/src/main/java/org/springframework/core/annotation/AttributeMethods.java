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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * 提供一种快速访问 {@link Annotation} 属性方法的方式，具有一致的顺序以及一些有用的工具方法。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
final class AttributeMethods {

	static final AttributeMethods NONE = new AttributeMethods(null, new Method[0]);


	private static final Map<Class<? extends Annotation>, AttributeMethods> cache =
			new ConcurrentReferenceHashMap<>();

	private static final Comparator<Method> methodComparator = (m1, m2) -> {
		if (m1 != null && m2 != null) {
			return m1.getName().compareTo(m2.getName());
		}
		return m1 != null ? -1 : 1;
	};


	@Nullable
	private final Class<? extends Annotation> annotationType;

	private final Method[] attributeMethods;

	private final boolean[] canThrowTypeNotPresentException;

	private final boolean hasDefaultValueMethod;

	private final boolean hasNestedAnnotation;


	private AttributeMethods(@Nullable Class<? extends Annotation> annotationType, Method[] attributeMethods) {
		this.annotationType = annotationType;
		this.attributeMethods = attributeMethods;
		this.canThrowTypeNotPresentException = new boolean[attributeMethods.length];
		boolean foundDefaultValueMethod = false;
		boolean foundNestedAnnotation = false;
		for (int i = 0; i < attributeMethods.length; i++) {
			Method method = this.attributeMethods[i];
			Class<?> type = method.getReturnType();
			if (!foundDefaultValueMethod && (method.getDefaultValue() != null)) {
				foundDefaultValueMethod = true;
			}
			if (!foundNestedAnnotation && (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation()))) {
				foundNestedAnnotation = true;
			}
			ReflectionUtils.makeAccessible(method);
			this.canThrowTypeNotPresentException[i] = (type == Class.class || type == Class[].class || type.isEnum());
		}
		this.hasDefaultValueMethod = foundDefaultValueMethod;
		this.hasNestedAnnotation = foundNestedAnnotation;
	}


	/**
	 * 判断是否可以安全地访问给定注解中的值而不会引发任何
	 * {@link TypeNotPresentException TypeNotPresentExceptions}。
	 * @param annotation the annotation to check
	 * @return {@code true} if all values are present
	 * @see #validate(Annotation)
	 */
	boolean isValid(Annotation annotation) {
		assertAnnotation(annotation);
		for (int i = 0; i < size(); i++) {
			if (canThrowTypeNotPresentException(i)) {
				try {
					AnnotationUtils.invokeAnnotationMethod(get(i), annotation);
				}
				catch (Throwable ex) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 检查是否可以安全地访问给定注解中的值而不会引发任何
	 * {@link TypeNotPresentException TypeNotPresentExceptions}。特别是，此方法旨在涵盖 Google App
	 * Engine 针对 {@code Class} 值延迟抛出此类异常的情况（而不是更典型的早期
	 * {@code Class.getAnnotations() failure}）。
	 * @param annotation the annotation to validate
	 * @throws IllegalStateException if a declared {@code Class} attribute could not be read
	 * @see #isValid(Annotation)
	 */
	void validate(Annotation annotation) {
		assertAnnotation(annotation);
		for (int i = 0; i < size(); i++) {
			if (canThrowTypeNotPresentException(i)) {
				try {
					AnnotationUtils.invokeAnnotationMethod(get(i), annotation);
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Could not obtain annotation attribute value for " +
							get(i).getName() + " declared on " + annotation.annotationType(), ex);
				}
			}
		}
	}

	private void assertAnnotation(Annotation annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		if (this.annotationType != null) {
			Assert.isInstanceOf(this.annotationType, annotation);
		}
	}

	/**
	 * 获取具有指定名称的属性，如果不存在匹配的属性则返回 {@code null}。
	 * @param name the attribute name to find
	 * @return the attribute method or {@code null}
	 */
	@Nullable
	Method get(String name) {
		int index = indexOf(name);
		return index != -1 ? this.attributeMethods[index] : null;
	}

	/**
	 * 获取指定索引处的属性。
	 * @param index the index of the attribute to return
	 * @return the attribute method
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * ({@code index < 0 || index >= size()})
	 */
	Method get(int index) {
		return this.attributeMethods[index];
	}

	/**
	 * 判断指定索引处的属性在被访问时是否可能抛出 {@link TypeNotPresentException}。
	 * @param index the index of the attribute to check
	 * @return {@code true} if the attribute can throw a
	 * {@link TypeNotPresentException}
	 */
	boolean canThrowTypeNotPresentException(int index) {
		return this.canThrowTypeNotPresentException[index];
	}

	/**
	 * 获取具有指定名称的属性的索引，如果不存在该名称的属性则返回 {@code -1}。
	 * @param name the name to find
	 * @return the index of the attribute, or {@code -1}
	 */
	int indexOf(String name) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i].getName().equals(name)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 获取指定属性的索引，如果该属性不在此集合中则返回 {@code -1}。
	 * @param attribute the attribute to find
	 * @return the index of the attribute, or {@code -1}
	 */
	int indexOf(Method attribute) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i].equals(attribute)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 获取此集合中属性的数量。
	 * @return the number of attributes
	 */
	int size() {
		return this.attributeMethods.length;
	}

	/**
	 * 判断是否至少有一个属性方法具有默认值。
	 * @return {@code true} if there is at least one attribute method with a default value
	 */
	boolean hasDefaultValueMethod() {
		return this.hasDefaultValueMethod;
	}

	/**
	 * 判断是否至少有一个属性方法是嵌套注解。
	 * @return {@code true} if there is at least one attribute method with a nested
	 * annotation type
	 */
	boolean hasNestedAnnotation() {
		return this.hasNestedAnnotation;
	}


	/**
	 * 获取给定注解类型的属性方法。
	 * @param annotationType the annotation type
	 * @return the attribute methods for the annotation type
	 */
	static AttributeMethods forAnnotationType(@Nullable Class<? extends Annotation> annotationType) {
		if (annotationType == null) {
			return NONE;
		}
		return cache.computeIfAbsent(annotationType, new Function<Class<? extends Annotation>, AttributeMethods>() {
			@Override
			public AttributeMethods apply(Class<? extends Annotation> annotationType1) {
				return compute(annotationType1);
			}
		});
	}

	private static AttributeMethods compute(Class<? extends Annotation> annotationType) {
		Method[] methods = annotationType.getDeclaredMethods();
		int size = methods.length;
		for (int i = 0; i < methods.length; i++) {
			if (!isAttributeMethod(methods[i])) {
				methods[i] = null;
				size--;
			}
		}
		if (size == 0) {
			return NONE;
		}
		Arrays.sort(methods, methodComparator);
		Method[] attributeMethods = Arrays.copyOf(methods, size);
		return new AttributeMethods(annotationType, attributeMethods);
	}

	private static boolean isAttributeMethod(Method method) {
		return (method.getParameterCount() == 0 && method.getReturnType() != void.class);
	}

	/**
	 * 为给定属性方法创建适合在异常消息和日志中使用的描述。
	 * @param attribute the attribute to describe
	 * @return a description of the attribute
	 */
	static String describe(@Nullable Method attribute) {
		if (attribute == null) {
			return "(none)";
		}
		return describe(attribute.getDeclaringClass(), attribute.getName());
	}

	/**
	 * 为给定属性方法创建适合在异常消息和日志中使用的描述。
	 * @param annotationType the annotation type
	 * @param attributeName the attribute name
	 * @return a description of the attribute
	 */
	static String describe(@Nullable Class<?> annotationType, @Nullable String attributeName) {
		if (attributeName == null) {
			return "(none)";
		}
		String in = (annotationType != null ? " in annotation [" + annotationType.getName() + "]" : "");
		return "attribute '" + attributeName + "'" + in;
	}

}
