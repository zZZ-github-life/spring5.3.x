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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 基于配置选项和数据库元数据，提供便捷存储过程调用基础功能的抽象类。
 *
 * <p>该类为 {@link SimpleJdbcCall} 提供基础 SPI。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public abstract class AbstractJdbcCall {

	/** 子类可用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 用于执行 SQL 的底层类。 */
	private final JdbcTemplate jdbcTemplate;

	/** 用于检索和管理数据库元数据的上下文。 */
	private final CallMetaDataContext callMetaDataContext = new CallMetaDataContext();

	/** SqlParameter 对象列表。 */
	private final List<SqlParameter> declaredParameters = new ArrayList<>();

	/** RefCursor/ResultSet RowMapper 对象列表。 */
	private final Map<String, RowMapper<?>> declaredRowMappers = new LinkedHashMap<>();

	/**
	 * 此操作是否已经编译？编译意味着至少检查
	 * 是否已提供 DataSource 或 JdbcTemplate。
	 */
	private volatile boolean compiled;

	/** 用于调用语句的生成字符串。 */
	@Nullable
	private String callString;

	/**
	 * 一个委托对象，使我们能够基于该类声明的参数
	 * 高效地创建 CallableStatementCreator。
	 */
	@Nullable
	private CallableStatementCreatorFactory callableStatementFactory;


	/**
	 * 使用 {@link DataSource} 进行初始化时使用的构造器。
	 * @param dataSource 要使用的 DataSource
	 */
	protected AbstractJdbcCall(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * 使用 {@link JdbcTemplate} 进行初始化时使用的构造器。
	 * @param jdbcTemplate 要使用的 JdbcTemplate
	 */
	protected AbstractJdbcCall(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
	}


	/**
	 * 获取配置的 {@link JdbcTemplate}。
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * 设置存储过程的名称。
	 */
	public void setProcedureName(@Nullable String procedureName) {
		this.callMetaDataContext.setProcedureName(procedureName);
	}

	/**
	 * 获取存储过程的名称。
	 */
	@Nullable
	public String getProcedureName() {
		return this.callMetaDataContext.getProcedureName();
	}

	/**
	 * 设置要使用的 in 参数名称。
	 */
	public void setInParameterNames(Set<String> inParameterNames) {
		this.callMetaDataContext.setLimitedInParameterNames(inParameterNames);
	}

	/**
	 * 获取要使用的 in 参数名称。
	 */
	public Set<String> getInParameterNames() {
		return this.callMetaDataContext.getLimitedInParameterNames();
	}

	/**
	 * 设置要使用的 catalog 名称。
	 */
	public void setCatalogName(@Nullable String catalogName) {
		this.callMetaDataContext.setCatalogName(catalogName);
	}

	/**
	 * 获取所使用的 catalog 名称。
	 */
	@Nullable
	public String getCatalogName() {
		return this.callMetaDataContext.getCatalogName();
	}

	/**
	 * 设置要使用的 schema 名称。
	 */
	public void setSchemaName(@Nullable String schemaName) {
		this.callMetaDataContext.setSchemaName(schemaName);
	}

	/**
	 * 获取所使用的 schema 名称。
	 */
	@Nullable
	public String getSchemaName() {
		return this.callMetaDataContext.getSchemaName();
	}

	/**
	 * 指定此调用是否为函数调用。
	 * 默认为 {@code false}。
	 */
	public void setFunction(boolean function) {
		this.callMetaDataContext.setFunction(function);
	}

	/**
	 * 此调用是否为函数调用？
	 */
	public boolean isFunction() {
		return this.callMetaDataContext.isFunction();
	}

	/**
	 * 指定此调用是否需要返回值。
	 * 默认为 {@code false}。
	 */
	public void setReturnValueRequired(boolean returnValueRequired) {
		this.callMetaDataContext.setReturnValueRequired(returnValueRequired);
	}

	/**
	 * 此调用是否需要返回值？
	 */
	public boolean isReturnValueRequired() {
		return this.callMetaDataContext.isReturnValueRequired();
	}

	/**
	 * 指定是否按名称绑定参数。
	 * 默认为 {@code false}。
	 * @since 4.2
	 */
	public void setNamedBinding(boolean namedBinding) {
		this.callMetaDataContext.setNamedBinding(namedBinding);
	}

	/**
	 * 参数是否应按名称绑定？
	 * @since 4.2
	 */
	public boolean isNamedBinding() {
		return this.callMetaDataContext.isNamedBinding();
	}

	/**
	 * 指定是否应使用此调用的参数元数据。
	 * 默认为 {@code true}。
	 */
	public void setAccessCallParameterMetaData(boolean accessCallParameterMetaData) {
		this.callMetaDataContext.setAccessCallParameterMetaData(accessCallParameterMetaData);
	}

	/**
	 * 获取基于参数和元数据应使用的调用字符串。
	 */
	@Nullable
	public String getCallString() {
		return this.callString;
	}

	/**
	 * 获取正在使用的 {@link CallableStatementCreatorFactory}。
	 */
	protected CallableStatementCreatorFactory getCallableStatementFactory() {
		Assert.state(this.callableStatementFactory != null, "No CallableStatementCreatorFactory available");
		return this.callableStatementFactory;
	}


	/**
	 * 向调用的参数列表中添加一个已声明的参数。
	 * <p>只有声明为 {@code SqlParameter} 和 {@code SqlInOutParameter} 的参数
	 * 才会用于提供输入值。这与 {@code StoredProcedure} 类不同，
	 * 后者出于向后兼容的原因允许为声明为 {@code SqlOutParameter} 的参数提供输入值。
	 * @param parameter 要添加的 {@link SqlParameter}
	 */
	public void addDeclaredParameter(SqlParameter parameter) {
		Assert.notNull(parameter, "The supplied parameter must not be null");
		if (!StringUtils.hasText(parameter.getName())) {
			throw new InvalidDataAccessApiUsageException(
					"You must specify a parameter name when declaring parameters for \"" + getProcedureName() + "\"");
		}
		this.declaredParameters.add(parameter);
		if (logger.isDebugEnabled()) {
			logger.debug("Added declared parameter for [" + getProcedureName() + "]: " + parameter.getName());
		}
	}

	/**
	 * 为指定的参数或列添加 {@link org.springframework.jdbc.core.RowMapper}。
	 * @param parameterName 参数或列的名称
	 * @param rowMapper 要使用的 RowMapper 实现
	 */
	public void addDeclaredRowMapper(String parameterName, RowMapper<?> rowMapper) {
		this.declaredRowMappers.put(parameterName, rowMapper);
		if (logger.isDebugEnabled()) {
			logger.debug("Added row mapper for [" + getProcedureName() + "]: " + parameterName);
		}
	}


	//-------------------------------------------------------------------------
	// Methods handling compilation issues
	//-------------------------------------------------------------------------

	/**
	 * 使用提供的参数、元数据以及其他设置编译此 JdbcCall。
	 * <p>这会最终确定此对象的配置，后续的编译尝试将被忽略。
	 * 首次执行未编译的调用时将隐式调用此方法。
	 * @throws org.springframework.dao.InvalidDataAccessApiUsageException 如果该对象尚未正确初始化，
	 * 例如未提供 DataSource
	 */
	public final synchronized void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getProcedureName() == null) {
				throw new InvalidDataAccessApiUsageException("Procedure or Function name is required");
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
				logger.debug("SqlCall for " + (isFunction() ? "function" : "procedure") +
						" [" + getProcedureName() + "] compiled");
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
		this.callMetaDataContext.initializeMetaData(dataSource);

		// Iterate over the declared RowMappers and register the corresponding SqlParameter
		this.declaredRowMappers.forEach((key, value) -> this.declaredParameters.add(this.callMetaDataContext.createReturnResultSetParameter(key, value)));
		this.callMetaDataContext.processParameters(this.declaredParameters);

		this.callString = this.callMetaDataContext.createCallString();
		if (logger.isDebugEnabled()) {
			logger.debug("Compiled stored procedure. Call string is [" + this.callString + "]");
		}

		this.callableStatementFactory = new CallableStatementCreatorFactory(
				this.callString, this.callMetaDataContext.getCallParameters());

		onCompileInternal();
	}

	/**
	 * 子类可以覆盖此钩子方法以响应编译过程。
	 * 此实现不执行任何操作。
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
	 * <p>由所有 {@code doExecute(...)} 方法自动调用。
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("JdbcCall call not compiled before execution - invoking compile");
			compile();
		}
	}


	//-------------------------------------------------------------------------
	// Methods handling execution
	//-------------------------------------------------------------------------

	/**
	 * 使用传入的 {@link SqlParameterSource} 执行调用的委托方法。
	 * @param parameterSource 调用中要使用的参数名称和值
	 * @return 输出参数组成的 Map
	 */
	protected Map<String, Object> doExecute(SqlParameterSource parameterSource) {
		checkCompiled();
		Map<String, Object> params = matchInParameterValuesWithCallParameters(parameterSource);
		return executeCallInternal(params);
	}

	/**
	 * 使用传入的参数数组执行调用的委托方法。
	 * @param args 参数值数组。值的顺序必须与为存储过程声明的顺序一致。
	 * @return 输出参数组成的 Map
	 */
	protected Map<String, Object> doExecute(Object... args) {
		checkCompiled();
		Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
		return executeCallInternal(params);
	}

	/**
	 * 使用传入的参数 Map 执行调用的委托方法。
	 * @param args 参数名称和值组成的 Map
	 * @return 输出参数组成的 Map
	 */
	protected Map<String, Object> doExecute(Map<String, ?> args) {
		checkCompiled();
		Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
		return executeCallInternal(params);
	}

	/**
	 * 执行实际调用处理的委托方法。
	 */
	private Map<String, Object> executeCallInternal(Map<String, ?> args) {
		CallableStatementCreator csc = getCallableStatementFactory().newCallableStatementCreator(args);
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for call " + getCallString() + " with " + args);
			int i = 1;
			for (SqlParameter param : getCallParameters()) {
				logger.debug(i + ": " +  param.getName() + ", SQL type "+ param.getSqlType() + ", type name " +
						param.getTypeName() + ", parameter class [" + param.getClass().getName() + "]");
				i++;
			}
		}
		return getJdbcTemplate().call(csc, getCallParameters());
	}


	/**
	 * 获取单个 out 参数或返回值的名称。
	 * 用于具有一个 out 参数的函数或过程。
	 */
	@Nullable
	protected String getScalarOutParameterName() {
		return this.callMetaDataContext.getScalarOutParameterName();
	}

	/**
	 * 获取要用于调用的所有调用参数的列表。
	 * 其中包括基于元数据处理添加的任何参数。
	 */
	protected List<SqlParameter> getCallParameters() {
		return this.callMetaDataContext.getCallParameters();
	}

	/**
	 * 将提供的 in 参数值与已注册的参数以及
	 * 通过元数据处理定义的参数进行匹配。
	 * @param parameterSource 以 {@link SqlParameterSource} 形式提供的参数值
	 * @return 包含参数名称和值的 Map
	 */
	protected Map<String, Object> matchInParameterValuesWithCallParameters(SqlParameterSource parameterSource) {
		return this.callMetaDataContext.matchInParameterValuesWithCallParameters(parameterSource);
	}

	/**
	 * 将提供的 in 参数值与已注册的参数以及
	 * 通过元数据处理定义的参数进行匹配。
	 * @param args 以数组形式提供的参数值
	 * @return 包含参数名称和值的 Map
	 */
	private Map<String, ?> matchInParameterValuesWithCallParameters(Object[] args) {
		return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
	}

	/**
	 * 将提供的 in 参数值与已注册的参数以及
	 * 通过元数据处理定义的参数进行匹配。
	 * @param args 在 Map 中提供的参数值
	 * @return 包含参数名称和值的 Map
	 */
	protected Map<String, ?> matchInParameterValuesWithCallParameters(Map<String, ?> args) {
		return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
	}

}
