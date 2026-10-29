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

package org.springframework.web.server.adapter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.WebHandlerDecorator;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * {@link WebHandler} 到 {@link HttpHandler} 契约的默认适配器。
 *
 * <p>默认情况下会创建并配置一个 {@link DefaultServerWebExchange}，
 * 然后调用目标 {@code WebHandler}。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class HttpWebHandlerAdapter extends WebHandlerDecorator implements HttpHandler {

	/**
	 * 用于客户端断开连接异常的专用日志类别。
	 * <p>Servlet 容器并未暴露客户端断开连接的回调；请参阅
	 * <a href="https://github.com/eclipse-ee4j/servlet-api/issues/44">eclipse-ee4j/servlet-api#44</a>。
	 * <p>为避免日志中充满不必要的堆栈跟踪，我们针对每个服务器尽力识别此类网络故障，
	 * 然后在单独的日志类别下记录一条简单的单行信息（DEBUG 级别），
	 * 或仅在 TRACE 级别记录完整堆栈跟踪。
	 */
	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.server.DisconnectedClient";

	 // Similar declaration exists in AbstractSockJsSession..
	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS = new HashSet<>(
			Arrays.asList("AbortedException", "ClientAbortException", "EOFException", "EofException"));


	private static final Log logger = LogFactory.getLog(HttpWebHandlerAdapter.class);

	private static final Log lostClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);


	private WebSessionManager sessionManager = new DefaultWebSessionManager();

	@Nullable
	private ServerCodecConfigurer codecConfigurer;

	private LocaleContextResolver localeContextResolver = new AcceptHeaderLocaleContextResolver();

	@Nullable
	private ForwardedHeaderTransformer forwardedHeaderTransformer;

	@Nullable
	private ApplicationContext applicationContext;

	/** 是否记录潜在的敏感信息（DEBUG 级别记录表单数据，TRACE 级别记录头部）。 */
	private boolean enableLoggingRequestDetails = false;


	public HttpWebHandlerAdapter(WebHandler delegate) {
		super(delegate);
	}


	/**
	 * 配置一个自定义的 {@link WebSessionManager}，用于管理 Web 会话。
	 * 所提供的实例会被设置到每个创建的 {@link DefaultServerWebExchange} 上。
	 * <p>默认设置为 {@link DefaultWebSessionManager}。
	 * @param sessionManager the session manager to use
	 */
	public void setSessionManager(WebSessionManager sessionManager) {
		Assert.notNull(sessionManager, "WebSessionManager must not be null");
		this.sessionManager = sessionManager;
	}

	/**
	 * 返回已配置的 {@link WebSessionManager}。
	 */
	public WebSessionManager getSessionManager() {
		return this.sessionManager;
	}

	/**
	 * 配置一个自定义的 {@link ServerCodecConfigurer}。所提供的实例会被设置到
	 * 每个创建的 {@link DefaultServerWebExchange} 上。
	 * <p>默认设置为 {@link ServerCodecConfigurer#create()}。
	 * @param codecConfigurer the codec configurer to use
	 */
	public void setCodecConfigurer(ServerCodecConfigurer codecConfigurer) {
		Assert.notNull(codecConfigurer, "ServerCodecConfigurer is required");
		this.codecConfigurer = codecConfigurer;

		this.enableLoggingRequestDetails = false;
		this.codecConfigurer.getReaders().stream()
				.filter(LoggingCodecSupport.class::isInstance)
				.forEach(reader -> {
					if (((LoggingCodecSupport) reader).isEnableLoggingRequestDetails()) {
						this.enableLoggingRequestDetails = true;
					}
				});
	}

	/**
	 * 返回已配置的 {@link ServerCodecConfigurer}。
	 */
	public ServerCodecConfigurer getCodecConfigurer() {
		if (this.codecConfigurer == null) {
			setCodecConfigurer(ServerCodecConfigurer.create());
		}
		return this.codecConfigurer;
	}

	/**
	 * 配置一个自定义的 {@link LocaleContextResolver}。所提供的实例会被设置到
	 * 每个创建的 {@link DefaultServerWebExchange} 上。
	 * <p>默认设置为
	 * {@link org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver}。
	 * @param resolver the locale context resolver to use
	 */
	public void setLocaleContextResolver(LocaleContextResolver resolver) {
		Assert.notNull(resolver, "LocaleContextResolver is required");
		this.localeContextResolver = resolver;
	}

	/**
	 * 返回已配置的 {@link LocaleContextResolver}。
	 */
	public LocaleContextResolver getLocaleContextResolver() {
		return this.localeContextResolver;
	}

	/**
	 * 启用转发头部的处理：要么提取并移除，要么仅移除。
	 * <p>默认情况下不设置。
	 * @param transformer the transformer to use
	 * @since 5.1
	 */
	public void setForwardedHeaderTransformer(ForwardedHeaderTransformer transformer) {
		Assert.notNull(transformer, "ForwardedHeaderTransformer is required");
		this.forwardedHeaderTransformer = transformer;
	}

	/**
	 * 返回已配置的 {@link ForwardedHeaderTransformer}。
	 * @since 5.1
	 */
	@Nullable
	public ForwardedHeaderTransformer getForwardedHeaderTransformer() {
		return this.forwardedHeaderTransformer;
	}

	/**
	 * 配置与 Web 应用关联的 {@code ApplicationContext}（如果已通过
	 * {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext(ApplicationContext)}
	 * 使用某个应用上下文进行了初始化）。
	 * @param applicationContext the context
	 * @since 5.0.3
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 返回已配置的 {@code ApplicationContext}（如果有）。
	 * @since 5.0.3
	 */
	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 必须在设置完所有属性之后调用此方法，以完成初始化。
	 */
	public void afterPropertiesSet() {
		if (logger.isDebugEnabled()) {
			String value = this.enableLoggingRequestDetails ?
					"shown which may lead to unsafe logging of potentially sensitive data" :
					"masked to prevent unsafe logging of potentially sensitive data";
			logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
					"': form data and headers will be " + value);
		}
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		if (this.forwardedHeaderTransformer != null) {
			try {
				request = this.forwardedHeaderTransformer.apply(request);
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to apply forwarded headers to " + formatRequest(request), ex);
				}
				response.setStatusCode(HttpStatus.BAD_REQUEST);
				return response.setComplete();
			}
		}
		ServerWebExchange exchange = createExchange(request, response);

		LogFormatUtils.traceDebug(logger, traceOn ->
				exchange.getLogPrefix() + formatRequest(exchange.getRequest()) +
						(traceOn ? ", headers=" + formatHeaders(exchange.getRequest().getHeaders()) : ""));

		return getDelegate().handle(exchange)
				.doOnSuccess(aVoid -> logResponse(exchange))
				.onErrorResume(ex -> handleUnresolvedError(exchange, ex))
				.then(Mono.defer(response::setComplete));
	}

	protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
		return new DefaultServerWebExchange(request, response, this.sessionManager,
				getCodecConfigurer(), getLocaleContextResolver(), this.applicationContext);
	}

	/**
	 * 为日志记录目的格式化请求，包括 HTTP 方法和 URL。
	 * <p>默认情况下会输出 HTTP 方法、URL 路径以及查询字符串。
	 * @param request the request to format
	 * @return the String to display, never empty or {@code null}
	 */
	protected String formatRequest(ServerHttpRequest request) {
		String rawQuery = request.getURI().getRawQuery();
		String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
		return "HTTP " + request.getMethod() + " \"" + request.getPath() + query + "\"";
	}

	private void logResponse(ServerWebExchange exchange) {
		LogFormatUtils.traceDebug(logger, traceOn -> {
			HttpStatus status = exchange.getResponse().getStatusCode();
			return exchange.getLogPrefix() + "Completed " + (status != null ? status : "200 OK") +
					(traceOn ? ", headers=" + formatHeaders(exchange.getResponse().getHeaders()) : "");
		});
	}

	private String formatHeaders(HttpHeaders responseHeaders) {
		return this.enableLoggingRequestDetails ?
				responseHeaders.toString() : responseHeaders.isEmpty() ? "{}" : "{masked}";
	}

	private Mono<Void> handleUnresolvedError(ServerWebExchange exchange, Throwable ex) {
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		String logPrefix = exchange.getLogPrefix();

		// Sometimes a remote call error can look like a disconnected client.
		// Try to set the response first before the "isDisconnectedClient" check.

		if (response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)) {
			logger.error(logPrefix + "500 Server Error for " + formatRequest(request), ex);
			return Mono.empty();
		}
		else if (isDisconnectedClientError(ex)) {
			if (lostClientLogger.isTraceEnabled()) {
				lostClientLogger.trace(logPrefix + "Client went away", ex);
			}
			else if (lostClientLogger.isDebugEnabled()) {
				lostClientLogger.debug(logPrefix + "Client went away: " + ex +
						" (stacktrace at TRACE level for '" + DISCONNECTED_CLIENT_LOG_CATEGORY + "')");
			}
			return Mono.empty();
		}
		else {
			// After the response is committed, propagate errors to the server...
			logger.error(logPrefix + "Error [" + ex + "] for " + formatRequest(request) +
					", but ServerHttpResponse already committed (" + response.getStatusCode() + ")");
			return Mono.error(ex);
		}
	}

	private boolean isDisconnectedClientError(Throwable ex) {
		String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
		if (message != null) {
			String text = message.toLowerCase();
			if (text.contains("broken pipe") || text.contains("connection reset by peer")) {
				return true;
			}
		}
		return DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.getClass().getSimpleName());
	}

}
