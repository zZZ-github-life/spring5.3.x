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

package org.springframework.beans.factory.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 简单的 {@link BeanFactoryPostProcessor} 实现，用于向所属的 {@link ConfigurableBeanFactory} 注册
 * 自定义的 {@link Scope Scope(s)}。
 *
 * <p>会将所有提供的 {@link #setScopes(java.util.Map) scopes} 注册到传递给
 * {@link #postProcessBeanFactory(ConfigurableListableBeanFactory)} 方法的
 * {@link ConfigurableListableBeanFactory} 中。
 *
 * <p>此类允许<i>声明式</i>地注册自定义作用域。或者，可以考虑实现一个自定义的
 * {@link BeanFactoryPostProcessor}，以编程方式调用 {@link ConfigurableBeanFactory#registerScope}。
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 * @see ConfigurableBeanFactory#registerScope
 */
public class CustomScopeConfigurer implements BeanFactoryPostProcessor, BeanClassLoaderAware, Ordered {

	@Nullable
	private Map<String, Object> scopes;

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	/**
	 * 指定要注册的自定义作用域。
	 * <p>键表示作用域名称（String 类型）；每个值应为对应的自定义 {@link Scope} 实例或类名。
	 */
	public void setScopes(Map<String, Object> scopes) {
		this.scopes = scopes;
	}

	/**
	 * 将给定作用域添加到此配置器的作用域映射中。
	 * @param scopeName the name of the scope
	 * @param scope the scope implementation
	 * @since 4.1.1
	 */
	public void addScope(String scopeName, Scope scope) {
		if (this.scopes == null) {
			this.scopes = new LinkedHashMap<>(1);
		}
		this.scopes.put(scopeName, scope);
	}


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.scopes != null) {
			this.scopes.forEach((scopeKey, value) -> {
				if (value instanceof Scope) {
					beanFactory.registerScope(scopeKey, (Scope) value);
				}
				else if (value instanceof Class) {
					Class<?> scopeClass = (Class<?>) value;
					Assert.isAssignable(Scope.class, scopeClass, "Invalid scope class");
					beanFactory.registerScope(scopeKey, (Scope) BeanUtils.instantiateClass(scopeClass));
				}
				else if (value instanceof String) {
					Class<?> scopeClass = ClassUtils.resolveClassName((String) value, this.beanClassLoader);
					Assert.isAssignable(Scope.class, scopeClass, "Invalid scope class");
					beanFactory.registerScope(scopeKey, (Scope) BeanUtils.instantiateClass(scopeClass));
				}
				else {
					throw new IllegalArgumentException("Mapped value [" + value + "] for scope key [" +
							scopeKey + "] is not an instance of required type [" + Scope.class.getName() +
							"] or a corresponding Class or String value indicating a Scope implementation");
				}
			});
		}
	}

}
