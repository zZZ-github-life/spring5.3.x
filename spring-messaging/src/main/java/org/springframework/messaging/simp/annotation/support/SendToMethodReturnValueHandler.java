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

package org.springframework.messaging.simp.annotation.support;

import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringUtils;

/**
 * 用于发送到 {@link SendTo} 或 {@link SendToUser} 方法级注解中指定目标的
 * {@link HandlerMethodReturnValueHandler}。
 *
 * <p>方法返回的值会被转换，并转换为 {@link Message}，
 * 然后通过提供的 {@link MessageChannel} 发送。随后，消息会使用输入消息的会话 ID
 * 以及注解中的目标进行丰富。如果指定了多个目标，则向每个目标发送消息副本。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class SendToMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final SimpMessageSendingOperations messagingTemplate;

	private final boolean annotationRequired;

	private String defaultDestinationPrefix = "/topic";

	private String defaultUserDestinationPrefix = "/queue";

	private PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}", null, false);

	@Nullable
	private MessageHeaderInitializer headerInitializer;


	public SendToMethodReturnValueHandler(SimpMessageSendingOperations messagingTemplate, boolean annotationRequired) {
		Assert.notNull(messagingTemplate, "'messagingTemplate' must not be null");
		this.messagingTemplate = messagingTemplate;
		this.annotationRequired = annotationRequired;
	}


	/**
	 * 配置在方法未使用 {@link SendTo @SendTo} 注解
	 * 或未通过注解的 value 属性指定任何目标时，添加到消息目标的默认前缀。
	 * <p>默认情况下，前缀设置为 "/topic"。
	 */
	public void setDefaultDestinationPrefix(String defaultDestinationPrefix) {
		this.defaultDestinationPrefix = defaultDestinationPrefix;
	}

	/**
	 * 返回已配置的默认目标前缀。
	 * @see #setDefaultDestinationPrefix(String)
	 */
	public String getDefaultDestinationPrefix() {
		return this.defaultDestinationPrefix;
	}

	/**
	 * 配置在方法使用 {@link SendToUser @SendToUser} 注解
	 * 但未通过注解的 value 属性指定任何目标时，添加到消息目标的默认前缀。
	 * <p>默认情况下，前缀设置为 "/queue"。
	 */
	public void setDefaultUserDestinationPrefix(String prefix) {
		this.defaultUserDestinationPrefix = prefix;
	}

	/**
	 * 返回已配置的默认用户目标前缀。
	 * @see #setDefaultUserDestinationPrefix(String)
	 */
	public String getDefaultUserDestinationPrefix() {
		return this.defaultUserDestinationPrefix;
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
		return (returnType.hasMethodAnnotation(SendTo.class) ||
				AnnotatedElementUtils.hasAnnotation(returnType.getDeclaringClass(), SendTo.class) ||
				returnType.hasMethodAnnotation(SendToUser.class) ||
				AnnotatedElementUtils.hasAnnotation(returnType.getDeclaringClass(), SendToUser.class) ||
				!this.annotationRequired);
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		DestinationHelper destinationHelper = getDestinationHelper(headers, returnType);

		SendToUser sendToUser = destinationHelper.getSendToUser();
		if (sendToUser != null) {
			boolean broadcast = sendToUser.broadcast();
			String user = getUserName(message, headers);
			if (user == null) {
				if (sessionId == null) {
					throw new MissingSessionUserException(message);
				}
				user = sessionId;
				broadcast = false;
			}
			String[] destinations = getTargetDestinations(sendToUser, message, this.defaultUserDestinationPrefix);
			for (String destination : destinations) {
				destination = destinationHelper.expandTemplateVars(destination);
				if (broadcast) {
					this.messagingTemplate.convertAndSendToUser(
							user, destination, returnValue, createHeaders(null, returnType));
				}
				else {
					this.messagingTemplate.convertAndSendToUser(
							user, destination, returnValue, createHeaders(sessionId, returnType));
				}
			}
		}

		SendTo sendTo = destinationHelper.getSendTo();
		if (sendTo != null || sendToUser == null) {
			String[] destinations = getTargetDestinations(sendTo, message, this.defaultDestinationPrefix);
			for (String destination : destinations) {
				destination = destinationHelper.expandTemplateVars(destination);
				this.messagingTemplate.convertAndSend(destination, returnValue, createHeaders(sessionId, returnType));
			}
		}
	}

	private DestinationHelper getDestinationHelper(MessageHeaders headers, MethodParameter returnType) {
		SendToUser m1 = AnnotatedElementUtils.findMergedAnnotation(returnType.getExecutable(), SendToUser.class);
		SendTo m2 = AnnotatedElementUtils.findMergedAnnotation(returnType.getExecutable(), SendTo.class);
		if ((m1 != null && !ObjectUtils.isEmpty(m1.value())) || (m2 != null && !ObjectUtils.isEmpty(m2.value()))) {
			return new DestinationHelper(headers, m1, m2);
		}

		SendToUser c1 = AnnotatedElementUtils.findMergedAnnotation(returnType.getDeclaringClass(), SendToUser.class);
		SendTo c2 = AnnotatedElementUtils.findMergedAnnotation(returnType.getDeclaringClass(), SendTo.class);
		if ((c1 != null && !ObjectUtils.isEmpty(c1.value())) || (c2 != null && !ObjectUtils.isEmpty(c2.value()))) {
			return new DestinationHelper(headers, c1, c2);
		}

		return (m1 != null || m2 != null ?
				new DestinationHelper(headers, m1, m2) : new DestinationHelper(headers, c1, c2));
	}

	@Nullable
	protected String getUserName(Message<?> message, MessageHeaders headers) {
		Principal principal = SimpMessageHeaderAccessor.getUser(headers);
		if (principal != null) {
			return (principal instanceof DestinationUserNameProvider ?
					((DestinationUserNameProvider) principal).getDestinationUserName() : principal.getName());
		}
		return null;
	}

	protected String[] getTargetDestinations(@Nullable Annotation annotation, Message<?> message, String defaultPrefix) {
		if (annotation != null) {
			String[] value = (String[]) AnnotationUtils.getValue(annotation);
			if (!ObjectUtils.isEmpty(value)) {
				return value;
			}
		}

		String name = DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER;
		String destination = (String) message.getHeaders().get(name);
		if (!StringUtils.hasText(destination)) {
			throw new IllegalStateException("No lookup destination header in " + message);
		}

		return (destination.startsWith("/") ?
				new String[] {defaultPrefix + destination} : new String[] {defaultPrefix + '/' + destination});
	}

	private MessageHeaders createHeaders(@Nullable String sessionId, MethodParameter returnType) {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(headerAccessor);
		}
		if (sessionId != null) {
			headerAccessor.setSessionId(sessionId);
		}
		headerAccessor.setHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER, returnType);
		headerAccessor.setLeaveMutable(true);
		return headerAccessor.getMessageHeaders();
	}


	@Override
	public String toString() {
		return "SendToMethodReturnValueHandler [annotationRequired=" + this.annotationRequired + "]";
	}


	private class DestinationHelper {

		private final PlaceholderResolver placeholderResolver;

		@Nullable
		private final SendTo sendTo;

		@Nullable
		private final SendToUser sendToUser;


		public DestinationHelper(MessageHeaders headers, @Nullable SendToUser sendToUser, @Nullable SendTo sendTo) {
			Map<String, String> variables = getTemplateVariables(headers);
			this.placeholderResolver = variables::get;
			this.sendTo = sendTo;
			this.sendToUser = sendToUser;
		}

		@SuppressWarnings("unchecked")
		private Map<String, String> getTemplateVariables(MessageHeaders headers) {
			String name = DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER;
			return (Map<String, String>) headers.getOrDefault(name, Collections.emptyMap());
		}

		@Nullable
		public SendTo getSendTo() {
			return this.sendTo;
		}

		@Nullable
		public SendToUser getSendToUser() {
			return this.sendToUser;
		}

		public String expandTemplateVars(String destination) {
			return placeholderHelper.replacePlaceholders(destination, this.placeholderResolver);
		}
	}

}
