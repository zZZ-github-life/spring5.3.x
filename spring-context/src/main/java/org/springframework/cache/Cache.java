/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.concurrent.Callable;

import org.springframework.lang.Nullable;

/**
 * 定义通用缓存操作的接口。
 *
 * <b>注意：</b>由于缓存的通用用法，建议实现允许存储 {@code null} 值（例如用于缓存返回 {@code null}
 * 的方法）。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public interface Cache {

	/**
	 * 返回缓存名称。
	 */
	String getName();

	/**
	 * 返回底层原生缓存提供者。
	 */
	Object getNativeCache();

	/**
	 * 返回此缓存将指定键映射到的值。
	 *
	 * <p>如果缓存中没有此键的映射，则返回 {@code null}；否则，缓存的值（其本身可能为 {@code null}）将
	 * 以 {@link ValueWrapper} 形式返回。
	 * @param key the key whose associated value is to be returned
	 * @return the value to which this cache maps the specified key,
	 * contained within a {@link ValueWrapper} which may also hold
	 * a cached {@code null} value. A straight {@code null} being
	 * returned means that the cache contains no mapping for this key.
	 * @see #get(Object, Class)
	 * @see #get(Object, Callable)
	 */
	@Nullable
	ValueWrapper get(Object key);

	/**
	 * 返回此缓存将指定键映射到的值，并以泛型方式指定返回值要强制转换到的类型。
	 *
	 * <p>注意：{@code get} 的这种变体无法区分缓存的 {@code null} 值与根本未找到缓存条目。请改用标准的
	 * {@link #get(Object)} 变体来实现此目的。
	 * @param key the key whose associated value is to be returned
	 * @param type the required type of the returned value (may be
	 * {@code null} to bypass a type check; in case of a {@code null}
	 * value found in the cache, the specified type is irrelevant)
	 * @return the value to which this cache maps the specified key
	 * (which may be {@code null} itself), or also {@code null} if
	 * the cache contains no mapping for this key
	 * @throws IllegalStateException if a cache entry has been found
	 * but failed to match the specified type
	 * @since 4.0
	 * @see #get(Object)
	 */
	@Nullable
	<T> T get(Object key, @Nullable Class<T> type);

	/**
	 * 返回此缓存将指定键映射到的值，必要时从 {@code valueLoader} 获取该值。此方法为传统的“如果已缓存则
	 * 返回；否则创建、缓存并返回”模式提供了一种简单的替代方式。
	 *
	 * <p>如果可能，实现应确保加载操作是同步的，以便在同一个键上发生并发访问时，指定的
	 * {@code valueLoader} 只被调用一次。
	 *
	 * <p>如果 {@code valueLoader} 抛出异常，则该异常会被包装在 {@link ValueRetrievalException} 中。
	 * @param key the key whose associated value is to be returned
	 * @return the value to which this cache maps the specified key
	 * @throws ValueRetrievalException if the {@code valueLoader} throws an exception
	 * @since 4.3
	 * @see #get(Object)
	 */
	@Nullable
	<T> T get(Object key, Callable<T> valueLoader);

	/**
	 * 将指定值与缓存中的指定键关联。
	 *
	 * <p>如果缓存先前包含此键的映射，则旧值将被指定值替换。
	 *
	 * <p>实际的注册可能以异步或延迟方式执行，后续查找可能还看不到该条目。事务性缓存装饰器就可能出现这
	 * 种情况。如需保证立即注册，请使用 {@link #putIfAbsent}。
	 * @param key the key with which the specified value is to be associated
	 * @param value the value to be associated with the specified key
	 * @see #putIfAbsent(Object, Object)
	 */
	void put(Object key, @Nullable Object value);

	/**
	 * 如果尚未设置，则以原子方式将指定值与缓存中的指定键关联。
	 *
	 * <p>这等价于：
	 * <pre><code>
	 * ValueWrapper existingValue = cache.get(key);
	 * if (existingValue == null) {
	 *     cache.put(key, value);
	 * }
	 * return existingValue;
	 * </code></pre>
	 * 区别在于该操作以原子方式执行。虽然所有开箱即用的 {@link CacheManager} 实现都能以原子方式执行 put，
	 * 但该操作也可以分两步以非原子方式实现，例如先检查是否存在，然后再执行 put。有关更多详细信息，请
	 * 查阅你所使用的原生缓存实现的文档。
	 *
	 * <p>默认实现按照上面的代码片段委托给 {@link #get(Object)} 和 {@link #put(Object, Object)}。
	 * @param key the key with which the specified value is to be associated
	 * @param value the value to be associated with the specified key
	 * @return the value to which this cache maps the specified key (which may be
	 * {@code null} itself), or also {@code null} if the cache did not contain any
	 * mapping for that key prior to this call. Returning {@code null} is therefore
	 * an indicator that the given {@code value} has been associated with the key.
	 * @since 4.1
	 * @see #put(Object, Object)
	 */
	@Nullable
	default ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		ValueWrapper existingValue = get(key);
		if (existingValue == null) {
			put(key, value);
		}
		return existingValue;
	}

	/**
	 * 如果存在，则从此缓存中逐出此键的映射。
	 *
	 * <p>实际的逐出可能以异步或延迟方式执行，后续查找可能仍会看到该条目。事务性缓存装饰器就可能出现这
	 * 种情况。如需保证立即移除，请使用 {@link #evictIfPresent}。
	 * @param key the key whose mapping is to be removed from the cache
	 * @see #evictIfPresent(Object)
	 */
	void evict(Object key);

	/**
	 * 如果存在，则从此缓存中逐出此键的映射，并期望该键对后续查找立即可见。
	 *
	 * <p>默认实现委托给 {@link #evict(Object)}，对于无法确定键先前是否存在的情况返回 {@code false}。鼓
	 * 励缓存提供者尤其是缓存装饰器尽可能执行立即逐出（例如在事务中通常延迟执行的缓存操作的情况下），并
	 * 可靠地确定给定键先前是否存在。
	 * @param key the key whose mapping is to be removed from the cache
	 * @return {@code true} if the cache was known to have a mapping for
	 * this key before, {@code false} if it did not (or if prior presence
	 * could not be determined)
	 * @since 5.2
	 * @see #evict(Object)
	 */
	default boolean evictIfPresent(Object key) {
		evict(key);
		return false;
	}

	/**
	 * 通过移除所有映射来清空缓存。
	 *
	 * <p>实际的清空可能以异步或延迟方式执行，后续查找可能仍会看到这些条目。事务性缓存装饰器就可能出现
	 * 这种情况。如需保证立即移除条目，请使用 {@link #invalidate()}。
	 * @see #invalidate()
	 */
	void clear();

	/**
	 * 通过移除所有映射使缓存失效，并期望所有条目对后续查找立即不可见。
	 * @return {@code true} if the cache was known to have mappings before,
	 * {@code false} if it did not (or if prior presence of entries could
	 * not be determined)
	 * @since 5.2
	 * @see #clear()
	 */
	default boolean invalidate() {
		clear();
		return false;
	}


	/**
	 * 表示缓存值的（包装）对象。
	 */
	@FunctionalInterface
	interface ValueWrapper {

		/**
		 * 返回缓存中的实际值。
		 */
		@Nullable
		Object get();
	}


	/**
	 * 当值加载器回调因异常而失败时，从 {@link #get(Object, Callable)} 抛出的包装异常。
	 * @since 4.3
	 */
	@SuppressWarnings("serial")
	class ValueRetrievalException extends RuntimeException {

		@Nullable
		private final Object key;

		public ValueRetrievalException(@Nullable Object key, Callable<?> loader, Throwable ex) {
			super(String.format("Value for key '%s' could not be loaded using '%s'", key, loader), ex);
			this.key = key;
		}

		@Nullable
		public Object getKey() {
			return this.key;
		}
	}

}
