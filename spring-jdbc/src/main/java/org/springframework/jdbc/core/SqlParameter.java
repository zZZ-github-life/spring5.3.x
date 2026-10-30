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

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 表示SQL参数定义的对象。
 *
 * <p>参数可以是匿名的，此时"name"为{@code null}。
 * 但是，所有参数必须根据{@link java.sql.Types}定义SQL类型。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see java.sql.Types
 */
public class SqlParameter {

	// The name of the parameter, if any
	@Nullable
	private String name;

	// SQL type constant from {@code java.sql.Types}
	private final int sqlType;

	// Used for types that are user-named like: STRUCT, DISTINCT, JAVA_OBJECT, named array types
	@Nullable
	private String typeName;

	// The scale to apply in case of a NUMERIC or DECIMAL type, if any
	@Nullable
	private Integer scale;


	/**
	 * 创建新的匿名SqlParameter，提供SQL类型。
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 */
	public SqlParameter(int sqlType) {
		this.sqlType = sqlType;
	}

	/**
	 * 创建新的匿名SqlParameter，提供SQL类型。
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param typeName 参数的类型名称（可选）
	 */
	public SqlParameter(int sqlType, @Nullable String typeName) {
		this.sqlType = sqlType;
		this.typeName = typeName;
	}

	/**
	 * 创建新的匿名SqlParameter，提供SQL类型。
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param scale 小数点后的位数（用于 DECIMAL 和 NUMERIC 类型）
	 */
	public SqlParameter(int sqlType, int scale) {
		this.sqlType = sqlType;
		this.scale = scale;
	}

	/**
	 * 创建新的SqlParameter，提供名称和SQL类型。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 */
	public SqlParameter(String name, int sqlType) {
		this.name = name;
		this.sqlType = sqlType;
	}

	/**
	 * 创建新的SqlParameter，提供名称和SQL类型。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param typeName 参数的类型名称（可选）
	 */
	public SqlParameter(String name, int sqlType, @Nullable String typeName) {
		this.name = name;
		this.sqlType = sqlType;
		this.typeName = typeName;
	}

	/**
	 * 创建新的SqlParameter，提供名称和SQL类型。
	 * @param name 参数名称，用于输入和输出 Map 中
	 * @param sqlType 根据 {@code java.sql.Types} 的参数 SQL 类型
	 * @param scale 小数点后的位数（用于 DECIMAL 和 NUMERIC 类型）
	 */
	public SqlParameter(String name, int sqlType, int scale) {
		this.name = name;
		this.sqlType = sqlType;
		this.scale = scale;
	}

	/**
	 * 拷贝构造函数。
	 * @param otherParam 要从中复制的 SqlParameter 对象
	 */
	public SqlParameter(SqlParameter otherParam) {
		Assert.notNull(otherParam, "SqlParameter object must not be null");
		this.name = otherParam.name;
		this.sqlType = otherParam.sqlType;
		this.typeName = otherParam.typeName;
		this.scale = otherParam.scale;
	}


	/**
	 * 返回参数名称，如果是匿名的则返回{@code null}。
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 返回参数的SQL类型。
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * 返回参数的类型名称（如果有）。
	 */
	@Nullable
	public String getTypeName() {
		return this.typeName;
	}

	/**
	 * 返回参数的精度（如果有）。
	 */
	@Nullable
	public Integer getScale() {
		return this.scale;
	}


	/**
	 * 返回此参数是否持有在执行前应设置的输入值，即使它们是{@code null}。
	 * <p>此实现始终返回{@code true}。
	 */
	public boolean isInputValueProvided() {
		return true;
	}

	/**
	 * 返回此参数是否为在{@code CallableStatement.getMoreResults/getUpdateCount}
	 * 结果处理期间使用的隐式返回参数。
	 * <p>此实现始终返回{@code false}。
	 */
	public boolean isResultsParameter() {
		return false;
	}


	/**
	 * 将JDBC类型列表（如{@code java.sql.Types}中定义的）转换为此包中使用的SqlParameter对象列表。
	 */
	public static List<SqlParameter> sqlTypesToAnonymousParameterList(@Nullable int... types) {
		if (types == null) {
			return new ArrayList<>();
		}
		List<SqlParameter> result = new ArrayList<>(types.length);
		for (int type : types) {
			result.add(new SqlParameter(type));
		}
		return result;
	}

}
