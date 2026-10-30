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

import org.springframework.lang.Nullable;

/**
 * {@link SqlParameter}的子类，表示输出参数。
 * 没有额外属性：将使用instanceof来检查此类类型。
 *
 * <p>输出参数 - 与所有存储过程参数一样 - 必须有名称。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see SqlReturnResultSet
 * @see SqlInOutParameter
 */
public class SqlOutParameter extends ResultSetSupportingSqlParameter {

	@Nullable
	private SqlReturnType sqlReturnType;


	/**
	 * 创建新的SqlOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 */
	public SqlOutParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param scale 小数点后的位数（用于 DECIMAL 和 NUMERIC 类型）
	 */
	public SqlOutParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param typeName 参数的类型名称（可选）
	 */
	public SqlOutParameter(String name, int sqlType, @Nullable String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param typeName 参数的类型名称（可选）
	 * @param sqlReturnType 复杂类型的自定义值处理器（可选）
	 */
	public SqlOutParameter(String name, int sqlType, @Nullable String typeName, @Nullable SqlReturnType sqlReturnType) {
		super(name, sqlType, typeName);
		this.sqlReturnType = sqlReturnType;
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param rse 用于解析 {@link ResultSet} 的 {@link ResultSetExtractor}
	 */
	public SqlOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType, rse);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param rch 用于解析 {@link ResultSet} 的 {@link RowCallbackHandler}
	 */
	public SqlOutParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType, rch);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param rm 用于解析 {@link ResultSet} 的 {@link RowMapper}
	 */
	public SqlOutParameter(String name, int sqlType, RowMapper<?> rm) {
		super(name, sqlType, rm);
	}


	/**
	 * 返回自定义返回类型（如果有）。
	 */
	@Nullable
	public SqlReturnType getSqlReturnType() {
		return this.sqlReturnType;
	}

	/**
	 * 返回此参数是否持有自定义返回类型。
	 */
	public boolean isReturnTypeSupported() {
		return (this.sqlReturnType != null);
	}

}
