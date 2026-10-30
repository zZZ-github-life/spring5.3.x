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
 * @param <D> 目标类型
 * @see DestinationResolver
 */
public interface DestinationResolvingMessageRequestReplyOperations<D> extends MessageRequestReplyOperations<D> {

	/**
	 * 将给定目标名称解析为目标并发送给定消息，接收应答并返回。
	 * @param destinationName 目标目的地的名称
	 * @param requestMessage 要发送的消息
	 * @return 接收到的消息；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException;

	/**
	 * 解析给定目标名称，将请求载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息并发送到解析后的目标，接收应答并将其正文转换为指定目标类。
	 * @param destinationName 目标目的地的名称
	 * @param request 要发送的请求消息的载荷
	 * @param targetClass 回复消息的载荷要转换成的目标类
	 * @return 回复消息转换后的载荷；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass)
			throws MessagingException;

	/**
	 * 解析给定目标名称，将请求载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为带有给定头的消息并发送到解析后的目标，
	 * 接收应答并将其正文转换为指定目标类。
	 * @param destinationName 目标目的地的名称
	 * @param request 要发送的请求消息的载荷
	 * @param headers 要发送的请求消息的消息头
	 * @param targetClass 回复消息的载荷要转换成的目标类
	 * @return 回复消息转换后的载荷；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request,
			@Nullable Map<String, Object> headers, Class<T> targetClass) throws MessagingException;

	/**
	 * 解析给定目标名称，将请求载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息，应用给定的后处理，并将结果消息发送到解析后的目标，
	 * 然后接收应答并将其正文转换为指定目标类。
	 * @param destinationName 目标目的地的名称
	 * @param request 要发送的请求消息的载荷
	 * @param targetClass 回复消息的载荷要转换成的目标类
	 * @param requestPostProcessor 对请求消息的后处理
	 * @return 回复消息转换后的载荷；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			@Nullable MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * 解析给定目标名称，将请求载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为带有给定头的消息，应用给定的后处理，并将结果消息发送到解析后的目标，
	 * 然后接收应答并将其正文转换为指定目标类。
	 * @param destinationName 目标目的地的名称
	 * @param request 要发送的请求消息的载荷
	 * @param headers 要发送的请求消息的消息头
	 * @param targetClass 回复消息的载荷要转换成的目标类
	 * @param requestPostProcessor 对请求消息的后处理
	 * @return 回复消息转换后的载荷；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, @Nullable Map<String, Object> headers,
			Class<T> targetClass, @Nullable MessagePostProcessor requestPostProcessor) throws MessagingException;

}
