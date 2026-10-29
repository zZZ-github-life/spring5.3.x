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

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;

/**
 * 在发生绑定错误时，{@code <hasBindErrors>} 标签会提供一个 {@link Errors} 实例。
 * HTML 转义标志会参与页面级或应用级的设置（即通过 HtmlEscapeTag 或
 * web.xml 中的 "defaultHtmlEscape" 上下文参数）。
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
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 HTML 转义。
 * 覆盖当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>name</td>
 * <td>true</td>
 * <td>true</td>
 * <td>请求中需要检查错误的 bean 的名称。
 * 如果该 bean 存在错误，它们将绑定在
 * 'errors' 键下。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BindTag
 * @see org.springframework.validation.Errors
 */
@SuppressWarnings("serial")
public class BindErrorsTag extends HtmlEscapingAwareTag {

	/**
	 * 存放 {@link Errors} 的页面上下文属性。
	 */
	public static final String ERRORS_VARIABLE_NAME = "errors";


	private String name = "";

	@Nullable
	private Errors errors;


	/**
	 * 设置此标签应检查的 bean 的名称。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回此标签所检查的 bean 的名称。
	 */
	public String getName() {
		return this.name;
	}


	@Override
	protected final int doStartTagInternal() throws ServletException, JspException {
		this.errors = getRequestContext().getErrors(this.name, isHtmlEscape());
		if (this.errors != null && this.errors.hasErrors()) {
			this.pageContext.setAttribute(ERRORS_VARIABLE_NAME, this.errors, PageContext.REQUEST_SCOPE);
			return EVAL_BODY_INCLUDE;
		}
		else {
			return SKIP_BODY;
		}
	}

	@Override
	public int doEndTag() {
		this.pageContext.removeAttribute(ERRORS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		return EVAL_PAGE;
	}

	/**
	 * 获取此标签当前所绑定的 Errors 实例。
	 * <p>该实例供协作的嵌套标签使用。
	 */
	@Nullable
	public final Errors getErrors() {
		return this.errors;
	}


	@Override
	public void doFinally() {
		super.doFinally();
		this.errors = null;
	}

}
