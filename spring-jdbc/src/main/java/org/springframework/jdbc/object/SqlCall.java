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

package org.springframework.jdbc.object;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.ParameterMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 使用 JdbcTemplate 的 RdbmsOperation，表示基于 SQL 的调用，
 * 例如存储过程或存储函数。
 *
 * <p>根据声明的参数配置 CallableStatementCreatorFactory。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @see CallableStatementCreatorFactory
 */
public abstract class SqlCall extends RdbmsOperation {

	/**
	 * 用于指示此调用针对函数、并使用 {? = call get_invoice_count(?)} 语法的标志。
	 */
	private boolean function = false;

	/**
	 * 用于指示应严格按定义原样使用此调用的 SQL 的标志。
	 * 无需添加转义语法和参数占位符。
	 */
	private boolean sqlReadyForUse = false;

	/**
	 * java.sql.CallableStatement 中定义的调用字符串。
	 * 形式为 {call add_invoice(?, ?, ?)}；如果 isFunction 设置为 true，
	 * 则为 {? = call get_invoice_count(?)}。每添加一个参数后都会更新。
	 */
	@Nullable
	private String callString;

	/**
	 * 基于本类声明的参数，能够高效创建 CallableStatementCreator 的对象。
	 */
	@Nullable
	private CallableStatementCreatorFactory callableStatementFactory;


	/**
	 * 允许作为 JavaBean 使用的构造函数。
	 * 在调用 {@code compile} 方法并使用此对象之前，必须提供
	 * DataSource、SQL 和任何参数。
	 * @see #setDataSource
	 * @see #setSql
	 * @see #compile
	 */
	public SqlCall() {
	}

	/**
	 * 使用 SQL 创建新的 SqlCall 对象，但不含参数。
	 * 必须添加参数，或者接受没有参数的情况。
	 * @param ds 从中获取连接的 DataSource
	 * @param sql 要执行的 SQL
	 */
	public SqlCall(DataSource ds, String sql) {
		setDataSource(ds);
		setSql(sql);
	}


	/**
	 * 设置此调用是否针对函数。
	 */
	public void setFunction(boolean function) {
		this.function = function;
	}

	/**
	 * 返回此调用是否针对函数。
	 */
	public boolean isFunction() {
		return this.function;
	}

	/**
	 * 设置 SQL 是否可以直接使用。
	 */
	public void setSqlReadyForUse(boolean sqlReadyForUse) {
		this.sqlReadyForUse = sqlReadyForUse;
	}

	/**
	 * 返回 SQL 是否可以直接使用。
	 */
	public boolean isSqlReadyForUse() {
		return this.sqlReadyForUse;
	}


	/**
	 * 重写的方法，根据我们声明的参数配置 CallableStatementCreatorFactory。
	 * @see RdbmsOperation#compileInternal()
	 */
	@Override
	protected final void compileInternal() {
		if (isSqlReadyForUse()) {
			this.callString = resolveSql();
		}
		else {
			StringBuilder callString = new StringBuilder(32);
			List<SqlParameter> parameters = getDeclaredParameters();
			int parameterCount = 0;
			if (isFunction()) {
				callString.append("{? = call ").append(resolveSql()).append('(');
				parameterCount = -1;
			}
			else {
				callString.append("{call ").append(resolveSql()).append('(');
			}
			for (SqlParameter parameter : parameters) {
				if (!parameter.isResultsParameter()) {
					if (parameterCount > 0) {
						callString.append(", ");
					}
					if (parameterCount >= 0) {
						callString.append('?');
					}
					parameterCount++;
				}
			}
			callString.append(")}");
			this.callString = callString.toString();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Compiled stored procedure. Call string is [" + this.callString + "]");
		}

		this.callableStatementFactory = new CallableStatementCreatorFactory(this.callString, getDeclaredParameters());
		this.callableStatementFactory.setResultSetType(getResultSetType());
		this.callableStatementFactory.setUpdatableResults(isUpdatableResults());

		onCompileInternal();
	}

	/**
	 * 钩子方法，子类可以重写以响应编译过程。
	 * 此实现不执行任何操作。
	 */
	protected void onCompileInternal() {
	}

	/**
	 * 获取调用字符串。
	 */
	@Nullable
	public String getCallString() {
		return this.callString;
	}

	/**
	 * 返回一个 CallableStatementCreator，用于使用这些参数执行操作。
	 * @param inParams 参数。可以为 {@code null}。
	 */
	protected CallableStatementCreator newCallableStatementCreator(@Nullable Map<String, ?> inParams) {
		Assert.state(this.callableStatementFactory != null, "No CallableStatementFactory available");
		return this.callableStatementFactory.newCallableStatementCreator(inParams);
	}

	/**
	 * 返回一个 CallableStatementCreator，用于使用此 ParameterMapper
	 * 返回的参数执行操作。
	 * @param inParamMapper 参数映射器。不能为 {@code null}。
	 */
	protected CallableStatementCreator newCallableStatementCreator(ParameterMapper inParamMapper) {
		Assert.state(this.callableStatementFactory != null, "No CallableStatementFactory available");
		return this.callableStatementFactory.newCallableStatementCreator(inParamMapper);
	}

}
