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

package org.springframework.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * 能识别常见 Java 和 Spring 集合类型的集合工厂。
 *
 * <p>主要供框架内部使用。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 1.1.1
 */
public final class CollectionFactory {

	private static final Set<Class<?>> approximableCollectionTypes = new HashSet<>();

	private static final Set<Class<?>> approximableMapTypes = new HashSet<>();


	static {
		// Standard collection interfaces
		approximableCollectionTypes.add(Collection.class);
		approximableCollectionTypes.add(List.class);
		approximableCollectionTypes.add(Set.class);
		approximableCollectionTypes.add(SortedSet.class);
		approximableCollectionTypes.add(NavigableSet.class);
		approximableMapTypes.add(Map.class);
		approximableMapTypes.add(SortedMap.class);
		approximableMapTypes.add(NavigableMap.class);

		// Common concrete collection classes
		approximableCollectionTypes.add(ArrayList.class);
		approximableCollectionTypes.add(LinkedList.class);
		approximableCollectionTypes.add(HashSet.class);
		approximableCollectionTypes.add(LinkedHashSet.class);
		approximableCollectionTypes.add(TreeSet.class);
		approximableCollectionTypes.add(EnumSet.class);
		approximableMapTypes.add(HashMap.class);
		approximableMapTypes.add(LinkedHashMap.class);
		approximableMapTypes.add(TreeMap.class);
		approximableMapTypes.add(EnumMap.class);
	}


	private CollectionFactory() {
	}


	/**
	 * 判断给定的集合类型是否为<em>可近似</em>类型，
	 * 即 {@link #createApproximateCollection} 能够近似创建的类型。
	 * @param collectionType 要检查的集合类型
	 * @return 如果该类型是 <em>approximable</em> 则返回 {@code true}
	 */
	public static boolean isApproximableCollectionType(@Nullable Class<?> collectionType) {
		return (collectionType != null && approximableCollectionTypes.contains(collectionType));
	}

	/**
	 * 为给定的集合创建最接近的集合。
	 * <p><strong>警告</strong>：
	 * 由于参数化类型 {@code E} 并未绑定到提供的 {@code collection} 中所包含元素的类型，
	 * 因此如果提供的 {@code collection} 是 {@link EnumSet}，则无法保证类型安全。在此类场景中，
	 * 调用者需负责确保提供的 {@code collection} 的元素类型是与类型 {@code E} 匹配的枚举类型。
	 * 作为替代方案，调用者可以将返回值视为原始集合或 {@link Object} 的集合。
	 * @param collection 原始集合对象，可能为 {@code null}
	 * @param capacity 初始容量
	 * @return 新的空集合实例
	 * @see #isApproximableCollectionType
	 * @see java.util.LinkedList
	 * @see java.util.ArrayList
	 * @see java.util.EnumSet
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings({"rawtypes", "unchecked", "cast"})
	public static <E> Collection<E> createApproximateCollection(@Nullable Object collection, int capacity) {
		if (collection instanceof LinkedList) {
			return new LinkedList<>();
		}
		else if (collection instanceof List) {
			return new ArrayList<>(capacity);
		}
		else if (collection instanceof EnumSet) {
			// Cast is necessary for compilation in Eclipse 4.4.1.
			Collection<E> enumSet = (Collection<E>) EnumSet.copyOf((EnumSet) collection);
			enumSet.clear();
			return enumSet;
		}
		else if (collection instanceof SortedSet) {
			return new TreeSet<>(((SortedSet<E>) collection).comparator());
		}
		else {
			return new LinkedHashSet<>(capacity);
		}
	}

	/**
	 * 为给定的集合类型创建最合适的集合。
	 * <p>以 {@code null} 元素类型委托给 {@link #createCollection(Class, Class, int)}。
	 * @param collectionType 目标集合的期望类型（永不为 {@code null}）
	 * @param capacity 初始容量
	 * @return 新的集合实例
	 * @throws IllegalArgumentException 如果提供的 {@code collectionType} 为 {@code null} 或类型为 {@link EnumSet}
	 */
	public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
		return createCollection(collectionType, null, capacity);
	}

	/**
	 * 为给定的集合类型创建最合适的集合。
	 * <p><strong>警告</strong>：由于参数化类型 {@code E} 未绑定到提供的 {@code elementType}，
	 * 因此如果期望的 {@code collectionType} 是 {@link EnumSet}，则无法保证类型安全。
	 * 在此类场景中，
	 * 调用者需负责确保提供的 {@code elementType} 是与类型 {@code E} 匹配的枚举类型。
	 * 作为替代方案，调用者可以将返回值视为原始集合或 {@link Object} 的集合。
	 * @param collectionType 目标集合的期望类型（永不为 {@code null}）
	 * @param elementType 集合的元素类型，未知时为 {@code null}（注意：仅与 {@link EnumSet} 的创建相关）
	 * @param capacity 初始容量
	 * @return 新的集合实例
	 * @throws IllegalArgumentException 如果提供的 {@code collectionType} 为 {@code null}；
	 * 或者期望的 {@code collectionType} 为 {@link EnumSet}，
	 * 且提供的 {@code elementType} 不是 {@link Enum} 的子类型
	 * @since 4.1.3
	 * @see java.util.LinkedHashSet
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.EnumSet
	 */
	@SuppressWarnings({"unchecked", "cast"})
	public static <E> Collection<E> createCollection(Class<?> collectionType, @Nullable Class<?> elementType, int capacity) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (LinkedHashSet.class == collectionType || HashSet.class == collectionType ||
				Set.class == collectionType || Collection.class == collectionType) {
			return new LinkedHashSet<>(capacity);
		}
		else if (ArrayList.class == collectionType || List.class == collectionType) {
			return new ArrayList<>(capacity);
		}
		else if (LinkedList.class == collectionType) {
			return new LinkedList<>();
		}
		else if (TreeSet.class == collectionType || NavigableSet.class == collectionType
				|| SortedSet.class == collectionType) {
			return new TreeSet<>();
		}
		else if (EnumSet.class.isAssignableFrom(collectionType)) {
			Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
			// Cast is necessary for compilation in Eclipse 4.4.1.
			return (Collection<E>) EnumSet.noneOf(asEnumType(elementType));
		}
		else {
			if (collectionType.isInterface() || !Collection.class.isAssignableFrom(collectionType)) {
				throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
			}
			try {
				return (Collection<E>) ReflectionUtils.accessibleConstructor(collectionType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
					"Could not instantiate Collection type: " + collectionType.getName(), ex);
			}
		}
	}

	/**
	 * 判断给定的映射类型是否为<em>可近似</em>类型，
	 * 即 {@link #createApproximateMap} 能够近似创建的类型。
	 * @param mapType 要检查的映射类型
	 * @return 如果该类型是 <em>approximable</em> 则返回 {@code true}
	 */
	public static boolean isApproximableMapType(@Nullable Class<?> mapType) {
		return (mapType != null && approximableMapTypes.contains(mapType));
	}

	/**
	 * 为给定的映射创建最接近的映射。
	 * <p><strong>警告</strong>：
	 * 由于参数化类型 {@code K} 未绑定到提供的 {@code map} 中所包含键的类型，
	 * 因此如果提供的 {@code map} 是 {@link EnumMap}，则无法保证类型安全。在此类场景中，
	 * 调用者需负责确保提供的 {@code map} 中的键类型是与类型 {@code K} 匹配的枚举类型。
	 * 作为替代方案，调用者可以将返回值视为原始映射或以 {@link Object} 为键的映射。
	 * @param map 原始映射对象，可能为 {@code null}
	 * @param capacity 初始容量
	 * @return 新的空映射实例
	 * @see #isApproximableMapType
	 * @see java.util.EnumMap
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <K, V> Map<K, V> createApproximateMap(@Nullable Object map, int capacity) {
		if (map instanceof EnumMap) {
			EnumMap enumMap = new EnumMap((EnumMap) map);
			enumMap.clear();
			return enumMap;
		}
		else if (map instanceof SortedMap) {
			return new TreeMap<>(((SortedMap<K, V>) map).comparator());
		}
		else {
			return new LinkedHashMap<>(capacity);
		}
	}

	/**
	 * 为给定的映射类型创建最合适的映射。
	 * <p>以 {@code null} 键类型委托给 {@link #createMap(Class, Class, int)}。
	 * @param mapType 目标映射的期望类型
	 * @param capacity 初始容量
	 * @return 新的映射实例
	 * @throws IllegalArgumentException 如果提供的 {@code mapType} 为 {@code null} 或类型为 {@link EnumMap}
	 */
	public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
		return createMap(mapType, null, capacity);
	}

	/**
	 * 为给定的映射类型创建最合适的映射。
	 * <p><strong>警告</strong>：由于参数化类型 {@code K} 未绑定到提供的 {@code keyType}，
	 * 因此如果期望的 {@code mapType} 是 {@link EnumMap}，则无法保证类型安全。在此类场景中，
	 * 调用者需负责确保 {@code keyType} 是与类型 {@code K} 匹配的枚举类型。作为替代方案，
	 * 调用者可以将返回值视为原始映射或以 {@link Object} 为键的映射。同样，
	 * 如果期望的 {@code mapType} 是 {@link MultiValueMap}，也无法强制保证类型安全。
	 * @param mapType 目标映射的期望类型（永不为 {@code null}）
	 * @param keyType 映射的键类型，未知时为 {@code null}（注意：仅与 {@link EnumMap} 的创建相关）
	 * @param capacity 初始容量
	 * @return 新的映射实例
	 * @throws IllegalArgumentException 如果提供的 {@code mapType} 为 {@code null}；或者期望的 {@code mapType} 为 {@link EnumMap}，
	 * 且提供的 {@code keyType} 不是 {@link Enum} 的子类型
	 * @since 4.1.3
	 * @see java.util.LinkedHashMap
	 * @see java.util.TreeMap
	 * @see org.springframework.util.LinkedMultiValueMap
	 * @see java.util.EnumMap
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <K, V> Map<K, V> createMap(Class<?> mapType, @Nullable Class<?> keyType, int capacity) {
		Assert.notNull(mapType, "Map type must not be null");
		if (mapType.isInterface()) {
			if (Map.class == mapType) {
				return new LinkedHashMap<>(capacity);
			}
			else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
				return new TreeMap<>();
			}
			else if (MultiValueMap.class == mapType) {
				return new LinkedMultiValueMap();
			}
			else {
				throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
			}
		}
		else if (EnumMap.class == mapType) {
			Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
			return new EnumMap(asEnumType(keyType));
		}
		else {
			if (!Map.class.isAssignableFrom(mapType)) {
				throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
			}
			try {
				return (Map<K, V>) ReflectionUtils.accessibleConstructor(mapType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
			}
		}
	}

	/**
	 * 创建 {@link java.util.Properties} 的一个变体，
	 * 它会在 {@link Properties#getProperty} 中自动将非 String 值适配为 String 表示形式。
	 * <p>此外，返回的 {@code Properties} 实例会根据键按字母数字顺序对属性排序。
	 * @return 新的 {@code Properties} 实例
	 * @since 4.3.4
	 * @see #createSortedProperties(boolean)
	 * @see #createSortedProperties(Properties, boolean)
	 */
	@SuppressWarnings("serial")
	public static Properties createStringAdaptingProperties() {
		return new SortedProperties(false) {
			@Override
			@Nullable
			public String getProperty(String key) {
				Object value = get(key);
				return (value != null ? value.toString() : null);
			}
		};
	}

	/**
	 * 创建 {@link java.util.Properties} 的一个变体，它会根据键按字母数字顺序对属性排序。
	 * <p>当把 {@link Properties} 实例存储到 properties 文件时，这可能很有用，
	 * 因为它允许以可重复的方式生成此类文件，并保持属性顺序一致。
	 * 生成的 properties 文件中的注释也可以选择省略。
	 * @param omitComments 如果将属性存储到文件时应省略注释则返回 {@code true}
	 * @return 新的 {@code Properties} 实例
	 * @since 5.2
	 * @see #createStringAdaptingProperties()
	 * @see #createSortedProperties(Properties, boolean)
	 */
	public static Properties createSortedProperties(boolean omitComments) {
		return new SortedProperties(omitComments);
	}

	/**
	 * 创建 {@link java.util.Properties} 的一个变体，它会根据键按字母数字顺序对属性排序。
	 * <p>当把 {@code Properties} 实例存储到 properties 文件时，这可能很有用，
	 * 因为它允许以可重复的方式生成此类文件，并保持属性顺序一致。
	 * 生成的 properties 文件中的注释也可以选择省略。
	 * <p>返回的 {@code Properties} 实例将使用提供的 {@code properties} 对象中的属性填充，
	 * 但提供的 {@code properties} 对象中的默认属性不会被复制。
	 * @param properties 要从中复制初始属性的 {@code Properties} 对象
	 * @param omitComments 如果将属性存储到文件时应省略注释则返回 {@code true}
	 * @return 新的 {@code Properties} 实例
	 * @since 5.2
	 * @see #createStringAdaptingProperties()
	 * @see #createSortedProperties(boolean)
	 */
	public static Properties createSortedProperties(Properties properties, boolean omitComments) {
		return new SortedProperties(properties, omitComments);
	}

	/**
	 * 将给定类型转换为 {@link Enum} 的子类型。
	 * @param enumType 枚举类型，永不为 {@code null}
	 * @return 作为 {@link Enum} 子类型的给定类型
	 * @throws IllegalArgumentException 如果给定类型不是 {@link Enum} 的子类型
	 */
	@SuppressWarnings("rawtypes")
	private static Class<? extends Enum> asEnumType(Class<?> enumType) {
		Assert.notNull(enumType, "Enum type must not be null");
		if (!Enum.class.isAssignableFrom(enumType)) {
			throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
		}
		return enumType.asSubclass(Enum.class);
	}

}
