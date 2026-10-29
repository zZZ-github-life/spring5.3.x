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

package org.springframework.messaging.core;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 用于向目标发送消息并接收应答的操作。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <D> the type of destination
 * @see GenericMessagingTemplate
 */
public interface MessageRequestReplyOperations<D> {

	/**
	 * 向默认目标发送请求消息并接收应答。
	 * @param requestMessage the message to send
	 * @return the reply, possibly {@code null} if the message could not be received,
	 * for example due to a timeout
	 */
	@Nullable
	Message<?> sendAndReceive(Message<?> requestMessage) throws MessagingException;

	/**
	 * 向给定目标发送请求消息并接收应答。
	 * @param destination the target destination
	 * @param requestMessage the message to send
	 * @return the reply, possibly {@code null} if the message could not be received,
	 * for example due to a timeout
	 */
	@Nullable
	Message<?> sendAndReceive(D destination, Message<?> requestMessage) throws MessagingException;

	/**
	 * 将给定请求 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其作为 {@link Message} 发送到默认目标，接收应答并将其正文转换为指定目标类。
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(Object request, Class<T> targetClass) throws MessagingException;

	/**
	 * 将给定请求 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其作为 {@link Message} 发送到给定目标，接收应答并将其正文转换为指定目标类。
	 * @param destination the target destination
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass) throws MessagingException;

	/**
	 * 将给定请求 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其作为带有给定头的 {@link Message} 发送到指定目标，
	 * 接收应答并将其正文转换为指定目标类。
	 * @param destination the target destination
	 * @param request payload for the request message to send
	 * @param headers the headers for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(
			D destination, Object request, @Nullable Map<String, Object> headers, Class<T> targetClass)
			throws MessagingException;

	/**
	 * 将给定请求 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 应用给定的后处理器并将结果 {@link Message} 发送到默认目标，
	 * 接收应答并将其正文转换为给定目标类。
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(
			Object request, Class<T> targetClass, @Nullable MessagePostProcessor requestPostProcessor)
			throws MessagingException;

	/**
	 * 将给定请求 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 应用给定的后处理器并将结果 {@link Message} 发送到给定目标，
	 * 接收应答并将其正文转换为给定目标类。
	 * @param destination the target destination
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass,
			MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * 将给定请求 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为带有给定头的消息，应用给定的后处理器，
	 * 并将结果 {@link Message} 发送到指定目标，接收应答并将其正文转换为给定目标类。
	 * @param destination the target destination
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(
			D destination, Object request, @Nullable Map<String, Object> headers, Class<T> targetClass,
			@Nullable MessagePostProcessor requestPostProcessor) throws MessagingException;

}
