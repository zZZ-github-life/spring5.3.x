/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 用于 {@link Message} 方法参数的 {@code HandlerMethodArgumentResolver}。
 * 校验载荷的泛型类型是否与消息值匹配，否则应用 {@link MessageConverter}
 * 转换为预期的载荷类型。
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MessageMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Nullable
	private final MessageConverter converter;


	/**
	 * 创建不带消息转换的默认解析器实例。
	 */
	public MessageMethodArgumentResolver() {
		this(null);
	}

	/**
	 * 使用给定 {@link MessageConverter} 创建解析器实例。
	 * @param converter 要使用的 MessageConverter（可以为 {@code null}）
	 * @since 4.3
	 */
	public MessageMethodArgumentResolver(@Nullable MessageConverter converter) {
		this.converter = converter;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Message.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		Class<?> targetMessageType = parameter.getParameterType();
		Class<?> targetPayloadType = getPayloadType(parameter, message);

		if (!targetMessageType.isAssignableFrom(message.getClass())) {
			throw new MethodArgumentTypeMismatchException(message, parameter, "Actual message type '" +
					ClassUtils.getDescriptiveType(message) + "' does not match expected type '" +
					ClassUtils.getQualifiedName(targetMessageType) + "'");
		}

		Object payload = message.getPayload();
		if (targetPayloadType.isInstance(payload)) {
			return message;
		}

		if (isEmptyPayload(payload)) {
			throw new MessageConversionException(message, "Cannot convert from actual payload type '" +
					ClassUtils.getDescriptiveType(payload) + "' to expected payload type '" +
					ClassUtils.getQualifiedName(targetPayloadType) + "' when payload is empty");
		}

		payload = convertPayload(message, parameter, targetPayloadType);
		return MessageBuilder.createMessage(payload, message.getHeaders());
	}

	/**
	 * 解析要将载荷转换为的目标类。
	 * <p>默认情况下，这是 {@code Message} 方法参数中声明的泛型类型，
	 * 但可以重写此方法，以便在同时考虑 "Content-Type" 后选择更具体的目标类型，
	 * 例如当目标类型为 {@code Object} 且 {@code "Content-Type:text/**"} 时返回 {@code String}。
	 * @param parameter 目标方法参数
	 * @param message 正在处理的消息
	 * @return 要使用的目标类型
	 * @since 5.2
	 */
	protected Class<?> getPayloadType(MethodParameter parameter, Message<?> message) {
		Type genericParamType = parameter.getGenericParameterType();
		ResolvableType resolvableType = ResolvableType.forType(genericParamType).as(Message.class);
		return resolvableType.getGeneric().toClass();
	}

	/**
	 * 检查给定 {@code payload} 是否为空。
	 * @param payload 要检查的载荷（可以为 {@code null}）
	 */
	protected boolean isEmptyPayload(@Nullable Object payload) {
		if (payload == null) {
			return true;
		}
		else if (payload instanceof byte[]) {
			return ((byte[]) payload).length == 0;
		}
		else if (payload instanceof String) {
			return !StringUtils.hasText((String) payload);
		}
		else {
			return false;
		}
	}

	private Object convertPayload(Message<?> message, MethodParameter parameter, Class<?> targetPayloadType) {
		Object result = null;
		if (this.converter instanceof SmartMessageConverter) {
			SmartMessageConverter smartConverter = (SmartMessageConverter) this.converter;
			result = smartConverter.fromMessage(message, targetPayloadType, parameter);
		}
		else if (this.converter != null) {
			result = this.converter.fromMessage(message, targetPayloadType);
		}

		if (result == null) {
			throw new MessageConversionException(message, "No converter found from actual payload type '" +
					ClassUtils.getDescriptiveType(message.getPayload()) + "' to expected payload type '" +
					ClassUtils.getQualifiedName(targetPayloadType) + "'");
		}
		return result;
	}

}
