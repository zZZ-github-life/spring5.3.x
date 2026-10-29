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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.util.Assert;

/**
 * 实现 {@link MessageSendingOperations} 的抽象基类。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 4.0
 * @param <D> the destination type
 */
public abstract class AbstractMessageSendingTemplate<D> implements MessageSendingOperations<D> {

	/**
	 * 可设置的头名称，用于提供有关载荷来源的更多信息
	 * （例如 {@code MethodParameter} 实例），并将其作为转换提示考虑。
	 * @since 4.2
	 */
	public static final String CONVERSION_HINT_HEADER = "conversionHint";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private D defaultDestination;

	private MessageConverter converter = new SimpleMessageConverter();


	/**
	 * 配置没有目标参数的发送方法中使用的默认目标。如果未配置默认目标，
	 * 则调用不带目标参数的发送方法时将抛出异常。
	 */
	public void setDefaultDestination(@Nullable D defaultDestination) {
		this.defaultDestination = defaultDestination;
	}

	/**
	 * 返回已配置的默认目标。
	 */
	@Nullable
	public D getDefaultDestination() {
		return this.defaultDestination;
	}

	/**
	 * 设置 {@code convertAndSend} 方法中使用的 {@link MessageConverter}。
	 * <p>默认使用 {@link SimpleMessageConverter}。
	 * @param messageConverter the message converter to use
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.converter = messageConverter;
	}

	/**
	 * 返回已配置的 {@link MessageConverter}。
	 */
	public MessageConverter getMessageConverter() {
		return this.converter;
	}


	@Override
	public void send(Message<?> message) {
		send(getRequiredDefaultDestination(), message);
	}

	protected final D getRequiredDefaultDestination() {
		Assert.state(this.defaultDestination != null, "No 'defaultDestination' configured");
		return this.defaultDestination;
	}

	@Override
	public void send(D destination, Message<?> message) {
		doSend(destination, message);
	}

	protected abstract void doSend(D destination, Message<?> message);


	@Override
	public void convertAndSend(Object payload) throws MessagingException {
		convertAndSend(payload, null);
	}

	@Override
	public void convertAndSend(D destination, Object payload) throws MessagingException {
		convertAndSend(destination, payload, (Map<String, Object>) null);
	}

	@Override
	public void convertAndSend(D destination, Object payload, @Nullable Map<String, Object> headers)
			throws MessagingException {

		convertAndSend(destination, payload, headers, null);
	}

	@Override
	public void convertAndSend(Object payload, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException {

		convertAndSend(getRequiredDefaultDestination(), payload, postProcessor);
	}

	@Override
	public void convertAndSend(D destination, Object payload, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException {

		convertAndSend(destination, payload, null, postProcessor);
	}

	@Override
	public void convertAndSend(D destination, Object payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException {

		Message<?> message = doConvert(payload, headers, postProcessor);
		send(destination, message);
	}

	/**
	 * 将给定 Object 转换为序列化形式（可能使用 {@link MessageConverter}），
	 * 将其包装为带有给定头的消息，并应用给定的后处理器。
	 * @param payload the Object to use as payload
	 * @param headers the headers for the message to send
	 * @param postProcessor the post processor to apply to the message
	 * @return the converted message
	 */
	protected Message<?> doConvert(Object payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) {

		MessageHeaders messageHeaders = null;
		Object conversionHint = (headers != null ? headers.get(CONVERSION_HINT_HEADER) : null);

		Map<String, Object> headersToUse = processHeadersToSend(headers);
		if (headersToUse != null) {
			if (headersToUse instanceof MessageHeaders) {
				messageHeaders = (MessageHeaders) headersToUse;
			}
			else {
				messageHeaders = new MessageHeaders(headersToUse);
			}
		}

		MessageConverter converter = getMessageConverter();
		Message<?> message = (converter instanceof SmartMessageConverter ?
				((SmartMessageConverter) converter).toMessage(payload, messageHeaders, conversionHint) :
				converter.toMessage(payload, messageHeaders));
		if (message == null) {
			String payloadType = payload.getClass().getName();
			Object contentType = (messageHeaders != null ? messageHeaders.get(MessageHeaders.CONTENT_TYPE) : null);
			throw new MessageConversionException("Unable to convert payload with type='" + payloadType +
					"', contentType='" + contentType + "', converter=[" + getMessageConverter() + "]");
		}
		if (postProcessor != null) {
			message = postProcessor.postProcessMessage(message);
		}
		return message;
	}

	/**
	 * 在发送操作之前提供对输入头映射的访问。
	 * 子类可以修改头，然后返回相同或不同的映射。
	 * <p>此类中的默认实现返回输入映射。
	 * @param headers the headers to send (or {@code null} if none)
	 * @return the actual headers to send (or {@code null} if none)
	 */
	@Nullable
	protected Map<String, Object> processHeadersToSend(@Nullable Map<String, Object> headers) {
		return headers;
	}

}
