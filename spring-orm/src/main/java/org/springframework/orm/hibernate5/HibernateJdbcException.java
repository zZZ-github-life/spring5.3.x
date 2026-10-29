/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.orm.hibernate5;

import java.sql.SQLException;

import org.hibernate.JDBCException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * UncategorizedDataAccessException 的 Hibernate 专用子类，
 * 用于 Hibernate 包装的 JDBC 异常。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see SessionFactoryUtils#convertHibernateAccessException
 */
@SuppressWarnings("serial")
public class HibernateJdbcException extends UncategorizedDataAccessException {

	public HibernateJdbcException(JDBCException ex) {
		super("JDBC exception on Hibernate data access: SQLException for SQL [" + ex.getSQL() + "]; SQL state [" +
				ex.getSQLState() + "]; error code [" + ex.getErrorCode() + "]; " + ex.getMessage(), ex);
	}

	/**
	 * 返回底层的 SQLException。
	 */
	public SQLException getSQLException() {
		return ((JDBCException) getCause()).getSQLException();
	}

	/**
	 * 返回导致问题的 SQL。
	 */
	public String getSql() {
		return ((JDBCException) getCause()).getSQL();
	}

}
