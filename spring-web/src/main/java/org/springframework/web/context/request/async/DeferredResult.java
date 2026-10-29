/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@code DeferredResult} 为异步请求处理提供了使用 {@link Callable} 之外的另一种选择。
 * {@code Callable} 是代表应用程序并发执行的，而使用 {@code DeferredResult} 时，应用程序可以从其
 * 选择的线程中产生结果。
 *
 * <p>子类可以扩展此类，以便轻松地为 {@link DeferredResult} 关联额外的数据或行为。例如，可以扩展
 * 该类并添加一个针对用户的额外属性，以关联创建 {@link DeferredResult} 的用户。这样，之后无需使用
 * 数据结构进行映射即可轻松访问该用户。
 *
 * <p>为类关联额外行为的一个例子是：扩展该类以实现一个额外的接口。例如，可以实现
 * {@link Comparable}，以便在将 {@link DeferredResult} 添加到 {@link PriorityQueue} 时能够按正确
 * 顺序处理。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Rob Winch
 * @since 3.2
 * @param <T> the result type
 */
public class DeferredResult<T> {

	private static final Object RESULT_NONE = new Object();

	private static final Log logger = LogFactory.getLog(DeferredResult.class);


	@Nullable
	private final Long timeoutValue;

	private final Supplier<?> timeoutResult;

	private Runnable timeoutCallback;

	private Consumer<Throwable> errorCallback;

	private Runnable completionCallback;

	private DeferredResultHandler resultHandler;

	private volatile Object result = RESULT_NONE;

	private volatile boolean expired;


	/**
	 * 创建一个 DeferredResult。
	 */
	public DeferredResult() {
		this(null, () -> RESULT_NONE);
	}

	/**
	 * 创建一个带有自定义超时值的 DeferredResult。
	 * <p>默认不设置，此时将使用 MVC Java 配置或 MVC 命名空间中配置的默认值；如果也未设置，则超时取决
	 * 于底层服务器的默认值。
	 * @param timeoutValue timeout value in milliseconds
	 */
	public DeferredResult(Long timeoutValue) {
		this(timeoutValue, () -> RESULT_NONE);
	}

	/**
	 * 创建一个带有超时值和默认结果的 DeferredResult，在超时时使用该默认结果。
	 * @param timeoutValue timeout value in milliseconds (ignored if {@code null})
	 * @param timeoutResult the result to use
	 */
	public DeferredResult(@Nullable Long timeoutValue, Object timeoutResult) {
		this.timeoutValue = timeoutValue;
		this.timeoutResult = () -> timeoutResult;
	}

	/**
	 * {@link #DeferredResult(Long, Object)} 的变体，接受基于 {@link Supplier} 的动态回退值。
	 * @param timeoutValue timeout value in milliseconds (ignored if {@code null})
	 * @param timeoutResult the result supplier to use
	 * @since 5.1.1
	 */
	public DeferredResult(@Nullable Long timeoutValue, Supplier<?> timeoutResult) {
		this.timeoutValue = timeoutValue;
		this.timeoutResult = timeoutResult;
	}


	/**
	 * 如果此 DeferredResult 因先前已被设置或底层请求已过期而不再可用，则返回 {@code true}。
	 * <p>结果可能是通过调用 {@link #setResult(Object)} 或 {@link #setErrorResult(Object)} 设置的，也
	 * 可能是超时导致的结果（如果构造时提供了超时结果）。请求也可能因超时或网络错误而过期。
	 */
	public final boolean isSetOrExpired() {
		return (this.result != RESULT_NONE || this.expired);
	}

	/**
	 * 如果 DeferredResult 已被设置，则返回 {@code true}。
	 * @since 4.0
	 */
	public boolean hasResult() {
		return (this.result != RESULT_NONE);
	}

	/**
	 * 返回结果，如果结果未设置则返回 {@code null}。由于结果本身也可能为 {@code null}，建议在调用此方
	 * 法之前先使用 {@link #hasResult()} 检查是否存在结果。
	 * @since 4.0
	 */
	@Nullable
	public Object getResult() {
		Object resultToCheck = this.result;
		return (resultToCheck != RESULT_NONE ? resultToCheck : null);
	}

	/**
	 * 返回配置的超时值（以毫秒为单位）。
	 */
	@Nullable
	final Long getTimeoutValue() {
		return this.timeoutValue;
	}

	/**
	 * 注册在异步请求超时时调用的代码。
	 * <p>当异步请求在 {@code DeferredResult} 被填充之前超时时，会从容器线程调用此方法。它可以调用
	 * {@link DeferredResult#setResult setResult} 或
	 * {@link DeferredResult#setErrorResult setErrorResult} 来恢复处理。
	 */
	public void onTimeout(Runnable callback) {
		this.timeoutCallback = callback;
	}

	/**
	 * 注册在异步请求发生错误时调用的代码。
	 * <p>当异步请求在 {@code DeferredResult} 被填充之前处理出错时，会从容器线程调用此方法。它可以调
	 * 用 {@link DeferredResult#setResult setResult} 或
	 * {@link DeferredResult#setErrorResult setErrorResult} 来恢复处理。
	 * @since 5.0
	 */
	public void onError(Consumer<Throwable> callback) {
		this.errorCallback = callback;
	}

	/**
	 * 注册在异步请求完成时调用的代码。
	 * <p>当异步请求因任何原因（包括超时和网络错误）完成时，会从容器线程调用此方法。这对于检测
	 * {@code DeferredResult} 实例是否已不再可用很有用。
	 */
	public void onCompletion(Runnable callback) {
		this.completionCallback = callback;
	}

	/**
	 * 提供用于处理结果值的处理器。
	 * @param resultHandler the handler
	 * @see DeferredResultProcessingInterceptor
	 */
	public final void setResultHandler(DeferredResultHandler resultHandler) {
		Assert.notNull(resultHandler, "DeferredResultHandler is required");
		// Immediate expiration check outside of the result lock
		if (this.expired) {
			return;
		}
		Object resultToHandle;
		synchronized (this) {
			// Got the lock in the meantime: double-check expiration status
			if (this.expired) {
				return;
			}
			resultToHandle = this.result;
			if (resultToHandle == RESULT_NONE) {
				// No result yet: store handler for processing once it comes in
				this.resultHandler = resultHandler;
				return;
			}
		}
		// If we get here, we need to process an existing result object immediately.
		// The decision is made within the result lock; just the handle call outside
		// of it, avoiding any deadlock potential with Servlet container locks.
		try {
			resultHandler.handleResult(resultToHandle);
		}
		catch (Throwable ex) {
			logger.debug("Failed to process async result", ex);
		}
	}

	/**
	 * 设置 DeferredResult 的值并进行处理。
	 * @param result the value to set
	 * @return {@code true} if the result was set and passed on for handling;
	 * {@code false} if the result was already set or the async request expired
	 * @see #isSetOrExpired()
	 */
	public boolean setResult(T result) {
		return setResultInternal(result);
	}

	private boolean setResultInternal(Object result) {
		// Immediate expiration check outside of the result lock
		if (isSetOrExpired()) {
			return false;
		}
		DeferredResultHandler resultHandlerToUse;
		synchronized (this) {
			// Got the lock in the meantime: double-check expiration status
			if (isSetOrExpired()) {
				return false;
			}
			// At this point, we got a new result to process
			this.result = result;
			resultHandlerToUse = this.resultHandler;
			if (resultHandlerToUse == null) {
				// No result handler set yet -> let the setResultHandler implementation
				// pick up the result object and invoke the result handler for it.
				return true;
			}
			// Result handler available -> let's clear the stored reference since
			// we don't need it anymore.
			this.resultHandler = null;
		}
		// If we get here, we need to process an existing result object immediately.
		// The decision is made within the result lock; just the handle call outside
		// of it, avoiding any deadlock potential with Servlet container locks.
		resultHandlerToUse.handleResult(result);
		return true;
	}

	/**
	 * 为 {@link DeferredResult} 设置错误值并进行处理。该值可以是 {@link Exception} 或
	 * {@link Throwable}，此时会像处理器抛出该异常一样进行处理。
	 * @param result the error result value
	 * @return {@code true} if the result was set to the error value and passed on
	 * for handling; {@code false} if the result was already set or the async
	 * request expired
	 * @see #isSetOrExpired()
	 */
	public boolean setErrorResult(Object result) {
		return setResultInternal(result);
	}


	final DeferredResultProcessingInterceptor getInterceptor() {
		return new DeferredResultProcessingInterceptor() {
			@Override
			public <S> boolean handleTimeout(NativeWebRequest request, DeferredResult<S> deferredResult) {
				boolean continueProcessing = true;
				try {
					if (timeoutCallback != null) {
						timeoutCallback.run();
					}
				}
				finally {
					Object value = timeoutResult.get();
					if (value != RESULT_NONE) {
						continueProcessing = false;
						try {
							setResultInternal(value);
						}
						catch (Throwable ex) {
							logger.debug("Failed to handle timeout result", ex);
						}
					}
				}
				return continueProcessing;
			}
			@Override
			public <S> boolean handleError(NativeWebRequest request, DeferredResult<S> deferredResult, Throwable t) {
				try {
					if (errorCallback != null) {
						errorCallback.accept(t);
					}
				}
				finally {
					try {
						setResultInternal(t);
					}
					catch (Throwable ex) {
						logger.debug("Failed to handle error result", ex);
					}
				}
				return false;
			}
			@Override
			public <S> void afterCompletion(NativeWebRequest request, DeferredResult<S> deferredResult) {
				expired = true;
				if (completionCallback != null) {
					completionCallback.run();
				}
			}
		};
	}


	/**
	 * 在设置 DeferredResult 值时对其进行处理。
	 */
	@FunctionalInterface
	public interface DeferredResultHandler {

		void handleResult(Object result);
	}

}
