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

package org.springframework.util;

import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * {@code Map} 接口的扩展，用于存储多个值。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @param <K> the key type
 * @param <V> the value element type
 */
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

	/**
	 * 返回给定键的第一个值。
	 * @param key the key
	 * @return the first value for the specified key, or {@code null} if none
	 */
	@Nullable
	V getFirst(K key);

	/**
	 * 将给定的单个值添加到给定键的当前值列表中。
	 * @param key the key
	 * @param value the value to be added
	 */
	void add(K key, @Nullable V value);

	/**
	 * 将给定列表中的所有值添加到给定键的当前值列表中。
	 * @param key they key
	 * @param values the values to be added
	 * @since 5.0
	 */
	void addAll(K key, List<? extends V> values);

	/**
	 * 将给定 {@code MultiValueMap} 的所有值添加到当前值中。
	 * @param values the values to be added
	 * @since 5.0
	 */
	void addAll(MultiValueMap<K, V> values);

	/**
	 * 仅当映射不{@link #containsKey(Object) contain}给定键时，才{@link #add(Object, Object) Add}给定值。
	 * @param key the key
	 * @param value the value to be added
	 * @since 5.2
	 */
	default void addIfAbsent(K key, @Nullable V value) {
		if (!containsKey(key)) {
			add(key, value);
		}
	}

	/**
	 * 在给定键下设置给定的单个值。
	 * @param key the key
	 * @param value the value to set
	 */
	void set(K key, @Nullable V value);

	/**
	 * 设置给定的各个值。
	 * @param values the values.
	 */
	void setAll(Map<K, V> values);

	/**
	 * 返回一个 {@code Map}，其中包含此 {@code MultiValueMap} 中的各个第一个值。
	 * @return a single value representation of this map
	 */
	Map<K, V> toSingleValueMap();

}
