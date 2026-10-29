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

package org.springframework.web.reactive.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;

/**
 * URL 映射的 {@link org.springframework.web.reactive.HandlerMapping} 实现的抽象基类。
 *
 * <p>支持直接匹配（例如注册的 "/test" 匹配 "/test"）以及各种路径模式匹配（例如注册的 "/t*" 模式同时匹配 "/test" 和 "/team"，"/test/*" 匹配 "/test" 下的所有路径，"/test/**" 匹配 "/test" 下的所有路径）。详情请参见 {@link org.springframework.web.util.pattern.PathPattern} 的 javadoc。
 *
 * <p>将搜索所有路径模式，以找到当前请求路径的最具体匹配。最具体的模式定义为捕获变量和通配符最少的最长路径模式。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping {

	private boolean lazyInitHandlers = false;

	private final Map<PathPattern, Object> handlerMap = new LinkedHashMap<>();

	@Nullable
	private BiPredicate<Object, ServerWebExchange> handlerPredicate;


	/**
	 * 设置是否延迟初始化处理器。仅适用于单例处理器，因为原型始终是延迟初始化的。默认值为 "false"，因为及早初始化可以通过直接引用控制器对象来提高效率。
	 * <p>如果希望允许控制器延迟初始化，请将它们设置为 "lazy-init" 并将此标志设置为 true。仅将它们设置为 "lazy-init" 不会生效，因为在这种情况下它们会通过处理器映射中的引用被初始化。
	 */
	public void setLazyInitHandlers(boolean lazyInitHandlers) {
		this.lazyInitHandlers = lazyInitHandlers;
	}

	/**
	 * 返回已注册路径模式和处理器（可能是实际处理器实例，也可能是延迟初始化处理器的 bean 名称）的只读视图。
	 */
	public final Map<PathPattern, Object> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}

	/**
	 * 配置谓词，用于对通过 URL 路径匹配的处理器进行扩展匹配。这允许通过检查请求的其他属性来进一步缩小映射范围。如果谓词返回 "false"，则结果为不匹配，从而允许另一个 {@link org.springframework.web.reactive.HandlerMapping} 进行匹配，或导致 404 (NOT_FOUND) 响应。
	 * @param handlerPredicate a bi-predicate to match the candidate handler
	 * against the current exchange.
	 * @since 5.3.5
	 * @see org.springframework.web.reactive.socket.server.support.WebSocketUpgradeHandlerPredicate
	 */
	public void setHandlerPredicate(BiPredicate<Object, ServerWebExchange> handlerPredicate) {
		this.handlerPredicate = (this.handlerPredicate != null ?
				this.handlerPredicate.and(handlerPredicate) : handlerPredicate);
	}


	@Override
	public Mono<Object> getHandlerInternal(ServerWebExchange exchange) {
		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();
		Object handler;
		try {
			handler = lookupHandler(lookupPath, exchange);
		}
		catch (Exception ex) {
			return Mono.error(ex);
		}
		return Mono.justOrEmpty(handler);
	}

	/**
	 * 查找给定 URL 查找路径的处理器实例。
	 * <p>支持直接匹配（例如注册的 "/test" 匹配 "/test"）以及各种路径模式匹配（例如注册的 "/t*" 同时匹配 "/test" 和 "/team"）。详情请参见 PathPattern 类。
	 * @param lookupPath the URL the handler is mapped to
	 * @param exchange the current exchange
	 * @return the associated handler instance, or {@code null} if not found
	 * @see org.springframework.web.util.pattern.PathPattern
	 */
	@Nullable
	protected Object lookupHandler(PathContainer lookupPath, ServerWebExchange exchange) throws Exception {
		List<PathPattern> matches = null;
		for (PathPattern pattern : this.handlerMap.keySet()) {
			if (pattern.matches(lookupPath)) {
				matches = (matches != null ? matches : new ArrayList<>());
				matches.add(pattern);
			}
		}
		if (matches == null) {
			return null;
		}
		if (matches.size() > 1) {
			matches.sort(PathPattern.SPECIFICITY_COMPARATOR);
			if (logger.isTraceEnabled()) {
				logger.debug(exchange.getLogPrefix() + "Matching patterns " + matches);
			}
		}

		PathPattern pattern = matches.get(0);
		PathContainer pathWithinMapping = pattern.extractPathWithinPattern(lookupPath);
		PathPattern.PathMatchInfo matchInfo = pattern.matchAndExtract(lookupPath);
		Assert.notNull(matchInfo, "Expected a match");

		Object handler = this.handlerMap.get(pattern);

		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}

		if (this.handlerPredicate != null && !this.handlerPredicate.test(handler, exchange)) {
			return null;
		}

		validateHandler(handler, exchange);

		exchange.getAttributes().put(BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
		exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
		exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
		exchange.getAttributes().put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, matchInfo.getUriVariables());

		return handler;
	}

	/**
	 * 根据当前请求校验给定处理器。
	 * <p>默认实现为空。可以在子类中重写，例如强制执行 URL 映射中表达的特定前置条件。
	 * @param handler the handler object to validate
	 * @param exchange current exchange
	 */
	@SuppressWarnings("UnusedParameters")
	protected void validateHandler(Object handler, ServerWebExchange exchange) {
	}

	/**
	 * 为给定 URL 路径注册指定的处理器。
	 * @param urlPaths the URLs that the bean should be mapped to
	 * @param beanName the name of the handler bean
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
		Assert.notNull(urlPaths, "URL path array must not be null");
		for (String urlPath : urlPaths) {
			registerHandler(urlPath, beanName);
		}
	}

	/**
	 * 为给定 URL 路径注册指定的处理器。
	 * @param urlPath the URL the bean should be mapped to
	 * @param handler the handler instance or handler bean name String
	 * (a bean name will automatically be resolved into the corresponding handler bean)
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
		Assert.notNull(urlPath, "URL path must not be null");
		Assert.notNull(handler, "Handler object must not be null");
		Object resolvedHandler = handler;

		// Parse path pattern
		urlPath = prependLeadingSlash(urlPath);
		PathPattern pattern = getPathPatternParser().parse(urlPath);
		if (this.handlerMap.containsKey(pattern)) {
			Object existingHandler = this.handlerMap.get(pattern);
			if (existingHandler != null && existingHandler != resolvedHandler) {
				throw new IllegalStateException(
						"Cannot map " + getHandlerDescription(handler) + " to [" + urlPath + "]: " +
						"there is already " + getHandlerDescription(existingHandler) + " mapped.");
			}
		}

		// Eagerly resolve handler if referencing singleton via name.
		if (!this.lazyInitHandlers && handler instanceof String) {
			String handlerName = (String) handler;
			if (obtainApplicationContext().isSingleton(handlerName)) {
				resolvedHandler = obtainApplicationContext().getBean(handlerName);
			}
		}

		// Register resolved handler
		this.handlerMap.put(pattern, resolvedHandler);
		if (logger.isTraceEnabled()) {
			logger.trace("Mapped [" + urlPath + "] onto " + getHandlerDescription(handler));
		}
	}

	private String getHandlerDescription(Object handler) {
		return (handler instanceof String ? "'" + handler + "'" : handler.toString());
	}


	private static String prependLeadingSlash(String pattern) {
		if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
			return "/" + pattern;
		}
		else {
			return pattern;
		}
	}

}
