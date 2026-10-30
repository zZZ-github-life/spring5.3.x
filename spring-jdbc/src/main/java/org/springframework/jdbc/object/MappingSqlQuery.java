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

import org.springframework.lang.Nullable;

/**
 * 可重用的查询，具体子类必须实现抽象的 mapRow(ResultSet, int) 方法，
 * 将 JDBC ResultSet 的每一行转换为对象。
 *
 * <p>通过去除参数和上下文简化了 MappingSqlQueryWithParameters 的 API。
 * 大多数子类并不关心参数。如果不使用上下文信息，请继承本类而不是
 * MappingSqlQueryWithParameters。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Jean-Pierre Pawlak
 * @param <T> 结果类型
 * @see MappingSqlQueryWithParameters
 */
public abstract class MappingSqlQuery<T> extends MappingSqlQueryWithParameters<T> {

	/**
	 * 允许作为 JavaBean 使用的构造函数。
	 */
	public MappingSqlQuery() {
	}

	/**
	 * 使用 DataSource 和 SQL 字符串的便捷构造函数。
	 * @param ds 用于获取连接的 DataSource
	 * @param sql 要运行的 SQL
	 */
	public MappingSqlQuery(DataSource ds, String sql) {
		super(ds, sql);
	}


	/**
	 * 实现此方法以调用更简单的 mapRow 模板方法，并忽略参数。
	 * @see #mapRow(ResultSet, int)
	 */
	@Override
	@Nullable
	protected final T mapRow(ResultSet rs, int rowNum, @Nullable Object[] parameters, @Nullable Map<?, ?> context)
			throws SQLException {

		return mapRow(rs, rowNum);
	}

	/**
	 * 子类必须实现此方法，将 ResultSet 的每一行转换为结果类型的对象。
	 * <p>与 MappingSqlQueryWithParameters 的直接子类不同，本类的子类
	 * 无需关心查询对象 execute 方法的参数。
	 * @param rs 正在处理的 ResultSet
	 * @param rowNum 当前行号（从 0 开始）
	 * @return 结果类型的对象
	 * @throws SQLException 如果提取数据时出错。
	 * 子类可以简单地不捕获 SQLExceptions，
	 * 并依赖框架进行清理。
	 */
	@Nullable
	protected abstract T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
