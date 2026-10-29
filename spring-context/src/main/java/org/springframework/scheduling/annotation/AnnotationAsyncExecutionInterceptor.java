/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncExecutionInterceptor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

/**
 * {@link AsyncExecutionInterceptor} 的特化，根据 {@link Async} 注解将方法执行委托给
 * {@code Executor}。专门设计用于支持 Spring 3.1.2 引入的 {@link Async#value()} 执行器限定机制。支
 * 持在方法或声明类级别通过 {@code @Async} 检测限定符元数据。详情请参见
 * {@link #getExecutorQualifier(Method)}。
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1.2
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.AsyncAnnotationAdvisor
 */
public class AnnotationAsyncExecutionInterceptor extends AsyncExecutionInterceptor {

	/**
	 * 使用给定的执行器和一个简单的 {@link AsyncUncaughtExceptionHandler} 创建一个新的
	 * {@code AnnotationAsyncExecutionInterceptor}。
	 * @param defaultExecutor the executor to be used by default if no more specific
	 * executor has been qualified at the method level using {@link Async#value()};
	 * as of 4.2.6, a local executor for this interceptor will be built otherwise
	 */
	public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
		super(defaultExecutor);
	}

	/**
	 * 使用给定的执行器创建一个新的 {@code AnnotationAsyncExecutionInterceptor}。
	 * @param defaultExecutor the executor to be used by default if no more specific
	 * executor has been qualified at the method level using {@link Async#value()};
	 * as of 4.2.6, a local executor for this interceptor will be built otherwise
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
	 * handle exceptions thrown by asynchronous method executions with {@code void}
	 * return type
	 */
	public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		super(defaultExecutor, exceptionHandler);
	}


	/**
	 * 返回执行给定方法时要使用的执行器的限定符或 Bean 名称，它通过方法或声明类级别的
	 * {@link Async#value} 指定。如果在方法级别和类级别都指定了 {@code @Async}，则方法的 {@code #value}
	 * 优先（即使为空字符串，也表示应优先使用默认执行器）。
	 * @param method the method to inspect for executor qualifier metadata
	 * @return the qualifier if specified, otherwise empty string indicating that the
	 * {@linkplain #setExecutor(Executor) default executor} should be used
	 * @see #determineAsyncExecutor(Method)
	 */
	@Override
	@Nullable
	protected String getExecutorQualifier(Method method) {
		// Maintainer's note: changes made here should also be made in
		// AnnotationAsyncExecutionAspect#getExecutorQualifier
		Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
		if (async == null) {
			async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
		}
		return (async != null ? async.value() : null);
	}

}
