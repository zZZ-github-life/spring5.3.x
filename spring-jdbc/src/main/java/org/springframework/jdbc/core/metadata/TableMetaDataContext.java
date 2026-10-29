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

package org.springframework.jdbc.core.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 管理数据库表操作的配置和执行所需的上下文元数据的类。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
public class TableMetaDataContext {

	// Logger available to subclasses
	protected final Log logger = LogFactory.getLog(getClass());

	// Name of table for this context
	@Nullable
	private String tableName;

	// Name of catalog for this context
	@Nullable
	private String catalogName;

	// Name of schema for this context
	@Nullable
	private String schemaName;

	// List of columns objects to be used in this context
	private List<String> tableColumns = new ArrayList<>();

	// Should we access insert parameter meta-data info or not
	private boolean accessTableColumnMetaData = true;

	// Should we override default for including synonyms for meta-data lookups
	private boolean overrideIncludeSynonymsDefault = false;

	// The provider of table meta-data
	@Nullable
	private TableMetaDataProvider metaDataProvider;

	// Are we using generated key columns
	private boolean generatedKeyColumnsUsed = false;


	/**
	 * 设置此上下文的表名。
	 */
	public void setTableName(@Nullable String tableName) {
		this.tableName = tableName;
	}

	/**
	 * 获取此上下文的表名。
	 */
	@Nullable
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * 设置此上下文的目录（catalog）名称。
	 */
	public void setCatalogName(@Nullable String catalogName) {
		this.catalogName = catalogName;
	}

	/**
	 * 获取此上下文的目录（catalog）名称。
	 */
	@Nullable
	public String getCatalogName() {
		return this.catalogName;
	}

	/**
	 * 设置此上下文的模式（schema）名称。
	 */
	public void setSchemaName(@Nullable String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * 获取此上下文的模式（schema）名称。
	 */
	@Nullable
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 * 指定是否应访问表列元数据。
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.accessTableColumnMetaData = accessTableColumnMetaData;
	}

	/**
	 * 是否正在访问表元数据？
	 */
	public boolean isAccessTableColumnMetaData() {
		return this.accessTableColumnMetaData;
	}


	/**
	 * 指定是否应覆盖包含同义词的默认设置。
	 */
	public void setOverrideIncludeSynonymsDefault(boolean override) {
		this.overrideIncludeSynonymsDefault = override;
	}

	/**
	 * 是否正在覆盖包含同义词的默认设置？
	 */
	public boolean isOverrideIncludeSynonymsDefault() {
		return this.overrideIncludeSynonymsDefault;
	}

	/**
	 * 获取表列名称列表。
	 */
	public List<String> getTableColumns() {
		return this.tableColumns;
	}


	/**
	 * 使用提供的配置选项处理当前元数据。
	 * @param dataSource 正在使用的 DataSource
	 * @param declaredColumns 已声明的列
	 * @param generatedKeyNames 生成键的名称
	 */
	public void processMetaData(DataSource dataSource, List<String> declaredColumns, String[] generatedKeyNames) {
		this.metaDataProvider = TableMetaDataProviderFactory.createMetaDataProvider(dataSource, this);
		this.tableColumns = reconcileColumnsToUse(declaredColumns, generatedKeyNames);
	}

	private TableMetaDataProvider obtainMetaDataProvider() {
		Assert.state(this.metaDataProvider != null, "No TableMetaDataProvider - call processMetaData first");
		return this.metaDataProvider;
	}

	/**
	 * 将从元数据创建的列与已声明的列进行比较，返回协调后的列表。
	 * @param declaredColumns 已声明的列名
	 * @param generatedKeyNames 生成键列的名称
	 */
	protected List<String> reconcileColumnsToUse(List<String> declaredColumns, String[] generatedKeyNames) {
		if (generatedKeyNames.length > 0) {
			this.generatedKeyColumnsUsed = true;
		}
		if (!declaredColumns.isEmpty()) {
			return new ArrayList<>(declaredColumns);
		}
		Set<String> keys = new LinkedHashSet<>(generatedKeyNames.length);
		for (String key : generatedKeyNames) {
			keys.add(key.toUpperCase());
		}
		List<String> columns = new ArrayList<>();
		for (TableParameterMetaData meta : obtainMetaDataProvider().getTableParameterMetaData()) {
			if (!keys.contains(meta.getParameterName().toUpperCase())) {
				columns.add(meta.getParameterName());
			}
		}
		return columns;
	}

	/**
	 * 将提供的列名和值与使用的列列表进行匹配。
	 * @param parameterSource 参数名称和值
	 */
	public List<Object> matchInParameterValuesWithInsertColumns(SqlParameterSource parameterSource) {
		List<Object> values = new ArrayList<>();
		// For parameter source lookups we need to provide case-insensitive lookup support since the
		// database meta-data is not necessarily providing case-sensitive column names
		Map<String, String> caseInsensitiveParameterNames =
				SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(parameterSource);
		for (String column : this.tableColumns) {
			if (parameterSource.hasValue(column)) {
				values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, column));
			}
			else {
				String lowerCaseName = column.toLowerCase();
				if (parameterSource.hasValue(lowerCaseName)) {
					values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, lowerCaseName));
				}
				else {
					String propertyName = JdbcUtils.convertUnderscoreNameToPropertyName(column);
					if (parameterSource.hasValue(propertyName)) {
						values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, propertyName));
					}
					else {
						if (caseInsensitiveParameterNames.containsKey(lowerCaseName)) {
							values.add(SqlParameterSourceUtils.getTypedValue(
									parameterSource, caseInsensitiveParameterNames.get(lowerCaseName)));
						}
						else {
							values.add(null);
						}
					}
				}
			}
		}
		return values;
	}

	/**
	 * 将提供的列名和值与使用的列列表进行匹配。
	 * @param inParameters 参数名称和值
	 */
	public List<Object> matchInParameterValuesWithInsertColumns(Map<String, ?> inParameters) {
		List<Object> values = new ArrayList<>(inParameters.size());
		for (String column : this.tableColumns) {
			Object value = inParameters.get(column);
			if (value == null) {
				value = inParameters.get(column.toLowerCase());
				if (value == null) {
					for (Map.Entry<String, ?> entry : inParameters.entrySet()) {
						if (column.equalsIgnoreCase(entry.getKey())) {
							value = entry.getValue();
							break;
						}
					}
				}
			}
			values.add(value);
		}
		return values;
	}


	/**
	 * 根据配置和元数据信息构建插入字符串。
	 * @return 要使用的插入字符串
	 */
	public String createInsertString(String... generatedKeyNames) {
		Set<String> keys = new LinkedHashSet<>(generatedKeyNames.length);
		for (String key : generatedKeyNames) {
			keys.add(key.toUpperCase());
		}
		StringBuilder insertStatement = new StringBuilder();
		insertStatement.append("INSERT INTO ");
		if (getSchemaName() != null) {
			insertStatement.append(getSchemaName());
			insertStatement.append('.');
		}
		insertStatement.append(getTableName());
		insertStatement.append(" (");
		int columnCount = 0;
		for (String columnName : getTableColumns()) {
			if (!keys.contains(columnName.toUpperCase())) {
				columnCount++;
				if (columnCount > 1) {
					insertStatement.append(", ");
				}
				insertStatement.append(columnName);
			}
		}
		insertStatement.append(") VALUES(");
		if (columnCount < 1) {
			if (this.generatedKeyColumnsUsed) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unable to locate non-key columns for table '" +
							getTableName() + "' so an empty insert statement is generated");
				}
			}
			else {
				String message = "Unable to locate columns for table '" + getTableName()
						+ "' so an insert statement can't be generated.";
				if (isAccessTableColumnMetaData()) {
					message += " Consider specifying explicit column names -- for example, via SimpleJdbcInsert#usingColumns().";
				}
				throw new InvalidDataAccessApiUsageException(message);
			}
		}
		String params = String.join(", ", Collections.nCopies(columnCount, "?"));
		insertStatement.append(params);
		insertStatement.append(')');
		return insertStatement.toString();
	}

	/**
	 * 根据配置和元数据信息构建 {@link java.sql.Types} 数组。
	 * @return 要使用的类型数组
	 */
	public int[] createInsertTypes() {
		int[] types = new int[getTableColumns().size()];
		List<TableParameterMetaData> parameters = obtainMetaDataProvider().getTableParameterMetaData();
		Map<String, TableParameterMetaData> parameterMap = CollectionUtils.newLinkedHashMap(parameters.size());
		for (TableParameterMetaData tpmd : parameters) {
			parameterMap.put(tpmd.getParameterName().toUpperCase(), tpmd);
		}
		int typeIndx = 0;
		for (String column : getTableColumns()) {
			if (column == null) {
				types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
			}
			else {
				TableParameterMetaData tpmd = parameterMap.get(column.toUpperCase());
				if (tpmd != null) {
					types[typeIndx] = tpmd.getSqlType();
				}
				else {
					types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
				}
			}
			typeIndx++;
		}
		return types;
	}


	/**
	 * 此数据库是否支持 JDBC 3.0 的检索生成键功能：
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}？
	 */
	public boolean isGetGeneratedKeysSupported() {
		return obtainMetaDataProvider().isGetGeneratedKeysSupported();
	}

	/**
	 * 当不支持 JDBC 3.0 功能时，此数据库是否支持通过简单查询检索生成键：
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}？
	 */
	public boolean isGetGeneratedKeysSimulated() {
		return obtainMetaDataProvider().isGetGeneratedKeysSimulated();
	}

	/**
	 * 当不支持 JDBC 3.0 功能时，此数据库是否支持通过简单查询检索生成键：
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}？
	 * @deprecated 自 4.3.15 起，推荐使用 {@link #getSimpleQueryForGetGeneratedKey}
	 */
	@Deprecated
	@Nullable
	public String getSimulationQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return getSimpleQueryForGetGeneratedKey(tableName, keyColumnName);
	}

	/**
	 * 当不支持 JDBC 3.0 功能时，此数据库是否支持通过简单查询检索生成键：
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}？
	 */
	@Nullable
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return obtainMetaDataProvider().getSimpleQueryForGetGeneratedKey(tableName, keyColumnName);
	}

	/**
	 * 是否支持用于检索生成键的列名 String 数组：
	 * {@link java.sql.Connection#createStruct(String, Object[])}？
	 */
	public boolean isGeneratedKeysColumnNameArraySupported() {
		return obtainMetaDataProvider().isGeneratedKeysColumnNameArraySupported();
	}

}
