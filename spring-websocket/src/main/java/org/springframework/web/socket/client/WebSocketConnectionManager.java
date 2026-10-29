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

package org.springframework.web.socket.client;

import java.util.List;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

/**
 * WebSocket {@link ConnectionManagerSupport 连接管理器}，通过
 * {@link WebSocketClient} 连接到服务器，并使用
 * {@link WebSocketHandler} 处理会话。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketConnectionManager extends ConnectionManagerSupport {

	private final WebSocketClient client;

	private final WebSocketHandler webSocketHandler;

	@Nullable
	private WebSocketSession webSocketSession;

	private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();


	public WebSocketConnectionManager(WebSocketClient client,
			WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVariables) {

		super(uriTemplate, uriVariables);
		this.client = client;
		this.webSocketHandler = decorateWebSocketHandler(webSocketHandler);
	}


	/**
	 * 设置要使用的子协议。如果进行了配置，指定的子协议将在握手时
	 * 通过 {@code Sec-WebSocket-Protocol} 头请求。生成的 WebSocket
	 * 会话将包含服务器接受的协议（如果有）。
	 */
	public void setSubProtocols(List<String> protocols) {
		this.headers.setSecWebSocketProtocol(protocols);
	}

	/**
	 * 返回配置的要使用的子协议。
	 */
	public List<String> getSubProtocols() {
		return this.headers.getSecWebSocketProtocol();
	}

	/**
	 * 设置要使用的源。
	 */
	public void setOrigin(@Nullable String origin) {
		this.headers.setOrigin(origin);
	}

	/**
	 * 返回配置的源。
	 */
	@Nullable
	public String getOrigin() {
		return this.headers.getOrigin();
	}

	/**
	 * 提供要添加到 WebSocket 握手请求中的默认头。
	 */
	public void setHeaders(HttpHeaders headers) {
		this.headers.clear();
		this.headers.putAll(headers);
	}

	/**
	 * 返回 WebSocket 握手请求的默认头。
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}


	@Override
	public void startInternal() {
		if (this.client instanceof Lifecycle && !((Lifecycle) this.client).isRunning()) {
			((Lifecycle) this.client).start();
		}
		super.startInternal();
	}

	@Override
	public void stopInternal() throws Exception {
		if (this.client instanceof Lifecycle && ((Lifecycle) this.client).isRunning()) {
			((Lifecycle) this.client).stop();
		}
		super.stopInternal();
	}

	@Override
	public boolean isConnected() {
		return (this.webSocketSession != null && this.webSocketSession.isOpen());
	}

	@Override
	protected void openConnection() {
		if (logger.isInfoEnabled()) {
			logger.info("Connecting to WebSocket at " + getUri());
		}

		ListenableFuture<WebSocketSession> future =
				this.client.doHandshake(this.webSocketHandler, this.headers, getUri());

		future.addCallback(new ListenableFutureCallback<WebSocketSession>() {
			@Override
			public void onSuccess(@Nullable WebSocketSession result) {
				webSocketSession = result;
				logger.info("Successfully connected");
			}
			@Override
			public void onFailure(Throwable ex) {
				logger.error("Failed to connect", ex);
			}
		});
	}

	@Override
	protected void closeConnection() throws Exception {
		if (this.webSocketSession != null) {
			this.webSocketSession.close();
		}
	}

	/**
	 * 装饰提供给类构造函数的 WebSocketHandler。
	 * <p>默认添加 {@link LoggingWebSocketHandlerDecorator}。
	 */
	protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
		return new LoggingWebSocketHandlerDecorator(handler);
	}

}
