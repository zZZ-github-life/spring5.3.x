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
 * @param <S> 源类型
 * @param <T> 目标类型
 */
@FunctionalInterface
public interface Converter<S, T> {

	/**
	 * 将类型为 {@code S} 的源对象转换为目标类型 {@code T}。
	 * @param source 要转换的源对象，必须是 {@code S} 的实例
	 * （永不为 {@code null}）
	 * @return 转换后的对象，必须是 {@code T} 的实例
	 * （可能为 {@code null}）
	 * @throws IllegalArgumentException 如果源无法转换为所需的目标类型
	 */
	@Nullable
	T convert(S source);

	/**
	 * 构造一个组合 {@link Converter}，它先对输入应用此 {@link Converter}，然后对结果应用 {@code after}
	 * {@link Converter}。
	 * @param after 在此 {@link Converter} 应用之后应用的 {@link Converter}
	 * @param <U> 同时作为 {@code after} {@link Converter} 和
	 * 组合 {@link Converter} 的输出类型
	 * @return 一个组合的 {@link Converter}，先应用此 {@link Converter}，
	 * 然后应用 {@code after} {@link Converter}
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
