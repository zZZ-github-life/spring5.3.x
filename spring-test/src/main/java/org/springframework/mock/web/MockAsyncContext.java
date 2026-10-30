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

package org.springframework.mock.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

/**
 * {@link AsyncContext} 接口的 mock 实现。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockAsyncContext implements AsyncContext {

	private final HttpServletRequest request;

	@Nullable
	private final HttpServletResponse response;

	private final List<AsyncListener> listeners = new ArrayList<>();

	@Nullable
	private String dispatchedPath;

	private long timeout = 10 * 1000L;

	private final List<Runnable> dispatchHandlers = new ArrayList<>();


	public MockAsyncContext(ServletRequest request, @Nullable ServletResponse response) {
		this.request = (HttpServletRequest) request;
		this.response = (HttpServletResponse) response;
	}


	public void addDispatchHandler(Runnable handler) {
		Assert.notNull(handler, "Dispatch handler must not be null");
		synchronized (this) {
			if (this.dispatchedPath == null) {
				this.dispatchHandlers.add(handler);
			}
			else {
				handler.run();
			}
		}
	}

	@Override
	public ServletRequest getRequest() {
		return this.request;
	}

	@Override
	@Nullable
	public ServletResponse getResponse() {
		return this.response;
	}

	@Override
	public boolean hasOriginalRequestAndResponse() {
		return (this.request instanceof MockHttpServletRequest && this.response instanceof MockHttpServletResponse);
	}

	@Override
	public void dispatch() {
		dispatch(this.request.getRequestURI());
	}

	@Override
	public void dispatch(String path) {
		dispatch(null, path);
	}

	@Override
	public void dispatch(@Nullable ServletContext context, String path) {
		synchronized (this) {
			this.dispatchedPath = path;
			this.dispatchHandlers.forEach(Runnable::run);
		}
	}

	@Nullable
	public String getDispatchedPath() {
		return this.dispatchedPath;
	}

	@Override
	public void complete() {
		MockHttpServletRequest mockRequest = WebUtils.getNativeRequest(this.request, MockHttpServletRequest.class);
		if (mockRequest != null) {
			mockRequest.setAsyncStarted(false);
		}
		for (AsyncListener listener : this.listeners) {
			try {
				listener.onComplete(new AsyncEvent(this, this.request, this.response));
			}
			catch (IOException ex) {
				throw new IllegalStateException("AsyncListener failure", ex);
			}
		}
	}

	@Override
	public void start(Runnable runnable) {
		runnable.run();
	}

	@Override
	public void addListener(AsyncListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void addListener(AsyncListener listener, ServletRequest request, ServletResponse response) {
		this.listeners.add(listener);
	}

	public List<AsyncListener> getListeners() {
		return this.listeners;
	}

	@Override
	public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
		return BeanUtils.instantiateClass(clazz);
	}

	/**
	 * 默认情况下，此值设置为 10000（10 秒），尽管 Servlet API
	 * 指定的默认异步请求超时为 30 秒。请记住，该超时
	 * 还可能受到通过 MVC Java 配置或 XML 命名空间进行的全局
	 * 配置的影响，并且可以在每个请求上通过
	 * {@link org.springframework.web.context.request.async.DeferredResult DeferredResult}
	 * 或
	 * {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter SseEmitter} 覆盖。
	 * @param timeout 要使用的超时值。
	 * @see AsyncContext#setTimeout(long)
	 */
	@Override
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public long getTimeout() {
		return this.timeout;
	}

}
