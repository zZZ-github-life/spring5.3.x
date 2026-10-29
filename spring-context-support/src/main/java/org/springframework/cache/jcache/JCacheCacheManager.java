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

package org.springframework.cache.jcache;

import java.util.Collection;
import java.util.LinkedHashSet;

import javax.cache.CacheManager;
import javax.cache.Caching;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 由 JCache {@link CacheManager javax.cache.CacheManager} 提供支持的
 * {@link org.springframework.cache.CacheManager} 实现。
 *
 * <p>注意：此类已针对 JCache 1.0 更新，从 Spring 4.0 开始。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2
 * @see JCacheCache
 */
public class JCacheCacheManager extends AbstractTransactionSupportingCacheManager {

	@Nullable
	private CacheManager cacheManager;

	private boolean allowNullValues = true;


	/**
	 * 创建一个新的 {@code JCacheCacheManager}，不设置底层的 JCache
	 * {@link CacheManager javax.cache.CacheManager}。
	 * <p>底层的 JCache {@code javax.cache.CacheManager} 可以通过
	 * {@link #setCacheManager} bean 属性进行设置。
	 */
	public JCacheCacheManager() {
	}

	/**
	 * 为给定的底层 JCache {@link CacheManager javax.cache.CacheManager}
	 * 创建一个新的 {@code JCacheCacheManager}。
	 * @param cacheManager the backing JCache {@code javax.cache.CacheManager}
	 */
	public JCacheCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * 设置底层的 JCache {@link CacheManager javax.cache.CacheManager}。
	 */
	public void setCacheManager(@Nullable CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 返回底层的 JCache {@link CacheManager javax.cache.CacheManager}。
	 */
	@Nullable
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}

	/**
	 * 指定此缓存管理器中所有缓存是否接受并转换 {@code null} 值。
	 * <p>默认为 "true"，尽管 JSR-107 本身不支持 {@code null} 值。
	 * 将使用内部持有器对象来存储用户级别的 {@code null}。
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}

	/**
	 * 返回此缓存管理器是否为其所有缓存接受并转换 {@code null} 值。
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	@Override
	public void afterPropertiesSet() {
		if (getCacheManager() == null) {
			setCacheManager(Caching.getCachingProvider().getCacheManager());
		}
		super.afterPropertiesSet();
	}


	@Override
	protected Collection<Cache> loadCaches() {
		CacheManager cacheManager = getCacheManager();
		Assert.state(cacheManager != null, "No CacheManager set");

		Collection<Cache> caches = new LinkedHashSet<>();
		for (String cacheName : cacheManager.getCacheNames()) {
			javax.cache.Cache<Object, Object> jcache = cacheManager.getCache(cacheName);
			caches.add(new JCacheCache(jcache, isAllowNullValues()));
		}
		return caches;
	}

	@Override
	protected Cache getMissingCache(String name) {
		CacheManager cacheManager = getCacheManager();
		Assert.state(cacheManager != null, "No CacheManager set");

		// Check the JCache cache again (in case the cache was added at runtime)
		javax.cache.Cache<Object, Object> jcache = cacheManager.getCache(name);
		if (jcache != null) {
			return new JCacheCache(jcache, isAllowNullValues());
		}
		return null;
	}

}
