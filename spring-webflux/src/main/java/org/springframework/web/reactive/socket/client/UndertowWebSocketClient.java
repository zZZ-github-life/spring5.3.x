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

package org.springframework.web.reactive.socket.client;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient.ConnectionBuilder;
import io.undertow.websockets.client.WebSocketClientNegotiation;
import io.undertow.websockets.core.WebSocketChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xnio.IoFuture;
import org.xnio.XnioWorker;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketSession;

/**
 * 基于 Undertow 的 {@link WebSocketClient} 实现。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowWebSocketClient implements WebSocketClient {

	private static final Log logger = LogFactory.getLog(UndertowWebSocketClient.class);

	private static final int DEFAULT_POOL_BUFFER_SIZE = 8192;


	private final XnioWorker worker;

	private ByteBufferPool byteBufferPool;

	private final Consumer<ConnectionBuilder> builderConsumer;


	/**
	 * 使用要传递给
	 * {@link io.undertow.websockets.client.WebSocketClient#connectionBuilder} 的
	 * {@link XnioWorker} 构造。
	 * @param worker 该 Xnio worker
	 */
	public UndertowWebSocketClient(XnioWorker worker) {
		this(worker, builder -> {
		});
	}

	/**
	 * 备用构造函数，可对每个 WebSocket 连接的 {@link ConnectionBuilder}
	 * 提供额外控制。
	 * @param worker 用于创建 {@code ConnectionBuilder} 的 Xnio worker
	 * @param builderConsumer 用于配置 {@code ConnectionBuilder} 的消费者
	 */
	public UndertowWebSocketClient(XnioWorker worker, Consumer<ConnectionBuilder> builderConsumer) {
		this(worker, new DefaultByteBufferPool(false, DEFAULT_POOL_BUFFER_SIZE), builderConsumer);
	}

	/**
	 * 备用构造函数，可对每个 WebSocket 连接的 {@link ConnectionBuilder}
	 * 提供额外控制。
	 * @param worker 用于创建 {@code ConnectionBuilder} 的 Xnio worker
	 * @param byteBufferPool 用于创建 {@code ConnectionBuilder} 的 ByteBufferPool
	 * @param builderConsumer 用于配置 {@code ConnectionBuilder} 的消费者
	 * @since 5.0.8
	 */
	public UndertowWebSocketClient(XnioWorker worker, ByteBufferPool byteBufferPool,
			Consumer<ConnectionBuilder> builderConsumer) {

		Assert.notNull(worker, "XnioWorker must not be null");
		Assert.notNull(byteBufferPool, "ByteBufferPool must not be null");
		this.worker = worker;
		this.byteBufferPool = byteBufferPool;
		this.builderConsumer = builderConsumer;
	}


	/**
	 * 返回配置的 {@link XnioWorker}。
	 */
	public XnioWorker getXnioWorker() {
		return this.worker;
	}

	/**
	 * 设置要传递给
	 * {@link io.undertow.websockets.client.WebSocketClient#connectionBuilder} 的
	 * {@link io.undertow.connector.ByteBufferPool ByteBufferPool}。
	 * <p>默认使用缓冲区大小为 8192 的间接
	 * {@link io.undertow.server.DefaultByteBufferPool}。
	 * @since 5.0.8
	 * @see #DEFAULT_POOL_BUFFER_SIZE
	 */
	public void setByteBufferPool(ByteBufferPool byteBufferPool) {
		Assert.notNull(byteBufferPool, "ByteBufferPool must not be null");
		this.byteBufferPool = byteBufferPool;
	}

	/**
	 * 返回此客户端当前用于新创建的 WebSocket 会话的
	 * {@link io.undertow.connector.ByteBufferPool}。
	 * @return 字节缓冲池
	 * @since 5.0.8
	 */
	public ByteBufferPool getByteBufferPool() {
		return this.byteBufferPool;
	}

	/**
	 * 返回配置的 <code>Consumer&lt;ConnectionBuilder&gt;</code>。
	 */
	public Consumer<ConnectionBuilder> getConnectionBuilderConsumer() {
		return this.builderConsumer;
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return executeInternal(url, headers, handler);
	}

	private Mono<Void> executeInternal(URI url, HttpHeaders headers, WebSocketHandler handler) {
		Sinks.Empty<Void> completion = Sinks.empty();
		return Mono.deferContextual(
				contextView -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Connecting to " + url);
					}
					List<String> protocols = handler.getSubProtocols();
					ConnectionBuilder builder = createConnectionBuilder(url);
					DefaultNegotiation negotiation = new DefaultNegotiation(protocols, headers, builder);
					builder.setClientNegotiation(negotiation);
					builder.connect().addNotifier(
							new IoFuture.HandlingNotifier<WebSocketChannel, Object>() {
								@Override
								public void handleDone(WebSocketChannel channel, Object attachment) {
									handleChannel(url, ContextWebSocketHandler.decorate(handler, contextView),
											completion, negotiation, channel);
								}
								@Override
								public void handleFailed(IOException ex, Object attachment) {
									// Ignore result: can't overflow, ok if not first or no one listens
									completion.tryEmitError(
											new IllegalStateException("Failed to connect to " + url, ex));
								}
							}, null);
					return completion.asMono();
				});
	}

	/**
	 * 为给定 URI 创建一个 {@link ConnectionBuilder}。
	 * <p>默认实现使用配置的 {@link #getXnioWorker() XnioWorker} 和
	 * {@link #getByteBufferPool() ByteBufferPool} 创建一个构建器，然后将其传递给
	 * 构造时提供的 {@link #getConnectionBuilderConsumer() consumer}。
	 */
	protected ConnectionBuilder createConnectionBuilder(URI url) {
		ConnectionBuilder builder = io.undertow.websockets.client.WebSocketClient
				.connectionBuilder(getXnioWorker(), getByteBufferPool(), url);
		this.builderConsumer.accept(builder);
		return builder;
	}

	private void handleChannel(URI url, WebSocketHandler handler, Sinks.Empty<Void> completionSink,
			DefaultNegotiation negotiation, WebSocketChannel channel) {

		HandshakeInfo info = createHandshakeInfo(url, negotiation);
		DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;
		UndertowWebSocketSession session = new UndertowWebSocketSession(channel, info, bufferFactory, completionSink);
		UndertowWebSocketHandlerAdapter adapter = new UndertowWebSocketHandlerAdapter(session);

		channel.getReceiveSetter().set(adapter);
		channel.resumeReceives();

		handler.handle(session)
				.checkpoint(url + " [UndertowWebSocketClient]")
				.subscribe(session);
	}

	private HandshakeInfo createHandshakeInfo(URI url, DefaultNegotiation negotiation) {
		HttpHeaders responseHeaders = negotiation.getResponseHeaders();
		String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
		return new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
	}


	private static final class DefaultNegotiation extends WebSocketClientNegotiation {

		private final HttpHeaders requestHeaders;

		private final HttpHeaders responseHeaders = new HttpHeaders();

		@Nullable
		private final WebSocketClientNegotiation delegate;

		public DefaultNegotiation(List<String> protocols, HttpHeaders requestHeaders,
				ConnectionBuilder connectionBuilder) {

			super(protocols, Collections.emptyList());
			this.requestHeaders = requestHeaders;
			this.delegate = connectionBuilder.getClientNegotiation();
		}

		public HttpHeaders getResponseHeaders() {
			return this.responseHeaders;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> headers) {
			headers.putAll(this.requestHeaders);
			if (this.delegate != null) {
				this.delegate.beforeRequest(headers);
			}
		}

		@Override
		public void afterRequest(Map<String, List<String>> headers) {
			this.responseHeaders.putAll(headers);
			if (this.delegate != null) {
				this.delegate.afterRequest(headers);
			}
		}
	}

}
