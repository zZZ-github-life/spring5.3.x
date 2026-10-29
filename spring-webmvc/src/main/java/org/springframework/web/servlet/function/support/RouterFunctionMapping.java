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

package org.springframework.web.servlet.function.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringProperties;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 支持 {@link RouterFunction RouterFunctions} 的 {@code HandlerMapping} 实现。
 *
 * <p>如果在
 * {@linkplain #RouterFunctionMapping(RouterFunction) 构建时}未提供 {@link RouterFunction}，
 * 则此映射会检测应用程序上下文中的所有路由函数，并按
 * {@linkplain org.springframework.core.annotation.Order 顺序}咨询它们。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.2
 */
public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，指示 Spring
	 * 忽略 XML，即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");


	@Nullable
	private RouterFunction<?> routerFunction;

	private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

	private boolean detectHandlerFunctionsInAncestorContexts = false;


	/**
	 * 创建一个空的 {@code RouterFunctionMapping}。
	 * <p>如果使用此构造函数，则此映射会检测应用程序上下文中
	 * 可用的所有 {@link RouterFunction} 实例。
	 */
	public RouterFunctionMapping() {
	}

	/**
	 * 使用给定的 {@link RouterFunction} 创建 {@code RouterFunctionMapping}。
	 * <p>如果使用此构造函数，则不会进行应用程序上下文检测。
	 * @param routerFunction the router function to use for mapping
	 */
	public RouterFunctionMapping(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


	/**
	 * 设置要映射到的路由函数。
	 * <p>如果使用此属性，则不会进行应用程序上下文检测。
	 */
	public void setRouterFunction(@Nullable RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}

	/**
	 * 返回所配置的 {@link RouterFunction}。
	 * <p><strong>注意：</strong>当从 ApplicationContext 检测路由函数时，
	 * 如果在 {@link #afterPropertiesSet()} 之前调用，此方法可能返回
	 * {@code null}。
	 * @return the router function or {@code null}
	 */
	@Nullable
	public RouterFunction<?> getRouterFunction() {
		return this.routerFunction;
	}

	/**
	 * 设置要使用的消息体转换器。
	 * <p>这些转换器用于与 HTTP 请求和响应进行双向转换。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * 设置是否在祖先 ApplicationContext 中检测处理器函数。
	 * <p>默认值为 "false"：只检测当前 ApplicationContext 中的处理器函数，
	 * 即仅在此 HandlerMapping 本身所定义的上下文中检测
	 * （通常是当前 DispatcherServlet 的上下文）。
	 * <p>开启此标志还可以检测祖先上下文
	 * （通常是 Spring 根 WebApplicationContext）中的处理器 bean。
	 */
	public void setDetectHandlerFunctionsInAncestorContexts(boolean detectHandlerFunctionsInAncestorContexts) {
		this.detectHandlerFunctionsInAncestorContexts = detectHandlerFunctionsInAncestorContexts;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.routerFunction == null) {
			initRouterFunctions();
		}
		if (CollectionUtils.isEmpty(this.messageConverters)) {
			initMessageConverters();
		}
		if (this.routerFunction != null) {
			PathPatternParser patternParser = getPatternParser();
			if (patternParser == null) {
				patternParser = new PathPatternParser();
				setPatternParser(patternParser);
			}
			RouterFunctions.changeParser(this.routerFunction, patternParser);
		}
	}

	/**
	 * 检测当前应用程序上下文中的所有{@linkplain RouterFunction 路由函数}。
	 */
	private void initRouterFunctions() {
		List<RouterFunction<?>> routerFunctions = obtainApplicationContext()
				.getBeanProvider(RouterFunction.class)
				.orderedStream()
				.map(router -> (RouterFunction<?>) router)
				.collect(Collectors.toList());

		ApplicationContext parentContext = obtainApplicationContext().getParent();
		if (parentContext != null && !this.detectHandlerFunctionsInAncestorContexts) {
			parentContext.getBeanProvider(RouterFunction.class).stream().forEach(routerFunctions::remove);
		}

		this.routerFunction = routerFunctions.stream().reduce(RouterFunction::andOther).orElse(null);
		logRouterFunctions(routerFunctions);
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

	/**
	 * 初始化一组默认的{@linkplain HttpMessageConverter 消息转换器}。
	 */
	private void initMessageConverters() {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>(4);
		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(new StringHttpMessageConverter());

		if (!shouldIgnoreXml) {
			try {
				messageConverters.add(new SourceHttpMessageConverter<>());
			}
			catch (Error err) {
				// Ignore when no TransformerFactory implementation is available
			}
		}
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());

		this.messageConverters = messageConverters;
	}


	@Override
	@Nullable
	protected Object getHandlerInternal(HttpServletRequest servletRequest) throws Exception {
		if (this.routerFunction != null) {
			ServerRequest request = ServerRequest.create(servletRequest, this.messageConverters);
			HandlerFunction<?> handlerFunction = this.routerFunction.route(request).orElse(null);
			setAttributes(servletRequest, request, handlerFunction);
			return handlerFunction;
		}
		else {
			return null;
		}
	}

	private void setAttributes(HttpServletRequest servletRequest, ServerRequest request,
			@Nullable HandlerFunction<?> handlerFunction) {

		PathPattern matchingPattern =
				(PathPattern) servletRequest.getAttribute(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
		if (matchingPattern != null) {
			servletRequest.removeAttribute(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
			servletRequest.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, matchingPattern.getPatternString());
		}
		servletRequest.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerFunction);
		servletRequest.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, request);
	}

}
