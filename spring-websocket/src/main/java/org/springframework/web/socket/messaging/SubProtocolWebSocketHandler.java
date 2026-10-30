/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.socket.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;
import org.springframework.web.socket.sockjs.transport.session.PollingSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.StreamingSockJsSession;

/**
 * {@link WebSocketHandler} 的实现，它将传入的 WebSocket 消息连同
 * {@link MessageChannel} 一起委托给 {@link SubProtocolHandler}，
 * 子协议处理器可以通过该通道将来自 WebSocket 客户端的消息发送到应用程序。
 *
 * <p>它同时也是 {@link MessageHandler} 的实现，会找到与 {@link Message}
 * 关联的 WebSocket 会话，并将其连同消息一起传递给子协议处理器，
 * 以便将消息从应用程序发送回客户端。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Andy Wilkinson
 * @author Artem Bilan
 * @since 4.0
 */
public class SubProtocolWebSocketHandler
		implements WebSocketHandler, SubProtocolCapable, MessageHandler, SmartLifecycle {

	/** {@link #setTimeToFirstMessage(int) timeToFirstMessage} 的默认值。 */
	private static final int DEFAULT_TIME_TO_FIRST_MESSAGE = 60 * 1000;


	private final Log logger = LogFactory.getLog(SubProtocolWebSocketHandler.class);


	private final MessageChannel clientInboundChannel;

	private final SubscribableChannel clientOutboundChannel;

	private final Map<String, SubProtocolHandler> protocolHandlerLookup =
			new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private final Set<SubProtocolHandler> protocolHandlers = new LinkedHashSet<>();

	@Nullable
	private SubProtocolHandler defaultProtocolHandler;

	private final Map<String, WebSocketSessionHolder> sessions = new ConcurrentHashMap<>();

	private int sendTimeLimit = 10 * 1000;

	private int sendBufferSizeLimit = 512 * 1024;

	private int timeToFirstMessage = DEFAULT_TIME_TO_FIRST_MESSAGE;

	private volatile long lastSessionCheckTime = System.currentTimeMillis();

	private final ReentrantLock sessionCheckLock = new ReentrantLock();

	private final DefaultStats stats = new DefaultStats();

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	/**
	 * 为给定的入站和出站通道创建一个新的 {@code SubProtocolWebSocketHandler}。
	 * @param clientInboundChannel 入站 {@code MessageChannel}
	 * @param clientOutboundChannel 出站 {@code MessageChannel}
	 */
	public SubProtocolWebSocketHandler(MessageChannel clientInboundChannel, SubscribableChannel clientOutboundChannel) {
		Assert.notNull(clientInboundChannel, "Inbound MessageChannel must not be null");
		Assert.notNull(clientOutboundChannel, "Outbound MessageChannel must not be null");
		this.clientInboundChannel = clientInboundChannel;
		this.clientOutboundChannel = clientOutboundChannel;
	}


	/**
	 * 配置一个或多个处理器，根据客户端在 WebSocket 握手请求中请求的子协议
	 * 来决定使用哪个。
	 * @param protocolHandlers 要使用的子协议处理器
	 */
	public void setProtocolHandlers(List<SubProtocolHandler> protocolHandlers) {
		this.protocolHandlerLookup.clear();
		this.protocolHandlers.clear();
		for (SubProtocolHandler handler : protocolHandlers) {
			addProtocolHandler(handler);
		}
	}

	public List<SubProtocolHandler> getProtocolHandlers() {
		return new ArrayList<>(this.protocolHandlers);
	}

	/**
	 * 注册一个子协议处理器。
	 */
	public void addProtocolHandler(SubProtocolHandler handler) {
		List<String> protocols = handler.getSupportedProtocols();
		if (CollectionUtils.isEmpty(protocols)) {
			if (logger.isErrorEnabled()) {
				logger.error("No sub-protocols for " + handler);
			}
			return;
		}
		for (String protocol : protocols) {
			SubProtocolHandler replaced = this.protocolHandlerLookup.put(protocol, handler);
			if (replaced != null && replaced != handler) {
				throw new IllegalStateException("Cannot map " + handler +
						" to protocol '" + protocol + "': already mapped to " + replaced + ".");
			}
		}
		this.protocolHandlers.add(handler);
	}

	/**
	 * 返回以协议名称为键的子协议。
	 */
	public Map<String, SubProtocolHandler> getProtocolHandlerMap() {
		return this.protocolHandlerLookup;
	}

	/**
	 * 设置客户端未请求子协议时要使用的 {@link SubProtocolHandler}。
	 * @param defaultProtocolHandler 默认处理器
	 */
	public void setDefaultProtocolHandler(@Nullable SubProtocolHandler defaultProtocolHandler) {
		this.defaultProtocolHandler = defaultProtocolHandler;
		if (this.protocolHandlerLookup.isEmpty()) {
			setProtocolHandlers(Collections.singletonList(defaultProtocolHandler));
		}
	}

	/**
	 * 返回要使用的默认子协议处理器。
	 */
	@Nullable
	public SubProtocolHandler getDefaultProtocolHandler() {
		return this.defaultProtocolHandler;
	}

	/**
	 * 返回所有支持的协议。
	 */
	@Override
	public List<String> getSubProtocols() {
		return new ArrayList<>(this.protocolHandlerLookup.keySet());
	}

	/**
	 * 指定发送时间限制（毫秒）。
	 * @see ConcurrentWebSocketSessionDecorator
	 */
	public void setSendTimeLimit(int sendTimeLimit) {
		this.sendTimeLimit = sendTimeLimit;
	}

	/**
	 * 返回发送时间限制（毫秒）。
	 */
	public int getSendTimeLimit() {
		return this.sendTimeLimit;
	}

	/**
	 * 指定缓冲区大小限制（字节数）。
	 * @see ConcurrentWebSocketSessionDecorator
	 */
	public void setSendBufferSizeLimit(int sendBufferSizeLimit) {
		this.sendBufferSizeLimit = sendBufferSizeLimit;
	}

	/**
	 * 返回缓冲区大小限制（字节数）。
	 */
	public int getSendBufferSizeLimit() {
		return this.sendBufferSizeLimit;
	}

	/**
	 * 设置 WebSocket 连接建立之后、收到第一条子协议消息之前所允许的
	 * 最大时间（毫秒）。
	 * <p>此处理器针对使用子协议的 WebSocket 连接。
	 * 因此，我们期望客户端在开始时至少发送一条子协议消息，
	 * 否则就认为连接状态不佳（例如代理问题、网络缓慢），
	 * 并可以将其关闭。
	 * <p>默认情况下设置为 {@code 60,000}（1 分钟）。
	 * @param timeToFirstMessage 允许的最长时间，单位为毫秒
	 * @since 5.1
	 * @see #checkSessions()
	 */
	public void setTimeToFirstMessage(int timeToFirstMessage) {
		this.timeToFirstMessage = timeToFirstMessage;
	}

	/**
	 * 返回 WebSocket 连接建立之后、收到第一条子协议消息之前所允许的
	 * 最大时间。
	 * @since 5.1
	 */
	public int getTimeToFirstMessage() {
		return this.timeToFirstMessage;
	}

	/**
	 * 返回描述内部状态和计数器的字符串。
	 * 实际上就是调用 {@link #getStats() getStats()} 的 {@code toString()}。
	 */
	public String getStatsInfo() {
		return this.stats.toString();
	}

	/**
	 * 返回包含各种会话计数器的结构化对象。
	 * @since 5.2
	 */
	public Stats getStats() {
		return this.stats;
	}



	@Override
	public final void start() {
		Assert.isTrue(this.defaultProtocolHandler != null || !this.protocolHandlers.isEmpty(), "No handlers");

		synchronized (this.lifecycleMonitor) {
			this.clientOutboundChannel.subscribe(this);
			this.running = true;
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.clientOutboundChannel.unsubscribe(this);
		}

		// Proactively notify all active WebSocket sessions
		for (WebSocketSessionHolder holder : this.sessions.values()) {
			try {
				holder.getSession().close(CloseStatus.GOING_AWAY);
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to close '" + holder.getSession() + "': " + ex);
				}
			}
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	@Override
	public final boolean isRunning() {
		return this.running;
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// WebSocketHandlerDecorator could close the session
		if (!session.isOpen()) {
			return;
		}

		this.stats.incrementSessionCount(session);
		session = decorateSession(session);
		this.sessions.put(session.getId(), new WebSocketSessionHolder(session));
		findProtocolHandler(session).afterSessionStarted(session, this.clientInboundChannel);
	}

	/**
	 * 处理来自 WebSocket 客户端的入站消息。
	 */
	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		WebSocketSessionHolder holder = this.sessions.get(session.getId());
		if (holder != null) {
			session = holder.getSession();
		}
		SubProtocolHandler protocolHandler = findProtocolHandler(session);
		protocolHandler.handleMessageFromClient(session, message, this.clientInboundChannel);
		if (holder != null) {
			holder.setHasHandledMessages();
		}
		checkSessions();
	}

	/**
	 * 处理发往 WebSocket 客户端的出站 Spring Message。
	 */
	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		String sessionId = resolveSessionId(message);
		if (sessionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("Could not find session id in " + message);
			}
			return;
		}

		WebSocketSessionHolder holder = this.sessions.get(sessionId);
		if (holder == null) {
			if (logger.isDebugEnabled()) {
				// The broker may not have removed the session yet
				logger.debug("No session for " + message);
			}
			return;
		}

		WebSocketSession session = holder.getSession();
		try {
			findProtocolHandler(session).handleMessageToClient(session, message);
		}
		catch (SessionLimitExceededException ex) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Terminating '" + session + "'", ex);
				}
				else if (logger.isWarnEnabled()) {
					logger.warn("Terminating '" + session + "': " + ex.getMessage());
				}
				this.stats.incrementLimitExceededCount();
				clearSession(session, ex.getStatus()); // clear first, session may be unresponsive
				session.close(ex.getStatus());
			}
			catch (Exception secondException) {
				logger.debug("Failure while closing session " + sessionId + ".", secondException);
			}
		}
		catch (Exception ex) {
			// Could be part of normal workflow (e.g. browser tab closed)
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to send message to client in " + session + ": " + message, ex);
			}
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		this.stats.incrementTransportError();
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		clearSession(session, closeStatus);
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}


	/**
	 * 如果需要，装饰给定的 {@link WebSocketSession}。
	 * <p>默认实现使用配置的 {@link #getSendTimeLimit() 发送时间限制}
	 * 和 {@link #getSendBufferSizeLimit() 缓冲区大小限制}
	 * 构建一个 {@link ConcurrentWebSocketSessionDecorator}。
	 * @param session 原始的 {@code WebSocketSession}
	 * @return 装饰后的 {@code WebSocketSession}，也可能是原样返回的给定会话
	 * @since 4.3.13
	 */
	protected WebSocketSession decorateSession(WebSocketSession session) {
		return new ConcurrentWebSocketSessionDecorator(session, getSendTimeLimit(), getSendBufferSizeLimit());
	}

	/**
	 * 为给定会话查找 {@link SubProtocolHandler}。
	 * @param session 要为其查找处理器的 {@code WebSocketSession}
	 */
	protected final SubProtocolHandler findProtocolHandler(WebSocketSession session) {
		String protocol = null;
		try {
			protocol = session.getAcceptedProtocol();
		}
		catch (Exception ex) {
			// Shouldn't happen
			logger.error("Failed to obtain session.getAcceptedProtocol(): " +
					"will use the default protocol handler (if configured).", ex);
		}

		SubProtocolHandler handler;
		if (StringUtils.hasLength(protocol)) {
			handler = this.protocolHandlerLookup.get(protocol);
			if (handler == null) {
				throw new IllegalStateException(
						"No handler for '" + protocol + "' among " + this.protocolHandlerLookup);
			}
		}
		else {
			if (this.defaultProtocolHandler != null) {
				handler = this.defaultProtocolHandler;
			}
			else if (this.protocolHandlers.size() == 1) {
				handler = this.protocolHandlers.iterator().next();
			}
			else {
				throw new IllegalStateException("Multiple protocol handlers configured and " +
						"no protocol was negotiated. Consider configuring a default SubProtocolHandler.");
			}
		}
		return handler;
	}

	@Nullable
	private String resolveSessionId(Message<?> message) {
		for (SubProtocolHandler handler : this.protocolHandlerLookup.values()) {
			String sessionId = handler.resolveSessionId(message);
			if (sessionId != null) {
				return sessionId;
			}
		}
		if (this.defaultProtocolHandler != null) {
			String sessionId = this.defaultProtocolHandler.resolveSessionId(message);
			if (sessionId != null) {
				return sessionId;
			}
		}
		return null;
	}

	/**
	 * 当会话通过更高级别协议连接时，它有机会使用心跳管理来关闭那些
	 * 发送或接收消息太慢的会话。然而，在 WebSocketSession 建立之后、
	 * 更高级别协议完全连接之前，会话有可能挂起。此方法会检查并关闭
	 * 任何已连接超过 60 秒但未收到任何消息的会话。
	 */
	private void checkSessions() {
		long currentTime = System.currentTimeMillis();
		if (!isRunning() || (currentTime - this.lastSessionCheckTime < getTimeToFirstMessage())) {
			return;
		}

		if (this.sessionCheckLock.tryLock()) {
			try {
				for (WebSocketSessionHolder holder : this.sessions.values()) {
					if (holder.hasHandledMessages()) {
						continue;
					}
					long timeSinceCreated = currentTime - holder.getCreateTime();
					if (timeSinceCreated < getTimeToFirstMessage()) {
						continue;
					}
					WebSocketSession session = holder.getSession();
					if (logger.isInfoEnabled()) {
						logger.info("No messages received after " + timeSinceCreated + " ms. " +
								"Closing " + holder.getSession() + ".");
					}
					try {
						this.stats.incrementNoMessagesReceivedCount();
						session.close(CloseStatus.SESSION_NOT_RELIABLE);
					}
					catch (Throwable ex) {
						if (logger.isWarnEnabled()) {
							logger.warn("Failed to close unreliable " + session, ex);
						}
					}
				}
			}
			finally {
				this.lastSessionCheckTime = currentTime;
				this.sessionCheckLock.unlock();
			}
		}
	}

	private void clearSession(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Clearing session " + session.getId());
		}
		if (this.sessions.remove(session.getId()) != null) {
			this.stats.decrementSessionCount(session);
		}
		findProtocolHandler(session).afterSessionEnded(session, closeStatus, this.clientInboundChannel);
	}


	@Override
	public String toString() {
		return "SubProtocolWebSocketHandler" + this.protocolHandlers;
	}


	private static class WebSocketSessionHolder {

		private final WebSocketSession session;

		private final long createTime;

		private volatile boolean hasHandledMessages;

		public WebSocketSessionHolder(WebSocketSession session) {
			this.session = session;
			this.createTime = System.currentTimeMillis();
		}

		public WebSocketSession getSession() {
			return this.session;
		}

		public long getCreateTime() {
			return this.createTime;
		}

		public void setHasHandledMessages() {
			this.hasHandledMessages = true;
		}

		public boolean hasHandledMessages() {
			return this.hasHandledMessages;
		}

		@Override
		public String toString() {
			return "WebSocketSessionHolder[session=" + this.session + ", createTime=" +
					this.createTime + ", hasHandledMessages=" + this.hasHandledMessages + "]";
		}
	}


	/**
	 * 用于访问会话计数器的契约。
	 * @since 5.2
	 */
	public interface Stats {

		int getTotalSessions();

		int getWebSocketSessions();

		int getHttpStreamingSessions();

		int getHttpPollingSessions();

		int getLimitExceededSessions();

		int getNoMessagesReceivedSessions();

		int getTransportErrorSessions();
	}


	private class DefaultStats implements Stats {

		private final AtomicInteger total = new AtomicInteger();

		private final AtomicInteger webSocket = new AtomicInteger();

		private final AtomicInteger httpStreaming = new AtomicInteger();

		private final AtomicInteger httpPolling = new AtomicInteger();

		private final AtomicInteger limitExceeded = new AtomicInteger();

		private final AtomicInteger noMessagesReceived = new AtomicInteger();

		private final AtomicInteger transportError = new AtomicInteger();

		@Override
		public int getTotalSessions() {
			return this.total.get();
		}

		@Override
		public int getWebSocketSessions() {
			return this.webSocket.get();
		}

		@Override
		public int getHttpStreamingSessions() {
			return this.httpStreaming.get();
		}

		@Override
		public int getHttpPollingSessions() {
			return this.httpPolling.get();
		}

		@Override
		public int getLimitExceededSessions() {
			return this.limitExceeded.get();
		}

		@Override
		public int getNoMessagesReceivedSessions() {
			return this.noMessagesReceived.get();
		}

		@Override
		public int getTransportErrorSessions() {
			return this.transportError.get();
		}

		void incrementSessionCount(WebSocketSession session) {
			getCountFor(session).incrementAndGet();
			this.total.incrementAndGet();
		}

		void decrementSessionCount(WebSocketSession session) {
			getCountFor(session).decrementAndGet();
		}

		void incrementLimitExceededCount() {
			this.limitExceeded.incrementAndGet();
		}

		void incrementNoMessagesReceivedCount() {
			this.noMessagesReceived.incrementAndGet();
		}

		void incrementTransportError() {
			this.transportError.incrementAndGet();
		}

		AtomicInteger getCountFor(WebSocketSession session) {
			session = WebSocketSessionDecorator.unwrap(session);
			if (session instanceof PollingSockJsSession) {
				return this.httpPolling;
			}
			else if (session instanceof StreamingSockJsSession) {
				return this.httpStreaming;
			}
			else {
				return this.webSocket;
			}
		}

		@Override
		public String toString() {
			return SubProtocolWebSocketHandler.this.sessions.size() +
					" current WS(" + this.webSocket.get() +
					")-HttpStream(" + this.httpStreaming.get() +
					")-HttpPoll(" + this.httpPolling.get() + "), " +
					this.total.get() + " total, " +
					(this.limitExceeded.get() + this.noMessagesReceived.get()) + " closed abnormally (" +
					this.noMessagesReceived.get() + " connect failure, " +
					this.limitExceeded.get() + " send limit, " +
					this.transportError.get() + " transport error)";
		}
	}

}
