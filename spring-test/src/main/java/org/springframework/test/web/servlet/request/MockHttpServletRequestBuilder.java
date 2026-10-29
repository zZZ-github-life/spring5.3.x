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

package org.springframework.test.web.servlet.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.springframework.beans.Mergeable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 在 {@link MockMvc} 中执行请求时作为输入所需的 {@link MockHttpServletRequest} 的默认构建器。
 *
 * <p>应用测试通常通过 {@link MockMvcRequestBuilders} 中的静态工厂方法访问此构建器。
 *
 * <p>此类不开放扩展。要对创建的 {@code MockHttpServletRequest} 应用自定义初始化，
 * 请使用 {@link #with(RequestPostProcessor)} 扩展点。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Kamill Sokol
 * @since 3.2
 */
public class MockHttpServletRequestBuilder
		implements ConfigurableSmartRequestBuilder<MockHttpServletRequestBuilder>, Mergeable {

	private final String method;

	private final URI url;

	private String contextPath = "";

	private String servletPath = "";

	@Nullable
	private String pathInfo = "";

	@Nullable
	private Boolean secure;

	@Nullable
	private Principal principal;

	@Nullable
	private MockHttpSession session;

	@Nullable
	private String characterEncoding;

	@Nullable
	private byte[] content;

	@Nullable
	private String contentType;

	private final MultiValueMap<String, Object> headers = new LinkedMultiValueMap<>();

	private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

	private final List<Cookie> cookies = new ArrayList<>();

	private final List<Locale> locales = new ArrayList<>();

	private final Map<String, Object> requestAttributes = new LinkedHashMap<>();

	private final Map<String, Object> sessionAttributes = new LinkedHashMap<>();

	private final Map<String, Object> flashAttributes = new LinkedHashMap<>();

	private final List<RequestPostProcessor> postProcessors = new ArrayList<>();


	/**
	 * 包级私有构造函数。要获取实例，请使用 {@link MockMvcRequestBuilders} 中的静态工厂方法。
	 * <p>尽管此类无法被继承，但可以通过 {@link #with(RequestPostProcessor)}
	 * 插入其他初始化 {@code MockHttpServletRequest} 的方式。
	 * @param httpMethod the HTTP method (GET, POST, etc)
	 * @param url a URL template; the resulting URL will be encoded
	 * @param vars zero or more URI variables
	 */
	MockHttpServletRequestBuilder(HttpMethod httpMethod, String url, Object... vars) {
		this(httpMethod.name(), initUri(url, vars));
	}

	private static URI initUri(String url, Object[] vars) {
		Assert.notNull(url, "'url' must not be null");
		Assert.isTrue(url.startsWith("/") || url.startsWith("http://") || url.startsWith("https://"),
				() -> "'url' should start with a path or be a complete HTTP URL: " + url);
		return UriComponentsBuilder.fromUriString(url).buildAndExpand(vars).encode().toUri();
	}

	/**
	 * {@link #MockHttpServletRequestBuilder(HttpMethod, String, Object...)} 的替代方案，
	 * 使用预先构建的 URI。
	 * @param httpMethod the HTTP method (GET, POST, etc)
	 * @param url the URL
	 * @since 4.0.3
	 */
	MockHttpServletRequestBuilder(HttpMethod httpMethod, URI url) {
		this(httpMethod.name(), url);
	}

	/**
	 * 用于自定义 HTTP 方法的替代构造函数。
	 * @param httpMethod the HTTP method (GET, POST, etc)
	 * @param url the URL
	 * @since 4.3
	 */
	MockHttpServletRequestBuilder(String httpMethod, URI url) {
		Assert.notNull(httpMethod, "'httpMethod' is required");
		Assert.notNull(url, "'url' is required");
		this.method = httpMethod;
		this.url = url;
	}


	/**
	 * 指定 requestURI 中表示上下文路径的部分。如果指定了上下文路径，它必须与请求 URI 的开头匹配。
	 * <p>在大多数情况下，编写测试时可以省略 requestURI 中的上下文路径。
	 * 这是因为大多数应用实际上并不依赖其部署时使用的名称。如果在此处指定，
	 * 上下文路径必须以 “/” 开头，并且不能以 “/” 结尾。
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	public MockHttpServletRequestBuilder contextPath(String contextPath) {
		if (StringUtils.hasText(contextPath)) {
			Assert.isTrue(contextPath.startsWith("/"), "Context path must start with a '/'");
			Assert.isTrue(!contextPath.endsWith("/"), "Context path must not end with a '/'");
		}
		this.contextPath = contextPath;
		return this;
	}

	/**
	 * 指定 requestURI 中表示 Servlet 映射路径的部分。这通常是 requestURI 中上下文路径之后的部分。
	 * <p>在大多数情况下，编写测试时可以省略 requestURI 中的 servlet 路径。
	 * 这是因为大多数应用实际上并不依赖 Servlet 映射的前缀。例如，如果 Servlet 映射到
	 * {@code "/main/*"}，测试可以使用 requestURI {@code "/accounts/1"} 而非
	 * {@code "/main/accounts/1"} 来编写。如果在此处指定，servletPath 必须以 “/” 开头，
	 * 并且不能以 “/” 结尾。
	 * @see javax.servlet.http.HttpServletRequest#getServletPath()
	 */
	public MockHttpServletRequestBuilder servletPath(String servletPath) {
		if (StringUtils.hasText(servletPath)) {
			Assert.isTrue(servletPath.startsWith("/"), "Servlet path must start with a '/'");
			Assert.isTrue(!servletPath.endsWith("/"), "Servlet path must not end with a '/'");
		}
		this.servletPath = servletPath;
		return this;
	}

	/**
	 * 指定 requestURI 中表示 pathInfo 的部分。
	 * <p>如果未指定（推荐），pathInfo 将通过从 requestURI 中移除 contextPath 和 servletPath
	 * 并使用剩余部分来自动推导。如果在此处指定，pathInfo 必须以 “/” 开头。
	 * <p>如果指定了，pathInfo 将按原样使用。
	 * @see javax.servlet.http.HttpServletRequest#getPathInfo()
	 */
	public MockHttpServletRequestBuilder pathInfo(@Nullable String pathInfo) {
		if (StringUtils.hasText(pathInfo)) {
			Assert.isTrue(pathInfo.startsWith("/"), "Path info must start with a '/'");
		}
		this.pathInfo = pathInfo;
		return this;
	}

	/**
	 * 设置 {@link ServletRequest} 的 secure 属性，以指示使用安全通道（如 HTTPS）。
	 * @param secure whether the request is using a secure channel
	 */
	public MockHttpServletRequestBuilder secure(boolean secure){
		this.secure = secure;
		return this;
	}

	/**
	 * 设置请求的字符编码。
	 * @param encoding the character encoding
	 * @since 5.3.10
	 * @see StandardCharsets
	 * @see #characterEncoding(String)
	 */
	public MockHttpServletRequestBuilder characterEncoding(Charset encoding) {
		return this.characterEncoding(encoding.name());
	}

	/**
	 * 设置请求的字符编码。
	 * @param encoding the character encoding
	 */
	public MockHttpServletRequestBuilder characterEncoding(String encoding) {
		this.characterEncoding = encoding;
		return this;
	}

	/**
	 * 设置请求体。
	 * <p>如果提供了内容，且 {@link #contentType(MediaType)} 设置为
	 * {@code application/x-www-form-urlencoded}，则会解析该内容并用其填充
	 * {@link #param(String, String...) request parameters} 映射。
	 * @param content the body content
	 */
	public MockHttpServletRequestBuilder content(byte[] content) {
		this.content = content;
		return this;
	}

	/**
	 * 将请求体设置为 UTF-8 字符串。
	 * <p>如果提供了内容，且 {@link #contentType(MediaType)} 设置为
	 * {@code application/x-www-form-urlencoded}，则会解析该内容并用其填充
	 * {@link #param(String, String...) request parameters} 映射。
	 * @param content the body content
	 */
	public MockHttpServletRequestBuilder content(String content) {
		this.content = content.getBytes(StandardCharsets.UTF_8);
		return this;
	}

	/**
	 * 设置请求的 'Content-Type' 头。
	 * <p>如果提供了内容，且 {@code contentType} 设置为
	 * {@code application/x-www-form-urlencoded}，则会解析该内容并用其填充
	 * {@link #param(String, String...) request parameters} 映射。
	 * @param contentType the content type
	 */
	public MockHttpServletRequestBuilder contentType(MediaType contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType.toString();
		return this;
	}

	/**
	 * 以原始字符串值设置请求的 'Content-Type' 头，
	 * 该值甚至可能不符合规范（用于测试目的）。
	 * @param contentType the content type
	 * @since 4.1.2
	 */
	public MockHttpServletRequestBuilder contentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
		return this;
	}

	/**
	 * 将 'Accept' 头设置为给定的媒体类型。
	 * @param mediaTypes one or more media types
	 */
	public MockHttpServletRequestBuilder accept(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		this.headers.set("Accept", MediaType.toString(Arrays.asList(mediaTypes)));
		return this;
	}

	/**
	 * 使用原始字符串值设置 {@code Accept} 头，
	 * 这些值甚至可能不符合规范（用于测试目的）。
	 * @param mediaTypes one or more media types; internally joined as
	 * comma-separated String
	 */
	public MockHttpServletRequestBuilder accept(String... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		this.headers.set("Accept", String.join(", ", mediaTypes));
		return this;
	}

	/**
	 * 向请求中添加一个头。值总是被追加。
	 * @param name the header name
	 * @param values one or more header values
	 */
	public MockHttpServletRequestBuilder header(String name, Object... values) {
		addToMultiValueMap(this.headers, name, values);
		return this;
	}

	/**
	 * 向请求中添加所有头。值总是被追加。
	 * @param httpHeaders the headers and values to add
	 */
	public MockHttpServletRequestBuilder headers(HttpHeaders httpHeaders) {
		httpHeaders.forEach(this.headers::addAll);
		return this;
	}

	/**
	 * 向 {@link MockHttpServletRequest#getParameterMap()} 中添加一个请求参数。
	 * <p>在 Servlet API 中，请求参数可以从查询字符串和/或
	 * {@code application/x-www-form-urlencoded} 请求体中解析得到。此方法只是添加到请求参数映射中。
	 * 你还可以通过以下方式之一指定查询或表单数据来添加 Servlet 请求参数：
	 * <ul>
	 * <li>向 {@link MockMvcRequestBuilders} 提供带查询字符串的 URL。
	 * <li>通过 {@link #queryParam} 或 {@link #queryParams} 添加查询参数。
	 * <li>提供 {@link #content}，并将 {@link #contentType} 设为
	 * {@code application/x-www-form-urlencoded}。
	 * </ul>
	 * @param name the parameter name
	 * @param values one or more values
	 */
	public MockHttpServletRequestBuilder param(String name, String... values) {
		addToMultiValueMap(this.parameters, name, values);
		return this;
	}

	/**
	 * {@link #param(String, String...)} 的变体，使用 {@link MultiValueMap}。
	 * @param params the parameters to add
	 * @since 4.2.4
	 */
	public MockHttpServletRequestBuilder params(MultiValueMap<String, String> params) {
		params.forEach((name, values) -> {
			for (String value : values) {
				this.parameters.add(name, value);
			}
		});
		return this;
	}

	/**
	 * 追加到查询字符串，同时也添加到
	 * {@link #param(String, String...) request parameters} 映射。参数名称和值
	 * 在添加到查询字符串时会被编码。
	 * @param name the parameter name
	 * @param values one or more values
	 * @since 5.2.2
	 */
	public MockHttpServletRequestBuilder queryParam(String name, String... values) {
		param(name, values);
		this.queryParams.addAll(name, Arrays.asList(values));
		return this;
	}

	/**
	 * 追加到查询字符串，同时也添加到
	 * {@link #params(MultiValueMap) request parameters} 映射。参数名称和值
	 * 在添加到查询字符串时会被编码。
	 * @param params the parameters to add
	 * @since 5.2.2
	 */
	public MockHttpServletRequestBuilder queryParams(MultiValueMap<String, String> params) {
		params(params);
		this.queryParams.addAll(params);
		return this;
	}

	/**
	 * 向请求中添加给定的 cookie。Cookie 总是被追加。
	 * @param cookies the cookies to add
	 */
	public MockHttpServletRequestBuilder cookie(Cookie... cookies) {
		Assert.notEmpty(cookies, "'cookies' must not be empty");
		this.cookies.addAll(Arrays.asList(cookies));
		return this;
	}

	/**
	 * 将指定的区域设置添加为首选请求区域设置。
	 * @param locales the locales to add
	 * @since 4.3.6
	 * @see #locale(Locale)
	 */
	public MockHttpServletRequestBuilder locale(Locale... locales) {
		Assert.notEmpty(locales, "'locales' must not be empty");
		this.locales.addAll(Arrays.asList(locales));
		return this;
	}

	/**
	 * 设置请求的区域设置，覆盖之前的任何区域设置。
	 * @param locale the locale, or {@code null} to reset it
	 * @see #locale(Locale...)
	 */
	public MockHttpServletRequestBuilder locale(@Nullable Locale locale) {
		this.locales.clear();
		if (locale != null) {
			this.locales.add(locale);
		}
		return this;
	}

	/**
	 * 设置请求属性。
	 * @param name the attribute name
	 * @param value the attribute value
	 */
	public MockHttpServletRequestBuilder requestAttr(String name, Object value) {
		addToMap(this.requestAttributes, name, value);
		return this;
	}

	/**
	 * 设置会话属性。
	 * @param name the session attribute name
	 * @param value the session attribute value
	 */
	public MockHttpServletRequestBuilder sessionAttr(String name, Object value) {
		addToMap(this.sessionAttributes, name, value);
		return this;
	}

	/**
	 * 设置会话属性。
	 * @param sessionAttributes the session attributes
	 */
	public MockHttpServletRequestBuilder sessionAttrs(Map<String, Object> sessionAttributes) {
		Assert.notEmpty(sessionAttributes, "'sessionAttributes' must not be empty");
		sessionAttributes.forEach(this::sessionAttr);
		return this;
	}

	/**
	 * 设置一个 “input” flash 属性。
	 * @param name the flash attribute name
	 * @param value the flash attribute value
	 */
	public MockHttpServletRequestBuilder flashAttr(String name, Object value) {
		addToMap(this.flashAttributes, name, value);
		return this;
	}

	/**
	 * 设置 flash 属性。
	 * @param flashAttributes the flash attributes
	 */
	public MockHttpServletRequestBuilder flashAttrs(Map<String, Object> flashAttributes) {
		Assert.notEmpty(flashAttributes, "'flashAttributes' must not be empty");
		flashAttributes.forEach(this::flashAttr);
		return this;
	}

	/**
	 * 设置要使用的 HTTP 会话，该会话可以跨请求重用。
	 * <p>通过 {@link #sessionAttr(String, Object)} 提供的单个属性
	 * 会覆盖此处提供的会话内容。
	 * @param session the HTTP session
	 */
	public MockHttpServletRequestBuilder session(MockHttpSession session) {
		Assert.notNull(session, "'session' must not be null");
		this.session = session;
		return this;
	}

	/**
	 * 设置请求的 principal。
	 * @param principal the principal
	 */
	public MockHttpServletRequestBuilder principal(Principal principal) {
		Assert.notNull(principal, "'principal' must not be null");
		this.principal = principal;
		return this;
	}

	/**
	 * 一个扩展点，用于以未直接内置于 {@code MockHttpServletRequestBuilder} 的方式
	 * 进一步初始化 {@link MockHttpServletRequest}。此接口的实现自身可以拥有
	 * 构建器风格的方法，并通过静态工厂方法提供访问。
	 * @param postProcessor a post-processor to add
	 */
	@Override
	public MockHttpServletRequestBuilder with(RequestPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "postProcessor is required");
		this.postProcessors.add(postProcessor);
		return this;
	}


	/**
	 * {@inheritDoc}
	 * @return always returns {@code true}.
	 */
	@Override
	public boolean isMergeEnabled() {
		return true;
	}

	/**
	 * 合并 “parent” RequestBuilder 的属性，
	 * 仅当 “this” 实例中尚未设置相应值时才接受这些值。
	 * @param parent the parent {@code RequestBuilder} to inherit properties from
	 * @return the result of the merge
	 */
	@Override
	public Object merge(@Nullable Object parent) {
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof MockHttpServletRequestBuilder)) {
			throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
		}
		MockHttpServletRequestBuilder parentBuilder = (MockHttpServletRequestBuilder) parent;

		if (!StringUtils.hasText(this.contextPath)) {
			this.contextPath = parentBuilder.contextPath;
		}
		if (!StringUtils.hasText(this.servletPath)) {
			this.servletPath = parentBuilder.servletPath;
		}
		if ("".equals(this.pathInfo)) {
			this.pathInfo = parentBuilder.pathInfo;
		}

		if (this.secure == null) {
			this.secure = parentBuilder.secure;
		}
		if (this.principal == null) {
			this.principal = parentBuilder.principal;
		}
		if (this.session == null) {
			this.session = parentBuilder.session;
		}

		if (this.characterEncoding == null) {
			this.characterEncoding = parentBuilder.characterEncoding;
		}
		if (this.content == null) {
			this.content = parentBuilder.content;
		}
		if (this.contentType == null) {
			this.contentType = parentBuilder.contentType;
		}

		for (Map.Entry<String, List<Object>> entry : parentBuilder.headers.entrySet()) {
			String headerName = entry.getKey();
			if (!this.headers.containsKey(headerName)) {
				this.headers.put(headerName, entry.getValue());
			}
		}
		for (Map.Entry<String, List<String>> entry : parentBuilder.parameters.entrySet()) {
			String paramName = entry.getKey();
			if (!this.parameters.containsKey(paramName)) {
				this.parameters.put(paramName, entry.getValue());
			}
		}
		for (Map.Entry<String, List<String>> entry : parentBuilder.queryParams.entrySet()) {
			String paramName = entry.getKey();
			if (!this.queryParams.containsKey(paramName)) {
				this.queryParams.put(paramName, entry.getValue());
			}
		}
		for (Cookie cookie : parentBuilder.cookies) {
			if (!containsCookie(cookie)) {
				this.cookies.add(cookie);
			}
		}
		for (Locale locale : parentBuilder.locales) {
			if (!this.locales.contains(locale)) {
				this.locales.add(locale);
			}
		}

		for (Map.Entry<String, Object> entry : parentBuilder.requestAttributes.entrySet()) {
			String attributeName = entry.getKey();
			if (!this.requestAttributes.containsKey(attributeName)) {
				this.requestAttributes.put(attributeName, entry.getValue());
			}
		}
		for (Map.Entry<String, Object> entry : parentBuilder.sessionAttributes.entrySet()) {
			String attributeName = entry.getKey();
			if (!this.sessionAttributes.containsKey(attributeName)) {
				this.sessionAttributes.put(attributeName, entry.getValue());
			}
		}
		for (Map.Entry<String, Object> entry : parentBuilder.flashAttributes.entrySet()) {
			String attributeName = entry.getKey();
			if (!this.flashAttributes.containsKey(attributeName)) {
				this.flashAttributes.put(attributeName, entry.getValue());
			}
		}

		this.postProcessors.addAll(0, parentBuilder.postProcessors);

		return this;
	}

	private boolean containsCookie(Cookie cookie) {
		for (Cookie cookieToCheck : this.cookies) {
			if (ObjectUtils.nullSafeEquals(cookieToCheck.getName(), cookie.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 构建 {@link MockHttpServletRequest}。
	 */
	@Override
	public final MockHttpServletRequest buildRequest(ServletContext servletContext) {
		MockHttpServletRequest request = createServletRequest(servletContext);

		request.setAsyncSupported(true);
		request.setMethod(this.method);

		String requestUri = this.url.getRawPath();
		request.setRequestURI(requestUri);

		if (this.url.getScheme() != null) {
			request.setScheme(this.url.getScheme());
		}
		if (this.url.getHost() != null) {
			request.setServerName(this.url.getHost());
		}
		if (this.url.getPort() != -1) {
			request.setServerPort(this.url.getPort());
		}

		updatePathRequestProperties(request, requestUri);

		if (this.secure != null) {
			request.setSecure(this.secure);
		}
		if (this.principal != null) {
			request.setUserPrincipal(this.principal);
		}
		if (this.session != null) {
			request.setSession(this.session);
		}

		request.setCharacterEncoding(this.characterEncoding);
		request.setContent(this.content);
		request.setContentType(this.contentType);

		this.headers.forEach((name, values) -> {
			for (Object value : values) {
				request.addHeader(name, value);
			}
		});

		if (!ObjectUtils.isEmpty(this.content) &&
				!this.headers.containsKey(HttpHeaders.CONTENT_LENGTH) &&
				!this.headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {

			request.addHeader(HttpHeaders.CONTENT_LENGTH, this.content.length);
		}

		String query = this.url.getRawQuery();
		if (!this.queryParams.isEmpty()) {
			String str = UriComponentsBuilder.newInstance().queryParams(this.queryParams).build().encode().getQuery();
			query = StringUtils.hasLength(query) ? (query + "&" + str) : str;
		}
		if (query != null) {
			request.setQueryString(query);
		}
		addRequestParams(request, UriComponentsBuilder.fromUri(this.url).build().getQueryParams());

		this.parameters.forEach((name, values) -> {
			for (String value : values) {
				request.addParameter(name, value);
			}
		});

		if (this.content != null && this.content.length > 0) {
			String requestContentType = request.getContentType();
			if (requestContentType != null) {
				try {
					MediaType mediaType = MediaType.parseMediaType(requestContentType);
					if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) {
						addRequestParams(request, parseFormData(mediaType));
					}
				}
				catch (Exception ex) {
					// Must be invalid, ignore..
				}
			}
		}

		if (!ObjectUtils.isEmpty(this.cookies)) {
			request.setCookies(this.cookies.toArray(new Cookie[0]));
		}
		if (!ObjectUtils.isEmpty(this.locales)) {
			request.setPreferredLocales(this.locales);
		}

		this.requestAttributes.forEach(request::setAttribute);
		this.sessionAttributes.forEach((name, attribute) -> {
			HttpSession session = request.getSession();
			Assert.state(session != null, "No HttpSession");
			session.setAttribute(name, attribute);
		});

		FlashMap flashMap = new FlashMap();
		flashMap.putAll(this.flashAttributes);
		FlashMapManager flashMapManager = getFlashMapManager(request);
		flashMapManager.saveOutputFlashMap(flashMap, request, new MockHttpServletResponse());

		return request;
	}

	/**
	 * 基于提供的 {@code ServletContext} 创建新的 {@link MockHttpServletRequest}。
	 * <p>可以在子类中重写。
	 */
	protected MockHttpServletRequest createServletRequest(ServletContext servletContext) {
		return new MockHttpServletRequest(servletContext);
	}

	/**
	 * 更新请求的 contextPath、servletPath 和 pathInfo。
	 */
	private void updatePathRequestProperties(MockHttpServletRequest request, String requestUri) {
		if (!requestUri.startsWith(this.contextPath)) {
			throw new IllegalArgumentException(
					"Request URI [" + requestUri + "] does not start with context path [" + this.contextPath + "]");
		}
		request.setContextPath(this.contextPath);
		request.setServletPath(this.servletPath);

		if ("".equals(this.pathInfo)) {
			if (!requestUri.startsWith(this.contextPath + this.servletPath)) {
				throw new IllegalArgumentException(
						"Invalid servlet path [" + this.servletPath + "] for request URI [" + requestUri + "]");
			}
			String extraPath = requestUri.substring(this.contextPath.length() + this.servletPath.length());
			this.pathInfo = (StringUtils.hasText(extraPath) ?
					UrlPathHelper.defaultInstance.decodeRequestString(request, extraPath) : null);
		}
		request.setPathInfo(this.pathInfo);
	}

	private void addRequestParams(MockHttpServletRequest request, MultiValueMap<String, String> map) {
		map.forEach((key, values) -> values.forEach(value -> {
			value = (value != null ? UriUtils.decode(value, StandardCharsets.UTF_8) : null);
			request.addParameter(UriUtils.decode(key, StandardCharsets.UTF_8), value);
		}));
	}

	private MultiValueMap<String, String> parseFormData(MediaType mediaType) {
		HttpInputMessage message = new HttpInputMessage() {
			@Override
			public InputStream getBody() {
				return (content != null ? new ByteArrayInputStream(content) : StreamUtils.emptyInput());
			}
			@Override
			public HttpHeaders getHeaders() {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(mediaType);
				return headers;
			}
		};

		try {
			return new FormHttpMessageConverter().read(null, message);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to parse form data in request body", ex);
		}
	}

	private FlashMapManager getFlashMapManager(MockHttpServletRequest request) {
		FlashMapManager flashMapManager = null;
		try {
			ServletContext servletContext = request.getServletContext();
			WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
			flashMapManager = wac.getBean(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
		}
		catch (IllegalStateException | NoSuchBeanDefinitionException ex) {
			// ignore
		}
		return (flashMapManager != null ? flashMapManager : new SessionFlashMapManager());
	}

	@Override
	public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
		for (RequestPostProcessor postProcessor : this.postProcessors) {
			request = postProcessor.postProcessRequest(request);
		}
		return request;
	}


	private static void addToMap(Map<String, Object> map, String name, Object value) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(value, "'value' must not be null");
		map.put(name, value);
	}

	private static <T> void addToMultiValueMap(MultiValueMap<String, T> map, String name, T[] values) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notEmpty(values, "'values' must not be empty");
		for (T value : values) {
			map.add(name, value);
		}
	}

}
