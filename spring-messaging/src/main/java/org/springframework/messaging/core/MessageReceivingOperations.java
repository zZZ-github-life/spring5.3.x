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
 * @param <D> the type of destination
 * @see GenericMessagingTemplate
 */
public interface MessageReceivingOperations<D> {

	/**
	 * 从默认目标接收消息。
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	@Nullable
	Message<?> receive() throws MessagingException;

	/**
	 * 从给定目标接收消息。
	 * @param destination the target destination
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	@Nullable
	Message<?> receive(D destination) throws MessagingException;

	/**
	 * 从默认目标接收消息，并将其载荷转换为指定目标类。
	 * @param targetClass the target class to convert the payload to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T receiveAndConvert(Class<T> targetClass) throws MessagingException;

	/**
	 * 从给定目标接收消息，并将其载荷转换为指定目标类。
	 * @param destination the target destination
	 * @param targetClass the target class to convert the payload to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T receiveAndConvert(D destination, Class<T> targetClass) throws MessagingException;

}
