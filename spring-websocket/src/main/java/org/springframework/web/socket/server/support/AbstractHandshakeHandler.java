/*
 * Copyright 2002-2022 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * {@link HandshakeHandler} 实现的基类，与 Servlet API 无关。
 *
 * <p>执行 WebSocket 握手请求的初始校验——可能通过适当的 HTTP 状态码拒绝请求——
 * 同时允许其子类覆盖协商过程的各个部分（例如来源校验、子协议协商、
 * 扩展协商等）。
 *
 * <p>如果协商成功，实际的升级操作将委托给特定于服务器的
 * {@link org.springframework.web.socket.server.RequestUpgradeStrategy}，由其根据需要更新
 * 响应并初始化 WebSocket。目前支持的服务器包括
 * Jetty 9.0-9.3、Tomcat 7.0.47+ 和 8.x、Undertow 1.0-1.3、GlassFish 4.1+、WebLogic 12.1.3+。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.2
 * @see org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy
 * @see org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy
 * @see org.springframework.web.socket.server.standard.UndertowRequestUpgradeStrategy
 * @see org.springframework.web.socket.server.standard.GlassFishRequestUpgradeStrategy
 * @see org.springframework.web.socket.server.standard.WebLogicRequestUpgradeStrategy
 */
public abstract class AbstractHandshakeHandler implements HandshakeHandler, Lifecycle {

	private static final boolean tomcatWsPresent;

	private static final boolean jettyWsPresent;

	private static final boolean jetty10WsPresent;

	private static final boolean undertowWsPresent;

	private static final boolean glassfishWsPresent;

	private static final boolean weblogicWsPresent;

	private static final boolean websphereWsPresent;

	static {
		ClassLoader classLoader = AbstractHandshakeHandler.class.getClassLoader();
		tomcatWsPresent = ClassUtils.isPresent(
				"org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", classLoader);
		jetty10WsPresent = ClassUtils.isPresent(
				"org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer", classLoader);
		jettyWsPresent = ClassUtils.isPresent(
				"org.eclipse.jetty.websocket.server.WebSocketServerFactory", classLoader);
		undertowWsPresent = ClassUtils.isPresent(
				"io.undertow.websockets.jsr.ServerWebSocketContainer", classLoader);
		glassfishWsPresent = ClassUtils.isPresent(
				"org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler", classLoader);
		weblogicWsPresent = ClassUtils.isPresent(
				"weblogic.websocket.tyrus.TyrusServletWriter", classLoader);
		websphereWsPresent = ClassUtils.isPresent(
				"com.ibm.websphere.wsoc.WsWsocServerContainer", classLoader);
	}


	protected final Log logger = LogFactory.getLog(getClass());

	private final RequestUpgradeStrategy requestUpgradeStrategy;

	private final List<String> supportedProtocols = new ArrayList<>();

	private volatile boolean running;


	/**
	 * 默认构造函数，自动检测并实例化适合当前运行时容器的
	 * {@link RequestUpgradeStrategy}。
	 * @throws IllegalStateException if no {@link RequestUpgradeStrategy} can be found.
	 */
	protected AbstractHandshakeHandler() {
		this(initRequestUpgradeStrategy());
	}

	/**
	 * 接受运行时特定 {@link RequestUpgradeStrategy} 的构造函数。
	 * @param requestUpgradeStrategy the upgrade strategy to use
	 */
	protected AbstractHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
		Assert.notNull(requestUpgradeStrategy, "RequestUpgradeStrategy must not be null");
		this.requestUpgradeStrategy = requestUpgradeStrategy;
	}


	private static RequestUpgradeStrategy initRequestUpgradeStrategy() {
		String className;
		if (tomcatWsPresent) {
			className = "org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy";
		}
		else if (jettyWsPresent) {
			className = "org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy";
		}
		else if (jetty10WsPresent) {
			className = "org.springframework.web.socket.server.jetty.Jetty10RequestUpgradeStrategy";
		}
		else if (undertowWsPresent) {
			className = "org.springframework.web.socket.server.standard.UndertowRequestUpgradeStrategy";
		}
		else if (glassfishWsPresent) {
			className = "org.springframework.web.socket.server.standard.GlassFishRequestUpgradeStrategy";
		}
		else if (weblogicWsPresent) {
			className = "org.springframework.web.socket.server.standard.WebLogicRequestUpgradeStrategy";
		}
		else if (websphereWsPresent) {
			className = "org.springframework.web.socket.server.standard.WebSphereRequestUpgradeStrategy";
		}
		else {
			throw new IllegalStateException("No suitable default RequestUpgradeStrategy found");
		}

		try {
			Class<?> clazz = ClassUtils.forName(className, AbstractHandshakeHandler.class.getClassLoader());
			return (RequestUpgradeStrategy) ReflectionUtils.accessibleConstructor(clazz).newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Failed to instantiate RequestUpgradeStrategy: " + className, ex);
		}
	}


	/**
	 * 返回用于 WebSocket 请求的 {@link RequestUpgradeStrategy}。
	 */
	public RequestUpgradeStrategy getRequestUpgradeStrategy() {
		return this.requestUpgradeStrategy;
	}

	/**
	 * 使用此属性配置受支持的子协议列表。
	 * 第一个与客户端请求的子协议匹配的已配置子协议将被接受。
	 * 如果没有匹配项，响应中将不包含
	 * {@literal Sec-WebSocket-Protocol} 头。
	 * <p>请注意，如果运行时传入的 WebSocketHandler 是
	 * {@link SubProtocolCapable} 的实例，则无需显式配置
	 * 此属性。内置的 STOMP over WebSocket 支持正是如此。
	 * 因此，仅当 WebSocketHandler 未实现 {@code SubProtocolCapable} 时，
	 * 才应显式配置此属性。
	 */
	public void setSupportedProtocols(String... protocols) {
		this.supportedProtocols.clear();
		for (String protocol : protocols) {
			this.supportedProtocols.add(protocol.toLowerCase());
		}
	}

	/**
	 * 返回受支持的子协议列表。
	 */
	public String[] getSupportedProtocols() {
		return StringUtils.toStringArray(this.supportedProtocols);
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			doStart();
		}
	}

	protected void doStart() {
		if (this.requestUpgradeStrategy instanceof Lifecycle) {
			((Lifecycle) this.requestUpgradeStrategy).start();
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			doStop();
		}
	}

	protected void doStop() {
		if (this.requestUpgradeStrategy instanceof Lifecycle) {
			((Lifecycle) this.requestUpgradeStrategy).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public final boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHeaders());
		if (logger.isTraceEnabled()) {
			logger.trace("Processing request " + request.getURI() + " with headers=" + headers);
		}
		try {
			if (HttpMethod.GET != request.getMethod()) {
				response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
				response.getHeaders().setAllow(Collections.singleton(HttpMethod.GET));
				if (logger.isErrorEnabled()) {
					logger.error("Handshake failed due to unexpected HTTP method: " + request.getMethod());
				}
				return false;
			}
			if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
				handleInvalidUpgradeHeader(request, response);
				return false;
			}
			if (!headers.getConnection().contains("Upgrade") && !headers.getConnection().contains("upgrade")) {
				handleInvalidConnectHeader(request, response);
				return false;
			}
			if (!isWebSocketVersionSupported(headers)) {
				handleWebSocketVersionNotSupported(request, response);
				return false;
			}
			if (!isValidOrigin(request)) {
				response.setStatusCode(HttpStatus.FORBIDDEN);
				return false;
			}
			String wsKey = headers.getSecWebSocketKey();
			if (wsKey == null) {
				if (logger.isErrorEnabled()) {
					logger.error("Missing \"Sec-WebSocket-Key\" header");
				}
				response.setStatusCode(HttpStatus.BAD_REQUEST);
				return false;
			}
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket: " + request.getURI(), ex);
		}

		String subProtocol = selectProtocol(headers.getSecWebSocketProtocol(), wsHandler);
		List<WebSocketExtension> requested = headers.getSecWebSocketExtensions();
		List<WebSocketExtension> supported = this.requestUpgradeStrategy.getSupportedExtensions(request);
		List<WebSocketExtension> extensions = filterRequestedExtensions(request, requested, supported);
		Principal user = determineUser(request, wsHandler, attributes);

		if (logger.isTraceEnabled()) {
			logger.trace("Upgrading to WebSocket, subProtocol=" + subProtocol + ", extensions=" + extensions);
		}
		this.requestUpgradeStrategy.upgrade(request, response, subProtocol, extensions, user, wsHandler, attributes);
		return true;
	}

	protected void handleInvalidUpgradeHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		if (logger.isErrorEnabled()) {
			logger.error(LogFormatUtils.formatValue(
					"Handshake failed due to invalid Upgrade header: " + request.getHeaders().getUpgrade(), -1, true));
		}
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("Can \"Upgrade\" only to \"WebSocket\".".getBytes(StandardCharsets.UTF_8));
	}

	protected void handleInvalidConnectHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		if (logger.isErrorEnabled()) {
			logger.error(LogFormatUtils.formatValue(
					"Handshake failed due to invalid Connection header" + request.getHeaders().getConnection(), -1, true));
		}
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("\"Connection\" must be \"upgrade\".".getBytes(StandardCharsets.UTF_8));
	}

	protected boolean isWebSocketVersionSupported(WebSocketHttpHeaders httpHeaders) {
		String version = httpHeaders.getSecWebSocketVersion();
		String[] supportedVersions = getSupportedVersions();
		for (String supportedVersion : supportedVersions) {
			if (supportedVersion.trim().equals(version)) {
				return true;
			}
		}
		return false;
	}

	protected String[] getSupportedVersions() {
		return this.requestUpgradeStrategy.getSupportedVersions();
	}

	protected void handleWebSocketVersionNotSupported(ServerHttpRequest request, ServerHttpResponse response) {
		if (logger.isErrorEnabled()) {
			String version = request.getHeaders().getFirst("Sec-WebSocket-Version");
			logger.error(LogFormatUtils.formatValue(
					"Handshake failed due to unsupported WebSocket version: " + version +
							". Supported versions: " + Arrays.toString(getSupportedVersions()), -1, true));
		}
		response.setStatusCode(HttpStatus.UPGRADE_REQUIRED);
		response.getHeaders().set(WebSocketHttpHeaders.SEC_WEBSOCKET_VERSION,
				StringUtils.arrayToCommaDelimitedString(getSupportedVersions()));
	}

	/**
	 * 返回请求的 {@code Origin} 头值是否有效。
	 * 默认情况下，所有来源都被视为有效。如有需要，可考虑使用
	 * {@link OriginHandshakeInterceptor} 过滤来源。
	 */
	protected boolean isValidOrigin(ServerHttpRequest request) {
		return true;
	}

	/**
	 * 基于请求的子协议和受支持的子协议执行子协议协商。
	 * 对于受支持的子协议列表，此方法首先检查目标
	 * WebSocketHandler 是否为 {@link SubProtocolCapable}，然后检查是否已通过
	 * {@link #setSupportedProtocols(String...)} 显式配置了任何子协议。
	 * @param requestedProtocols the requested sub-protocols
	 * @param webSocketHandler the WebSocketHandler that will be used
	 * @return the selected protocols or {@code null}
	 * @see #determineHandlerSupportedProtocols(WebSocketHandler)
	 */
	@Nullable
	protected String selectProtocol(List<String> requestedProtocols, WebSocketHandler webSocketHandler) {
		List<String> handlerProtocols = determineHandlerSupportedProtocols(webSocketHandler);
		for (String protocol : requestedProtocols) {
			if (handlerProtocols.contains(protocol.toLowerCase())) {
				return protocol;
			}
			if (this.supportedProtocols.contains(protocol.toLowerCase())) {
				return protocol;
			}
		}
		return null;
	}

	/**
	 * 通过检查给定 WebSocketHandler 是否为 {@link SubProtocolCapable} 的实例，
	 * 确定其支持的子协议。
	 * @param handler the handler to check
	 * @return a list of supported protocols, or an empty list if none available
	 */
	protected final List<String> determineHandlerSupportedProtocols(WebSocketHandler handler) {
		WebSocketHandler handlerToCheck = WebSocketHandlerDecorator.unwrap(handler);
		List<String> subProtocols = null;
		if (handlerToCheck instanceof SubProtocolCapable) {
			subProtocols = ((SubProtocolCapable) handlerToCheck).getSubProtocols();
		}
		return (subProtocols != null ? subProtocols : Collections.emptyList());
	}

	/**
	 * 过滤请求的 WebSocket 扩展列表。
	 * <p>自 4.1 起，此方法的默认实现会过滤列表，
	 * 仅保留既被请求又受支持的扩展。
	 * @param request the current request
	 * @param requestedExtensions the list of extensions requested by the client
	 * @param supportedExtensions the list of extensions supported by the server
	 * @return the selected extensions or an empty list
	 */
	protected List<WebSocketExtension> filterRequestedExtensions(ServerHttpRequest request,
			List<WebSocketExtension> requestedExtensions, List<WebSocketExtension> supportedExtensions) {

		List<WebSocketExtension> result = new ArrayList<>(requestedExtensions.size());
		for (WebSocketExtension extension : requestedExtensions) {
			if (supportedExtensions.contains(extension)) {
				result.add(extension);
			}
		}
		return result;
	}

	/**
	 * 可用于在 WebSocket 会话建立过程中将用户与会话关联的方法。
	 * 默认实现调用 {@link ServerHttpRequest#getPrincipal()}。
	 * <p>子类可以提供自定义逻辑来将用户与会话关联，
	 * 例如为匿名用户（即未完全通过身份验证的用户）分配名称。
	 * @param request the handshake request
	 * @param wsHandler the WebSocket handler that will handle messages
	 * @param attributes handshake attributes to pass to the WebSocket session
	 * @return the user for the WebSocket session, or {@code null} if not available
	 */
	@Nullable
	protected Principal determineUser(
			ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

		return request.getPrincipal();
	}

}
