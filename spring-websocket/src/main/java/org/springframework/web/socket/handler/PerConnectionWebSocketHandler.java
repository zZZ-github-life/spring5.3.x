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

package org.springframework.web.socket.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 为每个 WebSocket 连接初始化和销毁 {@link WebSocketHandler} 实例，
 * 并将所有其他方法委托给该实例的 {@link WebSocketHandler}。
 *
 * <p>本质上，只需创建此类的一个实例，提供为每个连接创建的
 * {@link WebSocketHandler} 类类型，然后将其传递给任何期望
 * {@link WebSocketHandler} 的 API 方法。
 *
 * <p>如果初始化目标 {@link WebSocketHandler} 类型需要 Spring
 * BeanFactory，则相应地设置 {@link #setBeanFactory(BeanFactory)} 属性。
 * 只需将此声明为 Spring Bean 即可做到这一点。否则，
 * 目标类型的 {@link WebSocketHandler} 实例将使用默认构造函数创建。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PerConnectionWebSocketHandler implements WebSocketHandler, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(PerConnectionWebSocketHandler.class);


	private final BeanCreatingHandlerProvider<WebSocketHandler> provider;

	private final Map<WebSocketSession, WebSocketHandler> handlers =
			new ConcurrentHashMap<>();

	private final boolean supportsPartialMessages;


	public PerConnectionWebSocketHandler(Class<? extends WebSocketHandler> handlerType) {
		this(handlerType, false);
	}

	public PerConnectionWebSocketHandler(Class<? extends WebSocketHandler> handlerType, boolean supportsPartialMessages) {
		this.provider = new BeanCreatingHandlerProvider<>(handlerType);
		this.supportsPartialMessages = supportsPartialMessages;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.provider.setBeanFactory(beanFactory);
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		WebSocketHandler handler = this.provider.getHandler();
		this.handlers.put(session, handler);
		handler.afterConnectionEstablished(session);
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		getHandler(session).handleMessage(session, message);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		getHandler(session).handleTransportError(session, exception);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		try {
			getHandler(session).afterConnectionClosed(session, closeStatus);
		}
		finally {
			destroyHandler(session);
		}
	}

	@Override
	public boolean supportsPartialMessages() {
		return this.supportsPartialMessages;
	}


	private WebSocketHandler getHandler(WebSocketSession session) {
		WebSocketHandler handler = this.handlers.get(session);
		if (handler == null) {
			throw new IllegalStateException("WebSocketHandler not found for " + session);
		}
		return handler;
	}

	private void destroyHandler(WebSocketSession session) {
		WebSocketHandler handler = this.handlers.remove(session);
		try {
			if (handler != null) {
				this.provider.destroy(handler);
			}
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error while destroying " + handler, ex);
			}
		}
	}


	@Override
	public String toString() {
		return "PerConnectionWebSocketHandlerProxy[handlerType=" + this.provider.getHandlerType() + "]";
	}

}
