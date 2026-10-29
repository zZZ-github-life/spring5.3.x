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

package org.springframework.web.socket.sockjs.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.util.WebUtils;

/**
 * {@link SockJsService} 实现的抽象基类，提供 SockJS 路径解析以及
 * 对静态 SockJS 请求（例如 "/info"、"/iframe.html"
 * 等）的处理。子类必须处理会话 URL（即特定于传输的请求）。
 *
 * <p>默认情况下，仅允许同源请求。请使用 {@link #setAllowedOrigins}
 * 指定允许的来源列表（包含 "*" 的列表将允许所有来源）。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public abstract class AbstractSockJsService implements SockJsService, CorsConfigurationSource {

	private static final String XFRAME_OPTIONS_HEADER = "X-Frame-Options";

	private static final long ONE_YEAR = TimeUnit.DAYS.toSeconds(365);


	private static final Random random = new Random();

	protected final Log logger = LogFactory.getLog(getClass());

	private final TaskScheduler taskScheduler;

	private String name = "SockJSService@" + ObjectUtils.getIdentityHexString(this);

	private String clientLibraryUrl = "https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js";

	private int streamBytesLimit = 128 * 1024;

	private boolean sessionCookieNeeded = true;

	private long heartbeatTime = TimeUnit.SECONDS.toMillis(25);

	private long disconnectDelay = TimeUnit.SECONDS.toMillis(5);

	private int httpMessageCacheSize = 100;

	private boolean webSocketEnabled = true;

	private boolean suppressCors = false;

	protected final CorsConfiguration corsConfiguration;

	private final SockJsRequestHandler infoHandler = new InfoHandler();

	private final SockJsRequestHandler iframeHandler = new IframeHandler();


	public AbstractSockJsService(TaskScheduler scheduler) {
		Assert.notNull(scheduler, "TaskScheduler must not be null");
		this.taskScheduler = scheduler;
		this.corsConfiguration = initCorsConfiguration();
	}

	private static CorsConfiguration initCorsConfiguration() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedMethod("*");
		config.setAllowedOrigins(Collections.emptyList());
		config.setAllowedOriginPatterns(Collections.emptyList());
		config.setAllowCredentials(true);
		config.setMaxAge(ONE_YEAR);
		config.addAllowedHeader("*");
		return config;
	}


	/**
	 * 用于调度心跳消息的调度器实例。
	 */
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * 为此服务设置唯一的名称（主要用于日志记录）。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回与此服务关联的唯一名称。
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 没有原生跨域通信的传输（例如 "eventsource"、
	 * "htmlfile"）必须在一个不可见的 iframe 中从"外部"域获取一个简单页面，
	 * 以便 iframe 中的代码可以从 SockJS 服务器本地域运行。
	 * 由于 iframe 需要加载 SockJS JavaScript 客户端库，
	 * 此属性允许指定从何处加载该库。
	 * <p>默认情况下，此属性设置为指向
	 * "https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js"。
	 * 不过，也可以将其设置为指向由应用程序提供的 URL。
	 * <p>请注意，可以指定相对 URL，在这种情况下，URL
	 * 必须相对于 iframe URL。例如，假设 SockJS 端点
	 * 映射到 "/sockjs"，生成的 iframe URL 为 "/sockjs/iframe.html"，那么
	 * 相对 URL 必须以 "../../" 开头，以向上到达 SockJS 映射
	 * 之上的位置。对于基于前缀的 Servlet 映射，可能还需要
	 * 再进行一次向上遍历。
	 */
	public void setSockJsClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
	}

	/**
	 * 返回 SockJS JavaScript 客户端库的 URL。
	 */
	public String getSockJsClientLibraryUrl() {
		return this.clientLibraryUrl;
	}

	/**
	 * 流式传输会在客户端保留响应，并且不会释放
	 * 已传递消息占用的内存。此类传输需要不时地回收
	 * 连接。此属性设置单个 HTTP 流式请求在被关闭之前
	 * 可以发送的最小字节数。之后客户端将打开
	 * 新的请求。将此值设置为 1 会实际禁用流式传输，
	 * 并使流式传输的行为类似于轮询传输。
	 * <p>默认值为 128K（即 128 * 1024）。
	 */
	public void setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
	}

	/**
	 * 返回在关闭单个 HTTP 流式请求之前可以通过其发送的最小字节数。
	 */
	public int getStreamBytesLimit() {
		return this.streamBytesLimit;
	}

	/**
	 * SockJS 协议要求服务器在对客户端初始 "/info" 请求的响应中包含一个
	 * "cookie_needed" 布尔属性，用于指示应用程序正常运行是否
	 * 需要使用 JSESSIONID cookie，例如
	 * 为了负载均衡，或者在 Java Servlet 容器中为了使用 HTTP 会话。
	 * <p>这对于支持 XDomainRequest（一种修改过的
	 * AJAX/XHR，可以跨域发送请求但不发送任何 cookie）的 IE 8、9 尤为重要。
	 * 在这些情况下，SockJS 客户端倾向于使用 "iframe-htmlfile" 传输而不是
	 * "xdr-streaming"，以便能够发送 cookie。
	 * <p>SockJS 协议还期望 SockJS 服务在此属性设置为 true 时
	 * 回显 JSESSIONID cookie。不过，在 Servlet
	 * 容器中运行时，这并非必需，因为容器会负责处理。
	 * <p>默认值为 "true"，以最大程度地确保应用程序支持 cookie（特别是
	 * JSESSIONID cookie）时能够在 IE 8、9 中正常工作。
	 * 不过，如果不需要使用 cookie（和 HTTP 会话），
	 * 应用程序可以选择将其设置为 "false"。
	 */
	public void setSessionCookieNeeded(boolean sessionCookieNeeded) {
		this.sessionCookieNeeded = sessionCookieNeeded;
	}

	/**
	 * 返回应用程序正常运行是否需要 JSESSIONID cookie。
	 */
	public boolean isSessionCookieNeeded() {
		return this.sessionCookieNeeded;
	}

	/**
	 * 指定服务器未发送任何消息时的毫秒数，超过该时间后，
	 * 服务器应向客户端发送心跳帧，
	 * 以防止连接中断。
	 * <p>默认值为 25,000（25 秒）。
	 */
	public void setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
	}

	/**
	 * 返回服务器未发送任何消息的毫秒数。
	 */
	public long getHeartbeatTime() {
		return this.heartbeatTime;
	}

	/**
	 * 在客户端没有接收连接（即服务器可以通过其向客户端发送数据的活动连接）后，
	 * 将客户端视为断开连接之前的毫秒数。
	 * <p>默认值为 5000。
	 */
	public void setDisconnectDelay(long disconnectDelay) {
		this.disconnectDelay = disconnectDelay;
	}

	/**
	 * 返回将客户端视为断开连接之前的毫秒数。
	 */
	public long getDisconnectDelay() {
		return this.disconnectDelay;
	}

	/**
	 * 会话在等待客户端下一次 HTTP 轮询请求时可以缓存的服务端到客户端消息数。
	 * 所有 HTTP 传输都使用此属性，因为即使是流式传输也会定期回收 HTTP 请求。
	 * <p>HTTP 请求之间的时间应相对较短，
	 * 并且不会超过允许的断开连接延迟（参见 {@link #setDisconnectDelay(long)}），
	 * 默认情况下为 5 秒。
	 * <p>默认大小为 100。
	 */
	public void setHttpMessageCacheSize(int httpMessageCacheSize) {
		this.httpMessageCacheSize = httpMessageCacheSize;
	}

	/**
	 * 返回 HTTP 消息缓存的大小。
	 */
	public int getHttpMessageCacheSize() {
		return this.httpMessageCacheSize;
	}

	/**
	 * 某些负载均衡器不支持 WebSocket。此选项可用于
	 * 在服务器端禁用 WebSocket 传输。
	 * <p>默认值为 "true"。
	 */
	public void setWebSocketEnabled(boolean webSocketEnabled) {
		this.webSocketEnabled = webSocketEnabled;
	}

	/**
	 * 返回是否启用了 WebSocket 传输。
	 */
	public boolean isWebSocketEnabled() {
		return this.webSocketEnabled;
	}

	/**
	 * 此选项可用于禁用为 SockJS 请求自动添加 CORS 头。
	 * <p>默认值为 "false"。
	 * @since 4.1.2
	 */
	public void setSuppressCors(boolean suppressCors) {
		this.suppressCors = suppressCors;
	}

	/**
	 * 返回自动添加 CORS 头是否已被禁用。
	 * @since 4.1.2
	 * @see #setSuppressCors
	 */
	public boolean shouldSuppressCors() {
		return this.suppressCors;
	}

	/**
	 * 设置允许浏览器发起跨域请求的来源。
	 * 有关格式细节和注意事项，请参阅 {@link CorsConfiguration#setAllowedOrigins(List)}，
	 * 并记住 CORS 规范不允许在 {@code allowCredentials=true} 时使用 {@code "*"}。
	 * 如需更灵活的来源模式，请改用 {@link #setAllowedOriginPatterns}。
	 *
	 * <p>默认情况下，不允许任何来源。当同时设置了
	 * {@link #setAllowedOriginPatterns(Collection) allowedOriginPatterns} 时，
	 * 该属性优先于此属性。
	 *
	 * <p>请注意，当启用 SockJS 且限制了来源时，不允许检查请求来源的
	 * 传输类型（基于 Iframe 的传输）将被禁用。因此，在限制来源时
	 * 不支持 IE 6 到 9。
	 * @since 4.1.2
	 * @see #setAllowedOriginPatterns(Collection)
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		Assert.notNull(allowedOrigins, "Allowed origins Collection must not be null");
		this.corsConfiguration.setAllowedOrigins(new ArrayList<>(allowedOrigins));
	}

	/**
	 * 返回 {@link #setAllowedOrigins(Collection) 已配置的}允许来源。
	 * @since 4.1.2
	 */
	@SuppressWarnings("ConstantConditions")
	public Collection<String> getAllowedOrigins() {
		return this.corsConfiguration.getAllowedOrigins();
	}
	/**
	 * {@link #setAllowedOrigins(Collection)} 的替代方案，支持更灵活的模式
	 * 来指定允许浏览器发起跨域请求的来源。有关格式细节和其他注意事项，
	 * 请参阅 {@link CorsConfiguration#setAllowedOriginPatterns(List)}。
	 * <p>默认情况下未设置。
	 * @since 5.2.3
	 */
	public void setAllowedOriginPatterns(Collection<String> allowedOriginPatterns) {
		Assert.notNull(allowedOriginPatterns, "Allowed origin patterns Collection must not be null");
		this.corsConfiguration.setAllowedOriginPatterns(new ArrayList<>(allowedOriginPatterns));
	}

	/**
	 * 返回 {@link #setAllowedOriginPatterns(Collection) 已配置的}来源模式。
	 * @since 5.3.2
	 */
	@SuppressWarnings("ConstantConditions")
	public Collection<String> getAllowedOriginPatterns() {
		return this.corsConfiguration.getAllowedOriginPatterns();
	}


	/**
	 * 此方法确定 SockJS 路径并处理 SockJS 静态 URL。
	 * 会话 URL 和原始 WebSocket 请求将委托给抽象方法。
	 */
	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String sockJsPath, WebSocketHandler wsHandler) throws SockJsException {

		if (sockJsPath == null) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Expected SockJS path. Failing request: " + request.getURI(), -1, true));
			}
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		try {
			request.getHeaders();
		}
		catch (InvalidMediaTypeException ex) {
			// As per SockJS protocol content-type can be ignored (it's always json)
		}

		String requestInfo = (logger.isDebugEnabled() ? request.getMethod() + " " + request.getURI() : null);

		try {
			if (sockJsPath.isEmpty() || sockJsPath.equals("/")) {
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				if ("websocket".equalsIgnoreCase(request.getHeaders().getUpgrade())) {
					response.setStatusCode(HttpStatus.BAD_REQUEST);
					return;
				}
				response.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
				response.getBody().write("Welcome to SockJS!\n".getBytes(StandardCharsets.UTF_8));
			}

			else if (sockJsPath.equals("/info")) {
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				this.infoHandler.handle(request, response);
			}

			else if (sockJsPath.matches("/iframe[0-9-.a-z_]*.html")) {
				if (!getAllowedOrigins().isEmpty() && !getAllowedOrigins().contains("*") ||
						!getAllowedOriginPatterns().isEmpty()) {
					if (requestInfo != null) {
						logger.debug("Iframe support is disabled when an origin check is required. " +
								"Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
				if (getAllowedOrigins().isEmpty()) {
					response.getHeaders().add(XFRAME_OPTIONS_HEADER, "SAMEORIGIN");
				}
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				this.iframeHandler.handle(request, response);
			}

			else if (sockJsPath.equals("/websocket")) {
				if (isWebSocketEnabled()) {
					if (requestInfo != null) {
						logger.debug("Processing transport request: " + requestInfo);
					}
					handleRawWebSocketRequest(request, response, wsHandler);
				}
				else if (requestInfo != null) {
					logger.debug("WebSocket disabled. Ignoring transport request: " + requestInfo);
				}
			}

			else {
				String[] pathSegments = StringUtils.tokenizeToStringArray(sockJsPath.substring(1), "/");
				if (pathSegments.length != 3) {
					if (logger.isWarnEnabled()) {
						logger.warn(LogFormatUtils.formatValue("Invalid SockJS path '" + sockJsPath + "' - " +
								"required to have 3 path segments", -1, true));
					}
					if (requestInfo != null) {
						logger.debug("Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}

				String serverId = pathSegments[0];
				String sessionId = pathSegments[1];
				String transport = pathSegments[2];

				if (!isWebSocketEnabled() && transport.equals("websocket")) {
					if (requestInfo != null) {
						logger.debug("WebSocket disabled. Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
				else if (!validateRequest(serverId, sessionId, transport) || !validatePath(request)) {
					if (requestInfo != null) {
						logger.debug("Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}

				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				handleTransportRequest(request, response, wsHandler, sessionId, transport);
			}
			response.close();
		}
		catch (IOException ex) {
			throw new SockJsException("Failed to write to the response", null, ex);
		}
	}

	protected boolean validateRequest(String serverId, String sessionId, String transport) {
		if (!StringUtils.hasText(serverId) || !StringUtils.hasText(sessionId) || !StringUtils.hasText(transport)) {
			logger.warn("No server, session, or transport path segment in SockJS request.");
			return false;
		}

		// Server and session id's must not contain "."
		if (serverId.contains(".") || sessionId.contains(".")) {
			logger.warn("Either server or session contains a \".\" which is not allowed by SockJS protocol.");
			return false;
		}

		return true;
	}

	/**
	 * 确保路径中不包含文件扩展名，无论是在文件名中
	 * （例如 "/jsonp.bat"）还是在路径参数之后（"/jsonp;Setup.bat"），
	 * 这些都可能被用于 RFD 漏洞利用。
	 * <p>由于路径的最后一部分预期是传输类型，
	 * 存在扩展名将无法正常工作。我们只需检查是否
	 * 存在任何路径参数（这些参数在请求映射期间会从
	 * SockJS 路径中移除），如果存在则拒绝该请求。
	 */
	private boolean validatePath(ServerHttpRequest request) {
		String path = request.getURI().getPath();
		int index = path.lastIndexOf('/') + 1;
		return (path.indexOf(';', index) == -1);
	}

	protected boolean checkOrigin(ServerHttpRequest request, ServerHttpResponse response, HttpMethod... httpMethods)
			throws IOException {

		if (WebUtils.isSameOrigin(request)) {
			return true;
		}

		if (this.corsConfiguration.checkOrigin(request.getHeaders().getOrigin()) == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Origin header value '" + request.getHeaders().getOrigin() + "' not allowed.");
			}
			response.setStatusCode(HttpStatus.FORBIDDEN);
			return false;
		}

		return true;
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		if (!this.suppressCors && (request.getHeader(HttpHeaders.ORIGIN) != null)) {
			return this.corsConfiguration;
		}
		return null;
	}

	protected void addCacheHeaders(ServerHttpResponse response) {
		response.getHeaders().setCacheControl("public, max-age=" + ONE_YEAR);
		response.getHeaders().setExpires(System.currentTimeMillis() + ONE_YEAR * 1000);
	}

	protected void addNoCacheHeaders(ServerHttpResponse response) {
		response.getHeaders().setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
	}

	protected void sendMethodNotAllowed(ServerHttpResponse response, HttpMethod... httpMethods) {
		logger.warn("Sending Method Not Allowed (405)");
		response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
		response.getHeaders().setAllow(new LinkedHashSet<>(Arrays.asList(httpMethods)));
	}


	/**
	 * 处理原始 WebSocket 通信请求，即不包含任何 SockJS 消息帧。
	 */
	protected abstract void handleRawWebSocketRequest(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler webSocketHandler) throws IOException;

	/**
	 * 处理 SockJS 会话 URL（即特定于传输的请求）。
	 */
	protected abstract void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler, String sessionId, String transport) throws SockJsException;


	private interface SockJsRequestHandler {

		void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException;
	}


	private class InfoHandler implements SockJsRequestHandler {

		private static final String INFO_CONTENT =
				"{\"entropy\":%s,\"origins\":[\"*:*\"],\"cookie_needed\":%s,\"websocket\":%s}";

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
			if (request.getMethod() == HttpMethod.GET) {
				addNoCacheHeaders(response);
				if (checkOrigin(request, response)) {
					response.getHeaders().setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
					String content = String.format(
							INFO_CONTENT, random.nextInt(), isSessionCookieNeeded(), isWebSocketEnabled());
					response.getBody().write(content.getBytes());
				}

			}
			else if (request.getMethod() == HttpMethod.OPTIONS) {
				if (checkOrigin(request, response)) {
					addCacheHeaders(response);
					response.setStatusCode(HttpStatus.NO_CONTENT);
				}
			}
			else {
				sendMethodNotAllowed(response, HttpMethod.GET, HttpMethod.OPTIONS);
			}
		}
	}


	private class IframeHandler implements SockJsRequestHandler {

		private static final String IFRAME_CONTENT =
				"<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" +
				"  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
				"  <title>SockJS iframe</title>\n" +
				"  <script>\n" +
				"    document.domain = document.domain;\n" +
				"    _sockjs_onload = function(){SockJS.bootstrap_iframe();};\n" +
				"  </script>\n" +
				"  <script src=\"%s\"></script>\n" +
				"</head>\n" +
				"<body>\n" +
				"  <h2>Don't panic!</h2>\n" +
				"  <p>This is a SockJS hidden iframe. It's used for cross domain magic.</p>\n" +
				"</body>\n" +
				"</html>";

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
			if (request.getMethod() != HttpMethod.GET) {
				sendMethodNotAllowed(response, HttpMethod.GET);
				return;
			}

			String content = String.format(IFRAME_CONTENT, getSockJsClientLibraryUrl());
			byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
			StringBuilder builder = new StringBuilder("\"0");
			DigestUtils.appendMd5DigestAsHex(contentBytes, builder);
			builder.append('"');
			String etagValue = builder.toString();

			List<String> ifNoneMatch = request.getHeaders().getIfNoneMatch();
			if (!CollectionUtils.isEmpty(ifNoneMatch) && ifNoneMatch.get(0).equals(etagValue)) {
				response.setStatusCode(HttpStatus.NOT_MODIFIED);
				return;
			}

			response.getHeaders().setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
			response.getHeaders().setContentLength(contentBytes.length);

			// No cache in order to check every time if IFrame are authorized
			addNoCacheHeaders(response);
			response.getHeaders().setETag(etagValue);
			response.getBody().write(contentBytes);
		}
	}

}
