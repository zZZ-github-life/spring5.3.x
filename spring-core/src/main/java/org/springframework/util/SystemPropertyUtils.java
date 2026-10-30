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

package org.springframework.util;

import org.springframework.lang.Nullable;

/**
 * 用于解析文本中占位符的辅助类。通常应用于文件路径。
 *
 * <p>文本中可能包含 {@code ${...}} 占位符，它们会被解析为系统属性：例如 {@code ${user.dir}}。可以
 * 使用键和值之间的 ":" 分隔符来提供默认值。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @since 1.2.5
 * @see #PLACEHOLDER_PREFIX
 * @see #PLACEHOLDER_SUFFIX
 * @see System#getProperty(String)
 */
public abstract class SystemPropertyUtils {

	/** 系统属性占位符的前缀："${"。 */
	public static final String PLACEHOLDER_PREFIX = "${";

	/** 系统属性占位符的后缀："}"。 */
	public static final String PLACEHOLDER_SUFFIX = "}";

	/** 系统属性占位符的值分隔符：":"。 */
	public static final String VALUE_SEPARATOR = ":";


	private static final PropertyPlaceholderHelper strictHelper =
			new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, false);

	private static final PropertyPlaceholderHelper nonStrictHelper =
			new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true);


	/**
	 * 解析给定文本中的 {@code ${...}} 占位符，将其替换为相应的系统属性值。
	 * @param text 要解析的 String
	 * @return 解析后的 String
	 * @throws IllegalArgumentException 如果存在无法解析的占位符
	 * @see #PLACEHOLDER_PREFIX
	 * @see #PLACEHOLDER_SUFFIX
	 */
	public static String resolvePlaceholders(String text) {
		return resolvePlaceholders(text, false);
	}

	/**
	 * 解析给定文本中的 {@code ${...}} 占位符，将其替换为相应的系统属性值。如果标志设置为 {@code true}，
	 * 则没有默认值的无法解析的占位符会被忽略并原样传递。
	 * @param text 要解析的 String
	 * @param ignoreUnresolvablePlaceholders 是否忽略无法解析的占位符
	 * @return 解析后的 String
	 * @throws IllegalArgumentException 如果存在无法解析的占位符
	 * @see #PLACEHOLDER_PREFIX
	 * @see #PLACEHOLDER_SUFFIX
	 * and the "ignoreUnresolvablePlaceholders" flag is {@code false}
	 */
	public static String resolvePlaceholders(String text, boolean ignoreUnresolvablePlaceholders) {
		if (text.isEmpty()) {
			return text;
		}
		PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
		return helper.replacePlaceholders(text, new SystemPropertyPlaceholderResolver(text));
	}


	/**
	 * PlaceholderResolver 实现，针对系统属性和系统环境变量进行解析。
	 */
	private static class SystemPropertyPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

		private final String text;

		public SystemPropertyPlaceholderResolver(String text) {
			this.text = text;
		}

		@Override
		@Nullable
		public String resolvePlaceholder(String placeholderName) {
			try {
				String propVal = System.getProperty(placeholderName);
				if (propVal == null) {
					// Fall back to searching the system environment.
					propVal = System.getenv(placeholderName);
				}
				return propVal;
			}
			catch (Throwable ex) {
				System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" +
						this.text + "] as system property: " + ex);
				return null;
			}
		}
	}

}
