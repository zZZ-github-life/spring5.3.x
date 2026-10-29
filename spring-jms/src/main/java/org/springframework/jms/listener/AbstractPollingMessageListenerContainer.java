/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jms.listener;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.support.JmsUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

/**
 * 基于轮询的监听器容器实现的基类。
 * 提供基于 {@link javax.jms.MessageConsumer} 的监听器处理支持，
 * 可选地参与外部管理的事务。
 *
 * <p>此监听器容器变体专为重复轮询尝试而构建，
 * 每次都会调用 {@link #receiveAndExecute} 方法。所使用的 MessageConsumer
 * 可以在每次尝试时重新获取，也可以在尝试之间缓存；这取决于具体实现。
 * 每次尝试的接收超时可以通过
 * {@link #setReceiveTimeout "receiveTimeout"} 属性配置。
 *
 * <p>底层机制基于标准的 JMS MessageConsumer 处理，
 * 与原生 JMS 以及 Java EE 环境中的 JMS 完全兼容。
 * 既不需要 JMS {@code MessageConsumer.setMessageListener} 设施，
 * 也不需要 JMS ServerSessionPool 设施。这种方式的另一个优点是可以
 * 完全控制监听过程，允许自定义扩展和限流，以及并发消息处理
 * （这取决于具体子类）。
 *
 * <p>通过将 Spring 的
 * {@link org.springframework.transaction.PlatformTransactionManager} 传入
 * {@link #setTransactionManager "transactionManager"} 属性，
 * 消息接收和监听器执行可以自动包装在事务中。
 * 在 Java EE 环境中，这通常是
 * {@link org.springframework.transaction.jta.JtaTransactionManager}，
 * 并与从 JNDI 获取的支持 JTA 的 JMS ConnectionFactory 结合使用
 * （请查阅应用服务器的文档）。
 *
 * <p>此基类不假定任何特定的轮询调用器异步执行机制。
 * 请查看 {@link DefaultMessageListenerContainer} 了解基于 Spring 的
 * {@link org.springframework.core.task.TaskExecutor} 抽象的具体实现，
 * 包括并发消费者的动态伸缩和自动自我恢复。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #createListenerConsumer
 * @see #receiveAndExecute
 * @see #setTransactionManager
 */
public abstract class AbstractPollingMessageListenerContainer extends AbstractMessageListenerContainer {

	/**
	 * 默认接收超时：1000 毫秒 = 1 秒。
	 */
	public static final long DEFAULT_RECEIVE_TIMEOUT = 1000;


	private final MessageListenerContainerResourceFactory transactionalResourceFactory =
			new MessageListenerContainerResourceFactory();

	private boolean sessionTransactedCalled = false;

	@Nullable
	private PlatformTransactionManager transactionManager;

	private DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

	private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;


	@Override
	public void setSessionTransacted(boolean sessionTransacted) {
		super.setSessionTransacted(sessionTransacted);
		this.sessionTransactedCalled = true;
	}

	/**
	 * 指定用于事务性包装消息接收及监听器执行的 Spring
	 * {@link org.springframework.transaction.PlatformTransactionManager}。
	 * <p>默认无，不执行任何事务性包装。
	 * 如果指定，通常是 Spring 的
	 * {@link org.springframework.transaction.jta.JtaTransactionManager} 或其某个子类，
	 * 并与此消息监听器容器获取连接所用的支持 JTA 的 ConnectionFactory 结合使用。
	 * <p><b>注意：请考虑改用本地 JMS 事务。</b>
	 * 只需将 {@link #setSessionTransacted "sessionTransacted"} 标志切换为 "true"，
	 * 即可在整个接收处理过程中使用本地事务化的 JMS Session，
	 * 包括由 {@link SessionAwareMessageListener} 执行的任何 Session 操作
	 * （例如发送响应消息）。这样可以基于本地 JMS 事务实现完全同步的
	 * Spring 事务，类似于
	 * {@link org.springframework.jms.connection.JmsTransactionManager} 提供的行为。
	 * 有关事务选择和消息重新投递场景的讨论，请查看
	 * {@link AbstractMessageListenerContainer} 的 javadoc。
	 * @see #setSessionTransacted(boolean)
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 * @see org.springframework.jms.connection.JmsTransactionManager
	 */
	public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回用于事务性包装消息接收及监听器执行的
	 * Spring PlatformTransactionManager。
	 */
	@Nullable
	protected final PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * 指定用于事务性包装的事务名称。
	 * 默认为此监听器容器的 Bean 名称（如果有）。
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	public void setTransactionName(String transactionName) {
		this.transactionDefinition.setName(transactionName);
	}

	/**
	 * 指定用于事务性包装的事务超时，单位为<b>秒</b>。
	 * 默认无，使用事务管理器的默认超时。
	 * @see org.springframework.transaction.TransactionDefinition#getTimeout()
	 * @see #setReceiveTimeout
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionDefinition.setTimeout(transactionTimeout);
	}

	/**
	 * 设置接收调用使用的超时，单位为<b>毫秒</b>。
	 * 默认是 1000 毫秒，即 1 秒。
	 * <p><b>注意：</b>此值需要小于事务管理器使用的事务超时
	 * （当然要使用相应的单位）。0 表示完全没有超时；
	 * 然而，这只有在不运行于事务管理器内时才可行，并且通常不建议这样做，
	 * 因为这样的监听器容器无法干净地关闭。
	 * 负值（例如 -1）表示不等待的接收操作。
	 * @see #receiveFromConsumer(MessageConsumer, long)
	 * @see javax.jms.MessageConsumer#receive(long)
	 * @see javax.jms.MessageConsumer#receiveNoWait()
	 * @see javax.jms.MessageConsumer#receive()
	 * @see #setTransactionTimeout
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * 返回为此监听器容器配置的接收超时（毫秒）。
	 * @since 4.2
	 */
	protected long getReceiveTimeout() {
		return this.receiveTimeout;
	}


	@Override
	public void initialize() {
		// Set sessionTransacted=true in case of a non-JTA transaction manager.
		if (!this.sessionTransactedCalled &&
				this.transactionManager instanceof ResourceTransactionManager &&
				!TransactionSynchronizationUtils.sameResourceFactory(
						(ResourceTransactionManager) this.transactionManager, obtainConnectionFactory())) {
			super.setSessionTransacted(true);
		}

		// Use bean name as default transaction name.
		if (this.transactionDefinition.getName() == null) {
			String beanName = getBeanName();
			if (beanName != null) {
				this.transactionDefinition.setName(beanName);
			}
		}

		// Proceed with superclass initialization.
		super.initialize();
	}


	/**
	 * 为给定的 JMS Session 创建 MessageConsumer，
	 * 并为指定的监听器注册 MessageListener。
	 * @param session the JMS Session to work on
	 * @return the MessageConsumer
	 * @throws javax.jms.JMSException if thrown by JMS methods
	 * @see #receiveAndExecute
	 */
	protected MessageConsumer createListenerConsumer(Session session) throws JMSException {
		Destination destination = getDestination();
		if (destination == null) {
			String destinationName = getDestinationName();
			Assert.state(destinationName != null, "No destination set");
			destination = resolveDestinationName(session, destinationName);
		}
		return createConsumer(session, destination);
	}

	/**
	 * 对从给定消费者接收到的消息执行监听器，
	 * 如有要求则将整个操作包装在外部事务中。
	 * @param session the JMS Session to work on
	 * @param consumer the MessageConsumer to work on
	 * @return whether a message has been received
	 * @throws JMSException if thrown by JMS methods
	 * @see #doReceiveAndExecute
	 */
	protected boolean receiveAndExecute(
			Object invoker, @Nullable Session session, @Nullable MessageConsumer consumer)
			throws JMSException {

		if (this.transactionManager != null) {
			// Execute receive within transaction.
			TransactionStatus status = this.transactionManager.getTransaction(this.transactionDefinition);
			boolean messageReceived;
			try {
				messageReceived = doReceiveAndExecute(invoker, session, consumer, status);
			}
			catch (JMSException | RuntimeException | Error ex) {
				rollbackOnException(this.transactionManager, status, ex);
				throw ex;
			}
			try {
				this.transactionManager.commit(status);
			}
			catch (TransactionException ex) {
				// Propagate transaction system exceptions as infrastructure problems.
				throw ex;
			}
			catch (RuntimeException ex) {
				// Typically a late persistence exception from a listener-used resource
				// -> handle it as listener exception, not as an infrastructure problem.
				// E.g. a database locking failure should not lead to listener shutdown.
				handleListenerException(ex);
			}
			return messageReceived;
		}

		else {
			// Execute receive outside of transaction.
			return doReceiveAndExecute(invoker, session, consumer, null);
		}
	}

	/**
	 * 实际对从给定消费者接收到的消息执行监听器，
	 * 获取所有所需资源并调用监听器。
	 * @param session the JMS Session to work on
	 * @param consumer the MessageConsumer to work on
	 * @param status the TransactionStatus (may be {@code null})
	 * @return whether a message has been received
	 * @throws JMSException if thrown by JMS methods
	 * @see #doExecuteListener(javax.jms.Session, javax.jms.Message)
	 */
	protected boolean doReceiveAndExecute(Object invoker, @Nullable Session session,
			@Nullable MessageConsumer consumer, @Nullable TransactionStatus status) throws JMSException {

		Connection conToClose = null;
		Session sessionToClose = null;
		MessageConsumer consumerToClose = null;
		try {
			Session sessionToUse = session;
			boolean transactional = false;
			if (sessionToUse == null) {
				sessionToUse = ConnectionFactoryUtils.doGetTransactionalSession(
						obtainConnectionFactory(), this.transactionalResourceFactory, true);
				transactional = (sessionToUse != null);
			}
			if (sessionToUse == null) {
				Connection conToUse;
				if (sharedConnectionEnabled()) {
					conToUse = getSharedConnection();
				}
				else {
					conToUse = createConnection();
					conToClose = conToUse;
					conToUse.start();
				}
				sessionToUse = createSession(conToUse);
				sessionToClose = sessionToUse;
			}
			MessageConsumer consumerToUse = consumer;
			if (consumerToUse == null) {
				consumerToUse = createListenerConsumer(sessionToUse);
				consumerToClose = consumerToUse;
			}
			Message message = receiveMessage(consumerToUse);
			if (message != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Received message of type [" + message.getClass() + "] from consumer [" +
							consumerToUse + "] of " + (transactional ? "transactional " : "") + "session [" +
							sessionToUse + "]");
				}
				messageReceived(invoker, sessionToUse);
				boolean exposeResource = (!transactional && isExposeListenerSession() &&
						!TransactionSynchronizationManager.hasResource(obtainConnectionFactory()));
				if (exposeResource) {
					TransactionSynchronizationManager.bindResource(
							obtainConnectionFactory(), new LocallyExposedJmsResourceHolder(sessionToUse));
				}
				try {
					doExecuteListener(sessionToUse, message);
				}
				catch (Throwable ex) {
					if (status != null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Rolling back transaction because of listener exception thrown: " + ex);
						}
						status.setRollbackOnly();
					}
					handleListenerException(ex);
					// Rethrow JMSException to indicate an infrastructure problem
					// that may have to trigger recovery...
					if (ex instanceof JMSException) {
						throw (JMSException) ex;
					}
				}
				finally {
					if (exposeResource) {
						TransactionSynchronizationManager.unbindResource(obtainConnectionFactory());
					}
				}
				// Indicate that a message has been received.
				return true;
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Consumer [" + consumerToUse + "] of " + (transactional ? "transactional " : "") +
							"session [" + sessionToUse + "] did not receive a message");
				}
				noMessageReceived(invoker, sessionToUse);
				// Nevertheless call commit, in order to reset the transaction timeout (if any).
				if (shouldCommitAfterNoMessageReceived(sessionToUse)) {
					commitIfNecessary(sessionToUse, null);
				}
				// Indicate that no message has been received.
				return false;
			}
		}
		finally {
			JmsUtils.closeMessageConsumer(consumerToClose);
			JmsUtils.closeSession(sessionToClose);
			ConnectionFactoryUtils.releaseConnection(conToClose, getConnectionFactory(), true);
		}
	}

	/**
	 * 此实现检查 Session 是否为外部同步的。
	 * 在这种情况下，尽管监听器容器的 "sessionTransacted" 标志设置为 "true"，
	 * Session 也不会进行本地事务化。
	 * @see org.springframework.jms.connection.JmsResourceHolder
	 */
	@Override
	protected boolean isSessionLocallyTransacted(Session session) {
		if (!super.isSessionLocallyTransacted(session)) {
			return false;
		}
		JmsResourceHolder resourceHolder =
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(obtainConnectionFactory());
		return (resourceHolder == null || resourceHolder instanceof LocallyExposedJmsResourceHolder ||
				!resourceHolder.containsSession(session));
	}

	/**
	 * 确定在未收到任何消息后是否触发提交。
	 * 在任何现代 JMS 提供者上这都是个好主意。
	 * @param session the current JMS Session which received no message
	 * @return whether to call {@link #commitIfNecessary} on the given Session
	 */
	protected boolean shouldCommitAfterNoMessageReceived(Session session) {
		return true;
	}

	/**
	 * 执行回滚，并妥善处理回滚异常。
	 * @param status object representing the transaction
	 * @param ex the thrown listener exception or error
	 */
	private void rollbackOnException(PlatformTransactionManager manager, TransactionStatus status, Throwable ex) {
		logger.debug("Initiating transaction rollback on listener exception", ex);
		try {
			manager.rollback(status);
		}
		catch (RuntimeException ex2) {
			logger.error("Listener exception overridden by rollback exception", ex);
			throw ex2;
		}
		catch (Error err) {
			logger.error("Listener exception overridden by rollback error", ex);
			throw err;
		}
	}

	/**
	 * 从给定消费者接收消息。
	 * @param consumer the MessageConsumer to use
	 * @return the Message, or {@code null} if none
	 * @throws JMSException if thrown by JMS methods
	 */
	@Nullable
	protected Message receiveMessage(MessageConsumer consumer) throws JMSException {
		return receiveFromConsumer(consumer, getReceiveTimeout());
	}

	/**
	 * 模板方法，在收到新消息后、尝试处理它之前立即调用。
	 * 允许子类对实际传入消息的事件作出反应，例如调整其消费者数量。
	 * @param invoker the invoker object (passed through)
	 * @param session the receiving JMS Session
	 */
	protected void messageReceived(Object invoker, Session session) {
	}

	/**
	 * 模板方法，在<i>未</i>收到任何消息时、返回接收循环之前调用。
	 * 允许子类对没有传入消息的事件作出反应，例如将调用器标记为空闲。
	 * @param invoker the invoker object (passed through)
	 * @param session the receiving JMS Session
	 */
	protected void noMessageReceived(Object invoker, Session session) {
	}

	/**
	 * 从给定的 JmsResourceHolder 中获取适当的 Connection。
	 * <p>此实现接受任何 JMS 1.1 Connection。
	 * @param holder the JmsResourceHolder
	 * @return an appropriate Connection fetched from the holder,
	 * or {@code null} if none found
	 */
	@Nullable
	protected Connection getConnection(JmsResourceHolder holder) {
		return holder.getConnection();
	}

	/**
	 * 从给定的 JmsResourceHolder 中获取适当的 Session。
	 * <p>此实现接受任何 JMS 1.1 Session。
	 * @param holder the JmsResourceHolder
	 * @return an appropriate Session fetched from the holder,
	 * or {@code null} if none found
	 */
	@Nullable
	protected Session getSession(JmsResourceHolder holder) {
		return holder.getSession();
	}


	/**
	 * ResourceFactory 实现，委托给此监听器容器的受保护回调方法。
	 */
	private class MessageListenerContainerResourceFactory implements ConnectionFactoryUtils.ResourceFactory {

		@Override
		@Nullable
		public Connection getConnection(JmsResourceHolder holder) {
			return AbstractPollingMessageListenerContainer.this.getConnection(holder);
		}

		@Override
		@Nullable
		public Session getSession(JmsResourceHolder holder) {
			return AbstractPollingMessageListenerContainer.this.getSession(holder);
		}

		@Override
		public Connection createConnection() throws JMSException {
			if (AbstractPollingMessageListenerContainer.this.sharedConnectionEnabled()) {
				Connection sharedCon = AbstractPollingMessageListenerContainer.this.getSharedConnection();
				return new SingleConnectionFactory(sharedCon).createConnection();
			}
			else {
				return AbstractPollingMessageListenerContainer.this.createConnection();
			}
		}

		@Override
		public Session createSession(Connection con) throws JMSException {
			return AbstractPollingMessageListenerContainer.this.createSession(con);
		}

		@Override
		public boolean isSynchedLocalTransactionAllowed() {
			return AbstractPollingMessageListenerContainer.this.isSessionTransacted();
		}
	}

}
