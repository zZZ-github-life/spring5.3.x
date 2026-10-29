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

package org.springframework.web.servlet.support;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;

import org.springframework.lang.Nullable;

/**
 * RequestContext 的 JSP 感知（以及 JSTL 感知）子类，允许从 {@code javax.servlet.jsp.PageContext} 填充上下文。
 *
 * <p>除了基类提供的回退区域设置策略之外，此上下文还会在页面/请求/会话/应用作用域中检测 JSTL 区域设置属性。
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see #getFallbackLocale
 */
public class JspAwareRequestContext extends RequestContext {

	private PageContext pageContext;


	/**
	 * 为给定的页面上下文创建一个新的 JspAwareRequestContext，使用请求属性来获取 Errors。
	 * @param pageContext current JSP page context
	 */
	public JspAwareRequestContext(PageContext pageContext) {
		this(pageContext, null);
	}

	/**
	 * 为给定的页面上下文创建一个新的 JspAwareRequestContext，使用给定的模型属性来获取 Errors。
	 * @param pageContext current JSP page context
	 * @param model the model attributes for the current view
	 * (can be {@code null}, using the request attributes for Errors retrieval)
	 */
	public JspAwareRequestContext(PageContext pageContext, @Nullable Map<String, Object> model) {
		super((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(),
				pageContext.getServletContext(), model);
		this.pageContext = pageContext;
	}


	/**
	 * 返回底层的 PageContext。
	 * 仅供此包中协作的类使用。
	 */
	protected final PageContext getPageContext() {
		return this.pageContext;
	}

	/**
	 * 此实现会检查页面、请求、会话或应用作用域中的 JSTL 区域设置属性；如果未找到，则返回 {@code HttpServletRequest.getLocale()}。
	 */
	@Override
	protected Locale getFallbackLocale() {
		if (jstlPresent) {
			Locale locale = JstlPageLocaleResolver.getJstlLocale(getPageContext());
			if (locale != null) {
				return locale;
			}
		}
		return getRequest().getLocale();
	}

	/**
	 * 此实现会检查页面、请求、会话或应用作用域中的 JSTL 时区属性；如果未找到，则返回 {@code null}。
	 */
	@Override
	protected TimeZone getFallbackTimeZone() {
		if (jstlPresent) {
			TimeZone timeZone = JstlPageLocaleResolver.getJstlTimeZone(getPageContext());
			if (timeZone != null) {
				return timeZone;
			}
		}
		return null;
	}


	/**
	 * 用于隔离 JSTL 依赖的内部类。
	 * 仅在 JSTL API 存在时调用，以解析回退区域设置。
	 */
	private static class JstlPageLocaleResolver {

		@Nullable
		public static Locale getJstlLocale(PageContext pageContext) {
			Object localeObject = Config.find(pageContext, Config.FMT_LOCALE);
			return (localeObject instanceof Locale ? (Locale) localeObject : null);
		}

		@Nullable
		public static TimeZone getJstlTimeZone(PageContext pageContext) {
			Object timeZoneObject = Config.find(pageContext, Config.FMT_TIME_ZONE);
			return (timeZoneObject instanceof TimeZone ? (TimeZone) timeZoneObject : null);
		}
	}

}
