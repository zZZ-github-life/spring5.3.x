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

package org.springframework.messaging.simp.broker;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 将消息代理给已注册订阅者的 {@link MessageHandler} 抽象基类。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractBrokerMessageHandler
		implements MessageHandler, ApplicationEventPublisherAware, SmartLifecycle {

	protected final Log logger = SimpLogging.forLogName(getClass());

	private final SubscribableChannel clientInboundChannel;

	private final MessageChannel clientOutboundChannel;

	private final SubscribableChannel brokerChannel;

	private final Collection<String> destinationPrefixes;

	@Nullable
	private Predicate<String> userDestinationPredicate;

	private boolean preservePublishOrder = false;

	@Nullable
	private ApplicationEventPublisher eventPublisher;

	private AtomicBoolean brokerAvailable = new AtomicBoolean();

	private final BrokerAvailabilityEvent availableEvent = new BrokerAvailabilityEvent(true, this);

	private final BrokerAvailabilityEvent notAvailableEvent = new BrokerAvailabilityEvent(false, this);

	private boolean autoStartup = true;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();

	private final ChannelInterceptor unsentDisconnectInterceptor = new UnsentDisconnectChannelInterceptor();


	/**
	 * 不带目标前缀（匹配所有目标）的构造函数。
	 * @param inboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param outboundChannel the channel for sending messages to clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for the application to send messages to the broker
	 */
	public AbstractBrokerMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel) {

		this(inboundChannel, outboundChannel, brokerChannel, Collections.emptyList());
	}

	/**
	 * 带目标前缀（用于匹配消息目标）的构造函数。
	 * @param inboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param outboundChannel the channel for sending messages to clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for the application to send messages to the broker
	 * @param destinationPrefixes prefixes to use to filter out messages
	 */
	public AbstractBrokerMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel, @Nullable Collection<String> destinationPrefixes) {

		Assert.notNull(inboundChannel, "'inboundChannel' must not be null");
		Assert.notNull(outboundChannel, "'outboundChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");

		this.clientInboundChannel = inboundChannel;
		this.clientOutboundChannel = outboundChannel;
		this.brokerChannel = brokerChannel;

		destinationPrefixes = (destinationPrefixes != null ? destinationPrefixes : Collections.emptyList());
		this.destinationPrefixes = Collections.unmodifiableCollection(destinationPrefixes);
	}


	public SubscribableChannel getClientInboundChannel() {
		return this.clientInboundChannel;
	}

	public MessageChannel getClientOutboundChannel() {
		return this.clientOutboundChannel;
	}

	public SubscribableChannel getBrokerChannel() {
		return this.brokerChannel;
	}

	/**
	 * 返回用于过滤要转发到代理的消息的目标前缀。
	 * 有目标但目标不匹配的消息会被忽略。
	 * <p>默认不设置。
	 */
	public Collection<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	/**
	 * 配置一个 Predicate 以识别具有用户目标的消息。当未配置任何
	 * {@link #getDestinationPrefixes() 目标前缀}时，这有助于识别并跳过那些
	 * 需要在到达代理之前由
	 * {@link org.springframework.messaging.simp.user.UserDestinationMessageHandler}
	 * 预处理的用户目标消息。
	 * @param predicate the predicate to identify user messages with a non-null
	 * destination as messages with a user destinations.
	 * @since 5.3.4
	 */
	public void setUserDestinationPredicate(@Nullable Predicate<String> predicate) {
		this.userDestinationPredicate = predicate;
	}

	/**
	 * 客户端是否必须按发布顺序接收消息。
	 * <p>默认情况下，发送到 {@code "clientOutboundChannel"} 的消息可能不会按相同顺序处理，
	 * 因为该通道由 ThreadPoolExecutor 支持，而 ThreadPoolExecutor 不保证按顺序处理。
	 * <p>当此标志设置为 {@code true} 时，同一会话内的消息将逐个发送到
	 * {@code "clientOutboundChannel"}，以保持发布顺序。仅在需要时启用此选项，
	 * 因为保持消息顺序会带来一定的性能开销。
	 * @param preservePublishOrder whether to publish in order
	 * @since 5.1
	 */
	public void setPreservePublishOrder(boolean preservePublishOrder) {
		OrderedMessageChannelDecorator.configureInterceptor(this.clientOutboundChannel, preservePublishOrder);
		this.preservePublishOrder = preservePublishOrder;
	}

	/**
	 * 是否确保消息按发布顺序接收。
	 * @since 5.1
	 */
	public boolean isPreservePublishOrder() {
		return this.preservePublishOrder;
	}

	@Override
	public void setApplicationEventPublisher(@Nullable ApplicationEventPublisher publisher) {
		this.eventPublisher = publisher;
	}

	@Nullable
	public ApplicationEventPublisher getApplicationEventPublisher() {
		return this.eventPublisher;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}


	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			logger.info("Starting...");
			this.clientInboundChannel.subscribe(this);
			this.brokerChannel.subscribe(this);
			if (this.clientInboundChannel instanceof InterceptableChannel) {
				((InterceptableChannel) this.clientInboundChannel).addInterceptor(0, this.unsentDisconnectInterceptor);
			}
			startInternal();
			this.running = true;
			logger.info("Started.");
		}
	}

	protected void startInternal() {
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			logger.info("Stopping...");
			stopInternal();
			this.clientInboundChannel.unsubscribe(this);
			this.brokerChannel.unsubscribe(this);
			if (this.clientInboundChannel instanceof InterceptableChannel) {
				((InterceptableChannel) this.clientInboundChannel).removeInterceptor(this.unsentDisconnectInterceptor);
			}
			this.running = false;
			logger.info("Stopped.");
		}
	}

	protected void stopInternal() {
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	/**
	 * 检查此消息处理器当前是否正在运行。
	 * <p>请注意，即使此消息处理器正在运行，根据具体子类实现，
	 * {@link #isBrokerAvailable()} 标志仍可能独立地在开启和关闭之间交替。
	 */
	@Override
	public final boolean isRunning() {
		return this.running;
	}

	/**
	 * 消息代理当前是否可用并能处理消息。
	 * <p>请注意，这是对 {@link #isRunning()} 标志的补充，后者表示此消息处理器是否正在运行。
	 * 换句话说，消息处理器必须首先处于运行状态，然后根据具体子类实现，
	 * {@code #isBrokerAvailable()} 标志仍可能独立地在开启和关闭之间交替。
	 * <p>应用程序组件可以实现
	 * {@code org.springframework.context.ApplicationListener<BrokerAvailabilityEvent>}，
	 * 以在代理变为可用和不可用时接收通知。
	 */
	public boolean isBrokerAvailable() {
		return this.brokerAvailable.get();
	}


	@Override
	public void handleMessage(Message<?> message) {
		if (!this.running) {
			if (logger.isTraceEnabled()) {
				logger.trace(this + " not running yet. Ignoring " + message);
			}
			return;
		}
		handleMessageInternal(message);
	}

	protected abstract void handleMessageInternal(Message<?> message);


	/**
	 * 是否应处理具有给定目标的消息。如果满足以下任一条件，则进行处理：
	 * <ol>
	 * <li>目标以某个已配置的 {@link #getDestinationPrefixes() 目标前缀}开头。
	 * <li>未配置前缀，且目标不匹配
	 * {@link #setUserDestinationPredicate(Predicate) userDestinationPredicate}。
	 * <li>消息没有目标。
	 * </ol>
	 * @param destination the destination to check
	 * @return whether to process (true) or skip (false) the destination
	 */
	protected boolean checkDestinationPrefix(@Nullable String destination) {
		if (destination == null) {
			return true;
		}
		if (CollectionUtils.isEmpty(this.destinationPrefixes)) {
			return !isUserDestination(destination);
		}
		for (String prefix : this.destinationPrefixes) {
			if (destination.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private boolean isUserDestination(String destination) {
		return (this.userDestinationPredicate != null && this.userDestinationPredicate.test(destination));
	}

	protected void publishBrokerAvailableEvent() {
		boolean shouldPublish = this.brokerAvailable.compareAndSet(false, true);
		if (this.eventPublisher != null && shouldPublish) {
			if (logger.isInfoEnabled()) {
				logger.info(this.availableEvent);
			}
			this.eventPublisher.publishEvent(this.availableEvent);
		}
	}

	protected void publishBrokerUnavailableEvent() {
		boolean shouldPublish = this.brokerAvailable.compareAndSet(true, false);
		if (this.eventPublisher != null && shouldPublish) {
			if (logger.isInfoEnabled()) {
				logger.info(this.notAvailableEvent);
			}
			this.eventPublisher.publishEvent(this.notAvailableEvent);
		}
	}

	/**
	 * 获取用于向客户端发送消息的 MessageChannel；当
	 * {@code preservePublishOrder=true} 时，可能是按会话包装的通道。
	 * @since 5.1
	 */
	protected MessageChannel getClientOutboundChannelForSession(String sessionId) {
		return this.preservePublishOrder ?
				new OrderedMessageChannelDecorator(getClientOutboundChannel(), logger) : getClientOutboundChannel();
	}


	/**
	 * 检测未发送的 DISCONNECT 消息并仍然处理它们。
	 */
	private class UnsentDisconnectChannelInterceptor implements ChannelInterceptor {

		@Override
		public void afterSendCompletion(
				Message<?> message, MessageChannel channel, boolean sent, @Nullable Exception ex) {

			if (!sent) {
				SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
				if (SimpMessageType.DISCONNECT.equals(messageType)) {
					logger.debug("Detected unsent DISCONNECT message. Processing anyway.");
					handleMessage(message);
				}
			}
		}
	}

}
