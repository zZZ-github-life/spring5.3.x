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

package org.springframework.r2dbc;

import io.r2dbc.spi.R2dbcException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;


/**
 * 在指定的 SQL 无效时抛出的异常。此类异常始终具有
 * {@link io.r2dbc.spi.R2dbcException} 作为根本原因。
 *
 * <p>可以为表不存在、列不存在等情况提供子类。
 * 自定义的 R2dbcExceptionTranslator 可以创建这类更具体的异常，
 * 而不会影响使用此类的代码。
 *
 * @author Mark Paluch
 * @since 5.3
 */
@SuppressWarnings("serial")
public class BadSqlGrammarException extends InvalidDataAccessResourceUsageException {

	private final String sql;


	/**
	 * BadSqlGrammarException 的构造函数。
	 * @param task name of current task
	 * @param sql the offending SQL statement
	 * @param ex the root cause
	 */
	public BadSqlGrammarException(String task, String sql, R2dbcException ex) {
		super(task + "; bad SQL grammar [" + sql + "]", ex);
		this.sql = sql;
	}


	/**
	 * 返回被包装的 {@link R2dbcException}。
	 */
	public R2dbcException getR2dbcException() {
		return (R2dbcException) getCause();
	}

	/**
	 * 返回导致问题的 SQL。
	 */
	public String getSql() {
		return this.sql;
	}

}
