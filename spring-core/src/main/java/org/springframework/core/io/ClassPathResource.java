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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 类路径资源的 {@link Resource} 实现。使用给定的 {@link ClassLoader} 或给定的 {@link Class} 来加载
 * 资源。
 *
 * <p>如果类路径资源位于文件系统中，则支持解析为 {@code java.io.File}，但不支持 JAR 中的资源。始终
 * 支持解析为 URL。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * @see ClassLoader#getResourceAsStream(String)
 * @see Class#getResourceAsStream(String)
 */
public class ClassPathResource extends AbstractFileResolvingResource {

	private final String path;

	@Nullable
	private ClassLoader classLoader;

	@Nullable
	private Class<?> clazz;


	/**
	 * 为 {@code ClassLoader} 用法创建一个新的 {@code ClassPathResource}。开头的斜杠将被移除，因为
	 * ClassLoader 的资源访问方法不接受它。
	 * <p>将使用线程上下文类加载器来加载资源。
	 * @param path 类路径内的绝对路径
	 * @see java.lang.ClassLoader#getResourceAsStream(String)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	public ClassPathResource(String path) {
		this(path, (ClassLoader) null);
	}

	/**
	 * 为 {@code ClassLoader} 用法创建一个新的 {@code ClassPathResource}。开头的斜杠将被移除，因为
	 * ClassLoader 的资源访问方法不接受它。
	 * @param path 类路径内的绝对路径
	 * @param classLoader 用于加载资源的类加载器；
	 * 若为 {@code null} 则使用线程上下文类加载器
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	public ClassPathResource(String path, @Nullable ClassLoader classLoader) {
		Assert.notNull(path, "Path must not be null");
		String pathToUse = StringUtils.cleanPath(path);
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		this.path = pathToUse;
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 为 {@code Class} 用法创建一个新的 {@code ClassPathResource}。路径可以相对于给定的类，也可以通过
	 * 前导斜杠表示类路径内的绝对路径。
	 * @param path 类路径内的相对或绝对路径
	 * @param clazz 用于加载资源的类
	 * @see java.lang.Class#getResourceAsStream
	 */
	public ClassPathResource(String path, @Nullable Class<?> clazz) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.clazz = clazz;
	}

	/**
	 * 创建一个带有可选 {@code ClassLoader} 和 {@code Class} 的新 {@code ClassPathResource}。仅供内部使
	 * 用。
	 * @param path 类路径内的相对或绝对路径
	 * @param classLoader 用于加载资源的类加载器（如果有）
	 * @param clazz 用于加载资源的类（如果有）
	 * @deprecated as of 4.3.13, in favor of selective use of
	 * {@link #ClassPathResource(String, ClassLoader)} vs {@link #ClassPathResource(String, Class)}
	 */
	@Deprecated
	protected ClassPathResource(String path, @Nullable ClassLoader classLoader, @Nullable Class<?> clazz) {
		this.path = StringUtils.cleanPath(path);
		this.classLoader = classLoader;
		this.clazz = clazz;
	}


	/**
	 * 返回此资源的路径（作为类路径内的资源路径）。
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * 返回将从中获取此资源的 ClassLoader。
	 */
	@Nullable
	public final ClassLoader getClassLoader() {
		return (this.clazz != null ? this.clazz.getClassLoader() : this.classLoader);
	}


	/**
	 * 此实现检查资源 URL 的解析情况。
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	@Override
	public boolean exists() {
		return (resolveURL() != null);
	}

	/**
	 * 此实现在开始时就检查资源 URL 的解析，然后继续执行 {@link AbstractFileResolvingResource} 的长度检
	 * 查。
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	@Override
	public boolean isReadable() {
		URL url = resolveURL();
		return (url != null && checkReadable(url));
	}

	/**
	 * 为底层类路径资源解析 URL。
	 * @return 解析后的 URL；如果无法解析则为 {@code null}
	 */
	@Nullable
	protected URL resolveURL() {
		try {
			if (this.clazz != null) {
				return this.clazz.getResource(this.path);
			}
			else if (this.classLoader != null) {
				return this.classLoader.getResource(this.path);
			}
			else {
				return ClassLoader.getSystemResource(this.path);
			}
		}
		catch (IllegalArgumentException ex) {
			// Should not happen according to the JDK's contract:
			// see https://github.com/openjdk/jdk/pull/2662
			return null;
		}
	}

	/**
	 * 此实现为给定的类路径资源打开 InputStream。
	 * @see java.lang.ClassLoader#getResourceAsStream(String)
	 * @see java.lang.Class#getResourceAsStream(String)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is;
		if (this.clazz != null) {
			is = this.clazz.getResourceAsStream(this.path);
		}
		else if (this.classLoader != null) {
			is = this.classLoader.getResourceAsStream(this.path);
		}
		else {
			is = ClassLoader.getSystemResourceAsStream(this.path);
		}
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	/**
	 * 此实现返回底层类路径资源的 URL（如果可用）。
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = resolveURL();
		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * 此实现创建一个 ClassPathResource，将给定路径应用于相对于此描述符底层资源路径的位置。
	 * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return (this.clazz != null ? new ClassPathResource(pathToUse, this.clazz) :
				new ClassPathResource(pathToUse, this.classLoader));
	}

	/**
	 * 此实现返回此类路径资源所引用的文件名。
	 * @see org.springframework.util.StringUtils#getFilename(String)
	 */
	@Override
	@Nullable
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	/**
	 * 此实现返回包含类路径位置的描述。
	 */
	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder("class path resource [");
		String pathToUse = this.path;
		if (this.clazz != null && !pathToUse.startsWith("/")) {
			builder.append(ClassUtils.classPackageAsResourcePath(this.clazz));
			builder.append('/');
		}
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		builder.append(pathToUse);
		builder.append(']');
		return builder.toString();
	}


	/**
	 * 此实现比较底层类路径位置。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ClassPathResource)) {
			return false;
		}
		ClassPathResource otherRes = (ClassPathResource) other;
		return (this.path.equals(otherRes.path) &&
				ObjectUtils.nullSafeEquals(this.classLoader, otherRes.classLoader) &&
				ObjectUtils.nullSafeEquals(this.clazz, otherRes.clazz));
	}

	/**
	 * 此实现返回底层类路径位置的哈希码。
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
