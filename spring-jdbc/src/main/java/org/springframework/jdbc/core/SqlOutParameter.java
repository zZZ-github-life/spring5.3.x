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
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 */
	public SqlOutParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 */
	public SqlOutParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 */
	public SqlOutParameter(String name, int sqlType, @Nullable String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 * @param sqlReturnType custom value handler for complex type (optional)
	 */
	public SqlOutParameter(String name, int sqlType, @Nullable String typeName, @Nullable SqlReturnType sqlReturnType) {
		super(name, sqlType, typeName);
		this.sqlReturnType = sqlReturnType;
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rse the {@link ResultSetExtractor} to use for parsing the {@link ResultSet}
	 */
	public SqlOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType, rse);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rch the {@link RowCallbackHandler} to use for parsing the {@link ResultSet}
	 */
	public SqlOutParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType, rch);
	}

	/**
	 * 创建新的SqlOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rm the {@link RowMapper} to use for parsing the {@link ResultSet}
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
