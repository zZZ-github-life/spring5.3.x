/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.sql.DataSource;

import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.lang.Nullable;

/**
 * 用于返回单行结果的查询的 SQL“函数”包装器。
 * 默认行为是返回 int，但可以通过带有额外返回类型参数的构造函数
 * 覆盖此行为。
 *
 * <p>适用于使用诸如 "select user()" 或 "select sysdate from dual" 之类的
 * 查询调用返回单个结果的 SQL 函数。它不适用于调用更复杂的存储函数，
 * 也不适用于使用 CallableStatement 调用存储过程或存储函数。此类处理
 * 请使用 StoredProcedure 或 SqlCall。
 *
 * <p>这是一个具体类，通常无需子类化。使用本包的代码可以创建此类型的对象，
 * 声明 SQL 和参数，然后反复调用相应的 {@code run} 方法来执行该函数。
 * 子类只应为特定的参数和返回类型添加专门的 {@code run} 方法。
 *
 * <p>与所有 RdbmsOperation 对象一样，SqlFunction 对象是线程安全的。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @param <T> 结果类型
 * @see StoredProcedure
 */
public class SqlFunction<T> extends MappingSqlQuery<T> {

	private final SingleColumnRowMapper<T> rowMapper = new SingleColumnRowMapper<>();


	/**
	 * 允许作为 JavaBean 使用的构造函数。
	 * 在调用 {@code compile} 方法并使用此对象之前，必须提供
	 * DataSource、SQL 和任何参数。
	 * @see #setDataSource
	 * @see #setSql
	 * @see #compile
	 */
	public SqlFunction() {
		setRowsExpected(1);
	}

	/**
	 * 使用 SQL 创建新的 SqlFunction 对象，但不含参数。
	 * 必须添加参数，或者接受没有参数的情况。
	 * @param ds 从中获取连接的 DataSource
	 * @param sql 要执行的 SQL
	 */
	public SqlFunction(DataSource ds, String sql) {
		setRowsExpected(1);
		setDataSource(ds);
		setSql(sql);
	}

	/**
	 * 使用 SQL 和参数创建新的 SqlFunction 对象。
	 * @param ds 从中获取连接的 DataSource
	 * @param sql 要执行的 SQL
	 * @param types 参数的 SQL 类型，定义于
	 * {@code java.sql.Types} 类中
	 * @see java.sql.Types
	 */
	public SqlFunction(DataSource ds, String sql, int[] types) {
		setRowsExpected(1);
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
	}

	/**
	 * 使用 SQL、参数和结果类型创建新的 SqlFunction 对象。
	 * @param ds 从中获取连接的 DataSource
	 * @param sql 要执行的 SQL
	 * @param types 参数的 SQL 类型，定义于
	 * {@code java.sql.Types} 类中
	 * @param resultType 结果对象必须匹配的类型
	 * @see #setResultType(Class)
	 * @see java.sql.Types
	 */
	public SqlFunction(DataSource ds, String sql, int[] types, Class<T> resultType) {
		setRowsExpected(1);
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
		setResultType(resultType);
	}


	/**
	 * 指定结果对象需要匹配的类型。
	 * <p>如果未指定，结果值将按 JDBC 驱动返回的原样暴露。
	 */
	public void setResultType(Class<T> resultType) {
		this.rowMapper.setRequiredType(resultType);
	}


	/**
	 * 该方法的此实现从函数返回的单行中提取单个值。
	 * 如果返回的行数不同，则视为错误。
	 */
	@Override
	@Nullable
	protected T mapRow(ResultSet rs, int rowNum) throws SQLException {
		return this.rowMapper.mapRow(rs, rowNum);
	}


	/**
	 * 无参数运行该函数的便捷方法。
	 * @return 函数的值
	 */
	public int run() {
		return run(new Object[0]);
	}

	/**
	 * 使用单个 int 参数运行该函数的便捷方法。
	 * @param parameter 单个 int 参数
	 * @return 函数的值
	 */
	public int run(int parameter) {
		return run(new Object[] {parameter});
	}

	/**
	 * 类似于 SqlQuery.execute([]) 方法。这是一个执行查询的通用方法，
	 * 接收若干参数。
	 * @param parameters 参数数组。它们将是对象或基本类型的对象包装类型。
	 * @return 函数的值
	 */
	public int run(Object... parameters) {
		Object obj = super.findObject(parameters);
		if (!(obj instanceof Number)) {
			throw new TypeMismatchDataAccessException("Could not convert result object [" + obj + "] to int");
		}
		return ((Number) obj).intValue();
	}

	/**
	 * 无参数运行该函数的便捷方法，将值作为对象返回。
	 * @return 函数的值
	 */
	@Nullable
	public Object runGeneric() {
		return findObject((Object[]) null, null);
	}

	/**
	 * 使用单个 int 参数运行该函数的便捷方法。
	 * @param parameter 单个 int 参数
	 * @return 以 Object 形式返回的函数值
	 */
	@Nullable
	public Object runGeneric(int parameter) {
		return findObject(parameter);
	}

	/**
	 * 类似于 {@code SqlQuery.findObject(Object[])} 方法。
	 * 这是一个执行查询的通用方法，接收若干参数。
	 * @param parameters 参数数组。它们将是对象或基本类型的对象包装类型。
	 * @return 以 Object 形式返回的函数值
	 * @see #execute(Object[])
	 */
	@Nullable
	public Object runGeneric(Object[] parameters) {
		return findObject(parameters);
	}

}
