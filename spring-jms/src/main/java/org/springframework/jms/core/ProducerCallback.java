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

package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.springframework.lang.Nullable;

/**
 * 用于向 JMS 目标发送消息的回调。
 *
 * <p>与 {@link JmsTemplate} 中接受 {@link ProducerCallback} 参数的回调方法一起使用，
 * 通常实现为匿名内部类或 lambda 表达式。
 *
 * <p>典型的实现会对所提供的 JMS {@link Session} 和 {@link MessageProducer}
 * 执行多个操作。
 *
 * @author Mark Pollack
 * @since 1.1
 * @param <T> the result type
 * @see JmsTemplate#execute(ProducerCallback)
 * @see JmsTemplate#execute(javax.jms.Destination, ProducerCallback)
 * @see JmsTemplate#execute(String, ProducerCallback)
 */
@FunctionalInterface
public interface ProducerCallback<T> {

	/**
	 * 对给定的 {@link Session} 和 {@link MessageProducer} 执行操作。
	 * <p>除非在 JmsTemplate 调用中指定，否则消息生产者不与任何目标关联。
	 * @param session the JMS {@code Session} object to use
	 * @param producer the JMS {@code MessageProducer} object to use
	 * @return a result object from working with the {@code Session}, if any
	 * (or {@code null} if none)
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	@Nullable
	T doInJms(Session session, MessageProducer producer) throws JMSException;

}
