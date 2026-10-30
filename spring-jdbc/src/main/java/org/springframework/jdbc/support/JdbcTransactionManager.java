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

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.core.SpringProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.lang.Nullable;

/**
 * 与 {@link JdbcAccessor} 对齐的普通 {@link DataSourceTransactionManager} 子类，
 * 为提交和回滚步骤添加了通用的 JDBC 异常转换。
 * 通常与 {@link org.springframework.jdbc.core.JdbcTemplate} 结合使用，
 * 后者默认应用相同的 {@link SQLExceptionTranslator} 基础设施。
 *
 * <p>异常转换对于可串行化事务（例如在 Postgres 上）中的提交步骤尤为重要，
 * 因为并发失败可能在提交后期才发生。这样便可以向调用方抛出
 * {@link org.springframework.dao.ConcurrencyFailureException}，而不是
 * {@link org.springframework.transaction.TransactionSystemException}。
 *
 * <p>与 {@code HibernateTransactionManager} 和 {@code JpaTransactionManager} 类似，
 * 此事务管理器可能会从 {@link #commit} 抛出 {@link DataAccessException}，
 * 也可能从 {@link #rollback} 抛出。调用方代码应准备好处理
 * {@link org.springframework.transaction.TransactionException} 以及此类异常，
 * 这通常是合理的，因为 {@code TransactionSynchronization} 实现也可能在其
 * {@code flush} 和 {@code beforeCommit} 阶段抛出此类异常。
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.3
 * @see DataSourceTransactionManager
 * @see #setDataSource
 * @see #setExceptionTranslator
 */
@SuppressWarnings("serial")
public class JdbcTransactionManager extends DataSourceTransactionManager {

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，指示 Spring 忽略 XML，
	 * 即不初始化与 XML 相关的基础设施。
	 * <p>默认为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");


	@Nullable
	private volatile SQLExceptionTranslator exceptionTranslator;

	private boolean lazyInit = true;


	/**
	 * 创建新的 JdbcTransactionManager 实例。
	 * 必须设置 DataSource 才能使用。
	 * @see #setDataSource
	 */
	public JdbcTransactionManager() {
		super();
	}

	/**
	 * 创建新的 JdbcTransactionManager 实例。
	 * @param dataSource 要为其管理事务的 JDBC DataSource
	 */
	public JdbcTransactionManager(DataSource dataSource) {
		this();
		setDataSource(dataSource);
		afterPropertiesSet();
	}


	/**
	 * 为此事务管理器所使用的 DataSource 指定数据库产品名称。
	 * 这样可以在不从 DataSource 获取 Connection 以读取元数据的情况下
	 * 初始化 SQLErrorCodeSQLExceptionTranslator。
	 * @param dbName 标识错误码条目的数据库产品名称
	 * @see JdbcAccessor#setDatabaseProductName
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
	 * @see JdbcAccessor#setExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 */
	public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
		this.exceptionTranslator = exceptionTranslator;
	}

	/**
	 * 返回此实例的异常转换器。
	 * <p>如果未设置，则为指定的 DataSource 创建默认的
	 * {@link SQLErrorCodeSQLExceptionTranslator}。
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
				if (shouldIgnoreXml) {
					exceptionTranslator = new SQLExceptionSubclassTranslator();
				}
				else {
					exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(obtainDataSource());
				}
				this.exceptionTranslator = exceptionTranslator;
			}
			return exceptionTranslator;
		}
	}

	/**
	 * 设置是否在首次遇到 SQLException 时延迟初始化此事务管理器的
	 * SQLExceptionTranslator。默认为 "true"；可以切换为 "false" 以在启动时初始化。
	 * <p>只有在调用 {@code afterPropertiesSet()} 时，提前初始化才会生效。
	 * @see #getExceptionTranslator()
	 * @see #afterPropertiesSet()
	 */
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回是否延迟初始化此事务管理器的 SQLExceptionTranslator。
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
		super.afterPropertiesSet();
		if (!isLazyInit()) {
			getExceptionTranslator();
		}
	}


	/**
	 * 此实现尝试使用 {@link SQLExceptionTranslator}，
	 * 如果无法转换，则回退到 {@link org.springframework.transaction.TransactionSystemException}。
	 * @see #getExceptionTranslator()
	 * @see DataSourceTransactionManager#translateException
	 */
	@Override
	protected RuntimeException translateException(String task, SQLException ex) {
		DataAccessException dae = getExceptionTranslator().translate(task, null, ex);
		if (dae != null) {
			return dae;
		}
		return super.translateException(task, ex);
	}

}
