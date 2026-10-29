/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jms.support;

import java.util.List;
import java.util.Map;

import javax.jms.Destination;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

/**
 * {@link org.springframework.messaging.support.MessageHeaderAccessor} 实现，
 * 提供对 JMS 特定消息头的访问。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class JmsMessageHeaderAccessor extends NativeMessageHeaderAccessor {

	protected JmsMessageHeaderAccessor(Map<String, List<String>> nativeHeaders) {
		super(nativeHeaders);
	}

	protected JmsMessageHeaderAccessor(Message<?> message) {
		super(message);
	}


	/**
	 * 返回 {@link JmsHeaders#CORRELATION_ID correlationId}。
	 * @see JmsHeaders#CORRELATION_ID
	 */
	@Nullable
	public String getCorrelationId() {
		return (String) getHeader(JmsHeaders.CORRELATION_ID);
	}

	/**
	 * 返回 {@link JmsHeaders#DESTINATION 目的地}。
	 * @see JmsHeaders#DESTINATION
	 */
	@Nullable
	public Destination getDestination() {
		return (Destination) getHeader(JmsHeaders.DESTINATION);
	}

	/**
	 * 返回 {@link JmsHeaders#DELIVERY_MODE 投递模式}。
	 * @see JmsHeaders#DELIVERY_MODE
	 */
	@Nullable
	public Integer getDeliveryMode() {
		return (Integer) getHeader(JmsHeaders.DELIVERY_MODE);
	}

	/**
	 * 返回消息的{@link JmsHeaders#EXPIRATION 过期时间}。
	 * @see JmsHeaders#EXPIRATION
	 */
	@Nullable
	public Long getExpiration() {
		return (Long) getHeader(JmsHeaders.EXPIRATION);
	}

	/**
	 * 返回 {@link JmsHeaders#MESSAGE_ID 消息 ID}。
	 * @see JmsHeaders#MESSAGE_ID
	 */
	@Nullable
	public String getMessageId() {
		return (String) getHeader(JmsHeaders.MESSAGE_ID);
	}

	/**
	 * 返回 {@link JmsHeaders#PRIORITY 优先级}。
	 * @see JmsHeaders#PRIORITY
	 */
	@Nullable
	public Integer getPriority() {
		return (Integer) getHeader(JmsHeaders.PRIORITY);
	}

	/**
	 * 返回 {@link JmsHeaders#REPLY_TO 回复地址}。
	 * @see JmsHeaders#REPLY_TO
	 */
	@Nullable
	public Destination getReplyTo() {
		return (Destination) getHeader(JmsHeaders.REPLY_TO);
	}

	/**
	 * 返回 {@link JmsHeaders#REDELIVERED 重新投递}标志。
	 * @see JmsHeaders#REDELIVERED
	 */
	@Nullable
	public Boolean getRedelivered() {
		return (Boolean) getHeader(JmsHeaders.REDELIVERED);
	}

	/**
	 * 返回 {@link JmsHeaders#TYPE 类型}。
	 * @see JmsHeaders#TYPE
	 */
	@Nullable
	public String getType() {
		return (String) getHeader(JmsHeaders.TYPE);
	}

	/**
	 * 返回 {@link JmsHeaders#TIMESTAMP 时间戳}。
	 * @see JmsHeaders#TIMESTAMP
	 */
	@Override
	@Nullable
	public Long getTimestamp() {
		return (Long) getHeader(JmsHeaders.TIMESTAMP);
	}


	// Static factory method

	/**
	 * 从现有消息的消息头创建 {@link JmsMessageHeaderAccessor}。
	 */
	public static JmsMessageHeaderAccessor wrap(Message<?> message) {
		return new JmsMessageHeaderAccessor(message);
	}

}
