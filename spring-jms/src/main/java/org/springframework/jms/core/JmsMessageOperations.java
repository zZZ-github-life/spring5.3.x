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

package org.springframework.jms.core;

import java.util.Map;

import javax.jms.Destination;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageReceivingOperations;
import org.springframework.messaging.core.MessageRequestReplyOperations;
import org.springframework.messaging.core.MessageSendingOperations;

/**
 * {@link MessageSendingOperations}、{@link MessageReceivingOperations}
 * 和 {@link MessageRequestReplyOperations} 针对 JMS 相关操作的特化，
 * 允许指定目标名称而不是实际的 {@link javax.jms.Destination}。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see org.springframework.jms.core.JmsTemplate
 * @see org.springframework.messaging.core.MessageSendingOperations
 * @see org.springframework.messaging.core.MessageReceivingOperations
 * @see org.springframework.messaging.core.MessageRequestReplyOperations
 */
public interface JmsMessageOperations extends MessageSendingOperations<Destination>,
		MessageReceivingOperations<Destination>, MessageRequestReplyOperations<Destination> {

	/**
	 * 向给定的目标发送消息。
	 * @param destinationName 目标目的地的名称
	 * @param message 要发送的消息
	 */
	void send(String destinationName, Message<?> message) throws MessagingException;

	/**
	 * 将给定对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息并发送到给定的目标。
	 * @param destinationName 目标目的地的名称
	 * @param payload 要用作负载的对象
	 */
	void convertAndSend(String destinationName, Object payload) throws MessagingException;

	/**
	 * 将给定对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 使用给定的头将其包装为消息并发送到给定的目标。
	 * @param destinationName 目标目的地的名称
	 * @param payload 要用作负载的对象
	 * @param headers 要发送的消息的头部
	 */
	void convertAndSend(String destinationName, Object payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * 将给定对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息，应用给定的后处理器，并将结果消息发送到给定的目标。
	 * @param destinationName 目标目的地的名称
	 * @param payload 要用作负载的对象
	 * @param postProcessor 要应用于该消息的后处理器
	 */
	void convertAndSend(String destinationName, Object payload, MessagePostProcessor postProcessor)
			throws MessagingException;

	/**
	 * 将给定对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 使用给定的头将其包装为消息，应用给定的后处理器，
	 * 并将结果消息发送到给定的目标。
	 * @param destinationName 目标目的地的名称
	 * @param payload 要用作负载的对象
	 * @param headers 要发送的消息的头部
	 * @param postProcessor 要应用于该消息的后处理器
	 */
	void convertAndSend(String destinationName, Object payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * 从给定的目标接收消息。
	 * @param destinationName 目标目的地的名称
	 * @return 接收到的消息；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	Message<?> receive(String destinationName) throws MessagingException;

	/**
	 * 从给定的目标接收消息，并将其负载转换为指定的目标类。
	 * @param destinationName 目标目的地的名称
	 * @param targetClass 要将负载转换成的目标类
	 * @return 回复消息转换后的负载；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException;

	/**
	 * 发送请求消息并从给定的目标接收回复。
	 * @param destinationName 目标目的地的名称
	 * @param requestMessage 要发送的消息
	 * @return 回复；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其作为 {@link Message} 发送到给定的目标，接收回复并将其消息体转换为
	 * 指定的目标类。
	 * @param destinationName 目标目的地的名称
	 * @param request 要发送的请求消息的负载
	 * @param targetClass 要将回复负载转换成的目标类型
	 * @return 回复消息的负载；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 使用给定的头将其作为 {@link Message} 发送到指定的目标，
	 * 接收回复并将其消息体转换为指定的目标类。
	 * @param destinationName 目标目的地的名称
	 * @param request 要发送的请求消息的负载
	 * @param headers 要发送的请求消息的头部
	 * @param targetClass 要将回复负载转换成的目标类型
	 * @return 回复消息的负载；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, @Nullable Map<String, Object> headers, Class<T> targetClass)
			throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 应用给定的后处理器，并将结果 {@link Message} 发送到给定的目标，
	 * 接收回复并将其消息体转换为给定的目标类。
	 * @param destinationName 目标目的地的名称
	 * @param request 要发送的请求消息的负载
	 * @param targetClass 要将回复负载转换成的目标类型
	 * @param requestPostProcessor 要应用于请求消息的后处理
	 * @return 回复消息的负载；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 使用给定的头将其包装为消息，应用给定的后处理器，并将结果
	 * {@link Message} 发送到指定的目标，接收回复并将其消息体转换为给定的目标类。
	 * @param destinationName 目标目的地的名称
	 * @param request 要发送的请求消息的负载
	 * @param targetClass 要将回复负载转换成的目标类型
	 * @param requestPostProcessor 要应用于请求消息的后处理
	 * @return 回复消息的负载；如果无法接收该消息（例如由于超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

}
