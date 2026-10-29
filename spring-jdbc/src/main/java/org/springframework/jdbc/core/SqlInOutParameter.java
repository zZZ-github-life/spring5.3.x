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
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 */
	public SqlInOutParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 */
	public SqlInOutParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 */
	public SqlInOutParameter(String name, int sqlType, String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 * @param sqlReturnType custom value handler for complex type (optional)
	 */
	public SqlInOutParameter(String name, int sqlType, String typeName, SqlReturnType sqlReturnType) {
		super(name, sqlType, typeName, sqlReturnType);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rse the {@link ResultSetExtractor} to use for parsing the {@link ResultSet}
	 */
	public SqlInOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType, rse);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rch the {@link RowCallbackHandler} to use for parsing the {@link ResultSet}
	 */
	public SqlInOutParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType, rch);
	}

	/**
	 * 创建新的SqlInOutParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rm the {@link RowMapper} to use for parsing the {@link ResultSet}
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
