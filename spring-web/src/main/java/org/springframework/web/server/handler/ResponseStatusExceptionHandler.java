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

package org.springframework.web.server.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

/**
 * 通过设置响应状态来处理 {@link ResponseStatusException}。
 *
 * <p>默认情况下，对于成功处理的异常不会显示异常堆栈跟踪。
 * 使用 {@link #setWarnLogCategory(String)} 可启用带堆栈跟踪的日志记录。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ResponseStatusExceptionHandler implements WebExceptionHandler {

	private static final Log logger = LogFactory.getLog(ResponseStatusExceptionHandler.class);


	@Nullable
	private Log warnLogger;


	/**
	 * 设置用于警告日志记录的日志类别。
	 * <p>默认不记录警告日志。指定此设置可将警告日志激活到特定类别。
	 * @since 5.1
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public void setWarnLogCategory(String loggerName) {
		this.warnLogger = LogFactory.getLog(loggerName);
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		if (!updateResponse(exchange.getResponse(), ex)) {
			return Mono.error(ex);
		}

		// Mirrors AbstractHandlerExceptionResolver in spring-webmvc...
		String logPrefix = exchange.getLogPrefix();
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			this.warnLogger.warn(logPrefix + formatError(ex, exchange.getRequest()));
		}
		else if (logger.isDebugEnabled()) {
			logger.debug(logPrefix + formatError(ex, exchange.getRequest()));
		}

		return exchange.getResponse().setComplete();
	}


	private String formatError(Throwable ex, ServerHttpRequest request) {
		String className = ex.getClass().getSimpleName();
		String message = LogFormatUtils.formatValue(ex.getMessage(), -1, true);
		String path = request.getURI().getRawPath();
		return "Resolved [" + className + ": " + message + "] for HTTP " + request.getMethod() + " " + path;
	}

	private boolean updateResponse(ServerHttpResponse response, Throwable ex) {
		boolean result = false;
		HttpStatus httpStatus = determineStatus(ex);
		int code = (httpStatus != null ? httpStatus.value() : determineRawStatusCode(ex));
		if (code != -1) {
			if (response.setRawStatusCode(code)) {
				if (ex instanceof ResponseStatusException) {
					((ResponseStatusException) ex).getResponseHeaders()
							.forEach((name, values) ->
									values.forEach(value -> response.getHeaders().add(name, value)));
				}
				result = true;
			}
		}
		else {
			Throwable cause = ex.getCause();
			if (cause != null) {
				result = updateResponse(response, cause);
			}
		}
		return result;
	}

	/**
	 * 确定给定异常的 HTTP 状态。
	 * <p>自 5.3 起，此方法始终返回 {@code null}，此时会改用
	 * {@link #determineRawStatusCode(Throwable)}。
	 * @param ex the exception to check
	 * @return the associated HTTP status, if any
	 * @deprecated as of 5.3 in favor of {@link #determineRawStatusCode(Throwable)}.
	 */
	@Nullable
	@Deprecated
	protected HttpStatus determineStatus(Throwable ex) {
		return null;
	}

	/**
	 * 确定给定异常的原始状态码。
	 * @param ex the exception to check
	 * @return the associated HTTP status code, or -1 if it can't be derived.
	 * @since 5.3
	 */
	protected int determineRawStatusCode(Throwable ex) {
		if (ex instanceof ResponseStatusException) {
			return ((ResponseStatusException) ex).getRawStatusCode();
		}
		return -1;
	}

}
