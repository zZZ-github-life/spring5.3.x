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

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.servlet.tags.EditorAwareTag;
import org.springframework.web.servlet.tags.NestedPathTag;

/**
 * 所有数据绑定感知 JSP 表单标签的基类。
 *
 * <p>提供通用的 {@link #setPath path} 和 {@link #setId id} 属性。
 * 为子类提供用于访问其绑定值 {@link BindStatus} 的工具方法，
 * 以及用于与 {@link TagWriter} 交互的 {@link #writeOptionalAttribute interacting} 方法。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractDataBoundFormElementTag extends AbstractFormTag implements EditorAwareTag {

	/**
	 * 此标签作用域内暴露的路径变量的名称："nestedPath"。
	 * 与 {@link org.springframework.web.servlet.tags.NestedPathTag#NESTED_PATH_VARIABLE_NAME} 的值相同。
	 */
	protected static final String NESTED_PATH_VARIABLE_NAME = NestedPathTag.NESTED_PATH_VARIABLE_NAME;


	/**
	 * 来自 {@link FormTag#setModelAttribute 表单对象}的属性路径。
	 */
	@Nullable
	private String path;

	/**
	 * '{@code id}' 属性的值。
	 */
	@Nullable
	private String id;

	/**
	 * 此标签的 {@link BindStatus}。
	 */
	@Nullable
	private BindStatus bindStatus;


	/**
	 * 设置来自 {@link FormTag#setModelAttribute 表单对象}的属性路径。
	 * 可以是运行时表达式。
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * 获取 {@link FormTag#setModelAttribute 表单对象}的{@link #evaluate 已解析}属性路径。
	 */
	protected final String getPath() throws JspException {
		String resolvedPath = (String) evaluate("path", this.path);
		return (resolvedPath != null ? resolvedPath : "");
	}

	/**
	 * 设置 '{@code id}' 属性的值。
	 * <p>可以是运行时表达式；默认值为 {@link #getName()}。
	 * 请注意，默认值对于某些标签可能无效。
	 */
	@Override
	public void setId(@Nullable String id) {
		this.id = id;
	}

	/**
	 * 获取 '{@code id}' 属性的值。
	 */
	@Override
	@Nullable
	public String getId() {
		return this.id;
	}


	/**
	 * 将默认属性集写入提供的 {@link TagWriter}。
	 * 此外，抽象子类应覆盖此方法以添加任何额外的默认属性，
	 * 但<strong>必须</strong>记得调用 {@code super} 方法。
	 * <p>具体子类在希望渲染默认属性时应调用此方法。
	 * @param tagWriter the {@link TagWriter} to which any attributes are to be written
	 */
	protected void writeDefaultAttributes(TagWriter tagWriter) throws JspException {
		writeOptionalAttribute(tagWriter, "id", resolveId());
		writeOptionalAttribute(tagWriter, "name", getName());
	}

	/**
	 * 确定此标签的 '{@code id}' 属性值，
	 * 如果未指定则自动生成一个。
	 * @see #getId()
	 * @see #autogenerateId()
	 */
	@Nullable
	protected String resolveId() throws JspException {
		Object id = evaluate("id", getId());
		if (id != null) {
			String idString = id.toString();
			return (StringUtils.hasText(idString) ? idString : null);
		}
		return autogenerateId();
	}

	/**
	 * 为此标签自动生成 '{@code id}' 属性值。
	 * <p>默认实现简单地委托给 {@link #getName()}，
	 * 并删除无效字符（例如 "[" 或 "]"）。
	 */
	@Nullable
	protected String autogenerateId() throws JspException {
		String name = getName();
		return (name != null ? StringUtils.deleteAny(name, "[]") : null);
	}

	/**
	 * 获取 HTML '{@code name}' 属性的值。
	 * <p>默认实现简单地委托给
	 * {@link #getPropertyPath()}，将属性路径用作名称。
	 * 大多数情况下这是可取的，因为它与服务器端对数据绑定的
	 * 期望相一致。但是，某些子类可能希望在不更改绑定路径的情况下
	 * 更改 '{@code name}' 属性的值。
	 * @return the value for the HTML '{@code name}' attribute
	 */
	@Nullable
	protected String getName() throws JspException {
		return getPropertyPath();
	}

	/**
	 * 获取此标签的 {@link BindStatus}。
	 */
	protected BindStatus getBindStatus() throws JspException {
		if (this.bindStatus == null) {
			// HTML escaping in tags is performed by the ValueFormatter class.
			String nestedPath = getNestedPath();
			String pathToUse = (nestedPath != null ? nestedPath + getPath() : getPath());
			if (pathToUse.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
				pathToUse = pathToUse.substring(0, pathToUse.length() - 1);
			}
			this.bindStatus = new BindStatus(getRequestContext(), pathToUse, false);
		}
		return this.bindStatus;
	}

	/**
	 * 获取可能由 {@link NestedPathTag} 暴露的嵌套路径的值。
	 */
	@Nullable
	protected String getNestedPath() {
		return (String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
	}

	/**
	 * 构建此标签的属性路径，包含嵌套路径，
	 * 但<i>不</i>以表单属性的名称作为前缀。
	 * @see #getNestedPath()
	 * @see #getPath()
	 */
	protected String getPropertyPath() throws JspException {
		String expression = getBindStatus().getExpression();
		return (expression != null ? expression : "");
	}

	/**
	 * 获取绑定值。
	 * @see #getBindStatus()
	 */
	@Nullable
	protected final Object getBoundValue() throws JspException {
		return getBindStatus().getValue();
	}

	/**
	 * 获取用于此标签所绑定值的 {@link PropertyEditor}（如果有）。
	 */
	@Nullable
	protected PropertyEditor getPropertyEditor() throws JspException {
		return getBindStatus().getEditor();
	}

	/**
	 * 为 {@link EditorAwareTag} 暴露 {@link PropertyEditor}。
	 * <p>内部渲染请使用 {@link #getPropertyEditor()}。
	 */
	@Override
	@Nullable
	public final PropertyEditor getEditor() throws JspException {
		return getPropertyEditor();
	}

	/**
	 * 获取给定值的显示字符串，该值由 BindStatus 可能为其 Class
	 * 注册的 PropertyEditor 进行转换。
	 */
	protected String convertToDisplayString(@Nullable Object value) throws JspException {
		PropertyEditor editor = (value != null ? getBindStatus().findEditor(value.getClass()) : null);
		return getDisplayString(value, editor);
	}

	/**
	 * 如果配置了 {@link RequestDataValueProcessor} 实例，则通过它处理给定的
	 * 表单字段，否则返回相同的值。
	 */
	protected final String processFieldValue(@Nullable String name, String value, String type) {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if (processor != null && request instanceof HttpServletRequest) {
			value = processor.processFormFieldValue((HttpServletRequest) request, name, value, type);
		}
		return value;
	}

	/**
	 * 释放 {@link BindStatus} 实例。
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.bindStatus = null;
	}

}
