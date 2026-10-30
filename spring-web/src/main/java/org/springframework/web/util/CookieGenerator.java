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

package org.springframework.web.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于生成 Cookie 的辅助类，以 bean 属性形式承载 Cookie 描述符设置，
 * 并能够向给定响应添加 Cookie 或从中移除 Cookie。
 *
 * <p>可作为生成特定 Cookie 的组件的基类，例如 CookieLocaleResolver
 * 和 CookieThemeResolver。
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see #addCookie
 * @see #removeCookie
 * @see org.springframework.web.servlet.i18n.CookieLocaleResolver
 * @see org.springframework.web.servlet.theme.CookieThemeResolver
 */
public class CookieGenerator {

	/**
	 * Cookie 可见的默认路径："/"，即整个服务器。
	 */
	public static final String DEFAULT_COOKIE_PATH = "/";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private String cookieName;

	@Nullable
	private String cookieDomain;

	private String cookiePath = DEFAULT_COOKIE_PATH;

	@Nullable
	private Integer cookieMaxAge;

	private boolean cookieSecure = false;

	private boolean cookieHttpOnly = false;


	/**
	 * 使用给定名称作为此生成器创建的 Cookie 的名称。
	 * @see javax.servlet.http.Cookie#getName()
	 */
	public void setCookieName(@Nullable String cookieName) {
		this.cookieName = cookieName;
	}

	/**
	 * 返回此生成器创建的 Cookie 的名称。
	 */
	@Nullable
	public String getCookieName() {
		return this.cookieName;
	}

	/**
	 * 使用给定域作为此生成器创建的 Cookie 的域。
	 * 该 Cookie 仅对该域中的服务器可见。
	 * @see javax.servlet.http.Cookie#setDomain
	 */
	public void setCookieDomain(@Nullable String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}

	/**
	 * 返回此生成器创建的 Cookie 的域（如果有）。
	 */
	@Nullable
	public String getCookieDomain() {
		return this.cookieDomain;
	}

	/**
	 * 使用给定路径作为此生成器创建的 Cookie 的路径。
	 * 该 Cookie 仅对此路径及其下方的 URL 可见。
	 * @see javax.servlet.http.Cookie#setPath
	 */
	public void setCookiePath(String cookiePath) {
		this.cookiePath = cookiePath;
	}

	/**
	 * 返回此生成器创建的 Cookie 的路径。
	 */
	public String getCookiePath() {
		return this.cookiePath;
	}

	/**
	 * 使用给定最大存活时间（秒）作为此生成器创建的 Cookie 的最大存活时间。
	 * 有用的特殊值：-1 ... 非持久化，客户端关闭时删除。
	 * <p>默认不指定最大存活时间，即使用 Servlet 容器的默认值。
	 * @see javax.servlet.http.Cookie#setMaxAge
	 */
	public void setCookieMaxAge(@Nullable Integer cookieMaxAge) {
		this.cookieMaxAge = cookieMaxAge;
	}

	/**
	 * 返回此生成器创建的 Cookie 的最大存活时间。
	 */
	@Nullable
	public Integer getCookieMaxAge() {
		return this.cookieMaxAge;
	}

	/**
	 * 设置是否仅使用安全协议（如 HTTPS/SSL）发送 Cookie。
	 * 这是给接收浏览器的指示，HTTP 服务器本身不会处理。
	 * <p>默认值为 "false"。
	 * @see javax.servlet.http.Cookie#setSecure
	 */
	public void setCookieSecure(boolean cookieSecure) {
		this.cookieSecure = cookieSecure;
	}

	/**
	 * 返回是否仅使用安全协议（如 HTTPS/SSL）发送 Cookie。
	 */
	public boolean isCookieSecure() {
		return this.cookieSecure;
	}

	/**
	 * 设置是否应使用 "HttpOnly" 属性标记 Cookie。
	 * <p>默认值为 "false"。
	 * @see javax.servlet.http.Cookie#setHttpOnly
	 */
	public void setCookieHttpOnly(boolean cookieHttpOnly) {
		this.cookieHttpOnly = cookieHttpOnly;
	}

	/**
	 * 返回是否应使用 "HttpOnly" 属性标记 Cookie。
	 */
	public boolean isCookieHttpOnly() {
		return this.cookieHttpOnly;
	}


	/**
	 * 使用此生成器的 Cookie 描述符设置，将具有给定值的 Cookie 添加到响应。
	 * <p>Cookie 的创建委托给 {@link #createCookie}。
	 * @param response 要向其添加 Cookie 的 HTTP 响应
	 * @param cookieValue 要添加的 Cookie 值
	 * @see #setCookieName
	 * @see #setCookieDomain
	 * @see #setCookiePath
	 * @see #setCookieMaxAge
	 */
	public void addCookie(HttpServletResponse response, String cookieValue) {
		Assert.notNull(response, "HttpServletResponse must not be null");
		Cookie cookie = createCookie(cookieValue);
		Integer maxAge = getCookieMaxAge();
		if (maxAge != null) {
			cookie.setMaxAge(maxAge);
		}
		if (isCookieSecure()) {
			cookie.setSecure(true);
		}
		if (isCookieHttpOnly()) {
			cookie.setHttpOnly(true);
		}
		response.addCookie(cookie);
		if (logger.isTraceEnabled()) {
			logger.trace("Added cookie [" + getCookieName() + "=" + cookieValue + "]");
		}
	}

	/**
	 * 从响应中移除此生成器所描述的 Cookie。
	 * 将生成一个值为空、最大存活时间为 0 的 Cookie。
	 * <p>Cookie 的创建委托给 {@link #createCookie}。
	 * @param response 要从中移除 Cookie 的 HTTP 响应
	 * @see #setCookieName
	 * @see #setCookieDomain
	 * @see #setCookiePath
	 */
	public void removeCookie(HttpServletResponse response) {
		Assert.notNull(response, "HttpServletResponse must not be null");
		Cookie cookie = createCookie("");
		cookie.setMaxAge(0);
		if (isCookieSecure()) {
			cookie.setSecure(true);
		}
		if (isCookieHttpOnly()) {
			cookie.setHttpOnly(true);
		}
		response.addCookie(cookie);
		if (logger.isTraceEnabled()) {
			logger.trace("Removed cookie '" + getCookieName() + "'");
		}
	}

	/**
	 * 使用此生成器的 Cookie 描述符设置（"cookieMaxAge" 除外），
	 * 创建具有给定值的 Cookie。
	 * @param cookieValue 要创建的 Cookie 值
	 * @return 该 Cookie
	 * @see #setCookieName
	 * @see #setCookieDomain
	 * @see #setCookiePath
	 */
	protected Cookie createCookie(String cookieValue) {
		Cookie cookie = new Cookie(getCookieName(), cookieValue);
		if (getCookieDomain() != null) {
			cookie.setDomain(getCookieDomain());
		}
		cookie.setPath(getCookiePath());
		return cookie;
	}

}
