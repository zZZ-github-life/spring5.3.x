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

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * 指定由 {@link SimpleJdbcCall} 实现的简单 JDBC 调用 API 的接口。
 * 此接口不常直接使用，但提供了增强可测试性的选项，
 * 因为它可以很容易地被 mock 或 stub。
 *
 * @author Thomas Risberg
 * @author Stephane Nicoll
 * @since 2.5
 */
public interface SimpleJdbcCallOperations {

	/**
	 * 指定要使用的过程名——这意味着我们将调用一个存储过程。
	 * @param procedureName 存储过程的名称
	 * @return 此 SimpleJdbcCall 的实例
	 */
	SimpleJdbcCallOperations withProcedureName(String procedureName);

	/**
	 * 指定要使用的函数名——这意味着我们将调用一个存储函数。
	 * @param functionName 存储函数的名称
	 * @return 此 SimpleJdbcCall 的实例
	 */
	SimpleJdbcCallOperations withFunctionName(String functionName);

	/**
	 * 可选：指定包含该存储过程的 schema 名称。
	 * @param schemaName schema 的名称
	 * @return 此 SimpleJdbcCall 的实例
	 */
	SimpleJdbcCallOperations withSchemaName(String schemaName);

	/**
	 * 可选：指定包含该存储过程的 catalog 名称。
	 * <p>为与 Oracle DatabaseMetaData 保持一致，如果过程是作为包的一部分声明的，
	 * 此参数用于指定包名。
	 * @param catalogName catalog 或包的名称
	 * @return 此 SimpleJdbcCall 的实例
	 */
	SimpleJdbcCallOperations withCatalogName(String catalogName);

	/**
	 * 表示过程的返回值应包含在返回的结果中。
	 * @return 此 SimpleJdbcCall 的实例
	 */
	SimpleJdbcCallOperations withReturnValue();

	/**
	 * 如果需要，指定一个或多个参数。这些参数将补充从数据库
	 * 元数据中检索到的任何参数信息。
	 * <p>请注意，只有声明为 {@code SqlParameter} 和 {@code SqlInOutParameter} 的参数
	 * 才会用于提供输入值。这与 {@code StoredProcedure} 类不同，
	 * 后者出于向后兼容的原因允许为声明为 {@code SqlOutParameter} 的参数提供输入值。
	 * @param sqlParameters 要使用的参数
	 * @return 此 SimpleJdbcCall 的实例
	 */
	SimpleJdbcCallOperations declareParameters(SqlParameter... sqlParameters);

	/** 尚未使用。 */
	SimpleJdbcCallOperations useInParameterNames(String... inParameterNames);

	/**
	 * 用于指定存储过程返回 ResultSet 并且希望它由 {@link RowMapper} 映射的情况。
	 * 结果将使用指定的参数名返回。多个 ResultSet 必须按正确的顺序声明。
	 * <p>如果你使用的数据库使用 ref cursor，则指定的名称必须与数据库中
	 * 为该过程声明的参数名称一致。
	 * @param parameterName 返回的结果集的名称和/或 ref cursor 参数的名称
	 * @param rowMapper 将映射每行返回数据的 RowMapper 实现
	 * */
	SimpleJdbcCallOperations returningResultSet(String parameterName, RowMapper<?> rowMapper);

	/**
	 * 关闭通过 JDBC 获取的参数元数据信息的所有处理。
	 * @return 此 SimpleJdbcCall 的实例
	 */
	SimpleJdbcCallOperations withoutProcedureColumnMetaDataAccess();

	/**
	 * 表示应按名称绑定参数。
	 * @return 此 SimpleJdbcCall 的实例
	 * @since 4.2
	 */
	SimpleJdbcCallOperations withNamedBinding();


	/**
	 * 执行存储函数，并将获得的结果作为指定返回类型的对象返回。
	 * @param returnType 要返回的值的类型
	 * @param args 包含调用中要使用的入参值的可选数组。
	 * 参数值必须按为存储过程定义参数的顺序提供。
	 */
	<T> T executeFunction(Class<T> returnType, Object... args);

	/**
	 * 执行存储函数，并将获得的结果作为指定返回类型的对象返回。
	 * @param returnType 要返回的值的类型
	 * @param args 包含调用中要使用的参数值的 Map
	 */
	<T> T executeFunction(Class<T> returnType, Map<String, ?> args);

	/**
	 * 执行存储函数，并将获得的结果作为指定返回类型的对象返回。
	 * @param returnType 要返回的值的类型
	 * @param args 包含调用中要使用的参数值的 MapSqlParameterSource
	 */
	<T> T executeFunction(Class<T> returnType, SqlParameterSource args);

	/**
	 * 执行存储过程，并将单个 out 参数作为指定返回类型的对象返回。
	 * 在有多个 out 参数的情况下，返回第一个，其他 out 参数将被忽略。
	 * @param returnType 要返回的值的类型
	 * @param args 包含调用中要使用的入参值的可选数组。
	 * 参数值必须按为存储过程定义参数的顺序提供。
	 */
	<T> T executeObject(Class<T> returnType, Object... args);

	/**
	 * 执行存储过程，并将单个 out 参数作为指定返回类型的对象返回。
	 * 在有多个 out 参数的情况下，返回第一个，其他 out 参数将被忽略。
	 * @param returnType 要返回的值的类型
	 * @param args 包含调用中要使用的参数值的 Map
	 */
	<T> T executeObject(Class<T> returnType, Map<String, ?> args);

	/**
	 * 执行存储过程，并将单个 out 参数作为指定返回类型的对象返回。
	 * 在有多个 out 参数的情况下，返回第一个，其他 out 参数将被忽略。
	 * @param returnType 要返回的值的类型
	 * @param args 包含调用中要使用的参数值的 MapSqlParameterSource
	 */
	<T> T executeObject(Class<T> returnType, SqlParameterSource args);

	/**
	 * 执行存储过程，并返回按名称（与参数声明中一致）索引的 output 参数 Map。
	 * @param args 包含调用中要使用的入参值的可选数组。
	 * 参数值必须按为存储过程定义参数的顺序提供。
	 * @return 输出参数组成的 Map
	 */
	Map<String, Object> execute(Object... args);

	/**
	 * 执行存储过程，并返回按名称（与参数声明中一致）索引的 output 参数 Map。
	 * @param args 包含调用中要使用的参数值的 Map
	 * @return 输出参数组成的 Map
	 */
	Map<String, Object> execute(Map<String, ?> args);

	/**
	 * 执行存储过程，并返回按名称（与参数声明中一致）索引的 output 参数 Map。
	 * @param args 包含调用中要使用的参数值的 SqlParameterSource
	 * @return 输出参数组成的 Map
	 */
	Map<String, Object> execute(SqlParameterSource args);

}
