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

package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * “RDBMS 操作”是一种多线程安全、可重用的对象，表示查询、更新或存储过程调用。
 * RDBMS 操作<b>不是</b>命令，因为命令不可重用。不过，execute 方法可以将命令
 * 作为参数接收。子类应为 JavaBean，以便于配置。
 *
 * <p>本类及其子类抛出运行时异常，这些异常定义在 {@code org.springframework.dao}
 * 包中（也是由 {@code org.springframework.jdbc.core} 包抛出的，本包中的类在
 * 底层使用该包来执行原始 JDBC 操作）。
 *
 * <p>子类应在调用 {@link #compile()} 方法之前设置 SQL 并添加参数。添加参数的
 * 顺序很重要。之后即可调用相应的 {@code execute} 或 {@code update} 方法。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SqlQuery
 * @see SqlUpdate
 * @see StoredProcedure
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public abstract class RdbmsOperation implements InitializingBean {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 用于执行 SQL 的更低层级的类。 */
	private JdbcTemplate jdbcTemplate = new JdbcTemplate();

	private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

	private boolean updatableResults = false;

	private boolean returnGeneratedKeys = false;

	@Nullable
	private String[] generatedKeysColumnNames;

	@Nullable
	private String sql;

	private final List<SqlParameter> declaredParameters = new ArrayList<>();

	/**
	 * 此操作是否已编译？编译至少意味着检查是否已提供 DataSource 和 sql，
	 * 但子类也可以实现自己的自定义校验。
	 */
	private volatile boolean compiled;


	/**
	 * 当希望跨多个 {@code RdbmsOperations} 复用同一个 {@link JdbcTemplate} 时，
	 * 可以将此方法作为更常用的 {@link #setDataSource} 的替代方案。如果该
	 * {@code JdbcTemplate} 具有特殊配置（例如需要重用的
	 * {@link org.springframework.jdbc.support.SQLExceptionTranslator}），则适合使用此方法。
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 返回此操作对象所使用的 {@link JdbcTemplate}。
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * 设置用于获取连接的 JDBC {@link DataSource}。
	 * @see org.springframework.jdbc.core.JdbcTemplate#setDataSource
	 */
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate.setDataSource(dataSource);
	}

	/**
	 * 设置此 RDBMS 操作的抓取大小。这对于处理大型结果集非常重要：
	 * 将其设置为高于默认值会提高处理速度，但会增加内存消耗；将其设置为
	 * 较低的值可以避免传输永远不会被应用程序读取的行数据。
	 * <p>默认值为 -1，表示使用驱动的默认值。
	 * @see org.springframework.jdbc.core.JdbcTemplate#setFetchSize
	 */
	public void setFetchSize(int fetchSize) {
		this.jdbcTemplate.setFetchSize(fetchSize);
	}

	/**
	 * 设置此 RDBMS 操作的最大行数。这对于处理大型结果集的子集非常重要，
	 * 可以避免在数据库或 JDBC 驱动中读取并保留整个结果集。
	 * <p>默认值为 -1，表示使用驱动的默认值。
	 * @see org.springframework.jdbc.core.JdbcTemplate#setMaxRows
	 */
	public void setMaxRows(int maxRows) {
		this.jdbcTemplate.setMaxRows(maxRows);
	}

	/**
	 * 设置此 RDBMS 操作所执行语句的查询超时时间。
	 * <p>默认值为 -1，表示使用 JDBC 驱动的默认值。
	 * <p>注意：在事务级别指定了超时时间的事务中执行时，此处指定的任何超时
	 * 都会被剩余的事务超时时间覆盖。
	 */
	public void setQueryTimeout(int queryTimeout) {
		this.jdbcTemplate.setQueryTimeout(queryTimeout);
	}

	/**
	 * 设置是否使用返回特定类型 ResultSet 的语句。
	 * @param resultSetType the ResultSet type
	 * @see java.sql.ResultSet#TYPE_FORWARD_ONLY
	 * @see java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE
	 * @see java.sql.ResultSet#TYPE_SCROLL_SENSITIVE
	 * @see java.sql.Connection#prepareStatement(String, int, int)
	 */
	public void setResultSetType(int resultSetType) {
		this.resultSetType = resultSetType;
	}

	/**
	 * 返回语句是否会返回特定类型的 ResultSet。
	 */
	public int getResultSetType() {
		return this.resultSetType;
	}

	/**
	 * 设置是否使用能够返回可更新 ResultSet 的语句。
	 * @see java.sql.Connection#prepareStatement(String, int, int)
	 */
	public void setUpdatableResults(boolean updatableResults) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"The updateableResults flag must be set before the operation is compiled");
		}
		this.updatableResults = updatableResults;
	}

	/**
	 * 返回语句是否会返回可更新的 ResultSet。
	 */
	public boolean isUpdatableResults() {
		return this.updatableResults;
	}

	/**
	 * 设置预处理语句是否应能够返回自动生成的主键。
	 * @see java.sql.Connection#prepareStatement(String, int)
	 */
	public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"The returnGeneratedKeys flag must be set before the operation is compiled");
		}
		this.returnGeneratedKeys = returnGeneratedKeys;
	}

	/**
	 * 返回语句是否应能够返回自动生成的主键。
	 */
	public boolean isReturnGeneratedKeys() {
		return this.returnGeneratedKeys;
	}

	/**
	 * 设置自动生成主键的列名。
	 * @see java.sql.Connection#prepareStatement(String, String[])
	 */
	public void setGeneratedKeysColumnNames(@Nullable String... names) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"The column names for the generated keys must be set before the operation is compiled");
		}
		this.generatedKeysColumnNames = names;
	}

	/**
	 * 返回自动生成主键的列名。
	 */
	@Nullable
	public String[] getGeneratedKeysColumnNames() {
		return this.generatedKeysColumnNames;
	}

	/**
	 * 设置此操作所执行的 SQL。
	 */
	public void setSql(@Nullable String sql) {
		this.sql = sql;
	}

	/**
	 * 子类可以根据需要重写此方法以提供动态 SQL，但 SQL 通常通过调用
	 * {@link #setSql} 方法或在子类构造函数中设置。
	 */
	@Nullable
	public String getSql() {
		return this.sql;
	}

	/**
	 * 解析已配置的 SQL 以供实际使用。
	 * @return the SQL (never {@code null})
	 * @since 5.0
	 */
	protected String resolveSql() {
		String sql = getSql();
		Assert.state(sql != null, "No SQL set");
		return sql;
	}

	/**
	 * 添加匿名参数，只需指定 {@code java.sql.Types} 类中定义的 SQL 类型。
	 * <p>参数的顺序很重要。此方法是 {@link #declareParameter} 方法的替代方案，
	 * 通常应优先使用后者。
	 * @param types array of SQL types as defined in the
	 * {@code java.sql.Types} class
	 * @throws InvalidDataAccessApiUsageException if the operation is already compiled
	 */
	public void setTypes(@Nullable int[] types) throws InvalidDataAccessApiUsageException {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Cannot add parameters once query is compiled");
		}
		if (types != null) {
			for (int type : types) {
				declareParameter(new SqlParameter(type));
			}
		}
	}

	/**
	 * 为此操作声明一个参数。
	 * <p>使用位置参数时，调用此方法的顺序很重要。在此处通过带名称的
	 * SqlParameter 对象使用命名参数时，顺序并不重要；但在此处将命名参数
	 * 与未命名的 SqlParameter 对象结合使用时，顺序仍然重要。
	 * @param param the SqlParameter to add. This will specify SQL type and (optionally)
	 * the parameter's name. Note that you typically use the {@link SqlParameter} class
	 * itself here, not any of its subclasses.
	 * @throws InvalidDataAccessApiUsageException if the operation is already compiled,
	 * and hence cannot be configured further
	 */
	public void declareParameter(SqlParameter param) throws InvalidDataAccessApiUsageException {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Cannot add parameters once the query is compiled");
		}
		this.declaredParameters.add(param);
	}

	/**
	 * 添加一个或多个已声明的参数。用于在 bean 工厂中使用时配置此操作。
	 * 每个参数都会指定 SQL 类型以及（可选的）参数名称。
	 * @param parameters an array containing the declared {@link SqlParameter} objects
	 * @see #declaredParameters
	 */
	public void setParameters(SqlParameter... parameters) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Cannot add parameters once the query is compiled");
		}
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i] != null) {
				this.declaredParameters.add(parameters[i]);
			}
			else {
				throw new InvalidDataAccessApiUsageException("Cannot add parameter at index " + i + " from " +
						Arrays.asList(parameters) + " since it is 'null'");
			}
		}
	}

	/**
	 * 返回已声明 {@link SqlParameter} 对象的列表。
	 */
	protected List<SqlParameter> getDeclaredParameters() {
		return this.declaredParameters;
	}


	/**
	 * 如果在 bean 工厂中使用，则确保完成编译。
	 */
	@Override
	public void afterPropertiesSet() {
		compile();
	}

	/**
	 * 编译此查询。
	 * 忽略后续的编译尝试。
	 * @throws InvalidDataAccessApiUsageException if the object hasn't
	 * been correctly initialized, for example if no DataSource has been provided
	 */
	public final void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getSql() == null) {
				throw new InvalidDataAccessApiUsageException("Property 'sql' is required");
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
				logger.debug("RdbmsOperation with SQL [" + getSql() + "] compiled");
			}
		}
	}

	/**
	 * 此操作是否已“编译”？与 JDO 中的概念类似，编译意味着操作已完全配置
	 * 并可以使用。编译的确切含义因各个子类而异。
	 * @return whether this operation is compiled and ready to use
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * 检查此操作是否已编译；如果尚未编译，则延迟编译。
	 * <p>由 {@code validateParameters} 自动调用。
	 * @see #validateParameters
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("SQL operation not compiled before execution - invoking compile");
			compile();
		}
	}

	/**
	 * 根据声明的参数校验传递给 execute 方法的参数。
	 * 子类应在每次调用 {@code executeQuery()} 或 {@code update()} 方法之前
	 * 调用此方法。
	 * @param parameters the parameters supplied (may be {@code null})
	 * @throws InvalidDataAccessApiUsageException if the parameters are invalid
	 */
	protected void validateParameters(@Nullable Object[] parameters) throws InvalidDataAccessApiUsageException {
		checkCompiled();
		int declaredInParameters = 0;
		for (SqlParameter param : this.declaredParameters) {
			if (param.isInputValueProvided()) {
				if (!supportsLobParameters() &&
						(param.getSqlType() == Types.BLOB || param.getSqlType() == Types.CLOB)) {
					throw new InvalidDataAccessApiUsageException(
							"BLOB or CLOB parameters are not allowed for this kind of operation");
				}
				declaredInParameters++;
			}
		}
		validateParameterCount((parameters != null ? parameters.length : 0), declaredInParameters);
	}

	/**
	 * 根据声明的参数校验传递给 execute 方法的命名参数。
	 * 子类应在每次调用 {@code executeQuery()} 或 {@code update()} 方法之前
	 * 调用此方法。
	 * @param parameters parameter Map supplied (may be {@code null})
	 * @throws InvalidDataAccessApiUsageException if the parameters are invalid
	 */
	protected void validateNamedParameters(@Nullable Map<String, ?> parameters) throws InvalidDataAccessApiUsageException {
		checkCompiled();
		Map<String, ?> paramsToUse = (parameters != null ? parameters : Collections.<String, Object> emptyMap());
		int declaredInParameters = 0;
		for (SqlParameter param : this.declaredParameters) {
			if (param.isInputValueProvided()) {
				if (!supportsLobParameters() &&
						(param.getSqlType() == Types.BLOB || param.getSqlType() == Types.CLOB)) {
					throw new InvalidDataAccessApiUsageException(
							"BLOB or CLOB parameters are not allowed for this kind of operation");
				}
				if (param.getName() != null && !paramsToUse.containsKey(param.getName())) {
					throw new InvalidDataAccessApiUsageException("The parameter named '" + param.getName() +
							"' was not among the parameters supplied: " + paramsToUse.keySet());
				}
				declaredInParameters++;
			}
		}
		validateParameterCount(paramsToUse.size(), declaredInParameters);
	}

	/**
	 * 根据给定声明的参数校验给定的参数数量。
	 * @param suppliedParamCount the number of actual parameters given
	 * @param declaredInParamCount the number of input parameters declared
	 */
	private void validateParameterCount(int suppliedParamCount, int declaredInParamCount) {
		if (suppliedParamCount < declaredInParamCount) {
			throw new InvalidDataAccessApiUsageException(suppliedParamCount + " parameters were supplied, but " +
					declaredInParamCount + " in parameters were declared in class [" + getClass().getName() + "]");
		}
		if (suppliedParamCount > this.declaredParameters.size() && !allowsUnusedParameters()) {
			throw new InvalidDataAccessApiUsageException(suppliedParamCount + " parameters were supplied, but " +
					declaredInParamCount + " parameters were declared in class [" + getClass().getName() + "]");
		}
	}


	/**
	 * 子类必须实现此模板方法以执行自己的编译逻辑。
	 * 在基类的编译完成之后调用。
	 * <p>子类可以假定 SQL 和 DataSource 已提供。
	 * @throws InvalidDataAccessApiUsageException if the subclass hasn't been
	 * properly configured
	 */
	protected abstract void compileInternal() throws InvalidDataAccessApiUsageException;

	/**
	 * 返回此类操作是否支持 BLOB/CLOB 参数。
	 * <p>默认为 {@code true}。
	 */
	protected boolean supportsLobParameters() {
		return true;
	}

	/**
	 * 返回此操作是否接受提供了但实际未使用的额外参数。尤其适用于参数 Map。
	 * <p>默认为 {@code false}。
	 * @see StoredProcedure
	 */
	protected boolean allowsUnusedParameters() {
		return false;
	}

}
