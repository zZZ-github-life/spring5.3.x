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

package org.springframework.jdbc.support;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.SpringProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.jdbc.core.JdbcTemplate} 及其他 JDBC 访问 DAO 辅助类的
 * 基类，定义了 DataSource 和异常转换器等公共属性。
 *
 * <p>不打算直接使用。
 * 请参见 {@link org.springframework.jdbc.core.JdbcTemplate}。
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 28.11.2003
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public abstract class JdbcAccessor implements InitializingBean {

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，指示 Spring 忽略 XML，
	 * 即不初始化与 XML 相关的基础设施。
	 * <p>默认为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private DataSource dataSource;

	@Nullable
	private volatile SQLExceptionTranslator exceptionTranslator;

	private boolean lazyInit = true;


	/**
	 * 设置用于获取连接的 JDBC DataSource。
	 */
	public void setDataSource(@Nullable DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 返回此模板所使用的 DataSource。
	 */
	@Nullable
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * 获取供实际使用的 DataSource。
	 * @return DataSource（永不为 {@code null}）
	 * @throws IllegalStateException 如果未设置 DataSource
	 * @since 5.0
	 */
	protected DataSource obtainDataSource() {
		DataSource dataSource = getDataSource();
		Assert.state(dataSource != null, "No DataSource set");
		return dataSource;
	}

	/**
	 * 为此访问器所使用的 DataSource 指定数据库产品名称。
	 * 这样可以在不从 DataSource 获取 Connection 以读取元数据的情况下
	 * 初始化 SQLErrorCodeSQLExceptionTranslator。
	 * @param dbName 标识错误码条目的数据库产品名称
	 * @see SQLErrorCodeSQLExceptionTranslator#setDatabaseProductName
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	public void setDatabaseProductName(String dbName) {
		if (!shouldIgnoreXml) {
			this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
		}
	}

	/**
	 * 设置此实例的异常转换器。
	 * <p>如果未提供自定义转换器，则使用默认的
	 * {@link SQLErrorCodeSQLExceptionTranslator}，它会检查 SQLException
	 * 中供应商特定的错误码。
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLStateSQLExceptionTranslator
	 */
	public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
		this.exceptionTranslator = exceptionTranslator;
	}

	/**
	 * 返回此实例的异常转换器。
	 * <p>如果未设置，则为指定的 DataSource 创建默认的
	 * {@link SQLErrorCodeSQLExceptionTranslator}；如果没有 DataSource，
	 * 则创建 {@link SQLStateSQLExceptionTranslator}。
	 * @see #getDataSource()
	 */
	public SQLExceptionTranslator getExceptionTranslator() {
		SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
		if (exceptionTranslator != null) {
			return exceptionTranslator;
		}
		synchronized (this) {
			exceptionTranslator = this.exceptionTranslator;
			if (exceptionTranslator == null) {
				DataSource dataSource = getDataSource();
				if (shouldIgnoreXml) {
					exceptionTranslator = new SQLExceptionSubclassTranslator();
				}
				else if (dataSource != null) {
					exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
				}
				else {
					exceptionTranslator = new SQLStateSQLExceptionTranslator();
				}
				this.exceptionTranslator = exceptionTranslator;
			}
			return exceptionTranslator;
		}
	}

	/**
	 * 设置是否在首次遇到 SQLException 时延迟初始化此访问器的 SQLExceptionTranslator。
	 * 默认为 "true"；可以切换为 "false" 以在启动时初始化。
	 * <p>只有在调用 {@code afterPropertiesSet()} 时，提前初始化才会生效。
	 * @see #getExceptionTranslator()
	 * @see #afterPropertiesSet()
	 */
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回是否延迟初始化此访问器的 SQLExceptionTranslator。
	 * @see #getExceptionTranslator()
	 */
	public boolean isLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 如果要求，则立即初始化异常转换器；如果未设置，则为指定的
	 * DataSource 创建默认的异常转换器。
	 */
	@Override
	public void afterPropertiesSet() {
		if (getDataSource() == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
		if (!isLazyInit()) {
			getExceptionTranslator();
		}
	}

}
