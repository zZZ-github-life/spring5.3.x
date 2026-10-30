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

package org.springframework.web.socket.server;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

/**
 * 用于处理 WebSocket 握手请求的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see HandshakeInterceptor
 * @see org.springframework.web.socket.server.support.WebSocketHttpRequestHandler
 * @see org.springframework.web.socket.sockjs.SockJsService
 */
public interface HandshakeHandler {

	/**
	 * 发起握手。
	 * @param request 当前请求
	 * @param response 当前响应
	 * @param wsHandler 处理 WebSocket 消息的处理器；
	 * 关于如何为每个连接提供具有独立生命周期的处理器，
	 * 请参见 {@link PerConnectionWebSocketHandler}。
	 * @param attributes 要与 WebSocket 会话关联的、来自 HTTP 握手的属性；
	 * 提供的属性会被复制，不使用原始 Map。
	 * @return 握手协商是否成功。无论成功与否，
	 * 响应状态、头和正文都已更新，
	 * 以反映协商结果
	 * @throws HandshakeFailureException 当握手处理因内部不可恢复的错误
	 * 而未能完成时抛出，即服务器错误，
	 * 而不是握手协商失败。
	 */
	boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws HandshakeFailureException;

}
