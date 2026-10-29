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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.lang.Nullable;

/**
 * 一个 {@link ConcurrentHashMap}，对 {@code keys} 和 {@code values} 都使用
 * {@link ReferenceType#SOFT 软}引用或 {@linkplain ReferenceType#WEAK 弱}引用。
 *
 * <p>此类可作为 {@code Collections.synchronizedMap(new WeakHashMap<K, Reference<V>>())} 的替代方案，
 * 以便在并发访问时提供更好的性能。此实现遵循与 {@link ConcurrentHashMap} 相同的设计约束，区别在
 * 于它支持 {@code null} 值和 {@code null} 键。
 *
 * <p><b>注意：</b>使用引用意味着无法保证放入映射中的项随后仍然可用。垃圾回收器可能随时丢弃引用，因
 * 此看起来就像是某个未知线程在悄然移除条目。
 *
 * <p>如果未显式指定，此实现将使用 {@linkplain SoftReference 软条目引用}。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 3.2
 * @param <K> the key type
 * @param <V> the value type
 */
public class ConcurrentReferenceHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

	private static final int DEFAULT_INITIAL_CAPACITY = 16;

	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

	private static final ReferenceType DEFAULT_REFERENCE_TYPE = ReferenceType.SOFT;

	private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;

	private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;


	/**
	 * 使用哈希的高位作为索引的段数组。
	 */
	private final Segment[] segments;

	/**
	 * 当每个表的平均引用数超过此值时，将尝试调整大小。
	 */
	private final float loadFactor;

	/**
	 * 引用类型：SOFT 或 WEAK。
	 */
	private final ReferenceType referenceType;

	/**
	 * 用于计算段数组大小以及根据哈希计算索引的移位值。
	 */
	private final int shift;

	/**
	 * 延迟绑定的条目集。
	 */
	@Nullable
	private volatile Set<Map.Entry<K, V>> entrySet;


	/**
	 * 创建一个新的 {@code ConcurrentReferenceHashMap} 实例。
	 */
	public ConcurrentReferenceHashMap() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * 创建一个新的 {@code ConcurrentReferenceHashMap} 实例。
	 * @param initialCapacity the initial capacity of the map
	 */
	public ConcurrentReferenceHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * 创建一个新的 {@code ConcurrentReferenceHashMap} 实例。
	 * @param initialCapacity the initial capacity of the map
	 * @param loadFactor the load factor. When the average number of references per table
	 * exceeds this value resize will be attempted
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * 创建一个新的 {@code ConcurrentReferenceHashMap} 实例。
	 * @param initialCapacity the initial capacity of the map
	 * @param concurrencyLevel the expected number of threads that will concurrently
	 * write to the map
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, int concurrencyLevel) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * 创建一个新的 {@code ConcurrentReferenceHashMap} 实例。
	 * @param initialCapacity the initial capacity of the map
	 * @param referenceType the reference type used for entries (soft or weak)
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, ReferenceType referenceType) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, referenceType);
	}

	/**
	 * 创建一个新的 {@code ConcurrentReferenceHashMap} 实例。
	 * @param initialCapacity the initial capacity of the map
	 * @param loadFactor the load factor. When the average number of references per
	 * table exceeds this value, resize will be attempted.
	 * @param concurrencyLevel the expected number of threads that will concurrently
	 * write to the map
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
		this(initialCapacity, loadFactor, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * 创建一个新的 {@code ConcurrentReferenceHashMap} 实例。
	 * @param initialCapacity the initial capacity of the map
	 * @param loadFactor the load factor. When the average number of references per
	 * table exceeds this value, resize will be attempted.
	 * @param concurrencyLevel the expected number of threads that will concurrently
	 * write to the map
	 * @param referenceType the reference type used for entries (soft or weak)
	 */
	@SuppressWarnings("unchecked")
	public ConcurrentReferenceHashMap(
			int initialCapacity, float loadFactor, int concurrencyLevel, ReferenceType referenceType) {

		Assert.isTrue(initialCapacity >= 0, "Initial capacity must not be negative");
		Assert.isTrue(loadFactor > 0f, "Load factor must be positive");
		Assert.isTrue(concurrencyLevel > 0, "Concurrency level must be positive");
		Assert.notNull(referenceType, "Reference type must not be null");
		this.loadFactor = loadFactor;
		this.shift = calculateShift(concurrencyLevel, MAXIMUM_CONCURRENCY_LEVEL);
		int size = 1 << this.shift;
		this.referenceType = referenceType;
		int roundedUpSegmentCapacity = (int) ((initialCapacity + size - 1L) / size);
		int initialSize = 1 << calculateShift(roundedUpSegmentCapacity, MAXIMUM_SEGMENT_SIZE);
		Segment[] segments = (Segment[]) Array.newInstance(Segment.class, size);
		int resizeThreshold = (int) (initialSize * getLoadFactor());
		for (int i = 0; i < segments.length; i++) {
			segments[i] = new Segment(initialSize, resizeThreshold);
		}
		this.segments = segments;
	}


	protected final float getLoadFactor() {
		return this.loadFactor;
	}

	protected final int getSegmentsSize() {
		return this.segments.length;
	}

	protected final Segment getSegment(int index) {
		return this.segments[index];
	}

	/**
	 * 返回 {@link ReferenceManager} 的工厂方法。此方法会为每个 {@link Segment} 调用一次。
	 * @return a new reference manager
	 */
	protected ReferenceManager createReferenceManager() {
		return new ReferenceManager();
	}

	/**
	 * 获取给定对象的哈希，并应用额外的哈希函数以减少冲突。此实现使用与 {@link ConcurrentHashMap} 相同
	 * 的 Wang/Jenkins 算法。子类可以重写以提供其他哈希方式。
	 * @param o the object to hash (may be null)
	 * @return the resulting hash code
	 */
	protected int getHash(@Nullable Object o) {
		int hash = (o != null ? o.hashCode() : 0);
		hash += (hash << 15) ^ 0xffffcd7d;
		hash ^= (hash >>> 10);
		hash += (hash << 3);
		hash ^= (hash >>> 6);
		hash += (hash << 2) + (hash << 14);
		hash ^= (hash >>> 16);
		return hash;
	}

	@Override
	@Nullable
	public V get(@Nullable Object key) {
		Reference<K, V> ref = getReference(key, Restructure.WHEN_NECESSARY);
		Entry<K, V> entry = (ref != null ? ref.get() : null);
		return (entry != null ? entry.getValue() : null);
	}

	@Override
	@Nullable
	public V getOrDefault(@Nullable Object key, @Nullable V defaultValue) {
		Reference<K, V> ref = getReference(key, Restructure.WHEN_NECESSARY);
		Entry<K, V> entry = (ref != null ? ref.get() : null);
		return (entry != null ? entry.getValue() : defaultValue);
	}

	@Override
	public boolean containsKey(@Nullable Object key) {
		Reference<K, V> ref = getReference(key, Restructure.WHEN_NECESSARY);
		Entry<K, V> entry = (ref != null ? ref.get() : null);
		return (entry != null && ObjectUtils.nullSafeEquals(entry.getKey(), key));
	}

	/**
	 * 返回指向指定 {@code key} 对应 {@link Entry} 的 {@link Reference}，如果未找到则返回 {@code null}。
	 *
	 * @param key the key (can be {@code null})
	 * @param restructure types of restructure allowed during this call
	 * @return the reference, or {@code null} if not found
	 */
	@Nullable
	protected final Reference<K, V> getReference(@Nullable Object key, Restructure restructure) {
		int hash = getHash(key);
		return getSegmentForHash(hash).getReference(key, hash, restructure);
	}

	@Override
	@Nullable
	public V put(@Nullable K key, @Nullable V value) {
		return put(key, value, true);
	}

	@Override
	@Nullable
	public V putIfAbsent(@Nullable K key, @Nullable V value) {
		return put(key, value, false);
	}

	@Nullable
	private V put(@Nullable final K key, @Nullable final V value, final boolean overwriteExisting) {
		return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.RESIZE) {
			@Override
			@Nullable
			protected V execute(@Nullable Reference<K, V> ref, @Nullable Entry<K, V> entry, @Nullable Entries<V> entries) {
				if (entry != null) {
					V oldValue = entry.getValue();
					if (overwriteExisting) {
						entry.setValue(value);
					}
					return oldValue;
				}
				Assert.state(entries != null, "No entries segment");
				entries.add(value);
				return null;
			}
		});
	}

	@Override
	@Nullable
	public V remove(@Nullable Object key) {
		return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_AFTER, TaskOption.SKIP_IF_EMPTY) {
			@Override
			@Nullable
			protected V execute(@Nullable Reference<K, V> ref, @Nullable Entry<K, V> entry) {
				if (entry != null) {
					if (ref != null) {
						ref.release();
					}
					return entry.value;
				}
				return null;
			}
		});
	}

	@Override
	public boolean remove(@Nullable Object key, final @Nullable Object value) {
		Boolean result = doTask(key, new Task<Boolean>(TaskOption.RESTRUCTURE_AFTER, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected Boolean execute(@Nullable Reference<K, V> ref, @Nullable Entry<K, V> entry) {
				if (entry != null && ObjectUtils.nullSafeEquals(entry.getValue(), value)) {
					if (ref != null) {
						ref.release();
					}
					return true;
				}
				return false;
			}
		});
		return (Boolean.TRUE.equals(result));
	}

	@Override
	public boolean replace(@Nullable K key, final @Nullable V oldValue, final @Nullable V newValue) {
		Boolean result = doTask(key, new Task<Boolean>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected Boolean execute(@Nullable Reference<K, V> ref, @Nullable Entry<K, V> entry) {
				if (entry != null && ObjectUtils.nullSafeEquals(entry.getValue(), oldValue)) {
					entry.setValue(newValue);
					return true;
				}
				return false;
			}
		});
		return (Boolean.TRUE.equals(result));
	}

	@Override
	@Nullable
	public V replace(@Nullable K key, final @Nullable V value) {
		return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.SKIP_IF_EMPTY) {
			@Override
			@Nullable
			protected V execute(@Nullable Reference<K, V> ref, @Nullable Entry<K, V> entry) {
				if (entry != null) {
					V oldValue = entry.getValue();
					entry.setValue(value);
					return oldValue;
				}
				return null;
			}
		});
	}

	@Override
	public void clear() {
		for (Segment segment : this.segments) {
			segment.clear();
		}
	}

	/**
	 * 移除所有已被垃圾回收且不再被引用的条目。正常情况下，当向 Map 添加或移除项时，已被垃圾回收的条目
	 * 会自动清除。此方法可用于强制执行清除，在读操作频繁但更新较少的 Map 中很有用。
	 */
	public void purgeUnreferencedEntries() {
		for (Segment segment : this.segments) {
			segment.restructureIfNecessary(false);
		}
	}


	@Override
	public int size() {
		int size = 0;
		for (Segment segment : this.segments) {
			size += segment.getCount();
		}
		return size;
	}

	@Override
	public boolean isEmpty() {
		for (Segment segment : this.segments) {
			if (segment.getCount() > 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> entrySet = this.entrySet;
		if (entrySet == null) {
			entrySet = new EntrySet();
			this.entrySet = entrySet;
		}
		return entrySet;
	}

	@Nullable
	private <T> T doTask(@Nullable Object key, Task<T> task) {
		int hash = getHash(key);
		return getSegmentForHash(hash).doTask(hash, key, task);
	}

	private Segment getSegmentForHash(int hash) {
		return this.segments[(hash >>> (32 - this.shift)) & (this.segments.length - 1)];
	}

	/**
	 * 计算一个移位值，用于在指定的最大值和最小值之间创建 2 的幂值。
	 * @param minimumValue the minimum value
	 * @param maximumValue the maximum value
	 * @return the calculated shift (use {@code 1 << shift} to obtain a value)
	 */
	protected static int calculateShift(int minimumValue, int maximumValue) {
		int shift = 0;
		int value = 1;
		while (value < minimumValue && value < maximumValue) {
			value <<= 1;
			shift++;
		}
		return shift;
	}


	/**
	 * 此映射支持的各种引用类型。
	 */
	public enum ReferenceType {

		/** 使用 {@link SoftReference SoftReferences}。 */
		SOFT,

		/** 使用 {@link WeakReference WeakReferences}。 */
		WEAK
	}


	/**
	 * 用于划分映射以提供更好并发性能的单个段。
	 */
	@SuppressWarnings("serial")
	protected final class Segment extends ReentrantLock {

		private final ReferenceManager referenceManager;

		private final int initialSize;

		/**
		 * 使用哈希的低位作为索引的引用数组。此属性只应与 {@code resizeThreshold} 一起设置。
		 */
		private volatile Reference<K, V>[] references;

		/**
		 * 此段中包含的引用总数。包括链式引用以及已被垃圾回收但尚未清除的引用。
		 */
		private final AtomicInteger count = new AtomicInteger();

		/**
		 * 应发生引用调整大小的阈值。当 {@code count} 超过此值时，引用将被调整大小。
		 */
		private int resizeThreshold;

		public Segment(int initialSize, int resizeThreshold) {
			this.referenceManager = createReferenceManager();
			this.initialSize = initialSize;
			this.references = createReferenceArray(initialSize);
			this.resizeThreshold = resizeThreshold;
		}

		@Nullable
		public Reference<K, V> getReference(@Nullable Object key, int hash, Restructure restructure) {
			if (restructure == Restructure.WHEN_NECESSARY) {
				restructureIfNecessary(false);
			}
			if (this.count.get() == 0) {
				return null;
			}
			// Use a local copy to protect against other threads writing
			Reference<K, V>[] references = this.references;
			int index = getIndex(hash, references);
			Reference<K, V> head = references[index];
			return findInChain(head, key, hash);
		}

		/**
		 * 对此段应用更新操作。更新期间该段将被锁定。
		 * @param hash the hash of the key
		 * @param key the key
		 * @param task the update operation
		 * @return the result of the operation
		 */
		@Nullable
		public <T> T doTask(final int hash, @Nullable final Object key, final Task<T> task) {
			boolean resize = task.hasOption(TaskOption.RESIZE);
			if (task.hasOption(TaskOption.RESTRUCTURE_BEFORE)) {
				restructureIfNecessary(resize);
			}
			if (task.hasOption(TaskOption.SKIP_IF_EMPTY) && this.count.get() == 0) {
				return task.execute(null, null, null);
			}
			lock();
			try {
				final int index = getIndex(hash, this.references);
				final Reference<K, V> head = this.references[index];
				Reference<K, V> ref = findInChain(head, key, hash);
				Entry<K, V> entry = (ref != null ? ref.get() : null);
				Entries<V> entries = value -> {
					@SuppressWarnings("unchecked")
					Entry<K, V> newEntry = new Entry<>((K) key, value);
					Reference<K, V> newReference = Segment.this.referenceManager.createReference(newEntry, hash, head);
					Segment.this.references[index] = newReference;
					Segment.this.count.incrementAndGet();
				};
				return task.execute(ref, entry, entries);
			}
			finally {
				unlock();
				if (task.hasOption(TaskOption.RESTRUCTURE_AFTER)) {
					restructureIfNecessary(resize);
				}
			}
		}

		/**
		 * 清除此段中的所有项。
		 */
		public void clear() {
			if (this.count.get() == 0) {
				return;
			}
			lock();
			try {
				this.references = createReferenceArray(this.initialSize);
				this.resizeThreshold = (int) (this.references.length * getLoadFactor());
				this.count.set(0);
			}
			finally {
				unlock();
			}
		}

		/**
		 * 在必要时重构底层数据结构。此方法可以增大引用表的容量，并清除所有已被垃圾回收的引用。
		 * @param allowResize if resizing is permitted
		 */
		protected final void restructureIfNecessary(boolean allowResize) {
			int currCount = this.count.get();
			boolean needsResize = allowResize && (currCount > 0 && currCount >= this.resizeThreshold);
			Reference<K, V> ref = this.referenceManager.pollForPurge();
			if (ref != null || (needsResize)) {
				restructure(allowResize, ref);
			}
		}

		private void restructure(boolean allowResize, @Nullable Reference<K, V> ref) {
			boolean needsResize;
			lock();
			try {
				int countAfterRestructure = this.count.get();
				Set<Reference<K, V>> toPurge = Collections.emptySet();
				if (ref != null) {
					toPurge = new HashSet<>();
					while (ref != null) {
						toPurge.add(ref);
						ref = this.referenceManager.pollForPurge();
					}
				}
				countAfterRestructure -= toPurge.size();

				// Recalculate taking into account count inside lock and items that
				// will be purged
				needsResize = (countAfterRestructure > 0 && countAfterRestructure >= this.resizeThreshold);
				boolean resizing = false;
				int restructureSize = this.references.length;
				if (allowResize && needsResize && restructureSize < MAXIMUM_SEGMENT_SIZE) {
					restructureSize <<= 1;
					resizing = true;
				}

				// Either create a new table or reuse the existing one
				Reference<K, V>[] restructured =
						(resizing ? createReferenceArray(restructureSize) : this.references);

				// Restructure
				for (int i = 0; i < this.references.length; i++) {
					ref = this.references[i];
					if (!resizing) {
						restructured[i] = null;
					}
					while (ref != null) {
						if (!toPurge.contains(ref)) {
							Entry<K, V> entry = ref.get();
							if (entry != null) {
								int index = getIndex(ref.getHash(), restructured);
								restructured[index] = this.referenceManager.createReference(
										entry, ref.getHash(), restructured[index]);
							}
						}
						ref = ref.getNext();
					}
				}

				// Replace volatile members
				if (resizing) {
					this.references = restructured;
					this.resizeThreshold = (int) (this.references.length * getLoadFactor());
				}
				this.count.set(Math.max(countAfterRestructure, 0));
			}
			finally {
				unlock();
			}
		}

		@Nullable
		private Reference<K, V> findInChain(Reference<K, V> ref, @Nullable Object key, int hash) {
			Reference<K, V> currRef = ref;
			while (currRef != null) {
				if (currRef.getHash() == hash) {
					Entry<K, V> entry = currRef.get();
					if (entry != null) {
						K entryKey = entry.getKey();
						if (ObjectUtils.nullSafeEquals(entryKey, key)) {
							return currRef;
						}
					}
				}
				currRef = currRef.getNext();
			}
			return null;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private Reference<K, V>[] createReferenceArray(int size) {
			return new Reference[size];
		}

		private int getIndex(int hash, Reference<K, V>[] references) {
			return (hash & (references.length - 1));
		}

		/**
		 * 返回当前引用数组的大小。
		 */
		public final int getSize() {
			return this.references.length;
		}

		/**
		 * 返回此段中引用的总数。
		 */
		public final int getCount() {
			return this.count.get();
		}
	}


	/**
	 * 对映射中包含的 {@link Entry} 的引用。实现通常是围绕特定 Java 引用实现（例如
	 * {@link SoftReference}）的包装器。
	 * @param <K> the key type
	 * @param <V> the value type
	 */
	protected interface Reference<K, V> {

		/**
		 * 返回被引用的条目，如果该条目不再可用则返回 {@code null}。
		 */
		@Nullable
		Entry<K, V> get();

		/**
		 * 返回该引用的哈希。
		 */
		int getHash();

		/**
		 * 返回链中的下一个引用，如果没有则返回 {@code null}。
		 */
		@Nullable
		Reference<K, V> getNext();

		/**
		 * 释放此条目，并确保它将由 {@code ReferenceManager#pollForPurge()} 返回。
		 */
		void release();
	}


	/**
	 * 单个映射条目。
	 * @param <K> the key type
	 * @param <V> the value type
	 */
	protected static final class Entry<K, V> implements Map.Entry<K, V> {

		@Nullable
		private final K key;

		@Nullable
		private volatile V value;

		public Entry(@Nullable K key, @Nullable V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		@Nullable
		public K getKey() {
			return this.key;
		}

		@Override
		@Nullable
		public V getValue() {
			return this.value;
		}

		@Override
		@Nullable
		public V setValue(@Nullable V value) {
			V previous = this.value;
			this.value = value;
			return previous;
		}

		@Override
		public String toString() {
			return (this.key + "=" + this.value);
		}

		@Override
		@SuppressWarnings("rawtypes")
		public final boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Map.Entry)) {
				return false;
			}
			Map.Entry otherEntry = (Map.Entry) other;
			return (ObjectUtils.nullSafeEquals(getKey(), otherEntry.getKey()) &&
					ObjectUtils.nullSafeEquals(getValue(), otherEntry.getValue()));
		}

		@Override
		public final int hashCode() {
			return (ObjectUtils.nullSafeHashCode(this.key) ^ ObjectUtils.nullSafeHashCode(this.value));
		}
	}


	/**
	 * 可针对 {@link Segment} {@link Segment#doTask 运行}的任务。
	 */
	private abstract class Task<T> {

		private final EnumSet<TaskOption> options;

		public Task(TaskOption... options) {
			this.options = (options.length == 0 ? EnumSet.noneOf(TaskOption.class) : EnumSet.of(options[0], options));
		}

		public boolean hasOption(TaskOption option) {
			return this.options.contains(option);
		}

		/**
		 * 执行该任务。
		 * @param ref the found reference (or {@code null})
		 * @param entry the found entry (or {@code null})
		 * @param entries access to the underlying entries
		 * @return the result of the task
		 * @see #execute(Reference, Entry)
		 */
		@Nullable
		protected T execute(@Nullable Reference<K, V> ref, @Nullable Entry<K, V> entry, @Nullable Entries<V> entries) {
			return execute(ref, entry);
		}

		/**
		 * 便捷方法，可用于不需要访问 {@link Entries} 的任务。
		 * @param ref the found reference (or {@code null})
		 * @param entry the found entry (or {@code null})
		 * @return the result of the task
		 * @see #execute(Reference, Entry, Entries)
		 */
		@Nullable
		protected T execute(@Nullable Reference<K, V> ref, @Nullable Entry<K, V> entry) {
			return null;
		}
	}


	/**
	 * {@code Task} 支持的各种选项。
	 */
	private enum TaskOption {

		RESTRUCTURE_BEFORE, RESTRUCTURE_AFTER, SKIP_IF_EMPTY, RESIZE
	}


	/**
	 * 允许任务访问 {@link ConcurrentReferenceHashMap.Segment} 条目。
	 */
	private interface Entries<V> {

		/**
		 * 使用指定的值添加新条目。
		 * @param value the value to add
		 */
		void add(@Nullable V value);
	}


	/**
	 * 内部条目集实现。
	 */
	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public boolean contains(@Nullable Object o) {
			if (o instanceof Map.Entry<?, ?>) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				Reference<K, V> ref = ConcurrentReferenceHashMap.this.getReference(entry.getKey(), Restructure.NEVER);
				Entry<K, V> otherEntry = (ref != null ? ref.get() : null);
				if (otherEntry != null) {
					return ObjectUtils.nullSafeEquals(entry.getValue(), otherEntry.getValue());
				}
			}
			return false;
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Map.Entry<?, ?>) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				return ConcurrentReferenceHashMap.this.remove(entry.getKey(), entry.getValue());
			}
			return false;
		}

		@Override
		public int size() {
			return ConcurrentReferenceHashMap.this.size();
		}

		@Override
		public void clear() {
			ConcurrentReferenceHashMap.this.clear();
		}
	}


	/**
	 * 内部条目迭代器实现。
	 */
	private class EntryIterator implements Iterator<Map.Entry<K, V>> {

		private int segmentIndex;

		private int referenceIndex;

		@Nullable
		private Reference<K, V>[] references;

		@Nullable
		private Reference<K, V> reference;

		@Nullable
		private Entry<K, V> next;

		@Nullable
		private Entry<K, V> last;

		public EntryIterator() {
			moveToNextSegment();
		}

		@Override
		public boolean hasNext() {
			getNextIfNecessary();
			return (this.next != null);
		}

		@Override
		public Entry<K, V> next() {
			getNextIfNecessary();
			if (this.next == null) {
				throw new NoSuchElementException();
			}
			this.last = this.next;
			this.next = null;
			return this.last;
		}

		private void getNextIfNecessary() {
			while (this.next == null) {
				moveToNextReference();
				if (this.reference == null) {
					return;
				}
				this.next = this.reference.get();
			}
		}

		private void moveToNextReference() {
			if (this.reference != null) {
				this.reference = this.reference.getNext();
			}
			while (this.reference == null && this.references != null) {
				if (this.referenceIndex >= this.references.length) {
					moveToNextSegment();
					this.referenceIndex = 0;
				}
				else {
					this.reference = this.references[this.referenceIndex];
					this.referenceIndex++;
				}
			}
		}

		private void moveToNextSegment() {
			this.reference = null;
			this.references = null;
			if (this.segmentIndex < ConcurrentReferenceHashMap.this.segments.length) {
				this.references = ConcurrentReferenceHashMap.this.segments[this.segmentIndex].references;
				this.segmentIndex++;
			}
		}

		@Override
		public void remove() {
			Assert.state(this.last != null, "No element to remove");
			ConcurrentReferenceHashMap.this.remove(this.last.getKey());
			this.last = null;
		}
	}


	/**
	 * 可执行的重构类型。
	 */
	protected enum Restructure {

		WHEN_NECESSARY, NEVER
	}


	/**
	 * 用于管理 {@link Reference 引用}的策略类。如果需要支持其他引用类型，可以重写此类。
	 */
	protected class ReferenceManager {

		private final ReferenceQueue<Entry<K, V>> queue = new ReferenceQueue<>();

		/**
		 * 用于创建新 {@link Reference} 的工厂方法。
		 * @param entry the entry contained in the reference
		 * @param hash the hash
		 * @param next the next reference in the chain, or {@code null} if none
		 * @return a new {@link Reference}
		 */
		public Reference<K, V> createReference(Entry<K, V> entry, int hash, @Nullable Reference<K, V> next) {
			if (ConcurrentReferenceHashMap.this.referenceType == ReferenceType.WEAK) {
				return new WeakEntryReference<>(entry, hash, next, this.queue);
			}
			return new SoftEntryReference<>(entry, hash, next, this.queue);
		}

		/**
		 * 返回任何已被垃圾回收且可从底层结构中清除的引用，如果无需清除引用则返回 {@code null}。此方法必须
		 * 是线程安全的，并且在返回 {@code null} 时最好不应阻塞。引用应当只返回一次。
		 * @return a reference to purge or {@code null}
		 */
		@SuppressWarnings("unchecked")
		@Nullable
		public Reference<K, V> pollForPurge() {
			return (Reference<K, V>) this.queue.poll();
		}
	}


	/**
	 * {@link SoftReference SoftReferences} 的内部 {@link Reference} 实现。
	 */
	private static final class SoftEntryReference<K, V> extends SoftReference<Entry<K, V>> implements Reference<K, V> {

		private final int hash;

		@Nullable
		private final Reference<K, V> nextReference;

		public SoftEntryReference(Entry<K, V> entry, int hash, @Nullable Reference<K, V> next,
				ReferenceQueue<Entry<K, V>> queue) {

			super(entry, queue);
			this.hash = hash;
			this.nextReference = next;
		}

		@Override
		public int getHash() {
			return this.hash;
		}

		@Override
		@Nullable
		public Reference<K, V> getNext() {
			return this.nextReference;
		}

		@Override
		public void release() {
			enqueue();
			clear();
		}
	}


	/**
	 * {@link WeakReference WeakReferences} 的内部 {@link Reference} 实现。
	 */
	private static final class WeakEntryReference<K, V> extends WeakReference<Entry<K, V>> implements Reference<K, V> {

		private final int hash;

		@Nullable
		private final Reference<K, V> nextReference;

		public WeakEntryReference(Entry<K, V> entry, int hash, @Nullable Reference<K, V> next,
				ReferenceQueue<Entry<K, V>> queue) {

			super(entry, queue);
			this.hash = hash;
			this.nextReference = next;
		}

		@Override
		public int getHash() {
			return this.hash;
		}

		@Override
		@Nullable
		public Reference<K, V> getNext() {
			return this.nextReference;
		}

		@Override
		public void release() {
			enqueue();
			clear();
		}
	}

}
