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

package org.springframework.jdbc.core.namedparam;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.lang.Nullable;

/**
 * 指定一组基本的 JDBC 操作接口，允许使用命名参数
 * 而不是传统的 '?' 占位符。
 *
 * <p>这是经典的 {@link org.springframework.jdbc.core.JdbcOperations} 接口的替代方案，
 * 由 {@link NamedParameterJdbcTemplate} 实现。此接口通常不直接使用，
 * 但提供了增强可测试性的有用选项，因为它可以轻松地被模拟或存根。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcTemplate
 * @see org.springframework.jdbc.core.JdbcOperations
 */
public interface NamedParameterJdbcOperations {

	/**
	 * 暴露经典的 Spring JdbcTemplate，以允许调用经典的 JDBC 操作。
	 */
	JdbcOperations getJdbcOperations();


	/**
	 * 执行 JDBC 数据访问操作，实现为在 JDBC PreparedStatement 上工作的回调操作。
	 * 这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现任意的数据访问操作：
	 * 即参与 Spring 管理的事务，并将 JDBC SQLException 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调操作可以返回结果对象，例如领域对象或领域对象集合。
	 * @param sql the SQL to execute
	 * @param paramSource container of arguments to bind to the query
	 * @param action callback object that specifies the action
	 * @return a result object returned by the action, or {@code null}
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action)
			throws DataAccessException;

	/**
	 * 执行 JDBC 数据访问操作，实现为在 JDBC PreparedStatement 上工作的回调操作。
	 * 这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现任意的数据访问操作：
	 * 即参与 Spring 管理的事务，并将 JDBC SQLException 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调操作可以返回结果对象，例如领域对象或领域对象集合。
	 * @param sql the SQL to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param action callback object that specifies the action
	 * @return a result object returned by the action, or {@code null}
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action)
			throws DataAccessException;

	/**
	 * 执行 JDBC 数据访问操作，实现为在 JDBC PreparedStatement 上工作的回调操作。
	 * 这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现任意的数据访问操作：
	 * 即参与 Spring 管理的事务，并将 JDBC SQLException 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调操作可以返回结果对象，例如领域对象或领域对象集合。
	 * @param sql the SQL to execute
	 * @param action callback object that specifies the action
	 * @return a result object returned by the action, or {@code null}
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 使用 ResultSetExtractor 读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @param rse object that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if the query fails
	 */
	@Nullable
	<T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 使用 ResultSetExtractor 读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param rse object that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if the query fails
	 */
	@Nullable
	<T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，
	 * 使用 ResultSetExtractor 读取 ResultSet。
	 * <p>注意：与具有相同签名的 JdbcOperations 方法不同，
	 * 此查询变体始终使用 PreparedStatement。它实际上等同于使用空参数 Map 的查询调用。
	 * @param sql the SQL query to execute
	 * @param rse object that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if the query fails
	 */
	@Nullable
	<T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 使用 RowCallbackHandler 逐行读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @param rch object that will extract results, one row at a time
	 * @throws DataAccessException if the query fails
	 */
	void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 使用 RowCallbackHandler 逐行读取 ResultSet。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param rch object that will extract results, one row at a time
	 * @throws DataAccessException if the query fails
	 */
	void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，
	 * 使用 RowCallbackHandler 逐行读取 ResultSet。
	 * <p>注意：与具有相同签名的 JdbcOperations 方法不同，
	 * 此查询变体始终使用 PreparedStatement。它实际上等同于使用空参数 Map 的查询调用。
	 * @param sql the SQL query to execute
	 * @param rch object that will extract results, one row at a time
	 * @throws DataAccessException if the query fails
	 */
	void query(String sql, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 通过 RowMapper 将每一行映射为 Java 对象。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @param rowMapper object that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 */
	<T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 通过 RowMapper 将每一行映射为 Java 对象。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param rowMapper object that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 */
	<T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，
	 * 通过 RowMapper 将每一行映射为 Java 对象。
	 * <p>注意：与具有相同签名的 JdbcOperations 方法不同，
	 * 此查询变体始终使用 PreparedStatement。它实际上等同于使用空参数 Map 的查询调用。
	 * @param sql the SQL query to execute
	 * @param rowMapper object that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 */
	<T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 通过 RowMapper 将每一行映射为 Java 对象，并将其转换为可迭代且可关闭的 Stream。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @param rowMapper object that will map one object per row
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (e.g. through a try-with-resources clause)
	 * @throws DataAccessException if the query fails
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 通过 RowMapper 将每一行映射为 Java 对象，并将其转换为可迭代且可关闭的 Stream。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param rowMapper object that will map one object per row
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (e.g. through a try-with-resources clause)
	 * @throws DataAccessException if the query fails
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 通过 RowMapper 将单行结果映射为 Java 对象。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @param rowMapper object that will map one object per row
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code} null)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 */
	@Nullable
	<T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 通过 RowMapper 将单行结果映射为 Java 对象。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param rowMapper object that will map one object per row
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code} null)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 */
	@Nullable
	<T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个结果对象。
	 * <p>查询应为单行/单列查询；返回的结果将直接映射到相应的对象类型。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @param requiredType the type that the result object is expected to match
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if the query fails
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForObject(String, Class)
	 * @see org.springframework.jdbc.core.SingleColumnRowMapper
	 */
	@Nullable
	<T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，期望返回一个结果对象。
	 * <p>查询应为单行/单列查询；返回的结果将直接映射到相应的对象类型。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param requiredType the type that the result object is expected to match
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if the query fails
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForObject(String, Class)
	 */
	@Nullable
	<T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个结果 Map。
	 * <p>查询应为单行查询；结果行将映射为一个 Map（每列一个条目，以列名作为键）。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @return the result Map (one entry for each column, using the column name as the key)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForMap(String)
	 * @see org.springframework.jdbc.core.ColumnMapRowMapper
	 */
	Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，期望返回一个结果 Map。
	 * 当你没有领域模型时，适合使用此接口定义的 queryForMap() 方法；
	 * 否则，请考虑使用 queryForObject() 方法之一。
	 * <p>查询应为单行查询；结果行将映射为一个 Map（每列一个条目，以列名作为键）。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @return the result Map (one entry for each column, using the column name as the key)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForMap(String)
	 * @see org.springframework.jdbc.core.ColumnMapRowMapper
	 */
	Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个结果列表。
	 * <p>结果将映射为一个 List（每行一个条目），每个结果对象匹配指定的元素类型。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if the query fails
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForList(String, Class)
	 * @see org.springframework.jdbc.core.SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，期望返回一个结果列表。
	 * <p>结果将映射为一个 List（每行一个条目），每个结果对象匹配指定的元素类型。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if the query fails
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForList(String, Class)
	 * @see org.springframework.jdbc.core.SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个结果列表。
	 * <p>结果将映射为一个 List（每行一个条目），每行为一个 Map（每列一个条目，以列名作为键）。
	 * 列表中的每个元素都将采用此接口 {@code queryForMap} 方法返回的形式。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @return a List that contains a Map per row
	 * @throws DataAccessException if the query fails
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForList(String)
	 */
	List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，期望返回一个结果列表。
	 * <p>结果将映射为一个 List（每行一个条目），每行为一个 Map（每列一个条目，以列名作为键）。
	 * 列表中的每个元素都将采用此接口 {@code queryForMap} 方法返回的形式。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @return a List that contains a Map per row
	 * @throws DataAccessException if the query fails
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForList(String)
	 */
	List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个 SqlRowSet。
	 * <p>结果将映射为一个 SqlRowSet，以断开连接的方式保存数据。此包装器将转换抛出的任何 SQLException。
	 * <p>请注意，对于默认实现，需要在运行时提供 JDBC RowSet 支持：默认情况下使用 Sun 的
	 * {@code com.sun.rowset.CachedRowSetImpl} 类，该类是 JDK 1.5+ 的一部分，
	 * 也可作为 Sun JDBC RowSet 实现下载（rowset.jar）的独立组件使用。
	 * @param sql the SQL query to execute
	 * @param paramSource container of arguments to bind to the query
	 * @return an SqlRowSet representation (possibly a wrapper around a
	 * {@code javax.sql.rowset.CachedRowSet})
	 * @throws DataAccessException if there is any problem executing the query
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForRowSet(String)
	 * @see org.springframework.jdbc.core.SqlRowSetResultSetExtractor
	 * @see javax.sql.rowset.CachedRowSet
	 */
	SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，期望返回一个 SqlRowSet。
	 * <p>结果将映射为一个 SqlRowSet，以断开连接的方式保存数据。此包装器将转换抛出的任何 SQLException。
	 * <p>请注意，对于默认实现，需要在运行时提供 JDBC RowSet 支持：默认情况下使用 Sun 的
	 * {@code com.sun.rowset.CachedRowSetImpl} 类，该类是 JDK 1.5+ 的一部分，
	 * 也可作为 Sun JDBC RowSet 实现下载（rowset.jar）的独立组件使用。
	 * @param sql the SQL query to execute
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @return an SqlRowSet representation (possibly a wrapper around a
	 * {@code javax.sql.rowset.CachedRowSet})
	 * @throws DataAccessException if there is any problem executing the query
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForRowSet(String)
	 * @see org.springframework.jdbc.core.SqlRowSetResultSetExtractor
	 * @see javax.sql.rowset.CachedRowSet
	 */
	SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 通过预编译语句执行更新操作，绑定给定的参数。
	 * @param sql the SQL containing named parameters
	 * @param paramSource container of arguments and SQL types to bind to the query
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int update(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 通过预编译语句执行更新操作，绑定给定的参数。
	 * @param sql the SQL containing named parameters
	 * @param paramMap map of parameters to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int update(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 通过预编译语句执行更新操作，绑定给定的参数，返回生成的键。
	 * @param sql the SQL containing named parameters
	 * @param paramSource container of arguments and SQL types to bind to the query
	 * @param generatedKeyHolder a {@link KeyHolder} that will hold the generated keys
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 * @see MapSqlParameterSource
	 * @see org.springframework.jdbc.support.GeneratedKeyHolder
	 */
	int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
			throws DataAccessException;

	/**
	 * 通过预编译语句执行更新操作，绑定给定的参数，返回生成的键。
	 * @param sql the SQL containing named parameters
	 * @param paramSource container of arguments and SQL types to bind to the query
	 * @param generatedKeyHolder a {@link KeyHolder} that will hold the generated keys
	 * @param keyColumnNames names of the columns that will have keys generated for them
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 * @see MapSqlParameterSource
	 * @see org.springframework.jdbc.support.GeneratedKeyHolder
	 */
	int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames)
			throws DataAccessException;

	/**
	 * 使用提供的 SQL 语句和批量参数执行批量更新操作。
	 * @param sql the SQL statement to execute
	 * @param batchValues the array of Maps containing the batch of arguments for the query
	 * @return an array containing the numbers of rows affected by each update in the batch
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int[] batchUpdate(String sql, Map<String, ?>[] batchValues);

	/**
	 * 使用提供的 SQL 语句和批量参数执行批量更新操作。
	 * @param sql the SQL statement to execute
	 * @param batchArgs the array of {@link SqlParameterSource} containing the batch of
	 * arguments for the query
	 * @return an array containing the numbers of rows affected by each update in the batch
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int[] batchUpdate(String sql, SqlParameterSource[] batchArgs);

}
