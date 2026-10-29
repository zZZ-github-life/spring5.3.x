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

package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * {@link UrlResource} 的子类，假定进行文件解析，并为此实现了 {@link WritableResource} 接口。此资源
 * 变体还会缓存从 {@link #getFile()} 解析得到的 {@link File} 句柄。
 *
 * <p>这是 {@link DefaultResourceLoader} 为 "file:..." URL 位置解析出的类，允许将其向下转型为
 * {@link WritableResource}。
 *
 * <p>或者，如需从 {@link java.io.File} 句柄或 NIO {@link java.nio.file.Path} 直接构造，请考虑使用
 * {@link FileSystemResource}。
 *
 * @author Juergen Hoeller
 * @since 5.0.2
 */
public class FileUrlResource extends UrlResource implements WritableResource {

	@Nullable
	private volatile File file;


	/**
	 * 基于给定的 URL 对象创建一个新的 {@code FileUrlResource}。
	 * <p>请注意，这并不强制要求 "file" 作为 URL 协议。如果某个协议已知可解析为文件，那么用于此目的是可
	 * 以接受的。
	 * @param url a URL
	 * @see ResourceUtils#isFileURL(URL)
	 * @see #getFile()
	 */
	public FileUrlResource(URL url) {
		super(url);
	}

	/**
	 * 基于给定的文件位置创建一个新的 {@code FileUrlResource}，使用 URL 协议 "file"。
	 * <p>给定的各部分将在必要时自动进行编码。
	 * @param location the location (i.e. the file path within that protocol)
	 * @throws MalformedURLException if the given URL specification is not valid
	 * @see UrlResource#UrlResource(String, String)
	 * @see ResourceUtils#URL_PROTOCOL_FILE
	 */
	public FileUrlResource(String location) throws MalformedURLException {
		super(ResourceUtils.URL_PROTOCOL_FILE, location);
	}


	@Override
	public File getFile() throws IOException {
		File file = this.file;
		if (file != null) {
			return file;
		}
		file = super.getFile();
		this.file = file;
		return file;
	}

	@Override
	public boolean isWritable() {
		try {
			File file = getFile();
			return (file.canWrite() && !file.isDirectory());
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return Files.newOutputStream(getFile().toPath());
	}

	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return FileChannel.open(getFile().toPath(), StandardOpenOption.WRITE);
	}

	@Override
	public Resource createRelative(String relativePath) throws MalformedURLException {
		return new FileUrlResource(createRelativeURL(relativePath));
	}

}
