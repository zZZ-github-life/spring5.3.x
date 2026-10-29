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

package org.springframework.web.servlet.support;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 用于便捷访问由 {@link org.springframework.web.servlet.DispatcherServlet} 设置的请求特定状态的工具类。
 *
 * <p>支持查找当前的 WebApplicationContext、LocaleResolver、Locale、ThemeResolver、Theme 和 MultipartResolver。
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 03.03.2003
 * @see RequestContext
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public abstract class RequestContextUtils {

	/**
	 * 用于确定是否已配置 {@link RequestDataValueProcessor} 实现的 bean 名称。
	 * @since 4.2.1
	 */
	public static final String REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME = "requestDataValueProcessor";


	/**
	 * 查找与发起请求处理的 DispatcherServlet 关联的 WebApplicationContext；如果未找到与当前请求关联的上下文，则查找全局上下文。全局上下文将通过 ServletContext 或 ContextLoader 的当前上下文找到。
	 * <p>注意：此变体保持与 Servlet 2.5 兼容，它会显式检查给定的 ServletContext，而不是从请求中推导。
	 * @param request current HTTP request
	 * @param servletContext current servlet context
	 * @return the request-specific WebApplicationContext, or the global one
	 * if no request-specific context has been found, or {@code null} if none
	 * @since 4.2.1
	 * @see DispatcherServlet#WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 * @see WebApplicationContextUtils#getWebApplicationContext(ServletContext)
	 * @see ContextLoader#getCurrentWebApplicationContext()
	 */
	@Nullable
	public static WebApplicationContext findWebApplicationContext(
			HttpServletRequest request, @Nullable ServletContext servletContext) {

		WebApplicationContext webApplicationContext = (WebApplicationContext) request.getAttribute(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (webApplicationContext == null) {
			if (servletContext != null) {
				webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			}
			if (webApplicationContext == null) {
				webApplicationContext = ContextLoader.getCurrentWebApplicationContext();
			}
		}
		return webApplicationContext;
	}

	/**
	 * 查找与发起请求处理的 DispatcherServlet 关联的 WebApplicationContext；如果未找到与当前请求关联的上下文，则查找全局上下文。全局上下文将通过 ServletContext 或 ContextLoader 的当前上下文找到。
	 * <p>注意：此变体要求 Servlet 3.0+，通常推荐用于具有前瞻性的自定义用户代码。
	 * @param request current HTTP request
	 * @return the request-specific WebApplicationContext, or the global one
	 * if no request-specific context has been found, or {@code null} if none
	 * @since 4.2.1
	 * @see #findWebApplicationContext(HttpServletRequest, ServletContext)
	 * @see ServletRequest#getServletContext()
	 * @see ContextLoader#getCurrentWebApplicationContext()
	 */
	@Nullable
	public static WebApplicationContext findWebApplicationContext(HttpServletRequest request) {
		return findWebApplicationContext(request, request.getServletContext());
	}

	/**
	 * 返回由 {@link DispatcherServlet} 绑定到请求的 {@link LocaleResolver}。
	 * @param request current HTTP request
	 * @return the current {@code LocaleResolver}, or {@code null} if not found
	 */
	@Nullable
	public static LocaleResolver getLocaleResolver(HttpServletRequest request) {
		return (LocaleResolver) request.getAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE);
	}

	/**
	 * 从给定请求中获取当前区域设置，使用由 {@link DispatcherServlet} 绑定到请求的 {@link LocaleResolver}（如果可用）；否则回退到基于 {@code Accept-Language} 头的请求区域设置或服务器的默认区域设置。
	 * <p>此方法可作为标准 Servlet {@link javax.servlet.http.HttpServletRequest#getLocale()} 方法的直接替代品，如果未找到更具体的区域设置则回退到后者。
	 * <p>考虑使用 {@link org.springframework.context.i18n.LocaleContextHolder#getLocale()}，它通常会用相同的 Locale 填充。
	 * @param request current HTTP request
	 * @return the current locale for the given request, either from the
	 * LocaleResolver or from the plain request itself
	 * @see #getLocaleResolver
	 * @see org.springframework.context.i18n.LocaleContextHolder#getLocale()
	 */
	public static Locale getLocale(HttpServletRequest request) {
		LocaleResolver localeResolver = getLocaleResolver(request);
		return (localeResolver != null ? localeResolver.resolveLocale(request) : request.getLocale());
	}

	/**
	 * 从给定请求中获取当前时区，使用由 {@link DispatcherServlet} 绑定到请求的 {@link LocaleResolver} 中的 {@link TimeZoneAwareLocaleContext}（如果可用）。
	 * <p>注意：如果无法为给定请求解析出特定时区，此方法返回 {@code null}。这与 {@link #getLocale} 不同，后者始终可以回退到基于 {@code Accept-Language} 头的请求区域设置或服务器的默认区域设置。
	 * <p>考虑使用 {@link org.springframework.context.i18n.LocaleContextHolder#getTimeZone()}，它通常会用相同的 {@code TimeZone} 填充：该方法仅在 {@code LocaleResolver} 未提供特定时区时回退到系统时区这一点上有所不同（而不是像此方法那样返回 {@code null}）。
	 * @param request current HTTP request
	 * @return the current time zone for the given request, either from the
	 * {@code TimeZoneAwareLocaleContext} or {@code null} if none associated
	 * @see #getLocaleResolver
	 * @see org.springframework.context.i18n.LocaleContextHolder#getTimeZone()
	 */
	@Nullable
	public static TimeZone getTimeZone(HttpServletRequest request) {
		LocaleResolver localeResolver = getLocaleResolver(request);
		if (localeResolver instanceof LocaleContextResolver) {
			LocaleContext localeContext = ((LocaleContextResolver) localeResolver).resolveLocaleContext(request);
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				return ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		}
		return null;
	}

	/**
	 * 返回由 DispatcherServlet 绑定到请求的 ThemeResolver。
	 * @param request current HTTP request
	 * @return the current ThemeResolver, or {@code null} if not found
	 */
	@Nullable
	public static ThemeResolver getThemeResolver(HttpServletRequest request) {
		return (ThemeResolver) request.getAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE);
	}

	/**
	 * 返回由 DispatcherServlet 绑定到请求的 ThemeSource。
	 * @param request current HTTP request
	 * @return the current ThemeSource
	 */
	@Nullable
	public static ThemeSource getThemeSource(HttpServletRequest request) {
		return (ThemeSource) request.getAttribute(DispatcherServlet.THEME_SOURCE_ATTRIBUTE);
	}

	/**
	 * 从给定请求中获取当前主题，使用由 DispatcherServlet 绑定到请求的 ThemeResolver 和 ThemeSource。
	 * @param request current HTTP request
	 * @return the current theme, or {@code null} if not found
	 * @see #getThemeResolver
	 */
	@Nullable
	public static Theme getTheme(HttpServletRequest request) {
		ThemeResolver themeResolver = getThemeResolver(request);
		ThemeSource themeSource = getThemeSource(request);
		if (themeResolver != null && themeSource != null) {
			String themeName = themeResolver.resolveThemeName(request);
			return themeSource.getTheme(themeName);
		}
		else {
			return null;
		}
	}

	/**
	 * 返回重定向之前请求中的只读 "input" flash 属性。
	 * @param request current request
	 * @return a read-only Map, or {@code null} if not found
	 * @see FlashMap
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static Map<String, ?> getInputFlashMap(HttpServletRequest request) {
		return (Map<String, ?>) request.getAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE);
	}

	/**
	 * 返回 "output" FlashMap，用于保存重定向后请求的属性。
	 * @param request current request
	 * @return a {@link FlashMap} instance, never {@code null} within a
	 * {@code DispatcherServlet}-handled request
	 */
	public static FlashMap getOutputFlashMap(HttpServletRequest request) {
		return (FlashMap) request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
	}

	/**
	 * 返回用于保存 flash 属性的 {@code FlashMapManager} 实例。
	 * <p>从 5.0 开始，可以使用便捷方法 {@link #saveOutputFlashMap} 来保存 "output" FlashMap。
	 * @param request the current request
	 * @return a {@link FlashMapManager} instance, never {@code null} within a
	 * {@code DispatcherServlet}-handled request
	 */
	@Nullable
	public static FlashMapManager getFlashMapManager(HttpServletRequest request) {
		return (FlashMapManager) request.getAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE);
	}

	/**
	 * 便捷方法：获取 {@link #getOutputFlashMap "output" FlashMap}，用目标 URL 的路径和查询参数更新它，然后使用 {@link #getFlashMapManager FlashMapManager} 保存它。
	 * @param location the target URL for the redirect
	 * @param request the current request
	 * @param response the current response
	 * @since 5.0
	 */
	public static void saveOutputFlashMap(String location, HttpServletRequest request, HttpServletResponse response) {
		FlashMap flashMap = getOutputFlashMap(request);
		if (CollectionUtils.isEmpty(flashMap)) {
			return;
		}

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(location).build();
		flashMap.setTargetRequestPath(uriComponents.getPath());
		flashMap.addTargetRequestParams(uriComponents.getQueryParams());

		FlashMapManager manager = getFlashMapManager(request);
		Assert.state(manager != null, "No FlashMapManager. Is this a DispatcherServlet handled request?");
		manager.saveOutputFlashMap(flashMap, request, response);
	}

}
