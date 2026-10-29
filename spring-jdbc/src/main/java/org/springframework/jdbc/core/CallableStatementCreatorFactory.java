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

package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;

/**
 * 辅助类，基于SQL语句和一组参数声明，
 * 高效地创建具有不同参数的多个{@link CallableStatementCreator}对象。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class CallableStatementCreatorFactory {

	/** SQL调用字符串，参数变化时不会改变。 */
	private final String callString;

	/** SqlParameter对象列表。不能为{@code null}。 */
	private final List<SqlParameter> declaredParameters;

	private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

	private boolean updatableResults = false;


	/**
	 * 创建新工厂。需要通过{@link #addParameter}方法添加参数，或者没有参数。
	 * @param callString the SQL call string
	 */
	public CallableStatementCreatorFactory(String callString) {
		this.callString = callString;
		this.declaredParameters = new ArrayList<>();
	}

	/**
	 * 使用给定的SQL和给定的参数创建新工厂。
	 * @param callString the SQL call string
	 * @param declaredParameters list of {@link SqlParameter} objects
	 */
	public CallableStatementCreatorFactory(String callString, List<SqlParameter> declaredParameters) {
		this.callString = callString;
		this.declaredParameters = declaredParameters;
	}


	/**
	 * 返回SQL调用字符串。
	 * @since 5.1.3
	 */
	public final String getCallString() {
		return this.callString;
	}

	/**
	 * 添加新的声明参数。
	 * <p>参数添加的顺序很重要。
	 * @param param the parameter to add to the list of declared parameters
	 */
	public void addParameter(SqlParameter param) {
		this.declaredParameters.add(param);
	}

	/**
	 * 设置是否使用返回特定类型ResultSet的预编译语句。
	 * @param resultSetType the ResultSet type
	 * @see java.sql.ResultSet#TYPE_FORWARD_ONLY
	 * @see java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE
	 * @see java.sql.ResultSet#TYPE_SCROLL_SENSITIVE
	 */
	public void setResultSetType(int resultSetType) {
		this.resultSetType = resultSetType;
	}

	/**
	 * 设置是否使用能够返回可更新ResultSet的预编译语句。
	 */
	public void setUpdatableResults(boolean updatableResults) {
		this.updatableResults = updatableResults;
	}


	/**
	 * 根据给定参数返回新的CallableStatementCreator实例。
	 * @param params list of parameters (may be {@code null})
	 */
	public CallableStatementCreator newCallableStatementCreator(@Nullable Map<String, ?> params) {
		return new CallableStatementCreatorImpl(params != null ? params : new HashMap<>());
	}

	/**
	 * 根据给定的参数映射器返回新的CallableStatementCreator实例。
	 * @param inParamMapper the ParameterMapper implementation that will return a Map of parameters
	 */
	public CallableStatementCreator newCallableStatementCreator(ParameterMapper inParamMapper) {
		return new CallableStatementCreatorImpl(inParamMapper);
	}


	/**
	 * 此类返回的CallableStatementCreator实现。
	 */
	private class CallableStatementCreatorImpl implements CallableStatementCreator, SqlProvider, ParameterDisposer {

		@Nullable
		private ParameterMapper inParameterMapper;

		@Nullable
		private Map<String, ?> inParameters;

		/**
		 * 创建新的CallableStatementCreatorImpl。
		 * @param inParamMapper the ParameterMapper implementation for mapping input parameters
		 */
		public CallableStatementCreatorImpl(ParameterMapper inParamMapper) {
			this.inParameterMapper = inParamMapper;
		}

		/**
		 * 创建新的CallableStatementCreatorImpl。
		 * @param inParams list of SqlParameter objects
		 */
		public CallableStatementCreatorImpl(Map<String, ?> inParams) {
			this.inParameters = inParams;
		}

		@Override
		public CallableStatement createCallableStatement(Connection con) throws SQLException {
			// If we were given a ParameterMapper, we must let the mapper do its thing to create the Map.
			if (this.inParameterMapper != null) {
				this.inParameters = this.inParameterMapper.createMap(con);
			}
			else {
				if (this.inParameters == null) {
					throw new InvalidDataAccessApiUsageException(
							"A ParameterMapper or a Map of parameters must be provided");
				}
			}

			CallableStatement cs = null;
			if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && !updatableResults) {
				cs = con.prepareCall(callString);
			}
			else {
				cs = con.prepareCall(callString, resultSetType,
						updatableResults ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
			}

			int sqlColIndx = 1;
			for (SqlParameter declaredParam : declaredParameters) {
				if (!declaredParam.isResultsParameter()) {
					// So, it's a call parameter - part of the call string.
					// Get the value - it may still be null.
					Object inValue = this.inParameters.get(declaredParam.getName());
					if (declaredParam instanceof ResultSetSupportingSqlParameter) {
						// It's an output parameter: SqlReturnResultSet parameters already excluded.
						// It need not (but may be) supplied by the caller.
						if (declaredParam instanceof SqlOutParameter) {
							if (declaredParam.getTypeName() != null) {
								cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getTypeName());
							}
							else {
								if (declaredParam.getScale() != null) {
									cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getScale());
								}
								else {
									cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType());
								}
							}
							if (declaredParam.isInputValueProvided()) {
								StatementCreatorUtils.setParameterValue(cs, sqlColIndx, declaredParam, inValue);
							}
						}
					}
					else {
						// It's an input parameter; must be supplied by the caller.
						if (!this.inParameters.containsKey(declaredParam.getName())) {
							throw new InvalidDataAccessApiUsageException(
									"Required input parameter '" + declaredParam.getName() + "' is missing");
						}
						StatementCreatorUtils.setParameterValue(cs, sqlColIndx, declaredParam, inValue);
					}
					sqlColIndx++;
				}
			}

			return cs;
		}

		@Override
		public String getSql() {
			return callString;
		}

		@Override
		public void cleanupParameters() {
			if (this.inParameters != null) {
				StatementCreatorUtils.cleanupParameters(this.inParameters.values());
			}
		}

		@Override
		public String toString() {
			return "CallableStatementCreator: sql=[" + callString + "]; parameters=" + this.inParameters;
		}
	}

}
