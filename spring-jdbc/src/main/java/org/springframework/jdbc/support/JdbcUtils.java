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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

/**
 * 用于 JDBC 操作的通用工具方法。主要用于框架内部，
 * 但也可用于自定义的 JDBC 访问代码。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public abstract class JdbcUtils {

	/**
	 * 表示未知（或未指定）SQL 类型的常量。
	 * @see java.sql.Types
	 */
	public static final int TYPE_UNKNOWN = Integer.MIN_VALUE;

	private static final Log logger = LogFactory.getLog(JdbcUtils.class);

	private static final Map<Integer, String> typeNames = new HashMap<>();

	static {
		try {
			for (Field field : Types.class.getFields()) {
				typeNames.put((Integer) field.get(null), field.getName());
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to resolve JDBC Types constants", ex);
		}
	}


	/**
	 * 关闭给定的 JDBC Connection，并忽略任何抛出的异常。
	 * 这对于手动 JDBC 代码中典型的 finally 块很有用。
	 * @param con the JDBC Connection to close (may be {@code null})
	 */
	public static void closeConnection(@Nullable Connection con) {
		if (con != null) {
			try {
				con.close();
			}
			catch (SQLException ex) {
				logger.debug("Could not close JDBC Connection", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JDBC driver: It might throw RuntimeException or Error.
				logger.debug("Unexpected exception on closing JDBC Connection", ex);
			}
		}
	}

	/**
	 * 关闭给定的 JDBC Statement，并忽略任何抛出的异常。
	 * 这对于手动 JDBC 代码中典型的 finally 块很有用。
	 * @param stmt the JDBC Statement to close (may be {@code null})
	 */
	public static void closeStatement(@Nullable Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			}
			catch (SQLException ex) {
				logger.trace("Could not close JDBC Statement", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JDBC driver: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing JDBC Statement", ex);
			}
		}
	}

	/**
	 * 关闭给定的 JDBC ResultSet，并忽略任何抛出的异常。
	 * 这对于手动 JDBC 代码中典型的 finally 块很有用。
	 * @param rs the JDBC ResultSet to close (may be {@code null})
	 */
	public static void closeResultSet(@Nullable ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			}
			catch (SQLException ex) {
				logger.trace("Could not close JDBC ResultSet", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JDBC driver: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing JDBC ResultSet", ex);
			}
		}
	}

	/**
	 * 从 ResultSet 中获取 JDBC 列值，使用指定的值类型。
	 * <p>使用特定类型的 ResultSet 访问器方法，对于未知类型则回退到
	 * {@link #getResultSetValue(java.sql.ResultSet, int)}。
	 * <p>请注意，对于未知类型，返回的值可能无法赋值给指定的必需类型。
	 * 调用方代码需要妥善处理这种情况，例如抛出相应的异常。
	 * @param rs is the ResultSet holding the data
	 * @param index is the column index
	 * @param requiredType the required value type (may be {@code null})
	 * @return the value object (possibly not of the specified required type,
	 * with further conversion steps necessary)
	 * @throws SQLException if thrown by the JDBC API
	 * @see #getResultSetValue(ResultSet, int)
	 */
	@Nullable
	public static Object getResultSetValue(ResultSet rs, int index, @Nullable Class<?> requiredType) throws SQLException {
		if (requiredType == null) {
			return getResultSetValue(rs, index);
		}

		Object value;

		// Explicitly extract typed value, as far as possible.
		if (String.class == requiredType) {
			return rs.getString(index);
		}
		else if (boolean.class == requiredType || Boolean.class == requiredType) {
			value = rs.getBoolean(index);
		}
		else if (byte.class == requiredType || Byte.class == requiredType) {
			value = rs.getByte(index);
		}
		else if (short.class == requiredType || Short.class == requiredType) {
			value = rs.getShort(index);
		}
		else if (int.class == requiredType || Integer.class == requiredType) {
			value = rs.getInt(index);
		}
		else if (long.class == requiredType || Long.class == requiredType) {
			value = rs.getLong(index);
		}
		else if (float.class == requiredType || Float.class == requiredType) {
			value = rs.getFloat(index);
		}
		else if (double.class == requiredType || Double.class == requiredType ||
				Number.class == requiredType) {
			value = rs.getDouble(index);
		}
		else if (BigDecimal.class == requiredType) {
			return rs.getBigDecimal(index);
		}
		else if (java.sql.Date.class == requiredType) {
			return rs.getDate(index);
		}
		else if (java.sql.Time.class == requiredType) {
			return rs.getTime(index);
		}
		else if (java.sql.Timestamp.class == requiredType || java.util.Date.class == requiredType) {
			return rs.getTimestamp(index);
		}
		else if (byte[].class == requiredType) {
			return rs.getBytes(index);
		}
		else if (Blob.class == requiredType) {
			return rs.getBlob(index);
		}
		else if (Clob.class == requiredType) {
			return rs.getClob(index);
		}
		else if (requiredType.isEnum()) {
			// Enums can either be represented through a String or an enum index value:
			// leave enum type conversion up to the caller (e.g. a ConversionService)
			// but make sure that we return nothing other than a String or an Integer.
			Object obj = rs.getObject(index);
			if (obj instanceof String) {
				return obj;
			}
			else if (obj instanceof Number) {
				// Defensively convert any Number to an Integer (as needed by our
				// ConversionService's IntegerToEnumConverterFactory) for use as index
				return NumberUtils.convertNumberToTargetClass((Number) obj, Integer.class);
			}
			else {
				// e.g. on Postgres: getObject returns a PGObject but we need a String
				return rs.getString(index);
			}
		}

		else {
			// Some unknown type desired -> rely on getObject.
			try {
				return rs.getObject(index, requiredType);
			}
			catch (AbstractMethodError err) {
				logger.debug("JDBC driver does not implement JDBC 4.1 'getObject(int, Class)' method", err);
			}
			catch (SQLFeatureNotSupportedException ex) {
				logger.debug("JDBC driver does not support JDBC 4.1 'getObject(int, Class)' method", ex);
			}
			catch (SQLException ex) {
				logger.debug("JDBC driver has limited support for JDBC 4.1 'getObject(int, Class)' method", ex);
			}

			// Corresponding SQL types for JSR-310 / Joda-Time types, left up
			// to the caller to convert them (e.g. through a ConversionService).
			String typeName = requiredType.getSimpleName();
			if ("LocalDate".equals(typeName)) {
				return rs.getDate(index);
			}
			else if ("LocalTime".equals(typeName)) {
				return rs.getTime(index);
			}
			else if ("LocalDateTime".equals(typeName)) {
				return rs.getTimestamp(index);
			}

			// Fall back to getObject without type specification, again
			// left up to the caller to convert the value if necessary.
			return getResultSetValue(rs, index);
		}

		// Perform was-null check if necessary (for results that the JDBC driver returns as primitives).
		return (rs.wasNull() ? null : value);
	}

	/**
	 * 从 ResultSet 中获取 JDBC 列值，使用最合适的值类型。返回的值应是脱离
	 * ResultSet 的值对象，与活动的 ResultSet 没有任何关联：特别是它不应是 Blob
	 * 或 Clob 对象，而应分别是对应的字节数组或 String 表示形式。
	 * <p>使用 {@code getObject(index)} 方法，但包含额外的“技巧”以绕开
	 * Oracle 10g 的问题：其 TIMESTAMP 数据类型返回非标准对象，DATE 列则返回
	 * 缺少时间部分的 {@code java.sql.Date}。这些列将被显式提取为标准
	 * {@code java.sql.Timestamp} 对象。
	 * @param rs is the ResultSet holding the data
	 * @param index is the column index
	 * @return the value object
	 * @throws SQLException if thrown by the JDBC API
	 * @see java.sql.Blob
	 * @see java.sql.Clob
	 * @see java.sql.Timestamp
	 */
	@Nullable
	public static Object getResultSetValue(ResultSet rs, int index) throws SQLException {
		Object obj = rs.getObject(index);
		String className = null;
		if (obj != null) {
			className = obj.getClass().getName();
		}
		if (obj instanceof Blob) {
			Blob blob = (Blob) obj;
			obj = blob.getBytes(1, (int) blob.length());
		}
		else if (obj instanceof Clob) {
			Clob clob = (Clob) obj;
			obj = clob.getSubString(1, (int) clob.length());
		}
		else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
			obj = rs.getTimestamp(index);
		}
		else if (className != null && className.startsWith("oracle.sql.DATE")) {
			String metaDataClassName = rs.getMetaData().getColumnClassName(index);
			if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
				obj = rs.getTimestamp(index);
			}
			else {
				obj = rs.getDate(index);
			}
		}
		else if (obj instanceof java.sql.Date) {
			if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
				obj = rs.getTimestamp(index);
			}
		}
		return obj;
	}

	/**
	 * 通过给定的 DatabaseMetaDataCallback 提取数据库元数据。
	 * <p>此方法将打开与数据库的连接并获取其元数据。由于此方法的调用发生在为
	 * DataSource 配置异常转换功能之前，因此它本身无法依赖 SQLException 转换。
	 * <p>任何异常都将包装在 MetaDataAccessException 中。这是一个受检异常，
	 * 所有调用方代码都应捕获并处理它。你可以仅记录错误并碰碰运气，但当你
	 * 再次尝试访问数据库时，很可能会重现一个更严重的错误。
	 * @param dataSource the DataSource to extract meta-data for
	 * @param action callback that will do the actual work
	 * @return object containing the extracted information, as returned by
	 * the DatabaseMetaDataCallback's {@code processMetaData} method
	 * @throws MetaDataAccessException if meta-data access failed
	 * @see java.sql.DatabaseMetaData
	 */
	public static <T> T extractDatabaseMetaData(DataSource dataSource, DatabaseMetaDataCallback<T> action)
			throws MetaDataAccessException {

		Connection con = null;
		try {
			con = DataSourceUtils.getConnection(dataSource);
			DatabaseMetaData metaData;
			try {
				metaData = con.getMetaData();
			}
			catch (SQLException ex) {
				if (DataSourceUtils.isConnectionTransactional(con, dataSource)) {
					// Probably a closed thread-bound Connection - retry against fresh Connection
					DataSourceUtils.releaseConnection(con, dataSource);
					con = null;
					logger.debug("Failed to obtain DatabaseMetaData from transactional Connection - " +
							"retrying against fresh Connection", ex);
					con = dataSource.getConnection();
					metaData = con.getMetaData();
				}
				else {
					throw ex;
				}
			}
			if (metaData == null) {
				// should only happen in test environments
				throw new MetaDataAccessException("DatabaseMetaData returned by Connection [" + con + "] was null");
			}
			return action.processMetaData(metaData);
		}
		catch (CannotGetJdbcConnectionException ex) {
			throw new MetaDataAccessException("Could not get Connection for extracting meta-data", ex);
		}
		catch (SQLException ex) {
			throw new MetaDataAccessException("Error while extracting DatabaseMetaData", ex);
		}
		catch (AbstractMethodError err) {
			throw new MetaDataAccessException(
					"JDBC DatabaseMetaData method not implemented by JDBC driver - upgrade your driver", err);
		}
		finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}
	}

	/**
	 * 针对给定的 DataSource 调用 DatabaseMetaData 上的指定方法，
	 * 并提取调用结果。
	 * @param dataSource the DataSource to extract meta-data for
	 * @param metaDataMethodName the name of the DatabaseMetaData method to call
	 * @return the object returned by the specified DatabaseMetaData method
	 * @throws MetaDataAccessException if we couldn't access the DatabaseMetaData
	 * or failed to invoke the specified method
	 * @see java.sql.DatabaseMetaData
	 * @deprecated as of 5.2.9, in favor of
	 * {@link #extractDatabaseMetaData(DataSource, DatabaseMetaDataCallback)}
	 * with a lambda expression or method reference and a generically typed result
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public static <T> T extractDatabaseMetaData(DataSource dataSource, final String metaDataMethodName)
			throws MetaDataAccessException {

		return (T) extractDatabaseMetaData(dataSource,
				dbmd -> {
					try {
						return DatabaseMetaData.class.getMethod(metaDataMethodName).invoke(dbmd);
					}
					catch (NoSuchMethodException ex) {
						throw new MetaDataAccessException("No method named '" + metaDataMethodName +
								"' found on DatabaseMetaData instance [" + dbmd + "]", ex);
					}
					catch (IllegalAccessException ex) {
						throw new MetaDataAccessException(
								"Could not access DatabaseMetaData method '" + metaDataMethodName + "'", ex);
					}
					catch (InvocationTargetException ex) {
						if (ex.getTargetException() instanceof SQLException) {
							throw (SQLException) ex.getTargetException();
						}
						throw new MetaDataAccessException(
								"Invocation of DatabaseMetaData method '" + metaDataMethodName + "' failed", ex);
					}
				});
	}

	/**
	 * 返回给定的 JDBC 驱动是否支持 JDBC 2.0 批量更新。
	 * <p>通常在执行某组语句之前立即调用：用于决定这组 SQL 语句应该通过
	 * JDBC 2.0 批处理机制执行，还是采用传统的逐条执行方式。
	 * <p>如果 "supportsBatchUpdates" 方法抛出异常，则记录一条警告，
	 * 并在这种情况下直接返回 {@code false}。
	 * @param con the Connection to check
	 * @return whether JDBC 2.0 batch updates are supported
	 * @see java.sql.DatabaseMetaData#supportsBatchUpdates()
	 */
	public static boolean supportsBatchUpdates(Connection con) {
		try {
			DatabaseMetaData dbmd = con.getMetaData();
			if (dbmd != null) {
				if (dbmd.supportsBatchUpdates()) {
					logger.debug("JDBC driver supports batch updates");
					return true;
				}
				else {
					logger.debug("JDBC driver does not support batch updates");
				}
			}
		}
		catch (SQLException ex) {
			logger.debug("JDBC driver 'supportsBatchUpdates' method threw exception", ex);
		}
		return false;
	}

	/**
	 * 提取所用目标数据库的通用名称，即使各种驱动/平台在运行时提供了
	 * 不同的名称。
	 * @param source the name as provided in database meta-data
	 * @return the common name to be used (e.g. "DB2" or "Sybase")
	 */
	@Nullable
	public static String commonDatabaseName(@Nullable String source) {
		String name = source;
		if (source != null && source.startsWith("DB2")) {
			name = "DB2";
		}
		else if ("MariaDB".equals(source)) {
			name = "MySQL";
		}
		else if ("Sybase SQL Server".equals(source) ||
				"Adaptive Server Enterprise".equals(source) ||
				"ASE".equals(source) ||
				"sql server".equalsIgnoreCase(source) ) {
			name = "Sybase";
		}
		return name;
	}

	/**
	 * 检查给定的 SQL 类型是否为数值类型。
	 * @param sqlType the SQL type to be checked
	 * @return whether the type is numeric
	 */
	public static boolean isNumeric(int sqlType) {
		return (Types.BIT == sqlType || Types.BIGINT == sqlType || Types.DECIMAL == sqlType ||
				Types.DOUBLE == sqlType || Types.FLOAT == sqlType || Types.INTEGER == sqlType ||
				Types.NUMERIC == sqlType || Types.REAL == sqlType || Types.SMALLINT == sqlType ||
				Types.TINYINT == sqlType);
	}

	/**
	 * 尽可能解析给定 SQL 类型的标准类型名称。
	 * @param sqlType the SQL type to resolve
	 * @return the corresponding constant name in {@link java.sql.Types}
	 * (e.g. "VARCHAR"/"NUMERIC"), or {@code null} if not resolvable
	 * @since 5.2
	 */
	@Nullable
	public static String resolveTypeName(int sqlType) {
		return typeNames.get(sqlType);
	}

	/**
	 * 确定要使用的列名。列名基于通过 ResultSetMetaData 的查找来确定。
	 * <p>此方法的实现考虑了 JDBC 4.0 规范中近期的澄清说明：
	 * <p><i>columnLabel —— 使用 SQL AS 子句为列指定的标签。
	 * 如果未指定 SQL AS 子句，则该标签就是列的名称</i>。
	 * @param resultSetMetaData the current meta-data to use
	 * @param columnIndex the index of the column for the lookup
	 * @return the column name to use
	 * @throws SQLException in case of lookup failure
	 */
	public static String lookupColumnName(ResultSetMetaData resultSetMetaData, int columnIndex) throws SQLException {
		String name = resultSetMetaData.getColumnLabel(columnIndex);
		if (!StringUtils.hasLength(name)) {
			name = resultSetMetaData.getColumnName(columnIndex);
		}
		return name;
	}

	/**
	 * 使用“驼峰命名法”将带下划线的列名转换为对应的属性名。
	 * 例如 "customer_number" 这样的名称将匹配 "customerNumber" 属性名。
	 * @param name the column name to be converted
	 * @return the name using "camel case"
	 */
	public static String convertUnderscoreNameToPropertyName(@Nullable String name) {
		StringBuilder result = new StringBuilder();
		boolean nextIsUpper = false;
		if (name != null && name.length() > 0) {
			if (name.length() > 1 && name.charAt(1) == '_') {
				result.append(Character.toUpperCase(name.charAt(0)));
			}
			else {
				result.append(Character.toLowerCase(name.charAt(0)));
			}
			for (int i = 1; i < name.length(); i++) {
				char c = name.charAt(i);
				if (c == '_') {
					nextIsUpper = true;
				}
				else {
					if (nextIsUpper) {
						result.append(Character.toUpperCase(c));
						nextIsUpper = false;
					}
					else {
						result.append(Character.toLowerCase(c));
					}
				}
			}
		}
		return result.toString();
	}

}
