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

package org.springframework.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link PropertyValues} 接口的默认实现。允许对属性进行简单的操作，并提供支持深拷贝和从 Map 构造的
 * 构造函数。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 13 May 2001
 */
@SuppressWarnings("serial")
public class MutablePropertyValues implements PropertyValues, Serializable {

	private final List<PropertyValue> propertyValueList;

	@Nullable
	private Set<String> processedProperties;

	private volatile boolean converted;


	/**
	 * 创建一个新的空 MutablePropertyValues 对象。
	 * <p>可以使用 {@code add} 方法添加属性值。
	 * @see #add(String, Object)
	 */
	public MutablePropertyValues() {
		this.propertyValueList = new ArrayList<>(0);
	}

	/**
	 * 深拷贝构造函数。保证 PropertyValue 引用相互独立，但不能深拷贝各个 PropertyValue 对象当前引用的对
	 * 象。
	 * @param original 要复制的 PropertyValues
	 * @see #addPropertyValues(PropertyValues)
	 */
	public MutablePropertyValues(@Nullable PropertyValues original) {
		// We can optimize this because it's all new:
		// There is no replacement of existing property values.
		if (original != null) {
			PropertyValue[] pvs = original.getPropertyValues();
			this.propertyValueList = new ArrayList<>(pvs.length);
			for (PropertyValue pv : pvs) {
				this.propertyValueList.add(new PropertyValue(pv));
			}
		}
		else {
			this.propertyValueList = new ArrayList<>(0);
		}
	}

	/**
	 * 从 Map 构造一个新的 MutablePropertyValues 对象。
	 * @param original 以属性名字符串为键的属性值 Map
	 * @see #addPropertyValues(Map)
	 */
	public MutablePropertyValues(@Nullable Map<?, ?> original) {
		// We can optimize this because it's all new:
		// There is no replacement of existing property values.
		if (original != null) {
			this.propertyValueList = new ArrayList<>(original.size());
			original.forEach((attrName, attrValue) -> this.propertyValueList.add(
					new PropertyValue(attrName.toString(), attrValue)));
		}
		else {
			this.propertyValueList = new ArrayList<>(0);
		}
	}

	/**
	 * 使用给定的 PropertyValue 对象 List 按原样构造一个新的 MutablePropertyValues 对象。
	 * <p>此构造函数适用于高级使用场景，不适用于典型的编程用途。
	 * @param propertyValueList PropertyValue 对象的 List
	 */
	public MutablePropertyValues(@Nullable List<PropertyValue> propertyValueList) {
		this.propertyValueList =
				(propertyValueList != null ? propertyValueList : new ArrayList<>());
	}


	/**
	 * 以原始形式返回底层的 PropertyValue 对象 List。可以直接修改返回的 List，但不推荐这样做。
	 * <p>这是一个用于优化访问所有 PropertyValue 对象的访问器，不适用于典型的编程用途。
	 */
	public List<PropertyValue> getPropertyValueList() {
		return this.propertyValueList;
	}

	/**
	 * 返回列表中 PropertyValue 条目的数量。
	 */
	public int size() {
		return this.propertyValueList.size();
	}

	/**
	 * 将所有给定的 PropertyValues 复制到此对象中。保证 PropertyValue 引用相互独立，但不能深拷贝各个
	 * PropertyValue 对象当前引用的对象。
	 * @param other 要复制的 PropertyValues
	 * @return this，以便以链式方式添加多个属性值
	 */
	public MutablePropertyValues addPropertyValues(@Nullable PropertyValues other) {
		if (other != null) {
			PropertyValue[] pvs = other.getPropertyValues();
			for (PropertyValue pv : pvs) {
				addPropertyValue(new PropertyValue(pv));
			}
		}
		return this;
	}

	/**
	 * 添加给定 Map 中的所有属性值。
	 * @param other 以属性名为键的属性值 Map，键必须是 String
	 * @return this，以便以链式方式添加多个属性值
	 */
	public MutablePropertyValues addPropertyValues(@Nullable Map<?, ?> other) {
		if (other != null) {
			other.forEach((attrName, attrValue) -> addPropertyValue(
					new PropertyValue(attrName.toString(), attrValue)));
		}
		return this;
	}

	/**
	 * 添加一个 PropertyValue 对象，替换对应属性的任何现有对象，或与之合并（如果适用）。
	 * @param pv 要添加的 PropertyValue 对象
	 * @return this，以便以链式方式添加多个属性值
	 */
	public MutablePropertyValues addPropertyValue(PropertyValue pv) {
		for (int i = 0; i < this.propertyValueList.size(); i++) {
			PropertyValue currentPv = this.propertyValueList.get(i);
			if (currentPv.getName().equals(pv.getName())) {
				pv = mergeIfRequired(pv, currentPv);
				setPropertyValueAt(pv, i);
				return this;
			}
		}
		this.propertyValueList.add(pv);
		return this;
	}

	/**
	 * {@code addPropertyValue} 的重载版本，接受属性名和属性值。
	 * <p>注意：从 Spring 3.0 开始，建议使用更简洁且支持链式调用的变体 {@link #add}。
	 * @param propertyName 属性的名称
	 * @param propertyValue 属性的值
	 * @see #addPropertyValue(PropertyValue)
	 */
	public void addPropertyValue(String propertyName, Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
	}

	/**
	 * 添加一个 PropertyValue 对象，替换对应属性的任何现有对象，或与之合并（如果适用）。
	 * @param propertyName 属性的名称
	 * @param propertyValue 属性的值
	 * @return this，以便以链式方式添加多个属性值
	 */
	public MutablePropertyValues add(String propertyName, @Nullable Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
		return this;
	}

	/**
	 * 修改此对象中持有的 PropertyValue 对象。索引从 0 开始。
	 */
	public void setPropertyValueAt(PropertyValue pv, int i) {
		this.propertyValueList.set(i, pv);
	}

	/**
	 * 如果支持并启用了合并，则将所提供的“新”{@link PropertyValue} 的值与当前 {@link PropertyValue} 的
	 * 值合并。
	 * @see Mergeable
	 */
	private PropertyValue mergeIfRequired(PropertyValue newPv, PropertyValue currentPv) {
		Object value = newPv.getValue();
		if (value instanceof Mergeable) {
			Mergeable mergeable = (Mergeable) value;
			if (mergeable.isMergeEnabled()) {
				Object merged = mergeable.merge(currentPv.getValue());
				return new PropertyValue(newPv.getName(), merged);
			}
		}
		return newPv;
	}

	/**
	 * 如果包含给定的 PropertyValue，则将其移除。
	 * @param pv 要移除的 PropertyValue
	 */
	public void removePropertyValue(PropertyValue pv) {
		this.propertyValueList.remove(pv);
	}

	/**
	 * {@code removePropertyValue} 的重载版本，接受属性名。
	 * @param propertyName 属性的名称
	 * @see #removePropertyValue(PropertyValue)
	 */
	public void removePropertyValue(String propertyName) {
		this.propertyValueList.remove(getPropertyValue(propertyName));
	}


	@Override
	public Iterator<PropertyValue> iterator() {
		return Collections.unmodifiableList(this.propertyValueList).iterator();
	}

	@Override
	public Spliterator<PropertyValue> spliterator() {
		return Spliterators.spliterator(this.propertyValueList, 0);
	}

	@Override
	public Stream<PropertyValue> stream() {
		return this.propertyValueList.stream();
	}

	@Override
	public PropertyValue[] getPropertyValues() {
		return this.propertyValueList.toArray(new PropertyValue[0]);
	}

	@Override
	@Nullable
	public PropertyValue getPropertyValue(String propertyName) {
		for (PropertyValue pv : this.propertyValueList) {
			if (pv.getName().equals(propertyName)) {
				return pv;
			}
		}
		return null;
	}

	/**
	 * 获取原始属性值（如果有）。
	 * @param propertyName 要搜索的名称
	 * @return 原始属性值，如果未找到则为 {@code null}
	 * @since 4.0
	 * @see #getPropertyValue(String)
	 * @see PropertyValue#getValue()
	 */
	@Nullable
	public Object get(String propertyName) {
		PropertyValue pv = getPropertyValue(propertyName);
		return (pv != null ? pv.getValue() : null);
	}

	@Override
	public PropertyValues changesSince(PropertyValues old) {
		MutablePropertyValues changes = new MutablePropertyValues();
		if (old == this) {
			return changes;
		}

		// for each property value in the new set
		for (PropertyValue newPv : this.propertyValueList) {
			// if there wasn't an old one, add it
			PropertyValue pvOld = old.getPropertyValue(newPv.getName());
			if (pvOld == null || !pvOld.equals(newPv)) {
				changes.addPropertyValue(newPv);
			}
		}
		return changes;
	}

	@Override
	public boolean contains(String propertyName) {
		return (getPropertyValue(propertyName) != null ||
				(this.processedProperties != null && this.processedProperties.contains(propertyName)));
	}

	@Override
	public boolean isEmpty() {
		return this.propertyValueList.isEmpty();
	}


	/**
	 * 将指定属性注册为“已处理”，即某个处理器在 PropertyValue(s) 机制之外调用了对应的 setter 方法。
	 * <p>这将导致对指定属性调用 {@link #contains} 时返回 {@code true}。
	 * @param propertyName 属性的名称。
	 */
	public void registerProcessedProperty(String propertyName) {
		if (this.processedProperties == null) {
			this.processedProperties = new HashSet<>(4);
		}
		this.processedProperties.add(propertyName);
	}

	/**
	 * 清除给定属性的“已处理”注册（如果有）。
	 * @since 3.2.13
	 */
	public void clearProcessedProperty(String propertyName) {
		if (this.processedProperties != null) {
			this.processedProperties.remove(propertyName);
		}
	}

	/**
	 * 将此持有者标记为仅包含已转换的值（即不再需要运行时解析）。
	 */
	public void setConverted() {
		this.converted = true;
	}

	/**
	 * 返回此持有者是仅包含已转换的值（{@code true}），还是这些值仍需要转换（{@code false}）。
	 */
	public boolean isConverted() {
		return this.converted;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MutablePropertyValues &&
				this.propertyValueList.equals(((MutablePropertyValues) other).propertyValueList)));
	}

	@Override
	public int hashCode() {
		return this.propertyValueList.hashCode();
	}

	@Override
	public String toString() {
		PropertyValue[] pvs = getPropertyValues();
		if (pvs.length > 0) {
			return "PropertyValues: length=" + pvs.length + "; " + StringUtils.arrayToDelimitedString(pvs, "; ");
		}
		return "PropertyValues: length=0";
	}

}
