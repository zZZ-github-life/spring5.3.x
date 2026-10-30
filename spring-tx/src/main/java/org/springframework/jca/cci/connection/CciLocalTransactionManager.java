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

package org.springframework.jca.cci.connection;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.LocalTransactionException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager} 实现，
 * 管理单个 CCI ConnectionFactory 的本地事务。
 * 将来自指定 ConnectionFactory 的 CCI Connection 绑定到线程，
 * 允许每个 ConnectionFactory 有一个线程绑定的 Connection。
 *
 * <p>应用程序代码需要通过
 * {@link ConnectionFactoryUtils#getConnection(ConnectionFactory)} 来获取 CCI Connection，
 * 而不是使用标准的 Java EE 风格 {@link ConnectionFactory#getConnection()} 调用。
 * Spring 的 {@link org.springframework.jca.cci.core.CciTemplate} 等类隐式使用此策略。
 * 如果不与此事务管理器结合使用，
 * {@link ConnectionFactoryUtils} 的查找策略与原生 DataSource 查找的行为完全相同；
 * 因此可以以可移植的方式使用。
 *
 * <p>或者，你可以允许应用程序代码使用标准的 Java EE 查找模式
 * {@link ConnectionFactory#getConnection()}，例如用于完全不感知 Spring 的遗留代码。
 * 在这种情况下，为你的目标 ConnectionFactory 定义一个
 * {@link TransactionAwareConnectionFactoryProxy}，它将自动参与 Spring 管理的事务。
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see ConnectionFactoryUtils#getConnection(javax.resource.cci.ConnectionFactory)
 * @see ConnectionFactoryUtils#releaseConnection
 * @see TransactionAwareConnectionFactoryProxy
 * @see org.springframework.jca.cci.core.CciTemplate
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@SuppressWarnings("serial")
public class CciLocalTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	@Nullable
	private ConnectionFactory connectionFactory;


	/**
	 * 创建新的 CciLocalTransactionManager 实例。
	 * 必须设置 ConnectionFactory 才能使用。
	 * @see #setConnectionFactory
	 */
	public CciLocalTransactionManager() {
	}

	/**
	 * 创建新的 CciLocalTransactionManager 实例。
	 * @param connectionFactory 要为其管理本地事务的 CCI ConnectionFactory
	 */
	public CciLocalTransactionManager(ConnectionFactory connectionFactory) {
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应管理本地事务的 CCI ConnectionFactory。
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory cf) {
		if (cf instanceof TransactionAwareConnectionFactoryProxy) {
			// If we got a TransactionAwareConnectionFactoryProxy, we need to perform transactions
			// for its underlying target ConnectionFactory, else JMS access code won't see
			// properly exposed transactions (i.e. transactions for the target ConnectionFactory).
			this.connectionFactory = ((TransactionAwareConnectionFactoryProxy) cf).getTargetConnectionFactory();
		}
		else {
			this.connectionFactory = cf;
		}
	}

	/**
	 * 返回此实例管理本地事务的 CCI ConnectionFactory。
	 */
	@Nullable
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	private ConnectionFactory obtainConnectionFactory() {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	@Override
	public Object getResourceFactory() {
		return obtainConnectionFactory();
	}

	@Override
	protected Object doGetTransaction() {
		CciLocalTransactionObject txObject = new CciLocalTransactionObject();
		ConnectionHolder conHolder =
				(ConnectionHolder) TransactionSynchronizationManager.getResource(obtainConnectionFactory());
		txObject.setConnectionHolder(conHolder);
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;
		// Consider a pre-bound connection as transaction.
		return txObject.hasConnectionHolder();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;
		ConnectionFactory connectionFactory = obtainConnectionFactory();
		Connection con = null;

		try {
			con = connectionFactory.getConnection();
			if (logger.isDebugEnabled()) {
				logger.debug("Acquired Connection [" + con + "] for local CCI transaction");
			}

			ConnectionHolder connectionHolder = new ConnectionHolder(con);
			connectionHolder.setSynchronizedWithTransaction(true);

			con.getLocalTransaction().begin();
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				connectionHolder.setTimeoutInSeconds(timeout);
			}

			txObject.setConnectionHolder(connectionHolder);
			TransactionSynchronizationManager.bindResource(connectionFactory, connectionHolder);
		}
		catch (NotSupportedException ex) {
			ConnectionFactoryUtils.releaseConnection(con, connectionFactory);
			throw new CannotCreateTransactionException("CCI Connection does not support local transactions", ex);
		}
		catch (LocalTransactionException ex) {
			ConnectionFactoryUtils.releaseConnection(con, connectionFactory);
			throw new CannotCreateTransactionException("Could not begin local CCI transaction", ex);
		}
		catch (Throwable ex) {
			ConnectionFactoryUtils.releaseConnection(con, connectionFactory);
			throw new TransactionSystemException("Unexpected failure on begin of CCI local transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;
		txObject.setConnectionHolder(null);
		return TransactionSynchronizationManager.unbindResource(obtainConnectionFactory());
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		ConnectionHolder conHolder = (ConnectionHolder) suspendedResources;
		TransactionSynchronizationManager.bindResource(obtainConnectionFactory(), conHolder);
	}

	protected boolean isRollbackOnly(Object transaction) throws TransactionException {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;
		return txObject.getConnectionHolder().isRollbackOnly();
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Committing CCI local transaction on Connection [" + con + "]");
		}
		try {
			con.getLocalTransaction().commit();
		}
		catch (LocalTransactionException ex) {
			throw new TransactionSystemException("Could not commit CCI local transaction", ex);
		}
		catch (ResourceException ex) {
			throw new TransactionSystemException("Unexpected failure on commit of CCI local transaction", ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Rolling back CCI local transaction on Connection [" + con + "]");
		}
		try {
			con.getLocalTransaction().rollback();
		}
		catch (LocalTransactionException ex) {
			throw new TransactionSystemException("Could not roll back CCI local transaction", ex);
		}
		catch (ResourceException ex) {
			throw new TransactionSystemException("Unexpected failure on rollback of CCI local transaction", ex);
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting CCI local transaction [" + txObject.getConnectionHolder().getConnection() +
					"] rollback-only");
		}
		txObject.getConnectionHolder().setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;
		ConnectionFactory connectionFactory = obtainConnectionFactory();

		// Remove the connection holder from the thread.
		TransactionSynchronizationManager.unbindResource(connectionFactory);
		txObject.getConnectionHolder().clear();

		Connection con = txObject.getConnectionHolder().getConnection();
		if (logger.isDebugEnabled()) {
			logger.debug("Releasing CCI Connection [" + con + "] after transaction");
		}
		ConnectionFactoryUtils.releaseConnection(con, connectionFactory);
	}


	/**
	 * CCI 本地事务对象，表示一个 ConnectionHolder。
	 * 由 CciLocalTransactionManager 用作事务对象。
	 * @see ConnectionHolder
	 */
	private static class CciLocalTransactionObject {

		@Nullable
		private ConnectionHolder connectionHolder;

		public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder) {
			this.connectionHolder = connectionHolder;
		}

		public ConnectionHolder getConnectionHolder() {
			Assert.state(this.connectionHolder != null, "No ConnectionHolder available");
			return this.connectionHolder;
		}

		public boolean hasConnectionHolder() {
			return (this.connectionHolder != null);
		}
	}

}
