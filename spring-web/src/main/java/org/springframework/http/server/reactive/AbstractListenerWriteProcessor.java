/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@code Processor} 实现的抽象基类，用于在事件监听式写入 API
 * 与 Reactive Streams 之间进行桥接。
 *
 * <p>具体而言，是用于通过 Servlet 3.1 非阻塞 I/O 和 Undertow XNIO 写入
 * HTTP 响应体，以及通过 Java WebSocket API（JSR-356）、Jetty 和 Undertow
 * 写入 WebSocket 消息的基类。
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 向 {@link Subscriber} 发出信号元素的类型
 */
public abstract class AbstractListenerWriteProcessor<T> implements Processor<T, Void> {

	/**
	 * 用于调试 Reactive Streams 信号的专用日志记录器。
	 * @see LogDelegateFactory#getHiddenLog(Class)
	 * @see AbstractListenerReadPublisher#rsReadLogger
	 * @see AbstractListenerWriteFlushProcessor#rsWriteFlushLogger
	 * @see WriteResultPublisher#rsWriteResultLogger
	 */
	protected static final Log rsWriteLogger = LogDelegateFactory.getHiddenLog(AbstractListenerWriteProcessor.class);


	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	@Nullable
	private Subscription subscription;

	@Nullable
	private volatile T currentData;

	/* Indicates "onComplete" was received during the (last) write. */
	private volatile boolean sourceCompleted;

	/**
	 * 表示在 "onComplete" 之后我们仍在等待最后一次
	 * isReady-onWritePossible 周期，因为某些 Servlet 容器希望在调用
	 * AsyncContext.complete() 之前发生此过程。
	 * 参见 https://github.com/eclipse-ee4j/servlet-api/issues/273
	 */
	private volatile boolean readyToCompleteAfterLastWrite;

	private final WriteResultPublisher resultPublisher;

	private final String logPrefix;


	public AbstractListenerWriteProcessor() {
		this("");
	}

	/**
	 * 使用给定日志前缀创建实例。
	 * @since 5.1
	 */
	public AbstractListenerWriteProcessor(String logPrefix) {
		// AbstractListenerFlushProcessor calls cancelAndSetCompleted directly, so this cancel task
		// won't be used for HTTP responses, but it can be for a WebSocket session.
		this.resultPublisher = new WriteResultPublisher(logPrefix + "[WP] ", this::cancelAndSetCompleted);
		this.logPrefix = (StringUtils.hasText(logPrefix) ? logPrefix : "");
	}


	/**
	 * 获取已配置的日志前缀。
	 * @since 5.1
	 */
	public String getLogPrefix() {
		return this.logPrefix;
	}


	// Subscriber methods and async I/O notification methods...

	@Override
	public final void onSubscribe(Subscription subscription) {
		this.state.get().onSubscribe(this, subscription);
	}

	@Override
	public final void onNext(T data) {
		if (rsWriteLogger.isTraceEnabled()) {
			rsWriteLogger.trace(getLogPrefix() + "onNext: " + data.getClass().getSimpleName());
		}
		this.state.get().onNext(this, data);
	}

	/**
	 * 来自上游（写入 Publisher）的错误信号。子类也用它来
	 * 委派来自容器的错误通知。
	 */
	@Override
	public final void onError(Throwable ex) {
		State state = this.state.get();
		if (rsWriteLogger.isTraceEnabled()) {
			rsWriteLogger.trace(getLogPrefix() + "onError: " + ex + " [" + state + "]");
		}
		state.onError(this, ex);
	}

	/**
	 * 来自上游（写入 Publisher）的完成信号。子类也用它来
	 * 委派来自容器的完成通知。
	 */
	@Override
	public final void onComplete() {
		State state = this.state.get();
		if (rsWriteLogger.isTraceEnabled()) {
			rsWriteLogger.trace(getLogPrefix() + "onComplete [" + state + "]");
		}
		state.onComplete(this);
	}

	/**
	 * 在可以写入时调用，可能是在通过 {@link #isWritePossible()} 检查后的
	 * 同一线程中，也可能是来自底层容器的回调。
	 */
	public final void onWritePossible() {
		State state = this.state.get();
		if (rsWriteLogger.isTraceEnabled()) {
			rsWriteLogger.trace(getLogPrefix() + "onWritePossible [" + state + "]");
		}
		state.onWritePossible(this);
	}

	/**
	 * 仅取消上游"写入"Publisher，例如由于
	 * Servlet 容器的错误/完成通知。通常应随后调用
	 * {@link #onError(Throwable)} 或 {@link #onComplete()} 来通知下游链，
	 * 除非取消来自下游。
	 */
	public void cancel() {
		if (rsWriteLogger.isTraceEnabled()) {
			rsWriteLogger.trace(getLogPrefix() + "cancel [" + this.state + "]");
		}
		if (this.subscription != null) {
			this.subscription.cancel();
		}
	}

	/**
	 * 取消"写入"Publisher 并立即转换为 COMPLETED，同时
	 * 不通知下游。用于取消来自下游的情况。
	 */
	void cancelAndSetCompleted() {
		cancel();
		for (;;) {
			State prev = this.state.get();
			if (prev == State.COMPLETED) {
				break;
			}
			if (this.state.compareAndSet(prev, State.COMPLETED)) {
				if (rsWriteLogger.isTraceEnabled()) {
					rsWriteLogger.trace(getLogPrefix() + prev + " -> " + this.state);
				}
				if (prev != State.WRITING) {
					discardCurrentData();
				}
				break;
			}
		}
	}

	// Publisher implementation for result notifications...

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		this.resultPublisher.subscribe(subscriber);
	}


	// Write API methods to be implemented or template methods to override...

	/**
	 * 给定的数据项是否有内容可写入。
	 * 如果为 false，则不会写入该项。
	 */
	protected abstract boolean isDataEmpty(T data);

	/**
	 * 在通过 {@link Subscriber#onNext(Object)} 接收到待写入的数据项后
	 * 调用的模板方法。默认实现会保存该数据项，以便在可以写入时再写入。
	 */
	protected void dataReceived(T data) {
		T prev = this.currentData;
		if (prev != null) {
			// This shouldn't happen:
			//   1. dataReceived can only be called from REQUESTED state
			//   2. currentData is cleared before requesting
			discardData(data);
			cancel();
			onError(new IllegalStateException("Received new data while current not processed yet."));
		}
		this.currentData = data;
	}

	/**
	 * 是否可以写入。
	 */
	protected abstract boolean isWritePossible();

	/**
	 * 写入给定的项。
	 * <p><strong>注意：</strong>如果底层容器使用池化缓冲区，
	 * 子类负责在完全写入后释放与该数据项关联的任何数据缓冲区。
	 * @param data 要写入的项
	 * @return 如果当前数据项已完全写入并请求了新项则返回 {@code true}，
	 * 如果只写入了部分、在完全写入之前还需要更多写入回调，
	 * 则返回 {@code false}
	 */
	protected abstract boolean write(T data) throws IOException;

	/**
	 * 在当前数据写入完成之后、从上游写入 Publisher 请求下一个项之前调用。
	 * <p>默认实现为空操作。
	 * @deprecated originally introduced for Undertow to stop write notifications
	 * when no data is available, but deprecated as of 5.0.6 since constant
	 * switching on every requested item causes a significant slowdown.
	 */
	@Deprecated
	protected void writingPaused() {
	}

	/**
	 * 在 onComplete 或 onError 通知之后调用。
	 * <p>默认实现为空操作。
	 */
	protected void writingComplete() {
	}

	/**
	 * 写入过程中发生 I/O 错误时调用。如果子类知道底层 API 会在
	 * 容器线程中提供错误通知，可以选择忽略此调用。
	 * <p>默认为空操作。
	 */
	protected void writingFailed(Throwable ex) {
	}

	/**
	 * 在发生任何错误（来自上游写入 Publisher 或对底层服务器的 I/O 操作）
	 * 以及取消之后调用，用于丢弃错误发生时正在写入的
	 * 在途数据。
	 * @param data 要释放的数据
	 * @since 5.0.11
	 */
	protected abstract void discardData(T data);


	// Private methods for use from State's...

	private boolean changeState(State oldState, State newState) {
		boolean result = this.state.compareAndSet(oldState, newState);
		if (result && rsWriteLogger.isTraceEnabled()) {
			rsWriteLogger.trace(getLogPrefix() + oldState + " -> " + newState);
		}
		return result;
	}

	private void changeStateToReceived(State oldState) {
		if (changeState(oldState, State.RECEIVED)) {
			writeIfPossible();
		}
	}

	private void changeStateToComplete(State oldState) {
		if (changeState(oldState, State.COMPLETED)) {
			discardCurrentData();
			writingComplete();
			this.resultPublisher.publishComplete();
		}
		else {
			this.state.get().onComplete(this);
		}
	}

	private void writeIfPossible() {
		boolean result = isWritePossible();
		if (!result && rsWriteLogger.isTraceEnabled()) {
			rsWriteLogger.trace(getLogPrefix() + "isWritePossible false");
		}
		if (result) {
			onWritePossible();
		}
	}

	private void discardCurrentData() {
		T data = this.currentData;
		this.currentData = null;
		if (data != null) {
			discardData(data);
		}
	}


	/**
	 * 表示 {@link Processor} 可能处于的状态。
	 *
	 * <p><pre>
	 *        UNSUBSCRIBED
	 *             |
	 *             v
	 *   +--- REQUESTED -------------> RECEIVED ---+
	 *   |        ^                       ^        |
	 *   |        |                       |        |
	 *   |        + ------ WRITING <------+        |
	 *   |                    |                    |
	 *   |                    v                    |
	 *   +--------------> COMPLETED <--------------+
	 * </pre>
	 */
	private enum State {

		UNSUBSCRIBED {
			@Override
			public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
				Assert.notNull(subscription, "Subscription must not be null");
				if (processor.changeState(this, REQUESTED)) {
					processor.subscription = subscription;
					subscription.request(1);
				}
				else {
					super.onSubscribe(processor, subscription);
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				// This can happen on (very early) completion notification from container..
				processor.changeStateToComplete(this);
			}
		},

		REQUESTED {
			@Override
			public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
				if (processor.isDataEmpty(data)) {
					Assert.state(processor.subscription != null, "No subscription");
					processor.subscription.request(1);
				}
				else {
					processor.dataReceived(data);
					processor.changeStateToReceived(this);
				}
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.readyToCompleteAfterLastWrite = true;
				processor.changeStateToReceived(this);
			}
		},

		RECEIVED {
			@SuppressWarnings("deprecation")
			@Override
			public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
				if (processor.readyToCompleteAfterLastWrite) {
					processor.changeStateToComplete(RECEIVED);
				}
				else if (processor.changeState(this, WRITING)) {
					T data = processor.currentData;
					Assert.state(data != null, "No data");
					try {
						if (processor.write(data)) {
							if (processor.changeState(WRITING, REQUESTED)) {
								processor.currentData = null;
								if (processor.sourceCompleted) {
									processor.readyToCompleteAfterLastWrite = true;
									processor.changeStateToReceived(REQUESTED);
								}
								else {
									processor.writingPaused();
									Assert.state(processor.subscription != null, "No subscription");
									processor.subscription.request(1);
								}
							}
						}
						else {
							processor.changeStateToReceived(WRITING);
						}
					}
					catch (IOException ex) {
						processor.writingFailed(ex);
					}
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.sourceCompleted = true;
				// A competing write might have completed very quickly
				if (processor.state.get() == State.REQUESTED) {
					processor.changeStateToComplete(State.REQUESTED);
				}
			}
		},

		WRITING {
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.sourceCompleted = true;
				// A competing write might have completed very quickly
				if (processor.state.get() == State.REQUESTED) {
					processor.changeStateToComplete(State.REQUESTED);
				}
			}
		},

		COMPLETED {
			@Override
			public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
				// ignore
			}
			@Override
			public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
				// ignore
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				// ignore
			}
		};

		public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
			subscription.cancel();
		}

		public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
			processor.discardData(data);
			processor.cancel();
			processor.onError(new IllegalStateException("Illegal onNext without demand"));
		}

		public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
			if (processor.changeState(this, COMPLETED)) {
				processor.discardCurrentData();
				processor.writingComplete();
				processor.resultPublisher.publishError(ex);
			}
			else {
				processor.state.get().onError(processor, ex);
			}
		}

		public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
			throw new IllegalStateException(toString());
		}

		public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
			// ignore
		}
	}

}
