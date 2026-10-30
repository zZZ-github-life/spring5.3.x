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

package org.springframework.web.socket.messaging;

import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 作为更高级别协议的一部分处理 WebSocket 消息的契约，该协议在 WebSocket
 * RFC 规范中称为“子协议”。
 * 既处理来自客户端的 {@link WebSocketMessage WebSocketMessage}，
 * 也处理发往客户端的 {@link Message Message}。
 *
 * <p>此接口的实现可以配置在
 * {@link SubProtocolWebSocketHandler} 上，后者根据客户端通过
 * {@code Sec-WebSocket-Protocol} 请求头请求的子协议，
 * 选择一个子协议处理器来委托消息处理。
 *
 * @author Andy Wilkinson
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SubProtocolHandler {

	/**
	 * 返回此处理器支持的子协议列表（永远不会是 {@code null}）。
	 */
	List<String> getSupportedProtocols();

	/**
	 * 处理从客户端接收到的给定 {@link WebSocketMessage}。
	 * @param session 客户端会话
	 * @param message 客户端消息
	 * @param outputChannel 用于发送消息的输出通道
	 */
	void handleMessageFromClient(WebSocketSession session, WebSocketMessage<?> message, MessageChannel outputChannel)
			throws Exception;

	/**
	 * 处理发往与给定 WebSocket 会话关联的客户端的给定 {@link Message}。
	 * @param session 客户端会话
	 * @param message 客户端消息
	 */
	void handleMessageToClient(WebSocketSession session, Message<?> message) throws Exception;

	/**
	 * 从给定消息中解析会话 ID，或返回 {@code null}。
	 * @param message 用于解析会话 ID 的消息
	 */
	@Nullable
	String resolveSessionId(Message<?> message);

	/**
	 * 在 {@link WebSocketSession} 启动后调用。
	 * @param session 客户端会话
	 * @param outputChannel 一个通道
	 */
	void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) throws Exception;

	/**
	 * 在 {@link WebSocketSession} 结束后调用。
	 * @param session 客户端会话
	 * @param closeStatus 会话关闭的原因
	 * @param outputChannel 一个通道
	 */
	void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel)
			throws Exception;

}
