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

package org.springframework.messaging.support;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * 用于创建 {@link GenericMessage} 的构建器（如果负载类型为 {@link Throwable}，
 * 则创建 {@link ErrorMessage}）。
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <T> 消息负载类型
 * @see GenericMessage
 * @see ErrorMessage
 */
public final class MessageBuilder<T> {

	private final T payload;

	@Nullable
	private final Message<T> providedMessage;

	private MessageHeaderAccessor headerAccessor;


	private MessageBuilder(Message<T> providedMessage) {
		Assert.notNull(providedMessage, "Message must not be null");
		this.payload = providedMessage.getPayload();
		this.providedMessage = providedMessage;
		this.headerAccessor = new MessageHeaderAccessor(providedMessage);
	}

	private MessageBuilder(T payload, MessageHeaderAccessor accessor) {
		Assert.notNull(payload, "Payload must not be null");
		Assert.notNull(accessor, "MessageHeaderAccessor must not be null");
		this.payload = payload;
		this.providedMessage = null;
		this.headerAccessor = accessor;
	}


	/**
	 * 通过提供 {@code MessageHeaderAccessor} 来设置要使用的消息头部。
	 * @param accessor 要使用的消息头
	 */
	public MessageBuilder<T> setHeaders(MessageHeaderAccessor accessor) {
		Assert.notNull(accessor, "MessageHeaderAccessor must not be null");
		this.headerAccessor = accessor;
		return this;
	}

	/**
	 * 设置给定头部名称的值。如果提供的值为 {@code null}，则将移除该头部。
	 */
	public MessageBuilder<T> setHeader(String headerName, @Nullable Object headerValue) {
		this.headerAccessor.setHeader(headerName, headerValue);
		return this;
	}

	/**
	 * 仅当给定头部名称尚未关联值时，才设置该头部名称的值。
	 */
	public MessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		this.headerAccessor.setHeaderIfAbsent(headerName, headerValue);
		return this;
	}

	/**
	 * 移除通过 'headerPatterns' 数组提供的所有头部。顾名思义，该数组可以包含
	 * 头部名称的简单匹配模式。支持的样式包括："xxx*"、"*xxx"、"*xxx*" 和
	 * "xxx*yyy"。
	 */
	public MessageBuilder<T> removeHeaders(String... headerPatterns) {
		this.headerAccessor.removeHeaders(headerPatterns);
		return this;
	}

	/**
	 * 移除给定头部名称的值。
	 */
	public MessageBuilder<T> removeHeader(String headerName) {
		this.headerAccessor.removeHeader(headerName);
		return this;
	}

	/**
	 * 从提供的 Map 复制名称-值对。此操作将覆盖任何现有值。可使用
	 * {@link #copyHeadersIfAbsent(Map)} 避免覆盖值。请注意，'id' 和 'timestamp'
	 * 头部值永远不会被覆盖。
	 */
	public MessageBuilder<T> copyHeaders(@Nullable Map<String, ?> headersToCopy) {
		this.headerAccessor.copyHeaders(headersToCopy);
		return this;
	}

	/**
	 * 从提供的 Map 复制名称-值对。此操作<em>不会</em>覆盖任何现有值。
	 */
	public MessageBuilder<T> copyHeadersIfAbsent(@Nullable Map<String, ?> headersToCopy) {
		this.headerAccessor.copyHeadersIfAbsent(headersToCopy);
		return this;
	}

	public MessageBuilder<T> setReplyChannel(MessageChannel replyChannel) {
		this.headerAccessor.setReplyChannel(replyChannel);
		return this;
	}

	public MessageBuilder<T> setReplyChannelName(String replyChannelName) {
		this.headerAccessor.setReplyChannelName(replyChannelName);
		return this;
	}

	public MessageBuilder<T> setErrorChannel(MessageChannel errorChannel) {
		this.headerAccessor.setErrorChannel(errorChannel);
		return this;
	}

	public MessageBuilder<T> setErrorChannelName(String errorChannelName) {
		this.headerAccessor.setErrorChannelName(errorChannelName);
		return this;
	}

	@SuppressWarnings("unchecked")
	public Message<T> build() {
		if (this.providedMessage != null && !this.headerAccessor.isModified()) {
			return this.providedMessage;
		}
		MessageHeaders headersToUse = this.headerAccessor.toMessageHeaders();
		if (this.payload instanceof Throwable) {
			if (this.providedMessage != null && this.providedMessage instanceof ErrorMessage) {
				Message<?> message = ((ErrorMessage) this.providedMessage).getOriginalMessage();
				if (message != null) {
					return (Message<T>) new ErrorMessage((Throwable) this.payload, headersToUse, message);
				}
			}
			return (Message<T>) new ErrorMessage((Throwable) this.payload, headersToUse);
		}
		else {
			return new GenericMessage<>(this.payload, headersToUse);
		}
	}


	/**
	 * 为新的 {@link Message} 实例创建构建器，并预先填充从所提供消息复制的所有头部。
	 * 所提供 Message 的负载也将用作新消息的负载。
	 * <p>如果提供的消息是 {@link ErrorMessage}，则其中包含的
	 * {@link ErrorMessage#getOriginalMessage() originalMessage} 将传递给新实例。
	 * @param message 将从中复制负载和所有消息头的 Message
	 */
	public static <T> MessageBuilder<T> fromMessage(Message<T> message) {
		return new MessageBuilder<>(message);
	}

	/**
	 * 使用给定负载为消息创建新的构建器。
	 * @param payload 负载
	 */
	public static <T> MessageBuilder<T> withPayload(T payload) {
		return new MessageBuilder<>(payload, new MessageHeaderAccessor());
	}

	/**
	 * 使用给定负载和 {@code MessageHeaders} 创建消息的快捷工厂方法。
	 * <p><strong>注意：</strong>给定的 {@code MessageHeaders} 实例会直接用于
	 * 新消息中，即不会被复制。
	 * @param payload 要使用的负载（永不为 {@code null}）
	 * @param messageHeaders 要使用的消息头（永不为 {@code null}）
	 * @return 创建的消息
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> Message<T> createMessage(@Nullable T payload, MessageHeaders messageHeaders) {
		Assert.notNull(payload, "Payload must not be null");
		Assert.notNull(messageHeaders, "MessageHeaders must not be null");
		if (payload instanceof Throwable) {
			return (Message<T>) new ErrorMessage((Throwable) payload, messageHeaders);
		}
		else {
			return new GenericMessage<>(payload, messageHeaders);
		}
	}

}
