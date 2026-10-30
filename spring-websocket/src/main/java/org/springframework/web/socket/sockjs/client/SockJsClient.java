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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * SockJS 的
 * {@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}
 * 实现，提供了通过纯 HTTP 流式传输和长轮询技术模拟 WebSocket 交互的
 * 回退替代方案。
 *
 * <p>实现 {@link Lifecycle} 以将生命周期事件传播给它所配置的
 * 传输。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see <a href="https://github.com/sockjs/sockjs-client">https://github.com/sockjs/sockjs-client</a>
 * @see org.springframework.web.socket.sockjs.client.Transport
 */
public class SockJsClient implements WebSocketClient, Lifecycle {

	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", SockJsClient.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(SockJsClient.class);

	private static final Set<String> supportedProtocols = new HashSet<>(4);

	static {
		supportedProtocols.add("ws");
		supportedProtocols.add("wss");
		supportedProtocols.add("http");
		supportedProtocols.add("https");
	}


	private final List<Transport> transports;

	@Nullable
	private String[] httpHeaderNames;

	private InfoReceiver infoReceiver;

	@Nullable
	private SockJsMessageCodec messageCodec;

	@Nullable
	private TaskScheduler connectTimeoutScheduler;

	private volatile boolean running;

	private final Map<URI, ServerInfo> serverInfoCache = new ConcurrentHashMap<>();


	/**
	 * 使用给定传输创建 {@code SockJsClient}。
	 * <p>如果列表包含 {@link XhrTransport}（或更具体地说，
	 * {@link InfoReceiver} 的实现），则该实例用于初始化
	 * {@link #setInfoReceiver(InfoReceiver) infoReceiver} 属性，否则
	 * 默认为 {@link RestTemplateXhrTransport}。
	 * @param transports 要使用的传输方式列表（非空）
	 */
	public SockJsClient(List<Transport> transports) {
		Assert.notEmpty(transports, "No transports provided");
		this.transports = new ArrayList<>(transports);
		this.infoReceiver = initInfoReceiver(transports);
		if (jackson2Present) {
			this.messageCodec = new Jackson2SockJsMessageCodec();
		}
	}

	private static InfoReceiver initInfoReceiver(List<Transport> transports) {
		for (Transport transport : transports) {
			if (transport instanceof InfoReceiver) {
				return ((InfoReceiver) transport);
			}
		}
		return new RestTemplateXhrTransport();
	}


	/**
	 * 应从每次调用 {@link SockJsClient#doHandshake(WebSocketHandler, WebSocketHttpHeaders, URI)}
	 * 的握手头中复制的 HTTP 头名称，并且还用于作为该 SockJS 连接一部分
	 * 发出的其他 HTTP 请求，例如初始 info 请求、XHR 发送或接收请求。
	 * <p>默认情况下，如果未设置此属性，所有握手头也会
	 * 用于其他 HTTP 请求。如果你希望只有一部分握手头
	 * （例如认证头）用于其他 HTTP 请求，请设置此属性。
	 * @param httpHeaderNames HTTP 头名称
	 */
	public void setHttpHeaderNames(@Nullable String... httpHeaderNames) {
		this.httpHeaderNames = httpHeaderNames;
	}

	/**
	 * 配置的 HTTP 头名称，将从握手头中复制，并包含在其他 HTTP 请求中。
	 */
	@Nullable
	public String[] getHttpHeaderNames() {
		return this.httpHeaderNames;
	}

	/**
	 * 配置用于在 SockJS 会话启动之前执行 SockJS "Info" 请求的
	 * {@code InfoReceiver}。
	 * <p>如果提供给构造函数的传输列表包含
	 * {@link XhrTransport} 或 {@link InfoReceiver} 的实现，则该
	 * 实例将用于初始化此属性，否则默认为
	 * {@link RestTemplateXhrTransport}。
	 * @param infoReceiver 用于 SockJS "Info" 请求的传输方式
	 */
	public void setInfoReceiver(InfoReceiver infoReceiver) {
		Assert.notNull(infoReceiver, "InfoReceiver is required");
		this.infoReceiver = infoReceiver;
	}

	/**
	 * 返回配置的 {@code InfoReceiver}（永不为 {@code null}）。
	 */
	public InfoReceiver getInfoReceiver() {
		return this.infoReceiver;
	}

	/**
	 * 设置要使用的 SockJsMessageCodec。
	 * <p>默认情况下，如果类路径上有 Jackson，则使用
	 * {@link org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec
	 * Jackson2SockJsMessageCodec}。
	 */
	public void setMessageCodec(SockJsMessageCodec messageCodec) {
		Assert.notNull(messageCodec, "SockJsMessageCodec is required");
		this.messageCodec = messageCodec;
	}

	/**
	 * 返回要使用的 SockJsMessageCodec。
	 */
	public SockJsMessageCodec getMessageCodec() {
		Assert.state(this.messageCodec != null, "No SockJsMessageCodec set");
		return this.messageCodec;
	}

	/**
	 * 配置一个 {@code TaskScheduler}，用于调度连接超时任务，
	 * 其中超时值基于初始 SockJS "Info" 请求的持续时间计算。
	 * 连接超时任务可确保更及时地进行回退，但除此之外完全是可选的。
	 * <p>默认情况下不配置此项，在这种情况下回退可能需要更长时间。
	 * @param connectTimeoutScheduler 要使用的任务调度器
	 */
	public void setConnectTimeoutScheduler(TaskScheduler connectTimeoutScheduler) {
		this.connectTimeoutScheduler = connectTimeoutScheduler;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			for (Transport transport : this.transports) {
				if (transport instanceof Lifecycle) {
					Lifecycle lifecycle = (Lifecycle) transport;
					if (!lifecycle.isRunning()) {
						lifecycle.start();
					}
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			for (Transport transport : this.transports) {
				if (transport instanceof Lifecycle) {
					Lifecycle lifecycle = (Lifecycle) transport;
					if (lifecycle.isRunning()) {
						lifecycle.stop();
					}
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public ListenableFuture<WebSocketSession> doHandshake(
			WebSocketHandler handler, String uriTemplate, Object... uriVars) {

		Assert.notNull(uriTemplate, "uriTemplate must not be null");
		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
		return doHandshake(handler, null, uri);
	}

	@Override
	public final ListenableFuture<WebSocketSession> doHandshake(
			WebSocketHandler handler, @Nullable WebSocketHttpHeaders headers, URI url) {

		Assert.notNull(handler, "WebSocketHandler is required");
		Assert.notNull(url, "URL is required");

		String scheme = url.getScheme();
		if (!supportedProtocols.contains(scheme)) {
			throw new IllegalArgumentException("Invalid scheme: '" + scheme + "'");
		}

		SettableListenableFuture<WebSocketSession> connectFuture = new SettableListenableFuture<>();
		try {
			SockJsUrlInfo sockJsUrlInfo = new SockJsUrlInfo(url);
			ServerInfo serverInfo = getServerInfo(sockJsUrlInfo, getHttpRequestHeaders(headers));
			createRequest(sockJsUrlInfo, headers, serverInfo).connect(handler, connectFuture);
		}
		catch (Exception exception) {
			if (logger.isErrorEnabled()) {
				logger.error("Initial SockJS \"Info\" request to server failed, url=" + url, exception);
			}
			connectFuture.setException(exception);
		}
		return connectFuture;
	}

	@Nullable
	private HttpHeaders getHttpRequestHeaders(@Nullable HttpHeaders webSocketHttpHeaders) {
		if (getHttpHeaderNames() == null || webSocketHttpHeaders == null) {
			return webSocketHttpHeaders;
		}
		else {
			HttpHeaders httpHeaders = new HttpHeaders();
			for (String name : getHttpHeaderNames()) {
				List<String> values = webSocketHttpHeaders.get(name);
				if (values != null) {
					httpHeaders.put(name, values);
				}
			}
			return httpHeaders;
		}
	}

	private ServerInfo getServerInfo(SockJsUrlInfo sockJsUrlInfo, @Nullable HttpHeaders headers) {
		URI infoUrl = sockJsUrlInfo.getInfoUrl();
		ServerInfo info = this.serverInfoCache.get(infoUrl);
		if (info == null) {
			long start = System.currentTimeMillis();
			String response = this.infoReceiver.executeInfoRequest(infoUrl, headers);
			long infoRequestTime = System.currentTimeMillis() - start;
			info = new ServerInfo(response, infoRequestTime);
			this.serverInfoCache.put(infoUrl, info);
		}
		return info;
	}

	private DefaultTransportRequest createRequest(
			SockJsUrlInfo urlInfo, @Nullable HttpHeaders headers, ServerInfo serverInfo) {

		List<DefaultTransportRequest> requests = new ArrayList<>(this.transports.size());
		for (Transport transport : this.transports) {
			for (TransportType type : transport.getTransportTypes()) {
				if (serverInfo.isWebSocketEnabled() || !TransportType.WEBSOCKET.equals(type)) {
					requests.add(new DefaultTransportRequest(urlInfo, headers, getHttpRequestHeaders(headers),
							transport, type, getMessageCodec()));
				}
			}
		}
		if (CollectionUtils.isEmpty(requests)) {
			throw new IllegalStateException(
					"No transports: " + urlInfo + ", webSocketEnabled=" + serverInfo.isWebSocketEnabled());
		}
		for (int i = 0; i < requests.size() - 1; i++) {
			DefaultTransportRequest request = requests.get(i);
			Principal user = getUser();
			if (user != null) {
				request.setUser(user);
			}
			if (this.connectTimeoutScheduler != null) {
				request.setTimeoutValue(serverInfo.getRetransmissionTimeout());
				request.setTimeoutScheduler(this.connectTimeoutScheduler);
			}
			request.setFallbackRequest(requests.get(i + 1));
		}
		return requests.get(0);
	}

	/**
	 * 返回要与 SockJS 会话关联并通过
	 * {@link org.springframework.web.socket.WebSocketSession#getPrincipal()} 提供的用户。
	 * <p>默认情况下，此方法返回 {@code null}。
	 * @return 要与会话关联的用户（可能为 {@code null}）
	 */
	@Nullable
	protected Principal getUser() {
		return null;
	}

	/**
	 * 默认情况下，SockJS "Info" 请求的结果（包括服务器是否禁用了 WebSocket
	 * 以及请求花费了多长时间，用于计算传输超时时间）会被缓存。此方法可用于
	 * 清除该缓存，从而使其重新填充。
	 */
	public void clearServerInfoCache() {
		this.serverInfoCache.clear();
	}


	/**
	 * 保存 SockJS "Info" 请求结果的简单值对象。
	 */
	private static class ServerInfo {

		private final boolean webSocketEnabled;

		private final long responseTime;

		public ServerInfo(String response, long responseTime) {
			this.responseTime = responseTime;
			this.webSocketEnabled = !response.matches(".*[\"']websocket[\"']\\s*:\\s*false.*");
		}

		public boolean isWebSocketEnabled() {
			return this.webSocketEnabled;
		}

		public long getRetransmissionTimeout() {
			return (this.responseTime > 100 ? 4 * this.responseTime : this.responseTime + 300);
		}
	}

}
