/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.orm.jpa;

import java.io.Serializable;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.lang.Nullable;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * {@link JpaDialect} 接口的默认实现。
 * 用作 {@link JpaTransactionManager} 的默认方言。
 *
 * <p>它在 {@link #beginTransaction} 中简单地开启一个标准 JPA 事务，
 * 并通过 {@link EntityManagerFactoryUtils} 执行标准的异常转换。
 *
 * <p><b>注意：自 Spring 5.0 起，Spring 的 JPA 支持要求 JPA 2.1 或更高版本。</b>
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see JpaTransactionManager#setJpaDialect
 */
@SuppressWarnings("serial")
public class DefaultJpaDialect implements JpaDialect, Serializable {

	/**
	 * 此实现调用标准的 JPA {@code Transaction.begin} 方法。
	 * 如果设置了非默认的隔离级别，则抛出 InvalidIsolationLevelException。
	 * <p>此实现不返回任何事务数据对象，因为标准 JPA 事务无需保留任何状态。
	 * 因此，子类不必关心此实现的返回值（{@code null}），
	 * 可以自由返回自己的事务数据对象。
	 * @see javax.persistence.EntityTransaction#begin
	 * @see org.springframework.transaction.InvalidIsolationLevelException
	 * @see #cleanupTransaction
	 */
	@Override
	@Nullable
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException(getClass().getSimpleName() +
					" does not support custom isolation levels due to limitations in standard JPA. " +
					"Specific arrangements may be implemented in custom JpaDialect variants.");
		}
		entityManager.getTransaction().begin();
		return null;
	}

	@Override
	@Nullable
	public Object prepareTransaction(EntityManager entityManager, boolean readOnly, @Nullable String name)
			throws PersistenceException {

		return null;
	}

	/**
	 * 此实现不执行任何操作，因为默认的 {@code beginTransaction}
	 * 实现不需要任何清理。
	 * @see #beginTransaction
	 */
	@Override
	public void cleanupTransaction(@Nullable Object transactionData) {
	}

	/**
	 * 此实现始终返回 {@code null}，
	 * 表示无法提供 JDBC Connection。
	 */
	@Override
	@Nullable
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		return null;
	}

	/**
	 * 此实现不执行任何操作，假定 Connection 将随 EntityManager 一起隐式关闭。
	 * <p>如果 JPA 实现返回了一个期望应用程序在使用后关闭的 Connection 句柄，
	 * 则方言实现需要在此处调用 {@code Connection.close()}
	 * （或其他具有类似效果的方法）。
	 * @see java.sql.Connection#close()
	 */
	@Override
	public void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager em)
			throws PersistenceException, SQLException {
	}


	//-----------------------------------------------------------------------------------
	// Hook for exception translation (used by JpaTransactionManager)
	//-----------------------------------------------------------------------------------

	/**
	 * 此实现委托给 EntityManagerFactoryUtils。
	 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
	 */
	@Override
	@Nullable
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
	}

}
