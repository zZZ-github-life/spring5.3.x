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
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;

/**
 * 对 {@link AbstractMessageSendingTemplate} 的扩展，增加了对
 * {@link MessageReceivingOperations} 定义的接收式操作的支持。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 4.1
 * @param <D> 目标类型
 */
public abstract class AbstractMessageReceivingTemplate<D> extends AbstractMessageSendingTemplate<D>
		implements MessageReceivingOperations<D> {

	@Override
	@Nullable
	public Message<?> receive() {
		return doReceive(getRequiredDefaultDestination());
	}

	@Override
	@Nullable
	public Message<?> receive(D destination) {
		return doReceive(destination);
	}

	/**
	 * 实际从给定目标接收消息。
	 * @param destination 目标目的地
	 * @return 接收到的消息；如果无法接收消息（例如因超时），
	 * 则可能为 {@code null}
	 */
	@Nullable
	protected abstract Message<?> doReceive(D destination);


	@Override
	@Nullable
	public <T> T receiveAndConvert(Class<T> targetClass) {
		return receiveAndConvert(getRequiredDefaultDestination(), targetClass);
	}

	@Override
	@Nullable
	public <T> T receiveAndConvert(D destination, Class<T> targetClass) {
		Message<?> message = doReceive(destination);
		if (message != null) {
			return doConvert(message, targetClass);
		}
		else {
			return null;
		}
	}

	/**
	 * 将给定消息转换为给定目标类。
	 * @param message 要转换的消息
	 * @param targetClass 载荷要转换成的目标类
	 * @return 回复消息转换后的载荷（永远不会为 {@code null}）
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected <T> T doConvert(Message<?> message, Class<T> targetClass) {
		MessageConverter messageConverter = getMessageConverter();
		T value = (T) messageConverter.fromMessage(message, targetClass);
		if (value == null) {
			throw new MessageConversionException(message, "Unable to convert payload [" + message.getPayload() +
					"] to type [" + targetClass + "] using converter [" + messageConverter + "]");
		}
		return value;
	}

}
