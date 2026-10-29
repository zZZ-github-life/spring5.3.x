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

package org.springframework.messaging.simp.stomp;

import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.tcp.FixedIntervalReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 一个 {@link org.springframework.messaging.MessageHandler}，通过将消息转发到 STOMP 代理来处理消息。
 *
 * <p>对于每个新的 {@link SimpMessageType#CONNECT CONNECT} 消息，
 * 都会打开一条独立的、到代理的 TCP 连接，并专门用于发起该 CONNECT 消息的客户端的所有消息。
 * 同一客户端的消息通过会话 ID 消息头来标识。反过来，当 STOMP 代理通过 TCP 连接
 * 发回消息时，这些消息会使用客户端的会话 ID 进行丰富，
 * 并通过提供给构造函数的 {@link MessageChannel} 发送回下游。
 *
 * <p>此类还会自动打开一条默认的“系统”TCP 连接，用于发送源自服务器应用程序的消息
 * （而不是来自客户端的消息）。此类消息不与任何客户端关联，因此没有会话 ID 头。
 * “系统”连接实际上是共享的，不能用于接收消息。提供了几个属性来配置“系统”连接，包括：
 * <ul>
 * <li>{@link #setSystemLogin}</li>
 * <li>{@link #setSystemPasscode}</li>
 * <li>{@link #setSystemHeartbeatSendInterval}</li>
 * <li>{@link #setSystemHeartbeatReceiveInterval}</li>
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 * @since 4.0
 */
public class StompBrokerRelayMessageHandler extends AbstractBrokerMessageHandler {

	/**
	 * 系统会话 ID。
	 */
	public static final String SYSTEM_SESSION_ID = "_system_";

	/** STOMP 建议的接收心跳错误容限。 */
	private static final long HEARTBEAT_MULTIPLIER = 3;

	/**
	 * 收到带有心跳设置的 CONNECTED 帧后开始心跳。
	 * 如果 CONNECTED 在一分钟内未到达，我们将关闭连接。
	 */
	private static final int MAX_TIME_TO_CONNECTED_FRAME = 60 * 1000;

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private static final ListenableFutureTask<Void> EMPTY_TASK = new ListenableFutureTask<>(new VoidCallable());

	private static final StompHeaderAccessor HEART_BEAT_ACCESSOR;

	private static final Message<byte[]> HEARTBEAT_MESSAGE;

	static {
		EMPTY_TASK.run();
		HEART_BEAT_ACCESSOR = StompHeaderAccessor.createForHeartbeat();
		HEARTBEAT_MESSAGE = MessageBuilder.createMessage(
				StompDecoder.HEARTBEAT_PAYLOAD, HEART_BEAT_ACCESSOR.getMessageHeaders());
	}


	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String clientLogin = "guest";

	private String clientPasscode = "guest";

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	private long systemHeartbeatSendInterval = 10000;

	private long systemHeartbeatReceiveInterval = 10000;

	private final Map<String, MessageHandler> systemSubscriptions = new HashMap<>(4);

	@Nullable
	private String virtualHost;

	@Nullable
	private TcpOperations<byte[]> tcpClient;

	@Nullable
	private MessageHeaderInitializer headerInitializer;

	private final DefaultStats stats = new DefaultStats();

	private final Map<String, RelayConnectionHandler> connectionHandlers = new ConcurrentHashMap<>();

	@Nullable
	private TaskScheduler taskScheduler;


	/**
	 * 使用给定消息通道和目标前缀创建 StompBrokerRelayMessageHandler 实例。
	 * @param inboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param outboundChannel the channel for sending messages to clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for the application to send messages to the broker
	 * @param destinationPrefixes the broker supported destination prefixes; destinations
	 * that do not match the given prefix are ignored.
	 */
	public StompBrokerRelayMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel, Collection<String> destinationPrefixes) {

		super(inboundChannel, outboundChannel, brokerChannel, destinationPrefixes);
	}


	/**
	 * 设置 STOMP 消息代理主机。
	 */
	public void setRelayHost(String relayHost) {
		Assert.hasText(relayHost, "relayHost must not be empty");
		this.relayHost = relayHost;
	}

	/**
	 * 返回 STOMP 消息代理主机。
	 */
	public String getRelayHost() {
		return this.relayHost;
	}

	/**
	 * 设置 STOMP 消息代理端口。
	 */
	public void setRelayPort(int relayPort) {
		this.relayPort = relayPort;
	}

	/**
	 * 返回 STOMP 消息代理端口。
	 */
	public int getRelayPort() {
		return this.relayPort;
	}
	/**
	 * 设置代表已连接客户端创建到 STOMP 代理连接时使用的登录名。
	 * <p>默认设置为 "guest"。
	 * @see #setSystemLogin(String)
	 */
	public void setClientLogin(String clientLogin) {
		Assert.hasText(clientLogin, "clientLogin must not be empty");
		this.clientLogin = clientLogin;
	}

	/**
	 * 返回代表已连接客户端连接到 STOMP 代理时使用的已配置登录名。
	 * @see #getSystemLogin()
	 */
	public String getClientLogin() {
		return this.clientLogin;
	}

	/**
	 * 设置代表已连接客户端创建到 STOMP 代理连接时使用的客户端密码。
	 * <p>默认设置为 "guest"。
	 * @see #setSystemPasscode
	 */
	public void setClientPasscode(String clientPasscode) {
		Assert.hasText(clientPasscode, "clientPasscode must not be empty");
		this.clientPasscode = clientPasscode;
	}

	/**
	 * 返回代表已连接客户端连接到 STOMP 代理时使用的已配置密码。
	 * @see #getSystemPasscode()
	 */
	public String getClientPasscode() {
		return this.clientPasscode;
	}

	/**
	 * 设置用于从应用程序内部向 STOMP 代理发送消息的共享“系统”连接的登录名，
	 * 即不与特定客户端会话关联的消息（例如 REST/HTTP 请求处理方法）。
	 * <p>默认设置为 "guest"。
	 */
	public void setSystemLogin(String systemLogin) {
		Assert.hasText(systemLogin, "systemLogin must not be empty");
		this.systemLogin = systemLogin;
	}

	/**
	 * 返回用于到 STOMP 代理的共享“系统”连接的登录名。
	 */
	public String getSystemLogin() {
		return this.systemLogin;
	}

	/**
	 * 设置用于从应用程序内部向 STOMP 代理发送消息的共享“系统”连接的密码，
	 * 即不与特定客户端会话关联的消息（例如 REST/HTTP 请求处理方法）。
	 * <p>默认设置为 "guest"。
	 */
	public void setSystemPasscode(String systemPasscode) {
		this.systemPasscode = systemPasscode;
	}

	/**
	 * 返回用于到 STOMP 代理的共享“系统”连接的密码。
	 */
	public String getSystemPasscode() {
		return this.systemPasscode;
	}


	/**
	 * 设置“系统”连接在没有任何其他数据发送时向 STOMP 代理发送心跳的间隔（以毫秒为单位）。
	 * 值为零将阻止向代理发送心跳。
	 * <p>默认值为 10000。
	 * <p>有关“系统”连接的更多信息，请参阅类级文档。
	 */
	public void setSystemHeartbeatSendInterval(long systemHeartbeatSendInterval) {
		this.systemHeartbeatSendInterval = systemHeartbeatSendInterval;
	}

	/**
	 * 返回“系统”连接向 STOMP 代理发送心跳的间隔（以毫秒为单位）。
	 */
	public long getSystemHeartbeatSendInterval() {
		return this.systemHeartbeatSendInterval;
	}

	/**
	 * 设置“系统”连接在没有任何其他数据时预期从 STOMP 代理接收心跳的最大间隔（以毫秒为单位）。
	 * 值为零将配置该连接不期望从代理接收心跳。
	 * <p>默认值为 10000。
	 * <p>有关“系统”连接的更多信息，请参阅类级文档。
	 */
	public void setSystemHeartbeatReceiveInterval(long heartbeatReceiveInterval) {
		this.systemHeartbeatReceiveInterval = heartbeatReceiveInterval;
	}

	/**
	 * 返回“系统”连接预期从 STOMP 代理接收心跳的间隔（以毫秒为单位）。
	 */
	public long getSystemHeartbeatReceiveInterval() {
		return this.systemHeartbeatReceiveInterval;
	}

	/**
	 * 配置共享“系统”连接上要订阅的一个或多个目标，以及用于处理接收消息的 MessageHandler。
	 * <p>这供多应用服务器场景内部使用，其中服务器之间会相互转发消息
	 * （例如未解析的用户目标）。
	 * @param subscriptions the destinations to subscribe to.
	 */
	public void setSystemSubscriptions(@Nullable Map<String, MessageHandler> subscriptions) {
		this.systemSubscriptions.clear();
		if (subscriptions != null) {
			this.systemSubscriptions.putAll(subscriptions);
		}
	}

	/**
	 * 返回已配置的“系统”连接订阅映射。
	 */
	public Map<String, MessageHandler> getSystemSubscriptions() {
		return this.systemSubscriptions;
	}

	/**
	 * 设置 STOMP CONNECT 帧中 "host" 头的值。配置此属性后，
	 * 每个发送到 STOMP 代理的 STOMP 帧都会添加一个 "host" 头。
	 * 例如，在云环境中，建立 TCP 连接的实际主机与提供基于云的 STOMP 服务的主机不同时，
	 * 这可能很有用。
	 * <p>默认不设置此属性。
	 */
	public void setVirtualHost(@Nullable String virtualHost) {
		this.virtualHost = virtualHost;
	}

	/**
	 * 返回已配置的虚拟主机值。
	 */
	@Nullable
	public String getVirtualHost() {
		return this.virtualHost;
	}

	/**
	 * 配置用于管理到 STOMP 代理的 TCP 连接的 TCP 客户端。
	 * <p>默认使用 {@link ReactorNettyTcpClient}。
	 * <p><strong>注意：</strong>使用此属性时，指定的任何
	 * {@link #setRelayHost(String) host} 或 {@link #setRelayPort(int) port} 都会被忽略。
	 */
	public void setTcpClient(@Nullable TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
	}

	/**
	 * 获取已配置的 TCP 客户端（除非未配置且在处理器启动前调用了此方法，
	 * 因而初始化了默认实现，否则永不为 {@code null}）。
	 */
	@Nullable
	public TcpOperations<byte[]> getTcpClient() {
		return this.tcpClient;
	}

	/**
	 * 配置要应用于通过 {@code StompBrokerRelayMessageHandler} 创建的、
	 * 发送到客户端出站消息通道的所有消息头的 {@link MessageHeaderInitializer}。
	 * <p>默认不设置此属性。
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * 返回已配置的头初始化器。
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}

	/**
	 * 返回描述内部状态和计数器的 String。
	 * 实际上是对 {@link #getStats() getStats()} 调用 {@code toString()}。
	 */
	public String getStatsInfo() {
		return this.stats.toString();
	}

	/**
	 * 返回包含内部状态和计数器的结构化对象。
	 * @since 5.2
	 */
	public Stats getStats() {
		return this.stats;
	}


	/**
	 * 返回当前到代理的 TCP 连接数。
	 */
	public int getConnectionCount() {
		return this.connectionHandlers.size();
	}

	/**
	 * 配置用于在当前心跳周期内重置客户端到代理消息计数的
	 * {@link TaskScheduler}。有关更多详细信息，请参阅
	 * {@link org.springframework.messaging.simp.config.StompBrokerRelayRegistration#setTaskScheduler(TaskScheduler)}。
	 * @param taskScheduler the scheduler to use
	 * @since 5.3
	 */
	public void setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	@Nullable
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}


	@Override
	protected void startInternal() {
		if (this.tcpClient == null) {
			this.tcpClient = initTcpClient();
		}

		if (logger.isInfoEnabled()) {
			logger.info("Starting \"system\" session, " + toString());
		}

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setAcceptVersion("1.1,1.2");
		accessor.setLogin(this.systemLogin);
		accessor.setPasscode(this.systemPasscode);
		accessor.setHeartbeat(this.systemHeartbeatSendInterval, this.systemHeartbeatReceiveInterval);
		String virtualHost = getVirtualHost();
		if (virtualHost != null) {
			accessor.setHost(virtualHost);
		}
		accessor.setSessionId(SYSTEM_SESSION_ID);
		if (logger.isDebugEnabled()) {
			logger.debug("Forwarding " + accessor.getShortLogMessage(EMPTY_PAYLOAD));
		}

		SystemSessionConnectionHandler handler = new SystemSessionConnectionHandler(accessor);
		this.connectionHandlers.put(handler.getSessionId(), handler);

		this.stats.incrementConnectCount();
		this.tcpClient.connect(handler, new FixedIntervalReconnectStrategy(5000));

		if (this.taskScheduler != null) {
			this.taskScheduler.scheduleWithFixedDelay(new ClientSendMessageCountTask(), 5000);
		}
	}

	private ReactorNettyTcpClient<byte[]> initTcpClient() {
		StompDecoder decoder = new StompDecoder();
		if (this.headerInitializer != null) {
			decoder.setHeaderInitializer(this.headerInitializer);
		}
		ReactorNettyCodec<byte[]> codec = new StompReactorNettyCodec(decoder);
		ReactorNettyTcpClient<byte[]> client = new ReactorNettyTcpClient<>(this.relayHost, this.relayPort, codec);
		client.setLogger(SimpLogging.forLog(client.getLogger()));
		return client;
	}

	@Override
	protected void stopInternal() {
		publishBrokerUnavailableEvent();
		if (this.tcpClient != null) {
			try {
				this.tcpClient.shutdown().get(5000, TimeUnit.MILLISECONDS);
			}
			catch (Throwable ex) {
				logger.error("Error in shutdown of TCP client", ex);
			}
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());

		if (!isBrokerAvailable()) {
			if (sessionId == null || SYSTEM_SESSION_ID.equals(sessionId)) {
				throw new MessageDeliveryException("Message broker not active. Consider subscribing to " +
						"receive BrokerAvailabilityEvent's from an ApplicationListener Spring bean.");
			}
			RelayConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler != null) {
				handler.sendStompErrorFrameToClient("Broker not available.");
				handler.clearConnection();
			}
			else {
				StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
				if (getHeaderInitializer() != null) {
					getHeaderInitializer().initHeaders(accessor);
				}
				accessor.setSessionId(sessionId);
				Principal user = SimpMessageHeaderAccessor.getUser(message.getHeaders());
				if (user != null) {
					accessor.setUser(user);
				}
				accessor.setMessage("Broker not available.");
				MessageHeaders headers = accessor.getMessageHeaders();
				getClientOutboundChannel().send(MessageBuilder.createMessage(EMPTY_PAYLOAD, headers));
			}
			return;
		}

		StompHeaderAccessor stompAccessor;
		StompCommand command;

		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor == null) {
			throw new IllegalStateException(
					"No header accessor (not using the SimpMessagingTemplate?): " + message);
		}
		else if (accessor instanceof StompHeaderAccessor) {
			stompAccessor = (StompHeaderAccessor) accessor;
			command = stompAccessor.getCommand();
		}
		else if (accessor instanceof SimpMessageHeaderAccessor) {
			stompAccessor = StompHeaderAccessor.wrap(message);
			command = stompAccessor.getCommand();
			if (command == null) {
				command = stompAccessor.updateStompCommandAsClientMessage();
			}
		}
		else {
			throw new IllegalStateException(
					"Unexpected header accessor type " + accessor.getClass() + " in " + message);
		}

		if (sessionId == null) {
			if (!SimpMessageType.MESSAGE.equals(stompAccessor.getMessageType())) {
				if (logger.isErrorEnabled()) {
					logger.error("Only STOMP SEND supported from within the server side. Ignoring " + message);
				}
				return;
			}
			sessionId = SYSTEM_SESSION_ID;
			stompAccessor.setSessionId(sessionId);
		}

		if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
			if (this.connectionHandlers.get(sessionId) != null) {
				if (logger.isWarnEnabled()) {
					logger.warn("Ignoring CONNECT in session " + sessionId + ". Already connected.");
				}
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug(stompAccessor.getShortLogMessage(EMPTY_PAYLOAD));
			}
			stompAccessor = (stompAccessor.isMutable() ? stompAccessor : StompHeaderAccessor.wrap(message));
			stompAccessor.setLogin(this.clientLogin);
			stompAccessor.setPasscode(this.clientPasscode);
			if (getVirtualHost() != null) {
				stompAccessor.setHost(getVirtualHost());
			}
			RelayConnectionHandler handler = new RelayConnectionHandler(sessionId, stompAccessor);
			this.connectionHandlers.put(sessionId, handler);
			this.stats.incrementConnectCount();
			Assert.state(this.tcpClient != null, "No TCP client available");
			this.tcpClient.connect(handler);
		}
		else if (StompCommand.DISCONNECT.equals(command)) {
			RelayConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring DISCONNECT in session " + sessionId + ". Connection already cleaned up.");
				}
				return;
			}
			this.stats.incrementDisconnectCount();
			handler.forward(message, stompAccessor);
		}
		else {
			RelayConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("No TCP connection for session " + sessionId + " in " + message);
				}
				return;
			}

			String destination = stompAccessor.getDestination();
			if (command != null && command.requiresDestination() && !checkDestinationPrefix(destination)) {
				// Not a broker destination but send a heartbeat to keep the connection
				if (handler.shouldSendHeartbeatForIgnoredMessage()) {
					handler.forward(HEARTBEAT_MESSAGE, HEART_BEAT_ACCESSOR);
				}
				return;
			}

			handler.forward(message, stompAccessor);
		}
	}

	@Override
	public String toString() {
		return "StompBrokerRelay[" + getTcpClientInfo() + "]";
	}

	private String getTcpClientInfo() {
		return this.tcpClient != null ? this.tcpClient.toString() : this.relayHost + ":" + this.relayPort;
	}


	private class RelayConnectionHandler implements StompTcpConnectionHandler<byte[]> {

		private final String sessionId;

		private final StompHeaderAccessor connectHeaders;

		private final boolean isRemoteClientSession;

		private final MessageChannel outboundChannel;

		@Nullable
		private volatile TcpConnection<byte[]> tcpConnection;

		private volatile boolean isStompConnected;

		private long clientSendInterval;

		@Nullable
		private final AtomicInteger clientSendMessageCount;

		private long clientSendMessageTimestamp;


		protected RelayConnectionHandler(String sessionId, StompHeaderAccessor connectHeaders) {
			this(sessionId, connectHeaders, true);
		}

		private RelayConnectionHandler(String sessionId, StompHeaderAccessor connectHeaders, boolean isClientSession) {
			Assert.notNull(sessionId, "'sessionId' must not be null");
			Assert.notNull(connectHeaders, "'connectHeaders' must not be null");
			this.sessionId = sessionId;
			this.connectHeaders = connectHeaders;
			this.isRemoteClientSession = isClientSession;
			this.outboundChannel = getClientOutboundChannelForSession(sessionId);
			if (isClientSession && taskScheduler != null) {
				this.clientSendInterval = connectHeaders.getHeartbeat()[0];
			}
			if (this.clientSendInterval > 0) {
				this.clientSendMessageCount = new AtomicInteger();
				this.clientSendMessageTimestamp = System.currentTimeMillis();
			}
			else {
				this.clientSendMessageCount = null;
			}
		}


		public String getSessionId() {
			return this.sessionId;
		}

		@Override
		public StompHeaderAccessor getConnectHeaders() {
			return this.connectHeaders;
		}

		@Nullable
		protected TcpConnection<byte[]> getTcpConnection() {
			return this.tcpConnection;
		}

		@Override
		public void afterConnected(TcpConnection<byte[]> connection) {
			if (logger.isDebugEnabled()) {
				logger.debug("TCP connection opened in session=" + getSessionId());
			}
			this.tcpConnection = connection;
			connection.onReadInactivity(() -> {
				if (this.tcpConnection != null && !this.isStompConnected) {
					handleTcpConnectionFailure("No CONNECTED frame received in " +
							MAX_TIME_TO_CONNECTED_FRAME + " ms.", null);
				}
			}, MAX_TIME_TO_CONNECTED_FRAME);
			connection.send(MessageBuilder.createMessage(EMPTY_PAYLOAD, this.connectHeaders.getMessageHeaders()));
		}

		@Override
		public void afterConnectFailure(Throwable ex) {
			handleTcpConnectionFailure("Failed to connect: " + ex.getMessage(), ex);
		}

		/**
		 * 检测到任何 TCP 连接问题时调用，即无法建立 TCP 连接、无法发送消息、错过心跳等。
		 */
		protected void handleTcpConnectionFailure(String error, @Nullable Throwable ex) {
			if (logger.isInfoEnabled()) {
				logger.info("TCP connection failure in session " + this.sessionId + ": " + error, ex);
			}
			try {
				sendStompErrorFrameToClient(error);
			}
			finally {
				try {
					clearConnection();
				}
				catch (Throwable ex2) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failure while clearing TCP connection state in session " + this.sessionId, ex2);
					}
				}
			}
		}

		private void sendStompErrorFrameToClient(String errorText) {
			if (this.isRemoteClientSession) {
				StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
				if (getHeaderInitializer() != null) {
					getHeaderInitializer().initHeaders(accessor);
				}
				accessor.setSessionId(this.sessionId);
				Principal user = this.connectHeaders.getUser();
				if (user != null) {
					accessor.setUser(user);
				}
				accessor.setMessage(errorText);
				accessor.setLeaveMutable(true);
				Message<?> errorMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
				handleInboundMessage(errorMessage);
			}
		}

		protected void handleInboundMessage(Message<?> message) {
			if (this.isRemoteClientSession) {
				this.outboundChannel.send(message);
			}
		}

		@Override
		public void handleMessage(Message<byte[]> message) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			Assert.state(accessor != null, "No StompHeaderAccessor");
			accessor.setSessionId(this.sessionId);
			Principal user = this.connectHeaders.getUser();
			if (user != null) {
				accessor.setUser(user);
			}

			StompCommand command = accessor.getCommand();
			if (StompCommand.CONNECTED.equals(command)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Received " + accessor.getShortLogMessage(EMPTY_PAYLOAD));
				}
				afterStompConnected(accessor);
			}
			else if (logger.isErrorEnabled() && StompCommand.ERROR.equals(command)) {
				logger.error("Received " + accessor.getShortLogMessage(message.getPayload()));
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Received " + accessor.getDetailedLogMessage(message.getPayload()));
			}

			handleInboundMessage(message);
		}

		/**
		 * 收到 STOMP CONNECTED 帧后调用。此时连接已准备好向代理发送 STOMP 消息。
		 */
		protected void afterStompConnected(StompHeaderAccessor connectedHeaders) {
			this.isStompConnected = true;
			stats.incrementConnectedCount();
			initHeartbeats(connectedHeaders);
		}

		protected void initHeartbeats(StompHeaderAccessor connectedHeaders) {
			if (taskScheduler != null) {
				long interval = connectedHeaders.getHeartbeat()[1];
				this.clientSendInterval = Math.max(interval, this.clientSendInterval);
			}
		}

		/**
		 * 是否转发心跳消息以代替具有非代理目标的消息。如果预期客户端心跳，
		 * 且当前心跳周期内没有其他消息，则执行此操作。
		 * @since 5.3
		 */
		protected boolean shouldSendHeartbeatForIgnoredMessage() {
			return (this.clientSendMessageCount != null && this.clientSendMessageCount.get() == 0);
		}

		/**
		 * 如果当前心跳周期已过期，则重置 clientSendMessageCount。
		 * @since 5.3
		 */
		void updateClientSendMessageCount(long now) {
			if (this.clientSendMessageCount != null && this.clientSendInterval > (now - this.clientSendMessageTimestamp)) {
				this.clientSendMessageCount.set(0);
				this.clientSendMessageTimestamp = now;
			}
		}

		@Override
		public void handleFailure(Throwable ex) {
			if (this.tcpConnection != null) {
				handleTcpConnectionFailure("Transport failure: " + ex.getMessage(), ex);
			}
			else if (logger.isErrorEnabled()) {
				logger.error("Transport failure: " + ex);
			}
		}

		@Override
		public void afterConnectionClosed() {
			if (this.tcpConnection == null) {
				return;
			}
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("TCP connection to broker closed in session " + this.sessionId);
				}
				sendStompErrorFrameToClient("Connection to broker closed.");
			}
			finally {
				try {
					// Prevent clearConnection() from trying to close
					this.tcpConnection = null;
					clearConnection();
				}
				catch (Throwable ex) {
					// Shouldn't happen with connection reset beforehand
				}
			}
		}

		/**
		 * 将给定消息转发到 STOMP 代理。
		 * <p>该方法检查我们是否有活跃的 TCP 连接，以及是否已收到 STOMP CONNECTED 帧。
		 * 对于客户端消息，只有在客户端消息被转发的同时丢失 TCP 连接时才会出现 false，
		 * 因此我们只需在 debug 级别记录被忽略的消息。对于从应用程序内部通过
		 * “系统”连接发送的消息，会抛出异常，以便发送消息的组件有机会处理它
		 * —— 默认情况下代理消息通道是同步的。
		 * <p>注意，如果消息在 TCP 连接丢失的同一时间并发到达，
		 * 则在连接重置之前会有一小段时间，可能有一或多条消息溜过去并尝试转发。
		 * 与其同步以防这种情况，此方法只是让它们尝试并失败。对于客户端会话，
		 * 这可能会导致向下游发送额外的 STOMP ERROR 帧，但处理下游的代码
		 * 在这种情况下应是幂等的。
		 * @param message the message to send (never {@code null})
		 * @return a future to wait for the result
		 */
		@SuppressWarnings("unchecked")
		public ListenableFuture<Void> forward(final Message<?> message, final StompHeaderAccessor accessor) {
			TcpConnection<byte[]> conn = this.tcpConnection;

			if (!this.isStompConnected || conn == null) {
				if (this.isRemoteClientSession) {
					if (logger.isDebugEnabled()) {
						logger.debug("TCP connection closed already, ignoring " +
								accessor.getShortLogMessage(message.getPayload()));
					}
					return EMPTY_TASK;
				}
				else {
					throw new IllegalStateException("Cannot forward messages " +
							(conn != null ? "before STOMP CONNECTED. " : "while inactive. ") +
							"Consider subscribing to receive BrokerAvailabilityEvent's from " +
							"an ApplicationListener Spring bean. Dropped " +
							accessor.getShortLogMessage(message.getPayload()));
				}
			}

			if (this.clientSendMessageCount != null) {
				this.clientSendMessageCount.incrementAndGet();
			}

			final Message<?> messageToSend = (accessor.isMutable() && accessor.isModified()) ?
					MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders()) : message;

			StompCommand command = accessor.getCommand();
			if (logger.isDebugEnabled() && (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command) ||
					StompCommand.UNSUBSCRIBE.equals(command) || StompCommand.DISCONNECT.equals(command))) {
				logger.debug("Forwarding " + accessor.getShortLogMessage(message.getPayload()));
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Forwarding " + accessor.getDetailedLogMessage(message.getPayload()));
			}

			ListenableFuture<Void> future = conn.send((Message<byte[]>) messageToSend);
			future.addCallback(new ListenableFutureCallback<Void>() {
				@Override
				public void onSuccess(@Nullable Void result) {
					if (accessor.getCommand() == StompCommand.DISCONNECT) {
						afterDisconnectSent(accessor);
					}
				}
				@Override
				public void onFailure(Throwable ex) {
					if (tcpConnection != null) {
						handleTcpConnectionFailure("failed to forward " +
								accessor.getShortLogMessage(message.getPayload()), ex);
					}
					else if (logger.isErrorEnabled()) {
						logger.error("Failed to forward " + accessor.getShortLogMessage(message.getPayload()));
					}
				}
			});
			return future;
		}

		/**
		 * DISCONNECT 之后不应再有客户端帧，因此我们可以主动关闭连接。
		 * 不过，如果 DISCONNECT 带有 receipt 头，则保持连接打开，
		 * 并期望服务器以 RECEIPT 响应，然后关闭连接。
		 * @see <a href="https://stomp.github.io/stomp-specification-1.2.html#DISCONNECT">
		 *     STOMP Specification 1.2 DISCONNECT</a>
		 */
		private void afterDisconnectSent(StompHeaderAccessor accessor) {
			if (accessor.getReceipt() == null) {
				try {
					clearConnection();
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failure while clearing TCP connection state in session " + this.sessionId, ex);
					}
				}
			}
		}

		/**
		 * 清理与连接关联的状态并关闭连接。
		 * 关闭连接时产生的任何异常都会被传播。
		 */
		public void clearConnection() {
			if (logger.isDebugEnabled()) {
				logger.debug("Cleaning up connection state for session " + this.sessionId);
			}

			if (this.isRemoteClientSession) {
				StompBrokerRelayMessageHandler.this.connectionHandlers.remove(this.sessionId);
			}

			this.isStompConnected = false;

			TcpConnection<byte[]> conn = this.tcpConnection;
			this.tcpConnection = null;
			if (conn != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Closing TCP connection in session " + this.sessionId);
				}
				conn.close();
			}
		}

		@Override
		public String toString() {
			return "StompConnectionHandler[sessionId=" + this.sessionId + "]";
		}
	}


	private class SystemSessionConnectionHandler extends RelayConnectionHandler {

		public SystemSessionConnectionHandler(StompHeaderAccessor connectHeaders) {
			super(SYSTEM_SESSION_ID, connectHeaders, false);
		}

		@Override
		protected void afterStompConnected(StompHeaderAccessor connectedHeaders) {
			if (logger.isInfoEnabled()) {
				logger.info("\"System\" session connected.");
			}
			super.afterStompConnected(connectedHeaders);
			publishBrokerAvailableEvent();
			sendSystemSubscriptions();
		}

		protected void initHeartbeats(StompHeaderAccessor connectedHeaders) {
			TcpConnection<byte[]> con = getTcpConnection();
			Assert.state(con != null, "No TcpConnection available");

			long clientSendInterval = getConnectHeaders().getHeartbeat()[0];
			long clientReceiveInterval = getConnectHeaders().getHeartbeat()[1];
			long serverSendInterval = connectedHeaders.getHeartbeat()[0];
			long serverReceiveInterval = connectedHeaders.getHeartbeat()[1];

			if (clientSendInterval > 0 && serverReceiveInterval > 0) {
				long interval = Math.max(clientSendInterval, serverReceiveInterval);
				con.onWriteInactivity(() ->
						con.send(HEARTBEAT_MESSAGE).addCallback(
								result -> {},
								ex -> handleTcpConnectionFailure(
										"Failed to forward heartbeat: " + ex.getMessage(), ex)), interval);
			}
			if (clientReceiveInterval > 0 && serverSendInterval > 0) {
				final long interval = Math.max(clientReceiveInterval, serverSendInterval) * HEARTBEAT_MULTIPLIER;
				con.onReadInactivity(
						() -> handleTcpConnectionFailure("No messages received in " + interval + " ms.", null), interval);
			}
		}

		private void sendSystemSubscriptions() {
			int i = 0;
			for (String destination : getSystemSubscriptions().keySet()) {
				StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
				accessor.setSubscriptionId(String.valueOf(i++));
				accessor.setDestination(destination);
				if (logger.isDebugEnabled()) {
					logger.debug("Subscribing to " + destination + " on \"system\" connection.");
				}
				TcpConnection<byte[]> conn = getTcpConnection();
				if (conn != null) {
					MessageHeaders headers = accessor.getMessageHeaders();
					conn.send(MessageBuilder.createMessage(EMPTY_PAYLOAD, headers)).addCallback(
							result -> {},
							ex -> {
								String error = "Failed to subscribe in \"system\" session.";
								handleTcpConnectionFailure(error, ex);
							});
				}
			}
		}

		@Override
		protected void handleInboundMessage(Message<?> message) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			if (accessor != null && StompCommand.MESSAGE.equals(accessor.getCommand())) {
				String destination = accessor.getDestination();
				if (destination == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Got message on \"system\" connection, with no destination: " +
								accessor.getDetailedLogMessage(message.getPayload()));
					}
					return;
				}
				if (!getSystemSubscriptions().containsKey(destination)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Got message on \"system\" connection with no handler: " +
								accessor.getDetailedLogMessage(message.getPayload()));
					}
					return;
				}
				try {
					MessageHandler handler = getSystemSubscriptions().get(destination);
					handler.handleMessage(message);
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Error while handling message on \"system\" connection.", ex);
					}
				}
			}
		}

		@Override
		protected void handleTcpConnectionFailure(String errorMessage, @Nullable Throwable ex) {
			super.handleTcpConnectionFailure(errorMessage, ex);
			publishBrokerUnavailableEvent();
		}

		@Override
		public void afterConnectionClosed() {
			super.afterConnectionClosed();
			publishBrokerUnavailableEvent();
		}

		@Override
		public ListenableFuture<Void> forward(Message<?> message, StompHeaderAccessor accessor) {
			try {
				ListenableFuture<Void> future = super.forward(message, accessor);
				if (message.getHeaders().get(SimpMessageHeaderAccessor.IGNORE_ERROR) == null) {
					future.get();
				}
				return future;
			}
			catch (Throwable ex) {
				throw new MessageDeliveryException(message, ex);
			}
		}

		@Override
		protected boolean shouldSendHeartbeatForIgnoredMessage() {
			return false;
		}
	}


	private class ClientSendMessageCountTask implements Runnable {

		@Override
		public void run() {
			long now = System.currentTimeMillis();
			for (RelayConnectionHandler handler : connectionHandlers.values()) {
				handler.updateClientSendMessageCount(now);
			}
		}
	}



	private static class VoidCallable implements Callable<Void> {

		@Override
		public Void call() {
			return null;
		}
	}


	/**
	 * 用于访问会话计数器的契约。
	 * @since 5.2
	 */
	public interface Stats {

		/**
	 * 连接处理器的数量。
	 */
		int getTotalHandlers();

		/**
	 * 已处理的 CONNECT 帧数量。
	 */
		int getTotalConnect();

		/**
	 * 已处理的 CONNECTED 帧数量。
	 */
		int getTotalConnected();

		/**
	 * 已处理的 DISCONNECT 帧数量。
	 */
		int getTotalDisconnect();
	}


	private class DefaultStats implements Stats {

		private final AtomicInteger connect = new AtomicInteger();

		private final AtomicInteger connected = new AtomicInteger();

		private final AtomicInteger disconnect = new AtomicInteger();

		public void incrementConnectCount() {
			this.connect.incrementAndGet();
		}

		public void incrementConnectedCount() {
			this.connected.incrementAndGet();
		}

		public void incrementDisconnectCount() {
			this.disconnect.incrementAndGet();
		}

		@Override
		public int getTotalHandlers() {
			return connectionHandlers.size();
		}

		@Override
		public int getTotalConnect() {
			return this.connect.get();
		}

		@Override
		public int getTotalConnected() {
			return this.connected.get();
		}

		@Override
		public int getTotalDisconnect() {
			return this.disconnect.get();
		}

		@Override
		public String toString() {
			return (connectionHandlers.size() + " sessions, " + getTcpClientInfo() +
					(isBrokerAvailable() ? " (available)" : " (not available)") +
					", processed CONNECT(" + this.connect.get() + ")-CONNECTED(" +
					this.connected.get() + ")-DISCONNECT(" + this.disconnect.get() + ")");
		}
	}

}
