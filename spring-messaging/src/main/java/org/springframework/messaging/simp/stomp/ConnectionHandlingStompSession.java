/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.util.concurrent.ListenableFuture;

/**
 * 实现
 * {@link org.springframework.messaging.tcp.TcpConnectionHandler TcpConnectionHandler}
 * 以发送和接收消息的 {@link StompSession}。
 *
 * <p>ConnectionHandlingStompSession 可用于任何适配
 * {@code TcpConnectionHandler} 契约的 TCP 或 WebSocket 库。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface ConnectionHandlingStompSession extends StompSession, StompTcpConnectionHandler<byte[]> {

	/**
	 * 返回一个 Future，在会话准备好使用时完成。
	 */
	ListenableFuture<StompSession> getSessionFuture();

}
