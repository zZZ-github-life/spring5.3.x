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

package org.springframework.beans.factory.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;

/**
 * 用于保存受管 Map 值的标记集合类，其中可能包含运行时 Bean 引用（将被解析为 Bean 对象）。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 27.05.2003
 * @param <K> 键类型
 * @param <V> 值类型
 */
@SuppressWarnings("serial")
public class ManagedMap<K, V> extends LinkedHashMap<K, V> implements Mergeable, BeanMetadataElement {

	@Nullable
	private Object source;

	@Nullable
	private String keyTypeName;

	@Nullable
	private String valueTypeName;

	private boolean mergeEnabled;


	public ManagedMap() {
	}

	public ManagedMap(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 返回一个新实例，其中包含从给定条目中提取的键和值。这些条目本身不会存储在 map 中。
	 * @param entries 包含用于填充该 Map 的键和值的 {@code Map.Entry}
	 * @param <K> {@code Map} 的键类型
	 * @param <V> {@code Map} 的值类型
	 * @return 包含指定映射关系的 {@code Map}
	 * @since 5.3.16
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <K,V> ManagedMap<K,V> ofEntries(Entry<? extends K, ? extends V>... entries) {
		ManagedMap<K,V > map = new ManagedMap<>();
		for (Entry<? extends K, ? extends V> entry : entries) {
			map.put(entry.getKey(), entry.getValue());
		}
		return map;
	}

	/**
	 * 为此元数据元素设置配置源 {@code Object}。
	 * <p>对象的具体类型取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * 设置此 map 要使用的默认键类型名（类名）。
	 */
	public void setKeyTypeName(@Nullable String keyTypeName) {
		this.keyTypeName = keyTypeName;
	}

	/**
	 * 返回此 map 要使用的默认键类型名（类名）。
	 */
	@Nullable
	public String getKeyTypeName() {
		return this.keyTypeName;
	}

	/**
	 * 设置此 map 要使用的默认值类型名（类名）。
	 */
	public void setValueTypeName(@Nullable String valueTypeName) {
		this.valueTypeName = valueTypeName;
	}

	/**
	 * 返回此 map 要使用的默认值类型名（类名）。
	 */
	@Nullable
	public String getValueTypeName() {
		return this.valueTypeName;
	}

	/**
	 * 设置当存在 'parent' 集合值时，是否为此集合启用合并。
	 */
	public void setMergeEnabled(boolean mergeEnabled) {
		this.mergeEnabled = mergeEnabled;
	}

	@Override
	public boolean isMergeEnabled() {
		return this.mergeEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object merge(@Nullable Object parent) {
		if (!this.mergeEnabled) {
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof Map)) {
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		Map<K, V> merged = new ManagedMap<>();
		merged.putAll((Map<K, V>) parent);
		merged.putAll(this);
		return merged;
	}

}
