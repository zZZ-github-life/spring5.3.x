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

package org.springframework.messaging.simp.stomp;

import org.springframework.lang.Nullable;

/**
 * 客户端 STOMP 会话生命周期事件的契约，包括会话建立时的回调，
 * 以及传输或消息处理失败的通知。
 *
 * <p>此契约还扩展了 {@link StompFrameHandler}，以便处理从代理收到的 STOMP ERROR 帧。
 *
 * <p>此接口的实现应考虑继承 {@link StompSessionHandlerAdapter}。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see StompSessionHandlerAdapter
 */
public interface StompSessionHandler extends StompFrameHandler {

	/**
	 * 会话准备好使用时调用，即底层传输（TCP、WebSocket）已连接，
	 * 并从代理收到 STOMP CONNECTED 帧之后。
	 * @param session the client STOMP session
	 * @param connectedHeaders the STOMP CONNECTED frame headers
	 */
	void afterConnected(StompSession session, StompHeaders connectedHeaders);

	/**
	 * 处理处理 STOMP 帧时产生的任何异常，例如载荷转换失败，
	 * 或应用程序 {@code StompFrameHandler} 中未处理的异常。
	 * @param session the client STOMP session
	 * @param command the STOMP command of the frame
	 * @param headers the headers
	 * @param payload the raw payload
	 * @param exception the exception
	 */
	void handleException(StompSession session, @Nullable StompCommand command,
			StompHeaders headers, byte[] payload, Throwable exception);

	/**
	 * 处理底层传输错误，可能是 I/O 错误，也可能是编码或解码 STOMP 消息失败。
	 * <p>请注意，当连接丢失而不是通过
	 * {@link StompSession#disconnect()} 正常关闭时，
	 * {@link org.springframework.messaging.simp.stomp.ConnectionLostException
	 * ConnectionLostException} 会传入此方法。
	 * @param session the client STOMP session
	 * @param exception the exception that occurred
	 */
	void handleTransportError(StompSession session, Throwable exception);

}
