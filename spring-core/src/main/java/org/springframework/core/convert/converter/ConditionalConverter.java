/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;

/**
 * 允许 {@link Converter}、{@link GenericConverter} 或 {@link ConverterFactory} 基于 {@code source}
 * 和 {@code target} 的 {@link TypeDescriptor} 属性有条件地执行。
 *
 * <p>通常用于根据字段或类级别特征（例如注解或方法）的存在来有选择地匹配自定义转换逻辑。例如，在从
 * String 字段转换为 Date 字段时，如果目标字段还标注了 {@code @DateTimeFormat}，则实现可能会返回
 * {@code true}。
 *
 * <p>再举一例，在从 String 字段转换为 {@code Account} 字段时，如果目标 Account 类定义了
 * {@code public static findAccount(String)} 方法，则实现可能会返回 {@code true}。
 *
 * @author Phillip Webb
 * @author Keith Donald
 * @since 3.2
 * @see Converter
 * @see GenericConverter
 * @see ConverterFactory
 * @see ConditionalGenericConverter
 */
public interface ConditionalConverter {

	/**
	 * 当前考虑的从 {@code sourceType} 到 {@code targetType} 的转换是否应被选中？
	 * @param sourceType 我们要从中转换的字段的类型描述符
	 * @param targetType 我们要转换到的字段的类型描述符
	 * @return 如果应执行转换则返回 true，否则返回 false
	 */
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);

}
