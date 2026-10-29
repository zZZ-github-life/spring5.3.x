/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.util;

import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.MappingMatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * 用于 URL 路径匹配的辅助类。为 {@code RequestDispatcher} 包含中的 URL 路径提供支持，并支持一致的 URL 解码。<p>被 {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping} 和 {@link org.springframework.web.servlet.support.RequestContext} 用于路径匹配和/或 URI 确定。
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @since 14.01.2004
 * @see #getLookupPathForRequest
 * @see javax.servlet.RequestDispatcher
 */
public class UrlPathHelper {

	/**
	 * 保存{@link #getLookupPathForRequest resolved} 查找路径的 Servlet 请求属性名称。
	 * @since 5.3
	 */
	public static final String PATH_ATTRIBUTE = UrlPathHelper.class.getName() + ".PATH";

	static final boolean servlet4Present =
			ClassUtils.hasMethod(HttpServletRequest.class, "getHttpServletMapping");

	/**
	 * 特殊的 WebSphere 请求属性，表示原始请求 URI。在 WebSphere 上优先于标准 Servlet 2.4 转发属性，仅仅是因为我们需要请求转发链中的第一个 URI。
	 */
	private static final String WEBSPHERE_URI_ATTRIBUTE = "com.ibm.websphere.servlet.uri_non_decoded";

	private static final Log logger = LogFactory.getLog(UrlPathHelper.class);

	@Nullable
	static volatile Boolean websphereComplianceFlag;


	private boolean alwaysUseFullPath = false;

	private boolean urlDecode = true;

	private boolean removeSemicolonContent = true;

	private String defaultEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;

	private boolean readOnly = false;


	/**
	 * URL 查找是否应始终使用当前 Web 应用上下文（即 {@link javax.servlet.ServletContext#getContextPath()} 范围内）中的完整路径。<p>如果设置为 {@literal false}，则在适用时（即对于基于前缀的 Servlet 映射，如 "/myServlet/*"）改用当前 servlet 映射内的路径。<p>默认设置为 "false"。
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		checkReadOnly();
		this.alwaysUseFullPath = alwaysUseFullPath;
	}

	/**
	 * 上下文路径和请求 URI 是否应被解码——与 servlet 路径不同，Servlet API 返回这两者时是<i>未解码</i>的。<p>设置为 "true" 时使用请求编码或默认 Servlet 规范编码（ISO-8859-1）。<p>默认设置为 {@literal true}。<p><strong>注意：</strong>请记住，与编码后的路径比较时 servlet 路径将不匹配。因此，使用 {@code urlDecode=false} 与基于前缀的 Servlet 映射不兼容，并且同样意味着还必须设置 {@code alwaysUseFullPath=true}。
	 * @see #getServletPath
	 * @see #getContextPath
	 * @see #getRequestUri
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public void setUrlDecode(boolean urlDecode) {
		checkReadOnly();
		this.urlDecode = urlDecode;
	}

	/**
	 * 在确定查找路径时是否解码请求 URI。
	 * @since 4.3.13
	 */
	public boolean isUrlDecode() {
		return this.urlDecode;
	}

	/**
	 * 设置是否应从请求 URI 中剥离 ";"（分号）内容。<p>默认为 "true"。
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		checkReadOnly();
		this.removeSemicolonContent = removeSemicolonContent;
	}

	/**
	 * 是否已配置为从请求 URI 中移除 ";"（分号）内容。
	 */
	public boolean shouldRemoveSemicolonContent() {
		return this.removeSemicolonContent;
	}

	/**
	 * 设置用于 URL 解码的默认字符编码。根据 Servlet 规范，默认是 ISO-8859-1。<p>如果请求本身指定了字符编码，则请求编码将覆盖此设置。这也允许在调用 {@code ServletRequest.setCharacterEncoding} 方法的过滤器中通用地覆盖字符编码。
	 * @param defaultEncoding the character encoding to use
	 * @see #determineEncoding
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(String)
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		checkReadOnly();
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 返回用于 URL 解码的默认字符编码。
	 */
	protected String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 切换到只读模式，不再允许进一步的配置更改。
	 */
	private void setReadOnly() {
		this.readOnly = true;
	}

	private void checkReadOnly() {
		Assert.isTrue(!this.readOnly, "This instance cannot be modified");
	}


	/**
	 * {@link #getLookupPathForRequest 解析} lookupPath，并将其以 {@link #PATH_ATTRIBUTE} 为键缓存到请求属性中，以便后续通过 {@link #getResolvedLookupPath(ServletRequest)} 访问。
	 * @param request the current request
	 * @return the resolved path
	 * @since 5.3
	 */
	public String resolveAndCacheLookupPath(HttpServletRequest request) {
		String lookupPath = getLookupPathForRequest(request);
		request.setAttribute(PATH_ATTRIBUTE, lookupPath);
		return lookupPath;
	}

	/**
	 * 返回先前{@link #getLookupPathForRequest resolved} 的查找路径。
	 * @param request the current request
	 * @return the previously resolved lookupPath
	 * @throws IllegalArgumentException if the lookup path is not found
	 * @since 5.3
	 */
	public static String getResolvedLookupPath(ServletRequest request) {
		String lookupPath = (String) request.getAttribute(PATH_ATTRIBUTE);
		Assert.notNull(lookupPath, () -> "Expected lookupPath in request attribute \"" + PATH_ATTRIBUTE + "\".");
		return lookupPath;
	}

	/**
	 * {@link #getLookupPathForRequest(HttpServletRequest)} 的变体，会自动检查保存为请求属性的先前计算的查找路径。该属性仅用于查找目的。
	 * @param request current HTTP request
	 * @param name the request attribute that holds the lookupPath
	 * @return the lookup path
	 * @since 5.2
	 * @deprecated as of 5.3 in favor of using
	 * {@link #resolveAndCacheLookupPath(HttpServletRequest)} and
	 * {@link #getResolvedLookupPath(ServletRequest)}.
	 */
	@Deprecated
	public String getLookupPathForRequest(HttpServletRequest request, @Nullable String name) {
		String result = null;
		if (name != null) {
			result = (String) request.getAttribute(name);
		}
		return (result != null ? result : getLookupPathForRequest(request));
	}

	/**
	 * 返回给定请求在适用时位于当前 servlet 映射内、否则位于 Web 应用内的映射查找路径。<p>如果在 RequestDispatcher 包含中调用，则检测包含请求 URL。
	 * @param request current HTTP request
	 * @return the lookup path
	 * @see #getPathWithinServletMapping
	 * @see #getPathWithinApplication
	 */
	public String getLookupPathForRequest(HttpServletRequest request) {
		String pathWithinApp = getPathWithinApplication(request);
		// Always use full path within current servlet context?
		if (this.alwaysUseFullPath || skipServletPathDetermination(request)) {
			return pathWithinApp;
		}
		// Else, use path within current servlet mapping if applicable
		String rest = getPathWithinServletMapping(request, pathWithinApp);
		if (StringUtils.hasLength(rest)) {
			return rest;
		}
		else {
			return pathWithinApp;
		}
	}

	/**
	 * 检查给定请求是否可以跳过 servlet 路径确定。
	 * @param request current HTTP request
	 * @return {@code true} if the request mapping has not been achieved using a path
	 * or if the servlet has been mapped to root; {@code false} otherwise
	 */
	private boolean skipServletPathDetermination(HttpServletRequest request) {
		if (servlet4Present) {
			return Servlet4Delegate.skipServletPathDetermination(request);
		}
		return false;
	}

	/**
	 * 返回给定请求在 servlet 映射内的路径，即请求 URL 中超出调用 servlet 部分的部分；如果整个 URL 都已用于标识 servlet，则返回 ""。
	 * @param request current HTTP request
	 * @return the path within the servlet mapping, or ""
	 * @see #getPathWithinServletMapping(HttpServletRequest, String)
	 */
	public String getPathWithinServletMapping(HttpServletRequest request) {
		return getPathWithinServletMapping(request, getPathWithinApplication(request));
	}

	/**
	 * 返回给定请求在 servlet 映射内的路径，即请求 URL 中超出调用 servlet 部分的部分；如果整个 URL 都已用于标识 servlet，则返回 ""。<p>如果在 RequestDispatcher 包含中调用，则检测包含请求 URL。<p>例如：servlet 映射 = "/*"；请求 URI = "/test/a" &rarr; "/test/a"。<p>例如：servlet 映射 = "/"；请求 URI = "/test/a" &rarr; "/test/a"。<p>例如：servlet 映射 = "/test/*"；请求 URI = "/test/a" &rarr; "/a"。<p>例如：servlet 映射 = "/test"；请求 URI = "/test" &rarr; ""。<p>例如：servlet 映射 = "/*.test"；请求 URI = "/a.test" &rarr; ""。
	 * @param request current HTTP request
	 * @param pathWithinApp a precomputed path within the application
	 * @return the path within the servlet mapping, or ""
	 * @since 5.2.9
	 * @see #getLookupPathForRequest
	 */
	protected String getPathWithinServletMapping(HttpServletRequest request, String pathWithinApp) {
		String servletPath = getServletPath(request);
		String sanitizedPathWithinApp = getSanitizedPath(pathWithinApp);
		String path;

		// If the app container sanitized the servletPath, check against the sanitized version
		if (servletPath.contains(sanitizedPathWithinApp)) {
			path = getRemainingPath(sanitizedPathWithinApp, servletPath, false);
		}
		else {
			path = getRemainingPath(pathWithinApp, servletPath, false);
		}

		if (path != null) {
			// Normal case: URI contains servlet path.
			return path;
		}
		else {
			// Special case: URI is different from servlet path.
			String pathInfo = request.getPathInfo();
			if (pathInfo != null) {
				// Use path info if available. Indicates index page within a servlet mapping?
				// e.g. with index page: URI="/", servletPath="/index.html"
				return pathInfo;
			}
			if (!this.urlDecode) {
				// No path info... (not mapped by prefix, nor by extension, nor "/*")
				// For the default servlet mapping (i.e. "/"), urlDecode=false can
				// cause issues since getServletPath() returns a decoded path.
				// If decoding pathWithinApp yields a match just use pathWithinApp.
				path = getRemainingPath(decodeInternal(request, pathWithinApp), servletPath, false);
				if (path != null) {
					return pathWithinApp;
				}
			}
			// Otherwise, use the full servlet path.
			return servletPath;
		}
	}

	/**
	 * 返回给定请求在 Web 应用内的路径。<p>如果在 RequestDispatcher 包含中调用，则检测包含请求 URL。
	 * @param request current HTTP request
	 * @return the path within the web application
	 * @see #getLookupPathForRequest
	 */
	public String getPathWithinApplication(HttpServletRequest request) {
		String contextPath = getContextPath(request);
		String requestUri = getRequestUri(request);
		String path = getRemainingPath(requestUri, contextPath, true);
		if (path != null) {
			// Normal case: URI contains context path.
			return (StringUtils.hasText(path) ? path : "/");
		}
		else {
			return requestUri;
		}
	}

	/**
	 * 将给定 "mapping" 与 "requestUri" 的开头匹配，如果匹配，则返回额外的部分。需要此方法是因为 HttpServletRequest 返回的上下文路径和 servlet 路径与 requestUri 不同，已去除了分号内容。
	 */
	@Nullable
	private String getRemainingPath(String requestUri, String mapping, boolean ignoreCase) {
		int index1 = 0;
		int index2 = 0;
		for (; (index1 < requestUri.length()) && (index2 < mapping.length()); index1++, index2++) {
			char c1 = requestUri.charAt(index1);
			char c2 = mapping.charAt(index2);
			if (c1 == ';') {
				index1 = requestUri.indexOf('/', index1);
				if (index1 == -1) {
					return null;
				}
				c1 = requestUri.charAt(index1);
			}
			if (c1 == c2 || (ignoreCase && (Character.toLowerCase(c1) == Character.toLowerCase(c2)))) {
				continue;
			}
			return null;
		}
		if (index2 != mapping.length()) {
			return null;
		}
		else if (index1 == requestUri.length()) {
			return "";
		}
		else if (requestUri.charAt(index1) == ';') {
			index1 = requestUri.indexOf('/', index1);
		}
		return (index1 != -1 ? requestUri.substring(index1) : "");
	}

	/**
	 * 清理给定路径。使用以下规则：<ul> <li>将所有 "//" 替换为 "/"</li> </ul>
	 */
	private static String getSanitizedPath(final String path) {
		int start = path.indexOf("//");
		if (start == -1) {
			return path;
		}
		char[] content = path.toCharArray();
		int slowIndex = start;
		for (int fastIndex = start + 1; fastIndex < content.length; fastIndex++) {
			if (content[fastIndex] != '/' || content[slowIndex] != '/') {
				content[++slowIndex] = content[fastIndex];
			}
		}
		return new String(content, 0, slowIndex + 1);
	}

	/**
	 * 返回给定请求的请求 URI，如果在 RequestDispatcher 包含中调用，则检测包含请求 URL。<p>由于 {@code request.getRequestURI()} 返回的值<i>未</i>由 servlet 容器解码，此方法将对其进行解码。<p>Web 容器解析出的 URI<i>应该</i>是正确的，但某些容器（如 JBoss/Jetty）会错误地在 URI 中包含 ";jsessionid" 之类的 ";" 字符串。此方法会截掉这些错误的附加部分。
	 * @param request current HTTP request
	 * @return the request URI
	 */
	public String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * 返回给定请求的上下文路径，如果在 RequestDispatcher 包含中调用，则检测包含请求 URL。<p>由于 {@code request.getContextPath()} 返回的值<i>未</i>由 servlet 容器解码，此方法将对其进行解码。
	 * @param request current HTTP request
	 * @return the context path
	 */
	public String getContextPath(HttpServletRequest request) {
		String contextPath = (String) request.getAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}
		if (StringUtils.matchesCharacter(contextPath, '/')) {
			// Invalid case, but happens for includes on Jetty: silently adapt it.
			contextPath = "";
		}
		return decodeRequestString(request, contextPath);
	}

	/**
	 * 返回给定请求的 servlet 路径，如果在 RequestDispatcher 包含中调用，则考虑包含请求 URL。<p>由于 {@code request.getServletPath()} 返回的值已由 servlet 容器解码，此方法不会尝试对其进行解码。
	 * @param request current HTTP request
	 * @return the servlet path
	 */
	public String getServletPath(HttpServletRequest request) {
		String servletPath = (String) request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE);
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}
		if (servletPath.length() > 1 && servletPath.endsWith("/") && shouldRemoveTrailingServletPathSlash(request)) {
			// On WebSphere, in non-compliant mode, for a "/foo/" case that would be "/foo"
			// on all other servlet containers: removing trailing slash, proceeding with
			// that remaining slash as final lookup path...
			servletPath = servletPath.substring(0, servletPath.length() - 1);
		}
		return servletPath;
	}


	/**
	 * 返回给定请求的请求 URI。如果这是转发请求，则正确解析为原始请求的请求 URI。
	 */
	public String getOriginatingRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WEBSPHERE_URI_ATTRIBUTE);
		if (uri == null) {
			uri = (String) request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
			if (uri == null) {
				uri = request.getRequestURI();
			}
		}
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * 返回给定请求的上下文路径，如果在 RequestDispatcher 包含中调用，则检测包含请求 URL。<p>由于 {@code request.getContextPath()} 返回的值<i>未</i>由 servlet 容器解码，此方法将对其进行解码。
	 * @param request current HTTP request
	 * @return the context path
	 */
	public String getOriginatingContextPath(HttpServletRequest request) {
		String contextPath = (String) request.getAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE);
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}
		return decodeRequestString(request, contextPath);
	}

	/**
	 * 返回给定请求的 servlet 路径，如果在 RequestDispatcher 包含中调用，则检测包含请求 URL。
	 * @param request current HTTP request
	 * @return the servlet path
	 */
	public String getOriginatingServletPath(HttpServletRequest request) {
		String servletPath = (String) request.getAttribute(WebUtils.FORWARD_SERVLET_PATH_ATTRIBUTE);
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}
		return servletPath;
	}

	/**
	 * 返回给定请求 URL 的查询字符串部分。如果这是转发请求，则正确解析为原始请求的查询字符串。
	 * @param request current HTTP request
	 * @return the query string
	 */
	public String getOriginatingQueryString(HttpServletRequest request) {
		if ((request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) != null) ||
				(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null)) {
			return (String) request.getAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE);
		}
		else {
			return request.getQueryString();
		}
	}

	/**
	 * 解码提供的 URI 字符串，并剥离 ';' 之后的任何多余部分。
	 */
	private String decodeAndCleanUriString(HttpServletRequest request, String uri) {
		uri = removeSemicolonContent(uri);
		uri = decodeRequestString(request, uri);
		uri = getSanitizedPath(uri);
		return uri;
	}

	/**
	 * 使用 URLDecoder 解码给定源字符串。编码将从请求中获取，否则回退到默认的 "ISO-8859-1"。<p>默认实现使用 {@code URLDecoder.decode(input, enc)}。
	 * @param request current HTTP request
	 * @param source the String to decode
	 * @return the decoded String
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see java.net.URLDecoder#decode(String, String)
	 * @see java.net.URLDecoder#decode(String)
	 */
	public String decodeRequestString(HttpServletRequest request, String source) {
		if (this.urlDecode) {
			return decodeInternal(request, source);
		}
		return source;
	}

	@SuppressWarnings("deprecation")
	private String decodeInternal(HttpServletRequest request, String source) {
		String enc = determineEncoding(request);
		try {
			return UriUtils.decode(source, enc);
		}
		catch (UnsupportedCharsetException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not decode request string [" + source + "] with encoding '" + enc +
						"': falling back to platform default encoding; exception message: " + ex.getMessage());
			}
			return URLDecoder.decode(source);
		}
	}

	/**
	 * 确定给定请求的编码。可在子类中重写。<p>默认实现检查请求编码，否则回退到此解析器指定的默认编码。
	 * @param request current HTTP request
	 * @return the encoding for the request (never {@code null})
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see #setDefaultEncoding
	 */
	protected String determineEncoding(HttpServletRequest request) {
		String enc = request.getCharacterEncoding();
		if (enc == null) {
			enc = getDefaultEncoding();
		}
		return enc;
	}

	/**
	 * 如果 {@linkplain #setRemoveSemicolonContent removeSemicolonContent} 属性设置为 "true"，则从给定请求 URI 中移除 ";"（分号）内容。请注意，"jsessionid" 始终会被移除。
	 * @param requestUri the request URI string to remove ";" content from
	 * @return the updated URI string
	 */
	public String removeSemicolonContent(String requestUri) {
		return (this.removeSemicolonContent ?
				removeSemicolonContentInternal(requestUri) : removeJsessionid(requestUri));
	}

	private static String removeSemicolonContentInternal(String requestUri) {
		int semicolonIndex = requestUri.indexOf(';');
		if (semicolonIndex == -1) {
			return requestUri;
		}
		StringBuilder sb = new StringBuilder(requestUri);
		while (semicolonIndex != -1) {
			int slashIndex = sb.indexOf("/", semicolonIndex + 1);
			if (slashIndex == -1) {
				return sb.substring(0, semicolonIndex);
			}
			sb.delete(semicolonIndex, slashIndex);
			semicolonIndex = sb.indexOf(";", semicolonIndex);
		}
		return sb.toString();
	}

	private String removeJsessionid(String requestUri) {
		String key = ";jsessionid=";
		int index = requestUri.toLowerCase().indexOf(key);
		if (index == -1) {
			return requestUri;
		}
		String start = requestUri.substring(0, index);
		for (int i = index + key.length(); i < requestUri.length(); i++) {
			char c = requestUri.charAt(i);
			if (c == ';' || c == '/') {
				return start + requestUri.substring(i);
			}
		}
		return start;
	}

	/**
	 * 通过 {@link #decodeRequestString} 解码给定 URI 路径变量，除非 {@link #setUrlDecode} 设置为 {@code true}，此时假定提取这些变量的 URL 路径已通过调用 {@link #getLookupPathForRequest(HttpServletRequest)} 解码。
	 * @param request current HTTP request
	 * @param vars the URI variables extracted from the URL path
	 * @return the same Map or a new Map instance
	 */
	public Map<String, String> decodePathVariables(HttpServletRequest request, Map<String, String> vars) {
		if (this.urlDecode) {
			return vars;
		}
		else {
			Map<String, String> decodedVars = CollectionUtils.newLinkedHashMap(vars.size());
			vars.forEach((key, value) -> decodedVars.put(key, decodeInternal(request, value)));
			return decodedVars;
		}
	}

	/**
	 * 通过 {@link #decodeRequestString} 解码给定矩阵变量，除非 {@link #setUrlDecode} 设置为 {@code true}，此时假定提取这些变量的 URL 路径已通过调用 {@link #getLookupPathForRequest(HttpServletRequest)} 解码。
	 * @param request current HTTP request
	 * @param vars the URI variables extracted from the URL path
	 * @return the same Map or a new Map instance
	 */
	public MultiValueMap<String, String> decodeMatrixVariables(
			HttpServletRequest request, MultiValueMap<String, String> vars) {

		if (this.urlDecode) {
			return vars;
		}
		else {
			MultiValueMap<String, String> decodedVars = new LinkedMultiValueMap<>(vars.size());
			vars.forEach((key, values) -> {
				for (String value : values) {
					decodedVars.add(key, decodeInternal(request, value));
				}
			});
			return decodedVars;
		}
	}

	private boolean shouldRemoveTrailingServletPathSlash(HttpServletRequest request) {
		if (request.getAttribute(WEBSPHERE_URI_ATTRIBUTE) == null) {
			// Regular servlet container: behaves as expected in any case,
			// so the trailing slash is the result of a "/" url-pattern mapping.
			// Don't remove that slash.
			return false;
		}
		Boolean flagToUse = websphereComplianceFlag;
		if (flagToUse == null) {
			ClassLoader classLoader = UrlPathHelper.class.getClassLoader();
			String className = "com.ibm.ws.webcontainer.WebContainer";
			String methodName = "getWebContainerProperties";
			String propName = "com.ibm.ws.webcontainer.removetrailingservletpathslash";
			boolean flag = false;
			try {
				Class<?> cl = classLoader.loadClass(className);
				Properties prop = (Properties) cl.getMethod(methodName).invoke(null);
				flag = Boolean.parseBoolean(prop.getProperty(propName));
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not introspect WebSphere web container properties: " + ex);
				}
			}
			flagToUse = flag;
			websphereComplianceFlag = flag;
		}
		// Don't bother if WebSphere is configured to be fully Servlet compliant.
		// However, if it is not compliant, do remove the improper trailing slash!
		return !flagToUse;
	}


	/**
	 * 使用默认值的共享只读实例。适用以下设置：<ul> <li>{@code alwaysUseFullPath=false} <li>{@code urlDecode=true} <li>{@code removeSemicolon=true} <li>{@code defaultEncoding=}{@link WebUtils#DEFAULT_CHARACTER_ENCODING} </ul>
	 */
	public static final UrlPathHelper defaultInstance = new UrlPathHelper();

	static {
		defaultInstance.setReadOnly();
	}


	/**
	 * 用于完整编码路径的共享只读实例。适用以下设置：<ul> <li>{@code alwaysUseFullPath=true} <li>{@code urlDecode=false} <li>{@code removeSemicolon=false} <li>{@code defaultEncoding=}{@link WebUtils#DEFAULT_CHARACTER_ENCODING} </ul>
	 */
	public static final UrlPathHelper rawPathInstance = new UrlPathHelper() {

		@Override
		public String removeSemicolonContent(String requestUri) {
			return requestUri;
		}
	};

	static {
		rawPathInstance.setAlwaysUseFullPath(true);
		rawPathInstance.setUrlDecode(false);
		rawPathInstance.setRemoveSemicolonContent(false);
		rawPathInstance.setReadOnly();
	}


	/**
	 * 内部类，用于在运行时避免对 Servlet 4 {@link HttpServletMapping} 和 {@link MappingMatch} 的硬依赖。
	 */
	private static class Servlet4Delegate {

		public static boolean skipServletPathDetermination(HttpServletRequest request) {
			HttpServletMapping mapping = (HttpServletMapping) request.getAttribute(RequestDispatcher.INCLUDE_MAPPING);
			if (mapping == null) {
				mapping = request.getHttpServletMapping();
			}
			MappingMatch match = mapping.getMappingMatch();
			return (match != null && (!match.equals(MappingMatch.PATH) || mapping.getPattern().equals("/*")));
		}
	}

}
