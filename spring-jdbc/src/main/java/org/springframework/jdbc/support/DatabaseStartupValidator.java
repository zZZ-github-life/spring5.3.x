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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.lang.Nullable;

/**
 * 用于检查数据库是否已启动的 Bean。可以通过 "depends-on" 从依赖数据库启动的
 * bean 中引用它，例如 Hibernate SessionFactory 或直接访问 DataSource 的自定义
 * 数据访问对象。
 *
 * <p>适用于将应用程序初始化推迟到数据库启动之后。尤其适合等待启动缓慢的
 * Oracle 数据库。
 *
 * @author Juergen Hoeller
 * @author Marten Deinum
 * @since 18.12.2003
 */
public class DatabaseStartupValidator implements InitializingBean {

	/**
	 * 默认间隔。
	 */
	public static final int DEFAULT_INTERVAL = 1;

	/**
	 * 默认超时时间。
	 */
	public static final int DEFAULT_TIMEOUT = 60;


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private DataSource dataSource;

	@Nullable
	private String validationQuery;

	private int interval = DEFAULT_INTERVAL;

	private int timeout = DEFAULT_TIMEOUT;


	/**
	 * 设置要校验的 DataSource。
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 设置用于校验的 SQL 查询字符串。
	 * @deprecated as of 5.3, in favor of the JDBC 4.0 connection validation
	 */
	@Deprecated
	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	/**
	 * 设置两次校验运行之间的间隔（秒）。
	 * 默认值为 {@value #DEFAULT_INTERVAL}。
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}

	/**
	 * 设置超时时间（秒），超过该时间后将抛出致命异常。
	 * 默认值为 {@value #DEFAULT_TIMEOUT}。
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}


	/**
	 * 检查是否可以在指定 DataSource 的连接上执行校验查询，按指定的检查间隔
	 * 重试，直到达到指定的超时时间。
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.dataSource == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}

		try {
			boolean validated = false;
			long beginTime = System.currentTimeMillis();
			long deadLine = beginTime + TimeUnit.SECONDS.toMillis(this.timeout);
			SQLException latestEx = null;

			while (!validated && System.currentTimeMillis() < deadLine) {
				Connection con = null;
				Statement stmt = null;
				try {
					con = this.dataSource.getConnection();
					if (con == null) {
						throw new CannotGetJdbcConnectionException("Failed to execute validation: " +
								"DataSource returned null from getConnection(): " + this.dataSource);
					}
					if (this.validationQuery == null) {
						validated = con.isValid(this.interval);
					}
					else {
						stmt = con.createStatement();
						stmt.execute(this.validationQuery);
						validated = true;
					}
				}
				catch (SQLException ex) {
					latestEx = ex;
					if (logger.isDebugEnabled()) {
						if (this.validationQuery != null) {
							logger.debug("Validation query [" + this.validationQuery + "] threw exception", ex);
						}
						else {
							logger.debug("Validation check threw exception", ex);
						}
					}
					if (logger.isInfoEnabled()) {
						float rest = ((float) (deadLine - System.currentTimeMillis())) / 1000;
						if (rest > this.interval) {
							logger.info("Database has not started up yet - retrying in " + this.interval +
									" seconds (timeout in " + rest + " seconds)");
						}
					}
				}
				finally {
					JdbcUtils.closeStatement(stmt);
					JdbcUtils.closeConnection(con);
				}

				if (!validated) {
					TimeUnit.SECONDS.sleep(this.interval);
				}
			}

			if (!validated) {
				throw new CannotGetJdbcConnectionException(
						"Database has not started up within " + this.timeout + " seconds", latestEx);
			}

			if (logger.isInfoEnabled()) {
				float duration = ((float) (System.currentTimeMillis() - beginTime)) / 1000;
				logger.info("Database startup detected after " + duration + " seconds");
			}
		}
		catch (InterruptedException ex) {
			// Re-interrupt current thread, to allow other threads to react.
			Thread.currentThread().interrupt();
		}
	}

}
