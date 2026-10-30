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

package org.springframework.web.socket.sockjs.transport;

import java.util.Map;

import org.springframework.web.socket.WebSocketHandler;

/**
 * 用于创建 SockJS 会话的工厂。{@link TransportHandler TransportHandler} 通常也
 * 充当 SockJS 会话工厂。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsSessionFactory {

	/**
	 * 创建一个新的 SockJS 会话。
	 * @param sessionId 会话的 ID
	 * @param handler 底层的 {@link WebSocketHandler}
	 * @param attributes 握手请求特有的属性
	 * @return 新的会话，永不为 {@code null}
	 */
	SockJsSession createSession(String sessionId, WebSocketHandler handler, Map<String, Object> attributes);

}
