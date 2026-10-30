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

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 扩展 {@link MessageReceivingOperations}，增加了从以（可解析的）String 名称
 * 指定的目标接收消息的操作。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <D> 接收消息的目的地类型
 * @see DestinationResolver
 */
public interface DestinationResolvingMessageReceivingOperations<D> extends MessageReceivingOperations<D> {

	/**
	 * 解析给定目标名称并从该目标接收消息。
	 * @param destinationName 要解析的目的地名
	 */
	@Nullable
	Message<?> receive(String destinationName) throws MessagingException;

	/**
	 * 解析给定目标名称，从该目标接收消息，并将载荷转换为指定目标类型。
	 * @param destinationName 要解析的目的地名
	 * @param targetClass 转换后载荷的目标类
	 */
	@Nullable
	<T> T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException;

}
