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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于 {@code java.net.URL} 定位符的 {@link Resource} 实现。支持解析为 {@code URL}，在
 * {@code “file:”} 协议的情况下也支持解析为 {@code File}。
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see java.net.URL
 */
public class UrlResource extends AbstractFileResolvingResource {

	/**
	 * 原始 URI（如果可用）；用于 URI 和 File 访问。
	 */
	@Nullable
	private final URI uri;

	/**
	 * 原始 URL，用于实际访问。
	 */
	private final URL url;

	/**
	 * 清理后的 URL（路径已规范化），用于比较。
	 */
	@Nullable
	private volatile URL cleanedUrl;


	/**
	 * 基于给定 URL 对象创建一个新的 {@code UrlResource}。
	 * @param url a URL
	 * @see #UrlResource(URI)
	 * @see #UrlResource(String)
	 */
	public UrlResource(URL url) {
		Assert.notNull(url, "URL must not be null");
		this.uri = null;
		this.url = url;
	}

	/**
	 * 基于给定 URI 对象创建一个新的 {@code UrlResource}。
	 * @param uri a URI
	 * @throws MalformedURLException if the given URL path is not valid
	 * @since 2.5
	 */
	public UrlResource(URI uri) throws MalformedURLException {
		Assert.notNull(uri, "URI must not be null");
		this.uri = uri;
		this.url = uri.toURL();
	}

	/**
	 * 基于 URL 路径创建一个新的 {@code UrlResource}。
	 * <p>注意：如有必要，给定路径需要预先编码。
	 * @param path a URL path
	 * @throws MalformedURLException if the given URL path is not valid
	 * @see java.net.URL#URL(String)
	 */
	public UrlResource(String path) throws MalformedURLException {
		Assert.notNull(path, "Path must not be null");
		this.uri = null;
		this.url = new URL(path);
		this.cleanedUrl = getCleanedUrl(this.url, path);
	}

	/**
	 * 基于 URI 规范创建一个新的 {@code UrlResource}。
	 * <p>如有必要，给定各部分将自动进行编码。
	 * @param protocol the URL protocol to use (e.g. "jar" or "file" - without colon);
	 * also known as "scheme"
	 * @param location the location (e.g. the file path within that protocol);
	 * also known as "scheme-specific part"
	 * @throws MalformedURLException if the given URL specification is not valid
	 * @see java.net.URI#URI(String, String, String)
	 */
	public UrlResource(String protocol, String location) throws MalformedURLException  {
		this(protocol, location, null);
	}

	/**
	 * 基于 URI 规范创建一个新的 {@code UrlResource}。
	 * <p>如有必要，给定各部分将自动进行编码。
	 * @param protocol the URL protocol to use (e.g. "jar" or "file" - without colon);
	 * also known as "scheme"
	 * @param location the location (e.g. the file path within that protocol);
	 * also known as "scheme-specific part"
	 * @param fragment the fragment within that location (e.g. anchor on an HTML page,
	 * as following after a "#" separator)
	 * @throws MalformedURLException if the given URL specification is not valid
	 * @see java.net.URI#URI(String, String, String)
	 */
	public UrlResource(String protocol, String location, @Nullable String fragment) throws MalformedURLException  {
		try {
			this.uri = new URI(protocol, location, fragment);
			this.url = this.uri.toURL();
		}
		catch (URISyntaxException ex) {
			MalformedURLException exToThrow = new MalformedURLException(ex.getMessage());
			exToThrow.initCause(ex);
			throw exToThrow;
		}
	}


	/**
	 * 为给定的原始 URL 确定清理后的 URL。
	 * @param originalUrl the original URL
	 * @param originalPath the original URL path
	 * @return the cleaned URL (possibly the original URL as-is)
	 * @see org.springframework.util.StringUtils#cleanPath
	 */
	private static URL getCleanedUrl(URL originalUrl, String originalPath) {
		String cleanedPath = StringUtils.cleanPath(originalPath);
		if (!cleanedPath.equals(originalPath)) {
			try {
				return new URL(cleanedPath);
			}
			catch (MalformedURLException ex) {
				// Cleaned URL path cannot be converted to URL -> take original URL.
			}
		}
		return originalUrl;
	}

	/**
	 * 延迟为给定的原始 URL 确定清理后的 URL。
	 * @see #getCleanedUrl(URL, String)
	 */
	private URL getCleanedUrl() {
		URL cleanedUrl = this.cleanedUrl;
		if (cleanedUrl != null) {
			return cleanedUrl;
		}
		cleanedUrl = getCleanedUrl(this.url, (this.uri != null ? this.uri : this.url).toString());
		this.cleanedUrl = cleanedUrl;
		return cleanedUrl;
	}


	/**
	 * 此实现为给定 URL 打开一个 InputStream。
	 * <p>它将 {@code useCaches} 标志设置为 {@code false}，主要是为了避免在 Windows 上锁定 jar 文件。
	 * @see java.net.URL#openConnection()
	 * @see java.net.URLConnection#setUseCaches(boolean)
	 * @see java.net.URLConnection#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		URLConnection con = this.url.openConnection();
		customizeConnection(con);
		try {
			return con.getInputStream();
		}
		catch (IOException ex) {
			// Close the HTTP connection (if applicable).
			if (con instanceof HttpURLConnection) {
				((HttpURLConnection) con).disconnect();
			}
			throw ex;
		}
	}

	/**
	 * 此实现返回底层 URL 引用。
	 */
	@Override
	public URL getURL() {
		return this.url;
	}

	/**
	 * 如果可能，此实现直接返回底层 URI。
	 */
	@Override
	public URI getURI() throws IOException {
		if (this.uri != null) {
			return this.uri;
		}
		else {
			return super.getURI();
		}
	}

	@Override
	public boolean isFile() {
		if (this.uri != null) {
			return super.isFile(this.uri);
		}
		else {
			return super.isFile();
		}
	}

	/**
	 * 此实现返回底层 URL/URI 的 File 引用，前提是它指向文件系统中的一个文件。
	 * @see org.springframework.util.ResourceUtils#getFile(java.net.URL, String)
	 */
	@Override
	public File getFile() throws IOException {
		if (this.uri != null) {
			return super.getFile(this.uri);
		}
		else {
			return super.getFile();
		}
	}

	/**
	 * 此实现创建一个 {@code UrlResource}，并委托给 {@link #createRelativeURL(String)} 来适配相对路径。
	 * @see #createRelativeURL(String)
	 */
	@Override
	public Resource createRelative(String relativePath) throws MalformedURLException {
		return new UrlResource(createRelativeURL(relativePath));
	}

	/**
	 * 此委托创建一个 {@code java.net.URL}，将给定路径应用于此资源描述符底层 URL 的路径。前导斜杠将被丢
	 * 弃；“#” 符号将被编码。
	 * @since 5.2
	 * @see #createRelative(String)
	 * @see java.net.URL#URL(java.net.URL, String)
	 */
	protected URL createRelativeURL(String relativePath) throws MalformedURLException {
		if (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		// # can appear in filenames, java.net.URL should not treat it as a fragment
		relativePath = StringUtils.replace(relativePath, "#", "%23");
		// Use the URL constructor for applying the relative path as a URL spec
		return new URL(this.url, relativePath);
	}

	/**
	 * 此实现返回此 URL 所指向文件的名称。
	 * @see java.net.URL#getPath()
	 */
	@Override
	public String getFilename() {
		return StringUtils.getFilename(getCleanedUrl().getPath());
	}

	/**
	 * 此实现返回包含该 URL 的描述。
	 */
	@Override
	public String getDescription() {
		return "URL [" + this.url + "]";
	}


	/**
	 * 此实现比较底层 URL 引用。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof UrlResource &&
				getCleanedUrl().equals(((UrlResource) other).getCleanedUrl())));
	}

	/**
	 * 此实现返回底层 URL 引用的哈希码。
	 */
	@Override
	public int hashCode() {
		return getCleanedUrl().hashCode();
	}

}
