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

package org.springframework.expression.common;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 任何表达式语言提供者都可以使用的通用工具方法。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class ExpressionUtils {

	/**
	 * 判断指定上下文中是否存在可用的类型转换器，并尝试用它把提供的值转换为指定类型。如果无法转换，则抛
	 * 出异常。
	 * @param context the evaluation context that may define a type converter
	 * @param typedValue the value to convert and a type descriptor describing it
	 * @param targetType the type to attempt conversion to
	 * @return the converted value
	 * @throws EvaluationException if there is a problem during conversion or conversion
	 * of the value to the specified type is not supported
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T convertTypedValue(
			@Nullable EvaluationContext context, TypedValue typedValue, @Nullable Class<T> targetType) {

		Object value = typedValue.getValue();
		if (targetType == null) {
			return (T) value;
		}
		if (context != null) {
			return (T) context.getTypeConverter().convertValue(
					value, typedValue.getTypeDescriptor(), TypeDescriptor.valueOf(targetType));
		}
		if (ClassUtils.isAssignableValue(targetType, value)) {
			return (T) value;
		}
		throw new EvaluationException("Cannot convert value '" + value + "' to type '" + targetType.getName() + "'");
	}

	/**
	 * 尝试使用提供的类型转换器把带类型的值转换为 int。
	 */
	public static int toInt(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Integer.class);
	}

	/**
	 * 尝试使用提供的类型转换器把带类型的值转换为 boolean。
	 */
	public static boolean toBoolean(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Boolean.class);
	}

	/**
	 * 尝试使用提供的类型转换器把带类型的值转换为 double。
	 */
	public static double toDouble(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Double.class);
	}

	/**
	 * 尝试使用提供的类型转换器把带类型的值转换为 long。
	 */
	public static long toLong(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Long.class);
	}

	/**
	 * 尝试使用提供的类型转换器把带类型的值转换为 char。
	 */
	public static char toChar(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Character.class);
	}

	/**
	 * 尝试使用提供的类型转换器把带类型的值转换为 short。
	 */
	public static short toShort(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Short.class);
	}

	/**
	 * 尝试使用提供的类型转换器把带类型的值转换为 float。
	 */
	public static float toFloat(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Float.class);
	}

	/**
	 * 尝试使用提供的类型转换器把带类型的值转换为 byte。
	 */
	public static byte toByte(TypeConverter typeConverter, TypedValue typedValue) {
		return convertValue(typeConverter, typedValue, Byte.class);
	}

	@SuppressWarnings("unchecked")
	private static <T> T convertValue(TypeConverter typeConverter, TypedValue typedValue, Class<T> targetType) {
		Object result = typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(targetType));
		if (result == null) {
			throw new IllegalStateException("Null conversion result for value [" + typedValue.getValue() + "]");
		}
		return (T) result;
	}

}
