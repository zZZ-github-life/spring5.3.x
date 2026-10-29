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

import java.util.List;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * SockJS 传输的客户端实现。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface Transport {

	/**
	 * 返回此传输可用于的 SockJS 传输类型。
	 * 特别是，由于从客户端的角度来看 XHR 和 XHR 流式传输之间没有
	 * 区别，一个 {@code XhrTransport} 可以同时完成两者。
	 */
	List<TransportType> getTransportTypes();

	/**
	 * 连接传输。
	 * @param request the transport request.
	 * @param webSocketHandler the application handler to delegate lifecycle events to.
	 * @return a future to indicate success or failure to connect.
	 */
	ListenableFuture<WebSocketSession> connect(TransportRequest request, WebSocketHandler webSocketHandler);

}
