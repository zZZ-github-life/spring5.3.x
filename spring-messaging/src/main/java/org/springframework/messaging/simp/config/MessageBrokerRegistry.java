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

package org.springframework.messaging.simp.config;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.context.event.SmartApplicationListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

/**
 * 用于配置消息代理选项的注册表。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class MessageBrokerRegistry {

	private final SubscribableChannel clientInboundChannel;

	private final MessageChannel clientOutboundChannel;

	@Nullable
	private SimpleBrokerRegistration simpleBrokerRegistration;

	@Nullable
	private StompBrokerRelayRegistration brokerRelayRegistration;

	private final ChannelRegistration brokerChannelRegistration = new ChannelRegistration();

	@Nullable
	private String[] applicationDestinationPrefixes;

	@Nullable
	private String userDestinationPrefix;

	@Nullable
	private Integer userRegistryOrder;

	@Nullable
	private PathMatcher pathMatcher;

	@Nullable
	private Integer cacheLimit;

	private boolean preservePublishOrder;


	public MessageBrokerRegistry(SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel) {
		Assert.notNull(clientInboundChannel, "Inbound channel must not be null");
		Assert.notNull(clientOutboundChannel, "Outbound channel must not be null");
		this.clientInboundChannel = clientInboundChannel;
		this.clientOutboundChannel = clientOutboundChannel;
	}


	/**
	 * 启用简单消息代理，并配置一个或多个前缀以过滤以该代理为目标的目标地址
	 * （例如以 "/topic" 为前缀的目标地址）。
	 */
	public SimpleBrokerRegistration enableSimpleBroker(String... destinationPrefixes) {
		this.simpleBrokerRegistration = new SimpleBrokerRegistration(
				this.clientInboundChannel, this.clientOutboundChannel, destinationPrefixes);
		return this.simpleBrokerRegistration;
	}

	/**
	 * 启用 STOMP 消息代理中继，并配置消息代理支持的目标地址前缀。
	 * 有关支持的目标地址，请查阅消息代理的 STOMP 文档。
	 */
	public StompBrokerRelayRegistration enableStompBrokerRelay(String... destinationPrefixes) {
		this.brokerRelayRegistration = new StompBrokerRelayRegistration(
				this.clientInboundChannel, this.clientOutboundChannel, destinationPrefixes);
		return this.brokerRelayRegistration;
	}

	/**
	 * 自定义用于从应用程序向消息代理发送消息的通道。默认情况下，从应用程序
	 * 发往消息代理的消息是同步发送的，这意味着发送消息的应用程序代码可以
	 * 通过异常得知消息是否无法发送。不过，如果在此处使用任务执行器属性配置
	 * 代理通道，则可以改变这一行为。
	 */
	public ChannelRegistration configureBrokerChannel() {
		return this.brokerChannelRegistration;
	}

	protected ChannelRegistration getBrokerChannelRegistration() {
		return this.brokerChannelRegistration;
	}

	@Nullable
	protected String getUserDestinationBroadcast() {
		return (this.brokerRelayRegistration != null ?
				this.brokerRelayRegistration.getUserDestinationBroadcast() : null);
	}

	@Nullable
	protected String getUserRegistryBroadcast() {
		return (this.brokerRelayRegistration != null ?
				this.brokerRelayRegistration.getUserRegistryBroadcast() : null);
	}

	/**
	 * 配置一个或多个前缀，用于过滤以应用程序注解方法为目标的目标地址。
	 * 例如，以 "/app" 为前缀的目标地址可由注解方法处理，而其他目标地址
	 * 则可以以消息代理为目标（例如 "/topic"、"/queue"）。
	 * <p>处理消息时，会从目标地址中移除匹配的前缀以构成查找路径。这意味着
	 * 注解中不应包含目标地址前缀。
	 * <p>没有尾部斜杠的前缀会自动追加一个斜杠。
	 */
	public MessageBrokerRegistry setApplicationDestinationPrefixes(String... prefixes) {
		this.applicationDestinationPrefixes = prefixes;
		return this;
	}

	@Nullable
	protected Collection<String> getApplicationDestinationPrefixes() {
		return (this.applicationDestinationPrefixes != null ?
				Arrays.asList(this.applicationDestinationPrefixes) : null);
	}

	/**
	 * 配置用于标识用户目标地址的前缀。用户目标地址使一个用户能够订阅
	 * 其会话唯一的队列名称，也使他人的消息能够发送到这些唯一的、
	 * 用户专属的队列。
	 * <p>例如，当用户尝试订阅 "/user/queue/position-updates" 时，
	 * 目标地址可能会被转换为 "/queue/position-updatesi9oqdfzo"，从而得到一个
	 * 不会与其他尝试相同操作的用户发生冲突的唯一队列名称。
	 * 随后，当消息发送到 "/user/{username}/queue/position-updates" 时，
	 * 目标地址会被转换为 "/queue/position-updatesi9oqdfzo"。
	 * <p>用于标识此类目标地址的默认前缀是 "/user/"。
	 */
	public MessageBrokerRegistry setUserDestinationPrefix(String destinationPrefix) {
		this.userDestinationPrefix = destinationPrefix;
		return this;
	}

	@Nullable
	protected String getUserDestinationPrefix() {
		return this.userDestinationPrefix;
	}

	/**
	 * 设置 {@link org.springframework.messaging.simp.user.SimpUserRegistry
	 * SimpUserRegistry} 作为 {@link SmartApplicationListener} 使用的顺序。
	 * @param order the order value
	 * @since 5.0.8
	 */
	public void setUserRegistryOrder(int order) {
		this.userRegistryOrder = order;
	}

	@Nullable
	protected Integer getUserRegistryOrder() {
		return this.userRegistryOrder;
	}

	/**
	 * 配置用于将传入消息的目标地址与 {@code @MessageMapping} 和
	 * {@code @SubscribeMapping} 方法进行匹配的 PathMatcher。
	 * <p>默认配置的是 {@link org.springframework.util.AntPathMatcher}。
	 * 不过，应用程序可以提供经过定制的 {@code AntPathMatcher} 实例，使用 "."
	 * （消息传递中常用）而不是 "/" 作为路径分隔符，或者提供完全不同的
	 * PathMatcher 实现。
	 * <p>请注意，所配置的 PathMatcher 仅用于匹配目标地址中已配置前缀之后的部分。
	 * 例如，给定应用程序目标前缀 "/app" 和目标地址 "/app/price.stock.**"，
	 * 消息可能被映射到类型级映射为 "price"、方法级映射为 "stock.**" 的控制器。
	 * <p>启用简单代理时，此处配置的 PathMatcher 也用于在代理消息时
	 * 匹配消息目标地址。
	 * @since 4.1
	 * @see org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry#setPathMatcher
	 */
	public MessageBrokerRegistry setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	@Nullable
	protected PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 配置应用于代理注册的缓存限制。
	 * <p>目前这仅应用于订阅注册表中的目标地址缓存。该处的默认缓存限制为 1024。
	 * @since 4.3.2
	 * @see org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry#setCacheLimit
	 */
	public MessageBrokerRegistry setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
		return this;
	}

	/**
	 * 客户端是否必须按发布顺序接收消息。
	 * <p>默认情况下，发送到 {@code "clientOutboundChannel"} 的消息可能不会按相同顺序
	 * 处理，因为该通道由 ThreadPoolExecutor 支持，而 ThreadPoolExecutor 不保证
	 * 按序处理。
	 * <p>当此标志设置为 {@code true} 时，同一会话内的消息将逐个按序发送到
	 * {@code "clientOutboundChannel"}，以保持发布顺序。仅在需要时启用此选项，
	 * 因为保持消息顺序会带来一定的性能开销。
	 * @since 5.1
	 */
	public MessageBrokerRegistry setPreservePublishOrder(boolean preservePublishOrder) {
		this.preservePublishOrder = preservePublishOrder;
		return this;
	}

	@Nullable
	protected SimpleBrokerMessageHandler getSimpleBroker(SubscribableChannel brokerChannel) {
		if (this.simpleBrokerRegistration == null && this.brokerRelayRegistration == null) {
			enableSimpleBroker();
		}
		if (this.simpleBrokerRegistration != null) {
			SimpleBrokerMessageHandler handler = this.simpleBrokerRegistration.getMessageHandler(brokerChannel);
			handler.setPathMatcher(this.pathMatcher);
			handler.setCacheLimit(this.cacheLimit);
			handler.setPreservePublishOrder(this.preservePublishOrder);
			return handler;
		}
		return null;
	}

	@Nullable
	protected StompBrokerRelayMessageHandler getStompBrokerRelay(SubscribableChannel brokerChannel) {
		if (this.brokerRelayRegistration != null) {
			StompBrokerRelayMessageHandler relay = this.brokerRelayRegistration.getMessageHandler(brokerChannel);
			relay.setPreservePublishOrder(this.preservePublishOrder);
			return relay;
		}
		return null;
	}

}
