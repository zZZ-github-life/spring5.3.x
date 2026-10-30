/*
 * Copyright 2002-2019 the original author or authors.
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
import javax.jms.Session;
import javax.jms.TransactionRolledBackException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 用于单个 JMS {@link javax.jms.ConnectionFactory} 的
 * {@link org.springframework.transaction.PlatformTransactionManager} 实现。
 * 将从指定 ConnectionFactory 获取的 JMS Connection/Session 对绑定到线程，
 * 从而每个 ConnectionFactory 最多允许一个线程绑定的 Session。
 *
 * <p>这种本地策略是在 JTA 事务中执行 JMS 操作的一种替代方案。其优点是能够
 * 在任何环境中工作，例如独立应用或测试套件，并以任意消息代理作为目标。
 * 但是，此策略<i>不能</i>提供 XA 事务，例如无法在消息传递和数据库访问之间
 * 共享事务。XA 事务需要完整的 JTA/XA 设置，通常以 Spring 的
 * {@link org.springframework.transaction.jta.JtaTransactionManager} 作为策略。
 *
 * <p>应用代码需要通过 {@link ConnectionFactoryUtils#getTransactionalSession}
 * 获取事务性 JMS Session，而不是使用标准的 Java EE 风格
 * {@link ConnectionFactory#createConnection()} 调用再创建 Session。
 * Spring 的 {@link org.springframework.jms.core.JmsTemplate} 会自动检测
 * 线程绑定的 Session 并自动参与其中。
 *
 * <p>或者，你也可以允许应用代码在 ConnectionFactory 上使用标准的
 * Java EE 风格查找模式，例如对于完全不感知 Spring 的遗留代码。
 * 在这种情况下，请为目标 ConnectionFactory 定义一个
 * {@link TransactionAwareConnectionFactoryProxy}，它将自动参与
 * Spring 管理的事务。
 *
 * <p><b>强烈建议使用 {@link CachingConnectionFactory} 作为此事务管理器的目标。</b>
 * CachingConnectionFactory 对所有 JMS 访问使用单个 JMS Connection，
 * 以避免重复创建 Connection 的开销，并维护 Session 缓存。
 * 这样每个事务将共享同一个 JMS Connection，同时仍使用各自独立的 JMS Session。
 *
 * <p>使用<i>原始</i>目标 ConnectionFactory 不仅会因为缺乏资源复用而效率低下，
 * 还可能产生奇怪的效果：当你的 JMS 驱动不接受在 {@code Session.commit()}
 * 之前调用 {@code MessageProducer.close()} 和/或 {@code MessageConsumer.close()}
 * 时（后者本应提交通过生产者句柄发送、通过消费者句柄接收的所有消息）。
 * 作为安全的通用解决方案，请始终向此事务管理器的
 * {@link #setConnectionFactory "connectionFactory"} 属性传入
 * {@link CachingConnectionFactory}。
 *
 * <p>事务同步默认关闭，因为此管理器可能与基于数据存储的 Spring 事务管理器
 * （如 JDBC {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}）
 * 一起使用，后者对同步有更强的需求。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see ConnectionFactoryUtils#getTransactionalSession
 * @see TransactionAwareConnectionFactoryProxy
 * @see org.springframework.jms.core.JmsTemplate
 */
@SuppressWarnings("serial")
public class JmsTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	@Nullable
	private ConnectionFactory connectionFactory;

	private boolean lazyResourceRetrieval = false;


	/**
	 * 创建一个用于 bean 风格用法的新 JmsTransactionManager。
	 * <p>注意：在使用实例之前必须设置 ConnectionFactory。
	 * 此构造函数可用于通过 BeanFactory 准备 JmsTemplate，
	 * 通常通过 setConnectionFactory 设置 ConnectionFactory。
	 * <p>默认关闭事务同步，因为此管理器可能与基于数据存储的 Spring
	 * 事务管理器（如 DataSourceTransactionManager）一起使用，
	 * 后者对同步有更强的需求。在任何时间点只允许一个管理器驱动同步。
	 * @see #setConnectionFactory
	 * @see #setTransactionSynchronization
	 */
	public JmsTransactionManager() {
		setTransactionSynchronization(SYNCHRONIZATION_NEVER);
	}

	/**
	 * 根据给定的 ConnectionFactory 创建一个新的 JmsTransactionManager。
	 * @param connectionFactory 用于获取连接的 ConnectionFactory
	 */
	public JmsTransactionManager(ConnectionFactory connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应为其管理事务的 JMS ConnectionFactory。
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
	 * 返回此实例应为其管理事务的 JMS ConnectionFactory。
	 */
	@Nullable
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * 获取实际使用的 ConnectionFactory。
	 * @return 该 ConnectionFactory（永不为 {@code null}）
	 * @throws IllegalStateException 如果未设置 ConnectionFactory
	 * @since 5.0
	 */
	protected final ConnectionFactory obtainConnectionFactory() {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory;
	}

	/**
	 * 指定此事务管理器是否应在事务中访问时延迟获取 JMS Connection 和 Session
	 * （{@code true}）。默认情况下，它会在事务开始时立即创建 JMS Connection
	 * 和 Session（{@code false}）。
	 * @since 5.1.6
	 * @see JmsResourceHolder#getConnection()
	 * @see JmsResourceHolder#getSession()
	 */
	public void setLazyResourceRetrieval(boolean lazyResourceRetrieval) {
		this.lazyResourceRetrieval = lazyResourceRetrieval;
	}

	/**
	 * 确保已设置 ConnectionFactory。
	 */
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
		JmsTransactionObject txObject = new JmsTransactionObject();
		txObject.setResourceHolder(
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(obtainConnectionFactory()));
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		return txObject.hasResourceHolder();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException("JMS does not support an isolation level concept");
		}

		ConnectionFactory connectionFactory = obtainConnectionFactory();
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		Connection con = null;
		Session session = null;
		try {
			JmsResourceHolder resourceHolder;
			if (this.lazyResourceRetrieval) {
				resourceHolder = new LazyJmsResourceHolder(connectionFactory);
			}
			else {
				con = createConnection();
				session = createSession(con);
				if (logger.isDebugEnabled()) {
					logger.debug("Created JMS transaction on Session [" + session + "] from Connection [" + con + "]");
				}
				resourceHolder = new JmsResourceHolder(connectionFactory, con, session);
			}
			resourceHolder.setSynchronizedWithTransaction(true);
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				resourceHolder.setTimeoutInSeconds(timeout);
			}
			txObject.setResourceHolder(resourceHolder);
			TransactionSynchronizationManager.bindResource(connectionFactory, resourceHolder);
		}
		catch (Throwable ex) {
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
			throw new CannotCreateTransactionException("Could not create JMS transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		txObject.setResourceHolder(null);
		return TransactionSynchronizationManager.unbindResource(obtainConnectionFactory());
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		TransactionSynchronizationManager.bindResource(obtainConnectionFactory(), suspendedResources);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		Session session = txObject.getResourceHolder().getOriginalSession();
		if (session != null) {
			try {
				if (status.isDebug()) {
					logger.debug("Committing JMS transaction on Session [" + session + "]");
				}
				session.commit();
			}
			catch (TransactionRolledBackException ex) {
				throw new UnexpectedRollbackException("JMS transaction rolled back", ex);
			}
			catch (JMSException ex) {
				throw new TransactionSystemException("Could not commit JMS transaction", ex);
			}
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		Session session = txObject.getResourceHolder().getOriginalSession();
		if (session != null) {
			try {
				if (status.isDebug()) {
					logger.debug("Rolling back JMS transaction on Session [" + session + "]");
				}
				session.rollback();
			}
			catch (JMSException ex) {
				throw new TransactionSystemException("Could not roll back JMS transaction", ex);
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		txObject.getResourceHolder().setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		TransactionSynchronizationManager.unbindResource(obtainConnectionFactory());
		txObject.getResourceHolder().closeAll();
		txObject.getResourceHolder().clear();
	}


	/**
	 * 通过此模板的 ConnectionFactory 创建 JMS Connection。
	 * <p>此实现使用 JMS 1.1 API。
	 * @return 新的 JMS Connection
	 * @throws javax.jms.JMSException 如果由 JMS API 方法抛出
	 */
	protected Connection createConnection() throws JMSException {
		return obtainConnectionFactory().createConnection();
	}

	/**
	 * 为给定的 Connection 创建 JMS Session。
	 * <p>此实现使用 JMS 1.1 API。
	 * @param con 要为其创建 Session 的 JMS Connection
	 * @return 新的 JMS Session
	 * @throws javax.jms.JMSException 如果由 JMS API 方法抛出
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(true, Session.AUTO_ACKNOWLEDGE);
	}


	/**
	 * {@link JmsResourceHolder} 的延迟初始化变体，
	 * 在用户访问时初始化 JMS Connection 和 Session。
	 */
	private class LazyJmsResourceHolder extends JmsResourceHolder {

		private boolean connectionInitialized = false;

		private boolean sessionInitialized = false;

		public LazyJmsResourceHolder(@Nullable ConnectionFactory connectionFactory) {
			super(connectionFactory);
		}

		@Override
		@Nullable
		public Connection getConnection() {
			initializeConnection();
			return super.getConnection();
		}

		@Override
		@Nullable
		public <C extends Connection> C getConnection(Class<C> connectionType) {
			initializeConnection();
			return super.getConnection(connectionType);
		}

		@Override
		@Nullable
		public Session getSession() {
			initializeSession();
			return super.getSession();
		}

		@Override
		@Nullable
		public <S extends Session> S getSession(Class<S> sessionType) {
			initializeSession();
			return super.getSession(sessionType);
		}

		@Override
		@Nullable
		public <S extends Session> S getSession(Class<S> sessionType, @Nullable Connection connection) {
			initializeSession();
			return super.getSession(sessionType, connection);
		}

		private void initializeConnection() {
			if (!this.connectionInitialized) {
				try {
					addConnection(createConnection());
				}
				catch (JMSException ex) {
					throw new CannotCreateTransactionException(
							"Failed to lazily initialize JMS Connection for transaction", ex);
				}
				this.connectionInitialized = true;
			}
		}

		private void initializeSession() {
			if (!this.sessionInitialized) {
				Connection con = getConnection();
				Assert.state(con != null, "No transactional JMS Connection");
				try {
					addSession(createSession(con), con);
				}
				catch (JMSException ex) {
					throw new CannotCreateTransactionException(
							"Failed to lazily initialize JMS Session for transaction", ex);
				}
				this.sessionInitialized = true;
			}
		}
	}


	/**
	 * JMS 事务对象，表示一个 JmsResourceHolder。
	 * 由 JmsTransactionManager 用作事务对象。
	 * @see JmsResourceHolder
	 */
	private static class JmsTransactionObject implements SmartTransactionObject {

		@Nullable
		private JmsResourceHolder resourceHolder;

		public void setResourceHolder(@Nullable JmsResourceHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		public JmsResourceHolder getResourceHolder() {
			Assert.state(this.resourceHolder != null, "No JmsResourceHolder available");
			return this.resourceHolder;
		}

		public boolean hasResourceHolder() {
			return (this.resourceHolder != null);
		}

		@Override
		public boolean isRollbackOnly() {
			return (this.resourceHolder != null && this.resourceHolder.isRollbackOnly());
		}

		@Override
		public void flush() {
			// no-op
		}
	}

}
