/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.simp.annotation.support;

import org.apache.commons.logging.Log;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.util.Assert;

/**
 * 用于直接回复订阅的 {@code HandlerMethodReturnValueHandler}。支持在使用
 * {@link org.springframework.messaging.simp.annotation.SubscribeMapping
 * SubscribeMapping} 注解的方法上使用，此时返回值会被视为要直接在会话上
 * 发回的响应。这使客户端能够实现请求-响应模式，例如在初始化时获取一些数据。
 *
 * <p>方法返回的值会被转换并转换为 {@link Message}，
 * 然后使用输入消息的 sessionId、subscriptionId 和目标进行丰富。
 *
 * <p><strong>注意：</strong>可以通过使用 {@link SendTo} 或 {@link SendToUser}
 * 注解覆盖 {@code @SubscribeMapping} 方法返回值的这种默认解释行为，
 * 此时会准备消息并发送到代理。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class SubscriptionMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private static final Log logger = SimpLogging.forLogName(SubscriptionMethodReturnValueHandler.class);


	private final MessageSendingOperations<String> messagingTemplate;

	@Nullable
	private MessageHeaderInitializer headerInitializer;


	/**
	 * 构造新的 SubscriptionMethodReturnValueHandler。
	 * @param template a messaging template to send messages to,
	 * most likely the "clientOutboundChannel" (must not be {@code null})
	 */
	public SubscriptionMethodReturnValueHandler(MessageSendingOperations<String> template) {
		Assert.notNull(template, "messagingTemplate must not be null");
		this.messagingTemplate = template;
	}


	/**
	 * 配置要应用于发送到客户端出站通道的所有消息头的
	 * {@link MessageHeaderInitializer}。
	 * <p>默认情况下，不设置此属性。
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * 返回已配置的头初始化器。
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (returnType.hasMethodAnnotation(SubscribeMapping.class) &&
				!returnType.hasMethodAnnotation(SendTo.class) &&
				!returnType.hasMethodAnnotation(SendToUser.class));
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
		String destination = SimpMessageHeaderAccessor.getDestination(headers);

		if (subscriptionId == null) {
			throw new IllegalStateException("No simpSubscriptionId in " + message +
					" returned by: " + returnType.getMethod());
		}
		if (destination == null) {
			throw new IllegalStateException("No simpDestination in " + message +
					" returned by: " + returnType.getMethod());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Reply to @SubscribeMapping: " + returnValue);
		}
		MessageHeaders headersToSend = createHeaders(sessionId, subscriptionId, returnType);
		this.messagingTemplate.convertAndSend(destination, returnValue, headersToSend);
	}

	private MessageHeaders createHeaders(@Nullable String sessionId, String subscriptionId, MethodParameter returnType) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(accessor);
		}
		if (sessionId != null) {
			accessor.setSessionId(sessionId);
		}
		accessor.setSubscriptionId(subscriptionId);
		accessor.setHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER, returnType);
		accessor.setLeaveMutable(true);
		return accessor.getMessageHeaders();
	}

}
