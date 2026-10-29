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

package org.springframework.core.convert;

import org.springframework.lang.Nullable;

/**
 * 用于类型转换的服务接口。这是进入转换系统的入口。调用 {@link #convert(Object, Class)} 可使用此系
 * 统执行线程安全的类型转换。
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 */
public interface ConversionService {

	/**
	 * 如果 {@code sourceType} 的对象可以转换为 {@code targetType}，则返回 {@code true}。
	 * <p>如果此方法返回 {@code true}，则意味着 {@link #convert(Object, Class)} 能够将
	 * {@code sourceType} 的实例转换为 {@code targetType}。
	 * <p>关于集合、数组和映射类型的特别说明：对于集合、数组和映射类型之间的转换，即使底层元素不可转换
	 * 时调用转换仍可能产生 {@link ConversionException}，此方法也会返回 {@code true}。在处理集合和映射
	 * 时，调用方应自行处理这种异常情况。
	 * @param sourceType the source type to convert from (may be {@code null} if source is {@code null})
	 * @param targetType the target type to convert to (required)
	 * @return {@code true} if a conversion can be performed, {@code false} if not
	 * @throws IllegalArgumentException if {@code targetType} is {@code null}
	 */
	boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);

	/**
	 * 如果 {@code sourceType} 的对象可以转换为 {@code targetType}，则返回 {@code true}。TypeDescriptor
	 * 提供了有关将发生转换的源位置和目标位置的额外上下文，通常是对象字段或属性位置。
	 * <p>如果此方法返回 {@code true}，则意味着
	 * {@link #convert(Object, TypeDescriptor, TypeDescriptor)} 能够将 {@code sourceType} 的实例转换为
	 * {@code targetType}。
	 * <p>关于集合、数组和映射类型的特别说明：对于集合、数组和映射类型之间的转换，即使底层元素不可转换
	 * 时调用转换仍可能产生 {@link ConversionException}，此方法也会返回 {@code true}。在处理集合和映射
	 * 时，调用方应自行处理这种异常情况。
	 * @param sourceType context about the source type to convert from
	 * (may be {@code null} if source is {@code null})
	 * @param targetType context about the target type to convert to (required)
	 * @return {@code true} if a conversion can be performed between the source and target types,
	 * {@code false} if not
	 * @throws IllegalArgumentException if {@code targetType} is {@code null}
	 */
	boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * 将给定的 {@code source} 转换为指定的 {@code targetType}。
	 * @param source the source object to convert (may be {@code null})
	 * @param targetType the target type to convert to (required)
	 * @return the converted object, an instance of targetType
	 * @throws ConversionException if a conversion exception occurred
	 * @throws IllegalArgumentException if targetType is {@code null}
	 */
	@Nullable
	<T> T convert(@Nullable Object source, Class<T> targetType);

	/**
	 * 将给定的 {@code source} 转换为指定的 {@code targetType}。TypeDescriptor 提供了有关将发生转换的源
	 * 位置和目标位置的额外上下文，通常是对象字段或属性位置。
	 * @param source the source object to convert (may be {@code null})
	 * @param sourceType context about the source type to convert from
	 * (may be {@code null} if source is {@code null})
	 * @param targetType context about the target type to convert to (required)
	 * @return the converted object, an instance of {@link TypeDescriptor#getObjectType() targetType}
	 * @throws ConversionException if a conversion exception occurred
	 * @throws IllegalArgumentException if targetType is {@code null},
	 * or {@code sourceType} is {@code null} but source is not {@code null}
	 */
	@Nullable
	Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}
