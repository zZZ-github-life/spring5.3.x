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

package org.springframework.web.socket.sockjs;

import java.util.List;

/**
 * 当消息帧通过 HTTP POST 成功接收并解析，但其中包含的一条或多条消息
 * 由于处理器失败或连接关闭而无法传递给 WebSocketHandler 时抛出的异常。
 *
 * <p>此异常发生后，SockJS 会话不会自动关闭。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SockJsMessageDeliveryException extends SockJsException {

	private final List<String> undeliveredMessages;


	public SockJsMessageDeliveryException(String sessionId, List<String> undeliveredMessages, Throwable cause) {
		super("Failed to deliver message(s) " + undeliveredMessages + " for session " + sessionId, sessionId, cause);
		this.undeliveredMessages = undeliveredMessages;
	}

	public SockJsMessageDeliveryException(String sessionId, List<String> undeliveredMessages, String message) {
		super("Failed to deliver message(s) " + undeliveredMessages + " for session "
				+ sessionId + ": " + message, sessionId, null);
		this.undeliveredMessages = undeliveredMessages;
	}

	public List<String> getUndeliveredMessages() {
		return this.undeliveredMessages;
	}

}
