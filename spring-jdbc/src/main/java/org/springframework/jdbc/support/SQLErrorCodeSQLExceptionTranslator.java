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

package org.springframework.jdbc.support;

import java.lang.reflect.Constructor;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.function.SupplierUtils;

/**
 * {@link SQLExceptionTranslator} 的实现，用于分析厂商特定的错误码。
 * 它比基于 SQL 状态的实现更为精确，但也高度依赖具体的数据库厂商。
 *
 * <p>本类应用以下匹配规则：
 * <ul>
 * <li>尝试由任意子类实现的自定义转换。请注意，本类是具体类，通常直接使用，
 * 在这种情况下此规则不适用。
 * <li>应用错误码匹配。错误码默认从 SQLErrorCodesFactory 获取。该工厂从类路径
 * 加载 "sql-error-codes.xml" 文件，为数据库元数据中的数据库名称定义错误码映射。
 * <li>回退到回退转换器。{@link SQLStateSQLExceptionTranslator} 是默认的回退转换器，
 * 仅分析异常的 SQL 状态。由于 Java 6 引入了自有的 {@code SQLException} 子类层次结构，
 * 我们默认使用 {@link SQLExceptionSubclassTranslator}，当未遇到特定子类时，
 * 它会转而回退到 Spring 自有的 SQL 状态转换。
 * </ul>
 *
 * <p>名为 "sql-error-codes.xml" 的配置文件默认从本包中读取。只要 Spring JDBC
 * 包从同一个 ClassLoader 加载，就可以通过类路径根目录（例如 "/WEB-INF/classes"
 * 目录）下的同名文件覆盖它。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see SQLErrorCodesFactory
 * @see SQLStateSQLExceptionTranslator
 */
public class SQLErrorCodeSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

	private static final int MESSAGE_ONLY_CONSTRUCTOR = 1;
	private static final int MESSAGE_THROWABLE_CONSTRUCTOR = 2;
	private static final int MESSAGE_SQLEX_CONSTRUCTOR = 3;
	private static final int MESSAGE_SQL_THROWABLE_CONSTRUCTOR = 4;
	private static final int MESSAGE_SQL_SQLEX_CONSTRUCTOR = 5;


	/** 此转换器使用的错误码。 */
	@Nullable
	private SingletonSupplier<SQLErrorCodes> sqlErrorCodes;


	/**
	 * 供作为 JavaBean 使用的构造函数。
	 * 必须设置 SqlErrorCodes 或 DataSource 属性。
	 */
	public SQLErrorCodeSQLExceptionTranslator() {
		setFallbackTranslator(new SQLExceptionSubclassTranslator());
	}

	/**
	 * 为给定的 DataSource 创建 SQL 错误码转换器。
	 * 调用此构造函数会导致从 DataSource 获取 Connection 以读取元数据。
	 * @param dataSource the DataSource to use to find meta-data and establish
	 * which error codes are usable
	 * @see SQLErrorCodesFactory
	 */
	public SQLErrorCodeSQLExceptionTranslator(DataSource dataSource) {
		this();
		setDataSource(dataSource);
	}

	/**
	 * 为给定的数据库产品名称创建 SQL 错误码转换器。
	 * 调用此构造函数可避免从 DataSource 获取 Connection 来读取元数据。
	 * @param dbName the database product name that identifies the error codes entry
	 * @see SQLErrorCodesFactory
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	public SQLErrorCodeSQLExceptionTranslator(String dbName) {
		this();
		setDatabaseProductName(dbName);
	}

	/**
	 * 使用给定的错误码创建 SQLErrorCode 转换器。
	 * 无需使用连接执行数据库元数据查找。
	 * @param sec error codes
	 */
	public SQLErrorCodeSQLExceptionTranslator(SQLErrorCodes sec) {
		this();
		this.sqlErrorCodes = SingletonSupplier.of(sec);
	}


	/**
	 * 设置此转换器使用的 DataSource。
	 * <p>设置此属性会导致从 DataSource 获取 Connection 以读取元数据。
	 * @param dataSource the DataSource to use to find meta-data and establish
	 * which error codes are usable
	 * @see SQLErrorCodesFactory#getErrorCodes(javax.sql.DataSource)
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	public void setDataSource(DataSource dataSource) {
		this.sqlErrorCodes =
				SingletonSupplier.of(() -> SQLErrorCodesFactory.getInstance().resolveErrorCodes(dataSource));
		this.sqlErrorCodes.get();  // try early initialization - otherwise the supplier will retry later
	}

	/**
	 * 设置此转换器使用的数据库产品名称。
	 * <p>设置此属性可避免从 DataSource 获取 Connection 来读取元数据。
	 * @param dbName the database product name that identifies the error codes entry
	 * @see SQLErrorCodesFactory#getErrorCodes(String)
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	public void setDatabaseProductName(String dbName) {
		this.sqlErrorCodes = SingletonSupplier.of(SQLErrorCodesFactory.getInstance().getErrorCodes(dbName));
	}

	/**
	 * 设置用于转换的自定义错误码。
	 * @param sec custom error codes to use
	 */
	public void setSqlErrorCodes(@Nullable SQLErrorCodes sec) {
		this.sqlErrorCodes = SingletonSupplier.ofNullable(sec);
	}

	/**
	 * 返回此转换器使用的错误码。
	 * 通常通过 DataSource 确定。
	 * @see #setDataSource
	 */
	@Nullable
	public SQLErrorCodes getSqlErrorCodes() {
		return SupplierUtils.resolve(this.sqlErrorCodes);
	}


	@Override
	@Nullable
	protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
		SQLException sqlEx = ex;
		if (sqlEx instanceof BatchUpdateException && sqlEx.getNextException() != null) {
			SQLException nestedSqlEx = sqlEx.getNextException();
			if (nestedSqlEx.getErrorCode() > 0 || nestedSqlEx.getSQLState() != null) {
				sqlEx = nestedSqlEx;
			}
		}

		// First, try custom translation from overridden method.
		DataAccessException dae = customTranslate(task, sql, sqlEx);
		if (dae != null) {
			return dae;
		}

		// Next, try the custom SQLException translator, if available.
		SQLErrorCodes sqlErrorCodes = getSqlErrorCodes();
		if (sqlErrorCodes != null) {
			SQLExceptionTranslator customTranslator = sqlErrorCodes.getCustomSqlExceptionTranslator();
			if (customTranslator != null) {
				DataAccessException customDex = customTranslator.translate(task, sql, sqlEx);
				if (customDex != null) {
					return customDex;
				}
			}
		}

		// Check SQLErrorCodes with corresponding error code, if available.
		if (sqlErrorCodes != null) {
			String errorCode;
			if (sqlErrorCodes.isUseSqlStateForTranslation()) {
				errorCode = sqlEx.getSQLState();
			}
			else {
				// Try to find SQLException with actual error code, looping through the causes.
				// E.g. applicable to java.sql.DataTruncation as of JDK 1.6.
				SQLException current = sqlEx;
				while (current.getErrorCode() == 0 && current.getCause() instanceof SQLException) {
					current = (SQLException) current.getCause();
				}
				errorCode = Integer.toString(current.getErrorCode());
			}

			if (errorCode != null) {
				// Look for defined custom translations first.
				CustomSQLErrorCodesTranslation[] customTranslations = sqlErrorCodes.getCustomTranslations();
				if (customTranslations != null) {
					for (CustomSQLErrorCodesTranslation customTranslation : customTranslations) {
						if (Arrays.binarySearch(customTranslation.getErrorCodes(), errorCode) >= 0 &&
								customTranslation.getExceptionClass() != null) {
							DataAccessException customException = createCustomException(
									task, sql, sqlEx, customTranslation.getExceptionClass());
							if (customException != null) {
								logTranslation(task, sql, sqlEx, true);
								return customException;
							}
						}
					}
				}
				// Next, look for grouped error codes.
				if (Arrays.binarySearch(sqlErrorCodes.getBadSqlGrammarCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new BadSqlGrammarException(task, (sql != null ? sql : ""), sqlEx);
				}
				else if (Arrays.binarySearch(sqlErrorCodes.getInvalidResultSetAccessCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new InvalidResultSetAccessException(task, (sql != null ? sql : ""), sqlEx);
				}
				else if (Arrays.binarySearch(sqlErrorCodes.getDuplicateKeyCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new DuplicateKeyException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(sqlErrorCodes.getDataIntegrityViolationCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new DataIntegrityViolationException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(sqlErrorCodes.getPermissionDeniedCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new PermissionDeniedDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(sqlErrorCodes.getDataAccessResourceFailureCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new DataAccessResourceFailureException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(sqlErrorCodes.getTransientDataAccessResourceCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new TransientDataAccessResourceException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(sqlErrorCodes.getCannotAcquireLockCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new CannotAcquireLockException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(sqlErrorCodes.getDeadlockLoserCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new DeadlockLoserDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(sqlErrorCodes.getCannotSerializeTransactionCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new CannotSerializeTransactionException(buildMessage(task, sql, sqlEx), sqlEx);
				}
			}
		}

		// We couldn't identify it more precisely - let's hand it over to the SQLState fallback translator.
		if (logger.isDebugEnabled()) {
			String codes;
			if (sqlErrorCodes != null && sqlErrorCodes.isUseSqlStateForTranslation()) {
				codes = "SQL state '" + sqlEx.getSQLState() + "', error code '" + sqlEx.getErrorCode();
			}
			else {
				codes = "Error code '" + sqlEx.getErrorCode() + "'";
			}
			logger.debug("Unable to translate SQLException with " + codes + ", will now try the fallback translator");
		}

		return null;
	}

	/**
	 * 子类可以重写此方法，尝试将 {@link SQLException} 自定义映射为
	 * {@link DataAccessException}。
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL query or update that caused the problem (may be {@code null})
	 * @param sqlEx the offending SQLException
	 * @return {@code null} if no custom translation applies, otherwise a {@link DataAccessException}
	 * resulting from custom translation. This exception should include the {@code sqlEx} parameter
	 * as a nested root cause. This implementation always returns {@code null}, meaning that the
	 * translator always falls back to the default error codes.
	 */
	@Nullable
	protected DataAccessException customTranslate(String task, @Nullable String sql, SQLException sqlEx) {
		return null;
	}

	/**
	 * 根据 {@link CustomSQLErrorCodesTranslation} 定义中给定的异常类，
	 * 创建自定义的 {@link DataAccessException}。
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL query or update that caused the problem (may be {@code null})
	 * @param sqlEx the offending SQLException
	 * @param exceptionClass the exception class to use, as defined in the
	 * {@link CustomSQLErrorCodesTranslation} definition
	 * @return {@code null} if the custom exception could not be created, otherwise
	 * the resulting {@link DataAccessException}. This exception should include the
	 * {@code sqlEx} parameter as a nested root cause.
	 * @see CustomSQLErrorCodesTranslation#setExceptionClass
	 */
	@Nullable
	protected DataAccessException createCustomException(
			String task, @Nullable String sql, SQLException sqlEx, Class<?> exceptionClass) {

		// Find appropriate constructor for the given exception class
		try {
			int constructorType = 0;
			Constructor<?>[] constructors = exceptionClass.getConstructors();
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length == 1 && String.class == parameterTypes[0] &&
						constructorType < MESSAGE_ONLY_CONSTRUCTOR) {
					constructorType = MESSAGE_ONLY_CONSTRUCTOR;
				}
				if (parameterTypes.length == 2 && String.class == parameterTypes[0] &&
						Throwable.class == parameterTypes[1] &&
						constructorType < MESSAGE_THROWABLE_CONSTRUCTOR) {
					constructorType = MESSAGE_THROWABLE_CONSTRUCTOR;
				}
				if (parameterTypes.length == 2 && String.class == parameterTypes[0] &&
						SQLException.class == parameterTypes[1] &&
						constructorType < MESSAGE_SQLEX_CONSTRUCTOR) {
					constructorType = MESSAGE_SQLEX_CONSTRUCTOR;
				}
				if (parameterTypes.length == 3 && String.class == parameterTypes[0] &&
						String.class == parameterTypes[1] && Throwable.class == parameterTypes[2] &&
						constructorType < MESSAGE_SQL_THROWABLE_CONSTRUCTOR) {
					constructorType = MESSAGE_SQL_THROWABLE_CONSTRUCTOR;
				}
				if (parameterTypes.length == 3 && String.class == parameterTypes[0] &&
						String.class == parameterTypes[1] && SQLException.class == parameterTypes[2] &&
						constructorType < MESSAGE_SQL_SQLEX_CONSTRUCTOR) {
					constructorType = MESSAGE_SQL_SQLEX_CONSTRUCTOR;
				}
			}

			// invoke constructor
			Constructor<?> exceptionConstructor;
			switch (constructorType) {
				case MESSAGE_SQL_SQLEX_CONSTRUCTOR:
					Class<?>[] messageAndSqlAndSqlExArgsClass = new Class<?>[] {String.class, String.class, SQLException.class};
					Object[] messageAndSqlAndSqlExArgs = new Object[] {task, sql, sqlEx};
					exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndSqlExArgsClass);
					return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndSqlExArgs);
				case MESSAGE_SQL_THROWABLE_CONSTRUCTOR:
					Class<?>[] messageAndSqlAndThrowableArgsClass = new Class<?>[] {String.class, String.class, Throwable.class};
					Object[] messageAndSqlAndThrowableArgs = new Object[] {task, sql, sqlEx};
					exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndThrowableArgsClass);
					return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndThrowableArgs);
				case MESSAGE_SQLEX_CONSTRUCTOR:
					Class<?>[] messageAndSqlExArgsClass = new Class<?>[] {String.class, SQLException.class};
					Object[] messageAndSqlExArgs = new Object[] {task + ": " + sqlEx.getMessage(), sqlEx};
					exceptionConstructor = exceptionClass.getConstructor(messageAndSqlExArgsClass);
					return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlExArgs);
				case MESSAGE_THROWABLE_CONSTRUCTOR:
					Class<?>[] messageAndThrowableArgsClass = new Class<?>[] {String.class, Throwable.class};
					Object[] messageAndThrowableArgs = new Object[] {task + ": " + sqlEx.getMessage(), sqlEx};
					exceptionConstructor = exceptionClass.getConstructor(messageAndThrowableArgsClass);
					return (DataAccessException)exceptionConstructor.newInstance(messageAndThrowableArgs);
				case MESSAGE_ONLY_CONSTRUCTOR:
					Class<?>[] messageOnlyArgsClass = new Class<?>[] {String.class};
					Object[] messageOnlyArgs = new Object[] {task + ": " + sqlEx.getMessage()};
					exceptionConstructor = exceptionClass.getConstructor(messageOnlyArgsClass);
					return (DataAccessException) exceptionConstructor.newInstance(messageOnlyArgs);
				default:
					if (logger.isWarnEnabled()) {
						logger.warn("Unable to find appropriate constructor of custom exception class [" +
								exceptionClass.getName() + "]");
					}
					return null;
				}
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Unable to instantiate custom exception class [" + exceptionClass.getName() + "]", ex);
			}
			return null;
		}
	}

	private void logTranslation(String task, @Nullable String sql, SQLException sqlEx, boolean custom) {
		if (logger.isDebugEnabled()) {
			String intro = custom ? "Custom translation of" : "Translating";
			logger.debug(intro + " SQLException with SQL state '" + sqlEx.getSQLState() +
					"', error code '" + sqlEx.getErrorCode() + "', message [" + sqlEx.getMessage() + "]" +
					(sql != null ? "; SQL was [" + sql + "]": "") + " for task [" + task + "]");
		}
	}

}
