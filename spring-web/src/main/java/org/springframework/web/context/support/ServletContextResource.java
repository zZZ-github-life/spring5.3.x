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

package org.springframework.web.context.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;

import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * 用于 {@link javax.servlet.ServletContext} 资源的 {@link org.springframework.core.io.Resource}
 * 实现，会相对于 Web 应用根目录解释路径。
 *
 * <p>始终支持流访问和 URL 访问，但仅在 Web 应用归档已展开时才允许 {@code java.io.File} 访问。
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see javax.servlet.ServletContext#getResourceAsStream
 * @see javax.servlet.ServletContext#getResource
 * @see javax.servlet.ServletContext#getRealPath
 */
public class ServletContextResource extends AbstractFileResolvingResource implements ContextResource {

	private final ServletContext servletContext;

	private final String path;


	/**
	 * 创建一个新的 ServletContextResource。
	 * <p>Servlet 规范要求资源路径以斜杠开头，尽管许多容器也接受不带前导斜杠的路径。因此，如果给定路
	 * 径尚未以斜杠开头，则会为其前置一个斜杠。
	 * @param servletContext 要从中加载的 ServletContext
	 * @param path 资源的路径
	 */
	public ServletContextResource(ServletContext servletContext, String path) {
		// check ServletContext
		Assert.notNull(servletContext, "Cannot resolve ServletContextResource without ServletContext");
		this.servletContext = servletContext;

		// check path
		Assert.notNull(path, "Path is required");
		String pathToUse = StringUtils.cleanPath(path);
		if (!pathToUse.startsWith("/")) {
			pathToUse = "/" + pathToUse;
		}
		this.path = pathToUse;
	}


	/**
	 * 返回此资源的 ServletContext。
	 */
	public final ServletContext getServletContext() {
		return this.servletContext;
	}

	/**
	 * 返回此资源的路径。
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * 此实现检查 {@code ServletContext.getResource}。
	 * @see javax.servlet.ServletContext#getResource(String)
	 */
	@Override
	public boolean exists() {
		try {
			URL url = this.servletContext.getResource(this.path);
			return (url != null);
		}
		catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * 此实现委托给 {@code ServletContext.getResourceAsStream}，对于不可读的资源（例如目录），它会返
	 * 回 {@code null}。
	 * @see javax.servlet.ServletContext#getResourceAsStream(String)
	 */
	@Override
	public boolean isReadable() {
		InputStream is = this.servletContext.getResourceAsStream(this.path);
		if (is != null) {
			try {
				is.close();
			}
			catch (IOException ex) {
				// ignore
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isFile() {
		try {
			URL url = this.servletContext.getResource(this.path);
			if (url != null && ResourceUtils.isFileURL(url)) {
				return true;
			}
			else {
				String realPath = this.servletContext.getRealPath(this.path);
				if (realPath == null) {
					return false;
				}
				File file = new File(realPath);
				return (file.exists() && file.isFile());
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * 此实现委托给 {@code ServletContext.getResourceAsStream}，但如果没有找到资源则抛出
	 * FileNotFoundException。
	 * @see javax.servlet.ServletContext#getResourceAsStream(String)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = this.servletContext.getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException("Could not open " + getDescription());
		}
		return is;
	}

	/**
	 * 此实现委托给 {@code ServletContext.getResource}，但如果没有找到资源则抛出
	 * FileNotFoundException。
	 * @see javax.servlet.ServletContext#getResource(String)
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = this.servletContext.getResource(this.path);
		if (url == null) {
			throw new FileNotFoundException(
					getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * 此实现解析 "file:" URL，或者委托给 {@code ServletContext.getRealPath}；如果未找到或无法解析则
	 * 抛出 FileNotFoundException。
	 * @see javax.servlet.ServletContext#getResource(String)
	 * @see javax.servlet.ServletContext#getRealPath(String)
	 */
	@Override
	public File getFile() throws IOException {
		URL url = this.servletContext.getResource(this.path);
		if (url != null && ResourceUtils.isFileURL(url)) {
			// Proceed with file system resolution...
			return super.getFile();
		}
		else {
			String realPath = WebUtils.getRealPath(this.servletContext, this.path);
			return new File(realPath);
		}
	}

	/**
	 * 此实现创建一个 ServletContextResource，将给定路径应用于此资源描述符底层文件路径的相对位置。
	 * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new ServletContextResource(this.servletContext, pathToUse);
	}

	/**
	 * 此实现返回此 ServletContext 资源所指向的文件名称。
	 * @see org.springframework.util.StringUtils#getFilename(String)
	 */
	@Override
	@Nullable
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	/**
	 * 此实现返回包含 ServletContext 资源位置的描述。
	 */
	@Override
	public String getDescription() {
		return "ServletContext resource [" + this.path + "]";
	}

	@Override
	public String getPathWithinContext() {
		return this.path;
	}


	/**
	 * 此实现比较底层的 ServletContext 资源位置。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ServletContextResource)) {
			return false;
		}
		ServletContextResource otherRes = (ServletContextResource) other;
		return (this.servletContext.equals(otherRes.servletContext) && this.path.equals(otherRes.path));
	}

	/**
	 * 此实现返回底层 ServletContext 资源位置的哈希码。
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
