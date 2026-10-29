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

package org.springframework.core.io.buffer;

/**
 * {@link DataBuffer} 的扩展，允许缓冲区共享内存池。引入了引用计数方法。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface PooledDataBuffer extends DataBuffer {

	/**
	 * 如果此缓冲区已分配，则返回 {@code true}；如果已被释放，则返回 {@code false}。
	 * @since 5.1
	 */
	boolean isAllocated();

	/**
	 * 将此缓冲区的引用计数增加一。
	 * @return this buffer
	 */
	PooledDataBuffer retain();

	/**
	 * 将给定的提示与数据缓冲区关联，用于调试目的。
	 * @return this buffer
	 * @since 5.3.2
	 */
	PooledDataBuffer touch(Object hint);

	/**
	 * 将此缓冲区的引用计数减少一，并在计数达到零时释放它。
	 * @return {@code true} if the buffer was deallocated;
	 * {@code false} otherwise
	 */
	boolean release();

}
