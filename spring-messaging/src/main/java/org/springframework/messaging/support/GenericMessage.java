/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.Serializable;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 带有泛型负载的 {@link Message} 实现。一旦创建，GenericMessage 就是不可变的。
 *
 * @author Mark Fisher
 * @since 4.0
 * @param <T> 负载类型
 * @see MessageBuilder
 */
public class GenericMessage<T> implements Message<T>, Serializable {

	private static final long serialVersionUID = 4268801052358035098L;


	private final T payload;

	private final MessageHeaders headers;


	/**
	 * 使用给定负载创建新的消息。
	 * @param payload 消息负载（永不为 {@code null}）
	 */
	public GenericMessage(T payload) {
		this(payload, new MessageHeaders(null));
	}

	/**
	 * 使用给定负载和头部创建新的消息。将复制给定头部映射的内容。
	 * @param payload 消息负载（永不为 {@code null}）
	 * @param headers 用于初始化的消息头
	 */
	public GenericMessage(T payload, Map<String, Object> headers) {
		this(payload, new MessageHeaders(headers));
	}

	/**
	 * 使用给定的 {@link MessageHeaders} 实例构造。
	 * <p><strong>注意：</strong>给定的 {@code MessageHeaders} 实例会直接用于
	 * 新消息中，即不会被复制。
	 * @param payload 消息负载（永不为 {@code null}）
	 * @param headers 消息头
	 */
	public GenericMessage(T payload, MessageHeaders headers) {
		Assert.notNull(payload, "Payload must not be null");
		Assert.notNull(headers, "MessageHeaders must not be null");
		this.payload = payload;
		this.headers = headers;
	}


	@Override
	public T getPayload() {
		return this.payload;
	}

	@Override
	public MessageHeaders getHeaders() {
		return this.headers;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof GenericMessage)) {
			return false;
		}
		GenericMessage<?> otherMsg = (GenericMessage<?>) other;
		// Using nullSafeEquals for proper array equals comparisons
		return (ObjectUtils.nullSafeEquals(this.payload, otherMsg.payload) && this.headers.equals(otherMsg.headers));
	}

	@Override
	public int hashCode() {
		// Using nullSafeHashCode for proper array hashCode handling
		return (ObjectUtils.nullSafeHashCode(this.payload) * 23 + this.headers.hashCode());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(" [payload=");
		if (this.payload instanceof byte[]) {
			sb.append("byte[").append(((byte[]) this.payload).length).append(']');
		}
		else {
			sb.append(this.payload);
		}
		sb.append(", headers=").append(this.headers).append(']');
		return sb.toString();
	}

}
