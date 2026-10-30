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

package org.springframework.test.context.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;

/**
 * {@link ContextCache} API 的默认实现。
 * <p>使用一个同步 {@link Map} 来缓存 {@link ApplicationContext} 实例，该映射配置了最大大小和<em>最近最少使用</em>（LRU）驱逐策略。
 * <p>最大大小可以通过 {@linkplain #DefaultContextCache(int) 构造函数参数}提供，也可以通过名为 {@code spring.test.context.cache.maxSize} 的系统属性或 Spring 属性设置。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ContextCacheUtils#retrieveMaxCacheSize()
 */
public class DefaultContextCache implements ContextCache {

	private static final Log statsLogger = LogFactory.getLog(CONTEXT_CACHE_LOGGING_CATEGORY);

	/**
	 * 从上下文键到 Spring {@code ApplicationContext} 实例的映射。
	 */
	private final Map<MergedContextConfiguration, ApplicationContext> contextMap =
			Collections.synchronizedMap(new LruCache(32, 0.75f));

	/**
	 * 从父键到子键集合的映射，表示上下文层次结构的自顶向下<em>树</em>。此信息用于确定在移除作为其他上下文父级的上下文时，需要递归移除并关闭哪些子树。
	 */
	private final Map<MergedContextConfiguration, Set<MergedContextConfiguration>> hierarchyMap =
			new ConcurrentHashMap<>(32);

	private final int maxSize;

	private final AtomicInteger hitCount = new AtomicInteger();

	private final AtomicInteger missCount = new AtomicInteger();


	/**
	 * 使用通过 {@link ContextCacheUtils#retrieveMaxCacheSize()} 获得的最大缓存大小创建一个新的 {@code DefaultContextCache}。
	 * @since 4.3
	 * @see #DefaultContextCache(int)
	 * @see ContextCacheUtils#retrieveMaxCacheSize()
	 */
	public DefaultContextCache() {
		this(ContextCacheUtils.retrieveMaxCacheSize());
	}

	/**
	 * 使用提供的最大缓存大小创建一个新的 {@code DefaultContextCache}。
	 * @param maxSize 最大缓存大小
	 * @throws IllegalArgumentException 如果提供的 {@code maxSize} 值不是正数
	 * @since 4.3
	 * @see #DefaultContextCache()
	 */
	public DefaultContextCache(int maxSize) {
		Assert.isTrue(maxSize > 0, "'maxSize' must be positive");
		this.maxSize = maxSize;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		return this.contextMap.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nullable
	public ApplicationContext get(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		ApplicationContext context = this.contextMap.get(key);
		if (context == null) {
			this.missCount.incrementAndGet();
		}
		else {
			this.hitCount.incrementAndGet();
		}
		return context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void put(MergedContextConfiguration key, ApplicationContext context) {
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(context, "ApplicationContext must not be null");

		this.contextMap.put(key, context);
		MergedContextConfiguration child = key;
		MergedContextConfiguration parent = child.getParent();
		while (parent != null) {
			Set<MergedContextConfiguration> list = this.hierarchyMap.computeIfAbsent(parent, k -> new HashSet<>());
			list.add(child);
			child = parent;
			parent = child.getParent();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(MergedContextConfiguration key, @Nullable HierarchyMode hierarchyMode) {
		Assert.notNull(key, "Key must not be null");

		// startKey is the level at which to begin clearing the cache,
		// depending on the configured hierarchy mode.s
		MergedContextConfiguration startKey = key;
		if (hierarchyMode == HierarchyMode.EXHAUSTIVE) {
			MergedContextConfiguration parent = startKey.getParent();
			while (parent != null) {
				startKey = parent;
				parent = startKey.getParent();
			}
		}

		List<MergedContextConfiguration> removedContexts = new ArrayList<>();
		remove(removedContexts, startKey);

		// Remove all remaining references to any removed contexts from the
		// hierarchy map.
		for (MergedContextConfiguration currentKey : removedContexts) {
			for (Set<MergedContextConfiguration> children : this.hierarchyMap.values()) {
				children.remove(currentKey);
			}
		}

		// Remove empty entries from the hierarchy map.
		for (Map.Entry<MergedContextConfiguration, Set<MergedContextConfiguration>> entry : this.hierarchyMap.entrySet()) {
			if (entry.getValue().isEmpty()) {
				this.hierarchyMap.remove(entry.getKey());
			}
		}
	}

	private void remove(List<MergedContextConfiguration> removedContexts, MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");

		Set<MergedContextConfiguration> children = this.hierarchyMap.get(key);
		if (children != null) {
			for (MergedContextConfiguration child : children) {
				// Recurse through lower levels
				remove(removedContexts, child);
			}
			// Remove the set of children for the current context from the hierarchy map.
			this.hierarchyMap.remove(key);
		}

		// Physically remove and close leaf nodes first (i.e., on the way back up the
		// stack as opposed to prior to the recursive call).
		ApplicationContext context = this.contextMap.remove(key);
		if (context instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) context).close();
		}
		removedContexts.add(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return this.contextMap.size();
	}

	/**
	 * 获取此缓存的最大大小。
	 */
	public int getMaxSize() {
		return this.maxSize;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getParentContextCount() {
		return this.hierarchyMap.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getHitCount() {
		return this.hitCount.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMissCount() {
		return this.missCount.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		synchronized (this.contextMap) {
			clear();
			clearStatistics();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		synchronized (this.contextMap) {
			this.contextMap.clear();
			this.hierarchyMap.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearStatistics() {
		synchronized (this.contextMap) {
			this.hitCount.set(0);
			this.missCount.set(0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void logStatistics() {
		if (statsLogger.isDebugEnabled()) {
			statsLogger.debug("Spring test ApplicationContext cache statistics: " + this);
		}
	}

	/**
	 * 生成包含此缓存实现类型及其统计信息的文本字符串。
	 * <p>此方法返回的字符串包含满足 {@link #logStatistics()} 契约所需的所有信息。
	 * @return 此缓存的字符串表示形式，包括统计信息
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("size", size())
				.append("maxSize", getMaxSize())
				.append("parentContextCount", getParentContextCount())
				.append("hitCount", getHitCount())
				.append("missCount", getMissCount())
				.toString();
	}


	/**
	 * 基于 {@link LinkedHashMap} 的简单缓存实现，具有最大大小和<em>最近最少使用</em>（LRU）驱逐策略，并能正确关闭应用程序上下文。
	 * @since 4.3
	 */
	@SuppressWarnings("serial")
	private class LruCache extends LinkedHashMap<MergedContextConfiguration, ApplicationContext> {

		/**
		 * 使用提供的初始容量和负载因子创建一个新的 {@code LruCache}。
		 * @param initialCapacity 初始容量
		 * @param loadFactor 加载因子
		 */
		LruCache(int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<MergedContextConfiguration, ApplicationContext> eldest) {
			if (this.size() > DefaultContextCache.this.getMaxSize()) {
				// Do NOT delete "DefaultContextCache.this."; otherwise, we accidentally
				// invoke java.util.Map.remove(Object, Object).
				DefaultContextCache.this.remove(eldest.getKey(), HierarchyMode.CURRENT_LEVEL);
			}

			// Return false since we invoke a custom eviction algorithm.
			return false;
		}
	}

}
