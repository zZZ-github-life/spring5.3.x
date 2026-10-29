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

package org.springframework.web.servlet.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

/**
 * {@code <message>} 标签在当前页面的作用域内查找消息。
 * 消息通过 ApplicationContext 解析，因此支持
 * 国际化。
 *
 * <p>会检测 HTML 转义设置，该设置可以在此标签实例上、页面级
 * 或 {@code web.xml} 级指定。也可以应用 JavaScript 转义。
 *
 * <p>如果 "code" 未设置或无法解析，则将 "text" 用作默认
 * 消息。因此，此标签也可用于对任意文本进行 HTML 转义。
 *
 * <p>消息参数可以通过 {@link #setArguments(Object) arguments}
 * 属性指定，也可以通过嵌套的 {@code <spring:argument>} 标签指定。
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
 * <td>arguments</td>
 * <td>false</td>
 * <td>true</td>
 * <td>为此标签设置可选的消息参数，可以是（逗号）分隔的
 * String（每个 String 参数可包含 JSP EL）、Object 数组（用作
 * 参数数组），或单个 Object（用作单个参数）。</td>
 * </tr>
 * <tr>
 * <td>argumentSeparator</td>
 * <td>false</td>
 * <td>true</td>
 * <td>用于拆分 arguments 字符串值的分隔符；
 * 默认为 'comma'（','）。</td>
 * </tr>
 * <tr>
 * <td>code</td>
 * <td>false</td>
 * <td>true</td>
 * <td>查找消息时使用的代码（键）。
 * 如果未提供 code，则将使用 text 属性。</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 HTML 转义。
 * 覆盖当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 JavaScript 转义。
 * 默认为 {@code false}。</td>
 * </tr>
 * <tr>
 * <td>message</td>
 * <td>false</td>
 * <td>true</td>
 * <td>一个 MessageSourceResolvable 参数（直接指定或通过 JSP EL）。
 * 与 Spring 自带的验证错误类配合使用时非常合适，这些类
 * 都实现了 MessageSourceResolvable 接口。
 * 例如，这允许你遍历表单中的所有错误，
 * 将每个错误（使用运行时表达式）作为该 'message' 属性的值传入，
 * 从而方便地显示这类错误
 * 消息。</td>
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
 * <td>text</td>
 * <td>false</td>
 * <td>true</td>
 * <td>当找不到给定代码对应的消息时输出的默认文本。
 * 如果 text 和 code 都未设置，该标签将输出 null。</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>将结果绑定到 page、request、session 或 application 作用域时
 * 使用的字符串。如果未指定，结果将输出到 writer
 * （即通常直接输出到 JSP）。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @see #setCode
 * @see #setText
 * @see #setHtmlEscape
 * @see #setJavaScriptEscape
 * @see HtmlEscapeTag#setDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#HTML_ESCAPE_CONTEXT_PARAM
 * @see ArgumentTag
 */
@SuppressWarnings("serial")
public class MessageTag extends HtmlEscapingAwareTag implements ArgumentAware {

	/**
	 * 拆分 arguments 字符串时使用的默认分隔符：逗号（","）。
	 */
	public static final String DEFAULT_ARGUMENT_SEPARATOR = ",";


	@Nullable
	private MessageSourceResolvable message;

	@Nullable
	private String code;

	@Nullable
	private Object arguments;

	private String argumentSeparator = DEFAULT_ARGUMENT_SEPARATOR;

	private List<Object> nestedArguments = Collections.emptyList();

	@Nullable
	private String text;

	@Nullable
	private String var;

	private String scope = TagUtils.SCOPE_PAGE;

	private boolean javaScriptEscape = false;


	/**
	 * 为此标签设置 MessageSourceResolvable。
	 * <p>如果指定了 MessageSourceResolvable，它将实际覆盖
	 * 此标签上指定的任何 code、arguments 或 text。
	 */
	public void setMessage(MessageSourceResolvable message) {
		this.message = message;
	}

	/**
	 * 为此标签设置消息代码。
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * 为此标签设置可选的消息参数，可以是逗号分隔的
	 * String（每个 String 参数可包含 JSP EL）、Object 数组
	 * （用作参数数组），或单个 Object（用作单个参数）。
	 */
	public void setArguments(Object arguments) {
		this.arguments = arguments;
	}

	/**
	 * 设置用于拆分 arguments 字符串的分隔符。
	 * 默认为逗号（","）。
	 * @see #setArguments
	 */
	public void setArgumentSeparator(String argumentSeparator) {
		this.argumentSeparator = argumentSeparator;
	}

	@Override
	public void addArgument(@Nullable Object argument) throws JspTagException {
		this.nestedArguments.add(argument);
	}

	/**
	 * 为此标签设置消息文本。
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * 设置 PageContext 属性名称，用于公开
	 * 包含已解析消息的变量。
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

	/**
	 * 以布尔值形式为该标签设置 JavaScript 转义。
	 * 默认为 "false"。
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	protected final int doStartTagInternal() throws JspException, IOException {
		this.nestedArguments = new ArrayList<>();
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 解析消息，按需对其进行转义，
	 * 并将其写入页面（或将其作为变量公开）。
	 * @see #resolveMessage()
	 * @see org.springframework.web.util.HtmlUtils#htmlEscape(String)
	 * @see org.springframework.web.util.JavaScriptUtils#javaScriptEscape(String)
	 * @see #writeMessage(String)
	 */
	@Override
	public int doEndTag() throws JspException {
		try {
			// Resolve the unescaped message.
			String msg = resolveMessage();

			// HTML and/or JavaScript escape, if demanded.
			msg = htmlEscape(msg);
			msg = this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(msg) : msg;

			// Expose as variable, if demanded, else write to the page.
			if (this.var != null) {
				this.pageContext.setAttribute(this.var, msg, TagUtils.getScope(this.scope));
			}
			else {
				writeMessage(msg);
			}

			return EVAL_PAGE;
		}
		catch (IOException ex) {
			throw new JspTagException(ex.getMessage(), ex);
		}
		catch (NoSuchMessageException ex) {
			throw new JspTagException(getNoSuchMessageExceptionDescription(ex));
		}
	}

	@Override
	public void release() {
		super.release();
		this.arguments = null;
	}


	/**
	 * 将指定的消息解析为具体的消息字符串。
	 * 返回的消息字符串应为未转义的形式。
	 */
	protected String resolveMessage() throws JspException, NoSuchMessageException {
		MessageSource messageSource = getMessageSource();

		// Evaluate the specified MessageSourceResolvable, if any.
		if (this.message != null) {
			// We have a given MessageSourceResolvable.
			return messageSource.getMessage(this.message, getRequestContext().getLocale());
		}

		if (this.code != null || this.text != null) {
			// We have a code or default text that we need to resolve.
			Object[] argumentsArray = resolveArguments(this.arguments);
			if (!this.nestedArguments.isEmpty()) {
				argumentsArray = appendArguments(argumentsArray, this.nestedArguments.toArray());
			}

			if (this.text != null) {
				// We have a fallback text to consider.
				String msg = messageSource.getMessage(
						this.code, argumentsArray, this.text, getRequestContext().getLocale());
				return (msg != null ? msg : "");
			}
			else {
				// We have no fallback text to consider.
				return messageSource.getMessage(
						this.code, argumentsArray, getRequestContext().getLocale());
			}
		}

		throw new JspTagException("No resolvable message");
	}

	private Object[] appendArguments(@Nullable Object[] sourceArguments, Object[] additionalArguments) {
		if (ObjectUtils.isEmpty(sourceArguments)) {
			return additionalArguments;
		}
		Object[] arguments = new Object[sourceArguments.length + additionalArguments.length];
		System.arraycopy(sourceArguments, 0, arguments, 0, sourceArguments.length);
		System.arraycopy(additionalArguments, 0, arguments, sourceArguments.length, additionalArguments.length);
		return arguments;
	}

	/**
	 * 将给定的 arguments Object 解析为 arguments 数组。
	 * @param arguments the specified arguments Object
	 * @return the resolved arguments as array
	 * @throws JspException if argument conversion failed
	 * @see #setArguments
	 */
	@Nullable
	protected Object[] resolveArguments(@Nullable Object arguments) throws JspException {
		if (arguments instanceof String) {
			return StringUtils.delimitedListToStringArray((String) arguments, this.argumentSeparator);
		}
		else if (arguments instanceof Object[]) {
			return (Object[]) arguments;
		}
		else if (arguments instanceof Collection) {
			return ((Collection<?>) arguments).toArray();
		}
		else if (arguments != null) {
			// Assume a single argument object.
			return new Object[] {arguments};
		}
		else {
			return null;
		}
	}

	/**
	 * 将消息写入页面。
	 * <p>可在子类中重写，例如用于测试目的。
	 * @param msg the message to write
	 * @throws IOException if writing failed
	 */
	protected void writeMessage(String msg) throws IOException {
		this.pageContext.getOut().write(msg);
	}

	/**
	 * 使用当前 RequestContext 的应用上下文作为 MessageSource。
	 */
	protected MessageSource getMessageSource() {
		return getRequestContext().getMessageSource();
	}

	/**
	 * 返回默认的异常消息。
	 */
	protected String getNoSuchMessageExceptionDescription(NoSuchMessageException ex) {
		return ex.getMessage();
	}

}
