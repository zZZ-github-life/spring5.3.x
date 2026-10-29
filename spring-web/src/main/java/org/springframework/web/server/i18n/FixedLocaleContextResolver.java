/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.server.i18n;

import java.util.Locale;
import java.util.TimeZone;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link LocaleContextResolver} 实现，始终返回固定的区域设置，并可选地返回时区。
 * 默认为当前 JVM 的默认区域设置。
 *
 * <p>注意：不支持 {@link #setLocaleContext}，因为固定的区域设置和时区无法更改。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class FixedLocaleContextResolver implements LocaleContextResolver {

	private final Locale locale;

	@Nullable
	private final TimeZone timeZone;


	/**
	 * 创建一个默认的 FixedLocaleResolver，暴露已配置的默认区域设置
	 * （或回退到 JVM 的默认区域设置）。
	 */
	public FixedLocaleContextResolver() {
		this(Locale.getDefault());
	}

	/**
	 * 创建一个暴露给定区域设置的 FixedLocaleResolver。
	 * @param locale the locale to expose
	 */
	public FixedLocaleContextResolver(Locale locale) {
		this(locale, null);
	}

	/**
	 * 创建一个暴露给定区域设置和时区的 FixedLocaleResolver。
	 * @param locale the locale to expose
	 * @param timeZone the time zone to expose
	 */
	public FixedLocaleContextResolver(Locale locale, @Nullable TimeZone timeZone) {
		Assert.notNull(locale, "Locale must not be null");
		this.locale = locale;
		this.timeZone = timeZone;
	}


	@Override
	public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			public Locale getLocale() {
				return locale;
			}
			@Override
			@Nullable
			public TimeZone getTimeZone() {
				return timeZone;
			}
		};
	}

	@Override
	public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext localeContext) {
		throw new UnsupportedOperationException(
				"Cannot change fixed locale - use a different locale context resolution strategy");
	}

}
