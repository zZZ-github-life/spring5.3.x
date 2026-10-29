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

package org.springframework.core.convert.converter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 转换器将类型为 {@code S} 的源对象转换为类型为 {@code T} 的目标对象。
 *
 * <p>此接口的实现是线程安全的，可以共享。
 *
 * <p>实现类还可以额外实现 {@link ConditionalConverter}。
 *
 * @author Keith Donald
 * @author Josh Cummings
 * @since 3.0
 * @param <S> the source type
 * @param <T> the target type
 */
@FunctionalInterface
public interface Converter<S, T> {

	/**
	 * 将类型为 {@code S} 的源对象转换为目标类型 {@code T}。
	 * @param source the source object to convert, which must be an instance of {@code S} (never {@code null})
	 * @return the converted object, which must be an instance of {@code T} (potentially {@code null})
	 * @throws IllegalArgumentException if the source cannot be converted to the desired target type
	 */
	@Nullable
	T convert(S source);

	/**
	 * 构造一个组合 {@link Converter}，它先对输入应用此 {@link Converter}，然后对结果应用 {@code after}
	 * {@link Converter}。
	 * @param after the {@link Converter} to apply after this {@link Converter}
	 * is applied
	 * @param <U> the type of output of both the {@code after} {@link Converter}
	 * and the composed {@link Converter}
	 * @return a composed {@link Converter} that first applies this {@link Converter}
	 * and then applies the {@code after} {@link Converter}
	 * @since 5.3
	 */
	default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
		Assert.notNull(after, "After Converter must not be null");
		return (S s) -> {
			T initialResult = convert(s);
			return (initialResult != null ? after.convert(initialResult) : null);
		};
	}

}
