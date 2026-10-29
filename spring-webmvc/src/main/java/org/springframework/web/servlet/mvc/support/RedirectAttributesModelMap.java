/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.mvc.support;

import java.util.Collection;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.validation.DataBinder;

/**
 * {@link RedirectAttributes} 的 {@link ModelMap} 实现，使用 {@link DataBinder}
 * 将值格式化为 String。它还提供了一个存储
 * flash 属性的位置，使其能够在重定向后继续存在，而无需嵌入
 * 重定向 URL 中。
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class RedirectAttributesModelMap extends ModelMap implements RedirectAttributes {

	@Nullable
	private final DataBinder dataBinder;

	private final ModelMap flashAttributes = new ModelMap();


	/**
	 * 不带 DataBinder 的默认构造函数。
	 * 属性值通过 {@link #toString()} 转换为 String。
	 */
	public RedirectAttributesModelMap() {
		this(null);
	}

	/**
	 * 带 DataBinder 的构造函数。
	 * @param dataBinder used to format attribute values as Strings
	 */
	public RedirectAttributesModelMap(@Nullable DataBinder dataBinder) {
		this.dataBinder = dataBinder;
	}


	/**
	 * 返回可用于 flash 存储的候选属性，或返回空 Map。
	 */
	@Override
	public Map<String, ?> getFlashAttributes() {
		return this.flashAttributes;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前将属性值格式化为 String。
	 */
	@Override
	public RedirectAttributesModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
		super.addAttribute(attributeName, formatValue(attributeValue));
		return this;
	}

	@Nullable
	private String formatValue(@Nullable Object value) {
		if (value == null) {
			return null;
		}
		return (this.dataBinder != null ? this.dataBinder.convertIfNecessary(value, String.class) : value.toString());
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前将属性值格式化为 String。
	 */
	@Override
	public RedirectAttributesModelMap addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>每个属性值在添加之前都会格式化为 String。
	 */
	@Override
	public RedirectAttributesModelMap addAllAttributes(@Nullable Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>每个属性值在添加之前都会格式化为 String。
	 */
	@Override
	public RedirectAttributesModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			attributes.forEach(this::addAttribute);
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>每个属性值在合并之前都会格式化为 String。
	 */
	@Override
	public RedirectAttributesModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			attributes.forEach((key, attribute) -> {
				if (!containsKey(key)) {
					addAttribute(key, attribute);
				}
			});
		}
		return this;
	}

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前将该值格式化为 String。
	 */
	@Override
	public Object put(String key, @Nullable Object value) {
		return super.put(key, formatValue(value));
	}

	/**
	 * {@inheritDoc}
	 * <p>每个值在添加之前都会格式化为 String。
	 */
	@Override
	public void putAll(@Nullable Map<? extends String, ? extends Object> map) {
		if (map != null) {
			map.forEach((key, value) -> put(key, formatValue(value)));
		}
	}

	@Override
	public RedirectAttributes addFlashAttribute(String attributeName, @Nullable Object attributeValue) {
		this.flashAttributes.addAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public RedirectAttributes addFlashAttribute(Object attributeValue) {
		this.flashAttributes.addAttribute(attributeValue);
		return this;
	}

}
