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
 * 用于渲染 HTML 表单 input 元素的数据绑定感知 JSP 标签的基类。
 *
 * <p>提供一组与表单 input 元素通用 HTML 属性相对应的属性。
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlInputElementTag extends AbstractHtmlElementTag {

	/**
	 * '{@code onfocus}' 属性的名称。
	 */
	public static final String ONFOCUS_ATTRIBUTE = "onfocus";

	/**
	 * '{@code onblur}' 属性的名称。
	 */
	public static final String ONBLUR_ATTRIBUTE = "onblur";

	/**
	 * '{@code onchange}' 属性的名称。
	 */
	public static final String ONCHANGE_ATTRIBUTE = "onchange";

	/**
	 * '{@code accesskey}' 属性的名称。
	 */
	public static final String ACCESSKEY_ATTRIBUTE = "accesskey";

	/**
	 * '{@code disabled}' 属性的名称。
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";

	/**
	 * '{@code readonly}' 属性的名称。
	 */
	public static final String READONLY_ATTRIBUTE = "readonly";


	@Nullable
	private String onfocus;

	@Nullable
	private String onblur;

	@Nullable
	private String onchange;

	@Nullable
	private String accesskey;

	private boolean disabled;

	private boolean readonly;


	/**
	 * 设置 '{@code onfocus}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnfocus(String onfocus) {
		this.onfocus = onfocus;
	}

	/**
	 * 获取 '{@code onfocus}' 属性的值。
	 */
	@Nullable
	protected String getOnfocus() {
		return this.onfocus;
	}

	/**
	 * 设置 '{@code onblur}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnblur(String onblur) {
		this.onblur = onblur;
	}

	/**
	 * 获取 '{@code onblur}' 属性的值。
	 */
	@Nullable
	protected String getOnblur() {
		return this.onblur;
	}

	/**
	 * 设置 '{@code onchange}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnchange(String onchange) {
		this.onchange = onchange;
	}

	/**
	 * 获取 '{@code onchange}' 属性的值。
	 */
	@Nullable
	protected String getOnchange() {
		return this.onchange;
	}

	/**
	 * 设置 '{@code accesskey}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setAccesskey(String accesskey) {
		this.accesskey = accesskey;
	}

	/**
	 * 获取 '{@code accesskey}' 属性的值。
	 */
	@Nullable
	protected String getAccesskey() {
		return this.accesskey;
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
	 * 设置 '{@code readonly}' 属性的值。
	 */
	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	/**
	 * 获取 '{@code readonly}' 属性的值。
	 */
	protected boolean isReadonly() {
		return this.readonly;
	}


	/**
	 * 添加此基类定义的 input 专用可选属性。
	 */
	@Override
	protected void writeOptionalAttributes(TagWriter tagWriter) throws JspException {
		super.writeOptionalAttributes(tagWriter);

		writeOptionalAttribute(tagWriter, ONFOCUS_ATTRIBUTE, getOnfocus());
		writeOptionalAttribute(tagWriter, ONBLUR_ATTRIBUTE, getOnblur());
		writeOptionalAttribute(tagWriter, ONCHANGE_ATTRIBUTE, getOnchange());
		writeOptionalAttribute(tagWriter, ACCESSKEY_ATTRIBUTE, getAccesskey());
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		if (isReadonly()) {
			writeOptionalAttribute(tagWriter, READONLY_ATTRIBUTE, "readonly");
		}
	}

}
