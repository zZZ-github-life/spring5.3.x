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

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;

/**
 * 用于执行实际升级到 WebSocket 交换的服务器特定策略。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface RequestUpgradeStrategy {

	/**
	 * 返回支持的 WebSocket 协议版本。
	 */
	String[] getSupportedVersions();

	/**
	 * 返回底层 WebSocket 服务器支持的 WebSocket 协议扩展。
	 */
	List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request);

	/**
	 * 执行运行时特定的步骤以完成升级。在握手请求成功协商后调用。
	 * @param request the current request
	 * @param response the current response
	 * @param selectedProtocol the selected sub-protocol, if any
	 * @param selectedExtensions the selected WebSocket protocol extensions
	 * @param user the user to associate with the WebSocket session
	 * @param wsHandler the handler for WebSocket messages
	 * @param attributes handshake request specific attributes to be set on the WebSocket
	 * session via {@link org.springframework.web.socket.server.HandshakeInterceptor} and
	 * thus made available to the {@link org.springframework.web.socket.WebSocketHandler}
	 * @throws HandshakeFailureException thrown when handshake processing failed to
	 * complete due to an internal, unrecoverable error, i.e. a server error as
	 * opposed to a failure to successfully negotiate the requirements of the
	 * handshake request.
	 */
	void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions,
			@Nullable Principal user, WebSocketHandler wsHandler, Map<String, Object> attributes)
			throws HandshakeFailureException;

}
