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

package org.springframework.beans.factory.xml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 尝试使用一组映射文件将 schema URL 解析为本地 {@link ClassPathResource classpath resources} 的
 * {@link EntityResolver} 实现。
 *
 * <p>默认情况下，此类将使用模式 {@code META-INF/spring.schemas} 在类路径中查找映射文件，允许类路径
 * 上同时存在多个文件。
 *
 * <p>{@code META-INF/spring.schemas} 的格式是属性文件，其中每一行的形式应为
 * {@code systemId=schema-location}，其中 {@code schema-location} 也应是类路径中的 schema 文件。由
 * 于 {@code systemId} 通常是 URL，必须小心转义任何在属性文件中被视为分隔符的 “:” 字符。
 *
 * <p>映射文件的模式可以使用 {@link #PluggableSchemaResolver(ClassLoader, String)} 构造函数覆盖。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class PluggableSchemaResolver implements EntityResolver {

	/**
	 * 定义 schema 映射的文件位置。可以存在于多个 JAR 文件中。
	 */
	public static final String DEFAULT_SCHEMA_MAPPINGS_LOCATION = "META-INF/spring.schemas";


	private static final Log logger = LogFactory.getLog(PluggableSchemaResolver.class);

	@Nullable
	private final ClassLoader classLoader;

	private final String schemaMappingsLocation;

	/** 存储 schema URL &rarr; 本地 schema 路径的映射。 */
	@Nullable
	private volatile Map<String, String> schemaMappings;


	/**
	 * 使用默认映射文件模式 “META-INF/spring.schemas” 加载 schema URL &rarr; schema 文件位置的映射。
	 * @param classLoader 用于加载的 ClassLoader
	 * （可以为 {@code null} 以使用默认 ClassLoader）
	 * @see PropertiesLoaderUtils#loadAllProperties(String, ClassLoader)
	 */
	public PluggableSchemaResolver(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
		this.schemaMappingsLocation = DEFAULT_SCHEMA_MAPPINGS_LOCATION;
	}

	/**
	 * 使用给定的映射文件模式加载 schema URL &rarr; schema 文件位置的映射。
	 * @param classLoader 用于加载的 ClassLoader
	 * （可以为 {@code null} 以使用默认 ClassLoader）
	 * @param schemaMappingsLocation 定义 schema 映射的文件位置
	 * （不能为空）
	 * @see PropertiesLoaderUtils#loadAllProperties(String, ClassLoader)
	 */
	public PluggableSchemaResolver(@Nullable ClassLoader classLoader, String schemaMappingsLocation) {
		Assert.hasText(schemaMappingsLocation, "'schemaMappingsLocation' must not be empty");
		this.classLoader = classLoader;
		this.schemaMappingsLocation = schemaMappingsLocation;
	}


	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Trying to resolve XML entity with public id [" + publicId +
					"] and system id [" + systemId + "]");
		}

		if (systemId != null) {
			String resourceLocation = getSchemaMappings().get(systemId);
			if (resourceLocation == null && systemId.startsWith("https:")) {
				// Retrieve canonical http schema mapping even for https declaration
				resourceLocation = getSchemaMappings().get("http:" + systemId.substring(6));
			}
			if (resourceLocation != null) {
				Resource resource = new ClassPathResource(resourceLocation, this.classLoader);
				try {
					InputSource source = new InputSource(resource.getInputStream());
					source.setPublicId(publicId);
					source.setSystemId(systemId);
					if (logger.isTraceEnabled()) {
						logger.trace("Found XML schema [" + systemId + "] in classpath: " + resourceLocation);
					}
					return source;
				}
				catch (FileNotFoundException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not find XML schema [" + systemId + "]: " + resource, ex);
					}
				}
			}
		}

		// Fall back to the parser's default behavior.
		return null;
	}

	/**
	 * 延迟加载指定的 schema 映射。
	 */
	private Map<String, String> getSchemaMappings() {
		Map<String, String> schemaMappings = this.schemaMappings;
		if (schemaMappings == null) {
			synchronized (this) {
				schemaMappings = this.schemaMappings;
				if (schemaMappings == null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Loading schema mappings from [" + this.schemaMappingsLocation + "]");
					}
					try {
						Properties mappings =
								PropertiesLoaderUtils.loadAllProperties(this.schemaMappingsLocation, this.classLoader);
						if (logger.isTraceEnabled()) {
							logger.trace("Loaded schema mappings: " + mappings);
						}
						schemaMappings = new ConcurrentHashMap<>(mappings.size());
						CollectionUtils.mergePropertiesIntoMap(mappings, schemaMappings);
						this.schemaMappings = schemaMappings;
					}
					catch (IOException ex) {
						throw new IllegalStateException(
								"Unable to load schema mappings from location [" + this.schemaMappingsLocation + "]", ex);
					}
				}
			}
		}
		return schemaMappings;
	}


	@Override
	public String toString() {
		return "EntityResolver using schema mappings " + getSchemaMappings();
	}

}
