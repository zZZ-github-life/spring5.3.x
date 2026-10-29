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

package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 拦截并发请求处理，其中并发结果通过使用 {@link AsyncTaskExecutor}
 * 代表应用程序执行 {@link Callable} 来获得。
 *
 * <p>{@code CallableProcessingInterceptor} 会在异步线程中调用 {@code Callable} 任务之前和之后被
 * 调用，也会在容器线程发生超时/错误时被调用，或在因任何原因（包括超时或网络错误）完成之后被调用
 * 。
 *
 * <p>作为一般规则，拦截器方法抛出的异常会导致异步处理恢复：将请求分派回容器，并将该 Exception 实
 * 例作为并发结果。随后这些异常将通过 {@code HandlerExceptionResolver} 机制处理。
 *
 * <p>{@link #handleTimeout(NativeWebRequest, Callable) handleTimeout} 方法可以选择一个用于恢复处
 * 理的值。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
public interface CallableProcessingInterceptor {

	/**
	 * 表示此拦截器未确定结果的常量，从而给后续拦截器留出机会。
	 * @see #handleTimeout
	 * @see #handleError
	 */
	Object RESULT_NONE = new Object();

	/**
	 * 表示响应已由此拦截器处理（无结果），且不应再调用后续拦截器的常量。
	 * @see #handleTimeout
	 * @see #handleError
	 */
	Object RESPONSE_HANDLED = new Object();


	/**
	 * 在并发处理开始<em>之前</em>调用，所在线程为提交 {@code Callable} 以进行并发处理的原始线程。
	 * <p>这对于在调用 {@link Callable} 之前捕获当前线程的状态很有用。捕获状态后，可以在
	 * {@link #preProcess(NativeWebRequest, Callable)} 中将其转移到新的 {@link Thread}。捕获 Spring
	 * Security 的 SecurityContextHolder 状态并将其迁移到新线程就是一个具体用例。
	 * <p>默认实现为空。
	 * @param request the current request
	 * @param task the task for the current async request
	 * @throws Exception in case of errors
	 */
	default <T> void beforeConcurrentHandling(NativeWebRequest request, Callable<T> task) throws Exception {
	}

	/**
	 * 在并发处理开始<em>之后</em>、实际调用 {@code Callable} <em>之前</em>调用，所在线程为执行
	 * {@code Callable} 的异步线程。
	 * <p>默认实现为空。
	 * @param request the current request
	 * @param task the task for the current async request
	 * @throws Exception in case of errors
	 */
	default <T> void preProcess(NativeWebRequest request, Callable<T> task) throws Exception {
	}

	/**
	 * 在 {@code Callable} 于执行它的异步线程中产生结果<em>之后</em>调用。根据 {@code Callable} 完成
	 * 处理的时间，此方法可能在 {@code afterTimeout} 或 {@code afterCompletion} 之后被调用。
	 * <p>默认实现为空。
	 * @param request the current request
	 * @param task the task for the current async request
	 * @param concurrentResult the result of concurrent processing, which could
	 * be a {@link Throwable} if the {@code Callable} raised an exception
	 * @throws Exception in case of errors
	 */
	default <T> void postProcess(NativeWebRequest request, Callable<T> task,
			Object concurrentResult) throws Exception {
	}

	/**
	 * 当异步请求在 {@code Callable} 任务完成之前超时时，从容器线程调用。实现可以返回一个值（包括
	 * {@link Exception}），用以替代 {@link Callable} 未能及时返回的值。
	 * <p>默认实现始终返回 {@link #RESULT_NONE}。
	 * @param request the current request
	 * @param task the task for the current async request
	 * @return a concurrent result value; if the value is anything other than
	 * {@link #RESULT_NONE} or {@link #RESPONSE_HANDLED}, concurrent processing
	 * is resumed and subsequent interceptors are not invoked
	 * @throws Exception in case of errors
	 */
	default <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
		return RESULT_NONE;
	}

	/**
	 * 当异步请求在 {@code Callable} 任务完成之前处理出错时，从容器线程调用。实现可以返回一个值（包括
	 * {@link Exception}），用以替代 {@link Callable} 未能及时返回的值。
	 * <p>默认实现始终返回 {@link #RESULT_NONE}。
	 * @param request the current request
	 * @param task the task for the current async request
	 * @param t the error that occurred while request processing
	 * @return a concurrent result value; if the value is anything other than
	 * {@link #RESULT_NONE} or {@link #RESPONSE_HANDLED}, concurrent processing
	 * is resumed and subsequent interceptors are not invoked
	 * @throws Exception in case of errors
	 * @since 5.0
	 */
	default <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) throws Exception {
		return RESULT_NONE;
	}

	/**
	 * 当异步处理因任何原因（包括超时或网络错误）完成时，从容器线程调用。
	 * <p>默认实现为空。
	 * @param request the current request
	 * @param task the task for the current async request
	 * @throws Exception in case of errors
	 */
	default <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
	}

}
