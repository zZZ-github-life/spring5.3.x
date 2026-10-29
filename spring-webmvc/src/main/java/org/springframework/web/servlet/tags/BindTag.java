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

import java.beans.PropertyEditor;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.BindStatus;

/**
 * {@code <bind>} 标签支持对某个 bean 或 bean 属性的绑定错误进行求值。
 * 它向 Java 表达式和 JSP EL 表达式公开一个类型为
 * {@link org.springframework.web.servlet.support.BindStatus} 的 "status" 变量。
 *
 * <p>可用于绑定到模型中的任意 bean 或 bean 属性。
 * 指定的路径决定该标签公开的是 bean 本身的状态（显示对象级错误）、
 * 特定 bean 属性的状态（显示字段错误），还是
 * 一组匹配的 bean 属性的状态（显示所有对应的字段错误）。
 *
 * <p>通过此标签绑定的 {@link org.springframework.validation.Errors} 对象
 * 会公开给协作的标签，同时公开的还有该 errors 对象所适用的 bean 属性。
 * 诸如 {@link TransformTag} 之类的嵌套标签可以访问这些公开的属性。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">属性</th>
 * <th class="colOne">是否必需？</th>
 * <th class="colOne">是否支持运行时表达式？</th>
 * <th class="colLast">描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>以布尔值形式为该标签设置 HTML 转义。覆盖当前页面的
 * 默认 HTML 转义设置。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>ignoreNestedPath</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>设置是否忽略嵌套路径（如果存在）。
 * 默认不忽略。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>path</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>要为其绑定状态信息的 bean 或 bean 属性的路径。
 * 例如 account.name、company.address.zipCode，或仅 employee。状态对象
 * 将被导出到页面作用域中，专门用于该 bean 或 bean
 * 属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setPath
 */
@SuppressWarnings("serial")
public class BindTag extends HtmlEscapingAwareTag implements EditorAwareTag {

	/**
	 * 此标签所公开变量在本标签作用域内的名称："status"。
	 */
	public static final String STATUS_VARIABLE_NAME = "status";


	private String path = "";

	private boolean ignoreNestedPath = false;

	@Nullable
	private BindStatus status;

	@Nullable
	private Object previousPageStatus;

	@Nullable
	private Object previousRequestStatus;


	/**
	 * 设置此标签所应用的路径。可以是 bean（如 "person"）以获取
	 * 全局错误，也可以是 bean 属性（如 "person.name"）以获取
	 * 字段错误（同样支持嵌套字段和 "person.na*" 形式的映射）。
	 * "person.*" 将返回指定 bean 的所有错误，包括全局错误
	 * 和字段错误。
	 * @see org.springframework.validation.Errors#getGlobalErrors
	 * @see org.springframework.validation.Errors#getFieldErrors
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * 返回此标签所应用的路径。
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * 设置是否忽略嵌套路径（如果存在）。
	 * 默认不忽略。
	 */
	public void setIgnoreNestedPath(boolean ignoreNestedPath) {
		this.ignoreNestedPath = ignoreNestedPath;
	}

	/**
	 * 返回是否忽略嵌套路径（如果存在）。
	 */
	public boolean isIgnoreNestedPath() {
		return this.ignoreNestedPath;
	}


	@Override
	protected final int doStartTagInternal() throws Exception {
		String resolvedPath = getPath();
		if (!isIgnoreNestedPath()) {
			String nestedPath = (String) this.pageContext.getAttribute(
					NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
			// only prepend if not already an absolute path
			if (nestedPath != null && !resolvedPath.startsWith(nestedPath) &&
					!resolvedPath.equals(nestedPath.substring(0, nestedPath.length() - 1))) {
				resolvedPath = nestedPath + resolvedPath;
			}
		}

		try {
			this.status = new BindStatus(getRequestContext(), resolvedPath, isHtmlEscape());
		}
		catch (IllegalStateException ex) {
			throw new JspTagException(ex.getMessage());
		}

		// Save previous status values, for re-exposure at the end of this tag.
		this.previousPageStatus = this.pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
		this.previousRequestStatus = this.pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);

		// Expose this tag's status object as PageContext attribute,
		// making it available for JSP EL.
		this.pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
		this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.status, PageContext.REQUEST_SCOPE);

		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() {
		// Reset previous status values.
		if (this.previousPageStatus != null) {
			this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousPageStatus, PageContext.PAGE_SCOPE);
		}
		if (this.previousRequestStatus != null) {
			this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousRequestStatus, PageContext.REQUEST_SCOPE);
		}
		else {
			this.pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		return EVAL_PAGE;
	}


	/**
	 * 返回当前的 BindStatus。
	 */
	private BindStatus getStatus() {
		Assert.state(this.status != null, "No current BindStatus");
		return this.status;
	}

	/**
	 * 获取此标签当前所绑定的属性，如果绑定的是对象而非
	 * 特定属性，则返回 {@code null}。
	 * 供协作的嵌套标签使用。
	 * @return the property that this tag is currently bound to,
	 * or {@code null} if none
	 */
	@Nullable
	public final String getProperty() {
		return getStatus().getExpression();
	}

	/**
	 * 获取此标签当前所绑定的 Errors 实例。
	 * 供协作的嵌套标签使用。
	 * @return the current Errors instance, or {@code null} if none
	 */
	@Nullable
	public final Errors getErrors() {
		return getStatus().getErrors();
	}

	@Override
	@Nullable
	public final PropertyEditor getEditor() {
		return getStatus().getEditor();
	}


	@Override
	public void doFinally() {
		super.doFinally();
		this.status = null;
		this.previousPageStatus = null;
		this.previousRequestStatus = null;
	}

}
