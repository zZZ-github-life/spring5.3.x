/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Map;
import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link PropertiesPropertySource} 的子类，从给定的 {@link org.springframework.core.io.Resource}
 * 或资源位置（例如 {@code "classpath:/com/myco/foo.properties"}
 * 或 {@code "file:/path/to/file.xml"}）加载 {@link Properties} 对象。
 *
 * <p>同时支持传统的和基于 XML 的属性文件格式；不过，
 * 要使 XML 处理生效，底层 {@code Resource} 的
 * {@link org.springframework.core.io.Resource#getFilename() getFilename()} 方法必须
 * 返回一个以 {@code ".xml"} 结尾的非 {@code null} 值。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.support.EncodedResource
 */
public class ResourcePropertySource extends PropertiesPropertySource {

	/** 原始资源名称（如果与给定名称不同）。 */
	@Nullable
	private final String resourceName;


	/**
	 * 根据从给定编码资源加载的 Properties，
	 * 创建一个具有给定名称的 PropertySource。
	 */
	public ResourcePropertySource(String name, EncodedResource resource) throws IOException {
		super(name, PropertiesLoaderUtils.loadProperties(resource));
		this.resourceName = getNameForResource(resource.getResource());
	}

	/**
	 * 根据从给定资源加载的 Properties 创建 PropertySource。
	 * PropertySource 的名称将根据
	 * 给定资源的 {@link Resource#getDescription() description} 生成。
	 */
	public ResourcePropertySource(EncodedResource resource) throws IOException {
		super(getNameForResource(resource.getResource()), PropertiesLoaderUtils.loadProperties(resource));
		this.resourceName = null;
	}

	/**
	 * 根据从给定编码资源加载的 Properties，
	 * 创建一个具有给定名称的 PropertySource。
	 */
	public ResourcePropertySource(String name, Resource resource) throws IOException {
		super(name, PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
		this.resourceName = getNameForResource(resource);
	}

	/**
	 * 根据从给定资源加载的 Properties 创建 PropertySource。
	 * PropertySource 的名称将根据
	 * 给定资源的 {@link Resource#getDescription() description} 生成。
	 */
	public ResourcePropertySource(Resource resource) throws IOException {
		super(getNameForResource(resource), PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
		this.resourceName = null;
	}

	/**
	 * 根据从给定资源位置加载的 Properties，创建一个具有给定名称的 PropertySource，
	 * 并使用给定的类加载器加载该
	 * 资源（假定其带有 {@code classpath:} 前缀）。
	 */
	public ResourcePropertySource(String name, String location, ClassLoader classLoader) throws IOException {
		this(name, new DefaultResourceLoader(classLoader).getResource(location));
	}

	/**
	 * 根据从给定资源位置加载的 Properties 创建 PropertySource，
	 * 并使用给定的类加载器加载该资源，假定其带有
	 * {@code classpath:} 前缀。PropertySource 的名称将
	 * 根据资源的 {@link Resource#getDescription() description}
	 * 生成。
	 */
	public ResourcePropertySource(String location, ClassLoader classLoader) throws IOException {
		this(new DefaultResourceLoader(classLoader).getResource(location));
	}

	/**
	 * 根据从给定资源位置加载的 Properties，创建一个具有给定名称的 PropertySource。
	 * 将使用默认的线程上下文类加载器
	 * 加载该资源（假定位置字符串带有
	 * {@code classpath:} 前缀）。
	 */
	public ResourcePropertySource(String name, String location) throws IOException {
		this(name, new DefaultResourceLoader().getResource(location));
	}

	/**
	 * 根据从给定资源位置加载的 Properties 创建 PropertySource。
	 * PropertySource 的名称将根据资源的
	 * {@link Resource#getDescription() description} 生成。
	 */
	public ResourcePropertySource(String location) throws IOException {
		this(new DefaultResourceLoader().getResource(location));
	}

	private ResourcePropertySource(String name, @Nullable String resourceName, Map<String, Object> source) {
		super(name, source);
		this.resourceName = resourceName;
	}


	/**
	 * 返回此 {@link ResourcePropertySource} 的一个可能经过适配的变体，
	 * 用指定名称覆盖先前给定（或派生）的名称。
	 * @since 4.0.4
	 */
	public ResourcePropertySource withName(String name) {
		if (this.name.equals(name)) {
			return this;
		}
		// Store the original resource name if necessary...
		if (this.resourceName != null) {
			if (this.resourceName.equals(name)) {
				return new ResourcePropertySource(this.resourceName, null, this.source);
			}
			else {
				return new ResourcePropertySource(name, this.resourceName, this.source);
			}
		}
		else {
			// Current name is resource name -> preserve it in the extra field...
			return new ResourcePropertySource(name, this.name, this.source);
		}
	}

	/**
	 * 返回此 {@link ResourcePropertySource} 的一个可能经过适配的变体，
	 * 用原始资源名称覆盖先前给定的名称（如果有）
	 * （等价于无名称构造器变体所生成的名称）。
	 * @since 4.1
	 */
	public ResourcePropertySource withResourceName() {
		if (this.resourceName == null) {
			return this;
		}
		return new ResourcePropertySource(this.resourceName, null, this.source);
	}


	/**
	 * 返回给定 Resource 的描述；如果描述为空，
	 * 则返回该资源的类名加上其标识哈希码。
	 * @see org.springframework.core.io.Resource#getDescription()
	 */
	private static String getNameForResource(Resource resource) {
		String name = resource.getDescription();
		if (!StringUtils.hasText(name)) {
			name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
		}
		return name;
	}

}
