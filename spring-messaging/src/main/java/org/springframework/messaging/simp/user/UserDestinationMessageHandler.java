/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.simp.user;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;

import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 支持“用户”目标的 {@code MessageHandler}。
 *
 * <p>监听带有“用户”目标的消息，将其目标转换为对应用户活跃会话唯一的实际目标，
 * 然后将解析后的消息发送到代理通道进行投递。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class UserDestinationMessageHandler implements MessageHandler, SmartLifecycle {

	private static final Log logger = SimpLogging.forLogName(UserDestinationMessageHandler.class);


	private final SubscribableChannel clientInboundChannel;

	private final SubscribableChannel brokerChannel;

	private final UserDestinationResolver destinationResolver;

	private final MessageSendingOperations<String> messagingTemplate;

	@Nullable
	private BroadcastHandler broadcastHandler;

	@Nullable
	private MessageHeaderInitializer headerInitializer;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	/**
	 * 使用给定的客户端通道和代理通道创建实例，订阅并处理来自各通道的消息，
	 * 然后将任何已解析的消息发送到代理通道。
	 * @param clientInboundChannel 从客户端接收的消息。
	 * @param brokerChannel 发送到 Broker 的消息。
	 * @param resolver "用户"目标地址的解析器。
	 */
	public UserDestinationMessageHandler(SubscribableChannel clientInboundChannel,
			SubscribableChannel brokerChannel, UserDestinationResolver resolver) {

		Assert.notNull(clientInboundChannel, "'clientInChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");
		Assert.notNull(resolver, "resolver must not be null");

		this.clientInboundChannel = clientInboundChannel;
		this.brokerChannel = brokerChannel;
		this.messagingTemplate = new SimpMessagingTemplate(brokerChannel);
		this.destinationResolver = resolver;
	}


	/**
	 * 返回已配置的 {@link UserDestinationResolver}。
	 */
	public UserDestinationResolver getUserDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * 设置一个目标，用于广播因用户未连接而仍未解析的消息。在多应用服务器场景中，
	 * 这为其他应用服务器提供了尝试处理的机会。
	 * <p>默认不设置。
	 * @param destination 目标地址。
	 */
	public void setBroadcastDestination(@Nullable String destination) {
		this.broadcastHandler = (StringUtils.hasText(destination) ?
				new BroadcastHandler(this.messagingTemplate, destination) : null);
	}

	/**
	 * 返回已配置的未解析消息目标。
	 */
	@Nullable
	public String getBroadcastDestination() {
		return (this.broadcastHandler != null ? this.broadcastHandler.getBroadcastDestination() : null);
	}

	/**
	 * 返回用于将已解析消息发送到代理通道的消息模板。
	 */
	public MessageSendingOperations<String> getBrokerMessagingTemplate() {
		return this.messagingTemplate;
	}

	/**
	 * 配置自定义 {@link MessageHeaderInitializer}，以初始化已解析目标消息的头。
	 * <p>默认不设置。
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * 返回已配置的头初始化器。
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}


	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			this.clientInboundChannel.subscribe(this);
			this.brokerChannel.subscribe(this);
			this.running = true;
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.clientInboundChannel.unsubscribe(this);
			this.brokerChannel.unsubscribe(this);
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
	public void handleMessage(Message<?> message) throws MessagingException {
		Message<?> messageToUse = message;
		if (this.broadcastHandler != null) {
			messageToUse = this.broadcastHandler.preHandle(message);
			if (messageToUse == null) {
				return;
			}
		}

		UserDestinationResult result = this.destinationResolver.resolveDestination(messageToUse);
		if (result == null) {
			return;
		}

		if (result.getTargetDestinations().isEmpty()) {
			if (logger.isTraceEnabled()) {
				logger.trace("No active sessions for user destination: " + result.getSourceDestination());
			}
			if (this.broadcastHandler != null) {
				this.broadcastHandler.handleUnresolved(messageToUse);
			}
			return;
		}

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(messageToUse);
		initHeaders(accessor);
		accessor.setNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, result.getSubscribeDestination());
		accessor.setLeaveMutable(true);

		messageToUse = MessageBuilder.createMessage(messageToUse.getPayload(), accessor.getMessageHeaders());
		if (logger.isTraceEnabled()) {
			logger.trace("Translated " + result.getSourceDestination() + " -> " + result.getTargetDestinations());
		}
		for (String target : result.getTargetDestinations()) {
			this.messagingTemplate.send(target, messageToUse);
		}
	}

	private void initHeaders(SimpMessageHeaderAccessor headerAccessor) {
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(headerAccessor);
		}
	}

	@Override
	public String toString() {
		return "UserDestinationMessageHandler[" + this.destinationResolver + "]";
	}


	/**
	 * 一个处理器，将本地未解析的消息广播到代理，并处理从代理收到的类似广播。
	 */
	private static class BroadcastHandler {

		private static final List<String> NO_COPY_LIST = Arrays.asList("subscription", "message-id");

		private final MessageSendingOperations<String> messagingTemplate;

		private final String broadcastDestination;

		public BroadcastHandler(MessageSendingOperations<String> template, String destination) {
			this.messagingTemplate = template;
			this.broadcastDestination = destination;
		}

		public String getBroadcastDestination() {
			return this.broadcastDestination;
		}

		@Nullable
		public Message<?> preHandle(Message<?> message) throws MessagingException {
			String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
			if (!getBroadcastDestination().equals(destination)) {
				return message;
			}
			SimpMessageHeaderAccessor accessor =
					SimpMessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
			Assert.state(accessor != null, "No SimpMessageHeaderAccessor");
			if (accessor.getSessionId() == null) {
				// Our own broadcast
				return null;
			}
			destination = accessor.getFirstNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION);
			if (logger.isTraceEnabled()) {
				logger.trace("Checking unresolved user destination: " + destination);
			}
			SimpMessageHeaderAccessor newAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
			for (String name : accessor.toNativeHeaderMap().keySet()) {
				if (NO_COPY_LIST.contains(name)) {
					continue;
				}
				newAccessor.setNativeHeader(name, accessor.getFirstNativeHeader(name));
			}
			if (destination != null) {
				newAccessor.setDestination(destination);
			}
			newAccessor.setHeader(SimpMessageHeaderAccessor.IGNORE_ERROR, true); // ensure send doesn't block
			return MessageBuilder.createMessage(message.getPayload(), newAccessor.getMessageHeaders());
		}

		public void handleUnresolved(Message<?> message) {
			MessageHeaders headers = message.getHeaders();
			if (SimpMessageHeaderAccessor.getFirstNativeHeader(
					SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, headers) != null) {
				// Re-broadcast
				return;
			}
			SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
			String destination = accessor.getDestination();
			accessor.setNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, destination);
			accessor.setLeaveMutable(true);
			message = MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
			if (logger.isTraceEnabled()) {
				logger.trace("Translated " + destination + " -> " + getBroadcastDestination());
			}
			this.messagingTemplate.send(getBroadcastDestination(), message);
		}
	}

}
