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

package org.springframework.messaging.simp.stomp;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link org.springframework.messaging.simp.stomp.StompDecoder} 的扩展，
 * 在父类从中读取所有（完整的）STOMP 帧后，缓冲输入 ByteBuffer 中剩余的内容。
 * 剩余内容表示一个不完整的 STOMP 帧。当使用额外数据反复调用时，
 * decode 方法会返回一个或多个消息，或者如果数据仍不足，则继续缓冲。
 *
 * <p>只要解码不失败，就可以重复调用此解码器的单个实例来读取单个流
 * （例如 WebSocket 会话）中的所有消息。如果发生异常，则不应再使用
 * StompDecoder 实例，因为无法保证其内部状态一致。
 * 此时预期底层会话已关闭。
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 * @see StompDecoder
 */
public class BufferingStompDecoder {

	private final StompDecoder stompDecoder;

	private final int bufferSizeLimit;

	private final Queue<ByteBuffer> chunks = new LinkedBlockingQueue<>();

	@Nullable
	private volatile Integer expectedContentLength;


	/**
	 * 创建一个包装给定 {@code StompDecoder} 的新 {@code BufferingStompDecoder}。
	 * @param stompDecoder 要包装的目标解码器
	 * @param bufferSizeLimit 缓冲区大小限制
	 */
	public BufferingStompDecoder(StompDecoder stompDecoder, int bufferSizeLimit) {
		Assert.notNull(stompDecoder, "StompDecoder is required");
		Assert.isTrue(bufferSizeLimit > 0, "Buffer size limit must be greater than 0");
		this.stompDecoder = stompDecoder;
		this.bufferSizeLimit = bufferSizeLimit;
	}


	/**
	 * 返回被包装的 {@link StompDecoder}。
	 */
	public final StompDecoder getStompDecoder() {
		return this.stompDecoder;
	}

	/**
	 * 返回已配置的缓冲区大小限制。
	 */
	public final int getBufferSizeLimit() {
		return this.bufferSizeLimit;
	}


	/**
	 * 将给定 {@code ByteBuffer} 中的一个或多个 STOMP 帧解码为
	 * {@link Message Messages} 列表。
	 * <p>如果有足够的数据解析 "content-length" 头，则使用该值确定
	 * 在再次尝试解码之前还需要多少数据。
	 * <p>如果没有足够的数据解析 "content-length"，或者不存在 "content-length" 头，
	 * 则后续每次 decode 调用都会尝试使用所有可用数据再次解析。
	 * 因此，"content-length" 头的存在有助于优化大消息的解码。
	 * @param newBuffer 包含待解码新数据的缓冲区
	 * @return 解码后的消息或空列表
	 * @throws StompConversionException 在解码出现问题时抛出
	 */
	public List<Message<byte[]>> decode(ByteBuffer newBuffer) {
		this.chunks.add(newBuffer);
		checkBufferLimits();

		Integer contentLength = this.expectedContentLength;
		if (contentLength != null && getBufferSize() < contentLength) {
			return Collections.emptyList();
		}

		ByteBuffer bufferToDecode = assembleChunksAndReset();
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		List<Message<byte[]>> messages = this.stompDecoder.decode(bufferToDecode, headers);

		if (bufferToDecode.hasRemaining()) {
			this.chunks.add(bufferToDecode);
			this.expectedContentLength = StompHeaderAccessor.getContentLength(headers);
		}

		return messages;
	}

	private ByteBuffer assembleChunksAndReset() {
		ByteBuffer result;
		if (this.chunks.size() == 1) {
			result = this.chunks.remove();
		}
		else {
			result = ByteBuffer.allocate(getBufferSize());
			for (ByteBuffer partial : this.chunks) {
				result.put(partial);
			}
			result.flip();
		}
		this.chunks.clear();
		this.expectedContentLength = null;
		return result;
	}

	private void checkBufferLimits() {
		Integer contentLength = this.expectedContentLength;
		if (contentLength != null && contentLength > this.bufferSizeLimit) {
			throw new StompConversionException(
					"STOMP 'content-length' header value " + this.expectedContentLength +
					"  exceeds configured buffer size limit " + this.bufferSizeLimit);
		}
		if (getBufferSize() > this.bufferSizeLimit) {
			throw new StompConversionException("The configured STOMP buffer size limit of " +
					this.bufferSizeLimit + " bytes has been exceeded");
		}
	}

	/**
	 * 计算当前缓冲区大小。
	 */
	public int getBufferSize() {
		int size = 0;
		for (ByteBuffer buffer : this.chunks) {
			size = size + buffer.remaining();
		}
		return size;
	}

	/**
	 * 获取当前缓冲的不完整 STOMP 帧的预期内容长度。
	 */
	@Nullable
	public Integer getExpectedContentLength() {
		return this.expectedContentLength;
	}

}
