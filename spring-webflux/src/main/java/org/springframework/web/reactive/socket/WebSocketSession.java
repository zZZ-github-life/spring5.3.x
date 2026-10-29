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

package org.springframework.web.reactive.socket;

import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

/**
 * 表示一个 WebSocket 会话。
 *
 * <p>使用 {@link WebSocketSession#receive() session.receive()} 组合入站消息流，
 * 使用 {@link WebSocketSession#send(Publisher) session.send(publisher)}
 * 提供出站消息流。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketSession {

	/**
	 * 返回会话的 id。
	 */
	String getId();

	/**
	 * 返回握手请求中的信息。
	 */
	HandshakeInfo getHandshakeInfo();

	/**
	 * 返回用于创建消息负载的 {@code DataBuffer} 工厂。
	 * @return the buffer factory for the session
	 */
	DataBufferFactory bufferFactory();

	/**
	 * 返回与 WebSocket 会话关联的属性 Map。
	 * @return a Map with the session attributes (never {@code null})
	 * @since 5.1
	 */
	Map<String, Object> getAttributes();

	/**
	 * 提供对入站消息流的访问。
	 * <p>当连接关闭时，此流会收到完成或错误信号。在典型的 {@link WebSocketHandler}
	 * 实现中，此流会组合到整体处理流程中，以便在连接关闭时结束处理。
	 * <p>有关如何处理会话的更多详细信息和示例，请参见 {@link WebSocketHandler}
	 * 的类级文档和参考手册。
	 */
	Flux<WebSocketMessage> receive();

	/**
	 * 提供一个出站消息源，写入这些消息，并返回一个 {@code Mono<Void>}，
	 * 当源完成且写入结束时该 Mono 完成。
	 * <p>有关如何处理会话的更多详细信息和示例，请参见 {@link WebSocketHandler}
	 * 的类级文档和参考手册。
	 */
	Mono<Void> send(Publisher<WebSocketMessage> messages);

	/**
	 * 底层连接是否已打开。
	 * @since 5.3.1
	 */
	boolean isOpen();

	/**
	 * 使用 {@link CloseStatus#NORMAL} 关闭 WebSocket 会话。
	 */
	default Mono<Void> close() {
		return close(CloseStatus.NORMAL);
	}

	/**
	 * 使用给定状态关闭 WebSocket 会话。
	 * @param status the close status
	 */
	Mono<Void> close(CloseStatus status);

	/**
	 * 提供对 {@code CloseStatus} 的访问，该状态表示会话是在本地还是远程关闭的；
	 * 如果会话在没有状态的情况下结束，则完成时不发出值。
	 * @since 5.3
	 */
	Mono<CloseStatus> closeStatus();

	// WebSocketMessage factory methods

	/**
	 * 使用会话的 {@link #bufferFactory()} 创建文本 {@link WebSocketMessage}
	 * 的工厂方法。
	 */
	WebSocketMessage textMessage(String payload);

	/**
	 * 使用会话的 {@link #bufferFactory()} 创建二进制 WebSocketMessage
	 * 的工厂方法。
	 */
	WebSocketMessage binaryMessage(Function<DataBufferFactory, DataBuffer> payloadFactory);

	/**
	 * 使用会话的 {@link #bufferFactory()} 创建 ping WebSocketMessage
	 * 的工厂方法。
	 */
	WebSocketMessage pingMessage(Function<DataBufferFactory, DataBuffer> payloadFactory);

	/**
	 * 使用会话的 {@link #bufferFactory()} 创建 pong WebSocketMessage
	 * 的工厂方法。
	 */
	WebSocketMessage pongMessage(Function<DataBufferFactory, DataBuffer> payloadFactory);

}
