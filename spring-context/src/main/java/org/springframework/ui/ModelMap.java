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

package org.springframework.ui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link java.util.Map} 的实现，用于为 UI 工具构建模型数据。支持链式调用以及模型属性名称的生成。
 *
 * <p>此类用作 Servlet MVC 的通用模型持有者，但并不与 Servlet MVC 绑定。请查看 {@link Model} 接口以
 * 了解接口形式的变体。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see Conventions#getVariableName
 * @see org.springframework.web.servlet.ModelAndView
 */
@SuppressWarnings("serial")
public class ModelMap extends LinkedHashMap<String, Object> {

	/**
	 * 构造一个新的空 {@code ModelMap}。
	 */
	public ModelMap() {
	}

	/**
	 * 构造一个新的 {@code ModelMap}，其中包含以给定名称提供的属性。
	 * @see #addAttribute(String, Object)
	 */
	public ModelMap(String attributeName, @Nullable Object attributeValue) {
		addAttribute(attributeName, attributeValue);
	}

	/**
	 * 构造一个新的 {@code ModelMap}，其中包含提供的属性。使用属性名生成方式为提供的模型对象生成键。
	 * @see #addAttribute(Object)
	 */
	public ModelMap(Object attributeValue) {
		addAttribute(attributeValue);
	}


	/**
	 * 在给定名称下添加提供的属性。
	 * @param attributeName the name of the model attribute (never {@code null})
	 * @param attributeValue the model attribute value (can be {@code null})
	 */
	public ModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
		Assert.notNull(attributeName, "Model attribute name must not be null");
		put(attributeName, attributeValue);
		return this;
	}

	/**
	 * 使用 {@link org.springframework.core.Conventions#getVariableName generated name} 将提供的属性添
	 * 加到此 {@code Map}。
	 *
	 * <p><i>注意：使用此方法时，空的 {@link Collection Collections} 不会被添加到模型中，因为我们无法正
	 * 确确定真正的约定名称。视图代码应检查 {@code null}，而不是检查空集合，JSTL 标签已经这样做了。</i>
	 * @param attributeValue the model attribute value (never {@code null})
	 */
	public ModelMap addAttribute(Object attributeValue) {
		Assert.notNull(attributeValue, "Model object must not be null");
		if (attributeValue instanceof Collection && ((Collection<?>) attributeValue).isEmpty()) {
			return this;
		}
		return addAttribute(Conventions.getVariableName(attributeValue), attributeValue);
	}

	/**
	 * 将提供的 {@code Collection} 中的所有属性复制到此 {@code Map}，并对每个元素使用属性名生成方式。
	 * @see #addAttribute(Object)
	 */
	public ModelMap addAllAttributes(@Nullable Collection<?> attributeValues) {
		if (attributeValues != null) {
			for (Object attributeValue : attributeValues) {
				addAttribute(attributeValue);
			}
		}
		return this;
	}

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map}。
	 * @see #addAttribute(String, Object)
	 */
	public ModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			putAll(attributes);
		}
		return this;
	}

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map}，同名对象以现有对象优先（即不会被替换）。
	 */
	public ModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			attributes.forEach((key, value) -> {
				if (!containsKey(key)) {
					put(key, value);
				}
			});
		}
		return this;
	}

	/**
	 * 此模型是否包含给定名称的属性？
	 * @param attributeName the name of the model attribute (never {@code null})
	 * @return whether this model contains a corresponding attribute
	 */
	public boolean containsAttribute(String attributeName) {
		return containsKey(attributeName);
	}

	/**
	 * 返回给定名称的属性值（如果有）。
	 * @param attributeName the name of the model attribute (never {@code null})
	 * @return the corresponding attribute value, or {@code null} if none
	 * @since 5.2
	 */
	@Nullable
	public Object getAttribute(String attributeName) {
		return get(attributeName);
	}

}
