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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link LinkedHashMap} 子类，表示由 {@link AnnotationUtils}、{@link AnnotatedElementUtils} 以及
 * Spring 基于反射和 ASM 的 {@link org.springframework.core.type.AnnotationMetadata} 实现所读取的注
 * 解属性<em>键值</em>对。
 *
 * <p>提供“伪具体化”，以避免调用代码中出现喧杂的 Map 泛型，同时提供以类型安全方式查找注解属性的便捷
 * 方法。
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.1.1
 * @see AnnotationUtils#getAnnotationAttributes
 * @see AnnotatedElementUtils
 */
@SuppressWarnings("serial")
public class AnnotationAttributes extends LinkedHashMap<String, Object> {

	private static final String UNKNOWN = "unknown";

	@Nullable
	private final Class<? extends Annotation> annotationType;

	final String displayName;

	boolean validated = false;


	/**
	 * 创建一个新的空 {@link AnnotationAttributes} 实例。
	 */
	public AnnotationAttributes() {
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 使用给定的初始容量创建一个新的空 {@link AnnotationAttributes} 实例，以优化性能。
	 * @param initialCapacity initial size of the underlying map
	 */
	public AnnotationAttributes(int initialCapacity) {
		super(initialCapacity);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 创建一个新的 {@link AnnotationAttributes} 实例，包装所提供的 Map 及其所有<em>键值</em>对。
	 * @param map original source of annotation attribute <em>key-value</em> pairs
	 * @see #fromMap(Map)
	 */
	public AnnotationAttributes(Map<String, Object> map) {
		super(map);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 创建一个新的 {@link AnnotationAttributes} 实例，包装所提供的 Map 及其所有<em>键值</em>对。
	 * @param other original source of annotation attribute <em>key-value</em> pairs
	 * @see #fromMap(Map)
	 */
	public AnnotationAttributes(AnnotationAttributes other) {
		super(other);
		this.annotationType = other.annotationType;
		this.displayName = other.displayName;
		this.validated = other.validated;
	}

	/**
	 * 为指定的 {@code annotationType} 创建一个新的空 {@link AnnotationAttributes} 实例。
	 * @param annotationType the type of annotation represented by this
	 * {@code AnnotationAttributes} instance; never {@code null}
	 * @since 4.2
	 */
	public AnnotationAttributes(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
	}

	/**
	 * 为指定的 {@code annotationType} 创建一个可能已经过校验的新的空 {@link AnnotationAttributes} 实例。
	 *
	 * @param annotationType the type of annotation represented by this
	 * {@code AnnotationAttributes} instance; never {@code null}
	 * @param validated if the attributes are considered already validated
	 * @since 5.2
	 */
	AnnotationAttributes(Class<? extends Annotation> annotationType, boolean validated) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
		this.validated = validated;
	}

	/**
	 * 为指定的 {@code annotationType} 创建一个新的空 {@link AnnotationAttributes} 实例。
	 * @param annotationType the annotation type name represented by this
	 * {@code AnnotationAttributes} instance; never {@code null}
	 * @param classLoader the ClassLoader to try to load the annotation type on,
	 * or {@code null} to just store the annotation type name
	 * @since 4.3.2
	 */
	public AnnotationAttributes(String annotationType, @Nullable ClassLoader classLoader) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = getAnnotationType(annotationType, classLoader);
		this.displayName = annotationType;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static Class<? extends Annotation> getAnnotationType(String annotationType, @Nullable ClassLoader classLoader) {
		if (classLoader != null) {
			try {
				return (Class<? extends Annotation>) classLoader.loadClass(annotationType);
			}
			catch (ClassNotFoundException ex) {
				// Annotation Class not resolvable
			}
		}
		return null;
	}


	/**
	 * 获取此 {@code AnnotationAttributes} 所表示的注解类型。
	 * @return the annotation type, or {@code null} if unknown
	 * @since 4.2
	 */
	@Nullable
	public Class<? extends Annotation> annotationType() {
		return this.annotationType;
	}

	/**
	 * 以字符串形式获取存储在指定 {@code attributeName} 下的值。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public String getString(String attributeName) {
		return getRequiredAttribute(attributeName, String.class);
	}

	/**
	 * 以字符串数组形式获取存储在指定 {@code attributeName} 下的值。
	 * <p>如果存储在指定 {@code attributeName} 下的值是字符串，则在返回之前会将其包装为单元素数组。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public String[] getStringArray(String attributeName) {
		return getRequiredAttribute(attributeName, String[].class);
	}

	/**
	 * 以布尔值形式获取存储在指定 {@code attributeName} 下的值。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public boolean getBoolean(String attributeName) {
		return getRequiredAttribute(attributeName, Boolean.class);
	}

	/**
	 * 以数字形式获取存储在指定 {@code attributeName} 下的值。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	@SuppressWarnings("unchecked")
	public <N extends Number> N getNumber(String attributeName) {
		return (N) getRequiredAttribute(attributeName, Number.class);
	}

	/**
	 * 以枚举形式获取存储在指定 {@code attributeName} 下的值。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	@SuppressWarnings("unchecked")
	public <E extends Enum<?>> E getEnum(String attributeName) {
		return (E) getRequiredAttribute(attributeName, Enum.class);
	}

	/**
	 * 以类形式获取存储在指定 {@code attributeName} 下的值。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> getClass(String attributeName) {
		return getRequiredAttribute(attributeName, Class.class);
	}

	/**
	 * 以类数组形式获取存储在指定 {@code attributeName} 下的值。
	 * <p>如果存储在指定 {@code attributeName} 下的值是类，则在返回之前会将其包装为单元素数组。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public Class<?>[] getClassArray(String attributeName) {
		return getRequiredAttribute(attributeName, Class[].class);
	}

	/**
	 * 获取存储在指定 {@code attributeName} 下的 {@link AnnotationAttributes}。
	 * <p>注意：如果期望获取实际的注解，请改为调用 {@link #getAnnotation(String, Class)}。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the {@code AnnotationAttributes}
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public AnnotationAttributes getAnnotation(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes.class);
	}

	/**
	 * 获取存储在指定 {@code attributeName} 下的 {@code annotationType} 类型的注解。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @param annotationType the expected annotation type; never {@code null}
	 * @return the annotation
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 * @since 4.2
	 */
	public <A extends Annotation> A getAnnotation(String attributeName, Class<A> annotationType) {
		return getRequiredAttribute(attributeName, annotationType);
	}

	/**
	 * 获取存储在指定 {@code attributeName} 下的 {@link AnnotationAttributes} 数组。
	 * <p>如果存储在指定 {@code attributeName} 下的值是 {@code AnnotationAttributes} 的实例，则在返回之
	 * 前会将其包装为单元素数组。
	 * <p>注意：如果期望获取实际的注解数组，请改为调用 {@link #getAnnotationArray(String, Class)}。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the array of {@code AnnotationAttributes}
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	public AnnotationAttributes[] getAnnotationArray(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes[].class);
	}

	/**
	 * 获取存储在指定 {@code attributeName} 下的 {@code annotationType} 类型的数组。
	 * <p>如果存储在指定 {@code attributeName} 下的值是 {@code Annotation}，则在返回之前会将其包装为单
	 * 元素数组。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @param annotationType the expected annotation type; never {@code null}
	 * @return the annotation array
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
		Object array = Array.newInstance(annotationType, 0);
		return (A[]) getRequiredAttribute(attributeName, array.getClass());
	}

	/**
	 * 获取存储在指定 {@code attributeName} 下的值，并确保该值为 {@code expectedType}。
	 * <p>如果 {@code expectedType} 是数组，而存储在指定 {@code attributeName} 下的值是预期数组类型的组
	 * 件类型的单个元素，则在返回之前会将该单个元素包装为相应类型的单元素数组。
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @param expectedType the expected type; never {@code null}
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	@SuppressWarnings("unchecked")
	private <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
		Assert.hasText(attributeName, "'attributeName' must not be null or empty");
		Object value = get(attributeName);
		assertAttributePresence(attributeName, value);
		assertNotException(attributeName, value);
		if (!expectedType.isInstance(value) && expectedType.isArray() &&
				expectedType.getComponentType().isInstance(value)) {
			Object array = Array.newInstance(expectedType.getComponentType(), 1);
			Array.set(array, 0, value);
			value = array;
		}
		assertAttributeType(attributeName, value, expectedType);
		return (T) value;
	}

	private void assertAttributePresence(String attributeName, Object attributeValue) {
		Assert.notNull(attributeValue, () -> String.format(
				"Attribute '%s' not found in attributes for annotation [%s]",
				attributeName, this.displayName));
	}

	private void assertNotException(String attributeName, Object attributeValue) {
		if (attributeValue instanceof Throwable) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
					attributeName, this.displayName, attributeValue), (Throwable) attributeValue);
		}
	}

	private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
		if (!expectedType.isInstance(attributeValue)) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' is of type %s, but %s was expected in attributes for annotation [%s]",
					attributeName, attributeValue.getClass().getSimpleName(), expectedType.getSimpleName(),
					this.displayName));
		}
	}

	@Override
	public String toString() {
		Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
		StringBuilder sb = new StringBuilder("{");
		while (entries.hasNext()) {
			Map.Entry<String, Object> entry = entries.next();
			sb.append(entry.getKey());
			sb.append('=');
			sb.append(valueToString(entry.getValue()));
			if (entries.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append('}');
		return sb.toString();
	}

	private String valueToString(Object value) {
		if (value == this) {
			return "(this Map)";
		}
		if (value instanceof Object[]) {
			return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
		}
		return String.valueOf(value);
	}


	/**
	 * 根据给定 Map 返回一个 {@link AnnotationAttributes} 实例。
	 * <p>如果该 Map 已经是 {@code AnnotationAttributes} 实例，则会直接强制转换并返回，不会创建新实例。
	 * 否则，将把所提供的 Map 传递给 {@link #AnnotationAttributes(Map)} 构造函数来创建新实例。
	 * @param map original source of annotation attribute <em>key-value</em> pairs
	 */
	@Nullable
	public static AnnotationAttributes fromMap(@Nullable Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		if (map instanceof AnnotationAttributes) {
			return (AnnotationAttributes) map;
		}
		return new AnnotationAttributes(map);
	}

}
