/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.converter;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.ClassUtils;

/**
 * 一个简单的转换器，只要消息载荷与预期的目标类匹配就直接解包该载荷；
 * 反之，也将载荷直接包装为消息。
 *
 * <p>注意，此转换器忽略消息头中可能存在的任何内容类型信息，
 * 如果确实需要载荷转换，则不应使用它。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleMessageConverter implements MessageConverter {

	@Override
	@Nullable
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		Object payload = message.getPayload();
		return (ClassUtils.isAssignableValue(targetClass, payload) ? payload : null);
	}

	@Override
	public Message<?> toMessage(Object payload, @Nullable MessageHeaders headers) {
		if (headers != null) {
			MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(headers, MessageHeaderAccessor.class);
			if (accessor != null && accessor.isMutable()) {
				return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
			}
		}
		return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
	}

}
