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

import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.lang.Nullable;

/**
 * 当无法将SQLException归类为我们的通用数据访问异常之一时抛出的异常。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class UncategorizedSQLException extends UncategorizedDataAccessException {

	/** 导致问题的SQL语句。 */
	@Nullable
	private final String sql;


	/**
	 * 创建UncategorizedSQLException实例。
	 * @param task name of current task
	 * @param sql the offending SQL statement
	 * @param ex the root cause
	 */
	public UncategorizedSQLException(String task, @Nullable String sql, SQLException ex) {
		super(task + "; uncategorized SQLException" + (sql != null ? " for SQL [" + sql + "]" : "") +
				"; SQL state [" + ex.getSQLState() + "]; error code [" + ex.getErrorCode() + "]; " +
				ex.getMessage(), ex);
		this.sql = sql;
	}


	/**
	 * 返回底层的SQLException。
	 */
	public SQLException getSQLException() {
		return (SQLException) getCause();
	}

	/**
	 * 返回导致问题的SQL语句（如果已知）。
	 */
	@Nullable
	public String getSql() {
		return this.sql;
	}

}
