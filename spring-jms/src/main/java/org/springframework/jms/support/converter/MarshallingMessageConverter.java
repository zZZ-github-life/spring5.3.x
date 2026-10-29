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

package org.springframework.jms.support.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;

/**
 * Spring JMS {@link MessageConverter}，使用 {@link Marshaller} 和 {@link Unmarshaller}。
 * 将对象编组为 {@link BytesMessage}；如果 {@link #setTargetType targetType}
 * 设置为 {@link MessageType#TEXT}，则编组为 {@link TextMessage}。
 * 从 {@link TextMessage} 或 {@link BytesMessage} 解组为对象。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MarshallingMessageConverter implements MessageConverter, InitializingBean {

	@Nullable
	private Marshaller marshaller;

	@Nullable
	private Unmarshaller unmarshaller;

	private MessageType targetType = MessageType.BYTES;


	/**
	 * 构造一个新的 {@code MarshallingMessageConverter}，未设置 {@link Marshaller}
	 * 和 {@link Unmarshaller}。必须在构造后通过调用
	 * {@link #setMarshaller(Marshaller)} 和 {@link #setUnmarshaller(Unmarshaller)} 设置 marshaller。
	 */
	public MarshallingMessageConverter() {
	}

	/**
	 * 使用给定的 {@link Marshaller} 构造一个新的 {@code MarshallingMessageConverter}。
	 * <p>如果给定的 {@link Marshaller} 也实现了 {@link Unmarshaller} 接口，
	 * 则它同时用于编组和解组。否则将抛出异常。
	 * <p>请注意，Spring 中的所有 {@link Marshaller} 实现也都实现了
	 * {@link Unmarshaller} 接口，因此可以安全地使用此构造函数。
	 * @param marshaller object used as marshaller and unmarshaller
	 * @throws IllegalArgumentException when {@code marshaller} does not implement the
	 * {@link Unmarshaller} interface as well
	 */
	public MarshallingMessageConverter(Marshaller marshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		if (!(marshaller instanceof Unmarshaller)) {
			throw new IllegalArgumentException(
					"Marshaller [" + marshaller + "] does not implement the Unmarshaller " +
					"interface. Please set an Unmarshaller explicitly by using the " +
					"MarshallingMessageConverter(Marshaller, Unmarshaller) constructor.");
		}
		else {
			this.marshaller = marshaller;
			this.unmarshaller = (Unmarshaller) marshaller;
		}
	}

	/**
	 * 使用给定的 Marshaller 和 Unmarshaller 构造一个新的
	 * {@code MarshallingMessageConverter}。
	 * @param marshaller the Marshaller to use
	 * @param unmarshaller the Unmarshaller to use
	 */
	public MarshallingMessageConverter(Marshaller marshaller, Unmarshaller unmarshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		Assert.notNull(unmarshaller, "Unmarshaller must not be null");
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}


	/**
	 * 设置此消息转换器要使用的 {@link Marshaller}。
	 */
	public void setMarshaller(Marshaller marshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
	}

	/**
	 * 设置此消息转换器要使用的 {@link Unmarshaller}。
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		Assert.notNull(unmarshaller, "Unmarshaller must not be null");
		this.unmarshaller = unmarshaller;
	}

	/**
	 * 指定 {@link #toMessage(Object, Session)} 应编组为
	 * {@link BytesMessage} 还是 {@link TextMessage}。
	 * <p>默认为 {@link MessageType#BYTES}，即此转换器编组为
	 * {@link BytesMessage}。请注意，此转换器的默认版本仅支持
	 * {@link MessageType#BYTES} 和 {@link MessageType#TEXT}。
	 * @see MessageType#BYTES
	 * @see MessageType#TEXT
	 */
	public void setTargetType(MessageType targetType) {
		Assert.notNull(targetType, "MessageType must not be null");
		this.targetType = targetType;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
		Assert.notNull(this.unmarshaller, "Property 'unmarshaller' is required");
	}


	/**
	 * 此实现将给定对象编组为 {@link javax.jms.TextMessage} 或
	 * {@link javax.jms.BytesMessage}。可以通过设置
	 * {@link #setTargetType "marshalTo"} 属性来指定所需的消息类型。
	 * @see #marshalToTextMessage
	 * @see #marshalToBytesMessage
	 */
	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		Assert.state(this.marshaller != null, "No Marshaller set");
		try {
			switch (this.targetType) {
				case TEXT:
					return marshalToTextMessage(object, session, this.marshaller);
				case BYTES:
					return marshalToBytesMessage(object, session, this.marshaller);
				default:
					return marshalToMessage(object, session, this.marshaller, this.targetType);
			}
		}
		catch (XmlMappingException | IOException ex) {
			throw new MessageConversionException("Could not marshal [" + object + "]", ex);
		}
	}

	/**
	 * 此实现将给定的 {@link Message} 解组为对象。
	 * @see #unmarshalFromTextMessage
	 * @see #unmarshalFromBytesMessage
	 */
	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		Assert.state(this.unmarshaller != null, "No Unmarshaller set");
		try {
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return unmarshalFromTextMessage(textMessage, this.unmarshaller);
			}
			else if (message instanceof BytesMessage) {
				BytesMessage bytesMessage = (BytesMessage) message;
				return unmarshalFromBytesMessage(bytesMessage, this.unmarshaller);
			}
			else {
				return unmarshalFromMessage(message, this.unmarshaller);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not access message content: " + message, ex);
		}
		catch (XmlMappingException ex) {
			throw new MessageConversionException("Could not unmarshal message: " + message, ex);
		}
	}


	/**
	 * 将给定对象编组为 {@link TextMessage}。
	 * @param object the object to be marshalled
	 * @param session current JMS session
	 * @param marshaller the marshaller to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 * @see Session#createTextMessage
	 * @see Marshaller#marshal(Object, Result)
	 */
	protected TextMessage marshalToTextMessage(Object object, Session session, Marshaller marshaller)
			throws JMSException, IOException, XmlMappingException {

		StringWriter writer = new StringWriter(1024);
		Result result = new StreamResult(writer);
		marshaller.marshal(object, result);
		return session.createTextMessage(writer.toString());
	}

	/**
	 * 将给定对象编组为 {@link BytesMessage}。
	 * @param object the object to be marshalled
	 * @param session current JMS session
	 * @param marshaller the marshaller to use
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 * @see Session#createBytesMessage
	 * @see Marshaller#marshal(Object, Result)
	 */
	protected BytesMessage marshalToBytesMessage(Object object, Session session, Marshaller marshaller)
			throws JMSException, IOException, XmlMappingException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		StreamResult streamResult = new StreamResult(bos);
		marshaller.marshal(object, streamResult);
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bos.toByteArray());
		return message;
	}

	/**
	 * 允许自定义消息编组的模板方法。
	 * 当 {@link #setTargetType} 不是 {@link MessageType#TEXT} 或
	 * {@link MessageType#BYTES} 时调用。
	 * <p>默认实现抛出 {@link IllegalArgumentException}。
	 * @param object the object to marshal
	 * @param session the JMS session
	 * @param marshaller the marshaller to use
	 * @param targetType the target message type (other than TEXT or BYTES)
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 */
	protected Message marshalToMessage(Object object, Session session, Marshaller marshaller, MessageType targetType)
			throws JMSException, IOException, XmlMappingException {

		throw new IllegalArgumentException("Unsupported message type [" + targetType +
				"]. MarshallingMessageConverter by default only supports TextMessages and BytesMessages.");
	}


	/**
	 * 将给定的 {@link TextMessage} 解组为对象。
	 * @param message the message
	 * @param unmarshaller the unmarshaller to use
	 * @return the unmarshalled object
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 * @see Unmarshaller#unmarshal(Source)
	 */
	protected Object unmarshalFromTextMessage(TextMessage message, Unmarshaller unmarshaller)
			throws JMSException, IOException, XmlMappingException {

		Source source = new StreamSource(new StringReader(message.getText()));
		return unmarshaller.unmarshal(source);
	}

	/**
	 * 将给定的 {@link BytesMessage} 解组为对象。
	 * @param message the message
	 * @param unmarshaller the unmarshaller to use
	 * @return the unmarshalled object
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 * @see Unmarshaller#unmarshal(Source)
	 */
	protected Object unmarshalFromBytesMessage(BytesMessage message, Unmarshaller unmarshaller)
			throws JMSException, IOException, XmlMappingException {

		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		StreamSource source = new StreamSource(bis);
		return unmarshaller.unmarshal(source);
	}

	/**
	 * 允许自定义消息解组的模板方法。
	 * 当调用 {@link #fromMessage(Message)} 时传入的消息
	 * 不是 {@link TextMessage} 或 {@link BytesMessage} 的情况下调用。
	 * <p>默认实现抛出 {@link IllegalArgumentException}。
	 * @param message the message
	 * @param unmarshaller the unmarshaller to use
	 * @return the unmarshalled object
	 * @throws JMSException if thrown by JMS methods
	 * @throws IOException in case of I/O errors
	 * @throws XmlMappingException in case of OXM mapping errors
	 */
	protected Object unmarshalFromMessage(Message message, Unmarshaller unmarshaller)
			throws JMSException, IOException, XmlMappingException {

		throw new IllegalArgumentException("Unsupported message type [" + message.getClass() +
				"]. MarshallingMessageConverter by default only supports TextMessages and BytesMessages.");
	}

}
