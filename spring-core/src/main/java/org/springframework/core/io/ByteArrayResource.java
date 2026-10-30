/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 针对给定字节数组的 {@link Resource} 实现。
 * <p>为给定的字节数组创建 {@link ByteArrayInputStream}。
 *
 * <p>适用于从任意给定字节数组加载内容，而无需退而使用一次性的 {@link InputStreamResource}。在从本
 * 地内容创建邮件附件时尤其有用，因为 JavaMail 需要能够多次读取该流。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2.3
 * @see java.io.ByteArrayInputStream
 * @see InputStreamResource
 * @see org.springframework.mail.javamail.MimeMessageHelper#addAttachment(String, InputStreamSource)
 */
public class ByteArrayResource extends AbstractResource {

	private final byte[] byteArray;

	private final String description;


	/**
	 * 创建一个新的 {@code ByteArrayResource}。
	 * @param byteArray 要包装的字节数组
	 */
	public ByteArrayResource(byte[] byteArray) {
		this(byteArray, "resource loaded from byte array");
	}

	/**
	 * 创建一个带描述的 {@code ByteArrayResource}。
	 * @param byteArray 要包装的字节数组
	 * @param description 字节数组的来源
	 */
	public ByteArrayResource(byte[] byteArray, @Nullable String description) {
		Assert.notNull(byteArray, "Byte array must not be null");
		this.byteArray = byteArray;
		this.description = (description != null ? description : "");
	}


	/**
	 * 返回底层字节数组。
	 */
	public final byte[] getByteArray() {
		return this.byteArray;
	}

	/**
	 * 此实现始终返回 {@code true}。
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * 此实现返回底层字节数组的长度。
	 */
	@Override
	public long contentLength() {
		return this.byteArray.length;
	}

	/**
	 * 此实现为底层字节数组返回 ByteArrayInputStream。
	 * @see java.io.ByteArrayInputStream
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.byteArray);
	}

	/**
	 * 此实现返回包含传入的 {@code description}（如果有）的描述。
	 */
	@Override
	public String getDescription() {
		return "Byte array resource [" + this.description + "]";
	}


	/**
	 * 此实现比较底层字节数组。
	 * @see java.util.Arrays#equals(byte[], byte[])
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ByteArrayResource &&
				Arrays.equals(((ByteArrayResource) other).byteArray, this.byteArray)));
	}

	/**
	 * 此实现返回基于底层字节数组的哈希码。
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.byteArray);
	}

}
