/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Field;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * 定义类型转换方法的接口。通常（但并非必须）与 {@link PropertyEditorRegistry} 接口配合实现。
 *
 * <p><b>注意：</b>由于 TypeConverter 实现通常基于并非线程安全的
 * {@link java.beans.PropertyEditor PropertyEditors}，因此 TypeConverter 本身也<em>不应</em>被视为
 * 线程安全。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleTypeConverter
 * @see BeanWrapperImpl
 */
public interface TypeConverter {

	/**
	 * 将值转换为所需的类型（如有必要，从 String 转换）。
	 * <p>从 String 到任意类型的转换通常使用 PropertyEditor 类的 {@code setAsText} 方法，或
	 * ConversionService 中的 Spring Converter。
	 * @param value the value to convert
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException;

	/**
	 * 将值转换为所需的类型（如有必要，从 String 转换）。
	 * <p>从 String 到任意类型的转换通常使用 PropertyEditor 类的 {@code setAsText} 方法，或
	 * ConversionService 中的 Spring Converter。
	 * @param value the value to convert
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @param methodParam the method parameter that is the target of the conversion
	 * (for analysis of generic types; may be {@code null})
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable MethodParameter methodParam) throws TypeMismatchException;

	/**
	 * 将值转换为所需的类型（如有必要，从 String 转换）。
	 * <p>从 String 到任意类型的转换通常使用 PropertyEditor 类的 {@code setAsText} 方法，或
	 * ConversionService 中的 Spring Converter。
	 * @param value the value to convert
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @param field the reflective field that is the target of the conversion
	 * (for analysis of generic types; may be {@code null})
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
			throws TypeMismatchException;

	/**
	 * 将值转换为所需的类型（如有必要，从 String 转换）。
	 * <p>从 String 到任意类型的转换通常使用 PropertyEditor 类的 {@code setAsText} 方法，或
	 * ConversionService 中的 Spring Converter。
	 * @param value the value to convert
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @param typeDescriptor the type descriptor to use (may be {@code null}))
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @since 5.1.4
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	@Nullable
	default <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		throw new UnsupportedOperationException("TypeDescriptor resolution not supported");
	}

}
