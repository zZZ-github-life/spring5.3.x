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

package org.springframework.web.socket.server;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;

/**
 * WebSocket 握手请求的拦截器。可用于检查握手请求和响应，
 * 以及向目标 {@link WebSocketHandler} 传递属性。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.web.socket.server.support.WebSocketHttpRequestHandler
 * @see org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService
 */
public interface HandshakeInterceptor {

	/**
	 * 在处理握手之前调用。
	 * @param request 当前请求
	 * @param response 当前响应
	 * @param wsHandler 目标 WebSocket 处理器
	 * @param attributes 要与 WebSocket 会话关联的、来自 HTTP 握手的属性；
	 * 提供的属性会被复制，不使用原始 Map。
	 * @return 是继续握手（{@code true}），还是中止（{@code false}）
	 */
	boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception;

	/**
	 * 在握手完成之后调用。响应状态和头表示握手的结果，
	 * 即握手是否成功。
	 * @param request 当前请求
	 * @param response 当前响应
	 * @param wsHandler 目标 WebSocket 处理器
	 * @param exception 握手期间抛出的异常；如果没有则为 {@code null}
	 */
	void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, @Nullable Exception exception);

}
