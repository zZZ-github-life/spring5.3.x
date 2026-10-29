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

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;

/**
 * 使用 HTTP 请求模拟 WebSocket 交互的 SockJS {@link Transport}。
 * 基础 {@code Transport} 接口的 {@code connect} 方法用于从服务器
 * 接收消息，而这里的 {@link #executeSendRequest} 方法用于发送消息。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface XhrTransport extends Transport, InfoReceiver {

	/**
	 * {@code XhrTransport} 同时支持 "xhr_streaming" 和 "xhr" SockJS
	 * 服务器传输。从客户端的角度来看，实现上没有
	 * 区别。
	 * <p>默认情况下，{@code XhrTransport} 将首先用于 "xhr_streaming"，
	 * 如果流式传输连接失败，则再用于 "xhr"。在某些
	 * 情况下，抑制流式传输以便仅使用 "xhr" 可能会有用。
	 */
	boolean isXhrStreamingDisabled();

	/**
	 * 执行将消息发送到服务器的请求。
	 * <p>请注意，自 4.2 起，此方法接受 {@code headers} 参数。
	 * @param transportUrl the URL for sending messages.
	 * @param message the message to send
	 */
	void executeSendRequest(URI transportUrl, HttpHeaders headers, TextMessage message);

}
