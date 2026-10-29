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

package org.springframework.test.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.util.StringUtils;

/**
 * {@code JdbcTestUtils} 是一组与 JDBC 相关的实用函数的集合，旨在简化标准数据库测试场景。
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Chris Baldwin
 * @since 2.5.4
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.datasource.init.ScriptUtils
 * @see org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * @see org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
 */
public abstract class JdbcTestUtils {

	private static final Log logger = LogFactory.getLog(JdbcTestUtils.class);


	/**
	 * 统计给定表中的行数。
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableName name of the table to count rows in
	 * @return the number of rows in the table
	 */
	public static int countRowsInTable(JdbcTemplate jdbcTemplate, String tableName) {
		Integer result = jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName, Integer.class);
		return (result != null ? result : 0);
	}

	/**
	 * 使用所提供的 {@code WHERE} 子句统计给定表中的行数。
	 * <p>如果所提供的 {@code WHERE} 子句包含文本，则会在其前面加上 {@code " WHERE "}，然后追加到生成的 {@code SELECT} 语句中。例如，如果所提供的表名为 {@code "person"}，所提供的 where 子句为 {@code "name = 'Bob' and age > 25"}，则要执行的生成 SQL 语句为 {@code "SELECT COUNT(0) FROM person WHERE name = 'Bob' and age > 25"}。
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableName the name of the table to count rows in
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @return the number of rows in the table that match the provided
	 * {@code WHERE} clause
	 */
	public static int countRowsInTableWhere(JdbcTemplate jdbcTemplate, String tableName, String whereClause) {
		String sql = "SELECT COUNT(0) FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
		return (result != null ? result : 0);
	}

	/**
	 * 删除指定表中的所有行。
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableNames the names of the tables to delete from
	 * @return the total number of rows deleted from all specified tables
	 */
	public static int deleteFromTables(JdbcTemplate jdbcTemplate, String... tableNames) {
		int totalRowCount = 0;
		for (String tableName : tableNames) {
			int rowCount = jdbcTemplate.update("DELETE FROM " + tableName);
			totalRowCount += rowCount;
			if (logger.isInfoEnabled()) {
				logger.info("Deleted " + rowCount + " rows from table " + tableName);
			}
		}
		return totalRowCount;
	}

	/**
	 * 使用所提供的 {@code WHERE} 子句从给定表中删除行。
	 * <p>如果所提供的 {@code WHERE} 子句包含文本，则会在其前面加上 {@code " WHERE "}，然后追加到生成的 {@code DELETE} 语句中。例如，如果所提供的表名为 {@code "person"}，所提供的 where 子句为 {@code "name = 'Bob' and age > 25"}，则要执行的生成 SQL 语句为 {@code "DELETE FROM person WHERE name = 'Bob' and age > 25"}。
	 * <p>作为硬编码值的替代，可以在 {@code WHERE} 子句中使用 {@code "?"} 占位符，并绑定到给定的参数。
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableName the name of the table to delete rows from
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @param args arguments to bind to the query (leaving it to the PreparedStatement
	 * to guess the corresponding SQL type); may also contain {@link SqlParameterValue}
	 * objects which indicate not only the argument value but also the SQL type and
	 * optionally the scale.
	 * @return the number of rows deleted from the table
	 */
	public static int deleteFromTableWhere(
			JdbcTemplate jdbcTemplate, String tableName, String whereClause, Object... args) {

		String sql = "DELETE FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		int rowCount = (args.length > 0 ? jdbcTemplate.update(sql, args) : jdbcTemplate.update(sql));
		if (logger.isInfoEnabled()) {
			logger.info("Deleted " + rowCount + " rows from table " + tableName);
		}
		return rowCount;
	}

	/**
	 * 删除指定的表。
	 * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
	 * @param tableNames the names of the tables to drop
	 */
	public static void dropTables(JdbcTemplate jdbcTemplate, String... tableNames) {
		for (String tableName : tableNames) {
			jdbcTemplate.execute("DROP TABLE " + tableName);
			if (logger.isInfoEnabled()) {
				logger.info("Dropped table " + tableName);
			}
		}
	}

}
