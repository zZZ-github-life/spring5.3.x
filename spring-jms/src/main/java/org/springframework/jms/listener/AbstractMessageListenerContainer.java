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

package org.springframework.jms.listener;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

/**
 * Spring 消息监听器容器实现的抽象基类。
 * 它既可以承载标准的 JMS {@link javax.jms.MessageListener}，
 * 也可以承载 Spring 的 {@link SessionAwareMessageListener} 来进行实际的消息处理。
 *
 * <p>通常持有一个所有监听器都应注册其上的单一 JMS {@link Connection}，
 * 这是管理监听器会话的标准 JMS 方式。它也可以选择为每个监听器使用新的 Connection，
 * 以支持 Java EE 风格的 XA 感知 JMS 消息传递。实际的注册过程由具体的子类负责。
 *
 * <p><b>注意：</b>此消息监听器容器的默认行为是<b>绝不</b>将消息监听器抛出的异常
 * 传播给 JMS 提供者。相反，它会以 error 级别记录任何此类异常。
 * 这意味着从相关 JMS 提供者的角度来看，这样的监听器永远不会失败。然而，
 * 如果需要错误处理，可以向 {@link #setErrorHandler(ErrorHandler)} 方法提供
 * 任何 {@link ErrorHandler} 策略实现。请注意，如果提供了 {@link ExceptionListener}，
 * JMSException <b>会</b>在传递给 {@link ExceptionListener} 之后（但之后）再传递给 ErrorHandler。
 *
 * <p>监听器容器提供以下消息确认选项：
 * <ul>
 * <li>"sessionAcknowledgeMode" 设置为 "AUTO_ACKNOWLEDGE"（默认）：
 * 此模式取决于具体容器：对于 {@link DefaultMessageListenerContainer}，
 * 它意味着在监听器执行<i>之前</i>进行自动消息确认，发生异常时不会重新投递，
 * 其他监听器执行中断情况下也不会重新投递。对于 {@link SimpleMessageListenerContainer}，
 * 它意味着在监听器执行<i>之后</i>进行自动消息确认，抛出用户异常时不会重新投递，
 * 但如果 JVM 在监听器执行期间终止则可能重新投递。为了在任意容器变体上一致地安排
 * 重新投递，请考虑使用 "CLIENT_ACKNOWLEDGE" 模式，或者（更好的做法）将
 * "sessionTransacted" 设置为 "true"。
 * <li>"sessionAcknowledgeMode" 设置为 "DUPS_OK_ACKNOWLEDGE"：
 * 在监听器执行<i>期间</i>（{@link DefaultMessageListenerContainer}）或
 * 稍后（{@link SimpleMessageListenerContainer}）进行<i>惰性</i>消息确认；
 * 抛出用户异常时不会重新投递，但如果 JVM 在监听器执行期间终止则可能重新投递。
 * 为了在任意容器变体上一致地安排重新投递，请考虑使用 "CLIENT_ACKNOWLEDGE" 模式，
 * 或者（更好的做法）将 "sessionTransacted" 设置为 "true"。
 * <li>"sessionAcknowledgeMode" 设置为 "CLIENT_ACKNOWLEDGE"：
 * 在监听器成功执行<i>之后</i>进行自动消息确认；抛出用户异常时以及其他监听器执行中断
 * （例如 JVM 终止）情况下都会尽力重新投递。
 * <li>"sessionTransacted" 设置为 "true"：
 * 在监听器成功执行后进行事务性确认；抛出用户异常时以及其他监听器执行中断
 * （例如 JVM 终止）情况下都会<i>保证重新投递</i>。
 * </ul>
 *
 * <p>重复消息处理问题有两种解决方案：
 * <ul>
 * <li>要么在监听器中添加<i>重复消息检测</i>，形式可以是业务实体存在性检查
 * 或协议表检查。这通常只需要在传入消息上设置了 JMSRedelivered 标志时进行
 * （否则直接正常处理即可）。请注意，当 "sessionTransacted" 设置为 "true" 时，
 * 重复消息只会在 JVM 恰好在最不幸的时刻终止时出现（即在业务逻辑已执行、
 * 但 JMS 部分尚未提交之前），因此重复消息检测只是用来覆盖这种边缘情况。
 * <li>或者<i>用 XA 事务包装整个处理过程</i>，同时涵盖 JMS 消息的接收
 * 以及消息监听器中业务逻辑的执行（包括数据库操作等）。只有
 * {@link DefaultMessageListenerContainer} 支持这种方式，方法是指定外部
 * "transactionManager"（通常是 {@link org.springframework.transaction.jta.JtaTransactionManager}，
 * 并传入相应的 XA 感知的 JMS {@link javax.jms.ConnectionFactory} 作为
 * "connectionFactory"）。
 * </ul>
 * 请注意，XA 事务协调会带来显著的运行时开销，因此除非绝对必要，
 * 避免使用它可能是可行的。
 *
 * <p><b>建议：</b>
 * <ul>
 * <li>一般建议将 "sessionTransacted" 设置为 "true"，通常与由监听器实现
 * 通过 Spring 标准事务设施触发的本地数据库事务结合使用。
 * 这在 Tomcat 或独立环境中工作良好，通常结合自定义的重复消息检测
 * （如果绝不能处理同一条消息两次）。
 * <li>或者，为完全 XA 感知的 JMS 提供者指定
 * {@link org.springframework.transaction.jta.JtaTransactionManager} 作为
 * "transactionManager"——通常是在 Java EE 服务器上运行时，但也适用于存在
 * JTA 事务管理器的其他环境。这样可以在没有自定义重复消息检查的情况下
 * 获得完整的“恰好一次”保证，代价是额外的运行时处理开销。
 * </ul>
 *
 * <p>请注意，只要不需要在外部管理事务，强烈建议使用 "sessionTransacted" 标志，
 * 而不是 {@link org.springframework.jms.connection.JmsTransactionManager}。
 * 因此，只有在使用 JTA 或需要与自定义外部事务安排同步时才设置事务管理器。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0
 * @see #setMessageListener
 * @see javax.jms.MessageListener
 * @see SessionAwareMessageListener
 * @see #handleListenerException
 * @see DefaultMessageListenerContainer
 * @see SimpleMessageListenerContainer
 * @see org.springframework.jms.listener.endpoint.JmsMessageEndpointManager
 */
public abstract class AbstractMessageListenerContainer extends AbstractJmsListeningContainer
		implements MessageListenerContainer {

	@Nullable
	private volatile Object destination;

	@Nullable
	private volatile String messageSelector;

	@Nullable
	private volatile Object messageListener;

	private boolean subscriptionDurable = false;

	private boolean subscriptionShared = false;

	@Nullable
	private String subscriptionName;

	@Nullable
	private Boolean replyPubSubDomain;

	@Nullable
	private QosSettings replyQosSettings;

	private boolean pubSubNoLocal = false;

	@Nullable
	private MessageConverter messageConverter;

	@Nullable
	private ExceptionListener exceptionListener;

	@Nullable
	private ErrorHandler errorHandler;

	private boolean exposeListenerSession = true;

	private boolean acceptMessagesWhileStopping = false;


	/**
	 * 指定并发限制。
	 */
	public abstract void setConcurrency(String concurrency);

	/**
	 * 设置要从中接收消息的目的地。
	 * <p>或者，指定 "destinationName"，以便通过
	 * {@link org.springframework.jms.support.destination.DestinationResolver} 动态解析。
	 * <p>注意：目的地可以在运行时替换，监听器容器会立即采用新的目的地
	 * （例如 DefaultMessageListenerContainer 可以做到这一点，只要缓存级别低于
	 * CACHE_CONSUMER）。不过，这属于高级用法；请谨慎使用！
	 * @see #setDestinationName(String)
	 */
	public void setDestination(@Nullable Destination destination) {
		this.destination = destination;
		if (destination instanceof Topic && !(destination instanceof Queue)) {
			// Clearly a Topic: let's set the "pubSubDomain" flag accordingly.
			setPubSubDomain(true);
		}
	}

	/**
	 * 返回要从中接收消息的目的地。如果配置的目的地不是实际的 {@link Destination}
	 * 类型，则返回 {@code null}；当目的地是 String 时，请参见
	 * {@link #setDestinationName(String)}。
	 */
	@Nullable
	public Destination getDestination() {
		return (this.destination instanceof Destination ? (Destination) this.destination : null);
	}

	/**
	 * 设置要从中接收消息的目的地的名称。
	 * <p>指定的名称将通过配置的 {@link #setDestinationResolver destination resolver}
	 * 动态解析。
	 * <p>或者，将 JMS {@link Destination} 对象指定为 "destination"。
	 * <p>注意：目的地可以在运行时替换，监听器容器会立即采用新的目的地
	 * （例如 DefaultMessageListenerContainer 可以做到这一点，只要缓存级别低于
	 * CACHE_CONSUMER）。不过，这属于高级用法；请谨慎使用！
	 * @see #setDestination(javax.jms.Destination)
	 */
	public void setDestinationName(@Nullable String destinationName) {
		this.destination = destinationName;
	}

	/**
	 * 返回要从中接收消息的目的地的名称。
	 * 如果配置的目的地不是 {@link String} 类型，则返回 {@code null}；
	 * 当它是实际的 Destination 时，请参见 {@link #setDestination(Destination)}。
	 */
	@Nullable
	public String getDestinationName() {
		return (this.destination instanceof String ? (String) this.destination : null);
	}

	/**
	 * 返回此容器的 JMS 目的地的描述性字符串（永不为 {@code null}）。
	 */
	protected String getDestinationDescription() {
		Object destination = this.destination;
		return (destination != null ? destination.toString() : "");
	}

	/**
	 * 设置 JMS 消息选择器表达式（如果没有则为 {@code null}）。默认为无。
	 * <p>关于选择器表达式的详细定义，请参阅 JMS 规范。
	 * <p>注意：消息选择器可以在运行时替换，监听器容器会立即采用新的选择器值
	 * （例如 DefaultMessageListenerContainer 可以做到这一点，只要缓存级别低于
	 * CACHE_CONSUMER）。不过，这属于高级用法；请谨慎使用！
	 */
	public void setMessageSelector(@Nullable String messageSelector) {
		this.messageSelector = messageSelector;
	}

	/**
	 * 返回 JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 */
	@Nullable
	public String getMessageSelector() {
		return this.messageSelector;
	}


	/**
	 * 设置要注册的消息监听器实现。
	 * 这既可以是标准的 JMS {@link MessageListener} 对象，
	 * 也可以是 Spring 的 {@link SessionAwareMessageListener} 对象。
	 * <p>注意：消息监听器可以在运行时替换，监听器容器会立即采用新的监听器对象
	 * （例如 DefaultMessageListenerContainer 可以做到这一点，只要缓存级别低于
	 * CACHE_CONSUMER）。不过，这属于高级用法；请谨慎使用！
	 * @throws IllegalArgumentException if the supplied listener is not a
	 * {@link MessageListener} or a {@link SessionAwareMessageListener}
	 * @see javax.jms.MessageListener
	 * @see SessionAwareMessageListener
	 */
	public void setMessageListener(@Nullable Object messageListener) {
		checkMessageListener(messageListener);
		this.messageListener = messageListener;
		if (messageListener != null && this.subscriptionName == null) {
			this.subscriptionName = getDefaultSubscriptionName(messageListener);
		}
	}

	/**
	 * 返回要注册的消息监听器对象。
	 */
	@Nullable
	public Object getMessageListener() {
		return this.messageListener;
	}

	/**
	 * 检查给定的消息监听器，如果它不符合受支持的监听器类型则抛出异常。
	 * <p>默认情况下，只接受标准的 JMS {@link MessageListener} 对象
	 * 或 Spring 的 {@link SessionAwareMessageListener} 对象。
	 * @param messageListener the message listener object to check
	 * @throws IllegalArgumentException if the supplied listener is not a
	 * {@link MessageListener} or a {@link SessionAwareMessageListener}
	 * @see javax.jms.MessageListener
	 * @see SessionAwareMessageListener
	 */
	protected void checkMessageListener(@Nullable Object messageListener) {
		if (messageListener != null && !(messageListener instanceof MessageListener ||
				messageListener instanceof SessionAwareMessageListener)) {
			throw new IllegalArgumentException(
					"Message listener needs to be of type [" + MessageListener.class.getName() +
					"] or [" + SessionAwareMessageListener.class.getName() + "]");
		}
	}

	/**
	 * 确定给定消息监听器的默认订阅名称。
	 * @param messageListener the message listener object to check
	 * @return the default subscription name
	 * @see SubscriptionNameProvider
	 */
	protected String getDefaultSubscriptionName(Object messageListener) {
		if (messageListener instanceof SubscriptionNameProvider) {
			return ((SubscriptionNameProvider) messageListener).getSubscriptionName();
		}
		else {
			return messageListener.getClass().getName();
		}
	}

	/**
	 * 设置是否使订阅持久化。要使用的持久订阅名称可以通过
	 * "subscriptionName" 属性指定。
	 * <p>默认值为 "false"。将其设置为 "true" 可注册持久订阅，
	 * 通常与 "subscriptionName" 值结合使用（除非你的消息监听器类名
	 * 足以作为订阅名称）。
	 * <p>仅在监听主题（发布-订阅域）时才有意义，
	 * 因此此方法也会切换 "pubSubDomain" 标志。
	 * @see #setSubscriptionName
	 * @see #setPubSubDomain
	 */
	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
		if (subscriptionDurable) {
			setPubSubDomain(true);
		}
	}

	/**
	 * 返回是否使订阅持久化。
	 */
	public boolean isSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	/**
	 * 设置是否使订阅共享。要使用的共享订阅名称可以通过
	 * "subscriptionName" 属性指定。
	 * <p>默认值为 "false"。将其设置为 "true" 可注册共享订阅，
	 * 通常与 "subscriptionName" 值结合使用（除非你的消息监听器类名
	 * 足以作为订阅名称）。请注意，共享订阅也可以是持久化的，
	 * 因此此标志可以（而且经常会）与 "subscriptionDurable" 一起使用。
	 * <p>仅在监听主题（发布-订阅域）时才有意义，
	 * 因此此方法也会切换 "pubSubDomain" 标志。
	 * <p><b>需要兼容 JMS 2.0 的消息代理。</b>
	 * @since 4.1
	 * @see #setSubscriptionName
	 * @see #setSubscriptionDurable
	 * @see #setPubSubDomain
	 */
	public void setSubscriptionShared(boolean subscriptionShared) {
		this.subscriptionShared = subscriptionShared;
		if (subscriptionShared) {
			setPubSubDomain(true);
		}
	}

	/**
	 * 返回是否使订阅共享。
	 * @since 4.1
	 */
	public boolean isSubscriptionShared() {
		return this.subscriptionShared;
	}

	/**
	 * 设置要创建的订阅名称。适用于带共享订阅或持久订阅的主题
	 * （发布-订阅域）场景。
	 * <p>订阅名称在此客户端的 JMS 客户端 ID 内必须唯一。
	 * 默认值为指定消息监听器的类名。
	 * <p>注意：每个订阅只允许 1 个并发消费者（这也是此消息监听器容器的默认值），
	 * 共享订阅除外（共享订阅需要 JMS 2.0）。
	 * @since 4.1
	 * @see #setPubSubDomain
	 * @see #setSubscriptionDurable
	 * @see #setSubscriptionShared
	 * @see #setClientId
	 * @see #setMessageListener
	 */
	public void setSubscriptionName(@Nullable String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	/**
	 * 返回要创建的订阅名称（如果有）。
	 * @since 4.1
	 */
	@Nullable
	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	/**
	 * 设置要创建的持久订阅名称。此方法会切换到发布-订阅域模式，
	 * 并同时激活订阅持久化。
	 * <p>持久订阅名称在此客户端的 JMS 客户端 ID 内必须唯一。
	 * 默认值为指定消息监听器的类名。
	 * <p>注意：每个持久订阅只允许 1 个并发消费者（这也是此消息监听器容器的默认值），
	 * 共享持久订阅除外（共享持久订阅需要 JMS 2.0）。
	 * @see #setPubSubDomain
	 * @see #setSubscriptionDurable
	 * @see #setSubscriptionShared
	 * @see #setClientId
	 * @see #setMessageListener
	 */
	public void setDurableSubscriptionName(@Nullable String durableSubscriptionName) {
		this.subscriptionName = durableSubscriptionName;
		this.subscriptionDurable = (durableSubscriptionName != null);
	}

	/**
	 * 返回要创建的持久订阅名称（如果有）。
	 */
	@Nullable
	public String getDurableSubscriptionName() {
		return (this.subscriptionDurable ? this.subscriptionName : null);
	}

	/**
	 * 设置是否阻止投递由其自身连接发布的消息。默认值为 "false"。
	 * @since 4.1
	 * @see javax.jms.Session#createConsumer(javax.jms.Destination, String, boolean)
	 */
	public void setPubSubNoLocal(boolean pubSubNoLocal) {
		this.pubSubNoLocal = pubSubNoLocal;
	}

	/**
	 * 返回是否阻止投递由其自身连接发布的消息。
	 * @since 4.1
	 */
	public boolean isPubSubNoLocal() {
		return this.pubSubNoLocal;
	}

	/**
	 * 配置回复目的地类型。默认情况下，使用配置的 {@code pubSubDomain} 值
	 * （参见 {@link #isPubSubDomain()}）。
	 * <p>如果启用了动态目的地，此设置主要指示要解析的目的地类型。
	 * @param replyPubSubDomain "true" for the Publish/Subscribe domain ({@link Topic Topics}),
	 * "false" for the Point-to-Point domain ({@link Queue Queues})
	 * @since 4.2
	 * @see #setDestinationResolver
	 */
	public void setReplyPubSubDomain(boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
	}

	/**
	 * 返回回复是否使用发布-订阅域（{@link javax.jms.Topic Topic}）。
	 * 否则使用点对点域（{@link javax.jms.Queue Queue}）。
	 * @since 4.2
	 */
	@Override
	public boolean isReplyPubSubDomain() {
		if (this.replyPubSubDomain != null) {
			return this.replyPubSubDomain;
		}
		else {
			return isPubSubDomain();
		}
	}

	/**
	 * 配置发送回复时要使用的 {@link QosSettings}。可以设置为 {@code null}，
	 * 表示应使用代理的默认值。
	 * @param replyQosSettings the QoS settings to use when sending a reply or {@code null}
	 * to use the default vas.
	 * @since 5.0
	 */
	public void setReplyQosSettings(@Nullable QosSettings replyQosSettings) {
		this.replyQosSettings = replyQosSettings;
	}

	@Override
	@Nullable
	public QosSettings getReplyQosSettings() {
		return this.replyQosSettings;
	}

	/**
	 * 设置用于转换 JMS 消息的 {@link MessageConverter} 策略。
	 * @since 4.1
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	@Override
	@Nullable
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * 设置要通知的 JMS ExceptionListener，用于在已注册的消息监听器
	 * 或调用基础设施抛出 JMSException 时进行通知。
	 */
	public void setExceptionListener(@Nullable ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	/**
	 * 返回要通知的 JMS ExceptionListener（如果有），用于在已注册的消息监听器
	 * 或调用基础设施抛出 JMSException 时进行通知。
	 */
	@Nullable
	public ExceptionListener getExceptionListener() {
		return this.exceptionListener;
	}

	/**
	 * 设置要在处理 Message 期间抛出任何未捕获异常时调用的 ErrorHandler。
	 * <p>默认情况下<b>没有</b> ErrorHandler，因此唯一的处理结果就是 error 级别的日志记录。
	 */
	public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 返回要在处理 Message 期间抛出任何未捕获异常时调用的 ErrorHandler。
	 * @since 4.1
	 */
	@Nullable
	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * 设置是否向已注册的 {@link SessionAwareMessageListener} 以及
	 * {@link org.springframework.jms.core.JmsTemplate} 调用暴露监听器 JMS Session。
	 * <p>默认值为 "true"，即复用监听器的 {@link Session}。
	 * 关闭此选项可以改为暴露从同一底层 JMS {@link Connection} 获取的新 JMS Session，
	 * 这在某些 JMS 提供者上可能是必需的。
	 * <p>请注意，由外部事务管理器管理的 Session 始终会暴露给
	 * {@link org.springframework.jms.core.JmsTemplate} 调用。
	 * 因此就 JmsTemplate 暴露而言，此设置只影响本地事务的 Session。
	 * @see SessionAwareMessageListener
	 */
	public void setExposeListenerSession(boolean exposeListenerSession) {
		this.exposeListenerSession = exposeListenerSession;
	}

	/**
	 * 返回是否向已注册的 {@link SessionAwareMessageListener}
	 * 暴露监听器 JMS {@link Session}。
	 */
	public boolean isExposeListenerSession() {
		return this.exposeListenerSession;
	}

	/**
	 * 设置是否在监听器容器停止的过程中继续接受已收到的消息。
	 * <p>默认值为 "false"，即通过中止接收尝试来拒绝此类消息。
	 * 启用此标志后，即使在停止阶段也会完整处理此类消息，
	 * 缺点是甚至新发送的消息也可能仍会被处理（如果在所有接收超时到期之前到达）。
	 * <p><b>注意：</b>中止对这些传入消息的接收尝试可能会导致提供者对
	 * 受影响消息的重试计数递减。如果你有大量的并发消费者，
	 * 请确保重试次数高于消费者数量，以便安全应对所有可能的停止场景。
	 */
	public void setAcceptMessagesWhileStopping(boolean acceptMessagesWhileStopping) {
		this.acceptMessagesWhileStopping = acceptMessagesWhileStopping;
	}

	/**
	 * 返回是否在监听器容器停止的过程中继续接受已收到的消息。
	 */
	public boolean isAcceptMessagesWhileStopping() {
		return this.acceptMessagesWhileStopping;
	}

	@Override
	protected void validateConfiguration() {
		if (this.destination == null) {
			throw new IllegalArgumentException("Property 'destination' or 'destinationName' is required");
		}
	}

	@Override
	public void setupMessageListener(Object messageListener) {
		setMessageListener(messageListener);
	}


	//-------------------------------------------------------------------------
	// Template methods for listener execution
	//-------------------------------------------------------------------------

	/**
	 * 执行指定的监听器，之后（如有必要）提交或回滚事务。
	 * @param session the JMS Session to operate on
	 * @param message the received JMS Message
	 * @see #invokeListener
	 * @see #commitIfNecessary
	 * @see #rollbackOnExceptionIfNecessary
	 * @see #handleListenerException
	 */
	protected void executeListener(Session session, Message message) {
		try {
			doExecuteListener(session, message);
		}
		catch (Throwable ex) {
			handleListenerException(ex);
		}
	}

	/**
	 * 执行指定的监听器，之后（如有必要）提交或回滚事务。
	 * @param session the JMS Session to operate on
	 * @param message the received JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 * @see #invokeListener
	 * @see #commitIfNecessary
	 * @see #rollbackOnExceptionIfNecessary
	 * @see #convertJmsAccessException
	 */
	protected void doExecuteListener(Session session, Message message) throws JMSException {
		if (!isAcceptMessagesWhileStopping() && !isRunning()) {
			if (logger.isWarnEnabled()) {
				logger.warn("Rejecting received message because of the listener container " +
						"having been stopped in the meantime: " + message);
			}
			rollbackIfNecessary(session);
			throw new MessageRejectedWhileStoppingException();
		}

		try {
			invokeListener(session, message);
		}
		catch (JMSException | RuntimeException | Error ex) {
			rollbackOnExceptionIfNecessary(session, ex);
			throw ex;
		}
		commitIfNecessary(session, message);
	}

	/**
	 * 调用指定的监听器：可以作为标准的 JMS MessageListener，
	 * 也可以（更可取地）作为 Spring 的 SessionAwareMessageListener。
	 * @param session the JMS Session to operate on
	 * @param message the received JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageListener
	 */
	@SuppressWarnings("rawtypes")
	protected void invokeListener(Session session, Message message) throws JMSException {
		Object listener = getMessageListener();

		if (listener instanceof SessionAwareMessageListener) {
			doInvokeListener((SessionAwareMessageListener) listener, session, message);
		}
		else if (listener instanceof MessageListener) {
			doInvokeListener((MessageListener) listener, message);
		}
		else if (listener != null) {
			throw new IllegalArgumentException(
					"Only MessageListener and SessionAwareMessageListener supported: " + listener);
		}
		else {
			throw new IllegalStateException("No message listener specified - see property 'messageListener'");
		}
	}

	/**
	 * 将指定的监听器作为 Spring 的 SessionAwareMessageListener 调用，
	 * 并在监听器需要时向其暴露一个新的 JMS Session（可能带有自己的事务）。
	 * @param listener the Spring SessionAwareMessageListener to invoke
	 * @param session the JMS Session to operate on
	 * @param message the received JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 * @see SessionAwareMessageListener
	 * @see #setExposeListenerSession
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void doInvokeListener(SessionAwareMessageListener listener, Session session, Message message)
			throws JMSException {

		Connection conToClose = null;
		Session sessionToClose = null;
		try {
			Session sessionToUse = session;
			if (!isExposeListenerSession()) {
				// We need to expose a separate Session.
				conToClose = createConnection();
				sessionToClose = createSession(conToClose);
				sessionToUse = sessionToClose;
			}
			// Actually invoke the message listener...
			listener.onMessage(message, sessionToUse);
			// Clean up specially exposed Session, if any.
			if (sessionToUse != session) {
				if (sessionToUse.getTransacted() && isSessionLocallyTransacted(sessionToUse)) {
					// Transacted session created by this container -> commit.
					JmsUtils.commitIfNecessary(sessionToUse);
				}
			}
		}
		finally {
			JmsUtils.closeSession(sessionToClose);
			JmsUtils.closeConnection(conToClose);
		}
	}

	/**
	 * 将指定的监听器作为标准的 JMS MessageListener 调用。
	 * <p>默认实现只是简单地调用 {@code onMessage} 方法。
	 * @param listener the JMS MessageListener to invoke
	 * @param message the received JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 * @see javax.jms.MessageListener#onMessage
	 */
	protected void doInvokeListener(MessageListener listener, Message message) throws JMSException {
		listener.onMessage(message);
	}

	/**
	 * 视情况执行提交或消息确认。
	 * @param session the JMS Session to commit
	 * @param message the Message to acknowledge
	 * @throws javax.jms.JMSException in case of commit failure
	 */
	protected void commitIfNecessary(Session session, @Nullable Message message) throws JMSException {
		// Commit session or acknowledge message.
		if (session.getTransacted()) {
			// Commit necessary - but avoid commit call within a JTA transaction.
			if (isSessionLocallyTransacted(session)) {
				// Transacted session created by this container -> commit.
				JmsUtils.commitIfNecessary(session);
			}
		}
		else if (message != null && isClientAcknowledge(session)) {
			message.acknowledge();
		}
	}

	/**
	 * 视情况执行回滚。
	 * @param session the JMS Session to rollback
	 * @throws javax.jms.JMSException in case of a rollback error
	 */
	protected void rollbackIfNecessary(Session session) throws JMSException {
		if (session.getTransacted()) {
			if (isSessionLocallyTransacted(session)) {
				// Transacted session created by this container -> rollback.
				JmsUtils.rollbackIfNecessary(session);
			}
		}
		else if (isClientAcknowledge(session)) {
			session.recover();
		}
	}

	/**
	 * 执行回滚，并正确处理回滚异常。
	 * @param session the JMS Session to rollback
	 * @param ex the thrown application exception or error
	 * @throws javax.jms.JMSException in case of a rollback error
	 */
	protected void rollbackOnExceptionIfNecessary(Session session, Throwable ex) throws JMSException {
		try {
			if (session.getTransacted()) {
				if (isSessionLocallyTransacted(session)) {
					// Transacted session created by this container -> rollback.
					if (logger.isDebugEnabled()) {
						logger.debug("Initiating transaction rollback on application exception", ex);
					}
					JmsUtils.rollbackIfNecessary(session);
				}
			}
			else if (isClientAcknowledge(session)) {
				session.recover();
			}
		}
		catch (IllegalStateException ex2) {
			logger.debug("Could not roll back because Session already closed", ex2);
		}
		catch (JMSException | RuntimeException | Error ex2) {
			logger.error("Application exception overridden by rollback error", ex);
			throw ex2;
		}
	}

	/**
	 * 检查给定的 Session 是否为本地事务的，也就是说，它的事务是否由此监听器容器的
	 * Session 处理机制管理，而不是由外部事务协调器管理。
	 * <p>注意：Session 自身的 transacted 标志此前已经检查过。此方法用于判断
	 * Session 的事务是本地事务还是外部协调的事务。
	 * @param session the Session to check
	 * @return whether the given Session is locally transacted
	 * @see #isSessionTransacted()
	 * @see org.springframework.jms.connection.ConnectionFactoryUtils#isSessionTransactional
	 */
	protected boolean isSessionLocallyTransacted(Session session) {
		return isSessionTransacted();
	}

	/**
	 * 为给定的 Session 和 Destination 创建 JMS MessageConsumer。
	 * <p>此实现使用 JMS 1.1 API。
	 * @param session the JMS Session to create a MessageConsumer for
	 * @param destination the JMS Destination to create a MessageConsumer for
	 * @return the new JMS MessageConsumer
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	protected MessageConsumer createConsumer(Session session, Destination destination) throws JMSException {
		if (isPubSubDomain() && destination instanceof Topic) {
			if (isSubscriptionShared()) {
				return (isSubscriptionDurable() ?
						session.createSharedDurableConsumer((Topic) destination, getSubscriptionName(), getMessageSelector()) :
						session.createSharedConsumer((Topic) destination, getSubscriptionName(), getMessageSelector()));
			}
			else if (isSubscriptionDurable()) {
				return session.createDurableSubscriber(
						(Topic) destination, getSubscriptionName(), getMessageSelector(), isPubSubNoLocal());
			}
			else {
				// Only pass in the NoLocal flag in case of a Topic (pub-sub mode):
				// Some JMS providers, such as WebSphere MQ 6.0, throw IllegalStateException
				// in case of the NoLocal flag being specified for a Queue.
				return session.createConsumer(destination, getMessageSelector(), isPubSubNoLocal());
			}
		}
		else {
			return session.createConsumer(destination, getMessageSelector());
		}
	}

	/**
	 * 处理监听器执行期间出现的给定异常。
	 * <p>默认实现以 warn 级别记录该异常，而不将其传播给 JMS 提供者
	 * ——假定确认和/或事务的全部处理都由本监听器容器完成。
	 * 可以在子类中重写。
	 * @param ex the exception to handle
	 */
	protected void handleListenerException(Throwable ex) {
		if (ex instanceof MessageRejectedWhileStoppingException) {
			// Internal exception - has been handled before.
			return;
		}
		if (ex instanceof JMSException) {
			invokeExceptionListener((JMSException) ex);
		}
		if (isActive()) {
			// Regular case: failed while active.
			// Invoke ErrorHandler if available.
			invokeErrorHandler(ex);
		}
		else {
			// Rare case: listener thread failed after container shutdown.
			// Log at debug level, to avoid spamming the shutdown log.
			logger.debug("Listener exception after container shutdown", ex);
		}
	}

	/**
	 * 调用已注册的 JMS ExceptionListener（如果有）。
	 * @param ex the exception that arose during JMS processing
	 * @see #setExceptionListener
	 */
	protected void invokeExceptionListener(JMSException ex) {
		ExceptionListener exceptionListener = getExceptionListener();
		if (exceptionListener != null) {
			exceptionListener.onException(ex);
		}
	}

	/**
	 * 调用已注册的 ErrorHandler（如果有）。否则以 warn 级别记录日志。
	 * @param ex the uncaught error that arose during JMS processing.
	 * @see #setErrorHandler
	 */
	protected void invokeErrorHandler(Throwable ex) {
		ErrorHandler errorHandler = getErrorHandler();
		if (errorHandler != null) {
			errorHandler.handleError(ex);
		}
		else {
			logger.warn("Execution of JMS message listener failed, and no ErrorHandler has been set.", ex);
		}
	}


	/**
	 * 内部异常类，表示在关闭时拒绝了一条消息。
	 * 用于在此情况下触发外部事务管理器的回滚。
	 */
	@SuppressWarnings("serial")
	private static class MessageRejectedWhileStoppingException extends RuntimeException {
	}

}
