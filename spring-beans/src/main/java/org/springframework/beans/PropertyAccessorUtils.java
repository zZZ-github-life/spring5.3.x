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

package org.springframework.beans;

import org.springframework.lang.Nullable;

/**
 * 用于按照 {@link PropertyAccessor} 接口执行 Bean 属性访问的类的工具方法。
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 */
public abstract class PropertyAccessorUtils {

	/**
	 * 返回给定属性路径的实际属性名。
	 * @param propertyPath 要为其确定属性名称的属性路径
	 * （可以包含属性键，例如用于指定 Map 条目）
	 * @return 实际的属性名称，不含任何键元素
	 */
	public static String getPropertyName(String propertyPath) {
		int separatorIndex = (propertyPath.endsWith(PropertyAccessor.PROPERTY_KEY_SUFFIX) ?
				propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) : -1);
		return (separatorIndex != -1 ? propertyPath.substring(0, separatorIndex) : propertyPath);
	}

	/**
	 * 检查给定属性路径是否表示索引属性或嵌套属性。
	 * @param propertyPath 要检查的属性路径
	 * @return 该路径是否表示索引属性或嵌套属性
	 */
	public static boolean isNestedOrIndexedProperty(@Nullable String propertyPath) {
		if (propertyPath == null) {
			return false;
		}
		for (int i = 0; i < propertyPath.length(); i++) {
			char ch = propertyPath.charAt(i);
			if (ch == PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR ||
					ch == PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定给定属性路径中第一个嵌套属性分隔符，忽略键中的点（如“map[my.key]”）。
	 * @param propertyPath 要检查的属性路径
	 * @return 嵌套属性分隔符的索引，如果没有则为 -1
	 */
	public static int getFirstNestedPropertySeparatorIndex(String propertyPath) {
		return getNestedPropertySeparatorIndex(propertyPath, false);
	}

	/**
	 * 确定给定属性路径中第一个嵌套属性分隔符，忽略键中的点（如“map[my.key]”）。
	 * @param propertyPath 要检查的属性路径
	 * @return 嵌套属性分隔符的索引，如果没有则为 -1
	 */
	public static int getLastNestedPropertySeparatorIndex(String propertyPath) {
		return getNestedPropertySeparatorIndex(propertyPath, true);
	}

	/**
	 * 确定给定属性路径中第一个（或最后一个）嵌套属性分隔符，忽略键中的点（如“map[my.key]”）。
	 * @param propertyPath 要检查的属性路径
	 * @param last 是否返回最后一个分隔符而不是第一个
	 * @return 嵌套属性分隔符的索引，如果没有则为 -1
	 */
	private static int getNestedPropertySeparatorIndex(String propertyPath, boolean last) {
		boolean inKey = false;
		int length = propertyPath.length();
		int i = (last ? length - 1 : 0);
		while (last ? i >= 0 : i < length) {
			switch (propertyPath.charAt(i)) {
				case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR:
				case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR:
					inKey = !inKey;
					break;
				case PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR:
					if (!inKey) {
						return i;
					}
			}
			if (last) {
				i--;
			}
			else {
				i++;
			}
		}
		return -1;
	}

	/**
	 * 确定给定的已注册路径是否与给定的属性路径匹配，即表示该属性本身或该属性的索引元素。
	 * @param propertyPath 属性路径（通常不含索引）
	 * @param registeredPath 已注册的路径（可能包含索引）
	 * @return 这些路径是否匹配
	 */
	public static boolean matchesProperty(String registeredPath, String propertyPath) {
		if (!registeredPath.startsWith(propertyPath)) {
			return false;
		}
		if (registeredPath.length() == propertyPath.length()) {
			return true;
		}
		if (registeredPath.charAt(propertyPath.length()) != PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
			return false;
		}
		return (registeredPath.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR, propertyPath.length() + 1) ==
				registeredPath.length() - 1);
	}

	/**
	 * 确定给定属性路径的规范名称。移除映射键两侧的引号：<br> {@code map['key']} &rarr;
	 * {@code map[key]}<br> {@code map["key"]} &rarr; {@code map[key]}
	 * @param propertyName Bean 属性路径
	 * @return 属性路径的规范表示形式
	 */
	public static String canonicalPropertyName(@Nullable String propertyName) {
		if (propertyName == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(propertyName);
		int searchIndex = 0;
		while (searchIndex != -1) {
			int keyStart = sb.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX, searchIndex);
			searchIndex = -1;
			if (keyStart != -1) {
				int keyEnd = sb.indexOf(
						PropertyAccessor.PROPERTY_KEY_SUFFIX, keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length());
				if (keyEnd != -1) {
					String key = sb.substring(keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length(), keyEnd);
					if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\""))) {
						sb.delete(keyStart + 1, keyStart + 2);
						sb.delete(keyEnd - 2, keyEnd - 1);
						keyEnd = keyEnd - 2;
					}
					searchIndex = keyEnd + PropertyAccessor.PROPERTY_KEY_SUFFIX.length();
				}
			}
		}
		return sb.toString();
	}

	/**
	 * 确定给定属性路径的规范名称。
	 * @param propertyNames Bean 属性路径（作为数组）
	 * @return 属性路径的规范表示形式（作为同样大小的数组）
	 * @see #canonicalPropertyName(String)
	 */
	@Nullable
	public static String[] canonicalPropertyNames(@Nullable String[] propertyNames) {
		if (propertyNames == null) {
			return null;
		}
		String[] result = new String[propertyNames.length];
		for (int i = 0; i < propertyNames.length; i++) {
			result[i] = canonicalPropertyName(propertyNames[i]);
		}
		return result;
	}

}
