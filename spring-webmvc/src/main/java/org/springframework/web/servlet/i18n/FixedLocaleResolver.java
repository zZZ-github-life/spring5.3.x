/*
 * Copyright 2002-2017 the original author or authors.
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

/**
 * 始终返回固定默认区域设置（可选固定时区）的 {@link org.springframework.web.servlet.LocaleResolver} 实现。默认值为当前 JVM 的默认区域设置。<p>注意：不支持 {@code setLocale(Context)}，因为固定的区域设置和时区无法更改。
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 */
public class FixedLocaleResolver extends AbstractLocaleContextResolver {

	/**
	 * 创建一个默认的 FixedLocaleResolver，公开已配置的默认区域设置（否则回退到 JVM 的默认区域设置）。
	 * @see #setDefaultLocale
	 * @see #setDefaultTimeZone
	 */
	public FixedLocaleResolver() {
		setDefaultLocale(Locale.getDefault());
	}

	/**
	 * 创建一个公开给定区域设置的 FixedLocaleResolver。
	 * @param locale 要暴露的 Locale
	 */
	public FixedLocaleResolver(Locale locale) {
		setDefaultLocale(locale);
	}

	/**
	 * 创建一个公开给定区域设置和时区的 FixedLocaleResolver。
	 * @param locale 要暴露的 Locale
	 * @param timeZone 要暴露的时区
	 */
	public FixedLocaleResolver(Locale locale, TimeZone timeZone) {
		setDefaultLocale(locale);
		setDefaultTimeZone(timeZone);
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		Locale locale = getDefaultLocale();
		if (locale == null) {
			locale = Locale.getDefault();
		}
		return locale;
	}

	@Override
	public LocaleContext resolveLocaleContext(HttpServletRequest request) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			@Nullable
			public Locale getLocale() {
				return getDefaultLocale();
			}
			@Override
			public TimeZone getTimeZone() {
				return getDefaultTimeZone();
			}
		};
	}

	@Override
	public void setLocaleContext( HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable LocaleContext localeContext) {

		throw new UnsupportedOperationException("Cannot change fixed locale - use a different locale resolution strategy");
	}

}
