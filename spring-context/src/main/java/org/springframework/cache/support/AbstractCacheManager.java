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

package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * 实现通用 {@link CacheManager} 方法的抽象基类。
 * 适用于底层缓存不发生变化的 'static' 环境。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

	private volatile Set<String> cacheNames = Collections.emptySet();


	// Early cache initialization on startup

	@Override
	public void afterPropertiesSet() {
		initializeCaches();
	}

	/**
	 * 初始化缓存的静态配置。
	 * <p>通过 {@link #afterPropertiesSet()} 在启动时触发；
	 * 也可以在运行时调用以重新初始化。
	 * @since 4.2.2
	 * @see #loadCaches()
	 */
	public void initializeCaches() {
		Collection<? extends Cache> caches = loadCaches();

		synchronized (this.cacheMap) {
			this.cacheNames = Collections.emptySet();
			this.cacheMap.clear();
			Set<String> cacheNames = new LinkedHashSet<>(caches.size());
			for (Cache cache : caches) {
				String name = cache.getName();
				this.cacheMap.put(name, decorateCache(cache));
				cacheNames.add(name);
			}
			this.cacheNames = Collections.unmodifiableSet(cacheNames);
		}
	}

	/**
	 * 为该缓存管理器加载初始缓存。
	 * <p>在启动时由 {@link #afterPropertiesSet()} 调用。
	 * 返回的集合可以为空，但不能为 {@code null}。
	 */
	protected abstract Collection<? extends Cache> loadCaches();


	// Lazy cache initialization on access

	@Override
	@Nullable
	public Cache getCache(String name) {
		// Quick check for existing cache...
		Cache cache = this.cacheMap.get(name);
		if (cache != null) {
			return cache;
		}

		// The provider may support on-demand cache creation...
		Cache missingCache = getMissingCache(name);
		if (missingCache != null) {
			// Fully synchronize now for missing cache registration
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = decorateCache(missingCache);
					this.cacheMap.put(name, cache);
					updateCacheNames(name);
				}
			}
		}
		return cache;
	}

	@Override
	public Collection<String> getCacheNames() {
		return this.cacheNames;
	}


	// Common cache initialization delegates for subclasses

	/**
	 * 检查是否存在给定名称的已注册缓存。
	 * 与 {@link #getCache(String)} 不同，此方法不会通过
	 * {@link #getMissingCache(String)} 触发对缺失缓存的延迟创建。
	 * @param name 缓存标识符（不能为 {@code null}）
	 * @return 关联的 Cache 实例；如果未找到，则为 {@code null}
	 * @since 4.1
	 * @see #getCache(String)
	 * @see #getMissingCache(String)
	 */
	@Nullable
	protected final Cache lookupCache(String name) {
		return this.cacheMap.get(name);
	}

	/**
	 * 向此管理器动态注册一个额外的 Cache。
	 * @param cache 要注册的 Cache
	 * @deprecated as of Spring 4.3, in favor of {@link #getMissingCache(String)}
	 */
	@Deprecated
	protected final void addCache(Cache cache) {
		String name = cache.getName();
		synchronized (this.cacheMap) {
			if (this.cacheMap.put(name, decorateCache(cache)) == null) {
				updateCacheNames(name);
			}
		}
	}

	/**
	 * 用给定名称更新公开的 {@link #cacheNames} 集合。
	 * <p>这一操作始终会在完整的 {@link #cacheMap} 锁内被调用，
	 * 其行为实际上类似于一个 {@code CopyOnWriteArraySet}，
	 * 保留顺序但以不可修改的引用对外暴露。
	 * @param name 要添加的缓存名称
	 */
	private void updateCacheNames(String name) {
		Set<String> cacheNames = new LinkedHashSet<>(this.cacheNames);
		cacheNames.add(name);
		this.cacheNames = Collections.unmodifiableSet(cacheNames);
	}


	// Overridable template methods for cache initialization

	/**
	 * 如有必要，装饰给定的 Cache 对象。
	 * @param cache 要添加到此 CacheManager 的 Cache 对象
	 * @return 改为使用的装饰后 Cache 对象；
	 * 默认情况下就是传入的 Cache 对象本身
	 */
	protected Cache decorateCache(Cache cache) {
		return cache;
	}

	/**
	 * 返回具有指定 {@code name} 的缺失缓存，如果此类缓存不存在
	 * 或无法按需创建，则返回 {@code null}。
	 * <p>如果原生提供程序支持，缓存可以在运行时延迟创建。
	 * 如果按名称查找未得到任何结果，{@code AbstractCacheManager}
	 * 子类就有机会在运行时注册这样的缓存。返回的缓存
	 * 将被自动添加到该缓存管理器。
	 * @param name 要获取的缓存名称
	 * @return 缺失的缓存；如果此类缓存不存在或无法按需创建，则为 {@code null}
	 * @since 4.1
	 * @see #getCache(String)
	 */
	@Nullable
	protected Cache getMissingCache(String name) {
		return null;
	}

}
