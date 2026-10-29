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

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLTransientException;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.lang.Nullable;

/**
 * {@link SQLExceptionTranslator} 的实现，用于分析 JDBC 驱动抛出的具体
 * {@link java.sql.SQLException} 子类。
 *
 * <p>如果 JDBC 驱动实际上并未暴露符合 JDBC 4 规范的 {@code SQLException} 子类，
 * 则回退到标准的 {@link SQLStateSQLExceptionTranslator}。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 * @see java.sql.SQLTransientException
 * @see java.sql.SQLNonTransientException
 * @see java.sql.SQLRecoverableException
 */
public class SQLExceptionSubclassTranslator extends AbstractFallbackSQLExceptionTranslator {

	public SQLExceptionSubclassTranslator() {
		setFallbackTranslator(new SQLStateSQLExceptionTranslator());
	}

	@Override
	@Nullable
	protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
		if (ex instanceof SQLTransientException) {
			if (ex instanceof SQLTransientConnectionException) {
				return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof SQLTransactionRollbackException) {
				return new ConcurrencyFailureException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof SQLTimeoutException) {
				return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
			}
		}
		else if (ex instanceof SQLNonTransientException) {
			if (ex instanceof SQLNonTransientConnectionException) {
				return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof SQLDataException) {
				return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof SQLIntegrityConstraintViolationException) {
				return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof SQLInvalidAuthorizationSpecException) {
				return new PermissionDeniedDataAccessException(buildMessage(task, sql, ex), ex);
			}
			if (ex instanceof SQLSyntaxErrorException) {
				return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
			}
			if (ex instanceof SQLFeatureNotSupportedException) {
				return new InvalidDataAccessApiUsageException(buildMessage(task, sql, ex), ex);
			}
		}
		else if (ex instanceof SQLRecoverableException) {
			return new RecoverableDataAccessException(buildMessage(task, sql, ex), ex);
		}

		// Fallback to Spring's own SQL state translation...
		return null;
	}

}
