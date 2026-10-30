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

package org.springframework.cache.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.lang.Nullable;

/**
 * {@link CacheManager} 实现，它为每个 {@link #getCache} 请求延迟构建 {@link ConcurrentMapCache} 实
 * 例。还支持“静态”模式，此时通过 {@link #setCacheNames} 预先定义缓存名称集合，运行时不再动态创建更
 * 多缓存区域。
 *
 * <p>注意：这绝不是一个复杂的 CacheManager；它不附带任何缓存配置选项。不过，它对于测试或简单的缓存
 * 场景可能很有用。对于高级的本地缓存需求，可考虑
 * {@link org.springframework.cache.jcache.JCacheCacheManager}、
 * {@link org.springframework.cache.ehcache.EhCacheCacheManager}、
 * {@link org.springframework.cache.caffeine.CaffeineCacheManager}。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see ConcurrentMapCache
 */
public class ConcurrentMapCacheManager implements CacheManager, BeanClassLoaderAware {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

	private boolean dynamic = true;

	private boolean allowNullValues = true;

	private boolean storeByValue = false;

	@Nullable
	private SerializationDelegate serialization;


	/**
	 * 构造一个动态的 ConcurrentMapCacheManager，在请求缓存实例时延迟创建它们。
	 */
	public ConcurrentMapCacheManager() {
	}

	/**
	 * 构造一个静态的 ConcurrentMapCacheManager，仅管理指定缓存名称对应的缓存。
	 */
	public ConcurrentMapCacheManager(String... cacheNames) {
		setCacheNames(Arrays.asList(cacheNames));
	}


	/**
	 * 为此 CacheManager 的“静态”模式指定缓存名称集合。
	 * <p>调用此方法后，缓存的数量及其名称将固定下来，运行时不再创建更多缓存区域。
	 * <p>以 {@code null} 集合作为参数调用此方法会将模式重置为“动态”，从而允许再次创建缓存。
	 */
	public void setCacheNames(@Nullable Collection<String> cacheNames) {
		if (cacheNames != null) {
			for (String name : cacheNames) {
				this.cacheMap.put(name, createConcurrentMapCache(name));
			}
			this.dynamic = false;
		}
		else {
			this.dynamic = true;
		}
	}

	/**
	 * 指定此缓存管理器中的所有缓存是否接受并转换 {@code null} 值。
	 * <p>默认值为 "true"，尽管 ConcurrentHashMap 本身不支持 {@code null} 值。将使用内部持有对象来存储
	 * 用户级别的 {@code null}。
	 * <p>注意：更改 null 值设置将重置所有现有缓存（如果有），以按照新的 null 值要求重新配置它们。
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		if (allowNullValues != this.allowNullValues) {
			this.allowNullValues = allowNullValues;
			// Need to recreate all Cache instances with the new null-value configuration...
			recreateCaches();
		}
	}

	/**
	 * 返回此缓存管理器是否为其所有缓存接受并转换 {@code null} 值。
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	/**
	 * 指定此缓存管理器为其所有缓存存储每个条目的副本（{@code true}）还是引用（{@code false}）。
	 * <p>默认值为 "false"，因此存储的是值本身，缓存的值不需要满足可序列化契约。
	 * <p>注意：更改按值存储设置将重置所有现有缓存（如果有），以按照新的按值存储要求重新配置它们。
	 * @since 4.3
	 */
	public void setStoreByValue(boolean storeByValue) {
		if (storeByValue != this.storeByValue) {
			this.storeByValue = storeByValue;
			// Need to recreate all Cache instances with the new store-by-value configuration...
			recreateCaches();
		}
	}

	/**
	 * 返回此缓存管理器为其所有缓存存储每个条目的副本还是引用。如果启用了按值存储，则任何缓存条目都必须
	 * 是可序列化的。
	 * @since 4.3
	 */
	public boolean isStoreByValue() {
		return this.storeByValue;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.serialization = new SerializationDelegate(classLoader);
		// Need to recreate all Cache instances with new ClassLoader in store-by-value mode...
		if (isStoreByValue()) {
			recreateCaches();
		}
	}


	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheMap.keySet());
	}

	@Override
	@Nullable
	public Cache getCache(String name) {
		Cache cache = this.cacheMap.get(name);
		if (cache == null && this.dynamic) {
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = createConcurrentMapCache(name);
					this.cacheMap.put(name, cache);
				}
			}
		}
		return cache;
	}

	private void recreateCaches() {
		for (Map.Entry<String, Cache> entry : this.cacheMap.entrySet()) {
			entry.setValue(createConcurrentMapCache(entry.getKey()));
		}
	}

	/**
	 * 为指定缓存名称创建一个新的 ConcurrentMapCache 实例。
	 * @param name 缓存的名称
	 * @return ConcurrentMapCache（或其装饰器）
	 */
	protected Cache createConcurrentMapCache(String name) {
		SerializationDelegate actualSerialization = (isStoreByValue() ? this.serialization : null);
		return new ConcurrentMapCache(name, new ConcurrentHashMap<>(256), isAllowNullValues(), actualSerialization);
	}

}
