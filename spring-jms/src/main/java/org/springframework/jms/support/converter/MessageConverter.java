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

package org.springframework.jms.support.converter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * 策略接口，指定 Java 对象与 JMS 消息之间的转换器。
 *
 * <p>请查看 {@link SimpleMessageConverter} 了解默认实现，
 * 它在“标准”消息负载与 JMS Message 类型之间进行转换。
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate#setMessageConverter
 * @see org.springframework.jms.listener.adapter.MessageListenerAdapter#setMessageConverter
 * @see org.springframework.jms.remoting.JmsInvokerClientInterceptor#setMessageConverter
 * @see org.springframework.jms.remoting.JmsInvokerServiceExporter#setMessageConverter
 */
public interface MessageConverter {

	/**
	 * 使用提供的会话将 Java 对象转换为 JMS Message，
	 * 由该会话创建消息对象。
	 * @param object 要转换的对象
	 * @param session 用于创建 JMS Message 的 Session
	 * @return 该 JMS Message
	 * @throws javax.jms.JMSException 如果由 JMS API 方法抛出
	 * @throws MessageConversionException 转换失败时
	 */
	Message toMessage(Object object, Session session) throws JMSException, MessageConversionException;

	/**
	 * 将 JMS Message 转换为 Java 对象。
	 * @param message 要转换的消息
	 * @return 转换后的 Java 对象
	 * @throws javax.jms.JMSException 如果由 JMS API 方法抛出
	 * @throws MessageConversionException 转换失败时
	 */
	Object fromMessage(Message message) throws JMSException, MessageConversionException;

}
