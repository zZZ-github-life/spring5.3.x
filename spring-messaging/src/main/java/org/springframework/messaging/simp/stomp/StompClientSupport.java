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

package org.springframework.messaging.simp.stomp;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * STOMP 客户端实现的基类。
 *
 * <p>子类可以使用任何库通过 WebSocket 或 TCP 进行连接。创建新连接时，
 * 子类可以创建 {@link DefaultStompSession} 实例，该实例实现了
 * {@link org.springframework.messaging.tcp.TcpConnectionHandler}，
 * 子类随后必须调用其生命周期方法。
 *
 * <p>实际上，{@code TcpConnectionHandler} 和 {@code TcpConnection} 是任何子类
 * 在使用 {@link StompEncoder} 和 {@link StompDecoder} 编解码 STOMP 消息时
 * 必须适配的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public abstract class StompClientSupport {

	private MessageConverter messageConverter = new SimpleMessageConverter();

	@Nullable
	private TaskScheduler taskScheduler;

	private long[] defaultHeartbeat = new long[] {10000, 10000};

	private long receiptTimeLimit = TimeUnit.SECONDS.toMillis(15);


	/**
	 * 设置用于根据对象类型和 "content-type" 头，
	 * 在传入和传出消息的载荷与 {@code byte[]} 之间进行转换的
	 * {@link MessageConverter}。
	 * <p>默认配置 {@link SimpleMessageConverter}。
	 * @param messageConverter 要使用的消息转换器
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * 返回已配置的 {@link MessageConverter}。
	 */
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * 配置用于心跳和回执跟踪的调度器。
	 * <p><strong>注意：</strong>某些传输内置支持心跳，因此不需要 TaskScheduler。
	 * 不过，如果需要回执，则确实需要配置 TaskScheduler。
	 * <p>默认不设置。
	 */
	public void setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * 已配置的 TaskScheduler。
	 */
	@Nullable
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * 配置 STOMP CONNECT 帧 "heart-beat" 头的默认值。第一个数字表示客户端写入或发送心跳的频率。
	 * 第二个数字表示服务器应写入的频率。值为 0 表示不发送心跳。
	 * <p>默认设置为 "10000,10000"，但子类可以覆盖该默认值，
	 * 例如在需要先配置 TaskScheduler 时将其设置为 "0,0"。
	 * <p><strong>注意：</strong>只有在不活跃时（即没有发送其他消息时）才会发送心跳。
	 * 在使用外部代理时，这可能会带来挑战，因为具有非代理目标的消息表示有活动，
	 * 但实际上并不会转发到代理。在这种情况下，可以通过
	 * {@link org.springframework.messaging.simp.config.StompBrokerRelayRegistration}
	 * 配置 `TaskScheduler`，以确保即使只发送具有非代理目标的消息，
	 * 也会向代理转发心跳。
	 * @param heartbeat CONNECT "heart-beat" 消息头的值
	 * @see <a href="https://stomp.github.io/stomp-specification-1.2.html#Heart-beating">
	 * https://stomp.github.io/stomp-specification-1.2.html#Heart-beating</a>
	 */
	public void setDefaultHeartbeat(long[] heartbeat) {
		if (heartbeat.length != 2 || heartbeat[0] < 0 || heartbeat[1] < 0) {
			throw new IllegalArgumentException("Invalid heart-beat: " + Arrays.toString(heartbeat));
		}
		this.defaultHeartbeat = heartbeat;
	}

	/**
	 * 返回已配置的默认心跳值（永不为 {@code null}）。
	 */
	public long[] getDefaultHeartbeat() {
		return this.defaultHeartbeat;
	}

	/**
	 * 确定是否启用心跳。
	 * <p>如果 {@link #setDefaultHeartbeat defaultHeartbeat} 设置为 "0,0"，则返回
	 * {@code false}，否则返回 {@code true}。
	 */
	public boolean isDefaultHeartbeatEnabled() {
		long[] heartbeat = getDefaultHeartbeat();
		return (heartbeat[0] != 0 && heartbeat[1] != 0);
	}

	/**
	 * 配置回执被视为过期之前的毫秒数。
	 * <p>默认设置为 15,000（15 秒）。
	 */
	public void setReceiptTimeLimit(long receiptTimeLimit) {
		Assert.isTrue(receiptTimeLimit > 0, "Receipt time limit must be larger than zero");
		this.receiptTimeLimit = receiptTimeLimit;
	}

	/**
	 * 返回已配置的回执时间限制。
	 */
	public long getReceiptTimeLimit() {
		return this.receiptTimeLimit;
	}


	/**
	 * 用于创建并配置新会话的工厂方法。
	 * @param connectHeaders STOMP CONNECT 帧的消息头
	 * @param handler STOMP 会话的处理器
	 * @return 创建的会话
	 */
	protected ConnectionHandlingStompSession createSession(
			@Nullable StompHeaders connectHeaders, StompSessionHandler handler) {

		connectHeaders = processConnectHeaders(connectHeaders);
		DefaultStompSession session = new DefaultStompSession(handler, connectHeaders);
		session.setMessageConverter(getMessageConverter());
		session.setTaskScheduler(getTaskScheduler());
		session.setReceiptTimeLimit(getReceiptTimeLimit());
		return session;
	}

	/**
	 * 进一步初始化 StompHeaders，例如在必要时设置 heart-beat 头。
	 * @param connectHeaders 要修改的消息头
	 * @return 修改后的消息头
	 */
	protected StompHeaders processConnectHeaders(@Nullable StompHeaders connectHeaders) {
		connectHeaders = (connectHeaders != null ? connectHeaders : new StompHeaders());
		if (connectHeaders.getHeartbeat() == null) {
			connectHeaders.setHeartbeat(getDefaultHeartbeat());
		}
		return connectHeaders;
	}

}
