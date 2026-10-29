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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * 用于 Map 的属性编辑器，可将任意源 Map 转换为给定的目标 Map 类型。
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see java.util.Map
 * @see java.util.SortedMap
 */
public class CustomMapEditor extends PropertyEditorSupport {

	@SuppressWarnings("rawtypes")
	private final Class<? extends Map> mapType;

	private final boolean nullAsEmptyMap;


	/**
	 * 为给定目标类型创建新的 CustomMapEditor，传入的 {@code null} 保持原样。
	 * @param mapType the target type, which needs to be a
	 * sub-interface of Map or a concrete Map class
	 * @see java.util.Map
	 * @see java.util.HashMap
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings("rawtypes")
	public CustomMapEditor(Class<? extends Map> mapType) {
		this(mapType, false);
	}

	/**
	 * 为给定目标类型创建新的 CustomMapEditor。<p>如果传入值的类型就是给定类型，则按原样使用。如果它是
	 * 其他 Map 类型或数组，则将其转换为给定 Map 类型的默认实现。如果值属于其他任何类型，则将创建一个仅
	 * 包含该值的目标 Map。<p>默认的 Map 实现为：SortedMap 使用 TreeMap，Map 使用 LinkedHashMap。
	 * @param mapType the target type, which needs to be a
	 * sub-interface of Map or a concrete Map class
	 * @param nullAsEmptyMap ap whether to convert an incoming {@code null}
	 * value to an empty Map (of the appropriate type)
	 * @see java.util.Map
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings("rawtypes")
	public CustomMapEditor(Class<? extends Map> mapType, boolean nullAsEmptyMap) {
		Assert.notNull(mapType, "Map type is required");
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException(
					"Map type [" + mapType.getName() + "] does not implement [java.util.Map]");
		}
		this.mapType = mapType;
		this.nullAsEmptyMap = nullAsEmptyMap;
	}


	/**
	 * 将给定的文本值转换为仅含一个元素的 Map。
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	/**
	 * 将给定值转换为目标类型的 Map。
	 */
	@Override
	public void setValue(@Nullable Object value) {
		if (value == null && this.nullAsEmptyMap) {
			super.setValue(createMap(this.mapType, 0));
		}
		else if (value == null || (this.mapType.isInstance(value) && !alwaysCreateNewMap())) {
			// Use the source value as-is, as it matches the target type.
			super.setValue(value);
		}
		else if (value instanceof Map) {
			// Convert Map elements.
			Map<?, ?> source = (Map<?, ?>) value;
			Map<Object, Object> target = createMap(this.mapType, source.size());
			source.forEach((key, val) -> target.put(convertKey(key), convertValue(val)));
			super.setValue(target);
		}
		else {
			throw new IllegalArgumentException("Value cannot be converted to Map: " + value);
		}
	}

	/**
	 * 创建给定类型的 Map，并使用给定的初始容量（如果该 Map 类型支持）。
	 * @param mapType a sub-interface of Map
	 * @param initialCapacity the initial capacity
	 * @return the new Map instance
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected Map<Object, Object> createMap(Class<? extends Map> mapType, int initialCapacity) {
		if (!mapType.isInterface()) {
			try {
				return ReflectionUtils.accessibleConstructor(mapType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate map class: " + mapType.getName(), ex);
			}
		}
		else if (SortedMap.class == mapType) {
			return new TreeMap<>();
		}
		else {
			return new LinkedHashMap<>(initialCapacity);
		}
	}

	/**
	 * 返回是否始终创建新的 Map，即使传入 Map 的类型已经匹配。<p>默认为 "false"；可以覆盖此方法以强制创
	 * 建新的 Map，例如在任何情况下都转换元素。
	 * @see #convertKey
	 * @see #convertValue
	 */
	protected boolean alwaysCreateNewMap() {
		return false;
	}

	/**
	 * 用于转换遇到的每个 Map 键的钩子。默认实现只是按原样返回传入的键。<p>可以覆盖以对某些键执行转换，
	 * 例如从 String 转换为 Integer。<p>仅在确实创建新 Map 时才调用！如果传入 Map 的类型已经匹配，默认
	 * 情况下不会创建新 Map。覆盖 {@link #alwaysCreateNewMap()} 以强制在每种情况下都创建新的 Map。
	 * @param key the source key
	 * @return the key to be used in the target Map
	 * @see #alwaysCreateNewMap
	 */
	protected Object convertKey(Object key) {
		return key;
	}

	/**
	 * 用于转换遇到的每个 Map 值的钩子。默认实现只是按原样返回传入的值。<p>可以覆盖以对某些值执行转换，
	 * 例如从 String 转换为 Integer。<p>仅在确实创建新 Map 时才调用！如果传入 Map 的类型已经匹配，默认
	 * 情况下不会创建新 Map。覆盖 {@link #alwaysCreateNewMap()} 以强制在每种情况下都创建新的 Map。
	 * @param value the source value
	 * @return the value to be used in the target Map
	 * @see #alwaysCreateNewMap
	 */
	protected Object convertValue(Object value) {
		return value;
	}


	/**
	 * 此实现返回 {@code null}，表示没有合适的文本表示形式。
	 */
	@Override
	@Nullable
	public String getAsText() {
		return null;
	}

}
