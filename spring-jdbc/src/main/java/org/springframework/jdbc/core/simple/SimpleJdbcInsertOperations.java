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

import java.util.Map;

import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 指定由 {@link SimpleJdbcInsert} 实现的简单 JDBC 插入 API 的接口。
 * 此接口不常直接使用，但提供了增强可测试性的选项，
 * 因为它可以很容易地被 mock 或 stub。
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public interface SimpleJdbcInsertOperations {

	/**
	 * 指定用于插入操作的表名。
	 * @param tableName 存储表的名称
	 * @return 此 SimpleJdbcInsert 的实例
	 */
	SimpleJdbcInsertOperations withTableName(String tableName);

	/**
	 * 指定用于插入操作的 schema 名称（如果有）。
	 * @param schemaName schema 的名称
	 * @return 此 SimpleJdbcInsert 的实例
	 */
	SimpleJdbcInsertOperations withSchemaName(String schemaName);

	/**
	 * 指定用于插入操作的 catalog 名称（如果有）。
	 * @param catalogName catalog 的名称
	 * @return 此 SimpleJdbcInsert 的实例
	 */
	SimpleJdbcInsertOperations withCatalogName(String catalogName);

	/**
	 * 指定插入语句应限制使用的列名。
	 * @param columnNames 一个或多个列名
	 * @return 此 SimpleJdbcInsert 的实例
	 */
	SimpleJdbcInsertOperations usingColumns(String... columnNames);

	/**
	 * 指定具有自动生成键的任何列的名称。
	 * @param columnNames 一个或多个列名
	 * @return 此 SimpleJdbcInsert 的实例
	 */
	SimpleJdbcInsertOperations usingGeneratedKeyColumns(String... columnNames);

	/**
	 * 关闭通过 JDBC 获取的列元数据信息的所有处理。
	 * @return 此 SimpleJdbcInsert 的实例
	 */
	SimpleJdbcInsertOperations withoutTableColumnMetaDataAccess();

	/**
	 * 在通过 JDBC 进行列元数据查找时包含同义词。
	 * <p>注意：只有 Oracle 需要包含此项，因为其他支持同义词的数据库
	 * 似乎会自动包含同义词。
	 * @return 此 SimpleJdbcInsert 的实例
	 */
	SimpleJdbcInsertOperations includeSynonymsForTableColumnMetaData();


	/**
	 * 使用传入的值执行插入操作。
	 * @param args 包含列名和对应值的 Map
	 * @return JDBC 驱动返回的受影响行数
	 */
	int execute(Map<String, ?> args);

	/**
	 * 使用传入的值执行插入操作。
	 * @param parameterSource 包含插入时要使用的值的 SqlParameterSource
	 * @return JDBC 驱动返回的受影响行数
	 */
	int execute(SqlParameterSource parameterSource);

	/**
	 * 使用传入的值执行插入操作并返回生成的键。
	 * <p>这要求已指定具有自动生成键的列名。
	 * 此方法始终返回 KeyHolder，但调用方必须验证它确实包含生成的键。
	 * @param args 包含列名和对应值的 Map
	 * @return 生成的主键值
	 */
	Number executeAndReturnKey(Map<String, ?> args);

	/**
	 * 使用传入的值执行插入操作并返回生成的键。
	 * <p>这要求已指定具有自动生成键的列名。
	 * 此方法始终返回 KeyHolder，但调用方必须验证它确实包含生成的键。
	 * @param parameterSource 包含插入时要使用的值的 SqlParameterSource
	 * @return 生成的主键值。
	 */
	Number executeAndReturnKey(SqlParameterSource parameterSource);

	/**
	 * 使用传入的值执行插入操作并返回生成的键。
	 * <p>这要求已指定具有自动生成键的列名。
	 * 此方法始终返回 KeyHolder，但调用方必须验证它确实包含生成的键。
	 * @param args 包含列名和对应值的 Map
	 * @return 包含所有生成主键的 KeyHolder
	 */
	KeyHolder executeAndReturnKeyHolder(Map<String, ?> args);

	/**
	 * 使用传入的值执行插入操作并返回生成的键。
	 * <p>这要求已指定具有自动生成键的列名。
	 * 此方法始终返回 KeyHolder，但调用方必须验证它确实包含生成的键。
	 * @param parameterSource 包含插入时要使用的值的 SqlParameterSource
	 * @return 包含所有生成主键的 KeyHolder
	 */
	KeyHolder executeAndReturnKeyHolder(SqlParameterSource parameterSource);

	/**
	 * 使用传入的一批值执行批量插入。
	 * @param batch 包含一批列名和对应值的 Map 数组
	 * @return JDBC 驱动返回的受影响行数数组
	 */
	@SuppressWarnings("unchecked")
	int[] executeBatch(Map<String, ?>... batch);

	/**
	 * 使用传入的一批值执行批量插入。
	 * @param batch 包含批量值的 SqlParameterSource 数组
	 * @return JDBC 驱动返回的受影响行数数组
	 */
	int[] executeBatch(SqlParameterSource... batch);

}
