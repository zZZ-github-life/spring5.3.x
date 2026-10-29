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

package org.springframework.jdbc.core.simple;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * SimpleJdbcCall 是一个多线程安全、可重复使用的对象，表示对存储过程
 * 或存储函数的调用。它提供元数据处理，以简化访问基本存储过程/函数
 * 所需的代码。执行调用时，你只需提供过程/函数的名称以及包含参数的
 * Map。所提供的参数名称将与创建存储过程时声明的 in 参数和 out 参数
 * 进行匹配。
 *
 * <p>元数据处理基于 JDBC 驱动提供的 DatabaseMetaData。由于我们依赖
 * JDBC 驱动，这种“自动检测”只能用于已知能够提供准确元数据的数据库。
 * 目前包括 Derby、MySQL、Microsoft SQL Server、Oracle、DB2、Sybase
 * 和 PostgreSQL。对于任何其他数据库，需要显式声明所有参数。当然，
 * 即使数据库提供了必要的元数据，你也可以显式声明所有参数。在这种情况下，
 * 你声明的参数将优先。如果你希望使用与存储过程编译时声明的名称不匹配的
 * 参数名，也可以关闭所有元数据处理。
 *
 * <p>实际的插入操作使用 Spring 的 {@link JdbcTemplate} 处理。
 *
 * <p>许多配置方法会返回当前的 SimpleJdbcCall 实例，以便能够以“流式”
 * 接口风格将多个方法链式调用组合在一起。
 *
 * @author Thomas Risberg
 * @author Stephane Nicoll
 * @since 2.5
 * @see java.sql.DatabaseMetaData
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public class SimpleJdbcCall extends AbstractJdbcCall implements SimpleJdbcCallOperations {

	/**
	 * 接受一个参数的构造器，参数为创建底层 JdbcTemplate 时使用的
	 * JDBC DataSource。
	 * @param dataSource the {@code DataSource} to use
	 * @see org.springframework.jdbc.core.JdbcTemplate#setDataSource
	 */
	public SimpleJdbcCall(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * 备选构造器，接受一个参数，即要使用的 JdbcTemplate。
	 * @param jdbcTemplate the {@code JdbcTemplate} to use
	 * @see org.springframework.jdbc.core.JdbcTemplate#setDataSource
	 */
	public SimpleJdbcCall(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}


	@Override
	public SimpleJdbcCall withProcedureName(String procedureName) {
		setProcedureName(procedureName);
		setFunction(false);
		return this;
	}

	@Override
	public SimpleJdbcCall withFunctionName(String functionName) {
		setProcedureName(functionName);
		setFunction(true);
		return this;
	}

	@Override
	public SimpleJdbcCall withSchemaName(String schemaName) {
		setSchemaName(schemaName);
		return this;
	}

	@Override
	public SimpleJdbcCall withCatalogName(String catalogName) {
		setCatalogName(catalogName);
		return this;
	}

	@Override
	public SimpleJdbcCall withReturnValue() {
		setReturnValueRequired(true);
		return this;
	}

	@Override
	public SimpleJdbcCall declareParameters(SqlParameter... sqlParameters) {
		for (SqlParameter sqlParameter : sqlParameters) {
			if (sqlParameter != null) {
				addDeclaredParameter(sqlParameter);
			}
		}
		return this;
	}

	@Override
	public SimpleJdbcCall useInParameterNames(String... inParameterNames) {
		setInParameterNames(new LinkedHashSet<>(Arrays.asList(inParameterNames)));
		return this;
	}

	@Override
	public SimpleJdbcCall returningResultSet(String parameterName, RowMapper<?> rowMapper) {
		addDeclaredRowMapper(parameterName, rowMapper);
		return this;
	}

	@Override
	public SimpleJdbcCall withoutProcedureColumnMetaDataAccess() {
		setAccessCallParameterMetaData(false);
		return this;
	}

	@Override
	public SimpleJdbcCall withNamedBinding() {
		setNamedBinding(true);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeFunction(Class<T> returnType, Object... args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeFunction(Class<T> returnType, Map<String, ?> args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeFunction(Class<T> returnType, SqlParameterSource args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeObject(Class<T> returnType, Object... args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeObject(Class<T> returnType, Map<String, ?> args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeObject(Class<T> returnType, SqlParameterSource args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	public Map<String, Object> execute(Object... args) {
		return doExecute(args);
	}

	@Override
	public Map<String, Object> execute(Map<String, ?> args) {
		return doExecute(args);
	}

	@Override
	public Map<String, Object> execute(SqlParameterSource parameterSource) {
		return doExecute(parameterSource);
	}

}
