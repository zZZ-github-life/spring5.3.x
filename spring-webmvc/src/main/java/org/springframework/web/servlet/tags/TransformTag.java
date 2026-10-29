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
import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.lang.Nullable;
import org.springframework.web.util.TagUtils;

/**
 * {@code <transform>} 标签为 {@code spring:bind} 标签（或 Spring 表单
 * 标签库中的数据绑定表单元素标签）内的控制器及其他对象
 * 提供引用数据值的转换。
 *
 * <p>BindTag 持有一个 PropertyEditor，用于将 bean 的属性转换为
 * 可在 HTML 表单中使用的 String。此标签使用该 PropertyEditor
 * 来转换传入此标签的对象。
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
 * <td>以布尔值形式为该标签设置 HTML 转义。覆盖当前页面的默认 HTML
 * 转义设置。</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>将结果导出到变量时使用的作用域。此属性
 * 仅在同时设置 var 时使用。可能的值为 page、request、session
 * 和 application。</td>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>true</td>
 * <td>true</td>
 * <td>要转换的值。这是你希望进行转换的实际对象
 * （例如一个 Date）。使用 'spring:bind' 标签当前
 * 所用的 PropertyEditor。</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>将结果绑定到 page、request、session 或 application 作用域时
 * 使用的字符串。如果未指定，结果将输出到
 * writer（即通常直接输出到 JSP）。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @since 20.09.2003
 * @see BindTag
 */
@SuppressWarnings("serial")
public class TransformTag extends HtmlEscapingAwareTag {

	/** 要使用相应属性编辑器进行转换的值。 */
	@Nullable
	private Object value;

	/** 用于存放结果的变量。 */
	@Nullable
	private String var;

	/** 存放结果的变量所在的作用域。 */
	private String scope = TagUtils.SCOPE_PAGE;


	/**
	 * 设置要转换的值，使用外层 BindTag 中相应的 PropertyEditor
	 * 进行转换。
	 * <p>该值既可以是要转换的普通值（JSP 中硬编码的 String
	 * 值或 JSP 表达式），也可以是要执行的 JSP EL 表达式
	 * （对表达式的结果进行转换）。
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 设置 PageContext 属性名称，用于公开
	 * 包含转换结果的变量。
	 * @see #setScope
	 * @see javax.servlet.jsp.PageContext#setAttribute
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置将变量导出到的作用域。
	 * 默认为 SCOPE_PAGE（"page"）。
	 * @see #setVar
	 * @see org.springframework.web.util.TagUtils#SCOPE_PAGE
	 * @see javax.servlet.jsp.PageContext#setAttribute
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}


	@Override
	protected final int doStartTagInternal() throws JspException {
		if (this.value != null) {
			// Find the containing EditorAwareTag (e.g. BindTag), if applicable.
			EditorAwareTag tag = (EditorAwareTag) TagSupport.findAncestorWithClass(this, EditorAwareTag.class);
			if (tag == null) {
				throw new JspException("TransformTag can only be used within EditorAwareTag (e.g. BindTag)");
			}

			// OK, let's obtain the editor...
			String result = null;
			PropertyEditor editor = tag.getEditor();
			if (editor != null) {
				// If an editor was found, edit the value.
				editor.setValue(this.value);
				result = editor.getAsText();
			}
			else {
				// Else, just do a toString.
				result = this.value.toString();
			}
			result = htmlEscape(result);
			if (this.var != null) {
				this.pageContext.setAttribute(this.var, result, TagUtils.getScope(this.scope));
			}
			else {
				try {
					// Else, just print it out.
					this.pageContext.getOut().print(result);
				}
				catch (IOException ex) {
					throw new JspException(ex);
				}
			}
		}

		return SKIP_BODY;
	}

}
