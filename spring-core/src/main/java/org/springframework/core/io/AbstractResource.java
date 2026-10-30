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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * {@link Resource} 实现的便捷基类，预先实现了典型行为。
 *
 * <p>"exists" 方法将检查能否打开 File 或 InputStream；"isOpen" 始终返回 false；"getURL" 和
 * "getFile" 会抛出异常；"toString" 将返回描述。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 */
public abstract class AbstractResource implements Resource {

	/**
	 * 此实现检查能否打开 File，若不行则回退为检查能否打开 InputStream。
	 * <p>这既涵盖目录，也涵盖内容资源。
	 */
	@Override
	public boolean exists() {
		// Try file existence: can we find the file in the file system?
		if (isFile()) {
			try {
				return getFile().exists();
			}
			catch (IOException ex) {
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Could not retrieve File for existence check of " + getDescription(), ex);
				}
			}
		}
		// Fall back to stream existence: can we open the stream?
		try {
			getInputStream().close();
			return true;
		}
		catch (Throwable ex) {
			Log logger = LogFactory.getLog(getClass());
			if (logger.isDebugEnabled()) {
				logger.debug("Could not retrieve InputStream for existence check of " + getDescription(), ex);
			}
			return false;
		}
	}

	/**
	 * 对于 {@link #exists() exists} 的资源，此实现始终返回 {@code true}（自 5.1 起修订）。
	 */
	@Override
	public boolean isReadable() {
		return exists();
	}

	/**
	 * 此实现始终返回 {@code false}。
	 */
	@Override
	public boolean isOpen() {
		return false;
	}

	/**
	 * 此实现始终返回 {@code false}。
	 */
	@Override
	public boolean isFile() {
		return false;
	}

	/**
	 * 此实现抛出 FileNotFoundException，假定该资源无法解析为 URL。
	 */
	@Override
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}

	/**
	 * 此实现基于 {@link #getURL()} 返回的 URL 构建 URI。
	 */
	@Override
	@SuppressWarnings("deprecation")
	public URI getURI() throws IOException {
		URL url = getURL();
		try {
			return ResourceUtils.toURI(url);
		}
		catch (URISyntaxException ex) {
			throw new org.springframework.core.NestedIOException("Invalid URI [" + url + "]", ex);
		}
	}

	/**
	 * 此实现抛出 FileNotFoundException，假定该资源无法解析为绝对文件路径。
	 */
	@Override
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}

	/**
	 * 此实现使用 {@link #getInputStream()} 的结果返回 {@link Channels#newChannel(InputStream)}。
	 * <p>这与 {@link Resource} 中对应的默认方法相同，但在此处镜像实现，以便在类层次结构中进行高效的
	 * JVM 级分派。
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 此方法会读取整个 InputStream 以确定内容长度。
	 * <p>对于 {@code InputStreamResource} 的自定义子类，我们强烈建议用更优的实现覆盖此方法，例如检查
	 * File 长度，或者在流只能读取一次时直接返回 -1。
	 * @see #getInputStream()
	 */
	@Override
	public long contentLength() throws IOException {
		InputStream is = getInputStream();
		try {
			long size = 0;
			byte[] buf = new byte[256];
			int read;
			while ((read = is.read(buf)) != -1) {
				size += read;
			}
			return size;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ex) {
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Could not close content-length InputStream for " + getDescription(), ex);
				}
			}
		}
	}

	/**
	 * 此实现检查底层 File 的时间戳（如果可用）。
	 * @see #getFileForLastModifiedCheck()
	 */
	@Override
	public long lastModified() throws IOException {
		File fileToCheck = getFileForLastModifiedCheck();
		long lastModified = fileToCheck.lastModified();
		if (lastModified == 0L && !fileToCheck.exists()) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for checking its last-modified timestamp");
		}
		return lastModified;
	}

	/**
	 * 确定用于时间戳检查的 File。
	 * <p>默认实现委托给 {@link #getFile()}。
	 * @return 用于时间戳检查的 File（永不为 {@code null}）
	 * @throws FileNotFoundException 如果资源无法解析为绝对文件路径，
	 * 即该资源在文件系统中不可用
	 * @throws IOException 在一般性的解析/读取失败时
	 */
	protected File getFileForLastModifiedCheck() throws IOException {
		return getFile();
	}

	/**
	 * 此实现抛出 FileNotFoundException，假定无法为此资源创建相对资源。
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
	}

	/**
	 * 此实现始终返回 {@code null}，假定此资源类型没有文件名。
	 */
	@Override
	@Nullable
	public String getFilename() {
		return null;
	}


	/**
	 * 此实现比较描述字符串。
	 * @see #getDescription()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof Resource &&
				((Resource) other).getDescription().equals(getDescription())));
	}

	/**
	 * 此实现返回描述的哈希码。
	 * @see #getDescription()
	 */
	@Override
	public int hashCode() {
		return getDescription().hashCode();
	}

	/**
	 * 此实现返回此资源的描述。
	 * @see #getDescription()
	 */
	@Override
	public String toString() {
		return getDescription();
	}

}
