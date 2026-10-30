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
	 * @param serviceConfig 由所属的
	 * {@link org.springframework.web.socket.sockjs.SockJsService} 定义的配置
	 */
	void initialize(SockJsServiceConfig serviceConfig);

	/**
	 * 返回此处理器支持的传输类型。
	 */
	TransportType getTransportType();

	/**
	 * 检查给定会话的类型是否与此 {@code TransportHandler} 的传输类型匹配，
	 * 其中会话 ID 和传输类型是从 SockJS URL 中提取的。
	 * @return 如果会话匹配（因而会被 {@link #handleRequest} 接受），
	 * 则返回 {@code true}，否则返回 {@code false}
	 * @since 4.3.4
	 */
	boolean checkSessionType(SockJsSession session);

	/**
	 * 处理给定请求并将消息委托给提供的
	 * {@link WebSocketHandler}。
	 * @param request 当前请求
	 * @param response 当前响应
	 * @param handler 目标 WebSocketHandler（永不为 {@code null}）
	 * @param session SockJS 会话（永不为 {@code null}）
	 * @throws SockJsException 当请求处理失败时抛出，
	 * 详情见 {@link SockJsService}
	 */
	void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler, SockJsSession session) throws SockJsException;

}
