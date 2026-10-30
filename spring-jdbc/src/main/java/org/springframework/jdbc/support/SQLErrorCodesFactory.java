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

import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * 工厂类，用于根据从 {@link java.sql.DatabaseMetaData} 中获取的
 * "databaseProductName" 创建 {@link SQLErrorCodes}。
 *
 * <p>返回填充了厂商错误码的 {@code SQLErrorCodes}，这些错误码定义在名为
 * "sql-error-codes.xml" 的配置文件中。如果类路径根目录（例如 "/WEB-INF/classes"
 * 目录）下存在同名文件覆盖默认配置，则读取该文件；否则读取本包中的默认文件。
 *
 * @author Thomas Risberg
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
 */
public class SQLErrorCodesFactory {

	/**
	 * 自定义 SQL 错误码文件的名称，从类路径根目录加载
	 * （例如 "/WEB-INF/classes" 目录）。
	 */
	public static final String SQL_ERROR_CODE_OVERRIDE_PATH = "sql-error-codes.xml";

	/**
	 * 默认 SQL 错误码文件的名称，从类路径加载。
	 */
	public static final String SQL_ERROR_CODE_DEFAULT_PATH = "org/springframework/jdbc/support/sql-error-codes.xml";


	private static final Log logger = LogFactory.getLog(SQLErrorCodesFactory.class);

	/**
	 * 跟踪单个实例，以便能够将其返回给请求它的类。
	 */
	private static final SQLErrorCodesFactory instance = new SQLErrorCodesFactory();


	/**
	 * 返回单例实例。
	 */
	public static SQLErrorCodesFactory getInstance() {
		return instance;
	}


	/**
	 * 用于保存配置文件中定义的所有数据库的错误码的 Map。
	 * 键为数据库产品名称，值为 SQLErrorCodes 实例。
	 */
	private final Map<String, SQLErrorCodes> errorCodesMap;

	/**
	 * 用于按 DataSource 缓存 SQLErrorCodes 实例的 Map。
	 */
	private final Map<DataSource, SQLErrorCodes> dataSourceCache = new ConcurrentReferenceHashMap<>(16);


	/**
	 * 创建一个新的 {@link SQLErrorCodesFactory} 类实例。
	 * <p>声明为非 public 以强制实现单例设计模式。之所以不是 private，是为了允许
	 * 通过重写 {@link #loadResource(String)} 方法进行测试。
	 * <p><b>请勿在应用程序代码中创建子类。</b>
	 * @see #loadResource(String)
	 */
	protected SQLErrorCodesFactory() {
		Map<String, SQLErrorCodes> errorCodes;

		try {
			DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
			lbf.setBeanClassLoader(getClass().getClassLoader());
			XmlBeanDefinitionReader bdr = new XmlBeanDefinitionReader(lbf);

			// Load default SQL error codes.
			Resource resource = loadResource(SQL_ERROR_CODE_DEFAULT_PATH);
			if (resource != null && resource.exists()) {
				bdr.loadBeanDefinitions(resource);
			}
			else {
				logger.info("Default sql-error-codes.xml not found (should be included in spring-jdbc jar)");
			}

			// Load custom SQL error codes, overriding defaults.
			resource = loadResource(SQL_ERROR_CODE_OVERRIDE_PATH);
			if (resource != null && resource.exists()) {
				bdr.loadBeanDefinitions(resource);
				logger.debug("Found custom sql-error-codes.xml file at the root of the classpath");
			}

			// Check all beans of type SQLErrorCodes.
			errorCodes = lbf.getBeansOfType(SQLErrorCodes.class, true, false);
			if (logger.isTraceEnabled()) {
				logger.trace("SQLErrorCodes loaded: " + errorCodes.keySet());
			}
		}
		catch (BeansException ex) {
			logger.warn("Error loading SQL error codes from config file", ex);
			errorCodes = Collections.emptyMap();
		}

		this.errorCodesMap = errorCodes;
	}

	/**
	 * 从类路径加载给定的资源。
	 * <p><b>应用程序开发者不应重写此方法，而应通过静态的 {@link #getInstance()}
	 * 方法获取此类的实例。</b>
	 * <p>声明为 protected 是为了便于测试。
	 * @param path 资源路径；可以是自定义路径，
	 * 也可以是 {@link #SQL_ERROR_CODE_DEFAULT_PATH} 或
	 * {@link #SQL_ERROR_CODE_OVERRIDE_PATH} 之一。
	 * @return 资源，如果未找到该资源则为 {@code null}
	 * @see #getInstance
	 */
	@Nullable
	protected Resource loadResource(String path) {
		return new ClassPathResource(path, getClass().getClassLoader());
	}


	/**
	 * 返回给定数据库对应的 {@link SQLErrorCodes} 实例。
	 * <p>无需进行数据库元数据查找。
	 * @param databaseName 数据库名称（不能为 {@code null}）
	 * @return 给定数据库的 {@code SQLErrorCodes} 实例
	 * （永不为 {@code null}；可能为空）
	 * @throws IllegalArgumentException 如果提供的数据库名称为 {@code null}
	 */
	public SQLErrorCodes getErrorCodes(String databaseName) {
		Assert.notNull(databaseName, "Database product name must not be null");

		SQLErrorCodes sec = this.errorCodesMap.get(databaseName);
		if (sec == null) {
			for (SQLErrorCodes candidate : this.errorCodesMap.values()) {
				if (PatternMatchUtils.simpleMatch(candidate.getDatabaseProductNames(), databaseName)) {
					sec = candidate;
					break;
				}
			}
		}
		if (sec != null) {
			checkCustomTranslatorRegistry(databaseName, sec);
			if (logger.isDebugEnabled()) {
				logger.debug("SQL error codes for '" + databaseName + "' found");
			}
			return sec;
		}

		// Could not find the database among the defined ones.
		if (logger.isDebugEnabled()) {
			logger.debug("SQL error codes for '" + databaseName + "' not found");
		}
		return new SQLErrorCodes();
	}

	/**
	 * 返回给定 {@link DataSource} 对应的 {@link SQLErrorCodes}，通过
	 * {@link java.sql.DatabaseMetaData} 评估 "databaseProductName"；
	 * 如果未找到任何 {@code SQLErrorCodes}，则返回空的错误码实例。
	 * @param dataSource 标识数据库的 {@code DataSource}
	 * @return 对应的 {@code SQLErrorCodes} 对象
	 * （永不为 {@code null}；可能为空）
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	public SQLErrorCodes getErrorCodes(DataSource dataSource) {
		SQLErrorCodes sec = resolveErrorCodes(dataSource);
		return (sec != null ? sec : new SQLErrorCodes());
	}

	/**
	 * 返回给定 {@link DataSource} 对应的 {@link SQLErrorCodes}，通过
	 * {@link java.sql.DatabaseMetaData} 评估 "databaseProductName"；
	 * 如果出现 JDBC 元数据访问问题，则返回 {@code null}。
	 * @param dataSource 标识数据库的 {@code DataSource}
	 * @return 对应的 {@code SQLErrorCodes} 对象，
	 * 如果出现 JDBC 元数据访问问题则为 {@code null}
	 * @since 5.2.9
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	@Nullable
	public SQLErrorCodes resolveErrorCodes(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up default SQLErrorCodes for DataSource [" + identify(dataSource) + "]");
		}

		// Try efficient lock-free access for existing cache entry
		SQLErrorCodes sec = this.dataSourceCache.get(dataSource);
		if (sec == null) {
			synchronized (this.dataSourceCache) {
				// Double-check within full dataSourceCache lock
				sec = this.dataSourceCache.get(dataSource);
				if (sec == null) {
					// We could not find it - got to look it up.
					try {
						String name = JdbcUtils.extractDatabaseMetaData(dataSource,
								DatabaseMetaData::getDatabaseProductName);
						if (StringUtils.hasLength(name)) {
							return registerDatabase(dataSource, name);
						}
					}
					catch (MetaDataAccessException ex) {
						logger.warn("Error while extracting database name", ex);
					}
					return null;
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("SQLErrorCodes found in cache for DataSource [" + identify(dataSource) + "]");
		}

		return sec;
	}

	/**
	 * 将指定的数据库名称与给定的 {@link DataSource} 关联起来。
	 * @param dataSource 标识数据库的 {@code DataSource}
	 * @param databaseName 错误码定义文件中所述的对应数据库名称
	 * （不能为 {@code null}）
	 * @return 对应的 {@code SQLErrorCodes} 对象（永不为 {@code null}）
	 * @see #unregisterDatabase(DataSource)
	 */
	public SQLErrorCodes registerDatabase(DataSource dataSource, String databaseName) {
		SQLErrorCodes sec = getErrorCodes(databaseName);
		if (logger.isDebugEnabled()) {
			logger.debug("Caching SQL error codes for DataSource [" + identify(dataSource) +
					"]: database product name is '" + databaseName + "'");
		}
		this.dataSourceCache.put(dataSource, sec);
		return sec;
	}

	/**
	 * 清除指定 {@link DataSource} 的缓存（如果已注册）。
	 * @param dataSource 标识数据库的 {@code DataSource}
	 * @return 被移除的对应 {@code SQLErrorCodes} 对象，
	 * 如果未注册则为 {@code null}
	 * @since 4.3.5
	 * @see #registerDatabase(DataSource, String)
	 */
	@Nullable
	public SQLErrorCodes unregisterDatabase(DataSource dataSource) {
		return this.dataSourceCache.remove(dataSource);
	}

	/**
	 * 为给定的 {@link DataSource} 构建标识字符串，主要用于日志记录。
	 * @param dataSource 要内省的 {@code DataSource}
	 * @return 标识字符串
	 */
	private String identify(DataSource dataSource) {
		return dataSource.getClass().getName() + '@' + Integer.toHexString(dataSource.hashCode());
	}

	/**
	 * 检查 {@link CustomSQLExceptionTranslatorRegistry} 中是否有对应条目。
	 */
	private void checkCustomTranslatorRegistry(String databaseName, SQLErrorCodes errorCodes) {
		SQLExceptionTranslator customTranslator =
				CustomSQLExceptionTranslatorRegistry.getInstance().findTranslatorForDatabase(databaseName);
		if (customTranslator != null) {
			if (errorCodes.getCustomSqlExceptionTranslator() != null && logger.isDebugEnabled()) {
				logger.debug("Overriding already defined custom translator '" +
						errorCodes.getCustomSqlExceptionTranslator().getClass().getSimpleName() +
						" with '" + customTranslator.getClass().getSimpleName() +
						"' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'");
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Using custom translator '" + customTranslator.getClass().getSimpleName() +
						"' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'");
			}
			errorCodes.setCustomSqlExceptionTranslator(customTranslator);
		}
	}

}
