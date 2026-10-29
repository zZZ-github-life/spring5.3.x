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

package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.springframework.context.ApplicationEvent;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * 用于表示从 WebSocket 客户端接收并解析为更高级别子协议（例如 STOMP）的消息
 * 的事件的基类。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
@SuppressWarnings("serial")
public abstract class AbstractSubProtocolEvent extends ApplicationEvent {

	private final Message<byte[]> message;

	@Nullable
	private final Principal user;


	/**
	 * 创建一个新的 AbstractSubProtocolEvent。
	 * @param source the component that published the event (never {@code null})
	 * @param message the incoming message (never {@code null})
	 */
	protected AbstractSubProtocolEvent(Object source, Message<byte[]> message) {
		this(source, message, null);
	}

	/**
	 * 创建一个新的 AbstractSubProtocolEvent。
	 * @param source the component that published the event (never {@code null})
	 * @param message the incoming message (never {@code null})
	 */
	protected AbstractSubProtocolEvent(Object source, Message<byte[]> message, @Nullable Principal user) {
		super(source);
		Assert.notNull(message, "Message must not be null");
		this.message = message;
		this.user = user;
	}


	/**
	 * 返回与事件关联的 Message。以下示例展示了
	 * 如何获取会话 ID 或消息中任何头的信息：
	 * <pre class="code">
	 * StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
	 * headers.getSessionId();
	 * headers.getSessionAttributes();
	 * headers.getPrincipal();
	 * </pre>
	 */
	public Message<byte[]> getMessage() {
		return this.message;
	}

	/**
	 * 返回与事件关联的会话的用户。
	 */
	@Nullable
	public Principal getUser() {
		return this.user;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + this.message + "]";
	}

}
