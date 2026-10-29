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

/**
 * 表示存储过程调用返回的{@link java.sql.ResultSet}。
 *
 * <p>必须提供{@link ResultSetExtractor}、{@link RowCallbackHandler}或{@link RowMapper}
 * 来处理任何返回的行。
 *
 * <p>返回的{@link java.sql.ResultSet ResultSet} - 与所有存储过程参数一样 - 必须有名称。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class SqlReturnResultSet extends ResultSetSupportingSqlParameter {

	/**
	 * 创建{@link SqlReturnResultSet}类的新实例。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param extractor the {@link ResultSetExtractor} to use for parsing the {@link java.sql.ResultSet}
	 */
	public SqlReturnResultSet(String name, ResultSetExtractor<?> extractor) {
		super(name, 0, extractor);
	}

	/**
	 * 创建{@link SqlReturnResultSet}类的新实例。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param handler the {@link RowCallbackHandler} to use for parsing the {@link java.sql.ResultSet}
	 */
	public SqlReturnResultSet(String name, RowCallbackHandler handler) {
		super(name, 0, handler);
	}

	/**
	 * 创建{@link SqlReturnResultSet}类的新实例。
	 * @param name the name of the parameter, as used in input and output maps
	 * @param mapper the {@link RowMapper} to use for parsing the {@link java.sql.ResultSet}
	 */
	public SqlReturnResultSet(String name, RowMapper<?> mapper) {
		super(name, 0, mapper);
	}


	/**
	 * 此实现始终返回{@code true}。
	 */
	@Override
	public boolean isResultsParameter() {
		return true;
	}

}
