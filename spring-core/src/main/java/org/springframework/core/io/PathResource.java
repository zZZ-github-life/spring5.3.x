/*
 * Copyright 2002-2019 the original author or authors.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于 {@link java.nio.file.Path} 句柄的 {@link Resource} 实现，所有操作和转换都通过 {@code Path}
 * API 执行。支持解析为 {@link File}，也支持解析为 {@link URL}。实现了扩展的
 * {@link WritableResource} 接口。
 *
 * <p>注意：从 5.1 开始，{@link FileSystemResource#FileSystemResource(Path) FileSystemResource} 中
 * 也提供了 {@link java.nio.file.Path} 支持，它应用 Spring 标准的基于 String 的路径转换，但所有操作
 * 都通过 {@link java.nio.file.Files} API 执行。这个 {@code PathResource} 实际上是一个纯粹的基于
 * {@code java.nio.path.Path} 的替代方案，其 {@code createRelative} 行为有所不同。
 *
 * @author Philippe Marschall
 * @author Juergen Hoeller
 * @since 4.0
 * @see java.nio.file.Path
 * @see java.nio.file.Files
 * @see FileSystemResource
 */
public class PathResource extends AbstractResource implements WritableResource {

	private final Path path;


	/**
	 * 从 Path 句柄创建一个新的 PathResource。
	 * <p>注意：与 {@link FileSystemResource} 不同，通过 {@link #createRelative} 构建相对资源时，相对路
	 * 径将构建在给定根路径<i>之下</i>：例如 Paths.get(“C:/dir1/”)，相对路径 “dir2” &rarr;
	 * “C:/dir1/dir2”！
	 * @param path Path 句柄
	 */
	public PathResource(Path path) {
		Assert.notNull(path, "Path must not be null");
		this.path = path.normalize();
	}

	/**
	 * 从 Path 句柄创建一个新的 PathResource。
	 * <p>注意：与 {@link FileSystemResource} 不同，通过 {@link #createRelative} 构建相对资源时，相对路
	 * 径将构建在给定根路径<i>之下</i>：例如 Paths.get(“C:/dir1/”)，相对路径 “dir2” &rarr;
	 * “C:/dir1/dir2”！
	 * @param path 路径
	 * @see java.nio.file.Paths#get(String, String...)
	 */
	public PathResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = Paths.get(path).normalize();
	}

	/**
	 * 从 Path 句柄创建一个新的 PathResource。
	 * <p>注意：与 {@link FileSystemResource} 不同，通过 {@link #createRelative} 构建相对资源时，相对路
	 * 径将构建在给定根路径<i>之下</i>：例如 Paths.get(“C:/dir1/”)，相对路径 “dir2” &rarr;
	 * “C:/dir1/dir2”！
	 * @param uri 路径 URI
	 * @see java.nio.file.Paths#get(URI)
	 */
	public PathResource(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.path = Paths.get(uri).normalize();
	}


	/**
	 * 返回此资源的文件路径。
	 */
	public final String getPath() {
		return this.path.toString();
	}

	/**
	 * 此实现返回底层文件是否存在。
	 * @see java.nio.file.Files#exists(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean exists() {
		return Files.exists(this.path);
	}

	/**
	 * 此实现检查底层文件是否标记为可读（并且对应的是有内容的实际文件，而不是目录）。
	 * @see java.nio.file.Files#isReadable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isReadable() {
		return (Files.isReadable(this.path) && !Files.isDirectory(this.path));
	}

	/**
	 * 此实现为底层文件打开一个 InputStream。
	 * @see java.nio.file.spi.FileSystemProvider#newInputStream(Path, OpenOption...)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		if (!exists()) {
			throw new FileNotFoundException(getPath() + " (no such file or directory)");
		}
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		return Files.newInputStream(this.path);
	}

	/**
	 * 此实现检查底层文件是否标记为可写（并且对应的是有内容的实际文件，而不是目录）。
	 * @see java.nio.file.Files#isWritable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isWritable() {
		return (Files.isWritable(this.path) && !Files.isDirectory(this.path));
	}

	/**
	 * 此实现为底层文件打开一个 OutputStream。
	 * @see java.nio.file.spi.FileSystemProvider#newOutputStream(Path, OpenOption...)
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		return Files.newOutputStream(this.path);
	}

	/**
	 * 此实现返回底层文件的 URL。
	 * @see java.nio.file.Path#toUri()
	 * @see java.net.URI#toURL()
	 */
	@Override
	public URL getURL() throws IOException {
		return this.path.toUri().toURL();
	}

	/**
	 * 此实现返回底层文件的 URI。
	 * @see java.nio.file.Path#toUri()
	 */
	@Override
	public URI getURI() throws IOException {
		return this.path.toUri();
	}

	/**
	 * 此实现始终指示为文件。
	 */
	@Override
	public boolean isFile() {
		return true;
	}

	/**
	 * 此实现返回底层 File 引用。
	 */
	@Override
	public File getFile() throws IOException {
		try {
			return this.path.toFile();
		}
		catch (UnsupportedOperationException ex) {
			// Only paths on the default file system can be converted to a File:
			// Do exception translation for cases where conversion is not possible.
			throw new FileNotFoundException(this.path + " cannot be resolved to absolute file path");
		}
	}

	/**
	 * 此实现为底层文件打开一个 Channel。
	 * @see Files#newByteChannel(Path, OpenOption...)
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			return Files.newByteChannel(this.path, StandardOpenOption.READ);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * 此实现为底层文件打开一个 Channel。
	 * @see Files#newByteChannel(Path, OpenOption...)
	 */
	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return Files.newByteChannel(this.path, StandardOpenOption.WRITE);
	}

	/**
	 * 此实现返回底层文件的长度。
	 */
	@Override
	public long contentLength() throws IOException {
		return Files.size(this.path);
	}

	/**
	 * 此实现返回底层 File 的时间戳。
	 * @see java.nio.file.Files#getLastModifiedTime(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public long lastModified() throws IOException {
		// We can not use the superclass method since it uses conversion to a File and
		// only a Path on the default file system can be converted to a File...
		return Files.getLastModifiedTime(this.path).toMillis();
	}

	/**
	 * 此实现创建一个 PathResource，并将给定路径应用于此资源描述符底层文件的路径。
	 * @see java.nio.file.Path#resolve(String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		return new PathResource(this.path.resolve(relativePath));
	}

	/**
	 * 此实现返回文件的名称。
	 * @see java.nio.file.Path#getFileName()
	 */
	@Override
	public String getFilename() {
		return this.path.getFileName().toString();
	}

	@Override
	public String getDescription() {
		return "path [" + this.path.toAbsolutePath() + "]";
	}


	/**
	 * 此实现比较底层 Path 引用。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PathResource &&
				this.path.equals(((PathResource) other).path)));
	}

	/**
	 * 此实现返回底层 Path 引用的哈希码。
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
