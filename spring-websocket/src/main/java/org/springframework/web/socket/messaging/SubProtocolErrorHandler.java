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

package org.springframework.web.socket.messaging;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * 用于处理发送给客户端的子协议错误的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @param <P> 消息负载类型
 */
public interface SubProtocolErrorHandler<P> {

	/**
	 * 处理在处理客户端消息时抛出的错误，并提供准备错误消息
	 * 或阻止发送错误消息的机会。
	 * <p>请注意，STOMP 协议要求服务器在发送 ERROR 帧后关闭连接。
	 * 要阻止发送 ERROR 帧，处理器可以返回 {@code null}，
	 * 并改为通过代理发送通知消息，例如通过用户目标。
	 * @param clientMessage 与该错误相关的客户端消息；
	 * 如果错误发生在解析 WebSocket 消息时，可能为 {@code null}
	 * @param ex 错误的原因，永不为 {@code null}
	 * @return 要发送给客户端的错误消息；
	 * 如果为 {@code null}，则不会发送任何消息。
	 */
	@Nullable
	Message<P> handleClientMessageProcessingError(@Nullable Message<P> clientMessage, Throwable ex);

	/**
	 * 处理从服务器端发送给客户端的错误，例如由于连接失败或外部代理
	 * 发送了错误消息等原因，导致
	 * {@link org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler
	 * “代理中继”}产生的错误等。
	 * @param errorMessage 错误消息，永不为 {@code null}
	 * @return 要发送给客户端的错误消息；
	 * 如果为 {@code null}，则不会发送任何消息。
	 */
	@Nullable
	Message<P> handleErrorMessageToClient(Message<P> errorMessage);

}
