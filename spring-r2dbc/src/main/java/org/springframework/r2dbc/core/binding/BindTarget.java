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

package org.springframework.r2dbc.core.binding;

/**
 * 用于应用绑定的目标。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see io.r2dbc.spi.Statement#bind
 * @see io.r2dbc.spi.Statement#bindNull
 */
public interface BindTarget {

	/**
	 * 绑定一个值。
	 * @param identifier the identifier to bind to
	 * @param value the value to bind
	 */
	void bind(String identifier, Object value);

	/**
	 * 将值绑定到索引。索引从零开始。
	 * @param index the index to bind to
	 * @param value the value to bind
	 */
	void bind(int index, Object value);

	/**
	 * 绑定一个 {@code null} 值。
	 * @param identifier the identifier to bind to
	 * @param type the type of {@code null} value
	 */
	void bindNull(String identifier, Class<?> type);

	/**
	 * 绑定一个 {@code null} 值。
	 * @param index the index to bind to
	 * @param type the type of {@code null} value
	 */
	void bindNull(int index, Class<?> type);

}
