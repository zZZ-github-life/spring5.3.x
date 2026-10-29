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

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.lang.Nullable;

/**
 * 指定提供调用元数据的类所要实现的 API 的接口。
 *
 * <p>此接口供 Spring 的
 * {@link org.springframework.jdbc.core.simple.SimpleJdbcCall} 内部使用。
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public interface CallMetaDataProvider {

	/**
	 * 使用提供的 DatabaseMetaData 进行初始化。
	 * @param databaseMetaData 用于检索数据库特定信息
	 * @throws SQLException 如果初始化失败
	 */
	void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

	/**
	 * 初始化数据库特定的过程列元数据管理。
	 * 仅对所支持的数据库调用此方法。可以通过指定不使用列元数据来关闭此初始化。
	 * @param databaseMetaData 用于检索数据库特定信息
	 * @param catalogName 要使用的目录名称（如果没有则为 {@code null}）
	 * @param schemaName 要使用的模式名称（如果没有则为 {@code null}）
	 * @param procedureName 存储过程的名称
	 * @throws SQLException 如果初始化失败
	 * @see	org.springframework.jdbc.core.simple.SimpleJdbcCall#withoutProcedureColumnMetaDataAccess()
	 */
	void initializeWithProcedureColumnMetaData(DatabaseMetaData databaseMetaData, @Nullable String catalogName,
			@Nullable String schemaName, @Nullable String procedureName) throws SQLException;

	/**
	 * 对传入的过程名称进行修改，以匹配当前使用的元数据。
	 * 这可能包括更改大小写。
	 */
	@Nullable
	String procedureNameToUse(@Nullable String procedureName);

	/**
	 * 对传入的目录名称进行修改，以匹配当前使用的元数据。
	 * 这可能包括更改大小写。
	 */
	@Nullable
	String catalogNameToUse(@Nullable String catalogName);

	/**
	 * 对传入的模式名称进行修改，以匹配当前使用的元数据。
	 * 这可能包括更改大小写。
	 */
	@Nullable
	String schemaNameToUse(@Nullable String schemaName);

	/**
	 * 对传入的目录名称进行修改，以匹配当前使用的元数据。
	 * 返回的值将用于元数据查找。这可能包括更改所使用的大小写，
	 * 或在未提供目录时提供一个基础目录。
	 */
	@Nullable
	String metaDataCatalogNameToUse(@Nullable String catalogName) ;

	/**
	 * 对传入的模式名称进行修改，以匹配当前使用的元数据。
	 * 返回的值将用于元数据查找。这可能包括更改所使用的大小写，
	 * 或在未提供模式时提供一个基础模式。
	 */
	@Nullable
	String metaDataSchemaNameToUse(@Nullable String schemaName);

	/**
	 * 对传入的列名称进行修改，以匹配当前使用的元数据。
	 * 这可能包括更改大小写。
	 * @param parameterName 列的参数名称
	 */
	@Nullable
	String parameterNameToUse(@Nullable String parameterName);

	/**
	 * 根据提供的元数据创建默认的输出参数。
	 * 当没有显式声明参数时使用此方法。
	 * @param parameterName 参数的名称
	 * @param meta 用于此调用的元数据
	 * @return 配置好的 SqlOutParameter
	 */
	SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta);

	/**
	 * 根据提供的元数据创建默认的输入/输出参数。
	 * 当没有显式声明参数时使用此方法。
	 * @param parameterName 参数的名称
	 * @param meta 用于此调用的元数据
	 * @return 配置好的 SqlInOutParameter
	 */
	SqlParameter createDefaultInOutParameter(String parameterName, CallParameterMetaData meta);

	/**
	 * 根据提供的元数据创建默认的输入参数。
	 * 当没有显式声明参数时使用此方法。
	 * @param parameterName 参数的名称
	 * @param meta 用于此调用的元数据
	 * @return 配置好的 SqlParameter
	 */
	SqlParameter createDefaultInParameter(String parameterName, CallParameterMetaData meta);

	/**
	 * 获取当前用户的名称。可用于元数据查找等。
	 * @return 来自数据库连接的当前用户名
	 */
	@Nullable
	String getUserName();

	/**
	 * 此数据库是否支持通过 JDBC 调用检索 ResultSet：
	 * {@link java.sql.Statement#getResultSet()}？
	 */
	boolean isReturnResultSetSupported();

	/**
	 * 此数据库是否支持将 ResultSet 作为引用游标返回，
	 * 通过 {@link java.sql.CallableStatement#getObject(int)} 获取指定列。
	 */
	boolean isRefCursorSupported();

	/**
	 * 如果此功能受支持，获取以引用游标形式返回 ResultSet 的列的 {@link java.sql.Types} 类型。
	 */
	int getRefCursorSqlType();

	/**
	 * 是否正在使用过程列的元数据？
	 */
	boolean isProcedureColumnMetaDataUsed();

	/**
	 * 是否应绕过指定名称的返回参数。
	 * 这允许数据库特定的实现跳过数据库调用返回的特定结果的处理。
	 */
	boolean byPassReturnParameter(String parameterName);

	/**
	 * 获取当前使用的调用参数元数据。
	 * @return {@link CallParameterMetaData} 的列表
	 */
	List<CallParameterMetaData> getCallParameterMetaData();

	/**
	 * 此数据库是否支持在存储过程调用中使用目录名称？
	 */
	boolean isSupportsCatalogsInProcedureCalls();

	/**
	 * 此数据库是否支持在存储过程调用中使用模式名称？
	 */
	boolean isSupportsSchemasInProcedureCalls();

}
