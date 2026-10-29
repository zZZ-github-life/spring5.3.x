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

package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;

/**
 * 用于配置 SockJS 回退选项的辅助类，可与
 * {@link org.springframework.web.socket.config.annotation.EnableWebSocket} 和
 * {@link WebSocketConfigurer} 配置一起使用。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsServiceRegistration {

	@Nullable
	private TaskScheduler scheduler;

	@Nullable
	private String clientLibraryUrl;

	@Nullable
	private Integer streamBytesLimit;

	@Nullable
	private Boolean sessionCookieNeeded;

	@Nullable
	private Long heartbeatTime;

	@Nullable
	private Long disconnectDelay;

	@Nullable
	private Integer httpMessageCacheSize;

	@Nullable
	private Boolean webSocketEnabled;

	private final List<TransportHandler> transportHandlers = new ArrayList<>();

	private final List<TransportHandler> transportHandlerOverrides = new ArrayList<>();

	private final List<HandshakeInterceptor> interceptors = new ArrayList<>();

	private final List<String> allowedOrigins = new ArrayList<>();

	private final List<String> allowedOriginPatterns = new ArrayList<>();

	@Nullable
	private Boolean suppressCors;

	@Nullable
	private SockJsMessageCodec messageCodec;


	public SockJsServiceRegistration() {
	}


	/**
	 * 用于调度 SockJS 心跳的调度器实例。
	 */
	public SockJsServiceRegistration setTaskScheduler(TaskScheduler scheduler) {
		Assert.notNull(scheduler, "TaskScheduler is required");
		this.scheduler = scheduler;
		return this;
	}

	/**
	 * 没有原生跨域通信能力的传输方式（例如 "eventsource"、
	 * "htmlfile"）必须在不可见的 iframe 中从“外部”域获取一个简单页面，
	 * 以使 iframe 中的代码能够从 SockJS 服务器所在域运行。由于 iframe
	 * 需要加载 SockJS JavaScript 客户端库，此属性允许指定从何处加载它。
	 * <p>默认情况下，此属性设置为
	 * "https://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js"。不过也可以
	 * 将其设置为应用程序提供的 URL。
	 * <p>请注意，可以指定相对 URL，在这种情况下 URL 必须相对于
	 * iframe URL。例如，假设 SockJS 端点映射到 "/sockjs"，
	 * 生成的 iframe URL 为 "/sockjs/iframe.html"，那么相对 URL 必须以
	 * "../../" 开头，以便上溯到 SockJS 映射所在位置之上。
	 * 在基于前缀的 Servlet 映射情况下，可能还需要再上溯一层。
	 */
	public SockJsServiceRegistration setClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
		return this;
	}

	/**
	 * 流式传输会在客户端保存响应，并且不会释放弃已投递消息所占用的内存。
	 * 此类传输需要不时地回收连接。此属性设置在单个 HTTP 流式请求
	 * 被关闭之前可通过其发送的最小字节数。之后客户端将打开一个新的请求。
	 * 将此值设置为 1 会有效地禁用流式传输，并使流式传输的行为
	 * 类似于轮询传输。
	 * <p>默认值为 128K（即 128 * 1024）。
	 */
	public SockJsServiceRegistration setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
		return this;
	}

	/**
	 * SockJS 协议要求服务器在响应来自客户端的初始 "/info" 请求时，返回一个
	 * "cookie_needed" 布尔属性，用于指示应用程序要正常运行是否需要使用
	 * JSESSIONID cookie，例如为了负载均衡，或在 Java Servlet 容器中
	 * 为了使用 HTTP 会话。
	 *
	 * <p>这对支持 XDomainRequest（一种修改过的 AJAX/XHR，可以跨域执行请求，
	 * 但不发送任何 cookie）的 IE 8、9 尤为重要。在这些情况下，
	 * SockJS 客户端更倾向于使用 "iframe-htmlfile" 传输而不是
	 * "xdr-streaming"，以便能够发送 cookie。
	 *
	 * <p>默认值为 "true"，以尽可能提高应用程序在 IE 8、9 中
	 * 正确工作的可能性，同时支持 cookie（尤其是 JSESSIONID cookie）。
	 * 不过，如果不需要使用 cookie（和 HTTP 会话），
	 * 应用程序可以选择将其设置为 "false"。
	 */
	public SockJsServiceRegistration setSessionCookieNeeded(boolean sessionCookieNeeded) {
		this.sessionCookieNeeded = sessionCookieNeeded;
		return this;
	}

	/**
	 * 服务器未发送任何消息的时间量（毫秒），超过该时间后服务器应向客户端
	 * 发送心跳帧，以防止连接中断。
	 * <p>默认值为 25,000（25 秒）。
	 */
	public SockJsServiceRegistration setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
		return this;
	}

	/**
	 * 在客户端没有接收连接（即服务器可以通过其向客户端发送数据的活动连接）
	 * 的情况下，客户端被视为断开之前的时间量（毫秒）。
	 * <p>默认值为 5000。
	 */
	public SockJsServiceRegistration setDisconnectDelay(long disconnectDelay) {
		this.disconnectDelay = disconnectDelay;
		return this;
	}

	/**
	 * 会话在等待来自客户端的下一个 HTTP 轮询请求期间可以缓存的
	 * 服务器到客户端消息数量。所有 HTTP 传输都会使用此属性，
	 * 因为即使是流式传输也会定期回收 HTTP 请求。
	 * <p>HTTP 请求之间的时间间隔应相对较短，并且不会超过允许的断开延迟
	 * （参见 {@link #setDisconnectDelay(long)}），默认情况下为 5 秒。
	 * <p>默认大小为 100。
	 */
	public SockJsServiceRegistration setHttpMessageCacheSize(int httpMessageCacheSize) {
		this.httpMessageCacheSize = httpMessageCacheSize;
		return this;
	}

	/**
	 * 某些负载均衡器不支持 WebSocket。此选项可用于在服务器端
	 * 禁用 WebSocket 传输。
	 * <p>默认值为 "true"。
	 */
	public SockJsServiceRegistration setWebSocketEnabled(boolean webSocketEnabled) {
		this.webSocketEnabled = webSocketEnabled;
		return this;
	}

	public SockJsServiceRegistration setTransportHandlers(TransportHandler... handlers) {
		this.transportHandlers.clear();
		if (!ObjectUtils.isEmpty(handlers)) {
			this.transportHandlers.addAll(Arrays.asList(handlers));
		}
		return this;
	}

	public SockJsServiceRegistration setTransportHandlerOverrides(TransportHandler... handlers) {
		this.transportHandlerOverrides.clear();
		if (!ObjectUtils.isEmpty(handlers)) {
			this.transportHandlerOverrides.addAll(Arrays.asList(handlers));
		}
		return this;
	}

	public SockJsServiceRegistration setInterceptors(HandshakeInterceptor... interceptors) {
		this.interceptors.clear();
		if (!ObjectUtils.isEmpty(interceptors)) {
			this.interceptors.addAll(Arrays.asList(interceptors));
		}
		return this;
	}

	/**
	 * 配置允许的 {@code Origin} 头值。
	 * @since 4.1.2
	 */
	protected SockJsServiceRegistration setAllowedOrigins(String... allowedOrigins) {
		this.allowedOrigins.clear();
		if (!ObjectUtils.isEmpty(allowedOrigins)) {
			this.allowedOrigins.addAll(Arrays.asList(allowedOrigins));
		}
		return this;
	}

	/**
	 * 配置允许的 {@code Origin} 模式头值。
	 * @since 5.3.2
	 */
	protected SockJsServiceRegistration setAllowedOriginPatterns(String... allowedOriginPatterns) {
		this.allowedOriginPatterns.clear();
		if (!ObjectUtils.isEmpty(allowedOriginPatterns)) {
			this.allowedOriginPatterns.addAll(Arrays.asList(allowedOriginPatterns));
		}
		return this;
	}

	/**
	 * 此选项可用于禁用为 SockJS 请求自动添加 CORS 头。
	 * <p>默认值为 "false"。
	 * @since 5.3.23
	 */
	public SockJsServiceRegistration setSuppressCors(boolean suppressCors) {
		this.suppressCors = suppressCors;
		return this;
	}

	/**
	 * 此选项可用于禁用为 SockJS 请求自动添加 CORS 头。
	 * <p>默认值为 "false"。
	 * @since 4.1.2
	 * @deprecated as of 5.3.23, in favor of {@link #setSuppressCors(boolean)}
	 */
	@Deprecated
	public SockJsServiceRegistration setSupressCors(boolean suppressCors) {
		return this.setSuppressCors(suppressCors);
	}

	/**
	 * 用于编码和解码 SockJS 消息的编解码器。
	 * <p>默认使用 {@code Jackson2SockJsMessageCodec}，
	 * 这要求类路径上存在 Jackson 库。
	 * @param codec the codec to use.
	 * @since 4.1
	 */
	public SockJsServiceRegistration setMessageCodec(SockJsMessageCodec codec) {
		this.messageCodec = codec;
		return this;
	}

	protected SockJsService getSockJsService() {
		TransportHandlingSockJsService service = createSockJsService();
		service.setHandshakeInterceptors(this.interceptors);

		if (this.clientLibraryUrl != null) {
			service.setSockJsClientLibraryUrl(this.clientLibraryUrl);
		}
		if (this.streamBytesLimit != null) {
			service.setStreamBytesLimit(this.streamBytesLimit);
		}
		if (this.sessionCookieNeeded != null) {
			service.setSessionCookieNeeded(this.sessionCookieNeeded);
		}
		if (this.heartbeatTime != null) {
			service.setHeartbeatTime(this.heartbeatTime);
		}
		if (this.disconnectDelay != null) {
			service.setDisconnectDelay(this.disconnectDelay);
		}
		if (this.httpMessageCacheSize != null) {
			service.setHttpMessageCacheSize(this.httpMessageCacheSize);
		}
		if (this.webSocketEnabled != null) {
			service.setWebSocketEnabled(this.webSocketEnabled);
		}
		if (this.suppressCors != null) {
			service.setSuppressCors(this.suppressCors);
		}
		service.setAllowedOrigins(this.allowedOrigins);
		service.setAllowedOriginPatterns(this.allowedOriginPatterns);

		if (this.messageCodec != null) {
			service.setMessageCodec(this.messageCodec);
		}
		return service;
	}

	/**
	 * 返回 TaskScheduler（如果已配置）。
	 */
	@Nullable
	protected TaskScheduler getTaskScheduler() {
		return this.scheduler;
	}

	private TransportHandlingSockJsService createSockJsService() {
		Assert.state(this.scheduler != null, "No TaskScheduler available");
		Assert.state(this.transportHandlers.isEmpty() || this.transportHandlerOverrides.isEmpty(),
				"Specify either TransportHandlers or TransportHandler overrides, not both");
		return (!this.transportHandlers.isEmpty() ?
				new TransportHandlingSockJsService(this.scheduler, this.transportHandlers) :
				new DefaultSockJsService(this.scheduler, this.transportHandlerOverrides));
	}

}
