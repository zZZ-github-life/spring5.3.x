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

package org.springframework.web.servlet.i18n;

import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * 拦截器，允许通过可配置的请求参数（默认参数名："locale"）在每个请求上更改当前区域设置。
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 20.06.2003
 * @see org.springframework.web.servlet.LocaleResolver
 */
public class LocaleChangeInterceptor implements HandlerInterceptor {

	/**
	 * 区域设置规范参数的默认名称："locale"。
	 */
	public static final String DEFAULT_PARAM_NAME = "locale";


	protected final Log logger = LogFactory.getLog(getClass());

	private String paramName = DEFAULT_PARAM_NAME;

	@Nullable
	private String[] httpMethods;

	private boolean ignoreInvalidLocale = false;


	/**
	 * 设置区域设置更改请求中包含区域设置规范的参数名称。默认为 "locale"。
	 */
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	/**
	 * 返回区域设置更改请求中包含区域设置规范的参数名称。
	 */
	public String getParamName() {
		return this.paramName;
	}

	/**
	 * 配置可以更改区域设置的 HTTP 方法。
	 * @param httpMethods the methods
	 * @since 4.2
	 */
	public void setHttpMethods(@Nullable String... httpMethods) {
		this.httpMethods = httpMethods;
	}

	/**
	 * 返回已配置的 HTTP 方法。
	 * @since 4.2
	 */
	@Nullable
	public String[] getHttpMethods() {
		return this.httpMethods;
	}

	/**
	 * 设置是否忽略区域设置参数的无效值。
	 * @since 4.2.2
	 */
	public void setIgnoreInvalidLocale(boolean ignoreInvalidLocale) {
		this.ignoreInvalidLocale = ignoreInvalidLocale;
	}

	/**
	 * 返回是否忽略区域设置参数的无效值。
	 * @since 4.2.2
	 */
	public boolean isIgnoreInvalidLocale() {
		return this.ignoreInvalidLocale;
	}

	/**
	 * 指定是否将请求参数值解析为 BCP 47 语言标签，而不是 Java 的传统区域设置规范格式。<p><b>注意：自 5.1 起，此解析器宽松地接受传统 {@link Locale#toString} 格式以及 BCP 47 语言标签。</b>
	 * @since 4.3
	 * @see Locale#forLanguageTag(String)
	 * @see Locale#toLanguageTag()
	 * @deprecated as of 5.1 since it only accepts {@code true} now
	 */
	@Deprecated
	public void setLanguageTagCompliant(boolean languageTagCompliant) {
		if (!languageTagCompliant) {
			throw new IllegalArgumentException("LocaleChangeInterceptor always accepts BCP 47 language tags");
		}
	}

	/**
	 * 返回是使用 BCP 47 语言标签，还是 Java 的传统区域设置规范格式。
	 * @since 4.3
	 * @deprecated as of 5.1 since it always returns {@code true} now
	 */
	@Deprecated
	public boolean isLanguageTagCompliant() {
		return true;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		String newLocale = request.getParameter(getParamName());
		if (newLocale != null) {
			if (checkHttpMethod(request.getMethod())) {
				LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
				if (localeResolver == null) {
					throw new IllegalStateException(
							"No LocaleResolver found: not in a DispatcherServlet request?");
				}
				try {
					localeResolver.setLocale(request, response, parseLocaleValue(newLocale));
				}
				catch (IllegalArgumentException ex) {
					if (isIgnoreInvalidLocale()) {
						if (logger.isDebugEnabled()) {
							logger.debug("Ignoring invalid locale value [" + newLocale + "]: " + ex.getMessage());
						}
					}
					else {
						throw ex;
					}
				}
			}
		}
		// Proceed in any case.
		return true;
	}

	private boolean checkHttpMethod(String currentMethod) {
		String[] configuredMethods = getHttpMethods();
		if (ObjectUtils.isEmpty(configuredMethods)) {
			return true;
		}
		for (String configuredMethod : configuredMethods) {
			if (configuredMethod.equalsIgnoreCase(currentMethod)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 解析来自请求参数的给定区域设置值。<p>默认实现调用 {@link StringUtils#parseLocale(String)}，接受 {@link Locale#toString} 格式以及 BCP 47 语言标签。
	 * @param localeValue the locale value to parse
	 * @return the corresponding {@code Locale} instance
	 * @since 4.3
	 */
	@Nullable
	protected Locale parseLocaleValue(String localeValue) {
		return StringUtils.parseLocale(localeValue);
	}

}
