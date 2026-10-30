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
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * {@link ResultSetExtractor}实现，为每个给定的{@link ResultSet}返回Spring {@link SqlRowSet}表示。
 *
 * <p>默认实现在底层使用标准JDBC CachedRowSet。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #newCachedRowSet
 * @see org.springframework.jdbc.support.rowset.SqlRowSet
 * @see JdbcTemplate#queryForRowSet(String)
 * @see javax.sql.rowset.CachedRowSet
 */
public class SqlRowSetResultSetExtractor implements ResultSetExtractor<SqlRowSet> {

	private static final RowSetFactory rowSetFactory;

	static {
		try {
			rowSetFactory = RowSetProvider.newFactory();
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Cannot create RowSetFactory through RowSetProvider", ex);
		}
	}


	@Override
	public SqlRowSet extractData(ResultSet rs) throws SQLException {
		return createSqlRowSet(rs);
	}

	/**
	 * 创建一个{@link SqlRowSet}，包装给定的{@link ResultSet}，
	 * 以断开连接的方式表示其数据。
	 * <p>此实现创建一个Spring {@link ResultSetWrappingSqlRowSet}实例，
	 * 包装标准JDBC {@link CachedRowSet}实例。可以重写以使用不同的实现。
	 * @param rs 原始 ResultSet（已连接）
	 * @return 已断开的 SqlRowSet
	 * @throws SQLException 如果由 JDBC 方法抛出
	 * @see #newCachedRowSet()
	 * @see org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet
	 */
	protected SqlRowSet createSqlRowSet(ResultSet rs) throws SQLException {
		CachedRowSet rowSet = newCachedRowSet();
		rowSet.populate(rs);
		return new ResultSetWrappingSqlRowSet(rowSet);
	}

	/**
	 * 创建新的{@link CachedRowSet}实例，由{@code createSqlRowSet}实现填充。
	 * <p>默认实现使用JDBC 4.1的{@link RowSetFactory}。
	 * @return 新的 CachedRowSet 实例
	 * @throws SQLException 如果由 JDBC 方法抛出
	 * @see #createSqlRowSet
	 * @see RowSetProvider#newFactory()
	 * @see RowSetFactory#createCachedRowSet()
	 */
	protected CachedRowSet newCachedRowSet() throws SQLException {
		return rowSetFactory.createCachedRowSet();
	}

}
