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

package org.springframework.jdbc.core.support;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.dao.support.DaoSupport;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 JDBC 的数据访问对象的便捷超类。
 *
 * <p>要求设置 {@link javax.sql.DataSource}，并基于它通过
 * {@link #getJdbcTemplate()} 方法向子类提供
 * {@link org.springframework.jdbc.core.JdbcTemplate}。
 *
 * <p>此基类主要面向 JdbcTemplate 的使用，但也可用于直接操作
 * Connection 或使用 {@code org.springframework.jdbc.object} 操作对象的场景。
 *
 * @author Juergen Hoeller
 * @since 28.07.2003
 * @see #setDataSource
 * @see #getJdbcTemplate
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public abstract class JdbcDaoSupport extends DaoSupport {

	@Nullable
	private JdbcTemplate jdbcTemplate;


	/**
	 * 设置此 DAO 要使用的 JDBC DataSource。
	 */
	public final void setDataSource(DataSource dataSource) {
		if (this.jdbcTemplate == null || dataSource != this.jdbcTemplate.getDataSource()) {
			this.jdbcTemplate = createJdbcTemplate(dataSource);
			initTemplateConfig();
		}
	}

	/**
	 * 为给定的 DataSource 创建一个 JdbcTemplate。
	 * 仅在为 DAO 填充 DataSource 引用时调用！
	 * <p>可以在子类中覆盖，以提供具有不同配置的 JdbcTemplate 实例，
	 * 或提供自定义的 JdbcTemplate 子类。
	 * @param dataSource 要为其创建 JdbcTemplate 的 JDBC DataSource
	 * @return 新的 JdbcTemplate 实例
	 * @see #setDataSource
	 */
	protected JdbcTemplate createJdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	/**
	 * 返回此 DAO 使用的 JDBC DataSource。
	 */
	@Nullable
	public final DataSource getDataSource() {
		return (this.jdbcTemplate != null ? this.jdbcTemplate.getDataSource() : null);
	}

	/**
	 * 显式设置此 DAO 的 JdbcTemplate，
	 * 作为指定 DataSource 的替代方式。
	 */
	public final void setJdbcTemplate(@Nullable JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		initTemplateConfig();
	}

	/**
	 * 返回此 DAO 的 JdbcTemplate，
	 * 它已用 DataSource 预初始化或被显式设置。
	 */
	@Nullable
	public final JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * 初始化此 DAO 基于模板的配置。
	 * 在新的 JdbcTemplate 被直接设置或通过 DataSource 设置之后调用。
	 * <p>此实现为空。子类可以覆盖此方法，以基于 JdbcTemplate
	 * 配置更多对象。
	 * @see #getJdbcTemplate()
	 */
	protected void initTemplateConfig() {
	}

	@Override
	protected void checkDaoConfig() {
		if (this.jdbcTemplate == null) {
			throw new IllegalArgumentException("'dataSource' or 'jdbcTemplate' is required");
		}
	}


	/**
	 * 返回此 DAO 的 JdbcTemplate 所使用的 SQLExceptionTranslator，
	 * 用于在自定义 JDBC 访问代码中转换 SQLException。
	 * @see org.springframework.jdbc.core.JdbcTemplate#getExceptionTranslator()
	 */
	protected final SQLExceptionTranslator getExceptionTranslator() {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		Assert.state(jdbcTemplate != null, "No JdbcTemplate set");
		return jdbcTemplate.getExceptionTranslator();
	}

	/**
	 * 获取一个 JDBC Connection，可以来自当前事务，也可以是新建的。
	 * @return 该 JDBC Connection
	 * @throws CannotGetJdbcConnectionException 如果尝试获取 Connection 失败
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection(javax.sql.DataSource)
	 */
	protected final Connection getConnection() throws CannotGetJdbcConnectionException {
		DataSource dataSource = getDataSource();
		Assert.state(dataSource != null, "No DataSource set");
		return DataSourceUtils.getConnection(dataSource);
	}

	/**
	 * 关闭通过此 DAO 的 DataSource 创建的给定 JDBC Connection，
	 * 前提是它未绑定到线程。
	 * @param con 要关闭的 Connection
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
	 */
	protected final void releaseConnection(Connection con) {
		DataSourceUtils.releaseConnection(con, getDataSource());
	}

}
