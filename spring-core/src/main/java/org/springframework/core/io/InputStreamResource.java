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

package org.springframework.core.io;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 针对给定 {@link InputStream} 的 {@link Resource} 实现。
 * <p>仅当没有其他特定的 {@code Resource} 实现适用时才应使用。特别是，在可能的情况下优先使用
 * {@link ByteArrayResource} 或任何基于文件的 {@code Resource} 实现。
 *
 * <p>与其他 {@code Resource} 实现不同，这是一个针对<i>已经打开</i>的资源的描述符——因此
 * {@link #isOpen()} 会返回 {@code true}。如果你需要在某处保留资源描述符，或者需要多次从流中读取，
 * 请不要使用 {@code InputStreamResource}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * @see ByteArrayResource
 * @see ClassPathResource
 * @see FileSystemResource
 * @see UrlResource
 */
public class InputStreamResource extends AbstractResource {

	private final InputStream inputStream;

	private final String description;

	private boolean read = false;


	/**
	 * 创建一个新的 InputStreamResource。
	 * @param inputStream 要使用的 InputStream
	 */
	public InputStreamResource(InputStream inputStream) {
		this(inputStream, "resource loaded through InputStream");
	}

	/**
	 * 创建一个新的 InputStreamResource。
	 * @param inputStream 要使用的 InputStream
	 * @param description 该 InputStream 的来源
	 */
	public InputStreamResource(InputStream inputStream, @Nullable String description) {
		Assert.notNull(inputStream, "InputStream must not be null");
		this.inputStream = inputStream;
		this.description = (description != null ? description : "");
	}


	/**
	 * 此实现始终返回 {@code true}。
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * 此实现始终返回 {@code true}。
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	/**
	 * 如果尝试多次读取底层流，此实现将抛出 IllegalStateException。
	 */
	@Override
	public InputStream getInputStream() throws IOException, IllegalStateException {
		if (this.read) {
			throw new IllegalStateException("InputStream has already been read - " +
					"do not use InputStreamResource if a stream needs to be read multiple times");
		}
		this.read = true;
		return this.inputStream;
	}

	/**
	 * 此实现返回包含传入描述（如果有）的描述。
	 */
	@Override
	public String getDescription() {
		return "InputStream resource [" + this.description + "]";
	}


	/**
	 * 此实现比较底层 InputStream。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof InputStreamResource &&
				((InputStreamResource) other).inputStream.equals(this.inputStream)));
	}

	/**
	 * 此实现返回底层 InputStream 的哈希码。
	 */
	@Override
	public int hashCode() {
		return this.inputStream.hashCode();
	}

}
