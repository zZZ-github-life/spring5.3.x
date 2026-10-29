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

package org.springframework.jms.support.converter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.lang.Nullable;

/**
 * 扩展的 {@link MessageConverter} SPI，支持转换提示。
 *
 * <p>如果提供了转换提示，且转换器实现了此接口，框架将调用
 * 扩展方法，而不是调用常规的 {@code toMessage} 变体。
 *
 * @author Stephane Nicoll
 * @since 4.3
 */
public interface SmartMessageConverter extends MessageConverter {

	/**
	 * {@link #toMessage(Object, Session)} 的变体，额外接受一个转换上下文
	 * 作为参数，从而可以考虑例如负载参数上的注解。
	 * @param object the object to convert
	 * @param session the Session to use for creating a JMS Message
	 * @param conversionHint an extra object passed to the {@link MessageConverter},
	 * e.g. the associated {@code MethodParameter} (may be {@code null}}
	 * @return the JMS Message
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 * @throws MessageConversionException in case of conversion failure
	 * @see #toMessage(Object, Session)
	 */
	Message toMessage(Object object, Session session, @Nullable Object conversionHint)
			throws JMSException, MessageConversionException;

}
