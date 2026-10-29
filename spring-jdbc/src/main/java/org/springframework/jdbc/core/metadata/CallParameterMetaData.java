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

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;

import org.springframework.lang.Nullable;

/**
 * 持有用于调用处理的特定参数的元数据。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 * @see GenericCallMetaDataProvider
 */
public class CallParameterMetaData {

	private final boolean function;

	@Nullable
	private final String parameterName;

	private final int parameterType;

	private final int sqlType;

	@Nullable
	private final String typeName;

	private final boolean nullable;


	/**
	 * 接受除函数标记外所有属性的构造函数。
	 */
	@Deprecated
	public CallParameterMetaData(
			@Nullable String columnName, int columnType, int sqlType, @Nullable String typeName, boolean nullable) {

		this(false, columnName, columnType, sqlType, typeName, nullable);
	}

	/**
	 * 接受包括函数标记在内所有属性的构造函数。
	 * @since 5.2.9
	 */
	public CallParameterMetaData(boolean function, @Nullable String columnName, int columnType,
			int sqlType, @Nullable String typeName, boolean nullable) {

		this.function = function;
		this.parameterName = columnName;
		this.parameterType = columnType;
		this.sqlType = sqlType;
		this.typeName = typeName;
		this.nullable = nullable;
	}


	/**
	 * 返回此参数是否在函数中声明。
	 * @since 5.2.9
	 */
	public boolean isFunction() {
		return this.function;
	}

	/**
	 * 返回参数名称。
	 */
	@Nullable
	public String getParameterName() {
		return this.parameterName;
	}

	/**
	 * 返回参数类型。
	 */
	public int getParameterType() {
		return this.parameterType;
	}

	/**
	 * 判断声明的参数是否符合我们的"返回"参数条件：
	 * 类型为 {@link DatabaseMetaData#procedureColumnReturn} 或
	 * {@link DatabaseMetaData#procedureColumnResult}，或者在函数的情况下，
	 * 类型为 {@link DatabaseMetaData#functionReturn}。
	 * @since 4.3.15
	 */
	public boolean isReturnParameter() {
		return (this.function ? this.parameterType == DatabaseMetaData.functionReturn :
				(this.parameterType == DatabaseMetaData.procedureColumnReturn ||
						this.parameterType == DatabaseMetaData.procedureColumnResult));
	}

	/**
	 * 返回参数的 SQL 类型。
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * 返回参数的类型名称。
	 */
	@Nullable
	public String getTypeName() {
		return this.typeName;
	}

	/**
	 * 返回参数是否可为空。
	 */
	public boolean isNullable() {
		return this.nullable;
	}

}
