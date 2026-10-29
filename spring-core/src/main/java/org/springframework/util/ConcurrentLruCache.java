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

package org.springframework.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * 简单的 LRU（最近最少使用）缓存，受指定的缓存上限约束。
 *
 * <p>此实现使用 {@code ConcurrentHashMap} 存储缓存值，并使用 {@code ConcurrentLinkedDeque} 对键进
 * 行排序，在缓存达到容量上限时选择最近最少使用的键。
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 5.3
 * @param <K> the type of the key used for cache retrieval
 * @param <V> the type of the cached values
 * @see #get
 */
public class ConcurrentLruCache<K, V> {

	private final int sizeLimit;

	private final Function<K, V> generator;

	private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();

	private final ConcurrentLinkedDeque<K> queue = new ConcurrentLinkedDeque<>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private volatile int size;


	/**
	 * 使用给定的上限和生成函数创建一个新的缓存实例。
	 * @param sizeLimit the maximum number of entries in the cache
	 * (0 indicates no caching, always generating a new value)
	 * @param generator a function to generate a new value for a given key
	 */
	public ConcurrentLruCache(int sizeLimit, Function<K, V> generator) {
		Assert.isTrue(sizeLimit >= 0, "Cache size limit must not be negative");
		Assert.notNull(generator, "Generator function must not be null");
		this.sizeLimit = sizeLimit;
		this.generator = generator;
	}


	/**
	 * 从缓存中检索条目，可能会触发值的生成。
	 * @param key the key to retrieve the entry for
	 * @return the cached or newly generated value
	 */
	public V get(K key) {
		if (this.sizeLimit == 0) {
			return this.generator.apply(key);
		}

		V cached = this.cache.get(key);
		if (cached != null) {
			if (this.size < this.sizeLimit) {
				return cached;
			}
			this.lock.readLock().lock();
			try {
				if (this.queue.removeLastOccurrence(key)) {
					this.queue.offer(key);
				}
				return cached;
			}
			finally {
				this.lock.readLock().unlock();
			}
		}

		this.lock.writeLock().lock();
		try {
			// Retrying in case of concurrent reads on the same key
			cached = this.cache.get(key);
			if (cached != null) {
				if (this.queue.removeLastOccurrence(key)) {
					this.queue.offer(key);
				}
				return cached;
			}
			// Generate value first, to prevent size inconsistency
			V value = this.generator.apply(key);
			if (this.size == this.sizeLimit) {
				K leastUsed = this.queue.poll();
				if (leastUsed != null) {
					this.cache.remove(leastUsed);
				}
			}
			this.queue.offer(key);
			this.cache.put(key, value);
			this.size = this.cache.size();
			return value;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * 判断给定键是否存在于该缓存中。
	 * @param key the key to check for
	 * @return {@code true} if the key is present,
	 * {@code false} if there was no matching key
	 */
	public boolean contains(K key) {
		return this.cache.containsKey(key);
	}

	/**
	 * 立即移除给定键及其关联的任何值。
	 * @param key the key to evict the entry for
	 * @return {@code true} if the key was present before,
	 * {@code false} if there was no matching key
	 */
	public boolean remove(K key) {
		this.lock.writeLock().lock();
		try {
			boolean wasPresent = (this.cache.remove(key) != null);
			this.queue.remove(key);
			this.size = this.cache.size();
			return wasPresent;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * 立即移除该缓存中的所有条目。
	 */
	public void clear() {
		this.lock.writeLock().lock();
		try {
			this.cache.clear();
			this.queue.clear();
			this.size = 0;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * 返回缓存的当前大小。
	 * @see #sizeLimit()
	 */
	public int size() {
		return this.size;
	}

	/**
	 * 返回缓存中条目的最大数量（0 表示不缓存，始终生成新值）。
	 * @see #size()
	 */
	public int sizeLimit() {
		return this.sizeLimit;
	}

}
