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
 * 扩展 {@link MessageSendingOperations}，增加了向以（可解析的）String 名称
 * 指定的目标发送消息的操作。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <D> 目标类型
 * @see DestinationResolver
 */
public interface DestinationResolvingMessageSendingOperations<D> extends MessageSendingOperations<D> {

	/**
	 * 将给定目标名称解析为目标并向其发送消息。
	 * @param destinationName 要解析的目的地名
	 * @param message 要发送的消息
	 */
	void send(String destinationName, Message<?> message) throws MessagingException;

	/**
	 * 将给定目标名称解析为目标，将载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息并发送到解析后的目标。
	 * @param destinationName 要解析的目的地名
	 * @param payload 用作载荷的对象
	 */
	<T> void convertAndSend(String destinationName, T payload) throws MessagingException;

	/**
	 * 将给定目标名称解析为目标，将载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为带有给定头的消息并发送到解析后的目标。
	 * @param destinationName 要解析的目的地名
	 * @param payload 用作载荷的对象
	 * @param headers 要发送的消息的消息头
	 */
	<T> void convertAndSend(String destinationName, T payload, @Nullable Map<String, Object> headers)
			throws MessagingException;

	/**
	 * 将给定目标名称解析为目标，将载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息，应用给定的后处理器，并将结果消息发送到解析后的目标。
	 * @param destinationName 要解析的目的地名
	 * @param payload 用作载荷的对象
	 * @param postProcessor 要应用到消息的后处理器
	 */
	<T> void convertAndSend(String destinationName, T payload, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException;

	/**
	 * 将给定目标名称解析为目标，将载荷 Object 转换为序列化形式
	 * （可能使用 {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为带有给定头的消息，应用给定的后处理器，
	 * 并将结果消息发送到解析后的目标。
	 * @param destinationName 要解析的目的地名
	 * @param payload 用作载荷的对象
	 * @param headers 要发送的消息的消息头
	 * @param postProcessor 要应用到消息的后处理器
	 */
	<T> void convertAndSend(String destinationName, T payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException;

}
