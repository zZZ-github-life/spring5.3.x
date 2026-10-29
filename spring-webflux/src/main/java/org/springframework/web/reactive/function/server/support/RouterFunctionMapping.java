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

package org.springframework.web.reactive.function.server.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 支持 {@link RouterFunction RouterFunctions} 的 {@code HandlerMapping} 实现。
 *
 * <p>如果在{@linkplain #RouterFunctionMapping(RouterFunction) 构造时}未提供 {@link RouterFunction}，此映射将检测应用上下文中的所有路由函数，并按{@linkplain org.springframework.core.annotation.Order 顺序}咨询它们。
 * @author Arjen Poutsma
 * @since 5.0
 */
public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

	@Nullable
	private RouterFunction<?> routerFunction;

	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();


	/**
	 * 创建空的 {@code RouterFunctionMapping}。
	 * <p>如果使用此构造函数，此映射将检测应用上下文中可用的所有 {@link RouterFunction} 实例。
	 */
	public RouterFunctionMapping() {
	}

	/**
	 * 使用给定的 {@link RouterFunction} 创建 {@code RouterFunctionMapping}。
	 * <p>如果使用此构造函数，则不会执行应用上下文检测。
	 * @param routerFunction the router function to use for mapping
	 */
	public RouterFunctionMapping(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


	/**
	 * 返回配置的 {@link RouterFunction}。
	 * <p><strong>注意：</strong>当从 ApplicationContext 检测路由函数时，如果在 {@link #afterPropertiesSet()} 之前调用，此方法可能返回 {@code null}。
	 * @return the router function or {@code null}
	 */
	@Nullable
	public RouterFunction<?> getRouterFunction() {
		return this.routerFunction;
	}

	/**
	 * 配置用于反序列化请求正文的 HTTP 消息读取器。
	 * <p>默认设置为 {@link ServerCodecConfigurer} 的默认值。
	 */
	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		this.messageReaders = messageReaders;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(this.messageReaders)) {
			ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
			this.messageReaders = codecConfigurer.getReaders();
		}

		if (this.routerFunction == null) {
			initRouterFunctions();
		}
		if (this.routerFunction != null) {
			RouterFunctions.changeParser(this.routerFunction, getPathPatternParser());
		}

	}

	/**
	 * 通过在应用上下文中检测路由函数来初始化它们。
	 */
	protected void initRouterFunctions() {
		List<RouterFunction<?>> routerFunctions = routerFunctions();
		this.routerFunction = routerFunctions.stream().reduce(RouterFunction::andOther).orElse(null);
		logRouterFunctions(routerFunctions);
	}

	private List<RouterFunction<?>> routerFunctions() {
		return obtainApplicationContext()
				.getBeanProvider(RouterFunction.class)
				.orderedStream()
				.map(router -> (RouterFunction<?>) router)
				.collect(Collectors.toList());
	}

	private void logRouterFunctions(List<RouterFunction<?>> routerFunctions) {
		if (mappingsLogger.isDebugEnabled()) {
			routerFunctions.forEach(function -> mappingsLogger.debug("Mapped " + function));
		}
		else if (logger.isDebugEnabled()) {
			int total = routerFunctions.size();
			String message = total + " RouterFunction(s) in " + formatMappingName();
			if (logger.isTraceEnabled()) {
				if (total > 0) {
					routerFunctions.forEach(function -> logger.trace("Mapped " + function));
				}
				else {
					logger.trace(message);
				}
			}
			else if (total > 0) {
				logger.debug(message);
			}
		}
	}


	@Override
	protected Mono<?> getHandlerInternal(ServerWebExchange exchange) {
		if (this.routerFunction != null) {
			ServerRequest request = ServerRequest.create(exchange, this.messageReaders);
			return this.routerFunction.route(request)
					.doOnNext(handler -> setAttributes(exchange.getAttributes(), request, handler));
		}
		else {
			return Mono.empty();
		}
	}

	@SuppressWarnings("unchecked")
	private void setAttributes(
			Map<String, Object> attributes, ServerRequest serverRequest, HandlerFunction<?> handlerFunction) {

		attributes.put(RouterFunctions.REQUEST_ATTRIBUTE, serverRequest);
		attributes.put(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerFunction);

		PathPattern matchingPattern = (PathPattern) attributes.get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
		if (matchingPattern != null) {
			attributes.put(BEST_MATCHING_PATTERN_ATTRIBUTE, matchingPattern);
		}
		Map<String, String> uriVariables =
				(Map<String, String>) attributes.get(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (uriVariables != null) {
			attributes.put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
		}
	}

}
