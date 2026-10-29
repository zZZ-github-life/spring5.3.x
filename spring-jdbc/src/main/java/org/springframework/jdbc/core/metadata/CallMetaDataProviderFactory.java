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

package org.springframework.jdbc.core.metadata;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * 工厂类，用于根据所使用的数据库类型创建 {@link CallMetaDataProvider} 实现。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public final class CallMetaDataProviderFactory {

	/** 存储过程调用支持的数据库产品列表。 */
	public static final List<String> supportedDatabaseProductsForProcedures = Arrays.asList(
			"Apache Derby",
			"DB2",
			"Informix Dynamic Server",
			"MariaDB",
			"Microsoft SQL Server",
			"MySQL",
			"Oracle",
			"PostgreSQL",
			"Sybase"
		);

	/** 函数调用支持的数据库产品列表。 */
	public static final List<String> supportedDatabaseProductsForFunctions = Arrays.asList(
			"MariaDB",
			"Microsoft SQL Server",
			"MySQL",
			"Oracle",
			"PostgreSQL"
		);

	private static final Log logger = LogFactory.getLog(CallMetaDataProviderFactory.class);


	private CallMetaDataProviderFactory() {
	}


	/**
	 * 根据数据库元数据创建 {@link CallMetaDataProvider}。
	 * @param dataSource 用于检索元数据的 JDBC DataSource
	 * @param context 持有配置和元数据的类
	 * @return 要使用的 CallMetaDataProvider 实现的实例
	 */
	public static CallMetaDataProvider createMetaDataProvider(DataSource dataSource, final CallMetaDataContext context) {
		try {
			return JdbcUtils.extractDatabaseMetaData(dataSource, databaseMetaData -> {
				String databaseProductName = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
				boolean accessProcedureColumnMetaData = context.isAccessCallParameterMetaData();
				if (context.isFunction()) {
					if (!supportedDatabaseProductsForFunctions.contains(databaseProductName)) {
						if (logger.isInfoEnabled()) {
							logger.info(databaseProductName + " is not one of the databases fully supported for function calls " +
									"-- supported are: " + supportedDatabaseProductsForFunctions);
						}
						if (accessProcedureColumnMetaData) {
							logger.info("Metadata processing disabled - you must specify all parameters explicitly");
							accessProcedureColumnMetaData = false;
						}
					}
				}
				else {
					if (!supportedDatabaseProductsForProcedures.contains(databaseProductName)) {
						if (logger.isInfoEnabled()) {
							logger.info(databaseProductName + " is not one of the databases fully supported for procedure calls " +
									"-- supported are: " + supportedDatabaseProductsForProcedures);
						}
						if (accessProcedureColumnMetaData) {
							logger.info("Metadata processing disabled - you must specify all parameters explicitly");
							accessProcedureColumnMetaData = false;
						}
					}
				}

				CallMetaDataProvider provider;
				if ("Oracle".equals(databaseProductName)) {
					provider = new OracleCallMetaDataProvider(databaseMetaData);
				}
				else if ("PostgreSQL".equals(databaseProductName)) {
					provider = new PostgresCallMetaDataProvider((databaseMetaData));
				}
				else if ("Apache Derby".equals(databaseProductName)) {
					provider = new DerbyCallMetaDataProvider((databaseMetaData));
				}
				else if ("DB2".equals(databaseProductName)) {
					provider = new Db2CallMetaDataProvider((databaseMetaData));
				}
				else if ("HDB".equals(databaseProductName)) {
					provider = new HanaCallMetaDataProvider((databaseMetaData));
				}
				else if ("Microsoft SQL Server".equals(databaseProductName)) {
					provider = new SqlServerCallMetaDataProvider((databaseMetaData));
				}
				else if ("Sybase".equals(databaseProductName)) {
					provider = new SybaseCallMetaDataProvider((databaseMetaData));
				}
				else {
					provider = new GenericCallMetaDataProvider(databaseMetaData);
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Using " + provider.getClass().getName());
				}
				provider.initializeWithMetaData(databaseMetaData);
				if (accessProcedureColumnMetaData) {
					provider.initializeWithProcedureColumnMetaData(databaseMetaData,
							context.getCatalogName(), context.getSchemaName(), context.getProcedureName());
				}
				return provider;
			});
		}
		catch (MetaDataAccessException ex) {
			throw new DataAccessResourceFailureException("Error retrieving database meta-data", ex);
		}
	}

}
