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

import org.springframework.lang.Nullable;

/**
 * 指定提供表元数据的类所要实现的 API 的接口。
 * 此接口供 Simple JDBC 类内部使用。
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public interface TableMetaDataProvider {

	/**
	 * 使用提供的数据库元数据进行初始化。
	 * @param databaseMetaData 用于检索数据库特定信息
	 * @throws SQLException 如果初始化失败
	 */
	void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

	/**
	 * 使用提供的数据库元数据、表和列信息进行初始化。
	 * 可以通过指定不使用列元数据来关闭此初始化。
	 * @param databaseMetaData 用于检索数据库特定信息
	 * @param catalogName 要使用的目录名称（如果没有则为 {@code null}）
	 * @param schemaName 要使用的模式名称（如果没有则为 {@code null}）
	 * @param tableName 表的名称
	 * @throws SQLException 如果初始化失败
	 */
	void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData, @Nullable String catalogName,
			@Nullable String schemaName, @Nullable String tableName) throws SQLException;

	/**
	 * 获取基于元数据信息格式化的表名。
	 * 这可能包括更改大小写。
	 */
	@Nullable
	String tableNameToUse(@Nullable String tableName);

	/**
	 * 获取基于元数据信息格式化的目录名称。
	 * 这可能包括更改大小写。
	 */
	@Nullable
	String catalogNameToUse(@Nullable String catalogName);

	/**
	 * 获取基于元数据信息格式化的模式名称。
	 * 这可能包括更改大小写。
	 */
	@Nullable
	String schemaNameToUse(@Nullable String schemaName);

	/**
	 * 对传入的目录名称进行修改，以匹配当前使用的元数据。
	 * 返回的值将用于元数据查找。
	 * 这可能包括更改所使用的大小写，或在未提供目录时提供一个基础目录。
	 */
	@Nullable
	String metaDataCatalogNameToUse(@Nullable String catalogName) ;

	/**
	 * 对传入的模式名称进行修改，以匹配当前使用的元数据。
	 * 返回的值将用于元数据查找。
	 * 这可能包括更改所使用的大小写，或在未提供模式时提供一个基础模式。
	 */
	@Nullable
	String metaDataSchemaNameToUse(@Nullable String schemaName) ;

	/**
	 * 是否正在使用表列的元数据？
	 */
	boolean isTableColumnMetaDataUsed();

	/**
	 * 此数据库是否支持 JDBC 3.0 的检索生成键功能：
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}？
	 */
	boolean isGetGeneratedKeysSupported();

	/**
	 * 当不支持 JDBC 3.0 的检索生成键功能时，此数据库是否支持通过简单查询检索生成键？
	 * @see #isGetGeneratedKeysSupported()
	 */
	boolean isGetGeneratedKeysSimulated();

	/**
	 * 获取检索生成键的简单查询。
	 */
	@Nullable
	String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName);

	/**
	 * 此数据库是否支持用于检索生成键的列名 String 数组：
	 * {@link java.sql.Connection#createStruct(String, Object[])}？
	 */
	boolean isGeneratedKeysColumnNameArraySupported();

	/**
	 * 获取当前使用的表参数元数据。
	 * @return {@link TableParameterMetaData} 的列表
	 */
	List<TableParameterMetaData> getTableParameterMetaData();

}
