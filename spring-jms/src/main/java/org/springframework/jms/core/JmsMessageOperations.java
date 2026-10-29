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
	 * @param destinationName the name of the target destination
	 * @param message the message to send
	 */
	void send(String destinationName, Message<?> message) throws MessagingException;

	/**
	 * 将给定对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息并发送到给定的目标。
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 */
	void convertAndSend(String destinationName, Object payload) throws MessagingException;

	/**
	 * 将给定对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 使用给定的头将其包装为消息并发送到给定的目标。
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 * @param headers the headers for the message to send
	 */
	void convertAndSend(String destinationName, Object payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * 将给定对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息，应用给定的后处理器，并将结果消息发送到给定的目标。
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 * @param postProcessor the post processor to apply to the message
	 */
	void convertAndSend(String destinationName, Object payload, MessagePostProcessor postProcessor)
			throws MessagingException;

	/**
	 * 将给定对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 使用给定的头将其包装为消息，应用给定的后处理器，
	 * 并将结果消息发送到给定的目标。
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 * @param headers the headers for the message to send
	 * @param postProcessor the post processor to apply to the message
	 */
	void convertAndSend(String destinationName, Object payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * 从给定的目标接收消息。
	 * @param destinationName the name of the target destination
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	@Nullable
	Message<?> receive(String destinationName) throws MessagingException;

	/**
	 * 从给定的目标接收消息，并将其负载转换为指定的目标类。
	 * @param destinationName the name of the target destination
	 * @param targetClass the target class to convert the payload to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException;

	/**
	 * 发送请求消息并从给定的目标接收回复。
	 * @param destinationName the name of the target destination
	 * @param requestMessage the message to send
	 * @return the reply, possibly {@code null} if the message could not be received,
	 * for example due to a timeout
	 */
	@Nullable
	Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其作为 {@link Message} 发送到给定的目标，接收回复并将其消息体转换为
	 * 指定的目标类。
	 * @param destinationName the name of the target destination
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 使用给定的头将其作为 {@link Message} 发送到指定的目标，
	 * 接收回复并将其消息体转换为指定的目标类。
	 * @param destinationName the name of the target destination
	 * @param request payload for the request message to send
	 * @param headers the headers for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, @Nullable Map<String, Object> headers, Class<T> targetClass)
			throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 应用给定的后处理器，并将结果 {@link Message} 发送到给定的目标，
	 * 接收回复并将其消息体转换为给定的目标类。
	 * @param destinationName the name of the target destination
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 使用给定的头将其包装为消息，应用给定的后处理器，并将结果
	 * {@link Message} 发送到指定的目标，接收回复并将其消息体转换为给定的目标类。
	 * @param destinationName the name of the target destination
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

}
