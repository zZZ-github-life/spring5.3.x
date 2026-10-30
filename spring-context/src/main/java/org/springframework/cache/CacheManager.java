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

package org.springframework.cache;

import java.util.Collection;

import org.springframework.lang.Nullable;

/**
 * Spring 的核心缓存管理器 SPI。
 *
 * <p>允许获取具名的 {@link Cache} 区域。
 *
 * @author Costin Leau
 * @author Sam Brannen
 * @since 3.1
 */
public interface CacheManager {

	/**
	 * 获取与给定名称关联的缓存。
	 *
	 * <p>请注意，如果原生提供者支持，缓存可能会在运行时延迟创建。
	 * @param name 缓存标识符（不能为 {@code null}）
	 * @return 关联的缓存；如果此类缓存不存在或无法创建，则为 {@code null}
	 */
	@Nullable
	Cache getCache(String name);

	/**
	 * 获取此管理器已知的缓存名称集合。
	 * @return 缓存管理器已知的所有缓存的名称
	 */
	Collection<String> getCacheNames();

}
