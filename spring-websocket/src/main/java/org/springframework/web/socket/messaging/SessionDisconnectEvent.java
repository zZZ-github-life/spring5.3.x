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

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;

/**
 * 当使用简单消息传递协议（例如 STOMP）作为 WebSocket 子协议的 WebSocket
 * 客户端会话关闭时引发的事件。
 *
 * <p>请注意，单个会话可能会多次引发此事件，因此事件消费者应当是幂等的，
 * 并忽略重复的事件。
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class SessionDisconnectEvent extends AbstractSubProtocolEvent {

	private final String sessionId;

	private final CloseStatus status;


	/**
	 * 创建一个新的 SessionDisconnectEvent。
	 * @param source 发布该事件的组件（永不为 {@code null}）
	 * @param message 该消息（永不为 {@code null}）
	 * @param sessionId 断开连接消息
	 * @param closeStatus 状态对象
	 */
	public SessionDisconnectEvent(Object source, Message<byte[]> message, String sessionId,
			CloseStatus closeStatus) {

		this(source, message, sessionId, closeStatus, null);
	}

	/**
	 * 创建一个新的 SessionDisconnectEvent。
	 * @param source 发布该事件的组件（永不为 {@code null}）
	 * @param message 该消息（永不为 {@code null}）
	 * @param sessionId 断开连接消息
	 * @param closeStatus 状态对象
	 * @param user 当前会话用户
	 */
	public SessionDisconnectEvent(Object source, Message<byte[]> message, String sessionId,
			CloseStatus closeStatus, @Nullable Principal user) {

		super(source, message, user);
		Assert.notNull(sessionId, "Session id must not be null");
		this.sessionId = sessionId;
		this.status = closeStatus;
	}


	/**
	 * 返回会话 ID。
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * 返回会话关闭时的状态。
	 */
	public CloseStatus getCloseStatus() {
		return this.status;
	}


	@Override
	public String toString() {
		return "SessionDisconnectEvent[sessionId=" + this.sessionId + ", " + this.status.toString() + "]";
	}

}
