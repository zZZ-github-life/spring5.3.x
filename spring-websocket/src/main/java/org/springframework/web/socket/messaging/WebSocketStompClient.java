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

package org.springframework.web.socket.messaging;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.BufferingStompDecoder;
import org.springframework.messaging.simp.stomp.ConnectionHandlingStompSession;
import org.springframework.messaging.simp.stomp.StompClientSupport;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 基于 WebSocket 的 STOMP 客户端，使用
 * {@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}
 * （包括 {@link org.springframework.web.socket.sockjs.client.SockJsClient
 * SockJsClient}）的实现进行连接。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class WebSocketStompClient extends StompClientSupport implements SmartLifecycle {

	private static final Log logger = LogFactory.getLog(WebSocketStompClient.class);

	private final WebSocketClient webSocketClient;

	private int inboundMessageSizeLimit = 64 * 1024;

	private boolean autoStartup = true;

	private int phase = DEFAULT_PHASE;

	private volatile boolean running;


	/**
	 * 类构造函数。将 {@link #setDefaultHeartbeat} 设置为 "0,0"，
	 * 但当配置了 {@link #setTaskScheduler} 时，
	 * 会将其重置回首选的 "10000,10000"。
	 * @param webSocketClient the WebSocket client to connect with
	 */
	public WebSocketStompClient(WebSocketClient webSocketClient) {
		Assert.notNull(webSocketClient, "WebSocketClient is required");
		this.webSocketClient = webSocketClient;
		setDefaultHeartbeat(new long[] {0, 0});
	}


	/**
	 * 返回配置的 WebSocketClient。
	 */
	public WebSocketClient getWebSocketClient() {
		return this.webSocketClient;
	}

	/**
	 * {@inheritDoc}
	 * <p>如果 {@link #setDefaultHeartbeat defaultHeartbeat} 属性当前为 "0,0"，
	 * 还会自动将其设置为 "10000,10000"。
	 */
	@Override
	public void setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		if (!isDefaultHeartbeatEnabled()) {
			setDefaultHeartbeat(new long[] {10000, 10000});
		}
		super.setTaskScheduler(taskScheduler);
	}

	/**
	 * 配置入站 STOMP 消息所允许的最大大小。
	 * 由于一条 STOMP 消息可以通过多条 WebSocket 消息接收，
	 * 因此可能需要缓冲，此属性决定每条消息的最大缓冲区大小。
	 * <p>默认情况下设置为 64 * 1024（64K）。
	 */
	public void setInboundMessageSizeLimit(int inboundMessageSizeLimit) {
		this.inboundMessageSizeLimit = inboundMessageSizeLimit;
	}

	/**
	 * 获取配置的入站消息缓冲区大小（字节）。
	 */
	public int getInboundMessageSizeLimit() {
		return this.inboundMessageSizeLimit;
	}

	/**
	 * 设置当 Spring 上下文刷新后是否自动启动所包含的 WebSocketClient。
	 * <p>默认值为 "true"。
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * 返回 'autoStartup' 属性的值。如果为 "true"，此客户端将自动启动
	 * 和停止所包含的 WebSocketClient。
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定 WebSocket 客户端应启动以及随后关闭所处的阶段。启动顺序从低到高，
	 * 关闭顺序则相反。
	 * <p>默认情况下为 Integer.MAX_VALUE，表示 WebSocket 客户端尽可能晚地
	 * 启动，并尽可能早地停止。
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回配置的阶段。
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			if (getWebSocketClient() instanceof Lifecycle) {
				((Lifecycle) getWebSocketClient()).start();
			}
		}

	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			if (getWebSocketClient() instanceof Lifecycle) {
				((Lifecycle) getWebSocketClient()).stop();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	/**
	 * 连接到给定的 WebSocket URL，并在收到 CONNECTED 帧后于 STOMP 层面连接成功时
	 * 通知给定的
	 * {@link org.springframework.messaging.simp.stomp.StompSessionHandler}。
	 * @param url the url to connect to
	 * @param handler the session handler
	 * @param uriVars the URI variables to expand into the URL
	 * @return a ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(String url, StompSessionHandler handler, Object... uriVars) {
		return connect(url, null, handler, uriVars);
	}

	/**
	 * {@link #connect(String, StompSessionHandler, Object...)} 的重载版本，
	 * 还接受用于 WebSocket 握手的 {@link WebSocketHttpHeaders}。
	 * @param url the url to connect to
	 * @param handshakeHeaders the headers for the WebSocket handshake
	 * @param handler the session handler
	 * @param uriVariables the URI variables to expand into the URL
	 * @return a ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(String url, @Nullable WebSocketHttpHeaders handshakeHeaders,
			StompSessionHandler handler, Object... uriVariables) {

		return connect(url, handshakeHeaders, null, handler, uriVariables);
	}

	/**
	 * {@link #connect(String, StompSessionHandler, Object...)} 的重载版本，
	 * 还接受用于 WebSocket 握手的 {@link WebSocketHttpHeaders}
	 * 以及用于 STOMP CONNECT 帧的 {@link StompHeaders}。
	 * @param url the url to connect to
	 * @param handshakeHeaders headers for the WebSocket handshake
	 * @param connectHeaders headers for the STOMP CONNECT frame
	 * @param handler the session handler
	 * @param uriVariables the URI variables to expand into the URL
	 * @return a ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(String url, @Nullable WebSocketHttpHeaders handshakeHeaders,
			@Nullable StompHeaders connectHeaders, StompSessionHandler handler, Object... uriVariables) {

		Assert.notNull(url, "'url' must not be null");
		URI uri = UriComponentsBuilder.fromUriString(url).buildAndExpand(uriVariables).encode().toUri();
		return connect(uri, handshakeHeaders, connectHeaders, handler);
	}

	/**
	 * {@link #connect(String, WebSocketHttpHeaders, StompSessionHandler, Object...)}
	 * 的重载版本，接受一个完全准备好的 {@link java.net.URI}。
	 * @param url the url to connect to
	 * @param handshakeHeaders the headers for the WebSocket handshake
	 * @param connectHeaders headers for the STOMP CONNECT frame
	 * @param sessionHandler the STOMP session handler
	 * @return a ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(URI url, @Nullable WebSocketHttpHeaders handshakeHeaders,
			@Nullable StompHeaders connectHeaders, StompSessionHandler sessionHandler) {

		Assert.notNull(url, "'url' must not be null");
		ConnectionHandlingStompSession session = createSession(connectHeaders, sessionHandler);
		WebSocketTcpConnectionHandlerAdapter adapter = new WebSocketTcpConnectionHandlerAdapter(session);
		getWebSocketClient()
				.doHandshake(new LoggingWebSocketHandlerDecorator(adapter), handshakeHeaders, url)
				.addCallback(adapter);
		return session.getSessionFuture();
	}

	@Override
	protected StompHeaders processConnectHeaders(@Nullable StompHeaders connectHeaders) {
		connectHeaders = super.processConnectHeaders(connectHeaders);
		if (connectHeaders.isHeartbeatEnabled()) {
			Assert.state(getTaskScheduler() != null, "TaskScheduler must be set if heartbeats are enabled");
		}
		return connectHeaders;
	}


	/**
	 * 将 WebSocket 适配到 TcpConnectionHandler 和 TcpConnection 契约。
	 */
	private class WebSocketTcpConnectionHandlerAdapter implements ListenableFutureCallback<WebSocketSession>,
			WebSocketHandler, TcpConnection<byte[]> {

		private final TcpConnectionHandler<byte[]> connectionHandler;

		private final StompWebSocketMessageCodec codec = new StompWebSocketMessageCodec(getInboundMessageSizeLimit());

		@Nullable
		private volatile WebSocketSession session;

		private volatile long lastReadTime = -1;

		private volatile long lastWriteTime = -1;

		private final List<ScheduledFuture<?>> inactivityTasks = new ArrayList<>(2);

		public WebSocketTcpConnectionHandlerAdapter(TcpConnectionHandler<byte[]> connectionHandler) {
			Assert.notNull(connectionHandler, "TcpConnectionHandler must not be null");
			this.connectionHandler = connectionHandler;
		}

		// ListenableFutureCallback implementation: handshake outcome

		@Override
		public void onSuccess(@Nullable WebSocketSession webSocketSession) {
		}

		@Override
		public void onFailure(Throwable ex) {
			this.connectionHandler.afterConnectFailure(ex);
		}

		// WebSocketHandler implementation

		@Override
		public void afterConnectionEstablished(WebSocketSession session) {
			this.session = session;
			this.connectionHandler.afterConnected(this);
		}

		@Override
		public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) {
			this.lastReadTime = (this.lastReadTime != -1 ? System.currentTimeMillis() : -1);
			List<Message<byte[]>> messages;
			try {
				messages = this.codec.decode(webSocketMessage);
			}
			catch (Throwable ex) {
				this.connectionHandler.handleFailure(ex);
				return;
			}
			for (Message<byte[]> message : messages) {
				this.connectionHandler.handleMessage(message);
			}
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable ex) throws Exception {
			this.connectionHandler.handleFailure(ex);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
			cancelInactivityTasks();
			this.connectionHandler.afterConnectionClosed();
		}

		private void cancelInactivityTasks() {
			for (ScheduledFuture<?> task : this.inactivityTasks) {
				try {
					task.cancel(true);
				}
				catch (Throwable ex) {
					// Ignore
				}
			}
			this.lastReadTime = -1;
			this.lastWriteTime = -1;
			this.inactivityTasks.clear();
		}

		@Override
		public boolean supportsPartialMessages() {
			return false;
		}

		// TcpConnection implementation

		@Override
		public ListenableFuture<Void> send(Message<byte[]> message) {
			updateLastWriteTime();
			SettableListenableFuture<Void> future = new SettableListenableFuture<>();
			try {
				WebSocketSession session = this.session;
				Assert.state(session != null, "No WebSocketSession available");
				session.sendMessage(this.codec.encode(message, session.getClass()));
				future.set(null);
			}
			catch (Throwable ex) {
				future.setException(ex);
			}
			finally {
				updateLastWriteTime();
			}
			return future;
		}

		private void updateLastWriteTime() {
			long lastWriteTime = this.lastWriteTime;
			if (lastWriteTime != -1) {
				this.lastWriteTime = System.currentTimeMillis();
			}
		}

		@Override
		public void onReadInactivity(final Runnable runnable, final long duration) {
			Assert.state(getTaskScheduler() != null, "No TaskScheduler configured");
			this.lastReadTime = System.currentTimeMillis();
			this.inactivityTasks.add(getTaskScheduler().scheduleWithFixedDelay(() -> {
				if (System.currentTimeMillis() - this.lastReadTime > duration) {
					try {
						runnable.run();
					}
					catch (Throwable ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("ReadInactivityTask failure", ex);
						}
					}
				}
			}, duration / 2));
		}

		@Override
		public void onWriteInactivity(final Runnable runnable, final long duration) {
			Assert.state(getTaskScheduler() != null, "No TaskScheduler configured");
			this.lastWriteTime = System.currentTimeMillis();
			this.inactivityTasks.add(getTaskScheduler().scheduleWithFixedDelay(() -> {
				if (System.currentTimeMillis() - this.lastWriteTime > duration) {
					try {
						runnable.run();
					}
					catch (Throwable ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("WriteInactivityTask failure", ex);
						}
					}
				}
			}, duration / 2));
		}

		@Override
		public void close() {
			WebSocketSession session = this.session;
			if (session != null) {
				try {
					session.close();
				}
				catch (IOException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to close session: " + session.getId(), ex);
					}
				}
			}
		}
	}


	/**
	 * 编码和解码 STOMP WebSocket 消息。
	 */
	private static class StompWebSocketMessageCodec {

		private static final StompEncoder ENCODER = new StompEncoder();

		private static final StompDecoder DECODER = new StompDecoder();

		private final BufferingStompDecoder bufferingDecoder;

		public StompWebSocketMessageCodec(int messageSizeLimit) {
			this.bufferingDecoder = new BufferingStompDecoder(DECODER, messageSizeLimit);
		}

		public List<Message<byte[]>> decode(WebSocketMessage<?> webSocketMessage) {
			List<Message<byte[]>> result = Collections.emptyList();
			ByteBuffer byteBuffer;
			if (webSocketMessage instanceof TextMessage) {
				byteBuffer = ByteBuffer.wrap(((TextMessage) webSocketMessage).asBytes());
			}
			else if (webSocketMessage instanceof BinaryMessage) {
				byteBuffer = ((BinaryMessage) webSocketMessage).getPayload();
			}
			else {
				return result;
			}
			result = this.bufferingDecoder.decode(byteBuffer);
			if (result.isEmpty()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Incomplete STOMP frame content received, bufferSize=" +
							this.bufferingDecoder.getBufferSize() + ", bufferSizeLimit=" +
							this.bufferingDecoder.getBufferSizeLimit() + ".");
				}
			}
			return result;
		}

		public WebSocketMessage<?> encode(Message<byte[]> message, Class<? extends WebSocketSession> sessionType) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			Assert.notNull(accessor, "No StompHeaderAccessor available");
			byte[] payload = message.getPayload();
			byte[] bytes = ENCODER.encode(accessor.getMessageHeaders(), payload);

			boolean useBinary = (payload.length > 0  &&
					!(SockJsSession.class.isAssignableFrom(sessionType)) &&
					MimeTypeUtils.APPLICATION_OCTET_STREAM.isCompatibleWith(accessor.getContentType()));

			return (useBinary ? new BinaryMessage(bytes) : new TextMessage(bytes));
		}
	}

}
