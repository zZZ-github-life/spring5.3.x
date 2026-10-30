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

package org.springframework.web.socket;

/**
 * 可以在 WebSocket 连接上处理或发送的消息。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <T> 负载类型
 */
public interface WebSocketMessage<T> {

	/**
	 * 返回消息负载（永远不会是 {@code null}）。
	 */
	T getPayload();

	/**
	 * 返回消息中包含的字节数。
	 */
	int getPayloadLength();

	/**
	 * 当部分消息支持可用且通过
	 * {@link org.springframework.web.socket.WebSocketHandler#supportsPartialMessages()}
	 * 请求启用时，如果当前消息是客户端发送的完整 WebSocket 消息的最后一部分，
	 * 此方法返回 {@code true}。如果部分消息支持不可用或未启用，
	 * 则返回 {@code false}。
	 */
	boolean isLast();

}
