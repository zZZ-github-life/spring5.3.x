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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于创建 {@link EmbeddedDatabase} 实例的工厂。
 *
 * <p>调用方可以确信返回的数据库已完全初始化并填充完毕。
 *
 * <p>工厂可以进行以下配置：
 * <ul>
 * <li>调用 {@link #generateUniqueDatabaseName} 为数据库设置唯一的随机名称。
 * <li>调用 {@link #setDatabaseName} 为数据库设置显式名称。
 * <li>如果希望使用受支持类型之一，调用 {@link #setDatabaseType} 设置数据库类型。
 * <li>调用 {@link #setDatabaseConfigurer} 配置对自定义嵌入式数据库类型的支持。
 * <li>调用 {@link #setDatabasePopulator} 更改用于填充数据库的算法。
 * <li>调用 {@link #setDataSourceFactory} 更改用于连接数据库的
 * {@link DataSource} 类型。
 * </ul>
 *
 * <p>配置完工厂后，调用 {@link #getDatabase()} 获取
 * {@link EmbeddedDatabase} 实例的引用。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class EmbeddedDatabaseFactory {

	/**
	 * 嵌入式数据库的默认名称：{@value}。
	 */
	public static final String DEFAULT_DATABASE_NAME = "testdb";

	private static final Log logger = LogFactory.getLog(EmbeddedDatabaseFactory.class);

	private boolean generateUniqueDatabaseName = false;

	private String databaseName = DEFAULT_DATABASE_NAME;

	private DataSourceFactory dataSourceFactory = new SimpleDriverDataSourceFactory();

	@Nullable
	private EmbeddedDatabaseConfigurer databaseConfigurer;

	@Nullable
	private DatabasePopulator databasePopulator;

	@Nullable
	private DataSource dataSource;


	/**
	 * 设置 {@code generateUniqueDatabaseName} 标志，以启用或禁用
	 * 生成用作数据库名称的伪随机唯一 ID。
	 * <p>将此标志设置为 {@code true} 将覆盖通过 {@link #setDatabaseName}
	 * 设置的任何显式名称。
	 * @since 4.2
	 * @see #setDatabaseName
	 */
	public void setGenerateUniqueDatabaseName(boolean generateUniqueDatabaseName) {
		this.generateUniqueDatabaseName = generateUniqueDatabaseName;
	}

	/**
	 * 设置数据库的名称。
	 * <p>默认为 {@value #DEFAULT_DATABASE_NAME}。
	 * <p>如果 {@code generateUniqueDatabaseName} 标志已设置为 {@code true}，则会被覆盖。
	 * @param databaseName name of the embedded database
	 * @see #setGenerateUniqueDatabaseName
	 */
	public void setDatabaseName(String databaseName) {
		Assert.hasText(databaseName, "Database name is required");
		this.databaseName = databaseName;
	}

	/**
	 * 设置用于创建连接嵌入式数据库的 {@link DataSource} 实例的工厂。
	 * <p>默认为 {@link SimpleDriverDataSourceFactory}。
	 */
	public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		Assert.notNull(dataSourceFactory, "DataSourceFactory is required");
		this.dataSourceFactory = dataSourceFactory;
	}

	/**
	 * 设置要使用的嵌入式数据库类型。
	 * <p>当你希望配置某个预支持的类型时调用此方法。
	 * <p>默认为 HSQL。
	 * @param type the database type
	 */
	public void setDatabaseType(EmbeddedDatabaseType type) {
		this.databaseConfigurer = EmbeddedDatabaseConfigurerFactory.getConfigurer(type);
	}

	/**
	 * 设置用于配置嵌入式数据库实例的策略。
	 * <p>当你希望使用尚未受支持的嵌入式数据库类型时调用此方法。
	 */
	public void setDatabaseConfigurer(EmbeddedDatabaseConfigurer configurer) {
		this.databaseConfigurer = configurer;
	}

	/**
	 * 设置用于初始化或填充嵌入式数据库的策略。
	 * <p>默认为 {@code null}。
	 */
	public void setDatabasePopulator(DatabasePopulator populator) {
		this.databasePopulator = populator;
	}

	/**
	 * 工厂方法，返回{@linkplain EmbeddedDatabase 嵌入式数据库}实例，
	 * 该实例同时也是一个 {@link DataSource}。
	 */
	public EmbeddedDatabase getDatabase() {
		if (this.dataSource == null) {
			initDatabase();
		}
		return new EmbeddedDataSourceProxy(this.dataSource);
	}


	/**
	 * 用于初始化嵌入式数据库的钩子。
	 * <p>如果 {@code generateUniqueDatabaseName} 标志已设置为 {@code true}，
	 * {@linkplain #setDatabaseName 数据库名称}的当前值将被自动生成的名称覆盖。
	 * <p>子类可以调用此方法来强制初始化；不过，
	 * 此方法只应被调用一次。
	 * <p>调用此方法后，{@link #getDataSource()} 会返回提供数据库连接的
	 * {@link DataSource}。
	 */
	protected void initDatabase() {
		if (this.generateUniqueDatabaseName) {
			setDatabaseName(UUID.randomUUID().toString());
		}

		// Create the embedded database first
		if (this.databaseConfigurer == null) {
			this.databaseConfigurer = EmbeddedDatabaseConfigurerFactory.getConfigurer(EmbeddedDatabaseType.HSQL);
		}
		this.databaseConfigurer.configureConnectionProperties(
				this.dataSourceFactory.getConnectionProperties(), this.databaseName);
		this.dataSource = this.dataSourceFactory.getDataSource();

		if (logger.isInfoEnabled()) {
			if (this.dataSource instanceof SimpleDriverDataSource) {
				SimpleDriverDataSource simpleDriverDataSource = (SimpleDriverDataSource) this.dataSource;
				logger.info(String.format("Starting embedded database: url='%s', username='%s'",
						simpleDriverDataSource.getUrl(), simpleDriverDataSource.getUsername()));
			}
			else {
				logger.info(String.format("Starting embedded database '%s'", this.databaseName));
			}
		}

		// Now populate the database
		if (this.databasePopulator != null) {
			try {
				DatabasePopulatorUtils.execute(this.databasePopulator, this.dataSource);
			}
			catch (RuntimeException ex) {
				// failed to populate, so leave it as not initialized
				shutdownDatabase();
				throw ex;
			}
		}
	}

	/**
	 * 用于关闭嵌入式数据库的钩子。子类可以调用此方法来强制关闭。
	 * <p>调用后，{@link #getDataSource()} 返回 {@code null}。
	 * <p>如果尚未初始化任何嵌入式数据库，则不做任何操作。
	 */
	protected void shutdownDatabase() {
		if (this.dataSource != null) {
			if (logger.isInfoEnabled()) {
				if (this.dataSource instanceof SimpleDriverDataSource) {
					logger.info(String.format("Shutting down embedded database: url='%s'",
						((SimpleDriverDataSource) this.dataSource).getUrl()));
				}
				else {
					logger.info(String.format("Shutting down embedded database '%s'", this.databaseName));
				}
			}
			if (this.databaseConfigurer != null) {
				this.databaseConfigurer.shutdown(this.dataSource, this.databaseName);
			}
			this.dataSource = null;
		}
	}

	/**
	 * 获取提供嵌入式数据库连接的 {@link DataSource} 的钩子。
	 * <p>如果 {@code DataSource} 尚未初始化或数据库已关闭，则返回 {@code null}。
	 * 子类可以调用此方法直接访问 {@code DataSource} 实例。
	 */
	@Nullable
	protected final DataSource getDataSource() {
		return this.dataSource;
	}


	private class EmbeddedDataSourceProxy implements EmbeddedDatabase {

		private final DataSource dataSource;

		public EmbeddedDataSourceProxy(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public Connection getConnection() throws SQLException {
			return this.dataSource.getConnection();
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return this.dataSource.getConnection(username, password);
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return this.dataSource.getLogWriter();
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
			this.dataSource.setLogWriter(out);
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return this.dataSource.getLoginTimeout();
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
			this.dataSource.setLoginTimeout(seconds);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return this.dataSource.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return this.dataSource.isWrapperFor(iface);
		}

		// getParentLogger() is required for JDBC 4.1 compatibility
		@Override
		public Logger getParentLogger() {
			return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		}

		@Override
		public void shutdown() {
			shutdownDatabase();
		}
	}

}
