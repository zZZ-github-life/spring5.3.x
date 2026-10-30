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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

import org.springframework.lang.Nullable;

/**
 * 杂项集合工具方法。主要供框架内部使用。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @since 1.1.3
 */
public abstract class CollectionUtils {

	/**
	 * 用于 {@link HashMap}/{@link LinkedHashMap} 变体的默认负载因子。
	 * @see #newHashMap(int)
	 * @see #newLinkedHashMap(int)
	 */
	static final float DEFAULT_LOAD_FACTOR = 0.75f;


	/**
	 * 如果所提供的 Collection 为 {@code null} 或为空，则返回 {@code true}。否则返回 {@code false}。
	 * @param collection 要检查的 Collection
	 * @return 给定 Collection 是否为空
	 */
	public static boolean isEmpty(@Nullable Collection<?> collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * 如果所提供的 Map 为 {@code null} 或为空，则返回 {@code true}。否则返回 {@code false}。
	 * @param map 要检查的 Map
	 * @return 给定 Map 是否为空
	 */
	public static boolean isEmpty(@Nullable Map<?, ?> map) {
		return (map == null || map.isEmpty());
	}

	/**
	 * 创建一个具有初始容量的新 {@link HashMap}，该容量可容纳指定数量的元素，而不会立即触发扩容/重新哈
	 * 希操作。
	 * <p>这与常规的 {@link HashMap} 构造方法不同，后者接受的是相对于负载因子的初始容量，但在效果上与
	 * JDK 的 {@link java.util.concurrent.ConcurrentHashMap#ConcurrentHashMap(int)} 保持一致。
	 * @param expectedSize 预期的元素数量
	 * （会据此推导相应容量，以免需要扩容或重新哈希）
	 * @since 5.3
	 * @see #newLinkedHashMap(int)
	 */
	public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
		return new HashMap<>(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
	}

	/**
	 * 创建一个具有初始容量的新 {@link LinkedHashMap}，该容量可容纳指定数量的元素，而不会立即触发扩容/
	 * 重新哈希操作。
	 * <p>这与常规的 {@link LinkedHashMap} 构造方法不同，后者接受相对于负载因子的初始容量，但从 5.3 起
	 * 与 Spring 自身的 {@link LinkedCaseInsensitiveMap} 和 {@link LinkedMultiValueMap} 构造方法语义保
	 * 持一致。
	 * @param expectedSize 预期的元素数量
	 * （会据此推导相应容量，以免需要扩容或重新哈希）
	 * @since 5.3
	 * @see #newHashMap(int)
	 */
	public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize) {
		return new LinkedHashMap<>(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
	}

	private static int computeMapInitialCapacity(int expectedSize) {
		return (int) Math.ceil(expectedSize / (double) DEFAULT_LOAD_FACTOR);
	}

	/**
	 * 将所提供的数组转换为 List。基本类型数组会被转换为相应包装器类型的 List。
	 * <p><b>NOTE:</b> 通常应优先使用标准的 {@link Arrays#asList} 方法。这里的 {@code arrayToList} 方法
	 * 只是用于处理运行时可能是 {@code Object[]} 或基本类型数组的传入 Object 值。
	 * <p>{@code null} 源值将被转换为空 List。
	 * @param source （可能为基本类型的）数组
	 * @return 转换后的 List 结果
	 * @see ObjectUtils#toObjectArray(Object)
	 * @see Arrays#asList(Object[])
	 */
	public static List<?> arrayToList(@Nullable Object source) {
		return Arrays.asList(ObjectUtils.toObjectArray(source));
	}

	/**
	 * 将给定数组合并到给定 Collection 中。
	 * @param array 要合并的数组（可以为 {@code null}）
	 * @param collection 要合并数组的目标 Collection
	 */
	@SuppressWarnings("unchecked")
	public static <E> void mergeArrayIntoCollection(@Nullable Object array, Collection<E> collection) {
		Object[] arr = ObjectUtils.toObjectArray(array);
		for (Object elem : arr) {
			collection.add((E) elem);
		}
	}

	/**
	 * 将给定的 Properties 实例合并到给定 Map 中，复制所有属性（键值对）。
	 * <p>使用 {@code Properties.propertyNames()}，以便也能捕获链接到原始 Properties 实例中的默认属性。
	 * @param props 要合并的 Properties 实例（可以为 {@code null}）
	 * @param map 要将属性合并到的目标 Map
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> void mergePropertiesIntoMap(@Nullable Properties props, Map<K, V> map) {
		if (props != null) {
			for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements();) {
				String key = (String) en.nextElement();
				Object value = props.get(key);
				if (value == null) {
					// Allow for defaults fallback or potentially overridden accessor...
					value = props.getProperty(key);
				}
				map.put((K) key, (V) value);
			}
		}
	}


	/**
	 * 检查给定 Iterator 是否包含给定元素。
	 * @param iterator 要检查的 Iterator
	 * @param element 要查找的元素
	 * @return 如果找到则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean contains(@Nullable Iterator<?> iterator, Object element) {
		if (iterator != null) {
			while (iterator.hasNext()) {
				Object candidate = iterator.next();
				if (ObjectUtils.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 检查给定 Enumeration 是否包含给定元素。
	 * @param enumeration 要检查的 Enumeration
	 * @param element 要查找的元素
	 * @return 如果找到则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean contains(@Nullable Enumeration<?> enumeration, Object element) {
		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				Object candidate = enumeration.nextElement();
				if (ObjectUtils.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 检查给定 Collection 是否包含给定的元素实例。
	 * <p>强制要求存在的是给定实例，而不是对相等的元素也返回 {@code true}。
	 * @param collection 要检查的 Collection
	 * @param element 要查找的元素
	 * @return 如果找到则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean containsInstance(@Nullable Collection<?> collection, Object element) {
		if (collection != null) {
			for (Object candidate : collection) {
				if (candidate == element) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 如果 '{@code candidates}' 中的任一元素包含在 '{@code source}' 中，则返回 {@code true}；否则返回
	 * {@code false}。
	 * @param source 源 Collection
	 * @param candidates 要搜索的候选对象
	 * @return 是否找到任一候选对象
	 */
	public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
		return findFirstMatch(source, candidates) != null;
	}

	/**
	 * 返回 '{@code candidates}' 中第一个包含在 '{@code source}' 中的元素。如果 '{@code candidates}' 中
	 * 没有元素存在于 '{@code source}' 中，则返回 {@code null}。迭代顺序取决于 {@link Collection} 的具
	 * 体实现。
	 * @param source 源 Collection
	 * @param candidates 要搜索的候选对象
	 * @return 第一个存在的对象，如果未找到则为 {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E> E findFirstMatch(Collection<?> source, Collection<E> candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return null;
		}
		for (Object candidate : candidates) {
			if (source.contains(candidate)) {
				return (E) candidate;
			}
		}
		return null;
	}

	/**
	 * 在给定 Collection 中查找给定类型的单个值。
	 * @param collection 要搜索的 Collection
	 * @param type 要查找的类型
	 * @return 在存在明确匹配时找到的给定类型的值，
	 * 如果没有找到或找到多个此类值则为 {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T findValueOfType(Collection<?> collection, @Nullable Class<T> type) {
		if (isEmpty(collection)) {
			return null;
		}
		T value = null;
		for (Object element : collection) {
			if (type == null || type.isInstance(element)) {
				if (value != null) {
					// More than one value found... no clear single value.
					return null;
				}
				value = (T) element;
			}
		}
		return value;
	}

	/**
	 * 在给定 Collection 中查找给定类型之一的单个值：先在 Collection 中搜索第一个类型的值，再搜索第二个
	 * 类型的值，依此类推。
	 * @param collection 要搜索的集合
	 * @param types 要查找的类型，按优先级排列
	 * @return 在存在明确匹配时找到的给定类型之一的值，
	 * 如果没有找到或找到多个此类值则为 {@code null}
	 */
	@Nullable
	public static Object findValueOfType(Collection<?> collection, Class<?>[] types) {
		if (isEmpty(collection) || ObjectUtils.isEmpty(types)) {
			return null;
		}
		for (Class<?> type : types) {
			Object value = findValueOfType(collection, type);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	/**
	 * 判断给定 Collection 是否只包含一个唯一的对象。
	 * @param collection 要检查的 Collection
	 * @return 如果集合包含单个引用或多个指向同一实例的引用，则返回 {@code true}，
	 * 否则返回 {@code false}
	 */
	public static boolean hasUniqueObject(Collection<?> collection) {
		if (isEmpty(collection)) {
			return false;
		}
		boolean hasCandidate = false;
		Object candidate = null;
		for (Object elem : collection) {
			if (!hasCandidate) {
				hasCandidate = true;
				candidate = elem;
			}
			else if (candidate != elem) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 查找给定 Collection 的公共元素类型（如果有）。
	 * @param collection 要检查的 Collection
	 * @return 公共元素类型，如果未找到明确的公共类型
	 * （或集合为空）则为 {@code null}
	 */
	@Nullable
	public static Class<?> findCommonElementType(Collection<?> collection) {
		if (isEmpty(collection)) {
			return null;
		}
		Class<?> candidate = null;
		for (Object val : collection) {
			if (val != null) {
				if (candidate == null) {
					candidate = val.getClass();
				}
				else if (candidate != val.getClass()) {
					return null;
				}
			}
		}
		return candidate;
	}

	/**
	 * 检索给定 Set 的第一个元素，使用 {@link SortedSet#first()}，否则使用迭代器。
	 * @param set 要检查的 Set（可以为 {@code null} 或为空）
	 * @return 第一个元素，如果没有则为 {@code null}
	 * @since 5.2.3
	 * @see SortedSet
	 * @see LinkedHashMap#keySet()
	 * @see java.util.LinkedHashSet
	 */
	@Nullable
	public static <T> T firstElement(@Nullable Set<T> set) {
		if (isEmpty(set)) {
			return null;
		}
		if (set instanceof SortedSet) {
			return ((SortedSet<T>) set).first();
		}

		Iterator<T> it = set.iterator();
		T first = null;
		if (it.hasNext()) {
			first = it.next();
		}
		return first;
	}

	/**
	 * 检索给定 List 的第一个元素，即访问索引 0 处的元素。
	 * @param list 要检查的 List（可以为 {@code null} 或为空）
	 * @return 第一个元素，如果没有则为 {@code null}
	 * @since 5.2.3
	 */
	@Nullable
	public static <T> T firstElement(@Nullable List<T> list) {
		if (isEmpty(list)) {
			return null;
		}
		return list.get(0);
	}

	/**
	 * 检索给定 Set 的最后一个元素，使用 {@link SortedSet#last()}，否则遍历所有元素（假定是链式 Set）。
	 * @param set 要检查的 Set（可以为 {@code null} 或为空）
	 * @return 最后一个元素，如果没有则为 {@code null}
	 * @since 5.0.3
	 * @see SortedSet
	 * @see LinkedHashMap#keySet()
	 * @see java.util.LinkedHashSet
	 */
	@Nullable
	public static <T> T lastElement(@Nullable Set<T> set) {
		if (isEmpty(set)) {
			return null;
		}
		if (set instanceof SortedSet) {
			return ((SortedSet<T>) set).last();
		}

		// Full iteration necessary...
		Iterator<T> it = set.iterator();
		T last = null;
		while (it.hasNext()) {
			last = it.next();
		}
		return last;
	}

	/**
	 * 检索给定 List 的最后一个元素，即访问最大索引处的元素。
	 * @param list 要检查的 List（可以为 {@code null} 或为空）
	 * @return 最后一个元素，如果没有则为 {@code null}
	 * @since 5.0.3
	 */
	@Nullable
	public static <T> T lastElement(@Nullable List<T> list) {
		if (isEmpty(list)) {
			return null;
		}
		return list.get(list.size() - 1);
	}

	/**
	 * 将给定枚举中的元素编组为给定类型的数组。枚举元素必须可赋值给给定数组的类型。返回的数组将是与给定
	 * 数组不同的实例。
	 */
	public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
		ArrayList<A> elements = new ArrayList<>();
		while (enumeration.hasMoreElements()) {
			elements.add(enumeration.nextElement());
		}
		return elements.toArray(array);
	}

	/**
	 * 将 {@link Enumeration} 适配为 {@link Iterator}。
	 * @param enumeration 原始的 {@code Enumeration}
	 * @return 适配后的 {@code Iterator}
	 */
	public static <E> Iterator<E> toIterator(@Nullable Enumeration<E> enumeration) {
		return (enumeration != null ? new EnumerationIterator<>(enumeration) : Collections.emptyIterator());
	}

	/**
	 * 将 {@code Map<K, List<V>>} 适配为 {@code MultiValueMap<K, V>}。
	 * @param targetMap 原始 map
	 * @return 适配后的多值 map（包装原始 map）
	 * @since 3.1
	 */
	public static <K, V> MultiValueMap<K, V> toMultiValueMap(Map<K, List<V>> targetMap) {
		return new MultiValueMapAdapter<>(targetMap);
	}

	/**
	 * 返回指定多值映射的不可修改视图。
	 * @param targetMap 要为其返回不可修改视图的 map。
	 * @return 指定多值 map 的不可修改视图
	 * @since 3.1
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> MultiValueMap<K, V> unmodifiableMultiValueMap(
			MultiValueMap<? extends K, ? extends V> targetMap) {

		Assert.notNull(targetMap, "'targetMap' must not be null");
		Map<K, List<V>> result = newLinkedHashMap(targetMap.size());
		targetMap.forEach((key, value) -> {
			List<? extends V> values = Collections.unmodifiableList(value);
			result.put(key, (List<V>) values);
		});
		Map<K, List<V>> unmodifiableMap = Collections.unmodifiableMap(result);
		return toMultiValueMap(unmodifiableMap);
	}


	/**
	 * 包装 Enumeration 的 Iterator。
	 */
	private static class EnumerationIterator<E> implements Iterator<E> {

		private final Enumeration<E> enumeration;

		public EnumerationIterator(Enumeration<E> enumeration) {
			this.enumeration = enumeration;
		}

		@Override
		public boolean hasNext() {
			return this.enumeration.hasMoreElements();
		}

		@Override
		public E next() {
			return this.enumeration.nextElement();
		}

		@Override
		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("Not supported");
		}
	}


}
