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

package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * 一个较简单的拦截器，它对经过 preSend 方法的消息头部调用
 * {@link MessageHeaderAccessor#setImmutable()}。
 *
 * <p>当配置为拦截器链中的最后一个拦截器时，它允许发送消息的组件在消息实际
 * 发送并暴露于并发访问之前，保持头部可变以便拦截器进行修改。
 *
 * @author Rossen Stoyanchev
 * @since 4.1.2
 */
public class ImmutableMessageChannelInterceptor implements ChannelInterceptor {

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor != null && accessor.isMutable()) {
			accessor.setImmutable();
		}
		return message;
	}

}
