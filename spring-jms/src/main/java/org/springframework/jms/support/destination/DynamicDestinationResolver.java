/*
 * Copyright 2002-2014 the original author or authors.
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
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 简单的 {@link DestinationResolver} 实现，将目的地名称解析为动态目的地。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see javax.jms.Session#createQueue
 * @see javax.jms.Session#createTopic
 */
public class DynamicDestinationResolver implements DestinationResolver {

	/**
	 * 将指定的目的地名称解析为动态目的地。
	 * @param session 当前的 JMS Session
	 * @param destinationName 目标的名称
	 * @param pubSubDomain 如果域为发布-订阅，则为 {@code true}；如果为点对点（P2P），则为 {@code false}
	 * @return JMS 目标（主题或队列）
	 * @throws javax.jms.JMSException 解析失败时
	 * @see #resolveTopic(javax.jms.Session, String)
	 * @see #resolveQueue(javax.jms.Session, String)
	 */
	@Override
	public Destination resolveDestinationName(@Nullable Session session, String destinationName, boolean pubSubDomain)
			throws JMSException {

		Assert.notNull(session, "Session must not be null");
		Assert.notNull(destinationName, "Destination name must not be null");
		if (pubSubDomain) {
			return resolveTopic(session, destinationName);
		}
		else {
			return resolveQueue(session, destinationName);
		}
	}


	/**
	 * 将给定的目的地名称解析为 {@link Topic}。
	 * @param session 当前的 JMS Session
	 * @param topicName 所需 {@link Topic} 的名称
	 * @return 该 JMS {@link Topic}
	 * @throws javax.jms.JMSException 解析失败时
	 * @see Session#createTopic(String)
	 */
	protected Topic resolveTopic(Session session, String topicName) throws JMSException {
		return session.createTopic(topicName);
	}

	/**
	 * 将给定的目的地名称解析为 {@link Queue}。
	 * @param session 当前的 JMS Session
	 * @param queueName 所需 {@link Queue} 的名称
	 * @return 该 JMS {@link Queue}
	 * @throws javax.jms.JMSException 解析失败时
	 * @see Session#createQueue(String)
	 */
	protected Queue resolveQueue(Session session, String queueName) throws JMSException {
		return session.createQueue(queueName);
	}

}
