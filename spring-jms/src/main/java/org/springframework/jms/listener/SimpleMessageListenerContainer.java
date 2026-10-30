/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.jms.support.JmsUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 消息监听器容器，使用普通 JMS 客户端 API 的
 * {@code MessageConsumer.setMessageListener()} 方法
 * 为指定的监听器创建并发的 MessageConsumer。
 *
 * <p>这是消息监听器容器的最简单形式。
 * 它创建固定数量的 JMS Session 来调用监听器，
 * 不允许根据运行时需求动态调整。其主要优点是复杂度低，
 * 对 JMS 提供者的要求最低：甚至不需要 ServerSessionPool 设施。
 *
 * <p>有关确认模式和事务选项的详细信息，请参见
 * {@link AbstractMessageListenerContainer} 的 javadoc。
 * 请注意，此容器在默认的 "AUTO_ACKNOWLEDGE" 模式下暴露出标准 JMS 行为：
 * 即在监听器执行后自动确认消息；如果抛出用户异常则不会重新投递，
 * 但如果 JVM 在监听器执行期间终止，则可能重新投递。
 *
 * <p>如需另一种 MessageListener 处理方式，即通过循环调用
 * {@code MessageConsumer.receive()}，并且还允许事务性接收消息
 * （将它们登记到 XA 事务中），请参见 {@link DefaultMessageListenerContainer}。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.jms.MessageConsumer#setMessageListener
 * @see DefaultMessageListenerContainer
 * @see org.springframework.jms.listener.endpoint.JmsMessageEndpointManager
 */
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer implements ExceptionListener {

	private boolean connectLazily = false;

	private boolean recoverOnException = true;

	private int concurrentConsumers = 1;

	@Nullable
	private Executor taskExecutor;

	@Nullable
	private Set<Session> sessions;

	@Nullable
	private Set<MessageConsumer> consumers;

	private final Object consumersMonitor = new Object();


	/**
	 * 指定是否延迟连接，即是否尽可能晚地（在此容器的启动阶段）
	 * 建立 JMS Connection 以及相应的 Session 和 MessageConsumer。
	 * <p>默认是 "false"：即尽早连接，在 Bean 初始化阶段进行。
	 * 如果目标代理可能尚未启动，且您希望甚至不尝试连接，
	 * 请将此标志设置为 "true" 以切换到延迟连接。
	 * @see #start()
	 * @see #initialize()
	 */
	public void setConnectLazily(boolean connectLazily) {
		this.connectLazily = connectLazily;
	}

	/**
	 * 指定每当报告 JMSException 时，是否显式恢复共享 JMS Connection
	 * 及其关联的 Session 和 MessageConsumer。
	 * <p>默认是 "true"：每当连接向其监听器传播异常时，
	 * 刷新共享连接并重新初始化消费者。
	 * 将此标志设置为 "false" 可依赖提供者内部的自动恢复，
	 * 保留现有的连接和消费者句柄。
	 * @since 5.1.8
	 * @see #onException(JMSException)
	 * @see Connection#setExceptionListener
	 */
	public void setRecoverOnException(boolean recoverOnException) {
		this.recoverOnException = recoverOnException;
	}

	/**
	 * 通过 "lower-upper" 形式的字符串（例如 "5-10"）或简单的上限字符串
	 * （例如 "10"）指定并发限制。
	 * <p>此监听器容器由于无法伸缩，将始终保留最大数量的
	 * {@link #setConcurrentConsumers} 消费者。
	 * <p>支持此属性主要是为了与
	 * {@link DefaultMessageListenerContainer} 的配置兼容。
	 * 对于此本地监听器容器，通常请改用 {@link #setConcurrentConsumers}。
	 */
	@Override
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setConcurrentConsumers(Integer.parseInt(concurrency.substring(separatorIndex + 1)));
			}
			else {
				setConcurrentConsumers(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported. " +
					"Note that SimpleMessageListenerContainer will effectively ignore the minimum value and " +
					"always keep a fixed number of consumers according to the maximum value.");
		}
	}

	/**
	 * 指定要创建的并发消费者数量。默认为 1。
	 * <p>建议增加并发消费者数量以扩展队列中消息的消费能力。
	 * 但请注意，一旦注册了多个消费者，任何顺序保证都会丢失。
	 * 通常，对于低流量队列请坚持使用 1 个消费者。
	 * <p><b>请勿为主题增加并发消费者数量。</b>
	 * 这会导致对同一消息的并发消费，几乎从不可取。
	 */
	public void setConcurrentConsumers(int concurrentConsumers) {
		Assert.isTrue(concurrentConsumers > 0, "'concurrentConsumers' value must be at least 1 (one)");
		this.concurrentConsumers = concurrentConsumers;
	}

	/**
	 * 设置 Spring TaskExecutor，用于在提供者收到消息后执行监听器。
	 * <p>默认无，即在 JMS 提供者自己的接收线程中运行，
	 * 执行监听器时会阻塞提供者的接收端点。
	 * <p>指定 TaskExecutor 可在不同线程中执行监听器，
	 * 而不是阻塞 JMS 提供者，通常与现有线程池集成。
	 * 这样可以在保持较低并发消费者数量（1）的同时
	 * 并发处理消息（与接收解耦！）。
	 * <p><b>注意：为监听器执行指定 TaskExecutor 会影响确认语义。</b>
	 * 此时消息将始终在监听器执行之前被确认，底层 Session 会立即被复用于
	 * 接收下一条消息。将其与事务会话或客户端确认结合使用
	 * 将导致未定义的结果！
	 * <p><b>注意：通过 TaskExecutor 并发执行监听器将导致
	 * 对由同一底层 Session 接收的消息进行并发处理。</b>
	 * 因此，不建议将此设置与 {@link SessionAwareMessageListener} 一起使用，
	 * 至少在后者要对给定 Session 执行实际工作的情况下。
	 * 通常，标准的 {@link javax.jms.MessageListener} 可以正常工作。
	 * @see #setConcurrentConsumers
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	protected void validateConfiguration() {
		super.validateConfiguration();
		if (isSubscriptionDurable() && this.concurrentConsumers != 1) {
			throw new IllegalArgumentException("Only 1 concurrent consumer supported for durable subscription");
		}
	}


	//-------------------------------------------------------------------------
	// Implementation of AbstractMessageListenerContainer's template methods
	//-------------------------------------------------------------------------

	/**
	 * 始终使用共享的 JMS Connection。
	 */
	@Override
	protected final boolean sharedConnectionEnabled() {
		return true;
	}

	/**
	 * 创建指定数量的并发消费者，
	 * 形式为 JMS Session 加上关联的 MessageConsumer。
	 * @see #createListenerConsumer
	 */
	@Override
	protected void doInitialize() throws JMSException {
		if (!this.connectLazily) {
			try {
				establishSharedConnection();
			}
			catch (JMSException ex) {
				logger.debug("Could not connect on initialization - registering message consumers lazily", ex);
				return;
			}
			initializeConsumers();
		}
	}

	/**
	 * 重新初始化此容器的 JMS 消息消费者（如果尚未初始化）。
	 */
	@Override
	protected void doStart() throws JMSException {
		super.doStart();
		initializeConsumers();
	}

	/**
	 * 将此监听器容器注册为共享连接上的 JMS ExceptionListener。
	 */
	@Override
	protected void prepareSharedConnection(Connection connection) throws JMSException {
		super.prepareSharedConnection(connection);
		connection.setExceptionListener(this);
	}

	/**
	 * JMS ExceptionListener 实现，在连接失败时由 JMS 提供者调用。
	 * 如有必要，重新初始化此监听器容器的共享连接及其会话和消费者。
	 * @param ex 报告的连接异常
	 * @see #setRecoverOnException
	 * @see #refreshSharedConnection()
	 * @see #initializeConsumers()
	 */
	@Override
	public void onException(JMSException ex) {
		// First invoke the user-specific ExceptionListener, if any.
		invokeExceptionListener(ex);

		// Now try to recover the shared Connection and all consumers...
		if (this.recoverOnException) {
			if (logger.isDebugEnabled()) {
				logger.debug("Trying to recover from JMS Connection exception: " + ex);
			}
			try {
				synchronized (this.consumersMonitor) {
					this.sessions = null;
					this.consumers = null;
				}
				refreshSharedConnection();
				initializeConsumers();
				logger.debug("Successfully refreshed JMS Connection");
			}
			catch (JMSException recoverEx) {
				logger.debug("Failed to recover JMS Connection", recoverEx);
				logger.error("Encountered non-recoverable JMSException", ex);
			}
		}
	}

	/**
	 * 为此容器初始化 JMS Session 和 MessageConsumer。
	 * @throws JMSException 设置失败时
	 */
	protected void initializeConsumers() throws JMSException {
		// Register Sessions and MessageConsumers.
		synchronized (this.consumersMonitor) {
			if (this.consumers == null) {
				this.sessions = new HashSet<>(this.concurrentConsumers);
				this.consumers = new HashSet<>(this.concurrentConsumers);
				Connection con = getSharedConnection();
				for (int i = 0; i < this.concurrentConsumers; i++) {
					Session session = createSession(con);
					MessageConsumer consumer = createListenerConsumer(session);
					this.sessions.add(session);
					this.consumers.add(consumer);
				}
			}
		}
	}

	/**
	 * 为给定的 JMS Session 创建 MessageConsumer，
	 * 并为指定的监听器注册 MessageListener。
	 * @param session 要操作的 JMS Session
	 * @return 该 MessageConsumer
	 * @throws JMSException 如果由 JMS 方法抛出
	 * @see #executeListener
	 */
	protected MessageConsumer createListenerConsumer(final Session session) throws JMSException {
		Destination destination = getDestination();
		if (destination == null) {
			String destinationName = getDestinationName();
			Assert.state(destinationName != null, "No destination set");
			destination = resolveDestinationName(session, destinationName);
		}
		MessageConsumer consumer = createConsumer(session, destination);

		if (this.taskExecutor != null) {
			consumer.setMessageListener(message -> this.taskExecutor.execute(() -> processMessage(message, session)));
		}
		else {
			consumer.setMessageListener(message -> processMessage(message, session));
		}

		return consumer;
	}

	/**
	 * 处理从提供者收到的消息。
	 * <p>执行监听器，并将当前 JMS Session 暴露为
	 * 线程绑定资源（如果 "exposeListenerSession" 为 "true"）。
	 * @param message 收到的 JMS 消息
	 * @param session 要操作的 JMS Session
	 * @see #executeListener
	 * @see #setExposeListenerSession
	 */
	protected void processMessage(Message message, Session session) {
		ConnectionFactory connectionFactory = getConnectionFactory();
		boolean exposeResource = (connectionFactory != null && isExposeListenerSession());
		if (exposeResource) {
			TransactionSynchronizationManager.bindResource(
					connectionFactory, new LocallyExposedJmsResourceHolder(session));
		}
		try {
			executeListener(session, message);
		}
		finally {
			if (exposeResource) {
				TransactionSynchronizationManager.unbindResource(getConnectionFactory());
			}
		}
	}

	/**
	 * 销毁已注册的 JMS Session 及关联的 MessageConsumer。
	 */
	@Override
	protected void doShutdown() throws JMSException {
		synchronized (this.consumersMonitor) {
			if (this.consumers != null) {
				logger.debug("Closing JMS MessageConsumers");
				for (MessageConsumer consumer : this.consumers) {
					JmsUtils.closeMessageConsumer(consumer);
				}
				if (this.sessions != null) {
					logger.debug("Closing JMS Sessions");
					for (Session session : this.sessions) {
						JmsUtils.closeSession(session);
					}
				}
			}
		}
	}

}
