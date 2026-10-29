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

/**
 * 用于数据绑定目的的 {@link org.springframework.expression.PropertyAccessor} 变体，通过反射读取属
 * 性，也可能写入属性。
 *
 * <p>属性可以通过公共 getter 方法（读取时）或公共 setter 方法（写入时）引用，也可以作为公共字段引
 * 用。
 *
 * <p>该访问器专为用户声明的属性而设计，不解析 {@code java.lang.Object} 或 {@code java.lang.Class}
 * 上的技术性属性。如需不受限制的解析，请改用 {@link ReflectivePropertyAccessor}。
 *
 * @author Juergen Hoeller
 * @since 4.3.15
 * @see #forReadOnlyAccess()
 * @see #forReadWriteAccess()
 * @see SimpleEvaluationContext
 * @see StandardEvaluationContext
 * @see ReflectivePropertyAccessor
 */
public final class DataBindingPropertyAccessor extends ReflectivePropertyAccessor {

	/**
	 * 创建一个新的属性访问器，用于读取，也可能用于写入。
	 * @param allowWrite whether to also allow for write operations
	 * @see #canWrite
	 */
	private DataBindingPropertyAccessor(boolean allowWrite) {
		super(allowWrite);
	}

	@Override
	protected boolean isCandidateForProperty(Method method, Class<?> targetClass) {
		Class<?> clazz = method.getDeclaringClass();
		return (clazz != Object.class && clazz != Class.class && !ClassLoader.class.isAssignableFrom(targetClass));
	}


	/**
	 * 创建一个新的数据绑定属性访问器，用于只读操作。
	 */
	public static DataBindingPropertyAccessor forReadOnlyAccess() {
		return new DataBindingPropertyAccessor(false);
	}

	/**
	 * 创建一个新的数据绑定属性访问器，用于读写操作。
	 */
	public static DataBindingPropertyAccessor forReadWriteAccess() {
		return new DataBindingPropertyAccessor(true);
	}

}
