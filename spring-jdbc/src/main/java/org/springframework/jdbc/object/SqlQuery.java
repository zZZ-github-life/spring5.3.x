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
 * @param <T> the result type
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
	 * @param ds the {@code DataSource} to use to get connections
	 * @param sql the SQL to execute; SQL can also be supplied at runtime
	 * by overriding the {@link #getSql()} method.
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
	 * @param params parameters, similar to JDO query parameters.
	 * Primitive parameters must be represented by their Object wrapper type.
	 * The ordering of parameters is significant.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @return a List of objects, one per row of the ResultSet. Normally all these
	 * will be of the same class, although it is possible to use different types.
	 */
	public List<T> execute(@Nullable Object[] params, @Nullable Map<?, ?> context) throws DataAccessException {
		validateParameters(params);
		RowMapper<T> rowMapper = newRowMapper(params, context);
		return getJdbcTemplate().query(newPreparedStatementCreator(params), rowMapper);
	}

	/**
	 * 不带上下文执行的便捷方法。
	 * @param params parameters for the query. Primitive parameters must
	 * be represented by their Object wrapper type. The ordering of parameters is
	 * significant.
	 */
	public List<T> execute(Object... params) throws DataAccessException {
		return execute(params, null);
	}

	/**
	 * 不带参数执行的便捷方法。
	 * @param context the contextual information for object creation
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
	 * @param p1 single int parameter
	 * @param context the contextual information for object creation
	 */
	public List<T> execute(int p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * 使用单个 int 参数执行的便捷方法。
	 * @param p1 single int parameter
	 */
	public List<T> execute(int p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * 使用两个 int 参数和上下文执行的便捷方法。
	 * @param p1 first int parameter
	 * @param p2 second int parameter
	 * @param context the contextual information for object creation
	 */
	public List<T> execute(int p1, int p2, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1, p2}, context);
	}

	/**
	 * 使用两个 int 参数执行的便捷方法。
	 * @param p1 first int parameter
	 * @param p2 second int parameter
	 */
	public List<T> execute(int p1, int p2) throws DataAccessException {
		return execute(p1, p2, null);
	}

	/**
	 * 使用单个 long 参数和上下文执行的便捷方法。
	 * @param p1 single long parameter
	 * @param context the contextual information for object creation
	 */
	public List<T> execute(long p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * 使用单个 long 参数执行的便捷方法。
	 * @param p1 single long parameter
	 */
	public List<T> execute(long p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * 使用单个 String 参数和上下文执行的便捷方法。
	 * @param p1 single String parameter
	 * @param context the contextual information for object creation
	 */
	public List<T> execute(String p1, @Nullable Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * 使用单个 String 参数执行的便捷方法。
	 * @param p1 single String parameter
	 */
	public List<T> execute(String p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * 核心执行方法。所有命名参数的执行都经过此方法。
	 * @param paramMap parameters associated with the name specified while declaring
	 * the SqlParameters. Primitive parameters must be represented by their Object wrapper
	 * type. The ordering of parameters is not significant since they are supplied in a
	 * SqlParameterMap which is an implementation of the Map interface.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @return a List of objects, one per row of the ResultSet. Normally all these
	 * will be of the same class, although it is possible to use different types.
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
	 * @param paramMap parameters associated with the name specified while declaring
	 * the SqlParameters. Primitive parameters must be represented by their Object wrapper
	 * type. The ordering of parameters is not significant.
	 */
	public List<T> executeByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		return executeByNamedParam(paramMap, null);
	}


	/**
	 * 通用对象查找方法，被所有其他 {@code findObject} 方法使用。
	 * 对象查找方法类似于 EJB 实体 bean 的查找器，如果返回多个结果，
	 * 则被视为错误。
	 * @return the result object, or {@code null} if not found. Subclasses may
	 * choose to treat this as an error and throw an exception.
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
	 * @param paramMap a Map of parameter name to parameter object,
	 * matching named parameters specified in the SQL statement.
	 * Ordering is not significant.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @return a List of objects, one per row of the ResultSet. Normally all these
	 * will be of the same class, although it is possible to use different types.
	 */
	@Nullable
	public T findObjectByNamedParam(Map<String, ?> paramMap, @Nullable Map<?, ?> context) throws DataAccessException {
		List<T> results = executeByNamedParam(paramMap, context);
		return DataAccessUtils.singleResult(results);
	}

	/**
	 * 不带上下文执行的便捷方法。
	 * @param paramMap a Map of parameter name to parameter object,
	 * matching named parameters specified in the SQL statement.
	 * Ordering is not significant.
	 */
	@Nullable
	public T findObjectByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		return findObjectByNamedParam(paramMap, null);
	}


	/**
	 * 子类必须实现此方法以提取每行的对象，由 {@code execute} 方法以聚合的
	 * {@link List} 形式返回。
	 * @param parameters the parameters to the {@code execute()} method,
	 * in case subclass is interested; may be {@code null} if there
	 * were no parameters.
	 * @param context the contextual information passed to the {@code mapRow}
	 * callback method. The JDBC operation itself doesn't rely on this parameter,
	 * but it can be useful for creating the objects of the result list.
	 * @see #execute
	 */
	protected abstract RowMapper<T> newRowMapper(@Nullable Object[] parameters, @Nullable Map<?, ?> context);

}
