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

package org.springframework.jms.core;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Queue;

import org.springframework.jms.JmsException;
import org.springframework.lang.Nullable;

/**
 * 指定一组基本的 JMS 操作。
 *
 * <p>由 {@link JmsTemplate} 实现。不常使用，但作为增强可测试性的选项很有用，
 * 因为它很容易被 mock 或 stub。
 *
 * <p>提供 {@code JmsTemplate} 的 {@code send(..)} 和 {@code receive(..)} 方法，
 * 对应各种 JMS API 方法。有关这些方法的详细信息，请参阅 JMS 规范和 javadoc。
 *
 * <p>还提供使用临时队列收集回复的基本请求-回复操作。
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 1.1
 * @see JmsTemplate
 * @see javax.jms.Destination
 * @see javax.jms.Session
 * @see javax.jms.MessageProducer
 * @see javax.jms.MessageConsumer
 */
public interface JmsOperations {

	/**
	 * 在 JMS Session 中执行由给定操作对象指定的操作。
	 * @param action callback object that exposes the session
	 * @return the result object from working with the session
	 * @throws JmsException if there is any problem
	 */
	@Nullable
	<T> T execute(SessionCallback<T> action) throws JmsException;

	/**
	 * 向默认 JMS 目标（或为每次发送操作指定的目标）发送消息。回调提供对
	 * JMS Session 和 MessageProducer 的访问，以执行复杂的发送操作。
	 * @param action callback object that exposes the session/producer pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	<T> T execute(ProducerCallback<T> action) throws JmsException;

	/**
	 * 向 JMS 目标发送消息。回调提供对 JMS Session 和 MessageProducer 的访问，
	 * 以执行复杂的发送操作。
	 * @param destination the destination to send messages to
	 * @param action callback object that exposes the session/producer pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	<T> T execute(Destination destination, ProducerCallback<T> action) throws JmsException;

	/**
	 * 向 JMS 目标发送消息。回调提供对 JMS Session 和 MessageProducer 的访问，
	 * 以执行复杂的发送操作。
	 * @param destinationName the name of the destination to send messages to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param action callback object that exposes the session/producer pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	<T> T execute(String destinationName, ProducerCallback<T> action) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages
	//---------------------------------------------------------------------------------------

	/**
	 * 向默认目标发送消息。
	 * <p>这仅在指定了默认目标时有效！
	 * @param messageCreator callback to create a message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void send(MessageCreator messageCreator) throws JmsException;

	/**
	 * 向指定的目标发送消息。
	 * MessageCreator 回调根据 Session 创建消息。
	 * @param destination the destination to send this message to
	 * @param messageCreator callback to create a message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void send(Destination destination, MessageCreator messageCreator) throws JmsException;

	/**
	 * 向指定的目标发送消息。
	 * MessageCreator 回调根据 Session 创建消息。
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param messageCreator callback to create a message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void send(String destinationName, MessageCreator messageCreator) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending auto-converted messages
	//---------------------------------------------------------------------------------------

	/**
	 * 将给定对象发送到默认目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。
	 * <p>这仅在指定了默认目标时有效！
	 * @param message the object to convert to a message
	 * @throws JmsException converted checked JMSException to unchecked
	 */
	void convertAndSend(Object message) throws JmsException;

	/**
	 * 将给定对象发送到指定的目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。
	 * @param destination the destination to send this message to
	 * @param message the object to convert to a message
	 * @throws JmsException converted checked JMSException to unchecked
	 */
	void convertAndSend(Destination destination, Object message) throws JmsException;

	/**
	 * 将给定对象发送到指定的目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param message the object to convert to a message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void convertAndSend(String destinationName, Object message) throws JmsException;

	/**
	 * 将给定对象发送到默认目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。MessagePostProcessor 回调允许在转换后修改消息。
	 * <p>这仅在指定了默认目标时有效！
	 * @param message the object to convert to a message
	 * @param postProcessor the callback to modify the message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void convertAndSend(Object message, MessagePostProcessor postProcessor)
		throws JmsException;

	/**
	 * 将给定对象发送到指定的目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。MessagePostProcessor 回调允许在转换后修改消息。
	 * @param destination the destination to send this message to
	 * @param message the object to convert to a message
	 * @param postProcessor the callback to modify the message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void convertAndSend(Destination destination, Object message, MessagePostProcessor postProcessor)
		throws JmsException;

	/**
	 * 将给定对象发送到指定的目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。MessagePostProcessor 回调允许在转换后修改消息。
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param message the object to convert to a message.
	 * @param postProcessor the callback to modify the message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void convertAndSend(String destinationName, Object message, MessagePostProcessor postProcessor)
		throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving messages
	//---------------------------------------------------------------------------------------

	/**
	 * 从默认目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * <p>这仅在指定了默认目标时有效！
	 * @return the message received by the consumer, or {@code null} if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Message receive() throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destination the destination to receive a message from
	 * @return the message received by the consumer, or {@code null} if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Message receive(Destination destination) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @return the message received by the consumer, or {@code null} if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Message receive(String destinationName) throws JmsException;

	/**
	 * 从默认目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * <p>这仅在指定了默认目标时有效！
	 * @param messageSelector the JMS message selector expression (or {@code null} if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message received by the consumer, or {@code null} if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Message receiveSelected(String messageSelector) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destination the destination to receive a message from
	 * @param messageSelector the JMS message selector expression (or {@code null} if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message received by the consumer, or {@code null} if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Message receiveSelected(Destination destination, String messageSelector) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param messageSelector the JMS message selector expression (or {@code null} if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message received by the consumer, or {@code null} if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Message receiveSelected(String destinationName, String messageSelector) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving auto-converted messages
	//---------------------------------------------------------------------------------------

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * <p>这仅在指定了默认目标时有效！
	 * @return the message produced for the consumer or {@code null} if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Object receiveAndConvert() throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。使用配置的
	 * MessageConverter 将消息转换为对象。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destination the destination to receive a message from
	 * @return the message produced for the consumer or {@code null} if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Object receiveAndConvert(Destination destination) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。使用配置的
	 * MessageConverter 将消息转换为对象。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @return the message produced for the consumer or {@code null} if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Object receiveAndConvert(String destinationName) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * <p>这仅在指定了默认目标时有效！
	 * @param messageSelector the JMS message selector expression (or {@code null} if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message produced for the consumer or {@code null} if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Object receiveSelectedAndConvert(String messageSelector) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。使用配置的
	 * MessageConverter 将消息转换为对象。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destination the destination to receive a message from
	 * @param messageSelector the JMS message selector expression (or {@code null} if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message produced for the consumer or {@code null} if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。使用配置的
	 * MessageConverter 将消息转换为对象。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param messageSelector the JMS message selector expression (or {@code null} if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message produced for the consumer or {@code null} if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	Object receiveSelectedAndConvert(String destinationName, String messageSelector) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages to and receiving the reply from a destination
	//---------------------------------------------------------------------------------------

	/**
	 * 发送请求消息并从默认目标接收回复。{@link MessageCreator} 回调根据
	 * Session 创建消息。此操作会创建临时队列，并将其设置到消息的
	 * {@code JMSReplyTO} 头中。
	 * <p>这仅在指定了默认目标时有效！
	 * @param messageCreator callback to create a request message
	 * @return the reply, possibly {@code null} if the message could not be received,
	 * for example due to a timeout
	 * @throws JmsException checked JMSException converted to unchecked
	 * @since 4.1
	 */
	@Nullable
	Message sendAndReceive(MessageCreator messageCreator) throws JmsException;

	/**
	 * 发送消息并从指定的目标接收回复。{@link MessageCreator} 回调根据
	 * Session 创建消息。此操作会创建临时队列，并将其设置到消息的
	 * {@code JMSReplyTO} 头中。
	 * @param destination the destination to send this message to
	 * @param messageCreator callback to create a message
	 * @return the reply, possibly {@code null} if the message could not be received,
	 * for example due to a timeout
	 * @throws JmsException checked JMSException converted to unchecked
	 * @since 4.1
	 */
	@Nullable
	Message sendAndReceive(Destination destination, MessageCreator messageCreator) throws JmsException;

	/**
	 * 发送消息并从指定的目标接收回复。{@link MessageCreator} 回调根据
	 * Session 创建消息。此操作会创建临时队列，并将其设置到消息的
	 * {@code JMSReplyTO} 头中。
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param messageCreator callback to create a message
	 * @return the reply, possibly {@code null} if the message could not be received,
	 * for example due to a timeout
	 * @throws JmsException checked JMSException converted to unchecked
	 * @since 4.1
	 */
	@Nullable
	Message sendAndReceive(String destinationName, MessageCreator messageCreator) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for browsing messages
	//---------------------------------------------------------------------------------------

	/**
	 * 浏览默认 JMS 队列中的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	<T> T browse(BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param queue the queue to browse
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	<T> T browse(Queue queue, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param queueName the name of the queue to browse
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	<T> T browse(String queueName, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中选定的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param messageSelector the JMS message selector expression (or {@code null} if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	<T> T browseSelected(String messageSelector, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中选定的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param queue the queue to browse
	 * @param messageSelector the JMS message selector expression (or {@code null} if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	<T> T browseSelected(Queue queue, String messageSelector, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中选定的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param queueName the name of the queue to browse
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param messageSelector the JMS message selector expression (or {@code null} if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	@Nullable
	<T> T browseSelected(String queueName, String messageSelector, BrowserCallback<T> action) throws JmsException;

}
