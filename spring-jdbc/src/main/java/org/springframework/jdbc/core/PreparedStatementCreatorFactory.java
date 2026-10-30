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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;

/**
 * 辅助类，基于SQL语句和一组参数声明，
 * 高效地创建具有不同参数的多个{@link PreparedStatementCreator}对象。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class PreparedStatementCreatorFactory {

	/** SQL语句，参数变化时不会改变。 */
	private final String sql;

	/** SqlParameter对象列表（不能为{@code null}）。 */
	private final List<SqlParameter> declaredParameters;

	private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

	private boolean updatableResults = false;

	private boolean returnGeneratedKeys = false;

	@Nullable
	private String[] generatedKeysColumnNames;


	/**
	 * 创建新工厂。需要通过{@link #addParameter}方法添加参数，或者没有参数。
	 * @param sql 要执行的 SQL 语句
	 */
	public PreparedStatementCreatorFactory(String sql) {
		this.sql = sql;
		this.declaredParameters = new ArrayList<>();
	}

	/**
	 * 使用给定的SQL和JDBC类型创建新工厂。
	 * @param sql 要执行的 SQL 语句
	 * @param types JDBC 类型的 int 数组
	 */
	public PreparedStatementCreatorFactory(String sql, int... types) {
		this.sql = sql;
		this.declaredParameters = SqlParameter.sqlTypesToAnonymousParameterList(types);
	}

	/**
	 * 使用给定的SQL和参数创建新工厂。
	 * @param sql 要执行的 SQL 语句
	 * @param declaredParameters {@link SqlParameter} 对象列表
	 */
	public PreparedStatementCreatorFactory(String sql, List<SqlParameter> declaredParameters) {
		this.sql = sql;
		this.declaredParameters = declaredParameters;
	}


	/**
	 * 返回要执行的SQL语句。
	 * @since 5.1.3
	 */
	public final String getSql() {
		return this.sql;
	}

	/**
	 * 添加新的声明参数。
	 * <p>参数添加的顺序很重要。
	 * @param param 要添加到已声明参数列表的参数
	 */
	public void addParameter(SqlParameter param) {
		this.declaredParameters.add(param);
	}

	/**
	 * 设置是否使用返回特定类型ResultSet的预编译语句。
	 * @param resultSetType ResultSet 类型
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
	 * 设置预编译语句是否能够返回自动生成的键。
	 */
	public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
		this.returnGeneratedKeys = returnGeneratedKeys;
	}

	/**
	 * 设置自动生成的键的列名。
	 */
	public void setGeneratedKeysColumnNames(String... names) {
		this.generatedKeysColumnNames = names;
	}


	/**
	 * 根据给定参数返回新的PreparedStatementSetter。
	 * @param params 参数列表（可以为 {@code null}）
	 */
	public PreparedStatementSetter newPreparedStatementSetter(@Nullable List<?> params) {
		return new PreparedStatementCreatorImpl(params != null ? params : Collections.emptyList());
	}

	/**
	 * 根据给定参数返回新的PreparedStatementSetter。
	 * @param params 参数数组（可以为 {@code null}）
	 */
	public PreparedStatementSetter newPreparedStatementSetter(@Nullable Object[] params) {
		return new PreparedStatementCreatorImpl(params != null ? Arrays.asList(params) : Collections.emptyList());
	}

	/**
	 * 根据给定参数返回新的PreparedStatementCreator。
	 * @param params 参数列表（可以为 {@code null}）
	 */
	public PreparedStatementCreator newPreparedStatementCreator(@Nullable List<?> params) {
		return new PreparedStatementCreatorImpl(params != null ? params : Collections.emptyList());
	}

	/**
	 * 根据给定参数返回新的PreparedStatementCreator。
	 * @param params 参数数组（可以为 {@code null}）
	 */
	public PreparedStatementCreator newPreparedStatementCreator(@Nullable Object[] params) {
		return new PreparedStatementCreatorImpl(params != null ? Arrays.asList(params) : Collections.emptyList());
	}

	/**
	 * 根据给定参数返回新的PreparedStatementCreator。
	 * @param sqlToUse 要使用的实际 SQL 语句（如果与工厂的不同，
	 * 例如由于命名参数展开）
	 * @param params 参数数组（可以为 {@code null}）
	 */
	public PreparedStatementCreator newPreparedStatementCreator(String sqlToUse, @Nullable Object[] params) {
		return new PreparedStatementCreatorImpl(
				sqlToUse, params != null ? Arrays.asList(params) : Collections.emptyList());
	}


	/**
	 * 此类返回的PreparedStatementCreator实现。
	 */
	private class PreparedStatementCreatorImpl
			implements PreparedStatementCreator, PreparedStatementSetter, SqlProvider, ParameterDisposer {

		private final String actualSql;

		private final List<?> parameters;

		public PreparedStatementCreatorImpl(List<?> parameters) {
			this(sql, parameters);
		}

		public PreparedStatementCreatorImpl(String actualSql, List<?> parameters) {
			this.actualSql = actualSql;
			this.parameters = parameters;
			if (parameters.size() != declaredParameters.size()) {
				// Account for named parameters being used multiple times
				Set<String> names = new HashSet<>();
				for (int i = 0; i < parameters.size(); i++) {
					Object param = parameters.get(i);
					if (param instanceof SqlParameterValue) {
						names.add(((SqlParameterValue) param).getName());
					}
					else {
						names.add("Parameter #" + i);
					}
				}
				if (names.size() != declaredParameters.size()) {
					throw new InvalidDataAccessApiUsageException(
							"SQL [" + sql + "]: given " + names.size() +
							" parameters but expected " + declaredParameters.size());
				}
			}
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			PreparedStatement ps;
			if (generatedKeysColumnNames != null || returnGeneratedKeys) {
				if (generatedKeysColumnNames != null) {
					ps = con.prepareStatement(this.actualSql, generatedKeysColumnNames);
				}
				else {
					ps = con.prepareStatement(this.actualSql, PreparedStatement.RETURN_GENERATED_KEYS);
				}
			}
			else if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && !updatableResults) {
				ps = con.prepareStatement(this.actualSql);
			}
			else {
				ps = con.prepareStatement(this.actualSql, resultSetType,
					updatableResults ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
			}
			setValues(ps);
			return ps;
		}

		@Override
		public void setValues(PreparedStatement ps) throws SQLException {
			// Set arguments: Does nothing if there are no parameters.
			int sqlColIndx = 1;
			for (int i = 0; i < this.parameters.size(); i++) {
				Object in = this.parameters.get(i);
				SqlParameter declaredParameter;
				// SqlParameterValue overrides declared parameter meta-data, in particular for
				// independence from the declared parameter position in case of named parameters.
				if (in instanceof SqlParameterValue) {
					SqlParameterValue paramValue = (SqlParameterValue) in;
					in = paramValue.getValue();
					declaredParameter = paramValue;
				}
				else {
					if (declaredParameters.size() <= i) {
						throw new InvalidDataAccessApiUsageException(
								"SQL [" + sql + "]: unable to access parameter number " + (i + 1) +
								" given only " + declaredParameters.size() + " parameters");

					}
					declaredParameter = declaredParameters.get(i);
				}
				if (in instanceof Iterable && declaredParameter.getSqlType() != Types.ARRAY) {
					Iterable<?> entries = (Iterable<?>) in;
					for (Object entry : entries) {
						if (entry instanceof Object[]) {
							Object[] valueArray = (Object[]) entry;
							for (Object argValue : valueArray) {
								StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, declaredParameter, argValue);
							}
						}
						else {
							StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, declaredParameter, entry);
						}
					}
				}
				else {
					StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, declaredParameter, in);
				}
			}
		}

		@Override
		public String getSql() {
			return sql;
		}

		@Override
		public void cleanupParameters() {
			StatementCreatorUtils.cleanupParameters(this.parameters);
		}

		@Override
		public String toString() {
			return "PreparedStatementCreator: sql=[" + sql + "]; parameters=" + this.parameters;
		}
	}

}
