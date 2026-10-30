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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 在给定类（包括其所有父类）中发现
 * {@linkplain ExceptionHandler @ExceptionHandler} 方法，
 * 并帮助将给定的 {@link Exception} 解析为给定 {@link Method} 所支持的异常类型。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public class ExceptionHandlerMethodResolver {

	/**
	 * 用于选择 {@code @ExceptionHandler} 方法的过滤器。
	 */
	public static final MethodFilter EXCEPTION_HANDLER_METHODS = method ->
			AnnotatedElementUtils.hasAnnotation(method, ExceptionHandler.class);

	private static final Method NO_MATCHING_EXCEPTION_HANDLER_METHOD;

	static {
		try {
			NO_MATCHING_EXCEPTION_HANDLER_METHOD =
					ExceptionHandlerMethodResolver.class.getDeclaredMethod("noMatchingExceptionHandler");
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Expected method not found: " + ex);
		}
	}


	private final Map<Class<? extends Throwable>, Method> mappedMethods = new HashMap<>(16);

	private final Map<Class<? extends Throwable>, Method> exceptionLookupCache = new ConcurrentReferenceHashMap<>(16);


	/**
	 * 一个构造函数，用于在给定类型中查找 {@link ExceptionHandler} 方法。
	 * @param handlerType 要内省的类型
	 */
	public ExceptionHandlerMethodResolver(Class<?> handlerType) {
		for (Method method : MethodIntrospector.selectMethods(handlerType, EXCEPTION_HANDLER_METHODS)) {
			for (Class<? extends Throwable> exceptionType : detectExceptionMappings(method)) {
				addExceptionMapping(exceptionType, method);
			}
		}
	}


	/**
	 * 首先从 {@code @ExceptionHandler} 注解中提取异常映射，
	 * 然后作为回退方案从方法签名本身提取。
	 */
	@SuppressWarnings("unchecked")
	private List<Class<? extends Throwable>> detectExceptionMappings(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<>();
		detectAnnotationExceptionMappings(method, result);
		if (result.isEmpty()) {
			for (Class<?> paramType : method.getParameterTypes()) {
				if (Throwable.class.isAssignableFrom(paramType)) {
					result.add((Class<? extends Throwable>) paramType);
				}
			}
		}
		if (result.isEmpty()) {
			throw new IllegalStateException("No exception types mapped to " + method);
		}
		return result;
	}

	private void detectAnnotationExceptionMappings(Method method, List<Class<? extends Throwable>> result) {
		ExceptionHandler ann = AnnotatedElementUtils.findMergedAnnotation(method, ExceptionHandler.class);
		Assert.state(ann != null, "No ExceptionHandler annotation");
		result.addAll(Arrays.asList(ann.value()));
	}

	private void addExceptionMapping(Class<? extends Throwable> exceptionType, Method method) {
		Method oldMethod = this.mappedMethods.put(exceptionType, method);
		if (oldMethod != null && !oldMethod.equals(method)) {
			throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
					exceptionType + "]: {" + oldMethod + ", " + method + "}");
		}
	}

	/**
	 * 所包含的类型是否有任何异常映射。
	 */
	public boolean hasExceptionMappings() {
		return !this.mappedMethods.isEmpty();
	}

	/**
	 * 查找用于处理给定异常的 {@link Method}。
	 * <p>如果找到多个匹配项，则使用 {@link ExceptionDepthComparator} 进行选择。
	 * @param exception 异常
	 * @return 处理该异常的 Method，如果未找到则为 {@code null}
	 */
	@Nullable
	public Method resolveMethod(Exception exception) {
		return resolveMethodByThrowable(exception);
	}

	/**
	 * 查找用于处理给定 Throwable 的 {@link Method}。
	 * <p>如果找到多个匹配项，则使用 {@link ExceptionDepthComparator} 进行选择。
	 * @param exception 异常
	 * @return 处理该异常的 Method，如果未找到则为 {@code null}
	 * @since 5.0
	 */
	@Nullable
	public Method resolveMethodByThrowable(Throwable exception) {
		Method method = resolveMethodByExceptionType(exception.getClass());
		if (method == null) {
			Throwable cause = exception.getCause();
			if (cause != null) {
				method = resolveMethodByThrowable(cause);
			}
		}
		return method;
	}

	/**
	 * 查找用于处理给定异常类型的 {@link Method}。当 {@link Exception} 实例
	 * 不可用时（例如供工具使用），此方法会很有用。
	 * <p>如果找到多个匹配项，则使用 {@link ExceptionDepthComparator} 进行选择。
	 * @param exceptionType 异常类型
	 * @return 处理该异常的 Method，如果未找到则为 {@code null}
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
	 * 返回映射到给定异常类型的 {@link Method}，如果没有则返回
	 * {@link #NO_MATCHING_EXCEPTION_HANDLER_METHOD}。
	 */
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
