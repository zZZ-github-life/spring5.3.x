/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jdbc;

import java.sql.SQLWarning;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * 当未忽略{@link java.sql.SQLWarning SQLWarnings}时抛出的异常。
 *
 * <p>如果报告了SQLWarning，则操作已完成，因此如果查看警告后不满意，
 * 我们需要显式地回滚。我们可以选择忽略（并记录）该警告，
 * 或者将其包装为此SQLWarningException的形式抛出。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.jdbc.core.JdbcTemplate#setIgnoreWarnings
 */
@SuppressWarnings("serial")
public class SQLWarningException extends UncategorizedDataAccessException {

	/**
	 * 创建SQLWarningException实例。
	 * @param msg 详细消息
	 * @param ex JDBC 警告
	 */
	public SQLWarningException(String msg, SQLWarning ex) {
		super(msg, ex);
	}

	/**
	 * 返回底层的SQLWarning。
	 */
	public SQLWarning SQLWarning() {
		return (SQLWarning) getCause();
	}

}
