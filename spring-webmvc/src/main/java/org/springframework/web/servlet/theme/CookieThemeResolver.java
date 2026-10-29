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

package org.springframework.web.servlet.theme;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

/**
 * {@link ThemeResolver} 实现：对于自定义设置，使用回传给用户的 cookie，
 * 并回退到默认主题。
 * 这特别适用于没有用户会话的无状态应用。
 *
 * <p>因此，自定义控制器可以通过调用
 * {@code setThemeName} 来覆盖用户的主题，例如响应某个主题变更请求。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 17.06.2003
 * @see #setThemeName
 */
public class CookieThemeResolver extends CookieGenerator implements ThemeResolver {

	/**
	 * 未提供其他主题名称时使用的默认主题名称。
	 */
	public static final String ORIGINAL_DEFAULT_THEME_NAME = "theme";

	/**
	 * 存放主题名称的请求属性名称。仅在当前请求处理过程中主题发生变化时，
	 * 用于覆盖 cookie 值！在控制器或视图中请使用 RequestContext.getTheme()
	 * 获取当前主题。
	 * @see org.springframework.web.servlet.support.RequestContext#getTheme
	 */
	public static final String THEME_REQUEST_ATTRIBUTE_NAME = CookieThemeResolver.class.getName() + ".THEME";

	/**
	 * 存放主题名称的 cookie 的默认名称。
	 */
	public static final String DEFAULT_COOKIE_NAME = CookieThemeResolver.class.getName() + ".THEME";


	private String defaultThemeName = ORIGINAL_DEFAULT_THEME_NAME;


	public CookieThemeResolver() {
		setCookieName(DEFAULT_COOKIE_NAME);
	}


	/**
	 * 设置默认主题的名称。
	 */
	public void setDefaultThemeName(String defaultThemeName) {
		this.defaultThemeName = defaultThemeName;
	}

	/**
	 * 返回默认主题的名称。
	 */
	public String getDefaultThemeName() {
		return this.defaultThemeName;
	}


	@Override
	public String resolveThemeName(HttpServletRequest request) {
		// Check request for preparsed or preset theme.
		String themeName = (String) request.getAttribute(THEME_REQUEST_ATTRIBUTE_NAME);
		if (themeName != null) {
			return themeName;
		}

		// Retrieve cookie value from request.
		String cookieName = getCookieName();
		if (cookieName != null) {
			Cookie cookie = WebUtils.getCookie(request, cookieName);
			if (cookie != null) {
				String value = cookie.getValue();
				if (StringUtils.hasText(value)) {
					themeName = value;
				}
			}
		}

		// Fall back to default theme.
		if (themeName == null) {
			themeName = getDefaultThemeName();
		}
		request.setAttribute(THEME_REQUEST_ATTRIBUTE_NAME, themeName);
		return themeName;
	}

	@Override
	public void setThemeName(
			HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName) {

		Assert.notNull(response, "HttpServletResponse is required for CookieThemeResolver");

		if (StringUtils.hasText(themeName)) {
			// Set request attribute and add cookie.
			request.setAttribute(THEME_REQUEST_ATTRIBUTE_NAME, themeName);
			addCookie(response, themeName);
		}
		else {
			// Set request attribute to fallback theme and remove cookie.
			request.setAttribute(THEME_REQUEST_ATTRIBUTE_NAME, getDefaultThemeName());
			removeCookie(response);
		}
	}

}
