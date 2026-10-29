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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

/**
 * 在存在自定义设置时使用发送回用户的 cookie 的 {@link LocaleResolver} 实现，并回退到配置的默认区域设置、请求的 {@code Accept-Language} 头或服务器默认区域设置。<p>这对于没有用户会话的无状态应用特别有用。cookie 还可以选择包含关联的时区值；或者，你也可以指定默认时区。<p>自定义控制器可以通过在解析器上调用 {@code #setLocale(Context)} 来覆盖用户的区域设置和时区，例如响应区域设置更改请求。更方便的替代方案是使用 {@link org.springframework.web.servlet.support.RequestContext#changeLocale}。
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @since 27.02.2003
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 */
public class CookieLocaleResolver extends CookieGenerator implements LocaleContextResolver {

	/**
	 * 保存 {@code Locale} 的请求属性名称。<p>仅当在当前请求过程中更改了区域设置时，才用于覆盖 cookie 值！<p>在控制器或视图中，请使用 {@code RequestContext(Utils).getLocale()} 获取当前区域设置。
	 * @see org.springframework.web.servlet.support.RequestContext#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 */
	public static final String LOCALE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * 保存 {@code TimeZone} 的请求属性名称。<p>仅当在当前请求过程中更改了区域设置时，才用于覆盖 cookie 值！<p>在控制器或视图中，请使用 {@code RequestContext(Utils).getTimeZone()} 获取当前时区。
	 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	public static final String TIME_ZONE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".TIME_ZONE";

	/**
	 * 如果未显式设置，则使用的默认 cookie 名称。
	 */
	public static final String DEFAULT_COOKIE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";


	private boolean languageTagCompliant = true;

	private boolean rejectInvalidCookies = true;

	@Nullable
	private Locale defaultLocale;

	@Nullable
	private TimeZone defaultTimeZone;


	/**
	 * 使用{@linkplain #DEFAULT_COOKIE_NAME 默认 cookie 名称}创建 {@link CookieLocaleResolver} 的新实例。
	 */
	public CookieLocaleResolver() {
		setCookieName(DEFAULT_COOKIE_NAME);
	}


	/**
	 * 指定此解析器的 cookie 是否应遵循 BCP 47 语言标签，而不是 Java 的传统区域设置规范格式。<p>自 5.1 起默认为 {@code true}。若要以 Java 的传统区域设置规范格式渲染，请将其切换为 {@code false}。解析时，此解析器宽松地接受传统 {@link Locale#toString} 格式以及任意大小写的 BCP 47 语言标签。
	 * @since 4.3
	 * @see #parseLocaleValue(String)
	 * @see #toLocaleValue(Locale)
	 * @see Locale#forLanguageTag(String)
	 * @see Locale#toLanguageTag()
	 */
	public void setLanguageTagCompliant(boolean languageTagCompliant) {
		this.languageTagCompliant = languageTagCompliant;
	}

	/**
	 * 返回此解析器的 cookie 是否应遵循 BCP 47 语言标签，而不是 Java 的传统区域设置规范格式。
	 * @since 4.3
	 */
	public boolean isLanguageTagCompliant() {
		return this.languageTagCompliant;
	}

	/**
	 * 指定是否拒绝内容无效（例如格式无效）的 cookie。<p>默认值为 {@code true}。关闭后可对解析失败进行宽松处理，在这种情况下回退到默认区域设置和时区。
	 * @since 5.1.7
	 * @see #setDefaultLocale
	 * @see #setDefaultTimeZone
	 * @see #determineDefaultLocale
	 * @see #determineDefaultTimeZone
	 */
	public void setRejectInvalidCookies(boolean rejectInvalidCookies) {
		this.rejectInvalidCookies = rejectInvalidCookies;
	}

	/**
	 * 返回是否拒绝内容无效（例如格式无效）的 cookie。
	 * @since 5.1.7
	 */
	public boolean isRejectInvalidCookies() {
		return this.rejectInvalidCookies;
	}

	/**
	 * 设置一个固定区域设置，如果未找到 cookie，此解析器将返回它。
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 返回如果未找到 cookie 时此解析器将返回的固定区域设置（如果有）。
	 */
	@Nullable
	protected Locale getDefaultLocale() {
		return this.defaultLocale;
	}

	/**
	 * 设置一个固定时区，如果未找到 cookie，此解析器将返回它。
	 * @since 4.0
	 */
	public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * 返回如果未找到 cookie 时此解析器将返回的固定时区（如果有）。
	 * @since 4.0
	 */
	@Nullable
	protected TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		parseLocaleCookieIfNecessary(request);
		return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
	}

	@Override
	public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
		parseLocaleCookieIfNecessary(request);
		return new TimeZoneAwareLocaleContext() {
			@Override
			@Nullable
			public Locale getLocale() {
				return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
			}
			@Override
			@Nullable
			public TimeZone getTimeZone() {
				return (TimeZone) request.getAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
			}
		};
	}

	private void parseLocaleCookieIfNecessary(HttpServletRequest request) {
		if (request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME) == null) {
			Locale locale = null;
			TimeZone timeZone = null;

			// Retrieve and parse cookie value.
			String cookieName = getCookieName();
			if (cookieName != null) {
				Cookie cookie = WebUtils.getCookie(request, cookieName);
				if (cookie != null) {
					String value = cookie.getValue();
					String localePart = value;
					String timeZonePart = null;
					int separatorIndex = localePart.indexOf('/');
					if (separatorIndex == -1) {
						// Leniently accept older cookies separated by a space...
						separatorIndex = localePart.indexOf(' ');
					}
					if (separatorIndex >= 0) {
						localePart = value.substring(0, separatorIndex);
						timeZonePart = value.substring(separatorIndex + 1);
					}
					try {
						locale = (!"-".equals(localePart) ? parseLocaleValue(localePart) : null);
						if (timeZonePart != null) {
							timeZone = StringUtils.parseTimeZoneString(timeZonePart);
						}
					}
					catch (IllegalArgumentException ex) {
						if (isRejectInvalidCookies() &&
								request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
							throw new IllegalStateException("Encountered invalid locale cookie '" +
									cookieName + "': [" + value + "] due to: " + ex.getMessage());
						}
						else {
							// Lenient handling (e.g. error dispatch): ignore locale/timezone parse exceptions
							if (logger.isDebugEnabled()) {
								logger.debug("Ignoring invalid locale cookie '" + cookieName +
										"': [" + value + "] due to: " + ex.getMessage());
							}
						}
					}
					if (logger.isTraceEnabled()) {
						logger.trace("Parsed cookie value [" + cookie.getValue() + "] into locale '" + locale +
								"'" + (timeZone != null ? " and time zone '" + timeZone.getID() + "'" : ""));
					}
				}
			}

			request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
					(locale != null ? locale : determineDefaultLocale(request)));
			request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
					(timeZone != null ? timeZone : determineDefaultTimeZone(request)));
		}
	}

	@Override
	public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
		setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
	}

	@Override
	public void setLocaleContext(HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable LocaleContext localeContext) {

		Assert.notNull(response, "HttpServletResponse is required for CookieLocaleResolver");

		Locale locale = null;
		TimeZone timeZone = null;
		if (localeContext != null) {
			locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
			addCookie(response,
					(locale != null ? toLocaleValue(locale) : "-") + (timeZone != null ? '/' + timeZone.getID() : ""));
		}
		else {
			removeCookie(response);
		}
		request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
				(locale != null ? locale : determineDefaultLocale(request)));
		request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
				(timeZone != null ? timeZone : determineDefaultTimeZone(request)));
	}


	/**
	 * 解析来自传入 cookie 的给定区域设置值。<p>默认实现调用 {@link StringUtils#parseLocale(String)}，接受 {@link Locale#toString} 格式以及 BCP 47 语言标签。
	 * @param localeValue the locale value to parse
	 * @return the corresponding {@code Locale} instance
	 * @since 4.3
	 * @see StringUtils#parseLocale(String)
	 */
	@Nullable
	protected Locale parseLocaleValue(String localeValue) {
		return StringUtils.parseLocale(localeValue);
	}

	/**
	 * 将给定区域设置渲染为文本值以包含在 cookie 中。<p>默认实现根据 {@link #setLanguageTagCompliant "languageTagCompliant"} 配置属性调用 {@link Locale#toString()} 或 {@link Locale#toLanguageTag()}。
	 * @param locale the locale to convert to a string
	 * @return a String representation for the given locale
	 * @since 4.3
	 * @see #isLanguageTagCompliant()
	 */
	protected String toLocaleValue(Locale locale) {
		return (isLanguageTagCompliant() ? locale.toLanguageTag() : locale.toString());
	}

	/**
	 * 确定给定请求的默认区域设置，在未找到区域设置 cookie 时调用。<p>默认实现返回已配置的默认区域设置（如果有），否则回退到请求的 {@code Accept-Language} 头区域设置或服务器默认区域设置。
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
	 * 确定给定请求的默认时区，在未找到区域设置 cookie 时调用。<p>默认实现返回已配置的默认时区（如果有），否则返回 {@code null}。
	 * @param request the request to resolve the time zone for
	 * @return the default time zone (or {@code null} if none defined)
	 * @see #setDefaultTimeZone
	 */
	@Nullable
	protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
		return getDefaultTimeZone();
	}

}
