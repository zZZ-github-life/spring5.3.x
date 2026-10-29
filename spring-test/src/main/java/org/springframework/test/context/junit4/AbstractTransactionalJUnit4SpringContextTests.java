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

package org.springframework.test.context.junit4;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * {@link AbstractJUnit4SpringContextTests} 的抽象{@linkplain Transactional 事务性}扩展，增加了 JDBC 访问的便捷功能。期望在 Spring {@linkplain ApplicationContext 应用程序上下文}中定义 {@link DataSource} bean 和 {@link PlatformTransactionManager} bean。
 * <p>此类公开一个 {@link JdbcTemplate}，并提供在事务中{@linkplain #countRowsInTable 统计表中的行数}（可以{@linkplain #countRowsInTableWhere 使用 WHERE 子句}）、{@linkplain #deleteFromTables 从表中删除数据}、{@linkplain #dropTables 删除表}以及{@linkplain #executeSqlScript 执行 SQL 脚本}的简便方法。
 * <p>具体子类必须满足 {@link AbstractJUnit4SpringContextTests} 中所述的相同要求。
 * <p>此类显式注册以下 {@code TestExecutionListener} 实现。如果希望切换到<em>默认</em>监听器集合，请参阅 {@link TestExecutionListeners @TestExecutionListeners} 的类级 Javadoc 以了解详情。
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener}
 * <li>{@link org.springframework.test.context.event.ApplicationEventsTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}
 * <li>{@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
 * <li>{@link org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener}
 * <li>{@link org.springframework.test.context.event.EventPublishingTestExecutionListener}
 * </ul>
 * <p>此类仅作为便于扩展的基类。
 * <ul>
 * <li>如果不希望测试类绑定到 Spring 特定的类层次结构，可以使用 {@link SpringRunner}、{@link ContextConfiguration @ContextConfiguration}、{@link TestExecutionListeners @TestExecutionListeners} 等配置自己的自定义测试类。</li>
 * <li>如果希望扩展此类并使用 {@link SpringRunner} 以外的运行器，从 Spring Framework 4.2 开始，可以使用 {@link org.springframework.test.context.junit4.rules.SpringClassRule SpringClassRule} 和 {@link org.springframework.test.context.junit4.rules.SpringMethodRule SpringMethodRule}，并通过 {@link org.junit.runner.RunWith @RunWith(...)} 指定所选的运行器。</li>
 * </ul>
 * <p><strong>注意：</strong>此类要求 JUnit 4.12 或更高版本。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see AbstractJUnit4SpringContextTests
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.TestExecutionListeners
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * @see org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.annotation.Commit
 * @see org.springframework.test.annotation.Rollback
 * @see org.springframework.test.context.transaction.BeforeTransaction
 * @see org.springframework.test.context.transaction.AfterTransaction
 * @see org.springframework.test.jdbc.JdbcTestUtils
 * @see org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
 */
@TestExecutionListeners(listeners = { ServletTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
	ApplicationEventsTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
	DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class,
	SqlScriptsTestExecutionListener.class, EventPublishingTestExecutionListener.class }, inheritListeners = false)
@Transactional
public abstract class AbstractTransactionalJUnit4SpringContextTests extends AbstractJUnit4SpringContextTests {

	/**
	 * 此类管理的 {@code JdbcTemplate}，可供子类使用。
	 * @since 3.2
	 */
	protected final JdbcTemplate jdbcTemplate = new JdbcTemplate();

	@Nullable
	private String sqlScriptEncoding;


	/**
	 * 设置 {@code DataSource}，通常通过依赖注入提供。
	 * <p>此方法还会实例化 {@link #jdbcTemplate} 实例变量。
	 */
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate.setDataSource(dataSource);
	}

	/**
	 * 指定 SQL 脚本的编码（如果与平台编码不同）。
	 * @see #executeSqlScript
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	/**
	 * 用于统计给定表中行数的便捷方法。
	 * @param tableName table name to count rows in
	 * @return the number of rows in the table
	 * @see JdbcTestUtils#countRowsInTable
	 */
	protected int countRowsInTable(String tableName) {
		return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
	}

	/**
	 * 使用提供的 {@code WHERE} 子句统计给定表中行数的便捷方法。
	 * <p>有关详情，请参见 {@link JdbcTestUtils#countRowsInTableWhere} 的 Javadoc。
	 * @param tableName the name of the table to count rows in
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @return the number of rows in the table that match the provided
	 * {@code WHERE} clause
	 * @since 3.2
	 * @see JdbcTestUtils#countRowsInTableWhere
	 */
	protected int countRowsInTableWhere(String tableName, String whereClause) {
		return JdbcTestUtils.countRowsInTableWhere(this.jdbcTemplate, tableName, whereClause);
	}

	/**
	 * 从指定表中删除所有行的便捷方法。
	 * <p>在事务外使用时请谨慎！
	 * @param names the names of the tables from which to delete
	 * @return the total number of rows deleted from all specified tables
	 * @see JdbcTestUtils#deleteFromTables
	 */
	protected int deleteFromTables(String... names) {
		return JdbcTestUtils.deleteFromTables(this.jdbcTemplate, names);
	}

	/**
	 * 使用提供的 {@code WHERE} 子句从给定表中删除所有行的便捷方法。
	 * <p>在事务外使用时请谨慎！
	 * <p>有关详情，请参见 {@link JdbcTestUtils#deleteFromTableWhere} 的 Javadoc。
	 * @param tableName the name of the table to delete rows from
	 * @param whereClause the {@code WHERE} clause to append to the query
	 * @param args arguments to bind to the query (leaving it to the {@code
	 * PreparedStatement} to guess the corresponding SQL type); may also contain
	 * {@link org.springframework.jdbc.core.SqlParameterValue SqlParameterValue}
	 * objects which indicate not only the argument value but also the SQL type
	 * and optionally the scale.
	 * @return the number of rows deleted from the table
	 * @since 4.0
	 * @see JdbcTestUtils#deleteFromTableWhere
	 */
	protected int deleteFromTableWhere(String tableName, String whereClause, Object... args) {
		return JdbcTestUtils.deleteFromTableWhere(this.jdbcTemplate, tableName, whereClause, args);
	}

	/**
	 * 删除所有指定表的便捷方法。
	 * <p>在事务外使用时请谨慎！
	 * @param names the names of the tables to drop
	 * @since 3.2
	 * @see JdbcTestUtils#dropTables
	 */
	protected void dropTables(String... names) {
		JdbcTestUtils.dropTables(this.jdbcTemplate, names);
	}

	/**
	 * 执行给定的 SQL 脚本。
	 * <p>在事务外使用时请谨慎！
	 * <p>脚本通常会从类路径加载。
	 * <p><b>如果期望回滚，请不要使用此方法执行 DDL。</b>
	 * @param sqlResourcePath the Spring resource path for the SQL script
	 * @param continueOnError whether to continue without throwing an
	 * exception in the event of an error
	 * @throws DataAccessException if there is an error executing a statement
	 * @see ResourceDatabasePopulator
	 * @see #setSqlScriptEncoding
	 */
	protected void executeSqlScript(String sqlResourcePath, boolean continueOnError) throws DataAccessException {
		DataSource ds = this.jdbcTemplate.getDataSource();
		Assert.state(ds != null, "No DataSource set");
		Assert.state(this.applicationContext != null, "No ApplicationContext set");
		Resource resource = this.applicationContext.getResource(sqlResourcePath);
		new ResourceDatabasePopulator(continueOnError, false, this.sqlScriptEncoding, resource).execute(ds);
	}

}
