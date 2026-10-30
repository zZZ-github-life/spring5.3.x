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
	 * @param sql 要执行的 SQL
	 * @param paramSource 要绑定到查询的参数容器
	 * @param action 指定操作的回调对象
	 * @return 由该操作返回的结果对象，或 {@code null}
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	<T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action)
			throws DataAccessException;

	/**
	 * 执行 JDBC 数据访问操作，实现为在 JDBC PreparedStatement 上工作的回调操作。
	 * 这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现任意的数据访问操作：
	 * 即参与 Spring 管理的事务，并将 JDBC SQLException 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调操作可以返回结果对象，例如领域对象或领域对象集合。
	 * @param sql 要执行的 SQL
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @param action 指定操作的回调对象
	 * @return 由该操作返回的结果对象，或 {@code null}
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	<T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action)
			throws DataAccessException;

	/**
	 * 执行 JDBC 数据访问操作，实现为在 JDBC PreparedStatement 上工作的回调操作。
	 * 这允许在 Spring 管理的 JDBC 环境中，在单个 Statement 上实现任意的数据访问操作：
	 * 即参与 Spring 管理的事务，并将 JDBC SQLException 转换为 Spring 的 DataAccessException 层次结构。
	 * <p>回调操作可以返回结果对象，例如领域对象或领域对象集合。
	 * @param sql 要执行的 SQL
	 * @param action 指定操作的回调对象
	 * @return 由该操作返回的结果对象，或 {@code null}
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	<T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 使用 ResultSetExtractor 读取 ResultSet。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @param rse 将提取结果的对象
	 * @return 任意结果对象，由 ResultSetExtractor 返回
	 * @throws DataAccessException 如果查询失败
	 */
	@Nullable
	<T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 使用 ResultSetExtractor 读取 ResultSet。
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @param rse 将提取结果的对象
	 * @return 任意结果对象，由 ResultSetExtractor 返回
	 * @throws DataAccessException 如果查询失败
	 */
	@Nullable
	<T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，
	 * 使用 ResultSetExtractor 读取 ResultSet。
	 * <p>注意：与具有相同签名的 JdbcOperations 方法不同，
	 * 此查询变体始终使用 PreparedStatement。它实际上等同于使用空参数 Map 的查询调用。
	 * @param sql 要执行的 SQL 查询
	 * @param rse 将提取结果的对象
	 * @return 任意结果对象，由 ResultSetExtractor 返回
	 * @throws DataAccessException 如果查询失败
	 */
	@Nullable
	<T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 使用 RowCallbackHandler 逐行读取 ResultSet。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @param rch 逐行提取结果的对象
	 * @throws DataAccessException 如果查询失败
	 */
	void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 使用 RowCallbackHandler 逐行读取 ResultSet。
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @param rch 逐行提取结果的对象
	 * @throws DataAccessException 如果查询失败
	 */
	void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，
	 * 使用 RowCallbackHandler 逐行读取 ResultSet。
	 * <p>注意：与具有相同签名的 JdbcOperations 方法不同，
	 * 此查询变体始终使用 PreparedStatement。它实际上等同于使用空参数 Map 的查询调用。
	 * @param sql 要执行的 SQL 查询
	 * @param rch 逐行提取结果的对象
	 * @throws DataAccessException 如果查询失败
	 */
	void query(String sql, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 通过 RowMapper 将每一行映射为 Java 对象。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @param rowMapper 将每行映射为一个对象的对象
	 * @return 结果 List，包含映射后的对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 通过 RowMapper 将每一行映射为 Java 对象。
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @param rowMapper 将每行映射为一个对象的对象
	 * @return 结果 List，包含映射后的对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，
	 * 通过 RowMapper 将每一行映射为 Java 对象。
	 * <p>注意：与具有相同签名的 JdbcOperations 方法不同，
	 * 此查询变体始终使用 PreparedStatement。它实际上等同于使用空参数 Map 的查询调用。
	 * @param sql 要执行的 SQL 查询
	 * @param rowMapper 将每行映射为一个对象的对象
	 * @return 结果 List，包含映射后的对象
	 * @throws DataAccessException 如果查询失败
	 */
	<T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 通过 RowMapper 将每一行映射为 Java 对象，并将其转换为可迭代且可关闭的 Stream。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @param rowMapper 将每行映射为一个对象的对象
	 * @return 结果 Stream，包含映射后的对象，在处理完成后需要关闭
	 * （例如通过 try-with-resources 子句）
	 * @throws DataAccessException 如果查询失败
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 通过 RowMapper 将每一行映射为 Java 对象，并将其转换为可迭代且可关闭的 Stream。
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @param rowMapper 将每行映射为一个对象的对象
	 * @return 结果 Stream，包含映射后的对象，在处理完成后需要关闭
	 * （例如通过 try-with-resources 子句）
	 * @throws DataAccessException 如果查询失败
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，
	 * 通过 RowMapper 将单行结果映射为 Java 对象。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @param rowMapper 将每行映射为一个对象的对象
	 * @return 单个映射后的对象（如果给定的 {@link RowMapper} 返回 {@code} null，
	 * 则可能为 {@code null}）
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 如果查询未恰好返回一行
	 * @throws DataAccessException 如果查询失败
	 */
	@Nullable
	<T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，
	 * 通过 RowMapper 将单行结果映射为 Java 对象。
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @param rowMapper 将每行映射为一个对象的对象
	 * @return 单个映射后的对象（如果给定的 {@link RowMapper} 返回 {@code} null，
	 * 则可能为 {@code null}）
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 如果查询未恰好返回一行
	 * @throws DataAccessException 如果查询失败
	 */
	@Nullable
	<T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个结果对象。
	 * <p>查询应为单行/单列查询；返回的结果将直接映射到相应的对象类型。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @param requiredType 结果对象应匹配的类型
	 * @return 所需类型的结果对象，如果为 SQL NULL 则为 {@code null}
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 如果查询未恰好返回一行
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException 如果查询未返回包含单列的行
	 * @throws DataAccessException 如果查询失败
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForObject(String, Class)
	 * @see org.springframework.jdbc.core.SingleColumnRowMapper
	 */
	@Nullable
	<T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，期望返回一个结果对象。
	 * <p>查询应为单行/单列查询；返回的结果将直接映射到相应的对象类型。
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @param requiredType 结果对象应匹配的类型
	 * @return 所需类型的结果对象，如果为 SQL NULL 则为 {@code null}
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 如果查询未恰好返回一行
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException 如果查询未返回包含单列的行
	 * @throws DataAccessException 如果查询失败
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForObject(String, Class)
	 */
	@Nullable
	<T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个结果 Map。
	 * <p>查询应为单行查询；结果行将映射为一个 Map（每列一个条目，以列名作为键）。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @return 结果 Map（每列一个条目，使用列名作为键）
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 如果查询未恰好返回一行
	 * @throws DataAccessException 如果查询失败
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForMap(String)
	 * @see org.springframework.jdbc.core.ColumnMapRowMapper
	 */
	Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，期望返回一个结果 Map。
	 * 当你没有领域模型时，适合使用此接口定义的 queryForMap() 方法；
	 * 否则，请考虑使用 queryForObject() 方法之一。
	 * <p>查询应为单行查询；结果行将映射为一个 Map（每列一个条目，以列名作为键）。
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @return 结果 Map（每列一个条目，使用列名作为键）
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 如果查询未恰好返回一行
	 * @throws DataAccessException 如果查询失败
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForMap(String)
	 * @see org.springframework.jdbc.core.ColumnMapRowMapper
	 */
	Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个结果列表。
	 * <p>结果将映射为一个 List（每行一个条目），每个结果对象匹配指定的元素类型。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @param elementType 结果列表中元素所需的类型（例如 {@code Integer.class}）
	 * @return 与指定元素类型匹配的对象 List
	 * @throws DataAccessException 如果查询失败
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForList(String, Class)
	 * @see org.springframework.jdbc.core.SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，期望返回一个结果列表。
	 * <p>结果将映射为一个 List（每行一个条目），每个结果对象匹配指定的元素类型。
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @param elementType 结果列表中元素所需的类型（例如 {@code Integer.class}）
	 * @return 与指定元素类型匹配的对象 List
	 * @throws DataAccessException 如果查询失败
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForList(String, Class)
	 * @see org.springframework.jdbc.core.SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType)
			throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个结果列表。
	 * <p>结果将映射为一个 List（每行一个条目），每行为一个 Map（每列一个条目，以列名作为键）。
	 * 列表中的每个元素都将采用此接口 {@code queryForMap} 方法返回的形式。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @return 每行包含一个 Map 的 List
	 * @throws DataAccessException 如果查询失败
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForList(String)
	 */
	List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数 Map 中的参数值，期望返回一个结果列表。
	 * <p>结果将映射为一个 List（每行一个条目），每行为一个 Map（每列一个条目，以列名作为键）。
	 * 列表中的每个元素都将采用此接口 {@code queryForMap} 方法返回的形式。
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @return 每行包含一个 Map 的 List
	 * @throws DataAccessException 如果查询失败
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForList(String)
	 */
	List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 根据给定的 SQL 创建预编译语句，绑定参数列表中的参数值，期望返回一个 SqlRowSet。
	 * <p>结果将映射为一个 SqlRowSet，以断开连接的方式保存数据。此包装器将转换抛出的任何 SQLException。
	 * <p>请注意，对于默认实现，需要在运行时提供 JDBC RowSet 支持：默认情况下使用 Sun 的
	 * {@code com.sun.rowset.CachedRowSetImpl} 类，该类是 JDK 1.5+ 的一部分，
	 * 也可作为 Sun JDBC RowSet 实现下载（rowset.jar）的独立组件使用。
	 * @param sql 要执行的 SQL 查询
	 * @param paramSource 要绑定到查询的参数容器
	 * @return SqlRowSet 表示形式
	 * （可能是对 {@code javax.sql.rowset.CachedRowSet} 的包装）
	 * @throws DataAccessException 如果执行查询时出现任何问题
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
	 * @param sql 要执行的 SQL 查询
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @return SqlRowSet 表示形式
	 * （可能是对 {@code javax.sql.rowset.CachedRowSet} 的包装）
	 * @throws DataAccessException 如果执行查询时出现任何问题
	 * @see org.springframework.jdbc.core.JdbcTemplate#queryForRowSet(String)
	 * @see org.springframework.jdbc.core.SqlRowSetResultSetExtractor
	 * @see javax.sql.rowset.CachedRowSet
	 */
	SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 通过预编译语句执行更新操作，绑定给定的参数。
	 * @param sql 包含命名参数的 SQL
	 * @param paramSource 要绑定到查询的参数和 SQL 类型容器
	 * @return 受影响的行数
	 * @throws DataAccessException 如果发出更新时出现任何问题
	 */
	int update(String sql, SqlParameterSource paramSource) throws DataAccessException;

	/**
	 * 通过预编译语句执行更新操作，绑定给定的参数。
	 * @param sql 包含命名参数的 SQL
	 * @param paramMap 绑定到查询的参数 Map（由 PreparedStatement 自行推断对应的 SQL 类型）
	 * @return 受影响的行数
	 * @throws DataAccessException 如果发出更新时出现任何问题
	 */
	int update(String sql, Map<String, ?> paramMap) throws DataAccessException;

	/**
	 * 通过预编译语句执行更新操作，绑定给定的参数，返回生成的键。
	 * @param sql 包含命名参数的 SQL
	 * @param paramSource 要绑定到查询的参数和 SQL 类型容器
	 * @param generatedKeyHolder 将保存生成键的 {@link KeyHolder}
	 * @return 受影响的行数
	 * @throws DataAccessException 如果发出更新时出现任何问题
	 * @see MapSqlParameterSource
	 * @see org.springframework.jdbc.support.GeneratedKeyHolder
	 */
	int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
			throws DataAccessException;

	/**
	 * 通过预编译语句执行更新操作，绑定给定的参数，返回生成的键。
	 * @param sql 包含命名参数的 SQL
	 * @param paramSource 要绑定到查询的参数和 SQL 类型容器
	 * @param generatedKeyHolder 将保存生成键的 {@link KeyHolder}
	 * @param keyColumnNames 将为其生成键的列名
	 * @return 受影响的行数
	 * @throws DataAccessException 如果发出更新时出现任何问题
	 * @see MapSqlParameterSource
	 * @see org.springframework.jdbc.support.GeneratedKeyHolder
	 */
	int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames)
			throws DataAccessException;

	/**
	 * 使用提供的 SQL 语句和批量参数执行批量更新操作。
	 * @param sql 要执行的 SQL 语句
	 * @param batchValues 包含查询批次参数的 Map 数组
	 * @return 包含批次中每次更新所影响行数的数组
	 * （也可能包含 JDBC 为受影响行定义的特殊负值，例如
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED}）
	 * @throws DataAccessException 如果发出更新时出现任何问题
	 */
	int[] batchUpdate(String sql, Map<String, ?>[] batchValues);

	/**
	 * 使用提供的 SQL 语句和批量参数执行批量更新操作。
	 * @param sql 要执行的 SQL 语句
	 * @param batchArgs 包含查询批次参数的 {@link SqlParameterSource} 数组
	 * @return 包含批次中每次更新所影响行数的数组
	 * （也可能包含 JDBC 为受影响行定义的特殊负值，例如
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED}）
	 * @throws DataAccessException 如果发出更新时出现任何问题
	 */
	int[] batchUpdate(String sql, SqlParameterSource[] batchArgs);

}
