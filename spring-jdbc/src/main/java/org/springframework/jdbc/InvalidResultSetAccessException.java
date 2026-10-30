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
import org.springframework.lang.Nullable;

/**
 * 当以无效方式访问ResultSet时抛出的异常。
 * 此类异常总是以{@code java.sql.SQLException}作为根本原因。
 *
 * <p>这通常发生在指定了无效的ResultSet列索引或列名时。
 * 断开连接的SqlRowSet也会抛出此异常。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see BadSqlGrammarException
 * @see org.springframework.jdbc.support.rowset.SqlRowSet
 */
@SuppressWarnings("serial")
public class InvalidResultSetAccessException extends InvalidDataAccessResourceUsageException {

	@Nullable
	private final String sql;


	/**
	 * 创建InvalidResultSetAccessException实例。
	 * @param task 当前任务的名称
	 * @param sql 出错的 SQL 语句
	 * @param ex 根本原因
	 */
	public InvalidResultSetAccessException(String task, String sql, SQLException ex) {
		super(task + "; invalid ResultSet access for SQL [" + sql + "]", ex);
		this.sql = sql;
	}

	/**
	 * 创建InvalidResultSetAccessException实例。
	 * @param ex 根本原因
	 */
	public InvalidResultSetAccessException(SQLException ex) {
		super(ex.getMessage(), ex);
		this.sql = null;
	}


	/**
	 * 返回包装的SQLException。
	 */
	public SQLException getSQLException() {
		return (SQLException) getCause();
	}

	/**
	 * 返回导致问题的SQL语句。
	 * @return 出错的 SQL（如果已知）
	 */
	@Nullable
	public String getSql() {
		return this.sql;
	}

}
