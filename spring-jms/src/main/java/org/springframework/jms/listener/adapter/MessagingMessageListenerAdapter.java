/*
 * Copyright 2002-2023 the original author or authors.
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

import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.core.MethodParameter;
import org.springframework.jms.listener.SubscriptionNameProvider;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * {@link javax.jms.MessageListener} 适配器，调用可配置的
 * {@link InvocableHandlerMethod}。
 *
 * <p>将传入的 {@link javax.jms.Message} 包装到 Spring 的 {@link Message}
 * 抽象中，并使用可配置的 {@link JmsHeaderMapper} 复制 JMS 标准消息头。
 *
 * <p>原始的 {@link javax.jms.Message} 和 {@link javax.jms.Session}
 * 作为额外参数提供，以便在需要时可以将它们注入为方法参数。
 *
 * <p>从 Spring Framework 5.3.26 开始，{@code MessagingMessageListenerAdapter}
 * 实现 {@link SubscriptionNameProvider}，以便提供有意义的默认订阅名称。
 * 详情请参见 {@link #getSubscriptionName()}。
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.1
 * @see Message
 * @see JmsHeaderMapper
 * @see InvocableHandlerMethod
 */
public class MessagingMessageListenerAdapter extends AbstractAdaptableMessageListener
		implements SubscriptionNameProvider {

	@Nullable
	private InvocableHandlerMethod handlerMethod;


	/**
	 * 设置用于调用处理传入 {@link javax.jms.Message} 的方法的
	 * {@link InvocableHandlerMethod}。
	 */
	public void setHandlerMethod(InvocableHandlerMethod handlerMethod) {
		this.handlerMethod = handlerMethod;
	}

	private InvocableHandlerMethod getHandlerMethod() {
		Assert.state(this.handlerMethod != null, "No HandlerMethod set");
		return this.handlerMethod;
	}


	@Override
	public void onMessage(javax.jms.Message jmsMessage, @Nullable Session session) throws JMSException {
		Message<?> message = toMessagingMessage(jmsMessage);
		if (logger.isDebugEnabled()) {
			logger.debug("Processing [" + message + "]");
		}
		Object result = invokeHandler(jmsMessage, session, message);
		if (result != null) {
			handleResult(result, jmsMessage, session);
		}
		else {
			logger.trace("No result object given - no result to handle");
		}
	}

	protected Message<?> toMessagingMessage(javax.jms.Message jmsMessage) {
		try {
			return (Message<?>) getMessagingMessageConverter().fromMessage(jmsMessage);
		}
		catch (JMSException ex) {
			throw new MessageConversionException("Could not convert JMS message", ex);
		}
	}

	/**
	 * 调用处理器，将任何异常包装到带有专属错误消息的
	 * {@link ListenerExecutionFailedException} 中。
	 */
	@Nullable
	private Object invokeHandler(javax.jms.Message jmsMessage, @Nullable Session session, Message<?> message) {
		InvocableHandlerMethod handlerMethod = getHandlerMethod();
		try {
			return handlerMethod.invoke(message, jmsMessage, session);
		}
		catch (MessagingException ex) {
			throw new ListenerExecutionFailedException(
					createMessagingErrorMessage("Listener method could not be invoked with incoming message"), ex);
		}
		catch (Exception ex) {
			throw new ListenerExecutionFailedException("Listener method '" +
					handlerMethod.getMethod().toGenericString() + "' threw exception", ex);
		}
	}

	private String createMessagingErrorMessage(String description) {
		InvocableHandlerMethod handlerMethod = getHandlerMethod();
		StringBuilder sb = new StringBuilder(description).append('\n')
				.append("Endpoint handler details:\n")
				.append("Method [").append(handlerMethod.getMethod()).append("]\n")
				.append("Bean [").append(handlerMethod.getBean()).append("]\n");
		return sb.toString();
	}

	@Override
	protected Object preProcessResponse(Object result) {
		MethodParameter returnType = getHandlerMethod().getReturnType();
		MessageBuilder<?> messageBuilder = (result instanceof Message ?
				MessageBuilder.fromMessage((Message<?>) result) :
				MessageBuilder.withPayload(result));
		return messageBuilder
				.setHeader(AbstractMessageSendingTemplate.CONVERSION_HINT_HEADER, returnType)
				.build();
	}

	/**
	 * 根据以下规则为这个 {@code MessageListener} 适配器生成订阅名称。
	 * <ul>
	 * <li>如果已设置 {@link #setHandlerMethod(InvocableHandlerMethod) handlerMethod}，
	 * 生成的订阅名称形式为
	 * {@code handlerMethod.getBeanType().getName() + "." + handlerMethod.getMethod().getName()}。</li>
	 * <li>否则，生成的订阅名称是调用 {@code getClass().getName()} 的结果，
	 * 这与 {@link org.springframework.jms.listener.AbstractMessageListenerContainer}
	 * 的默认行为一致。</li>
	 * </ul>
	 * @since 5.3.26
	 * @see SubscriptionNameProvider#getSubscriptionName()
	 */
	@Override
	public String getSubscriptionName() {
		if (this.handlerMethod != null) {
			return this.handlerMethod.getBeanType().getName() + "." + this.handlerMethod.getMethod().getName();
		}
		else {
			return getClass().getName();
		}
	}

}
