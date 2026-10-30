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
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 针对以文件系统为目标的 {@code java.io.File} 和 {@code java.nio.file.Path} 句柄的
 * {@link Resource} 实现。支持解析为 {@code File} 以及 {@code URL}。实现了扩展的
 * {@link WritableResource} 接口。
 *
 * <p>注意：自 Spring Framework 5.0 起，此 {@link Resource} 实现使用 NIO.2 API 进行读写交互。自 5.1
 * 起，可以使用 {@link java.nio.file.Path} 句柄构造它，此时它将通过 NIO.2 执行所有文件系统交互，仅
 * 在 {@link #getFile()} 时才使用 {@link File}。
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see #FileSystemResource(String)
 * @see #FileSystemResource(File)
 * @see #FileSystemResource(Path)
 * @see java.io.File
 * @see java.nio.file.Files
 */
public class FileSystemResource extends AbstractResource implements WritableResource {

	private final String path;

	@Nullable
	private final File file;

	private final Path filePath;


	/**
	 * 从文件路径创建一个新的 {@code FileSystemResource}。
	 * <p>注意：通过 {@link #createRelative} 构建相对资源时，此处指定的资源基路径是否以斜杠结尾会产生不
	 * 同结果。对于 "C:/dir1/"，相对路径将在该根目录之下构建：例如相对路径 "dir2" &rarr; "C:/dir1/dir2"。
	 * 对于 "C:/dir1"，相对路径将在同一目录级别应用：相对路径 "dir2" &rarr; "C:/dir2"。
	 * @param path 文件路径
	 * @see #FileSystemResource(Path)
	 */
	public FileSystemResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.file = new File(path);
		this.filePath = this.file.toPath();
	}

	/**
	 * 从 {@link File} 句柄创建一个新的 {@code FileSystemResource}。
	 * <p>注意：通过 {@link #createRelative} 构建相对资源时，相对路径将<i>在同一目录级别</i>应用：例如
	 * new File("C:/dir1")，相对路径 "dir2" &rarr; "C:/dir2"！如果你希望相对路径在给定根目录之下构建，
	 * 请使用 {@link #FileSystemResource(String) constructor with a file path} 在根路径后附加结尾斜杠：
	 * "C:/dir1/"，这表示将该目录作为所有相对路径的根。
	 * @param file File 句柄
	 * @see #FileSystemResource(Path)
	 * @see #getFile()
	 */
	public FileSystemResource(File file) {
		Assert.notNull(file, "File must not be null");
		this.path = StringUtils.cleanPath(file.getPath());
		this.file = file;
		this.filePath = file.toPath();
	}

	/**
	 * 从 {@link Path} 句柄创建一个新的 {@code FileSystemResource}，通过 NIO.2 而不是 {@link File} 执行
	 * 所有文件系统交互。
	 * <p>与 {@link PathResource} 不同，此变体严格遵循 {@link FileSystemResource} 的一般约定，特别是在
	 * 路径清理和 {@link #createRelative(String)} 处理方面。
	 * <p>注意：通过 {@link #createRelative} 构建相对资源时，相对路径将<i>在同一目录级别</i>应用：例如
	 * Paths.get("C:/dir1")，相对路径 "dir2" &rarr; "C:/dir2"！如果你希望相对路径在给定根目录之下构建，
	 * 请使用 {@link #FileSystemResource(String) constructor with a file path} 在根路径后附加结尾斜杠：
	 * "C:/dir1/"，这表示将该目录作为所有相对路径的根。或者，考虑使用
	 * {@link PathResource#PathResource(Path)} 在 {@code createRelative} 中解析
	 * {@code java.nio.path.Path}，始终嵌套相对路径。
	 * @param filePath 指向文件的 Path 句柄
	 * @since 5.1
	 * @see #FileSystemResource(File)
	 */
	public FileSystemResource(Path filePath) {
		Assert.notNull(filePath, "Path must not be null");
		this.path = StringUtils.cleanPath(filePath.toString());
		this.file = null;
		this.filePath = filePath;
	}

	/**
	 * 从 {@link FileSystem} 句柄创建一个新的 {@code FileSystemResource}，定位指定的路径。
	 * <p>这是 {@link #FileSystemResource(String)} 的替代方案，通过 NIO.2 而不是 {@link File} 执行所有
	 * 文件系统交互。
	 * @param fileSystem 用于定位该路径的 FileSystem
	 * @param path 文件路径
	 * @since 5.1.1
	 * @see #FileSystemResource(File)
	 */
	public FileSystemResource(FileSystem fileSystem, String path) {
		Assert.notNull(fileSystem, "FileSystem must not be null");
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.file = null;
		this.filePath = fileSystem.getPath(this.path).normalize();
	}


	/**
	 * 返回此资源的文件路径。
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * 此实现返回底层文件是否存在。
	 * @see java.io.File#exists()
	 */
	@Override
	public boolean exists() {
		return (this.file != null ? this.file.exists() : Files.exists(this.filePath));
	}

	/**
	 * 此实现检查底层文件是否被标记为可读（并且对应的是实际包含内容的文件，而不是目录）。
	 * @see java.io.File#canRead()
	 * @see java.io.File#isDirectory()
	 */
	@Override
	public boolean isReadable() {
		return (this.file != null ? this.file.canRead() && !this.file.isDirectory() :
				Files.isReadable(this.filePath) && !Files.isDirectory(this.filePath));
	}

	/**
	 * 此实现为底层文件打开 NIO 文件流。
	 * @see java.io.FileInputStream
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return Files.newInputStream(this.filePath);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * 此实现检查底层文件是否被标记为可写（并且对应的是实际包含内容的文件，而不是目录）。
	 * @see java.io.File#canWrite()
	 * @see java.io.File#isDirectory()
	 */
	@Override
	public boolean isWritable() {
		return (this.file != null ? this.file.canWrite() && !this.file.isDirectory() :
				Files.isWritable(this.filePath) && !Files.isDirectory(this.filePath));
	}

	/**
	 * 此实现为底层文件打开 FileOutputStream。
	 * @see java.io.FileOutputStream
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return Files.newOutputStream(this.filePath);
	}

	/**
	 * 此实现返回底层文件的 URL。
	 * @see java.io.File#toURI()
	 */
	@Override
	public URL getURL() throws IOException {
		return (this.file != null ? this.file.toURI().toURL() : this.filePath.toUri().toURL());
	}

	/**
	 * 此实现返回底层文件的 URI。
	 * @see java.io.File#toURI()
	 */
	@Override
	public URI getURI() throws IOException {
		return (this.file != null ? this.file.toURI() : this.filePath.toUri());
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
	public File getFile() {
		return (this.file != null ? this.file : this.filePath.toFile());
	}

	/**
	 * 此实现为底层文件打开 FileChannel。
	 * @see java.nio.channels.FileChannel
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			return FileChannel.open(this.filePath, StandardOpenOption.READ);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * 此实现为底层文件打开 FileChannel。
	 * @see java.nio.channels.FileChannel
	 */
	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return FileChannel.open(this.filePath, StandardOpenOption.WRITE);
	}

	/**
	 * 此实现返回底层 File/Path 的长度。
	 */
	@Override
	public long contentLength() throws IOException {
		if (this.file != null) {
			long length = this.file.length();
			if (length == 0L && !this.file.exists()) {
				throw new FileNotFoundException(getDescription() +
						" cannot be resolved in the file system for checking its content length");
			}
			return length;
		}
		else {
			try {
				return Files.size(this.filePath);
			}
			catch (NoSuchFileException ex) {
				throw new FileNotFoundException(ex.getMessage());
			}
		}
	}

	/**
	 * 此实现返回底层 File/Path 的最后修改时间。
	 */
	@Override
	public long lastModified() throws IOException {
		if (this.file != null) {
			return super.lastModified();
		}
		else {
			try {
				return Files.getLastModifiedTime(this.filePath).toMillis();
			}
			catch (NoSuchFileException ex) {
				throw new FileNotFoundException(ex.getMessage());
			}
		}
	}

	/**
	 * 此实现创建一个 FileSystemResource，将给定路径应用于相对于此资源描述符底层文件路径的位置。
	 * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return (this.file != null ? new FileSystemResource(pathToUse) :
				new FileSystemResource(this.filePath.getFileSystem(), pathToUse));
	}

	/**
	 * 此实现返回文件名。
	 * @see java.io.File#getName()
	 */
	@Override
	public String getFilename() {
		return (this.file != null ? this.file.getName() : this.filePath.getFileName().toString());
	}

	/**
	 * 此实现返回包含文件绝对路径的描述。
	 * @see java.io.File#getAbsolutePath()
	 */
	@Override
	public String getDescription() {
		return "file [" + (this.file != null ? this.file.getAbsolutePath() : this.filePath.toAbsolutePath()) + "]";
	}


	/**
	 * 此实现比较底层 File 引用。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof FileSystemResource &&
				this.path.equals(((FileSystemResource) other).path)));
	}

	/**
	 * 此实现返回底层 File 引用的哈希码。
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
