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

package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.springframework.lang.Nullable;

/**
 * {@code <param>} 标签收集名称-值参数，并将它们传递给标签层次结构中
 * 的 {@link ParamAware} 祖先标签。
 *
 * <p>此标签必须嵌套在支持参数的标签内部。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需？</th>
 * <th>是否支持运行时表达式？</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>name</td>
 * <td>true</td>
 * <td>true</td>
 * <td>参数的名称。</td>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>false</td>
 * <td>true</td>
 * <td>参数的值。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Scott Andrews
 * @author Nicholas Williams
 * @since 3.0
 * @see Param
 * @see UrlTag
 */
@SuppressWarnings("serial")
public class ParamTag extends BodyTagSupport {

	private String name = "";

	@Nullable
	private String value;

	private boolean valueSet;


	/**
	 * 设置参数的名称（必需）。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 设置参数的值（可选）。
	 */
	public void setValue(String value) {
		this.value = value;
		this.valueSet = true;
	}


	@Override
	public int doEndTag() throws JspException {
		Param param = new Param();
		param.setName(this.name);
		if (this.valueSet) {
			param.setValue(this.value);
		}
		else if (getBodyContent() != null) {
			// Get the value from the tag body
			param.setValue(getBodyContent().getString().trim());
		}

		// Find a param aware ancestor
		ParamAware paramAwareTag = (ParamAware) findAncestorWithClass(this, ParamAware.class);
		if (paramAwareTag == null) {
			throw new JspException("The param tag must be a descendant of a tag that supports parameters");
		}

		paramAwareTag.addParam(param);

		return EVAL_PAGE;
	}

	@Override
	public void release() {
		super.release();
		this.name = "";
		this.value = null;
		this.valueSet = false;
	}

}
