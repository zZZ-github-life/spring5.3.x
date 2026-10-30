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
 * 用于从目标接收消息的操作。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <D> 目的地类型
 * @see GenericMessagingTemplate
 */
public interface MessageReceivingOperations<D> {

	/**
	 * 从默认目标接收消息。
	 * @return 接收到的消息；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	Message<?> receive() throws MessagingException;

	/**
	 * 从给定目标接收消息。
	 * @param destination 目标目的地
	 * @return 接收到的消息；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	Message<?> receive(D destination) throws MessagingException;

	/**
	 * 从默认目标接收消息，并将其载荷转换为指定目标类。
	 * @param targetClass 载荷要转换成的目标类
	 * @return 回复消息转换后的载荷；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T receiveAndConvert(Class<T> targetClass) throws MessagingException;

	/**
	 * 从给定目标接收消息，并将其载荷转换为指定目标类。
	 * @param destination 目标目的地
	 * @param targetClass 载荷要转换成的目标类
	 * @return 回复消息转换后的载荷；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	<T> T receiveAndConvert(D destination, Class<T> targetClass) throws MessagingException;

}
