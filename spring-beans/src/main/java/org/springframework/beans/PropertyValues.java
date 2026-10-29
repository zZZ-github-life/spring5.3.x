/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 * 包含一个或多个 {@link PropertyValue} 对象的持有者，通常表示对某个特定目标 Bean 的一次更新。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13 May 2001
 * @see PropertyValue
 */
public interface PropertyValues extends Iterable<PropertyValue> {

	/**
	 * 返回遍历属性值的 {@link Iterator}。
	 * @since 5.1
	 */
	@Override
	default Iterator<PropertyValue> iterator() {
		return Arrays.asList(getPropertyValues()).iterator();
	}

	/**
	 * 返回遍历属性值的 {@link Spliterator}。
	 * @since 5.1
	 */
	@Override
	default Spliterator<PropertyValue> spliterator() {
		return Spliterators.spliterator(getPropertyValues(), 0);
	}

	/**
	 * 返回包含属性值的顺序 {@link Stream}。
	 * @since 5.1
	 */
	default Stream<PropertyValue> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * 返回此对象中所持有 PropertyValue 对象的数组。
	 */
	PropertyValue[] getPropertyValues();

	/**
	 * 返回具有给定名称的属性值（如果有）。
	 * @param propertyName the name to search for
	 * @return the property value, or {@code null} if none
	 */
	@Nullable
	PropertyValue getPropertyValue(String propertyName);

	/**
	 * 返回相对于前一个 PropertyValues 的变更。子类还应重写 {@code equals}。
	 * @param old the old property values
	 * @return the updated or new properties.
	 * Return empty PropertyValues if there are no changes.
	 * @see Object#equals
	 */
	PropertyValues changesSince(PropertyValues old);

	/**
	 * 此属性是否存在属性值（或其他处理项）？
	 * @param propertyName the name of the property we're interested in
	 * @return whether there is a property value for this property
	 */
	boolean contains(String propertyName);

	/**
	 * 此持有者是否完全不包含任何 PropertyValue 对象？
	 */
	boolean isEmpty();

}
