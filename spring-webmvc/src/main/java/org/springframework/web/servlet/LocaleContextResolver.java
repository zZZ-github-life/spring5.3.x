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

package org.springframework.web.servlet;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.lang.Nullable;

/**
 * {@link LocaleResolver} 的扩展，增加了对更丰富的区域设置上下文
 * （可能包含区域设置和时区信息）的支持。
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see org.springframework.context.i18n.LocaleContext
 * @see org.springframework.context.i18n.TimeZoneAwareLocaleContext
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
 */
public interface LocaleContextResolver extends LocaleResolver {

	/**
	 * 通过给定的请求解析当前的区域设置上下文。
	 * <p>这主要用于框架级别的处理；在应用级别访问当前区域设置和/或时区时，
	 * 请考虑使用 {@link org.springframework.web.servlet.support.RequestContextUtils}
	 * 或 {@link org.springframework.web.servlet.support.RequestContext}。
	 * <p>返回的上下文可能是
	 * {@link org.springframework.context.i18n.TimeZoneAwareLocaleContext}，
	 * 其中包含带有相应时区信息的区域设置。
	 * 只需执行 {@code instanceof} 检查并相应地向下转型即可。
	 * <p>自定义解析器实现还可以在返回的上下文中提供额外设置，
	 * 这些设置同样可以通过向下转型访问。
	 * @param request 要为其解析 Locale 上下文的请求
	 * @return 当前的 Locale 上下文（永不为 {@code null}）
	 * @see #resolveLocale(HttpServletRequest)
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	LocaleContext resolveLocaleContext(HttpServletRequest request);

	/**
	 * 将当前的区域设置上下文设置为给定的上下文，
	 * 可能包含带有相应时区信息的区域设置。
	 * @param request 用于修改 Locale 的请求
	 * @param response 用于修改 Locale 的响应
	 * @param localeContext 新的 Locale 上下文；为 {@code null} 时清除 Locale
	 * @throws UnsupportedOperationException 如果 LocaleResolver 实现不支持
	 * 动态更改 Locale 或时区
	 * @see #setLocale(HttpServletRequest, HttpServletResponse, Locale)
	 * @see org.springframework.context.i18n.SimpleLocaleContext
	 * @see org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext
	 */
	void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable LocaleContext localeContext);

}
