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

package org.springframework.jms.remoting;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.remoting.support.DefaultRemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;

/**
 * 用于访问基于 JMS 的远程服务的 {@link org.aopalliance.intercept.MethodInterceptor}。
 *
 * <p>序列化远程调用对象并反序列化远程调用结果对象。与 RMI 一样使用 Java 序列化，
 * 但以 JMS 提供者作为通信基础设施。
 *
 * <p>需要配置一个 {@link javax.jms.QueueConnectionFactory} 和一个目标队列
 * （可以是 {@link javax.jms.Queue} 引用，也可以是队列名称）。
 *
 * <p>感谢 James Strachan 提供了原始原型，此 JMS 调用者机制正是受其启发！
 *
 * @author Juergen Hoeller
 * @author James Strachan
 * @author Stephane Nicoll
 * @since 2.0
 * @see #setConnectionFactory
 * @see #setQueue
 * @see #setQueueName
 * @see org.springframework.jms.remoting.JmsInvokerServiceExporter
 * @see org.springframework.jms.remoting.JmsInvokerProxyFactoryBean
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class JmsInvokerClientInterceptor implements MethodInterceptor, InitializingBean {

	@Nullable
	private ConnectionFactory connectionFactory;

	@Nullable
	private Object queue;

	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private RemoteInvocationFactory remoteInvocationFactory = new DefaultRemoteInvocationFactory();

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private long receiveTimeout = 0;


	/**
	 * 设置用于获取 JMS QueueConnection 的 QueueConnectionFactory。
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * 返回用于获取 JMS QueueConnection 的 QueueConnectionFactory。
	 */
	@Nullable
	protected ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * 设置用于发送调用者请求的目标 Queue。
	 */
	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	/**
	 * 设置用于发送调用者请求的目标队列名称。
	 * <p>指定的名称将通过 {@link #setDestinationResolver DestinationResolver} 动态解析。
	 */
	public void setQueueName(String queueName) {
		this.queue = queueName;
	}

	/**
	 * 设置用于为此访问器解析 Queue 引用的 DestinationResolver。
	 * <p>默认解析器是 {@code DynamicDestinationResolver}。如果需要将目的地名称
	 * 作为 JNDI 位置解析，请指定 {@code JndiDestinationResolver}。
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.destination.JndiDestinationResolver
	 */
	public void setDestinationResolver(@Nullable DestinationResolver destinationResolver) {
		this.destinationResolver =
				(destinationResolver != null ? destinationResolver : new DynamicDestinationResolver());
	}

	/**
	 * 设置此访问器要使用的 {@link RemoteInvocationFactory}。
	 * <p>默认是 {@link DefaultRemoteInvocationFactory}。
	 * <p>自定义调用工厂可以向调用添加更多上下文信息，例如用户凭证。
	 */
	public void setRemoteInvocationFactory(@Nullable RemoteInvocationFactory remoteInvocationFactory) {
		this.remoteInvocationFactory =
				(remoteInvocationFactory != null ? remoteInvocationFactory : new DefaultRemoteInvocationFactory());
	}

	/**
	 * 指定用于将 {@link org.springframework.remoting.support.RemoteInvocation} 对象
	 * 转换为请求消息，以及将响应消息转换为
	 * {@link org.springframework.remoting.support.RemoteInvocationResult} 对象的
	 * {@link MessageConverter}。
	 * <p>默认是 {@link SimpleMessageConverter}，为每个调用/调用结果对象使用
	 * 标准的 JMS {@link javax.jms.ObjectMessage}。
	 * <p>自定义实现通常可以将 {@link java.io.Serializable} 对象适配为特殊类型的消息，
	 * 或者专门用于将 {@code RemoteInvocation(Result)} 转换为特定类型的消息。
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = (messageConverter != null ? messageConverter : new SimpleMessageConverter());
	}

	/**
	 * 设置接收请求响应消息所用的超时时间（单位为毫秒）。
	 * <p>默认值为 0，表示无超时的阻塞接收。
	 * @see javax.jms.MessageConsumer#receive(long)
	 * @see javax.jms.MessageConsumer#receive()
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * 返回接收请求响应消息所用的超时时间（单位为毫秒）。
	 */
	protected long getReceiveTimeout() {
		return this.receiveTimeout;
	}


	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
		if (this.queue == null) {
			throw new IllegalArgumentException("'queue' or 'queueName' is required");
		}
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "JMS invoker proxy for queue [" + this.queue + "]";
		}

		RemoteInvocation invocation = createRemoteInvocation(methodInvocation);
		RemoteInvocationResult result;
		try {
			result = executeRequest(invocation);
		}
		catch (JMSException ex) {
			throw convertJmsInvokerAccessException(ex);
		}
		try {
			return recreateRemoteInvocationResult(result);
		}
		catch (Throwable ex) {
			if (result.hasInvocationTargetException()) {
				throw ex;
			}
			else {
				throw new RemoteInvocationFailureException("Invocation of method [" + methodInvocation.getMethod() +
						"] failed in JMS invoker remote service at queue [" + this.queue + "]", ex);
			}
		}
	}

	/**
	 * 为给定的 AOP 方法调用创建一个新的 {@code RemoteInvocation} 对象。
	 * <p>默认实现委托给 {@link RemoteInvocationFactory}。
	 * <p>可以在子类中重写，以提供自定义的 {@code RemoteInvocation} 子类，
	 * 其中包含额外的调用参数（例如用户凭证）。
	 * 请注意，更可取的做法是使用自定义的 {@code RemoteInvocationFactory}，
	 * 它是一种可复用的策略。
	 * @param methodInvocation 当前的 AOP 方法调用
	 * @return RemoteInvocation 对象
	 * @see RemoteInvocationFactory#createRemoteInvocation
	 */
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return this.remoteInvocationFactory.createRemoteInvocation(methodInvocation);
	}

	/**
	 * 执行给定的远程调用：向此访问器的目标队列发送调用者请求消息，
	 * 并等待相应的响应。
	 * @param invocation 要执行的 RemoteInvocation
	 * @return RemoteInvocationResult 对象
	 * @throws JMSException JMS 失败时
	 * @see #doExecuteRequest
	 */
	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation) throws JMSException {
		Connection con = createConnection();
		Session session = null;
		try {
			session = createSession(con);
			Queue queueToUse = resolveQueue(session);
			Message requestMessage = createRequestMessage(session, invocation);
			con.start();
			Message responseMessage = doExecuteRequest(session, queueToUse, requestMessage);
			if (responseMessage != null) {
				return extractInvocationResult(responseMessage);
			}
			else {
				return onReceiveTimeout(invocation);
			}
		}
		finally {
			JmsUtils.closeSession(session);
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory(), true);
		}
	}

	/**
	 * 为此 JMS 调用者创建一个新的 JMS Connection。
	 */
	protected Connection createConnection() throws JMSException {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory.createConnection();
	}

	/**
	 * 为此 JMS 调用者创建一个新的 JMS Session。
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	/**
	 * 解析此访问器的目标队列。
	 * @param session 当前的 JMS Session
	 * @return 解析出的目标 Queue
	 * @throws JMSException 解析失败时
	 */
	protected Queue resolveQueue(Session session) throws JMSException {
		if (this.queue instanceof Queue) {
			return (Queue) this.queue;
		}
		else if (this.queue instanceof String) {
			return resolveQueueName(session, (String) this.queue);
		}
		else {
			throw new javax.jms.IllegalStateException(
					"Queue object [" + this.queue + "] is neither a [javax.jms.Queue] nor a queue name String");
		}
	}

	/**
	 * 通过此访问器的 {@link DestinationResolver} 将给定的队列名称解析为
	 * JMS {@link javax.jms.Queue}。
	 * @param session 当前的 JMS Session
	 * @param queueName 队列名称
	 * @return 定位到的 Queue
	 * @throws JMSException 解析失败时
	 * @see #setDestinationResolver
	 */
	protected Queue resolveQueueName(Session session, String queueName) throws JMSException {
		return (Queue) this.destinationResolver.resolveDestinationName(session, queueName, false);
	}

	/**
	 * 创建调用者请求消息。
	 * <p>默认实现为给定的 RemoteInvocation 对象创建一个 JMS
	 * {@link javax.jms.ObjectMessage}。
	 * @param session 当前的 JMS Session
	 * @param invocation 要发送的远程调用
	 * @return 要发送的 JMS Message
	 * @throws JMSException 如果无法创建消息
	 */
	protected Message createRequestMessage(Session session, RemoteInvocation invocation) throws JMSException {
		return this.messageConverter.toMessage(invocation, session);
	}

	/**
	 * 实际执行给定的请求：将调用者请求消息发送到指定的目标队列，
	 * 并等待相应的响应。
	 * <p>默认实现基于标准的 JMS 发送/接收，使用 {@link javax.jms.TemporaryQueue}
	 * 来接收响应。
	 * @param session 要使用的 JMS Session
	 * @param queue 要发送到的已解析目标 Queue
	 * @param requestMessage 要发送的 JMS Message
	 * @return RemoteInvocationResult 对象
	 * @throws JMSException JMS 失败时
	 */
	@Nullable
	protected Message doExecuteRequest(Session session, Queue queue, Message requestMessage) throws JMSException {
		TemporaryQueue responseQueue = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			responseQueue = session.createTemporaryQueue();
			producer = session.createProducer(queue);
			consumer = session.createConsumer(responseQueue);
			requestMessage.setJMSReplyTo(responseQueue);
			producer.send(requestMessage);
			long timeout = getReceiveTimeout();
			return (timeout > 0 ? consumer.receive(timeout) : consumer.receive());
		}
		finally {
			JmsUtils.closeMessageConsumer(consumer);
			JmsUtils.closeMessageProducer(producer);
			if (responseQueue != null) {
				responseQueue.delete();
			}
		}
	}

	/**
	 * 从响应消息中提取调用结果。
	 * <p>默认实现期望一个携带 {@link RemoteInvocationResult} 对象的
	 * JMS {@link javax.jms.ObjectMessage}。如果遇到无效的响应消息，
	 * 将调用 {@code onInvalidResponse} 回调。
	 * @param responseMessage 响应消息
	 * @return 调用结果
	 * @throws JMSException 如果发生 JMS 异常则抛出
	 * @see #onInvalidResponse
	 */
	protected RemoteInvocationResult extractInvocationResult(Message responseMessage) throws JMSException {
		Object content = this.messageConverter.fromMessage(responseMessage);
		if (content instanceof RemoteInvocationResult) {
			return (RemoteInvocationResult) content;
		}
		return onInvalidResponse(responseMessage);
	}

	/**
	 * 当指定的 {@link RemoteInvocation} 的接收超时已过时，由
	 * {@link #executeRequest} 调用的回调。
	 * <p>默认情况下会抛出 {@link RemoteTimeoutException}。子类可以选择抛出
	 * 更专门的异常，甚至返回一个默认的 {@link RemoteInvocationResult} 作为后备。
	 * @param invocation 该调用
	 * @return 接收超时后返回的默认结果
	 */
	protected RemoteInvocationResult onReceiveTimeout(RemoteInvocation invocation) {
		throw new RemoteTimeoutException("Receive timeout after " + this.receiveTimeout + " ms for " + invocation);
	}

	/**
	 * 当 {@link #extractInvocationResult} 遇到无效的响应消息时调用的回调。
	 * <p>默认实现抛出 {@link MessageFormatException}。
	 * @param responseMessage 无效的响应消息
	 * @return 应返回给调用方的替代调用结果（如需）
	 * @throws JMSException 如果无效响应应导致向调用方传播基础设施异常
	 * @see #extractInvocationResult
	 */
	protected RemoteInvocationResult onInvalidResponse(Message responseMessage) throws JMSException {
		throw new MessageFormatException("Invalid response message: " + responseMessage);
	}

	/**
	 * 重建给定 {@link RemoteInvocationResult} 对象中包含的调用结果。
	 * <p>默认实现调用默认的 {@code recreate()} 方法。
	 * <p>可以在子类中重写以提供自定义的重建逻辑，可能还会处理返回的结果对象。
	 * @param result 要重建的 RemoteInvocationResult
	 * @return 如果调用结果为成功返回，则返回该返回值
	 * @throws Throwable 如果调用结果为异常
	 * @see org.springframework.remoting.support.RemoteInvocationResult#recreate()
	 */
	@Nullable
	protected Object recreateRemoteInvocationResult(RemoteInvocationResult result) throws Throwable {
		return result.recreate();
	}

	/**
	 * 将给定的 JMS 调用者访问异常转换为适当的 Spring {@link RemoteAccessException}。
	 * @param ex 要转换的异常
	 * @return 要抛出的 RemoteAccessException
	 */
	protected RemoteAccessException convertJmsInvokerAccessException(JMSException ex) {
		return new RemoteAccessException("Could not access JMS invoker queue [" + this.queue + "]", ex);
	}

}
