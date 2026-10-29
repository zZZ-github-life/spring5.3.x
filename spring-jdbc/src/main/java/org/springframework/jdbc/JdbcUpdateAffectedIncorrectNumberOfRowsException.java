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

package org.springframework.jdbc;

import org.springframework.dao.IncorrectUpdateSemanticsDataAccessException;

/**
 * 当JDBC更新影响了意外行数时抛出的异常。
 * 通常我们期望更新影响单行，因此如果影响了多行则为错误。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class JdbcUpdateAffectedIncorrectNumberOfRowsException extends IncorrectUpdateSemanticsDataAccessException {

	/** 应该影响的行数。 */
	private final int expected;

	/** 实际影响的行数。 */
	private final int actual;


	/**
	 * 创建JdbcUpdateAffectedIncorrectNumberOfRowsException实例。
	 * @param sql the SQL we were trying to execute
	 * @param expected the expected number of rows affected
	 * @param actual the actual number of rows affected
	 */
	public JdbcUpdateAffectedIncorrectNumberOfRowsException(String sql, int expected, int actual) {
		super("SQL update '" + sql + "' affected " + actual + " rows, not " + expected + " as expected");
		this.expected = expected;
		this.actual = actual;
	}


	/**
	 * 返回应该影响的行数。
	 */
	public int getExpectedRowsAffected() {
		return this.expected;
	}

	/**
	 * 返回实际影响的行数。
	 */
	public int getActualRowsAffected() {
		return this.actual;
	}

	@Override
	public boolean wasDataUpdated() {
		return (getActualRowsAffected() > 0);
	}

}
