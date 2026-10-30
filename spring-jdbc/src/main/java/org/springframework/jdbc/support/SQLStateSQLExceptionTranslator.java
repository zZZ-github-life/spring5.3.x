/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.HashSet;
import java.util.Set;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.lang.Nullable;

/**
 * {@link SQLExceptionTranslator} 的实现，根据前两位数字（SQL 状态“类别”）
 * 分析 {@link SQLException} 中的 SQL 状态。它能够识别标准 SQL 状态值
 * 以及众所周知的厂商特定 SQL 状态。
 *
 * <p>它无法诊断所有问题，但可以在不同数据库之间移植，且不需要特殊初始化
 * （无需检测数据库厂商等）。如需更精确的转换，请考虑使用
 * {@link SQLErrorCodeSQLExceptionTranslator}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @see java.sql.SQLException#getSQLState()
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class SQLStateSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

	private static final Set<String> BAD_SQL_GRAMMAR_CODES = new HashSet<>(8);

	private static final Set<String> DATA_INTEGRITY_VIOLATION_CODES = new HashSet<>(8);

	private static final Set<String> DATA_ACCESS_RESOURCE_FAILURE_CODES = new HashSet<>(8);

	private static final Set<String> TRANSIENT_DATA_ACCESS_RESOURCE_CODES = new HashSet<>(8);

	private static final Set<String> CONCURRENCY_FAILURE_CODES = new HashSet<>(4);


	static {
		BAD_SQL_GRAMMAR_CODES.add("07");  // Dynamic SQL error
		BAD_SQL_GRAMMAR_CODES.add("21");  // Cardinality violation
		BAD_SQL_GRAMMAR_CODES.add("2A");  // Syntax error direct SQL
		BAD_SQL_GRAMMAR_CODES.add("37");  // Syntax error dynamic SQL
		BAD_SQL_GRAMMAR_CODES.add("42");  // General SQL syntax error
		BAD_SQL_GRAMMAR_CODES.add("65");  // Oracle: unknown identifier

		DATA_INTEGRITY_VIOLATION_CODES.add("01");  // Data truncation
		DATA_INTEGRITY_VIOLATION_CODES.add("02");  // No data found
		DATA_INTEGRITY_VIOLATION_CODES.add("22");  // Value out of range
		DATA_INTEGRITY_VIOLATION_CODES.add("23");  // Integrity constraint violation
		DATA_INTEGRITY_VIOLATION_CODES.add("27");  // Triggered data change violation
		DATA_INTEGRITY_VIOLATION_CODES.add("44");  // With check violation

		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("08");  // Connection exception
		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("53");  // PostgreSQL: insufficient resources (e.g. disk full)
		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("54");  // PostgreSQL: program limit exceeded (e.g. statement too complex)
		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("57");  // DB2: out-of-memory exception / database not started
		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("58");  // DB2: unexpected system error

		TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("JW");  // Sybase: internal I/O error
		TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("JZ");  // Sybase: unexpected I/O error
		TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("S1");  // DB2: communication failure

		CONCURRENCY_FAILURE_CODES.add("40");  // Transaction rollback
		CONCURRENCY_FAILURE_CODES.add("61");  // Oracle: deadlock
	}


	@Override
	@Nullable
	protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
		// First, the getSQLState check...
		String sqlState = getSqlState(ex);
		if (sqlState != null && sqlState.length() >= 2) {
			String classCode = sqlState.substring(0, 2);
			if (logger.isDebugEnabled()) {
				logger.debug("Extracted SQL state class '" + classCode + "' from value '" + sqlState + "'");
			}
			if (BAD_SQL_GRAMMAR_CODES.contains(classCode)) {
				return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
			}
			else if (DATA_INTEGRITY_VIOLATION_CODES.contains(classCode)) {
				return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
			}
			else if (DATA_ACCESS_RESOURCE_FAILURE_CODES.contains(classCode)) {
				return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
			}
			else if (TRANSIENT_DATA_ACCESS_RESOURCE_CODES.contains(classCode)) {
				return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
			}
			else if (CONCURRENCY_FAILURE_CODES.contains(classCode)) {
				return new ConcurrencyFailureException(buildMessage(task, sql, ex), ex);
			}
		}

		// For MySQL: exception class name indicating a timeout?
		// (since MySQL doesn't throw the JDBC 4 SQLTimeoutException)
		if (ex.getClass().getName().contains("Timeout")) {
			return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
		}

		// Couldn't resolve anything proper - resort to UncategorizedSQLException.
		return null;
	}

	/**
	 * 从提供的 {@link SQLException exception} 中获取 SQL 状态码。
	 * <p>某些 JDBC 驱动会将批量更新产生的实际异常嵌套起来，因此我们可能需要
	 * 深入嵌套异常中获取。
	 * @param ex 要从中提取 {@link SQLException#getSQLState() SQL 状态} 的异常
	 * @return SQL 状态码
	 */
	@Nullable
	private String getSqlState(SQLException ex) {
		String sqlState = ex.getSQLState();
		if (sqlState == null) {
			SQLException nestedEx = ex.getNextException();
			if (nestedEx != null) {
				sqlState = nestedEx.getSQLState();
			}
		}
		return sqlState;
	}

}
