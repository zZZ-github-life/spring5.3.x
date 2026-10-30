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

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

import org.springframework.lang.Nullable;

/**
 * 用于处理流的简单实用方法。此类的复制方法与 {@link FileCopyUtils} 中定义的方法类似，不同之处在于
 * 完成后所有受影响的流都保持打开状态。所有复制方法使用 4096 字节的块大小。
 *
 * <p>主要用于框架内部，但也可用于应用代码。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 3.2.2
 * @see FileCopyUtils
 */
public abstract class StreamUtils {

	/**
	 * 复制字节时使用的默认缓冲区大小。
	 */
	public static final int BUFFER_SIZE = 4096;

	private static final byte[] EMPTY_CONTENT = new byte[0];


	/**
	 * 将给定 InputStream 的内容复制到新的字节数组中。
	 * <p>完成后使流保持打开状态。
	 * @param in 要从中复制的流（可以为 {@code null} 或空）
	 * @return 已复制到的新的字节数组（可能为空）
	 * @throws IOException 发生 I/O 错误时
	 */
	public static byte[] copyToByteArray(@Nullable InputStream in) throws IOException {
		if (in == null) {
			return new byte[0];
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
		copy(in, out);
		return out.toByteArray();
	}

	/**
	 * 将给定 InputStream 的内容复制到 String 中。
	 * <p>完成后使流保持打开状态。
	 * @param in 要从中复制的 InputStream（可以为 {@code null} 或空）
	 * @param charset 用于解码字节的 {@link Charset}
	 * @return 已复制到的 String（可能为空）
	 * @throws IOException 发生 I/O 错误时
	 */
	public static String copyToString(@Nullable InputStream in, Charset charset) throws IOException {
		if (in == null) {
			return "";
		}

		StringBuilder out = new StringBuilder(BUFFER_SIZE);
		InputStreamReader reader = new InputStreamReader(in, charset);
		char[] buffer = new char[BUFFER_SIZE];
		int charsRead;
		while ((charsRead = reader.read(buffer)) != -1) {
			out.append(buffer, 0, charsRead);
		}
		return out.toString();
	}

	/**
	 * 将给定 {@link ByteArrayOutputStream} 的内容复制到 {@link String} 中。
	 * <p>这是 {@code new String(baos.toByteArray(), charset)} 的一种更高效的等价实现。
	 * @param baos 要复制到 String 中的 {@code ByteArrayOutputStream}
	 * @param charset 用于解码字节的 {@link Charset}
	 * @return 已复制到的 String（可能为空）
	 * @since 5.2.6
	 */
	public static String copyToString(ByteArrayOutputStream baos, Charset charset) {
		Assert.notNull(baos, "No ByteArrayOutputStream specified");
		Assert.notNull(charset, "No Charset specified");
		try {
			// Can be replaced with toString(Charset) call in Java 10+
			return baos.toString(charset.name());
		}
		catch (UnsupportedEncodingException ex) {
			// Should never happen
			throw new IllegalArgumentException("Invalid charset name: " + charset, ex);
		}
	}

	/**
	 * 将给定字节数组的内容复制到给定 OutputStream。
	 * <p>完成后使流保持打开状态。
	 * @param in 要从中复制的字节数组
	 * @param out 要复制到的 OutputStream
	 * @throws IOException 发生 I/O 错误时
	 */
	public static void copy(byte[] in, OutputStream out) throws IOException {
		Assert.notNull(in, "No input byte array specified");
		Assert.notNull(out, "No OutputStream specified");

		out.write(in);
		out.flush();
	}

	/**
	 * 将给定 String 的内容复制到给定 OutputStream。
	 * <p>完成后使流保持打开状态。
	 * @param in 要从中复制的 String
	 * @param charset 该 Charset
	 * @param out 要复制到的 OutputStream
	 * @throws IOException 发生 I/O 错误时
	 */
	public static void copy(String in, Charset charset, OutputStream out) throws IOException {
		Assert.notNull(in, "No input String specified");
		Assert.notNull(charset, "No Charset specified");
		Assert.notNull(out, "No OutputStream specified");

		Writer writer = new OutputStreamWriter(out, charset);
		writer.write(in);
		writer.flush();
	}

	/**
	 * 将给定 InputStream 的内容复制到给定 OutputStream。
	 * <p>完成后使两个流都保持打开状态。
	 * @param in 要从中复制的 InputStream
	 * @param out 要复制到的 OutputStream
	 * @return 已复制的字节数
	 * @throws IOException 发生 I/O 错误时
	 */
	public static int copy(InputStream in, OutputStream out) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");

		int byteCount = 0;
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = in.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
			byteCount += bytesRead;
		}
		out.flush();
		return byteCount;
	}

	/**
	 * 将给定 InputStream 的一段内容复制到给定 OutputStream。
	 * <p>如果指定的范围超出 InputStream 的长度，则最多复制到流的末尾，并返回实际复制的字节数。
	 * <p>完成后使两个流都保持打开状态。
	 * @param in 要从中复制的 InputStream
	 * @param out 要复制到的 OutputStream
	 * @param start 开始复制的起始位置
	 * @param end 结束复制的位置
	 * @return 已复制的字节数
	 * @throws IOException 发生 I/O 错误时
	 * @since 4.3
	 */
	public static long copyRange(InputStream in, OutputStream out, long start, long end) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");

		long skipped = in.skip(start);
		if (skipped < start) {
			throw new IOException("Skipped only " + skipped + " bytes out of " + start + " required");
		}

		long bytesToCopy = end - start + 1;
		byte[] buffer = new byte[(int) Math.min(StreamUtils.BUFFER_SIZE, bytesToCopy)];
		while (bytesToCopy > 0) {
			int bytesRead = in.read(buffer);
			if (bytesRead == -1) {
				break;
			}
			else if (bytesRead <= bytesToCopy) {
				out.write(buffer, 0, bytesRead);
				bytesToCopy -= bytesRead;
			}
			else {
				out.write(buffer, 0, (int) bytesToCopy);
				bytesToCopy = 0;
			}
		}
		return (end - start + 1 - bytesToCopy);
	}

	/**
	 * 排空给定 InputStream 的剩余内容。
	 * <p>完成后使 InputStream 保持打开状态。
	 * @param in 要排空的 InputStream
	 * @return 已读取的字节数
	 * @throws IOException 发生 I/O 错误时
	 * @since 4.3
	 */
	public static int drain(InputStream in) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;
		int byteCount = 0;
		while ((bytesRead = in.read(buffer)) != -1) {
			byteCount += bytesRead;
		}
		return byteCount;
	}

	/**
	 * 返回一个高效的空 {@link InputStream}。
	 * @return 基于空字节数组的 {@link ByteArrayInputStream}
	 * @since 4.2.2
	 */
	public static InputStream emptyInput() {
		return new ByteArrayInputStream(EMPTY_CONTENT);
	}

	/**
	 * 返回给定 {@link InputStream} 的一个变体，对其调用 {@link InputStream#close() close()} 不产生任何
	 * 效果。
	 * @param in 要装饰的 InputStream
	 * @return 忽略 close 调用的 InputStream 版本
	 */
	public static InputStream nonClosing(InputStream in) {
		Assert.notNull(in, "No InputStream specified");
		return new NonClosingInputStream(in);
	}

	/**
	 * 返回给定 {@link OutputStream} 的一个变体，对其调用 {@link OutputStream#close() close()} 不产生任
	 * 何效果。
	 * @param out 要装饰的 OutputStream
	 * @return 忽略 close 调用的 OutputStream 版本
	 */
	public static OutputStream nonClosing(OutputStream out) {
		Assert.notNull(out, "No OutputStream specified");
		return new NonClosingOutputStream(out);
	}


	private static class NonClosingInputStream extends FilterInputStream {

		public NonClosingInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() throws IOException {
		}
	}


	private static class NonClosingOutputStream extends FilterOutputStream {

		public NonClosingOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(byte[] b, int off, int let) throws IOException {
			// It is critical that we override this method for performance
			this.out.write(b, off, let);
		}

		@Override
		public void close() throws IOException {
		}
	}

}
