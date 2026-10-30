/*
 * Copyright 2002-2020 the original author or authors.
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

/**
 * 与 JmsTemplate 中可将对象转换为消息的 send 方法一起使用。
 *
 * <p>这允许在消息经过转换器处理之后对其进行进一步修改，
 * 对于设置 JMS 头和属性很有用。
 *
 * <p>通常实现为 lambda 表达式或匿名内部类。
 *
 * @author Mark Pollack
 * @since 1.1
 * @see JmsTemplate#convertAndSend(String, Object, MessagePostProcessor)
 * @see JmsTemplate#convertAndSend(javax.jms.Destination, Object, MessagePostProcessor)
 * @see org.springframework.jms.support.converter.MessageConverter
 */
@FunctionalInterface
public interface MessagePostProcessor {

	/**
	 * 处理给定的消息。
	 * <p>返回的消息通常是原始消息的修改版本。
	 * @param message 来自 MessageConverter 的 JMS 消息
	 * @return 消息后处理后的变体，或就是传入的消息；
	 * 永不为 {@code null}
	 * @throws javax.jms.JMSException 如果由 JMS API 方法抛出
	 */
	Message postProcessMessage(Message message) throws JMSException;

}
