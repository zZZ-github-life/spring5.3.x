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

package org.springframework.test.context.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * 用于在 <em>Spring TestContext Framework</em> 中处理资源的工具方法。主要用于框架内部。
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 4.1
 * @see org.springframework.util.ResourceUtils
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ClassPathResource
 * @see org.springframework.core.io.FileSystemResource
 * @see org.springframework.core.io.UrlResource
 * @see org.springframework.core.io.ResourceLoader
 */
public abstract class TestContextResourceUtils {

	private static final String SLASH = "/";

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(".*\\$\\{[^}]+\\}.*");


	/**
	 * 将提供的路径转换为类路径资源路径。
	 * <p>委托给 {@link #convertToClasspathResourcePaths(Class, boolean, String...)}，并将 {@code preservePlaceholders} 标志提供为 {@code false}。
	 * @param clazz the class with which the paths are associated
	 * @param paths the paths to be converted
	 * @return a new array of converted resource paths
	 * @see #convertToResources
	 */
	public static String[] convertToClasspathResourcePaths(Class<?> clazz, String... paths) {
		return convertToClasspathResourcePaths(clazz, false, paths);
	}

	/**
	 * 将提供的路径转换为类路径资源路径。
	 * <p>对于提供的每个路径：
	 * <ul>
	 * <li>普通路径——例如 {@code "context.xml"}——将被视为相对于指定类所在包的类路径资源。此类路径将添加 {@code classpath:} 前缀以及该类的包路径。
	 * <li>以斜杠开头的路径将被视为类路径中的绝对路径，例如：{@code "/org/example/schema.sql"}。此类路径将添加 {@code classpath:} 前缀。
	 * <li>已带有 URL 协议前缀的路径（例如 {@code classpath:}、{@code file:}、{@code http:} 等）将不会修改其协议。
	 * </ul>
	 * <p>然后，除非 {@code preservePlaceholders} 标志为 {@code true} 且路径包含一个或多个 <code>${placeholder.name}</code> 形式的占位符，否则每个路径都会被{@linkplain StringUtils#cleanPath 清理}。
	 * @param clazz the class with which the paths are associated
	 * @param preservePlaceholders {@code true} if placeholders should be preserved
	 * @param paths the paths to be converted
	 * @return a new array of converted resource paths
	 * @since 5.2
	 * @see #convertToResources
	 * @see ResourceUtils#CLASSPATH_URL_PREFIX
	 * @see ResourceUtils#FILE_URL_PREFIX
	 */
	public static String[] convertToClasspathResourcePaths(Class<?> clazz, boolean preservePlaceholders, String... paths) {
		String[] convertedPaths = new String[paths.length];
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];

			// Absolute path
			if (path.startsWith(SLASH)) {
				convertedPaths[i] = ResourceUtils.CLASSPATH_URL_PREFIX + path;
			}
			// Relative path
			else if (!ResourcePatternUtils.isUrl(path)) {
				convertedPaths[i] = ResourceUtils.CLASSPATH_URL_PREFIX + SLASH +
						ClassUtils.classPackageAsResourcePath(clazz) + SLASH + path;
			}
			// URL
			else {
				convertedPaths[i] = path;
			}

			if (!(preservePlaceholders && PLACEHOLDER_PATTERN.matcher(convertedPaths[i]).matches())) {
				convertedPaths[i] = StringUtils.cleanPath(convertedPaths[i]);
			}
		}
		return convertedPaths;
	}

	/**
	 * 使用给定的 {@link ResourceLoader} 将提供的路径转换为 {@link Resource} 句柄数组。
	 * @param resourceLoader the {@code ResourceLoader} to use to convert the paths
	 * @param paths the paths to be converted
	 * @return a new array of resources
	 * @see #convertToResourceList(ResourceLoader, String...)
	 * @see #convertToClasspathResourcePaths
	 */
	public static Resource[] convertToResources(ResourceLoader resourceLoader, String... paths) {
		return stream(resourceLoader, paths).toArray(Resource[]::new);
	}

	/**
	 * 使用给定的 {@link ResourceLoader} 将提供的路径转换为 {@link Resource} 句柄列表。
	 * @param resourceLoader the {@code ResourceLoader} to use to convert the paths
	 * @param paths the paths to be converted
	 * @return a new list of resources
	 * @since 4.2
	 * @see #convertToResources(ResourceLoader, String...)
	 * @see #convertToClasspathResourcePaths
	 */
	public static List<Resource> convertToResourceList(ResourceLoader resourceLoader, String... paths) {
		return stream(resourceLoader, paths).collect(Collectors.toList());
	}

	private static Stream<Resource> stream(ResourceLoader resourceLoader, String... paths) {
		return Arrays.stream(paths).map(resourceLoader::getResource);
	}

}
