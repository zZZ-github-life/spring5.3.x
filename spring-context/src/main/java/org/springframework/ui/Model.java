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

package org.springframework.ui;

import java.util.Collection;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * 定义模型属性持有者的接口。
 *
 * <p>主要设计用于向模型添加属性。
 *
 * <p>允许以 {@code java.util.Map} 的形式访问整个模型。
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public interface Model {

	/**
	 * 在给定名称下添加提供的属性。
	 * @param attributeName the name of the model attribute (never {@code null})
	 * @param attributeValue the model attribute value (can be {@code null})
	 */
	Model addAttribute(String attributeName, @Nullable Object attributeValue);

	/**
	 * 使用 {@link org.springframework.core.Conventions#getVariableName generated name} 将提供的属性添
	 * 加到此 {@code Map}。
	 *
	 * <p><i>注意：使用此方法时，空的 {@link java.util.Collection Collections} 不会被添加到模型中，因为
	 * 我们无法正确确定真正的约定名称。视图代码应检查 {@code null}，而不是检查空集合，JSTL 标签已经这样
	 * 做了。</i>
	 * @param attributeValue the model attribute value (never {@code null})
	 */
	Model addAttribute(Object attributeValue);

	/**
	 * 将提供的 {@code Collection} 中的所有属性复制到此 {@code Map}，并对每个元素使用属性名生成方式。
	 * @see #addAttribute(Object)
	 */
	Model addAllAttributes(Collection<?> attributeValues);

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map}。
	 * @see #addAttribute(String, Object)
	 */
	Model addAllAttributes(Map<String, ?> attributes);

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map}，同名对象以现有对象优先（即不会被替换）。
	 */
	Model mergeAttributes(Map<String, ?> attributes);

	/**
	 * 此模型是否包含给定名称的属性？
	 * @param attributeName the name of the model attribute (never {@code null})
	 * @return whether this model contains a corresponding attribute
	 */
	boolean containsAttribute(String attributeName);

	/**
	 * 返回给定名称的属性值（如果有）。
	 * @param attributeName the name of the model attribute (never {@code null})
	 * @return the corresponding attribute value, or {@code null} if none
	 * @since 5.2
	 */
	@Nullable
	Object getAttribute(String attributeName);

	/**
	 * 以 Map 形式返回当前的一组模型属性。
	 */
	Map<String, Object> asMap();

}
