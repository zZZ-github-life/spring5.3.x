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

package org.springframework.web.reactive.result.method;

import java.lang.annotation.Annotation;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.Assert;

/**
 * {@link HandlerMethodArgumentResolver} 实现的基类，可访问 {@code ReactiveAdapterRegistry}，并提供检查方法参数支持情况的方法。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class HandlerMethodArgumentResolverSupport implements HandlerMethodArgumentResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	private final ReactiveAdapterRegistry adapterRegistry;


	protected HandlerMethodArgumentResolverSupport(ReactiveAdapterRegistry adapterRegistry) {
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		this.adapterRegistry = adapterRegistry;
	}


	/**
	 * 返回配置的 {@link ReactiveAdapterRegistry}。
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}


	/**
	 * 在方法参数类型上或在响应式类型包装内的泛型类型上求值 {@code Predicate}。
	 */
	protected boolean checkParameterType(MethodParameter parameter, Predicate<Class<?>> predicate) {
		Class<?> type = parameter.getParameterType();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type);
		if (adapter != null) {
			assertHasValues(adapter, parameter);
			type = parameter.nested().getNestedParameterType();
		}
		return predicate.test(type);
	}

	private void assertHasValues(ReactiveAdapter adapter, MethodParameter param) {
		if (adapter.isNoValue()) {
			throw new IllegalArgumentException(
					"No value reactive types not supported: " + param.getGenericParameterType());
		}
	}

	/**
	 * 在方法参数类型上求值 {@code Predicate}，但如果它与响应式类型包装内的泛型类型匹配，则抛出 {@code IllegalStateException}。
	 */
	protected boolean checkParameterTypeNoReactiveWrapper(MethodParameter parameter, Predicate<Class<?>> predicate) {
		Class<?> type = parameter.getParameterType();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type);
		if (adapter != null) {
			assertHasValues(adapter, parameter);
			type = parameter.nested().getNestedParameterType();
		}
		if (predicate.test(type)) {
			if (adapter == null) {
				return true;
			}
			throw buildReactiveWrapperException(parameter);
		}
		return false;
	}

	private IllegalStateException buildReactiveWrapperException(MethodParameter parameter) {
		return new IllegalStateException(getClass().getSimpleName() +
				" does not support reactive type wrapper: " + parameter.getGenericParameterType());
	}

	/**
	 * 如果方法参数类型具有给定注解，则在其上求值 {@code Predicate}，必要时嵌套在 {@link java.util.Optional} 中，但如果它与响应式类型包装内的泛型类型匹配，则抛出 {@code IllegalStateException}。
	 */
	protected <A extends Annotation> boolean checkAnnotatedParamNoReactiveWrapper(
			MethodParameter parameter, Class<A> annotationType, BiPredicate<A, Class<?>> typePredicate) {

		A annotation = parameter.getParameterAnnotation(annotationType);
		if (annotation == null) {
			return false;
		}

		parameter = parameter.nestedIfOptional();
		Class<?> type = parameter.getNestedParameterType();

		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(type);
		if (adapter != null) {
			assertHasValues(adapter, parameter);
			parameter = parameter.nested();
			type = parameter.getNestedParameterType();
		}

		if (typePredicate.test(annotation, type)) {
			if (adapter == null) {
				return true;
			}
			throw buildReactiveWrapperException(parameter);
		}

		return false;
	}

}
