/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.springframework.lang.Nullable;

/**
 * {@link java.io.ByteArrayOutputStream} 的高速替代方案。注意，与它的同类
 * {@link ResizableByteArrayOutputStream} 不同，此变体<i>不</i>继承 {@code ByteArrayOutputStream}。
 *
 * <p>与 {@link java.io.ByteArrayOutputStream} 不同，此实现由 {@code byte[]} 的
 * {@link java.util.ArrayDeque} 支持，而不是单个不断调整大小的 {@code byte[]}。它在扩展时不会复制缓
 * 冲区。
 *
 * <p>初始缓冲区仅在第一次写入流时创建。如果使用 {@link #writeTo(OutputStream)} 方法提取其内容，也
 * 不会复制内部缓冲区。
 *
 * @author Craig Andrews
 * @author Juergen Hoeller
 * @since 4.2
 * @see #resize
 * @see ResizableByteArrayOutputStream
 */
public class FastByteArrayOutputStream extends OutputStream {

	private static final int DEFAULT_BLOCK_SIZE = 256;


	// The buffers used to store the content bytes
	private final Deque<byte[]> buffers = new ArrayDeque<>();

	// The size, in bytes, to use when allocating the first byte[]
	private final int initialBlockSize;

	// The size, in bytes, to use when allocating the next byte[]
	private int nextBlockSize = 0;

	// The number of bytes in previous buffers.
	// (The number of bytes in the current buffer is in 'index'.)
	private int alreadyBufferedSize = 0;

	// The index in the byte[] found at buffers.getLast() to be written next
	private int index = 0;

	// Is the stream closed?
	private boolean closed = false;


	/**
	 * 创建一个新的 <code>FastByteArrayOutputStream</code>，默认初始容量为 256 字节。
	 */
	public FastByteArrayOutputStream() {
		this(DEFAULT_BLOCK_SIZE);
	}

	/**
	 * 创建一个具有指定初始容量的新 <code>FastByteArrayOutputStream</code>。
	 * @param initialBlockSize 初始缓冲区大小，以字节为单位
	 */
	public FastByteArrayOutputStream(int initialBlockSize) {
		Assert.isTrue(initialBlockSize > 0, "Initial block size must be greater than 0");
		this.initialBlockSize = initialBlockSize;
		this.nextBlockSize = initialBlockSize;
	}


	// Overridden methods

	@Override
	public void write(int datum) throws IOException {
		if (this.closed) {
			throw new IOException("Stream closed");
		}
		else {
			if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
				addBuffer(1);
			}
			// store the byte
			this.buffers.getLast()[this.index++] = (byte) datum;
		}
	}

	@Override
	public void write(byte[] data, int offset, int length) throws IOException {
		if (offset < 0 || offset + length > data.length || length < 0) {
			throw new IndexOutOfBoundsException();
		}
		else if (this.closed) {
			throw new IOException("Stream closed");
		}
		else {
			if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
				addBuffer(length);
			}
			if (this.index + length > this.buffers.getLast().length) {
				int pos = offset;
				do {
					if (this.index == this.buffers.getLast().length) {
						addBuffer(length);
					}
					int copyLength = this.buffers.getLast().length - this.index;
					if (length < copyLength) {
						copyLength = length;
					}
					System.arraycopy(data, pos, this.buffers.getLast(), this.index, copyLength);
					pos += copyLength;
					this.index += copyLength;
					length -= copyLength;
				}
				while (length > 0);
			}
			else {
				// copy in the sub-array
				System.arraycopy(data, offset, this.buffers.getLast(), this.index, length);
				this.index += length;
			}
		}
	}

	@Override
	public void close() {
		this.closed = true;
	}

	/**
	 * 使用平台默认字符集将缓冲区内容解码为字符串。新 {@code String} 的长度取决于字符集，因此可能不等于
	 * 缓冲区的大小。
	 *
	 * <p>此方法始终使用平台默认字符集的默认替换字符串替换格式错误的输入和无法映射的字符序列。需要对解
	 * 码过程进行更多控制时，应使用 {@linkplain java.nio.charset.CharsetDecoder} 类。
	 * @return 从缓冲区内容解码得到的 String
	 */
	@Override
	public String toString() {
		return new String(toByteArrayUnsafe());
	}


	// Custom methods

	/**
	 * 返回此 <code>FastByteArrayOutputStream</code> 中存储的字节数。
	 */
	public int size() {
		return (this.alreadyBufferedSize + this.index);
	}

	/**
	 * 将流的数据转换为字节数组并返回该字节数组。
	 *
	 * <p>同时用该字节数组替换内部结构以节省内存：既然无论如何都要创建字节数组，不如直接使用它。这种方
	 * 式还意味着，如果在此期间没有任何写入而调用此方法两次，第二次调用将不执行任何操作。
	 *
	 * <p>此方法"不安全"，因为它返回内部缓冲区。调用者不应修改返回的缓冲区。
	 * @return 此输出流的当前内容，以字节数组形式返回。
	 * @see #size()
	 * @see #toByteArray()
	 */
	public byte[] toByteArrayUnsafe() {
		int totalSize = size();
		if (totalSize == 0) {
			return new byte[0];
		}
		resize(totalSize);
		return this.buffers.getFirst();
	}

	/**
	 * 创建一个新分配的字节数组。
	 *
	 * <p>其大小为当前此输出流的大小，并将包含内部缓冲区的有效内容。
	 * @return 此输出流的当前内容，以字节数组形式返回
	 * @see #size()
	 * @see #toByteArrayUnsafe()
	 */
	public byte[] toByteArray() {
		byte[] bytesUnsafe = toByteArrayUnsafe();
		return bytesUnsafe.clone();
	}

	/**
	 * 重置此 <code>FastByteArrayOutputStream</code> 的内容。
	 *
	 * <p>输出流中当前累积的所有输出都将被丢弃。该输出流可以再次使用。
	 */
	public void reset() {
		this.buffers.clear();
		this.nextBlockSize = this.initialBlockSize;
		this.closed = false;
		this.index = 0;
		this.alreadyBufferedSize = 0;
	}

	/**
	 * 获取一个 {@link InputStream} 以读取此 OutputStream 中的数据。
	 *
	 * <p>注意，如果在此 OutputStream 上调用任何方法（包括但不限于任何写入方法、{@link #reset()}、
	 * {@link #toByteArray()} 和 {@link #toByteArrayUnsafe()}），则 {@link java.io.InputStream} 的行为
	 * 是未定义的。
	 * @return 此 OutputStream 内容的 {@link InputStream}
	 */
	public InputStream getInputStream() {
		return new FastByteArrayInputStream(this);
	}

	/**
	 * 将缓冲区内容写入给定的 OutputStream。
	 * @param out 要写入的 OutputStream
	 */
	public void writeTo(OutputStream out) throws IOException {
		Iterator<byte[]> it = this.buffers.iterator();
		while (it.hasNext()) {
			byte[] bytes = it.next();
			if (it.hasNext()) {
				out.write(bytes, 0, bytes.length);
			}
			else {
				out.write(bytes, 0, this.index);
			}
		}
	}

	/**
	 * 将内部缓冲区大小调整为指定容量。
	 * @param targetCapacity 缓冲区的期望大小
	 * @throws IllegalArgumentException 如果给定容量小于缓冲区中
	 * 已存储内容的实际大小
	 * @see FastByteArrayOutputStream#size()
	 */
	public void resize(int targetCapacity) {
		Assert.isTrue(targetCapacity >= size(), "New capacity must not be smaller than current size");
		if (this.buffers.peekFirst() == null) {
			this.nextBlockSize = targetCapacity - size();
		}
		else if (size() == targetCapacity && this.buffers.getFirst().length == targetCapacity) {
			// do nothing - already at the targetCapacity
		}
		else {
			int totalSize = size();
			byte[] data = new byte[targetCapacity];
			int pos = 0;
			Iterator<byte[]> it = this.buffers.iterator();
			while (it.hasNext()) {
				byte[] bytes = it.next();
				if (it.hasNext()) {
					System.arraycopy(bytes, 0, data, pos, bytes.length);
					pos += bytes.length;
				}
				else {
					System.arraycopy(bytes, 0, data, pos, this.index);
				}
			}
			this.buffers.clear();
			this.buffers.add(data);
			this.index = totalSize;
			this.alreadyBufferedSize = 0;
		}
	}

	/**
	 * 创建一个新缓冲区并将其存储在 ArrayDeque 中。
	 *
	 * <p>添加一个至少可存储 {@code minCapacity} 字节的新缓冲区。
	 */
	private void addBuffer(int minCapacity) {
		if (this.buffers.peekLast() != null) {
			this.alreadyBufferedSize += this.index;
			this.index = 0;
		}
		if (this.nextBlockSize < minCapacity) {
			this.nextBlockSize = nextPowerOf2(minCapacity);
		}
		this.buffers.add(new byte[this.nextBlockSize]);
		this.nextBlockSize *= 2;  // block size doubles each time
	}

	/**
	 * 获取某个数的下一个 2 的幂（例如，119 的下一个 2 的幂是 128）。
	 */
	private static int nextPowerOf2(int val) {
		val--;
		val = (val >> 1) | val;
		val = (val >> 2) | val;
		val = (val >> 4) | val;
		val = (val >> 8) | val;
		val = (val >> 16) | val;
		val++;
		return val;
	}


	/**
	 * 从给定 <code>FastByteArrayOutputStream</code> 读取数据的 {@link java.io.InputStream} 实现。
	 */
	private static final class FastByteArrayInputStream extends UpdateMessageDigestInputStream {

		private final FastByteArrayOutputStream fastByteArrayOutputStream;

		private final Iterator<byte[]> buffersIterator;

		@Nullable
		private byte[] currentBuffer;

		private int currentBufferLength = 0;

		private int nextIndexInCurrentBuffer = 0;

		private int totalBytesRead = 0;

		/**
		 * 创建一个由给定 <code>FastByteArrayOutputStream</code> 支持的新 <code>
		 * FastByteArrayOutputStreamInputStream</code>。
		 */
		public FastByteArrayInputStream(FastByteArrayOutputStream fastByteArrayOutputStream) {
			this.fastByteArrayOutputStream = fastByteArrayOutputStream;
			this.buffersIterator = fastByteArrayOutputStream.buffers.iterator();
			if (this.buffersIterator.hasNext()) {
				this.currentBuffer = this.buffersIterator.next();
				if (this.currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
					this.currentBufferLength = fastByteArrayOutputStream.index;
				}
				else {
					this.currentBufferLength = (this.currentBuffer != null ? this.currentBuffer.length : 0);
				}
			}
		}

		@Override
		public int read() {
			if (this.currentBuffer == null) {
				// This stream doesn't have any data in it...
				return -1;
			}
			else {
				if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
					this.totalBytesRead++;
					return this.currentBuffer[this.nextIndexInCurrentBuffer++] & 0xFF;
				}
				else {
					if (this.buffersIterator.hasNext()) {
						this.currentBuffer = this.buffersIterator.next();
						updateCurrentBufferLength();
						this.nextIndexInCurrentBuffer = 0;
					}
					else {
						this.currentBuffer = null;
					}
					return read();
				}
			}
		}

		@Override
		public int read(byte[] b) {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte[] b, int off, int len) {
			if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException();
			}
			else if (len == 0) {
				return 0;
			}
			else {
				if (this.currentBuffer == null) {
					// This stream doesn't have any data in it...
					return -1;
				}
				else {
					if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
						int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
						System.arraycopy(this.currentBuffer, this.nextIndexInCurrentBuffer, b, off, bytesToCopy);
						this.totalBytesRead += bytesToCopy;
						this.nextIndexInCurrentBuffer += bytesToCopy;
						int remaining = read(b, off + bytesToCopy, len - bytesToCopy);
						return bytesToCopy + Math.max(remaining, 0);
					}
					else {
						if (this.buffersIterator.hasNext()) {
							this.currentBuffer = this.buffersIterator.next();
							updateCurrentBufferLength();
							this.nextIndexInCurrentBuffer = 0;
						}
						else {
							this.currentBuffer = null;
						}
						return read(b, off, len);
					}
				}
			}
		}

		@Override
		public long skip(long n) throws IOException {
			if (n > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("n exceeds maximum (" + Integer.MAX_VALUE + "): " + n);
			}
			else if (n == 0) {
				return 0;
			}
			else if (n < 0) {
				throw new IllegalArgumentException("n must be 0 or greater: " + n);
			}
			int len = (int) n;
			if (this.currentBuffer == null) {
				// This stream doesn't have any data in it...
				return 0;
			}
			else {
				if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
					int bytesToSkip = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
					this.totalBytesRead += bytesToSkip;
					this.nextIndexInCurrentBuffer += bytesToSkip;
					return (bytesToSkip + skip(len - bytesToSkip));
				}
				else {
					if (this.buffersIterator.hasNext()) {
						this.currentBuffer = this.buffersIterator.next();
						updateCurrentBufferLength();
						this.nextIndexInCurrentBuffer = 0;
					}
					else {
						this.currentBuffer = null;
					}
					return skip(len);
				}
			}
		}

		@Override
		public int available() {
			return (this.fastByteArrayOutputStream.size() - this.totalBytesRead);
		}

		/**
		 * 使用此流中剩余的字节更新消息摘要。
		 * @param messageDigest 要更新的消息摘要
		 */
		@Override
		public void updateMessageDigest(MessageDigest messageDigest) {
			updateMessageDigest(messageDigest, available());
		}

		/**
		 * 使用此流中接下来的 len 个字节更新消息摘要。为避免创建新的字节数组，出于性能考虑使用内部缓冲区。
		 * @param messageDigest 要更新的消息摘要
		 * @param len 要从此流读取并用于更新消息摘要的字节数
		 */
		@Override
		public void updateMessageDigest(MessageDigest messageDigest, int len) {
			if (this.currentBuffer == null) {
				// This stream doesn't have any data in it...
				return;
			}
			else if (len == 0) {
				return;
			}
			else if (len < 0) {
				throw new IllegalArgumentException("len must be 0 or greater: " + len);
			}
			else {
				if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
					int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
					messageDigest.update(this.currentBuffer, this.nextIndexInCurrentBuffer, bytesToCopy);
					this.nextIndexInCurrentBuffer += bytesToCopy;
					updateMessageDigest(messageDigest, len - bytesToCopy);
				}
				else {
					if (this.buffersIterator.hasNext()) {
						this.currentBuffer = this.buffersIterator.next();
						updateCurrentBufferLength();
						this.nextIndexInCurrentBuffer = 0;
					}
					else {
						this.currentBuffer = null;
					}
					updateMessageDigest(messageDigest, len);
				}
			}
		}

		private void updateCurrentBufferLength() {
			if (this.currentBuffer == this.fastByteArrayOutputStream.buffers.getLast()) {
				this.currentBufferLength = this.fastByteArrayOutputStream.index;
			}
			else {
				this.currentBufferLength = (this.currentBuffer != null ? this.currentBuffer.length : 0);
			}
		}
	}

}
