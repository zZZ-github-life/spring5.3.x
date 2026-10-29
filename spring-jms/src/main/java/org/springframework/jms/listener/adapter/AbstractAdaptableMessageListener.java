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

package org.springframework.jms.listener.adapter;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.SimpleJmsHeaderMapper;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessagingMessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.converter.SmartMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * 抽象的 JMS {@link MessageListener} 适配器，提供提取 JMS {@link Message}
 * 负载所需的基础设施。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 4.1
 * @see MessageListener
 * @see SessionAwareMessageListener
 */
public abstract class AbstractAdaptableMessageListener
		implements MessageListener, SessionAwareMessageListener<Message> {

	/** 子类可用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Object defaultResponseDestination;

	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	@Nullable
	private MessageConverter messageConverter = new SimpleMessageConverter();

	private final MessagingMessageConverterAdapter messagingMessageConverter = new MessagingMessageConverterAdapter();

	@Nullable
	private QosSettings responseQosSettings;


	/**
	 * 设置用于发送响应消息的默认目的地。在请求消息未携带 "JMSReplyTo" 字段时
	 * 将应用此设置。
	 * <p>响应目的地只与返回结果对象的监听器方法相关，这些结果对象将被包装成
	 * 响应消息并发送到响应目的地。
	 * <p>或者，指定 "defaultResponseQueueName" 或 "defaultResponseTopicName"，
	 * 以便通过 DestinationResolver 动态解析。
	 * @see #setDefaultResponseQueueName(String)
	 * @see #setDefaultResponseTopicName(String)
	 * @see #getResponseDestination
	 */
	public void setDefaultResponseDestination(Destination destination) {
		this.defaultResponseDestination = destination;
	}

	/**
	 * 设置用于发送响应消息的默认响应队列名称。
	 * 在请求消息未携带 "JMSReplyTo" 字段时将应用此设置。
	 * <p>或者，将 JMS Destination 对象指定为 "defaultResponseDestination"。
	 * @see #setDestinationResolver
	 * @see #setDefaultResponseDestination(javax.jms.Destination)
	 */
	public void setDefaultResponseQueueName(String destinationName) {
		this.defaultResponseDestination = new DestinationNameHolder(destinationName, false);
	}

	/**
	 * 设置用于发送响应消息的默认响应主题名称。
	 * 在请求消息未携带 "JMSReplyTo" 字段时将应用此设置。
	 * <p>或者，将 JMS Destination 对象指定为 "defaultResponseDestination"。
	 * @see #setDestinationResolver
	 * @see #setDefaultResponseDestination(javax.jms.Destination)
	 */
	public void setDefaultResponseTopicName(String destinationName) {
		this.defaultResponseDestination = new DestinationNameHolder(destinationName, true);
	}

	/**
	 * 设置用于为此适配器解析响应目的地名称的 DestinationResolver。
	 * <p>默认解析器是 DynamicDestinationResolver。如果需要将目的地名称作为
	 * JNDI 位置解析，请指定 JndiDestinationResolver。
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.destination.JndiDestinationResolver
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		Assert.notNull(destinationResolver, "DestinationResolver must not be null");
		this.destinationResolver = destinationResolver;
	}

	/**
	 * 返回此适配器的 DestinationResolver。
	 */
	protected DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * 设置转换器，用于将传入的 JMS 消息转换为监听器方法参数，
	 * 以及将监听器方法返回的对象转换回 JMS 消息。
	 * <p>默认转换器是 {@link SimpleMessageConverter}，它能够处理
	 * {@link javax.jms.BytesMessage BytesMessage}、
	 * {@link javax.jms.TextMessage TextMessage} 和
	 * {@link javax.jms.ObjectMessage ObjectMessage}。
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * 返回转换器，用于将传入的 JMS 消息转换为监听器方法参数，
	 * 以及将监听器方法返回的对象转换回 JMS 消息。
	 */
	@Nullable
	protected MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * 设置用于映射标准 JMS 头部的 {@link JmsHeaderMapper} 实现。
	 * 默认使用 {@link SimpleJmsHeaderMapper}。
	 * @see SimpleJmsHeaderMapper
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "HeaderMapper must not be null");
		this.messagingMessageConverter.setHeaderMapper(headerMapper);
	}

	/**
	 * 返回此监听器的 {@link MessagingMessageConverter}，
	 * 它能够转换 {@link org.springframework.messaging.Message}。
	 */
	protected final MessagingMessageConverter getMessagingMessageConverter() {
		return this.messagingMessageConverter;
	}

	/**
	 * 设置发送响应时要使用的 {@link QosSettings}。可以设置为 {@code null}，
	 * 表示应使用代理的默认值。
	 * @param responseQosSettings the QoS settings to use when sending a response or
	 * {@code null} to use the default values.
	 * @since 5.0
	 */
	public void setResponseQosSettings(@Nullable QosSettings responseQosSettings) {
		this.responseQosSettings = responseQosSettings;
	}

	/**
	 * 返回发送响应时要使用的 {@link QosSettings}，
	 * 如果应使用默认值则返回 {@code null}。
	 * @since 5.0
	 */
	@Nullable
	protected QosSettings getResponseQosSettings() {
		return this.responseQosSettings;
	}


	/**
	 * 标准 JMS {@link MessageListener} 入口点。
	 * <p>将消息（经过适当的消息参数转换）委托给目标监听器方法。
	 * 发生异常时，将调用 {@link #handleListenerException(Throwable)} 方法。
	 * <p><b>注意：</b>不支持根据监听器方法返回的结果对象发送响应消息。
	 * 如需同时处理结果对象，请使用 {@link SessionAwareMessageListener} 入口点
	 * （通常通过 Spring 消息监听器容器）。
	 * @param message the incoming JMS message
	 * @see #handleListenerException
	 * @see #onMessage(javax.jms.Message, javax.jms.Session)
	 */
	@Override
	public void onMessage(Message message) {
		try {
			onMessage(message, null);
		}
		catch (Throwable ex) {
			handleListenerException(ex);
		}
	}

	@Override
	public abstract void onMessage(Message message, @Nullable Session session) throws JMSException;

	/**
	 * 处理监听器执行期间出现的给定异常。
	 * 默认实现以 error 级别记录该异常。
	 * <p>此方法仅在作为标准 JMS {@link MessageListener} 使用时适用。
	 * 在 Spring {@link SessionAwareMessageListener} 机制下，异常由调用方处理。
	 * @param ex the exception to handle
	 * @see #onMessage(javax.jms.Message)
	 */
	protected void handleListenerException(Throwable ex) {
		logger.error("Listener execution failed", ex);
	}


	/**
	 * 从给定的 JMS 消息中提取消息体。
	 * @param message the JMS {@code Message}
	 * @return the content of the message, to be passed into the listener method
	 * as an argument
	 * @throws MessageConversionException if the message could not be extracted
	 */
	protected Object extractMessage(Message message)  {
		try {
			MessageConverter converter = getMessageConverter();
			if (converter != null) {
				return converter.fromMessage(message);
			}
			return message;
		}
		catch (JMSException ex) {
			throw new MessageConversionException("Could not convert JMS message", ex);
		}
	}

	/**
	 * 处理监听器方法返回的给定结果对象，并发送响应消息。
	 * @param result the result object to handle (never {@code null})
	 * @param request the original request message
	 * @param session the JMS Session to operate on (may be {@code null})
	 * @throws ReplyFailureException if the response message could not be sent
	 * @see #buildMessage
	 * @see #postProcessResponse
	 * @see #getResponseDestination
	 * @see #sendResponse
	 */
	protected void handleResult(Object result, Message request, @Nullable Session session) {
		if (session != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Listener method returned result [" + result +
						"] - generating response message for it");
			}
			try {
				Message response = buildMessage(session, result);
				postProcessResponse(request, response);
				Destination destination = getResponseDestination(request, response, session, result);
				sendResponse(session, destination, response);
			}
			catch (Exception ex) {
				throw new ReplyFailureException("Failed to send reply with payload [" + result + "]", ex);
			}
		}

		else {
			// No JMS Session available
			if (logger.isWarnEnabled()) {
				logger.warn("Listener method returned result [" + result +
						"]: not generating response message for it because of no JMS Session given");
			}
		}
	}

	/**
	 * 根据给定的结果对象构建要作为响应发送的 JMS 消息。
	 * @param session the JMS Session to operate on
	 * @param result the content of the message, as returned from the listener method
	 * @return the JMS {@code Message} (never {@code null})
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageConverter
	 */
	protected Message buildMessage(Session session, Object result) throws JMSException {
		Object content = preProcessResponse(result instanceof JmsResponse
				? ((JmsResponse<?>) result).getResponse() : result);

		MessageConverter converter = getMessageConverter();
		if (converter != null) {
			if (content instanceof org.springframework.messaging.Message) {
				return this.messagingMessageConverter.toMessage(content, session);
			}
			else {
				return converter.toMessage(content, session);
			}
		}

		if (!(content instanceof Message)) {
			throw new MessageConversionException(
					"No MessageConverter specified - cannot handle message [" + content + "]");
		}
		return (Message) content;
	}

	/**
	 * 在给定结果被转换为 {@link Message} 之前对其进行预处理。
	 * @param result the result of the invocation
	 * @return the payload response to handle, either the {@code result} argument
	 * or any other object (for instance wrapping the result).
	 * @since 4.3
	 */
	protected Object preProcessResponse(Object result) {
		return result;
	}

	/**
	 * 在发送之前对给定的响应消息进行后处理。
	 * <p>默认实现将响应的关联 ID 设置为请求消息的关联 ID（如果有）；
	 * 否则设置为请求消息的 ID。
	 * @param request the original incoming JMS message
	 * @param response the outgoing JMS message about to be sent
	 * @throws JMSException if thrown by JMS API methods
	 * @see javax.jms.Message#setJMSCorrelationID
	 */
	protected void postProcessResponse(Message request, Message response) throws JMSException {
		String correlation = request.getJMSCorrelationID();
		if (correlation == null) {
			correlation = request.getJMSMessageID();
		}
		response.setJMSCorrelationID(correlation);
	}

	private Destination getResponseDestination(Message request, Message response, Session session, Object result)
			throws JMSException {

		if (result instanceof JmsResponse) {
			JmsResponse<?> jmsResponse = (JmsResponse<?>) result;
			Destination destination = jmsResponse.resolveDestination(getDestinationResolver(), session);
			if (destination != null) {
				return destination;
			}
		}
		return getResponseDestination(request, response, session);
	}

	/**
	 * 为给定消息确定响应目的地。
	 * <p>默认实现首先检查所提供请求消息的 JMS Reply-To {@link Destination}；
	 * 如果不为 {@code null} 则返回它；如果为 {@code null}，则返回配置的
	 * {@link #resolveDefaultResponseDestination default response destination}；
	 * 如果这也为 {@code null}，则抛出 {@link javax.jms.InvalidDestinationException}。
	 * @param request the original incoming JMS message
	 * @param response the outgoing JMS message about to be sent
	 * @param session the JMS Session to operate on
	 * @return the response destination (never {@code null})
	 * @throws JMSException if thrown by JMS API methods
	 * @throws javax.jms.InvalidDestinationException if no {@link Destination} can be determined
	 * @see #setDefaultResponseDestination
	 * @see javax.jms.Message#getJMSReplyTo()
	 */
	protected Destination getResponseDestination(Message request, Message response, Session session)
			throws JMSException {

		Destination replyTo = request.getJMSReplyTo();
		if (replyTo == null) {
			replyTo = resolveDefaultResponseDestination(session);
			if (replyTo == null) {
				throw new InvalidDestinationException("Cannot determine response destination: " +
						"Request message does not contain reply-to destination, and no default response destination set.");
			}
		}
		return replyTo;
	}

	/**
	 * 使用此访问器的 {@link DestinationResolver} 将默认响应目的地
	 * （在目的地名称的情况下）解析为 JMS {@link Destination}。
	 * @return the located {@link Destination}
	 * @throws javax.jms.JMSException if resolution failed
	 * @see #setDefaultResponseDestination
	 * @see #setDefaultResponseQueueName
	 * @see #setDefaultResponseTopicName
	 * @see #setDestinationResolver
	 */
	@Nullable
	protected Destination resolveDefaultResponseDestination(Session session) throws JMSException {
		if (this.defaultResponseDestination instanceof Destination) {
			return (Destination) this.defaultResponseDestination;
		}
		if (this.defaultResponseDestination instanceof DestinationNameHolder) {
			DestinationNameHolder nameHolder = (DestinationNameHolder) this.defaultResponseDestination;
			return getDestinationResolver().resolveDestinationName(session, nameHolder.name, nameHolder.isTopic);
		}
		return null;
	}

	/**
	 * 将给定的响应消息发送到给定的目的地。
	 * @param response the JMS message to send
	 * @param destination the JMS destination to send to
	 * @param session the JMS session to operate on
	 * @throws JMSException if thrown by JMS API methods
	 * @see #postProcessProducer
	 * @see javax.jms.Session#createProducer
	 * @see javax.jms.MessageProducer#send
	 */
	protected void sendResponse(Session session, Destination destination, Message response) throws JMSException {
		MessageProducer producer = session.createProducer(destination);
		try {
			postProcessProducer(producer, response);
			QosSettings settings = getResponseQosSettings();
			if (settings != null) {
				producer.send(response, settings.getDeliveryMode(), settings.getPriority(),
						settings.getTimeToLive());
			}
			else {
				producer.send(response);
			}
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * 在使用给定的消息生产者发送响应之前对其进行后处理。
	 * <p>默认实现为空。
	 * @param producer the JMS message producer that will be used to send the message
	 * @param response the outgoing JMS message about to be sent
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected void postProcessProducer(MessageProducer producer, Message response) throws JMSException {
	}


	/**
	 * 一个 {@link MessagingMessageConverter}，它以惰性方式调用负载提取，
	 * 并将其委托给 {@link #extractMessage(javax.jms.Message)}，
	 * 以保持向后兼容性。
	 */
	private class MessagingMessageConverterAdapter extends MessagingMessageConverter {

		@Override
		public Object fromMessage(javax.jms.Message message) throws JMSException, MessageConversionException {
			return new LazyResolutionMessage(message);
		}

		@Override
		protected Object extractPayload(Message message) throws JMSException {
			Object payload = extractMessage(message);
			if (message instanceof BytesMessage) {
				try {
					// In case the BytesMessage is going to be received as a user argument:
					// reset it, otherwise it would appear empty to such processing code...
					((BytesMessage) message).reset();
				}
				catch (JMSException ex) {
					// Continue since the BytesMessage typically won't be used any further.
					logger.debug("Failed to reset BytesMessage after payload extraction", ex);
				}
			}
			return payload;
		}

		@Override
		protected Message createMessageForPayload(Object payload, Session session, @Nullable Object conversionHint)
				throws JMSException {

			MessageConverter converter = getMessageConverter();
			if (converter == null) {
				throw new IllegalStateException("No message converter, cannot handle '" + payload + "'");
			}
			if (converter instanceof SmartMessageConverter) {
				return ((SmartMessageConverter) converter).toMessage(payload, session, conversionHint);

			}
			return converter.toMessage(payload, session);
		}


		protected class LazyResolutionMessage implements org.springframework.messaging.Message<Object> {

			private final javax.jms.Message message;

			@Nullable
			private Object payload;

			@Nullable
			private MessageHeaders headers;

			public LazyResolutionMessage(javax.jms.Message message) {
				this.message = message;
			}

			@Override
			public Object getPayload() {
				if (this.payload == null) {
					try {
						this.payload = unwrapPayload();
					}
					catch (JMSException ex) {
						throw new MessageConversionException(
								"Failed to extract payload from [" + this.message + "]", ex);
					}
				}
				return this.payload;
			}

			/**
			 * 提取当前消息的负载。由于我们推迟了负载的解析，自定义转换器仍可能
			 * 为其返回一条完整消息。在这种情况下，将返回该消息的负载。
			 * @return the payload of the message
			 */
			@SuppressWarnings("rawtypes")
			private Object unwrapPayload() throws JMSException {
				Object payload = extractPayload(this.message);
				if (payload instanceof org.springframework.messaging.Message) {
					return ((org.springframework.messaging.Message) payload).getPayload();
				}
				return payload;
			}

			@Override
			public MessageHeaders getHeaders() {
				if (this.headers == null) {
					this.headers = extractHeaders(this.message);
				}
				return this.headers;
			}
		}
	}


	/**
	 * 内部类，组合了目的地名称及其目标目的地类型（队列或主题）。
	 */
	private static class DestinationNameHolder {

		public final String name;

		public final boolean isTopic;

		public DestinationNameHolder(String name, boolean isTopic) {
			this.name = name;
			this.isTopic = isTopic;
		}
	}

}
