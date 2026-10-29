/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.listener;

import org.springframework.context.SmartLifecycle;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;

/**
 * 框架内部使用的抽象，表示消息监听器容器。
 * 不打算由外部实现，支持 JMS 和 JCA 风格的容器。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface MessageListenerContainer extends SmartLifecycle {

	/**
	 * 设置要使用的消息监听器。如果该消息监听器类型不受支持，
	 * 则抛出 {@link IllegalArgumentException}。
	 */
	void setupMessageListener(Object messageListener);

	/**
	 * 返回可用于转换 {@link javax.jms.Message} 的 {@link MessageConverter}（如果有）。
	 */
	@Nullable
	MessageConverter getMessageConverter();

	/**
	 * 返回用于按名称解析目的地的 {@link DestinationResolver}。
	 */
	@Nullable
	DestinationResolver getDestinationResolver();

	/**
	 * 返回是否使用发布/订阅域（{@link javax.jms.Topic Topics}）。
	 * 否则使用点对点域（{@link javax.jms.Queue Queues}）。
	 */
	boolean isPubSubDomain();

	/**
	 * 返回回复目的地是否使用发布/订阅域
	 * （{@link javax.jms.Topic Topics}）。否则使用点对点域
	 * （{@link javax.jms.Queue Queues}）。
	 * <p>默认情况下，该值与 {@link #isPubSubDomain()} 相同。
	 */
	boolean isReplyPubSubDomain();

	/**
	 * 返回发送回复时要使用的 {@link QosSettings}；
	 * 如果应使用代理的默认值，则返回 {@code null}。
	 * @since 5.0
	 */
	@Nullable
	QosSettings getReplyQosSettings();

}
