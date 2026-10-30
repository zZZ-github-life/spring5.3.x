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
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;

import org.springframework.lang.Nullable;

/**
 * 用于文件和流复制的简单工具方法。所有复制方法都使用 4096 字节的块大小，并在完成后关闭所有受影响的
 * 流。此类中保持流打开的复制方法变体可在 {@link StreamUtils} 中找到。
 *
 * <p>主要用于框架内部，但对应用代码也很有用。
 *
 * @author Juergen Hoeller
 * @author Hyunjin Choi
 * @since 06.10.2003
 * @see StreamUtils
 * @see FileSystemUtils
 */
public abstract class FileCopyUtils {

	/**
	 * 复制字节时使用的默认缓冲区大小。
	 */
	public static final int BUFFER_SIZE = StreamUtils.BUFFER_SIZE;


	//---------------------------------------------------------------------
	// Copy methods for java.io.File
	//---------------------------------------------------------------------

	/**
	 * 将给定输入 File 的内容复制到给定输出 File。
	 * @param in 要从中复制的文件
	 * @param out 要复制到的文件
	 * @return 复制的字节数
	 * @throws IOException 发生 I/O 错误时
	 */
	public static int copy(File in, File out) throws IOException {
		Assert.notNull(in, "No input File specified");
		Assert.notNull(out, "No output File specified");
		return copy(Files.newInputStream(in.toPath()), Files.newOutputStream(out.toPath()));
	}

	/**
	 * 将给定字节数组的内容复制到给定输出 File。
	 * @param in 要从中复制的字节数组
	 * @param out 要复制到的文件
	 * @throws IOException 发生 I/O 错误时
	 */
	public static void copy(byte[] in, File out) throws IOException {
		Assert.notNull(in, "No input byte array specified");
		Assert.notNull(out, "No output File specified");
		copy(new ByteArrayInputStream(in), Files.newOutputStream(out.toPath()));
	}

	/**
	 * 将给定输入 File 的内容复制到新的字节数组中。
	 * @param in 要从中复制的文件
	 * @return 已复制到的新字节数组
	 * @throws IOException 发生 I/O 错误时
	 */
	public static byte[] copyToByteArray(File in) throws IOException {
		Assert.notNull(in, "No input File specified");
		return copyToByteArray(Files.newInputStream(in.toPath()));
	}


	//---------------------------------------------------------------------
	// Copy methods for java.io.InputStream / java.io.OutputStream
	//---------------------------------------------------------------------

	/**
	 * 将给定 InputStream 的内容复制到给定 OutputStream。完成后关闭两个流。
	 * @param in 要从中复制的流
	 * @param out 要复制到的流
	 * @return 复制的字节数
	 * @throws IOException 发生 I/O 错误时
	 */
	public static int copy(InputStream in, OutputStream out) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");

		try {
			return StreamUtils.copy(in, out);
		}
		finally {
			close(in);
			close(out);
		}
	}

	/**
	 * 将给定字节数组的内容复制到给定 OutputStream。完成后关闭该流。
	 * @param in 要从中复制的字节数组
	 * @param out 要复制到的 OutputStream
	 * @throws IOException 发生 I/O 错误时
	 */
	public static void copy(byte[] in, OutputStream out) throws IOException {
		Assert.notNull(in, "No input byte array specified");
		Assert.notNull(out, "No OutputStream specified");

		try {
			out.write(in);
		}
		finally {
			close(out);
		}
	}

	/**
	 * 将给定 InputStream 的内容复制到新的字节数组中。完成后关闭该流。
	 * @param in 要从中复制的流（可以为 {@code null} 或为空）
	 * @return 已复制到的新字节数组（可能为空）
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


	//---------------------------------------------------------------------
	// Copy methods for java.io.Reader / java.io.Writer
	//---------------------------------------------------------------------

	/**
	 * 将给定 Reader 的内容复制到给定 Writer。完成后关闭两者。
	 * @param in 要从中复制的 Reader
	 * @param out 要复制到的 Writer
	 * @return 复制的字符数
	 * @throws IOException 发生 I/O 错误时
	 */
	public static int copy(Reader in, Writer out) throws IOException {
		Assert.notNull(in, "No Reader specified");
		Assert.notNull(out, "No Writer specified");

		try {
			int charCount = 0;
			char[] buffer = new char[BUFFER_SIZE];
			int charsRead;
			while ((charsRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, charsRead);
				charCount += charsRead;
			}
			out.flush();
			return charCount;
		}
		finally {
			close(in);
			close(out);
		}
	}

	/**
	 * 将给定 String 的内容复制到给定 Writer。完成后关闭该 writer。
	 * @param in 要从中复制的 String
	 * @param out 要复制到的 Writer
	 * @throws IOException 发生 I/O 错误时
	 */
	public static void copy(String in, Writer out) throws IOException {
		Assert.notNull(in, "No input String specified");
		Assert.notNull(out, "No Writer specified");

		try {
			out.write(in);
		}
		finally {
			close(out);
		}
	}

	/**
	 * 将给定 Reader 的内容复制到 String 中。完成后关闭该 reader。
	 * @param in 要从中复制的 reader（可以为 {@code null} 或为空）
	 * @return 已复制到的 String（可能为空）
	 * @throws IOException 发生 I/O 错误时
	 */
	public static String copyToString(@Nullable Reader in) throws IOException {
		if (in == null) {
			return "";
		}

		StringWriter out = new StringWriter(BUFFER_SIZE);
		copy(in, out);
		return out.toString();
	}

	/**
	 * 尝试关闭给定的 {@link Closeable}，并静默忽略任何异常。
	 * @param closeable 要关闭的 {@code Closeable}
	 */
	private static void close(Closeable closeable) {
		try {
			closeable.close();
		}
		catch (IOException ex) {
			// ignore
		}
	}

}
