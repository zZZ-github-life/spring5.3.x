/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.cache;

import org.springframework.core.SpringProperties;
import org.springframework.util.StringUtils;

/**
 * 用于操作 {@link ContextCache ContextCaches} 的工具集合。
 *
 * @author Sam Brannen
 * @since 4.3
 */
public abstract class ContextCacheUtils {

	/**
	 * 获取 {@link ContextCache} 的最大大小。
	 * <p>使用 {@link SpringProperties} 获取名为 {@code spring.test.context.cache.maxSize} 的系统属性或 Spring 属性。
	 * <p>如果未设置此类属性或该属性不是整数，则回退到 {@link ContextCache#DEFAULT_MAX_CONTEXT_CACHE_SIZE} 的值。
	 * @return the maximum size of the context cache
	 * @see ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
	 */
	public static int retrieveMaxCacheSize() {
		try {
			String maxSize = SpringProperties.getProperty(ContextCache.MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME);
			if (StringUtils.hasText(maxSize)) {
				return Integer.parseInt(maxSize.trim());
			}
		}
		catch (Exception ex) {
			// ignore
		}

		// Fallback
		return ContextCache.DEFAULT_MAX_CONTEXT_CACHE_SIZE;
	}

}
