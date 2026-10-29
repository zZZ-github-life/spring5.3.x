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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于渲染 HTML 元素的数据绑定感知 JSP 标签的基类。提供
 * 一组与各元素通用 HTML 属性相对应的属性。
 *
 * <p>此外，此基类允许将非标准属性作为标签输出的一部分进行渲染。
 * 如有需要，子类可以通过
 * {@link AbstractHtmlElementTag#getDynamicAttributes() dynamicAttributes} map 访问这些属性。
 *
 * @author Rob Harrop
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlElementTag extends AbstractDataBoundFormElementTag implements DynamicAttributes {

	public static final String CLASS_ATTRIBUTE = "class";

	public static final String STYLE_ATTRIBUTE = "style";

	public static final String LANG_ATTRIBUTE = "lang";

	public static final String TITLE_ATTRIBUTE = "title";

	public static final String DIR_ATTRIBUTE = "dir";

	public static final String TABINDEX_ATTRIBUTE = "tabindex";

	public static final String ONCLICK_ATTRIBUTE = "onclick";

	public static final String ONDBLCLICK_ATTRIBUTE = "ondblclick";

	public static final String ONMOUSEDOWN_ATTRIBUTE = "onmousedown";

	public static final String ONMOUSEUP_ATTRIBUTE = "onmouseup";

	public static final String ONMOUSEOVER_ATTRIBUTE = "onmouseover";

	public static final String ONMOUSEMOVE_ATTRIBUTE = "onmousemove";

	public static final String ONMOUSEOUT_ATTRIBUTE = "onmouseout";

	public static final String ONKEYPRESS_ATTRIBUTE = "onkeypress";

	public static final String ONKEYUP_ATTRIBUTE = "onkeyup";

	public static final String ONKEYDOWN_ATTRIBUTE = "onkeydown";


	@Nullable
	private String cssClass;

	@Nullable
	private String cssErrorClass;

	@Nullable
	private String cssStyle;

	@Nullable
	private String lang;

	@Nullable
	private String title;

	@Nullable
	private String dir;

	@Nullable
	private String tabindex;

	@Nullable
	private String onclick;

	@Nullable
	private String ondblclick;

	@Nullable
	private String onmousedown;

	@Nullable
	private String onmouseup;

	@Nullable
	private String onmouseover;

	@Nullable
	private String onmousemove;

	@Nullable
	private String onmouseout;

	@Nullable
	private String onkeypress;

	@Nullable
	private String onkeyup;

	@Nullable
	private String onkeydown;

	@Nullable
	private Map<String, Object> dynamicAttributes;


	/**
	 * 设置 '{@code class}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	/**
	 * 获取 '{@code class}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getCssClass() {
		return this.cssClass;
	}

	/**
	 * 当绑定到特定标签的字段存在错误时要使用的 CSS class。
	 * 可以是运行时表达式。
	 */
	public void setCssErrorClass(String cssErrorClass) {
		this.cssErrorClass = cssErrorClass;
	}

	/**
	 * 当绑定到特定标签的字段存在错误时要使用的 CSS class。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getCssErrorClass() {
		return this.cssErrorClass;
	}

	/**
	 * 设置 '{@code style}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setCssStyle(String cssStyle) {
		this.cssStyle = cssStyle;
	}

	/**
	 * 获取 '{@code style}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getCssStyle() {
		return this.cssStyle;
	}

	/**
	 * 设置 '{@code lang}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * 获取 '{@code lang}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getLang() {
		return this.lang;
	}

	/**
	 * 设置 '{@code title}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * 获取 '{@code title}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getTitle() {
		return this.title;
	}

	/**
	 * 设置 '{@code dir}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setDir(String dir) {
		this.dir = dir;
	}

	/**
	 * 获取 '{@code dir}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getDir() {
		return this.dir;
	}

	/**
	 * 设置 '{@code tabindex}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setTabindex(String tabindex) {
		this.tabindex = tabindex;
	}

	/**
	 * 获取 '{@code tabindex}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getTabindex() {
		return this.tabindex;
	}

	/**
	 * 设置 '{@code onclick}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnclick(String onclick) {
		this.onclick = onclick;
	}

	/**
	 * 获取 '{@code onclick}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOnclick() {
		return this.onclick;
	}

	/**
	 * 设置 '{@code ondblclick}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOndblclick(String ondblclick) {
		this.ondblclick = ondblclick;
	}

	/**
	 * 获取 '{@code ondblclick}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOndblclick() {
		return this.ondblclick;
	}

	/**
	 * 设置 '{@code onmousedown}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnmousedown(String onmousedown) {
		this.onmousedown = onmousedown;
	}

	/**
	 * 获取 '{@code onmousedown}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOnmousedown() {
		return this.onmousedown;
	}

	/**
	 * 设置 '{@code onmouseup}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnmouseup(String onmouseup) {
		this.onmouseup = onmouseup;
	}

	/**
	 * 获取 '{@code onmouseup}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOnmouseup() {
		return this.onmouseup;
	}

	/**
	 * 设置 '{@code onmouseover}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnmouseover(String onmouseover) {
		this.onmouseover = onmouseover;
	}

	/**
	 * 获取 '{@code onmouseover}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOnmouseover() {
		return this.onmouseover;
	}

	/**
	 * 设置 '{@code onmousemove}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnmousemove(String onmousemove) {
		this.onmousemove = onmousemove;
	}

	/**
	 * 获取 '{@code onmousemove}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOnmousemove() {
		return this.onmousemove;
	}

	/**
	 * 设置 '{@code onmouseout}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnmouseout(String onmouseout) {
		this.onmouseout = onmouseout;
	}
	/**
	 * 获取 '{@code onmouseout}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOnmouseout() {
		return this.onmouseout;
	}

	/**
	 * 设置 '{@code onkeypress}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnkeypress(String onkeypress) {
		this.onkeypress = onkeypress;
	}

	/**
	 * 获取 '{@code onkeypress}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOnkeypress() {
		return this.onkeypress;
	}

	/**
	 * 设置 '{@code onkeyup}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnkeyup(String onkeyup) {
		this.onkeyup = onkeyup;
	}

	/**
	 * 获取 '{@code onkeyup}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOnkeyup() {
		return this.onkeyup;
	}

	/**
	 * 设置 '{@code onkeydown}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setOnkeydown(String onkeydown) {
		this.onkeydown = onkeydown;
	}

	/**
	 * 获取 '{@code onkeydown}' 属性的值。
	 * 可以是运行时表达式。
	 */
	@Nullable
	protected String getOnkeydown() {
		return this.onkeydown;
	}

	/**
	 * 获取动态属性的 map。
	 */
	@Nullable
	protected Map<String, Object> getDynamicAttributes() {
		return this.dynamicAttributes;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
		if (this.dynamicAttributes == null) {
			this.dynamicAttributes = new HashMap<>();
		}
		if (!isValidDynamicAttribute(localName, value)) {
			throw new IllegalArgumentException(
					"Attribute " + localName + "=\"" + value + "\" is not allowed");
		}
		this.dynamicAttributes.put(localName, value);
	}

	/**
	 * 给定的名称-值对是否为有效的动态属性。
	 */
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return true;
	}

	/**
	 * 将在此基类中配置的默认属性写入提供的 {@link TagWriter}。
	 * 当子类希望将基础属性集写入输出时，应调用此方法。
	 */
	@Override
	protected void writeDefaultAttributes(TagWriter tagWriter) throws JspException {
		super.writeDefaultAttributes(tagWriter);
		writeOptionalAttributes(tagWriter);
	}

	/**
	 * 将在此基类中配置的可选属性写入提供的 {@link TagWriter}。
	 * 将要渲染的可选属性集包括任何非标准动态属性。
	 * 由 {@link #writeDefaultAttributes(TagWriter)} 调用。
	 */
	protected void writeOptionalAttributes(TagWriter tagWriter) throws JspException {
		tagWriter.writeOptionalAttributeValue(CLASS_ATTRIBUTE, resolveCssClass());
		tagWriter.writeOptionalAttributeValue(STYLE_ATTRIBUTE,
				ObjectUtils.getDisplayString(evaluate("cssStyle", getCssStyle())));
		writeOptionalAttribute(tagWriter, LANG_ATTRIBUTE, getLang());
		writeOptionalAttribute(tagWriter, TITLE_ATTRIBUTE, getTitle());
		writeOptionalAttribute(tagWriter, DIR_ATTRIBUTE, getDir());
		writeOptionalAttribute(tagWriter, TABINDEX_ATTRIBUTE, getTabindex());
		writeOptionalAttribute(tagWriter, ONCLICK_ATTRIBUTE, getOnclick());
		writeOptionalAttribute(tagWriter, ONDBLCLICK_ATTRIBUTE, getOndblclick());
		writeOptionalAttribute(tagWriter, ONMOUSEDOWN_ATTRIBUTE, getOnmousedown());
		writeOptionalAttribute(tagWriter, ONMOUSEUP_ATTRIBUTE, getOnmouseup());
		writeOptionalAttribute(tagWriter, ONMOUSEOVER_ATTRIBUTE, getOnmouseover());
		writeOptionalAttribute(tagWriter, ONMOUSEMOVE_ATTRIBUTE, getOnmousemove());
		writeOptionalAttribute(tagWriter, ONMOUSEOUT_ATTRIBUTE, getOnmouseout());
		writeOptionalAttribute(tagWriter, ONKEYPRESS_ATTRIBUTE, getOnkeypress());
		writeOptionalAttribute(tagWriter, ONKEYUP_ATTRIBUTE, getOnkeyup());
		writeOptionalAttribute(tagWriter, ONKEYDOWN_ATTRIBUTE, getOnkeydown());

		if (!CollectionUtils.isEmpty(this.dynamicAttributes)) {
			for (Map.Entry<String, Object> entry : this.dynamicAttributes.entrySet()) {
				tagWriter.writeOptionalAttributeValue(entry.getKey(), getDisplayString(entry.getValue()));
			}
		}
	}

	/**
	 * 根据当前 {@link org.springframework.web.servlet.support.BindStatus} 对象的状态，
	 * 获取要使用的适当 CSS class。
	 */
	protected String resolveCssClass() throws JspException {
		if (getBindStatus().isError() && StringUtils.hasText(getCssErrorClass())) {
			return ObjectUtils.getDisplayString(evaluate("cssErrorClass", getCssErrorClass()));
		}
		else {
			return ObjectUtils.getDisplayString(evaluate("cssClass", getCssClass()));
		}
	}

}
