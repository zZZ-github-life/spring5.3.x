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

package org.springframework.jms.core;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.springframework.jms.JmsException;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 简化同步 JMS 访问代码的辅助类。
 *
 * <p>如果希望使用动态目标创建，必须通过 "pubSubDomain" 属性指定要创建的
 * JMS 目标类型。对于其他操作，这并非必需。点对点（队列）是默认域。
 *
 * <p>JMS Session 的默认设置为 "非事务" 和 "自动确认"。
 * 按照 Java EE 规范的定义，在活动事务（无论是 JTA 事务还是 Spring 管理的事务）
 * 内创建 JMS Session 时，事务和确认参数会被忽略。若要为原生 JMS 用法配置它们，
 * 请为 "sessionTransacted" 和 "sessionAcknowledgeMode" bean 属性指定适当的值。
 *
 * <p>此模板默认使用
 * {@link org.springframework.jms.support.destination.DynamicDestinationResolver}
 * 和 {@link org.springframework.jms.support.converter.SimpleMessageConverter}
 * 分别作为解析目标名称和转换消息的默认策略。这些默认值可以通过
 * "destinationResolver" 和 "messageConverter" bean 属性覆盖。
 *
 * <p><b>注意：与此模板一起使用的 {@code ConnectionFactory} 应返回池化的
 * Connection（或单个共享 Connection）以及池化的 Session 和 MessageProducer。
 * 否则，临时 JMS 操作的性能将受到影响。</b>最简单的选择是使用 Spring 提供的
 * {@link org.springframework.jms.connection.SingleConnectionFactory} 作为目标
 * {@code ConnectionFactory} 的装饰器，以线程安全的方式复用单个 JMS Connection；
 * 对于通过此模板发送消息而言，这通常已经足够。在 Java EE 环境中，
 * 请确保通过 JNDI 从应用环境的命名上下文获取 {@code ConnectionFactory}；
 * 应用服务器通常会在那里公开池化的、事务感知的工厂。
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 1.1
 * @see #setConnectionFactory
 * @see #setPubSubDomain
 * @see #setDestinationResolver
 * @see #setMessageConverter
 * @see javax.jms.MessageProducer
 * @see javax.jms.MessageConsumer
 */
public class JmsTemplate extends JmsDestinationAccessor implements JmsOperations {

	/** 用于与 ConnectionFactoryUtils 交互的内部 ResourceFactory 适配器。 */
	private final JmsTemplateResourceFactory transactionalResourceFactory = new JmsTemplateResourceFactory();


	@Nullable
	private Object defaultDestination;

	@Nullable
	private MessageConverter messageConverter;


	private boolean messageIdEnabled = true;

	private boolean messageTimestampEnabled = true;

	private boolean pubSubNoLocal = false;

	private long receiveTimeout = RECEIVE_TIMEOUT_INDEFINITE_WAIT;

	private long deliveryDelay = -1;


	private boolean explicitQosEnabled = false;

	private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;

	private int priority = Message.DEFAULT_PRIORITY;

	private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;


	/**
	 * 创建一个用于 bean 风格用法的新 JmsTemplate。
	 * <p>注意：在使用实例之前必须设置 ConnectionFactory。
	 * 此构造函数可用于通过 BeanFactory 准备 JmsTemplate，
	 * 通常通过 setConnectionFactory 设置 ConnectionFactory。
	 * @see #setConnectionFactory
	 */
	public JmsTemplate() {
		initDefaultStrategies();
	}

	/**
	 * 根据给定的 ConnectionFactory 创建一个新的 JmsTemplate。
	 * @param connectionFactory the ConnectionFactory to obtain Connections from
	 */
	public JmsTemplate(ConnectionFactory connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}

	/**
	 * 初始化模板策略的默认实现：
	 * DynamicDestinationResolver 和 SimpleMessageConverter。
	 * @see #setDestinationResolver
	 * @see #setMessageConverter
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.converter.SimpleMessageConverter
	 */
	protected void initDefaultStrategies() {
		setMessageConverter(new SimpleMessageConverter());
	}


	/**
	 * 设置用于没有目标参数的发送/接收操作的目标。
	 * <p>或者，指定 "defaultDestinationName"，通过 DestinationResolver 动态解析。
	 * @see #send(MessageCreator)
	 * @see #convertAndSend(Object)
	 * @see #convertAndSend(Object, MessagePostProcessor)
	 * @see #setDefaultDestinationName(String)
	 */
	public void setDefaultDestination(@Nullable Destination destination) {
		this.defaultDestination = destination;
	}

	/**
	 * 返回用于没有目标参数的发送/接收操作的目标。
	 */
	@Nullable
	public Destination getDefaultDestination() {
		return (this.defaultDestination instanceof Destination ? (Destination) this.defaultDestination : null);
	}

	@Nullable
	private Queue getDefaultQueue() {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null && !(defaultDestination instanceof Queue)) {
			throw new IllegalStateException(
					"'defaultDestination' does not correspond to a Queue. Check configuration of JmsTemplate.");
		}
		return (Queue) defaultDestination;
	}

	/**
	 * 设置用于没有目标参数的发送/接收操作的目标名称。
	 * 指定的名称将通过 DestinationResolver 动态解析。
	 * <p>或者，将 JMS Destination 对象指定为 "defaultDestination"。
	 * @see #send(MessageCreator)
	 * @see #convertAndSend(Object)
	 * @see #convertAndSend(Object, MessagePostProcessor)
	 * @see #setDestinationResolver
	 * @see #setDefaultDestination(javax.jms.Destination)
	 */
	public void setDefaultDestinationName(@Nullable String destinationName) {
		this.defaultDestination = destinationName;
	}

	/**
	 * 返回用于没有目标参数的发送/接收操作的目标名称。
	 */
	@Nullable
	public String getDefaultDestinationName() {
		return (this.defaultDestination instanceof String ? (String) this.defaultDestination : null);
	}

	private String getRequiredDefaultDestinationName() throws IllegalStateException {
		String name = getDefaultDestinationName();
		if (name == null) {
			throw new IllegalStateException(
					"No 'defaultDestination' or 'defaultDestinationName' specified. Check configuration of JmsTemplate.");
		}
		return name;
	}

	/**
	 * 设置此模板的消息转换器。用于将 convertAndSend 方法的 Object 参数
	 * 以及 receiveAndConvert 方法的 Object 结果进行解析转换。
	 * <p>默认转换器是 SimpleMessageConverter，能够处理 BytesMessage、
	 * TextMessage 和 ObjectMessage。
	 * @see #convertAndSend
	 * @see #receiveAndConvert
	 * @see org.springframework.jms.support.converter.SimpleMessageConverter
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * 返回此模板的消息转换器。
	 */
	@Nullable
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	private MessageConverter getRequiredMessageConverter() throws IllegalStateException {
		MessageConverter converter = getMessageConverter();
		if (converter == null) {
			throw new IllegalStateException("No 'messageConverter' specified. Check configuration of JmsTemplate.");
		}
		return converter;
	}


	/**
	 * 设置是否启用消息 ID。默认值为 "true"。
	 * <p>这只是对 JMS 生产者的一种提示。
	 * 详情请参阅 JMS javadoc。
	 * @see javax.jms.MessageProducer#setDisableMessageID
	 */
	public void setMessageIdEnabled(boolean messageIdEnabled) {
		this.messageIdEnabled = messageIdEnabled;
	}

	/**
	 * 返回是否启用消息 ID。
	 */
	public boolean isMessageIdEnabled() {
		return this.messageIdEnabled;
	}

	/**
	 * 设置是否启用消息时间戳。默认值为 "true"。
	 * <p>这只是对 JMS 生产者的一种提示。
	 * 详情请参阅 JMS javadoc。
	 * @see javax.jms.MessageProducer#setDisableMessageTimestamp
	 */
	public void setMessageTimestampEnabled(boolean messageTimestampEnabled) {
		this.messageTimestampEnabled = messageTimestampEnabled;
	}

	/**
	 * 返回是否启用消息时间戳。
	 */
	public boolean isMessageTimestampEnabled() {
		return this.messageTimestampEnabled;
	}

	/**
	 * 设置是否阻止投递由其自身连接发布的消息。默认值为 "false"。
	 * @see javax.jms.Session#createConsumer(javax.jms.Destination, String, boolean)
	 */
	public void setPubSubNoLocal(boolean pubSubNoLocal) {
		this.pubSubNoLocal = pubSubNoLocal;
	}

	/**
	 * 返回是否阻止投递由其自身连接发布的消息。
	 */
	public boolean isPubSubNoLocal() {
		return this.pubSubNoLocal;
	}

	/**
	 * 设置接收调用使用的超时时间（以毫秒为单位）。
	 * <p>默认值为 {@link #RECEIVE_TIMEOUT_INDEFINITE_WAIT}，表示无超时的阻塞接收。
	 * <p>指定 {@link #RECEIVE_TIMEOUT_NO_WAIT}（或任何其他负值）表示接收操作应
	 * 检查消息是否立即可用而不阻塞。
	 * @see #receiveFromConsumer(MessageConsumer, long)
	 * @see javax.jms.MessageConsumer#receive(long)
	 * @see javax.jms.MessageConsumer#receiveNoWait()
	 * @see javax.jms.MessageConsumer#receive()
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * 返回接收调用使用的超时时间（以毫秒为单位）。
	 */
	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}

	/**
	 * 设置发送调用使用的投递延迟（以毫秒为单位）。
	 * <p>默认值为 -1（不向代理传递投递延迟）。
	 * 请注意，此功能需要 JMS 2.0。
	 */
	public void setDeliveryDelay(long deliveryDelay) {
		this.deliveryDelay = deliveryDelay;
	}

	/**
	 * 返回发送调用使用的投递延迟（以毫秒为单位）。
	 */
	public long getDeliveryDelay() {
		return this.deliveryDelay;
	}


	/**
	 * 设置发送消息时是否应使用 QOS 值（deliveryMode、priority、timeToLive）。
	 * @see #setDeliveryMode
	 * @see #setPriority
	 * @see #setTimeToLive
	 */
	public void setExplicitQosEnabled(boolean explicitQosEnabled) {
		this.explicitQosEnabled = explicitQosEnabled;
	}

	/**
	 * 如果为 "true"，发送消息时将使用 deliveryMode、priority 和 timeToLive 的值。
	 * 否则，将使用可能通过管理方式设置的默认值。
	 * @return true if overriding default values of QOS parameters
	 * (deliveryMode, priority, and timeToLive)
	 * @see #setDeliveryMode
	 * @see #setPriority
	 * @see #setTimeToLive
	 */
	public boolean isExplicitQosEnabled() {
		return this.explicitQosEnabled;
	}

	/**
	 * 设置发送消息时要使用的 {@link QosSettings}。
	 * @param settings the deliveryMode, priority, and timeToLive settings to use
	 * @since 5.0
	 * @see #setExplicitQosEnabled(boolean)
	 * @see #setDeliveryMode(int)
	 * @see #setPriority(int)
	 * @see #setTimeToLive(long)
	 */
	public void setQosSettings(QosSettings settings) {
		Assert.notNull(settings, "Settings must not be null");
		setExplicitQosEnabled(true);
		setDeliveryMode(settings.getDeliveryMode());
		setPriority(settings.getPriority());
		setTimeToLive(settings.getTimeToLive());
	}

	/**
	 * 设置消息投递应为持久还是非持久，以布尔值（"true" 或 "false"）指定。
	 * 这将相应地将投递模式设置为 "PERSISTENT"（2）或 "NON_PERSISTENT"（1）。
	 * <p>默认值为 "true"，即投递模式 "PERSISTENT"。
	 * @see #setDeliveryMode(int)
	 * @see javax.jms.DeliveryMode#PERSISTENT
	 * @see javax.jms.DeliveryMode#NON_PERSISTENT
	 */
	public void setDeliveryPersistent(boolean deliveryPersistent) {
		this.deliveryMode = (deliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
	}

	/**
	 * 设置发送消息时使用的投递模式。
	 * 默认为 JMS Message 的默认值："PERSISTENT"。
	 * <p>由于默认值可能通过管理方式定义，
	 * 因此仅当 "isExplicitQosEnabled" 等于 "true" 时才会使用此设置。
	 * @param deliveryMode the delivery mode to use
	 * @see #isExplicitQosEnabled
	 * @see javax.jms.DeliveryMode#PERSISTENT
	 * @see javax.jms.DeliveryMode#NON_PERSISTENT
	 * @see javax.jms.Message#DEFAULT_DELIVERY_MODE
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	/**
	 * 返回发送消息时使用的投递模式。
	 */
	public int getDeliveryMode() {
		return this.deliveryMode;
	}

	/**
	 * 设置发送时消息的优先级。
	 * <p>由于默认值可能通过管理方式定义，
	 * 因此仅当 "isExplicitQosEnabled" 等于 "true" 时才会使用此设置。
	 * @see #isExplicitQosEnabled
	 * @see javax.jms.Message#DEFAULT_PRIORITY
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * 返回发送时消息的优先级。
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * 设置发送时消息的生存时间。
	 * <p>由于默认值可能通过管理方式定义，
	 * 因此仅当 "isExplicitQosEnabled" 等于 "true" 时才会使用此设置。
	 * @param timeToLive the message's lifetime (in milliseconds)
	 * @see #isExplicitQosEnabled
	 * @see javax.jms.Message#DEFAULT_TIME_TO_LIVE
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * 返回发送时消息的生存时间。
	 */
	public long getTimeToLive() {
		return this.timeToLive;
	}


	//---------------------------------------------------------------------------------------
	// JmsOperations execute methods
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
	public <T> T execute(SessionCallback<T> action) throws JmsException {
		return execute(action, false);
	}

	/**
	 * 在 JMS Session 中执行由给定操作对象指定的操作。
	 * {@code execute(SessionCallback)} 的通用版本，允许即时启动 JMS Connection。
	 * <p>对于一般情况，请使用 {@code execute(SessionCallback)}。
	 * 仅在接收消息时才需要启动 JMS Connection，这最好通过 {@code receive}
	 * 方法实现。
	 * @param action callback object that exposes the Session
	 * @param startConnection whether to start the Connection
	 * @return the result object from working with the Session
	 * @throws JmsException if there is any problem
	 * @see #execute(SessionCallback)
	 * @see #receive
	 */
	@Nullable
	public <T> T execute(SessionCallback<T> action, boolean startConnection) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		Connection conToClose = null;
		Session sessionToClose = null;
		try {
			Session sessionToUse = ConnectionFactoryUtils.doGetTransactionalSession(
					obtainConnectionFactory(), this.transactionalResourceFactory, startConnection);
			if (sessionToUse == null) {
				conToClose = createConnection();
				sessionToClose = createSession(conToClose);
				if (startConnection) {
					conToClose.start();
				}
				sessionToUse = sessionToClose;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Executing callback on JMS Session: " + sessionToUse);
			}
			return action.doInJms(sessionToUse);
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
		finally {
			JmsUtils.closeSession(sessionToClose);
			ConnectionFactoryUtils.releaseConnection(conToClose, getConnectionFactory(), startConnection);
		}
	}

	@Override
	@Nullable
	public <T> T execute(ProducerCallback<T> action) throws JmsException {
		String defaultDestinationName = getDefaultDestinationName();
		if (defaultDestinationName != null) {
			return execute(defaultDestinationName, action);
		}
		else {
			return execute(getDefaultDestination(), action);
		}
	}

	@Override
	@Nullable
	public <T> T execute(final @Nullable Destination destination, final ProducerCallback<T> action) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		return execute(session -> {
			MessageProducer producer = createProducer(session, destination);
			try {
				return action.doInJms(session, producer);
			}
			finally {
				JmsUtils.closeMessageProducer(producer);
			}
		}, false);
	}

	@Override
	@Nullable
	public <T> T execute(final String destinationName, final ProducerCallback<T> action) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		return execute(session -> {
			Destination destination = resolveDestinationName(session, destinationName);
			MessageProducer producer = createProducer(session, destination);
			try {
				return action.doInJms(session, producer);
			}
			finally {
				JmsUtils.closeMessageProducer(producer);
			}
		}, false);
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages
	//---------------------------------------------------------------------------------------

	@Override
	public void send(MessageCreator messageCreator) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			send(defaultDestination, messageCreator);
		}
		else {
			send(getRequiredDefaultDestinationName(), messageCreator);
		}
	}

	@Override
	public void send(final Destination destination, final MessageCreator messageCreator) throws JmsException {
		execute(session -> {
			doSend(session, destination, messageCreator);
			return null;
		}, false);
	}

	@Override
	public void send(final String destinationName, final MessageCreator messageCreator) throws JmsException {
		execute(session -> {
			Destination destination = resolveDestinationName(session, destinationName);
			doSend(session, destination, messageCreator);
			return null;
		}, false);
	}

	/**
	 * 发送给定的 JMS 消息。
	 * @param session the JMS Session to operate on
	 * @param destination the JMS Destination to send to
	 * @param messageCreator callback to create a JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected void doSend(Session session, Destination destination, MessageCreator messageCreator)
			throws JMSException {

		Assert.notNull(messageCreator, "MessageCreator must not be null");
		MessageProducer producer = createProducer(session, destination);
		try {
			Message message = messageCreator.createMessage(session);
			if (logger.isDebugEnabled()) {
				logger.debug("Sending created message: " + message);
			}
			doSend(producer, message);
			// Check commit - avoid commit call within a JTA transaction.
			if (session.getTransacted() && isSessionLocallyTransacted(session)) {
				// Transacted session created by this template -> commit.
				JmsUtils.commitIfNecessary(session);
			}
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * 实际发送给定的 JMS 消息。
	 * @param producer the JMS MessageProducer to send with
	 * @param message the JMS Message to send
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected void doSend(MessageProducer producer, Message message) throws JMSException {
		if (this.deliveryDelay >= 0) {
			producer.setDeliveryDelay(this.deliveryDelay);
		}
		if (isExplicitQosEnabled()) {
			producer.send(message, getDeliveryMode(), getPriority(), getTimeToLive());
		}
		else {
			producer.send(message);
		}
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending auto-converted messages
	//---------------------------------------------------------------------------------------

	@Override
	public void convertAndSend(Object message) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			convertAndSend(defaultDestination, message);
		}
		else {
			convertAndSend(getRequiredDefaultDestinationName(), message);
		}
	}

	@Override
	public void convertAndSend(Destination destination, final Object message) throws JmsException {
		send(destination, session -> getRequiredMessageConverter().toMessage(message, session));
	}

	@Override
	public void convertAndSend(String destinationName, final Object message) throws JmsException {
		send(destinationName, session -> getRequiredMessageConverter().toMessage(message, session));
	}

	@Override
	public void convertAndSend(Object message, MessagePostProcessor postProcessor) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			convertAndSend(defaultDestination, message, postProcessor);
		}
		else {
			convertAndSend(getRequiredDefaultDestinationName(), message, postProcessor);
		}
	}

	@Override
	public void convertAndSend(
			Destination destination, final Object message, final MessagePostProcessor postProcessor)
			throws JmsException {

		send(destination, session -> {
			Message msg = getRequiredMessageConverter().toMessage(message, session);
			return postProcessor.postProcessMessage(msg);
		});
	}

	@Override
	public void convertAndSend(
			String destinationName, final Object message, final MessagePostProcessor postProcessor)
		throws JmsException {

		send(destinationName, session -> {
			Message msg = getRequiredMessageConverter().toMessage(message, session);
			return postProcessor.postProcessMessage(msg);
		});
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving messages
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
	public Message receive() throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receive(defaultDestination);
		}
		else {
			return receive(getRequiredDefaultDestinationName());
		}
	}

	@Override
	@Nullable
	public Message receive(Destination destination) throws JmsException {
		return receiveSelected(destination, null);
	}

	@Override
	@Nullable
	public Message receive(String destinationName) throws JmsException {
		return receiveSelected(destinationName, null);
	}

	@Override
	@Nullable
	public Message receiveSelected(String messageSelector) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receiveSelected(defaultDestination, messageSelector);
		}
		else {
			return receiveSelected(getRequiredDefaultDestinationName(), messageSelector);
		}
	}

	@Override
	@Nullable
	public Message receiveSelected(final Destination destination, @Nullable final String messageSelector) throws JmsException {
		return execute(session -> doReceive(session, destination, messageSelector), true);
	}

	@Override
	@Nullable
	public Message receiveSelected(final String destinationName, @Nullable final String messageSelector) throws JmsException {
		return execute(session -> {
			Destination destination = resolveDestinationName(session, destinationName);
			return doReceive(session, destination, messageSelector);
		}, true);
	}

	/**
	 * 接收 JMS 消息。
	 * @param session the JMS Session to operate on
	 * @param destination the JMS Destination to receive from
	 * @param messageSelector the message selector for this consumer (can be {@code null})
	 * @return the JMS Message received, or {@code null} if none
	 * @throws JMSException if thrown by JMS API methods
	 */
	@Nullable
	protected Message doReceive(Session session, Destination destination, @Nullable String messageSelector)
			throws JMSException {

		return doReceive(session, createConsumer(session, destination, messageSelector));
	}

	/**
	 * 实际接收 JMS 消息。
	 * @param session the JMS Session to operate on
	 * @param consumer the JMS MessageConsumer to receive with
	 * @return the JMS Message received, or {@code null} if none
	 * @throws JMSException if thrown by JMS API methods
	 */
	@Nullable
	protected Message doReceive(Session session, MessageConsumer consumer) throws JMSException {
		try {
			// Use transaction timeout (if available).
			long timeout = getReceiveTimeout();
			ConnectionFactory connectionFactory = getConnectionFactory();
			JmsResourceHolder resourceHolder = null;
			if (connectionFactory != null) {
				resourceHolder = (JmsResourceHolder) TransactionSynchronizationManager.getResource(connectionFactory);
			}
			if (resourceHolder != null && resourceHolder.hasTimeout()) {
				timeout = Math.min(timeout, resourceHolder.getTimeToLiveInMillis());
			}
			Message message = receiveFromConsumer(consumer, timeout);
			if (session.getTransacted()) {
				// Commit necessary - but avoid commit call within a JTA transaction.
				if (isSessionLocallyTransacted(session)) {
					// Transacted session created by this template -> commit.
					JmsUtils.commitIfNecessary(session);
				}
			}
			else if (isClientAcknowledge(session)) {
				// Manually acknowledge message, if any.
				if (message != null) {
					message.acknowledge();
				}
			}
			return message;
		}
		finally {
			JmsUtils.closeMessageConsumer(consumer);
		}
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving auto-converted messages
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
	public Object receiveAndConvert() throws JmsException {
		return doConvertFromMessage(receive());
	}

	@Override
	@Nullable
	public Object receiveAndConvert(Destination destination) throws JmsException {
		return doConvertFromMessage(receive(destination));
	}

	@Override
	@Nullable
	public Object receiveAndConvert(String destinationName) throws JmsException {
		return doConvertFromMessage(receive(destinationName));
	}

	@Override
	@Nullable
	public Object receiveSelectedAndConvert(String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(messageSelector));
	}

	@Override
	@Nullable
	public Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(destination, messageSelector));
	}

	@Override
	@Nullable
	public Object receiveSelectedAndConvert(String destinationName, String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(destinationName, messageSelector));
	}

	/**
	 * 从给定的 JMS 消息中提取内容。
	 * @param message the JMS Message to convert (can be {@code null})
	 * @return the content of the message, or {@code null} if none
	 */
	@Nullable
	protected Object doConvertFromMessage(@Nullable Message message) {
		if (message != null) {
			try {
				return getRequiredMessageConverter().fromMessage(message);
			}
			catch (JMSException ex) {
				throw convertJmsAccessException(ex);
			}
		}
		return null;
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages to and receiving the reply from a destination
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
	public Message sendAndReceive(MessageCreator messageCreator) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return sendAndReceive(defaultDestination, messageCreator);
		}
		else {
			return sendAndReceive(getRequiredDefaultDestinationName(), messageCreator);
		}
	}

	@Override
	@Nullable
	public Message sendAndReceive(final Destination destination, final MessageCreator messageCreator) throws JmsException {
		return executeLocal(session -> doSendAndReceive(session, destination, messageCreator), true);
	}

	@Override
	@Nullable
	public Message sendAndReceive(final String destinationName, final MessageCreator messageCreator) throws JmsException {
		return executeLocal(session -> {
			Destination destination = resolveDestinationName(session, destinationName);
			return doSendAndReceive(session, destination, messageCreator);
		}, true);
	}

	/**
	 * 向给定的 {@link Destination} 发送请求消息，并阻塞等待在即时创建的
	 * 临时队列上收到回复。
	 * <p>返回响应消息；如果没有任何消息，则返回 {@code null}。
	 * @throws JMSException if thrown by JMS API methods
	 */
	@Nullable
	protected Message doSendAndReceive(Session session, Destination destination, MessageCreator messageCreator)
			throws JMSException {

		Assert.notNull(messageCreator, "MessageCreator must not be null");
		TemporaryQueue responseQueue = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			Message requestMessage = messageCreator.createMessage(session);
			responseQueue = session.createTemporaryQueue();
			producer = session.createProducer(destination);
			consumer = session.createConsumer(responseQueue);
			requestMessage.setJMSReplyTo(responseQueue);
			if (logger.isDebugEnabled()) {
				logger.debug("Sending created message: " + requestMessage);
			}
			doSend(producer, requestMessage);
			return receiveFromConsumer(consumer, getReceiveTimeout());
		}
		finally {
			JmsUtils.closeMessageConsumer(consumer);
			JmsUtils.closeMessageProducer(producer);
			if (responseQueue != null) {
				responseQueue.delete();
			}
		}
	}

	/**
	 * {@link #execute(SessionCallback, boolean)} 的一个变体，显式创建
	 * 非事务性 {@link Session}。给定的 {@link SessionCallback}
	 * 不参与现有事务。
	 */
	@Nullable
	private <T> T executeLocal(SessionCallback<T> action, boolean startConnection) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		Connection con = null;
		Session session = null;
		try {
			con = createConnection();
			session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
			if (startConnection) {
				con.start();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Executing callback on JMS Session: " + session);
			}
			return action.doInJms(session);
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
		finally {
			JmsUtils.closeSession(session);
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory(), startConnection);
		}
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for browsing messages
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
	public <T> T browse(BrowserCallback<T> action) throws JmsException {
		Queue defaultQueue = getDefaultQueue();
		if (defaultQueue != null) {
			return browse(defaultQueue, action);
		}
		else {
			return browse(getRequiredDefaultDestinationName(), action);
		}
	}

	@Override
	@Nullable
	public <T> T browse(Queue queue, BrowserCallback<T> action) throws JmsException {
		return browseSelected(queue, null, action);
	}

	@Override
	@Nullable
	public <T> T browse(String queueName, BrowserCallback<T> action) throws JmsException {
		return browseSelected(queueName, null, action);
	}

	@Override
	@Nullable
	public <T> T browseSelected(String messageSelector, BrowserCallback<T> action) throws JmsException {
		Queue defaultQueue = getDefaultQueue();
		if (defaultQueue != null) {
			return browseSelected(defaultQueue, messageSelector, action);
		}
		else {
			return browseSelected(getRequiredDefaultDestinationName(), messageSelector, action);
		}
	}

	@Override
	@Nullable
	public <T> T browseSelected(final Queue queue, @Nullable final String messageSelector, final BrowserCallback<T> action)
			throws JmsException {

		Assert.notNull(action, "Callback object must not be null");
		return execute(session -> {
			QueueBrowser browser = createBrowser(session, queue, messageSelector);
			try {
				return action.doInJms(session, browser);
			}
			finally {
				JmsUtils.closeQueueBrowser(browser);
			}
		}, true);
	}

	@Override
	@Nullable
	public <T> T browseSelected(final String queueName, @Nullable final String messageSelector, final BrowserCallback<T> action)
			throws JmsException {

		Assert.notNull(action, "Callback object must not be null");
		return execute(session -> {
			Queue queue = (Queue) getDestinationResolver().resolveDestinationName(session, queueName, false);
			QueueBrowser browser = createBrowser(session, queue, messageSelector);
			try {
				return action.doInJms(session, browser);
			}
			finally {
				JmsUtils.closeQueueBrowser(browser);
			}
		}, true);
	}


	/**
	 * 从给定的 JmsResourceHolder 中获取合适的 Connection。
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
	 * 从给定的 JmsResourceHolder 中获取合适的 Session。
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
	 * 检查给定的 Session 是否为本地事务性的，即其事务是否由此监听器容器的
	 * Session 处理逻辑管理，而不是由外部事务协调器管理。
	 * <p>注意：Session 自身的 transacted 标志此前已检查过。
	 * 此方法用于确定 Session 的事务是本地事务还是外部协调的。
	 * @param session the Session to check
	 * @return whether the given Session is locally transacted
	 * @see #isSessionTransacted()
	 * @see org.springframework.jms.connection.ConnectionFactoryUtils#isSessionTransactional
	 */
	protected boolean isSessionLocallyTransacted(Session session) {
		return isSessionTransacted() &&
				!ConnectionFactoryUtils.isSessionTransactional(session, getConnectionFactory());
	}

	/**
	 * 为给定的 Session 和 Destination 创建 JMS MessageProducer，
	 * 必要时将其配置为禁用消息 ID 和/或时间戳。
	 * <p>委托给 {@link #doCreateProducer} 创建原始的 JMS MessageProducer。
	 * @param session the JMS Session to create a MessageProducer for
	 * @param destination the JMS Destination to create a MessageProducer for
	 * @return the new JMS MessageProducer
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageIdEnabled
	 * @see #setMessageTimestampEnabled
	 */
	protected MessageProducer createProducer(Session session, @Nullable Destination destination) throws JMSException {
		MessageProducer producer = doCreateProducer(session, destination);
		if (!isMessageIdEnabled()) {
			producer.setDisableMessageID(true);
		}
		if (!isMessageTimestampEnabled()) {
			producer.setDisableMessageTimestamp(true);
		}
		return producer;
	}

	/**
	 * 为给定的 Session 和 Destination 创建原始的 JMS MessageProducer。
	 * <p>此实现使用 JMS 1.1 API。
	 * @param session the JMS Session to create a MessageProducer for
	 * @param destination the JMS Destination to create a MessageProducer for
	 * @return the new JMS MessageProducer
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected MessageProducer doCreateProducer(Session session, @Nullable Destination destination) throws JMSException {
		return session.createProducer(destination);
	}

	/**
	 * 为给定的 Session 和 Destination 创建 JMS MessageConsumer。
	 * <p>此实现使用 JMS 1.1 API。
	 * @param session the JMS Session to create a MessageConsumer for
	 * @param destination the JMS Destination to create a MessageConsumer for
	 * @param messageSelector the message selector for this consumer (can be {@code null})
	 * @return the new JMS MessageConsumer
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected MessageConsumer createConsumer(Session session, Destination destination, @Nullable String messageSelector)
			throws JMSException {

		// Only pass in the NoLocal flag in case of a Topic:
		// Some JMS providers, such as WebSphere MQ 6.0, throw IllegalStateException
		// in case of the NoLocal flag being specified for a Queue.
		if (isPubSubDomain()) {
			return session.createConsumer(destination, messageSelector, isPubSubNoLocal());
		}
		else {
			return session.createConsumer(destination, messageSelector);
		}
	}

	/**
	 * 为给定的 Session 和 Queue 创建 JMS QueueBrowser。
	 * @param session the JMS Session to create a QueueBrowser for
	 * @param queue the JMS Queue to create a QueueBrowser for
	 * @param messageSelector the message selector for this consumer (can be {@code null})
	 * @return the new JMS QueueBrowser
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageIdEnabled
	 * @see #setMessageTimestampEnabled
	 */
	protected QueueBrowser createBrowser(Session session, Queue queue, @Nullable String messageSelector)
			throws JMSException {

		return session.createBrowser(queue, messageSelector);
	}


	/**
	 * 委托给此模板受保护回调方法的 ResourceFactory 实现。
	 */
	private class JmsTemplateResourceFactory implements ConnectionFactoryUtils.ResourceFactory {

		@Override
		@Nullable
		public Connection getConnection(JmsResourceHolder holder) {
			return JmsTemplate.this.getConnection(holder);
		}

		@Override
		@Nullable
		public Session getSession(JmsResourceHolder holder) {
			return JmsTemplate.this.getSession(holder);
		}

		@Override
		public Connection createConnection() throws JMSException {
			return JmsTemplate.this.createConnection();
		}

		@Override
		public Session createSession(Connection con) throws JMSException {
			return JmsTemplate.this.createSession(con);
		}

		@Override
		public boolean isSynchedLocalTransactionAllowed() {
			return JmsTemplate.this.isSessionTransacted();
		}
	}

}
