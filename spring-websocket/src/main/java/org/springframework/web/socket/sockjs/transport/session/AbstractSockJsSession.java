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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.sockjs.SockJsMessageDeliveryException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.SockJsSession;

/**
 * 实现 {@link SockJsSession} 的 SockJS 会话的抽象基类。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class AbstractSockJsSession implements SockJsSession {

	private enum State {NEW, OPEN, CLOSED}


	/**
	 * 在客户端离开后发生网络 IO 异常时使用的日志类别。
	 * <p>Servlet 容器不暴露客户端断开连接的回调；参见
	 * <a href="https://github.com/eclipse-ee4j/servlet-api/issues/44">eclipse-ee4j/servlet-api#44</a>。
	 * 因此，网络 IO 失败可能仅仅因为客户端已经离开而发生，
	 * 这会导致日志中充满不必要的堆栈跟踪。
	 * <p>我们会尽力在每台服务器的基础上识别此类网络故障，
	 * 并将其记录在单独的日志类别下。一条简单的一行消息
	 * 以 DEBUG 级别记录，而完整堆栈跟踪以 TRACE 级别显示。
	 * @see #disconnectedClientLogger
	 */
	public static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.socket.sockjs.DisconnectedClient";

	/**
	 * Tomcat: ClientAbortException 或 EOFException
	 * Jetty: EofException
	 * WildFly、GlassFish: java.io.IOException "Broken pipe"（已涵盖）
	 * <p>TODO:
	 * 此定义目前在 HttpWebHandlerAdapter
	 * 和 AbstractSockJsSession 之间重复。它是公共工具类的候选。
	 * @see #indicatesDisconnectedClient(Throwable)
	 */
	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS =
			new HashSet<>(Arrays.asList("ClientAbortException", "EOFException", "EofException"));


	/**
	 * 在客户端离开后发生网络 IO 失败时使用的独立 logger。
	 * @see #DISCONNECTED_CLIENT_LOG_CATEGORY
	 */
	protected static final Log disconnectedClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);

	protected final Log logger = LogFactory.getLog(getClass());

	protected final Object responseLock = new Object();

	private final String id;

	private final SockJsServiceConfig config;

	private final WebSocketHandler handler;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	private volatile State state = State.NEW;

	private final long timeCreated = System.currentTimeMillis();

	private volatile long timeLastActive = this.timeCreated;

	@Nullable
	private ScheduledFuture<?> heartbeatFuture;

	@Nullable
	private HeartbeatTask heartbeatTask;

	private volatile boolean heartbeatDisabled;


	/**
	 * 创建一个新实例。
	 * @param id 会话 ID
	 * @param config SockJS 服务配置选项
	 * @param handler SockJS 消息的接收者
	 * @param attributes 要与 WebSocket 会话关联的、来自 HTTP 握手的属性；
	 * 提供的属性会被复制，不使用原始 Map。
	 */
	public AbstractSockJsSession(String id, SockJsServiceConfig config, WebSocketHandler handler,
			@Nullable Map<String, Object> attributes) {

		Assert.notNull(id, "Session id must not be null");
		Assert.notNull(config, "SockJsServiceConfig must not be null");
		Assert.notNull(handler, "WebSocketHandler must not be null");

		this.id = id;
		this.config = config;
		this.handler = handler;

		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
	}


	@Override
	public String getId() {
		return this.id;
	}

	protected SockJsMessageCodec getMessageCodec() {
		return this.config.getMessageCodec();
	}

	public SockJsServiceConfig getSockJsServiceConfig() {
		return this.config;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}


	// Message sending

	@Override
	public final void sendMessage(WebSocketMessage<?> message) throws IOException {
		Assert.state(!isClosed(), "Cannot send a message when session is closed");
		Assert.isInstanceOf(TextMessage.class, message, "SockJS supports text messages only");
		sendMessageInternal(((TextMessage) message).getPayload());
	}

	protected abstract void sendMessageInternal(String message) throws IOException;


	// Lifecycle related methods

	public boolean isNew() {
		return State.NEW.equals(this.state);
	}

	@Override
	public boolean isOpen() {
		return State.OPEN.equals(this.state);
	}

	public boolean isClosed() {
		return State.CLOSED.equals(this.state);
	}

	/**
	 * 执行清理并通知 {@link WebSocketHandler}。
	 */
	@Override
	public final void close() throws IOException {
		close(new CloseStatus(3000, "Go away!"));
	}

	/**
	 * 执行清理并通知 {@link WebSocketHandler}。
	 */
	@Override
	public final void close(CloseStatus status) throws IOException {
		if (isOpen()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing SockJS session " + getId() + " with " + status);
			}
			this.state = State.CLOSED;
			try {
				if (isActive() && !CloseStatus.SESSION_NOT_RELIABLE.equals(status)) {
					try {
						writeFrameInternal(SockJsFrame.closeFrame(status.getCode(), status.getReason()));
					}
					catch (Throwable ex) {
						logger.debug("Failure while sending SockJS close frame", ex);
					}
				}
				updateLastActiveTime();
				cancelHeartbeat();
				disconnect(status);
			}
			finally {
				try {
					this.handler.afterConnectionClosed(this, status);
				}
				catch (Throwable ex) {
					logger.debug("Error from WebSocketHandler.afterConnectionClosed in " + this, ex);
				}
			}
		}
	}

	@Override
	public long getTimeSinceLastActive() {
		if (isNew()) {
			return (System.currentTimeMillis() - this.timeCreated);
		}
		else {
			return (isActive() ? 0 : System.currentTimeMillis() - this.timeLastActive);
		}
	}

	/**
	 * 应在会话变为非活动状态时调用。
	 */
	protected void updateLastActiveTime() {
		this.timeLastActive = System.currentTimeMillis();
	}

	@Override
	public void disableHeartbeat() {
		this.heartbeatDisabled = true;
		cancelHeartbeat();
	}

	protected void sendHeartbeat() throws SockJsTransportFailureException {
		synchronized (this.responseLock) {
			if (isActive() && !this.heartbeatDisabled) {
				writeFrame(SockJsFrame.heartbeatFrame());
				scheduleHeartbeat();
			}
		}
	}

	protected void scheduleHeartbeat() {
		if (this.heartbeatDisabled) {
			return;
		}
		synchronized (this.responseLock) {
			cancelHeartbeat();
			if (!isActive()) {
				return;
			}
			Date time = new Date(System.currentTimeMillis() + this.config.getHeartbeatTime());
			this.heartbeatTask = new HeartbeatTask();
			this.heartbeatFuture = this.config.getTaskScheduler().schedule(this.heartbeatTask, time);
			if (logger.isTraceEnabled()) {
				logger.trace("Scheduled heartbeat in session " + getId());
			}
		}
	}

	protected void cancelHeartbeat() {
		synchronized (this.responseLock) {
			if (this.heartbeatFuture != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Cancelling heartbeat in session " + getId());
				}
				this.heartbeatFuture.cancel(false);
				this.heartbeatFuture = null;
			}
			if (this.heartbeatTask != null) {
				this.heartbeatTask.cancel();
				this.heartbeatTask = null;
			}
		}
	}

	/**
	 * 轮询和流式会话会定期关闭当前 HTTP 请求并等待下一个请求
	 * 到来。在此"停机"期间，会话仍处于打开状态但处于非活动状态，
	 * 无法发送消息，因此必须暂时缓冲消息。
	 * 相比之下，WebSocket 会话是有状态的，在关闭之前保持活动状态。
	 */
	public abstract boolean isActive();

	/**
	 * 实际关闭底层 WebSocket 会话，或者在 HTTP 传输的情况下
	 * 完成底层请求。
	 */
	protected abstract void disconnect(CloseStatus status) throws IOException;


	// Frame writing

	/**
	 * 供 TransportHandler 和（特定于 TransportHandler 的）会话类内部使用。
	 */
	protected void writeFrame(SockJsFrame frame) throws SockJsTransportFailureException {
		if (logger.isTraceEnabled()) {
			logger.trace("Preparing to write " + frame);
		}
		try {
			writeFrameInternal(frame);
		}
		catch (Exception ex) {
			logWriteFrameFailure(ex);
			try {
				// Force disconnect (so we won't try to send close frame)
				disconnect(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable disconnectFailure) {
				// Ignore
			}
			try {
				close(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable closeFailure) {
				// Nothing of consequence, already forced disconnect
			}
			throw new SockJsTransportFailureException("Failed to write " + frame, getId(), ex);
		}
	}

	protected abstract void writeFrameInternal(SockJsFrame frame) throws IOException;

	private void logWriteFrameFailure(Throwable ex) {
		if (indicatesDisconnectedClient(ex)) {
			if (disconnectedClientLogger.isTraceEnabled()) {
				disconnectedClientLogger.trace("Looks like the client has gone away", ex);
			}
			else if (disconnectedClientLogger.isDebugEnabled()) {
				disconnectedClientLogger.debug("Looks like the client has gone away: " + ex +
						" (For a full stack trace, set the log category '" + DISCONNECTED_CLIENT_LOG_CATEGORY +
						"' to TRACE level.)");
			}
		}
		else {
			logger.debug("Terminating connection after failure to send message to client", ex);
		}
	}

	private boolean indicatesDisconnectedClient(Throwable ex)  {
		String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
		message = (message != null ? message.toLowerCase() : "");
		String className = ex.getClass().getSimpleName();
		return (message.contains("broken pipe") || DISCONNECTED_CLIENT_EXCEPTIONS.contains(className));
	}


	// Delegation methods

	public void delegateConnectionEstablished() throws Exception {
		this.state = State.OPEN;
		this.handler.afterConnectionEstablished(this);
	}

	public void delegateMessages(String... messages) throws SockJsMessageDeliveryException {
		for (int i = 0; i < messages.length; i++) {
			try {
				if (isClosed()) {
					logUndeliveredMessages(i, messages);
					return;
				}
				this.handler.handleMessage(this, new TextMessage(messages[i]));
			}
			catch (Exception ex) {
				if (isClosed()) {
					if (logger.isTraceEnabled()) {
						logger.trace("Failed to handle message '" + messages[i] + "'", ex);
					}
					logUndeliveredMessages(i, messages);
					return;
				}
				throw new SockJsMessageDeliveryException(this.id, getUndelivered(messages, i), ex);
			}
		}
	}

	private void logUndeliveredMessages(int index, String[] messages) {
		List<String> undelivered = getUndelivered(messages, index);
		if (logger.isTraceEnabled() && !undelivered.isEmpty()) {
			logger.trace("Dropped inbound message(s) due to closed session: " + undelivered);
		}
	}

	private static List<String> getUndelivered(String[] messages, int i) {
		switch (messages.length - i) {
			case 0:
				return Collections.emptyList();
			case 1:
				return (messages[i].trim().isEmpty() ?
						Collections.emptyList() : Collections.singletonList(messages[i]));
			default:
				return Arrays.stream(Arrays.copyOfRange(messages, i, messages.length))
						.filter(message -> !message.trim().isEmpty())
						.collect(Collectors.toList());
		}
	}

	/**
	 * 在底层连接关闭时调用。
	 */
	public final void delegateConnectionClosed(CloseStatus status) throws Exception {
		if (!isClosed()) {
			try {
				updateLastActiveTime();
				// Avoid cancelHeartbeat() and responseLock within server "close" callback
				ScheduledFuture<?> future = this.heartbeatFuture;
				if (future != null) {
					this.heartbeatFuture = null;
					future.cancel(false);
				}
			}
			finally {
				this.state = State.CLOSED;
				this.handler.afterConnectionClosed(this, status);
			}
		}
	}

	/**
	 * 由于 SockJS 传输处理产生的错误而关闭。
	 */
	public void tryCloseWithSockJsTransportError(Throwable error, CloseStatus closeStatus) {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing due to transport error for " + this);
		}
		try {
			delegateError(error);
		}
		catch (Throwable delegateException) {
			// Ignore
			logger.debug("Exception from error handling delegate", delegateException);
		}
		try {
			close(closeStatus);
		}
		catch (Throwable closeException) {
			logger.debug("Failure while closing " + this, closeException);
		}
	}

	public void delegateError(Throwable ex) throws Exception {
		this.handler.handleTransportError(this, ex);
	}


	// Self description

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + getId() + "]";
	}


	private class HeartbeatTask implements Runnable {

		private boolean expired;

		@Override
		public void run() {
			synchronized (responseLock) {
				if (!this.expired && !isClosed()) {
					try {
						sendHeartbeat();
					}
					catch (Throwable ex) {
						// Ignore: already handled in writeFrame...
					}
					finally {
						this.expired = true;
					}
				}
			}
		}

		void cancel() {
			this.expired = true;
		}
	}

}
