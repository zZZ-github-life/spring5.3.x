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
 * 支持ResultSet的SqlParameter的公共基类，如
 * {@link SqlOutParameter}和{@link SqlReturnResultSet}。
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class ResultSetSupportingSqlParameter extends SqlParameter {

	@Nullable
	private ResultSetExtractor<?> resultSetExtractor;

	@Nullable
	private RowCallbackHandler rowCallbackHandler;

	@Nullable
	private RowMapper<?> rowMapper;


	/**
	 * 创建新的ResultSetSupportingSqlParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * 创建新的ResultSetSupportingSqlParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * 创建新的ResultSetSupportingSqlParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, @Nullable String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * 创建新的ResultSetSupportingSqlParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rse the {@link ResultSetExtractor} to use for parsing the {@link ResultSet}
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType);
		this.resultSetExtractor = rse;
	}

	/**
	 * 创建新的ResultSetSupportingSqlParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rch the {@link RowCallbackHandler} to use for parsing the {@link ResultSet}
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType);
		this.rowCallbackHandler = rch;
	}

	/**
	 * 创建新的ResultSetSupportingSqlParameter。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rm the {@link RowMapper} to use for parsing the {@link ResultSet}
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, RowMapper<?> rm) {
		super(name, sqlType);
		this.rowMapper = rm;
	}


	/**
	 * 此参数是否支持ResultSet，即是否持有ResultSetExtractor、RowCallbackHandler或RowMapper？
	 */
	public boolean isResultSetSupported() {
		return (this.resultSetExtractor != null || this.rowCallbackHandler != null || this.rowMapper != null);
	}

	/**
	 * 返回此参数持有的ResultSetExtractor（如果有）。
	 */
	@Nullable
	public ResultSetExtractor<?> getResultSetExtractor() {
		return this.resultSetExtractor;
	}

	/**
	 * 返回此参数持有的RowCallbackHandler（如果有）。
	 */
	@Nullable
	public RowCallbackHandler getRowCallbackHandler() {
		return this.rowCallbackHandler;
	}

	/**
	 * 返回此参数持有的RowMapper（如果有）。
	 */
	@Nullable
	public RowMapper<?> getRowMapper() {
		return this.rowMapper;
	}


	/**
	 * 此实现始终返回{@code false}。
	 */
	@Override
	public boolean isInputValueProvided() {
		return false;
	}

}
