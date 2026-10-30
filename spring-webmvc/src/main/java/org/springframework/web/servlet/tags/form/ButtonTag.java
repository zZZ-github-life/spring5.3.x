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
import org.springframework.web.servlet.support.RequestDataValueProcessor;

/**
 * {@code <button>} 标签在 HTML 'button' 标签中渲染表单字段标签。
 * 如果应用依赖于 {@link RequestDataValueProcessor}，
 * 则提供此标签以保证完整性。
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
 * <td><p>disabled</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性。将此属性设置为 'true'
 * 将禁用该 HTML 元素。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>name</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML button 标签的 name 属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>value</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML button 标签的 name 属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class ButtonTag extends AbstractHtmlElementTag {

	/**
	 * '{@code disabled}' 属性的名称。
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";


	@Nullable
	private TagWriter tagWriter;

	@Nullable
	private String name;

	@Nullable
	private String value;

	private boolean disabled;


	/**
	 * 获取 '{@code name}' 属性的值。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 设置 '{@code name}' 属性的值。
	 */
	@Override
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 设置 '{@code value}' 属性的值。
	 */
	public void setValue(@Nullable String value) {
		this.value = value;
	}

	/**
	 * 获取 '{@code value}' 属性的值。
	 */
	@Nullable
	public String getValue() {
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
	public boolean isDisabled() {
		return this.disabled;
	}


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("button");
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute("type", getType());
		writeValue(tagWriter);
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		tagWriter.forceBlock();
		this.tagWriter = tagWriter;
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 将 '{@code value}' 属性写入提供的 {@link TagWriter}。
	 * 子类可以选择覆盖此实现，以精确控制
	 * 写入值的时机。
	 */
	protected void writeValue(TagWriter tagWriter) throws JspException {
		String valueToUse = (getValue() != null ? getValue() : getDefaultValue());
		tagWriter.writeAttribute("value", processFieldValue(getName(), valueToUse, getType()));
	}

	/**
	 * 返回默认值。
	 * @return 未提供时的默认值
	 */
	protected String getDefaultValue() {
		return "Submit";
	}

	/**
	 * 获取 '{@code type}' 属性的值。子类可以覆盖此方法
	 * 以更改所渲染 '{@code input}' 元素的类型。
	 * 默认值为 '{@code submit}'。
	 */
	protected String getType() {
		return "submit";
	}

	/**
	 * 关闭 '{@code button}' 块标签。
	 */
	@Override
	public int doEndTag() throws JspException {
		Assert.state(this.tagWriter != null, "No TagWriter set");
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

}
