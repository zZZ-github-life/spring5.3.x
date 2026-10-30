/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code <errors>} 标签在 HTML 'span' 标签中渲染字段错误。
 * 可显示对象或特定字段的错误。
 *
 * <p>此标签支持三种主要使用模式：
 *
 * <ol>
 *	<li>仅字段 - 将 '{@code path}' 设置为字段名称（或路径）</li>
 *	<li>仅对象错误 - 省略 '{@code path}'</li>
 *	<li>所有错误 - 将 '{@code path}' 设置为 '{@code *}'</li>
 * </ol>
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
 * <td><p>cssClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssStyle</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>delimiter</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>用于显示多条错误消息的分隔符。
 * 默认为 br 标签。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>element</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>指定用于渲染外层错误容器的 HTML 元素。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用渲染值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>ondblclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>path</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>用于数据绑定的 errors 对象路径</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>tabindex</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>title</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ErrorsTag extends AbstractHtmlElementBodyTag implements BodyTag {

	/**
	 * 此标签在 {@link PageContext#PAGE_SCOPE page context scope} 中
	 * 暴露错误消息所使用的 key。
	 */
	public static final String MESSAGES_ATTRIBUTE = "messages";

	/**
	 * HTML '{@code span}' 标签。
	 */
	public static final String SPAN_TAG = "span";


	private String element = SPAN_TAG;

	private String delimiter = "<br/>";

	/**
	 * 存储标签启动之前 'errors messages' 中已有的任何值。
	 */
	@Nullable
	private Object oldMessages;

	private boolean errorMessagesWereExposed;


	/**
	 * 设置用于渲染错误消息的 HTML 元素。
	 * <p>默认为 HTML '{@code <span/>}' 标签。
	 */
	public void setElement(String element) {
		Assert.hasText(element, "'element' cannot be null or blank");
		this.element = element;
	}

	/**
	 * 获取用于渲染错误消息的 HTML 元素。
	 */
	public String getElement() {
		return this.element;
	}

	/**
	 * 设置错误消息之间使用的分隔符。
	 * <p>默认为 HTML '{@code <br/>}' 标签。
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * 返回错误消息之间使用的分隔符。
	 */
	public String getDelimiter() {
		return this.delimiter;
	}


	/**
	 * 获取 HTML '{@code id}' 属性的值。
	 * <p>在 {@link #getPropertyPath()} 返回的值后追加 '{@code .errors}'；
	 * 如果省略了 {@code <form:errors/>} 标签的
	 * '{@code path}' 属性，则追加到 model 属性名称后。
	 * @return HTML '{@code id}' 属性的值
	 * @see #getPropertyPath()
	 */
	@Override
	protected String autogenerateId() throws JspException {
		String path = getPropertyPath();
		if (!StringUtils.hasLength(path) || "*".equals(path)) {
			path = (String) this.pageContext.getAttribute(
					FormTag.MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		return StringUtils.deleteAny(path, "[]") + ".errors";
	}

	/**
	 * 获取 HTML '{@code name}' 属性的值。
	 * <p>直接返回 {@code null}，因为 '{@code name}' 属性
	 * 不是 '{@code span}' 元素的有效属性。
	 */
	@Override
	@Nullable
	protected String getName() throws JspException {
		return null;
	}

	/**
	 * 此标签是否应进行渲染？
	 * <p>仅当配置的 {@link #setPath path} 存在错误时才渲染输出。
	 * @return 仅当所配置的 {@link #setPath path} 存在错误时才为 {@code true}
	 */
	@Override
	protected boolean shouldRender() throws JspException {
		try {
			return getBindStatus().isError();
		}
		catch (IllegalStateException ex) {
			// Neither BindingResult nor target object available.
			return false;
		}
	}

	@Override
	protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag(getElement());
		writeDefaultAttributes(tagWriter);
		String delimiter = ObjectUtils.getDisplayString(evaluate("delimiter", getDelimiter()));
		String[] errorMessages = getBindStatus().getErrorMessages();
		for (int i = 0; i < errorMessages.length; i++) {
			String errorMessage = errorMessages[i];
			if (i > 0) {
				tagWriter.appendValue(delimiter);
			}
			tagWriter.appendValue(getDisplayString(errorMessage));
		}
		tagWriter.endTag();
	}

	/**
	 * 在 {@link PageContext#PAGE_SCOPE} 中，以 {@link #MESSAGES_ATTRIBUTE this key}
	 * 暴露任何绑定状态错误消息。
	 * <p>仅当 {@link #shouldRender()} 返回 {@code true} 时调用。
	 * @see #removeAttributes()
	 */
	@Override
	protected void exposeAttributes() throws JspException {
		List<String> errorMessages = new ArrayList<>(Arrays.asList(getBindStatus().getErrorMessages()));
		this.oldMessages = this.pageContext.getAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
		this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, errorMessages, PageContext.PAGE_SCOPE);
		this.errorMessagesWereExposed = true;
	}

	/**
	 * 移除之前存储在 {@link PageContext#PAGE_SCOPE} 中
	 * {@link #MESSAGES_ATTRIBUTE this key} 下的任何绑定状态错误消息。
	 * @see #exposeAttributes()
	 */
	@Override
	protected void removeAttributes() {
		if (this.errorMessagesWereExposed) {
			if (this.oldMessages != null) {
				this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, this.oldMessages, PageContext.PAGE_SCOPE);
				this.oldMessages = null;
			}
			else {
				this.pageContext.removeAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
			}
		}
	}

}
