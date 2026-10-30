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

package org.springframework.messaging.rsocket;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;

/**
 * 用于在 {@link DataBuffer} 与 {@link Payload} 之间相互转换的静态工具方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public abstract class PayloadUtils {

	/**
	 * 使用此方法对 {@code Payload} 的数据部分进行切片、保留和包装，并释放
	 * {@code Payload}。这里假设 Payload 元数据此时已被读取，从而确保下游代码
	 * 只需关注 {@code DataBuffer}。
	 * @param payload 要处理的负载
	 * @param bufferFactory 用于包装的 DataBufferFactory
	 * @return 创建的 {@code DataBuffer} 实例
	 */
	public static DataBuffer retainDataAndReleasePayload(Payload payload, DataBufferFactory bufferFactory) {
		try {
			if (bufferFactory instanceof NettyDataBufferFactory) {
				ByteBuf byteBuf = payload.sliceData().retain();
				return ((NettyDataBufferFactory) bufferFactory).wrap(byteBuf);
			}
			else {
				return bufferFactory.wrap(payload.getData());
			}
		}
		finally {
			if (payload.refCnt() > 0) {
				payload.release();
			}
		}
	}

	/**
	 * 根据给定的元数据和数据创建 Payload。
	 * <p>如果至少有一个是 {@link NettyDataBuffer}，则创建 {@link ByteBufPayload}，
	 * 其中要么获取底层原生 {@link ByteBuf}，要么在必要时使用
	 * {@link Unpooled#wrappedBuffer(ByteBuffer...)}。否则，如果两者都是
	 * {@link DefaultDataBuffer}，则创建 {@link DefaultPayload}。
	 * @param data 负载的数据部分
	 * @param metadata 负载的元数据部分
	 * @return 创建的负载
	 */
	public static Payload createPayload(DataBuffer data, DataBuffer metadata) {
		return data instanceof NettyDataBuffer || metadata instanceof NettyDataBuffer ?
				ByteBufPayload.create(asByteBuf(data), asByteBuf(metadata)) :
				DefaultPayload.create(asByteBuffer(data), asByteBuffer(metadata));
	}

	/**
	 * 仅使用数据创建 Payload。如果输入是 {@link NettyDataBuffer}，则创建
	 * {@link ByteBufPayload}，否则创建 {@link DefaultPayload}。
	 * @param data 负载的数据部分
	 * @return 创建的负载
	 */
	public static Payload createPayload(DataBuffer data) {
		return data instanceof NettyDataBuffer ?
				ByteBufPayload.create(asByteBuf(data)) : DefaultPayload.create(asByteBuffer(data));
	}


	static ByteBuf asByteBuf(DataBuffer buffer) {
		return buffer instanceof NettyDataBuffer ?
				((NettyDataBuffer) buffer).getNativeBuffer() : Unpooled.wrappedBuffer(buffer.asByteBuffer());
	}

	private static ByteBuffer asByteBuffer(DataBuffer buffer) {
		return buffer instanceof DefaultDataBuffer ?
				((DefaultDataBuffer) buffer).getNativeBuffer() : buffer.asByteBuffer();
	}

}
