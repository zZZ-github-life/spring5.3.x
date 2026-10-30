/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * 用于配置 {@link StompBrokerRelayMessageHandler} 的注册类。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompBrokerRelayRegistration extends AbstractBrokerRegistration {

	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String clientLogin = "guest";

	private String clientPasscode = "guest";

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	@Nullable
	private Long systemHeartbeatSendInterval;

	@Nullable
	private Long systemHeartbeatReceiveInterval;

	@Nullable
	private String virtualHost;

	@Nullable
	private TcpOperations<byte[]> tcpClient;

	@Nullable
	private TaskScheduler taskScheduler;

	private boolean autoStartup = true;

	@Nullable
	private String userDestinationBroadcast;

	@Nullable
	private String userRegistryBroadcast;


	/**
	 * 创建一个新的 {@code StompBrokerRelayRegistration}。
	 * @param clientInboundChannel 入站通道
	 * @param clientOutboundChannel 出站通道
	 * @param destinationPrefixes 目标地址前缀
	 */
	public StompBrokerRelayRegistration(SubscribableChannel clientInboundChannel,
			MessageChannel clientOutboundChannel, String[] destinationPrefixes) {

		super(clientInboundChannel, clientOutboundChannel, destinationPrefixes);
	}


	/**
	 * 设置 STOMP 消息代理主机。
	 */
	public StompBrokerRelayRegistration setRelayHost(String relayHost) {
		Assert.hasText(relayHost, "relayHost must not be empty");
		this.relayHost = relayHost;
		return this;
	}

	/**
	 * 设置 STOMP 消息代理端口。
	 */
	public StompBrokerRelayRegistration setRelayPort(int relayPort) {
		this.relayPort = relayPort;
		return this;
	}

	/**
	 * 设置代表已连接客户端创建到 STOMP 代理的连接时使用的登录名。
	 * <p>默认情况下此项设置为 "guest"。
	 */
	public StompBrokerRelayRegistration setClientLogin(String login) {
		Assert.hasText(login, "clientLogin must not be empty");
		this.clientLogin = login;
		return this;
	}

	/**
	 * 设置代表已连接客户端创建到 STOMP 代理的连接时使用的密码。
	 * <p>默认情况下此项设置为 "guest"。
	 */
	public StompBrokerRelayRegistration setClientPasscode(String passcode) {
		Assert.hasText(passcode, "clientPasscode must not be empty");
		this.clientPasscode = passcode;
		return this;
	}

	/**
	 * 设置共享的 "system" 连接（用于从应用程序内部向 STOMP 代理发送消息，
	 * 即不与特定客户端会话关联的消息，例如 REST/HTTP 请求处理方法）的登录名。
	 * <p>默认情况下此项设置为 "guest"。
	 */
	public StompBrokerRelayRegistration setSystemLogin(String login) {
		Assert.hasText(login, "systemLogin must not be empty");
		this.systemLogin = login;
		return this;
	}

	/**
	 * 设置共享的 "system" 连接（用于从应用程序内部向 STOMP 代理发送消息，
	 * 即不与特定客户端会话关联的消息，例如 REST/HTTP 请求处理方法）的密码。
	 * <p>默认情况下此项设置为 "guest"。
	 */
	public StompBrokerRelayRegistration setSystemPasscode(String passcode) {
		Assert.hasText(passcode, "systemPasscode must not be empty");
		this.systemPasscode = passcode;
		return this;
	}

	/**
	 * 设置 "system" 中继会话在没有其他数据发送时向 STOMP 代理发送心跳的
	 * 时间间隔（以毫秒为单位）。值为零将阻止向代理发送心跳。
	 * <p>默认值为 10000。
	 */
	public StompBrokerRelayRegistration setSystemHeartbeatSendInterval(long systemHeartbeatSendInterval) {
		this.systemHeartbeatSendInterval = systemHeartbeatSendInterval;
		return this;
	}

	/**
	 * 设置 "system" 中继会话在没有其他数据时期望从 STOMP 代理接收心跳的
	 * 最大时间间隔（以毫秒为单位）。值为零将配置该中继会话不期望从代理
	 * 接收心跳。
	 * <p>默认值为 10000。
	 */
	public StompBrokerRelayRegistration setSystemHeartbeatReceiveInterval(long heartbeatReceiveInterval) {
		this.systemHeartbeatReceiveInterval = heartbeatReceiveInterval;
		return this;
	}

	/**
	 * 设置 STOMP CONNECT 帧中使用的 "host" 头部的值。配置此属性后，
	 * 将向发送到 STOMP 代理的每个 STOMP 帧添加一个 "host" 头部。
	 * 例如，在云环境中，建立 TCP 连接的实际主机与提供基于云的 STOMP 服务的
	 * 主机不同时，这可能很有用。
	 * <p>默认情况下不设置此属性。
	 */
	public StompBrokerRelayRegistration setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
		return this;
	}

	/**
	 * 配置用于管理与 STOMP 代理的 TCP 连接的 TCP 客户端。
	 * <p>默认使用 {@code ReactorNettyTcpClient}。
	 * <p><strong>注意：</strong>使用此属性时，指定的任何
	 * {@link #setRelayHost(String) host} 或 {@link #setRelayPort(int) port}
	 * 实际上都会被忽略。
	 * @since 4.3.15
	 */
	public StompBrokerRelayRegistration setTcpClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
		return this;
	}

	/**
	 * 某些 STOMP 客户端（例如 stomp-js）总是以固定速率发送心跳，
	 * 而其他客户端（Spring STOMP 客户端）仅在没有发送其他消息时才发送心跳。
	 * 然而，具有非代理 {@link #getDestinationPrefixes()
	 * destination prefix} 的消息不会被转发，因此代理可能会认为连接处于非活动状态。
	 * <p>设置此 {@link TaskScheduler} 后，它用于重置自当前心跳周期开始以来
	 * 从客户端发送到代理的消息数量的计数。在忽略具有非代理目标前缀的消息时，
	 * 该计数用于决定是否向代理发送心跳。
	 * @since 5.3
	 */
	public StompBrokerRelayRegistration setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		return this;
	}

	/**
	 * 配置 Spring ApplicationContext 刷新时是否应自动启动
	 * {@link StompBrokerRelayMessageHandler}。
	 * <p>默认设置为 {@code true}。
	 */
	public StompBrokerRelayRegistration setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
		return this;
	}

	/**
	 * 设置一个目标地址，用于广播那些因用户似乎未连接而仍未解析的用户目标地址对应的消息。
	 * 在多应用服务器场景中，这为其他应用服务器提供了尝试处理的机会。
	 * <p>默认情况下不设置此项。
	 * @param destination 广播未解析消息的目标地址，
	 * 例如 "/topic/unresolved-user-destination"
	 */
	public StompBrokerRelayRegistration setUserDestinationBroadcast(String destination) {
		this.userDestinationBroadcast = destination;
		return this;
	}

	@Nullable
	protected String getUserDestinationBroadcast() {
		return this.userDestinationBroadcast;
	}

	/**
	 * 设置一个目标地址，用于广播本地用户注册表的内容，并监听来自其他服务器的
	 * 此类广播。在多应用服务器场景中，这使每个服务器的用户注册表能够感知
	 * 连接到其他服务器的用户。
	 * <p>默认情况下不设置此项。
	 * @param destination 广播用户注册表详细信息的目标地址，
	 * 例如 "/topic/simp-user-registry"。
	 */
	public StompBrokerRelayRegistration setUserRegistryBroadcast(String destination) {
		this.userRegistryBroadcast = destination;
		return this;
	}

	@Nullable
	protected String getUserRegistryBroadcast() {
		return this.userRegistryBroadcast;
	}


	@Override
	protected StompBrokerRelayMessageHandler getMessageHandler(SubscribableChannel brokerChannel) {
		StompBrokerRelayMessageHandler handler = new StompBrokerRelayMessageHandler(
				getClientInboundChannel(), getClientOutboundChannel(),
				brokerChannel, getDestinationPrefixes());

		handler.setRelayHost(this.relayHost);
		handler.setRelayPort(this.relayPort);

		handler.setClientLogin(this.clientLogin);
		handler.setClientPasscode(this.clientPasscode);

		handler.setSystemLogin(this.systemLogin);
		handler.setSystemPasscode(this.systemPasscode);

		if (this.systemHeartbeatSendInterval != null) {
			handler.setSystemHeartbeatSendInterval(this.systemHeartbeatSendInterval);
		}
		if (this.systemHeartbeatReceiveInterval != null) {
			handler.setSystemHeartbeatReceiveInterval(this.systemHeartbeatReceiveInterval);
		}
		if (this.virtualHost != null) {
			handler.setVirtualHost(this.virtualHost);
		}
		if (this.tcpClient != null) {
			handler.setTcpClient(this.tcpClient);
		}
		if (this.taskScheduler != null) {
			handler.setTaskScheduler(this.taskScheduler);
		}

		handler.setAutoStartup(this.autoStartup);

		return handler;
	}

}
