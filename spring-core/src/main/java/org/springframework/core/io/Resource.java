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

package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.springframework.lang.Nullable;

/**
 * 用于资源描述符的接口，抽象了底层资源的实际类型，例如文件或类路径资源。
 *
 * <p>如果资源以物理形式存在，则可以为每个资源打开 InputStream，但某些资源只能返回 URL 或 File 句柄。
 * 实际行为取决于具体实现。
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 * @see WritableResource
 * @see ContextResource
 * @see UrlResource
 * @see FileUrlResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see ByteArrayResource
 * @see InputStreamResource
 */
public interface Resource extends InputStreamSource {

	/**
	 * 确定此资源是否以物理形式实际存在。
	 * <p>此方法执行确定性的存在性检查，而 {@code Resource} 句柄的存在仅保证一个有效的描述符句柄。
	 */
	boolean exists();

	/**
	 * 指示是否可以通过 {@link #getInputStream()} 读取此资源的非空内容。
	 * <p>对于存在的典型资源描述符，此值将为 {@code true}，因为从 5.1 起它严格蕴含 {@link #exists()} 语
	 * 义。请注意，实际尝试读取内容时仍可能失败。不过，值为 {@code false} 是资源内容无法读取的确定性指
	 * 示。
	 * @see #getInputStream()
	 * @see #exists()
	 */
	default boolean isReadable() {
		return exists();
	}

	/**
	 * 指示此资源是否表示一个带有打开流的句柄。如果为 {@code true}，则 InputStream 不能被多次读取，必须
	 * 读取并关闭以避免资源泄漏。
	 * <p>对于典型的资源描述符，此值将为 {@code false}。
	 */
	default boolean isOpen() {
		return false;
	}

	/**
	 * 确定此资源是否表示文件系统中的文件。
	 * <p>值为 {@code true} 强烈表明（但不保证）{@link #getFile()} 调用会成功。
	 * <p>默认情况下保守地返回 {@code false}。
	 * @since 5.0
	 * @see #getFile()
	 */
	default boolean isFile() {
		return false;
	}

	/**
	 * 返回此资源的 URL 句柄。
	 * @throws IOException if the resource cannot be resolved as URL,
	 * i.e. if the resource is not available as a descriptor
	 */
	URL getURL() throws IOException;

	/**
	 * 返回此资源的 URI 句柄。
	 * @throws IOException if the resource cannot be resolved as URI,
	 * i.e. if the resource is not available as a descriptor
	 * @since 2.5
	 */
	URI getURI() throws IOException;

	/**
	 * 返回此资源的 File 句柄。
	 * @throws java.io.FileNotFoundException if the resource cannot be resolved as
	 * absolute file path, i.e. if the resource is not available in a file system
	 * @throws IOException in case of general resolution/reading failures
	 * @see #getInputStream()
	 */
	File getFile() throws IOException;

	/**
	 * 返回一个 {@link ReadableByteChannel}。
	 * <p>预期每次调用都会创建一个<i>全新的</i>通道。
	 * <p>默认实现返回带有 {@link #getInputStream()} 结果的 {@link Channels#newChannel(InputStream)}。
	 * @return the byte channel for the underlying resource (must not be {@code null})
	 * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
	 * @throws IOException if the content channel could not be opened
	 * @since 5.0
	 * @see #getInputStream()
	 */
	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 确定此资源的内容长度。
	 * @throws IOException if the resource cannot be resolved
	 * (in the file system or as some other known physical resource type)
	 */
	long contentLength() throws IOException;

	/**
	 * 确定此资源的最后修改时间戳。
	 * @throws IOException if the resource cannot be resolved
	 * (in the file system or as some other known physical resource type)
	 */
	long lastModified() throws IOException;

	/**
	 * 创建一个相对于此资源的资源。
	 * @param relativePath the relative path (relative to this resource)
	 * @return the resource handle for the relative resource
	 * @throws IOException if the relative resource cannot be determined
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * 确定此资源的文件名，即通常是路径的最后部分：例如 “myfile.txt”。
	 * <p>如果此类资源没有文件名，则返回 {@code null}。
	 */
	@Nullable
	String getFilename();

	/**
	 * 返回此资源的描述，用于在处理该资源时输出错误信息。
	 * <p>还鼓励实现在其 {@code toString} 方法中返回此值。
	 * @see Object#toString()
	 */
	String getDescription();

}
