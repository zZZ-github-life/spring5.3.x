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

package org.springframework.messaging.handler.invocation.reactive;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * ----------------------
 * <p><strong>注意：</strong>此类复制自
 * {@code org.springframework.http.server.reactive.ChannelSendOperator}，与其完全相同。
 * 它用于相同的目的，即如果输出发布者以错误开始时，能够切换到通过带注解的
 * 异常处理方法进行替代处理。
 * <p>----------------------<br>
 *
 * <p>给定一个写入函数，它接受用于写入的源 {@code Publisher<T>} 并返回结果
 * {@code Publisher<Void>}，此操作符有助于延迟写入函数的调用，
 * 直到我们确定源发布者是否会无错误地开始发布。如果第一个发射是错误，
 * 则绕过写入函数，并直接通过结果发布者发送该错误。否则调用写入函数。
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.2
 * @param <T> the type of element signaled
 */
class ChannelSendOperator<T> extends Mono<Void> implements Scannable {

	private final Function<Publisher<T>, Publisher<Void>> writeFunction;

	private final Flux<T> source;


	public ChannelSendOperator(Publisher<? extends T> source, Function<Publisher<T>, Publisher<Void>> writeFunction) {
		this.source = Flux.from(source);
		this.writeFunction = writeFunction;
	}


	@Override
	@Nullable
	@SuppressWarnings("rawtypes")
	public Object scanUnsafe(Attr key) {
		if (key == Attr.PREFETCH) {
			return Integer.MAX_VALUE;
		}
		if (key == Attr.PARENT) {
			return this.source;
		}
		return null;
	}

	@Override
	public void subscribe(CoreSubscriber<? super Void> actual) {
		this.source.subscribe(new WriteBarrier(actual));
	}


	private enum State {

		/** 上游源尚未发射任何内容。 */
		NEW,

		/**
		 * 已收到任意类型的至少一个信号；我们已准备好调用写入函数并继续实际写入。
		 */
		FIRST_SIGNAL_RECEIVED,

		/**
		 * 写入订阅者已订阅并请求；我们将发射缓存的信号。
		 */
		EMITTING_CACHED_SIGNALS,

		/**
		 * 写入订阅者已订阅，且缓存的信号已发射给它；我们已准备好对其余所有信号
		 * 切换到简单的直通模式。
		 **/
		READY_TO_WRITE

	}


	/**
	 * 插入在写入源和写入订阅者（即 HTTP 服务器适配器）之间的屏障，
	 * 它会预取并等待第一个信号，然后决定是否接入写入订阅者。
	 *
	 * <p>它充当：
	 * <ul>
	 * <li>写入源的订阅者。
	 * <li>写入订阅者的订阅。
	 * <li>写入订阅者的发布者。
	 * </ul>
	 *
	 * <p>还使用 {@link WriteCompletionBarrier} 来传达完成状态，
	 * 并检测来自完成订阅者的取消信号。
	 */
	private class WriteBarrier implements CoreSubscriber<T>, Subscription, Publisher<T> {

		/* Bridges signals to and from the completionSubscriber */
		private final WriteCompletionBarrier writeCompletionBarrier;

		/* Upstream write source subscription */
		@Nullable
		private Subscription subscription;

		/** readyToWrite 之前缓存的数据项。 */
		@Nullable
		private T item;

		/** readyToWrite 之前缓存的错误信号。 */
		@Nullable
		private Throwable error;

		/** readyToWrite 之前缓存的 onComplete 信号。 */
		private boolean completed = false;

		/** 发射缓存信号时的递归需求。 */
		private long demandBeforeReadyToWrite;

		/** 当前状态。 */
		private State state = State.NEW;

		/** 来自 HTTP 服务器适配器的实际 writeSubscriber。 */
		@Nullable
		private Subscriber<? super T> writeSubscriber;


		WriteBarrier(CoreSubscriber<? super Void> completionSubscriber) {
			this.writeCompletionBarrier = new WriteCompletionBarrier(completionSubscriber, this);
		}


		// Subscriber<T> methods (we're the subscriber to the write source)..

		@Override
		public final void onSubscribe(Subscription s) {
			if (Operators.validate(this.subscription, s)) {
				this.subscription = s;
				this.writeCompletionBarrier.connect();
				s.request(1);
			}
		}

		@Override
		public final void onNext(T item) {
			if (this.state == State.READY_TO_WRITE) {
				requiredWriteSubscriber().onNext(item);
				return;
			}
			//FIXME revisit in case of reentrant sync deadlock
			synchronized (this) {
				if (this.state == State.READY_TO_WRITE) {
					requiredWriteSubscriber().onNext(item);
				}
				else if (this.state == State.NEW) {
					this.item = item;
					this.state = State.FIRST_SIGNAL_RECEIVED;
					Publisher<Void> result;
					try {
						result = writeFunction.apply(this);
					}
					catch (Throwable ex) {
						this.writeCompletionBarrier.onError(ex);
						return;
					}
					result.subscribe(this.writeCompletionBarrier);
				}
				else {
					if (this.subscription != null) {
						this.subscription.cancel();
					}
					this.writeCompletionBarrier.onError(new IllegalStateException("Unexpected item."));
				}
			}
		}

		private Subscriber<? super T> requiredWriteSubscriber() {
			Assert.state(this.writeSubscriber != null, "No write subscriber");
			return this.writeSubscriber;
		}

		@Override
		public final void onError(Throwable ex) {
			if (this.state == State.READY_TO_WRITE) {
				requiredWriteSubscriber().onError(ex);
				return;
			}
			synchronized (this) {
				if (this.state == State.READY_TO_WRITE) {
					requiredWriteSubscriber().onError(ex);
				}
				else if (this.state == State.NEW) {
					this.state = State.FIRST_SIGNAL_RECEIVED;
					this.writeCompletionBarrier.onError(ex);
				}
				else {
					this.error = ex;
				}
			}
		}

		@Override
		public final void onComplete() {
			if (this.state == State.READY_TO_WRITE) {
				requiredWriteSubscriber().onComplete();
				return;
			}
			synchronized (this) {
				if (this.state == State.READY_TO_WRITE) {
					requiredWriteSubscriber().onComplete();
				}
				else if (this.state == State.NEW) {
					this.completed = true;
					this.state = State.FIRST_SIGNAL_RECEIVED;
					Publisher<Void> result;
					try {
						result = writeFunction.apply(this);
					}
					catch (Throwable ex) {
						this.writeCompletionBarrier.onError(ex);
						return;
					}
					result.subscribe(this.writeCompletionBarrier);
				}
				else {
					this.completed = true;
				}
			}
		}

		@Override
		public Context currentContext() {
			return this.writeCompletionBarrier.currentContext();
		}


		// Subscription methods (we're the Subscription to the writeSubscriber)..

		@Override
		public void request(long n) {
			Subscription s = this.subscription;
			if (s == null) {
				return;
			}
			if (this.state == State.READY_TO_WRITE) {
				s.request(n);
				return;
			}
			synchronized (this) {
				if (this.writeSubscriber != null) {
					if (this.state == State.EMITTING_CACHED_SIGNALS) {
						this.demandBeforeReadyToWrite = n;
						return;
					}
					try {
						this.state = State.EMITTING_CACHED_SIGNALS;
						if (emitCachedSignals()) {
							return;
						}
						n = n + this.demandBeforeReadyToWrite - 1;
						if (n == 0) {
							return;
						}
					}
					finally {
						this.state = State.READY_TO_WRITE;
					}
				}
			}
			s.request(n);
		}

		private boolean emitCachedSignals() {
			if (this.error != null) {
				try {
					requiredWriteSubscriber().onError(this.error);
				}
				finally {
					releaseCachedItem();
				}
				return true;
			}
			T item = this.item;
			this.item = null;
			if (item != null) {
				requiredWriteSubscriber().onNext(item);
			}
			if (this.completed) {
				requiredWriteSubscriber().onComplete();
				return true;
			}
			return false;
		}

		@Override
		public void cancel() {
			Subscription s = this.subscription;
			if (s != null) {
				this.subscription = null;
				try {
					s.cancel();
				}
				finally {
					releaseCachedItem();
				}
			}
		}

		private void releaseCachedItem() {
			synchronized (this) {
				Object item = this.item;
				if (item instanceof DataBuffer) {
					DataBufferUtils.release((DataBuffer) item);
				}
				this.item = null;
			}
		}


		// Publisher<T> methods (we're the Publisher to the writeSubscriber)..

		@Override
		public void subscribe(Subscriber<? super T> writeSubscriber) {
			synchronized (this) {
				Assert.state(this.writeSubscriber == null, "Only one write subscriber supported");
				this.writeSubscriber = writeSubscriber;
				if (this.error != null || this.completed) {
					this.writeSubscriber.onSubscribe(Operators.emptySubscription());
					emitCachedSignals();
				}
				else {
					this.writeSubscriber.onSubscribe(this);
				}
			}
		}
	}


	/**
	 * 我们需要在 WriteBarrier 本身和实际完成订阅者之间增加一个额外的屏障。
	 *
	 * <p>completionSubscriber 最初订阅 WriteBarrier。在收到第一个信号后，
	 * 我们需要再创建一个订阅者实例（按照规范只能订阅一次）来订阅写入函数，
	 * 并切换为委托其完成信号。
	 */
	private class WriteCompletionBarrier implements CoreSubscriber<Void>, Subscription {

		/* Downstream write completion subscriber */
		private final CoreSubscriber<? super Void> completionSubscriber;

		private final WriteBarrier writeBarrier;

		@Nullable
		private Subscription subscription;


		public WriteCompletionBarrier(CoreSubscriber<? super Void> subscriber, WriteBarrier writeBarrier) {
			this.completionSubscriber = subscriber;
			this.writeBarrier = writeBarrier;
		}


		/**
		 * 将底层完成订阅者连接到此屏障，以便跟踪取消信号并将其传递给写入屏障。
		 */
		public void connect() {
			this.completionSubscriber.onSubscribe(this);
		}

		// Subscriber methods (we're the subscriber to the write function)..

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
		}

		@Override
		public void onError(Throwable ex) {
			try {
				this.completionSubscriber.onError(ex);
			}
			finally {
				this.writeBarrier.releaseCachedItem();
			}
		}

		@Override
		public void onComplete() {
			this.completionSubscriber.onComplete();
		}

		@Override
		public Context currentContext() {
			return this.completionSubscriber.currentContext();
		}


		@Override
		public void request(long n) {
			// Ignore: we don't produce data
		}

		@Override
		public void cancel() {
			this.writeBarrier.cancel();
			Subscription subscription = this.subscription;
			if (subscription != null) {
				subscription.cancel();
			}
		}
	}

}
