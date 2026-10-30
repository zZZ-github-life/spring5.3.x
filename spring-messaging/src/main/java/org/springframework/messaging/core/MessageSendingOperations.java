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
 * 用于向目标发送消息的操作。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <D> 目标类型
 */
public interface MessageSendingOperations<D> {

	/**
	 * 向默认目标发送消息。
	 * @param message 要发送的消息
	 */
	void send(Message<?> message) throws MessagingException;

	/**
	 * 向给定目标发送消息。
	 * @param destination 目标目的地
	 * @param message 要发送的消息
	 */
	void send(D destination, Message<?> message) throws MessagingException;

	/**
	 * 将给定 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息并发送到默认目标。
	 * @param payload 用作载荷的对象
	 */
	void convertAndSend(Object payload) throws MessagingException;

	/**
	 * 将给定 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息并发送到给定目标。
	 * @param destination 目标目的地
	 * @param payload 用作载荷的对象
	 */
	void convertAndSend(D destination, Object payload) throws MessagingException;

	/**
	 * 将给定 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为带有给定头的消息并发送到给定目标。
	 * @param destination 目标目的地
	 * @param payload 用作载荷的对象
	 * @param headers 要发送的消息的消息头
	 */
	void convertAndSend(D destination, Object payload, Map<String, Object> headers) throws MessagingException;

	/**
	 * 将给定 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息，应用给定的后处理器，并将结果消息发送到默认目标。
	 * @param payload 用作载荷的对象
	 * @param postProcessor 要应用到消息的后处理器
	 */
	void convertAndSend(Object payload, @Nullable MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * 将给定 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为消息，应用给定的后处理器，并将结果消息发送到给定目标。
	 * @param destination 目标目的地
	 * @param payload 用作载荷的对象
	 * @param postProcessor 要应用到消息的后处理器
	 */
	void convertAndSend(D destination, Object payload, MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * 将给定 Object 转换为序列化形式（可能使用
	 * {@link org.springframework.messaging.converter.MessageConverter}），
	 * 将其包装为带有给定头的消息，应用给定的后处理器，
	 * 并将结果消息发送到给定目标。
	 * @param destination 目标目的地
	 * @param payload 用作载荷的对象
	 * @param headers 要发送的消息的消息头
	 * @param postProcessor 要应用到消息的后处理器
	 */
	void convertAndSend(D destination, Object payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException;

}
