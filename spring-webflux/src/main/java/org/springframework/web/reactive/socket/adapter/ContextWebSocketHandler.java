/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.socket.adapter;

import java.util.List;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * 用于丰富目标处理器上下文的 {@link WebSocketHandler} 装饰器。
 *
 * @author Rossen Stoyanchev
 * @since 5.3.3
 */
public final class ContextWebSocketHandler implements WebSocketHandler {

	private final WebSocketHandler delegate;

	private final ContextView contextView;


	private ContextWebSocketHandler(WebSocketHandler delegate, ContextView contextView) {
		this.delegate = delegate;
		this.contextView = contextView;
	}


	@Override
	public List<String> getSubProtocols() {
		return this.delegate.getSubProtocols();
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		return this.delegate.handle(session).contextWrite(this.contextView);
	}


	/**
	 * 返回给定处理器的装饰版本，以插入给定上下文；当上下文为空时返回同一处理器实例。
	 */
	public static WebSocketHandler decorate(WebSocketHandler handler, ContextView contextView) {
		return (!contextView.isEmpty() ? new ContextWebSocketHandler(handler, contextView) : handler);
	}

}
