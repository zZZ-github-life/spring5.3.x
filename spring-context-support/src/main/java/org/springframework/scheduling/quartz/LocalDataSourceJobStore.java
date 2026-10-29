/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.scheduling.quartz;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.quartz.SchedulerConfigException;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.impl.jdbcjobstore.SimpleSemaphore;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.lang.Nullable;

/**
 * Quartz {@link JobStoreCMT} 类的子类，委托给 Spring 管理的
 * {@link DataSource}，而不是使用 Quartz 管理的 JDBC 连接池。
 * 当设置了 SchedulerFactoryBean 的 "dataSource" 属性时，将使用此 JobStore。
 * 您也可以显式配置它，可能作为此
 * {@code LocalDataSourceJobStore} 的自定义子类或等价的 {@code JobStoreCMT} 变体。
 *
 * <p>支持事务性和非事务性的 DataSource 访问。
 * 使用非 XA DataSource 和本地 Spring 事务时，单个 DataSource
 * 参数即可满足需求。对于 XA DataSource 和全局 JTA 事务，
 * 应设置 SchedulerFactoryBean 的 "nonTransactionalDataSource" 属性，
 * 传入一个不参与全局事务的非 XA DataSource。
 *
 * <p>此 JobStore 执行的操作将正确参与任何类型的
 * Spring 管理的事务，因为它使用 Spring 的 DataSourceUtils
 * 连接处理方法，这些方法能感知当前事务。
 *
 * <p>请注意，所有影响持久化 job store 的 Quartz Scheduler 操作
 * 通常应在活跃事务中执行，因为它们需要正确的锁等。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see SchedulerFactoryBean#setDataSource
 * @see SchedulerFactoryBean#setNonTransactionalDataSource
 * @see SchedulerFactoryBean#getConfigTimeDataSource()
 * @see SchedulerFactoryBean#getConfigTimeNonTransactionalDataSource()
 * @see org.springframework.jdbc.datasource.DataSourceUtils#doGetConnection
 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
 */
@SuppressWarnings("unchecked")  // due to a warning in Quartz 2.2's JobStoreCMT
public class LocalDataSourceJobStore extends JobStoreCMT {

	/**
	 * 用于 Quartz 事务性 ConnectionProvider 的名称。
	 * 此提供者将委托给本地 Spring 管理的 DataSource。
	 * @see org.quartz.utils.DBConnectionManager#addConnectionProvider
	 * @see SchedulerFactoryBean#setDataSource
	 */
	public static final String TX_DATA_SOURCE_PREFIX = "springTxDataSource.";

	/**
	 * 用于 Quartz 非事务性 ConnectionProvider 的名称。
	 * 此提供者将委托给本地 Spring 管理的 DataSource。
	 * @see org.quartz.utils.DBConnectionManager#addConnectionProvider
	 * @see SchedulerFactoryBean#setDataSource
	 */
	public static final String NON_TX_DATA_SOURCE_PREFIX = "springNonTxDataSource.";


	@Nullable
	private DataSource dataSource;


	@Override
	public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
		// Absolutely needs thread-bound DataSource to initialize.
		this.dataSource = SchedulerFactoryBean.getConfigTimeDataSource();
		if (this.dataSource == null) {
			throw new SchedulerConfigException("No local DataSource found for configuration - " +
					"'dataSource' property must be set on SchedulerFactoryBean");
		}

		// Configure transactional connection settings for Quartz.
		setDataSource(TX_DATA_SOURCE_PREFIX + getInstanceName());
		setDontSetAutoCommitFalse(true);

		// Register transactional ConnectionProvider for Quartz.
		DBConnectionManager.getInstance().addConnectionProvider(
				TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					@Override
					public Connection getConnection() throws SQLException {
						// Return a transactional Connection, if any.
						return DataSourceUtils.doGetConnection(dataSource);
					}
					@Override
					public void shutdown() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
					@Override
					public void initialize() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
				}
		);

		// Non-transactional DataSource is optional: fall back to default
		// DataSource if not explicitly specified.
		DataSource nonTxDataSource = SchedulerFactoryBean.getConfigTimeNonTransactionalDataSource();
		final DataSource nonTxDataSourceToUse = (nonTxDataSource != null ? nonTxDataSource : this.dataSource);

		// Configure non-transactional connection settings for Quartz.
		setNonManagedTXDataSource(NON_TX_DATA_SOURCE_PREFIX + getInstanceName());

		// Register non-transactional ConnectionProvider for Quartz.
		DBConnectionManager.getInstance().addConnectionProvider(
				NON_TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					@Override
					public Connection getConnection() throws SQLException {
						// Always return a non-transactional Connection.
						return nonTxDataSourceToUse.getConnection();
					}
					@Override
					public void shutdown() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
					@Override
					public void initialize() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
				}
		);

		// No, if HSQL is the platform, we really don't want to use locks...
		try {
			String productName = JdbcUtils.extractDatabaseMetaData(this.dataSource,
					DatabaseMetaData::getDatabaseProductName);
			productName = JdbcUtils.commonDatabaseName(productName);
			if (productName != null && productName.toLowerCase().contains("hsql")) {
				setUseDBLocks(false);
				setLockHandler(new SimpleSemaphore());
			}
		}
		catch (MetaDataAccessException ex) {
			logWarnIfNonZero(1, "Could not detect database type. Assuming locks can be taken.");
		}

		super.initialize(loadHelper, signaler);

	}

	@Override
	protected void closeConnection(Connection con) {
		// Will work for transactional and non-transactional connections.
		DataSourceUtils.releaseConnection(con, this.dataSource);
	}

}
