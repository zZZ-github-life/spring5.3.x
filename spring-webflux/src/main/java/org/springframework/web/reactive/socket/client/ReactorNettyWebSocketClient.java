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

package org.springframework.web.reactive.socket.client;

import java.net.URI;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.netty.http.websocket.WebsocketInbound;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;

/**
 * 与 Reactor Netty 配合使用的 {@link WebSocketClient} 实现。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyWebSocketClient implements WebSocketClient {

	private static final Log logger = LogFactory.getLog(ReactorNettyWebSocketClient.class);


	private final HttpClient httpClient;

	private final Supplier<WebsocketClientSpec.Builder> specBuilderSupplier;

	@Nullable
	private Integer maxFramePayloadLength;

	@Nullable
	private Boolean handlePing;


	/**
	 * 默认构造函数。
	 */
	public ReactorNettyWebSocketClient() {
		this(HttpClient.create());
	}

	/**
	 * 接受现有 {@link HttpClient} 构建器并附带默认的
	 * {@link reactor.netty.http.client.WebsocketClientSpec.Builder} 的构造函数。
	 * @since 5.1
	 */
	public ReactorNettyWebSocketClient(HttpClient httpClient) {
		this(httpClient, WebsocketClientSpec.builder());
	}

	/**
	 * 接受现有 {@link HttpClient} 构建器以及预先配置的
	 * {@link reactor.netty.http.client.WebsocketClientSpec.Builder} 的构造函数。
	 * @since 5.3
	 */
	public ReactorNettyWebSocketClient(
			HttpClient httpClient, Supplier<WebsocketClientSpec.Builder> builderSupplier) {

		Assert.notNull(httpClient, "HttpClient is required");
		Assert.notNull(builderSupplier, "WebsocketClientSpec.Builder is required");
		this.httpClient = httpClient;
		this.specBuilderSupplier = builderSupplier;
	}


	/**
	 * 返回配置的 {@link HttpClient}。
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * 构建一个反映当前配置的 {@code WebsocketClientSpec} 实例。可用于检查已配置的
	 * 参数，但子协议除外，因为子协议取决于用于给定升级的 {@link WebSocketHandler}。
	 * @since 5.3
	 */
	public WebsocketClientSpec getWebsocketClientSpec() {
		return buildSpec(null);
	}

	private WebsocketClientSpec buildSpec(@Nullable String protocols) {
		WebsocketClientSpec.Builder builder = this.specBuilderSupplier.get();
		if (StringUtils.hasText(protocols)) {
			builder.protocols(protocols);
		}
		if (this.maxFramePayloadLength != null) {
			builder.maxFramePayloadLength(this.maxFramePayloadLength);
		}
		if (this.handlePing != null) {
			builder.handlePing(this.handlePing);
		}
		return builder.build();
	}

	/**
	 * 配置允许的最大帧负载长度。将此值设置为应用程序所需的值，可以减少利用长数据帧
	 * 进行的拒绝服务攻击。
	 * <p>对应于 Netty 中
	 * {@link io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
	 * WebSocketServerHandshakerFactory} 构造函数中同名参数。
	 * <p>默认设置为 65536 (64K)。
	 * @param maxFramePayloadLength 帧的最大长度。
	 * @since 5.2
	 * @deprecated as of 5.3 in favor of providing a supplier of
	 * {@link reactor.netty.http.client.WebsocketClientSpec.Builder} with a
	 * constructor argument
	 */
	@Deprecated
	public void setMaxFramePayloadLength(int maxFramePayloadLength) {
		this.maxFramePayloadLength = maxFramePayloadLength;
	}

	/**
	 * 返回配置的 {@link #setMaxFramePayloadLength(int) maxFramePayloadLength}。
	 * @since 5.2
	 * @deprecated as of 5.3 in favor of {@link #getWebsocketClientSpec()}
	 */
	@Deprecated
	public int getMaxFramePayloadLength() {
		return getWebsocketClientSpec().maxFramePayloadLength();
	}

	/**
	 * 配置是否让 ping 帧传递到传递给 execute 方法的 {@link WebSocketHandler}
	 * 进行处理。默认情况下，Reactor Netty 会自动回复 pong 帧以响应 ping。
	 * 这在代理中允许 ping 和 pong 帧通过时很有用。
	 * <p>默认情况下此值为 {@code false}，此时 ping 帧由 Reactor Netty 自动处理。
	 * 如果设置为 {@code true}，ping 帧将传递给 {@link WebSocketHandler}。
	 * @param handlePing 是否让 Ping 帧通过以便处理
	 * @since 5.2.4
	 * @deprecated as of 5.3 in favor of providing a supplier of
	 * {@link reactor.netty.http.client.WebsocketClientSpec.Builder} with a
	 * constructor argument
	 */
	@Deprecated
	public void setHandlePing(boolean handlePing) {
		this.handlePing = handlePing;
	}

	/**
	 * 返回配置的 {@link #setHandlePing(boolean)}。
	 * @since 5.2.4
	 * @deprecated as of 5.3 in favor of {@link #getWebsocketClientSpec()}
	 */
	@Deprecated
	public boolean getHandlePing() {
		return getWebsocketClientSpec().handlePing();
	}

	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders requestHeaders, WebSocketHandler handler) {
		String protocols = StringUtils.collectionToCommaDelimitedString(handler.getSubProtocols());
		return getHttpClient()
				.headers(nettyHeaders -> setNettyHeaders(requestHeaders, nettyHeaders))
				.websocket(buildSpec(protocols))
				.uri(url.toString())
				.handle((inbound, outbound) -> {
					HttpHeaders responseHeaders = toHttpHeaders(inbound);
					String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
					HandshakeInfo info = new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
					NettyDataBufferFactory factory = new NettyDataBufferFactory(outbound.alloc());
					WebSocketSession session = new ReactorNettyWebSocketSession(
							inbound, outbound, info, factory, getMaxFramePayloadLength());
					if (logger.isDebugEnabled()) {
						logger.debug("Started session '" + session.getId() + "' for " + url);
					}
					return handler.handle(session).checkpoint(url + " [ReactorNettyWebSocketClient]");
				})
				.doOnRequest(n -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Connecting to " + url);
					}
				})
				.next();
	}

	private void setNettyHeaders(HttpHeaders httpHeaders, io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
		httpHeaders.forEach(nettyHeaders::set);
	}

	private HttpHeaders toHttpHeaders(WebsocketInbound inbound) {
		HttpHeaders headers = new HttpHeaders();
		io.netty.handler.codec.http.HttpHeaders nettyHeaders = inbound.headers();
		nettyHeaders.forEach(entry -> {
			String name = entry.getKey();
			headers.put(name, nettyHeaders.getAll(name));
		});
		return headers;
	}

}
