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

package org.springframework.r2dbc.core;

import org.springframework.lang.Nullable;

/**
 * 定义通用功能的接口，供能够为命名绑定参数提供参数值的对象实现，
 * 可作为 {@link NamedParameterExpander} 操作的参数。
 *
 * <p>此接口除了参数值之外还允许指定类型。所有参数值和类型
 * 都通过指定参数名称来标识。
 *
 * <p>旨在以一致的接口包装各种实现，例如 {@link java.util.Map}。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see MapBindParameterSource
 */
interface BindParameterSource {

	/**
	 * 判断指定的命名参数是否有值。
	 * @param paramName the name of the parameter
	 * @return {@code true} if there is a value defined; {@code false} otherwise
	 */
	boolean hasValue(String paramName);

	/**
	 * 返回所请求的命名参数的参数值。
	 * @param paramName the name of the parameter
	 * @return the value of the specified parameter (can be {@code null})
	 * @throws IllegalArgumentException if there is no value
	 * for the requested parameter
	 */
	@Nullable
	Object getValue(String paramName) throws IllegalArgumentException;

	/**
	 * 确定指定的命名参数的类型。
	 * @param paramName the name of the parameter
	 * @return the type of the specified parameter, or
	 * {@link Object#getClass()} if not known.
	 */
	default Class<?> getType(String paramName) {
		return Object.class;
	}

	/**
	 * 返回底层参数源的参数名称。
	 * @return an iterator over the parameter names
	 */
	Iterable<String> getParameterNames();

}
