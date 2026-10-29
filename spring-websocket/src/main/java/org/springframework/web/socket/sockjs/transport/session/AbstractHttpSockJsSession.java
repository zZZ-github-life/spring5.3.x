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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpAsyncRequestControl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * 用于 HTTP 传输 SockJS 会话的抽象基类。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpSockJsSession extends AbstractSockJsSession {

	private final Queue<String> messageCache;

	@Nullable
	private volatile URI uri;

	@Nullable
	private volatile HttpHeaders handshakeHeaders;

	@Nullable
	private volatile Principal principal;

	@Nullable
	private volatile InetSocketAddress localAddress;

	@Nullable
	private volatile InetSocketAddress remoteAddress;

	@Nullable
	private volatile String acceptedProtocol;

	@Nullable
	private volatile ServerHttpResponse response;

	@Nullable
	private volatile SockJsFrameFormat frameFormat;

	@Nullable
	private volatile ServerHttpAsyncRequestControl asyncRequestControl;

	private boolean readyToSend;


	public AbstractHttpSockJsSession(String id, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(id, config, wsHandler, attributes);
		this.messageCache = new LinkedBlockingQueue<>(config.getHttpMessageCacheSize());
	}


	@Override
	public URI getUri() {
		URI uri = this.uri;
		Assert.state(uri != null, "No initial request yet");
		return uri;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		HttpHeaders headers = this.handshakeHeaders;
		Assert.state(headers != null, "No initial request yet");
		return headers;
	}

	@Override
	@Nullable
	public Principal getPrincipal() {
		return this.principal;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	/**
	 * 与 WebSocket 中子协议协商是初始握手的一部分不同，在 HTTP 传输中
	 * 必须模拟相同的协商，并通过此 setter 设置选定的协议。
	 * @param protocol the sub-protocol to set
	 */
	public void setAcceptedProtocol(@Nullable String protocol) {
		this.acceptedProtocol = protocol;
	}

	/**
	 * 返回要使用的已选子协议。
	 */
	@Override
	@Nullable
	public String getAcceptedProtocol() {
		return this.acceptedProtocol;
	}

	/**
	 * 返回在轮询请求之间透明存储的消息的 SockJS 缓冲区。如果轮询请求
	 * 超过 5 秒，则会话将被关闭。
	 * @see org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService
	 */
	protected Queue<String> getMessageCache() {
		return this.messageCache;
	}


	@Override
	public boolean isActive() {
		ServerHttpAsyncRequestControl control = this.asyncRequestControl;
		return (control != null && !control.isCompleted());
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		// ignore
	}

	@Override
	public int getTextMessageSizeLimit() {
		return -1;
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		// ignore
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		return -1;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		return Collections.emptyList();
	}


	/**
	 * 处理基于 SockJS HTTP 传输的会话中用于接收消息的第一个请求。
	 * <p>基于长轮询的传输（例如 "xhr"、"jsonp"）在写入 open 帧之后
	 * 完成请求。基于流式传输的传输（"xhr_streaming"、
	 * "eventsource" 和 "htmlfile"）会让响应保持打开更长时间以进一步
	 * 流式传输消息帧，但最终也会在发送一定量的数据后
	 * 关闭它。
	 * @param request the current request
	 * @param response the current response
	 * @param frameFormat the transport-specific SocksJS frame format to use
	 */
	public void handleInitialRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsFrameFormat frameFormat) throws SockJsException {

		this.uri = request.getURI();
		this.handshakeHeaders = request.getHeaders();
		this.principal = request.getPrincipal();
		try {
			this.localAddress = request.getLocalAddress();
		}
		catch (Exception ex) {
			// Ignore
		}
		try {
			this.remoteAddress = request.getRemoteAddress();
		}
		catch (Exception ex) {
			// Ignore
		}

		synchronized (this.responseLock) {
			try {
				this.response = response;
				this.frameFormat = frameFormat;
				ServerHttpAsyncRequestControl control = request.getAsyncRequestControl(response);
				this.asyncRequestControl = control;
				control.start(-1);
				disableShallowEtagHeaderFilter(request);
				// Let "our" handler know before sending the open frame to the remote handler
				delegateConnectionEstablished();
				handleRequestInternal(request, response, true);
				// Request might have been reset (e.g. polling sessions do after writing)
				this.readyToSend = isActive();
			}
			catch (Throwable ex) {
				tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
				throw new SockJsTransportFailureException("Failed to open session", getId(), ex);
			}
		}
	}

	/**
	 * 处理除第一个请求之外的所有请求，以在基于 SockJS HTTP 传输的会话中
	 * 接收消息。
	 * <p>基于长轮询的传输（例如 "xhr"、"jsonp"）在写入任何缓冲的消息帧
	 * （或下一个消息帧）之后完成请求。基于流式传输的传输（"xhr_streaming"、
	 * "eventsource" 和 "htmlfile"）会让响应保持打开更长时间以进一步
	 * 流式传输消息帧，但最终也会在发送一定量的数据后
	 * 关闭它。
	 * @param request the current request
	 * @param response the current response
	 * @param frameFormat the transport-specific SocksJS frame format to use
	 */
	public void handleSuccessiveRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsFrameFormat frameFormat) throws SockJsException {

		synchronized (this.responseLock) {
			try {
				if (isClosed()) {
					String formattedFrame = frameFormat.format(SockJsFrame.closeFrameGoAway());
					response.getBody().write(formattedFrame.getBytes(SockJsFrame.CHARSET));
					return;
				}
				this.response = response;
				this.frameFormat = frameFormat;
				ServerHttpAsyncRequestControl control = request.getAsyncRequestControl(response);
				this.asyncRequestControl = control;
				control.start(-1);
				disableShallowEtagHeaderFilter(request);
				handleRequestInternal(request, response, false);
				this.readyToSend = isActive();
			}
			catch (Throwable ex) {
				tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
				throw new SockJsTransportFailureException("Failed to handle SockJS receive request", getId(), ex);
			}
		}
	}

	private void disableShallowEtagHeaderFilter(ServerHttpRequest request) {
		if (request instanceof ServletServerHttpRequest) {
			ServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
			ShallowEtagHeaderFilter.disableContentCaching(servletRequest);
		}
	}

	/**
	 * 收到 SockJS 传输请求时调用。
	 * @param request the current request
	 * @param response the current response
	 * @param initialRequest whether it is the first request for the session
	 */
	protected abstract void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			boolean initialRequest) throws IOException;

	@Override
	protected final void sendMessageInternal(String message) throws SockJsTransportFailureException {
		synchronized (this.responseLock) {
			this.messageCache.add(message);
			if (logger.isTraceEnabled()) {
				logger.trace(this.messageCache.size() + " message(s) to flush in session " + getId());
			}
			if (isActive() && this.readyToSend) {
				if (logger.isTraceEnabled()) {
					logger.trace("Session is active, ready to flush.");
				}
				cancelHeartbeat();
				flushCache();
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Session is not active, not ready to flush.");
				}
			}
		}
	}

	/**
	 * 当连接处于活动状态并准备好写入响应时调用。
	 * 子类只应在已获取 "responseLock" 的方法中调用此方法。
	 */
	protected abstract void flushCache() throws SockJsTransportFailureException;


	@Override
	protected void disconnect(CloseStatus status) {
		resetRequest();
	}

	protected void resetRequest() {
		synchronized (this.responseLock) {
			ServerHttpAsyncRequestControl control = this.asyncRequestControl;
			this.asyncRequestControl = null;
			this.readyToSend = false;
			this.response = null;
			updateLastActiveTime();
			if (control != null && !control.isCompleted() && control.isStarted()) {
				try {
					control.complete();
				}
				catch (Throwable ex) {
					// Could be part of normal workflow (e.g. browser tab closed)
					logger.debug("Failed to complete request: " + ex.getMessage());
				}
			}
		}
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws IOException {
		if (isActive()) {
			SockJsFrameFormat frameFormat = this.frameFormat;
			ServerHttpResponse response = this.response;
			if (frameFormat != null && response != null) {
				String formattedFrame = frameFormat.format(frame);
				if (logger.isTraceEnabled()) {
					logger.trace("Writing to HTTP response: " + formattedFrame);
				}
				response.getBody().write(formattedFrame.getBytes(SockJsFrame.CHARSET));
				response.flush();
			}
		}
	}

}
