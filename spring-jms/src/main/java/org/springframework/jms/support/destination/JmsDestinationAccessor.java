/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jms.support.destination;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.jms.support.JmsAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.jms.core.JmsTemplate} 及其他 JMS 访问网关辅助类的基类，
 * 在 {@link JmsAccessor JmsAccessor} 的公共属性之上添加了与目的地相关的属性。
 *
 * <p>不打算直接使用。
 * 请参见 {@link org.springframework.jms.core.JmsTemplate}。
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see org.springframework.jms.support.JmsAccessor
 * @see org.springframework.jms.core.JmsTemplate
 */
public abstract class JmsDestinationAccessor extends JmsAccessor {

	/**
	 * 超时值，表示接收操作应在不阻塞的情况下检查消息是否立即可用。
	 * @since 4.3
	 */
	public static final long RECEIVE_TIMEOUT_NO_WAIT = -1;

	/**
	 * 超时值，表示无超时的阻塞接收。
	 * @since 4.3
	 */
	public static final long RECEIVE_TIMEOUT_INDEFINITE_WAIT = 0;


	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private boolean pubSubDomain = false;


	/**
	 * 设置用于为此访问器解析 {@link javax.jms.Destination} 引用的
	 * {@link DestinationResolver}。
	 * <p>默认解析器是 DynamicDestinationResolver。如果需要将目的地名称作为
	 * JNDI 位置解析，请指定 JndiDestinationResolver。
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.destination.JndiDestinationResolver
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		Assert.notNull(destinationResolver, "'destinationResolver' must not be null");
		this.destinationResolver = destinationResolver;
	}

	/**
	 * 返回此访问器的 DestinationResolver（永不为 {@code null}）。
	 */
	public DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * 配置目的地访问器，使其了解所使用的 JMS 域。
	 * 默认是点对点（队列）。
	 * <p>如果启用了动态目的地，此设置主要指示要解析的目的地类型。
	 * @param pubSubDomain 发布/订阅域（{@link javax.jms.Topic Topics}）为 "true"，
	 * 点对点域（{@link javax.jms.Queue Queues}）为 "false"
	 * @see #setDestinationResolver
	 */
	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	/**
	 * 返回是否使用发布-订阅域（{@link javax.jms.Topic Topic}）。
	 * 否则使用点对点域（{@link javax.jms.Queue Queue}）。
	 */
	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}


	/**
	 * 通过此访问器的 {@link DestinationResolver} 将给定的目的地名称解析为
	 * JMS {@link Destination}。
	 * @param session 当前的 JMS {@link Session}
	 * @param destinationName 目标的名称
	 * @return 定位到的 {@link Destination}
	 * @throws javax.jms.JMSException 解析失败时
	 * @see #setDestinationResolver
	 */
	protected Destination resolveDestinationName(Session session, String destinationName) throws JMSException {
		return getDestinationResolver().resolveDestinationName(session, destinationName, isPubSubDomain());
	}

	/**
	 * 实际从给定的消费者接收一条消息。
	 * @param consumer 用于接收消息的 JMS MessageConsumer
	 * @param timeout 接收超时（负值表示不等待直接接收；0 表示无限期等待）
	 * @return 收到的 JMS Message；如果没有，则为 {@code null}
	 * @throws JMSException 如果由 JMS API 方法抛出
	 * @since 4.3
	 * @see #RECEIVE_TIMEOUT_NO_WAIT
	 * @see #RECEIVE_TIMEOUT_INDEFINITE_WAIT
	 */
	@Nullable
	protected Message receiveFromConsumer(MessageConsumer consumer, long timeout) throws JMSException {
		if (timeout > 0) {
			return consumer.receive(timeout);
		}
		else if (timeout < 0) {
			return consumer.receiveNoWait();
		}
		else {
			return consumer.receive();
		}
	}

}
