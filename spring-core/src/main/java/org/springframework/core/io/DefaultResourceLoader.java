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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ResourceLoader} 接口的默认实现。
 *
 * <p>由 {@link ResourceEditor} 使用，并作为
 * {@link org.springframework.context.support.AbstractApplicationContext} 的基类。也可独立使用。
 *
 * <p>如果位置值是 URL，则返回 {@link UrlResource}；如果是非 URL 路径或 "classpath:" 伪 URL，则返回
 * {@link ClassPathResource}。
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see FileSystemResourceLoader
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 */
public class DefaultResourceLoader implements ResourceLoader {

	@Nullable
	private ClassLoader classLoader;

	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

	private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);


	/**
	 * 创建一个新的 DefaultResourceLoader。
	 * <p>自 5.3 起，ClassLoader 访问将在实际访问资源时使用线程上下文类加载器。如需更多控制，请将特定的
	 * ClassLoader 传递给 {@link #DefaultResourceLoader(ClassLoader)}。
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public DefaultResourceLoader() {
	}

	/**
	 * 创建一个新的 DefaultResourceLoader。
	 * @param classLoader the ClassLoader to load class path resources with, or {@code null}
	 * for using the thread context class loader at the time of actual resource access
	 */
	public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 指定用于加载类路径资源的 ClassLoader；若为 {@code null}，则在实际访问资源时使用线程上下文类加载
	 * 器。
	 * <p>默认情况下，自 5.3 起 ClassLoader 访问将在实际访问资源时使用线程上下文类加载器。
	 */
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * 返回用于加载类路径资源的 ClassLoader。
	 * <p>它将被传递给 ClassPathResource 的构造函数，用于由此资源加载器创建的所有 ClassPathResource 对
	 * 象。
	 * @see ClassPathResource
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 向此资源加载器注册给定的解析器，以处理额外的协议。
	 * <p>任何此类解析器都将在此加载器的标准解析规则之前被调用。因此它也可能覆盖任何默认规则。
	 * @since 4.3
	 * @see #getProtocolResolvers()
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	/**
	 * 返回当前已注册的协议解析器集合，既可用于内省，也可用于修改。
	 * @since 4.3
	 * @see #addProtocolResolver(ProtocolResolver)
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	/**
	 * 获取针对给定值类型的缓存，以 {@link Resource} 为键。
	 * @param valueType the value type, e.g. an ASM {@code MetadataReader}
	 * @return the cache {@link Map}, shared at the {@code ResourceLoader} level
	 * @since 5.0
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
		return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, new Function<Class<?>, Map<Resource, ?>>() {
			@Override
			public Map<Resource, ?> apply(Class<?> key) {
				return new ConcurrentHashMap<>();
			}
		});
	}

	/**
	 * 清除此资源加载器中的所有资源缓存。
	 * @since 5.0
	 * @see #getResourceCache
	 */
	public void clearResourceCaches() {
		this.resourceCaches.clear();
	}


	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");

		for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}

		if (location.startsWith("/")) {
			return getResourceByPath(location);
		}
		else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		else {
			try {
				// Try to parse the location as a URL...
				URL url = new URL(location);
				return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
			}
			catch (MalformedURLException ex) {
				// No URL -> resolve as resource path.
				return getResourceByPath(location);
			}
		}
	}

	/**
	 * 返回给定路径处资源的 Resource 句柄。
	 * <p>默认实现支持类路径位置。这应适用于独立实现，但也可以覆盖，例如针对 Servlet 容器的实现。
	 * @param path the path to the resource
	 * @return the corresponding Resource handle
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * 通过实现 ContextResource 接口显式表达上下文相对路径的 ClassPathResource。
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

}
