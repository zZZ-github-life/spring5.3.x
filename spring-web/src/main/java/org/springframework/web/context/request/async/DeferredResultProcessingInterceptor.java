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

import org.springframework.web.context.request.NativeWebRequest;

/**
 * 拦截并发请求处理，其中并发结果通过等待应用程序选择的线程（例如响应某个外部事件）设置
 * {@link DeferredResult} 来获得。
 *
 * <p>{@code DeferredResultProcessingInterceptor} 会在异步处理开始之前、{@code DeferredResult} 被
 * 设置之后、超时/出错时，或因任何原因（包括超时或网络错误）完成之后被调用。
 *
 * <p>作为一般规则，拦截器方法抛出的异常会导致异步处理恢复：将请求分派回容器，并将该 Exception 实
 * 例作为并发结果。随后这些异常将通过 {@code HandlerExceptionResolver} 机制处理。
 *
 * <p>{@link #handleTimeout(NativeWebRequest, DeferredResult) handleTimeout}
 * 方法可以设置 {@code DeferredResult} 以恢复处理。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
public interface DeferredResultProcessingInterceptor {

	/**
	 * 在并发处理开始之前立即调用，所在线程与启动并发处理的线程相同。此方法可用于在开始使用给定
	 * {@code DeferredResult} 进行并发处理之前捕获状态。
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request
	 * @throws Exception in case of errors
	 */
	default <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

	/**
	 * 在并发处理开始之后立即调用，所在线程与启动并发处理的线程相同。此方法可用于检测使用给定
	 * {@code DeferredResult} 的并发处理已经开始。
	 * <p>{@code DeferredResult} 可能已经被设置，例如在其创建时或由另一个线程设置。
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request
	 * @throws Exception in case of errors
	 */
	default <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

	/**
	 * 在 {@code DeferredResult} 通过
	 * {@link DeferredResult#setResult(Object)} 或
	 * {@link DeferredResult#setErrorResult(Object)} 被设置之后调用，
	 * 此时也已准备好处理并发结果。
	 * <p>如果 {@code DeferredResult} 是通过接受默认超时结果的构造函数创建的，
	 * 此方法也可能在超时之后被调用。
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request
	 * @param concurrentResult the result to which the {@code DeferredResult}
	 * @throws Exception in case of errors
	 */
	default <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult,
			Object concurrentResult) throws Exception {
	}

	/**
	 * 当异步请求在 {@code DeferredResult} 被设置之前超时时，从容器线程调用。
	 * 实现可以调用
	 * {@link DeferredResult#setResult(Object) setResult} 或
	 * {@link DeferredResult#setErrorResult(Object) setErrorResult} 来恢复处理。
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request; if the
	 * {@code DeferredResult} is set, then concurrent processing is resumed and
	 * subsequent interceptors are not invoked
	 * @return {@code true} if processing should continue, or {@code false} if
	 * other interceptors should not be invoked
	 * @throws Exception in case of errors
	 */
	default <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {

		return true;
	}

	/**
	 * 当异步请求在 {@code DeferredResult} 被设置之前处理出错时，从容器线程调用。
	 * 实现可以调用
	 * {@link DeferredResult#setResult(Object) setResult} 或
	 * {@link DeferredResult#setErrorResult(Object) setErrorResult} 来恢复处理。
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request; if the
	 * {@code DeferredResult} is set, then concurrent processing is resumed and
	 * subsequent interceptors are not invoked
	 * @param t the error that occurred while request processing
	 * @return {@code true} if error handling should continue, or {@code false} if
	 * other interceptors should be bypassed and not be invoked
	 * @throws Exception in case of errors
	 */
	default <T> boolean handleError(NativeWebRequest request, DeferredResult<T> deferredResult,
			Throwable t) throws Exception {

		return true;
	}

	/**
	 * 当异步请求因任何原因（包括超时和网络错误）完成时，从容器线程调用。
	 * 此方法可用于检测 {@code DeferredResult} 实例是否已不再可用。
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request
	 * @throws Exception in case of errors
	 */
	default <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

}
