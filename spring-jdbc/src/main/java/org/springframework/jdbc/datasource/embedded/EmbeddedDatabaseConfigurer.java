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

package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

/**
 * {@code EmbeddedDatabaseConfigurer} 封装了创建、连接和关闭特定类型
 * 嵌入式数据库（例如 HSQL、H2 或 Derby）所需的配置。
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 */
public interface EmbeddedDatabaseConfigurer {

	/**
	 * 配置创建和连接嵌入式数据库所需的属性。
	 * @param properties 要配置的连接属性
	 * @param databaseName 内嵌数据库的名称
	 */
	void configureConnectionProperties(ConnectionProperties properties, String databaseName);

	/**
	 * 关闭支持所提供 {@link DataSource} 的嵌入式数据库实例。
	 * @param dataSource 对应的 {@link DataSource}
	 * @param databaseName 要关闭的数据库的名称
	 */
	void shutdown(DataSource dataSource, String databaseName);

}
