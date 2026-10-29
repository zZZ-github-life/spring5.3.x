/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * 一个基本的空操作 {@link CacheManager} 实现，适用于禁用缓存，通常用于为没有实际后备存储的缓存声明
 * 提供支持。
 *
 * <p>只会接受放入缓存的任何条目，但并不会真正存储它们。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 3.1
 * @see NoOpCache
 */
public class NoOpCacheManager implements CacheManager {

	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>(16);

	private final Set<String> cacheNames = new LinkedHashSet<>(16);


	/**
	 * 此实现始终返回一个不会存储条目的 {@link Cache} 实现。此外，管理器会记住所请求的缓存以保持一致性。
	 *
	 */
	@Override
	@Nullable
	public Cache getCache(String name) {
		Cache cache = this.caches.get(name);
		if (cache == null) {
			this.caches.computeIfAbsent(name, key -> new NoOpCache(name));
			synchronized (this.cacheNames) {
				this.cacheNames.add(name);
			}
		}

		return this.caches.get(name);
	}

	/**
	 * 此实现返回先前请求过的缓存的名称。
	 */
	@Override
	public Collection<String> getCacheNames() {
		synchronized (this.cacheNames) {
			return Collections.unmodifiableSet(this.cacheNames);
		}
	}

}
