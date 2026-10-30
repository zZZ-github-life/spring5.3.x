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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.lang.Nullable;

/**
 * 表示 SQL 查询的可重用操作对象。
 *
 * <p>子类必须实现 {@link #newRowMapper} 方法，提供一个能够提取遍历
 * 查询执行期间创建的 {@code ResultSet} 结果的对象。
 *
 * <p>本类提供了许多公共的 {@code execute} 方法，类似于各种便捷的 JDO
 * 查询执行方法。子类既可以依赖这些继承的方法之一，也可以添加自己的
 * 自定义执行方法，并使用有意义的名称和类型化的参数（这绝对是最佳实践）。
 * 每个自定义查询方法都会调用本类的某个无类型查询方法。
 *
 * <p>与 Spring Framework 自带的所有 {@code RdbmsOperation} 类一样，
 * {@code SqlQuery} 实例在初始化完成后是线程安全的。也就是说，在通过
 * setter 方法构造并配置之后，它们可以安全地在多个线程中使用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @param <T> 结果类型
 * @see SqlUpdate
 */
public abstract class SqlQuery<T> extends SqlOperation {

	/** 预期行数；如果为 0，则表示未知。 */
	private int rowsExpected = 0;


	/**
	 * 允许作为 JavaBean 使用的构造函数。
	 * <p>在编译和使用之前，必须提供 {@code DataSource} 和 SQL。
	 */
	public SqlQuery() {
	}

	/**
	 * 使用 {@code DataSource} 和 SQL 字符串的便捷构造函数。
	 * @param ds 用于获取连接的 {@code DataSource}
	 * @param sql 要执行的 SQL；也可以在运行时通过重写
	 * {@link #getSql()} 方法提供 SQL。
	 */
	public SqlQuery(DataSource ds, String sql) {
		setDataSource(ds);
		setSql(sql);
	}


	/**
	 * 设置预期行数。
	 * <p>这可用于确保结果的高效存储。默认行为是不期望任何特定的行数。
	 */
	public void setRowsExpected(int rowsExpected) {
		this.rowsExpected = rowsExpected;
	}

	/**
	 * 获取预期行数。
	 */
	public int getRowsExpected() {
		return this.rowsExpected;
	}


	/**
	 * 核心执行方法。所有未命名参数的执行都经过此方法。
	 * @param params 参数，类似于 JDO 查询参数。
	 * 基本类型参数必须用其 Object 包装类型表示。
	 * 参数的顺序很重要。
	 * @param context 传递给 {@code mapRow} 回调方法的上下文信息。
	 * JDBC 操作本身不依赖此参数，
	 * 但它在创建结果列表的对象时可能有用。
	 * @return 对象列表，ResultSet 的每一行对应一个对象。
	 * 通常所有这些对象都属于同一个类，
	 * 不过也可以使用不同的类型。
	 */
	public List<T> execute(@Nullable Object[] params, @Nullable Map<?, ?> context) throws DataAccessException {
		validateParameters(params);
		RowMapper<T> rowMapper = newRowMapper(params, context);
		return getJdbcTemplate().query(newPreparedStatementCreator(params), rowMapper);
	}

	/**
	 * 不带上下文执行的便捷方法。
	 * @param params 查询的参数。基本类型参数必须用其 Object 包装类型表示。
	 * 参数的顺序很重要。
	 */
	public List<T> execute(Object... params) throws DataAccessException {
		return execute(params, null);
	}

	/**
	 * 不带参数执行的便捷方法。
	 * @param context 用于创建对象的上下文信息
	 */
	public List<T> execute(Map<?, ?> context) throws DataAccessException {
		return execute((Object[]) null, context);
	}

	/**
	 * 既不带参数也不带上下文执行的便捷方法。
	 */
	public List<T> execute() throws DataAccessException {
		return execute((Object[]) null, null);
	}

	/**
	 * 使用单个 int 参数和上下文执行的便捷方法。
	 * @param p1 单个 int 参数
	 * @param context 用于创建对象的上下文信息
	 */
	public List<T> execute(int p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * 使用单个 int 参数执行的便捷方法。
	 * @param p1 单个 int 参数
	 */
	public List<T> execute(int p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * 使用两个 int 参数和上下文执行的便捷方法。
	 * @param p1 第一个 int 参数
	 * @param p2 第二个 int 参数
	 * @param context 用于创建对象的上下文信息
	 */
	public List<T> execute(int p1, int p2, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1, p2}, context);
	}

	/**
	 * 使用两个 int 参数执行的便捷方法。
	 * @param p1 第一个 int 参数
	 * @param p2 第二个 int 参数
	 */
	public List<T> execute(int p1, int p2) throws DataAccessException {
		return execute(p1, p2, null);
	}

	/**
	 * 使用单个 long 参数和上下文执行的便捷方法。
	 * @param p1 单个 long 参数
	 * @param context 用于创建对象的上下文信息
	 */
	public List<T> execute(long p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * 使用单个 long 参数执行的便捷方法。
	 * @param p1 单个 long 参数
	 */
	public List<T> execute(long p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * 使用单个 String 参数和上下文执行的便捷方法。
	 * @param p1 单个 String 参数
	 * @param context 用于创建对象的上下文信息
	 */
	public List<T> execute(String p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * 使用单个 String 参数执行的便捷方法。
	 * @param p1 单个 String 参数
	 */
	public List<T> execute(String p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * 核心执行方法。所有命名参数的执行都经过此方法。
	 * @param paramMap 与声明 SqlParameters 时指定的名称相关联的参数。
	 * 基本类型参数必须用其 Object 包装类型表示。
	 * 参数的顺序不重要，因为它们是在
	 * SqlParameterMap（Map 接口的一个实现）中提供的。
	 * @param context 传递给 {@code mapRow} 回调方法的上下文信息。
	 * JDBC 操作本身不依赖此参数，
	 * 但它在创建结果列表的对象时可能有用。
	 * @return 对象列表，ResultSet 的每一行对应一个对象。
	 * 通常所有这些对象都属于同一个类，
	 * 不过也可以使用不同的类型。
	 */
	public List<T> executeByNamedParam(Map<String, ?> paramMap, @Nullable Map<?, ?> context) throws DataAccessException {
		validateNamedParameters(paramMap);
		ParsedSql parsedSql = getParsedSql();
		MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
		RowMapper<T> rowMapper = newRowMapper(params, context);
		return getJdbcTemplate().query(newPreparedStatementCreator(sqlToUse, params), rowMapper);
	}

	/**
	 * 不带上下文执行的便捷方法。
	 * @param paramMap 与声明 SqlParameters 时指定的名称相关联的参数。
	 * 基本类型参数必须用其 Object 包装类型表示。
	 * 参数的顺序不重要。
	 */
	public List<T> executeByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		return executeByNamedParam(paramMap, null);
	}


	/**
	 * 通用对象查找方法，被所有其他 {@code findObject} 方法使用。
	 * 对象查找方法类似于 EJB 实体 bean 的查找器，如果返回多个结果，
	 * 则被视为错误。
	 * @return 结果对象，如果未找到则为 {@code null}。
	 * 子类可以选择将其视为错误并抛出异常。
	 * @see org.springframework.dao.support.DataAccessUtils#singleResult
	 */
	@Nullable
	public T findObject(@Nullable Object[] params, @Nullable Map<?, ?> context) throws DataAccessException {
		List<T> results = execute(params, context);
		return DataAccessUtils.singleResult(results);
	}

	/**
	 * 不带上下文查找单个对象的便捷方法。
	 */
	@Nullable
	public T findObject(Object... params) throws DataAccessException {
		return findObject(params, null);
	}

	/**
	 * 在给定单个 int 参数和上下文的情况下查找单个对象的便捷方法。
	 */
	@Nullable
	public T findObject(int p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1}, context);
	}

	/**
	 * 在给定单个 int 参数的情况下查找单个对象的便捷方法。
	 */
	@Nullable
	public T findObject(int p1) throws DataAccessException {
		return findObject(p1, null);
	}

	/**
	 * 在给定两个 int 参数和上下文的情况下查找单个对象的便捷方法。
	 */
	@Nullable
	public T findObject(int p1, int p2, @Nullable Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1, p2}, context);
	}

	/**
	 * 在给定两个 int 参数的情况下查找单个对象的便捷方法。
	 */
	@Nullable
	public T findObject(int p1, int p2) throws DataAccessException {
		return findObject(p1, p2, null);
	}

	/**
	 * 在给定单个 long 参数和上下文的情况下查找单个对象的便捷方法。
	 */
	@Nullable
	public T findObject(long p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1}, context);
	}

	/**
	 * 在给定单个 long 参数的情况下查找单个对象的便捷方法。
	 */
	@Nullable
	public T findObject(long p1) throws DataAccessException {
		return findObject(p1, null);
	}

	/**
	 * 在给定单个 String 参数和上下文的情况下查找单个对象的便捷方法。
	 */
	@Nullable
	public T findObject(String p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1}, context);
	}

	/**
	 * 在给定单个 String 参数的情况下查找单个对象的便捷方法。
	 */
	@Nullable
	public T findObject(String p1) throws DataAccessException {
		return findObject(p1, null);
	}

	/**
	 * 针对命名参数的通用对象查找方法。
	 * @param paramMap 参数名到参数对象的 Map，
	 * 与 SQL 语句中指定的命名参数相匹配。排序不重要。
	 * @param context 传递给 {@code mapRow} 回调方法的上下文信息。
	 * JDBC 操作本身不依赖此参数，
	 * 但它在创建结果列表的对象时可能有用。
	 * @return 对象列表，ResultSet 的每一行对应一个对象。
	 * 通常所有这些对象都属于同一个类，
	 * 不过也可以使用不同的类型。
	 */
	@Nullable
	public T findObjectByNamedParam(Map<String, ?> paramMap, @Nullable Map<?, ?> context) throws DataAccessException {
		List<T> results = executeByNamedParam(paramMap, context);
		return DataAccessUtils.singleResult(results);
	}

	/**
	 * 不带上下文执行的便捷方法。
	 * @param paramMap 参数名到参数对象的 Map，
	 * 与 SQL 语句中指定的命名参数相匹配。排序不重要。
	 */
	@Nullable
	public T findObjectByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		return findObjectByNamedParam(paramMap, null);
	}


	/**
	 * 子类必须实现此方法以提取每行的对象，由 {@code execute} 方法以聚合的
	 * {@link List} 形式返回。
	 * @param parameters 传递给 {@code execute()} 方法的参数，以备子类需要；
	 * 如果没有参数，则可以为 {@code null}。
	 * @param context 传递给 {@code mapRow} 回调方法的上下文信息。
	 * JDBC 操作本身不依赖此参数，
	 * 但它在创建结果列表的对象时可能有用。
	 * @see #execute
	 */
	protected abstract RowMapper<T> newRowMapper(@Nullable Object[] parameters, @Nullable Map<?, ?> context);

}
