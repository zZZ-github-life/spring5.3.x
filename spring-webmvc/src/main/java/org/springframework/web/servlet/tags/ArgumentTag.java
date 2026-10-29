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
 * {@code <argument>} 标签基于 JSTL 的 {@code fmt:param} 标签。
 * 其目的在于支持 message 标签和 theme 标签中的参数。
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
 * <td>value</td>
 * <td>false</td>
 * <td>true</td>
 * <td>参数的值。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see MessageTag
 * @see ThemeTag
 */
@SuppressWarnings("serial")
public class ArgumentTag extends BodyTagSupport {

	@Nullable
	private Object value;

	private boolean valueSet;


	/**
	 * 设置参数的值（可选）。
	 * 如果未设置，则会对标签的正文内容求值。
	 * @param value the parameter value
	 */
	public void setValue(Object value) {
		this.value = value;
		this.valueSet = true;
	}


	@Override
	public int doEndTag() throws JspException {
		Object argument = null;
		if (this.valueSet) {
			argument = this.value;
		}
		else if (getBodyContent() != null) {
			// Get the value from the tag body
			argument = getBodyContent().getString().trim();
		}

		// Find a param-aware ancestor
		ArgumentAware argumentAwareTag = (ArgumentAware) findAncestorWithClass(this, ArgumentAware.class);
		if (argumentAwareTag == null) {
			throw new JspException("The argument tag must be a descendant of a tag that supports arguments");
		}
		argumentAwareTag.addArgument(argument);

		return EVAL_PAGE;
	}

	@Override
	public void release() {
		super.release();
		this.value = null;
		this.valueSet = false;
	}

}
