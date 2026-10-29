/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket.client;

import java.net.URI;

import org.springframework.lang.Nullable;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;

/**
 * 发起 WebSocket 请求的契约。另一种选择是使用声明式风格的
 * {@link WebSocketConnectionManager}，它在应用程序启动时启动到
 * 预配置 URI 的 WebSocket 连接。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see WebSocketConnectionManager
 */
public interface WebSocketClient {

	ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			String uriTemplate, Object... uriVariables);

	ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			@Nullable WebSocketHttpHeaders headers, URI uri);

}
