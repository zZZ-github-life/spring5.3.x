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

package org.springframework.messaging.simp;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.messaging.simp.SimpMessageSendingOperations} 的实现。
 *
 * <p>还提供向用户发送消息的方法。有关用户目标的更多信息，请参阅
 * {@link org.springframework.messaging.simp.user.UserDestinationResolver
 * UserDestinationResolver}。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpMessagingTemplate extends AbstractMessageSendingTemplate<String>
		implements SimpMessageSendingOperations {

	private final MessageChannel messageChannel;

	private String destinationPrefix = "/user/";

	private volatile long sendTimeout = -1;

	@Nullable
	private MessageHeaderInitializer headerInitializer;


	/**
	 * 创建新的 {@link SimpMessagingTemplate} 实例。
	 * @param messageChannel 消息通道（永不为 {@code null}）
	 */
	public SimpMessagingTemplate(MessageChannel messageChannel) {
		Assert.notNull(messageChannel, "MessageChannel must not be null");
		this.messageChannel = messageChannel;
	}


	/**
	 * 返回已配置的消息通道。
	 */
	public MessageChannel getMessageChannel() {
		return this.messageChannel;
	}

	/**
	 * 配置用于面向特定用户的目标前缀。
	 * <p>默认值为 "/user/"。
	 * @see org.springframework.messaging.simp.user.UserDestinationMessageHandler
	 */
	public void setUserDestinationPrefix(String prefix) {
		Assert.hasText(prefix, "User destination prefix must not be empty");
		this.destinationPrefix = (prefix.endsWith("/") ? prefix : prefix + "/");

	}

	/**
	 * 返回已配置的用户目标前缀。
	 */
	public String getUserDestinationPrefix() {
		return this.destinationPrefix;
	}

	/**
	 * 指定发送操作使用的超时值（以毫秒为单位）。
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * 返回已配置的发送超时（以毫秒为单位）。
	 */
	public long getSendTimeout() {
		return this.sendTimeout;
	}

	/**
	 * 配置要应用于通过 {@code SimpMessagingTemplate} 创建的所有消息头的
	 * {@link MessageHeaderInitializer}。
	 * <p>默认情况下，不设置此属性。
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


	/**
	 * 如果给定消息的头中已经包含
	 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor#DESTINATION_HEADER
	 * SimpMessageHeaderAccessor#DESTINATION_HEADER}，则消息将被直接发送，不再做进一步修改。
	 * <p>如果目标头尚不存在，则将消息发送到已配置的
	 * {@link #setDefaultDestination(Object) defaultDestination}；
	 * 如果未配置该目标，则抛出 {@code IllegalStateException}。
	 * @param message 要发送的消息（永不为 {@code null}）
	 */
	@Override
	public void send(Message<?> message) {
		Assert.notNull(message, "Message must not be null");
		String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
		if (destination != null) {
			sendInternal(message);
			return;
		}
		doSend(getRequiredDefaultDestination(), message);
	}

	@Override
	protected void doSend(String destination, Message<?> message) {
		Assert.notNull(destination, "Destination must not be null");

		SimpMessageHeaderAccessor simpAccessor =
				MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);

		if (simpAccessor != null) {
			if (simpAccessor.isMutable()) {
				simpAccessor.setDestination(destination);
				simpAccessor.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
				simpAccessor.setImmutable();
				sendInternal(message);
				return;
			}
			else {
				// Try and keep the original accessor type
				simpAccessor = (SimpMessageHeaderAccessor) MessageHeaderAccessor.getMutableAccessor(message);
				initHeaders(simpAccessor);
			}
		}
		else {
			simpAccessor = SimpMessageHeaderAccessor.wrap(message);
			initHeaders(simpAccessor);
		}

		simpAccessor.setDestination(destination);
		simpAccessor.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
		message = MessageBuilder.createMessage(message.getPayload(), simpAccessor.getMessageHeaders());
		sendInternal(message);
	}

	private void sendInternal(Message<?> message) {
		String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
		Assert.notNull(destination, "Destination header required");

		long timeout = this.sendTimeout;
		boolean sent = (timeout >= 0 ? this.messageChannel.send(message, timeout) : this.messageChannel.send(message));

		if (!sent) {
			throw new MessageDeliveryException(message,
					"Failed to send message to destination '" + destination + "' within timeout: " + timeout);
		}
	}

	private void initHeaders(SimpMessageHeaderAccessor simpAccessor) {
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(simpAccessor);
		}
	}


	@Override
	public void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException {
		convertAndSendToUser(user, destination, payload, (MessagePostProcessor) null);
	}

	@Override
	public void convertAndSendToUser(String user, String destination, Object payload,
			@Nullable Map<String, Object> headers) throws MessagingException {

		convertAndSendToUser(user, destination, payload, headers, null);
	}

	@Override
	public void convertAndSendToUser(String user, String destination, Object payload,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException {

		convertAndSendToUser(user, destination, payload, null, postProcessor);
	}

	@Override
	public void convertAndSendToUser(String user, String destination, Object payload,
			@Nullable Map<String, Object> headers, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException {

		Assert.notNull(user, "User must not be null");
		String username = user;
		Assert.isTrue(!user.contains("%2F"), () -> "Invalid sequence \"%2F\" in user name: " + username);
		user = StringUtils.replace(user, "/", "%2F");
		destination = destination.startsWith("/") ? destination : "/" + destination;
		super.convertAndSend(this.destinationPrefix + user + destination, payload, headers, postProcessor);
	}


	/**
	 * 创建一个新映射，并将给定头放在键
	 * {@link NativeMessageHeaderAccessor#NATIVE_HEADERS NATIVE_HEADERS NATIVE_HEADERS NATIVE_HEADERS}
	 * 下，从而有效地将输入头映射视为要发送到目标的外部头。
	 * <p>不过，如果给定头已经包含键
	 * {@code NATIVE_HEADERS NATIVE_HEADERS}，则返回同一个头实例，不做任何更改。
	 * <p>此外，如果给定头是使用
	 * {@link SimpMessageHeaderAccessor#getMessageHeaders()} 准备并获取的，
	 * 也会返回同一个头实例，不做任何更改。
	 */
	@Override
	protected Map<String, Object> processHeadersToSend(@Nullable Map<String, Object> headers) {
		if (headers == null) {
			SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
			initHeaders(headerAccessor);
			headerAccessor.setLeaveMutable(true);
			return headerAccessor.getMessageHeaders();
		}
		if (headers.containsKey(NativeMessageHeaderAccessor.NATIVE_HEADERS)) {
			return headers;
		}
		if (headers instanceof MessageHeaders) {
			SimpMessageHeaderAccessor accessor =
					MessageHeaderAccessor.getAccessor((MessageHeaders) headers, SimpMessageHeaderAccessor.class);
			if (accessor != null) {
				return headers;
			}
		}

		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		initHeaders(headerAccessor);
		headers.forEach((key, value) -> headerAccessor.setNativeHeader(key, (value != null ? value.toString() : null)));
		return headerAccessor.getMessageHeaders();
	}

}
