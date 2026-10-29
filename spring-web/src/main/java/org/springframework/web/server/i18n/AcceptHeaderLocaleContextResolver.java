/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link LocaleContextResolver} 实现，仅使用 HTTP 请求 "Accept-Language" 头部中
 * 指定的首选区域设置（即客户端浏览器发送的区域设置，通常是客户端操作系统的区域设置）。
 *
 * <p>注意：不支持 {@link #setLocaleContext}，因为 Accept 头部只能通过
 * 更改客户端的区域设置来改变。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.0
 * @see HttpHeaders#getAcceptLanguageAsLocales()
 */
public class AcceptHeaderLocaleContextResolver implements LocaleContextResolver {

	private final List<Locale> supportedLocales = new ArrayList<>(4);

	@Nullable
	private Locale defaultLocale;


	/**
	 * 配置受支持的区域设置，用于与通过
	 * {@link HttpHeaders#getAcceptLanguageAsLocales()} 确定的请求区域设置进行匹配。
	 * @param locales the supported locales
	 */
	public void setSupportedLocales(List<Locale> locales) {
		this.supportedLocales.clear();
		this.supportedLocales.addAll(locales);
	}

	/**
	 * 返回已配置的受支持区域设置列表。
	 */
	public List<Locale> getSupportedLocales() {
		return this.supportedLocales;
	}

	/**
	 * 配置一个固定的默认区域设置，当请求没有 "Accept-Language" 头部时作为后备
	 * （默认不设置）。
	 * @param defaultLocale the default locale to use
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 已配置的默认区域设置（如果有）。
	 * <p>此方法可以在子类中重写。
	 */
	@Nullable
	public Locale getDefaultLocale() {
		return this.defaultLocale;
	}


	@Override
	public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
		List<Locale> requestLocales = null;
		try {
			requestLocales = exchange.getRequest().getHeaders().getAcceptLanguageAsLocales();
		}
		catch (IllegalArgumentException ex) {
			// Invalid Accept-Language header: treat as empty for matching purposes
		}
		return new SimpleLocaleContext(resolveSupportedLocale(requestLocales));
	}

	@Nullable
	private Locale resolveSupportedLocale(@Nullable List<Locale> requestLocales) {
		if (CollectionUtils.isEmpty(requestLocales)) {
			return getDefaultLocale();  // may be null
		}
		List<Locale> supportedLocales = getSupportedLocales();
		if (supportedLocales.isEmpty()) {
			return requestLocales.get(0);  // never null
		}

		Locale languageMatch = null;
		for (Locale locale : requestLocales) {
			if (supportedLocales.contains(locale)) {
				if (languageMatch == null || languageMatch.getLanguage().equals(locale.getLanguage())) {
					// Full match: language + country, possibly narrowed from earlier language-only match
					return locale;
				}
			}
			else if (languageMatch == null) {
				// Let's try to find a language-only match as a fallback
				for (Locale candidate : supportedLocales) {
					if (!StringUtils.hasLength(candidate.getCountry()) &&
							candidate.getLanguage().equals(locale.getLanguage())) {
						languageMatch = candidate;
						break;
					}
				}
			}
		}
		if (languageMatch != null) {
			return languageMatch;
		}

		Locale defaultLocale = getDefaultLocale();
		return (defaultLocale != null ? defaultLocale : requestLocales.get(0));
	}

	@Override
	public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext locale) {
		throw new UnsupportedOperationException(
				"Cannot change HTTP accept header - use a different locale context resolution strategy");
	}

}
