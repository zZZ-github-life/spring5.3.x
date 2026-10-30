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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * {@link AsyncWebRequest} 的 Servlet 3.0 实现。
 *
 * <p>参与异步请求的 Servlet 和所有过滤器都必须通过 Servlet API 启用异步支持，或者在
 * {@code web.xml} 的 Servlet 和过滤器声明中添加
 * <code>&lt;async-supported&gt;true&lt;/async-supported&gt;</code> 元素。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class StandardServletAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest, AsyncListener {

	private Long timeout;

	private AsyncContext asyncContext;

	private AtomicBoolean asyncCompleted = new AtomicBoolean();

	private final List<Runnable> timeoutHandlers = new ArrayList<>();

	private final List<Consumer<Throwable>> exceptionHandlers = new ArrayList<>();

	private final List<Runnable> completionHandlers = new ArrayList<>();


	/**
	 * 为给定的请求/响应对创建一个新实例。
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 */
	public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}


	/**
	 * 在 Servlet 3 异步处理中，超时时间从容器处理线程退出后开始计算。
	 */
	@Override
	public void setTimeout(Long timeout) {
		Assert.state(!isAsyncStarted(), "Cannot change the timeout with concurrent handling in progress");
		this.timeout = timeout;
	}

	@Override
	public void addTimeoutHandler(Runnable timeoutHandler) {
		this.timeoutHandlers.add(timeoutHandler);
	}

	@Override
	public void addErrorHandler(Consumer<Throwable> exceptionHandler) {
		this.exceptionHandlers.add(exceptionHandler);
	}

	@Override
	public void addCompletionHandler(Runnable runnable) {
		this.completionHandlers.add(runnable);
	}

	@Override
	public boolean isAsyncStarted() {
		return (this.asyncContext != null && getRequest().isAsyncStarted());
	}

	/**
	 * 异步请求处理是否已完成。
	 * <p>异步处理完成后务必避免继续使用请求和响应对象。Servlet 容器经常会重用它们。
	 */
	@Override
	public boolean isAsyncComplete() {
		return this.asyncCompleted.get();
	}

	@Override
	public void startAsync() {
		Assert.state(getRequest().isAsyncSupported(),
				"Async support must be enabled on a servlet and for all filters involved " +
				"in async request processing. This is done in Java code using the Servlet API " +
				"or by adding \"<async-supported>true</async-supported>\" to servlet and " +
				"filter declarations in web.xml.");
		Assert.state(!isAsyncComplete(), "Async processing has already completed");

		if (isAsyncStarted()) {
			return;
		}
		this.asyncContext = getRequest().startAsync(getRequest(), getResponse());
		this.asyncContext.addListener(this);
		if (this.timeout != null) {
			this.asyncContext.setTimeout(this.timeout);
		}
	}

	@Override
	public void dispatch() {
		Assert.notNull(this.asyncContext, "Cannot dispatch without an AsyncContext");
		this.asyncContext.dispatch();
	}


	// ---------------------------------------------------------------------
	// Implementation of AsyncListener methods
	// ---------------------------------------------------------------------

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		this.exceptionHandlers.forEach(consumer -> consumer.accept(event.getThrowable()));
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		this.timeoutHandlers.forEach(Runnable::run);
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		this.completionHandlers.forEach(Runnable::run);
		this.asyncContext = null;
		this.asyncCompleted.set(true);
	}

}
