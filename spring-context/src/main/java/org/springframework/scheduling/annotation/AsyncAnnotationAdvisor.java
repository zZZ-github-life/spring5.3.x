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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * 通过 {@link Async} 注解启用异步方法执行的通知器。该注解既可以用在实现类的方法和类型级别，也可以
 * 用在服务接口中。
 *
 * <p>该通知器还会检测 EJB 3.1 的 {@code javax.ejb.Asynchronous} 注解，并将其完全等同于 Spring 自己
 * 的 {@code Async} 处理。此外，还可以通过 {@link #setAsyncAnnotationType "asyncAnnotationType"} 属
 * 性指定自定义的异步注解类型。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see Async
 * @see AnnotationAsyncExecutionInterceptor
 */
@SuppressWarnings("serial")
public class AsyncAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	private Advice advice;

	private Pointcut pointcut;


	/**
	 * 为 Bean 风格的配置创建一个新的 {@code AsyncAnnotationAdvisor}。
	 */
	public AsyncAnnotationAdvisor() {
		this((Supplier<Executor>) null, (Supplier<AsyncUncaughtExceptionHandler>) null);
	}

	/**
	 * 为给定的任务执行器创建一个新的 {@code AsyncAnnotationAdvisor}。
	 * @param executor the task executor to use for asynchronous methods
	 * (can be {@code null} to trigger default executor resolution)
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
	 * handle unexpected exception thrown by asynchronous method executions
	 * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
	 */
	public AsyncAnnotationAdvisor(
			@Nullable Executor executor, @Nullable AsyncUncaughtExceptionHandler exceptionHandler) {

		this(SingletonSupplier.ofNullable(executor), SingletonSupplier.ofNullable(exceptionHandler));
	}

	/**
	 * 为给定的任务执行器创建一个新的 {@code AsyncAnnotationAdvisor}。
	 * @param executor the task executor to use for asynchronous methods
	 * (can be {@code null} to trigger default executor resolution)
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
	 * handle unexpected exception thrown by asynchronous method executions
	 * @since 5.1
	 * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
	 */
	@SuppressWarnings("unchecked")
	public AsyncAnnotationAdvisor(
			@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		Set<Class<? extends Annotation>> asyncAnnotationTypes = new LinkedHashSet<>(2);
		asyncAnnotationTypes.add(Async.class);
		try {
			asyncAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("javax.ejb.Asynchronous", AsyncAnnotationAdvisor.class.getClassLoader()));
		}
		catch (ClassNotFoundException ex) {
			// If EJB 3.1 API not present, simply ignore.
		}
		this.advice = buildAdvice(executor, exceptionHandler);
		this.pointcut = buildPointcut(asyncAnnotationTypes);
	}


	/**
	 * 设置 'async' 注解类型。
	 * <p>默认的异步注解类型是 {@link Async} 注解，以及 EJB 3.1 的 {@code javax.ejb.Asynchronous} 注解
	 * （如果存在）。
	 * <p>提供此 setter 属性是为了让开发者可以提供自己的（非 Spring 专有的）注解类型，用于表示某个方法
	 * 应异步执行。
	 * @param asyncAnnotationType the desired annotation type
	 */
	public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
		Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
		Set<Class<? extends Annotation>> asyncAnnotationTypes = new HashSet<>();
		asyncAnnotationTypes.add(asyncAnnotationType);
		this.pointcut = buildPointcut(asyncAnnotationTypes);
	}

	/**
	 * 设置按限定符查找执行器时要使用的 {@code BeanFactory}。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.advice instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
		}
	}


	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}


	protected Advice buildAdvice(
			@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		AnnotationAsyncExecutionInterceptor interceptor = new AnnotationAsyncExecutionInterceptor(null);
		interceptor.configure(executor, exceptionHandler);
		return interceptor;
	}

	/**
	 * 为给定的异步注解类型（如果有）计算切点。
	 * @param asyncAnnotationTypes the async annotation types to introspect
	 * @return the applicable Pointcut object, or {@code null} if none
	 */
	protected Pointcut buildPointcut(Set<Class<? extends Annotation>> asyncAnnotationTypes) {
		ComposablePointcut result = null;
		for (Class<? extends Annotation> asyncAnnotationType : asyncAnnotationTypes) {
			Pointcut cpc = new AnnotationMatchingPointcut(asyncAnnotationType, true);
			Pointcut mpc = new AnnotationMatchingPointcut(null, asyncAnnotationType, true);
			if (result == null) {
				result = new ComposablePointcut(cpc);
			}
			else {
				result.union(cpc);
			}
			result = result.union(mpc);
		}
		return (result != null ? result : Pointcut.TRUE);
	}

}
