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

package org.springframework.mock.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link javax.servlet.http.HttpServletRequest} 接口的 mock 实现。
 *
 * <p>此请求所模拟的<em>服务器</em>的默认首选 {@link Locale}
 * 为 {@link Locale#ENGLISH}。可以通过 {@link #addPreferredLocale}
 * 或 {@link #setPreferredLocales} 更改此值。
 *
 * <p>从 Spring 5.0 开始，这组 mock 基于 Servlet 4.0 基线设计。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rick Evans
 * @author Mark Fisher
 * @author Chris Beams
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 1.0.2
 */
public class MockHttpServletRequest implements HttpServletRequest {

	private static final String HTTP = "http";

	private static final String HTTPS = "https";

	private static final String CHARSET_PREFIX = "charset=";

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	private static final ServletInputStream EMPTY_SERVLET_INPUT_STREAM =
			new DelegatingServletInputStream(StreamUtils.emptyInput());

	private static final BufferedReader EMPTY_BUFFERED_READER =
			new BufferedReader(new StringReader(""));

	/**
	 * HTTP RFC 中规定的日期格式。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	private static final String[] DATE_FORMATS = new String[] {
			"EEE, dd MMM yyyy HH:mm:ss zzz",
			"EEE, dd-MMM-yy HH:mm:ss zzz",
			"EEE MMM dd HH:mm:ss yyyy"
	};


	// ---------------------------------------------------------------------
	// Public constants
	// ---------------------------------------------------------------------

	/**
	 * 默认协议：'HTTP/1.1'。
	 * @since 4.3.7
	 */
	public static final String DEFAULT_PROTOCOL = "HTTP/1.1";

	/**
	 * 默认方案：'http'。
	 * @since 4.3.7
	 */
	public static final String DEFAULT_SCHEME = HTTP;

	/**
	 * 默认服务器地址：'127.0.0.1'。
	 */
	public static final String DEFAULT_SERVER_ADDR = "127.0.0.1";

	/**
	 * 默认服务器名称：'localhost'。
	 */
	public static final String DEFAULT_SERVER_NAME = "localhost";

	/**
	 * 默认服务器端口：'80'。
	 */
	public static final int DEFAULT_SERVER_PORT = 80;

	/**
	 * 默认远程地址：'127.0.0.1'。
	 */
	public static final String DEFAULT_REMOTE_ADDR = "127.0.0.1";

	/**
	 * 默认远程主机：'localhost'。
	 */
	public static final String DEFAULT_REMOTE_HOST = "localhost";


	// ---------------------------------------------------------------------
	// Lifecycle properties
	// ---------------------------------------------------------------------

	private final ServletContext servletContext;

	private boolean active = true;


	// ---------------------------------------------------------------------
	// ServletRequest properties
	// ---------------------------------------------------------------------

	private final Map<String, Object> attributes = new LinkedHashMap<>();

	@Nullable
	private String characterEncoding;

	@Nullable
	private byte[] content;

	@Nullable
	private String contentType;

	@Nullable
	private ServletInputStream inputStream;

	@Nullable
	private BufferedReader reader;

	private final Map<String, String[]> parameters = new LinkedHashMap<>(16);

	private String protocol = DEFAULT_PROTOCOL;

	private String scheme = DEFAULT_SCHEME;

	private String serverName = DEFAULT_SERVER_NAME;

	private int serverPort = DEFAULT_SERVER_PORT;

	private String remoteAddr = DEFAULT_REMOTE_ADDR;

	private String remoteHost = DEFAULT_REMOTE_HOST;

	/** 按降序排列的 Locale 列表。 */
	private final LinkedList<Locale> locales = new LinkedList<>();

	private boolean secure = false;

	private int remotePort = DEFAULT_SERVER_PORT;

	private String localName = DEFAULT_SERVER_NAME;

	private String localAddr = DEFAULT_SERVER_ADDR;

	private int localPort = DEFAULT_SERVER_PORT;

	private boolean asyncStarted = false;

	private boolean asyncSupported = false;

	@Nullable
	private MockAsyncContext asyncContext;

	private DispatcherType dispatcherType = DispatcherType.REQUEST;


	// ---------------------------------------------------------------------
	// HttpServletRequest properties
	// ---------------------------------------------------------------------

	@Nullable
	private String authType;

	@Nullable
	private Cookie[] cookies;

	private final Map<String, HeaderValueHolder> headers = new LinkedCaseInsensitiveMap<>();

	@Nullable
	private String method;

	@Nullable
	private String pathInfo;

	private String contextPath = "";

	@Nullable
	private String queryString;

	@Nullable
	private String remoteUser;

	private final Set<String> userRoles = new HashSet<>();

	@Nullable
	private Principal userPrincipal;

	@Nullable
	private String requestedSessionId;

	@Nullable
	private String requestURI;

	private String servletPath = "";

	@Nullable
	private HttpSession session;

	private boolean requestedSessionIdValid = true;

	private boolean requestedSessionIdFromCookie = true;

	private boolean requestedSessionIdFromURL = false;

	private final MultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();


	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------

	/**
	 * 使用默认的
	 * {@link MockServletContext} 创建一个新的 {@code MockHttpServletRequest}。
	 * @see #MockHttpServletRequest(ServletContext, String, String)
	 */
	public MockHttpServletRequest() {
		this(null, "", "");
	}

	/**
	 * 使用默认的
	 * {@link MockServletContext} 创建一个新的 {@code MockHttpServletRequest}。
	 * @param method 请求方法（可为 {@code null}）
	 * @param requestURI 请求 URI（可为 {@code null}）
	 * @see #setMethod
	 * @see #setRequestURI
	 * @see #MockHttpServletRequest(ServletContext, String, String)
	 */
	public MockHttpServletRequest(@Nullable String method, @Nullable String requestURI) {
		this(null, method, requestURI);
	}

	/**
	 * 使用提供的 {@link ServletContext} 创建一个新的 {@code MockHttpServletRequest}。
	 * @param servletContext 请求运行所在的 ServletContext
	 * （可为 {@code null} 以使用默认的 {@link MockServletContext}）
	 * @see #MockHttpServletRequest(ServletContext, String, String)
	 */
	public MockHttpServletRequest(@Nullable ServletContext servletContext) {
		this(servletContext, "", "");
	}

	/**
	 * 使用提供的 {@link ServletContext}、
	 * {@code method} 和 {@code requestURI} 创建一个新的 {@code MockHttpServletRequest}。
	 * <p>首选区域设置将设置为 {@link Locale#ENGLISH}。
	 * @param servletContext 请求运行所在的 ServletContext
	 * （可为 {@code null} 以使用默认的 {@link MockServletContext}）
	 * @param method 请求方法（可为 {@code null}）
	 * @param requestURI 请求 URI（可为 {@code null}）
	 * @see #setMethod
	 * @see #setRequestURI
	 * @see #setPreferredLocales
	 * @see MockServletContext
	 */
	public MockHttpServletRequest(@Nullable ServletContext servletContext, @Nullable String method, @Nullable String requestURI) {
		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.method = method;
		this.requestURI = requestURI;
		this.locales.add(Locale.ENGLISH);
	}


	// ---------------------------------------------------------------------
	// Lifecycle methods
	// ---------------------------------------------------------------------

	/**
	 * 返回此请求关联的 ServletContext。（由于某些原因，
	 * 标准 HttpServletRequest 接口中未提供此方法。）
	 */
	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	/**
	 * 返回此请求是否仍处于活动状态（即尚未完成）。
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * 将此请求标记为已完成，同时保留其状态。
	 */
	public void close() {
		this.active = false;
	}

	/**
	 * 使此请求无效，清除其状态。
	 */
	public void invalidate() {
		close();
		clearAttributes();
	}

	/**
	 * 检查此请求是否仍处于活动状态（即尚未完成），
	 * 如果不再活动则抛出 IllegalStateException。
	 */
	protected void checkActive() throws IllegalStateException {
		Assert.state(this.active, "Request is not active anymore");
	}


	// ---------------------------------------------------------------------
	// ServletRequest interface
	// ---------------------------------------------------------------------

	@Override
	public Object getAttribute(String name) {
		checkActive();
		return this.attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		checkActive();
		return Collections.enumeration(new LinkedHashSet<>(this.attributes.keySet()));
	}

	@Override
	@Nullable
	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	@Override
	public void setCharacterEncoding(@Nullable String characterEncoding) {
		this.characterEncoding = characterEncoding;
		updateContentTypeHeader();
	}

	private void updateContentTypeHeader() {
		if (StringUtils.hasLength(this.contentType)) {
			String value = this.contentType;
			if (StringUtils.hasLength(this.characterEncoding) && !this.contentType.toLowerCase().contains(CHARSET_PREFIX)) {
				value += ';' + CHARSET_PREFIX + this.characterEncoding;
			}
			doAddHeaderValue(HttpHeaders.CONTENT_TYPE, value, true);
		}
	}

	/**
	 * 以字节数组形式设置请求正文的内容。
	 * <p>如果提供的字节数组表示 XML 或 JSON 等文本，
	 * 通常还应设置 {@link #setCharacterEncoding 字符编码}。
	 * @see #setCharacterEncoding(String)
	 * @see #getContentAsByteArray()
	 * @see #getContentAsString()
	 */
	public void setContent(@Nullable byte[] content) {
		this.content = content;
		this.inputStream = null;
		this.reader = null;
	}

	/**
	 * 以字节数组形式获取请求正文的内容。
	 * @return 内容字节数组（可能为 {@code null}）
	 * @since 5.0
	 * @see #setContent(byte[])
	 * @see #getContentAsString()
	 */
	@Nullable
	public byte[] getContentAsByteArray() {
		return this.content;
	}

	/**
	 * 使用配置的
	 * {@linkplain #getCharacterEncoding 字符编码}，以 {@code String} 形式获取请求正文的内容。
	 * @return 内容，类型为 {@code String}，可能为 {@code null}
	 * @throws IllegalStateException 如果尚未设置字符编码
	 * @throws UnsupportedEncodingException 如果不支持该字符编码
	 * @since 5.0
	 * @see #setContent(byte[])
	 * @see #setCharacterEncoding(String)
	 * @see #getContentAsByteArray()
	 */
	@Nullable
	public String getContentAsString() throws IllegalStateException, UnsupportedEncodingException {
		Assert.state(this.characterEncoding != null,
				"Cannot get content as a String for a null character encoding. " +
				"Consider setting the characterEncoding in the request.");

		if (this.content == null) {
			return null;
		}
		return new String(this.content, this.characterEncoding);
	}

	@Override
	public int getContentLength() {
		return (this.content != null ? this.content.length : -1);
	}

	@Override
	public long getContentLengthLong() {
		return getContentLength();
	}

	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
		if (contentType != null) {
			try {
				MediaType mediaType = MediaType.parseMediaType(contentType);
				if (mediaType.getCharset() != null) {
					this.characterEncoding = mediaType.getCharset().name();
				}
			}
			catch (IllegalArgumentException ex) {
				// Try to get charset value anyway
				int charsetIndex = contentType.toLowerCase().indexOf(CHARSET_PREFIX);
				if (charsetIndex != -1) {
					this.characterEncoding = contentType.substring(charsetIndex + CHARSET_PREFIX.length());
				}
			}
			updateContentTypeHeader();
		}
	}

	@Override
	@Nullable
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public ServletInputStream getInputStream() {
		if (this.inputStream != null) {
			return this.inputStream;
		}
		else if (this.reader != null) {
			throw new IllegalStateException(
					"Cannot call getInputStream() after getReader() has already been called for the current request")			;
		}

		this.inputStream = (this.content != null ?
				new DelegatingServletInputStream(new ByteArrayInputStream(this.content)) :
				EMPTY_SERVLET_INPUT_STREAM);
		return this.inputStream;
	}

	/**
	 * 为指定的 HTTP 参数设置单个值。
	 * <p>如果已为给定参数名注册了一个或多个值，
	 * 它们将被替换。
	 */
	public void setParameter(String name, String value) {
		setParameter(name, new String[] {value});
	}

	/**
	 * 为指定的 HTTP 参数设置值数组。
	 * <p>如果已为给定参数名注册了一个或多个值，
	 * 它们将被替换。
	 */
	public void setParameter(String name, String... values) {
		Assert.notNull(name, "Parameter name must not be null");
		this.parameters.put(name, values);
	}

	/**
	 * 设置所有提供的参数，<strong>替换</strong>所提供的参数名的任何
	 * 现有值。若要在不替换现有值的情况下添加，
	 * 请使用 {@link #addParameters(java.util.Map)}。
	 */
	public void setParameters(Map<String, ?> params) {
		Assert.notNull(params, "Parameter map must not be null");
		params.forEach((key, value) -> {
			if (value instanceof String) {
				setParameter(key, (String) value);
			}
			else if (value instanceof String[]) {
				setParameter(key, (String[]) value);
			}
			else {
				throw new IllegalArgumentException(
						"Parameter map value must be single value " + " or array of type [" + String.class.getName() + "]");
			}
		});
	}

	/**
	 * 为指定的 HTTP 参数添加单个值。
	 * <p>如果已为给定参数名注册了一个或多个值，
	 * 给定的值将添加到列表末尾。
	 */
	public void addParameter(String name, @Nullable String value) {
		addParameter(name, new String[] {value});
	}

	/**
	 * 为指定的 HTTP 参数添加值数组。
	 * <p>如果已为给定参数名注册了一个或多个值，
	 * 给定的值将添加到列表末尾。
	 */
	public void addParameter(String name, String... values) {
		Assert.notNull(name, "Parameter name must not be null");
		String[] oldArr = this.parameters.get(name);
		if (oldArr != null) {
			String[] newArr = new String[oldArr.length + values.length];
			System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
			System.arraycopy(values, 0, newArr, oldArr.length, values.length);
			this.parameters.put(name, newArr);
		}
		else {
			this.parameters.put(name, values);
		}
	}

	/**
	 * 添加所有提供的参数，<strong>不</strong>替换任何
	 * 现有值。若要替换现有值，请使用
	 * {@link #setParameters(java.util.Map)}。
	 */
	public void addParameters(Map<String, ?> params) {
		Assert.notNull(params, "Parameter map must not be null");
		params.forEach((key, value) -> {
			if (value instanceof String) {
				addParameter(key, (String) value);
			}
			else if (value instanceof String[]) {
				addParameter(key, (String[]) value);
			}
			else {
				throw new IllegalArgumentException("Parameter map value must be single value " +
						" or array of type [" + String.class.getName() + "]");
			}
		});
	}

	/**
	 * 移除指定 HTTP 参数已注册的值（如果有）。
	 */
	public void removeParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		this.parameters.remove(name);
	}

	/**
	 * 移除所有现有参数。
	 */
	public void removeAllParameters() {
		this.parameters.clear();
	}

	@Override
	@Nullable
	public String getParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		String[] arr = this.parameters.get(name);
		return (arr != null && arr.length > 0 ? arr[0] : null);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(this.parameters.keySet());
	}

	@Override
	public String[] getParameterValues(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		return this.parameters.get(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return Collections.unmodifiableMap(this.parameters);
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@Override
	public String getProtocol() {
		return this.protocol;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	@Override
	public String getScheme() {
		return this.scheme;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	@Override
	public String getServerName() {
		String rawHostHeader = getHeader(HttpHeaders.HOST);
		String host = rawHostHeader;
		if (host != null) {
			host = host.trim();
			if (host.startsWith("[")) {
				int indexOfClosingBracket = host.indexOf(']');
				Assert.state(indexOfClosingBracket > -1, () -> "Invalid Host header: " + rawHostHeader);
				host = host.substring(0, indexOfClosingBracket + 1);
			}
			else if (host.contains(":")) {
				host = host.substring(0, host.indexOf(':'));
			}
			return host;
		}

		// else
		return this.serverName;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	@Override
	public int getServerPort() {
		String rawHostHeader = getHeader(HttpHeaders.HOST);
		String host = rawHostHeader;
		if (host != null) {
			host = host.trim();
			int idx;
			if (host.startsWith("[")) {
				int indexOfClosingBracket = host.indexOf(']');
				Assert.state(indexOfClosingBracket > -1, () -> "Invalid Host header: " + rawHostHeader);
				idx = host.indexOf(':', indexOfClosingBracket);
			}
			else {
				idx = host.indexOf(':');
			}
			if (idx != -1) {
				return Integer.parseInt(host.substring(idx + 1));
			}
		}

		// else
		return this.serverPort;
	}

	@Override
	public BufferedReader getReader() throws UnsupportedEncodingException {
		if (this.reader != null) {
			return this.reader;
		}
		else if (this.inputStream != null) {
			throw new IllegalStateException(
					"Cannot call getReader() after getInputStream() has already been called for the current request")			;
		}

		if (this.content != null) {
			InputStream sourceStream = new ByteArrayInputStream(this.content);
			Reader sourceReader = (this.characterEncoding != null) ?
					new InputStreamReader(sourceStream, this.characterEncoding) :
					new InputStreamReader(sourceStream);
			this.reader = new BufferedReader(sourceReader);
		}
		else {
			this.reader = EMPTY_BUFFERED_READER;
		}
		return this.reader;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	@Override
	public String getRemoteAddr() {
		return this.remoteAddr;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	@Override
	public String getRemoteHost() {
		return this.remoteHost;
	}

	@Override
	public void setAttribute(String name, @Nullable Object value) {
		checkActive();
		Assert.notNull(name, "Attribute name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	@Override
	public void removeAttribute(String name) {
		checkActive();
		Assert.notNull(name, "Attribute name must not be null");
		this.attributes.remove(name);
	}

	/**
	 * 清除此请求的所有属性。
	 */
	public void clearAttributes() {
		this.attributes.clear();
	}

	/**
	 * 在所有现有区域设置之前添加新的首选区域设置。
	 * @see #setPreferredLocales
	 */
	public void addPreferredLocale(Locale locale) {
		Assert.notNull(locale, "Locale must not be null");
		this.locales.addFirst(locale);
		updateAcceptLanguageHeader();
	}

	/**
	 * 按降序设置首选区域设置列表，实际上将替换
	 * 所有现有区域设置。
	 * @since 3.2
	 * @see #addPreferredLocale
	 */
	public void setPreferredLocales(List<Locale> locales) {
		Assert.notEmpty(locales, "Locale list must not be empty");
		this.locales.clear();
		this.locales.addAll(locales);
		updateAcceptLanguageHeader();
	}

	private void updateAcceptLanguageHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAcceptLanguageAsLocales(this.locales);
		doAddHeaderValue(HttpHeaders.ACCEPT_LANGUAGE, headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE), true);
	}

	/**
	 * 返回此 mock 请求中配置的第一个首选
	 * {@linkplain Locale 区域设置}。
	 * <p>如果未显式配置任何区域设置，则此
	 * 请求所模拟的<em>服务器</em>的默认首选
	 * {@link Locale} 为 {@link Locale#ENGLISH}。
	 * <p>与 Servlet 规范不同，此 mock 实现
	 * <strong>不</strong>考虑通过 {@code Accept-Language} 头
	 * 指定的任何区域设置。
	 * @see javax.servlet.ServletRequest#getLocale()
	 * @see #addPreferredLocale(Locale)
	 * @see #setPreferredLocales(List)
	 */
	@Override
	public Locale getLocale() {
		return this.locales.getFirst();
	}

	/**
	 * 返回此 mock 请求中配置的首选
	 * {@linkplain Locale 区域设置}的 {@linkplain Enumeration 枚举}。
	 * <p>如果未显式配置任何区域设置，则此
	 * 请求所模拟的<em>服务器</em>的默认首选
	 * {@link Locale} 为 {@link Locale#ENGLISH}。
	 * <p>与 Servlet 规范不同，此 mock 实现
	 * <strong>不</strong>考虑通过 {@code Accept-Language} 头
	 * 指定的任何区域设置。
	 * @see javax.servlet.ServletRequest#getLocales()
	 * @see #addPreferredLocale(Locale)
	 * @see #setPreferredLocales(List)
	 */
	@Override
	public Enumeration<Locale> getLocales() {
		return Collections.enumeration(this.locales);
	}

	/**
	 * 设置布尔型 {@code secure} 标志，指示该 mock 请求
	 * 是否通过安全通道（如 HTTPS）发出。
	 * @see #isSecure()
	 * @see #getScheme()
	 * @see #setScheme(String)
	 */
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	/**
	 * 如果 {@link #setSecure secure} 标志已设置为
	 * {@code true}，或 {@link #getScheme scheme} 为 {@code https}，则返回 {@code true}。
	 * @see javax.servlet.ServletRequest#isSecure()
	 */
	@Override
	public boolean isSecure() {
		return (this.secure || HTTPS.equalsIgnoreCase(this.scheme));
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return new MockRequestDispatcher(path);
	}

	@Override
	@Deprecated
	public String getRealPath(String path) {
		return this.servletContext.getRealPath(path);
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	@Override
	public int getRemotePort() {
		return this.remotePort;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	@Override
	public String getLocalName() {
		return this.localName;
	}

	public void setLocalAddr(String localAddr) {
		this.localAddr = localAddr;
	}

	@Override
	public String getLocalAddr() {
		return this.localAddr;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	@Override
	public int getLocalPort() {
		return this.localPort;
	}

	@Override
	public AsyncContext startAsync() {
		return startAsync(this, null);
	}

	@Override
	public AsyncContext startAsync(ServletRequest request, @Nullable ServletResponse response) {
		Assert.state(this.asyncSupported, "Async not supported");
		this.asyncStarted = true;
		this.asyncContext = new MockAsyncContext(request, response);
		return this.asyncContext;
	}

	public void setAsyncStarted(boolean asyncStarted) {
		this.asyncStarted = asyncStarted;
	}

	@Override
	public boolean isAsyncStarted() {
		return this.asyncStarted;
	}

	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	@Override
	public boolean isAsyncSupported() {
		return this.asyncSupported;
	}

	public void setAsyncContext(@Nullable MockAsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}

	@Override
	@Nullable
	public AsyncContext getAsyncContext() {
		return this.asyncContext;
	}

	public void setDispatcherType(DispatcherType dispatcherType) {
		this.dispatcherType = dispatcherType;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return this.dispatcherType;
	}


	// ---------------------------------------------------------------------
	// HttpServletRequest interface
	// ---------------------------------------------------------------------

	public void setAuthType(@Nullable String authType) {
		this.authType = authType;
	}

	@Override
	@Nullable
	public String getAuthType() {
		return this.authType;
	}

	public void setCookies(@Nullable Cookie... cookies) {
		this.cookies = (ObjectUtils.isEmpty(cookies) ? null : cookies);
		if (this.cookies == null) {
			removeHeader(HttpHeaders.COOKIE);
		}
		else {
			doAddHeaderValue(HttpHeaders.COOKIE, encodeCookies(this.cookies), true);
		}
	}

	private static String encodeCookies(@NonNull Cookie... cookies) {
		return Arrays.stream(cookies)
				.map(c -> c.getName() + '=' + (c.getValue() == null ? "" : c.getValue()))
				.collect(Collectors.joining("; "));
	}

	@Override
	@Nullable
	public Cookie[] getCookies() {
		return this.cookies;
	}

	/**
	 * 为给定名称添加 HTTP 头条目。
	 * <p>虽然此方法可以接受任何 {@code Object} 作为参数，
	 * 但建议使用以下类型：
	 * <ul>
	 * <li>String 或任何将通过 {@code toString()} 转换的 Object；请参见 {@link #getHeader}。</li>
	 * <li>用于日期头的 String、Number 或 Date；请参见 {@link #getDateHeader}。</li>
	 * <li>用于整数头的 String 或 Number；请参见 {@link #getIntHeader}。</li>
	 * <li>用于多个值的 {@code String[]} 或 {@code Collection<String>}；请参见 {@link #getHeaders}。</li>
	 * </ul>
	 * @see #getHeaderNames
	 * @see #getHeaders
	 * @see #getHeader
	 * @see #getDateHeader
	 */
	public void addHeader(String name, Object value) {
		if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name) &&
				!this.headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
			setContentType(value.toString());
		}
		else if (HttpHeaders.ACCEPT_LANGUAGE.equalsIgnoreCase(name) &&
				!this.headers.containsKey(HttpHeaders.ACCEPT_LANGUAGE)) {
			try {
				HttpHeaders headers = new HttpHeaders();
				headers.add(HttpHeaders.ACCEPT_LANGUAGE, value.toString());
				List<Locale> locales = headers.getAcceptLanguageAsLocales();
				this.locales.clear();
				this.locales.addAll(locales);
				if (this.locales.isEmpty()) {
					this.locales.add(Locale.ENGLISH);
				}
			}
			catch (IllegalArgumentException ex) {
				// Invalid Accept-Language format -> just store plain header
			}
			doAddHeaderValue(name, value, true);
		}
		else {
			doAddHeaderValue(name, value, false);
		}
	}

	private void doAddHeaderValue(String name, @Nullable Object value, boolean replace) {
		HeaderValueHolder header = this.headers.get(name);
		Assert.notNull(value, "Header value must not be null");
		if (header == null || replace) {
			header = new HeaderValueHolder();
			this.headers.put(name, header);
		}
		if (value instanceof Collection) {
			header.addValues((Collection<?>) value);
		}
		else if (value.getClass().isArray()) {
			header.addValueArray(value);
		}
		else {
			header.addValue(value);
		}
	}

	/**
	 * 移除指定 HTTP 头已注册的条目（如果有）。
	 * @since 4.3.20
	 */
	public void removeHeader(String name) {
		Assert.notNull(name, "Header name must not be null");
		this.headers.remove(name);
	}

	/**
	 * 返回给定 {@code name} 的日期头的长整型时间戳。
	 * <p>如果内部值表示为 String，此方法将尝试
	 * 使用支持的日期格式将其解析为日期：
	 * <ul>
	 * <li>"EEE, dd MMM yyyy HH:mm:ss zzz"</li>
	 * <li>"EEE, dd-MMM-yy HH:mm:ss zzz"</li>
	 * <li>"EEE MMM dd HH:mm:ss yyyy"</li>
	 * </ul>
	 * @param name 头名称
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	@Override
	public long getDateHeader(String name) {
		HeaderValueHolder header = this.headers.get(name);
		Object value = (header != null ? header.getValue() : null);
		if (value instanceof Date) {
			return ((Date) value).getTime();
		}
		else if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		else if (value instanceof String) {
			return parseDateHeader(name, (String) value);
		}
		else if (value != null) {
			throw new IllegalArgumentException(
					"Value for header '" + name + "' is not a Date, Number, or String: " + value);
		}
		else {
			return -1L;
		}
	}

	private long parseDateHeader(String name, String value) {
		for (String dateFormat : DATE_FORMATS) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
			simpleDateFormat.setTimeZone(GMT);
			try {
				return simpleDateFormat.parse(value).getTime();
			}
			catch (ParseException ex) {
				// ignore
			}
		}
		throw new IllegalArgumentException("Cannot parse date value '" + value + "' for '" + name + "' header");
	}

	@Override
	@Nullable
	public String getHeader(String name) {
		HeaderValueHolder header = this.headers.get(name);
		return (header != null ? header.getStringValue() : null);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		HeaderValueHolder header = this.headers.get(name);
		return Collections.enumeration(header != null ? header.getStringValues() : new LinkedList<>());
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(this.headers.keySet());
	}

	@Override
	public int getIntHeader(String name) {
		HeaderValueHolder header = this.headers.get(name);
		Object value = (header != null ? header.getValue() : null);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		else if (value instanceof String) {
			return Integer.parseInt((String) value);
		}
		else if (value != null) {
			throw new NumberFormatException("Value for header '" + name + "' is not a Number: " + value);
		}
		else {
			return -1;
		}
	}

	public void setMethod(@Nullable String method) {
		this.method = method;
	}

	@Override
	@Nullable
	public String getMethod() {
		return this.method;
	}

	public void setPathInfo(@Nullable String pathInfo) {
		this.pathInfo = pathInfo;
	}

	@Override
	@Nullable
	public String getPathInfo() {
		return this.pathInfo;
	}

	@Override
	@Nullable
	public String getPathTranslated() {
		return (this.pathInfo != null ? getRealPath(this.pathInfo) : null);
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	@Override
	public String getContextPath() {
		return this.contextPath;
	}

	public void setQueryString(@Nullable String queryString) {
		this.queryString = queryString;
	}

	@Override
	@Nullable
	public String getQueryString() {
		return this.queryString;
	}

	public void setRemoteUser(@Nullable String remoteUser) {
		this.remoteUser = remoteUser;
	}

	@Override
	@Nullable
	public String getRemoteUser() {
		return this.remoteUser;
	}

	public void addUserRole(String role) {
		this.userRoles.add(role);
	}

	@Override
	public boolean isUserInRole(String role) {
		return (this.userRoles.contains(role) || (this.servletContext instanceof MockServletContext &&
				((MockServletContext) this.servletContext).getDeclaredRoles().contains(role)));
	}

	public void setUserPrincipal(@Nullable Principal userPrincipal) {
		this.userPrincipal = userPrincipal;
	}

	@Override
	@Nullable
	public Principal getUserPrincipal() {
		return this.userPrincipal;
	}

	public void setRequestedSessionId(@Nullable String requestedSessionId) {
		this.requestedSessionId = requestedSessionId;
	}

	@Override
	@Nullable
	public String getRequestedSessionId() {
		return this.requestedSessionId;
	}

	public void setRequestURI(@Nullable String requestURI) {
		this.requestURI = requestURI;
	}

	@Override
	@Nullable
	public String getRequestURI() {
		return this.requestURI;
	}

	@Override
	public StringBuffer getRequestURL() {
		String scheme = getScheme();
		String server = getServerName();
		int port = getServerPort();
		String uri = getRequestURI();

		StringBuffer url = new StringBuffer(scheme).append("://").append(server);
		if (port > 0 && ((HTTP.equalsIgnoreCase(scheme) && port != 80) ||
				(HTTPS.equalsIgnoreCase(scheme) && port != 443))) {
			url.append(':').append(port);
		}
		if (StringUtils.hasText(uri)) {
			url.append(uri);
		}
		return url;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	@Override
	public String getServletPath() {
		return this.servletPath;
	}

	public void setSession(HttpSession session) {
		this.session = session;
		if (session instanceof MockHttpSession) {
			MockHttpSession mockSession = ((MockHttpSession) session);
			mockSession.access();
		}
	}

	@Override
	@Nullable
	public HttpSession getSession(boolean create) {
		checkActive();
		// Reset session if invalidated.
		if (this.session instanceof MockHttpSession && ((MockHttpSession) this.session).isInvalid()) {
			this.session = null;
		}
		// Create new session if necessary.
		if (this.session == null && create) {
			this.session = new MockHttpSession(this.servletContext);
		}
		return this.session;
	}

	@Override
	@Nullable
	public HttpSession getSession() {
		return getSession(true);
	}

	/**
	 * 此方法（Servlet 3.1+）的实现会在会话为 mock 会话时
	 * 调用 {@link MockHttpSession#changeSessionId()}。
	 * 否则，仅返回当前会话 id。
	 * @since 4.0.3
	 */
	@Override
	public String changeSessionId() {
		Assert.isTrue(this.session != null, "The request does not have a session");
		if (this.session instanceof MockHttpSession) {
			return ((MockHttpSession) this.session).changeSessionId();
		}
		return this.session.getId();
	}

	public void setRequestedSessionIdValid(boolean requestedSessionIdValid) {
		this.requestedSessionIdValid = requestedSessionIdValid;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return this.requestedSessionIdValid;
	}

	public void setRequestedSessionIdFromCookie(boolean requestedSessionIdFromCookie) {
		this.requestedSessionIdFromCookie = requestedSessionIdFromCookie;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return this.requestedSessionIdFromCookie;
	}

	public void setRequestedSessionIdFromURL(boolean requestedSessionIdFromURL) {
		this.requestedSessionIdFromURL = requestedSessionIdFromURL;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return this.requestedSessionIdFromURL;
	}

	@Override
	@Deprecated
	public boolean isRequestedSessionIdFromUrl() {
		return isRequestedSessionIdFromURL();
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void login(String username, String password) throws ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void logout() throws ServletException {
		this.userPrincipal = null;
		this.remoteUser = null;
		this.authType = null;
	}

	public void addPart(Part part) {
		this.parts.add(part.getName(), part);
	}

	@Override
	@Nullable
	public Part getPart(String name) throws IOException, ServletException {
		return this.parts.getFirst(name);
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		List<Part> result = new LinkedList<>();
		for (List<Part> list : this.parts.values()) {
			result.addAll(list);
		}
		return result;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		throw new UnsupportedOperationException();
	}

}
