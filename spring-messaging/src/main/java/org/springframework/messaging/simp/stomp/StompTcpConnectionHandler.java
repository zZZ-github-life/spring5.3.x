/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.tcp.TcpConnectionHandler;

/**
 * 与 STOMP 连接配合使用的 {@link TcpConnectionHandler}，公开有关连接的更多信息。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 * @param <P> the type of payload for inbound and outbound messages
 */
public interface StompTcpConnectionHandler<P> extends TcpConnectionHandler<P> {

	/**
	 * 返回与 STOMP 连接关联的
	 * {@link SimpMessageHeaderAccessor#getSessionId() sessionId}。
	 */
	String getSessionId();

	/**
	 * 返回将在 STOMP CONNECT 帧中发送的头。
	 */
	StompHeaderAccessor getConnectHeaders();

}
