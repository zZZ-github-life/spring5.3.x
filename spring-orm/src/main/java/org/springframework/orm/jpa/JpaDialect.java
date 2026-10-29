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

import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * SPI 策略，封装了标准 JPA 2.1 未提供的一些功能，
 * 例如访问底层 JDBC Connection。此策略主要面向 JPA 提供者的独立使用场景；
 * 在使用 JTA 事务运行时，其大部分功能并不相关。
 *
 * <p>通常建议继承 {@link DefaultJpaDialect} 而不是直接实现此接口。
 * 这样可以继承 DefaultJpaDialect 的通用行为（现在和将来），
 * 只需重写特定的钩子方法即可插入具体的供应商特定行为。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see DefaultJpaDialect
 * @see JpaTransactionManager#setJpaDialect
 * @see JpaVendorAdapter#getJpaDialect()
 * @see AbstractEntityManagerFactoryBean#setJpaDialect
 * @see AbstractEntityManagerFactoryBean#setJpaVendorAdapter
 */
public interface JpaDialect extends PersistenceExceptionTranslator {

	/**
	 * 开启给定的 JPA 事务，并应用给定 Spring 事务定义所指定的语义
	 * （特别是隔离级别和超时）。由 JpaTransactionManager 在事务开始时调用。
	 * <p>实现可以配置 JPA Transaction 对象，然后调用 {@code begin}，
	 * 或调用接受例如隔离级别的特殊 begin 方法。
	 * <p>实现可以将只读标志应用为 flush 模式。在这种情况下，
	 * 可以返回一个事务数据对象，其中保存了先前的 flush 模式
	 * （可能还有其他数据），以便在 {@code cleanupTransaction} 中重置。
	 * 实现还可以在开启事务之前将只读标志和隔离级别应用于底层 JDBC Connection。
	 * <p>实现还可以使用传入的 TransactionDefinition 所暴露的 Spring 事务名称，
	 * 针对特定的数据访问用例进行优化
	 * （实际上就是将当前事务名称用作用例标识符）。
	 * <p>如果持久化提供者支持，此方法还允许通过返回一个实现 Spring
	 * {@link org.springframework.transaction.SavepointManager} 接口的对象
	 * 来暴露保存点能力。{@link JpaTransactionManager} 将在需要时使用此能力。
	 * @param entityManager the EntityManager to begin a JPA transaction on
	 * @param definition the Spring transaction definition that defines semantics
	 * @return an arbitrary object that holds transaction data, if any
	 * (to be passed into {@link #cleanupTransaction}). May implement the
	 * {@link org.springframework.transaction.SavepointManager} interface.
	 * @throws javax.persistence.PersistenceException if thrown by JPA methods
	 * @throws java.sql.SQLException if thrown by JDBC methods
	 * @throws org.springframework.transaction.TransactionException in case of invalid arguments
	 * @see #cleanupTransaction
	 * @see javax.persistence.EntityTransaction#begin
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#prepareConnectionForTransaction
	 */
	@Nullable
	Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException;

	/**
	 * 准备 JPA 事务，并应用指定的语义。当 EntityManagerFactoryUtils 将
	 * EntityManager 登记到 JTA 事务或本地加入的事务中时调用
	 * （例如在将未同步的 EntityManager 升级为已同步的之后）。
	 * <p>实现可以将只读标志应用为 flush 模式。在这种情况下，
	 * 可以返回一个事务数据对象，其中保存了先前的 flush 模式
	 * （可能还有其他数据），以便在 {@code cleanupTransaction} 中重置。
	 * <p>实现还可以使用 Spring 事务名称，针对特定的数据访问用例进行优化
	 * （实际上就是将当前事务名称用作用例标识符）。
	 * @param entityManager the EntityManager to begin a JPA transaction on
	 * @param readOnly whether the transaction is supposed to be read-only
	 * @param name the name of the transaction (if any)
	 * @return an arbitrary object that holds transaction data, if any
	 * (to be passed into cleanupTransaction)
	 * @throws javax.persistence.PersistenceException if thrown by JPA methods
	 * @see #cleanupTransaction
	 */
	@Nullable
	Object prepareTransaction(EntityManager entityManager, boolean readOnly, @Nullable String name)
			throws PersistenceException;

	/**
	 * 通过给定的事务数据清理事务。由 JpaTransactionManager 和
	 * EntityManagerFactoryUtils 在事务清理时调用。
	 * <p>例如，实现可以重置底层 JDBC Connection 的只读标志和隔离级别。
	 * 此外，还可以在此处重置已暴露的数据访问用例。
	 * @param transactionData arbitrary object that holds transaction data, if any
	 * (as returned by beginTransaction or prepareTransaction)
	 * @see #beginTransaction
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#resetConnectionAfterTransaction
	 */
	void cleanupTransaction(@Nullable Object transactionData);

	/**
	 * 获取给定 JPA EntityManager 在底层使用的 JDBC Connection
	 * （如果访问的是关系型数据库）。只有在确实需要访问底层 JDBC Connection 时
	 * 才会调用此方法，通常是在活动的 JPA 事务中（例如由 JpaTransactionManager 调用）。
	 * 当不再需要时，返回的句柄将被传入 {@code releaseJdbcConnection} 方法。
	 * <p>此策略是必要的，因为 JPA 没有提供获取底层 JDBC Connection 的标准方式
	 * （原因是 JPA 实现可能根本不与关系型数据库配合工作）。
	 * <p>建议实现返回未包装的 Connection 对象，即从连接池中获取到的原始 Connection。
	 * 这样应用程序代码更容易访问底层的原生 JDBC Connection（例如 OracleConnection），
	 * 这在处理 LOB 等场景时有时是必要的。我们假定调用代码知道
	 * 如何正确处理返回的 Connection 对象。
	 * <p>在简单情况下，如果返回的 Connection 将随 EntityManager 自动关闭，
	 * 或者可以通过 Connection 对象本身释放，则实现可以返回一个仅包含该 Connection
	 * 的 SimpleConnectionHandle。如果 {@code releaseJdbcConnection} 中需要其他对象，
	 * 实现应使用一个引用该其他对象的特殊句柄。
	 * @param entityManager the current JPA EntityManager
	 * @param readOnly whether the Connection is only needed for read-only purposes
	 * @return a handle for the Connection, to be passed into {@code releaseJdbcConnection},
	 * or {@code null} if no JDBC Connection can be retrieved
	 * @throws javax.persistence.PersistenceException if thrown by JPA methods
	 * @throws java.sql.SQLException if thrown by JDBC methods
	 * @see #releaseJdbcConnection
	 * @see org.springframework.jdbc.datasource.ConnectionHandle#getConnection
	 * @see org.springframework.jdbc.datasource.SimpleConnectionHandle
	 * @see JpaTransactionManager#setDataSource
	 */
	@Nullable
	ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException;

	/**
	 * 释放给定的 JDBC Connection，该 Connection 最初是通过
	 * {@code getJdbcConnection} 获取的。无论如何都应调用此方法，
	 * 以便正确释放所获取的 Connection 句柄。
	 * <p>如果 {@code getJdbcConnection} 返回的 Connection 会在 JPA 事务完成时
	 * 或 EntityManager 关闭时隐式关闭，则实现可以简单地不执行任何操作。
	 * @param conHandle the JDBC Connection handle to release
	 * @param entityManager the current JPA EntityManager
	 * @throws javax.persistence.PersistenceException if thrown by JPA methods
	 * @throws java.sql.SQLException if thrown by JDBC methods
	 * @see #getJdbcConnection
	 */
	void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager entityManager)
			throws PersistenceException, SQLException;

}
