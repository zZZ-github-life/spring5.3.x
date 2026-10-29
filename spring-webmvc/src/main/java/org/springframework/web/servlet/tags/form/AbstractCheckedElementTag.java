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

import javax.servlet.jsp.JspException;

import org.springframework.lang.Nullable;

/**
 * 抽象基类，提供用于实现数据绑定感知 JSP 标签的通用方法，
 * 这些标签用于渲染 '{@code type}' 为 '{@code checkbox}' 或 '{@code radio}' 的
 * HTML '{@code input}' 元素。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.5
 */
@SuppressWarnings("serial")
public abstract class AbstractCheckedElementTag extends AbstractHtmlInputElementTag {

	/**
	 * 使用提供的值渲染 '{@code input(checkbox)}'，如果提供的值
	 * 与绑定值匹配，则将 '{@code input}' 元素标记为 'checked'。
	 */
	protected void renderFromValue(@Nullable Object value, TagWriter tagWriter) throws JspException {
		renderFromValue(value, value, tagWriter);
	}

	/**
	 * 使用提供的值渲染 '{@code input(checkbox)}'，如果提供的值
	 * 与绑定值匹配，则将 '{@code input}' 元素标记为 'checked'。
	 */
	protected void renderFromValue(@Nullable Object item, @Nullable Object value, TagWriter tagWriter)
			throws JspException {

		String displayValue = convertToDisplayString(value);
		tagWriter.writeAttribute("value", processFieldValue(getName(), displayValue, getInputType()));
		if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
			tagWriter.writeAttribute("checked", "checked");
		}
	}

	/**
	 * 通过委托给 {@link SelectedValueComparator#isSelected}，
	 * 判断提供的值是否与选中的值匹配。
	 */
	private boolean isOptionSelected(@Nullable Object value) throws JspException {
		return SelectedValueComparator.isSelected(getBindStatus(), value);
	}

	/**
	 * 使用提供的值渲染 '{@code input(checkbox)}'，如果提供的 Boolean 为
	 * {@code true}，则将 '{@code input}' 元素标记为 'checked'。
	 */
	protected void renderFromBoolean(Boolean boundValue, TagWriter tagWriter) throws JspException {
		tagWriter.writeAttribute("value", processFieldValue(getName(), "true", getInputType()));
		if (boundValue) {
			tagWriter.writeAttribute("checked", "checked");
		}
	}

	/**
	 * 在当前 PageContext 中为绑定名称返回唯一的 ID。
	 */
	@Override
	@Nullable
	protected String autogenerateId() throws JspException {
		String id = super.autogenerateId();
		return (id != null ? TagIdGenerator.nextId(id, this.pageContext) : null);
	}


	/**
	 * 将 '{@code input}' 元素写入提供的
	 * {@link TagWriter}，
	 * 并在适当时将其标记为 'checked'。
	 */
	@Override
	protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

	/**
	 * 将 "type" 标记为非法的动态属性。
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !"type".equals(localName);
	}

	/**
	 * 返回要生成的 HTML input 元素的类型：
	 * "checkbox" 或 "radio"。
	 */
	protected abstract String getInputType();

}
