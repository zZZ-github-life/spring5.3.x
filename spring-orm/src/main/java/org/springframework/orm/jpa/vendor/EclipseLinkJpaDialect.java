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

package org.springframework.orm.jpa.vendor;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.eclipse.persistence.sessions.UnitOfWork;

import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * 适用于 Eclipse Persistence Services（EclipseLink）的
 * {@link org.springframework.orm.jpa.JpaDialect} 实现。基于 EclipseLink 2.7
 * 开发并测试；在运行时向后兼容 EclipseLink 2.5 和 2.6。
 *
 * <p>默认情况下，对于非只读事务，此类会通过提前获取的 JDBC 连接
 * 提前启动 EclipseLink 事务。这样可以在同一事务中混合使用 JDBC 和
 * JPA/EclipseLink 操作，并让二者的影响相互可见。如果不需要这样做，
 * 请将 "lazyDatabaseTransaction" 标志设置为 {@code true}，
 * 或一致地将所有受影响的事务声明为只读。
 * 从 Spring 4.1.2 开始，这将可靠地避免提前获取 JDBC 连接，
 * 从而使 EclipseLink 保持在共享缓存模式。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see #setLazyDatabaseTransaction
 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
 */
@SuppressWarnings("serial")
public class EclipseLinkJpaDialect extends DefaultJpaDialect {

	private boolean lazyDatabaseTransaction = false;


	/**
	 * 设置是否在 Spring 管理的 EclipseLink 事务中延迟启动数据库资源事务。
	 * <p>默认情况下，只读事务会延迟启动，而常规的非只读事务会提前启动。
	 * 这样可以在整个 EclipseLink 事务中复用同一个 JDBC 连接，
	 * 以便与在同一 DataSource 上工作的 JDBC 访问代码保持强制隔离
	 * 和一致的可见性。
	 * <p>将此标志切换为 "true" 可强制对非只读事务也延迟开始数据库事务，
	 * 从而允许访问 EclipseLink 的共享缓存并遵循 EclipseLink 的连接模式配置，
	 * 前提是 JDBC 层面的隔离性和可见性不太重要。
	 * @see org.eclipse.persistence.sessions.UnitOfWork#beginEarlyTransaction()
	 */
	public void setLazyDatabaseTransaction(boolean lazyDatabaseTransaction) {
		this.lazyDatabaseTransaction = lazyDatabaseTransaction;
	}


	@Override
	@Nullable
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			// Pass custom isolation level on to EclipseLink's DatabaseLogin configuration
			// (since Spring 4.1.2)
			UnitOfWork uow = entityManager.unwrap(UnitOfWork.class);
			uow.getLogin().setTransactionIsolation(definition.getIsolationLevel());
		}

		entityManager.getTransaction().begin();

		if (!definition.isReadOnly() && !this.lazyDatabaseTransaction) {
			// Begin an early transaction to force EclipseLink to get a JDBC Connection
			// so that Spring can manage transactions with JDBC as well as EclipseLink.
			entityManager.unwrap(UnitOfWork.class).beginEarlyTransaction();
		}

		return null;
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		// As of Spring 4.1.2, we're using a custom ConnectionHandle for lazy retrieval
		// of the underlying Connection (allowing for deferred internal transaction begin
		// within the EclipseLink EntityManager)
		return new EclipseLinkConnectionHandle(entityManager);
	}


	/**
	 * {@link ConnectionHandle} 实现，在第一次调用 {@code getConnection} 时
	 * 才延迟获取 EclipseLink 提供的 Connection——如果没有应用代码请求
	 * JDBC Connection，则可能永远不会调用。
	 * 这对于推迟在 EclipseLink EntityManager 中获取 JDBC Connection
	 * 所隐含的提前事务启动非常有用。
	 */
	private static class EclipseLinkConnectionHandle implements ConnectionHandle {

		private final EntityManager entityManager;

		@Nullable
		private Connection connection;

		public EclipseLinkConnectionHandle(EntityManager entityManager) {
			this.entityManager = entityManager;
		}

		@Override
		public Connection getConnection() {
			if (this.connection == null) {
				this.connection = this.entityManager.unwrap(Connection.class);
			}
			return this.connection;
		}
	}

}
