/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jdbc.core.simple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.core.metadata.TableMetaDataContext;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于配置选项和数据库元数据，提供便捷插入操作基础功能的抽象类。
 *
 * <p>该类为 {@link SimpleJdbcInsert} 提供基础 SPI。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public abstract class AbstractJdbcInsert {

	/** 子类可用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 用于执行 SQL 的底层类。 */
	private final JdbcTemplate jdbcTemplate;

	/** 用于检索和管理数据库元数据的上下文。 */
	private final TableMetaDataContext tableMetaDataContext = new TableMetaDataContext();

	/** 用于插入语句的列名列表。 */
	private final List<String> declaredColumns = new ArrayList<>();

	/** 保存生成键的列的名称。 */
	private String[] generatedKeyNames = new String[0];

	/**
	 * 此操作是否已经编译？编译意味着至少检查
	 * 是否已提供 DataSource 或 JdbcTemplate。
	 */
	private volatile boolean compiled;

	/** 用于插入语句的生成字符串。 */
	private String insertString = "";

	/** 插入列的 SQL 类型信息。 */
	private int[] insertTypes = new int[0];


	/**
	 * 使用 {@link DataSource} 进行初始化时使用的构造器。
	 * @param dataSource 要使用的 DataSource
	 */
	protected AbstractJdbcInsert(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * 使用 {@link JdbcTemplate} 进行初始化时使用的构造器。
	 * @param jdbcTemplate 要使用的 JdbcTemplate
	 */
	protected AbstractJdbcInsert(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with configuration properties
	//-------------------------------------------------------------------------

	/**
	 * 获取配置的 {@link JdbcTemplate}。
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * 设置此插入操作使用的表名。
	 */
	public void setTableName(@Nullable String tableName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setTableName(tableName);
	}

	/**
	 * 获取此插入操作使用的表名。
	 */
	@Nullable
	public String getTableName() {
		return this.tableMetaDataContext.getTableName();
	}

	/**
	 * 设置此插入操作使用的 schema 名称。
	 */
	public void setSchemaName(@Nullable String schemaName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setSchemaName(schemaName);
	}

	/**
	 * 获取此插入操作使用的 schema 名称。
	 */
	@Nullable
	public String getSchemaName() {
		return this.tableMetaDataContext.getSchemaName();
	}

	/**
	 * 设置此插入操作使用的 catalog 名称。
	 */
	public void setCatalogName(@Nullable String catalogName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setCatalogName(catalogName);
	}

	/**
	 * 获取此插入操作使用的 catalog 名称。
	 */
	@Nullable
	public String getCatalogName() {
		return this.tableMetaDataContext.getCatalogName();
	}

	/**
	 * 设置要使用的列名。
	 */
	public void setColumnNames(List<String> columnNames) {
		checkIfConfigurationModificationIsAllowed();
		this.declaredColumns.clear();
		this.declaredColumns.addAll(columnNames);
	}

	/**
	 * 获取所使用的列名。
	 */
	public List<String> getColumnNames() {
		return Collections.unmodifiableList(this.declaredColumns);
	}

	/**
	 * 指定单个生成键列的名称。
	 */
	public void setGeneratedKeyName(String generatedKeyName) {
		checkIfConfigurationModificationIsAllowed();
		this.generatedKeyNames = new String[] {generatedKeyName};
	}

	/**
	 * 设置所有生成键的名称。
	 */
	public void setGeneratedKeyNames(String... generatedKeyNames) {
		checkIfConfigurationModificationIsAllowed();
		this.generatedKeyNames = generatedKeyNames;
	}

	/**
	 * 获取所有生成键的名称。
	 */
	public String[] getGeneratedKeyNames() {
		return this.generatedKeyNames;
	}

	/**
	 * 指定是否应使用此调用的参数元数据。
	 * 默认为 {@code true}。
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.tableMetaDataContext.setAccessTableColumnMetaData(accessTableColumnMetaData);
	}

	/**
	 * 指定是否应更改包含同义词的默认设置。
	 * 默认为 {@code false}。
	 */
	public void setOverrideIncludeSynonymsDefault(boolean override) {
		this.tableMetaDataContext.setOverrideIncludeSynonymsDefault(override);
	}

	/**
	 * 获取要使用的插入字符串。
	 */
	public String getInsertString() {
		return this.insertString;
	}

	/**
	 * 获取插入操作要使用的 {@link java.sql.Types} 数组。
	 */
	public int[] getInsertTypes() {
		return this.insertTypes;
	}


	//-------------------------------------------------------------------------
	// Methods handling compilation issues
	//-------------------------------------------------------------------------

	/**
	 * 使用提供的参数、元数据以及其他设置编译此 JdbcInsert。
	 * 这会最终确定此对象的配置，后续的编译尝试将被忽略。
	 * 首次执行未编译的插入操作时将隐式调用此方法。
	 * @throws InvalidDataAccessApiUsageException 如果该对象尚未正确初始化，
	 * 例如未提供 DataSource
	 */
	public final synchronized void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getTableName() == null) {
				throw new InvalidDataAccessApiUsageException("Table name is required");
			}
			try {
				this.jdbcTemplate.afterPropertiesSet();
			}
			catch (IllegalArgumentException ex) {
				throw new InvalidDataAccessApiUsageException(ex.getMessage());
			}
			compileInternal();
			this.compiled = true;
			if (logger.isDebugEnabled()) {
				logger.debug("JdbcInsert for table [" + getTableName() + "] compiled");
			}
		}
	}

	/**
	 * 执行实际编译的委托方法。
	 * <p>子类可以覆盖此模板方法以执行自己的编译。
	 * 在基类的编译完成之后调用。
	 */
	protected void compileInternal() {
		DataSource dataSource = getJdbcTemplate().getDataSource();
		Assert.state(dataSource != null, "No DataSource set");
		this.tableMetaDataContext.processMetaData(dataSource, getColumnNames(), getGeneratedKeyNames());
		this.insertString = this.tableMetaDataContext.createInsertString(getGeneratedKeyNames());
		this.insertTypes = this.tableMetaDataContext.createInsertTypes();
		if (logger.isDebugEnabled()) {
			logger.debug("Compiled insert object: insert string is [" + this.insertString + "]");
		}
		onCompileInternal();
	}

	/**
	 * 子类可以覆盖此钩子方法以响应编译过程。
	 * <p>此实现为空。
	 */
	protected void onCompileInternal() {
	}

	/**
	 * 此操作是否已“编译”？
	 * @return 此操作是否已编译并可以使用
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * 检查此操作是否已经编译；
	 * 如果尚未编译，则延迟编译。
	 * <p>由所有 {@code doExecute*(...)} 方法自动调用。
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("JdbcInsert not compiled before execution - invoking compile");
			compile();
		}
	}

	/**
	 * 用于检查此时是否允许进行配置更改的方法。
	 * <p>如果该类已经编译，则不允许再对配置进行任何更改。
	 */
	protected void checkIfConfigurationModificationIsAllowed() {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"Configuration cannot be altered once the class has been compiled or used");
		}
	}


	//-------------------------------------------------------------------------
	// Methods handling execution
	//-------------------------------------------------------------------------

	/**
	 * 使用传入的参数 Map 执行插入操作的委托方法。
	 * @param args 包含插入时要使用的参数名称和值的 Map
	 * @return 受影响的行数
	 */
	protected int doExecute(Map<String, ?> args) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(args);
		return executeInsertInternal(values);
	}

	/**
	 * 使用传入的 {@link SqlParameterSource} 执行插入操作的委托方法。
	 * @param parameterSource 插入时要使用的参数名称和值
	 * @return 受影响的行数
	 */
	protected int doExecute(SqlParameterSource parameterSource) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(parameterSource);
		return executeInsertInternal(values);
	}

	/**
	 * 执行插入操作的委托方法。
	 */
	private int executeInsertInternal(List<?> values) {
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for insert " + getInsertString() + " with: " + values);
		}
		return getJdbcTemplate().update(getInsertString(), values.toArray(), getInsertTypes());
	}

	/**
	 * 使用传入的参数 Map 执行插入操作并返回生成键的方法。
	 * @param args 包含插入时要使用的参数名称和值的 Map
	 * @return 插入生成的主键
	 */
	protected Number doExecuteAndReturnKey(Map<String, ?> args) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(args);
		return executeInsertAndReturnKeyInternal(values);
	}

	/**
	 * 使用传入的 {@link SqlParameterSource} 执行插入操作并返回生成键的方法。
	 * @param parameterSource 插入时要使用的参数名称和值
	 * @return 插入生成的主键
	 */
	protected Number doExecuteAndReturnKey(SqlParameterSource parameterSource) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(parameterSource);
		return executeInsertAndReturnKeyInternal(values);
	}

	/**
	 * 使用传入的参数 Map 执行插入操作并返回所有生成键的方法。
	 * @param args 包含插入时要使用的参数名称和值的 Map
	 * @return 包含插入生成的主键的 KeyHolder
	 */
	protected KeyHolder doExecuteAndReturnKeyHolder(Map<String, ?> args) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(args);
		return executeInsertAndReturnKeyHolderInternal(values);
	}

	/**
	 * 使用传入的 {@link SqlParameterSource} 执行插入操作并返回所有生成键的方法。
	 * @param parameterSource 插入时要使用的参数名称和值
	 * @return 包含插入生成的主键的 KeyHolder
	 */
	protected KeyHolder doExecuteAndReturnKeyHolder(SqlParameterSource parameterSource) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(parameterSource);
		return executeInsertAndReturnKeyHolderInternal(values);
	}

	/**
	 * 执行插入操作并生成单个键的委托方法。
	 */
	private Number executeInsertAndReturnKeyInternal(final List<?> values) {
		KeyHolder kh = executeInsertAndReturnKeyHolderInternal(values);
		if (kh.getKey() != null) {
			return kh.getKey();
		}
		else {
			throw new DataIntegrityViolationException(
					"Unable to retrieve the generated key for the insert: " + getInsertString());
		}
	}

	/**
	 * 执行插入操作并生成任意数量键的委托方法。
	 */
	private KeyHolder executeInsertAndReturnKeyHolderInternal(final List<?> values) {
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for call " + getInsertString() + " with: " + values);
		}
		final KeyHolder keyHolder = new GeneratedKeyHolder();

		if (this.tableMetaDataContext.isGetGeneratedKeysSupported()) {
			getJdbcTemplate().update(
					con -> {
						PreparedStatement ps = prepareStatementForGeneratedKeys(con);
						setParameterValues(ps, values, getInsertTypes());
						return ps;
					},
					keyHolder);
		}

		else {
			if (!this.tableMetaDataContext.isGetGeneratedKeysSimulated()) {
				throw new InvalidDataAccessResourceUsageException(
						"The getGeneratedKeys feature is not supported by this database");
			}
			if (getGeneratedKeyNames().length < 1) {
				throw new InvalidDataAccessApiUsageException("Generated Key Name(s) not specified. " +
						"Using the generated keys features requires specifying the name(s) of the generated column(s)");
			}
			if (getGeneratedKeyNames().length > 1) {
				throw new InvalidDataAccessApiUsageException(
						"Current database only supports retrieving the key for a single column. There are " +
						getGeneratedKeyNames().length  + " columns specified: " + Arrays.toString(getGeneratedKeyNames()));
			}

			Assert.state(getTableName() != null, "No table name set");
			final String keyQuery = this.tableMetaDataContext.getSimpleQueryForGetGeneratedKey(
					getTableName(), getGeneratedKeyNames()[0]);
			Assert.state(keyQuery != null, "Query for simulating get generated keys must not be null");

			// This is a hack to be able to get the generated key from a database that doesn't support
			// get generated keys feature. HSQL is one, PostgreSQL is another. Postgres uses a RETURNING
			// clause while HSQL uses a second query that has to be executed with the same connection.

			if (keyQuery.toUpperCase().startsWith("RETURNING")) {
				Long key = getJdbcTemplate().queryForObject(
						getInsertString() + " " + keyQuery, Long.class, values.toArray());
				Map<String, Object> keys = new HashMap<>(2);
				keys.put(getGeneratedKeyNames()[0], key);
				keyHolder.getKeyList().add(keys);
			}
			else {
				getJdbcTemplate().execute((ConnectionCallback<Object>) con -> {
					// Do the insert
					PreparedStatement ps = null;
					try {
						ps = con.prepareStatement(getInsertString());
						setParameterValues(ps, values, getInsertTypes());
						ps.executeUpdate();
					}
					finally {
						JdbcUtils.closeStatement(ps);
					}
					//Get the key
					Statement keyStmt = null;
					ResultSet rs = null;
					try {
						keyStmt = con.createStatement();
						rs = keyStmt.executeQuery(keyQuery);
						if (rs.next()) {
							long key = rs.getLong(1);
							Map<String, Object> keys = new HashMap<>(2);
							keys.put(getGeneratedKeyNames()[0], key);
							keyHolder.getKeyList().add(keys);
						}
					}
					finally {
						JdbcUtils.closeResultSet(rs);
						JdbcUtils.closeStatement(keyStmt);
					}
					return null;
				});
			}
		}

		return keyHolder;
	}

	/**
	 * 创建用于带生成键的插入操作的 PreparedStatement。
	 * @param con 要使用的 Connection
	 * @return 该 PreparedStatement
	 */
	private PreparedStatement prepareStatementForGeneratedKeys(Connection con) throws SQLException {
		if (getGeneratedKeyNames().length < 1) {
			throw new InvalidDataAccessApiUsageException("Generated Key Name(s) not specified. " +
					"Using the generated keys features requires specifying the name(s) of the generated column(s).");
		}
		PreparedStatement ps;
		if (this.tableMetaDataContext.isGeneratedKeysColumnNameArraySupported()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using generated keys support with array of column names.");
			}
			ps = con.prepareStatement(getInsertString(), getGeneratedKeyNames());
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Using generated keys support with Statement.RETURN_GENERATED_KEYS.");
			}
			ps = con.prepareStatement(getInsertString(), Statement.RETURN_GENERATED_KEYS);
		}
		return ps;
	}

	/**
	 * 使用传入的参数 Map 数组执行批量插入操作的委托方法。
	 * @param batch 包含批量插入时要使用的参数名称和值的 Map 数组
	 * @return 受影响行数数组
	 */
	@SuppressWarnings("unchecked")
	protected int[] doExecuteBatch(Map<String, ?>... batch) {
		checkCompiled();
		List<List<Object>> batchValues = new ArrayList<>(batch.length);
		for (Map<String, ?> args : batch) {
			batchValues.add(matchInParameterValuesWithInsertColumns(args));
		}
		return executeBatchInternal(batchValues);
	}

	/**
	 * 使用传入的 {@link SqlParameterSource SqlParameterSources} 执行批量插入操作的委托方法。
	 * @param batch 包含插入时要使用的参数名称和值的 SqlParameterSource 数组
	 * @return 受影响行数数组
	 */
	protected int[] doExecuteBatch(SqlParameterSource... batch) {
		checkCompiled();
		List<List<Object>> batchValues = new ArrayList<>(batch.length);
		for (SqlParameterSource parameterSource : batch) {
			batchValues.add(matchInParameterValuesWithInsertColumns(parameterSource));
		}
		return executeBatchInternal(batchValues);
	}

	/**
	 * 执行批量插入操作的委托方法。
	 */
	private int[] executeBatchInternal(final List<List<Object>> batchValues) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing statement " + getInsertString() + " with batch of size: " + batchValues.size());
		}
		return getJdbcTemplate().batchUpdate(getInsertString(),
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						setParameterValues(ps, batchValues.get(i), getInsertTypes());
					}
					@Override
					public int getBatchSize() {
						return batchValues.size();
					}
				});
	}

	/**
	 * 设置参数值的内部实现。
	 * @param preparedStatement 该 PreparedStatement
	 * @param values 要设置的值
	 */
	private void setParameterValues(PreparedStatement preparedStatement, List<?> values, @Nullable int... columnTypes)
			throws SQLException {

		int colIndex = 0;
		for (Object value : values) {
			colIndex++;
			if (columnTypes == null || colIndex > columnTypes.length) {
				StatementCreatorUtils.setParameterValue(preparedStatement, colIndex, SqlTypeValue.TYPE_UNKNOWN, value);
			}
			else {
				StatementCreatorUtils.setParameterValue(preparedStatement, colIndex, columnTypes[colIndex - 1], value);
			}
		}
	}

	/**
	 * 将提供的 in 参数值与已注册的参数以及
	 * 通过元数据处理定义的参数进行匹配。
	 * @param parameterSource 以 {@link SqlParameterSource} 形式提供的参数值
	 * @return 包含参数名称和值的 Map
	 */
	protected List<Object> matchInParameterValuesWithInsertColumns(SqlParameterSource parameterSource) {
		return this.tableMetaDataContext.matchInParameterValuesWithInsertColumns(parameterSource);
	}

	/**
	 * 将提供的 in 参数值与已注册的参数以及
	 * 通过元数据处理定义的参数进行匹配。
	 * @param args 在 Map 中提供的参数值
	 * @return 包含参数名称和值的 Map
	 */
	protected List<Object> matchInParameterValuesWithInsertColumns(Map<String, ?> args) {
		return this.tableMetaDataContext.matchInParameterValuesWithInsertColumns(args);
	}

}
