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

package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;

/**
 * 可重用的 RDBMS 查询，具体子类必须实现抽象的
 * updateRow(ResultSet, int, context) 方法，以更新 JDBC ResultSet 的每一行，
 * 并可选地将内容映射到对象中。
 *
 * <p>子类在构造时可以传入 SQL、参数类型和 DataSource。SQL 往往因
 * 子类而异。
 *
 * @author Thomas Risberg
 * @param <T> the result type
 * @see org.springframework.jdbc.object.SqlQuery
 */
public abstract class UpdatableSqlQuery<T> extends SqlQuery<T> {

	/**
	 * 允许作为 JavaBean 使用的构造函数。
	 */
	public UpdatableSqlQuery() {
		setUpdatableResults(true);
	}

	/**
	 * 使用 DataSource 和 SQL 字符串的便捷构造函数。
	 * @param ds the DataSource to use to get connections
	 * @param sql the SQL to run
	 */
	public UpdatableSqlQuery(DataSource ds, String sql) {
		super(ds, sql);
		setUpdatableResults(true);
	}


	/**
	 * 父类模板方法的实现。这会调用子类的 {@code updateRow()} 方法实现。
	 */
	@Override
	protected RowMapper<T> newRowMapper(@Nullable Object[] parameters, @Nullable Map<?, ?> context) {
		return new RowMapperImpl(context);
	}

	/**
	 * 子类必须实现此方法以更新 ResultSet 的每一行，并可选地创建结果类型的对象。
	 * @param rs the ResultSet we're working through
	 * @param rowNum row number (from 0) we're up to
	 * @param context passed to the {@code execute()} method.
	 * It can be {@code null} if no contextual information is need.  If you
	 * need to pass in data for each row, you can pass in a HashMap with
	 * the primary key of the row being the key for the HashMap.  That way
	 * it is easy to locate the updates for each row
	 * @return an object of the result type
	 * @throws SQLException if there's an error updating data.
	 * Subclasses can simply not catch SQLExceptions, relying on the
	 * framework to clean up.
	 */
	protected abstract T updateRow(ResultSet rs, int rowNum, @Nullable Map<?, ?> context) throws SQLException;


	/**
	 * RowMapper 的实现，对每一行调用外部类的 {@code updateRow()} 方法。
	 */
	protected class RowMapperImpl implements RowMapper<T> {

		@Nullable
		private final Map<?, ?> context;

		public RowMapperImpl(@Nullable Map<?, ?> context) {
			this.context = context;
		}

		@Override
		public T mapRow(ResultSet rs, int rowNum) throws SQLException {
			T result = updateRow(rs, rowNum, this.context);
			rs.updateRow();
			return result;
		}
	}

}
