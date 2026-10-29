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
import org.springframework.util.Assert;

/**
 * 抽象基类，提供用于实现数据绑定感知 JSP 标签的通用方法，
 * 这些标签用于渲染 '{@code type}' 为 '{@code checkbox}' 或 '{@code radio}' 的
 * <i>单个</i> HTML '{@code input}' 元素。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
@SuppressWarnings("serial")
public abstract class AbstractSingleCheckedElementTag extends AbstractCheckedElementTag {

	/**
	 * '{@code value}' 属性的值。
	 */
	@Nullable
	private Object value;

	/**
	 * '{@code label}' 属性的值。
	 */
	@Nullable
	private Object label;


	/**
	 * 设置 '{@code value}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 获取 '{@code value}' 属性的值。
	 */
	@Nullable
	protected Object getValue() {
		return this.value;
	}

	/**
	 * 设置 '{@code label}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setLabel(Object label) {
		this.label = label;
	}

	/**
	 * 获取 '{@code label}' 属性的值。
	 */
	@Nullable
	protected Object getLabel() {
		return this.label;
	}


	/**
	 * 使用配置的 {@link #setValue(Object) value} 渲染 '{@code input(radio)}' 元素。
	 * 如果该值与 {@link #getValue 绑定值}匹配，则将元素标记为 checked。
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("input");
		String id = resolveId();
		writeOptionalAttribute(tagWriter, "id", id);
		writeOptionalAttribute(tagWriter, "name", getName());
		writeOptionalAttributes(tagWriter);
		writeTagDetails(tagWriter);
		tagWriter.endTag();

		Object resolvedLabel = evaluate("label", getLabel());
		if (resolvedLabel != null) {
			Assert.state(id != null, "Label id is required");
			tagWriter.startTag("label");
			tagWriter.writeAttribute("for", id);
			tagWriter.appendValue(convertToDisplayString(resolvedLabel));
			tagWriter.endTag();
		}

		return SKIP_BODY;
	}

	/**
	 * 写入给定主标签的详细信息：
	 * 即特殊属性和标签的值。
	 */
	protected abstract void writeTagDetails(TagWriter tagWriter) throws JspException;

}
