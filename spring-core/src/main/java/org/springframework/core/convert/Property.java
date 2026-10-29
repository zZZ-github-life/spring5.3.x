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

package org.springframework.core.convert;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 对 JavaBeans Property 的描述，使我们能够避免依赖 {@code java.beans.PropertyDescriptor}。
 * {@code java.beans} 包在许多环境中不可用（例如 Android、Java ME），因此这对于 Spring 核心转换设施
 * 的便携性而言是理想的。
 *
 * <p>用于从属性位置构建 {@link TypeDescriptor}。构建出的 {@code TypeDescriptor} 随后可用于在该属性
 * 类型与其他类型之间进行转换。
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.1
 * @see TypeDescriptor#TypeDescriptor(Property)
 * @see TypeDescriptor#nested(Property, int)
 */
public final class Property {

	private static final Map<Property, Annotation[]> annotationCache = new ConcurrentReferenceHashMap<>();

	private final Class<?> objectType;

	@Nullable
	private final Method readMethod;

	@Nullable
	private final Method writeMethod;

	private final String name;

	private final MethodParameter methodParameter;

	@Nullable
	private Annotation[] annotations;


	public Property(Class<?> objectType, @Nullable Method readMethod, @Nullable Method writeMethod) {
		this(objectType, readMethod, writeMethod, null);
	}

	public Property(
			Class<?> objectType, @Nullable Method readMethod, @Nullable Method writeMethod, @Nullable String name) {

		this.objectType = objectType;
		this.readMethod = readMethod;
		this.writeMethod = writeMethod;
		this.methodParameter = resolveMethodParameter();
		this.name = (name != null ? name : resolveName());
	}


	/**
	 * 声明此属性的对象，该声明可能直接位于此对象中，也可能位于此对象所继承的父类中。
	 */
	public Class<?> getObjectType() {
		return this.objectType;
	}

	/**
	 * 属性名称：例如 'foo'
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 属性类型：例如 {@code java.lang.String}
	 */
	public Class<?> getType() {
		return this.methodParameter.getParameterType();
	}

	/**
	 * 属性的 getter 方法：例如 {@code getFoo()}
	 */
	@Nullable
	public Method getReadMethod() {
		return this.readMethod;
	}

	/**
	 * 属性的 setter 方法：例如 {@code setFoo(String)}
	 */
	@Nullable
	public Method getWriteMethod() {
		return this.writeMethod;
	}


	// Package private

	MethodParameter getMethodParameter() {
		return this.methodParameter;
	}

	Annotation[] getAnnotations() {
		if (this.annotations == null) {
			this.annotations = resolveAnnotations();
		}
		return this.annotations;
	}


	// Internal helpers

	private String resolveName() {
		if (this.readMethod != null) {
			int index = this.readMethod.getName().indexOf("get");
			if (index != -1) {
				index += 3;
			}
			else {
				index = this.readMethod.getName().indexOf("is");
				if (index != -1) {
					index += 2;
				}
				else {
					// Record-style plain accessor method, e.g. name()
					index = 0;
				}
			}
			return StringUtils.uncapitalize(this.readMethod.getName().substring(index));
		}
		else if (this.writeMethod != null) {
			int index = this.writeMethod.getName().indexOf("set");
			if (index == -1) {
				throw new IllegalArgumentException("Not a setter method");
			}
			index += 3;
			return StringUtils.uncapitalize(this.writeMethod.getName().substring(index));
		}
		else {
			throw new IllegalStateException("Property is neither readable nor writeable");
		}
	}

	private MethodParameter resolveMethodParameter() {
		MethodParameter read = resolveReadMethodParameter();
		MethodParameter write = resolveWriteMethodParameter();
		if (write == null) {
			if (read == null) {
				throw new IllegalStateException("Property is neither readable nor writeable");
			}
			return read;
		}
		if (read != null) {
			Class<?> readType = read.getParameterType();
			Class<?> writeType = write.getParameterType();
			if (!writeType.equals(readType) && writeType.isAssignableFrom(readType)) {
				return read;
			}
		}
		return write;
	}

	@Nullable
	private MethodParameter resolveReadMethodParameter() {
		if (getReadMethod() == null) {
			return null;
		}
		return new MethodParameter(getReadMethod(), -1).withContainingClass(getObjectType());
	}

	@Nullable
	private MethodParameter resolveWriteMethodParameter() {
		if (getWriteMethod() == null) {
			return null;
		}
		return new MethodParameter(getWriteMethod(), 0).withContainingClass(getObjectType());
	}

	private Annotation[] resolveAnnotations() {
		Annotation[] annotations = annotationCache.get(this);
		if (annotations == null) {
			Map<Class<? extends Annotation>, Annotation> annotationMap = new LinkedHashMap<>();
			addAnnotationsToMap(annotationMap, getReadMethod());
			addAnnotationsToMap(annotationMap, getWriteMethod());
			addAnnotationsToMap(annotationMap, getField());
			annotations = annotationMap.values().toArray(new Annotation[0]);
			annotationCache.put(this, annotations);
		}
		return annotations;
	}

	private void addAnnotationsToMap(
			Map<Class<? extends Annotation>, Annotation> annotationMap, @Nullable AnnotatedElement object) {

		if (object != null) {
			for (Annotation annotation : object.getAnnotations()) {
				annotationMap.put(annotation.annotationType(), annotation);
			}
		}
	}

	@Nullable
	private Field getField() {
		String name = getName();
		if (!StringUtils.hasLength(name)) {
			return null;
		}
		Field field = null;
		Class<?> declaringClass = declaringClass();
		if (declaringClass != null) {
			field = ReflectionUtils.findField(declaringClass, name);
			if (field == null) {
				// Same lenient fallback checking as in CachedIntrospectionResults...
				field = ReflectionUtils.findField(declaringClass, StringUtils.uncapitalize(name));
				if (field == null) {
					field = ReflectionUtils.findField(declaringClass, StringUtils.capitalize(name));
				}
			}
		}
		return field;
	}

	@Nullable
	private Class<?> declaringClass() {
		if (getReadMethod() != null) {
			return getReadMethod().getDeclaringClass();
		}
		else if (getWriteMethod() != null) {
			return getWriteMethod().getDeclaringClass();
		}
		else {
			return null;
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Property)) {
			return false;
		}
		Property otherProperty = (Property) other;
		return (ObjectUtils.nullSafeEquals(this.objectType, otherProperty.objectType) &&
				ObjectUtils.nullSafeEquals(this.name, otherProperty.name) &&
				ObjectUtils.nullSafeEquals(this.readMethod, otherProperty.readMethod) &&
				ObjectUtils.nullSafeEquals(this.writeMethod, otherProperty.writeMethod));
	}

	@Override
	public int hashCode() {
		return (ObjectUtils.nullSafeHashCode(this.objectType) * 31 + ObjectUtils.nullSafeHashCode(this.name));
	}

}
