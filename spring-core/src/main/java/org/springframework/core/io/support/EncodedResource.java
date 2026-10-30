/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.io.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 将 {@link Resource} 描述符与用于从资源读取的特定编码或 {@code Charset} 组合在一起的持有者。
 *
 * <p>用作支持以特定编码读取内容的操作的参数，通常通过 {@code java.io.Reader}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2.6
 * @see Resource#getInputStream()
 * @see java.io.Reader
 * @see java.nio.charset.Charset
 */
public class EncodedResource implements InputStreamSource {

	private final Resource resource;

	@Nullable
	private final String encoding;

	@Nullable
	private final Charset charset;


	/**
	 * 为给定的 {@code Resource} 创建一个新的 {@code EncodedResource}，不指定显式编码或 {@code Charset}。
	 *
	 * @param resource 要持有的 {@code Resource}（永不为 {@code null}）
	 */
	public EncodedResource(Resource resource) {
		this(resource, null, null);
	}

	/**
	 * 为给定的 {@code Resource} 创建一个新的 {@code EncodedResource}，使用指定的 {@code encoding}。
	 * @param resource 要持有的 {@code Resource}（永不为 {@code null}）
	 * @param encoding 从资源读取时使用的编码
	 */
	public EncodedResource(Resource resource, @Nullable String encoding) {
		this(resource, encoding, null);
	}

	/**
	 * 为给定的 {@code Resource} 创建一个新的 {@code EncodedResource}，使用指定的 {@code Charset}。
	 * @param resource 要持有的 {@code Resource}（永不为 {@code null}）
	 * @param charset 从资源读取时使用的 {@code Charset}
	 */
	public EncodedResource(Resource resource, @Nullable Charset charset) {
		this(resource, null, charset);
	}

	private EncodedResource(Resource resource, @Nullable String encoding, @Nullable Charset charset) {
		super();
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
		this.encoding = encoding;
		this.charset = charset;
	}


	/**
	 * 返回此 {@code EncodedResource} 持有的 {@code Resource}。
	 */
	public final Resource getResource() {
		return this.resource;
	}

	/**
	 * 返回用于从 {@linkplain #getResource() resource} 读取的编码，如果未指定则返回 {@code null}。
	 */
	@Nullable
	public final String getEncoding() {
		return this.encoding;
	}

	/**
	 * 返回用于从 {@linkplain #getResource() resource} 读取的 {@code Charset}，如果未指定则返回
	 * {@code null}。
	 */
	@Nullable
	public final Charset getCharset() {
		return this.charset;
	}

	/**
	 * 确定是否需要 {@link Reader} 而不是 {@link InputStream}，即是否已指定
	 * {@linkplain #getEncoding() encoding} 或 {@link #getCharset() Charset}。
	 * @see #getReader()
	 * @see #getInputStream()
	 */
	public boolean requiresReader() {
		return (this.encoding != null || this.charset != null);
	}

	/**
	 * 为指定的资源打开一个 {@code java.io.Reader}，使用指定的 {@link #getCharset() Charset} 或
	 * {@linkplain #getEncoding() encoding}（如果有）。
	 * @throws IOException 如果打开 Reader 失败
	 * @see #requiresReader()
	 * @see #getInputStream()
	 */
	public Reader getReader() throws IOException {
		if (this.charset != null) {
			return new InputStreamReader(this.resource.getInputStream(), this.charset);
		}
		else if (this.encoding != null) {
			return new InputStreamReader(this.resource.getInputStream(), this.encoding);
		}
		else {
			return new InputStreamReader(this.resource.getInputStream());
		}
	}

	/**
	 * 为指定的资源打开一个 {@code InputStream}，忽略任何指定的 {@link #getCharset() Charset} 或
	 * {@linkplain #getEncoding() encoding}。
	 * @throws IOException 如果打开 InputStream 失败
	 * @see #requiresReader()
	 * @see #getReader()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return this.resource.getInputStream();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof EncodedResource)) {
			return false;
		}
		EncodedResource otherResource = (EncodedResource) other;
		return (this.resource.equals(otherResource.resource) &&
				ObjectUtils.nullSafeEquals(this.charset, otherResource.charset) &&
				ObjectUtils.nullSafeEquals(this.encoding, otherResource.encoding));
	}

	@Override
	public int hashCode() {
		return this.resource.hashCode();
	}

	@Override
	public String toString() {
		return this.resource.toString();
	}

}
