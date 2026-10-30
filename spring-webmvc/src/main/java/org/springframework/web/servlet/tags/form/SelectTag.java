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
import java.util.Map;

import javax.servlet.jsp.JspException;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.support.BindStatus;

/**
 * {@code <select>} 标签渲染一个 HTML 'select' 元素。
 * 支持与选中选项进行数据绑定。
 *
 * <p>可以使用 OptionWriter 类支持的某种方法来渲染内部
 * '{@code option}' 标签。
 *
 * <p>还支持使用嵌套的 {@link OptionTag OptionTags} 或
 * （通常一个）嵌套的 {@link OptionsTag}。
 *
 * <p>
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">属性</th>
 * <th class="colOne">是否必需？</th>
 * <th class="colOne">运行时表达式？</th>
 * <th class="colLast">描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>accesskey</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>cssErrorClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。当绑定字段存在错误时使用。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssStyle</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>disabled</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。将此属性值设置为 'true' 将禁用该 HTML 元素。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用渲染值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>itemLabel</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>映射到 'option' 标签内部文本的属性名</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>items</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>用于生成内部 'option' 标签的 Collection、Map 或对象数组</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>itemValue</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>映射到 'option' 标签的 'value' 属性的属性名</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>multiple</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onblur</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onchange</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>ondblclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onfocus</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>path</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>用于数据绑定的属性路径</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>size</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>tabindex</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>title</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see OptionTag
 */
@SuppressWarnings("serial")
public class SelectTag extends AbstractHtmlInputElementTag {

	/**
	 * 将绑定值暴露给内部 {@link OptionTag OptionTags} 的 {@link javax.servlet.jsp.PageContext} 属性名称。
	 */
	public static final String LIST_VALUE_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.form.SelectTag.listValue";

	/**
	 * 用于表示已指定但解析为 null 的项的标志对象。可区分“已设置为 null”和“完全未设置”。
	 */
	private static final Object EMPTY = new Object();


	/**
	 * 用于生成内部 '{@code option}' 标签的 {@link Collection}、{@link Map} 或对象数组。
	 */
	@Nullable
	private Object items;

	/**
	 * 映射到 '{@code option}' 标签的 '{@code value}' 属性的属性名。
	 */
	@Nullable
	private String itemValue;

	/**
	 * 映射到 '{@code option}' 标签内部文本的属性名。
	 */
	@Nullable
	private String itemLabel;

	/**
	 * 在最终渲染的 '{@code select}' 元素上使用的 HTML '{@code size}' 属性值。
	 */
	@Nullable
	private String size;

	/**
	 * 指示 '{@code select}' 标签是否允许多选。
	 */
	@Nullable
	private Object multiple;

	/**
	 * 用于写出输出的 {@link TagWriter} 实例。<p>仅在与嵌套的 {@link OptionTag OptionTags} 一起使用时有效。
	 */
	@Nullable
	private TagWriter tagWriter;


	/**
	 * 设置用于生成内部 '{@code option}' 标签的 {@link Collection}、{@link Map} 或对象数组。<p>当希望从数组、{@link Collection} 或 {@link Map} 渲染 '{@code option}' 标签时为必需。<p>通常为运行时表达式。
	 * @param items 组成该选择框选项的项
	 */
	public void setItems(@Nullable Object items) {
		this.items = (items != null ? items : EMPTY);
	}

	/**
	 * 获取 '{@code items}' 属性的值。<p>可以是运行时表达式。
	 */
	@Nullable
	protected Object getItems() {
		return this.items;
	}

	/**
	 * 设置映射到 '{@code option}' 标签的 '{@code value}' 属性的属性名。<p>当希望从数组或 {@link Collection} 渲染 '{@code option}' 标签时为必需。<p>可以是运行时表达式。
	 */
	public void setItemValue(String itemValue) {
		this.itemValue = itemValue;
	}

	/**
	 * 获取 '{@code itemValue}' 属性的值。<p>可以是运行时表达式。
	 */
	@Nullable
	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * 设置映射到 '{@code option}' 标签的标签（内部文本）的属性名。<p>可以是运行时表达式。
	 */
	public void setItemLabel(String itemLabel) {
		this.itemLabel = itemLabel;
	}

	/**
	 * 获取 '{@code itemLabel}' 属性的值。<p>可以是运行时表达式。
	 */
	@Nullable
	protected String getItemLabel() {
		return this.itemLabel;
	}

	/**
	 * 设置最终渲染的 '{@code select}' 元素上 HTML '{@code size}' 属性的值。
	 */
	public void setSize(String size) {
		this.size = size;
	}

	/**
	 * 获取 '{@code size}' 属性的值。
	 */
	@Nullable
	protected String getSize() {
		return this.size;
	}

	/**
	 * 设置最终渲染的 '{@code select}' 元素上 HTML '{@code multiple}' 属性的值。
	 */
	public void setMultiple(Object multiple) {
		this.multiple = multiple;
	}

	/**
	 * 获取最终渲染的 '{@code select}' 元素上 HTML '{@code multiple}' 属性的值。
	 */
	@Nullable
	protected Object getMultiple() {
		return this.multiple;
	}


	/**
	 * 将 HTML '{@code select}' 标签渲染到给定的 {@link TagWriter}。<p>如果设置了 {@link #setItems items} 属性，则渲染嵌套的 '{@code option}' 标签；否则将绑定值暴露给嵌套的 {@link OptionTag OptionTags}。
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("select");
		writeDefaultAttributes(tagWriter);
		if (isMultiple()) {
			tagWriter.writeAttribute("multiple", "multiple");
		}
		tagWriter.writeOptionalAttributeValue("size", getDisplayString(evaluate("size", getSize())));

		Object items = getItems();
		if (items != null) {
			// Items specified, but might still be empty...
			if (items != EMPTY) {
				Object itemsObject = evaluate("items", items);
				if (itemsObject != null) {
					final String selectName = getName();
					String valueProperty = (getItemValue() != null ?
							ObjectUtils.getDisplayString(evaluate("itemValue", getItemValue())) : null);
					String labelProperty = (getItemLabel() != null ?
							ObjectUtils.getDisplayString(evaluate("itemLabel", getItemLabel())) : null);
					OptionWriter optionWriter =
							new OptionWriter(itemsObject, getBindStatus(), valueProperty, labelProperty, isHtmlEscape()) {
								@Override
								protected String processOptionValue(String resolvedValue) {
									return processFieldValue(selectName, resolvedValue, "option");
								}
							};
					optionWriter.writeOptions(tagWriter);
				}
			}
			tagWriter.endTag(true);
			writeHiddenTagIfNecessary(tagWriter);
			return SKIP_BODY;
		}
		else {
			// Using nested <form:option/> tags, so just expose the value in the PageContext...
			tagWriter.forceBlock();
			this.tagWriter = tagWriter;
			this.pageContext.setAttribute(LIST_VALUE_PAGE_ATTRIBUTE, getBindStatus());
			return EVAL_BODY_INCLUDE;
		}
	}

	/**
	 * 如果使用多选，则需要一个隐藏元素，以确保在响应 {@code null} 提交时，服务端能正确取消选择所有项。
	 */
	private void writeHiddenTagIfNecessary(TagWriter tagWriter) throws JspException {
		if (isMultiple()) {
			tagWriter.startTag("input");
			tagWriter.writeAttribute("type", "hidden");
			String name = WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX + getName();
			tagWriter.writeAttribute("name", name);
			tagWriter.writeAttribute("value", processFieldValue(name, "1", "hidden"));
			tagWriter.endTag();
		}
	}

	private boolean isMultiple() throws JspException {
		Object multiple = getMultiple();
		if (multiple != null) {
			String stringValue = multiple.toString();
			return ("multiple".equalsIgnoreCase(stringValue) || Boolean.parseBoolean(stringValue));
		}
		return forceMultiple();
	}

	/**
	 * 如果绑定值要求生成的 '{@code select}' 标签为多选，则返回 '{@code true}'。
	 */
	private boolean forceMultiple() throws JspException {
		BindStatus bindStatus = getBindStatus();
		Class<?> valueType = bindStatus.getValueType();
		if (valueType != null && typeRequiresMultiple(valueType)) {
			return true;
		}
		else if (bindStatus.getEditor() != null) {
			Object editorValue = bindStatus.getEditor().getValue();
			if (editorValue != null && typeRequiresMultiple(editorValue.getClass())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 对于数组、{@link Collection Collections} 和 {@link Map Maps} 返回 '{@code true}'。
	 */
	private static boolean typeRequiresMultiple(Class<?> type) {
		return (type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type));
	}

	/**
	 * 关闭使用嵌套 {@link OptionTag options} 时可能已打开的所有块级标签。
	 */
	@Override
	public int doEndTag() throws JspException {
		if (this.tagWriter != null) {
			this.tagWriter.endTag();
			writeHiddenTagIfNecessary(this.tagWriter);
		}
		return EVAL_PAGE;
	}

	/**
	 * 清除使用嵌套 {@link OptionTag options} 时可能遗留的 {@link TagWriter}。
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.tagWriter = null;
		this.pageContext.removeAttribute(LIST_VALUE_PAGE_ATTRIBUTE);
	}

}
