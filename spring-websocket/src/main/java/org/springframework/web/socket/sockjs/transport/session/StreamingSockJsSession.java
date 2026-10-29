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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * 用于流式 HTTP 传输的 SockJS 会话。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class StreamingSockJsSession extends AbstractHttpSockJsSession {

	private int byteCount;


	public StreamingSockJsSession(String sessionId, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(sessionId, config, wsHandler, attributes);
	}


	/**
	 * 获取在任何其他数据之前写入响应的前导内容。
	 * @since 4.2
	 */
	protected abstract byte[] getPrelude(ServerHttpRequest request);


	@Override
	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			boolean initialRequest) throws IOException {

		byte[] prelude = getPrelude(request);
		response.getBody().write(prelude);
		response.flush();

		if (initialRequest) {
			writeFrame(SockJsFrame.openFrame());
		}
		flushCache();
	}

	@Override
	protected void flushCache() throws SockJsTransportFailureException {
		while (!getMessageCache().isEmpty()) {
			String message = getMessageCache().poll();
			SockJsMessageCodec messageCodec = getSockJsServiceConfig().getMessageCodec();
			SockJsFrame frame = SockJsFrame.messageFrame(messageCodec, message);
			writeFrame(frame);

			this.byteCount += (frame.getContentBytes().length + 1);
			if (logger.isTraceEnabled()) {
				logger.trace(this.byteCount + " bytes written so far, " +
						getMessageCache().size() + " more messages not flushed");
			}
			if (this.byteCount >= getSockJsServiceConfig().getStreamBytesLimit()) {
				logger.trace("Streamed bytes limit reached, recycling current request");
				resetRequest();
				this.byteCount = 0;
				break;
			}
		}
		scheduleHeartbeat();
	}

}
