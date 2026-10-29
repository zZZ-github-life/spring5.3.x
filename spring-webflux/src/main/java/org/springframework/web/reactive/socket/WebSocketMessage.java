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

package org.springframework.web.reactive.socket;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * WebSocket 消息的描述。
 *
 * <p>有关使用会话的 {@link org.springframework.core.io.buffer.DataBufferFactory}
 * 创建消息的信息，请参见 {@link WebSocketSession} 中的静态工厂方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebSocketMessage {

	private final Type type;

	private final DataBuffer payload;

	@Nullable
	private final Object nativeMessage;


	/**
	 * WebSocketMessage 的构造函数。
	 * <p>请参见 {@link WebSocketSession} 中的静态工厂方法，或者使用
	 * {@link WebSocketSession#bufferFactory()} 创建负载，然后调用此构造函数。
	 */
	public WebSocketMessage(Type type, DataBuffer payload) {
		this(type, payload, null);
	}

	/**
	 * 用于入站消息并可以访问底层消息的构造函数。
	 * @param type the type of WebSocket message
	 * @param payload the message content
	 * @param nativeMessage the message from the API of the underlying WebSocket
	 * library, if applicable.
	 * @since 5.3
	 */
	public WebSocketMessage(Type type, DataBuffer payload, @Nullable Object nativeMessage) {
		Assert.notNull(type, "'type' must not be null");
		Assert.notNull(payload, "'payload' must not be null");
		this.type = type;
		this.payload = payload;
		this.nativeMessage = nativeMessage;
	}

	/**
	 * 返回消息类型（文本、二进制等）。
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * 返回消息负载。
	 */
	public DataBuffer getPayload() {
		return this.payload;
	}

	/**
	 * 返回底层 WebSocket 库 API 中的消息。这仅适用于入站消息，并且当底层消息
	 * 除内容之外还有其他字段时。目前只有 Reactor Netty 属于这种情况。
	 * @param <T> the type to cast the underlying message to
	 * @return the underlying message, or {@code null}
	 * @since 5.3
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getNativeMessage() {
		return (T) this.nativeMessage;
	}

	/**
	 * {@link #getPayloadAsText(Charset)} 的一个变体，使用 {@code UTF-8}
	 * 将原始内容解码为文本。
	 */
	public String getPayloadAsText() {
		return getPayloadAsText(StandardCharsets.UTF_8);
	}

	/**
	 * 使用给定的字符编码将消息原始内容解码为文本的快捷方式。这对于文本 WebSocket
	 * 消息很有用，或者在负载预期包含文本的其他情况下也很有用。
	 * @param charset the character encoding
	 * @since 5.0.5
	 */
	public String getPayloadAsText(Charset charset) {
		return this.payload.toString(charset);
	}

	/**
	 * 保留消息负载的数据缓冲区，这在具有池化缓冲区的运行时（例如 Netty）中很有用。
	 * 是以下代码的快捷方式：
	 * <pre>
	 * DataBuffer payload = message.getPayload();
	 * DataBufferUtils.retain(payload);
	 * </pre>
	 * @see DataBufferUtils#retain(DataBuffer)
	 */
	public WebSocketMessage retain() {
		DataBufferUtils.retain(this.payload);
		return this;
	}

	/**
	 * 释放负载 {@code DataBuffer}，这在具有池化缓冲区的运行时（例如 Netty）中
	 * 很有用。是以下代码的快捷方式：
	 * <pre>
	 * DataBuffer payload = message.getPayload();
	 * DataBufferUtils.release(payload);
	 * </pre>
	 * @see DataBufferUtils#release(DataBuffer)
	 */
	public void release() {
		DataBufferUtils.release(this.payload);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WebSocketMessage)) {
			return false;
		}
		WebSocketMessage otherMessage = (WebSocketMessage) other;
		return (this.type.equals(otherMessage.type) &&
				ObjectUtils.nullSafeEquals(this.payload, otherMessage.payload));
	}

	@Override
	public int hashCode() {
		return this.type.hashCode() * 29 + this.payload.hashCode();
	}

	@Override
	public String toString() {
		return "WebSocket " + this.type.name() + " message (" + this.payload.readableByteCount() + " bytes)";
	}


	/**
	 * WebSocket 消息类型。
	 */
	public enum Type {
		/**
		 * 文本 WebSocket 消息。
		 */
		TEXT,
		/**
		 * 二进制 WebSocket 消息。
		 */
		BINARY,
		/**
		 * WebSocket ping。
		 */
		PING,
		/**
		 * WebSocket pong。
		 */
		PONG
	}

}
