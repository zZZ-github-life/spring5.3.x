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

package org.springframework.web.reactive.socket.adapter;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.AbstractListenerReadPublisher;
import org.springframework.http.server.reactive.AbstractListenerWriteProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * {@link WebSocketSession} 实现的基类，用于在事件监听式 WebSocket API
 * （例如 Java WebSocket API (JSR-356)、Jetty、Undertow）与 Reactive Streams
 * 之间进行桥接。
 *
 * <p>同时实现了 {@code Subscriber<Void>}，因此可用于订阅
 * {@link WebSocketHandler#handle(WebSocketSession)} 的完成。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 原生委托类型
 */
public abstract class AbstractListenerWebSocketSession<T> extends AbstractWebSocketSession<T>
		implements Subscriber<Void> {

	/**
	 * 如果底层 WebSocket API 不支持接收消息的流控制，则使用的"背压"缓冲区大小。
	 */
	private static final int RECEIVE_BUFFER_SIZE = 8192;


	@Nullable
	private final Sinks.Empty<Void> handlerCompletionSink;

	@Nullable
	@SuppressWarnings("deprecation")
	private final reactor.core.publisher.MonoProcessor<Void> handlerCompletionMono;

	private final WebSocketReceivePublisher receivePublisher;

	@Nullable
	private volatile WebSocketSendProcessor sendProcessor;

	private final AtomicBoolean sendCalled = new AtomicBoolean();

	private final Sinks.One<CloseStatus> closeStatusSink = Sinks.one();


	/**
	 * 基本构造函数。
	 * @param delegate 原生 WebSocket 会话、通道或连接
	 * @param id 会话 ID
	 * @param info 握手信息
	 * @param bufferFactory 当前连接的 DataBuffer 工厂
	 */
	public AbstractListenerWebSocketSession(
			T delegate, String id, HandshakeInfo info, DataBufferFactory bufferFactory) {

		this(delegate, id, info, bufferFactory, (Sinks.Empty<Void>) null);
	}

	/**
	 * 带有完成 sink 的备用构造函数，用于在会话处理完成（成功或出错）时发出信号。
	 * <p>主要供 {@code WebSocketClient} 使用，以便能够传达处理结束的通知。
	 */
	public AbstractListenerWebSocketSession(T delegate, String id, HandshakeInfo info,
			DataBufferFactory bufferFactory, @Nullable Sinks.Empty<Void> handlerCompletionSink) {

		super(delegate, id, info, bufferFactory);
		this.receivePublisher = new WebSocketReceivePublisher();
		this.handlerCompletionSink = handlerCompletionSink;
		this.handlerCompletionMono = null;
	}

	/**
	 * 带有完成 MonoProcessor 的备用构造函数，用于在会话处理完成（成功或出错）时
	 * 发出信号。
	 * <p>主要供 {@code WebSocketClient} 使用，以便能够传达处理结束的通知。
	 * @deprecated as of 5.3 in favor of
	 * {@link #AbstractListenerWebSocketSession(Object, String, HandshakeInfo, DataBufferFactory, Sinks.Empty)}
	 */
	@Deprecated
	public AbstractListenerWebSocketSession(T delegate, String id, HandshakeInfo info,
			DataBufferFactory bufferFactory, @Nullable reactor.core.publisher.MonoProcessor<Void> handlerCompletion) {

		super(delegate, id, info, bufferFactory);
		this.receivePublisher = new WebSocketReceivePublisher();
		this.handlerCompletionMono = handlerCompletion;
		this.handlerCompletionSink = null;
	}


	protected WebSocketSendProcessor getSendProcessor() {
		WebSocketSendProcessor sendProcessor = this.sendProcessor;
		Assert.state(sendProcessor != null, "No WebSocketSendProcessor available");
		return sendProcessor;
	}

	@Override
	public Flux<WebSocketMessage> receive() {
		return (canSuspendReceiving() ? Flux.from(this.receivePublisher) :
				Flux.from(this.receivePublisher).onBackpressureBuffer(RECEIVE_BUFFER_SIZE));
	}

	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		if (this.sendCalled.compareAndSet(false, true)) {
			WebSocketSendProcessor sendProcessor = new WebSocketSendProcessor();
			this.sendProcessor = sendProcessor;
			return Mono.from(subscriber -> {
					messages.subscribe(sendProcessor);
					sendProcessor.subscribe(subscriber);
			});
		}
		else {
			return Mono.error(new IllegalStateException("send() has already been called"));
		}
	}

	@Override
	public Mono<CloseStatus> closeStatus() {
		return this.closeStatusSink.asMono();
	}

	/**
	 * 底层 WebSocket API 是否具有流控制能力，能否暂停和恢复接收消息。
	 * <p><strong>注意：</strong>如果可以，建议子类以暂停模式启动，并等待接收到
	 * 需求后再继续。
	 */
	protected abstract boolean canSuspendReceiving();

	/**
	 * 暂停接收，直到已接收的消息被处理完毕，并且下游 Subscriber 产生更多需求。
	 * <p><strong>注意：</strong>如果底层 WebSocket API 不为接收消息提供流控制，
	 * 此方法应为空操作，且 {@link #canSuspendReceiving()} 应返回 {@code false}。
	 */
	protected abstract void suspendReceiving();

	/**
	 * 在下游 Subscriber 产生需求后恢复接收新消息。
	 * <p><strong>注意：</strong>如果底层 WebSocket API 不为接收消息提供流控制，
	 * 此方法应为空操作，且 {@link #canSuspendReceiving()} 应返回 {@code false}。
	 */
	protected abstract void resumeReceiving();

	/**
	 * 发送给定的 WebSocket 消息。
	 * <p><strong>注意：</strong>如果底层容器使用池化缓冲区，子类负责在完全写入后
	 * 释放负载数据缓冲区。
	 */
	protected abstract boolean sendMessage(WebSocketMessage message) throws IOException;


	// WebSocketHandler adapter delegate methods

	/** 处理来自 WebSocketHandler 适配器的消息回调。 */
	void handleMessage(Type type, WebSocketMessage message) {
		this.receivePublisher.handleMessage(message);
	}

	/** 处理来自 WebSocket 引擎的错误回调。 */
	void handleError(Throwable ex) {
		// Ignore result: can't overflow, ok if not first or no one listens
		this.closeStatusSink.tryEmitEmpty();
		this.receivePublisher.onError(ex);
		WebSocketSendProcessor sendProcessor = this.sendProcessor;
		if (sendProcessor != null) {
			sendProcessor.cancel();
			sendProcessor.onError(ex);
		}
	}

	/** 处理来自 WebSocket 引擎的关闭回调。 */
	void handleClose(CloseStatus closeStatus) {
		// Ignore result: can't overflow, ok if not first or no one listens
		this.closeStatusSink.tryEmitValue(closeStatus);
		this.receivePublisher.onAllDataRead();
		WebSocketSendProcessor sendProcessor = this.sendProcessor;
		if (sendProcessor != null) {
			sendProcessor.cancel();
			sendProcessor.onComplete();
		}
	}


	// Subscriber<Void> implementation tracking WebSocketHandler#handle completion

	@Override
	public void onSubscribe(Subscription subscription) {
		subscription.request(Long.MAX_VALUE);
	}

	@Override
	public void onNext(Void aVoid) {
		// no op
	}

	@Override
	public void onError(Throwable ex) {
		if (this.handlerCompletionSink != null) {
			// Ignore result: can't overflow, ok if not first or no one listens
			this.handlerCompletionSink.tryEmitError(ex);
		}
		if (this.handlerCompletionMono != null) {
			this.handlerCompletionMono.onError(ex);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("WebSocket session completed with error", ex);
		}
		else if (logger.isInfoEnabled()) {
			logger.info("WebSocket session completed with error: " + ex.getMessage());
		}
		close(CloseStatus.SERVER_ERROR);
	}

	@Override
	public void onComplete() {
		if (this.handlerCompletionSink != null) {
			// Ignore result: can't overflow, ok if not first or no one listens
			this.handlerCompletionSink.tryEmitEmpty();
		}
		if (this.handlerCompletionMono != null) {
			this.handlerCompletionMono.onComplete();
		}
		close();
	}


	private final class WebSocketReceivePublisher extends AbstractListenerReadPublisher<WebSocketMessage> {

		private volatile Queue<Object> pendingMessages = Queues.unbounded(Queues.SMALL_BUFFER_SIZE).get();


		WebSocketReceivePublisher() {
			super(AbstractListenerWebSocketSession.this.getLogPrefix());
		}


		@Override
		protected void checkOnDataAvailable() {
			resumeReceiving();
			int size = this.pendingMessages.size();
			if (rsReadLogger.isTraceEnabled()) {
				rsReadLogger.trace(getLogPrefix() + "checkOnDataAvailable (" + size + " pending)");
			}
			if (size > 0) {
				onDataAvailable();
			}
		}

		@Override
		protected void readingPaused() {
			suspendReceiving();
		}

		@Override
		@Nullable
		protected WebSocketMessage read() throws IOException {
			return (WebSocketMessage) this.pendingMessages.poll();
		}

		void handleMessage(WebSocketMessage message) {
			if (logger.isTraceEnabled()) {
				logger.trace(getLogPrefix() + "Received " + message);
			}
			else if (rsReadLogger.isTraceEnabled()) {
				rsReadLogger.trace(getLogPrefix() + "Received " + message);
			}
			if (!this.pendingMessages.offer(message)) {
				discardData();
				throw new IllegalStateException(
						"Too many messages. Please ensure WebSocketSession.receive() is subscribed to.");
			}
			onDataAvailable();
		}

		@Override
		protected void discardData() {
			while (true) {
				WebSocketMessage message = (WebSocketMessage) this.pendingMessages.poll();
				if (message == null) {
					return;
				}
				message.release();
			}
		}
	}


	/**
	 * 用于发送 WebSocket 消息的处理器。
	 */
	protected final class WebSocketSendProcessor extends AbstractListenerWriteProcessor<WebSocketMessage> {

		private volatile boolean isReady = true;


		WebSocketSendProcessor() {
			super(receivePublisher.getLogPrefix());
		}


		@Override
		protected boolean write(WebSocketMessage message) throws IOException {
			if (logger.isTraceEnabled()) {
				logger.trace(getLogPrefix() + "Sending " + message);
			}
			else if (rsWriteLogger.isTraceEnabled()) {
				rsWriteLogger.trace(getLogPrefix() + "Sending " + message);
			}
			// In case of IOException, onError handling should call discardData(WebSocketMessage)..
			return sendMessage(message);
		}

		@Override
		protected boolean isDataEmpty(WebSocketMessage message) {
			return (message.getPayload().readableByteCount() == 0);
		}

		@Override
		protected boolean isWritePossible() {
			return (this.isReady);
		}

		/**
		 * 子类可以在发送消息之前（false）和收到异步发送回调之后（true）调用此
		 * 方法，从而有效地将异步完成回调转换为简单的流控制。
		 */
		public void setReadyToSend(boolean ready) {
			if (ready && rsWriteLogger.isTraceEnabled()) {
				rsWriteLogger.trace(getLogPrefix() + "Ready to send");
			}
			this.isReady = ready;
		}

		@Override
		protected void discardData(WebSocketMessage message) {
			message.release();
		}
	}

}
