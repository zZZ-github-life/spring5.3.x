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
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

/**
 * Bean 后处理器，通过向暴露的代理（现有 AOP 代理或新生成的、实现目标所有接口的代理）添加相应的
 * {@link AsyncAnnotationAdvisor}，自动为在类级别或方法级别携带 {@link Async} 注解的任何 Bean 应用
 * 异步调用行为。
 *
 * <p>可以提供负责异步执行的 {@link TaskExecutor}，也可以提供用于表示方法应异步调用的注解类型。如果
 * 未指定注解类型，该后处理器将同时检测 Spring 的 {@link Async @Async} 注解和 EJB 3.1 的
 * {@code javax.ejb.Asynchronous} 注解。
 *
 * <p>对于返回类型为 {@code void} 的方法，调用方无法获取异步方法调用期间抛出的任何异常。可以指定
 * {@link AsyncUncaughtExceptionHandler} 来处理这些情况。
 *
 * <p>注意：默认情况下，底层异步通知器会在现有通知器之前应用，以便在调用链中尽可能早地切换到异步执
 * 行。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.0
 * @see Async
 * @see AsyncAnnotationAdvisor
 * @see #setBeforeExistingAdvisors
 * @see ScheduledAnnotationBeanPostProcessor
 */
@SuppressWarnings("serial")
public class AsyncAnnotationBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

	/**
	 * 要获取的 {@link TaskExecutor} Bean 的默认名称："taskExecutor"。
	 * <p>请注意，初始查找是按类型进行的；此名称仅用于在上下文中找到多个执行器 Bean 时的后备方案。
	 * @since 4.2
	 * @see AnnotationAsyncExecutionInterceptor#DEFAULT_TASK_EXECUTOR_BEAN_NAME
	 */
	public static final String DEFAULT_TASK_EXECUTOR_BEAN_NAME =
			AnnotationAsyncExecutionInterceptor.DEFAULT_TASK_EXECUTOR_BEAN_NAME;


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Supplier<Executor> executor;

	@Nullable
	private Supplier<AsyncUncaughtExceptionHandler> exceptionHandler;

	@Nullable
	private Class<? extends Annotation> asyncAnnotationType;



	public AsyncAnnotationBeanPostProcessor() {
		setBeforeExistingAdvisors(true);
	}


	/**
	 * 使用给定的执行器和异常处理器提供者配置该后处理器；如果某个提供者无法解析，则应用相应的默认值。
	 * @since 5.1
	 */
	public void configure(
			@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		this.executor = executor;
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * 设置异步调用方法时要使用的 {@link Executor}。
	 * <p>如果未指定，将应用默认的执行器解析方式：在上下文中查找唯一的 {@link TaskExecutor} Bean，否则
	 * 查找名为 "taskExecutor" 的 {@link Executor} Bean。如果两者都无法解析，则会在拦截器内部创建一个本
	 * 地默认执行器。
	 * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
	 * @see #DEFAULT_TASK_EXECUTOR_BEAN_NAME
	 */
	public void setExecutor(Executor executor) {
		this.executor = SingletonSupplier.of(executor);
	}

	/**
	 * 设置用于处理异步方法执行所抛出的未捕获异常的 {@link AsyncUncaughtExceptionHandler}。
	 * @since 4.1
	 */
	public void setExceptionHandler(AsyncUncaughtExceptionHandler exceptionHandler) {
		this.exceptionHandler = SingletonSupplier.of(exceptionHandler);
	}

	/**
	 * 设置要在类级别或方法级别检测的 'async' 注解类型。默认情况下，将同时检测 {@link Async} 注解和 EJB
	 * 3.1 的 {@code javax.ejb.Asynchronous} 注解。
	 * <p>提供此 setter 属性是为了让开发者可以提供自己的（非 Spring 专有的）注解类型，用于表示某个方法
	 * （或给定类的所有方法）应异步调用。
	 * @param asyncAnnotationType the desired annotation type
	 */
	public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
		Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
		this.asyncAnnotationType = asyncAnnotationType;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);

		AsyncAnnotationAdvisor advisor = new AsyncAnnotationAdvisor(this.executor, this.exceptionHandler);
		if (this.asyncAnnotationType != null) {
			advisor.setAsyncAnnotationType(this.asyncAnnotationType);
		}
		advisor.setBeanFactory(beanFactory);
		this.advisor = advisor;
	}

}
