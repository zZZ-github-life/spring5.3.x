/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.support;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.ResourceBundleThemeSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * 请求特定状态的上下文持有者，例如当前的 Web 应用上下文、当前区域设置、当前主题以及可能的绑定错误。提供对本地化消息和 Errors 实例的便捷访问。
 *
 * <p>适用于向视图暴露，以及在 JSP 的 "useBean" 标签、JSP 脚本片段、JSTL EL 等中使用。对于无法访问 servlet 请求的视图（如 FreeMarker 模板）是必需的。
 *
 * <p>可以手动实例化，也可以通过 AbstractView 的 "requestContextAttribute" 属性自动作为模型属性暴露给视图。
 *
 * <p>在 DispatcherServlet 请求之外也能工作，此时会访问根 WebApplicationContext，并为区域设置使用适当的回退（HttpServletRequest 的首选区域设置）。
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 03.03.2003
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.view.AbstractView#setRequestContextAttribute
 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setRequestContextAttribute
 */
public class RequestContext {

	/**
	 * 当 RequestContext 无法找到 ThemeResolver 时使用的默认主题名称。仅适用于非 DispatcherServlet 请求。
	 * <p>与 AbstractThemeResolver 的默认值相同，但此处未建立链接，以避免包之间的相互依赖。
	 * @see org.springframework.web.servlet.theme.AbstractThemeResolver#ORIGINAL_DEFAULT_THEME_NAME
	 */
	public static final String DEFAULT_THEME_NAME = "theme";

	/**
	 * 用于保存当前 Web 应用上下文以供 RequestContext 使用的请求属性。默认情况下，暴露的是 DispatcherServlet 的上下文（或作为回退的根上下文）。
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = RequestContext.class.getName() + ".CONTEXT";


	protected static final boolean jstlPresent = ClassUtils.isPresent(
			"javax.servlet.jsp.jstl.core.Config", RequestContext.class.getClassLoader());

	private HttpServletRequest request;

	@Nullable
	private HttpServletResponse response;

	@Nullable
	private Map<String, Object> model;

	private WebApplicationContext webApplicationContext;

	@Nullable
	private Locale locale;

	@Nullable
	private TimeZone timeZone;

	@Nullable
	private Theme theme;

	@Nullable
	private Boolean defaultHtmlEscape;

	@Nullable
	private Boolean responseEncodedHtmlEscape;

	private UrlPathHelper urlPathHelper;

	@Nullable
	private RequestDataValueProcessor requestDataValueProcessor;

	@Nullable
	private Map<String, Errors> errorsMap;


	/**
	 * 为给定请求创建一个新的 RequestContext，使用请求属性来获取 Errors。
	 * <p>这仅适用于 InternalResourceViews，因为 Errors 实例是模型的一部分，通常不会作为请求属性暴露。它通常会在 JSP 或自定义标签中使用。
	 * <p><b>仅在 DispatcherServlet 请求中有效。</b>传入 ServletContext 以便能够回退到根 WebApplicationContext。
	 * @param request current HTTP request
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(javax.servlet.http.HttpServletRequest, javax.servlet.ServletContext)
	 */
	public RequestContext(HttpServletRequest request) {
		this(request, null, null, null);
	}

	/**
	 * 为给定请求创建一个新的 RequestContext，使用请求属性来获取 Errors。
	 * <p>这仅适用于 InternalResourceViews，因为 Errors 实例是模型的一部分，通常不会作为请求属性暴露。它通常会在 JSP 或自定义标签中使用。
	 * <p><b>仅在 DispatcherServlet 请求中有效。</b>传入 ServletContext 以便能够回退到根 WebApplicationContext。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.ServletContext, Map)
	 */
	public RequestContext(HttpServletRequest request, HttpServletResponse response) {
		this(request, response, null, null);
	}

	/**
	 * 为给定请求创建一个新的 RequestContext，使用请求属性来获取 Errors。
	 * <p>这仅适用于 InternalResourceViews，因为 Errors 实例是模型的一部分，通常不会作为请求属性暴露。它通常会在 JSP 或自定义标签中使用。
	 * <p>如果指定了 ServletContext，RequestContext 还可以在根 WebApplicationContext 中工作（在 DispatcherServlet 之外）。
	 * @param request current HTTP request
	 * @param servletContext the servlet context of the web application (can be {@code null};
	 * necessary for fallback to root WebApplicationContext)
	 * @see org.springframework.web.context.WebApplicationContext
	 * @see org.springframework.web.servlet.DispatcherServlet
	 */
	public RequestContext(HttpServletRequest request, @Nullable ServletContext servletContext) {
		this(request, null, servletContext, null);
	}

	/**
	 * 为给定请求创建一个新的 RequestContext，使用给定的模型属性来获取 Errors。
	 * <p>这适用于所有 View 实现。通常由 View 实现使用。
	 * <p><b>仅在 DispatcherServlet 请求中有效。</b>传入 ServletContext 以便能够回退到根 WebApplicationContext。
	 * @param request current HTTP request
	 * @param model the model attributes for the current view (can be {@code null},
	 * using the request attributes for Errors retrieval)
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.ServletContext, Map)
	 */
	public RequestContext(HttpServletRequest request, @Nullable Map<String, Object> model) {
		this(request, null, null, model);
	}

	/**
	 * 为给定请求创建一个新的 RequestContext，使用给定的模型属性来获取 Errors。
	 * <p>这适用于所有 View 实现。通常由 View 实现使用。
	 * <p>如果指定了 ServletContext，RequestContext 还可以在根 WebApplicationContext 中工作（在 DispatcherServlet 之外）。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param servletContext the servlet context of the web application (can be {@code null}; necessary for
	 * fallback to root WebApplicationContext)
	 * @param model the model attributes for the current view (can be {@code null}, using the request attributes
	 * for Errors retrieval)
	 * @see org.springframework.web.context.WebApplicationContext
	 * @see org.springframework.web.servlet.DispatcherServlet
	 */
	public RequestContext(HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable ServletContext servletContext, @Nullable Map<String, Object> model) {

		this.request = request;
		this.response = response;
		this.model = model;

		// Fetch WebApplicationContext, either from DispatcherServlet or the root context.
		// ServletContext needs to be specified to be able to fall back to the root context!
		WebApplicationContext wac = (WebApplicationContext) request.getAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (wac == null) {
			wac = RequestContextUtils.findWebApplicationContext(request, servletContext);
			if (wac == null) {
				throw new IllegalStateException("No WebApplicationContext found: not in a DispatcherServlet " +
						"request and no ContextLoaderListener registered?");
			}
		}
		this.webApplicationContext = wac;

		Locale locale = null;
		TimeZone timeZone = null;

		// Determine locale to use for this RequestContext.
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
		if (localeResolver instanceof LocaleContextResolver) {
			LocaleContext localeContext = ((LocaleContextResolver) localeResolver).resolveLocaleContext(request);
			locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		}
		else if (localeResolver != null) {
			// Try LocaleResolver (we're within a DispatcherServlet request).
			locale = localeResolver.resolveLocale(request);
		}

		this.locale = locale;
		this.timeZone = timeZone;

		// Determine default HTML escape setting from the "defaultHtmlEscape"
		// context-param in web.xml, if any.
		this.defaultHtmlEscape = WebUtils.getDefaultHtmlEscape(this.webApplicationContext.getServletContext());

		// Determine response-encoded HTML escape setting from the "responseEncodedHtmlEscape"
		// context-param in web.xml, if any.
		this.responseEncodedHtmlEscape =
				WebUtils.getResponseEncodedHtmlEscape(this.webApplicationContext.getServletContext());

		this.urlPathHelper = new UrlPathHelper();

		if (this.webApplicationContext.containsBean(RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			this.requestDataValueProcessor = this.webApplicationContext.getBean(
					RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
		}
	}


	/**
	 * 返回底层的 HttpServletRequest。仅供此包中协作的类使用。
	 */
	protected final HttpServletRequest getRequest() {
		return this.request;
	}

	/**
	 * 返回底层的 ServletContext。仅供此包中协作的类使用。
	 */
	@Nullable
	protected final ServletContext getServletContext() {
		return this.webApplicationContext.getServletContext();
	}

	/**
	 * 返回当前的 WebApplicationContext。
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}

	/**
	 * 将当前的 WebApplicationContext 作为 MessageSource 返回。
	 */
	public final MessageSource getMessageSource() {
		return this.webApplicationContext;
	}

	/**
	 * 返回此 RequestContext 所封装的模型 Map（如果有）。
	 * @return the populated model Map, or {@code null} if none available
	 */
	@Nullable
	public final Map<String, Object> getModel() {
		return this.model;
	}

	/**
	 * 返回当前的 Locale（回退到请求的区域设置；绝不会是 {@code null}）。
	 * <p>通常来自 DispatcherServlet 的 {@link LocaleResolver}。还包括对 JSTL Locale 属性的回退检查。
	 * @see RequestContextUtils#getLocale
	 */
	public final Locale getLocale() {
		return (this.locale != null ? this.locale : getFallbackLocale());
	}

	/**
	 * 返回当前的 TimeZone（如果无法从请求中推导出，则为 {@code null}）。
	 * <p>通常来自 DispatcherServlet 的 {@link LocaleContextResolver}。还包括对 JSTL TimeZone 属性的回退检查。
	 * @see RequestContextUtils#getTimeZone
	 */
	@Nullable
	public TimeZone getTimeZone() {
		return (this.timeZone != null ? this.timeZone : getFallbackTimeZone());
	}

	/**
	 * 确定此上下文的回退区域设置。
	 * <p>默认实现会检查请求、会话或应用作用域中的 JSTL 区域设置属性；如果未找到，则返回 {@code HttpServletRequest.getLocale()}。
	 * @return the fallback locale (never {@code null})
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	protected Locale getFallbackLocale() {
		if (jstlPresent) {
			Locale locale = JstlLocaleResolver.getJstlLocale(getRequest(), getServletContext());
			if (locale != null) {
				return locale;
			}
		}
		return getRequest().getLocale();
	}

	/**
	 * 确定此上下文的回退时区。
	 * <p>默认实现会检查请求、会话或应用作用域中的 JSTL 时区属性；如果未找到则返回 {@code null}。
	 * @return the fallback time zone (or {@code null} if none derivable from the request)
	 */
	@Nullable
	protected TimeZone getFallbackTimeZone() {
		if (jstlPresent) {
			TimeZone timeZone = JstlLocaleResolver.getJstlTimeZone(getRequest(), getServletContext());
			if (timeZone != null) {
				return timeZone;
			}
		}
		return null;
	}

	/**
	 * 将当前区域设置更改为指定的区域设置，并通过所配置的 {@link LocaleResolver} 存储新的区域设置。
	 * @param locale the new locale
	 * @see LocaleResolver#setLocale
	 * @see #changeLocale(java.util.Locale, java.util.TimeZone)
	 */
	public void changeLocale(Locale locale) {
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(this.request);
		if (localeResolver == null) {
			throw new IllegalStateException("Cannot change locale if no LocaleResolver configured");
		}
		localeResolver.setLocale(this.request, this.response, locale);
		this.locale = locale;
	}

	/**
	 * 将当前区域设置更改为指定的区域设置和时区上下文，并通过所配置的 {@link LocaleResolver} 存储新的区域设置上下文。
	 * @param locale the new locale
	 * @param timeZone the new time zone
	 * @see LocaleContextResolver#setLocaleContext
	 * @see org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext
	 */
	public void changeLocale(Locale locale, TimeZone timeZone) {
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(this.request);
		if (!(localeResolver instanceof LocaleContextResolver)) {
			throw new IllegalStateException("Cannot change locale context if no LocaleContextResolver configured");
		}
		((LocaleContextResolver) localeResolver).setLocaleContext(this.request, this.response,
				new SimpleTimeZoneAwareLocaleContext(locale, timeZone));
		this.locale = locale;
		this.timeZone = timeZone;
	}

	/**
	 * 返回当前主题（绝不会是 {@code null}）。
	 * <p>采用延迟解析，以便在未使用主题支持时提高效率。
	 */
	public Theme getTheme() {
		if (this.theme == null) {
			// Lazily determine theme to use for this RequestContext.
			this.theme = RequestContextUtils.getTheme(this.request);
			if (this.theme == null) {
				// No ThemeResolver and ThemeSource available -> try fallback.
				this.theme = getFallbackTheme();
			}
		}
		return this.theme;
	}

	/**
	 * 确定此上下文的回退主题。
	 * <p>默认实现返回默认主题（名称为 "theme"）。
	 * @return the fallback theme (never {@code null})
	 */
	protected Theme getFallbackTheme() {
		ThemeSource themeSource = RequestContextUtils.getThemeSource(getRequest());
		if (themeSource == null) {
			themeSource = new ResourceBundleThemeSource();
		}
		Theme theme = themeSource.getTheme(DEFAULT_THEME_NAME);
		if (theme == null) {
			throw new IllegalStateException("No theme defined and no fallback theme found");
		}
		return theme;
	}

	/**
	 * 将当前主题更改为指定的主题，并通过所配置的 {@link ThemeResolver} 存储新的主题名称。
	 * @param theme the new theme
	 * @see ThemeResolver#setThemeName
	 */
	public void changeTheme(@Nullable Theme theme) {
		ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(this.request);
		if (themeResolver == null) {
			throw new IllegalStateException("Cannot change theme if no ThemeResolver configured");
		}
		themeResolver.setThemeName(this.request, this.response, (theme != null ? theme.getName() : null));
		this.theme = theme;
	}

	/**
	 * 按名称将当前主题更改为指定的主题，并通过所配置的 {@link ThemeResolver} 存储新的主题名称。
	 * @param themeName the name of the new theme
	 * @see ThemeResolver#setThemeName
	 */
	public void changeTheme(String themeName) {
		ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(this.request);
		if (themeResolver == null) {
			throw new IllegalStateException("Cannot change theme if no ThemeResolver configured");
		}
		themeResolver.setThemeName(this.request, this.response, themeName);
		// Ask for re-resolution on next getTheme call.
		this.theme = null;
	}

	/**
	 * 在此 RequestContext 的范围内启用或停用消息和错误的默认 HTML 转义。
	 * <p>默认值为应用范围的设置（web.xml 中的 "defaultHtmlEscape" context-param）。
	 * @see org.springframework.web.util.WebUtils#getDefaultHtmlEscape
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}

	/**
	 * 默认 HTML 转义是否处于活动状态？如果未给出显式默认值，则回退为 {@code false}。
	 */
	public boolean isDefaultHtmlEscape() {
		return (this.defaultHtmlEscape != null && this.defaultHtmlEscape.booleanValue());
	}

	/**
	 * 返回默认的 HTML 转义设置，并区分未指定默认值与显式值。
	 * @return whether default HTML escaping is enabled (null = no explicit default)
	 */
	@Nullable
	public Boolean getDefaultHtmlEscape() {
		return this.defaultHtmlEscape;
	}

	/**
	 * 默认情况下 HTML 转义是否使用响应编码？如果启用，则仅对 XML 标记中的重要字符使用 UTF-* 编码进行转义。
	 * <p>从 Spring 4.2 开始，如果未给出显式默认值，则回退为 {@code true}。
	 * @since 4.1.2
	 */
	public boolean isResponseEncodedHtmlEscape() {
		return (this.responseEncodedHtmlEscape == null || this.responseEncodedHtmlEscape.booleanValue());
	}

	/**
	 * 返回关于 HTML 转义是否使用响应编码的默认设置，并区分未指定默认值与显式值。
	 * @return whether default use of response encoding HTML escaping is enabled (null = no explicit default)
	 * @since 4.1.2
	 */
	@Nullable
	public Boolean getResponseEncodedHtmlEscape() {
		return this.responseEncodedHtmlEscape;
	}


	/**
	 * 设置用于上下文路径和请求 URI 解码的 UrlPathHelper。可用于传入共享的 UrlPathHelper 实例。
	 * <p>始终有一个默认的 UrlPathHelper 可用。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回用于上下文路径和请求 URI 解码的 UrlPathHelper。可用于配置当前的 UrlPathHelper。
	 * <p>始终有一个默认的 UrlPathHelper 可用。
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 返回从 WebApplicationContext 中以 {@code "requestDataValueProcessor"} 名称获取的要使用的 RequestDataValueProcessor 实例。如果未找到匹配的 bean，则为 {@code null}。
	 */
	@Nullable
	public RequestDataValueProcessor getRequestDataValueProcessor() {
		return this.requestDataValueProcessor;
	}

	/**
	 * 返回原始请求的上下文路径，即指示当前 Web 应用的路径。这对于构建指向应用中其他资源的链接很有用。
	 * <p>解码委托给 UrlPathHelper 完成。
	 * @see javax.servlet.http.HttpServletRequest#getContextPath
	 * @see #getUrlPathHelper
	 */
	public String getContextPath() {
		return this.urlPathHelper.getOriginatingContextPath(this.request);
	}

	/**
	 * 为给定的相对 URL 返回一个感知上下文的 URL。
	 * @param relativeUrl the relative URL part
	 * @return a URL that points back to the server with an absolute path (also URL-encoded accordingly)
	 */
	public String getContextUrl(String relativeUrl) {
		String url = getContextPath() + relativeUrl;
		if (this.response != null) {
			url = this.response.encodeURL(url);
		}
		return url;
	}

	/**
	 * 为带占位符（花括号 {@code {}} 中的命名键）的给定相对 URL 返回一个感知上下文的 URL。例如，传入相对 URL {@code foo/{bar}?spam={spam}} 和参数映射 {@code {bar=baz,spam=nuts}}，结果将是 {@code [contextpath]/foo/baz?spam=nuts}。
	 * @param relativeUrl the relative URL part
	 * @param params a map of parameters to insert as placeholders in the url
	 * @return a URL that points back to the server with an absolute path (also URL-encoded accordingly)
	 */
	public String getContextUrl(String relativeUrl, Map<String, ?> params) {
		String url = getContextPath() + relativeUrl;
		url = UriComponentsBuilder.fromUriString(url).buildAndExpand(params).encode().toUri().toASCIIString();
		if (this.response != null) {
			url = this.response.encodeURL(url);
		}
		return url;
	}

	/**
	 * 返回当前 servlet 内 URL 映射的路径，包括原始请求的上下文路径和 servlet 路径。当使用 {@code "/main/*"} 风格的 servlet 映射时，这对于构建指向应用中其他资源的链接很有用。
	 * <p>上下文路径和 servlet 路径的确定委托给 UrlPathHelper 完成。
	 */
	public String getPathToServlet() {
		String path = this.urlPathHelper.getOriginatingContextPath(this.request);
		if (StringUtils.hasText(this.urlPathHelper.getPathWithinServletMapping(this.request))) {
			path += this.urlPathHelper.getOriginatingServletPath(this.request);
		}
		return path;
	}

	/**
	 * 返回原始请求的请求 URI，即不带参数的调用 URL。这作为 HTML 表单的 action 目标特别有用，也可以与原始查询字符串结合使用。
	 * <p>解码委托给 UrlPathHelper 完成。
	 * @see #getQueryString
	 * @see org.springframework.web.util.UrlPathHelper#getOriginatingRequestUri
	 * @see #getUrlPathHelper
	 */
	public String getRequestUri() {
		return this.urlPathHelper.getOriginatingRequestUri(this.request);
	}

	/**
	 * 返回当前请求的查询字符串，即请求路径之后的部分。结合原始请求 URI 构建 HTML 表单的 action 目标时特别有用。
	 * <p>解码委托给 UrlPathHelper 完成。
	 * @see #getRequestUri
	 * @see org.springframework.web.util.UrlPathHelper#getOriginatingQueryString
	 * @see #getUrlPathHelper
	 */
	public String getQueryString() {
		return this.urlPathHelper.getOriginatingQueryString(this.request);
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置获取给定代码对应的消息。
	 * @param code the code of the message
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, String defaultMessage) {
		return getMessage(code, null, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置获取给定代码对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
		return getMessage(code, args, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置获取给定代码对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, @Nullable List<?> args, String defaultMessage) {
		return getMessage(code, (args != null ? args.toArray() : null), defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 获取给定代码对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @param htmlEscape if the message should be HTML-escaped
	 * @return the message
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage, boolean htmlEscape) {
		String msg = this.webApplicationContext.getMessage(code, args, defaultMessage, getLocale());
		if (msg == null) {
			return "";
		}
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置获取给定代码对应的消息。
	 * @param code the code of the message
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return getMessage(code, null, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置获取给定代码对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
		return getMessage(code, args, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置获取给定代码对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, @Nullable List<?> args) throws NoSuchMessageException {
		return getMessage(code, (args != null ? args.toArray() : null), isDefaultHtmlEscape());
	}

	/**
	 * 获取给定代码对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param htmlEscape if the message should be HTML-escaped
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, @Nullable Object[] args, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.webApplicationContext.getMessage(code, args, getLocale());
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置获取给定的 MessageSourceResolvable（例如 ObjectError 实例）。
	 * @param resolvable the MessageSourceResolvable
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getMessage(resolvable, isDefaultHtmlEscape());
	}

	/**
	 * 获取给定的 MessageSourceResolvable（例如 ObjectError 实例）。
	 * @param resolvable the MessageSourceResolvable
	 * @param htmlEscape if the message should be HTML-escaped
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.webApplicationContext.getMessage(resolvable, getLocale());
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 获取给定代码对应的主题消息。
	 * <p>请注意，主题消息从不进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 * @param code the code of the message
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getThemeMessage(String code, String defaultMessage) {
		String msg = getTheme().getMessageSource().getMessage(code, null, defaultMessage, getLocale());
		return (msg != null ? msg : "");
	}

	/**
	 * 获取给定代码对应的主题消息。
	 * <p>请注意，主题消息从不进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getThemeMessage(String code, @Nullable Object[] args, String defaultMessage) {
		String msg = getTheme().getMessageSource().getMessage(code, args, defaultMessage, getLocale());
		return (msg != null ? msg : "");
	}

	/**
	 * 获取给定代码对应的主题消息。
	 * <p>请注意，主题消息从不进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 * @param code the code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getThemeMessage(String code, @Nullable List<?> args, String defaultMessage) {
		String msg = getTheme().getMessageSource().getMessage(code, (args != null ? args.toArray() : null),
				defaultMessage, getLocale());
		return (msg != null ? msg : "");
	}

	/**
	 * 获取给定代码对应的主题消息。
	 * <p>请注意，主题消息从不进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 * @param code the code of the message
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getThemeMessage(String code) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, null, getLocale());
	}

	/**
	 * 获取给定代码对应的主题消息。
	 * <p>请注意，主题消息从不进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getThemeMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, args, getLocale());
	}

	/**
	 * 获取给定代码对应的主题消息。
	 * <p>请注意，主题消息从不进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 * @param code the code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getThemeMessage(String code, @Nullable List<?> args) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, (args != null ? args.toArray() : null), getLocale());
	}

	/**
	 * 在当前主题中获取给定的 MessageSourceResolvable。
	 * <p>请注意，主题消息从不进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 * @param resolvable the MessageSourceResolvable
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getThemeMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(resolvable, getLocale());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置获取给定绑定对象的 Errors 实例。
	 * @param name the name of the bind object
	 * @return the Errors instance, or {@code null} if not found
	 */
	@Nullable
	public Errors getErrors(String name) {
		return getErrors(name, isDefaultHtmlEscape());
	}

	/**
	 * 获取给定绑定对象的 Errors 实例。
	 * @param name the name of the bind object
	 * @param htmlEscape create an Errors instance with automatic HTML escaping?
	 * @return the Errors instance, or {@code null} if not found
	 */
	@Nullable
	public Errors getErrors(String name, boolean htmlEscape) {
		if (this.errorsMap == null) {
			this.errorsMap = new HashMap<>();
		}
		Errors errors = this.errorsMap.get(name);
		boolean put = false;
		if (errors == null) {
			errors = (Errors) getModelObject(BindingResult.MODEL_KEY_PREFIX + name);
			// Check old BindException prefix for backwards compatibility.
			if (errors instanceof BindException) {
				errors = ((BindException) errors).getBindingResult();
			}
			if (errors == null) {
				return null;
			}
			put = true;
		}
		if (htmlEscape && !(errors instanceof EscapedErrors)) {
			errors = new EscapedErrors(errors);
			put = true;
		}
		else if (!htmlEscape && errors instanceof EscapedErrors) {
			errors = ((EscapedErrors) errors).getSource();
			put = true;
		}
		if (put) {
			this.errorsMap.put(name, errors);
		}
		return errors;
	}

	/**
	 * 为给定的模型名称获取模型对象，可以从模型中获取，也可以从请求属性中获取。
	 * @param modelName the name of the model object
	 * @return the model object
	 */
	@Nullable
	protected Object getModelObject(String modelName) {
		if (this.model != null) {
			return this.model.get(modelName);
		}
		else {
			return this.request.getAttribute(modelName);
		}
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置为给定的绑定对象创建 BindStatus。
	 * @param path the bean and property path for which values and errors will be resolved (e.g. "person.age")
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path) throws IllegalStateException {
		return new BindStatus(this, path, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置为给定的绑定对象创建 BindStatus。
	 * @param path the bean and property path for which values and errors will be resolved (e.g. "person.age")
	 * @param htmlEscape create a BindStatus with automatic HTML escaping?
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(this, path, htmlEscape);
	}


	/**
	 * 用于隔离 JSTL 依赖的内部类。
	 * 仅在 JSTL API 存在时调用，以解析回退区域设置。
	 */
	private static class JstlLocaleResolver {

		@Nullable
		public static Locale getJstlLocale(HttpServletRequest request, @Nullable ServletContext servletContext) {
			Object localeObject = Config.get(request, Config.FMT_LOCALE);
			if (localeObject == null) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					localeObject = Config.get(session, Config.FMT_LOCALE);
				}
				if (localeObject == null && servletContext != null) {
					localeObject = Config.get(servletContext, Config.FMT_LOCALE);
				}
			}
			return (localeObject instanceof Locale ? (Locale) localeObject : null);
		}

		@Nullable
		public static TimeZone getJstlTimeZone(HttpServletRequest request, @Nullable ServletContext servletContext) {
			Object timeZoneObject = Config.get(request, Config.FMT_TIME_ZONE);
			if (timeZoneObject == null) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					timeZoneObject = Config.get(session, Config.FMT_TIME_ZONE);
				}
				if (timeZoneObject == null && servletContext != null) {
					timeZoneObject = Config.get(servletContext, Config.FMT_TIME_ZONE);
				}
			}
			return (timeZoneObject instanceof TimeZone ? (TimeZone) timeZoneObject : null);
		}
	}

}
