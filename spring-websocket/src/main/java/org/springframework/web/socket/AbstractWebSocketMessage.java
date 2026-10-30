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

package org.springframework.web.socket;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 可以在 WebSocket 连接上处理或发送的消息。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <T> 负载类型
 */
public abstract class AbstractWebSocketMessage<T> implements WebSocketMessage<T> {

	private final T payload;

	private final boolean last;


	/**
	 * 使用给定的负载创建一个新的 WebSocket 消息。
	 * @param payload 非 null 的负载
	 */
	AbstractWebSocketMessage(T payload) {
		this(payload, true);
	}

	/**
	 * 使用表示完整或部分消息内容的负载创建一个新的 WebSocket 消息。
	 * 当 {@code isLast} 布尔标志设置为 {@code false} 时，
	 * 该消息将作为部分内容发送，并且会预期收到更多部分消息，
	 * 直到该布尔标志设置为 {@code true} 为止。
	 * @param payload 非 null 的负载
	 * @param isLast 消息是否为一系列部分消息中的最后一条
	 */
	AbstractWebSocketMessage(T payload, boolean isLast) {
		Assert.notNull(payload, "payload must not be null");
		this.payload = payload;
		this.last = isLast;
	}


	/**
	 * 返回消息负载（永远不为 {@code null}）。
	 */
	@Override
	public T getPayload() {
		return this.payload;
	}

	/**
	 * 判断这是否是以一系列部分消息形式发送的消息的最后一部分。
	 */
	@Override
	public boolean isLast() {
		return this.last;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractWebSocketMessage)) {
			return false;
		}
		AbstractWebSocketMessage<?> otherMessage = (AbstractWebSocketMessage<?>) other;
		return ObjectUtils.nullSafeEquals(this.payload, otherMessage.payload);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.payload);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " payload=[" + toStringPayload() +
				"], byteCount=" + getPayloadLength() + ", last=" + isLast() + "]";
	}

	protected abstract String toStringPayload();

}
