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

package org.springframework.web.servlet.i18n;

import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.util.WebUtils;

/**
 * 在存在自定义设置时使用用户会话中的区域设置属性的 {@link org.springframework.web.servlet.LocaleResolver} 实现，并回退到配置的默认区域设置、请求的 {@code Accept-Language} 头或服务器默认区域设置。<p>如果应用本来就需要用户会话（即不必仅为存储用户区域设置而创建 {@code HttpSession}），则此实现最为合适。会话还可以选择包含关联的时区属性；或者，你也可以指定默认时区。<p>自定义控制器可以通过在解析器上调用 {@code #setLocale(Context)} 来覆盖用户的区域设置和时区，例如响应区域设置更改请求。更方便的替代方案是使用 {@link org.springframework.web.servlet.support.RequestContext#changeLocale}。<p>与 {@link CookieLocaleResolver} 不同，此策略将本地选择的区域设置存储在 Servlet 容器的 {@code HttpSession} 中。因此，这些设置仅对每个会话临时有效，会在每个会话终止时丢失。<p>请注意，它与 "Spring Session" 项目等外部会话管理机制没有直接关系。此 {@code LocaleResolver} 只会针对当前 {@code HttpServletRequest} 评估和修改相应的 {@code HttpSession} 属性。
 * @author Juergen Hoeller
 * @since 27.02.2003
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 */
public class SessionLocaleResolver extends AbstractLocaleContextResolver {

	/**
	 * 保存 Locale 的会话属性的默认名称。<p>仅由此实现内部使用。<p>在控制器或视图中，请使用 {@code RequestContext(Utils).getLocale()} 获取当前区域设置。
	 * @see org.springframework.web.servlet.support.RequestContext#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 */
	public static final String LOCALE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * 保存 TimeZone 的会话属性的默认名称。<p>仅由此实现内部使用。<p>在控制器或视图中，请使用 {@code RequestContext(Utils).getTimeZone()} 获取当前时区。
	 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	public static final String TIME_ZONE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".TIME_ZONE";


	private String localeAttributeName = LOCALE_SESSION_ATTRIBUTE_NAME;

	private String timeZoneAttributeName = TIME_ZONE_SESSION_ATTRIBUTE_NAME;


	/**
	 * 指定 {@code HttpSession} 中对应属性的名称，该属性保存当前 {@link Locale} 值。<p>默认是内部的 {@link #LOCALE_SESSION_ATTRIBUTE_NAME}。
	 * @since 4.3.8
	 */
	public void setLocaleAttributeName(String localeAttributeName) {
		this.localeAttributeName = localeAttributeName;
	}

	/**
	 * 指定 {@code HttpSession} 中对应属性的名称，该属性保存当前 {@link TimeZone} 值。<p>默认是内部的 {@link #TIME_ZONE_SESSION_ATTRIBUTE_NAME}。
	 * @since 4.3.8
	 */
	public void setTimeZoneAttributeName(String timeZoneAttributeName) {
		this.timeZoneAttributeName = timeZoneAttributeName;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		Locale locale = (Locale) WebUtils.getSessionAttribute(request, this.localeAttributeName);
		if (locale == null) {
			locale = determineDefaultLocale(request);
		}
		return locale;
	}

	@Override
	public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			public Locale getLocale() {
				Locale locale = (Locale) WebUtils.getSessionAttribute(request, localeAttributeName);
				if (locale == null) {
					locale = determineDefaultLocale(request);
				}
				return locale;
			}
			@Override
			@Nullable
			public TimeZone getTimeZone() {
				TimeZone timeZone = (TimeZone) WebUtils.getSessionAttribute(request, timeZoneAttributeName);
				if (timeZone == null) {
					timeZone = determineDefaultTimeZone(request);
				}
				return timeZone;
			}
		};
	}

	@Override
	public void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable LocaleContext localeContext) {

		Locale locale = null;
		TimeZone timeZone = null;
		if (localeContext != null) {
			locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		}
		WebUtils.setSessionAttribute(request, this.localeAttributeName, locale);
		WebUtils.setSessionAttribute(request, this.timeZoneAttributeName, timeZone);
	}


	/**
	 * 确定给定请求的默认区域设置，在未找到 {@link Locale} 会话属性时调用。<p>默认实现返回已配置的{@linkplain #setDefaultLocale(Locale) 默认区域设置}（如果有），否则回退到请求的 {@code Accept-Language} 头区域设置或服务器默认区域设置。
	 * @param request the request to resolve the locale for
	 * @return the default locale (never {@code null})
	 * @see #setDefaultLocale
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	protected Locale determineDefaultLocale(HttpServletRequest request) {
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale == null) {
			defaultLocale = request.getLocale();
		}
		return defaultLocale;
	}

	/**
	 * 确定给定请求的默认时区，在未找到 {@link TimeZone} 会话属性时调用。<p>默认实现返回已配置的默认时区（如果有），否则返回 {@code null}。
	 * @param request the request to resolve the time zone for
	 * @return the default time zone (or {@code null} if none defined)
	 * @see #setDefaultTimeZone
	 */
	@Nullable
	protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
		return getDefaultTimeZone();
	}

}
