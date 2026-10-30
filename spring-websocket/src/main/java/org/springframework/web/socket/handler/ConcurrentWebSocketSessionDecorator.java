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

package org.springframework.web.socket.handler;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 包装 {@link org.springframework.web.socket.WebSocketSession WebSocketSession}
 * 以保证同一时间只有一个线程可以发送消息。
 *
 * <p>如果发送速度较慢，其他线程后续尝试发送更多消息时将无法获取刷新锁，
 * 消息会被缓冲。此时会检查指定的缓冲区大小限制和发送时间限制，
 * 如果超出限制则关闭会话。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0.3
 */
public class ConcurrentWebSocketSessionDecorator extends WebSocketSessionDecorator {

	private static final Log logger = LogFactory.getLog(ConcurrentWebSocketSessionDecorator.class);


	private final int sendTimeLimit;

	private final int bufferSizeLimit;

	private final OverflowStrategy overflowStrategy;

	@Nullable
	private Consumer<WebSocketMessage<?>> preSendCallback;


	private final Queue<WebSocketMessage<?>> buffer = new LinkedBlockingQueue<>();

	private final AtomicInteger bufferSize = new AtomicInteger();

	private volatile long sendStartTime;

	private volatile boolean limitExceeded;

	private volatile boolean closeInProgress;

	private final Lock flushLock = new ReentrantLock();

	private final Lock closeLock = new ReentrantLock();


	/**
	 * 基本构造函数。
	 * @param delegate 要委托的 {@code WebSocketSession}
	 * @param sendTimeLimit 发送时间限制（毫秒）
	 * @param bufferSizeLimit 缓冲区大小限制（字节数）
	 */
	public ConcurrentWebSocketSessionDecorator(WebSocketSession delegate, int sendTimeLimit, int bufferSizeLimit) {
		this(delegate, sendTimeLimit, bufferSizeLimit, OverflowStrategy.TERMINATE);
	}

	/**
	 * 同时指定要使用的溢出策略的构造函数。
	 * @param delegate 要委托的 {@code WebSocketSession}
	 * @param sendTimeLimit 发送时间限制（毫秒）
	 * @param bufferSizeLimit 缓冲区大小限制（字节数）
	 * @param overflowStrategy 要使用的溢出策略；默认情况下
	 * 会话会被终止。
	 * @since 5.1
	 */
	public ConcurrentWebSocketSessionDecorator(
			WebSocketSession delegate, int sendTimeLimit, int bufferSizeLimit, OverflowStrategy overflowStrategy) {

		super(delegate);
		this.sendTimeLimit = sendTimeLimit;
		this.bufferSizeLimit = bufferSizeLimit;
		this.overflowStrategy = overflowStrategy;
	}


	/**
	 * 返回配置的发送时间限制（毫秒）。
	 * @since 4.3.13
	 */
	public int getSendTimeLimit() {
		return this.sendTimeLimit;
	}

	/**
	 * 返回配置的缓冲区大小限制（字节数）。
	 * @since 4.3.13
	 */
	public int getBufferSizeLimit() {
		return this.bufferSizeLimit;
	}

	/**
	 * 返回当前缓冲区大小（字节数）。
	 */
	public int getBufferSize() {
		return this.bufferSize.get();
	}

	/**
	 * 返回自当前发送开始以来经过的时间（毫秒），
	 * 如果当前没有正在进行的发送则返回 0。
	 */
	public long getTimeSinceSendStarted() {
		long start = this.sendStartTime;
		return (start > 0 ? (System.currentTimeMillis() - start) : 0);
	}

	/**
	 * 设置一个回调，在消息被添加到发送缓冲区后调用。
	 * @param callback 要调用的回调
	 * @since 5.3
	 */
	public void setMessageCallback(Consumer<WebSocketMessage<?>> callback) {
		this.preSendCallback = callback;
	}


	@Override
	public void sendMessage(WebSocketMessage<?> message) throws IOException {
		if (shouldNotSend()) {
			return;
		}

		this.buffer.add(message);
		this.bufferSize.addAndGet(message.getPayloadLength());

		if (this.preSendCallback != null) {
			this.preSendCallback.accept(message);
		}

		do {
			if (!tryFlushMessageBuffer()) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Another send already in progress: " +
							"session id '%s':, \"in-progress\" send time %d (ms), buffer size %d bytes",
							getId(), getTimeSinceSendStarted(), getBufferSize()));
				}
				checkSessionLimits();
				break;
			}
		}
		while (!this.buffer.isEmpty() && !shouldNotSend());
	}

	private boolean shouldNotSend() {
		return (this.limitExceeded || this.closeInProgress);
	}

	private boolean tryFlushMessageBuffer() throws IOException {
		if (this.flushLock.tryLock()) {
			try {
				while (true) {
					WebSocketMessage<?> message = this.buffer.poll();
					if (message == null || shouldNotSend()) {
						break;
					}
					this.bufferSize.addAndGet(-message.getPayloadLength());
					this.sendStartTime = System.currentTimeMillis();
					getDelegate().sendMessage(message);
					this.sendStartTime = 0;
				}
			}
			finally {
				this.sendStartTime = 0;
				this.flushLock.unlock();
			}
			return true;
		}
		return false;
	}

	private void checkSessionLimits() {
		if (!shouldNotSend() && this.closeLock.tryLock()) {
			try {
				if (getTimeSinceSendStarted() > getSendTimeLimit()) {
					String format = "Send time %d (ms) for session '%s' exceeded the allowed limit %d";
					String reason = String.format(format, getTimeSinceSendStarted(), getId(), getSendTimeLimit());
					limitExceeded(reason);
				}
				else if (getBufferSize() > getBufferSizeLimit()) {
					switch (this.overflowStrategy) {
						case TERMINATE:
							String format = "Buffer size %d bytes for session '%s' exceeds the allowed limit %d";
							String reason = String.format(format, getBufferSize(), getId(), getBufferSizeLimit());
							limitExceeded(reason);
							break;
						case DROP:
							int i = 0;
							while (getBufferSize() > getBufferSizeLimit()) {
								WebSocketMessage<?> message = this.buffer.poll();
								if (message == null) {
									break;
								}
								this.bufferSize.addAndGet(-message.getPayloadLength());
								i++;
							}
							if (logger.isDebugEnabled()) {
								logger.debug("Dropped " + i + " messages, buffer size: " + getBufferSize());
							}
							break;
						default:
							// Should never happen..
							throw new IllegalStateException("Unexpected OverflowStrategy: " + this.overflowStrategy);
					}
				}
			}
			finally {
				this.closeLock.unlock();
			}
		}
	}

	private void limitExceeded(String reason) {
		this.limitExceeded = true;
		throw new SessionLimitExceededException(reason, CloseStatus.SESSION_NOT_RELIABLE);
	}

	@Override
	public void close(CloseStatus status) throws IOException {
		this.closeLock.lock();
		try {
			if (this.closeInProgress) {
				return;
			}
			if (!CloseStatus.SESSION_NOT_RELIABLE.equals(status)) {
				try {
					checkSessionLimits();
				}
				catch (SessionLimitExceededException ex) {
					// Ignore
				}
				if (this.limitExceeded) {
					if (logger.isDebugEnabled()) {
						logger.debug("Changing close status " + status + " to SESSION_NOT_RELIABLE.");
					}
					status = CloseStatus.SESSION_NOT_RELIABLE;
				}
			}
			this.closeInProgress = true;
			super.close(status);
		}
		finally {
			this.closeLock.unlock();
		}
	}


	@Override
	public String toString() {
		return getDelegate().toString();
	}


	/**
	 * 用于指定缓冲区已满时采取何种操作的选项枚举。
	 * @since 5.1
	 */
	public enum OverflowStrategy {

		/**
		 * 抛出 {@link SessionLimitExceededException}，这会导致
		 * 会话被终止。
		 */
		TERMINATE,

		/**
		 * 从缓冲区中丢弃最旧的消息。
		 */
		DROP
	}

}
