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

package org.springframework.web.socket.sockjs;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;

/**
 * 处理来自 SockJS 客户端的 HTTP 请求的主要入口点。
 *
 * <p>在 Servlet 3+ 容器中，可以使用 {@link org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler}
 * 调用此服务。处理请求的 servlet 以及所有涉及的过滤器必须通过 ServletContext API
 * 或通过在 web.xml 中为 servlet 和过滤器声明添加
 * <code>&lt;async-support&gt;true&lt;/async-support&gt;</code> 元素来启用异步支持。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler
 */
public interface SockJsService {

	/**
	 * 处理 SockJS HTTP 请求。
	 * <p>有关所需 URL 类型的详细信息，请参阅 <a
	 * href="https://sockjs.github.io/sockjs-protocol/sockjs-protocol-0.3.3.html">SockJS
	 * 协议</a>中的 "Base URL"、"Static URLs" 和 "Session URLs" 部分。
	 * @param request the current request
	 * @param response the current response
	 * @param sockJsPath the remainder of the path within the SockJS service prefix
	 * @param handler the handler that will exchange messages with the SockJS client
	 * @throws SockJsException raised when request processing fails; generally, failed
	 * attempts to send messages to clients automatically close the SockJS session
	 * and raise {@link SockJsTransportFailureException}; failed attempts to read
	 * messages from clients do not automatically close the session and may result
	 * in {@link SockJsMessageDeliveryException} or {@link SockJsException};
	 * exceptions from the WebSocketHandler can be handled internally or through
	 * {@link ExceptionWebSocketHandlerDecorator} or some alternative decorator.
	 * The former is automatically added when using
	 * {@link org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler}.
	 */
	void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String sockJsPath, WebSocketHandler handler) throws SockJsException;

}
