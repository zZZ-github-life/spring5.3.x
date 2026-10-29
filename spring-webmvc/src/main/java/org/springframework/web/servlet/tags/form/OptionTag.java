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
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

/**
 * {@code <option>} 标签渲染单个 HTML 'option'。它根据绑定的值适当地设置 'selected'。
 *
 * <p><b>必须嵌套在 {@link SelectTag} 内部使用。</b>
 *
 * <p>如果 {@link #setValue value} 与外部 {@link SelectTag} 所绑定的值匹配，
 * 则将 '{@code option}' 标记为 'selected'，从而提供完整的数据绑定支持。
 *
 * <p>{@link #setValue value} 属性是必需的，它对应于所渲染的 '{@code option}'
 * 的 '{@code value}' 属性。
 *
 * <p>可以指定可选的 {@link #setLabel label} 属性，其值对应于所渲染
 * '{@code option}' 标签的内部文本。如果未指定 {@link #setLabel label}，
 * 则在渲染内部文本时使用 {@link #setValue value} 属性。
 *
 * <p>
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">属性</th>
 * <th class="colOne">是否必需？</th>
 * <th class="colOne">是否运行时表达式？</th>
 * <th class="colLast">描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>cssClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssErrorClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。当绑定的字段存在错误时使用。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>cssStyle</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>disabled</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。将此属性设置为 'true'
 * 将禁用该 HTML 元素。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用对渲染值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>label</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
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
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>tabindex</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>title</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>value</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class OptionTag extends AbstractHtmlElementBodyTag implements BodyTag {

	/**
	 * 用于暴露此标签的值的 JSP 变量名。
	 */
	public static final String VALUE_VARIABLE_NAME = "value";

	/**
	 * 用于暴露此标签的显示值的 JSP 变量名。
	 */
	public static final String DISPLAY_VALUE_VARIABLE_NAME = "displayValue";

	/**
	 * '{@code selected}' 属性的名称。
	 */
	private static final String SELECTED_ATTRIBUTE = "selected";

	/**
	 * '{@code value}' 属性的名称。
	 */
	private static final String VALUE_ATTRIBUTE = VALUE_VARIABLE_NAME;

	/**
	 * '{@code disabled}' 属性的名称。
	 */
	private static final String DISABLED_ATTRIBUTE = "disabled";


	/**
	 * 所渲染的 HTML {@code <option>} 标签的 'value' 属性。
	 */
	@Nullable
	private Object value;

	/**
	 * 所渲染的 HTML {@code <option>} 标签的文本正文。
	 */
	@Nullable
	private String label;

	@Nullable
	private Object oldValue;

	@Nullable
	private Object oldDisplayValue;

	private boolean disabled;


	/**
	 * 设置所渲染的 HTML {@code <option>} 标签的 'value' 属性。
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 获取所渲染的 HTML {@code <option>} 标签的 'value' 属性。
	 */
	@Nullable
	protected Object getValue() {
		return this.value;
	}

	/**
	 * 设置 '{@code disabled}' 属性的值。
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * 获取 '{@code disabled}' 属性的值。
	 */
	protected boolean isDisabled() {
		return this.disabled;
	}

	/**
	 * 设置所渲染的 HTML {@code <option>} 标签的文本正文。
	 * <p>可以是运行时表达式。
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * 获取所渲染的 HTML {@code <option>} 标签的文本正文。
	 */
	@Nullable
	protected String getLabel() {
		return this.label;
	}


	@Override
	protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
		Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		String label = getLabelValue(value);
		renderOption(value, label, tagWriter);
	}

	@Override
	protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
		Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		String label = bodyContent.getString();
		renderOption(value, label, tagWriter);
	}

	/**
	 * 确保在执行后续操作前处于 '{@code select}' 标签之内。
	 */
	@Override
	protected void onWriteTagContent() {
		assertUnderSelectTag();
	}

	@Override
	protected void exposeAttributes() throws JspException {
		Object value = resolveValue();
		this.oldValue = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		this.pageContext.setAttribute(VALUE_VARIABLE_NAME, value);
		this.oldDisplayValue = this.pageContext.getAttribute(DISPLAY_VALUE_VARIABLE_NAME);
		this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, getDisplayString(value, getBindStatus().getEditor()));
	}

	@Override
	protected BindStatus getBindStatus() {
		return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
	}

	@Override
	protected void removeAttributes() {
		if (this.oldValue != null) {
			this.pageContext.setAttribute(VALUE_ATTRIBUTE, this.oldValue);
			this.oldValue = null;
		}
		else {
			this.pageContext.removeAttribute(VALUE_VARIABLE_NAME);
		}

		if (this.oldDisplayValue != null) {
			this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, this.oldDisplayValue);
			this.oldDisplayValue = null;
		}
		else {
			this.pageContext.removeAttribute(DISPLAY_VALUE_VARIABLE_NAME);
		}
	}

	private void renderOption(Object value, String label, TagWriter tagWriter) throws JspException {
		tagWriter.startTag("option");
		writeOptionalAttribute(tagWriter, "id", resolveId());
		writeOptionalAttributes(tagWriter);
		String renderedValue = getDisplayString(value, getBindStatus().getEditor());
		renderedValue = processFieldValue(getSelectTag().getName(), renderedValue, "option");
		tagWriter.writeAttribute(VALUE_ATTRIBUTE, renderedValue);
		if (isSelected(value)) {
			tagWriter.writeAttribute(SELECTED_ATTRIBUTE, SELECTED_ATTRIBUTE);
		}
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		tagWriter.appendValue(label);
		tagWriter.endTag();
	}

	@Override
	protected String autogenerateId() throws JspException {
		return null;
	}

	/**
	 * 返回此 '{@code option}' 元素的标签值。
	 * <p>如果设置了 {@link #setLabel label} 属性，则使用该属性的解析值，
	 * 否则使用 {@code resolvedValue} 参数的值。
	 */
	private String getLabelValue(Object resolvedValue) throws JspException {
		String label = getLabel();
		Object labelObj = (label == null ? resolvedValue : evaluate("label", label));
		return getDisplayString(labelObj, getBindStatus().getEditor());
	}

	private void assertUnderSelectTag() {
		TagUtils.assertHasAncestorOfType(this, SelectTag.class, "option", "select");
	}

	private SelectTag getSelectTag() {
		return (SelectTag) findAncestorWithClass(this, SelectTag.class);
	}

	private boolean isSelected(Object resolvedValue) {
		return SelectedValueComparator.isSelected(getBindStatus(), resolvedValue);
	}

	@Nullable
	private Object resolveValue() throws JspException {
		return evaluate(VALUE_VARIABLE_NAME, getValue());
	}

}
