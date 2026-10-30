/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * {@link PersistenceExceptionTranslator}，能够将 {@link HibernateException}
 * 实例转换为 Spring 的 {@link DataAccessException} 层次结构。从 Spring 4.3.2 和
 * Hibernate 5.2 开始，它还会转换标准的 JPA {@link PersistenceException} 实例。
 *
 * <p>已由 {@link LocalSessionFactoryBean} 扩展，因此除了 {@code LocalSessionFactoryBean}
 * 之外无需再声明此转换器。
 *
 * <p>使用 {@code @Configuration} 类配置容器时，必须手动注册此类型的 {@code @Bean}。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * @see SessionFactoryUtils#convertHibernateAccessException(HibernateException)
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible(RuntimeException)
 */
public class HibernateExceptionTranslator implements PersistenceExceptionTranslator {

	@Nullable
	private SQLExceptionTranslator jdbcExceptionTranslator;


	/**
	 * 设置用于 Hibernate 异常转换目的的 JDBC 异常转换器。
	 * <p>应用于检测到的 Hibernate {@link JDBCException} 的任意 {@link java.sql.SQLException} 根因，
	 * 覆盖 Hibernate 自身的 {@code SQLException} 转换
	 * （后者基于针对特定目标数据库的 Hibernate Dialect）。
	 * @since 5.1
	 * @see java.sql.SQLException
	 * @see org.hibernate.JDBCException
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLStateSQLExceptionTranslator
	 */
	public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
		this.jdbcExceptionTranslator = jdbcExceptionTranslator;
	}


	@Override
	@Nullable
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof HibernateException) {
			return convertHibernateAccessException((HibernateException) ex);
		}
		if (ex instanceof PersistenceException) {
			if (ex.getCause() instanceof HibernateException) {
				return convertHibernateAccessException((HibernateException) ex.getCause());
			}
			return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
		}
		return null;
	}

	/**
	 * 将给定的 HibernateException 转换为 {@code org.springframework.dao} 层次结构中
	 * 相应的异常。
	 * <p>会自动将指定的 SQLExceptionTranslator 应用于
	 * Hibernate JDBCException，否则依赖 Hibernate 的默认转换。
	 * @param ex 发生的 HibernateException
	 * @return 相应的 DataAccessException
	 * @see SessionFactoryUtils#convertHibernateAccessException
	 */
	protected DataAccessException convertHibernateAccessException(HibernateException ex) {
		if (this.jdbcExceptionTranslator != null && ex instanceof JDBCException) {
			JDBCException jdbcEx = (JDBCException) ex;
			DataAccessException dae = this.jdbcExceptionTranslator.translate(
					"Hibernate operation: " + jdbcEx.getMessage(), jdbcEx.getSQL(), jdbcEx.getSQLException());
			if (dae != null) {
				throw dae;
			}
		}
		return SessionFactoryUtils.convertHibernateAccessException(ex);
	}

}
