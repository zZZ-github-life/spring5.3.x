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

package org.springframework.http.codec.multipart;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * 用于处理 multipart 解析的各种静态工具方法。
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class MultipartUtils {

	/**
	 * 返回给定头的字符集，如
	 * {@link HttpHeaders#getContentType()} 头中所定义。
	 */
	public static Charset charset(HttpHeaders headers) {
		MediaType contentType = headers.getContentType();
		if (contentType != null) {
			Charset charset = contentType.getCharset();
			if (charset != null) {
				return charset;
			}
		}
		return StandardCharsets.UTF_8;
	}

	/**
	 * 拼接给定的字节数组数组。
	 */
	public static byte[] concat(byte[]... byteArrays) {
		int len = 0;
		for (byte[] byteArray : byteArrays) {
			len += byteArray.length;
		}
		byte[] result = new byte[len];
		len = 0;
		for (byte[] byteArray : byteArrays) {
			System.arraycopy(byteArray, 0, result, len, byteArray.length);
			len += byteArray.length;
		}
		return result;
	}

	/**
	 * 将给定缓冲区切分到给定索引（不含该索引）。
	 */
	public static DataBuffer sliceTo(DataBuffer buf, int idx) {
		int pos = buf.readPosition();
		int len = idx - pos + 1;
		return buf.retainedSlice(pos, len);
	}

	/**
	 * 从给定索引（含该索引）开始切分给定缓冲区。
	 */
	public static DataBuffer sliceFrom(DataBuffer buf, int idx) {
		int len = buf.writePosition() - idx - 1;
		return buf.retainedSlice(idx + 1, len);
	}

	public static void closeChannel(Channel channel) {
		try {
			if (channel.isOpen()) {
				channel.close();
			}
		}
		catch (IOException ignore) {
		}
	}

	public static void deleteFile(Path file) {
		try {
			Files.delete(file);
		}
		catch (IOException ignore) {
		}
	}

}
