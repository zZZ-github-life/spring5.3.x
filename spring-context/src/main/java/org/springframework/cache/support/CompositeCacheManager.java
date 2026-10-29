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

package org.springframework.cache.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * 组合式 {@link CacheManager} 实现，会遍历给定的委托 {@link CacheManager} 实例集合。
 *
 * <p>允许将 {@link NoOpCacheManager} 自动添加到列表末尾，用于处理没有后备存储的缓存声明。否则，任
 * 何自定义 {@link CacheManager} 也可以扮演最后一个委托的角色，为任何请求的名称延迟创建缓存区域。
 *
 * <p>注意：此组合管理器所委托的常规 CacheManager 在不知道指定缓存名称时，需要从
 * {@link #getCache(String)} 返回 {@code null}，以便迭代到下一个委托。然而，大多数
 * {@link CacheManager} 实现在被请求后会退回到按需延迟创建命名缓存；如果可用，请查阅有关使用固定缓
 * 存名称的“静态”模式的具体配置细节。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setFallbackToNoOpCache
 * @see org.springframework.cache.concurrent.ConcurrentMapCacheManager#setCacheNames
 */
public class CompositeCacheManager implements CacheManager, InitializingBean {

	private final List<CacheManager> cacheManagers = new ArrayList<>();

	private boolean fallbackToNoOpCache = false;


	/**
	 * 构造一个空的 CompositeCacheManager，委托的 CacheManager 通过
	 * {@link #setCacheManagers "cacheManagers"} 属性添加。
	 */
	public CompositeCacheManager() {
	}

	/**
	 * 根据给定的委托 CacheManager 构造一个 CompositeCacheManager。
	 * @param cacheManagers the CacheManagers to delegate to
	 */
	public CompositeCacheManager(CacheManager... cacheManagers) {
		setCacheManagers(Arrays.asList(cacheManagers));
	}


	/**
	 * 指定要委托的 CacheManager。
	 */
	public void setCacheManagers(Collection<CacheManager> cacheManagers) {
		this.cacheManagers.addAll(cacheManagers);
	}

	/**
	 * 指示是否应在委托列表末尾添加一个 {@link NoOpCacheManager}。在这种情况下，配置的 CacheManager 未
	 * 处理的任何 {@code getCache} 请求都将由 {@link NoOpCacheManager} 自动处理（因此永远不会返回
	 * {@code null}）。
	 */
	public void setFallbackToNoOpCache(boolean fallbackToNoOpCache) {
		this.fallbackToNoOpCache = fallbackToNoOpCache;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.fallbackToNoOpCache) {
			this.cacheManagers.add(new NoOpCacheManager());
		}
	}


	@Override
	@Nullable
	public Cache getCache(String name) {
		for (CacheManager cacheManager : this.cacheManagers) {
			Cache cache = cacheManager.getCache(name);
			if (cache != null) {
				return cache;
			}
		}
		return null;
	}

	@Override
	public Collection<String> getCacheNames() {
		Set<String> names = new LinkedHashSet<>();
		for (CacheManager manager : this.cacheManagers) {
			names.addAll(manager.getCacheNames());
		}
		return Collections.unmodifiableSet(names);
	}

}
