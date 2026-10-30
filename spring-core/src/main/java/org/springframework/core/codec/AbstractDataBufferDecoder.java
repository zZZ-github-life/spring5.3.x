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

package org.springframework.core.codec;

import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * 抽象基类，适用于可以直接将 {@code DataBuffer} 解码为目标元素类型的 {@code Decoder} 实现。
 *
 * <p>子类必须实现 {@link #decodeDataBuffer}，以提供将 {@code DataBuffer} 转换为目标数据类型的方法。
 * 默认的 {@link #decode} 实现会逐个转换各个数据缓冲区，而 {@link #decodeToMono} 会应用“reduce”并
 * 转换聚合后的缓冲区。
 *
 * <p>子类可以重写 {@link #decode}，以便沿不同的边界拆分输入流（例如对 {@code String} 按换行符拆分），
 * 或者始终归约为单个数据缓冲区（例如 {@code Resource}）。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 元素类型
 */
@SuppressWarnings("deprecation")
public abstract class AbstractDataBufferDecoder<T> extends AbstractDecoder<T> {

	private int maxInMemorySize = 256 * 1024;


	protected AbstractDataBufferDecoder(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}


	/**
	 * 配置输入流需要聚合时可缓冲的最大字节数限制。这可能是由于解码为单个 {@code DataBuffer}、
	 * {@link java.nio.ByteBuffer ByteBuffer}、{@code byte[]}、
	 * {@link org.springframework.core.io.Resource Resource}、{@code String} 等而产生的。在拆分输入流时
	 * （例如分隔文本）也可能出现这种情况，此时该限制适用于分隔符之间缓冲的数据。
	 * <p>默认设置为 256K。
	 * @param byteCount 要缓冲的最大字节数；-1 表示不限制
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * 返回 {@link #setMaxInMemorySize configured} 的字节数限制。
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public Flux<T> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(input).map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
	}

	@Override
	public Mono<T> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return DataBufferUtils.join(input, this.maxInMemorySize)
				.map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
	}

	/**
	 * 如何将 {@code DataBuffer} 解码为目标元素类型。
	 * @deprecated as of 5.2, please implement
	 * {@link #decode(DataBuffer, ResolvableType, MimeType, Map)} instead
	 */
	@Deprecated
	@Nullable
	protected T decodeDataBuffer(DataBuffer buffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return decode(buffer, elementType, mimeType, hints);
	}

}
