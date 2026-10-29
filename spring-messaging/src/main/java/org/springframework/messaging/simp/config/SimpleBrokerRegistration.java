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

package org.springframework.messaging.simp.config;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.scheduling.TaskScheduler;

/**
 * 用于配置 {@link SimpleBrokerMessageHandler} 的注册类。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleBrokerRegistration extends AbstractBrokerRegistration {

	@Nullable
	private TaskScheduler taskScheduler;

	@Nullable
	private long[] heartbeat;

	@Nullable
	private String selectorHeaderName = "selector";


	/**
	 * 创建一个新的 {@code SimpleBrokerRegistration}。
	 * @param clientInboundChannel the inbound channel
	 * @param clientOutboundChannel the outbound channel
	 * @param destinationPrefixes the destination prefixes
	 */
	public SimpleBrokerRegistration(SubscribableChannel clientInboundChannel,
			MessageChannel clientOutboundChannel, String[] destinationPrefixes) {

		super(clientInboundChannel, clientOutboundChannel, destinationPrefixes);
	}


	/**
	 * 配置用于提供心跳支持的 {@link org.springframework.scheduling.TaskScheduler}。
	 * 设置此属性还会将 {@link #setHeartbeatValue heartbeatValue} 设置为 "10000, 10000"。
	 * <p>默认情况下不设置此项。
	 * @since 4.2
	 */
	public SimpleBrokerRegistration setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		return this;
	}

	/**
	 * 配置心跳设置的值。第一个数字表示服务器写入或发送心跳的频率。
	 * 第二个数字表示客户端应写入的频率。0 表示不发送心跳。
	 * <p>默认情况下此项设置为 "0, 0"，除非设置了 {@link #setTaskScheduler
	 * taskScheduler}，此时默认值变为 "10000,10000"（以毫秒为单位）。
	 * @since 4.2
	 */
	public SimpleBrokerRegistration setHeartbeatValue(long[] heartbeat) {
		this.heartbeat = heartbeat;
		return this;
	}

	/**
	 * 配置订阅消息可携带的、用于过滤与该订阅匹配的消息的头部名称。
	 * 该头部的值应为 Spring EL 布尔表达式，将应用于与该订阅匹配的消息的头部。
	 * <p>例如：
	 * <pre>
	 * headers.foo == 'bar'
	 * </pre>
	 * <p>默认情况下此项设置为 "selector"。可以将其设置为其他名称，
	 * 或设置为 {@code null} 以关闭对选择器头部的支持。
	 * @param selectorHeaderName the name to use for a selector header
	 * @since 4.3.17
	 */
	public void setSelectorHeaderName(@Nullable String selectorHeaderName) {
		this.selectorHeaderName = selectorHeaderName;
	}


	@Override
	protected SimpleBrokerMessageHandler getMessageHandler(SubscribableChannel brokerChannel) {
		SimpleBrokerMessageHandler handler = new SimpleBrokerMessageHandler(getClientInboundChannel(),
				getClientOutboundChannel(), brokerChannel, getDestinationPrefixes());
		if (this.taskScheduler != null) {
			handler.setTaskScheduler(this.taskScheduler);
		}
		if (this.heartbeat != null) {
			handler.setHeartbeatValue(this.heartbeat);
		}
		handler.setSelectorHeaderName(this.selectorHeaderName);
		return handler;
	}

}
