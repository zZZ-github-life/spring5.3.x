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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

/**
 * 帮助配置异步请求处理的相关选项。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class AsyncSupportConfigurer {

	@Nullable
	private AsyncTaskExecutor taskExecutor;

	@Nullable
	private Long timeout;

	private final List<CallableProcessingInterceptor> callableInterceptors = new ArrayList<>();

	private final List<DeferredResultProcessingInterceptor> deferredResultInterceptors = new ArrayList<>();


	/**
	 * 所提供的任务执行器用于：
	 * <ol>
	 * <li>处理 {@link Callable} 控制器方法的返回值。
	 * <li>在通过响应式（例如 Reactor、RxJava）控制器方法返回值向响应进行流式传输时
	 * 执行阻塞式写入。
	 * </ol>
	 * <p>默认仅使用 {@link SimpleAsyncTaskExecutor}。不过在使用上述两种场景时，
	 * 建议配置一个基于线程池的执行器，例如 {@link ThreadPoolTaskExecutor}。
	 * @param taskExecutor the task executor instance to use by default
	 */
	public AsyncSupportConfigurer setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}

	/**
	 * 指定异步请求处理超时之前的毫秒数。
	 * 在 Servlet 3 中，超时从主请求处理线程退出后开始，
	 * 到请求被重新分派以进一步处理并发产生的结果时结束。
	 * <p>如果未设置此值，则使用底层实现的默认超时时间。
	 * @param timeout the timeout value in milliseconds
	 */
	public AsyncSupportConfigurer setDefaultTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * 配置生命周期拦截器，在控制器返回
	 * {@link java.util.concurrent.Callable} 时启动的并发请求执行前后提供回调。
	 * @param interceptors the interceptors to register
	 */
	public AsyncSupportConfigurer registerCallableInterceptors(CallableProcessingInterceptor... interceptors) {
		this.callableInterceptors.addAll(Arrays.asList(interceptors));
		return this;
	}

	/**
	 * 配置生命周期拦截器，在控制器返回 {@link DeferredResult} 时
	 * 启动的并发请求执行前后提供回调。
	 * @param interceptors the interceptors to register
	 */
	public AsyncSupportConfigurer registerDeferredResultInterceptors(
			DeferredResultProcessingInterceptor... interceptors) {

		this.deferredResultInterceptors.addAll(Arrays.asList(interceptors));
		return this;
	}


	@Nullable
	protected AsyncTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	@Nullable
	protected Long getTimeout() {
		return this.timeout;
	}

	protected List<CallableProcessingInterceptor> getCallableInterceptors() {
		return this.callableInterceptors;
	}

	protected List<DeferredResultProcessingInterceptor> getDeferredResultInterceptors() {
		return this.deferredResultInterceptors;
	}

}
