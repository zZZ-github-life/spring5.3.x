/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.socket.server.support;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * {@link SimpleUrlHandlerMapping} 的扩展，支持将 WebSocket 握手请求
 * 更精确地映射到 {@link WebSocketHttpRequestHandler} 类型的处理器。
 * 还会将 {@link Lifecycle} 方法委托给 {@link #getUrlMap()} 中实现了
 * 这些方法的处理器。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class WebSocketHandlerMapping extends SimpleUrlHandlerMapping implements SmartLifecycle {

	private boolean webSocketUpgradeMatch;

	private volatile boolean running;


	/**
	 * 设置后，如果匹配的处理器是
	 * {@link WebSocketHttpRequestHandler}，则确保请求是 WebSocket
	 * 握手，即带有 {@code "Upgrade:websocket"} 头的 HTTP GET 请求，
	 * 否则取消匹配并返回 {@code null}，允许另一个
	 * {@link org.springframework.web.servlet.HandlerMapping} 匹配同一 URL 路径。
	 * @param match 是否启用对 {@code "Upgrade: websocket"} 的匹配
	 * @since 5.3.5
	 */
	public void setWebSocketUpgradeMatch(boolean match) {
		this.webSocketUpgradeMatch = match;
	}


	@Override
	protected void initServletContext(ServletContext servletContext) {
		for (Object handler : getUrlMap().values()) {
			if (handler instanceof ServletContextAware) {
				((ServletContextAware) handler).setServletContext(servletContext);
			}
		}
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			for (Object handler : getUrlMap().values()) {
				if (handler instanceof Lifecycle) {
					((Lifecycle) handler).start();
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			for (Object handler : getUrlMap().values()) {
				if (handler instanceof Lifecycle) {
					((Lifecycle) handler).stop();
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	@Nullable
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		Object handler = super.getHandlerInternal(request);
		return (matchWebSocketUpgrade(handler, request) ? handler : null);
	}

	private boolean matchWebSocketUpgrade(@Nullable Object handler, HttpServletRequest request) {
		handler = (handler instanceof HandlerExecutionChain ?
				((HandlerExecutionChain) handler).getHandler() : handler);
		if (this.webSocketUpgradeMatch && handler instanceof WebSocketHttpRequestHandler) {
			String header = request.getHeader(HttpHeaders.UPGRADE);
			return (request.getMethod().equals("GET") &&
					header != null && header.equalsIgnoreCase("websocket"));
		}
		return true;
	}

}
