/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

/**
 * 管理异步请求处理的核心类，主要作为 SPI 使用，通常不由应用类直接使用。
 *
 * <p>异步场景以在线程（T1）中照常处理请求开始。并发请求处理可以通过调用
 * {@link #startCallableProcessing(Callable, Object...) startCallableProcessing} 或
 * {@link #startDeferredResultProcessing(DeferredResult, Object...) startDeferredResultProcessing} 来启动，
 * 两者都会在单独的线程（T2）中产生结果。结果会被保存，并将请求分派到容器，
 * 以便在第三个线程（T3）中使用保存的结果恢复处理。在分派后的线程（T3）中，
 * 可以通过 {@link #getConcurrentResult()} 访问保存的结果，或通过
 * {@link #hasConcurrentResult()} 检测其是否存在。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 * @see org.springframework.web.context.request.AsyncWebRequestInterceptor
 * @see org.springframework.web.servlet.AsyncHandlerInterceptor
 * @see org.springframework.web.filter.OncePerRequestFilter#shouldNotFilterAsyncDispatch
 * @see org.springframework.web.filter.OncePerRequestFilter#isAsyncDispatch
 */
public final class WebAsyncManager {

	private static final Object RESULT_NONE = new Object();

	private static final AsyncTaskExecutor DEFAULT_TASK_EXECUTOR =
			new SimpleAsyncTaskExecutor(WebAsyncManager.class.getSimpleName());

	private static final Log logger = LogFactory.getLog(WebAsyncManager.class);

	private static final CallableProcessingInterceptor timeoutCallableInterceptor =
			new TimeoutCallableProcessingInterceptor();

	private static final DeferredResultProcessingInterceptor timeoutDeferredResultInterceptor =
			new TimeoutDeferredResultProcessingInterceptor();

	private static Boolean taskExecutorWarning = true;


	private AsyncWebRequest asyncWebRequest;

	private AsyncTaskExecutor taskExecutor = DEFAULT_TASK_EXECUTOR;

	private volatile Object concurrentResult = RESULT_NONE;

	private volatile Object[] concurrentResultContext;

	/*
	 * Whether the concurrentResult is an error. If such errors remain unhandled, some
	 * Servlet containers will call AsyncListener#onError at the end, after the ASYNC
	 * and/or the ERROR dispatch (Boot's case), and we need to ignore those.
	 */
	private volatile boolean errorHandlingInProgress;

	private final Map<Object, CallableProcessingInterceptor> callableInterceptors = new LinkedHashMap<>();

	private final Map<Object, DeferredResultProcessingInterceptor> deferredResultInterceptors = new LinkedHashMap<>();


	/**
	 * 包级私有的构造函数。
	 * @see WebAsyncUtils#getAsyncManager(javax.servlet.ServletRequest)
	 * @see WebAsyncUtils#getAsyncManager(org.springframework.web.context.request.WebRequest)
	 */
	WebAsyncManager() {
	}


	/**
	 * 配置要使用的 {@link AsyncWebRequest}。在单个请求期间可以多次设置此属性，
	 * 以准确反映请求的当前状态（例如在转发、请求/响应包装之后等）。不过，
	 * 不应在并发处理进行中（即 {@link #isConcurrentHandlingStarted()} 为
	 * {@code true} 时）设置此属性。
	 * @param asyncWebRequest the web request to use
	 */
	public void setAsyncWebRequest(AsyncWebRequest asyncWebRequest) {
		Assert.notNull(asyncWebRequest, "AsyncWebRequest must not be null");
		this.asyncWebRequest = asyncWebRequest;
		this.asyncWebRequest.addCompletionHandler(() -> asyncWebRequest.removeAttribute(
				WebAsyncUtils.WEB_ASYNC_MANAGER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
	}

	/**
	 * 配置 AsyncTaskExecutor，以便通过
	 * {@link #startCallableProcessing(Callable, Object...)} 用于并发处理。
	 * <p>默认情况下使用 {@link SimpleAsyncTaskExecutor} 实例。
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 当前请求所选的处理器是否选择异步处理该请求。返回 "true" 表示并发处理正在进行中，响应将保持打开。
	 * 返回 "false" 表示并发处理尚未开始，或者可能已经完成且请求已被分派以进一步处理并发结果。
	 */
	public boolean isConcurrentHandlingStarted() {
		return (this.asyncWebRequest != null && this.asyncWebRequest.isAsyncStarted());
	}

	/**
	 * 并发处理是否产生了结果值。
	 */
	public boolean hasConcurrentResult() {
		return (this.concurrentResult != RESULT_NONE);
	}

	/**
	 * 提供对并发处理结果的访问。
	 * @return an Object, possibly an {@code Exception} or {@code Throwable} if
	 * concurrent handling raised one.
	 * @see #clearConcurrentResult()
	 */
	public Object getConcurrentResult() {
		return this.concurrentResult;
	}

	/**
	 * 提供对在并发处理开始时保存的额外处理上下文的访问。
	 * @see #clearConcurrentResult()
	 */
	public Object[] getConcurrentResultContext() {
		return this.concurrentResultContext;
	}

	/**
	 * 获取在给定键下注册的 {@link CallableProcessingInterceptor}。
	 * @param key the key
	 * @return the interceptor registered under that key, or {@code null} if none
	 */
	@Nullable
	public CallableProcessingInterceptor getCallableInterceptor(Object key) {
		return this.callableInterceptors.get(key);
	}

	/**
	 * 获取在给定键下注册的 {@link DeferredResultProcessingInterceptor}。
	 * @param key the key
	 * @return the interceptor registered under that key, or {@code null} if none
	 */
	@Nullable
	public DeferredResultProcessingInterceptor getDeferredResultInterceptor(Object key) {
		return this.deferredResultInterceptors.get(key);
	}

	/**
	 * 在给定键下注册 {@link CallableProcessingInterceptor}。
	 * @param key the key
	 * @param interceptor the interceptor to register
	 */
	public void registerCallableInterceptor(Object key, CallableProcessingInterceptor interceptor) {
		Assert.notNull(key, "Key is required");
		Assert.notNull(interceptor, "CallableProcessingInterceptor is required");
		this.callableInterceptors.put(key, interceptor);
	}

	/**
	 * 注册不带键的 {@link CallableProcessingInterceptor}。键由类名和哈希码派生。
	 * @param interceptors one or more interceptors to register
	 */
	public void registerCallableInterceptors(CallableProcessingInterceptor... interceptors) {
		Assert.notNull(interceptors, "A CallableProcessingInterceptor is required");
		for (CallableProcessingInterceptor interceptor : interceptors) {
			String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
			this.callableInterceptors.put(key, interceptor);
		}
	}

	/**
	 * 在给定键下注册 {@link DeferredResultProcessingInterceptor}。
	 * @param key the key
	 * @param interceptor the interceptor to register
	 */
	public void registerDeferredResultInterceptor(Object key, DeferredResultProcessingInterceptor interceptor) {
		Assert.notNull(key, "Key is required");
		Assert.notNull(interceptor, "DeferredResultProcessingInterceptor is required");
		this.deferredResultInterceptors.put(key, interceptor);
	}

	/**
	 * 注册一个或多个未指定键的
	 * {@link DeferredResultProcessingInterceptor DeferredResultProcessingInterceptors}。默认键由拦截
	 * 器类名和哈希码派生。
	 * @param interceptors one or more interceptors to register
	 */
	public void registerDeferredResultInterceptors(DeferredResultProcessingInterceptor... interceptors) {
		Assert.notNull(interceptors, "A DeferredResultProcessingInterceptor is required");
		for (DeferredResultProcessingInterceptor interceptor : interceptors) {
			String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
			this.deferredResultInterceptors.put(key, interceptor);
		}
	}

	/**
	 * 清除 {@linkplain #getConcurrentResult() concurrentResult} 和
	 * {@linkplain #getConcurrentResultContext() concurrentResultContext}。
	 */
	public void clearConcurrentResult() {
		synchronized (WebAsyncManager.this) {
			this.concurrentResult = RESULT_NONE;
			this.concurrentResultContext = null;
		}
	}

	/**
	 * 启动并发请求处理，并使用 {@link #setTaskExecutor(AsyncTaskExecutor) AsyncTaskExecutor} 执行给
	 * 定任务。任务执行的结果会被保存，并分派请求以恢复对该结果的处理。如果任务抛出 Exception，则保存
	 * 的结果将是所抛出的 Exception。
	 * @param callable a unit of work to be executed asynchronously
	 * @param processingContext additional context to save that can be accessed
	 * via {@link #getConcurrentResultContext()}
	 * @throws Exception if concurrent processing failed to start
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void startCallableProcessing(Callable<?> callable, Object... processingContext) throws Exception {
		Assert.notNull(callable, "Callable must not be null");
		startCallableProcessing(new WebAsyncTask(callable), processingContext);
	}

	/**
	 * 在委托给 {@link #startCallableProcessing(Callable, Object...)} 之前，
	 * 使用给定的 {@link WebAsyncTask} 配置任务执行器以及
	 * {@code AsyncWebRequest} 的超时值。
	 * @param webAsyncTask a WebAsyncTask containing the target {@code Callable}
	 * @param processingContext additional context to save that can be accessed
	 * via {@link #getConcurrentResultContext()}
	 * @throws Exception if concurrent processing failed to start
	 */
	public void startCallableProcessing(final WebAsyncTask<?> webAsyncTask, Object... processingContext)
			throws Exception {

		Assert.notNull(webAsyncTask, "WebAsyncTask must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		Long timeout = webAsyncTask.getTimeout();
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		AsyncTaskExecutor executor = webAsyncTask.getExecutor();
		if (executor != null) {
			this.taskExecutor = executor;
		}
		else {
			logExecutorWarning();
		}

		List<CallableProcessingInterceptor> interceptors = new ArrayList<>();
		interceptors.add(webAsyncTask.getInterceptor());
		interceptors.addAll(this.callableInterceptors.values());
		interceptors.add(timeoutCallableInterceptor);

		final Callable<?> callable = webAsyncTask.getCallable();
		final CallableInterceptorChain interceptorChain = new CallableInterceptorChain(interceptors);

		this.asyncWebRequest.addTimeoutHandler(() -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Async request timeout for " + formatRequestUri());
			}
			Object result = interceptorChain.triggerAfterTimeout(this.asyncWebRequest, callable);
			if (result != CallableProcessingInterceptor.RESULT_NONE) {
				setConcurrentResultAndDispatch(result);
			}
		});

		this.asyncWebRequest.addErrorHandler(ex -> {
			if (!this.errorHandlingInProgress) {
				if (logger.isDebugEnabled()) {
					logger.debug("Async request error for " + formatRequestUri() + ": " + ex);
				}
				Object result = interceptorChain.triggerAfterError(this.asyncWebRequest, callable, ex);
				result = (result != CallableProcessingInterceptor.RESULT_NONE ? result : ex);
				setConcurrentResultAndDispatch(result);
			}
		});

		this.asyncWebRequest.addCompletionHandler(() ->
				interceptorChain.triggerAfterCompletion(this.asyncWebRequest, callable));

		interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, callable);
		startAsyncProcessing(processingContext);
		try {
			Future<?> future = this.taskExecutor.submit(() -> {
				Object result = null;
				try {
					interceptorChain.applyPreProcess(this.asyncWebRequest, callable);
					result = callable.call();
				}
				catch (Throwable ex) {
					result = ex;
				}
				finally {
					result = interceptorChain.applyPostProcess(this.asyncWebRequest, callable, result);
				}
				setConcurrentResultAndDispatch(result);
			});
			interceptorChain.setTaskFuture(future);
		}
		catch (RejectedExecutionException ex) {
			Object result = interceptorChain.applyPostProcess(this.asyncWebRequest, callable, ex);
			setConcurrentResultAndDispatch(result);
			throw ex;
		}
	}

	private void logExecutorWarning() {
		if (taskExecutorWarning && logger.isWarnEnabled()) {
			synchronized (DEFAULT_TASK_EXECUTOR) {
				AsyncTaskExecutor executor = this.taskExecutor;
				if (taskExecutorWarning &&
						(executor instanceof SimpleAsyncTaskExecutor || executor instanceof SyncTaskExecutor)) {
					String executorTypeName = executor.getClass().getSimpleName();
					logger.warn("\n!!!\n" +
							"An Executor is required to handle java.util.concurrent.Callable return values.\n" +
							"Please, configure a TaskExecutor in the MVC config under \"async support\".\n" +
							"The " + executorTypeName + " currently in use is not suitable under load.\n" +
							"-------------------------------\n" +
							"Request URI: '" + formatRequestUri() + "'\n" +
							"!!!");
					taskExecutorWarning = false;
				}
			}
		}
	}

	private String formatRequestUri() {
		HttpServletRequest request = this.asyncWebRequest.getNativeRequest(HttpServletRequest.class);
		return request != null ? request.getRequestURI() : "servlet container";
	}

	private void setConcurrentResultAndDispatch(Object result) {
		synchronized (WebAsyncManager.this) {
			if (this.concurrentResult != RESULT_NONE) {
				return;
			}
			this.concurrentResult = result;
			this.errorHandlingInProgress = (result instanceof Throwable);
		}

		if (this.asyncWebRequest.isAsyncComplete()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Async result set but request already complete: " + formatRequestUri());
			}
			return;
		}

		if (logger.isDebugEnabled()) {
			boolean isError = result instanceof Throwable;
			logger.debug("Async " + (isError ? "error" : "result set") + ", dispatch to " + formatRequestUri());
		}
		this.asyncWebRequest.dispatch();
	}

	/**
	 * 启动并发请求处理，并使用一个 {@link DeferredResultHandler} 初始化给定的 {@link DeferredResult}，
	 * 该处理器会保存结果并分派请求以恢复对该结果的处理。{@code AsyncWebRequest} 还会被更新：添加一个
	 * 使 {@code DeferredResult} 过期的完成处理器，以及一个超时处理器（假定 {@code DeferredResult} 具
	 * 有默认的超时结果）。
	 * @param deferredResult the DeferredResult instance to initialize
	 * @param processingContext additional context to save that can be accessed
	 * via {@link #getConcurrentResultContext()}
	 * @throws Exception if concurrent processing failed to start
	 * @see #getConcurrentResult()
	 * @see #getConcurrentResultContext()
	 */
	public void startDeferredResultProcessing(
			final DeferredResult<?> deferredResult, Object... processingContext) throws Exception {

		Assert.notNull(deferredResult, "DeferredResult must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		Long timeout = deferredResult.getTimeoutValue();
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		List<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
		interceptors.add(deferredResult.getInterceptor());
		interceptors.addAll(this.deferredResultInterceptors.values());
		interceptors.add(timeoutDeferredResultInterceptor);

		final DeferredResultInterceptorChain interceptorChain = new DeferredResultInterceptorChain(interceptors);

		this.asyncWebRequest.addTimeoutHandler(() -> {
			try {
				interceptorChain.triggerAfterTimeout(this.asyncWebRequest, deferredResult);
			}
			catch (Throwable ex) {
				setConcurrentResultAndDispatch(ex);
			}
		});

		this.asyncWebRequest.addErrorHandler(ex -> {
			if (!this.errorHandlingInProgress) {
				try {
					if (!interceptorChain.triggerAfterError(this.asyncWebRequest, deferredResult, ex)) {
						return;
					}
					deferredResult.setErrorResult(ex);
				}
				catch (Throwable interceptorEx) {
					setConcurrentResultAndDispatch(interceptorEx);
				}
			}
		});

		this.asyncWebRequest.addCompletionHandler(()
				-> interceptorChain.triggerAfterCompletion(this.asyncWebRequest, deferredResult));

		interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		startAsyncProcessing(processingContext);

		try {
			interceptorChain.applyPreProcess(this.asyncWebRequest, deferredResult);
			deferredResult.setResultHandler(result -> {
				result = interceptorChain.applyPostProcess(this.asyncWebRequest, deferredResult, result);
				setConcurrentResultAndDispatch(result);
			});
		}
		catch (Throwable ex) {
			setConcurrentResultAndDispatch(ex);
		}
	}

	private void startAsyncProcessing(Object[] processingContext) {
		synchronized (WebAsyncManager.this) {
			this.concurrentResult = RESULT_NONE;
			this.concurrentResultContext = processingContext;
			this.errorHandlingInProgress = false;
		}
		this.asyncWebRequest.startAsync();

		if (logger.isDebugEnabled()) {
			logger.debug("Started async request");
		}
	}

}
