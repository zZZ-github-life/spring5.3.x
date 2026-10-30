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
 * 可重用的 RDBMS 查询，具体子类必须实现抽象的 mapRow(ResultSet, int) 方法，
 * 将 JDBC ResultSet 的每一行映射为对象。
 *
 * <p>在非简单的场景下，这种手动映射通常优于使用反射的“自动”映射，
 * 因为后者可能变得十分复杂。例如，当前类允许为不同的行使用不同的对象
 * （比如通过子类指示时），允许设置计算字段，并且无需让 ResultSet 的列名
 * 与 bean 属性名保持一致。这正是帕累托原则的体现：为进一步自动化提取过程
 * 而付出额外努力，会让框架复杂得多，却几乎没有实际收益。
 *
 * <p>子类在构造时可以传入 SQL、参数类型和 DataSource。SQL 往往因
 * 子类而异。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Jean-Pierre Pawlak
 * @param <T> 结果类型
 * @see org.springframework.jdbc.object.MappingSqlQuery
 * @see org.springframework.jdbc.object.SqlQuery
 */
public abstract class MappingSqlQueryWithParameters<T> extends SqlQuery<T> {

	/**
	 * 允许作为 JavaBean 使用的构造函数。
	 */
	public MappingSqlQueryWithParameters() {
	}

	/**
	 * 使用 DataSource 和 SQL 字符串的便捷构造函数。
	 * @param ds 用于获取连接的 DataSource
	 * @param sql 要运行的 SQL
	 */
	public MappingSqlQueryWithParameters(DataSource ds, String sql) {
		super(ds, sql);
	}


	/**
	 * 受保护抽象方法的实现。这会调用子类的 mapRow() 方法实现。
	 */
	@Override
	protected RowMapper<T> newRowMapper(@Nullable Object[] parameters, @Nullable Map<?, ?> context) {
		return new RowMapperImpl(parameters, context);
	}

	/**
	 * 子类必须实现此方法，将 ResultSet 的每一行转换为结果类型的对象。
	 * @param rs 正在处理的 ResultSet
	 * @param rowNum 当前行号（从 0 开始）
	 * @param parameters 传递给查询（传入 execute() 方法）的参数。
	 * 子类很少关注这些参数。
	 * 如果没有参数，则可以为 {@code null}。
	 * @param context 传递给 execute() 方法的上下文。
	 * 如果不需要上下文信息，则可以为 {@code null}。
	 * @return 结果类型的对象
	 * @throws SQLException 如果提取数据时出错。
	 * 子类可以简单地不捕获 SQLExceptions，
	 * 并依赖框架进行清理。
	 */
	@Nullable
	protected abstract T mapRow(ResultSet rs, int rowNum, @Nullable Object[] parameters, @Nullable Map<?, ?> context)
			throws SQLException;


	/**
	 * RowMapper 的实现，对每一行调用外部类的 {@code mapRow} 方法。
	 */
	protected class RowMapperImpl implements RowMapper<T> {

		@Nullable
		private final Object[] params;

		@Nullable
		private final Map<?, ?> context;

		/**
		 * 使用数组结果。如果已知预期结果的数量，这种方式效率更高。
		 */
		public RowMapperImpl(@Nullable Object[] parameters, @Nullable Map<?, ?> context) {
			this.params = parameters;
			this.context = context;
		}

		@Override
		@Nullable
		public T mapRow(ResultSet rs, int rowNum) throws SQLException {
			return MappingSqlQueryWithParameters.this.mapRow(rs, rowNum, this.params, this.context);
		}
	}

}
