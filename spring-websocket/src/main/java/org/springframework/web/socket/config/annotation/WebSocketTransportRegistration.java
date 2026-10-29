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
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * 配置接收来自 WebSocket 客户端的消息以及向其发送消息的处理。
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
public class WebSocketTransportRegistration {

	@Nullable
	private Integer messageSizeLimit;

	@Nullable
	private Integer sendTimeLimit;

	@Nullable
	private Integer sendBufferSizeLimit;

	@Nullable
	private Integer timeToFirstMessage;

	private final List<WebSocketHandlerDecoratorFactory> decoratorFactories = new ArrayList<>(2);


	/**
	 * 配置入站子协议消息的最大大小，例如可能由多条 WebSocket 消息
	 * 聚合而成的 STOMP 帧。
	 * <p>默认值为 64K（即 64 * 1024）。
	 * <p><strong>注意：</strong>这与单条 WebSocket 消息的大小不同，
	 * 后者需要在 WebSocket 服务器级别进行配置。详情请参见参考文档。
	 */
	public WebSocketTransportRegistration setMessageSizeLimit(int messageSizeLimit) {
		this.messageSizeLimit = messageSizeLimit;
		return this;
	}

	/**
	 * 仅供内部使用的受保护访问器。
	 */
	@Nullable
	protected Integer getMessageSizeLimit() {
		return this.messageSizeLimit;
	}

	/**
	 * 配置时间限制（毫秒），即向 WebSocket 会话发送消息或在使用 SockJS
	 * 回退选项时写入 HTTP 响应所允许的最长时间。
	 * <p>通常，WebSocket 服务器期望发往单个 WebSocket 会话的消息
	 * 同一时间只由一个线程发送。使用 {@code @EnableWebSocketMessageBroker}
	 * 配置时会自动保证这一点。
	 * 如果消息发送较慢，或者至少比消息发送速率更慢，
	 * 后续消息将被缓冲，直到达到 {@code sendTimeLimit}
	 * 或 {@code sendBufferSizeLimit}，此时会清除会话状态
	 * 并尝试关闭会话。
	 * <p><strong>注意</strong>：会话时间限制仅在尝试发送附加消息时才会检查。
	 * 因此，如果只发送了一条消息并且它挂起了，则在发送另一条消息
	 * 或者底层物理套接字超时之前，会话不会超时。所以，这不能替代
	 * WebSocket 服务器或 HTTP 连接超时，而是用于控制未发送消息的
	 * 缓冲程度。
	 * <p><strong>注意</strong>：关闭会话可能无法真正关闭物理套接字，
	 * 也可能挂起。在使用阻塞 IO 时尤其如此，例如 Tomcat 7 中默认使用的
	 * BIO 连接器。因此，建议确保服务器使用非阻塞 IO，例如 Tomcat 8
	 * 中默认使用的 NIO 连接器。如果必须使用阻塞 IO，
	 * 请考虑自定义操作系统级别的 TCP 设置，例如 Linux 上的
	 * {@code /proc/sys/net/ipv4/tcp_retries2}。
	 * <p>默认值为 10 秒（即 10 * 1000）。
	 * @param timeLimit the timeout value in milliseconds; the value must be
	 * greater than 0, otherwise it is ignored.
	 */
	public WebSocketTransportRegistration setSendTimeLimit(int timeLimit) {
		this.sendTimeLimit = timeLimit;
		return this;
	}

	/**
	 * 仅供内部使用的受保护访问器。
	 */
	@Nullable
	protected Integer getSendTimeLimit() {
		return this.sendTimeLimit;
	}

	/**
	 * 配置向 WebSocket 会话发送消息或在使用 SockJS 回退选项时写入
	 * HTTP 响应时要缓冲的最大数据量。
	 * <p>通常，WebSocket 服务器期望发往单个 WebSocket 会话的消息
	 * 同一时间只由一个线程发送。使用 {@code @EnableWebSocketMessageBroker}
	 * 配置时会自动保证这一点。
	 * 如果消息发送较慢，或者至少比消息发送速率更慢，
	 * 后续消息将被缓冲，直到达到 {@code sendTimeLimit}
	 * 或 {@code sendBufferSizeLimit}，此时会清除会话状态
	 * 并尝试关闭会话。
	 * <p><strong>注意</strong>：关闭会话可能无法真正关闭物理套接字，
	 * 也可能挂起。在使用阻塞 IO 时尤其如此，例如 Tomcat 7 中默认配置的
	 * BIO 连接器。因此，建议确保服务器使用非阻塞 IO，例如 Tomcat 8
	 * 中默认使用的 NIO 连接器。如果必须使用阻塞 IO，
	 * 请考虑自定义操作系统级别的 TCP 设置，例如 Linux 上的
	 * {@code /proc/sys/net/ipv4/tcp_retries2}。
	 * <p>默认值为 512K（即 512 * 1024）。
	 * @param sendBufferSizeLimit the maximum number of bytes to buffer when
	 * sending messages; if the value is less than or equal to 0 then buffering
	 * is effectively disabled.
	 */
	public WebSocketTransportRegistration setSendBufferSizeLimit(int sendBufferSizeLimit) {
		this.sendBufferSizeLimit = sendBufferSizeLimit;
		return this;
	}

	/**
	 * 仅供内部使用的受保护访问器。
	 */
	@Nullable
	protected Integer getSendBufferSizeLimit() {
		return this.sendBufferSizeLimit;
	}

	/**
	 * 设置 WebSocket 连接建立之后、收到第一条子协议消息之前所允许的
	 * 最大时间（毫秒）。
	 * <p>此处理器针对使用子协议的 WebSocket 连接。
	 * 因此，我们期望客户端在开始时至少发送一条子协议消息，
	 * 否则就认为连接状态不佳（例如代理问题、网络缓慢），
	 * 并可以将其关闭。
	 * <p>默认情况下设置为 {@code 60,000}（1 分钟）。
	 * @param timeToFirstMessage the maximum time allowed in milliseconds
	 * @since 5.1
	 */
	public WebSocketTransportRegistration setTimeToFirstMessage(int timeToFirstMessage) {
		this.timeToFirstMessage = timeToFirstMessage;
		return this;
	}

	/**
	 * 仅供内部使用的受保护访问器。
	 */
	@Nullable
	protected Integer getTimeToFirstMessage() {
		return this.timeToFirstMessage;
	}

	/**
	 * 配置一个或多个工厂，用于装饰处理 WebSocket 消息的处理器。
	 * 这在某些高级用例中可能很有用，例如当相应的 HTTP 会话过期时，
	 * 允许 Spring Security 强制关闭 WebSocket 会话。
	 * @since 4.1.2
	 */
	public WebSocketTransportRegistration setDecoratorFactories(WebSocketHandlerDecoratorFactory... factories) {
		this.decoratorFactories.addAll(Arrays.asList(factories));
		return this;
	}

	/**
	 * 添加一个工厂，用于装饰处理 WebSocket 消息的处理器。
	 * 这在某些高级用例中可能很有用，例如当相应的 HTTP 会话过期时，
	 * 允许 Spring Security 强制关闭 WebSocket 会话。
	 * @since 4.1.2
	 */
	public WebSocketTransportRegistration addDecoratorFactory(WebSocketHandlerDecoratorFactory factory) {
		this.decoratorFactories.add(factory);
		return this;
	}

	protected List<WebSocketHandlerDecoratorFactory> getDecoratorFactories() {
		return this.decoratorFactories;
	}

}
