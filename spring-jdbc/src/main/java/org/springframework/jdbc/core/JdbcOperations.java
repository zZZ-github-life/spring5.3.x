/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.lang.Nullable;

/**
 * 指定一组基本 JDBC 操作的接口。
 *
 * <p>由 {@link JdbcTemplate} 实现。不常直接使用，
 * 但它是一个增强可测试性的有用选项，因为它可以很容易地被 mock 或 stub。
 *
 * <p>或者，也可以 mock 标准的 JDBC 基础设施。
 * 不过，mock 此接口的工作量要小得多。
 * 作为用 mock 对象测试数据访问代码的替代方案，
 * 可以考虑使用 {@code spring-test} 构件中
 * <em>Spring TestContext Framework</em> 提供的强大集成测试支持。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JdbcTemplate
 */
public interface JdbcOperations {

	//-------------------------------------------------------------------------
	// Methods dealing with a plain java.sql.Connection
	//-------------------------------------------------------------------------

	/**
	 * 执行 JDBC 数据访问操作，以在 JDBC Connection 上工作的回调动作形式实现。
	 * 这允许在 Spring 管理的 JDBC 环境中实现任意数据访问操作：
	 * 即参与 Spring 管理的事务，并将 JDBC SQLException 转换为
	 * Spring 的 DataAccessException 层次结构。
	 * <p>回调动作可以返回结果对象，例如一个领域对象或一组领域对象。
	 * @param action a callback object that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(ConnectionCallback<T> action) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Methods dealing with static SQL (java.sql.Statement)
	//-------------------------------------------------------------------------

	/**
	 * 执行 JDBC 数据访问操作，以在 JDBC Statement 上工作的回调动作形式实现。
	 * 这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现任意
	 * 数据访问操作：即参与 Spring 管理的事务，并将 JDBC SQLException
	 * 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调动作可以返回结果对象，例如一个领域对象或一组领域对象。
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(StatementCallback<T> action) throws DataAccessException;

	/**
	 * 执行单条 SQL execute，通常是 DDL 语句。
	 * @param sql static SQL to execute
	 * @throws DataAccessException if there is any problem
	 */
	void execute(String sql) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，使用 ResultSetExtractor 读取 ResultSet。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null} 作为参数数组的
	 * 重载 {@code query} 方法。
	 * @param sql the SQL query to execute
	 * @param rse a callback that will extract all rows of results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #query(String, ResultSetExtractor, Object...)
	 */
	@Nullable
	<T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，使用 RowCallbackHandler 逐行读取 ResultSet。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null} 作为参数数组的
	 * 重载 {@code query} 方法。
	 * @param sql the SQL query to execute
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #query(String, RowCallbackHandler, Object...)
	 */
	void query(String sql, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，通过 RowMapper 将每一行映射为结果对象。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null} 作为参数数组的
	 * 重载 {@code query} 方法。
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #query(String, RowMapper, Object...)
	 */
	<T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，通过 RowMapper 将每一行映射为结果对象，
	 * 并将其转换为可迭代且可关闭的 Stream。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null} 作为参数数组的
	 * 重载 {@code query} 方法。
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (e.g. through a try-with-resources clause)
	 * @throws DataAccessException if there is any problem executing the query
	 * @since 5.3
	 * @see #queryForStream(String, RowMapper, Object...)
	 */
	<T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，通过 RowMapper 将单个结果行映射为
	 * 结果对象。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null} 作为参数数组的
	 * 重载 {@link #queryForObject(String, RowMapper, Object...)} 方法。
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code} null)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForObject(String, RowMapper, Object...)
	 */
	@Nullable
	<T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，返回一个结果对象。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null} 作为参数数组的
	 * 重载 {@link #queryForObject(String, Class, Object...)} 方法。
	 * <p>此方法适用于运行具有已知结果的静态 SQL。
	 * 该查询应为单行/单列查询；返回的结果将直接映射到相应的对象类型。
	 * @param sql the SQL query to execute
	 * @param requiredType the type that the result object is expected to match
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForObject(String, Class, Object...)
	 */
	@Nullable
	<T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，返回一个结果 Map。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null}
	 * 作为参数数组的重载 {@link #queryForMap(String, Object...)} 方法。
	 * <p>该查询应为单行查询；结果行将映射到 Map
	 * （每列一个条目，使用列名作为键）。
	 * @param sql the SQL query to execute
	 * @return the result Map (one entry per column, with column name as key)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForMap(String, Object...)
	 * @see ColumnMapRowMapper
	 */
	Map<String, Object> queryForMap(String sql) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，返回一个结果列表。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null} 作为参数数组的
	 * 重载 {@code queryForList} 方法。
	 * <p>结果将映射为结果对象的 List（每行一个条目），
	 * 每个结果对象都与指定的元素类型匹配。
	 * @param sql the SQL query to execute
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForList(String, Class, Object...)
	 * @see SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，返回一个结果列表。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null} 作为参数数组的
	 * 重载 {@code queryForList} 方法。
	 * <p>结果将映射为 Map 的 List（每行一个条目）
	 * （每列一个条目，使用列名作为键）。
	 * 列表中的每个元素将采用此接口的 {@code queryForMap} 方法所返回的形式。
	 * @param sql the SQL query to execute
	 * @return a List that contains a Map per row
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForList(String, Object...)
	 */
	List<Map<String, Object>> queryForList(String sql) throws DataAccessException;

	/**
	 * 执行给定静态 SQL 的查询，返回一个 SqlRowSet。
	 * <p>使用 JDBC Statement，而不是 PreparedStatement。如果你想使用
	 * PreparedStatement 执行静态查询，请使用以 {@code null} 作为参数数组的
	 * 重载 {@code queryForRowSet} 方法。
	 * <p>结果将映射到一个以断开连接方式保存数据的 SqlRowSet。
	 * 此包装器会转换任何抛出的 SQLException。
	 * <p>请注意，对于默认实现，运行时需要 JDBC RowSet 支持：
	 * 默认使用 Sun 的 {@code com.sun.rowset.CachedRowSetImpl} 类，
	 * 它是 JDK 1.5+ 的一部分，也可以从 Sun 的 JDBC RowSet Implementations
	 * 下载包（rowset.jar）中单独获取。
	 * @param sql the SQL query to execute
	 * @return an SqlRowSet representation (possibly a wrapper around a
	 * {@code javax.sql.rowset.CachedRowSet})
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForRowSet(String, Object...)
	 * @see SqlRowSetResultSetExtractor
	 * @see javax.sql.rowset.CachedRowSet
	 */
	SqlRowSet queryForRowSet(String sql) throws DataAccessException;

	/**
	 * 执行单条 SQL 更新操作（例如 insert、update 或 delete 语句）。
	 * @param sql static SQL to execute
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem.
	 */
	int update(String sql) throws DataAccessException;

	/**
	 * 使用批处理在单个 JDBC Statement 上执行多条 SQL 更新。
	 * <p>如果 JDBC 驱动不支持批量更新，将回退为在单个 Statement 上
	 * 分别执行更新。
	 * @param sql defining an array of SQL statements that will be executed.
	 * @return an array of the number of rows affected by each statement
	 * @throws DataAccessException if there is any problem executing the batch
	 */
	int[] batchUpdate(String... sql) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Methods dealing with prepared statements
	//-------------------------------------------------------------------------

	/**
	 * 执行 JDBC 数据访问操作，以在 JDBC PreparedStatement 上工作的回调动作
	 * 形式实现。这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现
	 * 任意数据访问操作：即参与 Spring 管理的事务，并将 JDBC SQLException
	 * 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调动作可以返回结果对象，例如一个领域对象或一组领域对象。
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) throws DataAccessException;

	/**
	 * 执行 JDBC 数据访问操作，以在 JDBC PreparedStatement 上工作的回调动作
	 * 形式实现。这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现
	 * 任意数据访问操作：即参与 Spring 管理的事务，并将 JDBC SQLException
	 * 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调动作可以返回结果对象，例如一个领域对象或一组领域对象。
	 * @param sql the SQL to execute
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException;

	/**
	 * 使用预编译语句进行查询，使用 ResultSetExtractor 读取 ResultSet。
	 * <p>PreparedStatementCreator 可以直接实现，
	 * 也可以通过 PreparedStatementCreatorFactory 配置。
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param rse a callback that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if there is any problem
	 * @see PreparedStatementCreatorFactory
	 */
	@Nullable
	<T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 使用预编译语句进行查询，使用 ResultSetExtractor 读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * Even if there are no bind parameters, this callback may be used to set the
	 * fetch size and other performance options.
	 * @param rse a callback that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T query(String sql, @Nullable PreparedStatementSetter pss, ResultSetExtractor<T> rse)
			throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 使用 ResultSetExtractor 读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param rse a callback that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if the query fails
	 * @see java.sql.Types
	 */
	@Nullable
	<T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 使用 ResultSetExtractor 读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param rse a callback that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if the query fails
	 * @deprecated as of 5.3, in favor of {@link #query(String, ResultSetExtractor, Object...)}
	 */
	@Deprecated
	@Nullable
	<T> T query(String sql, @Nullable Object[] args, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 使用 ResultSetExtractor 读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param rse a callback that will extract results
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 */
	@Nullable
	<T> T query(String sql, ResultSetExtractor<T> rse, @Nullable Object... args) throws DataAccessException;

	/**
	 * 使用预编译语句进行查询，使用 RowCallbackHandler 逐行读取 ResultSet。
	 * <p>PreparedStatementCreator 可以直接实现，
	 * 也可以通过 PreparedStatementCreatorFactory 配置。
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if there is any problem
	 * @see PreparedStatementCreatorFactory
	 */
	void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及知道如何将值绑定到查询的
	 * PreparedStatementSetter 实现进行查询，使用 RowCallbackHandler 逐行读取
	 * ResultSet。
	 * @param sql the SQL query to execute
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * Even if there are no bind parameters, this callback may be used to set the
	 * fetch size and other performance options.
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if the query fails
	 */
	void query(String sql, @Nullable PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 使用 RowCallbackHandler 逐行读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if the query fails
	 * @see java.sql.Types
	 */
	void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 使用 RowCallbackHandler 逐行读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if the query fails
	 * @deprecated as of 5.3, in favor of {@link #query(String, RowCallbackHandler, Object...)}
	 */
	@Deprecated
	void query(String sql, @Nullable Object[] args, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 使用 RowCallbackHandler 逐行读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param rch a callback that will extract results, one row at a time
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 */
	void query(String sql, RowCallbackHandler rch, @Nullable Object... args) throws DataAccessException;

	/**
	 * 使用预编译语句进行查询，通过 RowMapper 将每一行映射为结果对象。
	 * <p>PreparedStatementCreator 可以直接实现，
	 * 也可以通过 PreparedStatementCreatorFactory 配置。
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if there is any problem
	 * @see PreparedStatementCreatorFactory
	 */
	<T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及知道如何将值绑定到查询的
	 * PreparedStatementSetter 实现进行查询，通过 RowMapper 将每一行映射为
	 * 结果对象。
	 * @param sql the SQL query to execute
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * Even if there are no bind parameters, this callback may be used to set the
	 * fetch size and other performance options.
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 */
	<T> List<T> query(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 通过 RowMapper 将每一行映射为结果对象。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 * @see java.sql.Types
	 */
	<T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 通过 RowMapper 将每一行映射为结果对象。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 * @deprecated as of 5.3, in favor of {@link #query(String, RowMapper, Object...)}
	 */
	@Deprecated
	<T> List<T> query(String sql, @Nullable Object[] args, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 通过 RowMapper 将每一行映射为结果对象。
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 */
	<T> List<T> query(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException;

	/**
	 * 使用预编译语句进行查询，通过 RowMapper 将每一行映射为结果对象，
	 * 并将其转换为可迭代且可关闭的 Stream。
	 * <p>PreparedStatementCreator 可以直接实现，
	 * 也可以通过 PreparedStatementCreatorFactory 配置。
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param rowMapper a callback that will map one object per row
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (e.g. through a try-with-resources clause)
	 * @throws DataAccessException if there is any problem
	 * @see PreparedStatementCreatorFactory
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及知道如何将值绑定到查询的
	 * PreparedStatementSetter 实现进行查询，通过 RowMapper 将每一行映射为
	 * 结果对象，并将其转换为可迭代且可关闭的 Stream。
	 * @param sql the SQL query to execute
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * Even if there are no bind parameters, this callback may be used to set the
	 * fetch size and other performance options.
	 * @param rowMapper a callback that will map one object per row
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (e.g. through a try-with-resources clause)
	 * @throws DataAccessException if the query fails
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 通过 RowMapper 将每一行映射为结果对象，并将其转换为可迭代且
	 * 可关闭的 Stream。
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (e.g. through a try-with-resources clause)
	 * @throws DataAccessException if the query fails
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, @Nullable Object... args)
			throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 通过 RowMapper 将单个结果行映射为结果对象。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param rowMapper a callback that will map one object per row
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code} null)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 */
	@Nullable
	<T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 通过 RowMapper 将单个结果行映射为结果对象。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param rowMapper a callback that will map one object per row
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code} null)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @deprecated as of 5.3, in favor of {@link #queryForObject(String, RowMapper, Object...)}
	 */
	@Deprecated
	@Nullable
	<T> T queryForObject(String sql, @Nullable Object[] args, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 通过 RowMapper 将单个结果行映射为结果对象。
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code} null)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 */
	@Nullable
	<T> T queryForObject(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果对象。
	 * <p>该查询应为单行/单列查询；返回的结果将直接映射到相应的对象类型。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param requiredType the type that the result object is expected to match
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if the query fails
	 * @see #queryForObject(String, Class)
	 * @see java.sql.Types
	 */
	@Nullable
	<T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType)
			throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果对象。
	 * <p>该查询应为单行/单列查询；返回的结果将直接映射到相应的对象类型。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param requiredType the type that the result object is expected to match
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if the query fails
	 * @see #queryForObject(String, Class)
	 * @deprecated as of 5.3, in favor of {@link #queryForObject(String, Class, Object...)}
	 */
	@Deprecated
	@Nullable
	<T> T queryForObject(String sql, @Nullable Object[] args, Class<T> requiredType) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果对象。
	 * <p>该查询应为单行/单列查询；返回的结果将直接映射到相应的对象类型。
	 * @param sql the SQL query to execute
	 * @param requiredType the type that the result object is expected to match
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 * @see #queryForObject(String, Class)
	 */
	@Nullable
	<T> T queryForObject(String sql, Class<T> requiredType, @Nullable Object... args) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果 Map。
	 * <p>该查询应为单行查询；结果行将映射到 Map
	 * （每列一个条目，使用列名作为键）。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return the result Map (one entry per column, with column name as key)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @see #queryForMap(String)
	 * @see ColumnMapRowMapper
	 * @see java.sql.Types
	 */
	Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果 Map。
	 * <p>当没有领域模型时，此接口定义的 {@code queryForMap} 方法是合适的。
	 * 否则，请考虑使用某个 {@code queryForObject} 方法。
	 * <p>该查询应为单行查询；结果行将映射到 Map
	 * （每列一个条目，使用列名作为键）。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the result Map (one entry for each column, using the
	 * column name as the key)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @see #queryForMap(String)
	 * @see ColumnMapRowMapper
	 */
	Map<String, Object> queryForMap(String sql, @Nullable Object... args) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果列表。
	 * <p>结果将映射为结果对象的 List（每行一个条目），
	 * 每个结果对象都与指定的元素类型匹配。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if the query fails
	 * @see #queryForList(String, Class)
	 * @see SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType)
			throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果列表。
	 * <p>结果将映射为结果对象的 List（每行一个条目），
	 * 每个结果对象都与指定的元素类型匹配。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if the query fails
	 * @see #queryForList(String, Class)
	 * @see SingleColumnRowMapper
	 * @deprecated as of 5.3, in favor of {@link #queryForList(String, Class, Object...)}
	 */
	@Deprecated
	<T> List<T> queryForList(String sql, @Nullable Object[] args, Class<T> elementType) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果列表。
	 * <p>结果将映射为结果对象的 List（每行一个条目），
	 * 每个结果对象都与指定的元素类型匹配。
	 * @param sql the SQL query to execute
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 * @see #queryForList(String, Class)
	 * @see SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, Class<T> elementType, @Nullable Object... args) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果列表。
	 * <p>结果将映射为 Map 的 List（每行一个条目）
	 * （每列一个条目，使用列名作为键）。
	 * 列表中的每个元素将采用此接口的 {@code queryForMap} 方法所返回的形式。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return a List that contains a Map per row
	 * @throws DataAccessException if the query fails
	 * @see #queryForList(String)
	 * @see java.sql.Types
	 */
	List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个结果列表。
	 * <p>结果将映射为 Map 的 List（每行一个条目）
	 * （每列一个条目，使用列名作为键）。
	 * 列表中的每个元素将采用此接口的 {@code queryForMap} 方法所返回的形式。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return a List that contains a Map per row
	 * @throws DataAccessException if the query fails
	 * @see #queryForList(String)
	 */
	List<Map<String, Object>> queryForList(String sql, @Nullable Object... args) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个 SqlRowSet。
	 * <p>结果将映射到一个以断开连接方式保存数据的 SqlRowSet。
	 * 此包装器会转换任何抛出的 SQLException。
	 * <p>请注意，对于默认实现，运行时需要 JDBC RowSet 支持：
	 * 默认使用 Sun 的 {@code com.sun.rowset.CachedRowSetImpl} 类，
	 * 它是 JDK 1.5+ 的一部分，也可以从 Sun 的 JDBC RowSet Implementations
	 * 下载包（rowset.jar）中单独获取。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return an SqlRowSet representation (possibly a wrapper around a
	 * {@code javax.sql.rowset.CachedRowSet})
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForRowSet(String)
	 * @see SqlRowSetResultSetExtractor
	 * @see javax.sql.rowset.CachedRowSet
	 * @see java.sql.Types
	 */
	SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException;

	/**
	 * 根据创建预编译语句的 SQL 以及要绑定到查询的参数列表进行查询，
	 * 期望返回一个 SqlRowSet。
	 * <p>结果将映射到一个以断开连接方式保存数据的 SqlRowSet。
	 * 此包装器会转换任何抛出的 SQLException。
	 * <p>请注意，对于默认实现，运行时需要 JDBC RowSet 支持：
	 * 默认使用 Sun 的 {@code com.sun.rowset.CachedRowSetImpl} 类，
	 * 它是 JDK 1.5+ 的一部分，也可以从 Sun 的 JDBC RowSet Implementations
	 * 下载包（rowset.jar）中单独获取。
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return an SqlRowSet representation (possibly a wrapper around a
	 * {@code javax.sql.rowset.CachedRowSet})
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForRowSet(String)
	 * @see SqlRowSetResultSetExtractor
	 * @see javax.sql.rowset.CachedRowSet
	 */
	SqlRowSet queryForRowSet(String sql, @Nullable Object... args) throws DataAccessException;

	/**
	 * 执行单条 SQL 更新操作（例如 insert、update 或 delete 语句），
	 * 使用 PreparedStatementCreator 提供 SQL 及任何必需的参数。
	 * <p>PreparedStatementCreator 可以直接实现，
	 * 也可以通过 PreparedStatementCreatorFactory 配置。
	 * @param psc a callback that provides SQL and any necessary parameters
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 * @see PreparedStatementCreatorFactory
	 */
	int update(PreparedStatementCreator psc) throws DataAccessException;

	/**
	 * 使用 PreparedStatementCreator 提供 SQL 及任何必需的参数，执行更新语句。
	 * 生成的键将放入给定的 KeyHolder 中。
	 * <p>请注意，给定的 PreparedStatementCreator 必须创建一个已启用生成键提取的
	 * 语句（JDBC 3.0 特性）。这可以直接完成，
	 * 也可以通过 PreparedStatementCreatorFactory 完成。
	 * @param psc a callback that provides SQL and any necessary parameters
	 * @param generatedKeyHolder a KeyHolder that will hold the generated keys
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 * @see PreparedStatementCreatorFactory
	 * @see org.springframework.jdbc.support.GeneratedKeyHolder
	 */
	int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) throws DataAccessException;

	/**
	 * 使用 PreparedStatementSetter 设置绑定参数以及给定的 SQL，执行更新语句。
	 * 比使用 PreparedStatementCreator 更简单，因为此方法会创建
	 * PreparedStatement：PreparedStatementSetter 只需设置参数即可。
	 * @param sql the SQL containing bind parameters
	 * @param pss helper that sets bind parameters. If this is {@code null}
	 * we run an update with static SQL.
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int update(String sql, @Nullable PreparedStatementSetter pss) throws DataAccessException;

	/**
	 * 通过预编译语句执行单条 SQL 更新操作（例如 insert、update 或 delete 语句），
	 * 并绑定给定的参数。
	 * @param sql the SQL containing bind parameters
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 * @see java.sql.Types
	 */
	int update(String sql, Object[] args, int[] argTypes) throws DataAccessException;

	/**
	 * 通过预编译语句执行单条 SQL 更新操作（例如 insert、update 或 delete 语句），
	 * 并绑定给定的参数。
	 * @param sql the SQL containing bind parameters
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int update(String sql, @Nullable Object... args) throws DataAccessException;

	/**
	 * 在单个 PreparedStatement 上执行多条更新语句，
	 * 使用批量更新和 BatchPreparedStatementSetter 来设置值。
	 * <p>如果 JDBC 驱动不支持批量更新，将回退为在单个 PreparedStatement 上
	 * 分别执行更新。
	 * @param sql defining PreparedStatement that will be reused.
	 * All statements in the batch will use the same SQL.
	 * @param pss object to set parameters on the PreparedStatement
	 * created by this method
	 * @return an array of the number of rows affected by each statement
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException;

	/**
	 * 使用提供的 SQL 语句以及一批提供的参数执行批量操作。
	 * @param sql the SQL statement to execute
	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
	 * @return an array containing the numbers of rows affected by each update in the batch
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException;

	/**
	 * 使用提供的 SQL 语句以及一批提供的参数执行批量操作。
	 * @param sql the SQL statement to execute.
	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return an array containing the numbers of rows affected by each update in the batch
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes) throws DataAccessException;

	/**
	 * 使用提供的 SQL 语句以及提供的参数集合执行多个批次操作。
	 * 参数值将使用 ParameterizedPreparedStatementSetter 设置。
	 * 每个批次的大小应为“batchSize”中指定的大小。
	 * @param sql the SQL statement to execute.
	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
	 * @param batchSize batch size
	 * @param pss the ParameterizedPreparedStatementSetter to use
	 * @return an array containing for each batch another array containing the numbers of
	 * rows affected by each update in the batch
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 * @since 3.1
	 */
	<T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize,
			ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Methods dealing with callable statements
	//-------------------------------------------------------------------------

	/**
	 * 执行 JDBC 数据访问操作，以在 JDBC CallableStatement 上工作的回调动作
	 * 形式实现。这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现
	 * 任意数据访问操作：即参与 Spring 管理的事务，并将 JDBC SQLException
	 * 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调动作可以返回结果对象，例如一个领域对象或一组领域对象。
	 * @param csc a callback that creates a CallableStatement given a Connection
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action) throws DataAccessException;

	/**
	 * 执行 JDBC 数据访问操作，以在 JDBC CallableStatement 上工作的回调动作
	 * 形式实现。这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现
	 * 任意数据访问操作：即参与 Spring 管理的事务，并将 JDBC SQLException
	 * 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调动作可以返回结果对象，例如一个领域对象或一组领域对象。
	 * @param callString the SQL call string to execute
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException;

	/**
	 * 使用 CallableStatementCreator 提供 SQL 及任何必需的参数，执行 SQL 调用。
	 * @param csc a callback that provides SQL and any necessary parameters
	 * @param declaredParameters list of declared SqlParameter objects
	 * @return a Map of extracted out parameters
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
			throws DataAccessException;

}
