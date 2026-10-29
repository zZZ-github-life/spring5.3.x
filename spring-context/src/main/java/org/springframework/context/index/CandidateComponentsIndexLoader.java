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

package org.springframework.context.index;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.SpringProperties;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * 供框架内部使用的候选组件索引加载机制。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
public final class CandidateComponentsIndexLoader {

	/**
	 * 查找组件的位置。<p>可以存在于多个 JAR 文件中。
	 */
	public static final String COMPONENTS_RESOURCE_LOCATION = "META-INF/spring.components";

	/**
	 * 系统属性，用于指示 Spring 忽略组件索引，即始终让 {@link #loadIndex(ClassLoader)} 返回
	 * {@code null}。<p>默认值为 "false"，允许正常使用索引。将此标志切换为 {@code true} 可满足一种边缘
	 * 情况：某些库（或用例）的索引只是部分可用，但无法为整个应用构建索引。在这种情况下，应用上下文会回
	 * 退到常规的类路径处理方式（即如同完全不存在索引）。
	 */
	public static final String IGNORE_INDEX = "spring.index.ignore";


	private static final boolean shouldIgnoreIndex = SpringProperties.getFlag(IGNORE_INDEX);

	private static final Log logger = LogFactory.getLog(CandidateComponentsIndexLoader.class);

	private static final ConcurrentMap<ClassLoader, CandidateComponentsIndex> cache =
			new ConcurrentReferenceHashMap<>();


	private CandidateComponentsIndexLoader() {
	}


	/**
	 * 使用给定的类加载器，从 {@value #COMPONENTS_RESOURCE_LOCATION} 加载并实例化
	 * {@link CandidateComponentsIndex}。如果没有可用的索引，则返回 {@code null}。
	 * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
	 * @return the index to use or {@code null} if no index was found
	 * @throws IllegalArgumentException if any module index cannot
	 * be loaded or if an error occurs while creating {@link CandidateComponentsIndex}
	 */
	@Nullable
	public static CandidateComponentsIndex loadIndex(@Nullable ClassLoader classLoader) {
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = CandidateComponentsIndexLoader.class.getClassLoader();
		}
		return cache.computeIfAbsent(classLoaderToUse, new Function<ClassLoader, CandidateComponentsIndex>() {
			@Override
			public CandidateComponentsIndex apply(ClassLoader classLoader1) {
				return doLoadIndex(classLoader1);
			}
		});
	}

	@Nullable
	private static CandidateComponentsIndex doLoadIndex(ClassLoader classLoader) {
		if (shouldIgnoreIndex) {
			return null;
		}

		try {
			Enumeration<URL> urls = classLoader.getResources(COMPONENTS_RESOURCE_LOCATION);
			if (!urls.hasMoreElements()) {
				return null;
			}
			List<Properties> result = new ArrayList<>();
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				Properties properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url));
				result.add(properties);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + result.size() + " index(es)");
			}
			int totalCount = result.stream().mapToInt(Properties::size).sum();
			return (totalCount > 0 ? new CandidateComponentsIndex(result) : null);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load indexes from location [" +
					COMPONENTS_RESOURCE_LOCATION + "]", ex);
		}
	}
}
