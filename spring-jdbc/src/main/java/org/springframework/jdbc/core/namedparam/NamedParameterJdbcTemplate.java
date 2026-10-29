/*
 * Copyright 2002-2020 the original author or authors.
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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlRowSetResultSetExtractor;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;

/**
 * 模板类，提供一组基本的 JDBC 操作，允许使用命名参数
 * 而不是传统的 '?' 占位符。
 *
 * <p>此类在执行时将命名参数替换为 JDBC 风格的 '?' 占位符后，
 * 委托给包装的 {@link #getJdbcOperations() JdbcTemplate}。
 * 它还支持将 {@link java.util.List} 值展开为适当数量的占位符。
 *
 * <p>底层的 {@link org.springframework.jdbc.core.JdbcTemplate} 被暴露出来，
 * 以便方便地访问传统的 {@link org.springframework.jdbc.core.JdbcTemplate} 方法。
 *
 * <p><b>注意：一旦配置完成，此类的实例是线程安全的。</b>
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcOperations
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public class NamedParameterJdbcTemplate implements NamedParameterJdbcOperations {

	/** 此模板 SQL 缓存的默认最大条目数：256。 */
	public static final int DEFAULT_CACHE_LIMIT = 256;


	/** 我们要包装的 JdbcTemplate。 */
	private final JdbcOperations classicJdbcTemplate;

	/** 原始 SQL 字符串到 ParsedSql 表示的缓存。 */
	private volatile ConcurrentLruCache<String, ParsedSql> parsedSqlCache =
			new ConcurrentLruCache<>(DEFAULT_CACHE_LIMIT, NamedParameterUtils::parseSqlStatement);


	/**
	 * 为给定的 {@link DataSource} 创建一个新的 NamedParameterJdbcTemplate。
	 * <p>创建经典的 Spring {@link org.springframework.jdbc.core.JdbcTemplate} 并包装它。
	 * @param dataSource the JDBC DataSource to access
	 */
	public NamedParameterJdbcTemplate(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		this.classicJdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * 为给定的经典 Spring {@link org.springframework.jdbc.core.JdbcTemplate}
	 * 创建一个新的 NamedParameterJdbcTemplate。
	 * @param classicJdbcTemplate the classic Spring JdbcTemplate to wrap
	 */
	public NamedParameterJdbcTemplate(JdbcOperations classicJdbcTemplate) {
		Assert.notNull(classicJdbcTemplate, "JdbcTemplate must not be null");
		this.classicJdbcTemplate = classicJdbcTemplate;
	}


	/**
	 * 暴露经典的 Spring JdbcTemplate 操作，以便调用
	 * 不太常用的方法。
	 */
	@Override
	public JdbcOperations getJdbcOperations() {
		return this.classicJdbcTemplate;
	}

	/**
	 * 暴露经典的 Spring {@link JdbcTemplate} 本身（如果可用），
	 * 特别是为了将其传递给其他 {@code JdbcTemplate} 使用方。
	 * <p>如果就当前目的而言已经足够，推荐使用 {@link #getJdbcOperations()}
	 * 而不是此变体。
	 * @since 5.0.3
	 */
	public JdbcTemplate getJdbcTemplate() {
		Assert.state(this.classicJdbcTemplate instanceof JdbcTemplate, "No JdbcTemplate available");
		return (JdbcTemplate) this.classicJdbcTemplate;
	}

	/**
	 * 指定此模板 SQL 缓存的最大条目数。
	 * 默认为 256。0 表示不缓存，每次都解析语句。
	 */
	public void setCacheLimit(int cacheLimit) {
		this.parsedSqlCache = new ConcurrentLruCache<>(cacheLimit, NamedParameterUtils::parseSqlStatement);
	}

	/**
	 * 返回此模板 SQL 缓存的最大条目数。
	 */
	public int getCacheLimit() {
		return this.parsedSqlCache.sizeLimit();
	}


	@Override
	@Nullable
	public <T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action)
			throws DataAccessException {

		return getJdbcOperations().execute(getPreparedStatementCreator(sql, paramSource), action);
	}

	@Override
	@Nullable
	public <T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action)
			throws DataAccessException {

		return execute(sql, new MapSqlParameterSource(paramMap), action);
	}

	@Override
	@Nullable
	public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
		return execute(sql, EmptySqlParameterSource.INSTANCE, action);
	}

	@Override
	@Nullable
	public <T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse)
			throws DataAccessException {

		return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse)
			throws DataAccessException {

		return query(sql, new MapSqlParameterSource(paramMap), rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, EmptySqlParameterSource.INSTANCE, rse);
	}

	@Override
	public void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch)
			throws DataAccessException {

		getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rch);
	}

	@Override
	public void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch)
			throws DataAccessException {

		query(sql, new MapSqlParameterSource(paramMap), rch);
	}

	@Override
	public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
		query(sql, EmptySqlParameterSource.INSTANCE, rch);
	}

	@Override
	public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException {

		return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
	}

	@Override
	public <T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException {

		return query(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		return query(sql, EmptySqlParameterSource.INSTANCE, rowMapper);
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException {

		return getJdbcOperations().queryForStream(getPreparedStatementCreator(sql, paramSource), rowMapper);
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException {

		return queryForStream(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException {

		List<T> results = getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T>rowMapper)
			throws DataAccessException {

		return queryForObject(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType)
			throws DataAccessException {

		return queryForObject(sql, paramSource, new SingleColumnRowMapper<>(requiredType));
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType)
			throws DataAccessException {

		return queryForObject(sql, paramMap, new SingleColumnRowMapper<>(requiredType));
	}

	@Override
	public Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException {
		Map<String, Object> result = queryForObject(sql, paramSource, new ColumnMapRowMapper());
		Assert.state(result != null, "No result map");
		return result;
	}

	@Override
	public Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException {
		Map<String, Object> result = queryForObject(sql, paramMap, new ColumnMapRowMapper());
		Assert.state(result != null, "No result map");
		return result;
	}

	@Override
	public <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType)
			throws DataAccessException {

		return query(sql, paramSource, new SingleColumnRowMapper<>(elementType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType)
			throws DataAccessException {

		return queryForList(sql, new MapSqlParameterSource(paramMap), elementType);
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource)
			throws DataAccessException {

		return query(sql, paramSource, new ColumnMapRowMapper());
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap)
			throws DataAccessException {

		return queryForList(sql, new MapSqlParameterSource(paramMap));
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException {
		SqlRowSet result = getJdbcOperations().query(
				getPreparedStatementCreator(sql, paramSource), new SqlRowSetResultSetExtractor());
		Assert.state(result != null, "No result");
		return result;
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException {
		return queryForRowSet(sql, new MapSqlParameterSource(paramMap));
	}

	@Override
	public int update(String sql, SqlParameterSource paramSource) throws DataAccessException {
		return getJdbcOperations().update(getPreparedStatementCreator(sql, paramSource));
	}

	@Override
	public int update(String sql, Map<String, ?> paramMap) throws DataAccessException {
		return update(sql, new MapSqlParameterSource(paramMap));
	}

	@Override
	public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
			throws DataAccessException {

		return update(sql, paramSource, generatedKeyHolder, null);
	}

	@Override
	public int update(
			String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, @Nullable String[] keyColumnNames)
			throws DataAccessException {

		PreparedStatementCreator psc = getPreparedStatementCreator(sql, paramSource, pscf -> {
			if (keyColumnNames != null) {
				pscf.setGeneratedKeysColumnNames(keyColumnNames);
			}
			else {
				pscf.setReturnGeneratedKeys(true);
			}
		});
		return getJdbcOperations().update(psc, generatedKeyHolder);
	}

	@Override
	public int[] batchUpdate(String sql, Map<String, ?>[] batchValues) {
		return batchUpdate(sql, SqlParameterSourceUtils.createBatch(batchValues));
	}

	@Override
	public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs) {
		if (batchArgs.length == 0) {
			return new int[0];
		}

		ParsedSql parsedSql = getParsedSql(sql);
		PreparedStatementCreatorFactory pscf = getPreparedStatementCreatorFactory(parsedSql, batchArgs[0]);

		return getJdbcOperations().batchUpdate(
				pscf.getSql(),
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Object[] values = NamedParameterUtils.buildValueArray(parsedSql, batchArgs[i], null);
						pscf.newPreparedStatementSetter(values).setValues(ps);
					}
					@Override
					public int getBatchSize() {
						return batchArgs.length;
					}
				});
	}


	/**
	 * 基于给定的 SQL 和命名参数构建 {@link PreparedStatementCreator}。
	 * <p>注意：由所有 {@code query} 变体直接调用。委托给通用的
	 * {@link #getPreparedStatementCreator(String, SqlParameterSource, Consumer)} 方法。
	 * @param sql the SQL statement to execute
	 * @param paramSource container of arguments to bind
	 * @return the corresponding {@link PreparedStatementCreator}
	 * @see #getPreparedStatementCreator(String, SqlParameterSource, Consumer)
	 */
	protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource) {
		return getPreparedStatementCreator(sql, paramSource, null);
	}

	/**
	 * 基于给定的 SQL 和命名参数构建 {@link PreparedStatementCreator}。
	 * <p>注意：用于带生成键处理的 {@code update} 变体，同时也
	 * 由 {@link #getPreparedStatementCreator(String, SqlParameterSource)} 委托调用。
	 * @param sql the SQL statement to execute
	 * @param paramSource container of arguments to bind
	 * @param customizer callback for setting further properties on the
	 * {@link PreparedStatementCreatorFactory} in use, applied before the
	 * actual {@code newPreparedStatementCreator} call
	 * @return the corresponding {@link PreparedStatementCreator}
	 * @since 5.0.5
	 * @see #getParsedSql(String)
	 * @see PreparedStatementCreatorFactory#PreparedStatementCreatorFactory(String, List)
	 * @see PreparedStatementCreatorFactory#newPreparedStatementCreator(Object[])
	 */
	protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource,
			@Nullable Consumer<PreparedStatementCreatorFactory> customizer) {

		ParsedSql parsedSql = getParsedSql(sql);
		PreparedStatementCreatorFactory pscf = getPreparedStatementCreatorFactory(parsedSql, paramSource);
		if (customizer != null) {
			customizer.accept(pscf);
		}
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
		return pscf.newPreparedStatementCreator(params);
	}

	/**
	 * 获取给定 SQL 语句的已解析表示。
	 * <p>默认实现使用上限为 256 个条目的 LRU 缓存。
	 * @param sql the original SQL statement
	 * @return a representation of the parsed SQL statement
	 */
	protected ParsedSql getParsedSql(String sql) {
		return this.parsedSqlCache.get(sql);
	}

	/**
	 * 基于给定的 SQL 和命名参数构建 {@link PreparedStatementCreatorFactory}。
	 * @param parsedSql parsed representation of the given SQL statement
	 * @param paramSource container of arguments to bind
	 * @return the corresponding {@link PreparedStatementCreatorFactory}
	 * @since 5.1.3
	 * @see #getPreparedStatementCreator(String, SqlParameterSource, Consumer)
	 * @see #getParsedSql(String)
	 */
	protected PreparedStatementCreatorFactory getPreparedStatementCreatorFactory(
			ParsedSql parsedSql, SqlParameterSource paramSource) {

		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		List<SqlParameter> declaredParameters = NamedParameterUtils.buildSqlParameterList(parsedSql, paramSource);
		return new PreparedStatementCreatorFactory(sqlToUse, declaredParameters);
	}

}
