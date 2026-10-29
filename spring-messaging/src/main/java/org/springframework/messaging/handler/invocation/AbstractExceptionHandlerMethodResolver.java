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

package org.springframework.messaging.handler.invocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * 缓存异常处理方法映射，并提供查找应处理某个异常的方法的选项。
 * 如果匹配到多个方法，则使用 {@link ExceptionDepthComparator} 排序并返回最佳匹配。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class AbstractExceptionHandlerMethodResolver {

	private static final Method NO_MATCHING_EXCEPTION_HANDLER_METHOD;

	static {
		try {
			NO_MATCHING_EXCEPTION_HANDLER_METHOD =
					AbstractExceptionHandlerMethodResolver.class.getDeclaredMethod("noMatchingExceptionHandler");
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Expected method not found: " + ex);
		}
	}


	private final Map<Class<? extends Throwable>, Method> mappedMethods = new HashMap<>(16);

	private final Map<Class<? extends Throwable>, Method> exceptionLookupCache = new ConcurrentReferenceHashMap<>(16);


	/**
	 * 受保护的构造函数，接受异常到方法的映射。
	 */
	protected AbstractExceptionHandlerMethodResolver(Map<Class<? extends Throwable>, Method> mappedMethods) {
		Assert.notNull(mappedMethods, "Mapped Methods must not be null");
		this.mappedMethods.putAll(mappedMethods);
	}

	/**
	 * 提取此方法处理的异常。此实现会在方法签名中查找 Throwable 的子类。
	 * <p>该方法为静态方法，以确保从子类构造函数中安全使用。
	 */
	@SuppressWarnings("unchecked")
	protected static List<Class<? extends Throwable>> getExceptionsFromMethodSignature(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<>();
		for (Class<?> paramType : method.getParameterTypes()) {
			if (Throwable.class.isAssignableFrom(paramType)) {
				result.add((Class<? extends Throwable>) paramType);
			}
		}
		if (result.isEmpty()) {
			throw new IllegalStateException("No exception types mapped to " + method);
		}
		return result;
	}


	/**
	 * 所包含的类型是否有任何异常映射。
	 */
	public boolean hasExceptionMappings() {
		return !this.mappedMethods.isEmpty();
	}

	/**
	 * 查找用于处理给定异常的 {@link Method}。
	 * <p>如果找到多个匹配项，则使用 {@link ExceptionDepthComparator}。
	 * @param exception the exception
	 * @return a Method to handle the exception, or {@code null} if none found
	 */
	@Nullable
	public Method resolveMethod(Throwable exception) {
		Method method = resolveMethodByExceptionType(exception.getClass());
		if (method == null) {
			Throwable cause = exception.getCause();
			if (cause != null) {
				method = resolveMethodByExceptionType(cause.getClass());
			}
		}
		return method;
	}

	/**
	 * 查找用于处理给定异常类型的 {@link Method}。当 {@link Exception} 实例不可用时
	 * （例如对于工具），这可能很有用。
	 * <p>如果找到多个匹配项，则使用 {@link ExceptionDepthComparator}。
	 * @param exceptionType the exception type
	 * @return a Method to handle the exception, or {@code null} if none found
	 * @since 4.3.1
	 */
	@Nullable
	public Method resolveMethodByExceptionType(Class<? extends Throwable> exceptionType) {
		Method method = this.exceptionLookupCache.get(exceptionType);
		if (method == null) {
			method = getMappedMethod(exceptionType);
			this.exceptionLookupCache.put(exceptionType, method);
		}
		return (method != NO_MATCHING_EXCEPTION_HANDLER_METHOD ? method : null);
	}

	/**
	 * 返回映射到给定异常类型的 {@link Method}，
	 * 如果没有则返回 {@link #NO_MATCHING_EXCEPTION_HANDLER_METHOD}。
	 */
	@Nullable
	private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
		List<Class<? extends Throwable>> matches = new ArrayList<>();
		for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
			if (mappedException.isAssignableFrom(exceptionType)) {
				matches.add(mappedException);
			}
		}
		if (!matches.isEmpty()) {
			if (matches.size() > 1) {
				matches.sort(new ExceptionDepthComparator(exceptionType));
			}
			return this.mappedMethods.get(matches.get(0));
		}
		else {
			return NO_MATCHING_EXCEPTION_HANDLER_METHOD;
		}
	}

	/**
	 * 用于 {@link #NO_MATCHING_EXCEPTION_HANDLER_METHOD} 常量。
	 */
	@SuppressWarnings("unused")
	private void noMatchingExceptionHandler() {
	}

}
