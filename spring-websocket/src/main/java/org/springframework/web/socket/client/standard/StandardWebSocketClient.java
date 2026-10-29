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

package org.springframework.web.socket.client.standard;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.WebSocketContainer;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.adapter.standard.WebSocketToStandardExtensionAdapter;
import org.springframework.web.socket.client.AbstractWebSocketClient;

/**
 * 基于标准 Java WebSocket API 的 WebSocketClient。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketClient extends AbstractWebSocketClient {

	private final WebSocketContainer webSocketContainer;

	private final Map<String,Object> userProperties = new HashMap<>();

	@Nullable
	private AsyncListenableTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


	/**
	 * 默认构造函数，调用 {@code ContainerProvider.getWebSocketContainer()}
	 * 以获取（新的）{@link WebSocketContainer} 实例。另请参见接受现有
	 * {@code WebSocketContainer} 实例的构造函数。
	 */
	public StandardWebSocketClient() {
		this.webSocketContainer = ContainerProvider.getWebSocketContainer();
	}

	/**
	 * 接受现有 {@link WebSocketContainer} 实例的构造函数。
	 * <p>对于 XML 配置，请参见 {@link WebSocketContainerFactoryBean}。
	 * 对于 Java 配置，请使用 {@code ContainerProvider.getWebSocketContainer()}
	 * 获取 {@code WebSocketContainer} 实例。
	 */
	public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
		Assert.notNull(webSocketContainer, "WebSocketContainer must not be null");
		this.webSocketContainer = webSocketContainer;
	}


	/**
	 * 标准 Java WebSocket API 允许通过
	 * {@link ClientEndpointConfig#getUserProperties() userProperties} 向服务器传递“用户属性”。
	 * 使用此属性可配置每次握手时要传递的一个或多个属性。
	 */
	public void setUserProperties(@Nullable Map<String, Object> userProperties) {
		if (userProperties != null) {
			this.userProperties.putAll(userProperties);
		}
	}

	/**
	 * 配置的用户属性。
	 */
	public Map<String, Object> getUserProperties() {
		return this.userProperties;
	}

	/**
	 * 设置用于打开连接的 {@link AsyncListenableTaskExecutor}。
	 * 如果此属性设置为 {@code null}，则对任何
	 * {@code doHandshake} 方法的调用都会阻塞，直到连接建立。
	 * <p>默认情况下使用 {@code SimpleAsyncTaskExecutor} 的实例。
	 */
	public void setTaskExecutor(@Nullable AsyncListenableTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 返回配置的 {@link TaskExecutor}。
	 */
	@Nullable
	public AsyncListenableTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	protected ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
			HttpHeaders headers, final URI uri, List<String> protocols,
			List<WebSocketExtension> extensions, Map<String, Object> attributes) {

		int port = getPort(uri);
		InetSocketAddress localAddress = new InetSocketAddress(getLocalHost(), port);
		InetSocketAddress remoteAddress = new InetSocketAddress(uri.getHost(), port);

		final StandardWebSocketSession session = new StandardWebSocketSession(headers,
				attributes, localAddress, remoteAddress);

		final ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
				.configurator(new StandardWebSocketClientConfigurator(headers))
				.preferredSubprotocols(protocols)
				.extensions(adaptExtensions(extensions)).build();

		endpointConfig.getUserProperties().putAll(getUserProperties());

		final Endpoint endpoint = new StandardWebSocketHandlerAdapter(webSocketHandler, session);

		Callable<WebSocketSession> connectTask = () -> {
			this.webSocketContainer.connectToServer(endpoint, endpointConfig, uri);
			return session;
		};

		if (this.taskExecutor != null) {
			return this.taskExecutor.submitListenable(connectTask);
		}
		else {
			ListenableFutureTask<WebSocketSession> task = new ListenableFutureTask<>(connectTask);
			task.run();
			return task;
		}
	}

	private static List<Extension> adaptExtensions(List<WebSocketExtension> extensions) {
		List<Extension> result = new ArrayList<>();
		for (WebSocketExtension extension : extensions) {
			result.add(new WebSocketToStandardExtensionAdapter(extension));
		}
		return result;
	}

	private InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException ex) {
			return InetAddress.getLoopbackAddress();
		}
	}

	private int getPort(URI uri) {
		if (uri.getPort() == -1) {
			String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);
			return ("wss".equals(scheme) ? 443 : 80);
		}
		return uri.getPort();
	}


	private class StandardWebSocketClientConfigurator extends Configurator {

		private final HttpHeaders headers;

		public StandardWebSocketClientConfigurator(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> requestHeaders) {
			requestHeaders.putAll(this.headers);
			if (logger.isTraceEnabled()) {
				logger.trace("Handshake request headers: " + requestHeaders);
			}
		}
		@Override
		public void afterResponse(HandshakeResponse response) {
			if (logger.isTraceEnabled()) {
				logger.trace("Handshake response headers: " + response.getHeaders());
			}
		}
	}

}
