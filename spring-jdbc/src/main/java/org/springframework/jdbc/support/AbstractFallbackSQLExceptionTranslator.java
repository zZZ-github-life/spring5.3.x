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

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link SQLExceptionTranslator} 实现的基类，允许回退到其他
 * {@link SQLExceptionTranslator}。
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 */
public abstract class AbstractFallbackSQLExceptionTranslator implements SQLExceptionTranslator {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private SQLExceptionTranslator fallbackTranslator;


	/**
	 * 覆盖默认的 SQL 状态回退转换器
	 * （通常是 {@link SQLStateSQLExceptionTranslator}）。
	 */
	public void setFallbackTranslator(@Nullable SQLExceptionTranslator fallback) {
		this.fallbackTranslator = fallback;
	}

	/**
	 * 返回回退异常转换器（如果有）。
	 */
	@Nullable
	public SQLExceptionTranslator getFallbackTranslator() {
		return this.fallbackTranslator;
	}


	/**
	 * 先对参数进行预检查，调用 {@link #doTranslate}，并在必要时调用
	 * {@link #getFallbackTranslator() 回退转换器}。
	 */
	@Override
	@Nullable
	public DataAccessException translate(String task, @Nullable String sql, SQLException ex) {
		Assert.notNull(ex, "Cannot translate a null SQLException");

		DataAccessException dae = doTranslate(task, sql, ex);
		if (dae != null) {
			// Specific exception match found.
			return dae;
		}

		// Looking for a fallback...
		SQLExceptionTranslator fallback = getFallbackTranslator();
		if (fallback != null) {
			return fallback.translate(task, sql, ex);
		}

		return null;
	}

	/**
	 * 用于实际转换给定异常的模板方法。
	 * <p>传入的参数已经过预检查。此外，此方法允许返回 {@code null}，
	 * 表示未找到匹配的异常，应启用回退转换。
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL query or update that caused the problem (if known)
	 * @param ex the offending {@code SQLException}
	 * @return the DataAccessException, wrapping the {@code SQLException};
	 * or {@code null} if no exception match found
	 */
	@Nullable
	protected abstract DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex);


	/**
	 * 为给定的 {@link java.sql.SQLException} 构建消息 {@code String}。
	 * <p>由转换器子类在创建通用 {@link org.springframework.dao.DataAccessException}
	 * 类的实例时调用。
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL statement that caused the problem
	 * @param ex the offending {@code SQLException}
	 * @return the message {@code String} to use
	 */
	protected String buildMessage(String task, @Nullable String sql, SQLException ex) {
		return task + "; " + (sql != null ? ("SQL [" + sql + "]; ") : "") + ex.getMessage();
	}

}
