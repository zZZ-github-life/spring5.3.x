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

package org.springframework.beans.factory.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 构造函数参数值的持有者，通常作为 Bean 定义的一部分。
 *
 * <p>支持构造函数参数列表中特定索引的值，也支持按类型进行通用参数匹配的值。
 *
 * @author Juergen Hoeller
 * @since 09.11.2003
 * @see BeanDefinition#getConstructorArgumentValues
 */
public class ConstructorArgumentValues {

	private final Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<>();

	private final List<ValueHolder> genericArgumentValues = new ArrayList<>();


	/**
	 * 创建一个新的空 ConstructorArgumentValues 对象。
	 */
	public ConstructorArgumentValues() {
	}

	/**
	 * 深拷贝构造函数。
	 * @param original the ConstructorArgumentValues to copy
	 */
	public ConstructorArgumentValues(ConstructorArgumentValues original) {
		addArgumentValues(original);
	}


	/**
	 * 将所有给定的参数值复制到此对象中，使用独立的持有者实例使这些值与原对象保持独立。
	 * <p>注意：相同的 ValueHolder 实例只会注册一次，以允许参数值定义的合并和重复合并。当然，内容相同的
	 * 不同 ValueHolder 实例是允许的。
	 */
	public void addArgumentValues(@Nullable ConstructorArgumentValues other) {
		if (other != null) {
			other.indexedArgumentValues.forEach(
				(index, argValue) -> addOrMergeIndexedArgumentValue(index, argValue.copy())
			);
			other.genericArgumentValues.stream()
					.filter(valueHolder -> !this.genericArgumentValues.contains(valueHolder))
					.forEach(valueHolder -> addOrMergeGenericArgumentValue(valueHolder.copy()));
		}
	}


	/**
	 * 为构造函数参数列表中的给定索引添加参数值。
	 * @param index the index in the constructor argument list
	 * @param value the argument value
	 */
	public void addIndexedArgumentValue(int index, @Nullable Object value) {
		addIndexedArgumentValue(index, new ValueHolder(value));
	}

	/**
	 * 为构造函数参数列表中的给定索引添加参数值。
	 * @param index the index in the constructor argument list
	 * @param value the argument value
	 * @param type the type of the constructor argument
	 */
	public void addIndexedArgumentValue(int index, @Nullable Object value, String type) {
		addIndexedArgumentValue(index, new ValueHolder(value, type));
	}

	/**
	 * 为构造函数参数列表中的给定索引添加参数值。
	 * @param index the index in the constructor argument list
	 * @param newValue the argument value in the form of a ValueHolder
	 */
	public void addIndexedArgumentValue(int index, ValueHolder newValue) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		Assert.notNull(newValue, "ValueHolder must not be null");
		addOrMergeIndexedArgumentValue(index, newValue);
	}

	/**
	 * 为构造函数参数列表中的给定索引添加参数值，如果需要，将新值（通常是集合）与当前值合并：参见
	 * {@link org.springframework.beans.Mergeable}。
	 * @param key the index in the constructor argument list
	 * @param newValue the argument value in the form of a ValueHolder
	 */
	private void addOrMergeIndexedArgumentValue(Integer key, ValueHolder newValue) {
		ValueHolder currentValue = this.indexedArgumentValues.get(key);
		if (currentValue != null && newValue.getValue() instanceof Mergeable) {
			Mergeable mergeable = (Mergeable) newValue.getValue();
			if (mergeable.isMergeEnabled()) {
				newValue.setValue(mergeable.merge(currentValue.getValue()));
			}
		}
		this.indexedArgumentValues.put(key, newValue);
	}

	/**
	 * 检查是否已为给定索引注册参数值。
	 * @param index the index in the constructor argument list
	 */
	public boolean hasIndexedArgumentValue(int index) {
		return this.indexedArgumentValues.containsKey(index);
	}

	/**
	 * 获取构造函数参数列表中给定索引的参数值。
	 * @param index the index in the constructor argument list
	 * @param requiredType the type to match (can be {@code null} to match
	 * untyped values only)
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType) {
		return getIndexedArgumentValue(index, requiredType, null);
	}

	/**
	 * 获取构造函数参数列表中给定索引的参数值。
	 * @param index the index in the constructor argument list
	 * @param requiredType the type to match (can be {@code null} to match
	 * untyped values only)
	 * @param requiredName the type to match (can be {@code null} to match
	 * unnamed values only, or empty String to match any name)
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType, @Nullable String requiredName) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		ValueHolder valueHolder = this.indexedArgumentValues.get(index);
		if (valueHolder != null &&
				(valueHolder.getType() == null || (requiredType != null &&
						ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) &&
				(valueHolder.getName() == null || (requiredName != null &&
						(requiredName.isEmpty() || requiredName.equals(valueHolder.getName()))))) {
			return valueHolder;
		}
		return null;
	}

	/**
	 * 返回带索引参数值的映射。
	 * @return unmodifiable Map with Integer index as key and ValueHolder as value
	 * @see ValueHolder
	 */
	public Map<Integer, ValueHolder> getIndexedArgumentValues() {
		return Collections.unmodifiableMap(this.indexedArgumentValues);
	}


	/**
	 * 添加一个按类型匹配的通用参数值。
	 * <p>注意：单个通用参数值只会被使用一次，而不会被多次匹配。
	 * @param value the argument value
	 */
	public void addGenericArgumentValue(Object value) {
		this.genericArgumentValues.add(new ValueHolder(value));
	}

	/**
	 * 添加一个按类型匹配的通用参数值。
	 * <p>注意：单个通用参数值只会被使用一次，而不会被多次匹配。
	 * @param value the argument value
	 * @param type the type of the constructor argument
	 */
	public void addGenericArgumentValue(Object value, String type) {
		this.genericArgumentValues.add(new ValueHolder(value, type));
	}

	/**
	 * 添加一个按类型或名称（如果可用）匹配的通用参数值。
	 * <p>注意：单个通用参数值只会被使用一次，而不会被多次匹配。
	 * @param newValue the argument value in the form of a ValueHolder
	 * <p>Note: Identical ValueHolder instances will only be registered once,
	 * to allow for merging and re-merging of argument value definitions. Distinct
	 * ValueHolder instances carrying the same content are of course allowed.
	 */
	public void addGenericArgumentValue(ValueHolder newValue) {
		Assert.notNull(newValue, "ValueHolder must not be null");
		if (!this.genericArgumentValues.contains(newValue)) {
			addOrMergeGenericArgumentValue(newValue);
		}
	}

	/**
	 * 添加一个通用参数值，如果需要，将新值（通常是集合）与当前值合并：参见
	 * {@link org.springframework.beans.Mergeable}。
	 * @param newValue the argument value in the form of a ValueHolder
	 */
	private void addOrMergeGenericArgumentValue(ValueHolder newValue) {
		if (newValue.getName() != null) {
			for (Iterator<ValueHolder> it = this.genericArgumentValues.iterator(); it.hasNext();) {
				ValueHolder currentValue = it.next();
				if (newValue.getName().equals(currentValue.getName())) {
					if (newValue.getValue() instanceof Mergeable) {
						Mergeable mergeable = (Mergeable) newValue.getValue();
						if (mergeable.isMergeEnabled()) {
							newValue.setValue(mergeable.merge(currentValue.getValue()));
						}
					}
					it.remove();
				}
			}
		}
		this.genericArgumentValues.add(newValue);
	}

	/**
	 * 查找与给定类型匹配的通用参数值。
	 * @param requiredType the type to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getGenericArgumentValue(Class<?> requiredType) {
		return getGenericArgumentValue(requiredType, null, null);
	}

	/**
	 * 查找与给定类型匹配的通用参数值。
	 * @param requiredType the type to match
	 * @param requiredName the name to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getGenericArgumentValue(Class<?> requiredType, String requiredName) {
		return getGenericArgumentValue(requiredType, requiredName, null);
	}

	/**
	 * 查找与给定类型匹配的下一个通用参数值，忽略在当前解析过程中已经使用过的参数值。
	 * @param requiredType the type to match (can be {@code null} to find
	 * an arbitrary next generic argument value)
	 * @param requiredName the name to match (can be {@code null} to not
	 * match argument values by name, or empty String to match any name)
	 * @param usedValueHolders a Set of ValueHolder objects that have already been used
	 * in the current resolution process and should therefore not be returned again
	 * @return the ValueHolder for the argument, or {@code null} if none found
	 */
	@Nullable
	public ValueHolder getGenericArgumentValue(@Nullable Class<?> requiredType, @Nullable String requiredName,
			@Nullable Set<ValueHolder> usedValueHolders) {

		for (ValueHolder valueHolder : this.genericArgumentValues) {
			if (usedValueHolders != null && usedValueHolders.contains(valueHolder)) {
				continue;
			}
			if (valueHolder.getName() != null && (requiredName == null ||
					(!requiredName.isEmpty() && !requiredName.equals(valueHolder.getName())))) {
				continue;
			}
			if (valueHolder.getType() != null && (requiredType == null ||
					!ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) {
				continue;
			}
			if (requiredType != null && valueHolder.getType() == null && valueHolder.getName() == null &&
					!ClassUtils.isAssignableValue(requiredType, valueHolder.getValue())) {
				continue;
			}
			return valueHolder;
		}
		return null;
	}

	/**
	 * 返回通用参数值的列表。
	 * @return unmodifiable List of ValueHolders
	 * @see ValueHolder
	 */
	public List<ValueHolder> getGenericArgumentValues() {
		return Collections.unmodifiableList(this.genericArgumentValues);
	}


	/**
	 * 查找与构造函数参数列表中给定索引对应、或按类型通用匹配的参数值。
	 * @param index the index in the constructor argument list
	 * @param requiredType the parameter type to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getArgumentValue(int index, Class<?> requiredType) {
		return getArgumentValue(index, requiredType, null, null);
	}

	/**
	 * 查找与构造函数参数列表中给定索引对应、或按类型通用匹配的参数值。
	 * @param index the index in the constructor argument list
	 * @param requiredType the parameter type to match
	 * @param requiredName the parameter name to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getArgumentValue(int index, Class<?> requiredType, String requiredName) {
		return getArgumentValue(index, requiredType, requiredName, null);
	}

	/**
	 * 查找与构造函数参数列表中给定索引对应、或按类型通用匹配的参数值。
	 * @param index the index in the constructor argument list
	 * @param requiredType the parameter type to match (can be {@code null}
	 * to find an untyped argument value)
	 * @param requiredName the parameter name to match (can be {@code null}
	 * to find an unnamed argument value, or empty String to match any name)
	 * @param usedValueHolders a Set of ValueHolder objects that have already
	 * been used in the current resolution process and should therefore not
	 * be returned again (allowing to return the next generic argument match
	 * in case of multiple generic argument values of the same type)
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	@Nullable
	public ValueHolder getArgumentValue(int index, @Nullable Class<?> requiredType,
			@Nullable String requiredName, @Nullable Set<ValueHolder> usedValueHolders) {

		Assert.isTrue(index >= 0, "Index must not be negative");
		ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);
		if (valueHolder == null) {
			valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
		}
		return valueHolder;
	}

	/**
	 * 返回此实例中持有的参数值数量，同时计入带索引的参数值和通用参数值。
	 */
	public int getArgumentCount() {
		return (this.indexedArgumentValues.size() + this.genericArgumentValues.size());
	}

	/**
	 * 返回此持有者是否不包含任何参数值，既没有带索引的，也没有通用的。
	 */
	public boolean isEmpty() {
		return (this.indexedArgumentValues.isEmpty() && this.genericArgumentValues.isEmpty());
	}

	/**
	 * 清空此持有者，移除所有参数值。
	 */
	public void clear() {
		this.indexedArgumentValues.clear();
		this.genericArgumentValues.clear();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ConstructorArgumentValues)) {
			return false;
		}
		ConstructorArgumentValues that = (ConstructorArgumentValues) other;
		if (this.genericArgumentValues.size() != that.genericArgumentValues.size() ||
				this.indexedArgumentValues.size() != that.indexedArgumentValues.size()) {
			return false;
		}
		Iterator<ValueHolder> it1 = this.genericArgumentValues.iterator();
		Iterator<ValueHolder> it2 = that.genericArgumentValues.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			ValueHolder vh1 = it1.next();
			ValueHolder vh2 = it2.next();
			if (!vh1.contentEquals(vh2)) {
				return false;
			}
		}
		for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
			ValueHolder vh1 = entry.getValue();
			ValueHolder vh2 = that.indexedArgumentValues.get(entry.getKey());
			if (vh2 == null || !vh1.contentEquals(vh2)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 7;
		for (ValueHolder valueHolder : this.genericArgumentValues) {
			hashCode = 31 * hashCode + valueHolder.contentHashCode();
		}
		hashCode = 29 * hashCode;
		for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
			hashCode = 31 * hashCode + (entry.getValue().contentHashCode() ^ entry.getKey().hashCode());
		}
		return hashCode;
	}


	/**
	 * 构造函数参数值的持有者，带有可选的类型属性，用于指示实际构造函数参数的目标类型。
	 */
	public static class ValueHolder implements BeanMetadataElement {

		@Nullable
		private Object value;

		@Nullable
		private String type;

		@Nullable
		private String name;

		@Nullable
		private Object source;

		private boolean converted = false;

		@Nullable
		private Object convertedValue;

		/**
		 * 为给定值创建一个新的 ValueHolder。
		 * @param value the argument value
		 */
		public ValueHolder(@Nullable Object value) {
			this.value = value;
		}

		/**
		 * 为给定值和类型创建一个新的 ValueHolder。
		 * @param value the argument value
		 * @param type the type of the constructor argument
		 */
		public ValueHolder(@Nullable Object value, @Nullable String type) {
			this.value = value;
			this.type = type;
		}

		/**
		 * 为给定值、类型和名称创建一个新的 ValueHolder。
		 * @param value the argument value
		 * @param type the type of the constructor argument
		 * @param name the name of the constructor argument
		 */
		public ValueHolder(@Nullable Object value, @Nullable String type, @Nullable String name) {
			this.value = value;
			this.type = type;
			this.name = name;
		}

		/**
		 * 设置构造函数参数的值。
		 */
		public void setValue(@Nullable Object value) {
			this.value = value;
		}

		/**
		 * 返回构造函数参数的值。
		 */
		@Nullable
		public Object getValue() {
			return this.value;
		}

		/**
		 * 设置构造函数参数的类型。
		 */
		public void setType(@Nullable String type) {
			this.type = type;
		}

		/**
		 * 返回构造函数参数的类型。
		 */
		@Nullable
		public String getType() {
			return this.type;
		}

		/**
		 * 设置构造函数参数的名称。
		 */
		public void setName(@Nullable String name) {
			this.name = name;
		}

		/**
		 * 返回构造函数参数的名称。
		 */
		@Nullable
		public String getName() {
			return this.name;
		}

		/**
		 * 为此元数据元素设置配置源 {@code Object}。
		 * <p>该对象的确切类型取决于所使用的配置机制。
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
		 * 返回此持有者是否已包含转换后的值（{@code true}），或者该值是否仍需要转换（{@code false}）。
		 */
		public synchronized boolean isConverted() {
			return this.converted;
		}

		/**
		 * 设置构造函数参数的转换后值，在完成类型转换之后。
		 */
		public synchronized void setConvertedValue(@Nullable Object value) {
			this.converted = (value != null);
			this.convertedValue = value;
		}

		/**
		 * 返回构造函数参数的转换后值，在完成类型转换之后。
		 */
		@Nullable
		public synchronized Object getConvertedValue() {
			return this.convertedValue;
		}

		/**
		 * 确定此 ValueHolder 的内容是否与给定的另一个 ValueHolder 的内容相等。
		 * <p>注意，ValueHolder 不直接实现 {@code equals}，以允许内容相同的多个 ValueHolder 实例共存于同一
		 * 个 Set 中。
		 */
		private boolean contentEquals(ValueHolder other) {
			return (this == other ||
					(ObjectUtils.nullSafeEquals(this.value, other.value) && ObjectUtils.nullSafeEquals(this.type, other.type)));
		}

		/**
		 * 确定此 ValueHolder 内容的哈希码。
		 * <p>注意，ValueHolder 不直接实现 {@code hashCode}，以允许内容相同的多个 ValueHolder 实例共存于同
		 * 一个 Set 中。
		 */
		private int contentHashCode() {
			return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.type);
		}

		/**
		 * 创建此 ValueHolder 的副本：即一个内容相同但独立的 ValueHolder 实例。
		 */
		public ValueHolder copy() {
			ValueHolder copy = new ValueHolder(this.value, this.type, this.name);
			copy.setSource(this.source);
			return copy;
		}
	}

}
