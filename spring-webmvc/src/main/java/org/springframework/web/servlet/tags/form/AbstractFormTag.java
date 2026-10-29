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

import java.beans.PropertyEditor;

import javax.servlet.jsp.JspException;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;

/**
 * 所有 JSP 表单标签的基类。提供用于空值安全的 EL 求值
 * 以及访问和操作 {@link TagWriter} 的工具方法。
 *
 * <p>子类应实现 {@link #writeTagContent(TagWriter)} 来执行
 * 实际的标签渲染。
 *
 * <p>子类（或测试类）可以覆盖 {@link #createTagWriter()} 方法，
 * 将输出重定向到与当前 {@link javax.servlet.jsp.PageContext} 关联的
 * {@link javax.servlet.jsp.JspWriter} 之外的 {@link java.io.Writer}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractFormTag extends HtmlEscapingAwareTag {

	/**
	 * 对给定属性名称的提供值进行求值。
	 * <p>默认实现仅按原样返回给定值。
	 */
	@Nullable
	protected Object evaluate(String attributeName, @Nullable Object value) throws JspException {
		return value;
	}

	/**
	 * 可选地将提供值以给定属性名称写入提供的
	 * {@link TagWriter}。在这种情况下，先对提供值进行 {@link #evaluate 求值}，
	 * 然后将 {@link ObjectUtils#getDisplayString 字符串表示}写入
	 * 属性值。如果得到的 {@code String} 表示为 {@code null}
	 * 或为空，则不写入任何属性。
	 * @see TagWriter#writeOptionalAttributeValue(String, String)
	 */
	protected final void writeOptionalAttribute(TagWriter tagWriter, String attributeName, @Nullable String value)
			throws JspException {

		if (value != null) {
			tagWriter.writeOptionalAttributeValue(attributeName, getDisplayString(evaluate(attributeName, value)));
		}
	}

	/**
	 * 创建用于写入所有输出的 {@link TagWriter}。默认情况下，
	 * {@link TagWriter} 将输出写入当前 {@link javax.servlet.jsp.PageContext}
	 * 的 {@link javax.servlet.jsp.JspWriter}。子类可以选择
	 * 更改实际写入输出的 {@link java.io.Writer}。
	 */
	protected TagWriter createTagWriter() {
		return new TagWriter(this.pageContext);
	}

	/**
	 * 提供一个简单的模板方法，它调用 {@link #createTagWriter()}，
	 * 并将创建的 {@link TagWriter} 传递给 {@link #writeTagContent(TagWriter)} 方法。
	 * @return the value returned by {@link #writeTagContent(TagWriter)}
	 */
	@Override
	protected final int doStartTagInternal() throws Exception {
		return writeTagContent(createTagWriter());
	}

	/**
	 * 获取提供 {@code Object} 的显示值，并按需进行 HTML 转义。
	 * 此版本<strong>不</strong>感知 {@link PropertyEditor}。
	 */
	protected String getDisplayString(@Nullable Object value) {
		return ValueFormatter.getDisplayString(value, isHtmlEscape());
	}

	/**
	 * 获取提供 {@code Object} 的显示值，并按需进行 HTML 转义。
	 * 如果提供的值不是 {@link String} 且提供的
	 * {@link PropertyEditor} 不为 null，则使用 {@link PropertyEditor}
	 * 获取显示值。
	 */
	protected String getDisplayString(@Nullable Object value, @Nullable PropertyEditor propertyEditor) {
		return ValueFormatter.getDisplayString(value, propertyEditor, isHtmlEscape());
	}

	/**
	 * 重写为在没有给出显式默认值时默认为 {@code true}。
	 */
	@Override
	protected boolean isDefaultHtmlEscape() {
		Boolean defaultHtmlEscape = getRequestContext().getDefaultHtmlEscape();
		return (defaultHtmlEscape == null || defaultHtmlEscape.booleanValue());
	}


	/**
	 * 子类应实现此方法以执行标签内容渲染。
	 * @return valid tag render instruction as per {@link javax.servlet.jsp.tagext.Tag#doStartTag()}.
	 */
	protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

}
