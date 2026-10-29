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

package org.springframework.core.env;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * {@link PropertySources} 接口的默认实现。
 * 允许操作所包含的属性源，并提供一个用于复制现有 {@code PropertySources} 实例的构造器。
 *
 * <p>在 {@link #addFirst} 和 {@link #addLast} 等方法中提到<em>优先级</em>时，
 * 指的是使用 {@link PropertyResolver} 解析给定属性时搜索属性源的顺序。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySourcesPropertyResolver
 */
public class MutablePropertySources implements PropertySources {

	private final List<PropertySource<?>> propertySourceList = new CopyOnWriteArrayList<>();


	/**
	 * 创建一个新的 {@link MutablePropertySources} 对象。
	 */
	public MutablePropertySources() {
	}

	/**
	 * 根据给定的 propertySources 对象创建一个新的 {@code MutablePropertySources}，
	 * 并保留所包含 {@code PropertySource} 对象的原始顺序。
	 */
	public MutablePropertySources(PropertySources propertySources) {
		this();
		for (PropertySource<?> propertySource : propertySources) {
			addLast(propertySource);
		}
	}


	@Override
	public Iterator<PropertySource<?>> iterator() {
		return this.propertySourceList.iterator();
	}

	@Override
	public Spliterator<PropertySource<?>> spliterator() {
		return Spliterators.spliterator(this.propertySourceList, 0);
	}

	@Override
	public Stream<PropertySource<?>> stream() {
		return this.propertySourceList.stream();
	}

	@Override
	public boolean contains(String name) {
		for (PropertySource<?> propertySource : this.propertySourceList) {
			if (propertySource.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Nullable
	public PropertySource<?> get(String name) {
		for (PropertySource<?> propertySource : this.propertySourceList) {
			if (propertySource.getName().equals(name)) {
				return propertySource;
			}
		}
		return null;
	}


	/**
	 * 以最高优先级添加给定的属性源对象。
	 */
	public void addFirst(PropertySource<?> propertySource) {
		synchronized (this.propertySourceList) {
			removeIfPresent(propertySource);
			this.propertySourceList.add(0, propertySource);
		}
	}

	/**
	 * 以最低优先级添加给定的属性源对象。
	 */
	public void addLast(PropertySource<?> propertySource) {
		synchronized (this.propertySourceList) {
			removeIfPresent(propertySource);
			this.propertySourceList.add(propertySource);
		}
	}

	/**
	 * 以比指定的相对属性源略高的优先级添加给定的属性源对象。
	 */
	public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		synchronized (this.propertySourceList) {
			removeIfPresent(propertySource);
			int index = assertPresentAndGetIndex(relativePropertySourceName);
			addAtIndex(index, propertySource);
		}
	}

	/**
	 * 以比指定的相对属性源略低的优先级添加给定的属性源对象。
	 */
	public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		synchronized (this.propertySourceList) {
			removeIfPresent(propertySource);
			int index = assertPresentAndGetIndex(relativePropertySourceName);
			addAtIndex(index + 1, propertySource);
		}
	}

	/**
	 * 返回给定属性源的优先级，如果未找到则返回 {@code -1}。
	 */
	public int precedenceOf(PropertySource<?> propertySource) {
		return this.propertySourceList.indexOf(propertySource);
	}

	/**
	 * 移除并返回具有给定名称的属性源，如果未找到则返回 {@code null}。
	 * @param name the name of the property source to find and remove
	 */
	@Nullable
	public PropertySource<?> remove(String name) {
		synchronized (this.propertySourceList) {
			int index = this.propertySourceList.indexOf(PropertySource.named(name));
			return (index != -1 ? this.propertySourceList.remove(index) : null);
		}
	}

	/**
	 * 用给定的属性源对象替换具有给定名称的属性源。
	 * @param name the name of the property source to find and replace
	 * @param propertySource the replacement property source
	 * @throws IllegalArgumentException if no property source with the given name is present
	 * @see #contains
	 */
	public void replace(String name, PropertySource<?> propertySource) {
		synchronized (this.propertySourceList) {
			int index = assertPresentAndGetIndex(name);
			this.propertySourceList.set(index, propertySource);
		}
	}

	/**
	 * 返回所包含的 {@link PropertySource} 对象数量。
	 */
	public int size() {
		return this.propertySourceList.size();
	}

	@Override
	public String toString() {
		return this.propertySourceList.toString();
	}


	/**
	 * 确保给定的属性源不是相对于其自身添加的。
	 */
	protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
		String newPropertySourceName = propertySource.getName();
		if (relativePropertySourceName.equals(newPropertySourceName)) {
			throw new IllegalArgumentException(
					"PropertySource named '" + newPropertySourceName + "' cannot be added relative to itself");
		}
	}

	/**
	 * 移除给定的属性源（如果存在）。
	 */
	protected void removeIfPresent(PropertySource<?> propertySource) {
		this.propertySourceList.remove(propertySource);
	}

	/**
	 * 将给定的属性源添加到列表中的特定索引位置。
	 */
	private void addAtIndex(int index, PropertySource<?> propertySource) {
		removeIfPresent(propertySource);
		this.propertySourceList.add(index, propertySource);
	}

	/**
	 * 断言指定的属性源存在并返回其索引。
	 * @param name {@linkplain PropertySource#getName() name of the property source} to find
	 * @throws IllegalArgumentException if the named property source is not present
	 */
	private int assertPresentAndGetIndex(String name) {
		int index = this.propertySourceList.indexOf(PropertySource.named(name));
		if (index == -1) {
			throw new IllegalArgumentException("PropertySource named '" + name + "' does not exist");
		}
		return index;
	}

}
