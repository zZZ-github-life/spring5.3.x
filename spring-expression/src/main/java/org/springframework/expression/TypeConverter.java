/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * 类型转换器可以在表达式求值期间遇到的不同类型之间转换值。这是表达式解析器的 SPI；Spring 转换设施
 * 的主要用户 API 参见 {@link org.springframework.core.convert.ConversionService}。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface TypeConverter {

	/**
	 * 如果类型转换器可以将指定类型转换为所需的目标类型，则返回 {@code true}。
	 * @param sourceType a type descriptor that describes the source type
	 * @param targetType a type descriptor that describes the requested result type
	 * @return {@code true} if that conversion can be performed
	 */
	boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * 将值从一种类型转换（或强制转换）为另一种类型，例如从 {@code boolean} 转换为 {@code String}。
	 * <p>{@link TypeDescriptor} 参数支持带类型的集合：例如，调用方可能更希望使用 {@code List<Integer>}，
	 * 而不是任意 {@code List}。
	 * @param value the value to be converted
	 * @param sourceType a type descriptor that supplies extra information about the
	 * source object
	 * @param targetType a type descriptor that supplies extra information about the
	 * requested result type
	 * @return the converted value
	 * @throws EvaluationException if conversion failed or is not possible to begin with
	 */
	@Nullable
	Object convertValue(@Nullable Object value, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}
