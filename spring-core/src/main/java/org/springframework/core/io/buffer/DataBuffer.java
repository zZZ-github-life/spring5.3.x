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

package org.springframework.core.io.buffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.function.IntPredicate;

import org.springframework.util.Assert;

/**
 * 对字节缓冲区的基本抽象。
 *
 * <p>{@code DataBuffer} 具有独立的 {@linkplain #readPosition() read} 位置和
 * {@linkplain #writePosition() write} 位置，而 {@code ByteBuffer} 只有一个
 * {@linkplain ByteBuffer#position() position}。因此，{@code DataBuffer} 在写入后读取时不需要
 * {@linkplain ByteBuffer#flip() flip}。通常，读位置、写位置和容量满足以下不变式：
 *
 * <blockquote>
 * {@code 0} {@code <=}
 * <i>readPosition</i> {@code <=}
 * <i>writePosition</i> {@code <=}
 * <i>capacity</i>
 * </blockquote>
 *
 * <p>{@code DataBuffer} 的 {@linkplain #capacity() capacity} 会按需扩展，类似于
 * {@code StringBuilder}。
 *
 * <p>{@code DataBuffer} 抽象的主要目的是为 {@link ByteBuffer} 提供一个便捷的包装，类似于 Netty 的
 * {@link io.netty.buffer.ByteBuf}，但也可用于非 Netty 平台（即 Servlet 容器）。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 * @see DataBufferFactory
 */
public interface DataBuffer {

	/**
	 * 返回创建此缓冲区的 {@link DataBufferFactory}。
	 * @return 创建该缓冲区的缓冲区工厂
	 */
	DataBufferFactory factory();

	/**
	 * 返回此缓冲区中匹配给定谓词的第一个字节的索引。
	 * @param predicate 要匹配的谓词
	 * @param fromIndex 开始搜索的索引
	 * @return 第一个匹配 {@code predicate} 的字节的索引；
	 * 如果没有匹配项则为 {@code -1}
	 */
	int indexOf(IntPredicate predicate, int fromIndex);

	/**
	 * 返回此缓冲区中匹配给定谓词的最后一个字节的索引。
	 * @param predicate 要匹配的谓词
	 * @param fromIndex 开始搜索的索引
	 * @return 最后一个匹配 {@code predicate} 的字节的索引；
	 * 如果没有匹配项则为 {@code -1}
	 */
	int lastIndexOf(IntPredicate predicate, int fromIndex);

	/**
	 * 返回可从此数据缓冲区读取的字节数。
	 * @return 可读取的字节数
	 */
	int readableByteCount();

	/**
	 * 返回可写入此数据缓冲区的字节数。
	 * @return 可写入的字节数
	 * @since 5.0.1
	 */
	int writableByteCount();

	/**
	 * 返回此缓冲区可容纳的字节数。
	 * @return 容量
	 * @since 5.0.1
	 */
	int capacity();

	/**
	 * 设置此缓冲区可容纳的字节数。
	 * <p>如果新容量小于当前容量，此缓冲区的内容将被截断。如果新容量大于当前容量，则会进行扩展。
	 * @param capacity 新的容量
	 * @return 此缓冲区
	 */
	DataBuffer capacity(int capacity);

	/**
	 * 确保当前缓冲区具有足够的 {@link #writableByteCount()} 来写入作为参数给出的数据量。如果不足，缺少
	 * 的容量将被添加到缓冲区中。
	 * @param capacity 要检查的可写容量
	 * @return 此缓冲区
	 * @since 5.1.4
	 */
	default DataBuffer ensureCapacity(int capacity) {
		return this;
	}

	/**
	 * 返回此缓冲区将从中读取的位置。
	 * @return 读取位置
	 * @since 5.0.1
	 */
	int readPosition();

	/**
	 * 设置此缓冲区将从中读取的位置。
	 * @param readPosition 新的读取位置
	 * @return 此缓冲区
	 * @throws IndexOutOfBoundsException 如果 {@code readPosition} 小于 0
	 * 或大于 {@link #writePosition()}
	 * @since 5.0.1
	 */
	DataBuffer readPosition(int readPosition);

	/**
	 * 返回此缓冲区将写入到的位置。
	 * @return 写入位置
	 * @since 5.0.1
	 */
	int writePosition();

	/**
	 * 设置此缓冲区将写入到的位置。
	 * @param writePosition 新的写入位置
	 * @return 此缓冲区
	 * @throws IndexOutOfBoundsException 如果 {@code writePosition} 小于
	 * {@link #readPosition()} 或大于 {@link #capacity()}
	 * @since 5.0.1
	 */
	DataBuffer writePosition(int writePosition);

	/**
	 * 从此数据缓冲区的给定索引处读取单个字节。
	 * @param index 读取字节处的索引
	 * @return 给定索引处的字节
	 * @throws IndexOutOfBoundsException 当 {@code index} 越界时
	 * @since 5.0.4
	 */
	byte getByte(int index);

	/**
	 * 从此数据缓冲区的当前读取位置读取单个字节。
	 * @return 此缓冲区当前读取位置处的字节
	 */
	byte read();

	/**
	 * 将此缓冲区的数据读入指定目标，从此缓冲区的当前读取位置开始。
	 * @param destination 字节要写入的数组
	 * @return 此缓冲区
	 */
	DataBuffer read(byte[] destination);

	/**
	 * 将此缓冲区最多 {@code length} 个字节读入指定目标，从此缓冲区的当前读取位置开始。
	 * @param destination 字节要写入的数组
	 * @param offset 要写入的第一个字节在 {@code destination} 中的索引
	 * @param length 要写入 {@code destination} 的最大字节数
	 * @return 此缓冲区
	 */
	DataBuffer read(byte[] destination, int offset, int length);

	/**
	 * 在当前写入位置向此缓冲区写入单个字节。
	 * @param b 要写入的字节
	 * @return 此缓冲区
	 */
	DataBuffer write(byte b);

	/**
	 * 将给定源写入此缓冲区，从此缓冲区的当前写入位置开始。
	 * @param source 要写入此缓冲区的字节
	 * @return 此缓冲区
	 */
	DataBuffer write(byte[] source);

	/**
	 * 将给定源最多 {@code length} 个字节写入此缓冲区，从此缓冲区的当前写入位置开始。
	 * @param source 要写入此缓冲区的字节
	 * @param offset 在 {@code source} 中开始写入的索引
	 * @param length 要从 {@code source} 写入的最大字节数
	 * @return 此缓冲区
	 */
	DataBuffer write(byte[] source, int offset, int length);

	/**
	 * 将一个或多个 {@code DataBuffer} 写入此缓冲区，从当前写入位置开始。调用方负责
	 * {@linkplain DataBufferUtils#release(DataBuffer) release} 给定的数据缓冲区。
	 * @param buffers 要写入此缓冲区的字节缓冲区
	 * @return 此缓冲区
	 */
	DataBuffer write(DataBuffer... buffers);

	/**
	 * 将一个或多个 {@link ByteBuffer} 写入此缓冲区，从当前写入位置开始。
	 * @param buffers 要写入此缓冲区的字节缓冲区
	 * @return 此缓冲区
	 */
	DataBuffer write(ByteBuffer... buffers);

	/**
	 * 使用给定 {@code Charset} 写入给定 {@code CharSequence}，从当前写入位置开始。
	 * @param charSequence 要写入此缓冲区的字符序列
	 * @param charset 用于编码该字符序列的字符集
	 * @return 此缓冲区
	 * @since 5.1.4
	 */
	default DataBuffer write(CharSequence charSequence, Charset charset) {
		Assert.notNull(charSequence, "CharSequence must not be null");
		Assert.notNull(charset, "Charset must not be null");
		if (charSequence.length() != 0) {
			CharsetEncoder charsetEncoder = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			CharBuffer inBuffer = CharBuffer.wrap(charSequence);
			int estimatedSize = (int) (inBuffer.remaining() * charsetEncoder.averageBytesPerChar());
			ByteBuffer outBuffer = ensureCapacity(estimatedSize)
					.asByteBuffer(writePosition(), writableByteCount());
			while (true) {
				CoderResult cr = (inBuffer.hasRemaining() ?
						charsetEncoder.encode(inBuffer, outBuffer, true) : CoderResult.UNDERFLOW);
				if (cr.isUnderflow()) {
					cr = charsetEncoder.flush(outBuffer);
				}
				if (cr.isUnderflow()) {
					break;
				}
				if (cr.isOverflow()) {
					writePosition(writePosition() + outBuffer.position());
					int maximumSize = (int) (inBuffer.remaining() * charsetEncoder.maxBytesPerChar());
					ensureCapacity(maximumSize);
					outBuffer = asByteBuffer(writePosition(), writableByteCount());
				}
			}
			writePosition(writePosition() + outBuffer.position());
		}
		return this;
	}

	/**
	 * 创建一个新的 {@code DataBuffer}，其内容为此数据缓冲区内容的共享子序列。此数据缓冲区与返回缓冲区
	 * 之间的数据是共享的；不过返回缓冲区位置的变化不会反映在此数据缓冲区的读位置或写位置中。
	 * <p>请注意，此方法<strong>不会</strong>对生成的切片调用
	 * {@link DataBufferUtils#retain(DataBuffer)}：引用计数不会增加。
	 * @param index 切片的起始索引
	 * @param length 切片的长度
	 * @return 此数据缓冲区的指定切片
	 */
	DataBuffer slice(int index, int length);

	/**
	 * 创建一个新的 {@code DataBuffer}，其内容为此数据缓冲区内容的共享、保留子序列。此数据缓冲区与返回
	 * 缓冲区之间的数据是共享的；不过返回缓冲区位置的变化不会反映在此数据缓冲区的读位置或写位置中。
	 * <p>请注意，与 {@link #slice(int, int)} 不同，此方法<strong>会</strong>对生成的切片调用
	 * {@link DataBufferUtils#retain(DataBuffer)}（或等效方法）。
	 * @param index 切片的起始索引
	 * @param length 切片的长度
	 * @return 此数据缓冲区的指定保留切片
	 * @since 5.2
	 */
	default DataBuffer retainedSlice(int index, int length) {
		return DataBufferUtils.retain(slice(index, length));
	}

	/**
	 * 将此缓冲区的字节暴露为 {@link ByteBuffer}。此 {@code DataBuffer} 与返回的 {@code ByteBuffer} 之
	 * 间的数据是共享的；不过返回缓冲区 {@linkplain ByteBuffer#position() position} 的变化不会反映在此
	 * 数据缓冲区的读位置或写位置中。
	 * @return 此数据缓冲区作为字节缓冲区
	 */
	ByteBuffer asByteBuffer();

	/**
	 * 将此缓冲区字节的子序列暴露为 {@link ByteBuffer}。此 {@code DataBuffer} 与返回的
	 * {@code ByteBuffer} 之间的数据是共享的；不过返回缓冲区
	 * {@linkplain ByteBuffer#position() position} 的变化不会反映在此数据缓冲区的读位置或写位置中。
	 * @param index 字节缓冲区的起始索引
	 * @param length 返回的字节缓冲区的长度
	 * @return 此数据缓冲区作为字节缓冲区
	 * @since 5.0.1
	 */
	ByteBuffer asByteBuffer(int index, int length);

	/**
	 * 将此缓冲区的数据暴露为 {@link InputStream}。数据与读位置在返回的流与此数据缓冲区之间共享。当输入
	 * 流被 {@linkplain InputStream#close() closed} 时，底层缓冲区<strong>不会</strong>被
	 * {@linkplain DataBufferUtils#release(DataBuffer) released}。
	 * @return 此数据缓冲区作为输入流
	 * @see #asInputStream(boolean)
	 */
	InputStream asInputStream();

	/**
	 * 将此缓冲区的数据暴露为 {@link InputStream}。数据与读位置在返回的流与此数据缓冲区之间共享。
	 * @param releaseOnClose 当输入流被{@linkplain InputStream#close() closed}时，
	 * 底层缓冲区是否会被
	 * {@linkplain DataBufferUtils#release(DataBuffer) released}。
	 * @return 此数据缓冲区作为输入流
	 * @since 5.0.4
	 */
	InputStream asInputStream(boolean releaseOnClose);

	/**
	 * 将此缓冲区的数据暴露为 {@link OutputStream}。数据与写位置在返回的流与此数据缓冲区之间共享。
	 * @return 此数据缓冲区作为输出流
	 */
	OutputStream asOutputStream();

	/**
	 * 使用指定字符集将此缓冲区的数据作为 String 返回。默认实现委托给
	 * {@code toString(readPosition(), readableByteCount(), charset)}。
	 * @param charset 要使用的字符集
	 * @return 此缓冲区全部数据的字符串表示
	 * @since 5.2
	 */
	default String toString(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		return toString(readPosition(), readableByteCount(), charset);
	}

	/**
	 * 使用指定字符集将此缓冲区数据的一部分作为 String 返回。
	 * @param index 字符串的起始索引
	 * @param length 用于该字符串的字节数
	 * @param charset 要使用的字符集
	 * @return 此缓冲区部分数据的字符串表示
	 * @since 5.2
	 */
	String toString(int index, int length, Charset charset);

}
