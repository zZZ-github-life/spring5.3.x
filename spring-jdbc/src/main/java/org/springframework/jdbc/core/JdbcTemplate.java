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

package org.springframework.jdbc.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * <b>这是 JDBC 核心包中的核心类。</b>
 * 它简化了 JDBC 的使用，并帮助避免常见错误。
 * 它执行核心 JDBC 工作流，让应用程序代码负责提供 SQL 和提取结果。
 * 该类执行 SQL 查询或更新，启动对 ResultSet 的迭代，捕获 JDBC 异常，
 * 并将其转换为 {@code org.springframework.dao} 包中定义的通用且信息更丰富的
 * 异常层次结构。
 *
 * <p>使用此类的代码只需实现回调接口，从而获得清晰定义的契约。
 * {@link PreparedStatementCreator} 回调接口根据 Connection 创建预编译语句，
 * 提供 SQL 和任何必需的参数。{@link ResultSetExtractor} 接口从 ResultSet
 * 中提取值。另请参见 {@link PreparedStatementSetter} 和 {@link RowMapper}
 * 这两个常用的备选回调接口。
 *
 * <p>既可以在服务实现中通过 DataSource 引用直接实例化使用，也可以在应用程序
 * 上下文中准备好并以 bean 引用的形式注入服务。注意：DataSource 应始终作为
 * 应用程序上下文中的 bean 进行配置，在第一种情况下直接提供给服务，
 * 在第二种情况下提供给准备好的模板。
 *
 * <p>由于此类可以通过回调接口和
 * {@link org.springframework.jdbc.support.SQLExceptionTranslator} 接口进行参数化，
 * 因此应该不需要继承它。
 *
 * <p>此类执行的所有 SQL 操作都以 debug 级别记录日志，
 * 使用“org.springframework.jdbc.core.JdbcTemplate”作为日志类别。
 *
 * <p><b>注意：此类一旦配置完成，其实例是线程安全的。</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since May 3, 2001
 * @see PreparedStatementCreator
 * @see PreparedStatementSetter
 * @see CallableStatementCreator
 * @see PreparedStatementCallback
 * @see CallableStatementCallback
 * @see ResultSetExtractor
 * @see RowCallbackHandler
 * @see RowMapper
 * @see org.springframework.jdbc.support.SQLExceptionTranslator
 */
public class JdbcTemplate extends JdbcAccessor implements JdbcOperations {

	private static final String RETURN_RESULT_SET_PREFIX = "#result-set-";

	private static final String RETURN_UPDATE_COUNT_PREFIX = "#update-count-";


	/** 如果此变量为 {@code false}，遇到 SQL 警告时我们将抛出异常。 */
	private boolean ignoreWarnings = true;

	/**
	 * 如果此变量被设置为非负值，它将用于设置查询处理所用语句的
	 * fetchSize 属性。
	 */
	private int fetchSize = -1;

	/**
	 * 如果此变量被设置为非负值，它将用于设置查询处理所用语句的
	 * maxRows 属性。
	 */
	private int maxRows = -1;

	/**
	 * 如果此变量被设置为非负值，它将用于设置查询处理所用语句的
	 * queryTimeout 属性。
	 */
	private int queryTimeout = -1;

	/**
	 * 如果此变量被设置为 true，则对于任何可调用语句处理都会跳过所有结果检查。
	 * 这可用于规避某些较旧的 Oracle JDBC 驱动（如 10.1.0.2）中的缺陷。
	 */
	private boolean skipResultsProcessing = false;

	/**
	 * 如果此变量被设置为 true，则存储过程调用中所有没有对应 SqlOutParameter
	 * 声明的结果都将被跳过。除非变量 {@code skipResultsProcessing} 被设置为
	 * {@code true}，否则所有其他结果处理都将照常进行。
	 */
	private boolean skipUndeclaredResults = false;

	/**
	 * 如果此变量被设置为 true，则执行 CallableStatement 将以使用
	 * 大小写不敏感参数名的 Map 形式返回结果。
	 */
	private boolean resultsMapCaseInsensitive = false;


	/**
	 * 构造一个新的 JdbcTemplate 以供 bean 使用。
	 * <p>注意：在使用该实例之前必须设置 DataSource。
	 * @see #setDataSource
	 */
	public JdbcTemplate() {
	}

	/**
	 * 使用给定的 DataSource 构造一个新的 JdbcTemplate 以获取连接。
	 * <p>注意：这不会触发异常转换器的初始化。
	 * @param dataSource 用于获取连接的 JDBC DataSource
	 */
	public JdbcTemplate(DataSource dataSource) {
		setDataSource(dataSource);
		afterPropertiesSet();
	}

	/**
	 * 使用给定的 DataSource 构造一个新的 JdbcTemplate 以获取连接。
	 * <p>注意：根据“lazyInit”标志，将触发异常转换器的初始化。
	 * @param dataSource 用于获取连接的 JDBC DataSource
	 * @param lazyInit 是否延迟初始化 SQLExceptionTranslator
	 */
	public JdbcTemplate(DataSource dataSource, boolean lazyInit) {
		setDataSource(dataSource);
		setLazyInit(lazyInit);
		afterPropertiesSet();
	}


	/**
	 * 设置是否忽略 SQLWarning。
	 * <p>默认值为“true”，即吞掉并记录所有警告。将此标志切换为“false”
	 * 可使 JdbcTemplate 改为抛出 SQLWarningException。
	 * @see java.sql.SQLWarning
	 * @see org.springframework.jdbc.SQLWarningException
	 * @see #handleWarnings
	 */
	public void setIgnoreWarnings(boolean ignoreWarnings) {
		this.ignoreWarnings = ignoreWarnings;
	}

	/**
	 * 返回我们是否忽略 SQLWarning。
	 */
	public boolean isIgnoreWarnings() {
		return this.ignoreWarnings;
	}

	/**
	 * 为此 JdbcTemplate 设置 fetch size。这对于处理大型结果集非常重要：
	 * 将其设置为高于默认值可提高处理速度，但会增加内存消耗；将其设置得更低
	 * 则可以避免传输应用程序永远不会读取的行数据。
	 * <p>默认值为 -1，表示使用 JDBC 驱动的默认配置
	 * （即不向驱动传递特定的 fetch size 设置）。
	 * <p>注意：从 4.3 开始，-1 以外的负值将被传递给驱动，
	 * 因为例如 MySQL 对 {@code Integer.MIN_VALUE} 支持特殊行为。
	 * @see java.sql.Statement#setFetchSize
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * 返回为此 JdbcTemplate 指定的 fetch size。
	 */
	public int getFetchSize() {
		return this.fetchSize;
	}

	/**
	 * 为此 JdbcTemplate 设置最大行数。这对于处理大型结果集的子集非常重要，
	 * 如果我们从一开始就不关心完整结果（例如在执行可能返回大量匹配项的
	 * 搜索时），可以避免在数据库或 JDBC 驱动中读取并保留整个结果集。
	 * <p>默认值为 -1，表示使用 JDBC 驱动的默认配置
	 * （即不向驱动传递特定的最大行数设置）。
	 * <p>注意：从 4.3 开始，-1 以外的负值将被传递给驱动，
	 * 与 {@link #setFetchSize} 对特殊 MySQL 值的支持保持一致。
	 * @see java.sql.Statement#setMaxRows
	 */
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	/**
	 * 返回为此 JdbcTemplate 指定的最大行数。
	 */
	public int getMaxRows() {
		return this.maxRows;
	}

	/**
	 * 设置此 JdbcTemplate 所执行语句的查询超时时间。
	 * <p>默认值为 -1，表示使用 JDBC 驱动的默认配置
	 * （即不向驱动传递特定的查询超时设置）。
	 * <p>注意：在具有事务级超时设置的事务中执行时，
	 * 此处指定的任何超时都会被剩余的事务超时时间覆盖。
	 * @see java.sql.Statement#setQueryTimeout
	 */
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	/**
	 * 返回此 JdbcTemplate 所执行语句的查询超时时间。
	 */
	public int getQueryTimeout() {
		return this.queryTimeout;
	}

	/**
	 * 设置是否应跳过结果处理。当我们知道不会传回任何结果时，
	 * 可用于优化可调用语句处理——out 参数的处理仍将进行。
	 * 这可用于规避某些较旧的 Oracle JDBC 驱动（如 10.1.0.2）中的缺陷。
	 */
	public void setSkipResultsProcessing(boolean skipResultsProcessing) {
		this.skipResultsProcessing = skipResultsProcessing;
	}

	/**
	 * 返回是否应跳过结果处理。
	 */
	public boolean isSkipResultsProcessing() {
		return this.skipResultsProcessing;
	}

	/**
	 * 设置是否应跳过未声明的结果。
	 */
	public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
		this.skipUndeclaredResults = skipUndeclaredResults;
	}

	/**
	 * 返回是否应跳过未声明的结果。
	 */
	public boolean isSkipUndeclaredResults() {
		return this.skipUndeclaredResults;
	}

	/**
	 * 设置执行 CallableStatement 是否将以使用大小写不敏感参数名的 Map
	 * 形式返回结果。
	 */
	public void setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
		this.resultsMapCaseInsensitive = resultsMapCaseInsensitive;
	}

	/**
	 * 返回执行 CallableStatement 是否将以使用大小写不敏感参数名的 Map
	 * 形式返回结果。
	 */
	public boolean isResultsMapCaseInsensitive() {
		return this.resultsMapCaseInsensitive;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with a plain java.sql.Connection
	//-------------------------------------------------------------------------

	@Override
	@Nullable
	public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		try {
			// Create close-suppressing Connection proxy, also preparing returned Statements.
			Connection conToUse = createConnectionProxy(con);
			return action.doInConnection(conToUse);
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			String sql = getSql(action);
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("ConnectionCallback", sql, ex);
		}
		finally {
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	/**
	 * 为给定的 JDBC Connection 创建一个抑制 close 调用的代理。
	 * 由 {@code execute} 方法调用。
	 * <p>该代理还会准备返回的 JDBC Statement，应用 fetch size、max rows
	 * 和 query timeout 等语句设置。
	 * @param con 要为其创建代理的 JDBC Connection
	 * @return Connection 代理
	 * @see java.sql.Connection#close()
	 * @see #execute(ConnectionCallback)
	 * @see #applyStatementSettings
	 */
	protected Connection createConnectionProxy(Connection con) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new CloseSuppressingInvocationHandler(con));
	}


	//-------------------------------------------------------------------------
	// Methods dealing with static SQL (java.sql.Statement)
	//-------------------------------------------------------------------------

	@Nullable
	private <T> T execute(StatementCallback<T> action, boolean closeResources) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			applyStatementSettings(stmt);
			T result = action.doInStatement(stmt);
			handleWarnings(stmt);
			return result;
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			String sql = getSql(action);
			JdbcUtils.closeStatement(stmt);
			stmt = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("StatementCallback", sql, ex);
		}
		finally {
			if (closeResources) {
				JdbcUtils.closeStatement(stmt);
				DataSourceUtils.releaseConnection(con, getDataSource());
			}
		}
	}

	@Override
	@Nullable
	public <T> T execute(StatementCallback<T> action) throws DataAccessException {
		return execute(action, true);
	}

	@Override
	public void execute(final String sql) throws DataAccessException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL statement [" + sql + "]");
		}

		/**
		 * 用于执行语句的回调。
		 */
		class ExecuteStatementCallback implements StatementCallback<Object>, SqlProvider {
			@Override
			@Nullable
			public Object doInStatement(Statement stmt) throws SQLException {
				stmt.execute(sql);
				return null;
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		execute(new ExecuteStatementCallback(), true);
	}

	@Override
	@Nullable
	public <T> T query(final String sql, final ResultSetExtractor<T> rse) throws DataAccessException {
		Assert.notNull(sql, "SQL must not be null");
		Assert.notNull(rse, "ResultSetExtractor must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL query [" + sql + "]");
		}

		/**
		 * 用于执行查询的回调。
		 */
		class QueryStatementCallback implements StatementCallback<T>, SqlProvider {
			@Override
			@Nullable
			public T doInStatement(Statement stmt) throws SQLException {
				ResultSet rs = null;
				try {
					rs = stmt.executeQuery(sql);
					return rse.extractData(rs);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
				}
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		return execute(new QueryStatementCallback(), true);
	}

	@Override
	public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
		query(sql, new RowCallbackHandlerResultSetExtractor(rch));
	}

	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		class StreamStatementCallback implements StatementCallback<Stream<T>>, SqlProvider {
			@Override
			public Stream<T> doInStatement(Statement stmt) throws SQLException {
				ResultSet rs = stmt.executeQuery(sql);
				Connection con = stmt.getConnection();
				return new ResultSetSpliterator<>(rs, rowMapper).stream().onClose(() -> {
					JdbcUtils.closeResultSet(rs);
					JdbcUtils.closeStatement(stmt);
					DataSourceUtils.releaseConnection(con, getDataSource());
				});
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		return result(execute(new StreamStatementCallback(), false));
	}

	@Override
	public Map<String, Object> queryForMap(String sql) throws DataAccessException {
		return result(queryForObject(sql, getColumnMapRowMapper()));
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		List<T> results = query(sql, rowMapper);
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
		return queryForObject(sql, getSingleColumnRowMapper(requiredType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException {
		return query(sql, getSingleColumnRowMapper(elementType));
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql) throws DataAccessException {
		return query(sql, getColumnMapRowMapper());
	}

	@Override
	public SqlRowSet queryForRowSet(String sql) throws DataAccessException {
		return result(query(sql, new SqlRowSetResultSetExtractor()));
	}

	@Override
	public int update(final String sql) throws DataAccessException {
		Assert.notNull(sql, "SQL must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL update [" + sql + "]");
		}

		/**
		 * 用于执行更新语句的回调。
		 */
		class UpdateStatementCallback implements StatementCallback<Integer>, SqlProvider {
			@Override
			public Integer doInStatement(Statement stmt) throws SQLException {
				int rows = stmt.executeUpdate(sql);
				if (logger.isTraceEnabled()) {
					logger.trace("SQL update affected " + rows + " rows");
				}
				return rows;
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		return updateCount(execute(new UpdateStatementCallback(), true));
	}

	@Override
	public int[] batchUpdate(final String... sql) throws DataAccessException {
		Assert.notEmpty(sql, "SQL array must not be empty");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update of " + sql.length + " statements");
		}

		/**
		 * 用于执行批量更新的回调。
		 */
		class BatchUpdateStatementCallback implements StatementCallback<int[]>, SqlProvider {

			@Nullable
			private String currSql;

			@Override
			public int[] doInStatement(Statement stmt) throws SQLException, DataAccessException {
				int[] rowsAffected = new int[sql.length];
				if (JdbcUtils.supportsBatchUpdates(stmt.getConnection())) {
					for (String sqlStmt : sql) {
						this.currSql = appendSql(this.currSql, sqlStmt);
						stmt.addBatch(sqlStmt);
					}
					try {
						rowsAffected = stmt.executeBatch();
					}
					catch (BatchUpdateException ex) {
						String batchExceptionSql = null;
						for (int i = 0; i < ex.getUpdateCounts().length; i++) {
							if (ex.getUpdateCounts()[i] == Statement.EXECUTE_FAILED) {
								batchExceptionSql = appendSql(batchExceptionSql, sql[i]);
							}
						}
						if (StringUtils.hasLength(batchExceptionSql)) {
							this.currSql = batchExceptionSql;
						}
						throw ex;
					}
				}
				else {
					for (int i = 0; i < sql.length; i++) {
						this.currSql = sql[i];
						if (!stmt.execute(sql[i])) {
							rowsAffected[i] = stmt.getUpdateCount();
						}
						else {
							throw new InvalidDataAccessApiUsageException("Invalid batch SQL statement: " + sql[i]);
						}
					}
				}
				return rowsAffected;
			}

			private String appendSql(@Nullable String sql, String statement) {
				return (StringUtils.hasLength(sql) ? sql + "; " + statement : statement);
			}

			@Override
			@Nullable
			public String getSql() {
				return this.currSql;
			}
		}

		int[] result = execute(new BatchUpdateStatementCallback(), true);
		Assert.state(result != null, "No update counts");
		return result;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with prepared statements
	//-------------------------------------------------------------------------

	@Nullable
	private <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action, boolean closeResources)
			throws DataAccessException {

		Assert.notNull(psc, "PreparedStatementCreator must not be null");
		Assert.notNull(action, "Callback object must not be null");
		if (logger.isDebugEnabled()) {
			String sql = getSql(psc);
			logger.debug("Executing prepared SQL statement" + (sql != null ? " [" + sql + "]" : ""));
		}

		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		PreparedStatement ps = null;
		try {
			ps = psc.createPreparedStatement(con);
			applyStatementSettings(ps);
			T result = action.doInPreparedStatement(ps);
			handleWarnings(ps);
			return result;
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			if (psc instanceof ParameterDisposer) {
				((ParameterDisposer) psc).cleanupParameters();
			}
			String sql = getSql(psc);
			psc = null;
			JdbcUtils.closeStatement(ps);
			ps = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("PreparedStatementCallback", sql, ex);
		}
		finally {
			if (closeResources) {
				if (psc instanceof ParameterDisposer) {
					((ParameterDisposer) psc).cleanupParameters();
				}
				JdbcUtils.closeStatement(ps);
				DataSourceUtils.releaseConnection(con, getDataSource());
			}
		}
	}

	@Override
	@Nullable
	public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action)
			throws DataAccessException {

		return execute(psc, action, true);
	}

	@Override
	@Nullable
	public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
		return execute(new SimplePreparedStatementCreator(sql), action, true);
	}

	/**
	 * 使用预编译语句执行查询，允许传入 PreparedStatementCreator
	 * 和 PreparedStatementSetter。大多数其他查询方法都使用此方法，
	 * 但应用程序代码始终只会使用 creator 或 setter 之一。
	 * @param psc 根据 Connection 创建 PreparedStatement 的回调
	 * @param pss 知道如何在 PreparedStatement 上设置值的回调。
	 * 如果此参数为 {@code null}，则假定该 SQL 不包含绑定参数。
	 * @param rse 提取结果的回调
	 * @return 任意结果对象，由 ResultSetExtractor 返回
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	public <T> T query(
			PreparedStatementCreator psc, @Nullable final PreparedStatementSetter pss, final ResultSetExtractor<T> rse)
			throws DataAccessException {

		Assert.notNull(rse, "ResultSetExtractor must not be null");
		logger.debug("Executing prepared SQL query");

		return execute(psc, new PreparedStatementCallback<T>() {
			@Override
			@Nullable
			public T doInPreparedStatement(PreparedStatement ps) throws SQLException {
				ResultSet rs = null;
				try {
					if (pss != null) {
						pss.setValues(ps);
					}
					rs = ps.executeQuery();
					return rse.extractData(rs);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
					if (pss instanceof ParameterDisposer) {
						((ParameterDisposer) pss).cleanupParameters();
					}
				}
			}
		}, true);
	}

	@Override
	@Nullable
	public <T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(psc, null, rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, @Nullable PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(new SimplePreparedStatementCreator(sql), pss, rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, newArgTypePreparedStatementSetter(args, argTypes), rse);
	}

	@Deprecated
	@Override
	@Nullable
	public <T> T query(String sql, @Nullable Object[] args, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, newArgPreparedStatementSetter(args), rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, ResultSetExtractor<T> rse, @Nullable Object... args) throws DataAccessException {
		return query(sql, newArgPreparedStatementSetter(args), rse);
	}

	@Override
	public void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException {
		query(psc, new RowCallbackHandlerResultSetExtractor(rch));
	}

	@Override
	public void query(String sql, @Nullable PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException {
		query(sql, pss, new RowCallbackHandlerResultSetExtractor(rch));
	}

	@Override
	public void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException {
		query(sql, newArgTypePreparedStatementSetter(args, argTypes), rch);
	}

	@Deprecated
	@Override
	public void query(String sql, @Nullable Object[] args, RowCallbackHandler rch) throws DataAccessException {
		query(sql, newArgPreparedStatementSetter(args), rch);
	}

	@Override
	public void query(String sql, RowCallbackHandler rch, @Nullable Object... args) throws DataAccessException {
		query(sql, newArgPreparedStatementSetter(args), rch);
	}

	@Override
	public <T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(psc, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Override
	public <T> List<T> query(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, pss, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Override
	public <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, args, argTypes, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Deprecated
	@Override
	public <T> List<T> query(String sql, @Nullable Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, args, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
		return result(query(sql, args, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	/**
	 * 使用预编译语句进行查询，允许使用 PreparedStatementCreator
	 * 和 PreparedStatementSetter。大多数其他查询方法都使用此方法，
	 * 但应用程序代码通常只使用 creator 或 setter 之一。
	 * @param psc 根据 Connection 创建 PreparedStatement 的回调
	 * @param pss 知道如何在预编译语句上设置值的回调。
	 * 如果为 {@code null}，则假定 SQL 不包含绑定参数。
	 * @param rowMapper 将每行映射为一个对象的回调
	 * @return 结果 Stream，包含映射后的对象，在处理完成后需要关闭
	 * （例如通过 try-with-resources 子句）
	 * @throws DataAccessException 如果查询失败
	 * @since 5.3
	 */
	public <T> Stream<T> queryForStream(PreparedStatementCreator psc, @Nullable PreparedStatementSetter pss,
			RowMapper<T> rowMapper) throws DataAccessException {

		return result(execute(psc, ps -> {
			if (pss != null) {
				pss.setValues(ps);
			}
			ResultSet rs = ps.executeQuery();
			Connection con = ps.getConnection();
			return new ResultSetSpliterator<>(rs, rowMapper).stream().onClose(() -> {
				JdbcUtils.closeResultSet(rs);
				if (pss instanceof ParameterDisposer) {
					((ParameterDisposer) pss).cleanupParameters();
				}
				JdbcUtils.closeStatement(ps);
				DataSourceUtils.releaseConnection(con, getDataSource());
			});
		}, false));
	}

	@Override
	public <T> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
		return queryForStream(psc, null, rowMapper);
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
		return queryForStream(new SimplePreparedStatementCreator(sql), pss, rowMapper);
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
		return queryForStream(new SimplePreparedStatementCreator(sql), newArgPreparedStatementSetter(args), rowMapper);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
			throws DataAccessException {

		List<T> results = query(sql, args, argTypes, new RowMapperResultSetExtractor<>(rowMapper, 1));
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Deprecated
	@Override
	@Nullable
	public <T> T queryForObject(String sql, @Nullable Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
		List<T> results = query(sql, args, new RowMapperResultSetExtractor<>(rowMapper, 1));
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
		List<T> results = query(sql, args, new RowMapperResultSetExtractor<>(rowMapper, 1));
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType)
			throws DataAccessException {

		return queryForObject(sql, args, argTypes, getSingleColumnRowMapper(requiredType));
	}

	@Deprecated
	@Override
	public <T> T queryForObject(String sql, @Nullable Object[] args, Class<T> requiredType) throws DataAccessException {
		return queryForObject(sql, args, getSingleColumnRowMapper(requiredType));
	}

	@Override
	public <T> T queryForObject(String sql, Class<T> requiredType, @Nullable Object... args) throws DataAccessException {
		return queryForObject(sql, args, getSingleColumnRowMapper(requiredType));
	}

	@Override
	public Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return result(queryForObject(sql, args, argTypes, getColumnMapRowMapper()));
	}

	@Override
	public Map<String, Object> queryForMap(String sql, @Nullable Object... args) throws DataAccessException {
		return result(queryForObject(sql, args, getColumnMapRowMapper()));
	}

	@Override
	public <T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType) throws DataAccessException {
		return query(sql, args, argTypes, getSingleColumnRowMapper(elementType));
	}

	@Deprecated
	@Override
	public <T> List<T> queryForList(String sql, @Nullable Object[] args, Class<T> elementType) throws DataAccessException {
		return query(sql, args, getSingleColumnRowMapper(elementType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Class<T> elementType, @Nullable Object... args) throws DataAccessException {
		return query(sql, args, getSingleColumnRowMapper(elementType));
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return query(sql, args, argTypes, getColumnMapRowMapper());
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, @Nullable Object... args) throws DataAccessException {
		return query(sql, args, getColumnMapRowMapper());
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return result(query(sql, args, argTypes, new SqlRowSetResultSetExtractor()));
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, @Nullable Object... args) throws DataAccessException {
		return result(query(sql, args, new SqlRowSetResultSetExtractor()));
	}

	protected int update(final PreparedStatementCreator psc, @Nullable final PreparedStatementSetter pss)
			throws DataAccessException {

		logger.debug("Executing prepared SQL update");

		return updateCount(execute(psc, ps -> {
			try {
				if (pss != null) {
					pss.setValues(ps);
				}
				int rows = ps.executeUpdate();
				if (logger.isTraceEnabled()) {
					logger.trace("SQL update affected " + rows + " rows");
				}
				return rows;
			}
			finally {
				if (pss instanceof ParameterDisposer) {
					((ParameterDisposer) pss).cleanupParameters();
				}
			}
		}, true));
	}

	@Override
	public int update(PreparedStatementCreator psc) throws DataAccessException {
		return update(psc, (PreparedStatementSetter) null);
	}

	@Override
	public int update(final PreparedStatementCreator psc, final KeyHolder generatedKeyHolder)
			throws DataAccessException {

		Assert.notNull(generatedKeyHolder, "KeyHolder must not be null");
		logger.debug("Executing SQL update and returning generated keys");

		return updateCount(execute(psc, ps -> {
			int rows = ps.executeUpdate();
			List<Map<String, Object>> generatedKeys = generatedKeyHolder.getKeyList();
			generatedKeys.clear();
			ResultSet keys = ps.getGeneratedKeys();
			if (keys != null) {
				try {
					RowMapperResultSetExtractor<Map<String, Object>> rse =
							new RowMapperResultSetExtractor<>(getColumnMapRowMapper(), 1);
					generatedKeys.addAll(result(rse.extractData(keys)));
				}
				finally {
					JdbcUtils.closeResultSet(keys);
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("SQL update affected " + rows + " rows and returned " + generatedKeys.size() + " keys");
			}
			return rows;
		}, true));
	}

	@Override
	public int update(String sql, @Nullable PreparedStatementSetter pss) throws DataAccessException {
		return update(new SimplePreparedStatementCreator(sql), pss);
	}

	@Override
	public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return update(sql, newArgTypePreparedStatementSetter(args, argTypes));
	}

	@Override
	public int update(String sql, @Nullable Object... args) throws DataAccessException {
		return update(sql, newArgPreparedStatementSetter(args));
	}

	@Override
	public int[] batchUpdate(String sql, final BatchPreparedStatementSetter pss) throws DataAccessException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update [" + sql + "]");
		}

		int[] result = execute(sql, (PreparedStatementCallback<int[]>) ps -> {
			try {
				int batchSize = pss.getBatchSize();
				InterruptibleBatchPreparedStatementSetter ipss =
						(pss instanceof InterruptibleBatchPreparedStatementSetter ?
						(InterruptibleBatchPreparedStatementSetter) pss : null);
				if (JdbcUtils.supportsBatchUpdates(ps.getConnection())) {
					for (int i = 0; i < batchSize; i++) {
						pss.setValues(ps, i);
						if (ipss != null && ipss.isBatchExhausted(i)) {
							break;
						}
						ps.addBatch();
					}
					return ps.executeBatch();
				}
				else {
					List<Integer> rowsAffected = new ArrayList<>();
					for (int i = 0; i < batchSize; i++) {
						pss.setValues(ps, i);
						if (ipss != null && ipss.isBatchExhausted(i)) {
							break;
						}
						rowsAffected.add(ps.executeUpdate());
					}
					int[] rowsAffectedArray = new int[rowsAffected.size()];
					for (int i = 0; i < rowsAffectedArray.length; i++) {
						rowsAffectedArray[i] = rowsAffected.get(i);
					}
					return rowsAffectedArray;
				}
			}
			finally {
				if (pss instanceof ParameterDisposer) {
					((ParameterDisposer) pss).cleanupParameters();
				}
			}
		});

		Assert.state(result != null, "No result array");
		return result;
	}

	@Override
	public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
		return batchUpdate(sql, batchArgs, new int[0]);
	}

	@Override
	public int[] batchUpdate(String sql, List<Object[]> batchArgs, final int[] argTypes) throws DataAccessException {
		if (batchArgs.isEmpty()) {
			return new int[0];
		}

		return batchUpdate(
				sql,
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Object[] values = batchArgs.get(i);
						int colIndex = 0;
						for (Object value : values) {
							colIndex++;
							if (value instanceof SqlParameterValue) {
								SqlParameterValue paramValue = (SqlParameterValue) value;
								StatementCreatorUtils.setParameterValue(ps, colIndex, paramValue, paramValue.getValue());
							}
							else {
								int colType;
								if (argTypes.length < colIndex) {
									colType = SqlTypeValue.TYPE_UNKNOWN;
								}
								else {
									colType = argTypes[colIndex - 1];
								}
								StatementCreatorUtils.setParameterValue(ps, colIndex, colType, value);
							}
						}
					}
					@Override
					public int getBatchSize() {
						return batchArgs.size();
					}
				});
	}

	@Override
	public <T> int[][] batchUpdate(String sql, final Collection<T> batchArgs, final int batchSize,
			final ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {

		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update [" + sql + "] with a batch size of " + batchSize);
		}
		int[][] result = execute(sql, (PreparedStatementCallback<int[][]>) ps -> {
			List<int[]> rowsAffected = new ArrayList<>();
			try {
				boolean batchSupported = JdbcUtils.supportsBatchUpdates(ps.getConnection());
				int n = 0;
				for (T obj : batchArgs) {
					pss.setValues(ps, obj);
					n++;
					if (batchSupported) {
						ps.addBatch();
						if (n % batchSize == 0 || n == batchArgs.size()) {
							if (logger.isTraceEnabled()) {
								int batchIdx = (n % batchSize == 0) ? n / batchSize : (n / batchSize) + 1;
								int items = n - ((n % batchSize == 0) ? n / batchSize - 1 : (n / batchSize)) * batchSize;
								logger.trace("Sending SQL batch update #" + batchIdx + " with " + items + " items");
							}
							rowsAffected.add(ps.executeBatch());
						}
					}
					else {
						int i = ps.executeUpdate();
						rowsAffected.add(new int[] {i});
					}
				}
				int[][] result1 = new int[rowsAffected.size()][];
				for (int i = 0; i < result1.length; i++) {
					result1[i] = rowsAffected.get(i);
				}
				return result1;
			}
			finally {
				if (pss instanceof ParameterDisposer) {
					((ParameterDisposer) pss).cleanupParameters();
				}
			}
		});

		Assert.state(result != null, "No result array");
		return result;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with callable statements
	//-------------------------------------------------------------------------

	@Override
	@Nullable
	public <T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action)
			throws DataAccessException {

		Assert.notNull(csc, "CallableStatementCreator must not be null");
		Assert.notNull(action, "Callback object must not be null");
		if (logger.isDebugEnabled()) {
			String sql = getSql(csc);
			logger.debug("Calling stored procedure" + (sql != null ? " [" + sql  + "]" : ""));
		}

		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		CallableStatement cs = null;
		try {
			cs = csc.createCallableStatement(con);
			applyStatementSettings(cs);
			T result = action.doInCallableStatement(cs);
			handleWarnings(cs);
			return result;
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			if (csc instanceof ParameterDisposer) {
				((ParameterDisposer) csc).cleanupParameters();
			}
			String sql = getSql(csc);
			csc = null;
			JdbcUtils.closeStatement(cs);
			cs = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("CallableStatementCallback", sql, ex);
		}
		finally {
			if (csc instanceof ParameterDisposer) {
				((ParameterDisposer) csc).cleanupParameters();
			}
			JdbcUtils.closeStatement(cs);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	@Override
	@Nullable
	public <T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException {
		return execute(new SimpleCallableStatementCreator(callString), action);
	}

	@Override
	public Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
			throws DataAccessException {

		final List<SqlParameter> updateCountParameters = new ArrayList<>();
		final List<SqlParameter> resultSetParameters = new ArrayList<>();
		final List<SqlParameter> callParameters = new ArrayList<>();

		for (SqlParameter parameter : declaredParameters) {
			if (parameter.isResultsParameter()) {
				if (parameter instanceof SqlReturnResultSet) {
					resultSetParameters.add(parameter);
				}
				else {
					updateCountParameters.add(parameter);
				}
			}
			else {
				callParameters.add(parameter);
			}
		}

		Map<String, Object> result = execute(csc, cs -> {
			boolean retVal = cs.execute();
			int updateCount = cs.getUpdateCount();
			if (logger.isTraceEnabled()) {
				logger.trace("CallableStatement.execute() returned '" + retVal + "'");
				logger.trace("CallableStatement.getUpdateCount() returned " + updateCount);
			}
			Map<String, Object> resultsMap = createResultsMap();
			if (retVal || updateCount != -1) {
				resultsMap.putAll(extractReturnedResults(cs, updateCountParameters, resultSetParameters, updateCount));
			}
			resultsMap.putAll(extractOutputParameters(cs, callParameters));
			return resultsMap;
		});

		Assert.state(result != null, "No result map");
		return result;
	}

	/**
	 * 从已完成的存储过程中提取返回的 ResultSet。
	 * @param cs 存储过程的 JDBC 包装器
	 * @param updateCountParameters 存储过程声明的更新计数参数列表
	 * @param resultSetParameters 存储过程声明的 resultSet 参数列表
	 * @return 包含返回结果的 Map
	 */
	protected Map<String, Object> extractReturnedResults(CallableStatement cs,
			@Nullable List<SqlParameter> updateCountParameters, @Nullable List<SqlParameter> resultSetParameters,
			int updateCount) throws SQLException {

		Map<String, Object> results = new LinkedHashMap<>(4);
		int rsIndex = 0;
		int updateIndex = 0;
		boolean moreResults;
		if (!this.skipResultsProcessing) {
			do {
				if (updateCount == -1) {
					if (resultSetParameters != null && resultSetParameters.size() > rsIndex) {
						SqlReturnResultSet declaredRsParam = (SqlReturnResultSet) resultSetParameters.get(rsIndex);
						results.putAll(processResultSet(cs.getResultSet(), declaredRsParam));
						rsIndex++;
					}
					else {
						if (!this.skipUndeclaredResults) {
							String rsName = RETURN_RESULT_SET_PREFIX + (rsIndex + 1);
							SqlReturnResultSet undeclaredRsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
							if (logger.isTraceEnabled()) {
								logger.trace("Added default SqlReturnResultSet parameter named '" + rsName + "'");
							}
							results.putAll(processResultSet(cs.getResultSet(), undeclaredRsParam));
							rsIndex++;
						}
					}
				}
				else {
					if (updateCountParameters != null && updateCountParameters.size() > updateIndex) {
						SqlReturnUpdateCount ucParam = (SqlReturnUpdateCount) updateCountParameters.get(updateIndex);
						String declaredUcName = ucParam.getName();
						results.put(declaredUcName, updateCount);
						updateIndex++;
					}
					else {
						if (!this.skipUndeclaredResults) {
							String undeclaredName = RETURN_UPDATE_COUNT_PREFIX + (updateIndex + 1);
							if (logger.isTraceEnabled()) {
								logger.trace("Added default SqlReturnUpdateCount parameter named '" + undeclaredName + "'");
							}
							results.put(undeclaredName, updateCount);
							updateIndex++;
						}
					}
				}
				moreResults = cs.getMoreResults();
				updateCount = cs.getUpdateCount();
				if (logger.isTraceEnabled()) {
					logger.trace("CallableStatement.getUpdateCount() returned " + updateCount);
				}
			}
			while (moreResults || updateCount != -1);
		}
		return results;
	}

	/**
	 * 从已完成的存储过程中提取输出参数。
	 * @param cs 存储过程的 JDBC 包装器
	 * @param parameters 存储过程的参数列表
	 * @return 包含返回结果的 Map
	 */
	protected Map<String, Object> extractOutputParameters(CallableStatement cs, List<SqlParameter> parameters)
			throws SQLException {

		Map<String, Object> results = CollectionUtils.newLinkedHashMap(parameters.size());
		int sqlColIndex = 1;
		for (SqlParameter param : parameters) {
			if (param instanceof SqlOutParameter) {
				SqlOutParameter outParam = (SqlOutParameter) param;
				Assert.state(outParam.getName() != null, "Anonymous parameters not allowed");
				SqlReturnType returnType = outParam.getSqlReturnType();
				if (returnType != null) {
					Object out = returnType.getTypeValue(cs, sqlColIndex, outParam.getSqlType(), outParam.getTypeName());
					results.put(outParam.getName(), out);
				}
				else {
					Object out = cs.getObject(sqlColIndex);
					if (out instanceof ResultSet) {
						if (outParam.isResultSetSupported()) {
							results.putAll(processResultSet((ResultSet) out, outParam));
						}
						else {
							String rsName = outParam.getName();
							SqlReturnResultSet rsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
							results.putAll(processResultSet((ResultSet) out, rsParam));
							if (logger.isTraceEnabled()) {
								logger.trace("Added default SqlReturnResultSet parameter named '" + rsName + "'");
							}
						}
					}
					else {
						results.put(outParam.getName(), out);
					}
				}
			}
			if (!(param.isResultsParameter())) {
				sqlColIndex++;
			}
		}
		return results;
	}

	/**
	 * 处理存储过程返回的给定 ResultSet。
	 * @param rs 要处理的 ResultSet
	 * @param param 对应的存储过程参数
	 * @return 包含返回结果的 Map
	 */
	protected Map<String, Object> processResultSet(
			@Nullable ResultSet rs, ResultSetSupportingSqlParameter param) throws SQLException {

		if (rs != null) {
			try {
				if (param.getRowMapper() != null) {
					RowMapper<?> rowMapper = param.getRowMapper();
					Object data = (new RowMapperResultSetExtractor<>(rowMapper)).extractData(rs);
					return Collections.singletonMap(param.getName(), data);
				}
				else if (param.getRowCallbackHandler() != null) {
					RowCallbackHandler rch = param.getRowCallbackHandler();
					(new RowCallbackHandlerResultSetExtractor(rch)).extractData(rs);
					return Collections.singletonMap(param.getName(),
							"ResultSet returned from stored procedure was processed");
				}
				else if (param.getResultSetExtractor() != null) {
					Object data = param.getResultSetExtractor().extractData(rs);
					return Collections.singletonMap(param.getName(), data);
				}
			}
			finally {
				JdbcUtils.closeResultSet(rs);
			}
		}
		return Collections.emptyMap();
	}


	//-------------------------------------------------------------------------
	// Implementation hooks and helper methods
	//-------------------------------------------------------------------------

	/**
	 * 创建一个新的 RowMapper，用于将列读取为键值对。
	 * @return 要使用的 RowMapper
	 * @see ColumnMapRowMapper
	 */
	protected RowMapper<Map<String, Object>> getColumnMapRowMapper() {
		return new ColumnMapRowMapper();
	}

	/**
	 * 创建一个新的 RowMapper，用于从单列中读取结果对象。
	 * @param requiredType 每个结果对象应匹配的类型
	 * @return 要使用的 RowMapper
	 * @see SingleColumnRowMapper
	 */
	protected <T> RowMapper<T> getSingleColumnRowMapper(Class<T> requiredType) {
		return new SingleColumnRowMapper<>(requiredType);
	}

	/**
	 * 创建一个用作结果 Map 的 Map 实例。
	 * <p>如果 {@link #resultsMapCaseInsensitive} 已被设置为 true，
	 * 则创建 {@link LinkedCaseInsensitiveMap}；否则创建
	 * {@link LinkedHashMap}。
	 * @return 结果 Map 实例
	 * @see #setResultsMapCaseInsensitive
	 * @see #isResultsMapCaseInsensitive
	 */
	protected Map<String, Object> createResultsMap() {
		if (isResultsMapCaseInsensitive()) {
			return new LinkedCaseInsensitiveMap<>();
		}
		else {
			return new LinkedHashMap<>();
		}
	}

	/**
	 * 准备给定的 JDBC Statement（或 PreparedStatement、CallableStatement），
	 * 并应用 fetch size、max rows 和查询超时等语句设置。
	 * @param stmt 要准备的 JDBC Statement
	 * @throws SQLException 如果由 JDBC API 抛出
	 * @see #setFetchSize
	 * @see #setMaxRows
	 * @see #setQueryTimeout
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
	 */
	protected void applyStatementSettings(Statement stmt) throws SQLException {
		int fetchSize = getFetchSize();
		if (fetchSize != -1) {
			stmt.setFetchSize(fetchSize);
		}
		int maxRows = getMaxRows();
		if (maxRows != -1) {
			stmt.setMaxRows(maxRows);
		}
		DataSourceUtils.applyTimeout(stmt, getDataSource(), getQueryTimeout());
	}

	/**
	 * 使用传入的参数创建一个基于参数的 PreparedStatementSetter。
	 * <p>默认情况下，我们会创建 {@link ArgumentPreparedStatementSetter}。
	 * 此方法允许子类覆盖创建过程。
	 * @param args 包含参数的对象数组
	 * @return 要使用的新 PreparedStatementSetter
	 */
	protected PreparedStatementSetter newArgPreparedStatementSetter(@Nullable Object[] args) {
		return new ArgumentPreparedStatementSetter(args);
	}

	/**
	 * 使用传入的参数和类型创建一个基于参数类型的 PreparedStatementSetter。
	 * <p>默认情况下，我们会创建 {@link ArgumentTypePreparedStatementSetter}。
	 * 此方法允许子类覆盖创建过程。
	 * @param args 包含参数的对象数组
	 * @param argTypes 相关参数的 SQLTypes int 数组
	 * @return 要使用的新 PreparedStatementSetter
	 */
	protected PreparedStatementSetter newArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
		return new ArgumentTypePreparedStatementSetter(args, argTypes);
	}

	/**
	 * 如果未忽略警告，则抛出 SQLWarningException，
	 * 否则以 debug 级别记录这些警告。
	 * @param stmt 当前的 JDBC 语句
	 * @throws SQLWarningException 如果不忽略警告
	 * @see org.springframework.jdbc.SQLWarningException
	 */
	protected void handleWarnings(Statement stmt) throws SQLException {
		if (isIgnoreWarnings()) {
			if (logger.isDebugEnabled()) {
				SQLWarning warningToLog = stmt.getWarnings();
				while (warningToLog != null) {
					logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '" +
							warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
					warningToLog = warningToLog.getNextWarning();
				}
			}
		}
		else {
			handleWarnings(stmt.getWarnings());
		}
	}

	/**
	 * 如果遇到实际警告，则抛出 SQLWarningException。
	 * @param warning 当前语句的警告对象。
	 * 可以为 {@code null}，此时本方法不执行任何操作。
	 * @throws SQLWarningException 在需要抛出实际警告的情况下
	 */
	protected void handleWarnings(@Nullable SQLWarning warning) throws SQLWarningException {
		if (warning != null) {
			throw new SQLWarningException("Warning not ignored", warning);
		}
	}

	/**
	 * 将给定的 {@link SQLException} 转换为通用的 {@link DataAccessException}。
	 * @param task 描述所尝试任务的可读文本
	 * @param sql 导致问题的 SQL 查询或更新（可以为 {@code null}）
	 * @param ex 出错的 {@code SQLException}
	 * @return 包装该 {@code SQLException} 的 DataAccessException（永不为 {@code null}）
	 * @since 5.0
	 * @see #getExceptionTranslator()
	 */
	protected DataAccessException translateException(String task, @Nullable String sql, SQLException ex) {
		DataAccessException dae = getExceptionTranslator().translate(task, sql, ex);
		return (dae != null ? dae : new UncategorizedSQLException(task, sql, ex));
	}


	/**
	 * 从可能的 provider 对象中确定 SQL。
	 * @param sqlProvider 可能是 SqlProvider 的对象
	 * @return SQL 字符串，如果未知则为 {@code null}
	 * @see SqlProvider
	 */
	@Nullable
	private static String getSql(Object sqlProvider) {
		if (sqlProvider instanceof SqlProvider) {
			return ((SqlProvider) sqlProvider).getSql();
		}
		else {
			return null;
		}
	}

	private static <T> T result(@Nullable T result) {
		Assert.state(result != null, "No result");
		return result;
	}

	private static int updateCount(@Nullable Integer result) {
		Assert.state(result != null, "No update count");
		return result;
	}


	/**
	 * 调用处理器，用于抑制对 JDBC Connection 的 close 调用。
	 * 同时还会准备返回的 Statement（PreparedStatement/CallableStatement）对象。
	 * @see java.sql.Connection#close()
	 */
	private class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		public CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of PersistenceManager proxy.
					return System.identityHashCode(proxy);
				case "close":
					// Handle close method: suppress, not valid.
					return null;
				case "isClosed":
					return false;
				case "getTargetConnection":
					// Handle getTargetConnection method: return underlying Connection.
					return this.target;
				case "unwrap":
					return (((Class<?>) args[0]).isInstance(proxy) ? proxy : this.target.unwrap((Class<?>) args[0]));
				case "isWrapperFor":
					return (((Class<?>) args[0]).isInstance(proxy) || this.target.isWrapperFor((Class<?>) args[0]));
			}

			// Invoke method on target Connection.
			try {
				Object retVal = method.invoke(this.target, args);

				// If return value is a JDBC Statement, apply statement settings
				// (fetch size, max rows, transaction timeout).
				if (retVal instanceof Statement) {
					applyStatementSettings(((Statement) retVal));
				}

				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * PreparedStatementCreator 的简单适配器，允许使用普通的 SQL 语句。
	 */
	private static class SimplePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		private final String sql;

		public SimplePreparedStatementCreator(String sql) {
			Assert.notNull(sql, "SQL must not be null");
			this.sql = sql;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			return con.prepareStatement(this.sql);
		}

		@Override
		public String getSql() {
			return this.sql;
		}
	}


	/**
	 * CallableStatementCreator 的简单适配器，允许使用普通的 SQL 语句。
	 */
	private static class SimpleCallableStatementCreator implements CallableStatementCreator, SqlProvider {

		private final String callString;

		public SimpleCallableStatementCreator(String callString) {
			Assert.notNull(callString, "Call string must not be null");
			this.callString = callString;
		}

		@Override
		public CallableStatement createCallableStatement(Connection con) throws SQLException {
			return con.prepareCall(this.callString);
		}

		@Override
		public String getSql() {
			return this.callString;
		}
	}


	/**
	 * 适配器，用于在 ResultSetExtractor 内部使用 RowCallbackHandler。
	 * <p>它使用常规的 ResultSet，因此使用时必须小心：
	 * 我们不会用它进行导航，因为这可能导致不可预测的后果。
	 */
	private static class RowCallbackHandlerResultSetExtractor implements ResultSetExtractor<Object> {

		private final RowCallbackHandler rch;

		public RowCallbackHandlerResultSetExtractor(RowCallbackHandler rch) {
			this.rch = rch;
		}

		@Override
		@Nullable
		public Object extractData(ResultSet rs) throws SQLException {
			while (rs.next()) {
				this.rch.processRow(rs);
			}
			return null;
		}
	}


	/**
	 * 用于 queryForStream 的 Spliterator，将 ResultSet 适配为 Stream。
	 * @since 5.3
	 */
	private static class ResultSetSpliterator<T> implements Spliterator<T> {

		private final ResultSet rs;

		private final RowMapper<T> rowMapper;

		private int rowNum = 0;

		public ResultSetSpliterator(ResultSet rs, RowMapper<T> rowMapper) {
			this.rs = rs;
			this.rowMapper = rowMapper;
		}

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			try {
				if (this.rs.next()) {
					action.accept(this.rowMapper.mapRow(this.rs, this.rowNum++));
					return true;
				}
				return false;
			}
			catch (SQLException ex) {
				throw new InvalidResultSetAccessException(ex);
			}
		}

		@Override
		@Nullable
		public Spliterator<T> trySplit() {
			return null;
		}

		@Override
		public long estimateSize() {
			return Long.MAX_VALUE;
		}

		@Override
		public int characteristics() {
			return Spliterator.ORDERED;
		}

		public Stream<T> stream() {
			return StreamSupport.stream(this, false);
		}
	}

}
