/*
 * Copyright 2002-2021 the original author or authors.
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
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;
import org.springframework.web.util.UriUtils;

/**
 * {@code <url>} 标签用于创建 URL。其设计参考了 JSTL 的 {@code c:url} 标签，
 * 并兼顾向后兼容性。
 *
 * <p>对 JSTL 功能的增强包括：
 * <ul>
 * <li>URL 编码的模板 URI 变量</li>
 * <li>URL 的 HTML/XML 转义</li>
 * <li>URL 的 JavaScript 转义</li>
 * </ul>
 *
 * <p>模板 URI 变量在 {@link #setValue(String) 'value'}
 * 属性中指定，并以花括号 '{variableName}' 标记。花括号和属性名将被
 * 由 url 标签正文中 spring:param 标签所定义参数的 URL 编码值
 * 替换。如果没有可用的参数，则直接传递字面值。
 * 与模板变量匹配的参数不会被添加到查询
 * 字符串中。
 *
 * <p>强烈建议使用 spring:param 标签来指定 URI 模板变量，
 * 而不是直接进行 EL 替换，因为只有前者会对值进行 URL 编码。未能正确
 * 编码 URL 可能使应用容易受到 XSS 及其他注入攻击。
 *
 * <p>可以通过将 {@link #setHtmlEscape(boolean)
 * 'htmlEscape'} 属性设置为 'true' 来对 URL 进行 HTML/XML 转义。会检测 HTML 转义设置，
 * 该设置可以在此标签实例上、页面级或 {@code web.xml} 级指定。默认
 * 值为 'false'。将 URL 值设置到变量中时，不建议进行转义。
 *
 * <p>用法示例：
 * <pre class="code">&lt;spring:url value="/url/path/{variableName}"&gt;
 *   &lt;spring:param name="variableName" value="more than JSTL c:url" /&gt;
 * &lt;/spring:url&gt;</pre>
 *
 * <p>上述示例的结果为：
 * {@code /currentApplicationContext/url/path/more%20than%20JSTL%20c%3Aurl}
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
 * <td>true</td>
 * <td>true</td>
 * <td>要构建的 URL。此值可以包含模板 {placeholders}，
 * 它们将被指定参数的 URL 编码值替换。参数
 * 必须使用此标签正文内的 param 标签定义。</td>
 * </tr>
 * <tr>
 * <td>context</td>
 * <td>false</td>
 * <td>true</td>
 * <td>指定远程应用上下文路径。
 * 默认为当前应用上下文路径。</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>用于导出 URL 值的变量名称。
 * 如果未指定，URL 将作为输出写出。</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>var 的作用域。支持 'application'、'session'、'request' 和 'page'
 * 作用域。默认为 page 作用域。除非同时定义了 var 属性，
 * 否则此属性
 * 无效。</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 HTML 转义。覆盖
 * 当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 JavaScript 转义。
 * 默认为 {@code false}。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Scott Andrews
 * @since 3.0
 * @see ParamTag
 */
@SuppressWarnings("serial")
public class UrlTag extends HtmlEscapingAwareTag implements ParamAware {

	private static final String URL_TEMPLATE_DELIMITER_PREFIX = "{";

	private static final String URL_TEMPLATE_DELIMITER_SUFFIX = "}";

	private static final String URL_TYPE_ABSOLUTE = "://";


	private List<Param> params = Collections.emptyList();

	private Set<String> templateParams = Collections.emptySet();

	@Nullable
	private UrlType type;

	@Nullable
	private String value;

	@Nullable
	private String context;

	@Nullable
	private String var;

	private int scope = PageContext.PAGE_SCOPE;

	private boolean javaScriptEscape = false;


	/**
	 * 设置 URL 的值。
	 */
	public void setValue(String value) {
		if (value.contains(URL_TYPE_ABSOLUTE)) {
			this.type = UrlType.ABSOLUTE;
			this.value = value;
		}
		else if (value.startsWith("/")) {
			this.type = UrlType.CONTEXT_RELATIVE;
			this.value = value;
		}
		else {
			this.type = UrlType.RELATIVE;
			this.value = value;
		}
	}

	/**
	 * 设置 URL 的上下文路径。
	 * 默认为当前上下文。
	 */
	public void setContext(String context) {
		if (context.startsWith("/")) {
			this.context = context;
		}
		else {
			this.context = "/" + context;
		}
	}

	/**
	 * 设置用于公开 URL 的变量名称。默认将 URL
	 * 渲染到当前的 JspWriter
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置将 URL 变量导出到的作用域。除非同时定义了 var，
	 * 否则此属性没有意义。
	 */
	public void setScope(String scope) {
		this.scope = TagUtils.getScope(scope);
	}

	/**
	 * 以布尔值形式为该标签设置 JavaScript 转义。
	 * 默认为 "false"。
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}

	@Override
	public void addParam(Param param) {
		this.params.add(param);
	}


	@Override
	public int doStartTagInternal() throws JspException {
		this.params = new ArrayList<>();
		this.templateParams = new HashSet<>();
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		String url = createUrl();

		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if ((processor != null) && (request instanceof HttpServletRequest)) {
			url = processor.processUrl((HttpServletRequest) request, url);
		}

		if (this.var == null) {
			// print the url to the writer
			try {
				this.pageContext.getOut().print(url);
			}
			catch (IOException ex) {
				throw new JspException(ex);
			}
		}
		else {
			// store the url as a variable
			this.pageContext.setAttribute(this.var, url, this.scope);
		}
		return EVAL_PAGE;
	}


	/**
	 * 根据标签属性和参数为此标签构建 URL。
	 * @return 以 String 形式表示的 URL 值
	 */
	String createUrl() throws JspException {
		Assert.state(this.value != null, "No value set");
		HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		HttpServletResponse response = (HttpServletResponse) this.pageContext.getResponse();

		StringBuilder url = new StringBuilder();
		if (this.type == UrlType.CONTEXT_RELATIVE) {
			// add application context to url
			if (this.context == null) {
				url.append(request.getContextPath());
			}
			else {
				if (this.context.endsWith("/")) {
					url.append(this.context, 0, this.context.length() - 1);
				}
				else {
					url.append(this.context);
				}
			}
		}
		if (this.type != UrlType.RELATIVE && this.type != UrlType.ABSOLUTE && !this.value.startsWith("/")) {
			url.append('/');
		}
		url.append(replaceUriTemplateParams(this.value, this.params, this.templateParams));
		url.append(createQueryString(this.params, this.templateParams, (url.indexOf("?") == -1)));

		String urlStr = url.toString();
		if (this.type != UrlType.ABSOLUTE) {
			// Add the session identifier if needed
			// (Do not embed the session identifier in a remote link!)
			urlStr = response.encodeURL(urlStr);
		}

		// HTML and/or JavaScript escape, if demanded.
		urlStr = htmlEscape(urlStr);
		urlStr = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(urlStr) : urlStr);

		return urlStr;
	}

	/**
	 * 从尚未作为模板参数应用的可用参数构建查询字符串。
	 * <p>参数的名称和值均经过 URL 编码。
	 * @param params 用于构建查询字符串的参数
	 * @param usedParams 已作为模板参数应用的参数名称集合
	 * @param includeQueryStringDelimiter 如果查询字符串应以 '?' 而不是 '&amp;' 开头则为 true
	 * @return 查询字符串
	 */
	protected String createQueryString(List<Param> params, Set<String> usedParams, boolean includeQueryStringDelimiter)
			throws JspException {

		String encoding = this.pageContext.getResponse().getCharacterEncoding();
		StringBuilder qs = new StringBuilder();
		for (Param param : params) {
			if (!usedParams.contains(param.getName()) && StringUtils.hasLength(param.getName())) {
				if (includeQueryStringDelimiter && qs.length() == 0) {
					qs.append('?');
				}
				else {
					qs.append('&');
				}
				try {
					qs.append(UriUtils.encodeQueryParam(param.getName(), encoding));
					if (param.getValue() != null) {
						qs.append('=');
						qs.append(UriUtils.encodeQueryParam(param.getValue(), encoding));
					}
				}
				catch (UnsupportedCharsetException ex) {
					throw new JspException(ex);
				}
			}
		}
		return qs.toString();
	}

	/**
	 * 替换 URL 中与可用参数匹配的模板标记。匹配参数的
	 * 名称会被添加到已使用参数集合中。
	 * <p>参数值经过 URL 编码。
	 * @param uri 包含待替换模板参数的 URL
	 * @param params 用于替换模板标记的参数
	 * @param usedParams 已替换的模板参数名称集合
	 * @return 替换模板参数后的 URL
	 */
	protected String replaceUriTemplateParams(String uri, List<Param> params, Set<String> usedParams)
			throws JspException {

		String encoding = this.pageContext.getResponse().getCharacterEncoding();
		for (Param param : params) {
			String template = URL_TEMPLATE_DELIMITER_PREFIX + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
			if (uri.contains(template)) {
				usedParams.add(param.getName());
				String value = param.getValue();
				try {
					uri = StringUtils.replace(uri, template,
							(value != null ? UriUtils.encodePath(value, encoding) : ""));
				}
				catch (UnsupportedCharsetException ex) {
					throw new JspException(ex);
				}
			}
			else {
				template = URL_TEMPLATE_DELIMITER_PREFIX + '/' + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
				if (uri.contains(template)) {
					usedParams.add(param.getName());
					String value = param.getValue();
					try {
						uri = StringUtils.replace(uri, template,
								(value != null ? UriUtils.encodePathSegment(value, encoding) : ""));
					}
					catch (UnsupportedCharsetException ex) {
						throw new JspException(ex);
					}
				}
			}
		}
		return uri;
	}


	/**
	 * 按类型对 URL 进行分类的内部枚举。
	 */
	private enum UrlType {

		CONTEXT_RELATIVE, RELATIVE, ABSOLUTE
	}

}
