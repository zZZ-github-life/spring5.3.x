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

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Operators;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@code Publisher} 实现的抽象基类，用于在事件监听式读取 API
 * 与 Reactive Streams 之间进行桥接。
 *
 * <p>具体而言，是用于通过 Servlet 3.1 非阻塞 I/O 和 Undertow XNIO 从 HTTP
 * 请求体读取数据，以及通过标准 Java WebSocket（JSR-356）、Jetty 和 Undertow
 * 处理传入 WebSocket 消息的基类。
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of element signaled
 */
public abstract class AbstractListenerReadPublisher<T> implements Publisher<T> {

	/**
	 * 用于调试 Reactive Streams 信号的专用日志记录器。
	 * @see LogDelegateFactory#getHiddenLog(Class)
	 * @see AbstractListenerWriteProcessor#rsWriteLogger
	 * @see AbstractListenerWriteFlushProcessor#rsWriteFlushLogger
	 * @see WriteResultPublisher#rsWriteResultLogger
	 */
	protected static Log rsReadLogger = LogDelegateFactory.getHiddenLog(AbstractListenerReadPublisher.class);

	final static DataBuffer EMPTY_BUFFER = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);


	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	private volatile long demand;

	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<AbstractListenerReadPublisher> DEMAND_FIELD_UPDATER =
			AtomicLongFieldUpdater.newUpdater(AbstractListenerReadPublisher.class, "demand");

	@Nullable
	private volatile Subscriber<? super T> subscriber;

	private volatile boolean completionPending;

	@Nullable
	private volatile Throwable errorPending;

	private final String logPrefix;


	public AbstractListenerReadPublisher() {
		this("");
	}

	/**
	 * 使用给定日志前缀创建实例。
	 * @since 5.1
	 */
	public AbstractListenerReadPublisher(String logPrefix) {
		this.logPrefix = logPrefix;
	}


	/**
	 * 返回已配置的日志消息前缀。
	 * @since 5.1
	 */
	public String getLogPrefix() {
		return this.logPrefix;
	}


	// Publisher implementation...

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		this.state.get().subscribe(this, subscriber);
	}


	// Async I/O notification methods...

	/**
	 * 在可以读取时调用，可能是在通过 {@link #checkOnDataAvailable()}
	 * 检查后的同一线程中，也可能是来自底层容器的回调。
	 */
	public final void onDataAvailable() {
		rsReadLogger.trace(getLogPrefix() + "onDataAvailable");
		this.state.get().onDataAvailable(this);
	}

	/**
	 * 子类可以调用此方法，在所有数据读取完毕后委派容器通知。
	 */
	public void onAllDataRead() {
		State state = this.state.get();
		if (rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + "onAllDataRead [" + state + "]");
		}
		state.onAllDataRead(this);
	}

	/**
	 * 子类可以调用此方法，委派容器错误通知。
	 */
	public final void onError(Throwable ex) {
		State state = this.state.get();
		if (rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + "onError: " + ex + " [" + state + "]");
		}
		state.onError(this, ex);
	}


	// Read API methods to be implemented or template methods to override...

	/**
	 * 检查数据是否可用，并立即调用 {@link #onDataAvailable()}
	 * 或安排通知。
	 */
	protected abstract void checkOnDataAvailable();

	/**
	 * 如果可能，从输入中读取一次。
	 * @return the item that was read; or {@code null}
	 */
	@Nullable
	protected abstract T read() throws IOException;

	/**
	 * 由于需求不足而暂停读取时调用。
	 * <p><strong>注意：</strong>此方法保证不会与
	 * {@link #checkOnDataAvailable()} 竞争，因此可以在底层 API 支持的情况下
	 * 安全地暂停读取，即不会与通过 {@code checkOnDataAvailable()}
	 * 隐式恢复的调用发生竞争。
	 * @since 5.0.2
	 */
	protected abstract void readingPaused();

	/**
	 * 在底层服务器发生 I/O 读取错误之后，或在下游消费者发出取消信号之后调用，
	 * 以便子类丢弃它们可能持有的任何现有缓存数据。
	 * @since 5.0.11
	 */
	protected abstract void discardData();


	// Private methods for use in State...

	/**
	 * 一次读取并发布一条数据，直到没有更多数据、没有更多需求，
	 * 或者在此期间已经完成。
	 * @return {@code true} if there is more demand; {@code false} if there is
	 * no more demand or we have completed.
	 */
	private boolean readAndPublish() throws IOException {
		long r;
		while ((r = this.demand) > 0 && (this.state.get() != State.COMPLETED)) {
			T data = read();
			if (data == EMPTY_BUFFER) {
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "0 bytes read, trying again");
				}
			}
			else if (data != null) {
				if (r != Long.MAX_VALUE) {
					DEMAND_FIELD_UPDATER.addAndGet(this, -1L);
				}
				Subscriber<? super T> subscriber = this.subscriber;
				Assert.state(subscriber != null, "No subscriber");
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "Publishing " + data.getClass().getSimpleName());
				}
				subscriber.onNext(data);
			}
			else {
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "No more to read");
				}
				return true;
			}
		}
		return false;
	}

	private boolean changeState(State oldState, State newState) {
		boolean result = this.state.compareAndSet(oldState, newState);
		if (result && rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + oldState + " -> " + newState);
		}
		return result;
	}

	private void changeToDemandState(State oldState) {
		if (changeState(oldState, State.DEMAND)) {
			// Protect from infinite recursion in Undertow, where we can't check if data
			// is available, so all we can do is to try to read.
			// Generally, no need to check if we just came out of readAndPublish()...
			if (oldState != State.READING) {
				checkOnDataAvailable();
			}
		}
	}

	private boolean handlePendingCompletionOrError() {
		State state = this.state.get();
		if (state == State.DEMAND || state  == State.NO_DEMAND) {
			if (this.completionPending) {
				rsReadLogger.trace(getLogPrefix() + "Processing pending completion");
				this.state.get().onAllDataRead(this);
				return true;
			}
			Throwable ex = this.errorPending;
			if (ex != null) {
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "Processing pending completion with error: " + ex);
				}
				this.state.get().onError(this, ex);
				return true;
			}
		}
		return false;
	}

	private Subscription createSubscription() {
		return new ReadSubscription();
	}


	/**
	 * 将信号委托给 State 的 Subscription。
	 */
	private final class ReadSubscription implements Subscription {


		@Override
		public final void request(long n) {
			if (rsReadLogger.isTraceEnabled()) {
				rsReadLogger.trace(getLogPrefix() + "request " + (n != Long.MAX_VALUE ? n : "Long.MAX_VALUE"));
			}
			state.get().request(AbstractListenerReadPublisher.this, n);
		}

		@Override
		public final void cancel() {
			State state = AbstractListenerReadPublisher.this.state.get();
			if (rsReadLogger.isTraceEnabled()) {
				rsReadLogger.trace(getLogPrefix() + "cancel [" + state + "]");
			}
			state.cancel(AbstractListenerReadPublisher.this);
		}
	}


	/**
	 * 表示 {@link Publisher} 可能处于的状态。
	 * <p><pre>
	 *        UNSUBSCRIBED
	 *             |
	 *             v
	 *        SUBSCRIBING
	 *             |
	 *             v
	 *    +---- NO_DEMAND ---------------> DEMAND ---+
	 *    |        ^                         ^       |
	 *    |        |                         |       |
	 *    |        +------- READING <--------+       |
	 *    |                    |                     |
	 *    |                    v                     |
	 *    +--------------> COMPLETED <---------------+
	 * </pre>
	 */
	private enum State {

		UNSUBSCRIBED {
			@Override
			<T> void subscribe(AbstractListenerReadPublisher<T> publisher, Subscriber<? super T> subscriber) {
				Assert.notNull(publisher, "Publisher must not be null");
				Assert.notNull(subscriber, "Subscriber must not be null");
				if (publisher.changeState(this, SUBSCRIBING)) {
					Subscription subscription = publisher.createSubscription();
					publisher.subscriber = subscriber;
					subscriber.onSubscribe(subscription);
					publisher.changeState(SUBSCRIBING, NO_DEMAND);
					publisher.handlePendingCompletionOrError();
				}
				else {
					throw new IllegalStateException("Failed to transition to SUBSCRIBING, " +
							"subscriber: " + subscriber);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				publisher.completionPending = true;
				publisher.handlePendingCompletionOrError();
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				publisher.errorPending = ex;
				publisher.handlePendingCompletionOrError();
			}
		},

		/**
		 * 一个非常短暂的状态：此时已知有 Subscriber，但必须等到
		 * onSubscribe 之后才能发送 onComplete 和 onError。
		 */
		SUBSCRIBING {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					publisher.changeToDemandState(this);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				publisher.completionPending = true;
				publisher.handlePendingCompletionOrError();
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				publisher.errorPending = ex;
				publisher.handlePendingCompletionOrError();
			}
		},

		NO_DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					publisher.changeToDemandState(this);
				}
			}
		},

		DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					// Did a concurrent read transition to NO_DEMAND just before us?
					publisher.changeToDemandState(NO_DEMAND);
				}
			}

			@Override
			<T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
				if (publisher.changeState(this, READING)) {
					try {
						boolean demandAvailable = publisher.readAndPublish();
						if (demandAvailable) {
							publisher.changeToDemandState(READING);
							publisher.handlePendingCompletionOrError();
						}
						else {
							publisher.readingPaused();
							if (publisher.changeState(READING, NO_DEMAND)) {
								if (!publisher.handlePendingCompletionOrError()) {
									// Demand may have arrived since readAndPublish returned
									long r = publisher.demand;
									if (r > 0) {
										publisher.changeToDemandState(NO_DEMAND);
									}
								}
							}
						}
					}
					catch (IOException ex) {
						publisher.onError(ex);
					}
				}
				// Else, either competing onDataAvailable (request vs container), or concurrent completion
			}
		},

		READING {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					// Did a concurrent read transition to NO_DEMAND just before us?
					publisher.changeToDemandState(NO_DEMAND);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				publisher.completionPending = true;
				publisher.handlePendingCompletionOrError();
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				publisher.errorPending = ex;
				publisher.handlePendingCompletionOrError();
			}
		},

		COMPLETED {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				// ignore
			}
			@Override
			<T> void cancel(AbstractListenerReadPublisher<T> publisher) {
				// ignore
			}
			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				// ignore
			}
			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
				// ignore
			}
		};

		<T> void subscribe(AbstractListenerReadPublisher<T> publisher, Subscriber<? super T> subscriber) {
			throw new IllegalStateException(toString());
		}

		<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
			throw new IllegalStateException(toString());
		}

		<T> void cancel(AbstractListenerReadPublisher<T> publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				publisher.discardData();
			}
			else {
				publisher.state.get().cancel(publisher);
			}
		}

		<T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
			// ignore
		}

		<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				Subscriber<? super T> s = publisher.subscriber;
				if (s != null) {
					s.onComplete();
				}
			}
			else {
				publisher.state.get().onAllDataRead(publisher);
			}
		}

		<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
			if (publisher.changeState(this, COMPLETED)) {
				publisher.discardData();
				Subscriber<? super T> s = publisher.subscriber;
				if (s != null) {
					s.onError(t);
				}
			}
			else {
				publisher.state.get().onError(publisher, t);
			}
		}
	}

}
