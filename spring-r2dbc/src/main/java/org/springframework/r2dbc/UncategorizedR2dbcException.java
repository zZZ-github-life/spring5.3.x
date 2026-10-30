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

import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.lang.Nullable;

/**
 * 当我们无法将 {@link R2dbcException} 归类为通用数据访问异常之一时抛出的异常。
 *
 * @author Mark Paluch
 * @since 5.3
 */
@SuppressWarnings("serial")
public class UncategorizedR2dbcException extends UncategorizedDataAccessException {

	/** 导致问题的 SQL。 */
	@Nullable
	private final String sql;


	/**
	 * {@code UncategorizedSQLException} 的构造函数。
	 * @param msg 详细消息
	 * @param sql 出错的 SQL 语句
	 * @param ex 底层数据访问 API 抛出的异常
	 */
	public UncategorizedR2dbcException(String msg, @Nullable String sql, R2dbcException ex) {
		super(msg, ex);
		this.sql = sql;
	}


	/**
	 * 返回被包装的 {@link R2dbcException}。
	 */
	public R2dbcException getR2dbcException() {
		return (R2dbcException) getCause();
	}

	/**
	 * 返回导致问题的 SQL（如果已知）。
	 */
	@Nullable
	public String getSql() {
		return this.sql;
	}

}
