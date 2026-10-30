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

package org.springframework.web.reactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.cors.reactive.PreFlightRequestHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * HTTP 请求处理器/控制器的中央调度器。将请求分派给已注册的处理器进行处理，并提供便捷的映射机制。
 *
 * <p>{@code DispatcherHandler} 从 Spring 配置中发现其所需的委托组件。它会在应用上下文中检测以下内容：
 * <ul>
 * <li>{@link HandlerMapping} -- 将请求映射到处理器对象
 * <li>{@link HandlerAdapter} -- 用于支持任意处理器接口
 * <li>{@link HandlerResultHandler} -- 处理处理器返回值
 * </ul>
 *
 * <p>{@code DispatcherHandler} 本身也被设计为 Spring bean，并实现 {@link ApplicationContextAware} 以访问其运行所在的上下文。如果 {@code DispatcherHandler} 被声明为名为 "webHandler" 的 bean，则会被 {@link WebHttpHandlerBuilder#applicationContext(ApplicationContext)} 发现，后者会将其与 {@code WebFilter}、{@code WebExceptionHandler} 等一起组装成处理链。
 *
 * <p>{@code DispatcherHandler} 的 bean 声明包含在 {@link org.springframework.web.reactive.config.EnableWebFlux @EnableWebFlux} 配置中。
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.0
 * @see WebHttpHandlerBuilder#applicationContext(ApplicationContext)
 */
public class DispatcherHandler implements WebHandler, PreFlightRequestHandler, ApplicationContextAware {

	@Nullable
	private List<HandlerMapping> handlerMappings;

	@Nullable
	private List<HandlerAdapter> handlerAdapters;

	@Nullable
	private List<HandlerResultHandler> resultHandlers;


	/**
	 * 创建一个新的 {@code DispatcherHandler}，需要通过 {@link #setApplicationContext} 为其配置 {@link ApplicationContext}。
	 */
	public DispatcherHandler() {
	}

	/**
	 * 为给定的 {@link ApplicationContext} 创建一个新的 {@code DispatcherHandler}。
	 * @param applicationContext 用于查找处理器 Bean 的应用上下文
	 */
	public DispatcherHandler(ApplicationContext applicationContext) {
		initStrategies(applicationContext);
	}


	/**
	 * 返回在{@link #setApplicationContext 注入的上下文}中按类型检测到的所有 {@link HandlerMapping} bean，并已通过 {@link AnnotationAwareOrderComparator#sort(List)} 排序。
	 * <p><strong>注意：</strong>如果在 {@link #setApplicationContext(ApplicationContext)} 之前调用，此方法可能返回 {@code null}。
	 * @return 包含所配置映射的不可变列表，或 {@code null}
	 */
	@Nullable
	public final List<HandlerMapping> getHandlerMappings() {
		return this.handlerMappings;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		initStrategies(applicationContext);
	}


	protected void initStrategies(ApplicationContext context) {
		Map<String, HandlerMapping> mappingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerMapping.class, true, false);

		ArrayList<HandlerMapping> mappings = new ArrayList<>(mappingBeans.values());
		AnnotationAwareOrderComparator.sort(mappings);
		this.handlerMappings = Collections.unmodifiableList(mappings);

		Map<String, HandlerAdapter> adapterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerAdapter.class, true, false);

		this.handlerAdapters = new ArrayList<>(adapterBeans.values());
		AnnotationAwareOrderComparator.sort(this.handlerAdapters);

		Map<String, HandlerResultHandler> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerResultHandler.class, true, false);

		this.resultHandlers = new ArrayList<>(beans.values());
		AnnotationAwareOrderComparator.sort(this.resultHandlers);
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		if (this.handlerMappings == null) {
			return createNotFoundError();
		}
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			return handlePreFlight(exchange);
		}
		return Flux.fromIterable(this.handlerMappings)
				.concatMap(mapping -> mapping.getHandler(exchange))
				.next()
				.switchIfEmpty(createNotFoundError())
				.flatMap(handler -> invokeHandler(exchange, handler))
				.flatMap(result -> handleResult(exchange, result));
	}

	private <R> Mono<R> createNotFoundError() {
		return Mono.defer(() -> {
			Exception ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "No matching handler");
			return Mono.error(ex);
		});
	}

	private Mono<HandlerResult> invokeHandler(ServerWebExchange exchange, Object handler) {
		if (ObjectUtils.nullSafeEquals(exchange.getResponse().getStatusCode(), HttpStatus.FORBIDDEN)) {
			return Mono.empty();  // CORS rejection
		}
		if (this.handlerAdapters != null) {
			for (HandlerAdapter handlerAdapter : this.handlerAdapters) {
				if (handlerAdapter.supports(handler)) {
					return handlerAdapter.handle(exchange, handler);
				}
			}
		}
		return Mono.error(new IllegalStateException("No HandlerAdapter: " + handler));
	}

	private Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		return getResultHandler(result).handleResult(exchange, result)
				.checkpoint("Handler " + result.getHandler() + " [DispatcherHandler]")
				.onErrorResume(ex ->
						result.applyExceptionHandler(ex).flatMap(exResult -> {
							String text = "Exception handler " + exResult.getHandler() +
									", error=\"" + ex.getMessage() + "\" [DispatcherHandler]";
							return getResultHandler(exResult).handleResult(exchange, exResult).checkpoint(text);
						}));
	}

	private HandlerResultHandler getResultHandler(HandlerResult handlerResult) {
		if (this.resultHandlers != null) {
			for (HandlerResultHandler resultHandler : this.resultHandlers) {
				if (resultHandler.supports(handlerResult)) {
					return resultHandler;
				}
			}
		}
		throw new IllegalStateException("No HandlerResultHandler for " + handlerResult.getReturnValue());
	}

	@Override
	public Mono<Void> handlePreFlight(ServerWebExchange exchange) {
		return Flux.fromIterable(this.handlerMappings != null ? this.handlerMappings : Collections.emptyList())
				.concatMap(mapping -> mapping.getHandler(exchange))
				.switchIfEmpty(Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN)))
				.next()
				.then();
	}

}
