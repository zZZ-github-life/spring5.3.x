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

package org.springframework.aop.support.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 简单的 {@link org.springframework.aop.MethodMatcher MethodMatcher}，用于查找方法上是否存在指定的
 * 注解（同时检查被调用接口上的方法（如果有）以及目标类上对应的方法）。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 * @see AnnotationMatchingPointcut
 */
public class AnnotationMethodMatcher extends StaticMethodMatcher {

	private final Class<? extends Annotation> annotationType;

	private final boolean checkInherited;


	/**
	 * 为给定的注解类型创建一个新的 AnnotationClassFilter。
	 * @param annotationType 要查找的注解类型
	 */
	public AnnotationMethodMatcher(Class<? extends Annotation> annotationType) {
		this(annotationType, false);
	}

	/**
	 * 为给定的注解类型创建一个新的 AnnotationClassFilter。
	 * @param annotationType 要查找的注解类型
	 * @param checkInherited 是否还检查父类、接口以及元注解中的该注解类型
	 * （即是否使用 {@link AnnotatedElementUtils#hasAnnotation} 语义
	 * 而非标准 Java 的 {@link Method#isAnnotationPresent}）
	 * @since 5.0
	 */
	public AnnotationMethodMatcher(Class<? extends Annotation> annotationType, boolean checkInherited) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		this.annotationType = annotationType;
		this.checkInherited = checkInherited;
	}



	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		if (matchesMethod(method)) {
			return true;
		}
		// Proxy classes never have annotations on their redeclared methods.
		if (Proxy.isProxyClass(targetClass)) {
			return false;
		}
		// The method may be on an interface, so let's check on the target class as well.
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
		return (specificMethod != method && matchesMethod(specificMethod));
	}

	private boolean matchesMethod(Method method) {
		return (this.checkInherited ? AnnotatedElementUtils.hasAnnotation(method, this.annotationType) :
				method.isAnnotationPresent(this.annotationType));
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationMethodMatcher)) {
			return false;
		}
		AnnotationMethodMatcher otherMm = (AnnotationMethodMatcher) other;
		return (this.annotationType.equals(otherMm.annotationType) && this.checkInherited == otherMm.checkInherited);
	}

	@Override
	public int hashCode() {
		return this.annotationType.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.annotationType;
	}

}
