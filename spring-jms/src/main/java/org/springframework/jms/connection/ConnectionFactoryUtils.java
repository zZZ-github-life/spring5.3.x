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

package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 用于管理 JMS {@link javax.jms.ConnectionFactory} 的辅助类，
 * 特别是为给定的 ConnectionFactory 获取事务性 JMS 资源。
 *
 * <p>主要供框架内部使用。由 {@link org.springframework.jms.core.JmsTemplate}
 * 以及 {@link org.springframework.jms.listener.DefaultMessageListenerContainer} 使用。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SmartConnectionFactory
 */
public abstract class ConnectionFactoryUtils {

	private static final Log logger = LogFactory.getLog(ConnectionFactoryUtils.class);


	/**
	 * 释放给定的 Connection，必要时停止它，并最终关闭它。
	 * <p>如果可用，会检查 {@link SmartConnectionFactory#shouldStop}。
	 * 这本质上是 {@link org.springframework.jms.support.JmsUtils#closeConnection}
	 * 的更精细版本。
	 * @param con 要释放的 Connection（如果为 {@code null}，则忽略该调用）
	 * @param cf 获取该 Connection 所用的 ConnectionFactory（可为 {@code null}）
	 * @param started 该 Connection 是否可能已由应用启动
	 * @see SmartConnectionFactory#shouldStop
	 * @see org.springframework.jms.support.JmsUtils#closeConnection
	 */
	public static void releaseConnection(@Nullable Connection con, @Nullable ConnectionFactory cf, boolean started) {
		if (con == null) {
			return;
		}
		if (started && cf instanceof SmartConnectionFactory && ((SmartConnectionFactory) cf).shouldStop(con)) {
			try {
				con.stop();
			}
			catch (Throwable ex) {
				logger.debug("Could not stop JMS Connection before closing it", ex);
			}
		}
		try {
			con.close();
		}
		catch (Throwable ex) {
			logger.debug("Could not close JMS Connection", ex);
		}
	}

	/**
	 * 返回给定 Session 最内层的目标 Session。如果给定的 Session 是代理，
	 * 将一直解包直到找到非代理 Session 为止。否则，按原样返回传入的 Session。
	 * @param session 要解包的 Session 代理
	 * @return 最内层的目标 Session；若无代理，则为传入的 Session
	 * @see SessionProxy#getTargetSession()
	 */
	public static Session getTargetSession(Session session) {
		Session sessionToUse = session;
		while (sessionToUse instanceof SessionProxy) {
			sessionToUse = ((SessionProxy) sessionToUse).getTargetSession();
		}
		return sessionToUse;
	}



	/**
	 * 判断给定的 JMS Session 是否为事务性的，即是否由 Spring 的事务设施绑定到当前线程。
	 * @param session 要检查的 JMS Session
	 * @param cf 该 Session 来源的 JMS ConnectionFactory
	 * @return 该 Session 是否为事务性的
	 */
	public static boolean isSessionTransactional(@Nullable Session session, @Nullable ConnectionFactory cf) {
		if (session == null || cf == null) {
			return false;
		}
		JmsResourceHolder resourceHolder = (JmsResourceHolder) TransactionSynchronizationManager.getResource(cf);
		return (resourceHolder != null && resourceHolder.containsSession(session));
	}


	/**
	 * 获取与当前事务（如果有）同步的 JMS Session。
	 * @param cf 要为其获取 Session 的 ConnectionFactory
	 * @param existingCon 要为其获取 Session 的现有 JMS Connection（可为 {@code null}）
	 * @param synchedLocalTransactionAllowed 是否允许本地 JMS 事务与 Spring 管理的事务同步
	 * （其中主事务可能是针对特定 DataSource 的基于 JDBC 的事务），
	 * JMS 事务在主事务之后立即提交。如果不允许，
	 * 给定的 ConnectionFactory 需要在底层处理事务的登记。
	 * @return 事务性 Session；如果未找到，则为 {@code null}
	 * @throws JMSException 如果发生 JMS 失败
	 */
	@Nullable
	public static Session getTransactionalSession(final ConnectionFactory cf,
			@Nullable final Connection existingCon, final boolean synchedLocalTransactionAllowed)
			throws JMSException {

		return doGetTransactionalSession(cf, new ResourceFactory() {
			@Override
			@Nullable
			public Session getSession(JmsResourceHolder holder) {
				return holder.getSession(Session.class, existingCon);
			}
			@Override
			@Nullable
			public Connection getConnection(JmsResourceHolder holder) {
				return (existingCon != null ? existingCon : holder.getConnection());
			}
			@Override
			public Connection createConnection() throws JMSException {
				return cf.createConnection();
			}
			@Override
			public Session createSession(Connection con) throws JMSException {
				return con.createSession(synchedLocalTransactionAllowed, Session.AUTO_ACKNOWLEDGE);
			}
			@Override
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		}, true);
	}

	/**
	 * 获取与当前事务（如果有）同步的 JMS QueueSession。
	 * <p>主要用于 JMS 1.0.2 API。
	 * @param cf 要为其获取 Session 的 ConnectionFactory
	 * @param existingCon 要为其获取 Session 的现有 JMS Connection（可为 {@code null}）
	 * @param synchedLocalTransactionAllowed 是否允许本地 JMS 事务与 Spring 管理的事务同步
	 * （其中主事务可能是针对特定 DataSource 的基于 JDBC 的事务），
	 * JMS 事务在主事务之后立即提交。如果不允许，
	 * 给定的 ConnectionFactory 需要在底层处理事务的登记。
	 * @return 事务性 Session；如果未找到，则为 {@code null}
	 * @throws JMSException 如果发生 JMS 失败
	 */
	@Nullable
	public static QueueSession getTransactionalQueueSession(final QueueConnectionFactory cf,
			@Nullable final QueueConnection existingCon, final boolean synchedLocalTransactionAllowed)
			throws JMSException {

		return (QueueSession) doGetTransactionalSession(cf, new ResourceFactory() {
			@Override
			@Nullable
			public Session getSession(JmsResourceHolder holder) {
				return holder.getSession(QueueSession.class, existingCon);
			}
			@Override
			@Nullable
			public Connection getConnection(JmsResourceHolder holder) {
				return (existingCon != null ? existingCon : holder.getConnection(QueueConnection.class));
			}
			@Override
			public Connection createConnection() throws JMSException {
				return cf.createQueueConnection();
			}
			@Override
			public Session createSession(Connection con) throws JMSException {
				return ((QueueConnection) con).createQueueSession(synchedLocalTransactionAllowed, Session.AUTO_ACKNOWLEDGE);
			}
			@Override
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		}, true);
	}

	/**
	 * 获取与当前事务（如果有）同步的 JMS TopicSession。
	 * <p>主要用于 JMS 1.0.2 API。
	 * @param cf 要为其获取 Session 的 ConnectionFactory
	 * @param existingCon 要为其获取 Session 的现有 JMS Connection（可为 {@code null}）
	 * @param synchedLocalTransactionAllowed 是否允许本地 JMS 事务与 Spring 管理的事务同步
	 * （其中主事务可能是针对特定 DataSource 的基于 JDBC 的事务），
	 * JMS 事务在主事务之后立即提交。如果不允许，
	 * 给定的 ConnectionFactory 需要在底层处理事务的登记。
	 * @return 事务性 Session；如果未找到，则为 {@code null}
	 * @throws JMSException 如果发生 JMS 失败
	 */
	@Nullable
	public static TopicSession getTransactionalTopicSession(final TopicConnectionFactory cf,
			@Nullable final TopicConnection existingCon, final boolean synchedLocalTransactionAllowed)
			throws JMSException {

		return (TopicSession) doGetTransactionalSession(cf, new ResourceFactory() {
			@Override
			@Nullable
			public Session getSession(JmsResourceHolder holder) {
				return holder.getSession(TopicSession.class, existingCon);
			}
			@Override
			@Nullable
			public Connection getConnection(JmsResourceHolder holder) {
				return (existingCon != null ? existingCon : holder.getConnection(TopicConnection.class));
			}
			@Override
			public Connection createConnection() throws JMSException {
				return cf.createTopicConnection();
			}
			@Override
			public Session createSession(Connection con) throws JMSException {
				return ((TopicConnection) con).createTopicSession(
						synchedLocalTransactionAllowed, Session.AUTO_ACKNOWLEDGE);
			}
			@Override
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		}, true);
	}

	/**
	 * 获取与当前事务（如果有）同步的 JMS Session。
	 * <p>此 {@code doGetTransactionalSession} 变体始终启动底层
	 * JMS Connection，假定该 Session 将用于接收消息。
	 * @param connectionFactory 要绑定的 JMS ConnectionFactory
	 * （用作 TransactionSynchronizationManager 键）
	 * @param resourceFactory 用于提取或创建 JMS 资源的 ResourceFactory
	 * @return 事务性 Session；如果未找到，则为 {@code null}
	 * @throws JMSException 如果发生 JMS 失败
	 * @see #doGetTransactionalSession(javax.jms.ConnectionFactory, ResourceFactory, boolean)
	 */
	@Nullable
	public static Session doGetTransactionalSession(
			ConnectionFactory connectionFactory, ResourceFactory resourceFactory) throws JMSException {

		return doGetTransactionalSession(connectionFactory, resourceFactory, true);
	}

	/**
	 * 获取与当前事务（如果有）同步的 JMS Session。
	 * @param connectionFactory 要绑定的 JMS ConnectionFactory
	 * （用作 TransactionSynchronizationManager 键）
	 * @param resourceFactory 用于提取或创建 JMS 资源的 ResourceFactory
	 * @param startConnection 是否应启动底层 JMS Connection 方式以便接收消息。
	 * 注意，复用的 Connection 可能此前已经启动，
	 * 即使此标志为 {@code false}。
	 * @return 事务性 Session；如果未找到，则为 {@code null}
	 * @throws JMSException 如果发生 JMS 失败
	 */
	@Nullable
	public static Session doGetTransactionalSession(
			ConnectionFactory connectionFactory, ResourceFactory resourceFactory, boolean startConnection)
			throws JMSException {

		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		Assert.notNull(resourceFactory, "ResourceFactory must not be null");

		JmsResourceHolder resourceHolder =
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(connectionFactory);
		if (resourceHolder != null) {
			Session session = resourceFactory.getSession(resourceHolder);
			if (session != null) {
				if (startConnection) {
					Connection con = resourceFactory.getConnection(resourceHolder);
					if (con != null) {
						con.start();
					}
				}
				return session;
			}
			if (resourceHolder.isFrozen()) {
				return null;
			}
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return null;
		}
		JmsResourceHolder resourceHolderToUse = resourceHolder;
		if (resourceHolderToUse == null) {
			resourceHolderToUse = new JmsResourceHolder(connectionFactory);
		}
		Connection con = resourceFactory.getConnection(resourceHolderToUse);
		Session session = null;
		try {
			boolean isExistingCon = (con != null);
			if (!isExistingCon) {
				con = resourceFactory.createConnection();
				resourceHolderToUse.addConnection(con);
			}
			session = resourceFactory.createSession(con);
			resourceHolderToUse.addSession(session, con);
			if (startConnection) {
				con.start();
			}
		}
		catch (JMSException ex) {
			if (session != null) {
				try {
					session.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			if (con != null) {
				try {
					con.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			throw ex;
		}
		if (resourceHolderToUse != resourceHolder) {
			TransactionSynchronizationManager.registerSynchronization(
					new JmsResourceSynchronization(resourceHolderToUse, connectionFactory,
							resourceFactory.isSynchedLocalTransactionAllowed()));
			resourceHolderToUse.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.bindResource(connectionFactory, resourceHolderToUse);
		}
		return session;
	}


	/**
	 * 用于创建资源的回调接口。
	 * 作为 {@code doGetTransactionalSession} 方法的参数。
	 */
	public interface ResourceFactory {

		/**
		 * 从给定的 JmsResourceHolder 中获取合适的 Session。
		 * @param holder 该 JmsResourceHolder
		 * @return 从持有者中获取的相应 Session；如果未找到，则为 {@code null}
		 */
		@Nullable
		Session getSession(JmsResourceHolder holder);

		/**
		 * 从给定的 JmsResourceHolder 中获取合适的 Connection。
		 * @param holder 该 JmsResourceHolder
		 * @return 从持有者中获取的相应 Connection；如果未找到，则为 {@code null}
		 */
		@Nullable
		Connection getConnection(JmsResourceHolder holder);

		/**
		 * 创建一个新的 JMS Connection 并注册到 JmsResourceHolder。
		 * @return 新的 JMS Connection
		 * @throws JMSException 如果由 JMS API 方法抛出
		 */
		Connection createConnection() throws JMSException;

		/**
		 * 创建一个新的 JMS Session 并注册到 JmsResourceHolder。
		 * @param con 要为其创建 Session 的 JMS Connection
		 * @return 新的 JMS Session
		 * @throws JMSException 如果由 JMS API 方法抛出
		 */
		Session createSession(Connection con) throws JMSException;

		/**
		 * 返回是否允许与 Spring 管理的事务同步的本地 JMS 事务
		 * （其中主事务可能是针对特定 DataSource 的基于 JDBC 的事务等），
		 * 该 JMS 事务在主事务之后立即提交。
		 * @return 是否允许同步本地 JMS 事务
		 */
		boolean isSynchedLocalTransactionAllowed();
	}


	/**
	 * 用于在非原生 JMS 事务结束时清理资源的回调
	 * （例如，参与 JtaTransactionManager 事务时）。
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class JmsResourceSynchronization extends ResourceHolderSynchronization<JmsResourceHolder, Object> {

		private final boolean transacted;

		public JmsResourceSynchronization(JmsResourceHolder resourceHolder, Object resourceKey, boolean transacted) {
			super(resourceHolder, resourceKey);
			this.transacted = transacted;
		}

		@Override
		protected boolean shouldReleaseBeforeCompletion() {
			return !this.transacted;
		}

		@Override
		protected void processResourceAfterCommit(JmsResourceHolder resourceHolder) {
			try {
				resourceHolder.commitAll();
			}
			catch (JMSException ex) {
				throw new SynchedLocalTransactionFailedException("Local JMS transaction failed to commit", ex);
			}
		}

		@Override
		protected void releaseResource(JmsResourceHolder resourceHolder, Object resourceKey) {
			resourceHolder.closeAll();
		}
	}

}
