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

import java.nio.ByteBuffer;

/**
 * 二进制 WebSocket 消息。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class BinaryMessage extends AbstractWebSocketMessage<ByteBuffer> {

	/**
	 * 使用给定的 ByteBuffer 负载创建一个新的二进制 WebSocket 消息。
	 * @param payload 非 null 的负载
	 */
	public BinaryMessage(ByteBuffer payload) {
		super(payload, true);
	}

	/**
	 * 使用表示完整或部分消息内容的给定负载创建一个新的二进制 WebSocket 消息。
	 * 当 {@code isLast} 布尔标志设置为 {@code false} 时，
	 * 该消息将作为部分内容发送，并且会预期收到更多部分消息，
	 * 直到该布尔标志设置为 {@code true} 为止。
	 * @param payload 非 null 的负载
	 * @param isLast 消息是否为一系列部分消息中的最后一条
	 */
	public BinaryMessage(ByteBuffer payload, boolean isLast) {
		super(payload, isLast);
	}

	/**
	 * 使用给定的 byte[] 负载创建一个新的二进制 WebSocket 消息。
	 * @param payload 非 null 的负载；注意此值不会被复制，
	 * 因此必须小心不要修改该数组。
	 */
	public BinaryMessage(byte[] payload) {
		this(payload, true);
	}

	/**
	 * 使用表示完整或部分消息内容的给定 byte[] 负载创建一个新的二进制 WebSocket 消息。
	 * 当 {@code isLast} 布尔标志设置为 {@code false} 时，
	 * 该消息将作为部分内容发送，并且会预期收到更多部分消息，
	 * 直到该布尔标志设置为 {@code true} 为止。
	 * @param payload 非 null 的负载；注意此值不会被复制，
	 * 因此必须小心不要修改该数组。
	 * @param isLast 消息是否为一系列部分消息中的最后一条
	 */
	public BinaryMessage(byte[] payload, boolean isLast) {
		this(payload, 0, payload.length, isLast);
	}

	/**
	 * 通过包装现有字节数组创建一个新的二进制 WebSocket 消息。
	 * @param payload 非 null 的负载；注意此值不会被复制，
	 * 因此必须小心不要修改该数组。
	 * @param offset 负载在数组中起始处的偏移量
	 * @param length 参与构成负载的数组长度
	 * @param isLast 消息是否为一系列部分消息中的最后一条
	 */
	public BinaryMessage(byte[] payload, int offset, int length, boolean isLast) {
		super(ByteBuffer.wrap(payload, offset, length), isLast);
	}


	@Override
	public int getPayloadLength() {
		return getPayload().remaining();
	}

	@Override
	protected String toStringPayload() {
		return getPayload().toString();
	}

}
