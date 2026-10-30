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
 * @param <T> 结果类型
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
	 * @param ds 用于获取连接的 DataSource
	 * @param sql 要运行的 SQL
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
	 * @param rs 正在处理的 ResultSet
	 * @param rowNum 当前行号（从 0 开始）
	 * @param context 传递给 {@code execute()} 方法的上下文。
	 * 如果不需要上下文信息，则可以为 {@code null}。
	 * 如果需要为每一行传入数据，可以传入一个 HashMap，
	 * 以该行的主键作为 HashMap 的键。
	 * 这样就可以方便地定位每一行的更新
	 * @return 结果类型的对象
	 * @throws SQLException 如果更新数据时出错。
	 * 子类可以简单地不捕获 SQLExceptions，
	 * 并依赖框架进行清理。
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
