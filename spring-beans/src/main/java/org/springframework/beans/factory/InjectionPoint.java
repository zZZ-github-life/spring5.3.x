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

package org.springframework.beans.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 注入点的简单描述符，指向方法/构造器参数或字段。由 {@link UnsatisfiedDependencyException} 公开。
 * 也可作为工厂方法的参数使用，以便针对请求的注入点构建自定义的 Bean 实例。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see UnsatisfiedDependencyException#getInjectionPoint()
 * @see org.springframework.beans.factory.config.DependencyDescriptor
 */
public class InjectionPoint {

	@Nullable
	protected MethodParameter methodParameter;

	@Nullable
	protected Field field;

	@Nullable
	private volatile Annotation[] fieldAnnotations;


	/**
	 * 为方法或构造器参数创建注入点描述符。
	 * @param methodParameter the MethodParameter to wrap
	 */
	public InjectionPoint(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
	}

	/**
	 * 为字段创建注入点描述符。
	 * @param field the field to wrap
	 */
	public InjectionPoint(Field field) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
	}

	/**
	 * 复制构造器。
	 * @param original the original descriptor to create a copy from
	 */
	protected InjectionPoint(InjectionPoint original) {
		this.methodParameter = (original.methodParameter != null ?
				new MethodParameter(original.methodParameter) : null);
		this.field = original.field;
		this.fieldAnnotations = original.fieldAnnotations;
	}

	/**
	 * 仅供子类中的序列化使用。
	 */
	protected InjectionPoint() {
	}


	/**
	 * 返回所包装的 MethodParameter（如果有）。<p>注意：MethodParameter 或 Field 中只有一个是可用的。
	 * @return the MethodParameter, or {@code null} if none
	 */
	@Nullable
	public MethodParameter getMethodParameter() {
		return this.methodParameter;
	}

	/**
	 * 返回所包装的 Field（如果有）。<p>注意：MethodParameter 或 Field 中只有一个是可用的。
	 * @return the Field, or {@code null} if none
	 */
	@Nullable
	public Field getField() {
		return this.field;
	}

	/**
	 * 返回所包装的 MethodParameter，假定其存在。
	 * @return the MethodParameter (never {@code null})
	 * @throws IllegalStateException if no MethodParameter is available
	 * @since 5.0
	 */
	protected final MethodParameter obtainMethodParameter() {
		Assert.state(this.methodParameter != null, "Neither Field nor MethodParameter");
		return this.methodParameter;
	}

	/**
	 * 获取与所包装的字段或方法/构造器参数关联的注解。
	 */
	public Annotation[] getAnnotations() {
		if (this.field != null) {
			Annotation[] fieldAnnotations = this.fieldAnnotations;
			if (fieldAnnotations == null) {
				fieldAnnotations = this.field.getAnnotations();
				this.fieldAnnotations = fieldAnnotations;
			}
			return fieldAnnotations;
		}
		else {
			return obtainMethodParameter().getParameterAnnotations();
		}
	}

	/**
	 * 检索给定类型的字段/参数注解（如果有）。
	 * @param annotationType the annotation type to retrieve
	 * @return the annotation instance, or {@code null} if none found
	 * @since 4.3.9
	 */
	@Nullable
	public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
		return (this.field != null ? this.field.getAnnotation(annotationType) :
				obtainMethodParameter().getParameterAnnotation(annotationType));
	}

	/**
	 * 返回底层字段或方法/构造器参数所声明的类型，表示注入类型。
	 */
	public Class<?> getDeclaredType() {
		return (this.field != null ? this.field.getType() : obtainMethodParameter().getParameterType());
	}

	/**
	 * 返回所包装的成员，其中包含注入点。
	 * @return the Field / Method / Constructor as Member
	 */
	public Member getMember() {
		return (this.field != null ? this.field : obtainMethodParameter().getMember());
	}

	/**
	 * 返回所包装的注解元素。<p>注意：对于方法/构造器参数，这会公开声明在方法或构造器本身上的注解（即在
	 * 方法/构造器级别，而非参数级别）。在这种情况下，请使用 {@link #getAnnotations()} 获取参数级别的注
	 * 解，其与相应的字段注解一样是透明的。
	 * @return the Field / Method / Constructor as AnnotatedElement
	 */
	public AnnotatedElement getAnnotatedElement() {
		return (this.field != null ? this.field : obtainMethodParameter().getAnnotatedElement());
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		InjectionPoint otherPoint = (InjectionPoint) other;
		return (ObjectUtils.nullSafeEquals(this.field, otherPoint.field) &&
				ObjectUtils.nullSafeEquals(this.methodParameter, otherPoint.methodParameter));
	}

	@Override
	public int hashCode() {
		return (this.field != null ? this.field.hashCode() : ObjectUtils.nullSafeHashCode(this.methodParameter));
	}

	@Override
	public String toString() {
		return (this.field != null ? "field '" + this.field.getName() + "'" : String.valueOf(this.methodParameter));
	}

}
