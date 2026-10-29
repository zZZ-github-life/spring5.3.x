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
 * 扩展 {@link MessageRequestReplyOperations}，增加了向以（可解析的）String 名称
 * 指定的目标发送消息以及从该目标接收消息的操作。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <D> the destination type
 * @see DestinationResolver
 */
public interface DestinationResolvingMessageRequestReplyOperations<D> extends MessageRequestReplyOperations<D> {

	/**
	 * 将给定目标名称解析为目标并发送给定消息，接收应答并返回。
	 * @param destinationName the name of the target destination
	 * @param requestMessage the message to send
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	@Nullable
	Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException;

	/**
	 * 解析给定目标名称，将请求载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息并发送到解析后的目标，接收应答并将其正文转换为指定目标类。
	 * @param destinationName the name of the target destination
	 * @param request the payload for the request message to send
	 * @param targetClass the target class to convert the payload of the reply to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass)
			throws MessagingException;

	/**
	 * 解析给定目标名称，将请求载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为带有给定头的消息并发送到解析后的目标，
	 * 接收应答并将其正文转换为指定目标类。
	 * @param destinationName the name of the target destination
	 * @param request the payload for the request message to send
	 * @param headers the headers for the request message to send
	 * @param targetClass the target class to convert the payload of the reply to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request,
			@Nullable Map<String, Object> headers, Class<T> targetClass) throws MessagingException;

	/**
	 * 解析给定目标名称，将请求载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息，应用给定的后处理，并将结果消息发送到解析后的目标，
	 * 然后接收应答并将其正文转换为指定目标类。
	 * @param destinationName the name of the target destination
	 * @param request the payload for the request message to send
	 * @param targetClass the target class to convert the payload of the reply to
	 * @param requestPostProcessor post process for the request message
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			@Nullable MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * 解析给定目标名称，将请求载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为带有给定头的消息，应用给定的后处理，并将结果消息发送到解析后的目标，
	 * 然后接收应答并将其正文转换为指定目标类。
	 * @param destinationName the name of the target destination
	 * @param request the payload for the request message to send
	 * @param headers the headers for the request message to send
	 * @param targetClass the target class to convert the payload of the reply to
	 * @param requestPostProcessor post process for the request message
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, @Nullable Map<String, Object> headers,
			Class<T> targetClass, @Nullable MessagePostProcessor requestPostProcessor) throws MessagingException;

}
