/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.lang.Nullable;

/**
 * 用于数据绑定目的的 {@link org.springframework.expression.MethodResolver} 变体，通过反射访问给定
 * 目标对象上的实例方法。
 *
 * <p>该访问器不解析静态方法，也不解析 {@code java.lang.Object} 或 {@code java.lang.Class} 上的技术
 * 性方法。如需不受限制的解析，请改用 {@link ReflectiveMethodResolver}。
 *
 * @author Juergen Hoeller
 * @since 4.3.15
 * @see #forInstanceMethodInvocation()
 * @see DataBindingPropertyAccessor
 */
public final class DataBindingMethodResolver extends ReflectiveMethodResolver {

	private DataBindingMethodResolver() {
		super();
	}

	@Override
	@Nullable
	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException {

		if (targetObject instanceof Class) {
			throw new IllegalArgumentException("DataBindingMethodResolver does not support Class targets");
		}
		return super.resolve(context, targetObject, name, argumentTypes);
	}

	@Override
	protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
		if (Modifier.isStatic(method.getModifiers())) {
			return false;
		}
		Class<?> clazz = method.getDeclaringClass();
		return (clazz != Object.class && clazz != Class.class && !ClassLoader.class.isAssignableFrom(targetClass));
	}


	/**
	 * 创建一个新的数据绑定方法解析器，用于实例方法解析。
	 */
	public static DataBindingMethodResolver forInstanceMethodInvocation() {
		return new DataBindingMethodResolver();
	}

}
