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

package org.springframework.web.socket.handler;

import java.io.IOException;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

/**
 * {@link WebSocketHandler} 实现的便捷基类，仅处理二进制消息。
 *
 * <p>文本消息会以 {@link CloseStatus#NOT_ACCEPTABLE} 拒绝。
 * 所有其他方法均为空实现。
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0
 */
public class BinaryWebSocketHandler extends AbstractWebSocketHandler {

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		try {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
		}
		catch (IOException ex) {
			// ignore
		}
	}

}
