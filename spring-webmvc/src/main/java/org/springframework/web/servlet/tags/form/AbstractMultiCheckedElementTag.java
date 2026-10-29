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

package org.springframework.web.servlet.tags.form;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.jsp.JspException;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 抽象基类，提供用于实现数据绑定感知 JSP 标签的通用方法，
 * 这些标签用于渲染 '{@code type}' 为 '{@code checkbox}' 或 '{@code radio}' 的
 * <i>多个</i> HTML '{@code input}' 元素。
 *
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @since 2.5.2
 */
@SuppressWarnings("serial")
public abstract class AbstractMultiCheckedElementTag extends AbstractCheckedElementTag {

	/**
	 * HTML '{@code span}' 标签。
	 */
	private static final String SPAN_TAG = "span";


	/**
	 * 用于生成 '{@code input type="checkbox/radio"}' 标签的
	 * {@link java.util.Collection}、{@link java.util.Map} 或对象数组。
	 */
	@Nullable
	private Object items;

	/**
	 * 映射到 '{@code input type="checkbox/radio"}' 标签的
	 * '{@code value}' 属性的属性名称。
	 */
	@Nullable
	private String itemValue;

	/**
	 * 作为 '{@code input type="checkbox/radio"}' 标签的一部分显示的值。
	 */
	@Nullable
	private String itemLabel;

	/**
	 * 用于包围 '{@code input type="checkbox/radio"}' 标签的 HTML 元素。
	 */
	private String element = SPAN_TAG;

	/**
	 * 在每个 '{@code input type="checkbox/radio"}' 标签之间使用的分隔符。
	 */
	@Nullable
	private String delimiter;


	/**
	 * 设置用于生成 '{@code input type="checkbox/radio"}' 标签的
	 * {@link java.util.Collection}、{@link java.util.Map} 或对象数组。
	 * <p>通常是运行时表达式。
	 * @param items said items
	 */
	public void setItems(Object items) {
		Assert.notNull(items, "'items' must not be null");
		this.items = items;
	}

	/**
	 * 获取用于生成 '{@code input type="checkbox/radio"}' 标签的
	 * {@link java.util.Collection}、{@link java.util.Map} 或对象数组。
	 */
	@Nullable
	protected Object getItems() {
		return this.items;
	}

	/**
	 * 设置映射到 '{@code input type="checkbox/radio"}' 标签的
	 * '{@code value}' 属性的属性名称。
	 * <p>可以是运行时表达式。
	 */
	public void setItemValue(String itemValue) {
		Assert.hasText(itemValue, "'itemValue' must not be empty");
		this.itemValue = itemValue;
	}

	/**
	 * 获取映射到 '{@code input type="checkbox/radio"}' 标签的
	 * '{@code value}' 属性的属性名称。
	 */
	@Nullable
	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * 设置作为 '{@code input type="checkbox/radio"}' 标签的一部分
	 * 显示的值。
	 * <p>可以是运行时表达式。
	 */
	public void setItemLabel(String itemLabel) {
		Assert.hasText(itemLabel, "'itemLabel' must not be empty");
		this.itemLabel = itemLabel;
	}

	/**
	 * 获取作为 '{@code input type="checkbox/radio"}' 标签的一部分
	 * 显示的值。
	 */
	@Nullable
	protected String getItemLabel() {
		return this.itemLabel;
	}

	/**
	 * 设置在每个 '{@code input type="checkbox/radio"}' 标签之间
	 * 使用的分隔符。
	 * <p>默认情况下<em>没有</em>分隔符。
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * 返回在每个 '{@code input type="radio"}' 标签之间使用的分隔符。
	 */
	@Nullable
	public String getDelimiter() {
		return this.delimiter;
	}

	/**
	 * 设置用于包围 '{@code input type="checkbox/radio"}' 标签的
	 * HTML 元素。
	 * <p>默认为 HTML '{@code <span/>}' 标签。
	 */
	public void setElement(String element) {
		Assert.hasText(element, "'element' cannot be null or blank");
		this.element = element;
	}

	/**
	 * 获取用于包围 '{@code input type="checkbox/radio"}' 标签的 HTML 元素。
	 */
	public String getElement() {
		return this.element;
	}


	/**
	 * 同时向指定 id 追加一个计数器，
	 * 因为我们处理的是多个 HTML 元素。
	 */
	@Override
	protected String resolveId() throws JspException {
		Object id = evaluate("id", getId());
		if (id != null) {
			String idString = id.toString();
			return (StringUtils.hasText(idString) ? TagIdGenerator.nextId(idString, this.pageContext) : null);
		}
		return autogenerateId();
	}

	/**
	 * 使用配置的 {@link #setItems(Object)} 值渲染 '{@code input type="radio"}' 元素。
	 * 如果值与绑定值匹配，则将元素标记为 checked。
	 */
	@Override
	@SuppressWarnings("rawtypes")
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		Object items = getItems();
		Object itemsObject = (items instanceof String ? evaluate("items", items) : items);

		String itemValue = getItemValue();
		String itemLabel = getItemLabel();
		String valueProperty =
				(itemValue != null ? ObjectUtils.getDisplayString(evaluate("itemValue", itemValue)) : null);
		String labelProperty =
				(itemLabel != null ? ObjectUtils.getDisplayString(evaluate("itemLabel", itemLabel)) : null);

		Class<?> boundType = getBindStatus().getValueType();
		if (itemsObject == null && boundType != null && boundType.isEnum()) {
			itemsObject = boundType.getEnumConstants();
		}

		if (itemsObject == null) {
			throw new IllegalArgumentException("Attribute 'items' is required and must be a Collection, an Array or a Map");
		}

		if (itemsObject.getClass().isArray()) {
			Object[] itemsArray = (Object[]) itemsObject;
			for (int i = 0; i < itemsArray.length; i++) {
				Object item = itemsArray[i];
				writeObjectEntry(tagWriter, valueProperty, labelProperty, item, i);
			}
		}
		else if (itemsObject instanceof Collection) {
			final Collection<?> optionCollection = (Collection<?>) itemsObject;
			int itemIndex = 0;
			for (Iterator<?> it = optionCollection.iterator(); it.hasNext(); itemIndex++) {
				Object item = it.next();
				writeObjectEntry(tagWriter, valueProperty, labelProperty, item, itemIndex);
			}
		}
		else if (itemsObject instanceof Map) {
			final Map<?, ?> optionMap = (Map<?, ?>) itemsObject;
			int itemIndex = 0;
			for (Iterator it = optionMap.entrySet().iterator(); it.hasNext(); itemIndex++) {
				Map.Entry entry = (Map.Entry) it.next();
				writeMapEntry(tagWriter, valueProperty, labelProperty, entry, itemIndex);
			}
		}
		else {
			throw new IllegalArgumentException("Attribute 'items' must be an array, a Collection or a Map");
		}

		return SKIP_BODY;
	}

	private void writeObjectEntry(TagWriter tagWriter, @Nullable String valueProperty,
			@Nullable String labelProperty, Object item, int itemIndex) throws JspException {

		BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
		Object renderValue;
		if (valueProperty != null) {
			renderValue = wrapper.getPropertyValue(valueProperty);
		}
		else if (item instanceof Enum) {
			renderValue = ((Enum<?>) item).name();
		}
		else {
			renderValue = item;
		}
		Object renderLabel = (labelProperty != null ? wrapper.getPropertyValue(labelProperty) : item);
		writeElementTag(tagWriter, item, renderValue, renderLabel, itemIndex);
	}

	private void writeMapEntry(TagWriter tagWriter, @Nullable String valueProperty,
			@Nullable String labelProperty, Map.Entry<?, ?> entry, int itemIndex) throws JspException {

		Object mapKey = entry.getKey();
		Object mapValue = entry.getValue();
		BeanWrapper mapKeyWrapper = PropertyAccessorFactory.forBeanPropertyAccess(mapKey);
		BeanWrapper mapValueWrapper = PropertyAccessorFactory.forBeanPropertyAccess(mapValue);
		Object renderValue = (valueProperty != null ?
				mapKeyWrapper.getPropertyValue(valueProperty) : mapKey.toString());
		Object renderLabel = (labelProperty != null ?
				mapValueWrapper.getPropertyValue(labelProperty) : mapValue.toString());
		writeElementTag(tagWriter, mapKey, renderValue, renderLabel, itemIndex);
	}

	private void writeElementTag(TagWriter tagWriter, Object item, @Nullable Object value,
			@Nullable Object label, int itemIndex) throws JspException {

		tagWriter.startTag(getElement());
		if (itemIndex > 0) {
			Object resolvedDelimiter = evaluate("delimiter", getDelimiter());
			if (resolvedDelimiter != null) {
				tagWriter.appendValue(resolvedDelimiter.toString());
			}
		}
		tagWriter.startTag("input");
		String id = resolveId();
		Assert.state(id != null, "Attribute 'id' is required");
		writeOptionalAttribute(tagWriter, "id", id);
		writeOptionalAttribute(tagWriter, "name", getName());
		writeOptionalAttributes(tagWriter);
		tagWriter.writeAttribute("type", getInputType());
		renderFromValue(item, value, tagWriter);
		tagWriter.endTag();
		tagWriter.startTag("label");
		tagWriter.writeAttribute("for", id);
		tagWriter.appendValue(convertToDisplayString(label));
		tagWriter.endTag();
		tagWriter.endTag();
	}

}
