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

package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于持有 {@link Callable}、超时值和任务执行器的容器。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 * @param <V> the value type
 */
public class WebAsyncTask<V> implements BeanFactoryAware {

	private final Callable<V> callable;

	private Long timeout;

	private AsyncTaskExecutor executor;

	private String executorName;

	private BeanFactory beanFactory;

	private Callable<V> timeoutCallback;

	private Callable<V> errorCallback;

	private Runnable completionCallback;


	/**
	 * 创建一个包装给定 {@link Callable} 的 {@code WebAsyncTask}。
	 * @param callable the callable for concurrent handling
	 */
	public WebAsyncTask(Callable<V> callable) {
		Assert.notNull(callable, "Callable must not be null");
		this.callable = callable;
	}

	/**
	 * 创建一个带有超时值和 {@link Callable} 的 {@code WebAsyncTask}。
	 * @param timeout a timeout value in milliseconds
	 * @param callable the callable for concurrent handling
	 */
	public WebAsyncTask(long timeout, Callable<V> callable) {
		this(callable);
		this.timeout = timeout;
	}

	/**
	 * 创建一个带有超时值、执行器名称和 {@link Callable} 的 {@code WebAsyncTask}。
	 * @param timeout the timeout value in milliseconds; ignored if {@code null}
	 * @param executorName the name of an executor bean to use
	 * @param callable the callable for concurrent handling
	 */
	public WebAsyncTask(@Nullable Long timeout, String executorName, Callable<V> callable) {
		this(callable);
		Assert.notNull(executorName, "Executor name must not be null");
		this.executorName = executorName;
		this.timeout = timeout;
	}

	/**
	 * 创建一个带有超时值、执行器实例和 Callable 的 {@code WebAsyncTask}。
	 * @param timeout the timeout value in milliseconds; ignored if {@code null}
	 * @param executor the executor to use
	 * @param callable the callable for concurrent handling
	 */
	public WebAsyncTask(@Nullable Long timeout, AsyncTaskExecutor executor, Callable<V> callable) {
		this(callable);
		Assert.notNull(executor, "Executor must not be null");
		this.executor = executor;
		this.timeout = timeout;
	}


	/**
	 * 返回用于并发处理的 {@link Callable}（永不为 {@code null}）。
	 */
	public Callable<?> getCallable() {
		return this.callable;
	}

	/**
	 * 返回超时值（以毫秒为单位），如果未设置超时则返回 {@code null}。
	 */
	@Nullable
	public Long getTimeout() {
		return this.timeout;
	}

	/**
	 * 用于解析执行器名称的 {@link BeanFactory}。
	 * <p>当 {@code WebAsyncTask} 在 Spring MVC 控制器中使用时，会自动设置此工厂引用。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回用于并发处理的 AsyncTaskExecutor，如果未指定则返回 {@code null}。
	 */
	@Nullable
	public AsyncTaskExecutor getExecutor() {
		if (this.executor != null) {
			return this.executor;
		}
		else if (this.executorName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory is required to look up an executor bean by name");
			return this.beanFactory.getBean(this.executorName, AsyncTaskExecutor.class);
		}
		else {
			return null;
		}
	}


	/**
	 * 注册在异步请求超时时调用的代码。
	 * <p>当异步请求在 {@code Callable} 完成之前超时时，会从容器线程调用此方法。
	 * 回调在同一线程中执行，因此应无阻塞地返回。它可以返回一个替代值，
	 * 包括 {@link Exception}，或者返回
	 * {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}。
	 */
	public void onTimeout(Callable<V> callback) {
		this.timeoutCallback = callback;
	}

	/**
	 * 注册在异步请求处理期间发生错误时调用的代码。
	 * <p>当异步请求在 {@code Callable} 完成之前处理出错时，会从容器线程调用此方法。
	 * 回调在同一线程中执行，因此应无阻塞地返回。它可以返回一个替代值，
	 * 包括 {@link Exception}，或者返回
	 * {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}。
	 * @since 5.0
	 */
	public void onError(Callable<V> callback) {
		this.errorCallback = callback;
	}

	/**
	 * 注册在异步请求完成时调用的代码。
	 * <p>当异步请求因任何原因（包括超时和网络错误）完成时，会从容器线程调用此方法。
	 */
	public void onCompletion(Runnable callback) {
		this.completionCallback = callback;
	}

	CallableProcessingInterceptor getInterceptor() {
		return new CallableProcessingInterceptor() {
			@Override
			public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
				return (timeoutCallback != null ? timeoutCallback.call() : CallableProcessingInterceptor.RESULT_NONE);
			}
			@Override
			public <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) throws Exception {
				return (errorCallback != null ? errorCallback.call() : CallableProcessingInterceptor.RESULT_NONE);
			}
			@Override
			public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
				if (completionCallback != null) {
					completionCallback.run();
				}
			}
		};
	}

}
