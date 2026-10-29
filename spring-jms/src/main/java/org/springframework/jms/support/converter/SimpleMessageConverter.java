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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.util.ObjectUtils;

/**
 * 一个简单的消息转换器，能够处理 TextMessage、BytesMessage、
 * MapMessage 和 ObjectMessage。被 {@link org.springframework.jms.core.JmsTemplate}
 * 用作 {@code convertAndSend} 和 {@code receiveAndConvert} 操作的默认转换策略。
 *
 * <p>将 String 转换为 {@link javax.jms.TextMessage}，将字节数组转换为
 * {@link javax.jms.BytesMessage}，将 Map 转换为 {@link javax.jms.MapMessage}，
 * 将 Serializable 对象转换为 {@link javax.jms.ObjectMessage}（或反之）。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate#convertAndSend
 * @see org.springframework.jms.core.JmsTemplate#receiveAndConvert
 */
public class SimpleMessageConverter implements MessageConverter {

	/**
	 * 此实现为 String 创建 TextMessage，为字节数组创建
	 * BytesMessage，为 Map 创建 MapMessage，
	 * 为 Serializable 对象创建 ObjectMessage。
	 * @see #createMessageForString
	 * @see #createMessageForByteArray
	 * @see #createMessageForMap
	 * @see #createMessageForSerializable
	 */
	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		if (object instanceof Message) {
			return (Message) object;
		}
		else if (object instanceof String) {
			return createMessageForString((String) object, session);
		}
		else if (object instanceof byte[]) {
			return createMessageForByteArray((byte[]) object, session);
		}
		else if (object instanceof Map) {
			return createMessageForMap((Map<? ,?>) object, session);
		}
		else if (object instanceof Serializable) {
			return createMessageForSerializable(((Serializable) object), session);
		}
		else {
			throw new MessageConversionException("Cannot convert object of type [" +
					ObjectUtils.nullSafeClassName(object) + "] to JMS message. Supported message " +
					"payloads are: String, byte array, Map<String,?>, Serializable object.");
		}
	}

	/**
	 * 此实现将 TextMessage 转换回 String，将 BytesMessage 转换回
	 * 字节数组，将 MapMessage 转换回 Map，
	 * 将 ObjectMessage 转换回 Serializable 对象。
	 * 对于未知的消息类型，返回原始的 Message 对象。
	 * @see #extractStringFromMessage
	 * @see #extractByteArrayFromMessage
	 * @see #extractMapFromMessage
	 * @see #extractSerializableFromMessage
	 */
	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		if (message instanceof TextMessage) {
			return extractStringFromMessage((TextMessage) message);
		}
		else if (message instanceof BytesMessage) {
			return extractByteArrayFromMessage((BytesMessage) message);
		}
		else if (message instanceof MapMessage) {
			return extractMapFromMessage((MapMessage) message);
		}
		else if (message instanceof ObjectMessage) {
			return extractSerializableFromMessage((ObjectMessage) message);
		}
		else {
			return message;
		}
	}


	/**
	 * 为给定的 String 创建 JMS TextMessage。
	 * @param text the String to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see javax.jms.Session#createTextMessage
	 */
	protected TextMessage createMessageForString(String text, Session session) throws JMSException {
		return session.createTextMessage(text);
	}

	/**
	 * 为给定的字节数组创建 JMS BytesMessage。
	 * @param bytes the byte array to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see javax.jms.Session#createBytesMessage
	 */
	protected BytesMessage createMessageForByteArray(byte[] bytes, Session session) throws JMSException {
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bytes);
		return message;
	}

	/**
	 * 为给定的 Map 创建 JMS MapMessage。
	 * @param map the Map to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see javax.jms.Session#createMapMessage
	 */
	protected MapMessage createMessageForMap(Map<?, ?> map, Session session) throws JMSException {
		MapMessage message = session.createMapMessage();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object key = entry.getKey();
			if (!(key instanceof String)) {
				throw new MessageConversionException("Cannot convert non-String key of type [" +
						ObjectUtils.nullSafeClassName(key) + "] to JMS MapMessage entry");
			}
			message.setObject((String) key, entry.getValue());
		}
		return message;
	}

	/**
	 * 为给定的 Serializable 对象创建 JMS ObjectMessage。
	 * @param object the Serializable object to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see javax.jms.Session#createObjectMessage
	 */
	protected ObjectMessage createMessageForSerializable(Serializable object, Session session) throws JMSException {
		return session.createObjectMessage(object);
	}


	/**
	 * 从给定的 TextMessage 中提取 String。
	 * @param message the message to convert
	 * @return the resulting String
	 * @throws JMSException if thrown by JMS methods
	 */
	protected String extractStringFromMessage(TextMessage message) throws JMSException {
		return message.getText();
	}

	/**
	 * 从给定的 {@link BytesMessage} 中提取字节数组。
	 * @param message the message to convert
	 * @return the resulting byte array
	 * @throws JMSException if thrown by JMS methods
	 */
	protected byte[] extractByteArrayFromMessage(BytesMessage message) throws JMSException {
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		return bytes;
	}

	/**
	 * 从给定的 {@link MapMessage} 中提取 Map。
	 * @param message the message to convert
	 * @return the resulting Map
	 * @throws JMSException if thrown by JMS methods
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> extractMapFromMessage(MapMessage message) throws JMSException {
		Map<String, Object> map = new HashMap<>();
		Enumeration<String> en = message.getMapNames();
		while (en.hasMoreElements()) {
			String key = en.nextElement();
			map.put(key, message.getObject(key));
		}
		return map;
	}

	/**
	 * 从给定的 {@link ObjectMessage} 中提取 Serializable 对象。
	 * @param message the message to convert
	 * @return the resulting Serializable object
	 * @throws JMSException if thrown by JMS methods
	 */
	protected Serializable extractSerializableFromMessage(ObjectMessage message) throws JMSException {
		return message.getObject();
	}

}
