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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 定义穷尽搜索与元数据关联的方法的算法，搜索范围包括接口和父类，同时处理参数化方法以及基于接口和基
 * 于类的代理所遇到的常见场景。
 *
 * <p>通常（但并非必然）用于查找带注解的处理方法。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 4.2.3
 */
public final class MethodIntrospector {

	private MethodIntrospector() {
	}


	/**
	 * 基于关联元数据的查找，在给定目标类型上选择方法。
	 *
	 * <p>调用方通过 {@link MetadataLookup} 参数定义感兴趣的方法，从而可以将关联的元数据收集到结果映射
	 * 中。
	 * @param targetType 要在其上搜索方法的目标类型
	 * @param metadataLookup 用于检查关注方法的 {@link MetadataLookup} 回调，如果匹配则返回要与给定方法关联的非 null 元数据，
	 * 不匹配则返回 {@code null}
	 * @return 与元数据关联的已选方法（按检索顺序），如果没有匹配则返回空 Map
	 */
	public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
		final Map<Method, T> methodMap = new LinkedHashMap<>();
		Set<Class<?>> handlerTypes = new LinkedHashSet<>();
		Class<?> specificHandlerType = null;

		if (!Proxy.isProxyClass(targetType)) {
			specificHandlerType = ClassUtils.getUserClass(targetType);
			handlerTypes.add(specificHandlerType);
		}
		handlerTypes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetType));

		for (Class<?> currentHandlerType : handlerTypes) {
			final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);

			ReflectionUtils.doWithMethods(currentHandlerType, method -> {
				Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
				T result = metadataLookup.inspect(specificMethod);
				if (result != null) {
					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
					if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {
						methodMap.put(specificMethod, result);
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}

		return methodMap;
	}

	/**
	 * 基于过滤器在给定目标类型上选择方法。
	 *
	 * <p>调用方通过 {@code MethodFilter} 参数定义感兴趣的方法。
	 * @param targetType 要在其上搜索方法的目标类型
	 * @param methodFilter 用于帮助识别关注的处理方法的 {@code MethodFilter}
	 * @return 选中的方法，如果没有匹配则返回空 Set
	 */
	public static Set<Method> selectMethods(Class<?> targetType, final ReflectionUtils.MethodFilter methodFilter) {
		return selectMethods(targetType,
				(MetadataLookup<Boolean>) method -> (methodFilter.matches(method) ? Boolean.TRUE : null)).keySet();
	}

	/**
	 * 在目标类型上选择一个可调用的方法：如果给定方法确实在目标类型上暴露，则为该方法本身；否则为目标类
	 * 型某个接口上的对应方法，或目标类型自身的方法。
	 *
	 * <p>优先匹配用户声明的接口，因为它们很可能包含与目标类上的方法相对应的相关元数据。
	 * @param method 要检查的方法
	 * @param targetType 要在其上搜索方法的目标类型（通常是基于接口的 JDK 代理）
	 * @return 目标类型上对应的可调用方法
	 * @throws IllegalStateException 如果给定方法无法在给定目标类型上调用（通常由于代理不匹配）
	 */
	public static Method selectInvocableMethod(Method method, Class<?> targetType) {
		if (method.getDeclaringClass().isAssignableFrom(targetType)) {
			return method;
		}
		try {
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (Class<?> ifc : targetType.getInterfaces()) {
				try {
					return ifc.getMethod(methodName, parameterTypes);
				}
				catch (NoSuchMethodException ex) {
					// Alright, not on this interface then...
				}
			}
			// A final desperate attempt on the proxy class itself...
			return targetType.getMethod(methodName, parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(String.format(
					"Need to invoke method '%s' declared on target class '%s', " +
					"but not found in any interface(s) of the exposed proxy type. " +
					"Either pull the method up to an interface or switch to CGLIB " +
					"proxies by enforcing proxy-target-class mode in your configuration.",
					method.getName(), method.getDeclaringClass().getSimpleName()));
		}
	}


	/**
	 * 用于对给定方法进行元数据查找的回调接口。
	 * @param <T> 返回的元数据类型
	 */
	@FunctionalInterface
	public interface MetadataLookup<T> {

		/**
		 * 对给定方法执行查找，并返回关联的元数据（如果有）。
		 * @param method 要检查的方法
		 * @return 如果匹配则为要与方法关联的非 null 元数据，不匹配则为 {@code null}
		 */
		@Nullable
		T inspect(Method method);
	}

}
