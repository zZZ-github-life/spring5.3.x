/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jdbc.support.incrementer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * 用于使用数据库序列的 {@link DataFieldMaxValueIncrementer} 实现的抽象基类。
 * 子类需要提供要使用的数据库特定 SQL。
 *
 * @author Juergen Hoeller
 * @since 26.02.2004
 * @see #getSequenceQuery
 */
public abstract class AbstractSequenceMaxValueIncrementer extends AbstractDataFieldMaxValueIncrementer {

	/**
	 * 用于 bean 属性风格用法的默认构造函数。
	 * @see #setDataSource
	 * @see #setIncrementerName
	 */
	public AbstractSequenceMaxValueIncrementer() {
	}

	/**
	 * 便捷构造函数。
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 */
	public AbstractSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	/**
	 * 执行由 {@link #getSequenceQuery()} 指定的 SQL。
	 */
	@Override
	protected long getNextKey() throws DataAccessException {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
			rs = stmt.executeQuery(getSequenceQuery());
			if (rs.next()) {
				return rs.getLong(1);
			}
			else {
				throw new DataAccessResourceFailureException("Sequence query did not return a result");
			}
		}
		catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain sequence value", ex);
		}
		finally {
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	/**
	 * 返回用于检索序列值的数据库特定查询。
	 * <p>所提供的 SQL 应返回一行一列的结果，
	 * 以便从中提取 {@code long} 值。
	 */
	protected abstract String getSequenceQuery();

}
