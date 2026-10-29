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

package org.springframework.web.reactive.socket.server.upgrade;

import java.net.URI;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * 与 Reactor Netty 配合使用的 {@link RequestUpgradeStrategy}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	private final Supplier<WebsocketServerSpec.Builder> specBuilderSupplier;

	@Nullable
	private Integer maxFramePayloadLength;

	@Nullable
	private Boolean handlePing;


	/**
	 * 使用默认的 {@link reactor.netty.http.server.WebsocketServerSpec.Builder}
	 * 创建一个实例。
	 * @since 5.2.6
	 */
	public ReactorNettyRequestUpgradeStrategy() {
		this(WebsocketServerSpec::builder);
	}


	/**
	 * 使用预先配置的 {@link reactor.netty.http.server.WebsocketServerSpec.Builder}
	 * 创建一个实例，用于 WebSocket 升级。
	 * @since 5.2.6
	 */
	public ReactorNettyRequestUpgradeStrategy(Supplier<WebsocketServerSpec.Builder> builderSupplier) {
		Assert.notNull(builderSupplier, "WebsocketServerSpec.Builder is required");
		this.specBuilderSupplier = builderSupplier;
	}


	/**
	 * 构建一个反映当前配置的 {@code WebsocketServerSpec} 实例。可用于检查已配置的
	 * 参数，但子协议除外，因为子协议取决于用于给定升级的 {@link WebSocketHandler}。
	 * @since 5.2.6
	 */
	public WebsocketServerSpec getWebsocketServerSpec() {
		return buildSpec(null);
	}

	WebsocketServerSpec buildSpec(@Nullable String subProtocol) {
		WebsocketServerSpec.Builder builder = this.specBuilderSupplier.get();
		if (subProtocol != null) {
			builder.protocols(subProtocol);
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
	 * @param maxFramePayloadLength the max length for frames.
	 * @since 5.1
	 * @deprecated as of 5.2.6 in favor of providing a supplier of
	 * {@link reactor.netty.http.server.WebsocketServerSpec.Builder} with a
	 * constructor argument
	 */
	@Deprecated
	public void setMaxFramePayloadLength(Integer maxFramePayloadLength) {
		this.maxFramePayloadLength = maxFramePayloadLength;
	}

	/**
	 * 返回配置的帧最大长度。
	 * @since 5.1
	 * @deprecated as of 5.2.6 in favor of {@link #getWebsocketServerSpec()}
	 */
	@Deprecated
	public int getMaxFramePayloadLength() {
		return getWebsocketServerSpec().maxFramePayloadLength();
	}

	/**
	 * 配置是否让 ping 帧传递到传递给 upgrade 方法的 {@link WebSocketHandler}
	 * 进行处理。默认情况下，Reactor Netty 会自动回复 pong 帧以响应 ping。
	 * 这在代理中允许 ping 和 pong 帧通过时很有用。
	 * <p>默认情况下此值为 {@code false}，此时 ping 帧由 Reactor Netty 自动处理。
	 * 如果设置为 {@code true}，ping 帧将传递给 {@link WebSocketHandler}。
	 * @param handlePing whether to let Ping frames through for handling
	 * @since 5.2.4
	 * @deprecated as of 5.2.6 in favor of providing a supplier of
	 * {@link reactor.netty.http.server.WebsocketServerSpec.Builder} with a
	 * constructor argument
	 */
	@Deprecated
	public void setHandlePing(boolean handlePing) {
		this.handlePing = handlePing;
	}

	/**
	 * 返回配置的 {@link #setHandlePing(boolean)}。
	 * @since 5.2.4
	 * @deprecated as of 5.2.6 in favor of {@link #getWebsocketServerSpec()}
	 */
	@Deprecated
	public boolean getHandlePing() {
		return getWebsocketServerSpec().handlePing();
	}


	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		ServerHttpResponse response = exchange.getResponse();
		HttpServerResponse reactorResponse = ServerHttpResponseDecorator.getNativeResponse(response);
		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		NettyDataBufferFactory bufferFactory = (NettyDataBufferFactory) response.bufferFactory();
		URI uri = exchange.getRequest().getURI();

		// Trigger WebFlux preCommit actions and upgrade
		return response.setComplete()
				.then(Mono.defer(() -> {
					WebsocketServerSpec spec = buildSpec(subProtocol);
					return reactorResponse.sendWebsocket((in, out) -> {
						ReactorNettyWebSocketSession session =
								new ReactorNettyWebSocketSession(
										in, out, handshakeInfo, bufferFactory, spec.maxFramePayloadLength());
						return handler.handle(session).checkpoint(uri + " [ReactorNettyRequestUpgradeStrategy]");
					}, spec);
				}));
	}

}
