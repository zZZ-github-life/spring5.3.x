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

package org.springframework.expression.spel.ast;

import java.util.List;
import java.util.StringJoiner;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 解析和求值期间使用的工具方法（格式化器等）。
 *
 * @author Andy Clement
 */
abstract class FormatHelper {

	/**
	 * 为带指定参数的给定方法名生成可读的表示形式。
	 * @param name 方法名称
	 * @param argumentTypes 方法参数的类型
	 * @return 格式良好的表示形式，例如 {@code foo(String,int)}
	 */
	public static String formatMethodForMessage(String name, List<TypeDescriptor> argumentTypes) {
		StringJoiner sj = new StringJoiner(",", "(", ")");
		for (TypeDescriptor typeDescriptor : argumentTypes) {
			if (typeDescriptor != null) {
				sj.add(formatClassNameForMessage(typeDescriptor.getType()));
			}
			else {
				sj.add(formatClassNameForMessage(null));
			}
		}
		return name + sj.toString();
	}

	/**
	 * 确定给定 Class 对象的可读名称。
	 * <p>String 数组的格式化名称为 "java.lang.String[]"。
	 * @param clazz 要格式化其名称的 Class
	 * @return 适合包含在消息中的格式化字符串
	 * @see ClassUtils#getQualifiedName(Class)
	 */
	public static String formatClassNameForMessage(@Nullable Class<?> clazz) {
		return (clazz != null ? ClassUtils.getQualifiedName(clazz) : "null");
	}

}
