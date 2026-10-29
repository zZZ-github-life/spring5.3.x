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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.security.Principal;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * 通常向 {@link Transport} 和
 * {@link AbstractClientSockJsSession session} 实现公开有关通过给定传输
 * 连接到 SockJS 服务器端点的请求的信息。
 *
 * <p>请注意，通过 {@link SockJsClient} 进行的单个连接请求可能会产生
 * 多个 {@link TransportRequest} 实例，在连接成功建立之前
 * 每个传输对应一个实例。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface TransportRequest {

	/**
	 * 返回有关 SockJS URL 的信息，包括服务器和会话 ID。
	 */
	SockJsUrlInfo getSockJsUrlInfo();

	/**
	 * 返回随连接请求发送的头。
	 */
	HttpHeaders getHandshakeHeaders();

	/**
	 * 返回除握手请求之外要添加到所有其他 HTTP 请求（例如 XHR 接收和发送请求）
	 * 的头。
	 * @since 4.2
	 */
	HttpHeaders getHttpRequestHeaders();

	/**
	 * 返回给定传输的传输 URL。
	 * <p>对于 {@link XhrTransport}，这是用于接收消息的 URL。
	 */
	URI getTransportUrl();

	/**
	 * 返回与请求关联的用户（如果有）。
	 */
	@Nullable
	Principal getUser();

	/**
	 * 返回用于编码 SockJS 消息的消息编解码器。
	 */
	SockJsMessageCodec getMessageCodec();

	/**
	 * 注册一个超时清理任务，如果在计算的重新传输超时期限内
	 * SockJS 会话未完全建立，则调用该任务。
	 */
	void addTimeoutTask(Runnable runnable);

}
