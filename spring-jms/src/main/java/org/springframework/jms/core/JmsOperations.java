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
	 * @param action 暴露 Session 的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 如果出现任何问题
	 */
	@Nullable
	<T> T execute(SessionCallback<T> action) throws JmsException;

	/**
	 * 向默认 JMS 目标（或为每次发送操作指定的目标）发送消息。回调提供对
	 * JMS Session 和 MessageProducer 的访问，以执行复杂的发送操作。
	 * @param action 暴露 Session/生产者对的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	<T> T execute(ProducerCallback<T> action) throws JmsException;

	/**
	 * 向 JMS 目标发送消息。回调提供对 JMS Session 和 MessageProducer 的访问，
	 * 以执行复杂的发送操作。
	 * @param destination 要发送消息到的目的地
	 * @param action 暴露 Session/生产者对的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	<T> T execute(Destination destination, ProducerCallback<T> action) throws JmsException;

	/**
	 * 向 JMS 目标发送消息。回调提供对 JMS Session 和 MessageProducer 的访问，
	 * 以执行复杂的发送操作。
	 * @param destinationName 要发送消息到的目的地名称
	 * （将由 DestinationResolver 解析为实际目的地）
	 * @param action 暴露 Session/生产者对的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	<T> T execute(String destinationName, ProducerCallback<T> action) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages
	//---------------------------------------------------------------------------------------

	/**
	 * 向默认目标发送消息。
	 * <p>这仅在指定了默认目标时有效！
	 * @param messageCreator 用于创建消息的回调
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	void send(MessageCreator messageCreator) throws JmsException;

	/**
	 * 向指定的目标发送消息。
	 * MessageCreator 回调根据 Session 创建消息。
	 * @param destination 要发送此消息到的目的地
	 * @param messageCreator 用于创建消息的回调
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	void send(Destination destination, MessageCreator messageCreator) throws JmsException;

	/**
	 * 向指定的目标发送消息。
	 * MessageCreator 回调根据 Session 创建消息。
	 * @param destinationName 要发送此消息到的目的地名称
	 * （将由 DestinationResolver 解析为实际目的地）
	 * @param messageCreator 用于创建消息的回调
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	void send(String destinationName, MessageCreator messageCreator) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending auto-converted messages
	//---------------------------------------------------------------------------------------

	/**
	 * 将给定对象发送到默认目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。
	 * <p>这仅在指定了默认目标时有效！
	 * @param message 要转换为消息的对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	void convertAndSend(Object message) throws JmsException;

	/**
	 * 将给定对象发送到指定的目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。
	 * @param destination 要发送此消息到的目的地
	 * @param message 要转换为消息的对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	void convertAndSend(Destination destination, Object message) throws JmsException;

	/**
	 * 将给定对象发送到指定的目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。
	 * @param destinationName 要发送此消息到的目的地名称
	 * （将由 DestinationResolver 解析为实际目的地）
	 * @param message 要转换为消息的对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	void convertAndSend(String destinationName, Object message) throws JmsException;

	/**
	 * 将给定对象发送到默认目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。MessagePostProcessor 回调允许在转换后修改消息。
	 * <p>这仅在指定了默认目标时有效！
	 * @param message 要转换为消息的对象
	 * @param postProcessor 用于修改消息的回调
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	void convertAndSend(Object message, MessagePostProcessor postProcessor)
		throws JmsException;

	/**
	 * 将给定对象发送到指定的目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。MessagePostProcessor 回调允许在转换后修改消息。
	 * @param destination 要发送此消息到的目的地
	 * @param message 要转换为消息的对象
	 * @param postProcessor 用于修改消息的回调
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	void convertAndSend(Destination destination, Object message, MessagePostProcessor postProcessor)
		throws JmsException;

	/**
	 * 将给定对象发送到指定的目标，并使用配置的 MessageConverter 将对象转换为
	 * JMS 消息。MessagePostProcessor 回调允许在转换后修改消息。
	 * @param destinationName 要发送此消息到的目的地名称
	 * （将由 DestinationResolver 解析为实际目的地）
	 * @param message 要转换为消息的对象。
	 * @param postProcessor 用于修改消息的回调
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
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
	 * @return 消费者接收到的消息；如果超时，则为 {@code null}
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Message receive() throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destination 要从中接收消息的目的地
	 * @return 消费者接收到的消息；如果超时，则为 {@code null}
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Message receive(Destination destination) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destinationName 要发送此消息到的目的地名称
	 * （将由 DestinationResolver 解析为实际目的地）
	 * @return 消费者接收到的消息；如果超时，则为 {@code null}
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Message receive(String destinationName) throws JmsException;

	/**
	 * 从默认目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * <p>这仅在指定了默认目标时有效！
	 * @param messageSelector JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 * 选择器表达式的详细定义参见 JMS 规范。
	 * @return 消费者接收到的消息；如果超时，则为 {@code null}
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Message receiveSelected(String messageSelector) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destination 要从中接收消息的目的地
	 * @param messageSelector JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 * 选择器表达式的详细定义参见 JMS 规范。
	 * @return 消费者接收到的消息；如果超时，则为 {@code null}
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Message receiveSelected(Destination destination, String messageSelector) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destinationName 要发送此消息到的目的地名称
	 * （将由 DestinationResolver 解析为实际目的地）
	 * @param messageSelector JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 * 选择器表达式的详细定义参见 JMS 规范。
	 * @return 消费者接收到的消息；如果超时，则为 {@code null}
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
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
	 * @return 为消费者生成的消息；如果超时，则为 {@code null}。
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Object receiveAndConvert() throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。使用配置的
	 * MessageConverter 将消息转换为对象。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destination 要从中接收消息的目的地
	 * @return 为消费者生成的消息；如果超时，则为 {@code null}。
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Object receiveAndConvert(Destination destination) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。使用配置的
	 * MessageConverter 将消息转换为对象。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destinationName 要发送此消息到的目的地名称
	 * （将由 DestinationResolver 解析为实际目的地）
	 * @return 为消费者生成的消息；如果超时，则为 {@code null}。
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Object receiveAndConvert(String destinationName) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * <p>这仅在指定了默认目标时有效！
	 * @param messageSelector JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 * 选择器表达式的详细定义参见 JMS 规范。
	 * @return 为消费者生成的消息；如果超时，则为 {@code null}。
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Object receiveSelectedAndConvert(String messageSelector) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。使用配置的
	 * MessageConverter 将消息转换为对象。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destination 要从中接收消息的目的地
	 * @param messageSelector JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 * 选择器表达式的详细定义参见 JMS 规范。
	 * @return 为消费者生成的消息；如果超时，则为 {@code null}。
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException;

	/**
	 * 从指定的目标同步接收消息，但最多只等待指定的时间。使用配置的
	 * MessageConverter 将消息转换为对象。
	 * <p>应谨慎使用此方法，因为它将阻塞线程，直到消息可用或超过超时值。
	 * @param destinationName 要发送此消息到的目的地名称
	 * （将由 DestinationResolver 解析为实际目的地）
	 * @param messageSelector JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 * 选择器表达式的详细定义参见 JMS 规范。
	 * @return 为消费者生成的消息；如果超时，则为 {@code null}。
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
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
	 * @param messageCreator 用于创建请求消息的回调
	 * @return 回复；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 * @since 4.1
	 */
	@Nullable
	Message sendAndReceive(MessageCreator messageCreator) throws JmsException;

	/**
	 * 发送消息并从指定的目标接收回复。{@link MessageCreator} 回调根据
	 * Session 创建消息。此操作会创建临时队列，并将其设置到消息的
	 * {@code JMSReplyTO} 头中。
	 * @param destination 要发送此消息到的目的地
	 * @param messageCreator 用于创建消息的回调
	 * @return 回复；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 * @since 4.1
	 */
	@Nullable
	Message sendAndReceive(Destination destination, MessageCreator messageCreator) throws JmsException;

	/**
	 * 发送消息并从指定的目标接收回复。{@link MessageCreator} 回调根据
	 * Session 创建消息。此操作会创建临时队列，并将其设置到消息的
	 * {@code JMSReplyTO} 头中。
	 * @param destinationName 要发送此消息到的目的地名称
	 * （将由 DestinationResolver 解析为实际目的地）
	 * @param messageCreator 用于创建消息的回调
	 * @return 回复；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
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
	 * @param action 暴露 Session/浏览器对的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	<T> T browse(BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param queue 要浏览的队列
	 * @param action 暴露 Session/浏览器对的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	<T> T browse(Queue queue, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param queueName 要浏览的队列名称（将由 DestinationResolver 解析为实际目的地）
	 * @param action 暴露 Session/浏览器对的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	<T> T browse(String queueName, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中选定的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param messageSelector JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 * 选择器表达式的详细定义参见 JMS 规范。
	 * @param action 暴露 Session/浏览器对的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	<T> T browseSelected(String messageSelector, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中选定的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param queue 要浏览的队列
	 * @param messageSelector JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 * 选择器表达式的详细定义参见 JMS 规范。
	 * @param action 暴露 Session/浏览器对的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	<T> T browseSelected(Queue queue, String messageSelector, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览 JMS 队列中选定的消息。回调提供对 JMS Session 和 QueueBrowser 的访问，
	 * 以浏览队列并对内容做出响应。
	 * @param queueName 要浏览的队列名称（将由 DestinationResolver 解析为实际目的地）
	 * @param messageSelector JMS 消息选择器表达式（如果没有则为 {@code null}）。
	 * 选择器表达式的详细定义参见 JMS 规范。
	 * @param action 暴露 Session/浏览器对的回调对象
	 * @return 使用 Session 得到的结果对象
	 * @throws JmsException 将受检的 JMSException 转换为非受检异常
	 */
	@Nullable
	<T> T browseSelected(String queueName, String messageSelector, BrowserCallback<T> action) throws JmsException;

}
