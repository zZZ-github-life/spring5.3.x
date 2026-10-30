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

package org.springframework.ui;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 {@link ConcurrentHashMap} 的 {@link Model} 接口实现，适用于并发场景。
 *
 * <p>由 Spring WebFlux 暴露给处理方法，通常通过声明 {@link Model} 接口来实现。通常无需在用户代码中
 * 创建它。如有必要，处理方法可以返回一个常规的 {@code java.util.Map}（很可能是
 * {@code java.util.ConcurrentMap}），以提供预先确定的模型。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ConcurrentModel extends ConcurrentHashMap<String, Object> implements Model {

	/**
	 * 构造一个新的空 {@code ConcurrentModel}。
	 */
	public ConcurrentModel() {
	}

	/**
	 * 构造一个新的 {@code ModelMap}，其中包含以给定名称提供的属性。
	 * @see #addAttribute(String, Object)
	 */
	public ConcurrentModel(String attributeName, Object attributeValue) {
		addAttribute(attributeName, attributeValue);
	}

	/**
	 * 构造一个新的 {@code ModelMap}，其中包含提供的属性。使用属性名生成方式为提供的模型对象生成键。
	 * @see #addAttribute(Object)
	 */
	public ConcurrentModel(Object attributeValue) {
		addAttribute(attributeValue);
	}


	@Override
	@Nullable
	public Object put(String key, @Nullable Object value) {
		if (value != null) {
			return super.put(key, value);
		}
		else {
			return remove(key);
		}
	}

	@Override
	public void putAll(Map<? extends String, ?> map) {
		for (Map.Entry<? extends String, ?> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 在给定名称下添加提供的属性。
	 * @param attributeName 模型属性的名称（永不为 {@code null}）
	 * @param attributeValue 模型属性值（如果为 {@code null} 则忽略，
	 * 仅移除现有条目（如果有））
	 */
	@Override
	public ConcurrentModel addAttribute(String attributeName, @Nullable Object attributeValue) {
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
	 * @param attributeValue 模型属性值（永不为 {@code null}）
	 */
	@Override
	public ConcurrentModel addAttribute(Object attributeValue) {
		Assert.notNull(attributeValue, "Model attribute value must not be null");
		if (attributeValue instanceof Collection && ((Collection<?>) attributeValue).isEmpty()) {
			return this;
		}
		return addAttribute(Conventions.getVariableName(attributeValue), attributeValue);
	}

	/**
	 * 将提供的 {@code Collection} 中的所有属性复制到此 {@code Map}，并对每个元素使用属性名生成方式。
	 * @see #addAttribute(Object)
	 */
	@Override
	public ConcurrentModel addAllAttributes(@Nullable Collection<?> attributeValues) {
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
	@Override
	public ConcurrentModel addAllAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			putAll(attributes);
		}
		return this;
	}

	/**
	 * 将提供的 {@code Map} 中的所有属性复制到此 {@code Map}，同名对象以现有对象优先（即不会被替换）。
	 */
	@Override
	public ConcurrentModel mergeAttributes(@Nullable Map<String, ?> attributes) {
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
	 * @param attributeName 模型属性的名称（永不为 {@code null}）
	 * @return 此模型是否包含相应的属性
	 */
	@Override
	public boolean containsAttribute(String attributeName) {
		return containsKey(attributeName);
	}

	@Override
	@Nullable
	public Object getAttribute(String attributeName) {
		return get(attributeName);
	}

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

}
