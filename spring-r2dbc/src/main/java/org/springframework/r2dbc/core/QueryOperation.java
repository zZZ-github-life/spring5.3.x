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

import java.util.function.Supplier;

/**
 * 声明可用查询字符串表示的查询操作的接口。
 * 此接口通常由表示 SQL 操作（如 {@code SELECT}、{@code INSERT} 等）
 * 的类实现。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see PreparedOperation
 */
@FunctionalInterface
public interface QueryOperation extends Supplier<String> {

	/**
	 * 返回此操作的字符串表示，供创建 {@link io.r2dbc.spi.Statement} 时使用。
	 * @return 以 SQL 字符串表示的操作
	 * @see io.r2dbc.spi.Connection#createStatement(String)
	 */
	String toQuery();

	@Override
	default String get() {
		return toQuery();
	}

}
