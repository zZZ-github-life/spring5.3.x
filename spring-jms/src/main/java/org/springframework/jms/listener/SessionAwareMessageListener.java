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

package org.springframework.jms.listener;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * 标准 JMS {@link javax.jms.MessageListener} 接口的变体，
 * 不仅提供接收到的 Message，还提供底层的 JMS Session 对象。
 * 后者可用于发送回复消息，而无需访问外部的 Connection/Session，
 * 即无需访问底层的 ConnectionFactory。
 *
 * <p>由 Spring 的 {@link DefaultMessageListenerContainer}
 * 和 {@link SimpleMessageListenerContainer} 支持，
 * 作为标准 JMS MessageListener 接口的直接替代方案。
 * 基于 JCA 的监听器容器通常<i>不</i>支持它：
 * 为获得最大兼容性，请实现标准 JMS MessageListener。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @param <M> the message type
 * @see AbstractMessageListenerContainer#setMessageListener
 * @see DefaultMessageListenerContainer
 * @see SimpleMessageListenerContainer
 * @see org.springframework.jms.listener.endpoint.JmsMessageEndpointManager
 * @see javax.jms.MessageListener
 */
@FunctionalInterface
public interface SessionAwareMessageListener<M extends Message> {

	/**
	 * 处理接收到的 JMS 消息的回调。
	 * <p>实现者应处理给定的 Message，
	 * 通常通过给定的 Session 发送回复消息。
	 * @param message the received JMS message (never {@code null})
	 * @param session the underlying JMS Session (never {@code null})
	 * @throws JMSException if thrown by JMS methods
	 */
	void onMessage(M message, Session session) throws JMSException;

}
