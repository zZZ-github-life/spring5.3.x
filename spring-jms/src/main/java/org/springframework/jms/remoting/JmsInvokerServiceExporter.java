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

package org.springframework.jms.remoting;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * JMS 消息监听器，将指定的服务 Bean 导出为
 * JMS 服务端点，可通过 JMS 调用器代理访问。
 *
 * <p>请注意，此类实现了 Spring 的
 * {@link org.springframework.jms.listener.SessionAwareMessageListener}
 * 接口，因为它需要访问活动的 JMS Session。
 * 因此，此类只能与支持 SessionAwareMessageListener 接口的
 * 消息监听器容器（例如 Spring 的
 * {@link org.springframework.jms.listener.DefaultMessageListenerContainer}）一起使用。
 *
 * <p>感谢 James Strachan 提供了启发此 JMS 调用器机制的原始原型！
 *
 * @author Juergen Hoeller
 * @author James Strachan
 * @since 2.0
 * @see JmsInvokerClientInterceptor
 * @see JmsInvokerProxyFactoryBean
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class JmsInvokerServiceExporter extends RemoteInvocationBasedExporter
		implements SessionAwareMessageListener<Message>, InitializingBean {

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private boolean ignoreInvalidRequests = true;

	@Nullable
	private Object proxy;


	/**
	 * 指定用于将请求消息转换为
	 * {@link org.springframework.remoting.support.RemoteInvocation} 对象，
	 * 以及将 {@link org.springframework.remoting.support.RemoteInvocationResult}
	 * 对象转换为响应消息的 MessageConverter。
	 * <p>默认是 {@link org.springframework.jms.support.converter.SimpleMessageConverter}，
	 * 为每个调用/调用结果对象使用标准的 JMS {@link javax.jms.ObjectMessage}。
	 * <p>自定义实现通常可以将 Serializable 适配为特殊类型的消息，
	 * 或者专门定制用于将 RemoteInvocation(Result) 转换为特定类型的消息。
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = (messageConverter != null ? messageConverter : new SimpleMessageConverter());
	}

	/**
	 * 设置是否丢弃格式无效的消息。默认为 "true"。
	 * <p>将此标志切换为 "false" 可将异常抛回监听器容器。
	 * 这通常会导致消息重新投递，而这通常是不可取的——
	 * 因为消息内容仍然相同（即仍然无效）。
	 */
	public void setIgnoreInvalidRequests(boolean ignoreInvalidRequests) {
		this.ignoreInvalidRequests = ignoreInvalidRequests;
	}

	@Override
	public void afterPropertiesSet() {
		this.proxy = getProxyForService();
	}


	@Override
	public void onMessage(Message requestMessage, Session session) throws JMSException {
		RemoteInvocation invocation = readRemoteInvocation(requestMessage);
		if (invocation != null) {
			RemoteInvocationResult result = invokeAndCreateResult(invocation, this.proxy);
			writeRemoteInvocationResult(requestMessage, session, result);
		}
	}

	/**
	 * 从给定的 JMS 消息中读取 RemoteInvocation。
	 * @param requestMessage 当前请求消息
	 * @return RemoteInvocation 对象（如果消息无效且将被直接忽略，则为 {@code null}）
	 * @throws javax.jms.JMSException 消息访问失败时
	 */
	@Nullable
	protected RemoteInvocation readRemoteInvocation(Message requestMessage) throws JMSException {
		Object content = this.messageConverter.fromMessage(requestMessage);
		if (content instanceof RemoteInvocation) {
			return (RemoteInvocation) content;
		}
		return onInvalidRequest(requestMessage);
	}


	/**
	 * 将给定的 RemoteInvocationResult 作为 JMS 消息发送给发起方。
	 * @param requestMessage 当前请求消息
	 * @param session 要使用的 JMS Session
	 * @param result RemoteInvocationResult 对象
	 * @throws javax.jms.JMSException 如果尝试发送消息时抛出
	 */
	protected void writeRemoteInvocationResult(
			Message requestMessage, Session session, RemoteInvocationResult result) throws JMSException {

		Message response = createResponseMessage(requestMessage, session, result);
		MessageProducer producer = session.createProducer(requestMessage.getJMSReplyTo());
		try {
			producer.send(response);
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * 创建调用结果响应消息。
	 * <p>默认实现为给定的 RemoteInvocationResult 对象创建一个 JMS ObjectMessage。
	 * 它将响应的关联 ID 设置为请求消息的关联 ID（如果存在）；
	 * 否则设置为请求消息 ID。
	 * @param request 原始请求消息
	 * @param session 要使用的 JMS Session
	 * @param result 调用结果
	 * @return 要发送的响应消息
	 * @throws javax.jms.JMSException 如果创建消息失败
	 */
	protected Message createResponseMessage(Message request, Session session, RemoteInvocationResult result)
			throws JMSException {

		Message response = this.messageConverter.toMessage(result, session);
		String correlation = request.getJMSCorrelationID();
		if (correlation == null) {
			correlation = request.getJMSMessageID();
		}
		response.setJMSCorrelationID(correlation);
		return response;
	}

	/**
	 * 由 {@link #readRemoteInvocation} 在遇到无效请求消息时调用的回调。
	 * <p>默认实现会根据 "ignoreInvalidRequests" 标志
	 * 丢弃无效消息或抛出 MessageFormatException，
	 * 该标志默认设置为 "true"（即丢弃无效消息）。
	 * @param requestMessage 无效的请求消息
	 * @return 为无效请求暴露的 RemoteInvocation
	 * （通常，如果消息无效且将被直接忽略，则为 {@code null}）
	 * @throws javax.jms.JMSException 如果无效请求应导致异常（而不是被忽略）
	 * @see #readRemoteInvocation
	 * @see #setIgnoreInvalidRequests
	 */
	@Nullable
	protected RemoteInvocation onInvalidRequest(Message requestMessage) throws JMSException {
		if (this.ignoreInvalidRequests) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invalid request message will be discarded: " + requestMessage);
			}
			return null;
		}
		else {
			throw new MessageFormatException("Invalid request message: " + requestMessage);
		}
	}

}
