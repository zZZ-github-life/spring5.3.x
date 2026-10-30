/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.util;

import java.io.ByteArrayOutputStream;

/**
 * {@link java.io.ByteArrayOutputStream} 的扩展，它：
 * <ul>
 * <li>提供公有的 {@link org.springframework.util.ResizableByteArrayOutputStream#grow(int)} 和 {@link org.springframework.util.ResizableByteArrayOutputStream#resize(int)} 方法，以便更精细地控制内部缓冲区的大小</li>
 * <li>默认初始容量更大（256）</li>
 * </ul>
 *
 * <p>自 4.2 起，在不需要可赋值给 {@link ByteArrayOutputStream} 的 Spring 内部使用中，该类已被
 * {@link FastByteArrayOutputStream} 取代（因为 {@link FastByteArrayOutputStream} 在缓冲区扩容管理
 * 方面更高效，但不继承标准 {@link ByteArrayOutputStream}）。
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.0.3
 * @see #resize
 * @see FastByteArrayOutputStream
 */
public class ResizableByteArrayOutputStream extends ByteArrayOutputStream {

	private static final int DEFAULT_INITIAL_CAPACITY = 256;


	/**
	 * 创建一个新的 <code>ResizableByteArrayOutputStream</code>，默认初始容量为 256 字节。
	 */
	public ResizableByteArrayOutputStream() {
		super(DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * 创建一个新的 <code>ResizableByteArrayOutputStream</code>，具有指定的初始容量。
	 * @param initialCapacity 初始缓冲区大小（以字节为单位）
	 */
	public ResizableByteArrayOutputStream(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 将内部缓冲区大小调整为指定的容量。
	 * @param targetCapacity 缓冲区的期望大小
	 * @throws IllegalArgumentException 如果给定容量小于
	 * 缓冲区中已存储内容的实际大小
	 * @see ResizableByteArrayOutputStream#size()
	 */
	public synchronized void resize(int targetCapacity) {
		Assert.isTrue(targetCapacity >= this.count, "New capacity must not be smaller than current size");
		byte[] resizedBuffer = new byte[targetCapacity];
		System.arraycopy(this.buf, 0, resizedBuffer, 0, this.count);
		this.buf = resizedBuffer;
	}

	/**
	 * 增大内部缓冲区大小。
	 * @param additionalCapacity 要在当前缓冲区大小上增加的字节数
	 * @see ResizableByteArrayOutputStream#size()
	 */
	public synchronized void grow(int additionalCapacity) {
		Assert.isTrue(additionalCapacity >= 0, "Additional capacity must be 0 or higher");
		if (this.count + additionalCapacity > this.buf.length) {
			int newCapacity = Math.max(this.buf.length * 2, this.count + additionalCapacity);
			resize(newCapacity);
		}
	}

	/**
	 * 返回此流内部缓冲区的当前大小。
	 */
	public synchronized int capacity() {
		return this.buf.length;
	}

}
