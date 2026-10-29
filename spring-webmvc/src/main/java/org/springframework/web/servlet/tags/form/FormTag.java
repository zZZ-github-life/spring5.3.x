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

import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.beans.PropertyAccessor;
import org.springframework.core.Conventions;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

/**
 * {@code <form>} 标签渲染 HTML 'form' 标签，并向内部标签暴露用于绑定的绑定路径。
 *
 * <p>用户在为其视图填充数据时，应将表单对象放入
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView}。
 * 此表单对象的名称可以通过
 * {@link #setModelAttribute "modelAttribute"} 属性进行配置。
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
 * <td><p>acceptCharset</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>指定处理此表单的服务器所接受的输入数据字符编码列表。
 * 该值是以空格和/或逗号分隔的字符集值列表。
 * 客户端必须将此列表解释为异或列表，即服务器能够接受
 * 所接收的每个实体中的任意单一字符编码。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>action</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 必需属性</p></td>
 * </tr>
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
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>enctype</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用渲染值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>method</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>methodParam</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>用于除 GET 和 POST 之外的 HTTP 方法的参数名称。
 * 默认为 '_method'。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>modelAttribute</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>暴露表单对象所使用的 model 属性名称。
 * 默认为 'command'。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>name</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性 - 为向后兼容情况而添加</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>ondblclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onreset</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onsubmit</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>servletRelativeAction</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>要追加到当前 servlet 路径的 action 引用</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>target</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
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
 * @author Scott Andrews
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public class FormTag extends AbstractHtmlElementTag {

	/** 表单值发送到服务器时使用的默认 HTTP 方法："post"。 */
	private static final String DEFAULT_METHOD = "post";

	/** 默认属性名称：&quot;command&quot;。 */
	public static final String DEFAULT_COMMAND_NAME = "command";

	/** '{@code modelAttribute}' 设置的名称。 */
	private static final String MODEL_ATTRIBUTE = "modelAttribute";

	/**
	 * 暴露表单对象名称所使用的 {@link javax.servlet.jsp.PageContext} 属性的名称。
	 */
	public static final String MODEL_ATTRIBUTE_VARIABLE_NAME =
			Conventions.getQualifiedAttributeName(AbstractFormTag.class, MODEL_ATTRIBUTE);

	/** 默认方法参数，即 {@code _method}。 */
	private static final String DEFAULT_METHOD_PARAM = "_method";

	private static final String FORM_TAG = "form";

	private static final String INPUT_TAG = "input";

	private static final String ACTION_ATTRIBUTE = "action";

	private static final String METHOD_ATTRIBUTE = "method";

	private static final String TARGET_ATTRIBUTE = "target";

	private static final String ENCTYPE_ATTRIBUTE = "enctype";

	private static final String ACCEPT_CHARSET_ATTRIBUTE = "accept-charset";

	private static final String ONSUBMIT_ATTRIBUTE = "onsubmit";

	private static final String ONRESET_ATTRIBUTE = "onreset";

	private static final String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";

	private static final String NAME_ATTRIBUTE = "name";

	private static final String VALUE_ATTRIBUTE = "value";

	private static final String TYPE_ATTRIBUTE = "type";


	@Nullable
	private TagWriter tagWriter;

	private String modelAttribute = DEFAULT_COMMAND_NAME;

	@Nullable
	private String name;

	@Nullable
	private String action;

	@Nullable
	private String servletRelativeAction;

	private String method = DEFAULT_METHOD;

	@Nullable
	private String target;

	@Nullable
	private String enctype;

	@Nullable
	private String acceptCharset;

	@Nullable
	private String onsubmit;

	@Nullable
	private String onreset;

	@Nullable
	private String autocomplete;

	private String methodParam = DEFAULT_METHOD_PARAM;

	/** 缓存之前的嵌套路径，以便可以重置。 */
	@Nullable
	private String previousNestedPath;


	/**
	 * 设置模型中表单属性的名称。
	 * <p>可以是运行时表达式。
	 */
	public void setModelAttribute(String modelAttribute) {
		this.modelAttribute = modelAttribute;
	}

	/**
	 * 获取模型中表单属性的名称。
	 */
	protected String getModelAttribute() {
		return this.modelAttribute;
	}

	/**
	 * 设置 '{@code name}' 属性的值。
	 * <p>可以是运行时表达式。
	 * <p>在 XHTML 1.0 中，name 不是 form 的有效属性。但是，
	 * 有时出于向后兼容的需要会使用它。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 获取 '{@code name}' 属性的值。
	 */
	@Override
	@Nullable
	protected String getName() throws JspException {
		return this.name;
	}

	/**
	 * 设置 '{@code action}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setAction(@Nullable String action) {
		this.action = (action != null ? action : "");
	}

	/**
	 * 获取 '{@code action}' 属性的值。
	 */
	@Nullable
	protected String getAction() {
		return this.action;
	}

	/**
	 * 通过一个要追加到当前 servlet 路径的值来设置
	 * '{@code action}' 属性的值。
	 * <p>可以是运行时表达式。
	 * @since 3.2.3
	 */
	public void setServletRelativeAction(@Nullable String servletRelativeAction) {
		this.servletRelativeAction = servletRelativeAction;
	}

	/**
	 * 获取 '{@code action}' 属性的 servlet 相对值。
	 * @since 3.2.3
	 */
	@Nullable
	protected String getServletRelativeAction() {
		return this.servletRelativeAction;
	}

	/**
	 * 设置 '{@code method}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * 获取 '{@code method}' 属性的值。
	 */
	protected String getMethod() {
		return this.method;
	}

	/**
	 * 设置 '{@code target}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * 获取 '{@code target}' 属性的值。
	 */
	@Nullable
	public String getTarget() {
		return this.target;
	}

	/**
	 * 设置 '{@code enctype}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setEnctype(String enctype) {
		this.enctype = enctype;
	}

	/**
	 * 获取 '{@code enctype}' 属性的值。
	 */
	@Nullable
	protected String getEnctype() {
		return this.enctype;
	}

	/**
	 * 设置 '{@code acceptCharset}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setAcceptCharset(String acceptCharset) {
		this.acceptCharset = acceptCharset;
	}

	/**
	 * 获取 '{@code acceptCharset}' 属性的值。
	 */
	@Nullable
	protected String getAcceptCharset() {
		return this.acceptCharset;
	}

	/**
	 * 设置 '{@code onsubmit}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setOnsubmit(String onsubmit) {
		this.onsubmit = onsubmit;
	}

	/**
	 * 获取 '{@code onsubmit}' 属性的值。
	 */
	@Nullable
	protected String getOnsubmit() {
		return this.onsubmit;
	}

	/**
	 * 设置 '{@code onreset}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setOnreset(String onreset) {
		this.onreset = onreset;
	}

	/**
	 * 获取 '{@code onreset}' 属性的值。
	 */
	@Nullable
	protected String getOnreset() {
		return this.onreset;
	}

	/**
	 * 设置 '{@code autocomplete}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setAutocomplete(String autocomplete) {
		this.autocomplete = autocomplete;
	}

	/**
	 * 获取 '{@code autocomplete}' 属性的值。
	 */
	@Nullable
	protected String getAutocomplete() {
		return this.autocomplete;
	}

	/**
	 * 设置非浏览器支持的 HTTP 方法所使用的请求参数名称。
	 */
	public void setMethodParam(String methodParam) {
		this.methodParam = methodParam;
	}

	/**
	 * 获取非浏览器支持的 HTTP 方法所使用的请求参数名称。
	 * @since 4.2.3
	 */
	protected String getMethodParam() {
		return this.methodParam;
	}

	/**
	 * 判断浏览器是否支持该 HTTP 方法（即 GET 或 POST）。
	 */
	protected boolean isMethodBrowserSupported(String method) {
		return ("get".equalsIgnoreCase(method) || "post".equalsIgnoreCase(method));
	}


	/**
	 * 写入块	'{@code form}' 标签的开头部分，并在
	 * {@link javax.servlet.jsp.PageContext} 中暴露表单对象名称。
	 * @param tagWriter the {@link TagWriter} to which the form content is to be written
	 * @return {@link javax.servlet.jsp.tagext.Tag#EVAL_BODY_INCLUDE}
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		this.tagWriter = tagWriter;

		tagWriter.startTag(FORM_TAG);
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute(ACTION_ATTRIBUTE, resolveAction());
		writeOptionalAttribute(tagWriter, METHOD_ATTRIBUTE, getHttpMethod());
		writeOptionalAttribute(tagWriter, TARGET_ATTRIBUTE, getTarget());
		writeOptionalAttribute(tagWriter, ENCTYPE_ATTRIBUTE, getEnctype());
		writeOptionalAttribute(tagWriter, ACCEPT_CHARSET_ATTRIBUTE, getAcceptCharset());
		writeOptionalAttribute(tagWriter, ONSUBMIT_ATTRIBUTE, getOnsubmit());
		writeOptionalAttribute(tagWriter, ONRESET_ATTRIBUTE, getOnreset());
		writeOptionalAttribute(tagWriter, AUTOCOMPLETE_ATTRIBUTE, getAutocomplete());

		tagWriter.forceBlock();

		if (!isMethodBrowserSupported(getMethod())) {
			assertHttpMethod(getMethod());
			String inputName = getMethodParam();
			String inputType = "hidden";
			tagWriter.startTag(INPUT_TAG);
			writeOptionalAttribute(tagWriter, TYPE_ATTRIBUTE, inputType);
			writeOptionalAttribute(tagWriter, NAME_ATTRIBUTE, inputName);
			writeOptionalAttribute(tagWriter, VALUE_ATTRIBUTE, processFieldValue(inputName, getMethod(), inputType));
			tagWriter.endTag();
		}

		// Expose the form object name for nested tags...
		String modelAttribute = resolveModelAttribute();
		this.pageContext.setAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, modelAttribute, PageContext.REQUEST_SCOPE);

		// Save previous nestedPath value, build and expose current nestedPath value.
		// Use request scope to expose nestedPath to included pages too.
		this.previousNestedPath =
				(String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME,
				modelAttribute + PropertyAccessor.NESTED_PROPERTY_SEPARATOR, PageContext.REQUEST_SCOPE);

		return EVAL_BODY_INCLUDE;
	}

	private String getHttpMethod() {
		return (isMethodBrowserSupported(getMethod()) ? getMethod() : DEFAULT_METHOD);
	}

	private void assertHttpMethod(String method) {
		for (HttpMethod httpMethod : HttpMethod.values()) {
			if (httpMethod.name().equalsIgnoreCase(method)) {
				return;
			}
		}
		throw new IllegalArgumentException("Invalid HTTP method: " + method);
	}

	/**
	 * 自动生成的 ID 与表单对象名称相对应。
	 */
	@Override
	protected String autogenerateId() throws JspException {
		return resolveModelAttribute();
	}

	/**
	 * {@link #evaluate 解析}并返回表单对象的名称。
	 * @throws IllegalArgumentException if the form object resolves to {@code null}
	 */
	protected String resolveModelAttribute() throws JspException {
		Object resolvedModelAttribute = evaluate(MODEL_ATTRIBUTE, getModelAttribute());
		if (resolvedModelAttribute == null) {
			throw new IllegalArgumentException(MODEL_ATTRIBUTE + " must not be null");
		}
		return (String) resolvedModelAttribute;
	}

	/**
	 * 解析 '{@code action}' 属性的值。
	 * <p>如果用户配置了 '{@code action}' 值，则使用对该值求值的结果。
	 * 如果用户配置了 '{@code servletRelativeAction}' 值，
	 * 则在该值前面加上上下文和 servlet 路径，并使用结果。
	 * 否则，使用
	 * {@link org.springframework.web.servlet.support.RequestContext#getRequestUri()
	 * originating URI}。
	 * @return the value that is to be used for the '{@code action}' attribute
	 */
	protected String resolveAction() throws JspException {
		String action = getAction();
		String servletRelativeAction = getServletRelativeAction();
		if (StringUtils.hasText(action)) {
			action = getDisplayString(evaluate(ACTION_ATTRIBUTE, action));
			return processAction(action);
		}
		else if (StringUtils.hasText(servletRelativeAction)) {
			String pathToServlet = getRequestContext().getPathToServlet();
			if (servletRelativeAction.startsWith("/") &&
					!servletRelativeAction.startsWith(getRequestContext().getContextPath())) {
				servletRelativeAction = pathToServlet + servletRelativeAction;
			}
			servletRelativeAction = getDisplayString(evaluate(ACTION_ATTRIBUTE, servletRelativeAction));
			return processAction(servletRelativeAction);
		}
		else {
			String requestUri = getRequestContext().getRequestUri();
			String encoding = this.pageContext.getResponse().getCharacterEncoding();
			try {
				requestUri = UriUtils.encodePath(requestUri, encoding);
			}
			catch (UnsupportedCharsetException ex) {
				// shouldn't happen - if it does, proceed with requestUri as-is
			}
			ServletResponse response = this.pageContext.getResponse();
			if (response instanceof HttpServletResponse) {
				requestUri = ((HttpServletResponse) response).encodeURL(requestUri);
				String queryString = getRequestContext().getQueryString();
				if (StringUtils.hasText(queryString)) {
					requestUri += "?" + HtmlUtils.htmlEscape(queryString);
				}
			}
			if (StringUtils.hasText(requestUri)) {
				return processAction(requestUri);
			}
			else {
				throw new IllegalArgumentException("Attribute 'action' is required. " +
						"Attempted to resolve against current request URI but request URI was null.");
			}
		}
	}

	/**
	 * 如果配置了 {@link RequestDataValueProcessor} 实例，则通过它处理 action，
	 * 否则返回未修改的 action。
	 */
	private String processAction(String action) {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if (processor != null && request instanceof HttpServletRequest) {
			action = processor.processAction((HttpServletRequest) request, action, getHttpMethod());
		}
		return action;
	}

	/**
	 * 关闭 '{@code form}' 块标签，并从
	 * {@link javax.servlet.jsp.PageContext} 中移除表单对象名称。
	 */
	@Override
	public int doEndTag() throws JspException {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if (processor != null && request instanceof HttpServletRequest) {
			writeHiddenFields(processor.getExtraHiddenFields((HttpServletRequest) request));
		}
		Assert.state(this.tagWriter != null, "No TagWriter set");
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

	/**
	 * 将给定值作为隐藏字段写入。
	 */
	private void writeHiddenFields(@Nullable Map<String, String> hiddenFields) throws JspException {
		if (!CollectionUtils.isEmpty(hiddenFields)) {
			Assert.state(this.tagWriter != null, "No TagWriter set");
			this.tagWriter.appendValue("<div>\n");
			for (Map.Entry<String, String> entry : hiddenFields.entrySet()) {
				this.tagWriter.appendValue("<input type=\"hidden\" ");
				this.tagWriter.appendValue("name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" ");
				this.tagWriter.appendValue("/>\n");
			}
			this.tagWriter.appendValue("</div>");
		}
	}

	/**
	 * 清空存储的 {@link TagWriter}。
	 */
	@Override
	public void doFinally() {
		super.doFinally();

		this.pageContext.removeAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		if (this.previousNestedPath != null) {
			// Expose previous nestedPath value.
			this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
		}
		else {
			// Remove exposed nestedPath value.
			this.pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		this.tagWriter = null;
		this.previousNestedPath = null;
	}


	/**
	 * 覆盖解析 CSS class，因为不支持 error class。
	 */
	@Override
	protected String resolveCssClass() throws JspException {
		return ObjectUtils.getDisplayString(evaluate("cssClass", getCssClass()));
	}

	/**
	 * 不支持 form。
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setPath(String path) {
		throw new UnsupportedOperationException("The 'path' attribute is not supported for forms");
	}

	/**
	 * 不支持 form。
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setCssErrorClass(String cssErrorClass) {
		throw new UnsupportedOperationException("The 'cssErrorClass' attribute is not supported for forms");
	}

}
