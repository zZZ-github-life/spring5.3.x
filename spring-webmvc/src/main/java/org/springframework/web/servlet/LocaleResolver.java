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

import org.springframework.lang.Nullable;

/**
 * 基于 Web 的区域设置解析策略接口，既支持通过请求解析区域设置，
 * 也支持通过请求和响应修改区域设置。
 *
 * <p>该接口允许基于请求、会话、Cookie 等实现多种策略。默认实现是
 * {@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}，
 * 它仅使用相应 HTTP 头提供的请求区域设置。
 *
 * <p>在控制器或视图中可使用
 * {@link org.springframework.web.servlet.support.RequestContext#getLocale()}
 * 获取当前区域设置，而无需关心实际使用的解析策略。
 *
 * <p>注意：从 Spring 4.0 开始，有一个名为 {@link LocaleContextResolver} 的扩展策略接口，
 * 允许解析 {@link org.springframework.context.i18n.LocaleContext} 对象，
 * 其中可能包含关联的时区信息。Spring 提供的解析器实现在适当的地方
 * 都实现了扩展的 {@link LocaleContextResolver} 接口。
 *
 * @author Juergen Hoeller
 * @since 27.02.2003
 * @see LocaleContextResolver
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.servlet.support.RequestContext#getLocale
 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
 */
public interface LocaleResolver {

	/**
	 * 通过给定的请求解析当前的区域设置。
	 * <p>在任何情况下都可以返回默认区域设置作为回退。
	 * @param request the request to resolve the locale for
	 * @return the current locale (never {@code null})
	 */
	Locale resolveLocale(HttpServletRequest request);

	/**
	 * 将当前的区域设置设置为给定的区域设置。
	 * @param request the request to be used for locale modification
	 * @param response the response to be used for locale modification
	 * @param locale the new locale, or {@code null} to clear the locale
	 * @throws UnsupportedOperationException if the LocaleResolver
	 * implementation does not support dynamic changing of the locale
	 */
	void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale);

}
