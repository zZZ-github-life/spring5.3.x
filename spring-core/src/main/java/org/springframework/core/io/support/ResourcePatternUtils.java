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

package org.springframework.core.io.support;

import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * 用于判断给定 URL 是否为可通过 {@link ResourcePatternResolver}
 * 加载的资源位置的工具类。
 *
 * <p>如果 {@link #isUrl(String)} 方法返回 {@code false}，
 * 调用方通常会将位置视为相对路径。
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 */
public abstract class ResourcePatternUtils {

	/**
	 * 返回给定资源位置是否为 URL：可以是
	 * 特殊的 "classpath" 或 "classpath*" 伪 URL，也可以是标准 URL。
	 * @param resourceLocation the location String to check
	 * @return whether the location qualifies as a URL
	 * @see ResourcePatternResolver#CLASSPATH_ALL_URL_PREFIX
	 * @see org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX
	 * @see org.springframework.util.ResourceUtils#isUrl(String)
	 * @see java.net.URL
	 */
	public static boolean isUrl(@Nullable String resourceLocation) {
		return (resourceLocation != null &&
				(resourceLocation.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX) ||
						ResourceUtils.isUrl(resourceLocation)));
	}

	/**
	 * 为给定的 {@link ResourceLoader} 返回默认的 {@link ResourcePatternResolver}。
	 * <p>如果该 {@code ResourceLoader} 实现了 {@code ResourcePatternResolver} 扩展，
	 * 则可能就是它本身；否则是在给定 {@code ResourceLoader} 之上构建的
	 * 默认 {@link PathMatchingResourcePatternResolver}。
	 * @param resourceLoader the ResourceLoader to build a pattern resolver for
	 * (may be {@code null} to indicate a default ResourceLoader)
	 * @return the ResourcePatternResolver
	 * @see PathMatchingResourcePatternResolver
	 */
	public static ResourcePatternResolver getResourcePatternResolver(@Nullable ResourceLoader resourceLoader) {
		if (resourceLoader instanceof ResourcePatternResolver) {
			return (ResourcePatternResolver) resourceLoader;
		}
		else if (resourceLoader != null) {
			return new PathMatchingResourcePatternResolver(resourceLoader);
		}
		else {
			return new PathMatchingResourcePatternResolver();
		}
	}

}
