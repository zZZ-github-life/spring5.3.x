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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 用于异步请求处理的控制器方法返回类型，
 * 可向响应写入一个或多个对象。
 *
 * <p>虽然 {@link org.springframework.web.context.request.async.DeferredResult}
 * 用于产生单个结果，但 {@code ResponseBodyEmitter} 可用于
 * 发送多个对象，其中每个对象都使用兼容的
 * {@link org.springframework.http.converter.HttpMessageConverter} 写出。
 *
 * <p>既可作为独立的返回类型，也可在
 * {@link org.springframework.http.ResponseEntity} 中使用。
 *
 * <pre>
 * &#064;RequestMapping(value="/stream", method=RequestMethod.GET)
 * public ResponseBodyEmitter handle() {
 * 	   ResponseBodyEmitter emitter = new ResponseBodyEmitter();
 * 	   // Pass the emitter to another component...
 * 	   return emitter;
 * }
 *
 * // in another thread
 * emitter.send(foo1);
 *
 * // and again
 * emitter.send(foo2);
 *
 * // and done
 * emitter.complete();
 * </pre>
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.2
 */
public class ResponseBodyEmitter {

	@Nullable
	private final Long timeout;

	@Nullable
	private Handler handler;

	/** 在处理器初始化之前保存要发送的数据。 */
	private final Set<DataWithMediaType> earlySendAttempts = new LinkedHashSet<>(8);

	/** 在处理器初始化之前保存成功完成状态。 */
	private boolean complete;

	/** 在处理器初始化之前保存错误。 */
	@Nullable
	private Throwable failure;

	/**
	 * 发生 I/O 错误后，我们不会直接调用 {@link #completeWithError}，
	 * 而是等待 Servlet 容器在容器线程上通过 {@code AsyncListener#onError}
	 * 回调我们，此时再调用 completeWithError。
	 * 此标志用于忽略可能来自 I/O 错误线程上应用程序 try-catch 块的、
	 * 对 complete 或 completeWithError 的进一步调用。
	 */
	private boolean sendFailed;

	private final DefaultCallback timeoutCallback = new DefaultCallback();

	private final ErrorCallback errorCallback = new ErrorCallback();

	private final DefaultCallback completionCallback = new DefaultCallback();


	/**
	 * 创建一个新的 ResponseBodyEmitter 实例。
	 */
	public ResponseBodyEmitter() {
		this.timeout = null;
	}

	/**
	 * 创建一个带有自定义超时值的 ResponseBodyEmitter。
	 * <p>默认不设置，此时将使用 MVC Java 配置或 MVC 命名空间
	 * 中配置的默认值；如果也未设置，则超时取决于
	 * 底层服务器的默认值。
	 * @param timeout 超时值，单位为毫秒
	 */
	public ResponseBodyEmitter(Long timeout) {
		this.timeout = timeout;
	}


	/**
	 * 返回已配置的超时值（如果有）。
	 */
	@Nullable
	public Long getTimeout() {
		return this.timeout;
	}


	synchronized void initialize(Handler handler) throws IOException {
		this.handler = handler;

		try {
			for (DataWithMediaType sendAttempt : this.earlySendAttempts) {
				sendInternal(sendAttempt.getData(), sendAttempt.getMediaType());
			}
		}
		finally {
			this.earlySendAttempts.clear();
		}

		if (this.complete) {
			if (this.failure != null) {
				this.handler.completeWithError(this.failure);
			}
			else {
				this.handler.complete();
			}
		}
		else {
			this.handler.onTimeout(this.timeoutCallback);
			this.handler.onError(this.errorCallback);
			this.handler.onCompletion(this.completionCallback);
		}
	}

	synchronized void initializeWithError(Throwable ex) {
		this.complete = true;
		this.failure = ex;
		this.earlySendAttempts.clear();
		this.errorCallback.accept(ex);
	}

	/**
	 * 如果 ResponseBodyEmitter 被包装在 ResponseEntity 中，
	 * 则在用状态码和响应头更新响应之后、但在响应提交之前，
	 * 即向响应体写入之前调用。
	 * <p>默认实现为空。
	 */
	protected void extendResponse(ServerHttpResponse outputMessage) {
	}

	/**
	 * 将给定对象写入响应。
	 * <p>如果发生任何异常，会向应用服务器发起一次分派，
	 * Spring MVC 将在那里通过其异常处理机制传递该异常。
	 * <p><strong>注意：</strong>如果发送因 IOException 失败，
	 * 你无需调用 {@link #completeWithError(Throwable)} 进行清理。
	 * 相反，Servlet 容器会创建一个通知，触发一次分派，
	 * Spring MVC 会在其中调用异常解析器并完成处理。
	 * @param object 要写入的对象
	 * @throws IOException 发生 I/O 错误时抛出
	 * @throws java.lang.IllegalStateException 包装任何其他错误
	 */
	public void send(Object object) throws IOException {
		send(object, null);
	}

	/**
	 * {@link #send(Object)} 的重载变体，还接受一个 MediaType
	 * 提示，用于说明如何序列化给定对象。
	 * @param object 要写入的对象
	 * @param mediaType 用于选择 HttpMessageConverter 的 MediaType 提示
	 * @throws IOException 发生 I/O 错误时抛出
	 * @throws java.lang.IllegalStateException 包装任何其他错误
	 */
	public synchronized void send(Object object, @Nullable MediaType mediaType) throws IOException {
		Assert.state(!this.complete, () -> "ResponseBodyEmitter has already completed" +
						(this.failure != null ? " with error: " + this.failure : ""));
		sendInternal(object, mediaType);
	}

	private void sendInternal(Object object, @Nullable MediaType mediaType) throws IOException {
		if (this.handler != null) {
			try {
				this.handler.send(object, mediaType);
			}
			catch (IOException ex) {
				this.sendFailed = true;
				throw ex;
			}
			catch (Throwable ex) {
				this.sendFailed = true;
				throw new IllegalStateException("Failed to send " + object, ex);
			}
		}
		else {
			this.earlySendAttempts.add(new DataWithMediaType(object, mediaType));
		}
	}

	/**
	 * 通过向 Servlet 容器执行一次分派来完成请求处理，
	 * Spring MVC 会在那里再次被调用，并完成
	 * 请求处理生命周期。
	 * <p><strong>注意：</strong>应用程序应调用此方法来
	 * 结束请求处理。在诸如 {@link #send(Object) sending}
	 * 期间发生错误之类的容器相关事件之后，不应再使用此方法。
	 */
	public synchronized void complete() {
		// Ignore, after send failure
		if (this.sendFailed) {
			return;
		}
		this.complete = true;
		if (this.handler != null) {
			this.handler.complete();
		}
	}

	/**
	 * 以错误结束请求处理。
	 * <p>会向应用服务器发起一次分派，Spring MVC 将在那里通过其
	 * 异常处理机制传递该异常。但请注意，
	 * 在请求处理的这个阶段，响应已提交，
	 * 响应状态码无法再更改。
	 * <p><strong>注意：</strong>应用程序应调用此方法
	 * 以错误结束请求处理。在诸如
	 * {@link #send(Object) sending} 期间发生错误之类的
	 * 容器相关事件之后，不应再使用此方法。
	 */
	public synchronized void completeWithError(Throwable ex) {
		// Ignore, after send failure
		if (this.sendFailed) {
			return;
		}
		this.complete = true;
		this.failure = ex;
		if (this.handler != null) {
			this.handler.completeWithError(ex);
		}
	}

	/**
	 * 注册当异步请求超时时要调用的代码。此方法
	 * 会在异步请求超时时从容器线程中被调用。
	 */
	public synchronized void onTimeout(Runnable callback) {
		this.timeoutCallback.setDelegate(callback);
	}

	/**
	 * 注册异步请求处理期间发生错误时要调用的代码。
	 * 当处理异步请求期间发生错误时，此方法会从容器线程中被调用。
	 * @since 5.0
	 */
	public synchronized void onError(Consumer<Throwable> callback) {
		this.errorCallback.setDelegate(callback);
	}

	/**
	 * 注册异步请求完成时要调用的代码。此方法
	 * 会在异步请求因任何原因（包括超时和网络错误）完成时从容器线程中被调用。
	 * 此方法可用于检测 {@code ResponseBodyEmitter} 实例是否已不再可用。
	 */
	public synchronized void onCompletion(Runnable callback) {
		this.completionCallback.setDelegate(callback);
	}


	@Override
	public String toString() {
		return "ResponseBodyEmitter@" + ObjectUtils.getIdentityHexString(this);
	}


	/**
	 * 用于处理事件数据发送、事件发送完成，
	 * 以及注册在超时、错误和因任何原因（包括来自
	 * 容器侧）完成时要调用的回调的约定。
	 */
	interface Handler {

		void send(Object data, @Nullable MediaType mediaType) throws IOException;

		void complete();

		void completeWithError(Throwable failure);

		void onTimeout(Runnable callback);

		void onError(Consumer<Throwable> callback);

		void onCompletion(Runnable callback);
	}


	/**
	 * 待写入数据的简单持有者，同时带有用于
	 * 选择消息转换器的 MediaType 提示。
	 */
	public static class DataWithMediaType {

		private final Object data;

		@Nullable
		private final MediaType mediaType;

		public DataWithMediaType(Object data, @Nullable MediaType mediaType) {
			this.data = data;
			this.mediaType = mediaType;
		}

		public Object getData() {
			return this.data;
		}

		@Nullable
		public MediaType getMediaType() {
			return this.mediaType;
		}
	}


	private class DefaultCallback implements Runnable {

		@Nullable
		private Runnable delegate;

		public void setDelegate(Runnable delegate) {
			this.delegate = delegate;
		}

		@Override
		public void run() {
			ResponseBodyEmitter.this.complete = true;
			if (this.delegate != null) {
				this.delegate.run();
			}
		}
	}


	private class ErrorCallback implements Consumer<Throwable> {

		@Nullable
		private Consumer<Throwable> delegate;

		public void setDelegate(Consumer<Throwable> callback) {
			this.delegate = callback;
		}

		@Override
		public void accept(Throwable t) {
			ResponseBodyEmitter.this.complete = true;
			if (this.delegate != null) {
				this.delegate.accept(t);
			}
		}
	}

}
