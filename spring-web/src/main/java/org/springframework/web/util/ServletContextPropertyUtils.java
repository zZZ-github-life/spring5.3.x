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

package org.springframework.web.util;

import javax.servlet.ServletContext;

import org.springframework.lang.Nullable;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * 用于解析文本中占位符的辅助类。通常应用于文件路径。
 *
 * <p>文本中可能包含 {@code ${...}} 占位符，将被解析为 Servlet 上下文
 * 初始化参数或系统属性，例如 {@code ${user.dir}}。可以使用键和值之间的
 * ":" 分隔符提供默认值。
 *
 * @author Juergen Hoeller
 * @author Marten Deinum
 * @since 3.2.2
 * @see SystemPropertyUtils
 * @see ServletContext#getInitParameter(String)
 */
public abstract class ServletContextPropertyUtils {

	private static final PropertyPlaceholderHelper strictHelper =
			new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, false);

	private static final PropertyPlaceholderHelper nonStrictHelper =
			new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true);


	/**
	 * 解析给定文本中的 ${...} 占位符，将其替换为对应的
	 * Servlet 上下文初始化参数或系统属性值。
	 * @param text 要解析的 String
	 * @param servletContext 用于查找的 servletContext。
	 * @return 解析后的 String
	 * @throws IllegalArgumentException 如果存在无法解析的占位符
	 * @see SystemPropertyUtils#PLACEHOLDER_PREFIX
	 * @see SystemPropertyUtils#PLACEHOLDER_SUFFIX
	 * @see SystemPropertyUtils#resolvePlaceholders(String, boolean)
	 */
	public static String resolvePlaceholders(String text, ServletContext servletContext) {
		return resolvePlaceholders(text, servletContext, false);
	}

	/**
	 * 解析给定文本中的 ${...} 占位符，将其替换为对应的
	 * Servlet 上下文初始化参数或系统属性值。如果标志设置为 true，
	 * 则没有默认值的无法解析占位符会被忽略并原样保留。
	 * @param text 要解析的 String
	 * @param servletContext 用于查找的 servletContext。
	 * @param ignoreUnresolvablePlaceholders 用于确定是否忽略无法解析的占位符的标志
	 * @return 解析后的 String
	 * @throws IllegalArgumentException 如果存在无法解析的占位符且标志为 {@code false}
	 * @see SystemPropertyUtils#PLACEHOLDER_PREFIX
	 * @see SystemPropertyUtils#PLACEHOLDER_SUFFIX
	 * @see SystemPropertyUtils#resolvePlaceholders(String, boolean)
	 */
	public static String resolvePlaceholders(
			String text, ServletContext servletContext, boolean ignoreUnresolvablePlaceholders) {

		if (text.isEmpty()) {
			return text;
		}
		PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
		return helper.replacePlaceholders(text, new ServletContextPlaceholderResolver(text, servletContext));
	}


	private static class ServletContextPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

		private final String text;

		private final ServletContext servletContext;

		public ServletContextPlaceholderResolver(String text, ServletContext servletContext) {
			this.text = text;
			this.servletContext = servletContext;
		}

		@Override
		@Nullable
		public String resolvePlaceholder(String placeholderName) {
			try {
				String propVal = this.servletContext.getInitParameter(placeholderName);
				if (propVal == null) {
					// Fall back to system properties.
					propVal = System.getProperty(placeholderName);
					if (propVal == null) {
						// Fall back to searching the system environment.
						propVal = System.getenv(placeholderName);
					}
				}
				return propVal;
			}
			catch (Throwable ex) {
				System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" +
						this.text + "] as ServletContext init-parameter or system property: " + ex);
				return null;
			}
		}
	}

}
