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

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 表示对某个方法的覆盖，该方法在同一 IoC 上下文中查找对象，可以按 Bean 名称或按 Bean 类型（基于声
 * 明的方法返回类型）查找。
 *
 * <p>可用于 lookup 覆盖的方法可以声明参数，此时给定参数会传递给 Bean 获取操作。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.beans.factory.BeanFactory#getBean(String)
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
 * @see org.springframework.beans.factory.BeanFactory#getBean(String, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBeanProvider(ResolvableType)
 */
public class LookupOverride extends MethodOverride {

	@Nullable
	private final String beanName;

	@Nullable
	private Method method;


	/**
	 * 构造一个新的 LookupOverride。
	 * @param methodName the name of the method to override
	 * @param beanName the name of the bean in the current {@code BeanFactory} that the
	 * overridden method should return (may be {@code null} for type-based bean retrieval)
	 */
	public LookupOverride(String methodName, @Nullable String beanName) {
		super(methodName);
		this.beanName = beanName;
	}

	/**
	 * 构造一个新的 LookupOverride。
	 * @param method the method declaration to override
	 * @param beanName the name of the bean in the current {@code BeanFactory} that the
	 * overridden method should return (may be {@code null} for type-based bean retrieval)
	 */
	public LookupOverride(Method method, @Nullable String beanName) {
		super(method.getName());
		this.method = method;
		this.beanName = beanName;
	}


	/**
	 * 返回此方法应返回的 Bean 的名称。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 按 {@link Method} 引用或方法名匹配指定的方法。
	 * <p>出于向后兼容的考虑，在存在同名重载的非抽象方法的场景下，只有方法的无参变体才会被转换为由容器
	 * 驱动的 lookup 方法。
	 * <p>如果提供了 {@link Method}，则只考虑直接匹配，通常由 {@code @Lookup} 注解来界定。
	 */
	@Override
	public boolean matches(Method method) {
		if (this.method != null) {
			return method.equals(this.method);
		}
		else {
			return (method.getName().equals(getMethodName()) && (!isOverloaded() ||
					Modifier.isAbstract(method.getModifiers()) || method.getParameterCount() == 0));
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof LookupOverride) || !super.equals(other)) {
			return false;
		}
		LookupOverride that = (LookupOverride) other;
		return (ObjectUtils.nullSafeEquals(this.method, that.method) &&
				ObjectUtils.nullSafeEquals(this.beanName, that.beanName));
	}

	@Override
	public int hashCode() {
		return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.beanName));
	}

	@Override
	public String toString() {
		return "LookupOverride for method '" + getMethodName() + "'";
	}

}
