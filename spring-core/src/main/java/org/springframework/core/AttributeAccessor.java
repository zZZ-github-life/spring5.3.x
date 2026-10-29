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

package org.springframework.core;

import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 该接口定义了向任意对象附加元数据以及从中访问元数据的通用契约。
 *
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
public interface AttributeAccessor {

	/**
	 * 将 {@code name} 定义的属性设置为提供的 {@code value}。
	 * <p>如果 {@code value} 为 {@code null}，则该属性会被 {@link #removeAttribute removed}。
	 * <p>通常，用户应注意通过使用完全限定名称来避免与其他元数据属性重叠，
	 * 例如可以使用类名或包名作为前缀。
	 * @param name the unique attribute key
	 * @param value the attribute value to be attached
	 */
	void setAttribute(String name, @Nullable Object value);

	/**
	 * 获取由 {@code name} 标识的属性的值。
	 * <p>如果该属性不存在，则返回 {@code null}。
	 * @param name the unique attribute key
	 * @return the current value of the attribute, if any
	 */
	@Nullable
	Object getAttribute(String name);

	/**
	 * 如有必要，为 {@code name} 标识的属性计算新值，
	 * 并将新值 {@linkplain #setAttribute set} 到此 {@code AttributeAccessor} 中。
	 * <p>如果此 {@code AttributeAccessor} 中已存在由 {@code name} 标识的属性的值，
	 * 则直接返回该现有值，而不应用提供的计算函数。
	 * <p>此方法的默认实现不是线程安全的，但可以由该接口的具体实现覆盖。
	 * @param <T> the type of the attribute value
	 * @param name the unique attribute key
	 * @param computeFunction a function that computes a new value for the attribute
	 * name; the function must not return a {@code null} value
	 * @return the existing value or newly computed value for the named attribute
	 * @since 5.3.3
	 * @see #getAttribute(String)
	 * @see #setAttribute(String, Object)
	 */
	@SuppressWarnings("unchecked")
	default <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(computeFunction, "Compute function must not be null");
		Object value = getAttribute(name);
		if (value == null) {
			value = computeFunction.apply(name);
			Assert.state(value != null,
					() -> String.format("Compute function must not return null for attribute named '%s'", name));
			setAttribute(name, value);
		}
		return (T) value;
	}

	/**
	 * 移除由 {@code name} 标识的属性并返回其值。
	 * <p>如果未找到 {@code name} 对应的属性，则返回 {@code null}。
	 * @param name the unique attribute key
	 * @return the last value of the attribute, if any
	 */
	@Nullable
	Object removeAttribute(String name);

	/**
	 * 如果由 {@code name} 标识的属性存在，则返回 {@code true}。
	 * <p>否则返回 {@code false}。
	 * @param name the unique attribute key
	 */
	boolean hasAttribute(String name);

	/**
	 * 返回所有属性的名称。
	 */
	String[] attributeNames();

}
