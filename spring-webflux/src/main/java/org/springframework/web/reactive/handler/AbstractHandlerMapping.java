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

import java.util.Map;

import org.apache.commons.logging.Log;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsProcessor;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@link org.springframework.web.reactive.HandlerMapping} 实现的抽象基类。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport
		implements HandlerMapping, Ordered, BeanNameAware {

	private static final WebHandler NO_OP_HANDLER = exchange -> Mono.empty();

	/** 用于请求映射的专用“隐藏”日志记录器。 */
	protected final Log mappingsLogger =
			LogDelegateFactory.getHiddenLog(HandlerMapping.class.getName() + ".Mappings");


	private final PathPatternParser patternParser;

	@Nullable
	private CorsConfigurationSource corsConfigurationSource;

	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	@Nullable
	private String beanName;


	public AbstractHandlerMapping() {
		this.patternParser = new PathPatternParser();
	}


	/**
	 * 在所使用底层模式解析器上设置相同属性的快捷方法。更多详情请参见：
	 * <ul>
	 * <li>{@link #getPathPatternParser()} -- 底层模式解析器
	 * <li>{@link PathPatternParser#setCaseSensitive(boolean)} -- 大小写敏感斜杠选项及其默认值。
	 * </ul>
	 * <p><strong>注意：</strong>除上述内容外
	 */
	public void setUseCaseSensitiveMatch(boolean caseSensitiveMatch) {
		this.patternParser.setCaseSensitive(caseSensitiveMatch);
	}

	/**
	 * 在所使用底层模式解析器上设置相同属性的快捷方法。更多详情请参见：
	 * <ul>
	 * <li>{@link #getPathPatternParser()} -- 底层模式解析器
	 * <li>{@link PathPatternParser#setMatchOptionalTrailingSeparator(boolean)} -- 末尾斜杠选项及其默认值。
	 * </ul>
	 */
	public void setUseTrailingSlashMatch(boolean trailingSlashMatch) {
		this.patternParser.setMatchOptionalTrailingSeparator(trailingSlashMatch);
	}

	/**
	 * 返回用于{@link #setCorsConfigurations(Map) CORS 配置检查}的 {@link PathPatternParser} 实例。子类也可以将此模式解析器用于自己的请求映射目的。
	 */
	public PathPatternParser getPathPatternParser() {
		return this.patternParser;
	}

	/**
	 * 基于 URL 模式设置“全局” CORS 配置。默认情况下，第一个匹配的 URL 模式会与处理器级 CORS 配置（如果有）合并。
	 * @see #setCorsConfigurationSource(CorsConfigurationSource)
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		Assert.notNull(corsConfigurations, "corsConfigurations must not be null");
		if (!corsConfigurations.isEmpty()) {
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(this.patternParser);
			source.setCorsConfigurations(corsConfigurations);
			this.corsConfigurationSource = source;
		}
		else {
			this.corsConfigurationSource = null;
		}
	}

	/**
	 * 设置“全局” CORS 配置源。默认情况下，第一个匹配的 URL 模式会与处理器的 CORS 配置（如果有）合并。
	 * @since 5.1
	 * @see #setCorsConfigurations(Map)
	 */
	public void setCorsConfigurationSource(CorsConfigurationSource corsConfigurationSource) {
		Assert.notNull(corsConfigurationSource, "corsConfigurationSource must not be null");
		this.corsConfigurationSource = corsConfigurationSource;
	}

	/**
	 * 配置自定义 {@link CorsProcessor}，用于为请求应用匹配的 {@link CorsConfiguration}。
	 * <p>默认使用 {@link DefaultCorsProcessor} 实例。
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * 返回配置的 {@link CorsProcessor}。
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}

	/**
	 * 指定此 HandlerMapping bean 的顺序值。
	 * <p>默认值为 {@code Ordered.LOWEST_PRECEDENCE}，表示无序。
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected String formatMappingName() {
		return this.beanName != null ? "'" + this.beanName + "'" : "<unknown>";
	}


	@Override
	public Mono<Object> getHandler(ServerWebExchange exchange) {
		return getHandlerInternal(exchange).map(handler -> {
			if (logger.isDebugEnabled()) {
				logger.debug(exchange.getLogPrefix() + "Mapped to " + handler);
			}
			ServerHttpRequest request = exchange.getRequest();
			if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
				CorsConfiguration config = (this.corsConfigurationSource != null ?
						this.corsConfigurationSource.getCorsConfiguration(exchange) : null);
				CorsConfiguration handlerConfig = getCorsConfiguration(handler, exchange);
				config = (config != null ? config.combine(handlerConfig) : handlerConfig);
				if (config != null) {
					config.validateAllowCredentials();
				}
				if (!this.corsProcessor.process(config, exchange) || CorsUtils.isPreFlightRequest(request)) {
					return NO_OP_HANDLER;
				}
			}
			return handler;
		});
	}

	/**
	 * 查找给定请求的处理器，如果未找到特定处理器，则返回空 {@code Mono}。此方法由 {@link #getHandler} 调用。
	 * <p>对于 CORS 预检请求，此方法不应返回预检请求的匹配，而应基于 URL 路径、"Access-Control-Request-Method" 头中的 HTTP 方法以及 "Access-Control-Request-Headers" 头中的头，返回预期实际请求的匹配。
	 * @param exchange 当前交换
	 * @return 匹配的处理器的 {@code Mono}（如果有）
	 */
	protected abstract Mono<?> getHandlerInternal(ServerWebExchange exchange);

	/**
	 * 如果此处理器存在 {@link CorsConfigurationSource}，则返回 {@code true}。
	 * @since 5.2
	 */
	protected boolean hasCorsConfigurationSource(Object handler) {
		return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
	}

	/**
	 * 获取给定处理器的 CORS 配置。
	 * @param handler 要检查的处理器（永不为 {@code null}）
	 * @param exchange 当前交换
	 * @return 处理器的 CORS 配置；如果没有则为 {@code null}
	 */
	@Nullable
	protected CorsConfiguration getCorsConfiguration(Object handler, ServerWebExchange exchange) {
		if (handler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) handler).getCorsConfiguration(exchange);
		}
		return null;
	}

}
