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

package org.springframework.web.servlet.view;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

/**
 * 将请求重定向到绝对 URL、相对于上下文（context）的 URL 或相对于当前请求的 URL 的 View。
 * URL 可以是一个 URI 模板，此时 URI 模板变量将被替换为 model 中可用的值。
 * 默认情况下，所有简单类型（primitive）的 model 属性（或其集合）都会作为
 * HTTP 查询参数暴露出来（前提是它们未被用作 URI 模板变量），
 * 但可以通过重写 {@link #isEligibleProperty(String, Object)} 方法来改变此行为。
 *
 * <p>此视图的 URL 应当是一个 HTTP 重定向 URL，即适合传给 HttpServletResponse 的
 * {@code sendRedirect} 方法；如果开启了 HTTP 1.0 标志，实际的重定向就由该方法完成，
 * 或者在关闭 HTTP 1.0 兼容标志时，通过返回 HTTP 303 状态码完成重定向。
 *
 * <p>请注意：虽然 "contextRelative" 标志的默认值为关闭，但你可能几乎总是希望将其设为 true。
 * 当标志关闭时，以 "/" 开头的 URL 被视为相对于 Web 服务器根路径；
 * 当标志开启时，它们则被视为相对于 Web 应用根路径。
 * 由于大多数 Web 应用从不了解也不关心自己的 context path 究竟是什么，
 * 因此最好将此标志设为 true，并提交相对于 Web 应用根路径解释的路径。
 *
 * <p><b>在 Portlet 环境中使用此重定向视图时的注意事项：</b>请确保
 * 你的控制器遵守 Portlet {@code sendRedirect} 的约束。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Sam Brannen
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see #setContextRelative
 * @see #setHttp10Compatible
 * @see #setExposeModelAttributes
 * @see javax.servlet.http.HttpServletResponse#sendRedirect
 */
public class RedirectView extends AbstractUrlBasedView implements SmartView {

	private static final Pattern URI_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");


	private boolean contextRelative = false;

	private boolean http10Compatible = true;

	private boolean exposeModelAttributes = true;

	@Nullable
	private String encodingScheme;

	@Nullable
	private HttpStatus statusCode;

	private boolean expandUriTemplateVariables = true;

	private boolean propagateQueryParams = false;

	@Nullable
	private String[] hosts;


	/**
	 * 用于作为 bean 使用的构造器。
	 */
	public RedirectView() {
		setExposePathVariables(false);
	}

	/**
	 * 使用给定 URL 创建一个新的 RedirectView。
	 * <p>给定的 URL 将被视为相对于 Web 服务器，而不是相对于当前 ServletContext。
	 * @param url the URL to redirect to
	 * @see #RedirectView(String, boolean)
	 */
	public RedirectView(String url) {
		super(url);
		setExposePathVariables(false);
	}

	/**
	 * 使用给定 URL 创建一个新的 RedirectView。
	 * @param url the URL to redirect to
	 * @param contextRelative whether to interpret the given URL as
	 * relative to the current ServletContext
	 */
	public RedirectView(String url, boolean contextRelative) {
		super(url);
		this.contextRelative = contextRelative;
		setExposePathVariables(false);
	}

	/**
	 * 使用给定 URL 创建一个新的 RedirectView。
	 * @param url the URL to redirect to
	 * @param contextRelative whether to interpret the given URL as
	 * relative to the current ServletContext
	 * @param http10Compatible whether to stay compatible with HTTP 1.0 clients
	 */
	public RedirectView(String url, boolean contextRelative, boolean http10Compatible) {
		super(url);
		this.contextRelative = contextRelative;
		this.http10Compatible = http10Compatible;
		setExposePathVariables(false);
	}

	/**
	 * 使用给定 URL 创建一个新的 RedirectView。
	 * @param url the URL to redirect to
	 * @param contextRelative whether to interpret the given URL as
	 * relative to the current ServletContext
	 * @param http10Compatible whether to stay compatible with HTTP 1.0 clients
	 * @param exposeModelAttributes whether model attributes should be
	 * exposed as query parameters
	 */
	public RedirectView(String url, boolean contextRelative, boolean http10Compatible, boolean exposeModelAttributes) {
		super(url);
		this.contextRelative = contextRelative;
		this.http10Compatible = http10Compatible;
		this.exposeModelAttributes = exposeModelAttributes;
		setExposePathVariables(false);
	}


	/**
	 * 设置是否将以斜杠（"/"）开头的给定 URL 解释为相对于当前 ServletContext，
	 * 即相对于 Web 应用根路径。
	 * <p>默认值为 "false"：以斜杠开头的 URL 将被解释为绝对路径，即按原样使用。
	 * 如果设为 "true"，则在这种情况下会将 context path 添加到 URL 前面。
	 * @see javax.servlet.http.HttpServletRequest#getContextPath
	 */
	public void setContextRelative(boolean contextRelative) {
		this.contextRelative = contextRelative;
	}

	/**
	 * 设置是否保持与 HTTP 1.0 客户端兼容。
	 * <p>在默认实现中，无论如何都会强制使用 HTTP 状态码 302，
	 * 即委托给 {@code HttpServletResponse.sendRedirect}。
	 * 关闭此选项将发送 HTTP 状态码 303，这是适用于 HTTP 1.1 客户端的正确状态码，
	 * 但 HTTP 1.0 客户端无法理解。
	 * <p>许多 HTTP 1.1 客户端对待 302 与 303 的方式相同，并无区别。
	 * 不过，有些客户端在 POST 请求之后进行重定向时依赖 303；
	 * 在这种场景下请关闭此标志。
	 * @see javax.servlet.http.HttpServletResponse#sendRedirect
	 */
	public void setHttp10Compatible(boolean http10Compatible) {
		this.http10Compatible = http10Compatible;
	}

	/**
	 * 设置 {@code exposeModelAttributes} 标志，它表示是否将
	 * model 属性暴露为 HTTP 查询参数。
	 * <p>默认值为 {@code true}。
	 */
	public void setExposeModelAttributes(final boolean exposeModelAttributes) {
		this.exposeModelAttributes = exposeModelAttributes;
	}

	/**
	 * 设置此视图使用的编码方案。
	 * <p>默认使用请求的编码方案（如果未另行指定，则为 ISO-8859-1）。
	 */
	public void setEncodingScheme(String encodingScheme) {
		this.encodingScheme = encodingScheme;
	}

	/**
	 * 设置此视图使用的状态码。
	 * <p>默认发送 302/303，具体取决于
	 * {@link #setHttp10Compatible(boolean) http10Compatible} 标志的值。
	 */
	public void setStatusCode(HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * 是否将重定向 URL 视为 URI 模板。
	 * 如果重定向 URL 中包含左花括号 "{" 和右花括号 "}"，
	 * 而你不希望它们被解释为 URI 变量，请将此标志设为 {@code false}。
	 * <p>默认值为 {@code true}。
	 */
	public void setExpandUriTemplateVariables(boolean expandUriTemplateVariables) {
		this.expandUriTemplateVariables = expandUriTemplateVariables;
	}

	/**
	 * 当设为 {@code true} 时，当前 URL 的查询字符串将被追加，
	 * 从而传递到重定向后的 URL。
	 * <p>默认值为 {@code false}。
	 * @since 4.1
	 */
	public void setPropagateQueryParams(boolean propagateQueryParams) {
		this.propagateQueryParams = propagateQueryParams;
	}

	/**
	 * 是否传播当前 URL 的查询参数。
	 * @since 4.1
	 */
	public boolean isPropagateQueryProperties() {
		return this.propagateQueryParams;
	}

	/**
	 * 配置与应用关联的一个或多个主机。
	 * 所有其他主机都将被视为外部主机。
	 * <p>实际上，此属性提供了一种对带有主机且该主机未被列为已知主机的 URL
	 * 关闭通过 {@link HttpServletResponse#encodeRedirectURL} 进行编码的方式。
	 * <p>如果未设置（默认情况），则所有 URL 都会通过响应进行编码。
	 * @param hosts one or more application hosts
	 * @since 4.3
	 */
	public void setHosts(@Nullable String... hosts) {
		this.hosts = hosts;
	}

	/**
	 * 返回已配置的应用主机。
	 * @since 4.3
	 */
	@Nullable
	public String[] getHosts() {
		return this.hosts;
	}

	/**
	 * 返回 "true"，表示此视图执行重定向。
	 */
	@Override
	public boolean isRedirectView() {
		return true;
	}

	/**
	 * RedirectView 并不严格要求 ApplicationContext。
	 */
	@Override
	protected boolean isContextRequired() {
		return false;
	}


	/**
	 * 将 model 转换为请求参数并重定向到给定 URL。
	 * @see #appendQueryProperties
	 * @see #sendRedirect
	 */
	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String targetUrl = createTargetUrl(model, request);
		targetUrl = updateTargetUrl(targetUrl, model, request, response);

		// Save flash attributes
		RequestContextUtils.saveOutputFlashMap(targetUrl, request, response);

		// Redirect
		sendRedirect(request, response, targetUrl, this.http10Compatible);
	}

	/**
	 * 创建目标 URL：首先检查重定向字符串是否为 URI 模板，
	 * 若是则用给定的 model 展开它，然后可选地将简单类型的 model 属性
	 * 作为查询字符串参数追加到 URL。
	 */
	protected final String createTargetUrl(Map<String, Object> model, HttpServletRequest request)
			throws UnsupportedEncodingException {

		// Prepare target URL.
		StringBuilder targetUrl = new StringBuilder();
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		if (this.contextRelative && getUrl().startsWith("/")) {
			// Do not apply context path to relative URLs.
			targetUrl.append(getContextPath(request));
		}
		targetUrl.append(getUrl());

		String enc = this.encodingScheme;
		if (enc == null) {
			enc = request.getCharacterEncoding();
		}
		if (enc == null) {
			enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
		}

		if (this.expandUriTemplateVariables && StringUtils.hasText(targetUrl)) {
			Map<String, String> variables = getCurrentRequestUriVariables(request);
			targetUrl = replaceUriTemplateVariables(targetUrl.toString(), model, variables, enc);
		}
		if (isPropagateQueryProperties()) {
			appendCurrentQueryParams(targetUrl, request);
		}
		if (this.exposeModelAttributes) {
			appendQueryProperties(targetUrl, model, enc);
		}

		return targetUrl.toString();
	}

	private String getContextPath(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		while (contextPath.startsWith("//")) {
			contextPath = contextPath.substring(1);
		}
		return contextPath;
	}

	/**
	 * 用编码后的 model 属性或当前请求的 URI 变量替换目标 URL 中的 URI 模板变量。
	 * URL 中引用到的 model 属性将从 model 中移除。
	 * @param targetUrl the redirect URL
	 * @param model a Map that contains model attributes
	 * @param currentUriVariables current request URI variables to use
	 * @param encodingScheme the encoding scheme to use
	 * @throws UnsupportedEncodingException if string encoding failed
	 */
	protected StringBuilder replaceUriTemplateVariables(
			String targetUrl, Map<String, Object> model, Map<String, String> currentUriVariables, String encodingScheme)
			throws UnsupportedEncodingException {

		StringBuilder result = new StringBuilder();
		Matcher matcher = URI_TEMPLATE_VARIABLE_PATTERN.matcher(targetUrl);
		int endLastMatch = 0;
		while (matcher.find()) {
			String name = matcher.group(1);
			Object value = (model.containsKey(name) ? model.remove(name) : currentUriVariables.get(name));
			if (value == null) {
				throw new IllegalArgumentException("Model has no value for key '" + name + "'");
			}
			result.append(targetUrl, endLastMatch, matcher.start());
			result.append(UriUtils.encodePathSegment(value.toString(), encodingScheme));
			endLastMatch = matcher.end();
		}
		result.append(targetUrl.substring(endLastMatch));
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getCurrentRequestUriVariables(HttpServletRequest request) {
		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVars = (Map<String, String>) request.getAttribute(name);
		return (uriVars != null) ? uriVars : Collections.emptyMap();
	}

	/**
	 * 将当前请求的查询字符串追加到目标重定向 URL。
	 * @param targetUrl the StringBuilder to append the properties to
	 * @param request the current request
	 * @since 4.1
	 */
	protected void appendCurrentQueryParams(StringBuilder targetUrl, HttpServletRequest request) {
		String query = request.getQueryString();
		if (StringUtils.hasText(query)) {
			// Extract anchor fragment, if any.
			String fragment = null;
			int anchorIndex = targetUrl.indexOf("#");
			if (anchorIndex > -1) {
				fragment = targetUrl.substring(anchorIndex);
				targetUrl.delete(anchorIndex, targetUrl.length());
			}

			if (targetUrl.toString().indexOf('?') < 0) {
				targetUrl.append('?').append(query);
			}
			else {
				targetUrl.append('&').append(query);
			}
			// Append anchor fragment, if any, to end of URL.
			if (fragment != null) {
				targetUrl.append(fragment);
			}
		}
	}

	/**
	 * 将查询属性追加到重定向 URL。
	 * 将 model 属性转换为字符串、进行 URL 编码并格式化为查询属性。
	 * @param targetUrl the StringBuilder to append the properties to
	 * @param model a Map that contains model attributes
	 * @param encodingScheme the encoding scheme to use
	 * @throws UnsupportedEncodingException if string encoding failed
	 * @see #queryProperties
	 */
	@SuppressWarnings("unchecked")
	protected void appendQueryProperties(StringBuilder targetUrl, Map<String, Object> model, String encodingScheme)
			throws UnsupportedEncodingException {

		// Extract anchor fragment, if any.
		String fragment = null;
		int anchorIndex = targetUrl.indexOf("#");
		if (anchorIndex > -1) {
			fragment = targetUrl.substring(anchorIndex);
			targetUrl.delete(anchorIndex, targetUrl.length());
		}

		// If there aren't already some parameters, we need a "?".
		boolean first = (targetUrl.toString().indexOf('?') < 0);
		for (Map.Entry<String, Object> entry : queryProperties(model).entrySet()) {
			Object rawValue = entry.getValue();
			Collection<?> values;
			if (rawValue != null && rawValue.getClass().isArray()) {
				values = CollectionUtils.arrayToList(rawValue);
			}
			else if (rawValue instanceof Collection) {
				values = ((Collection<?>) rawValue);
			}
			else {
				values = Collections.singleton(rawValue);
			}
			for (Object value : values) {
				if (first) {
					targetUrl.append('?');
					first = false;
				}
				else {
					targetUrl.append('&');
				}
				String encodedKey = urlEncode(entry.getKey(), encodingScheme);
				String encodedValue = (value != null ? urlEncode(value.toString(), encodingScheme) : "");
				targetUrl.append(encodedKey).append('=').append(encodedValue);
			}
		}

		// Append anchor fragment, if any, to end of URL.
		if (fragment != null) {
			targetUrl.append(fragment);
		}
	}

	/**
	 * 确定查询字符串的名称-值对，这些名称-值对将由
	 * {@link #appendQueryProperties} 转换为字符串、进行 URL 编码并格式化。
	 * <p>此实现通过对每个元素调用 {@link #isEligibleProperty(String, Object)}
	 * 来过滤 model，默认只接受 String、简单类型（primitive）及其包装类型。
	 * @param model the original model Map
	 * @return the filtered Map of eligible query properties
	 * @see #isEligibleProperty(String, Object)
	 */
	protected Map<String, Object> queryProperties(Map<String, Object> model) {
		Map<String, Object> result = new LinkedHashMap<>();
		model.forEach((name, value) -> {
			if (isEligibleProperty(name, value)) {
				result.put(name, value);
			}
		});
		return result;
	}

	/**
	 * 确定给定的 model 元素是否应作为查询属性暴露。
	 * <p>默认实现认为 String 和简单类型（primitive）是符合条件的，
	 * 元素类型相应的数组和 Collection/Iterable 也符合条件。
	 * 此方法可以在子类中重写。
	 * @param key the key of the model element
	 * @param value the value of the model element
	 * @return whether the element is eligible as query property
	 */
	protected boolean isEligibleProperty(String key, @Nullable Object value) {
		if (value == null) {
			return false;
		}
		if (isEligibleValue(value)) {
			return true;
		}
		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			if (length == 0) {
				return false;
			}
			for (int i = 0; i < length; i++) {
				Object element = Array.get(value, i);
				if (!isEligibleValue(element)) {
					return false;
				}
			}
			return true;
		}
		if (value instanceof Collection) {
			Collection<?> coll = (Collection<?>) value;
			if (coll.isEmpty()) {
				return false;
			}
			for (Object element : coll) {
				if (!isEligibleValue(element)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * 确定给定的 model 元素值是否适合暴露。
	 * <p>默认实现根据 {@link BeanUtils#isSimpleValueType} 认为简单类型（primitive）、
	 * 字符串、数字、日期、URI、URL 等是符合条件的。
	 * 此方法可以在子类中重写。
	 * @param value the model element value
	 * @return whether the element value is eligible
	 * @see BeanUtils#isSimpleValueType
	 */
	protected boolean isEligibleValue(@Nullable Object value) {
		return (value != null && BeanUtils.isSimpleValueType(value.getClass()));
	}

	/**
	 * 使用给定的编码方案对给定的输入字符串进行 URL 编码。
	 * <p>默认实现使用 {@code URLEncoder.encode(input, enc)}。
	 * @param input the unencoded input String
	 * @param encodingScheme the encoding scheme
	 * @return the encoded output String
	 * @throws UnsupportedEncodingException if thrown by the JDK URLEncoder
	 * @see java.net.URLEncoder#encode(String, String)
	 */
	protected String urlEncode(String input, String encodingScheme) throws UnsupportedEncodingException {
		return URLEncoder.encode(input, encodingScheme);
	}

	/**
	 * 查找已注册的 {@link RequestDataValueProcessor}（如果有），
	 * 并允许它更新重定向目标 URL。
	 * @param targetUrl the given redirect URL
	 * @return the updated URL or the same as URL as the one passed in
	 */
	protected String updateTargetUrl(String targetUrl, Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) {

		WebApplicationContext wac = getWebApplicationContext();
		if (wac == null) {
			wac = RequestContextUtils.findWebApplicationContext(request, getServletContext());
		}

		if (wac != null && wac.containsBean(RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			RequestDataValueProcessor processor = wac.getBean(
					RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
			return processor.processUrl(request, targetUrl);
		}

		return targetUrl;
	}

	/**
	 * 向 HTTP 客户端发送重定向响应。
	 * @param request current HTTP request (allows for reacting to request method)
	 * @param response current HTTP response (for sending response headers)
	 * @param targetUrl the target URL to redirect to
	 * @param http10Compatible whether to stay compatible with HTTP 1.0 clients
	 * @throws IOException if thrown by response methods
	 */
	protected void sendRedirect(HttpServletRequest request, HttpServletResponse response,
			String targetUrl, boolean http10Compatible) throws IOException {

		String encodedURL = (isRemoteHost(targetUrl) ? targetUrl : response.encodeRedirectURL(targetUrl));
		if (http10Compatible) {
			HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);
			if (this.statusCode != null) {
				response.setStatus(this.statusCode.value());
				response.setHeader("Location", encodedURL);
			}
			else if (attributeStatusCode != null) {
				response.setStatus(attributeStatusCode.value());
				response.setHeader("Location", encodedURL);
			}
			else {
				// Send status code 302 by default.
				response.sendRedirect(encodedURL);
			}
		}
		else {
			HttpStatus statusCode = getHttp11StatusCode(request, response, targetUrl);
			response.setStatus(statusCode.value());
			response.setHeader("Location", encodedURL);
		}
	}

	/**
	 * 判断给定 targetUrl 的主机是否为“外部”系统，若是，
	 * 则不会应用 {@link HttpServletResponse#encodeRedirectURL}。
	 * <p>如果配置了 {@link #setHosts(String[])} 属性，
	 * 并且目标 URL 的主机与之不匹配，此方法返回 {@code true}。
	 * @param targetUrl the target redirect URL
	 * @return {@code true} if the target URL has a remote host, {@code false} if
	 * the URL does not have a host or the "host" property is not configured
	 * @since 4.3
	 */
	protected boolean isRemoteHost(String targetUrl) {
		if (ObjectUtils.isEmpty(getHosts())) {
			return false;
		}
		String targetHost = UriComponentsBuilder.fromUriString(targetUrl).build().getHost();
		if (!StringUtils.hasLength(targetHost)) {
			return false;
		}
		for (String host : getHosts()) {
			if (targetHost.equals(host)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 确定用于 HTTP 1.1 兼容请求的状态码。
	 * <p>默认实现会返回 {@link #setStatusCode(HttpStatus) statusCode} 属性（如果已设置），
	 * 否则返回 {@link #RESPONSE_STATUS_ATTRIBUTE} 属性的值。
	 * 如果两者都未设置，则默认为 {@link HttpStatus#SEE_OTHER}（303）。
	 * @param request the request to inspect
	 * @param response the servlet response
	 * @param targetUrl the target URL
	 * @return the response status
	 */
	protected HttpStatus getHttp11StatusCode(
			HttpServletRequest request, HttpServletResponse response, String targetUrl) {

		if (this.statusCode != null) {
			return this.statusCode;
		}
		HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);
		if (attributeStatusCode != null) {
			return attributeStatusCode;
		}
		return HttpStatus.SEE_OTHER;
	}

}
