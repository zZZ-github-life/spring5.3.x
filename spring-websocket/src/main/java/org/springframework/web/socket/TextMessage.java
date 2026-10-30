/*
 * Copyright 2002-2017 the original author or authors.
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

import java.nio.charset.StandardCharsets;

import org.springframework.lang.Nullable;

/**
 * 文本 WebSocket 消息。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class TextMessage extends AbstractWebSocketMessage<String> {

	@Nullable
	private final byte[] bytes;


	/**
	 * 使用给定的 CharSequence 负载创建一个新的文本 WebSocket 消息。
	 * @param payload 非 null 的负载
	 */
	public TextMessage(CharSequence payload) {
		super(payload.toString(), true);
		this.bytes = null;
	}

	/**
	 * 使用给定的 byte[] 创建一个新的文本 WebSocket 消息。假定
	 * 该字节数组可以编码为 UTF-8 字符串。
	 * @param payload 非 null 的负载
	 */
	public TextMessage(byte[] payload) {
		super(new String(payload, StandardCharsets.UTF_8));
		this.bytes = payload;
	}

	/**
	 * 使用表示完整或部分消息内容的给定负载创建一个新的文本 WebSocket 消息。
	 * 当 {@code isLast} 布尔标志设置为 {@code false} 时，
	 * 该消息将作为部分内容发送，并且会预期收到更多部分消息，
	 * 直到该布尔标志设置为 {@code true} 为止。
	 * @param payload 非 null 的负载
	 * @param isLast 这是否为一系列部分消息中的最后一部分
	 */
	public TextMessage(CharSequence payload, boolean isLast) {
		super(payload.toString(), isLast);
		this.bytes = null;
	}


	@Override
	public int getPayloadLength() {
		return asBytes().length;
	}

	public byte[] asBytes() {
		return (this.bytes != null ? this.bytes : getPayload().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	protected String toStringPayload() {
		String payload = getPayload();
		return (payload.length() > 10 ? payload.substring(0, 10) + ".." : payload);
	}

}
