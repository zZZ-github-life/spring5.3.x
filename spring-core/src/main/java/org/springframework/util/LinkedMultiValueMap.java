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

package org.springframework.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link MultiValueMap} 的简单实现，它包装一个 {@link LinkedHashMap}，将多个值存储在
 * {@link ArrayList} 中。
 *
 * <p>此 Map 实现通常不是线程安全的。它主要设计用于从请求对象中暴露的数据结构，仅限单线程使用。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @param <K> the key type
 * @param <V> the value element type
 */
public class LinkedMultiValueMap<K, V> extends MultiValueMapAdapter<K, V>  // new public base class in 5.3
		implements Serializable, Cloneable {

	private static final long serialVersionUID = 3801124242820219131L;


	/**
	 * 创建一个包装 {@link LinkedHashMap} 的新 LinkedMultiValueMap。
	 */
	public LinkedMultiValueMap() {
		super(new LinkedHashMap<>());
	}

	/**
	 * 创建一个新的 LinkedMultiValueMap，它包装一个 {@link LinkedHashMap}，其初始容量可容纳指定数量的元
	 * 素，预计不会立即发生调整大小/重新哈希操作。
	 * @param expectedSize the expected number of elements (with a corresponding
	 * capacity to be derived so that no resize/rehash operations are needed)
	 * @see CollectionUtils#newLinkedHashMap(int)
	 */
	public LinkedMultiValueMap(int expectedSize) {
		super(CollectionUtils.newLinkedHashMap(expectedSize));
	}

	/**
	 * 复制构造函数：创建一个与指定 Map 具有相同映射的新 LinkedMultiValueMap。注意，这是浅拷贝；其持有
	 * 值的 List 条目将被复用，因此无法独立修改。
	 * @param otherMap the Map whose mappings are to be placed in this Map
	 * @see #clone()
	 * @see #deepCopy()
	 */
	public LinkedMultiValueMap(Map<K, List<V>> otherMap) {
		super(new LinkedHashMap<>(otherMap));
	}


	/**
	 * 创建此 Map 的深拷贝。
	 * @return a copy of this Map, including a copy of each value-holding List entry
	 * (consistently using an independent modifiable {@link ArrayList} for each entry)
	 * along the lines of {@code MultiValueMap.addAll} semantics
	 * @since 4.2
	 * @see #addAll(MultiValueMap)
	 * @see #clone()
	 */
	public LinkedMultiValueMap<K, V> deepCopy() {
		LinkedMultiValueMap<K, V> copy = new LinkedMultiValueMap<>(size());
		forEach((key, values) -> copy.put(key, new ArrayList<>(values)));
		return copy;
	}

	/**
	 * 创建此 Map 的常规拷贝。
	 * @return a shallow copy of this Map, reusing this Map's value-holding List entries
	 * (even if some entries are shared or unmodifiable) along the lines of standard
	 * {@code Map.put} semantics
	 * @since 4.2
	 * @see #put(Object, List)
	 * @see #putAll(Map)
	 * @see LinkedMultiValueMap#LinkedMultiValueMap(Map)
	 * @see #deepCopy()
	 */
	@Override
	public LinkedMultiValueMap<K, V> clone() {
		return new LinkedMultiValueMap<>(this);
	}

}
