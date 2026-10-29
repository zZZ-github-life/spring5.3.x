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

import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * 当指定的SQL无效时抛出此异常。此类异常总是以{@code java.sql.SQLException}作为根本原因。
 *
 * <p>可以为不存在表、不存在列等情况创建子类。
 * 自定义的SQLExceptionTranslator可以创建此类更具体的异常，
 * 而不会影响使用此类的代码。
 *
 * @author Rod Johnson
 * @see InvalidResultSetAccessException
 */
@SuppressWarnings("serial")
public class BadSqlGrammarException extends InvalidDataAccessResourceUsageException {

	private final String sql;


	/**
	 * 创建BadSqlGrammarException实例。
	 * @param task name of current task
	 * @param sql the offending SQL statement
	 * @param ex the root cause
	 */
	public BadSqlGrammarException(String task, String sql, SQLException ex) {
		super(task + "; bad SQL grammar [" + sql + "]", ex);
		this.sql = sql;
	}


	/**
	 * 返回包装的SQLException。
	 */
	public SQLException getSQLException() {
		return (SQLException) getCause();
	}

	/**
	 * 返回导致问题的SQL语句。
	 */
	public String getSql() {
		return this.sql;
	}

}
