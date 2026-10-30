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

package org.springframework.jdbc.core;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.SpringProperties;
import org.springframework.jdbc.support.SqlValue;
import org.springframework.lang.Nullable;

/**
 * PreparedStatementSetter/Creator和CallableStatementCreator实现的工具方法，
 * 提供复杂的参数管理（包括对LOB值的支持）。
 *
 * <p>由PreparedStatementCreatorFactory和CallableStatementCreatorFactory使用，
 * 但也可在自定义setter/creator实现中直接使用。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 * @see PreparedStatementSetter
 * @see PreparedStatementCreator
 * @see CallableStatementCreator
 * @see PreparedStatementCreatorFactory
 * @see CallableStatementCreatorFactory
 * @see SqlParameter
 * @see SqlTypeValue
 * @see org.springframework.jdbc.core.support.SqlLobValue
 */
public abstract class StatementCreatorUtils {

	/**
	 * 系统属性，指示Spring完全忽略{@link java.sql.ParameterMetaData#getParameterType}，
	 * 即甚至不为{@link StatementCreatorUtils#setNull}调用尝试检索
	 * {@link PreparedStatement#getParameterMetaData()}。
	 * <p>默认为"false"，首先尝试{@code getParameterType}调用，
	 * 然后基于常见数据库的已知行为回退到{@link PreparedStatement#setNull}
	 * / {@link PreparedStatement#setObject}调用。
	 * <p>如果在运行时遇到错误行为，请考虑将此标志切换为"true"，
	 * 例如在{@code getParameterType}抛出异常时出现连接池问题（如在JBoss AS 7上报告的），
	 * 或在出现性能问题时（如在PostgreSQL上报告的）。
	 */
	public static final String IGNORE_GETPARAMETERTYPE_PROPERTY_NAME = "spring.jdbc.getParameterType.ignore";


	static boolean shouldIgnoreGetParameterType = SpringProperties.getFlag(IGNORE_GETPARAMETERTYPE_PROPERTY_NAME);

	private static final Log logger = LogFactory.getLog(StatementCreatorUtils.class);

	private static final Map<Class<?>, Integer> javaTypeToSqlTypeMap = new HashMap<>(32);

	static {
		javaTypeToSqlTypeMap.put(boolean.class, Types.BOOLEAN);
		javaTypeToSqlTypeMap.put(Boolean.class, Types.BOOLEAN);
		javaTypeToSqlTypeMap.put(byte.class, Types.TINYINT);
		javaTypeToSqlTypeMap.put(Byte.class, Types.TINYINT);
		javaTypeToSqlTypeMap.put(short.class, Types.SMALLINT);
		javaTypeToSqlTypeMap.put(Short.class, Types.SMALLINT);
		javaTypeToSqlTypeMap.put(int.class, Types.INTEGER);
		javaTypeToSqlTypeMap.put(Integer.class, Types.INTEGER);
		javaTypeToSqlTypeMap.put(long.class, Types.BIGINT);
		javaTypeToSqlTypeMap.put(Long.class, Types.BIGINT);
		javaTypeToSqlTypeMap.put(BigInteger.class, Types.BIGINT);
		javaTypeToSqlTypeMap.put(float.class, Types.FLOAT);
		javaTypeToSqlTypeMap.put(Float.class, Types.FLOAT);
		javaTypeToSqlTypeMap.put(double.class, Types.DOUBLE);
		javaTypeToSqlTypeMap.put(Double.class, Types.DOUBLE);
		javaTypeToSqlTypeMap.put(BigDecimal.class, Types.DECIMAL);
		javaTypeToSqlTypeMap.put(LocalDate.class, Types.DATE);
		javaTypeToSqlTypeMap.put(LocalTime.class, Types.TIME);
		javaTypeToSqlTypeMap.put(LocalDateTime.class, Types.TIMESTAMP);
		javaTypeToSqlTypeMap.put(java.sql.Date.class, Types.DATE);
		javaTypeToSqlTypeMap.put(java.sql.Time.class, Types.TIME);
		javaTypeToSqlTypeMap.put(java.sql.Timestamp.class, Types.TIMESTAMP);
		javaTypeToSqlTypeMap.put(Blob.class, Types.BLOB);
		javaTypeToSqlTypeMap.put(Clob.class, Types.CLOB);
	}


	/**
	 * 从给定的Java类型推导默认的SQL类型。
	 * @param javaType 要转换的 Java 类型
	 * @return 对应的 SQL 类型，如果未找到则为
	 * {@link SqlTypeValue#TYPE_UNKNOWN}
	 */
	public static int javaTypeToSqlParameterType(@Nullable Class<?> javaType) {
		if (javaType == null) {
			return SqlTypeValue.TYPE_UNKNOWN;
		}
		Integer sqlType = javaTypeToSqlTypeMap.get(javaType);
		if (sqlType != null) {
			return sqlType;
		}
		if (Number.class.isAssignableFrom(javaType)) {
			return Types.NUMERIC;
		}
		if (isStringValue(javaType)) {
			return Types.VARCHAR;
		}
		if (isDateValue(javaType) || Calendar.class.isAssignableFrom(javaType)) {
			return Types.TIMESTAMP;
		}
		return SqlTypeValue.TYPE_UNKNOWN;
	}

	/**
	 * 设置参数的值。使用的方法基于参数的SQL类型，
	 * 我们可以处理复杂类型如数组和LOB。
	 * @param ps 预编译语句或可调用语句
	 * @param paramIndex 正在设置的参数索引
	 * @param param 按声明形式给出的参数（包括类型）
	 * @param inValue 要设置的值
	 * @throws SQLException 如果由 PreparedStatement 方法抛出
	 */
	public static void setParameterValue(PreparedStatement ps, int paramIndex, SqlParameter param,
			@Nullable Object inValue) throws SQLException {

		setParameterValueInternal(ps, paramIndex, param.getSqlType(), param.getTypeName(), param.getScale(), inValue);
	}

	/**
	 * 设置参数的值。使用的方法基于参数的SQL类型，
	 * 我们可以处理复杂类型如数组和LOB。
	 * @param ps 预编译语句或可调用语句
	 * @param paramIndex 正在设置的参数索引
	 * @param sqlType 参数的 SQL 类型
	 * @param inValue 要设置的值（普通值或 SqlTypeValue）
	 * @throws SQLException 如果由 PreparedStatement 方法抛出
	 * @see SqlTypeValue
	 */
	public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType,
			@Nullable Object inValue) throws SQLException {

		setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
	}

	/**
	 * 设置参数的值。使用的方法基于参数的SQL类型，
	 * 我们可以处理复杂类型如数组和LOB。
	 * @param ps 预编译语句或可调用语句
	 * @param paramIndex 正在设置的参数索引
	 * @param sqlType 参数的 SQL 类型
	 * @param typeName 参数的类型名称
	 * （可选，仅用于 SQL NULL 和 SqlTypeValue）
	 * @param inValue 要设置的值（普通值或 SqlTypeValue）
	 * @throws SQLException 如果由 PreparedStatement 方法抛出
	 * @see SqlTypeValue
	 */
	public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName,
			@Nullable Object inValue) throws SQLException {

		setParameterValueInternal(ps, paramIndex, sqlType, typeName, null, inValue);
	}

	/**
	 * 设置参数的值。使用的方法基于参数的SQL类型，
	 * 我们可以处理复杂类型如数组和LOB。
	 * @param ps 预编译语句或可调用语句
	 * @param paramIndex 正在设置的参数索引
	 * @param sqlType 参数的 SQL 类型
	 * @param typeName 参数的类型名称
	 * （可选，仅用于 SQL NULL 和 SqlTypeValue）
	 * @param scale 小数点后的位数（用于 DECIMAL 和 NUMERIC 类型）
	 * @param inValue 要设置的值（普通值或 SqlTypeValue）
	 * @throws SQLException 如果由 PreparedStatement 方法抛出
	 * @see SqlTypeValue
	 */
	private static void setParameterValueInternal(PreparedStatement ps, int paramIndex, int sqlType,
			@Nullable String typeName, @Nullable Integer scale, @Nullable Object inValue) throws SQLException {

		String typeNameToUse = typeName;
		int sqlTypeToUse = sqlType;
		Object inValueToUse = inValue;

		// override type info?
		if (inValue instanceof SqlParameterValue) {
			SqlParameterValue parameterValue = (SqlParameterValue) inValue;
			if (logger.isDebugEnabled()) {
				logger.debug("Overriding type info with runtime info from SqlParameterValue: column index " + paramIndex +
						", SQL type " + parameterValue.getSqlType() + ", type name " + parameterValue.getTypeName());
			}
			if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
				sqlTypeToUse = parameterValue.getSqlType();
			}
			if (parameterValue.getTypeName() != null) {
				typeNameToUse = parameterValue.getTypeName();
			}
			inValueToUse = parameterValue.getValue();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Setting SQL statement parameter value: column index " + paramIndex +
					", parameter value [" + inValueToUse +
					"], value class [" + (inValueToUse != null ? inValueToUse.getClass().getName() : "null") +
					"], SQL type " + (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
		}

		if (inValueToUse == null) {
			setNull(ps, paramIndex, sqlTypeToUse, typeNameToUse);
		}
		else {
			setValue(ps, paramIndex, sqlTypeToUse, typeNameToUse, scale, inValueToUse);
		}
	}

	/**
	 * 将指定的PreparedStatement参数设置为null，
	 * 尊重数据库特定的行为。
	 */
	private static void setNull(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
			throws SQLException {

		if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER && typeName == null)) {
			boolean useSetObject = false;
			Integer sqlTypeToUse = null;
			if (!shouldIgnoreGetParameterType) {
				try {
					sqlTypeToUse = ps.getParameterMetaData().getParameterType(paramIndex);
				}
				catch (SQLException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("JDBC getParameterType call failed - using fallback method instead: " + ex);
					}
				}
			}
			if (sqlTypeToUse == null) {
				// Proceed with database-specific checks
				sqlTypeToUse = Types.NULL;
				DatabaseMetaData dbmd = ps.getConnection().getMetaData();
				String jdbcDriverName = dbmd.getDriverName();
				String databaseProductName = dbmd.getDatabaseProductName();
				if (databaseProductName.startsWith("Informix") ||
						(jdbcDriverName.startsWith("Microsoft") && jdbcDriverName.contains("SQL Server"))) {
						// "Microsoft SQL Server JDBC Driver 3.0" versus "Microsoft JDBC Driver 4.0 for SQL Server"
					useSetObject = true;
				}
				else if (databaseProductName.startsWith("DB2") ||
						jdbcDriverName.startsWith("jConnect") ||
						jdbcDriverName.startsWith("SQLServer") ||
						jdbcDriverName.startsWith("Apache Derby")) {
					sqlTypeToUse = Types.VARCHAR;
				}
			}
			if (useSetObject) {
				ps.setObject(paramIndex, null);
			}
			else {
				ps.setNull(paramIndex, sqlTypeToUse);
			}
		}
		else if (typeName != null) {
			ps.setNull(paramIndex, sqlType, typeName);
		}
		else {
			ps.setNull(paramIndex, sqlType);
		}
	}

	private static void setValue(PreparedStatement ps, int paramIndex, int sqlType,
			@Nullable String typeName, @Nullable Integer scale, Object inValue) throws SQLException {

		if (inValue instanceof SqlTypeValue) {
			((SqlTypeValue) inValue).setTypeValue(ps, paramIndex, sqlType, typeName);
		}
		else if (inValue instanceof SqlValue) {
			((SqlValue) inValue).setValue(ps, paramIndex);
		}
		else if (sqlType == Types.VARCHAR || sqlType == Types.LONGVARCHAR ) {
			ps.setString(paramIndex, inValue.toString());
		}
		else if (sqlType == Types.NVARCHAR || sqlType == Types.LONGNVARCHAR) {
			ps.setNString(paramIndex, inValue.toString());
		}
		else if ((sqlType == Types.CLOB || sqlType == Types.NCLOB) && isStringValue(inValue.getClass())) {
			String strVal = inValue.toString();
			if (strVal.length() > 4000) {
				// Necessary for older Oracle drivers, in particular when running against an Oracle 10 database.
				// Should also work fine against other drivers/databases since it uses standard JDBC 4.0 API.
				if (sqlType == Types.NCLOB) {
					ps.setNClob(paramIndex, new StringReader(strVal), strVal.length());
				}
				else {
					ps.setClob(paramIndex, new StringReader(strVal), strVal.length());
				}
			}
			else {
				// Fallback: setString or setNString binding
				if (sqlType == Types.NCLOB) {
					ps.setNString(paramIndex, strVal);
				}
				else {
					ps.setString(paramIndex, strVal);
				}
			}
		}
		else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) {
			if (inValue instanceof BigDecimal) {
				ps.setBigDecimal(paramIndex, (BigDecimal) inValue);
			}
			else if (scale != null) {
				ps.setObject(paramIndex, inValue, sqlType, scale);
			}
			else {
				ps.setObject(paramIndex, inValue, sqlType);
			}
		}
		else if (sqlType == Types.BOOLEAN) {
			if (inValue instanceof Boolean) {
				ps.setBoolean(paramIndex, (Boolean) inValue);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.BOOLEAN);
			}
		}
		else if (sqlType == Types.DATE) {
			if (inValue instanceof java.util.Date) {
				if (inValue instanceof java.sql.Date) {
					ps.setDate(paramIndex, (java.sql.Date) inValue);
				}
				else {
					ps.setDate(paramIndex, new java.sql.Date(((java.util.Date) inValue).getTime()));
				}
			}
			else if (inValue instanceof Calendar) {
				Calendar cal = (Calendar) inValue;
				ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.DATE);
			}
		}
		else if (sqlType == Types.TIME) {
			if (inValue instanceof java.util.Date) {
				if (inValue instanceof java.sql.Time) {
					ps.setTime(paramIndex, (java.sql.Time) inValue);
				}
				else {
					ps.setTime(paramIndex, new java.sql.Time(((java.util.Date) inValue).getTime()));
				}
			}
			else if (inValue instanceof Calendar) {
				Calendar cal = (Calendar) inValue;
				ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.TIME);
			}
		}
		else if (sqlType == Types.TIMESTAMP) {
			if (inValue instanceof java.util.Date) {
				if (inValue instanceof java.sql.Timestamp) {
					ps.setTimestamp(paramIndex, (java.sql.Timestamp) inValue);
				}
				else {
					ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
				}
			}
			else if (inValue instanceof Calendar) {
				Calendar cal = (Calendar) inValue;
				ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.TIMESTAMP);
			}
		}
		else if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER &&
				"Oracle".equals(ps.getConnection().getMetaData().getDatabaseProductName()))) {
			if (isStringValue(inValue.getClass())) {
				ps.setString(paramIndex, inValue.toString());
			}
			else if (isDateValue(inValue.getClass())) {
				ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
			}
			else if (inValue instanceof Calendar) {
				Calendar cal = (Calendar) inValue;
				ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
			}
			else {
				// Fall back to generic setObject call without SQL type specified.
				ps.setObject(paramIndex, inValue);
			}
		}
		else {
			// Fall back to generic setObject call with SQL type specified.
			ps.setObject(paramIndex, inValue, sqlType);
		}
	}

	/**
	 * 检查给定的值是否可以被视为String值。
	 */
	private static boolean isStringValue(Class<?> inValueType) {
		// Consider any CharSequence (including StringBuffer and StringBuilder) as a String.
		return (CharSequence.class.isAssignableFrom(inValueType) ||
				StringWriter.class.isAssignableFrom(inValueType));
	}

	/**
	 * 检查给定的值是否为{@code java.util.Date}
	 * （但不是JDBC特定的子类之一）。
	 */
	private static boolean isDateValue(Class<?> inValueType) {
		return (java.util.Date.class.isAssignableFrom(inValueType) &&
				!(java.sql.Date.class.isAssignableFrom(inValueType) ||
						java.sql.Time.class.isAssignableFrom(inValueType) ||
						java.sql.Timestamp.class.isAssignableFrom(inValueType)));
	}

	/**
	 * 清理传递给execute方法的参数值所持有的所有资源。
	 * 例如，这对于关闭LOB值很重要。
	 * @param paramValues 提供的参数值。可以为 {@code null}。
	 * @see DisposableSqlTypeValue#cleanup()
	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
	 */
	public static void cleanupParameters(@Nullable Object... paramValues) {
		if (paramValues != null) {
			cleanupParameters(Arrays.asList(paramValues));
		}
	}

	/**
	 * 清理传递给execute方法的参数值所持有的所有资源。
	 * 例如，这对于关闭LOB值很重要。
	 * @param paramValues 提供的参数值。可以为 {@code null}。
	 * @see DisposableSqlTypeValue#cleanup()
	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
	 */
	public static void cleanupParameters(@Nullable Collection<?> paramValues) {
		if (paramValues != null) {
			for (Object inValue : paramValues) {
				// Unwrap SqlParameterValue first...
				if (inValue instanceof SqlParameterValue) {
					inValue = ((SqlParameterValue) inValue).getValue();
				}
				// Check for disposable value types
				if (inValue instanceof SqlValue) {
					((SqlValue) inValue).cleanup();
				}
				else if (inValue instanceof DisposableSqlTypeValue) {
					((DisposableSqlTypeValue) inValue).cleanup();
				}
			}
		}
	}

}
