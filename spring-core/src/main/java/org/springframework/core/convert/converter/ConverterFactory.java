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

package org.springframework.core.convert.converter;

/**
 * 用于“范围”转换器的工厂，可将对象从 S 转换为 R 的子类型。
 *
 * <p>实现类还可以额外实现 {@link ConditionalConverter}。
 *
 * @author Keith Donald
 * @since 3.0
 * @param <S> 本工厂创建的转换器可从中转换的源类型
 * @param <R> 本工厂创建的转换器可转换到的目标范围（或基础）类型；
 * 例如对于一组数字子类型，为 {@link Number}。
 * @see ConditionalConverter
 */
public interface ConverterFactory<S, R> {

	/**
	 * 获取用于从 S 转换为目标类型 T 的转换器，其中 T 也是 R 的实例。
	 * @param <T> 目标类型
	 * @param targetType 要转换到的目标类型
	 * @return 从 S 到 T 的转换器
	 */
	<T extends R> Converter<S, T> getConverter(Class<T> targetType);

}
