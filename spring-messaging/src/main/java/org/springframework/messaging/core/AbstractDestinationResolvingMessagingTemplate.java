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
import org.springframework.util.Assert;

/**
 * 对 {@link AbstractMessagingTemplate} 的扩展，增加了向可解析的目标名称发送消息的操作。
 * 支持以下接口定义的目标解析：
 * <ul>
 * <li>{@link DestinationResolvingMessageSendingOperations}</li>
 * <li>{@link DestinationResolvingMessageReceivingOperations}</li>
 * <li>{@link DestinationResolvingMessageRequestReplyOperations}</li>
 * </ul>
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <D> the destination type
 */
public abstract class AbstractDestinationResolvingMessagingTemplate<D> extends AbstractMessagingTemplate<D>
		implements DestinationResolvingMessageSendingOperations<D>,
		DestinationResolvingMessageReceivingOperations<D>,
		DestinationResolvingMessageRequestReplyOperations<D> {

	@Nullable
	private DestinationResolver<D> destinationResolver;


	/**
	 * 配置用于将 String 目标名称解析为类型为 {@code <D>} 的实际目标的
	 * {@link DestinationResolver}。
	 * <p>此字段没有默认设置。如果未配置，需要解析目标名称的方法
	 * 将抛出 {@link IllegalArgumentException}。
	 * @param destinationResolver the destination resolver to use
	 */
	public void setDestinationResolver(@Nullable DestinationResolver<D> destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * 返回已配置的目标解析器。
	 */
	@Nullable
	public DestinationResolver<D> getDestinationResolver() {
		return this.destinationResolver;
	}


	@Override
	public void send(String destinationName, Message<?> message) {
		D destination = resolveDestination(destinationName);
		doSend(destination, message);
	}

	protected final D resolveDestination(String destinationName) {

		Assert.state(this.destinationResolver != null, "DestinationResolver is required to resolve destination names");
		return this.destinationResolver.resolveDestination(destinationName);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload) {
		convertAndSend(destinationName, payload, null, null);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, @Nullable Map<String, Object> headers) {
		convertAndSend(destinationName, payload, headers, null);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, @Nullable MessagePostProcessor postProcessor) {
		convertAndSend(destinationName, payload, null, postProcessor);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload,
			@Nullable Map<String, Object> headers, @Nullable MessagePostProcessor postProcessor) {

		D destination = resolveDestination(destinationName);
		super.convertAndSend(destination, payload, headers, postProcessor);
	}

	@Override
	@Nullable
	public Message<?> receive(String destinationName) {
		D destination = resolveDestination(destinationName);
		return super.receive(destination);
	}

	@Override
	@Nullable
	public <T> T receiveAndConvert(String destinationName, Class<T> targetClass) {
		D destination = resolveDestination(destinationName);
		return super.receiveAndConvert(destination, targetClass);
	}

	@Override
	@Nullable
	public Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) {
		D destination = resolveDestination(destinationName);
		return super.sendAndReceive(destination, requestMessage);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass) {
		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, targetClass);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(String destinationName, Object request,
			@Nullable Map<String, Object> headers, Class<T> targetClass) {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, headers, targetClass);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			@Nullable MessagePostProcessor postProcessor) {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, targetClass, postProcessor);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(String destinationName, Object request,
			@Nullable Map<String, Object> headers, Class<T> targetClass,
			@Nullable MessagePostProcessor postProcessor) {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, headers, targetClass, postProcessor);
	}

}
