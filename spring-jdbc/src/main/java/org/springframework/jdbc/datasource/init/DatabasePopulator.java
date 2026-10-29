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

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 用于填充、初始化或清理数据库的策略。
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 * @see ResourceDatabasePopulator
 * @see DatabasePopulatorUtils
 * @see DataSourceInitializer
 */
@FunctionalInterface
public interface DatabasePopulator {

	/**
	 * 使用提供的 JDBC 连接填充、初始化或清理数据库。
	 * <p><strong>警告</strong>：具体实现不应关闭所提供的 {@link Connection}。
	 * <p>如果遇到错误，具体实现<em>可以</em>抛出 {@link SQLException}，
	 * 但<em>强烈建议</em>改为抛出特定的 {@link ScriptException}。
	 * 例如，Spring 的 {@link ResourceDatabasePopulator} 和
	 * {@link DatabasePopulatorUtils} 会将所有 {@code SQLException}
	 * 包装为 {@code ScriptException}。
	 * @param connection the JDBC connection to use; already configured and
	 * ready to use; never {@code null}
	 * @throws SQLException if an unrecoverable data access exception occurs
	 * while interacting with the database
	 * @throws ScriptException in all other error cases
	 * @see DatabasePopulatorUtils#execute
	 */
	void populate(Connection connection) throws SQLException, ScriptException;

}
