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

package org.springframework.jdbc.core.simple;

import java.util.Arrays;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

/**
 * SimpleJdbcInsert 是一个多线程安全、可重复使用的对象，为表提供便捷的
 * 插入能力。它提供元数据处理，以简化构造基本插入语句所需的代码。
 * 你只需提供表名以及包含列名和列值的 Map。
 *
 * <p>元数据处理基于 JDBC 驱动提供的 DatabaseMetaData。只要 JDBC 驱动
 * 能够提供指定表的列名，我们就可以依赖这种自动检测功能。如果情况
 * 并非如此，则必须显式指定列名。
 *
 * <p>实际的插入操作使用 Spring 的 {@link JdbcTemplate} 处理。
 *
 * <p>许多配置方法会返回当前的 SimpleJdbcInsert 实例，以便能够以“流式”
 * 接口风格将多个方法链式调用组合在一起。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 * @see java.sql.DatabaseMetaData
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public class SimpleJdbcInsert extends AbstractJdbcInsert implements SimpleJdbcInsertOperations {

	/**
	 * 接受一个参数的构造器，参数为创建 JdbcTemplate 时使用的
	 * JDBC DataSource。
	 * @param dataSource the {@code DataSource} to use
	 * @see org.springframework.jdbc.core.JdbcTemplate#setDataSource
	 */
	public SimpleJdbcInsert(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * 备选构造器，接受一个参数，即要使用的 JdbcTemplate。
	 * @param jdbcTemplate the {@code JdbcTemplate} to use
	 * @see org.springframework.jdbc.core.JdbcTemplate#setDataSource
	 */
	public SimpleJdbcInsert(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}


	@Override
	public SimpleJdbcInsert withTableName(String tableName) {
		setTableName(tableName);
		return this;
	}

	@Override
	public SimpleJdbcInsert withSchemaName(String schemaName) {
		setSchemaName(schemaName);
		return this;
	}

	@Override
	public SimpleJdbcInsert withCatalogName(String catalogName) {
		setCatalogName(catalogName);
		return this;
	}

	@Override
	public SimpleJdbcInsert usingColumns(String... columnNames) {
		setColumnNames(Arrays.asList(columnNames));
		return this;
	}

	@Override
	public SimpleJdbcInsert usingGeneratedKeyColumns(String... columnNames) {
		setGeneratedKeyNames(columnNames);
		return this;
	}

	@Override
	public SimpleJdbcInsertOperations withoutTableColumnMetaDataAccess() {
		setAccessTableColumnMetaData(false);
		return this;
	}

	@Override
	public SimpleJdbcInsertOperations includeSynonymsForTableColumnMetaData() {
		setOverrideIncludeSynonymsDefault(true);
		return this;
	}

	@Override
	public int execute(Map<String, ?> args) {
		return doExecute(args);
	}

	@Override
	public int execute(SqlParameterSource parameterSource) {
		return doExecute(parameterSource);
	}

	@Override
	public Number executeAndReturnKey(Map<String, ?> args) {
		return doExecuteAndReturnKey(args);
	}

	@Override
	public Number executeAndReturnKey(SqlParameterSource parameterSource) {
		return doExecuteAndReturnKey(parameterSource);
	}

	@Override
	public KeyHolder executeAndReturnKeyHolder(Map<String, ?> args) {
		return doExecuteAndReturnKeyHolder(args);
	}

	@Override
	public KeyHolder executeAndReturnKeyHolder(SqlParameterSource parameterSource) {
		return doExecuteAndReturnKeyHolder(parameterSource);
	}

	@Override
	@SuppressWarnings("unchecked")
	public int[] executeBatch(Map<String, ?>... batch) {
		return doExecuteBatch(batch);
	}

	@Override
	public int[] executeBatch(SqlParameterSource... batch) {
		return doExecuteBatch(batch);
	}

}
