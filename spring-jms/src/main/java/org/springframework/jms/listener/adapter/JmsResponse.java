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

package org.springframework.jms.listener.adapter;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 任何 JMS 监听器方法的返回类型，用于在返回响应的同时指明实际的响应目的地。
 * 通常用于需要在运行时计算该目的地的情况。
 *
 * <p>下面的示例将内容为 {@code result} 参数的响应发送到 {@code queueOut Queue}：
 *
 * <pre class="code">
 * package com.acme.foo;
 *
 * public class MyService {
 *     &#064;JmsListener
 *     public JmsResponse process(String msg) {
 *         // process incoming message
 *         return JmsResponse.forQueue(result, "queueOut");
 *     }
 * }</pre>
 *
 * 如果目的地不需要在运行时计算，推荐使用声明式的
 * {@link org.springframework.messaging.handler.annotation.SendTo @SendTo}。
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @param <T> 响应的类型
 * @see org.springframework.jms.annotation.JmsListener
 * @see org.springframework.messaging.handler.annotation.SendTo
 */
public class JmsResponse<T> {

	private final T response;

	private final Object destination;


	/**
	 * 创建一个新的 {@link JmsResponse} 实例。
	 * @param response 结果的内容
	 * @param destination 目标
	 */
	protected JmsResponse(T response, Object destination) {
		Assert.notNull(response, "Result must not be null");
		this.response = response;
		this.destination = destination;
	}


	/**
	 * 返回响应的内容。
	 */
	public T getResponse() {
		return this.response;
	}

	/**
	 * 解析此实例要使用的 {@link Destination}。
	 * 可以在运行时使用 {@link DestinationResolver} 和 {@link Session} 解析目的地。
	 * @param destinationResolver 必要时使用的目标解析器
	 * @param session 必要时使用的 Session
	 * @return 要使用的 {@link Destination}
	 * @throws JMSException 如果 DestinationResolver 未能解析目标
	 */
	@Nullable
	public Destination resolveDestination(DestinationResolver destinationResolver, Session session)
			throws JMSException {

		if (this.destination instanceof Destination) {
			return (Destination) this.destination;
		}
		if (this.destination instanceof DestinationNameHolder) {
			DestinationNameHolder nameHolder = (DestinationNameHolder) this.destination;
			return destinationResolver.resolveDestinationName(session,
					nameHolder.destinationName, nameHolder.pubSubDomain);
		}
		return null;
	}

	@Override
	public String toString() {
		return "JmsResponse [" + "response=" + this.response + ", destination=" + this.destination + ']';
	}


	/**
	 * 创建针对具有指定名称的队列的 {@link JmsResponse}。
	 */
	public static <T> JmsResponse<T> forQueue(T result, String queueName) {
		Assert.notNull(queueName, "Queue name must not be null");
		return new JmsResponse<>(result, new DestinationNameHolder(queueName, false));
	}

	/**
	 * 创建针对具有指定名称的主题的 {@link JmsResponse}。
	 */
	public static <T> JmsResponse<T> forTopic(T result, String topicName) {
		Assert.notNull(topicName, "Topic name must not be null");
		return new JmsResponse<>(result, new DestinationNameHolder(topicName, true));
	}

	/**
	 * 创建针对指定 {@link Destination} 的 {@link JmsResponse}。
	 */
	public static <T> JmsResponse<T> forDestination(T result, Destination destination) {
		Assert.notNull(destination, "Destination must not be null");
		return new JmsResponse<>(result, destination);
	}


	/**
	 * 内部类，组合目的地名称及其目标目的地类型（队列或主题）。
	 */
	private static class DestinationNameHolder {

		private final String destinationName;

		private final boolean pubSubDomain;

		public DestinationNameHolder(String destinationName, boolean pubSubDomain) {
			this.destinationName = destinationName;
			this.pubSubDomain = pubSubDomain;
		}

		@Override
		public String toString() {
			return this.destinationName + "{" + "pubSubDomain=" + this.pubSubDomain + '}';
		}
	}

}
