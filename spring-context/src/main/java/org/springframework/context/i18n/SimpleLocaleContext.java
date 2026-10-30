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

package org.springframework.context.i18n;

import java.util.Locale;

import org.springframework.lang.Nullable;

/**
 * {@link LocaleContext} 接口的简单实现，始终返回指定的 {@code Locale}。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocaleContextHolder#setLocaleContext
 * @see LocaleContextHolder#getLocale()
 * @see SimpleTimeZoneAwareLocaleContext
 */
public class SimpleLocaleContext implements LocaleContext {

	@Nullable
	private final Locale locale;


	/**
	 * 创建一个新的 {@code SimpleLocaleContext}，暴露指定的 {@link Locale}。<p>每次调用
	 * {@link #getLocale()} 都会返回此 locale。
	 * @param locale 要暴露的 {@code Locale}；若无特定值则为 {@code null}
	 */
	public SimpleLocaleContext(@Nullable Locale locale) {
		this.locale = locale;
	}

	@Override
	@Nullable
	public Locale getLocale() {
		return this.locale;
	}

	@Override
	public String toString() {
		return (this.locale != null ? this.locale.toString() : "-");
	}

}
