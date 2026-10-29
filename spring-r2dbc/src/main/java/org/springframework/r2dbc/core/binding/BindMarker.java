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

import io.r2dbc.spi.Statement;

/**
 * 绑定标记表示查询中的单个可绑定参数。
 * 绑定标记与方言相关，并提供一个用于实际查询中的
 * {@link #getPlaceholder() 占位符}。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see Statement#bind
 * @see BindMarkers
 * @see BindMarkersFactory
 */
public interface BindMarker {

	/**
	 * 返回给定替换项对应的数据库特定占位符。
	 */
	String getPlaceholder();

	/**
	 * 使用底层绑定策略将给定的 {@code value} 绑定到 {@link Statement}。
	 * @param bindTarget the target to bind the value to
	 * @param value the actual value (must not be {@code null};
	 * use {@link #bindNull(BindTarget, Class)} for {@code null} values)
	 * @see Statement#bind
	 */
	void bind(BindTarget bindTarget, Object value);

	/**
	 * 使用底层绑定策略将 {@code null} 值绑定到 {@link Statement}。
	 * @param bindTarget the target to bind the value to
	 * @param valueType the value type (must not be {@code null})
	 * @see Statement#bindNull
	 */
	void bindNull(BindTarget bindTarget, Class<?> valueType);

}
