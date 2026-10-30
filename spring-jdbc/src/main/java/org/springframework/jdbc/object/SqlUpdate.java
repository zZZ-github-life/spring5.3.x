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

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 表示 SQL 更新的可重用操作对象。
 *
 * <p>本类提供了许多 {@code update} 方法，类似于查询对象的 {@code execute} 方法。
 *
 * <p>本类是具体类。虽然可以子类化（例如添加自定义的更新方法），但通过设置
 * SQL 并声明参数即可轻松实现参数化配置。
 *
 * <p>与 Spring Framework 自带的所有 {@code RdbmsOperation} 类一样，
 * {@code SqlQuery} 实例在初始化完成后是线程安全的。也就是说，在通过
 * setter 方法构造并配置之后，它们可以安全地在多个线程中使用。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see SqlQuery
 */
public class SqlUpdate extends SqlOperation {

	/**
	 * 更新最多可影响的行数。如果影响的行数超过此值，将抛出异常。
	 * 为 0 时忽略。
	 */
	private int maxRowsAffected = 0;

	/**
	 * 必须受影响的确切行数。
	 * 为 0 时忽略。
	 */
	private int requiredRowsAffected = 0;


	/**
	 * 允许作为 JavaBean 使用的构造函数。在编译和使用之前，
	 * 必须提供 DataSource 和 SQL。
	 * @see #setDataSource
	 * @see #setSql
	 */
	public SqlUpdate() {
	}

	/**
	 * 使用给定的 DataSource 和 SQL 构造更新对象。
	 * @param ds 用于获取连接的 DataSource
	 * @param sql 要执行的 SQL 语句
	 */
	public SqlUpdate(DataSource ds, String sql) {
		setDataSource(ds);
		setSql(sql);
	}

	/**
	 * 使用给定的 DataSource、SQL 和匿名参数构造更新对象。
	 * @param ds 用于获取连接的 DataSource
	 * @param sql 要执行的 SQL 语句
	 * @param types 参数的 SQL 类型，定义于
	 * {@code java.sql.Types} 类中
	 * @see java.sql.Types
	 */
	public SqlUpdate(DataSource ds, String sql, int[] types) {
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
	}

	/**
	 * 使用给定的 DataSource、SQL、匿名参数构造更新对象，
	 * 并指定最多可影响的行数。
	 * @param ds 用于获取连接的 DataSource
	 * @param sql 要执行的 SQL 语句
	 * @param types 参数的 SQL 类型，定义于
	 * {@code java.sql.Types} 类中
	 * @param maxRowsAffected 该更新可能影响的最大行数
	 * @see java.sql.Types
	 */
	public SqlUpdate(DataSource ds, String sql, int[] types, int maxRowsAffected) {
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
		this.maxRowsAffected = maxRowsAffected;
	}


	/**
	 * 设置此更新最多可影响的行数。
	 * 默认值为 0，表示不限制受影响的行数。
	 * @param maxRowsAffected 此更新最多可影响的行数，
	 * 超过时该类的 update 方法会将其视为错误
	 */
	public void setMaxRowsAffected(int maxRowsAffected) {
		this.maxRowsAffected = maxRowsAffected;
	}

	/**
	 * 设置此更新必须影响的<i>确切</i>行数。
	 * 默认值为 0，表示允许影响任意数量的行。
	 * <p>这是设置<i>最大</i>可影响行数的替代方案。
	 * @param requiredRowsAffected 此更新必须影响的确切行数，
	 * 否则该类的 update 方法会将其视为错误
	 */
	public void setRequiredRowsAffected(int requiredRowsAffected) {
		this.requiredRowsAffected = requiredRowsAffected;
	}

	/**
	 * 根据指定的最大行数或必需行数检查给定的受影响行数。
	 * @param rowsAffected 受影响的行数
	 * @throws JdbcUpdateAffectedIncorrectNumberOfRowsException 如果实际受影响的行数超出范围
	 * @see #setMaxRowsAffected
	 * @see #setRequiredRowsAffected
	 */
	protected void checkRowsAffected(int rowsAffected) throws JdbcUpdateAffectedIncorrectNumberOfRowsException {
		if (this.maxRowsAffected > 0 && rowsAffected > this.maxRowsAffected) {
			throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(resolveSql(), this.maxRowsAffected, rowsAffected);
		}
		if (this.requiredRowsAffected > 0 && rowsAffected != this.requiredRowsAffected) {
			throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(resolveSql(), this.requiredRowsAffected, rowsAffected);
		}
	}


	/**
	 * 根据参数执行更新的通用方法。
	 * 所有其他 update 方法都会调用此方法。
	 * @param params 参数对象数组
	 * @return 更新影响的行数
	 */
	public int update(Object... params) throws DataAccessException {
		validateParameters(params);
		int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(params));
		checkRowsAffected(rowsAffected);
		return rowsAffected;
	}

	/**
	 * 执行更新并获取参数，通过 KeyHolder 获取生成的键。
	 * @param params 参数对象数组
	 * @param generatedKeyHolder 用于存放生成键的 KeyHolder
	 * @return 更新影响的行数
	 */
	public int update(Object[] params, KeyHolder generatedKeyHolder) throws DataAccessException {
		if (!isReturnGeneratedKeys() && getGeneratedKeysColumnNames() == null) {
			throw new InvalidDataAccessApiUsageException(
					"The update method taking a KeyHolder should only be used when generated keys have " +
					"been configured by calling either 'setReturnGeneratedKeys' or " +
					"'setGeneratedKeysColumnNames'.");
		}
		validateParameters(params);
		int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(params), generatedKeyHolder);
		checkRowsAffected(rowsAffected);
		return rowsAffected;
	}

	/**
	 * 执行无参数更新的便捷方法。
	 */
	public int update() throws DataAccessException {
		return update(new Object[0]);
	}

	/**
	 * 在给定一个 int 参数的情况下执行更新的便捷方法。
	 */
	public int update(int p1) throws DataAccessException {
		return update(new Object[] {p1});
	}

	/**
	 * 在给定两个 int 参数的情况下执行更新的便捷方法。
	 */
	public int update(int p1, int p2) throws DataAccessException {
		return update(new Object[] {p1, p2});
	}

	/**
	 * 在给定一个 long 参数的情况下执行更新的便捷方法。
	 */
	public int update(long p1) throws DataAccessException {
		return update(new Object[] {p1});
	}

	/**
	 * 在给定两个 long 参数的情况下执行更新的便捷方法。
	 */
	public int update(long p1, long p2) throws DataAccessException {
		return update(new Object[] {p1, p2});
	}

	/**
	 * 在给定一个 String 参数的情况下执行更新的便捷方法。
	 */
	public int update(String p) throws DataAccessException {
		return update(new Object[] {p});
	}

	/**
	 * 在给定两个 String 参数的情况下执行更新的便捷方法。
	 */
	public int update(String p1, String p2) throws DataAccessException {
		return update(new Object[] {p1, p2});
	}

	/**
	 * 根据命名参数执行更新的通用方法。
	 * 所有其他 update 方法都会调用此方法。
	 * @param paramMap 参数名到参数对象的 Map，
	 * 与 SQL 语句中指定的命名参数相匹配
	 * @return 更新影响的行数
	 */
	public int updateByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		validateNamedParameters(paramMap);
		ParsedSql parsedSql = getParsedSql();
		MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
		int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(sqlToUse, params));
		checkRowsAffected(rowsAffected);
		return rowsAffected;
	}

	/**
	 * 执行更新并获取参数，通过 KeyHolder 获取生成的键。
	 * @param paramMap 参数名到参数对象的 Map，
	 * 与 SQL 语句中指定的命名参数相匹配
	 * @param generatedKeyHolder 用于存放生成键的 KeyHolder
	 * @return 更新影响的行数
	 */
	public int updateByNamedParam(Map<String, ?> paramMap, KeyHolder generatedKeyHolder) throws DataAccessException {
		validateNamedParameters(paramMap);
		ParsedSql parsedSql = getParsedSql();
		MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
		int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(sqlToUse, params), generatedKeyHolder);
		checkRowsAffected(rowsAffected);
		return rowsAffected;
	}

}
