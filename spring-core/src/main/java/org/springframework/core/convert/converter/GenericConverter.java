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

package org.springframework.core.convert.converter;

import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于在两种或多种类型之间进行转换的通用转换器接口。
 *
 * <p>这是 Converter SPI 接口中最灵活的一个，但也是最复杂的一个。其灵活性在于，一个
 * GenericConverter 可以支持在多个源/目标类型对之间进行转换（参见 {@link #getConvertibleTypes()}）。
 * 此外，GenericConverter 实现在类型转换过程中可以访问源/目标
 * {@link TypeDescriptor field context}。这使得它可以解析源和目标字段的元数据，例如注解和泛型信息，
 * 这些信息可用于影响转换逻辑。
 *
 * <p>当更简单的 {@link Converter} 或 {@link ConverterFactory} 接口已足够时，通常不应使用此接口。
 *
 * <p>实现类还可以额外实现 {@link ConditionalConverter}。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see TypeDescriptor
 * @see Converter
 * @see ConverterFactory
 * @see ConditionalConverter
 */
public interface GenericConverter {

	/**
	 * 返回此转换器可在其间进行转换的源类型和目标类型。
	 * <p>每个条目都是一个可转换的源到目标类型对。
	 * <p>对于 {@link ConditionalConverter conditional converters}，此方法可以返回 {@code null}，表示应
	 * 考虑所有源到目标类型对。
	 */
	@Nullable
	Set<ConvertiblePair> getConvertibleTypes();

	/**
	 * 将源对象转换为 {@code TypeDescriptor} 所描述的目标类型。
	 * @param source 要转换的源对象（可为 {@code null}）
	 * @param sourceType 我们要从中转换的字段的类型描述符
	 * @param targetType 我们要转换到的字段的类型描述符
	 * @return 转换后的对象
	 */
	@Nullable
	Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType);


	/**
	 * 源到目标类对的持有者。
	 */
	final class ConvertiblePair {

		private final Class<?> sourceType;

		private final Class<?> targetType;

		/**
		 * 创建一个新的源到目标类型对。
		 * @param sourceType 源类型
		 * @param targetType 目标类型
		 */
		public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
			Assert.notNull(sourceType, "Source type must not be null");
			Assert.notNull(targetType, "Target type must not be null");
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		public Class<?> getSourceType() {
			return this.sourceType;
		}

		public Class<?> getTargetType() {
			return this.targetType;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || other.getClass() != ConvertiblePair.class) {
				return false;
			}
			ConvertiblePair otherPair = (ConvertiblePair) other;
			return (this.sourceType == otherPair.sourceType && this.targetType == otherPair.targetType);
		}

		@Override
		public int hashCode() {
			return (this.sourceType.hashCode() * 31 + this.targetType.hashCode());
		}

		@Override
		public String toString() {
			return (this.sourceType.getName() + " -> " + this.targetType.getName());
		}
	}

}
