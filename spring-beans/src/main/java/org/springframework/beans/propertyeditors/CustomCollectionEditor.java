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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * 用于 Collection 的属性编辑器，可将任意源 Collection 转换为给定的目标 Collection 类型。
 *
 * <p>默认注册用于 Set、SortedSet 和 List，当类型与目标属性不匹配时，自动将任意给定 Collection 转换
 * 为这些目标类型之一。
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see java.util.Collection
 * @see java.util.Set
 * @see java.util.SortedSet
 * @see java.util.List
 */
public class CustomCollectionEditor extends PropertyEditorSupport {

	@SuppressWarnings("rawtypes")
	private final Class<? extends Collection> collectionType;

	private final boolean nullAsEmptyCollection;


	/**
	 * 为给定目标类型创建新的 CustomCollectionEditor，传入的 {@code null} 保持原样。
	 * @param collectionType the target type, which needs to be a
	 * sub-interface of Collection or a concrete Collection class
	 * @see java.util.Collection
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings("rawtypes")
	public CustomCollectionEditor(Class<? extends Collection> collectionType) {
		this(collectionType, false);
	}

	/**
	 * 为给定目标类型创建新的 CustomCollectionEditor。<p>如果传入值的类型就是给定类型，则按原样使用。如
	 * 果它是其他 Collection 类型或数组，则将其转换为给定 Collection 类型的默认实现。如果值属于其他任何
	 * 类型，则将创建一个仅包含该值的目标 Collection。<p>默认的 Collection 实现为：List 使用 ArrayList，
	 * SortedSet 使用 TreeSet，Set 使用 LinkedHashSet。
	 * @param collectionType the target type, which needs to be a
	 * sub-interface of Collection or a concrete Collection class
	 * @param nullAsEmptyCollection whether to convert an incoming {@code null}
	 * value to an empty Collection (of the appropriate type)
	 * @see java.util.Collection
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings("rawtypes")
	public CustomCollectionEditor(Class<? extends Collection> collectionType, boolean nullAsEmptyCollection) {
		Assert.notNull(collectionType, "Collection type is required");
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException(
					"Collection type [" + collectionType.getName() + "] does not implement [java.util.Collection]");
		}
		this.collectionType = collectionType;
		this.nullAsEmptyCollection = nullAsEmptyCollection;
	}


	/**
	 * 将给定的文本值转换为仅含一个元素的 Collection。
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	/**
	 * 将给定值转换为目标类型的 Collection。
	 */
	@Override
	public void setValue(@Nullable Object value) {
		if (value == null && this.nullAsEmptyCollection) {
			super.setValue(createCollection(this.collectionType, 0));
		}
		else if (value == null || (this.collectionType.isInstance(value) && !alwaysCreateNewCollection())) {
			// Use the source value as-is, as it matches the target type.
			super.setValue(value);
		}
		else if (value instanceof Collection) {
			// Convert Collection elements.
			Collection<?> source = (Collection<?>) value;
			Collection<Object> target = createCollection(this.collectionType, source.size());
			for (Object elem : source) {
				target.add(convertElement(elem));
			}
			super.setValue(target);
		}
		else if (value.getClass().isArray()) {
			// Convert array elements to Collection elements.
			int length = Array.getLength(value);
			Collection<Object> target = createCollection(this.collectionType, length);
			for (int i = 0; i < length; i++) {
				target.add(convertElement(Array.get(value, i)));
			}
			super.setValue(target);
		}
		else {
			// A plain value: convert it to a Collection with a single element.
			Collection<Object> target = createCollection(this.collectionType, 1);
			target.add(convertElement(value));
			super.setValue(target);
		}
	}

	/**
	 * 创建给定类型的 Collection，并使用给定的初始容量（如果该 Collection 类型支持）。
	 * @param collectionType a sub-interface of Collection
	 * @param initialCapacity the initial capacity
	 * @return the new Collection instance
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected Collection<Object> createCollection(Class<? extends Collection> collectionType, int initialCapacity) {
		if (!collectionType.isInterface()) {
			try {
				return ReflectionUtils.accessibleConstructor(collectionType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate collection class: " + collectionType.getName(), ex);
			}
		}
		else if (List.class == collectionType) {
			return new ArrayList<>(initialCapacity);
		}
		else if (SortedSet.class == collectionType) {
			return new TreeSet<>();
		}
		else {
			return new LinkedHashSet<>(initialCapacity);
		}
	}

	/**
	 * 返回是否始终创建新的 Collection，即使传入 Collection 的类型已经匹配。<p>默认为 "false"；可以覆盖
	 * 此方法以强制创建新的 Collection，例如在任何情况下都转换元素。
	 * @see #convertElement
	 */
	protected boolean alwaysCreateNewCollection() {
		return false;
	}

	/**
	 * 用于转换遇到的每个 Collection/数组元素的钩子。默认实现只是按原样返回传入的元素。<p>可以覆盖以对
	 * 某些元素执行转换，例如当传入 String 数组并且应转换为 Integer 对象的 Set 时，将 String 转换为
	 * Integer。<p>仅在确实创建新 Collection 时才调用！如果传入 Collection 的类型已经匹配，默认情况下不
	 * 会创建新 Collection。覆盖 {@link #alwaysCreateNewCollection()} 以强制在每种情况下都创建新的
	 * Collection。
	 * @param element the source element
	 * @return the element to be used in the target Collection
	 * @see #alwaysCreateNewCollection()
	 */
	protected Object convertElement(Object element) {
		return element;
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
