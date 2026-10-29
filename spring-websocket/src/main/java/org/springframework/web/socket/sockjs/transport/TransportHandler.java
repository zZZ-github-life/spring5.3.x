/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;

/**
 * 处理 SockJS 会话 URL，即特定于传输的请求。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface TransportHandler {

	/**
	 * 使用给定配置初始化此处理器。
	 * @param serviceConfig the configuration as defined by the containing
	 * {@link org.springframework.web.socket.sockjs.SockJsService}
	 */
	void initialize(SockJsServiceConfig serviceConfig);

	/**
	 * 返回此处理器支持的传输类型。
	 */
	TransportType getTransportType();

	/**
	 * 检查给定会话的类型是否与此 {@code TransportHandler} 的传输类型匹配，
	 * 其中会话 ID 和传输类型是从 SockJS URL 中提取的。
	 * @return {@code true} if the session matches (and would therefore get
	 * accepted by {@link #handleRequest}), or {@code false} otherwise
	 * @since 4.3.4
	 */
	boolean checkSessionType(SockJsSession session);

	/**
	 * 处理给定请求并将消息委托给提供的
	 * {@link WebSocketHandler}。
	 * @param request the current request
	 * @param response the current response
	 * @param handler the target WebSocketHandler (never {@code null})
	 * @param session the SockJS session (never {@code null})
	 * @throws SockJsException raised when request processing fails as
	 * explained in {@link SockJsService}
	 */
	void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler, SockJsSession session) throws SockJsException;

}
