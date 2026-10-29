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

package org.springframework.web.socket;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * WebSocket 会话抽象。允许通过 WebSocket 连接发送消息并将其关闭。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface WebSocketSession extends Closeable {

	/**
	 * 返回唯一的会话标识符。
	 */
	String getId();

	/**
	 * 返回用于打开 WebSocket 连接的 URI。
	 */
	@Nullable
	URI getUri();

	/**
	 * 返回握手请求中使用的头信息（永远不会是 {@code null}）。
	 */
	HttpHeaders getHandshakeHeaders();

	/**
	 * 返回与 WebSocket 会话关联的属性映射。
	 * <p>在服务器端，该映射可以通过
	 * {@link org.springframework.web.socket.server.HandshakeInterceptor
	 * HandshakeInterceptor} 进行初始填充。在客户端，
	 * 该映射可以通过
	 * {@link org.springframework.web.socket.client.WebSocketClient
	 * WebSocketClient} 的握手方法进行填充。
	 * @return a Map with the session attributes (never {@code null})
	 */
	Map<String, Object> getAttributes();

	/**
	 * 返回包含已认证用户名称的 {@link java.security.Principal} 实例。
	 * <p>如果用户尚未通过认证，则该方法返回 <code>null</code>。
	 */
	@Nullable
	Principal getPrincipal();

	/**
	 * 返回接收请求的地址。
	 */
	@Nullable
	InetSocketAddress getLocalAddress();

	/**
	 * 返回远程客户端的地址。
	 */
	@Nullable
	InetSocketAddress getRemoteAddress();

	/**
	 * 返回协商后的子协议。
	 * @return the protocol identifier, or {@code null} if no protocol
	 * was specified or negotiated successfully
	 */
	@Nullable
	String getAcceptedProtocol();

	/**
	 * 配置传入文本消息的最大大小。
	 */
	void setTextMessageSizeLimit(int messageSizeLimit);

	/**
	 * 获取为传入文本消息配置的最大大小。
	 */
	int getTextMessageSizeLimit();

	/**
	 * 配置传入二进制消息的最大大小。
	 */
	void setBinaryMessageSizeLimit(int messageSizeLimit);

	/**
	 * 获取为传入二进制消息配置的最大大小。
	 */
	int getBinaryMessageSizeLimit();

	/**
	 * 确定协商后的扩展。
	 * @return the list of extensions, or an empty list if no extension
	 * was specified or negotiated successfully
	 */
	List<WebSocketExtension> getExtensions();

	/**
	 * 发送 WebSocket 消息：{@link TextMessage} 或 {@link BinaryMessage}。
	 * <p><strong>注意：</strong>底层标准 WebSocket 会话（JSR-356）不允许
	 * 并发发送。因此，发送必须同步进行。为确保这一点，
	 * 一种选择是使用
	 * {@link org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
	 * ConcurrentWebSocketSessionDecorator} 包装 {@code WebSocketSession}。
	 * @see org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
	 */
	void sendMessage(WebSocketMessage<?> message) throws IOException;

	/**
	 * 底层连接是否已打开。
	 */
	boolean isOpen();

	/**
	 * 以状态码 1000 关闭 WebSocket 连接，即等价于：
	 * <pre class="code">
	 * session.close(CloseStatus.NORMAL);
	 * </pre>
	 */
	@Override
	void close() throws IOException;

	/**
	 * 使用给定的关闭状态关闭 WebSocket 连接。
	 */
	void close(CloseStatus status) throws IOException;

}
