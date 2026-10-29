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

package org.springframework.web.server.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;

/**
 * WebHandler 装饰器，在委托的 {@link WebHandler} 之后调用一个或多个
 * {@link WebExceptionHandler WebExceptionHandler}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExceptionHandlingWebHandler extends WebHandlerDecorator {

	private final List<WebExceptionHandler> exceptionHandlers;


	/**
	 * 为给定委托创建一个 {@code ExceptionHandlingWebHandler}。
	 * @param delegate the WebHandler delegate
	 * @param handlers the WebExceptionHandlers to apply
	 */
	public ExceptionHandlingWebHandler(WebHandler delegate, List<WebExceptionHandler> handlers) {
		super(delegate);
		List<WebExceptionHandler> handlersToUse = new ArrayList<>();
		handlersToUse.add(new CheckpointInsertingHandler());
		handlersToUse.addAll(handlers);
		this.exceptionHandlers = Collections.unmodifiableList(handlersToUse);
	}


	/**
	 * 返回已配置异常处理器的只读列表。
	 */
	public List<WebExceptionHandler> getExceptionHandlers() {
		return this.exceptionHandlers;
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		Mono<Void> completion;
		try {
			completion = super.handle(exchange);
		}
		catch (Throwable ex) {
			completion = Mono.error(ex);
		}

		for (WebExceptionHandler handler : this.exceptionHandlers) {
			completion = completion.onErrorResume(ex -> handler.handle(exchange, ex));
		}
		return completion;
	}


	/**
	 * WebExceptionHandler，用于插入带有当前 URL 信息的检查点。
	 * 必须排在第一位，以确保在异常被处理并（例如）转换为错误响应之前
	 * 捕获错误信号。
	 * @since 5.2
 	 */
	private static class CheckpointInsertingHandler implements WebExceptionHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			ServerHttpRequest request = exchange.getRequest();
			String rawQuery = request.getURI().getRawQuery();
			String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
			HttpMethod httpMethod = request.getMethod();
			String description = "HTTP " + httpMethod + " \"" + request.getPath() + query + "\"";
			return Mono.<Void>error(ex).checkpoint(description + " [ExceptionHandlingWebHandler]");
		}
	}

}
