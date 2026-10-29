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

package org.springframework.jms.support.converter;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.SimpleJmsHeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.AbstractMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * 使用底层的 {@link MessageConverter} 转换负载，并使用
 * {@link org.springframework.jms.support.JmsHeaderMapper} 在 JMS 消息头
 * 与标准消息头之间进行映射，从而在消息传递抽象中的 {@link Message}
 * 与 {@link javax.jms.Message} 之间进行转换。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class MessagingMessageConverter implements MessageConverter, InitializingBean {

	private MessageConverter payloadConverter;

	private JmsHeaderMapper headerMapper;


	/**
	 * 使用默认的负载转换器创建实例。
	 * @see org.springframework.jms.support.converter.SimpleMessageConverter
	 * @see org.springframework.jms.support.SimpleJmsHeaderMapper
	 */
	public MessagingMessageConverter() {
		this(new SimpleMessageConverter(), new SimpleJmsHeaderMapper());
	}

	/**
	 * 使用指定的负载转换器创建实例。
	 * @param payloadConverter the payload converter to use
	 * @since 4.3.12
	 */
	public MessagingMessageConverter(MessageConverter payloadConverter) {
		this(payloadConverter, new SimpleJmsHeaderMapper());
	}

	/**
	 * 使用指定的负载转换器和消息头映射器创建实例。
	 */
	public MessagingMessageConverter(MessageConverter payloadConverter, JmsHeaderMapper headerMapper) {
		Assert.notNull(payloadConverter, "PayloadConverter must not be null");
		Assert.notNull(headerMapper, "HeaderMapper must not be null");
		this.payloadConverter = payloadConverter;
		this.headerMapper = headerMapper;
	}


	/**
	 * 设置用于转换负载的 {@link MessageConverter}。
	 */
	public void setPayloadConverter(MessageConverter payloadConverter) {
		this.payloadConverter = payloadConverter;
	}

	/**
	 * 设置用于在 JMS 消息头与标准消息头之间进行映射的 {@link JmsHeaderMapper}。
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.payloadConverter, "Property 'payloadConverter' is required");
		Assert.notNull(this.headerMapper, "Property 'headerMapper' is required");
	}


	@Override
	public javax.jms.Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		if (!(object instanceof Message)) {
			throw new IllegalArgumentException("Could not convert [" + object + "] - only [" +
					Message.class.getName() + "] is handled by this converter");
		}
		Message<?> input = (Message<?>) object;
		MessageHeaders headers = input.getHeaders();
		Object conversionHint = headers.get(AbstractMessagingTemplate.CONVERSION_HINT_HEADER);
		javax.jms.Message reply = createMessageForPayload(input.getPayload(), session, conversionHint);
		this.headerMapper.fromHeaders(headers, reply);
		return reply;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object fromMessage(javax.jms.Message message) throws JMSException, MessageConversionException {
		Map<String, Object> mappedHeaders = extractHeaders(message);
		Object convertedObject = extractPayload(message);
		MessageBuilder<Object> builder = (convertedObject instanceof org.springframework.messaging.Message ?
				MessageBuilder.fromMessage((org.springframework.messaging.Message<Object>) convertedObject) :
				MessageBuilder.withPayload(convertedObject));
		return builder.copyHeadersIfAbsent(mappedHeaders).build();
	}

	/**
	 * 提取指定 {@link javax.jms.Message} 的负载。
	 */
	protected Object extractPayload(javax.jms.Message message) throws JMSException {
		return this.payloadConverter.fromMessage(message);
	}

	/**
	 * 为指定的负载和 conversionHint 创建 JMS 消息。
	 * conversionHint 是传递给 {@link MessageConverter} 的额外对象，
	 * 例如关联的 {@code MethodParameter}（可能为 {@code null}}）。
	 * @since 4.3
	 * @see MessageConverter#toMessage(Object, Session)
	 */
	protected javax.jms.Message createMessageForPayload(
			Object payload, Session session, @Nullable Object conversionHint) throws JMSException {

		return this.payloadConverter.toMessage(payload, session);
	}

	protected final MessageHeaders extractHeaders(javax.jms.Message message) {
		return this.headerMapper.toHeaders(message);
	}

}
