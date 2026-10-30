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

package org.springframework.jdbc.core;

import java.sql.ResultSet;

/**
 * {@link SqlOutParameter}的子类，表示INOUT参数。
 * 与标准的SqlOutParameter相反，对于SqlParameter的{@link #isInputValueProvided}测试将返回{@code true}。
 *
 * <p>输出参数 - 与所有存储过程参数一样 - 必须有名称。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 */
public class SqlInOutParameter extends SqlOutParameter {

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 */
	public SqlInOutParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param scale 小数点后的位数（用于 DECIMAL 和 NUMERIC 类型）
	 */
	public SqlInOutParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param typeName 参数的类型名称（可选）
	 */
	public SqlInOutParameter(String name, int sqlType, String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param typeName 参数的类型名称（可选）
	 * @param sqlReturnType 复杂类型的自定义值处理器（可选）
	 */
	public SqlInOutParameter(String name, int sqlType, String typeName, SqlReturnType sqlReturnType) {
		super(name, sqlType, typeName, sqlReturnType);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param rse 用于解析 {@link ResultSet} 的 {@link ResultSetExtractor}
	 */
	public SqlInOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType, rse);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param rch 用于解析 {@link ResultSet} 的 {@link RowCallbackHandler}
	 */
	public SqlInOutParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType, rch);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param rm 用于解析 {@link ResultSet} 的 {@link RowMapper}
	 */
	public SqlInOutParameter(String name, int sqlType, RowMapper<?> rm) {
		super(name, sqlType, rm);
	}


	/**
	 * 此实现始终返回{@code true}。
	 */
	@Override
	public boolean isInputValueProvided() {
		return true;
	}

}
