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

package org.springframework.web.socket.server.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 用于处理 WebSocket 握手请求的 {@link HttpRequestHandler}。
 *
 * <p>这是在特定 URL 上配置服务器 WebSocket 时应使用的主要类。
 * 它是对 {@link WebSocketHandler} 和 {@link HandshakeHandler} 的非常薄的封装，
 * 同时分别将 {@link HttpServletRequest} 和 {@link HttpServletResponse} 适配为
 * {@link ServerHttpRequest} 和 {@link ServerHttpResponse}。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHttpRequestHandler implements HttpRequestHandler, Lifecycle, ServletContextAware {

	private static final Log logger = LogFactory.getLog(WebSocketHttpRequestHandler.class);

	private final WebSocketHandler wsHandler;

	private final HandshakeHandler handshakeHandler;

	private final List<HandshakeInterceptor> interceptors = new ArrayList<>();

	private volatile boolean running;


	public WebSocketHttpRequestHandler(WebSocketHandler wsHandler) {
		this(wsHandler, new DefaultHandshakeHandler());
	}

	public WebSocketHttpRequestHandler(WebSocketHandler wsHandler, HandshakeHandler handshakeHandler) {
		Assert.notNull(wsHandler, "wsHandler must not be null");
		Assert.notNull(handshakeHandler, "handshakeHandler must not be null");
		this.wsHandler = decorate(wsHandler);
		this.handshakeHandler = handshakeHandler;
	}

	/**
	 * 装饰传入构造函数的 {@code WebSocketHandler}。
	 * <p>默认情况下，会添加 {@link LoggingWebSocketHandlerDecorator} 和
	 * {@link ExceptionWebSocketHandlerDecorator}。
	 * @since 5.2.2
	 */
	protected WebSocketHandler decorate(WebSocketHandler handler) {
		return new ExceptionWebSocketHandlerDecorator(new LoggingWebSocketHandlerDecorator(handler));
	}


	/**
	 * 返回 WebSocketHandler。
	 */
	public WebSocketHandler getWebSocketHandler() {
		return this.wsHandler;
	}

	/**
	 * 返回 HandshakeHandler。
	 */
	public HandshakeHandler getHandshakeHandler() {
		return this.handshakeHandler;
	}

	/**
	 * 配置一个或多个 WebSocket 握手请求拦截器。
	 */
	public void setHandshakeInterceptors(@Nullable List<HandshakeInterceptor> interceptors) {
		this.interceptors.clear();
		if (interceptors != null) {
			this.interceptors.addAll(interceptors);
		}
	}

	/**
	 * 返回配置的 WebSocket 握手请求拦截器。
	 */
	public List<HandshakeInterceptor> getHandshakeInterceptors() {
		return this.interceptors;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.handshakeHandler instanceof ServletContextAware) {
			((ServletContextAware) this.handshakeHandler).setServletContext(servletContext);
		}
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			if (this.handshakeHandler instanceof Lifecycle) {
				((Lifecycle) this.handshakeHandler).start();
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			if (this.handshakeHandler instanceof Lifecycle) {
				((Lifecycle) this.handshakeHandler).stop();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		ServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, this.wsHandler);
		HandshakeFailureException failure = null;

		try {
			if (logger.isDebugEnabled()) {
				logger.debug(servletRequest.getMethod() + " " + servletRequest.getRequestURI());
			}
			Map<String, Object> attributes = new HashMap<>();
			if (!chain.applyBeforeHandshake(request, response, attributes)) {
				return;
			}
			this.handshakeHandler.doHandshake(request, response, this.wsHandler, attributes);
			chain.applyAfterHandshake(request, response, null);
		}
		catch (HandshakeFailureException ex) {
			failure = ex;
		}
		catch (Exception ex) {
			failure = new HandshakeFailureException("Uncaught failure for request " + request.getURI(), ex);
		}
		finally {
			if (failure != null) {
				chain.applyAfterHandshake(request, response, failure);
				response.close();
				throw failure;
			}
			response.close();
		}
	}

}
