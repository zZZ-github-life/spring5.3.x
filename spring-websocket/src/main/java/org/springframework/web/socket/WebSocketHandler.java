/*
 * Copyright 2002-2013 the original author or authors.
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
 * WebSocket 消息和生命周期事件的处理程序。
 *
 * <p>建议此接口的实现尽可能在合适的地方就地处理异常，或者让异常向外冒泡，
 * 这种情况下默认会记录异常日志，并使用
 * {@link CloseStatus#SERVER_ERROR SERVER_ERROR(1011)} 关闭会话。
 * 异常处理策略由
 * {@link org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator
 * ExceptionWebSocketHandlerDecorator} 提供，可以通过使用不同的装饰器
 * 装饰 {@link WebSocketHandler} 来定制或替换它。
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0
 */
public interface WebSocketHandler {

	/**
	 * 在 WebSocket 协商成功、WebSocket 连接已打开并可以使用之后调用。
	 * @throws Exception this method can handle or propagate exceptions; see class-level
	 * Javadoc for details.
	 */
	void afterConnectionEstablished(WebSocketSession session) throws Exception;

	/**
	 * 当新的 WebSocket 消息到达时调用。
	 * @throws Exception this method can handle or propagate exceptions; see class-level
	 * Javadoc for details.
	 */
	void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception;

	/**
	 * 处理来自底层 WebSocket 消息传输的错误。
	 * @throws Exception this method can handle or propagate exceptions; see class-level
	 * Javadoc for details.
	 */
	void handleTransportError(WebSocketSession session, Throwable exception) throws Exception;

	/**
	 * 在 WebSocket 连接被任一方关闭之后，或者发生传输错误之后调用。
	 * 尽管根据底层实现的不同，会话在技术上可能仍处于打开状态，
	 * 但此时不鼓励发送消息，且很可能不会成功。
	 * @throws Exception this method can handle or propagate exceptions; see class-level
	 * Javadoc for details.
	 */
	void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception;

	/**
	 * WebSocketHandler 是否处理部分消息。如果此标志设置为
	 * {@code true} 且底层 WebSocket 服务器支持部分消息，
	 * 那么一条较大的 WebSocket 消息或大小未知的消息可能被拆分，
	 * 并可能通过多次调用
	 * {@link #handleMessage(WebSocketSession, WebSocketMessage)} 接收。
	 * 标志
	 * {@link org.springframework.web.socket.WebSocketMessage#isLast()} 指示
	 * 该消息是否为部分消息以及是否为最后一部分。
	 */
	boolean supportsPartialMessages();

}
