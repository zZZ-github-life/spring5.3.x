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

package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * 根据给定的 {@link Session} 创建 JMS 消息。
 *
 * <p>{@code Session} 通常由 {@link JmsTemplate} 类的实例提供。
 *
 * <p>实现<i>不需要</i>关心其尝试执行的操作可能抛出的受检
 * {@code JMSExceptions}（来自 '{@code javax.jms}' 包）。
 * {@code JmsTemplate} 会捕获并妥善处理这些 {@code JMSExceptions}。
 *
 * @author Mark Pollack
 * @since 1.1
 */
@FunctionalInterface
public interface MessageCreator {

	/**
	 * 创建一个要发送的 {@link Message}。
	 * @param session the JMS {@link Session} to be used to create the
	 * {@code Message} (never {@code null})
	 * @return the {@code Message} to be sent
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	Message createMessage(Session session) throws JMSException;

}
