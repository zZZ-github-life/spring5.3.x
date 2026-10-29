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

package org.springframework.core.io.buffer;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 用于 {@link DataBuffer DataBuffers} 的工厂，允许分配和包装数据缓冲区。
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @see DataBuffer
 */
public interface DataBufferFactory {

	/**
	 * 分配具有默认初始容量的数据缓冲区。取决于底层实现及其配置，这将是基于堆的缓冲区或直接缓冲区。
	 * @return the allocated buffer
	 */
	DataBuffer allocateBuffer();

	/**
	 * 分配具有给定初始容量的数据缓冲区。取决于底层实现及其配置，这将是基于堆的缓冲区或直接缓冲区。
	 * @param initialCapacity the initial capacity of the buffer to allocate
	 * @return the allocated buffer
	 */
	DataBuffer allocateBuffer(int initialCapacity);

	/**
	 * 将给定 {@link ByteBuffer} 包装为 {@code DataBuffer}。与
	 * {@linkplain #allocateBuffer(int) allocating} 不同，包装不会使用新的内存。
	 * @param byteBuffer the NIO byte buffer to wrap
	 * @return the wrapped buffer
	 */
	DataBuffer wrap(ByteBuffer byteBuffer);

	/**
	 * 将给定 {@code byte} 数组包装为 {@code DataBuffer}。与
	 * {@linkplain #allocateBuffer(int) allocating} 不同，包装不会使用新的内存。
	 * @param bytes the byte array to wrap
	 * @return the wrapped buffer
	 */
	DataBuffer wrap(byte[] bytes);

	/**
	 * 返回一个新的 {@code DataBuffer}，它由连接在一起的 {@code dataBuffers} 元素组成。取决于具体实现，
	 * 返回的缓冲区可能是包含所提供缓冲区全部数据的单个缓冲区，也可能是包含对这些缓冲区的引用的真正复合
	 * 缓冲区。
	 * <p>请注意，给定的数据缓冲区<strong>不必</strong>释放，因为它们会作为返回的复合缓冲区的一部分被释
	 * 放。
	 * @param dataBuffers the data buffers to be composed
	 * @return a buffer that is composed of the {@code dataBuffers} argument
	 * @since 5.0.3
	 */
	DataBuffer join(List<? extends DataBuffer> dataBuffers);

}
