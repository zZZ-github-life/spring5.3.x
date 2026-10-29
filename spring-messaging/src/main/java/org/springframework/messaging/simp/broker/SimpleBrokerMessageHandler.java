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

package org.springframework.messaging.simp.broker;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;

/**
 * 一个“简单”的消息代理，它识别 {@link SimpMessageType} 中定义的消息类型，
 * 借助 {@link SubscriptionRegistry} 跟踪订阅，并向订阅者发送消息。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimpleBrokerMessageHandler extends AbstractBrokerMessageHandler {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];


	@Nullable
	private PathMatcher pathMatcher;

	@Nullable
	private Integer cacheLimit;

	@Nullable
	private String selectorHeaderName = "selector";

	@Nullable
	private TaskScheduler taskScheduler;

	@Nullable
	private long[] heartbeatValue;

	@Nullable
	private MessageHeaderInitializer headerInitializer;


	private SubscriptionRegistry subscriptionRegistry;

	private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

	@Nullable
	private ScheduledFuture<?> heartbeatFuture;


	/**
	 * 使用给定消息通道和目标前缀创建 SimpleBrokerMessageHandler 实例。
	 * @param clientInboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param clientOutboundChannel the channel for sending messages to clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for the application to send messages to the broker
	 * @param destinationPrefixes prefixes to use to filter out messages
	 */
	public SimpleBrokerMessageHandler(SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel,
			SubscribableChannel brokerChannel, Collection<String> destinationPrefixes) {

		super(clientInboundChannel, clientOutboundChannel, brokerChannel, destinationPrefixes);
		this.subscriptionRegistry = new DefaultSubscriptionRegistry();
	}


	/**
	 * 配置用于存储订阅的自定义 SubscriptionRegistry。
	 * <p><strong>注意</strong>，当通过 {@link #setPathMatcher} 配置自定义 PathMatcher 时，
	 * 如果自定义注册表不是 {@link DefaultSubscriptionRegistry} 实例，
	 * 则不会使用提供的 PathMatcher，必须直接在自定义注册表上进行配置。
	 */
	public void setSubscriptionRegistry(SubscriptionRegistry subscriptionRegistry) {
		Assert.notNull(subscriptionRegistry, "SubscriptionRegistry must not be null");
		this.subscriptionRegistry = subscriptionRegistry;
		initPathMatcherToUse();
		initCacheLimitToUse();
		initSelectorHeaderNameToUse();
	}

	public SubscriptionRegistry getSubscriptionRegistry() {
		return this.subscriptionRegistry;
	}

	/**
	 * 配置后，给定 PathMatcher 会传递给底层 SubscriptionRegistry，
	 * 用于将目标与订阅进行匹配。
	 * <p>默认使用标准的 {@link org.springframework.util.AntPathMatcher}。
	 * @since 4.1
	 * @see #setSubscriptionRegistry
	 * @see DefaultSubscriptionRegistry#setPathMatcher
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(@Nullable PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		initPathMatcherToUse();
	}

	private void initPathMatcherToUse() {
		if (this.pathMatcher != null && this.subscriptionRegistry instanceof DefaultSubscriptionRegistry) {
			((DefaultSubscriptionRegistry) this.subscriptionRegistry).setPathMatcher(this.pathMatcher);
		}
	}

	/**
	 * 配置后，指定的缓存限制会传递给底层 SubscriptionRegistry，覆盖其中的任何默认值。
	 * <p>使用标准的 {@link DefaultSubscriptionRegistry} 时，默认缓存限制为 1024。
	 * @since 4.3.2
	 * @see #setSubscriptionRegistry
	 * @see DefaultSubscriptionRegistry#setCacheLimit
	 * @see DefaultSubscriptionRegistry#DEFAULT_CACHE_LIMIT
	 */
	public void setCacheLimit(@Nullable Integer cacheLimit) {
		this.cacheLimit = cacheLimit;
		initCacheLimitToUse();
	}

	private void initCacheLimitToUse() {
		if (this.cacheLimit != null && this.subscriptionRegistry instanceof DefaultSubscriptionRegistry) {
			((DefaultSubscriptionRegistry) this.subscriptionRegistry).setCacheLimit(this.cacheLimit);
		}
	}

	/**
	 * 配置订阅消息可以带有的头名称，用于过滤与订阅匹配的消息。
	 * 该头值应为 Spring EL 布尔表达式，应用于与订阅匹配的消息头。
	 * <p>例如：
	 * <pre>
	 * headers.foo == 'bar'
	 * </pre>
	 * <p>默认设置为 "selector"。可以将其设置为其他名称，或设置为 {@code null}
	 * 以关闭对选择器头的支持。
	 * @param selectorHeaderName the name to use for a selector header
	 * @since 4.3.17
	 * @see #setSubscriptionRegistry
	 * @see DefaultSubscriptionRegistry#setSelectorHeaderName(String)
	 */
	public void setSelectorHeaderName(@Nullable String selectorHeaderName) {
		this.selectorHeaderName = selectorHeaderName;
		initSelectorHeaderNameToUse();
	}

	private void initSelectorHeaderNameToUse() {
		if (this.subscriptionRegistry instanceof DefaultSubscriptionRegistry) {
			((DefaultSubscriptionRegistry) this.subscriptionRegistry).setSelectorHeaderName(this.selectorHeaderName);
		}
	}

	/**
	 * 配置用于提供心跳支持的
	 * {@link org.springframework.scheduling.TaskScheduler}。设置此属性还会将
	 * {@link #setHeartbeatValue heartbeatValue} 设置为 "10000, 10000"。
	 * <p>默认不设置。
	 * @since 4.2
	 */
	public void setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		if (taskScheduler != null && this.heartbeatValue == null) {
			this.heartbeatValue = new long[] {10000, 10000};
		}
	}

	/**
	 * 返回已配置的 TaskScheduler。
	 * @since 4.2
	 */
	@Nullable
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * 配置心跳设置的值。第一个数字表示服务器写入或发送心跳的频率。
	 * 第二个数字表示客户端应写入的频率。0 表示不发送心跳。
	 * <p>默认设置为 "0, 0"，除非设置了 {@link #setTaskScheduler
	 * taskScheduler}，此时默认值变为 "10000,10000"（以毫秒为单位）。
	 * @since 4.2
	 */
	public void setHeartbeatValue(@Nullable long[] heartbeat) {
		if (heartbeat != null && (heartbeat.length != 2 || heartbeat[0] < 0 || heartbeat[1] < 0)) {
			throw new IllegalArgumentException("Invalid heart-beat: " + Arrays.toString(heartbeat));
		}
		this.heartbeatValue = heartbeat;
	}

	/**
	 * 心跳设置的已配置值。
	 * @since 4.2
	 */
	@Nullable
	public long[] getHeartbeatValue() {
		return this.heartbeatValue;
	}

	/**
	 * 配置要应用于发送到客户端出站通道的所有消息头的
	 * {@link MessageHeaderInitializer}。
	 * <p>默认情况下，不设置此属性。
	 * @since 4.1
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * 返回已配置的头初始化器。
	 * @since 4.1
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}


	@Override
	public void startInternal() {
		publishBrokerAvailableEvent();
		if (this.taskScheduler != null) {
			long interval = initHeartbeatTaskDelay();
			if (interval > 0) {
				this.heartbeatFuture = this.taskScheduler.scheduleWithFixedDelay(new HeartbeatTask(), interval);
			}
		}
		else {
			Assert.isTrue(getHeartbeatValue() == null ||
					(getHeartbeatValue()[0] == 0 && getHeartbeatValue()[1] == 0),
					"Heartbeat values configured but no TaskScheduler provided");
		}
	}

	private long initHeartbeatTaskDelay() {
		if (getHeartbeatValue() == null) {
			return 0;
		}
		else if (getHeartbeatValue()[0] > 0 && getHeartbeatValue()[1] > 0) {
			return Math.min(getHeartbeatValue()[0], getHeartbeatValue()[1]);
		}
		else {
			return (getHeartbeatValue()[0] > 0 ? getHeartbeatValue()[0] : getHeartbeatValue()[1]);
		}
	}

	@Override
	public void stopInternal() {
		publishBrokerUnavailableEvent();
		if (this.heartbeatFuture != null) {
			this.heartbeatFuture.cancel(true);
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		String destination = SimpMessageHeaderAccessor.getDestination(headers);
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);

		updateSessionReadTime(sessionId);

		if (!checkDestinationPrefix(destination)) {
			return;
		}

		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		if (SimpMessageType.MESSAGE.equals(messageType)) {
			logMessage(message);
			sendMessageToSubscribers(destination, message);
		}
		else if (SimpMessageType.CONNECT.equals(messageType)) {
			logMessage(message);
			if (sessionId != null) {
				if (this.sessions.get(sessionId) != null) {
					if (logger.isWarnEnabled()) {
						logger.warn("Ignoring CONNECT in session " + sessionId + ". Already connected.");
					}
					return;
				}
				long[] heartbeatIn = SimpMessageHeaderAccessor.getHeartbeat(headers);
				long[] heartbeatOut = getHeartbeatValue();
				Principal user = SimpMessageHeaderAccessor.getUser(headers);
				MessageChannel outChannel = getClientOutboundChannelForSession(sessionId);
				this.sessions.put(sessionId, new SessionInfo(sessionId, user, outChannel, heartbeatIn, heartbeatOut));
				SimpMessageHeaderAccessor connectAck = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
				initHeaders(connectAck);
				connectAck.setSessionId(sessionId);
				if (user != null) {
					connectAck.setUser(user);
				}
				connectAck.setHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER, message);
				connectAck.setHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER, heartbeatOut);
				Message<byte[]> messageOut = MessageBuilder.createMessage(EMPTY_PAYLOAD, connectAck.getMessageHeaders());
				getClientOutboundChannel().send(messageOut);
			}
		}
		else if (SimpMessageType.DISCONNECT.equals(messageType)) {
			logMessage(message);
			if (sessionId != null) {
				Principal user = SimpMessageHeaderAccessor.getUser(headers);
				handleDisconnect(sessionId, user, message);
			}
		}
		else if (SimpMessageType.SUBSCRIBE.equals(messageType)) {
			logMessage(message);
			this.subscriptionRegistry.registerSubscription(message);
		}
		else if (SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			logMessage(message);
			this.subscriptionRegistry.unregisterSubscription(message);
		}
	}

	private void updateSessionReadTime(@Nullable String sessionId) {
		if (sessionId != null) {
			SessionInfo info = this.sessions.get(sessionId);
			if (info != null) {
				info.setLastReadTime(System.currentTimeMillis());
			}
		}
	}

	private void logMessage(Message<?> message) {
		if (logger.isDebugEnabled()) {
			SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
			accessor = (accessor != null ? accessor : SimpMessageHeaderAccessor.wrap(message));
			logger.debug("Processing " + accessor.getShortLogMessage(message.getPayload()));
		}
	}

	private void initHeaders(SimpMessageHeaderAccessor accessor) {
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(accessor);
		}
	}

	private void handleDisconnect(String sessionId, @Nullable Principal user, @Nullable Message<?> origMessage) {
		this.sessions.remove(sessionId);
		this.subscriptionRegistry.unregisterAllSubscriptions(sessionId);
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT_ACK);
		accessor.setSessionId(sessionId);
		if (user != null) {
			accessor.setUser(user);
		}
		if (origMessage != null) {
			accessor.setHeader(SimpMessageHeaderAccessor.DISCONNECT_MESSAGE_HEADER, origMessage);
		}
		initHeaders(accessor);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
		getClientOutboundChannel().send(message);
	}

	protected void sendMessageToSubscribers(@Nullable String destination, Message<?> message) {
		MultiValueMap<String,String> subscriptions = this.subscriptionRegistry.findSubscriptions(message);
		if (!subscriptions.isEmpty() && logger.isDebugEnabled()) {
			logger.debug("Broadcasting to " + subscriptions.size() + " sessions.");
		}
		long now = System.currentTimeMillis();
		subscriptions.forEach((sessionId, subscriptionIds) -> {
			for (String subscriptionId : subscriptionIds) {
				SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
				initHeaders(headerAccessor);
				headerAccessor.setSessionId(sessionId);
				headerAccessor.setSubscriptionId(subscriptionId);
				headerAccessor.copyHeadersIfAbsent(message.getHeaders());
				headerAccessor.setLeaveMutable(true);
				Object payload = message.getPayload();
				Message<?> reply = MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
				SessionInfo info = this.sessions.get(sessionId);
				if (info != null) {
					try {
						info.getClientOutboundChannel().send(reply);
					}
					catch (Throwable ex) {
						if (logger.isErrorEnabled()) {
							logger.error("Failed to send " + message, ex);
						}
					}
					finally {
						info.setLastWriteTime(now);
					}
				}
			}
		});
	}

	@Override
	public String toString() {
		return "SimpleBrokerMessageHandler [" + this.subscriptionRegistry + "]";
	}


	private static class SessionInfo {

		/* STOMP spec: receiver SHOULD take into account an error margin */
		private static final long HEARTBEAT_MULTIPLIER = 3;

		private final String sessionId;

		@Nullable
		private final Principal user;

		private final MessageChannel clientOutboundChannel;

		private final long readInterval;

		private final long writeInterval;

		private volatile long lastReadTime;

		private volatile long lastWriteTime;


		public SessionInfo(String sessionId, @Nullable Principal user, MessageChannel outboundChannel,
				@Nullable long[] clientHeartbeat, @Nullable long[] serverHeartbeat) {

			this.sessionId = sessionId;
			this.user = user;
			this.clientOutboundChannel = outboundChannel;
			if (clientHeartbeat != null && serverHeartbeat != null) {
				this.readInterval = (clientHeartbeat[0] > 0 && serverHeartbeat[1] > 0 ?
						Math.max(clientHeartbeat[0], serverHeartbeat[1]) * HEARTBEAT_MULTIPLIER : 0);
				this.writeInterval = (clientHeartbeat[1] > 0 && serverHeartbeat[0] > 0 ?
						Math.max(clientHeartbeat[1], serverHeartbeat[0]) : 0);
			}
			else {
				this.readInterval = 0;
				this.writeInterval = 0;
			}
			this.lastReadTime = this.lastWriteTime = System.currentTimeMillis();
		}

		public String getSessionId() {
			return this.sessionId;
		}

		@Nullable
		public Principal getUser() {
			return this.user;
		}

		public MessageChannel getClientOutboundChannel() {
			return this.clientOutboundChannel;
		}

		public long getReadInterval() {
			return this.readInterval;
		}

		public long getWriteInterval() {
			return this.writeInterval;
		}

		public long getLastReadTime() {
			return this.lastReadTime;
		}

		public void setLastReadTime(long lastReadTime) {
			this.lastReadTime = lastReadTime;
		}

		public long getLastWriteTime() {
			return this.lastWriteTime;
		}

		public void setLastWriteTime(long lastWriteTime) {
			this.lastWriteTime = lastWriteTime;
		}
	}


	private class HeartbeatTask implements Runnable {

		@Override
		public void run() {
			long now = System.currentTimeMillis();
			for (SessionInfo info : sessions.values()) {
				if (info.getReadInterval() > 0 && (now - info.getLastReadTime()) > info.getReadInterval()) {
					handleDisconnect(info.getSessionId(), info.getUser(), null);
				}
				if (info.getWriteInterval() > 0 && (now - info.getLastWriteTime()) > info.getWriteInterval()) {
					SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.HEARTBEAT);
					accessor.setSessionId(info.getSessionId());
					Principal user = info.getUser();
					if (user != null) {
						accessor.setUser(user);
					}
					initHeaders(accessor);
					accessor.setLeaveMutable(true);
					MessageHeaders headers = accessor.getMessageHeaders();
					info.getClientOutboundChannel().send(MessageBuilder.createMessage(EMPTY_PAYLOAD, headers));
				}
			}
		}
	}

}
