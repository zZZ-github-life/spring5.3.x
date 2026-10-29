/*
 * Copyright 2002-2016 the original author or authors.
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
 * 用于向类型转换系统注册转换器。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface ConverterRegistry {

	/**
	 * 向此注册表添加一个普通转换器。可转换的源/目标类型对由该转换器的参数化类型推导得出。
	 * @throws IllegalArgumentException if the parameterized types could not be resolved
	 */
	void addConverter(Converter<?, ?> converter);

	/**
	 * 向此注册表添加一个普通转换器。可转换的源/目标类型对需显式指定。
	 * <p>这使得同一个转换器可以复用于多个不同的类型对，而无需为每个类型对创建转换器类。
	 * @since 3.1
	 */
	<S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter);

	/**
	 * 向此注册表添加一个通用转换器。
	 */
	void addConverter(GenericConverter converter);

	/**
	 * 向此注册表添加一个范围转换器工厂。可转换的源/目标类型对由该 ConverterFactory 的参数化类型推导。
	 * @throws IllegalArgumentException if the parameterized types could not be resolved
	 */
	void addConverterFactory(ConverterFactory<?, ?> factory);

	/**
	 * 移除从 {@code sourceType} 到 {@code targetType} 的所有转换器。
	 * @param sourceType the source type
	 * @param targetType the target type
	 */
	void removeConvertible(Class<?> sourceType, Class<?> targetType);

}
