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

import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleContextResolver;

/**
 * 用于 {@link LocaleContextResolver} 实现的抽象基类。<p>提供对{@linkplain #setDefaultLocale(Locale) 默认区域设置}和{@linkplain #setDefaultTimeZone(TimeZone) 默认时区}的支持。<p>还提供 {@link #resolveLocale} 和 {@link #setLocale} 的预实现版本，分别委托给 {@link #resolveLocaleContext} 和 {@link #setLocaleContext}。
 * @author Juergen Hoeller
 * @since 4.0
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 */
public abstract class AbstractLocaleContextResolver extends AbstractLocaleResolver implements LocaleContextResolver {

	@Nullable
	private TimeZone defaultTimeZone;


	/**
	 * 设置一个默认 {@link TimeZone}，如果未找到其他时区，此解析器将返回它。
	 */
	public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * 获取此解析器应回退到的默认 {@link TimeZone}（如果有）。
	 */
	@Nullable
	public TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		Locale locale = resolveLocaleContext(request).getLocale();
		return (locale != null ? locale : request.getLocale());
	}

	@Override
	public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
		setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
	}

}
